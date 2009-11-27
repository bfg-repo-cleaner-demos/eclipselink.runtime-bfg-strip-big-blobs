/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 *     07/16/2009-2.0 Guy Pelletier 
 *       - 277039: JPA 2.0 Cache Usage Settings
 ******************************************************************************/  
package org.eclipse.persistence.queries;

import java.util.*;
import java.io.*;
import org.eclipse.persistence.internal.helper.*;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.DescriptorQueryManager;
import org.eclipse.persistence.expressions.*;
import org.eclipse.persistence.internal.expressions.*;
import org.eclipse.persistence.internal.databaseaccess.*;
import org.eclipse.persistence.internal.queries.*;
import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.internal.sessions.remote.*;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.UnitOfWorkImpl;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.sessions.remote.*;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.sessions.Record;
import org.eclipse.persistence.sessions.SessionProfiler;

/**
 * <p><b>Purpose</b>:
 * Abstract class for all database query objects.
 * DatabaseQuery is a visible class to the EclipseLink user. Users create an appropriate
 * query by creating an instance of a concrete subclasses of DatabaseQuery.
 *
 * <p><b>Responsibilities</b>:
 * <ul>
 * <li> Provide a common protocol for query objects.
 * <li> Defines a generic execution interface.
 * <li> Provides  query property values
 * <li> Holds arguments to the query
 * </ul>
 *
 * @author Yvon Lavoie
 * @since TOPLink/Java 1.0
 */
public abstract class DatabaseQuery implements Cloneable, Serializable {

    /** Queries can be given a name and registered with a descriptor to allow common queries to be reused. */
    protected String name;

    /** Arguments can be given and specified to predefined queries to allow reuse. */
    protected List<String> arguments;
    
    /** PERF: Argument fields are cached in prepare to avoid rebuilding on each execution. */
    protected List<DatabaseField> argumentFields;

    /** Arguments values can be given and specified to predefined queries to allow reuse. */
    protected List<Object> argumentValues;

    /** Needed to differentiate queries with the same name. */
    protected List<Class> argumentTypes;

    /** Used to build a list of argumentTypes by name pre-initialization     */
    protected List<String> argumentTypeNames;

    /** The descriptor cached on the prepare for object level queries. */
    protected transient ClassDescriptor descriptor;

    /** The query mechanism determines the mechanism on how the database will be accessed. */
    protected DatabaseQueryMechanism queryMechanism;

    /** A redirector allows for a queries execution to be the execution of a piece of code. */
    protected QueryRedirector redirector;
    
    /**
     * Can be set to true in the case there is a redirector or a default redirector but
     * the user does not want the query redirected.
     */
    protected boolean doNotRedirect = false;

    /** Flag used for a query to bypass the identitymap and unit of work. */

    // Bug#3476483 - Restore shouldMaintainCache to previous state after reverse of bug fix 3240668
    protected boolean shouldMaintainCache;
    
    /** JPA flags to control the shared cache*/
    protected boolean shouldRetrieveBypassCache = false;
    protected boolean shouldStoreBypassCache = false;

    /** Internally used by the mappings as a temporary store. */
    protected Map<Object, Object> properties;

    /** Only used after the query is cloned for execution to store the session under which the query was executed. */
    protected transient AbstractSession session;
    
    /** Only used after the query is cloned for execution to store the execution session under which the query was executed. */
    protected transient AbstractSession executionSession;

    /** Connection to use for database access, required for server session connection pooling. */
    protected transient Accessor accessor;

    /** Mappings and the descriptor use parameterized mechanisms that will be translated with the data from the row. */
    protected AbstractRecord translationRow;

    /** Internal flag used to bypass user define queries when executing one for custom sql/query support. */
    protected boolean isUserDefined;

    /** Policy that determines how the query will cascade to its object's parts. */
    protected int cascadePolicy;

    /** Used to override the default session in the session broker. */
    protected String sessionName;

    /** Queries prepare common stated in themselves. */
    protected boolean isPrepared;

    /** Used to indicate whether or not the call needs to be cloned. */
    protected boolean shouldCloneCall;
    
    
    /** Allow for the prepare of queries to be turned off, this allow for dynamic non-pre SQL generated queries. */
    protected boolean shouldPrepare;

    /** Bind all arguments to the SQL statement. */

    // Has False, Undefined or True value. In case of Undefined -
    // Session's shouldBindAllParameters() defines whether to bind or not.
    protected Boolean shouldBindAllParameters;

    /** Cache the prepared statement, this requires full parameter binding as well. */

    // Has False, Undefined or True value. In case of Undefined -
    // Session's shouldCacheAllStatements() defines whether to cache or not.
    protected Boolean shouldCacheStatement;

    /** Use the WrapperPolicy for the objects returned by the query */
    protected boolean shouldUseWrapperPolicy;
    
    /** 
     * Table per class requires multiple query executions. Internally we
     * prepare those queries and cache them against the source mapping's 
     * selection query. When queries are executed they are cloned so we
     * need a mechanism to keep a reference back to the actual selection
     * query so that we can successfully look up and chain query executions
     * within a table per class inheritance hierarchy.
     */
    protected DatabaseMapping sourceMapping;

    /**
     * queryTimeout has three possible settings: DefaultTimeout, NoTimeout, and 1..N
     * This applies to both DatabaseQuery.queryTimeout and DescriptorQueryManager.queryTimeout
     *
     * DatabaseQuery.queryTimeout:
     * - DefaultTimeout: get queryTimeout from DescriptorQueryManager
     * - NoTimeout, 1..N: overrides queryTimeout in DescriptorQueryManager
     *
     * DescriptorQueryManager.queryTimeout:
     * - DefaultTimeout: get queryTimeout from parent DescriptorQueryManager. If there is no
     * parent, default to NoTimeout
     * - NoTimeout, 1..N: overrides parent queryTimeout
     */
    protected int queryTimeout;

    /* Used as default for read, means shallow write for modify. */
    public static final int NoCascading = 1;

    /* Used as default for write, used for refreshing to refresh the whole object. */
    public static final int CascadePrivateParts = 2;

    /* Currently not supported, used for deep write/refreshes/reads in the future. */
    public static final int CascadeAllParts = 3;

    /* Used by the unit of work. */
    public static final int CascadeDependentParts = 4;

    /* Used by aggregate Collections:  As aggregates delete at update time, cascaded deletes
     * must know to stop when entering postDelete for a particular mapping.  Only used by the
     * aggregate collection when update is occurring in a UnitOfWork
     * CR 2811
     */
    public static final int CascadeAggregateDelete = 5;

    /*
     * Used when refreshing should check the mappings to determine if a particular
     * mapping should be cascaded.
     */
    public static final int CascadeByMapping = 6;

    /** Used for adding hints to the query string in oracle */
    protected String hintString;

    /*
     * Stores the FlushMode of this Query.  This is only applicable when executed
     * in a flushable UnitOfWork and will be ignored otherwise.
     */
    protected Boolean flushOnExecute;

    /** PERF: Determines if the query has already been cloned for execution, to avoid duplicate cloning. */
    protected boolean isExecutionClone;
    
    /** PERF: Store if this query will use the descriptor custom query. */
    protected volatile Boolean isCustomQueryUsed;    
    
    /** Allow connection unwrapping to be configured. */
    protected boolean isNativeConnectionRequired;
    
    /**
     * PUBLIC:
     * Initialize the state of the query
     */
    public DatabaseQuery() {
        this.shouldMaintainCache = true;
        // bug 3524620: lazy-init query mechanism
        //this.queryMechanism = new ExpressionQueryMechanism(this);
        this.isUserDefined = false;
        this.cascadePolicy = NoCascading;
        this.isPrepared = false;
        this.shouldUseWrapperPolicy = true;
        this.queryTimeout = DescriptorQueryManager.DefaultTimeout;
        this.shouldPrepare = true;
        this.shouldCloneCall = false;
        this.shouldBindAllParameters = null;
        this.shouldCacheStatement = null;
        this.isExecutionClone = false;
    }
    
    /**
     * PUBLIC:
     * Add the argument named argumentName.
     * This will cause the translation of references of argumentName in the receiver's expression,
     * with the value of the argument as supplied to the query in order from executeQuery()
     */
    public void addArgument(String argumentName) {
        // CR#3545 - Changed the default argument type to make argument types work more consistently
        // with the SDK
        addArgument(argumentName, java.lang.Object.class);
    }

    /**
     * PUBLIC:
     * Add the argument named argumentName and its class type.
     * This will cause the translation of references of argumentName in the receiver's expression,
     * with the value of the argument as supplied to the query in order from executeQuery().
     * Specifying the class type is important if identically named queries are used but with
     * different argument lists.
     */
    public void addArgument(String argumentName, Class type) {
        getArguments().add(argumentName);
        getArgumentTypes().add(type);
        getArgumentTypeNames().add(type.getName());
    }

    /**
     * PUBLIC:
     * Add the argument named argumentName and its class type.
     * This will cause the translation of references of argumentName in the receiver's expression,
     * with the value of the argument as supplied to the query in order from executeQuery().
     * Specifying the class type is important if identically named queries are used but with
     * different argument lists.
     */
    public void addArgument(String argumentName, String typeAsString) {
        getArguments().add(argumentName);
        //bug 3197587
        getArgumentTypes().add(Helper.getObjectClass(ConversionManager.loadClass(typeAsString)));
        getArgumentTypeNames().add(typeAsString);
    }

    /**
     * INTERNAL:
     * Add an argument to the query, but do not resovle the class yet.
     * This is useful for building a query without putting the domain classes
     * on the classpath for the Mapping Workbench.
     */
    public void addArgumentByTypeName(String argumentName, String typeAsString) {
        getArguments().add(argumentName);
        getArgumentTypeNames().add(typeAsString);
    }

    /**
     * PUBLIC:
     * Add the argumentValue.
     * Argument values must be added in the same order the arguments are defined.
     */
    public void addArgumentValue(Object argumentValue) {
        getArgumentValues().add(argumentValue);
    }

    /**
     * PUBLIC:
     * Add the argumentValues to the query.
     * Argument values must be added in the same order the arguments are defined.
     */
    public void addArgumentValues(List theArgumentValues) {
        getArgumentValues().addAll(theArgumentValues);
    }

    /**
     * PUBLIC:
     * Used to define a store procedure or SQL query.
     * This may be used for multiple SQL executions to be mapped to a single query.
     * This cannot be used for cursored selects, delete alls or does exists.
     */
    public void addCall(Call call) {
        setQueryMechanism(call.buildQueryMechanism(this, getQueryMechanism()));
        // Must un-prepare is prepare as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC:
     * Used to define a statement level query.
     * This may be used for multiple SQL executions to be mapped to a single query.
     * This cannot be used for cursored selects, delete all(s) or does exists.
     */
    public void addStatement(SQLStatement statement) {
        // bug 3524620: lazy-init query mechanism
        if (!hasQueryMechanism()) {
            setQueryMechanism(new StatementQueryMechanism(this));
        } else if (!getQueryMechanism().isStatementQueryMechanism()) {
            setQueryMechanism(new StatementQueryMechanism(this));
        }
        ((StatementQueryMechanism)getQueryMechanism()).getSQLStatements().addElement(statement);
        // Must un-prepare is prepare as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC:
     * Bind all arguments to any SQL statement.
     */
    public void bindAllParameters() {
        setShouldBindAllParameters(true);
    }

    /**
     * INTERNAL:
     * In the case of EJBQL, an expression needs to be generated. Build the required expression.
     */
    protected void buildSelectionCriteria(AbstractSession session) {
        this.getQueryMechanism().buildSelectionCriteria(session);
    }

    /**
     * PUBLIC:
     * Cache the prepared statements, this requires full parameter binding as well.
     */
    public void cacheStatement() {
        setShouldCacheStatement(true);
    }

    /**
     * PUBLIC:
     * Cascade the query and its properties on the queries object(s) and all objects related to the queries object(s).
     * This includes private and independent relationships, but not read-only relationships.
     * This will still stop on uninstantiated indirection objects except for deletion.
     * Great caution should be used in using the property as the query may effect a large number of objects.
     * This policy is used by the unit of work to ensure persistence by reachability.
     */
    public void cascadeAllParts() {
        setCascadePolicy(CascadeAllParts);
    }

    /**
     * PUBLIC:
     * Cascade the query and its properties on the queries object(s) and all related objects where the mapping has
     * been set to cascade the merge.
     */
    public void cascadeByMapping() {
        setCascadePolicy(CascadeByMapping);
    }

    /**
     * INTERNAL:
     * Used by unit of work, only cascades constraint dependencies.
     */
    public void cascadeOnlyDependentParts() {
        setCascadePolicy(CascadeDependentParts);
    }

    /**
     * PUBLIC:
     * Cascade the query and its properties on the queries object(s)
     * and all privately owned objects related to the queries object(s).
     * This is the default for write and delete queries.
     * This policy should normally be used for refreshing, otherwise you could refresh half of any object.
     */
    public void cascadePrivateParts() {
        setCascadePolicy(CascadePrivateParts);
    }

    /**
     * INTERNAL:
     * Ensure that the descriptor has been set.
     */
    public void checkDescriptor(AbstractSession session) throws QueryException {
    }

    /**
     * INTERNAL:
     * Check to see if this query already knows the return value without performing any further work.
     */
    public Object checkEarlyReturn(AbstractSession session, AbstractRecord translationRow) {
        return null;
    }

    /**
     * INTERNAL:
     * Check to see if a custom query should be used for this query.
     * This is done before the query is copied and prepared/executed.
     * null means there is none.
     */
    protected DatabaseQuery checkForCustomQuery(AbstractSession session, AbstractRecord translationRow) {
        return null;
    }

    /**
     * INTERNAL:
     * Check to see if this query needs to be prepare and prepare it.
     * The prepare is done on the original query to ensure that the work is not repeated.
     */
    public void checkPrepare(AbstractSession session, AbstractRecord translationRow) {
        this.checkPrepare(session, translationRow, false);
    }
    
    /**
     * INTERNAL:
     * Check to see if this query needs to be prepare and prepare it.
     * The prepare is done on the original query to ensure that the work is not repeated.
     */
    public void checkPrepare(AbstractSession session, AbstractRecord translationRow, boolean force) {
        // This query is first prepared for global common state, this must be synced.
        if (!this.isPrepared) {// Avoid the monitor is already prepare, must check again for concurrency.
            // Profile the query preparation time.
            session.startOperationProfile(SessionProfiler.QUERY_PREPARE, this, SessionProfiler.ALL);
            // If this query will use the custom query, do not prepare.
            if ((!force) && shouldPrepare() && (checkForCustomQuery(session, translationRow) != null)) {
                // Profile the query preparation time.
                session.endOperationProfile(SessionProfiler.QUERY_PREPARE, this, SessionProfiler.ALL);
                return;
            }
            // Prepared queries cannot be custom as then they would never have been prepared.
            synchronized (this) {
                if (!isPrepared()) {
                    // When custom SQL is used there is a possibility that the SQL contains the # token.
                    // Avoid this by telling the call if this is custom SQL with parameters.
                    // This must not be called for SDK calls.
                    if ((isReadQuery() || isDataModifyQuery()) && isCallQuery() && (getQueryMechanism() instanceof CallQueryMechanism) && ((translationRow == null) || translationRow.isEmpty())) {
                        // Must check for read object queries as the row will be empty until the prepare.
                        if (isReadObjectQuery() || isUserDefined()) {
                            ((CallQueryMechanism)getQueryMechanism()).setCallHasCustomSQLArguments();
                        }
                    } else if (isCallQuery() && (getQueryMechanism() instanceof CallQueryMechanism)) {
                        ((CallQueryMechanism)getQueryMechanism()).setCallHasCustomSQLArguments();
                    }
                    setSession(session);// Session is required for some init stuff.
                    prepare();
                    setSession(null);
                    setIsPrepared(true);// MUST not set prepare until done as other thread may hit before finishing the prepare.
                }
            }
            // Profile the query preparation time.
            session.endOperationProfile(SessionProfiler.QUERY_PREPARE, this, SessionProfiler.ALL);
        }
    }

    /**
     * INTERNAL:
     * Clone the query
     */
    public Object clone() {
        try {
            DatabaseQuery cloneQuery = (DatabaseQuery)super.clone();

            // Keep a reference back to the original source query.
            cloneQuery.sourceMapping = this.sourceMapping;
            
            // partial fix for 3054240 
            // need to pay attention to other components of the query, too  MWN
            if (cloneQuery.properties != null) {
                if (cloneQuery.properties.isEmpty()) {
                    cloneQuery.setProperties(null);
                } else {
                    cloneQuery.setProperties(new HashMap(getProperties()));
                }
            }

            // bug 3524620: now that the query mechanism is lazy-init'd,
            // only clone the query mechanism if we have one.
            if (hasQueryMechanism()) {
                cloneQuery.setQueryMechanism(getQueryMechanism().clone(cloneQuery));
            }
            cloneQuery.setIsPrepared(isPrepared());// Setting some things will trigger unprepare.
            return cloneQuery;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * INTERNAL
     * Used to give the subclasses opportunity to copy aspects of the cloned query
     * to the original query.
     */
    protected void clonedQueryExecutionComplete(DatabaseQuery query, AbstractSession session) {
        //no-op for this class
    }

    /**
     * INTERNAL:
     * Convert all the class-name-based settings in this query to actual class-based
     * settings
     * This method is implemented by subclasses as necessary.
     * @param classLoader 
     */
    public void convertClassNamesToClasses(ClassLoader classLoader){
        // note: normally we would fix the argument types here, but they are already
        // lazily instantiated
    };

    /**
     * PUBLIC:
     * Do not Bind all arguments to any SQL statement.
     */
    public void dontBindAllParameters() {
        setShouldBindAllParameters(false);
    }

    /**
     * PUBLIC:
     * Don't cache the prepared statements, this requires full parameter binding as well.
     */
    public void dontCacheStatement() {
        setShouldCacheStatement(false);
    }

    /**
     * PUBLIC:
     * Do not cascade the query and its properties on the queries object(s) relationships.
     * This does not effect the queries private parts but only the object(s) direct row-level attributes.
     * This is the default for read queries and can be used in writing if it is known that only
     * row-level attributes changed, or to resolve circular foreign key dependencies.
     */
    public void dontCascadeParts() {
        setCascadePolicy(NoCascading);
    }

    /**
     * PUBLIC:
     * Set for the identity map (cache) to be ignored completely.
     * The cache check will be skipped and the result will not be put into the identity map.
     * This can be used to retrieve the exact state of an object on the database.
     * By default the identity map is always maintained.
     */
    public void dontMaintainCache() {
        setShouldMaintainCache(false);
    }

    /**
     * INTERNAL:
     * Execute the query
     *
     * @exception  DatabaseException - an error has occurred on the database.
     * @exception  OptimisticLockException - an error has occurred using the optimistic lock feature.
     * @return - the result of executing the query.
     */
    public abstract Object executeDatabaseQuery() throws DatabaseException, OptimisticLockException;

    /**
     * INTERNAL:
     * Override query execution where Session is a UnitOfWork.
     * <p>
     * If there are objects in the cache return the results of the cache lookup.
     *
     * @param unitOfWork - the session in which the receiver will be executed.
     * @param translationRow - the arguments
     * @exception  DatabaseException - an error has occurred on the database.
     * @exception  OptimisticLockException - an error has occurred using the optimistic lock feature.
     * @return An object, the result of executing the query.
     */
    public Object executeInUnitOfWork(UnitOfWorkImpl unitOfWork, AbstractRecord translationRow) throws DatabaseException, OptimisticLockException {
        return execute(unitOfWork, translationRow);
    }

    /**
     * INTERNAL:
     * Execute the query. If there are objects in the cache  return the results
     * of the cache lookup.
     *
     * @param session - the session in which the receiver will be executed.
     * @exception  DatabaseException - an error has occurred on the database.
     * @exception  OptimisticLockException - an error has occurred using the optimistic lock feature.
     * @return An object, the result of executing the query.
     */
    public Object execute(AbstractSession session, AbstractRecord translationRow) throws DatabaseException, OptimisticLockException {
        DatabaseQuery queryToExecute = this;

        QueryRedirector localRedirector = getRedirector();
        // refactored redirection for bug 3241138
        if ( localRedirector!= null) {
            return redirectQuery(localRedirector, queryToExecute, session, translationRow);
        }

        // Bug 5529564 - If this is a user defined selection query (custom SQL), 
        // prepare the query before hand so that we may look up the correct fk 
        // values from the query parameters when checking early return.
        if (queryToExecute.isCustomSelectionQuery() && queryToExecute.shouldPrepare()) {
            queryToExecute.checkPrepare(session, translationRow);    
        }
        
        // This allows the query to check the cache or return early without doing any work.
        Object earlyReturn = queryToExecute.checkEarlyReturn(session, translationRow);
        // If know not to exist (checkCacheOnly, deleted, null primary key), return null.
        if (earlyReturn == InvalidObject.instance) {
            return null;
        }
        if (earlyReturn != null) {
            return earlyReturn;
        }
        
        boolean hasCustomQuery = false;
        if (!isPrepared() && shouldPrepare()) {
            // Prepared queries cannot be custom as then they would never have been prepared.
            DatabaseQuery customQuery = checkForCustomQuery(session, translationRow);
            if (customQuery != null) {
                hasCustomQuery = true;
                // The custom query will be used not the original.
                queryToExecute = customQuery;
            }
        }

        // PERF: Queries need to be cloned for execution as they may be
        // concurrently reused, and execution specific state is stored in the clone.
        // In some case the query is known to be a one off, or cloned elsewhere
        // so the query keeps track if it has been cloned already.
        queryToExecute = session.prepareDatabaseQuery(queryToExecute);

        if (queryToExecute.shouldPrepare()) {
            queryToExecute.checkPrepare(session, translationRow);
        }

        // Then cloned for concurrency and repeatable execution.
        if (!queryToExecute.isExecutionClone()) {
            queryToExecute = (DatabaseQuery)queryToExecute.clone();
        }
        // Check for query argument values.
        if ((this.argumentValues != null) && (!this.argumentValues.isEmpty()) && translationRow.isEmpty()) {
            translationRow = rowFromArguments(this.argumentValues);
        }
        queryToExecute.setTranslationRow(translationRow);
        
        // If the prepare has been disable the clone is prepare dynamically to not parameterize the SQL.
        if (!queryToExecute.shouldPrepare()) {
            queryToExecute.checkPrepare(session, translationRow);
        }
        queryToExecute.setSession(session);
        if (hasCustomQuery) {
            prepareCustomQuery(queryToExecute);
            localRedirector = queryToExecute.getRedirector();
            // refactored redirection for bug 3241138
            if ( localRedirector!= null) {
                return redirectQuery(localRedirector, queryToExecute, session, queryToExecute.getTranslationRow());
            }
        }
        queryToExecute.prepareForExecution();

        // Then executed.
        Object result = queryToExecute.executeDatabaseQuery();

        // Give the subclasses the opportunity to retrieve aspects of the cloned query.
        clonedQueryExecutionComplete(queryToExecute, session);
        return result;
    }

    /**
     * INTERNAL:
     * Extract the correct query result from the transporter.
     */
    public Object extractRemoteResult(Transporter transporter) {
        return transporter.getObject();
    }

    /**
     * INTERNAL:
     * Return the accessor.
     */
    public Accessor getAccessor() {
        return accessor;
    }

    /**
     * INTERNAL:
     * Return the arguments for use with the pre-defined query option
     */
    public List<String> getArguments() {
        if (arguments == null) {
            arguments = new ArrayList<String>();
        }
        return arguments;
    }

    /**
     * INTERNAL:
     * Return the argumentTypes for use with the pre-defined query option
     */
    public List<Class> getArgumentTypes() {
        if ((this.argumentTypes == null) || (this.argumentTypes.isEmpty()
                && (this.argumentTypeNames != null) && !this.argumentTypeNames.isEmpty())) {
            this.argumentTypes = new ArrayList<Class>();
            // Bug 3256198 - lazily initialize the argument types from their class names
            if (this.argumentTypeNames != null) {
                Iterator args = this.argumentTypeNames.iterator();
                while (args.hasNext()) {
                    String argumentTypeName = (String)args.next();
                    this.argumentTypes.add(Helper.getObjectClass(ConversionManager.loadClass(argumentTypeName)));
                }
            }
        }
        return this.argumentTypes;
    }

    /**
     * INTERNAL:
     * Return the argumentTypeNames for use with the pre-defined query option
     * These are used pre-initialization to construct the argumentTypes list.
     */
    public List<String> getArgumentTypeNames() {
        if (argumentTypeNames == null) {
            argumentTypeNames = org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance();
        }
        return argumentTypeNames;
    }

    /**
     * INTERNAL:
     * Set the argumentTypes for use with the pre-defined query option
     */
    public void setArgumentTypes(List<Class> argumentTypes) {
        this.argumentTypes = argumentTypes;
        // bug 3256198 - ensure the list of type names matches the argument types.
        getArgumentTypeNames().clear();
        for (Class type : argumentTypes) {
            this.argumentTypeNames.add(type.getName());
        }
    }

    /**
     * INTERNAL:
     * Set the argumentTypes for use with the pre-defined query option
     */
    public void setArgumentTypeNames(List<String> argumentTypeNames) {
        this.argumentTypeNames = argumentTypeNames;
    }

    /**
     * INTERNAL:
     * Set the arguments for use with the pre-defined query option.
     * Maintain the argumentTypes as well.
     */
    public void setArguments(List<String> arguments) {
        List<Class> types = new ArrayList<Class>(arguments.size());
        List<String> typeNames = new ArrayList<String>(arguments.size());
        List<DatabaseField> typeFields = new ArrayList<DatabaseField>(arguments.size());
        int size = arguments.size();
        for (int index = 0; index < size; index++) {
            types.add(Object.class);
            typeNames.add("java.lang.Object");
            DatabaseField field = new DatabaseField(arguments.get(index));
            typeFields.add(field);
        }
        this.arguments = arguments;
        this.argumentTypes = types;
        this.argumentTypeNames = typeNames;   
        this.argumentFields = typeFields;       
    }

    /**
     * INTERNAL:
     * Return the argumentValues for use with argumented queries.
     */
    public List<Object> getArgumentValues() {
        if (this.argumentValues == null) {
            this.argumentValues = new ArrayList<Object>();
        }
        return this.argumentValues;
    }

    /**
     * INTERNAL:
     * Set the argumentValues for use with argumented queries.
     */
    public void setArgumentValues(List<Object> theArgumentValues) {
        this.argumentValues = theArgumentValues;
    }

    /**
     * OBSOLETE:
     * Return the call for this query.
     * This call contains the SQL and argument list.
     * @see #getDatasourceCall()
     */
    public DatabaseCall getCall() {
        Call call = getDatasourceCall();
        if (call instanceof DatabaseCall) {
            return (DatabaseCall)call;
        } else {
            return null;
        }
    }


    /**
     * ADVANCED:
     * Return the call for this query.
     * This call contains the SQL and argument list.
     * @see #prepareCall(Session, Record);
     */
    public Call getDatasourceCall() {
        Call call = null;
        if (getQueryMechanism() instanceof DatasourceCallQueryMechanism) {
            DatasourceCallQueryMechanism mechanism = (DatasourceCallQueryMechanism)getQueryMechanism();
            call = mechanism.getCall();
            // If has multiple calls return the first one.
            if (mechanism.hasMultipleCalls()) {
                call = (Call)mechanism.getCalls().get(0);
            }
        }
        if ((call == null) && getQueryMechanism().isJPQLCallQueryMechanism()) {
            call = ((JPQLCallQueryMechanism)getQueryMechanism()).getJPQLCall();
        }
        return call;
    }

    /**
     * ADVANCED:
     * Return the calls for this query.  This method can be called for queries with multiple calls
     * This call contains the SQL and argument list.
     * @see #prepareCall(Session, Record);
     */
    public List getDatasourceCalls() {
        List calls = new Vector();
        if (getQueryMechanism() instanceof DatasourceCallQueryMechanism) {
            DatasourceCallQueryMechanism mechanism = (DatasourceCallQueryMechanism)getQueryMechanism();

            // If has multiple calls return the first one.
            if (mechanism.hasMultipleCalls()) {
                calls = mechanism.getCalls();
            } else {
                calls.add(mechanism.getCall());
            }
        }
        if ((calls.isEmpty()) && getQueryMechanism().isJPQLCallQueryMechanism()) {
            calls.add(((JPQLCallQueryMechanism)getQueryMechanism()).getJPQLCall());
        }
        return calls;
    }

    /**
     * INTERNAL:
     * Return the cascade policy.
     */
    public int getCascadePolicy() {
        return cascadePolicy;
    }

    /**
     * INTERNAL:
     * Return the descriptor assigned with the reference class
     */
    public ClassDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     *  PUBLIC:
     *  Return the name of the query
     */
    public String getName() {
        return name;
    }

    /**
     * INTERNAL:
     * Property support for use by mappings.
     */
    public Map<Object, Object> getProperties() {
        if (this.properties == null) {
            //Lazy initialize to conserve space and allocation time.
            this.properties = new HashMap();
        }
        return this.properties;
    }

    /**
     * INTERNAL:
     * Property support used by mappings to store temporary stuff in the query.
     */
    public synchronized Object getProperty(Object property) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(property);
    }

    /**
     * INTERNAL:
     * Return the mechanism assigned to the query
     */
    public DatabaseQueryMechanism getQueryMechanism() {
        // Bug 3524620 - lazy init
        if (this.queryMechanism == null) {
            this.queryMechanism = new ExpressionQueryMechanism(this);
        }
        return this.queryMechanism;
    }

    /**
     * INTERNAL:
     * Check if the mechanism has been set yet, used for lazy init.
     */
    public boolean hasQueryMechanism() {
        return (this.queryMechanism != null);
    }

    /**
     * PUBLIC:
     * Return the number of seconds the driver will wait for a Statement to execute to the given number of seconds.
     *
     * @see DescriptorQueryManager#getQueryTimeout()
     */
    public int getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * INTERNAL:
     * Returns the specific default redirector for this query type.  There are numerous default query redirectors.
     * See ClassDescriptor for their types.
     */
    protected QueryRedirector getDefaultRedirector(){
        return this.descriptor.getDefaultQueryRedirector();
    }
    
    /**
     * PUBLIC:
     * Return the query redirector.
     * A redirector can be used in a query to replace its execution with the execution of code.
     * This can be used for named or parameterized queries to allow dynamic configuration of the query base on the query arguments.
     * @see QueryRedirector
     */
    public QueryRedirector getRedirector() {
        if (doNotRedirect){
            return null;
        }
        if (redirector != null){
            return redirector;
        }
        if (descriptor != null){
            redirector = getDefaultRedirector();
            if (redirector == null) doNotRedirect = true; // PERF - short circuit no default redirector
            return redirector;
        }
        return null;
    }

    /**
     * PUBLIC:
     * Return the domain class associated with this query.
     * By default this is null, but should be overridden in subclasses.
     */
    public Class getReferenceClass() {
        return null;
    }

    /**
     * INTERNAL:
     * return the name of the reference class.  Added for Mapping Workbench removal
     * of classpath dependency.  Overridden by subclasses.
     */
    public String getReferenceClassName() {
        return null;
    }

    /**
     * PUBLIC:
     * Return the selection criteria of the query.
     * This should only be used with expression queries, null will be returned for others.
     */
    public Expression getSelectionCriteria() {
        return getQueryMechanism().getSelectionCriteria();
    }

    /**
     * INTERNAL:
     * Return the current session.
     */
    public AbstractSession getSession() {
        return session;
    }
    
    /**
     * INTERNAL:
     * Return the execution session.
     * This is the session used to build objects returned by the query.
     */
    public AbstractSession getExecutionSession() {
        if (this.executionSession == null) {
            if (getSession() != null) {
                this.executionSession = getSession().getExecutionSession(this);
            }            
        }
        return this.executionSession;
    }
    
    /**
     * INTERNAL:
     * Set the execution session.
     * This is the session used to build objects returned by the query.
     */
    protected void setExecutionSession(AbstractSession executionSession) {
        this.executionSession = executionSession;
    }

    /**
     * PUBLIC:
     * Return the name of the session that the query should be executed under.
     * This can be with the session broker to override the default session.
     */
    public String getSessionName() {
        return sessionName;
    }

    /**
     * PUBLIC:
     * Return the SQL statement of the query.
     * This can only be used with statement queries.
     */
    public SQLStatement getSQLStatement() {
        return ((StatementQueryMechanism)getQueryMechanism()).getSQLStatement();
    }

    /**
     * PUBLIC:
     * Return the JPQL string of the query.
     */
    public String getJPQLString() {
        return getEJBQLString();
    }
    
    /**
     * PUBLIC:
     * Return the EJBQL string of the query.
     */
    public String getEJBQLString() {
        if (!(getQueryMechanism().isJPQLCallQueryMechanism())) {
            return null;
        }
        JPQLCall call = ((JPQLCallQueryMechanism)getQueryMechanism()).getJPQLCall();
        return call.getEjbqlString();
    }

    /**
     * PUBLIC:
     * Return the current database hint string of the query.
     */
    public String getHintString() {
        return hintString;
    }

    /**
     * ADVANCED:
     * Return the SQL string of the query.
     * This can be used for SQL queries.
     * This can also be used for normal queries if they have been prepared, (i.e. query.prepareCall()).
     * @see #prepareCall(Session, Record)
     */
    public String getSQLString() {
        Call call = getDatasourceCall();
        if (call == null) {
            return null;
        }
        if (!(call instanceof SQLCall)) {
            return null;
        }

        return ((SQLCall)call).getSQLString();
    }

    /**
     * ADVANCED:
     * Return the SQL strings of the query.  Used for queries with multiple calls
     * This can be used for SQL queries.
     * This can also be used for normal queries if they have been prepared, (i.e. query.prepareCall()).
     * @see #prepareCall(Session, Record)
     */
    public List getSQLStrings() {
        List calls = getDatasourceCalls();
        if ((calls == null) || calls.isEmpty()) {
            return null;
        }
        Vector returnSQL = new Vector(calls.size());
        Iterator iterator = calls.iterator();
        while (iterator.hasNext()) {
            Call call = (Call)iterator.next();
            if (!(call instanceof SQLCall)) {
                return null;
            }
            returnSQL.addElement(((SQLCall)call).getSQLString());
        }
        return returnSQL;
    }

    /**
     * INTERNAL:
     * Returns the internal tri-state value of shouldBindParameters
     * used far cascading these settings
     */
    public Boolean getShouldBindAllParameters() {
        return this.shouldBindAllParameters;
    }

    /**
     * INTERNAL:
     */
    public DatabaseMapping getSourceMapping() {
       return sourceMapping; 
    }
    
    /**
     * ADVANCED:
     * This can be used to access a queries translated SQL if they have been prepared, (i.e. query.prepareCall()).
     * The Record argument is one of (Record, XMLRecord) that contains the query arguments.
     * @see #prepareCall(org.eclipse.persistence.sessions.Session, Record)
     */
    public String getTranslatedSQLString(org.eclipse.persistence.sessions.Session session, Record translationRow) {
        prepareCall(session, translationRow);        
        //CR#2859559 fix to use Session and Record interfaces not impl classes.
        CallQueryMechanism queryMechanism = (CallQueryMechanism)getQueryMechanism();
        if (queryMechanism.getCall() == null) {
            return null;
        }
        SQLCall call = (SQLCall)queryMechanism.getCall().clone();
        call.setUsesBinding(false);
        call.translate((AbstractRecord)translationRow, queryMechanism.getModifyRow(), (AbstractSession)session);
        return call.getSQLString();
    }

    /**
     * ADVANCED:
     * This can be used to access a queries translated SQL if they have been prepared, (i.e. query.prepareCall()).
     * This method can be used for queries with multiple calls.
     * @see #prepareCall(Session, Record)
     */
    public List getTranslatedSQLStrings(org.eclipse.persistence.sessions.Session session, Record translationRow) {
        prepareCall(session, translationRow);        
        CallQueryMechanism queryMechanism = (CallQueryMechanism)getQueryMechanism();
        if ((queryMechanism.getCalls() == null) || queryMechanism.getCalls().isEmpty()) {
            return null;
        }
        Vector calls = new Vector(queryMechanism.getCalls().size());
        Iterator iterator = queryMechanism.getCalls().iterator();
        while (iterator.hasNext()) {
            SQLCall call = (SQLCall)iterator.next();
            call = (SQLCall)call.clone();
            call.setUsesBinding(false);
            call.translate((AbstractRecord)translationRow, queryMechanism.getModifyRow(), (AbstractSession)session);
            calls.add(call.getSQLString());
        }
        return calls;
    }

    /**
     * INTERNAL:
     * Return the row for translation
     */
    public AbstractRecord getTranslationRow() {
        return translationRow;
    }

    /**
     * INTERNAL:
     * returns true if the accessor has already been set. The getAccessor() will attempt to
     * lazily initialize it.
     */
    public boolean hasAccessor() {
        return accessor != null;
    }

    /**
     * INTERNAL:
     * Return if any properties exist in the query.
     */
    public boolean hasProperties() {
        return (properties != null) && (!properties.isEmpty());
    }

    /**
     * INTERNAL:
     * Return if any arguments exist in the query.
     */
    public boolean hasArguments() {
        return (arguments != null) && (!arguments.isEmpty());
    }

    /**
     * PUBLIC:
     * Return if a name of the session that the query should be executed under has been specified.
     * This can be with the session broker to override the default session.
     */
    public boolean hasSessionName() {
        return sessionName != null;
    }

    /**
     * PUBLIC:
     * Session's shouldBindAllParameters() defines whether to bind or not
     * (default setting)
     */
    public void ignoreBindAllParameters() {
        this.shouldBindAllParameters = null;
    }

    /**
     * PUBLIC:
     * Session's shouldCacheAllStatements() defines whether to cache or not
     * (default setting)
     */
    public void ignoreCacheStatement() {
        this.shouldCacheStatement = null;
    }

    /**
     * PUBLIC:
     * Return true if this query uses an SQL or stored procedure, or SDK call.
     */
    public boolean isCallQuery() {
        return getQueryMechanism().isCallQueryMechanism();
    }

    /**
     * INTERNAL:
     * Returns true if this query has been created as the result of cascading a delete of an aggregate collection
     * in a UnitOfWork
     * CR 2811
     */
    public boolean isCascadeOfAggregateDelete() {
        return getCascadePolicy() == CascadeAggregateDelete;
    }

    /**
     * PUBLIC:
     * Return if this is a data modify query.
     */
    public boolean isDataModifyQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return if this is a data read query.
     */
    public boolean isDataReadQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return if this is a value read query.
     */
    public boolean isValueReadQuery() {
        return false;
    }
    
    /**
     * PUBLIC:
     * Return if this is a direct read query.
     */
    public boolean isDirectReadQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return if this is a delete all query.
     */
    public boolean isDeleteAllQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return if this is a delete object query.
     */
    public boolean isDeleteObjectQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return true if this query uses an expression query mechanism
     */
    public boolean isExpressionQuery() {
        return getQueryMechanism().isExpressionQueryMechanism();
    }

    /**
     * PUBLIC:
     * Return true if this is a modify all query.
     */
    public boolean isModifyAllQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return true if this is a modify query.
     */
    public boolean isModifyQuery() {
        return false;
    }

    /**
       * PUBLIC:
       * Return true if this is an update all query.
       */
    public boolean isUpdateAllQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return true if this is an update object query.
     */
    public boolean isUpdateObjectQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * If executed against a RepeatableWriteUnitOfWork if this attribute is true
     * EclipseLink will write changes to the database before executing the query.
     */
    public Boolean getFlushOnExecute(){
        return this.flushOnExecute;
    }
    
    /**
     * PUBLIC:
     * Return true if this is an insert object query.
     */
    public boolean isInsertObjectQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return true if this is an object level modify query.
     */
    public boolean isObjectLevelModifyQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return true if this is an object level read query.
     */
    public boolean isObjectLevelReadQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return if this is an object building query.
     */
    public boolean isObjectBuildingQuery() {
        return false;
    }

    /**
     * INTERNAL:
     * Queries are prepared when they are executed and then do not need to be
     * prepared on subsequent executions. This method returns true if this
     * query has been prepared.  Updating the settings on a query will 'un-prepare'
     * the query.
     */
    public boolean isPrepared() {
        return isPrepared;
    }

    /**
     * PUBLIC:
     * Return true if this is a read all query.
     */
    public boolean isReadAllQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return true if this is a read object query.
     */
    public boolean isReadObjectQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return true if this is a read query.
     */
    public boolean isReadQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return true if this is a report query.
     */
    public boolean isReportQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Return true if this query uses an SQL query mechanism .
     */
    public boolean isSQLCallQuery() {
        // BUG#2669342 CallQueryMechanism and isCallQueryMechanism have different meaning as SDK return true but isn't.
        Call call = getDatasourceCall();
        return (call != null) && (call instanceof SQLCall);
    }

    /**
     * PUBLIC:
     * Return true if this query uses an JPQL query mechanism .
     */
    public boolean isJPQLCallQuery() {
        Call call = getDatasourceCall();
        return (call != null) && (call instanceof JPQLCall);
    }

    /**
     * INTERNAL:
     * Return true if the query is a custom user defined query.
     */
    public boolean isUserDefined() {
        return isUserDefined;
    }
    
    /**
     * INTERNAL:
     * Return true if the query uses default properties.
     * This is used to determine if this query is cacheable.
     * i.e. does not use any properties that may conflict with another query
     * with the same EJBQL or selection criteria.
     */
    public boolean isDefaultPropertiesQuery() {
        return (!isUserDefined())
            && (shouldPrepare())
            && (getQueryTimeout() == DescriptorQueryManager.DefaultTimeout)
            && (getHintString() == null)
            && (shouldIgnoreBindAllParameters())
            && (shouldIgnoreCacheStatement())
            && (shouldUseWrapperPolicy());
    }

    /**
     * PUBLIC:
     * Return true if this is a write object query.
     */
    public boolean isWriteObjectQuery() {
        return false;
    }

    /**
     * PUBLIC:
     * Set for the identity map (cache) to be maintained.
     * This is the default.
     */
    public void maintainCache() {
        setShouldMaintainCache(true);
    }

    /**
     * INTERNAL:
     * This is different from 'prepareForExecution' in that this is called on the original query,
     * and the other is called on the copy of the query.
     * This query is copied for concurrency so this prepare can only setup things that
     * will apply to any future execution of this query.
     *
     * Resolve the queryTimeout using the DescriptorQueryManager if required.
     */
    protected void prepare() throws QueryException {
        // If the queryTimeout is DefaultTimeout, resolve using the DescriptorQueryManager.
        if (getQueryTimeout() == DescriptorQueryManager.DefaultTimeout) {
            if (getDescriptor() == null) {
                setQueryTimeout(DescriptorQueryManager.NoTimeout);
            } else {
                setQueryTimeout(getDescriptor().getQueryManager().getQueryTimeout());
            }
        }
 
        if(getQueryTimeout() == DescriptorQueryManager.DefaultTimeout ||  getQueryTimeout() == DescriptorQueryManager.NoTimeout  ){
            // Timeout not overridden at descriptor level. Use session timeout
            setQueryTimeout(session.getQueryTimeoutDefault());
        }
         
        this.argumentFields = buildArgumentFields();

        getQueryMechanism().prepare();
    }
        
    /**
     * INTERNAL:
     * Copy all setting from the query.
     * This is used to morph queries from one type to the other.
     * By default this calls prepareFromQuery, but additional properties may be required
     * to be copied as prepareFromQuery o nly copies properties that affect the SQL.
     */
    public void copyFromQuery(DatabaseQuery query) {
        prepareFromQuery(query);
        this.cascadePolicy = query.cascadePolicy;
        this.flushOnExecute = query.flushOnExecute;
        this.arguments = query.arguments;
        this.argumentTypes = query.argumentTypes;
        this.argumentTypeNames = query.argumentTypeNames;
        this.argumentValues = query.argumentValues;
        this.queryTimeout = query.queryTimeout;
        this.redirector = query.redirector;
        this.sessionName = query.sessionName;
        this.shouldBindAllParameters = query.shouldBindAllParameters;
        this.shouldCacheStatement = query.shouldCacheStatement;
        this.shouldMaintainCache = query.shouldMaintainCache;
        this.shouldPrepare = query.shouldPrepare;
        this.shouldUseWrapperPolicy = query.shouldUseWrapperPolicy;
        this.properties = query.properties;
    }
    
    /**
     * INTERNAL:
     * Prepare the query from the prepared query.
     * This allows a dynamic query to prepare itself directly from a prepared query instance.
     * This is used in the JPQL parse cache to allow preparsed queries to be used to prepare
     * dynamic queries.
     * This only copies over properties that are configured through JPQL.
     */
    public void prepareFromQuery(DatabaseQuery query) {
        setQueryMechanism((DatabaseQueryMechanism)query.getQueryMechanism().clone());
        getQueryMechanism().setQuery(this);
        this.descriptor = query.descriptor;
        this.hintString = query.hintString;
        this.isCustomQueryUsed = query.isCustomQueryUsed;
        this.argumentFields = query.argumentFields;
        //this.properties = query.properties; - Cannot set properties and CMP stores finders there.
    }

    /**
     * ADVANCED:
     * Pre-generate the call/SQL for the query.
     * This method takes a Session and an implementor of Record (DatebaseRow or XMLRecord).
     * This can be used to access the SQL for a query without executing it.
     * To access the call use, query.getCall(), or query.getSQLString() for the SQL.
     * Note the SQL will have argument markers in it (i.e. "?").
     * To translate these use query.getTranslatedSQLString(session, translationRow).
     * @see #getCall()
     * @see #getSQLString()
     * @see #getTranslatedSQLString(org.eclipse.persistence.sessions.Session, Record)
     */
    public void prepareCall(org.eclipse.persistence.sessions.Session session, Record translationRow) throws QueryException {
        //CR#2859559 fix to use Session and Record interfaces not impl classes.
        checkPrepare((AbstractSession)session, (AbstractRecord)translationRow, true);
    }

    /**
     * INTERNAL:
     * Set the properties needed to be cascaded into the custom query.
     */
    protected void prepareCustomQuery(DatabaseQuery customQuery) {
        // Nothing by default.
    }

    /**
     * INTERNAL:
     * Prepare the receiver for execution in a session. In particular,
     * set the descriptor of the receiver to the ClassDescriptor for the
     * appropriate class for the receiver's object.
     */
    public void prepareForExecution() throws QueryException {}

    protected void prepareForRemoteExecution() {}

    /**
     * INTERNAL:
     * Use a EclipseLink redirector to redirect this query to a method.
     * Added for bug 3241138
     *
     */
    public Object redirectQuery(QueryRedirector redirector, DatabaseQuery queryToRedirect, AbstractSession session, AbstractRecord translationRow) {
        if (redirector == null ) {
            return null;
        }
        DatabaseQuery queryToExecute = (DatabaseQuery)queryToRedirect.clone();
        queryToExecute.setRedirector(null);

        // since we deal with a clone when using a redirector, the descriptor will 
        // get set on the clone, but not on the original object. So before returning 
        // the results, set the descriptor from the clone onto this object (original
        // query) - GJPP, BUG# 2692956
        Object toReturn = redirector.invokeQuery(queryToExecute, translationRow, session);
        setDescriptor(queryToExecute.getDescriptor());
        return toReturn;
    }

    protected Object remoteExecute() {
        Transporter transporter = ((RemoteSession)getSession()).getRemoteConnection().remoteExecute((DatabaseQuery)this.clone());
        return extractRemoteResult(transporter);
    }

    /**
     * INTERNAL:
     */
    public Object remoteExecute(AbstractSession session) {
        setSession(session);
        prepareForRemoteExecution();
        return remoteExecute();
    }

    /**
     * INTERNAL:
     * Property support used by mappings.
     */
    public void removeProperty(Object property) {
        getProperties().remove(property);
    }

    /**
     * INTERNAL:
     * replace the value holders in the specified result object(s)
     */
    public Map replaceValueHoldersIn(Object object, RemoteSessionController controller) {
        // by default, do nothing
        return null;
    }
    
    /**
     * ADVANCED:
     * JPA flag used to control the behavior of the shared cache. This flag
     * specifies the behavior when data is retrieved by the find methods and 
     * by the execution of queries. Calling this method will set a retrieve
     * bypass to true. 
     */
    public void retrieveBypassCache() {
        setShouldRetrieveBypassCache(true);
    }

    /**
     * INTERNAL: Build the list of arguments fields from the argument names and types.
     */
    public List<DatabaseField> buildArgumentFields() {
        List arguments = getArguments();
        List argumentTypes = getArgumentTypes();
        List argumentFields = new ArrayList(arguments.size());
        int size = arguments.size();
        for (int index = 0; index < size; index++) {
            DatabaseField argumentField = new DatabaseField((String)arguments.get(index));
            argumentField.setType((Class)argumentTypes.get(index));
            argumentFields.add(argumentField);
        }
        return argumentFields;
    }

    /**
     * INTERNAL:
     * Translate argumentValues into a database row.
     */
    public AbstractRecord rowFromArguments(List argumentValues) throws QueryException {
        List<DatabaseField> argumentFields = this.argumentFields;
        // PERF: argumentFields are set in prepare, but need to be built if query is not prepared.
        if (!isPrepared() || (argumentFields == null)) {
            argumentFields = buildArgumentFields();
        }
        
        if (argumentFields.size() != argumentValues.size()) {
            throw QueryException.argumentSizeMismatchInQueryAndQueryDefinition(this);
        }
        int argumentsSize = argumentFields.size();
        AbstractRecord row = new DatabaseRecord(argumentsSize);
        for (int index = 0; index < argumentsSize; index++) {
            row.put(argumentFields.get(index), argumentValues.get(index));
        }

        return row;
    }

    /**
     * INTERNAL:
     * Set the accessor, the query must always use the same accessor for database access.
     * This is required to support connection pooling.
     */
    public void setAccessor(Accessor accessor) {
        this.accessor = accessor;
    }

    /**
     * PUBLIC:
     * Used to define a store procedure or SQL query.
     */
    public void setDatasourceCall(Call call) {
        if (call == null) {
            return;
        }
        setQueryMechanism(call.buildNewQueryMechanism(this));
    }

    /**
     * PUBLIC:
     * Used to define a store procedure or SQL query.
     */
    public void setCall(Call call) {
        setDatasourceCall(call);
    }

    /**
     * INTERNAL:
     * Set the cascade policy.
     */
    public void setCascadePolicy(int policyConstant) {
        cascadePolicy = policyConstant;
    }

    /**
     * INTERNAL:
     * Set the descriptor for the query.
     */
    public void setDescriptor(ClassDescriptor descriptor) {
        // If the descriptor changed must unprepare as the SQL may change.
        if (this.descriptor != descriptor) {
            setIsPrepared(false);
        }
        this.descriptor = descriptor;
    }

    /**
     * PUBLIC:
     * Set the JPQL string of the query.
     * If arguments are required in the string they will be preceded by ":" then the argument name.
     * The JPQL arguments must also be added as argument to the query.
     */
    public void setJPQLString(String jpqlString) {
        setEJBQLString(jpqlString);
    }

    /**
     * PUBLIC:
     * Set the EJBQL string of the query.
     * If arguments are required in the string they will be preceded by "?" then the argument number.
     */
    public void setEJBQLString(String ejbqlString) {
        //Added the check for when we are building the query from the deployment XML
        if ((ejbqlString != null) && (!ejbqlString.equals(""))) {
            JPQLCallQueryMechanism mechanism = new JPQLCallQueryMechanism(this, new JPQLCall(ejbqlString));
            setQueryMechanism(mechanism);
        }
    }

    /**
     * PUBLIC:
     * If executed against a RepeatableWriteUnitOfWork if this attribute is true
     * EclipseLink will write changes to the database before executing the query.
     */
    public void setFlushOnExecute(Boolean flushMode){
        this.flushOnExecute = flushMode;
    }
    
    /**
     * PUBLIC:
     * Used to set the database hint string on the query.
     * This should be the full hint string including the comment delimiters.
     * This method will cause a query to re-prepare if it has already been executed.
     * @param newHintString the hint string to be added into the SQL call.
     */
    public void setHintString(String newHintString) {
        hintString = newHintString;
        setIsPrepared(false);
    }

    /**
     * INTERNAL:
     * If changes are made to the query that affect the derived SQL or Call
     * parameters the query needs to be prepared again.
     * <p>
     * Automatically called internally.
     */
    public void setIsPrepared(boolean isPrepared) {
        this.isPrepared = isPrepared;
        this.isCustomQueryUsed = null;
    }
    
    /**
     * INTERNAL:
     * PERF: Return if the query is an execution clone.
     * This allow the clone during execution to be avoided in the cases when the query has
     * already been clone elsewhere.
     */
    public boolean isExecutionClone() {
        return isExecutionClone;
    }
    
    /**
     * INTERNAL:
     * PERF: Set if the query is an execution clone.
     * This allow the clone during execution to be avoided in the cases when the query has
     * already been clone elsewhere.
     */
    public void setIsExecutionClone(boolean isExecutionClone) {
        this.isExecutionClone = isExecutionClone;
    }
    
    /**
     * INTERNAL:
     * PERF: Return if this query will use the descriptor custom query
     * instead of executing itself.
     */
    public Boolean isCustomQueryUsed() {
        return this.isCustomQueryUsed;
    }
    
    /**
     * INTERNAL:
     * If the query mechanism is a call query mechanism and there are no
     * arguments on the query then it must be a foreign reference custom 
     * selection query.
     */
    protected boolean isCustomSelectionQuery() {
        return getQueryMechanism().isCallQueryMechanism() && getArguments().isEmpty();
    }
    
    /**
     * INTERNAL:
     * PERF: Set if this query will use the descriptor custom query
     * instead of executing itself.
     */
    protected void setIsCustomQueryUsed(boolean isCustomQueryUsed) {
        if (isCustomQueryUsed) {
            this.isCustomQueryUsed = Boolean.TRUE;
        } else {
            this.isCustomQueryUsed = Boolean.FALSE;            
        }
    }

    /**
     * INTERNAL:
     * Set if the query is a custom user defined query.
     */
    public void setIsUserDefined(boolean isUserDefined) {
        this.isUserDefined = isUserDefined;
    }

    /**
     * PUBLIC:
     * Set the query's name.
     * Queries can be named and added to a descriptor or the session and then referenced by name.
     */
    public void setName(String queryName) {
        name = queryName;
    }

    /**
     * INTERNAL:
     * Property support used by mappings.
     */
    public void setProperties(Map<Object, Object> properties) {
        this.properties = properties;
    }

    /**
     * INTERNAL:
     * Property support used by mappings to store temporary stuff.
     */
    public synchronized void setProperty(Object property, Object value) {
        getProperties().put(property, value);
    }

    /**
     * Set the query mechanism for the query.
     */
    protected void setQueryMechanism(DatabaseQueryMechanism queryMechanism) {
        this.queryMechanism = queryMechanism;
        // Must un-prepare is prepare as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * PUBLIC:
     * Set the number of seconds the driver will wait for a Statement to execute to the given number of seconds.
     * If the limit is exceeded, a DatabaseException is thrown.
     *
     * queryTimeout - the new query timeout limit in seconds; DefaultTimeout is the default, which
     * redirects to DescriptorQueryManager's queryTimeout.
     *
     * @see DescriptorQueryManager#setQueryTimeout(int)
     *
     */
    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
        this.shouldCloneCall=true;
    }

    /**
     * PUBLIC:
     * Set the query redirector.
     * A redirector can be used in a query to replace its execution with the execution of code.
     * This can be used for named or parameterized queries to allow dynamic configuration of the query base on the query arguments.
     * @see QueryRedirector
     */
    public void setRedirector(QueryRedirector redirector) {
        this.redirector = redirector;
        this.doNotRedirect = false;
    }

    /**
     * PUBLIC:
     * To any user of this object. Set the selection criteria of the query.
     * This method be used when dealing with expressions.
     */
    public void setSelectionCriteria(Expression expression) {
        // Do not overwrite the call if the expression is null.
        if ((expression == null) && (!getQueryMechanism().isExpressionQueryMechanism())) {
            return;
        }
        if (!getQueryMechanism().isExpressionQueryMechanism()) {
            setQueryMechanism(new ExpressionQueryMechanism(this, expression));
        } else {
            ((ExpressionQueryMechanism)getQueryMechanism()).setSelectionCriteria(expression);
        }

        // Must un-prepare is prepare as the SQL may change.
        setIsPrepared(false);
    }

    /**
     * INTERNAL:
     * Set the session for the query
     */
    public void setSession(AbstractSession session) {
        this.session = session;
        this.executionSession = null;
    }

    /**
     * PUBLIC:
     * Set the name of the session that the query should be executed under.
     * This can be with the session broker to override the default session.
     */
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    /**
     * PUBLIC:
     * Bind all arguments to any SQL statement.
     */
    public void setShouldBindAllParameters(boolean shouldBindAllParameters) {
        this.shouldBindAllParameters = Boolean.valueOf(shouldBindAllParameters);
        setIsPrepared(false);
    }

    /**
     * INTERNAL:
     * Sets the internal tri-state value of shouldBindAllParams
     * Used to cascade this value to other queries
     */
    public void setShouldBindAllParameters(Boolean bindAllParams) {
        this.shouldBindAllParameters = bindAllParams;
    }

    /**
     * PUBLIC:
     * Cache the prepared statements, this requires full parameter binding as well.
     */
    public void setShouldCacheStatement(boolean shouldCacheStatement) {
        this.shouldCacheStatement = Boolean.valueOf(shouldCacheStatement);
        setIsPrepared(false);
    }
    
    /**
     * PUBLIC:
     * Set if the identity map (cache) should be used or not.
     * If not the cache check will be skipped and the result will not be put into the identity map.
     * By default the identity map is always maintained.
     */
    public void setShouldMaintainCache(boolean shouldMaintainCache) {
        this.shouldMaintainCache = shouldMaintainCache;
    }

    /**
     * PUBLIC:
     * Set if the query should be prepared.
     * EclipseLink automatically prepares queries to generate their SQL only once,
     * one each execution of the query the SQL does not need to be generated again only the arguments need to be translated.
     * This option is provide to disable this optimization as in can cause problems with certain types of queries that require dynamic SQL based on their arguments.
     * <p>These queries include:
     * <ul>
     * <li> Expressions that make use of 'equal' where the argument value has the potential to be null, this can cause problems on databases that require IS NULL, instead of = NULL.
     * <li> Expressions that make use of 'in' and that use parameter binding, this will cause problems as the in values must be bound individually.
     * </ul>
     */
    public void setShouldPrepare(boolean shouldPrepare) {
        this.shouldPrepare = shouldPrepare;
        setIsPrepared(false);
    }

    /**
     * ADVANCED:
     * JPA flag used to control the behavior of the shared cache. This flag
     * specifies the behavior when data is retrieved by the find methods and 
     * by the execution of queries.
     */
    public void setShouldRetrieveBypassCache(boolean shouldRetrieveBypassCache) {
        this.shouldRetrieveBypassCache = shouldRetrieveBypassCache;
    }
    
    /**
     * ADVANCED:
     * JPA flag used to control the behavior of the shared cache. This flag
     * specifies the behavior when data is read from the database and when
     * data is committed into the database.
     */
    public void setShouldStoreBypassCache(boolean shouldStoreBypassCache) {
        this.shouldStoreBypassCache = shouldStoreBypassCache;
    }
    
    /**
     * ADVANCED:
     * The wrapper policy can be enable on a query.
     */
    public void setShouldUseWrapperPolicy(boolean shouldUseWrapperPolicy) {
        this.shouldUseWrapperPolicy = shouldUseWrapperPolicy;
    }

    /**
     * INTERNAL:
     */
    public void setSourceMapping(DatabaseMapping sourceMapping) {
       this.sourceMapping = sourceMapping; 
    }
    
    /**
     * PUBLIC:
     * To any user of this object. Set the SQL statement of the query.
     * This method should only be used when dealing with statement objects.
     */
    public void setSQLStatement(SQLStatement sqlStatement) {
        setQueryMechanism(new StatementQueryMechanism(this, sqlStatement));
    }

    /**
     * PUBLIC:
     * To any user of this object. Set the SQL string of the query.
     * This method should only be used when dealing with user defined SQL strings.
     * If arguments are required in the string they will be preceded by "#" then the argument name.
     * Warning: Allowing an unverified SQL string to be passed into this 
     * method makes your application vulnerable to SQL injection attacks. 
     */
    public void setSQLString(String sqlString) {
        //Added the check for when we are building the query from the deployment XML
        if ((sqlString != null) && (!sqlString.equals(""))) {
            setCall(new SQLCall(sqlString));
        }
    }

    /**
     * INTERNAL:
     * Set the row for translation
     */
    public void setTranslationRow(AbstractRecord translationRow) {
        this.translationRow = translationRow;
    }

    /**
     * PUBLIC:
     * Bind all arguments to any SQL statement.
     */
    public boolean shouldBindAllParameters() {
        return Boolean.TRUE.equals(shouldBindAllParameters);
    }

    /**
     * PUBLIC:
     * Cache the prepared statements, this requires full parameter binding as well.
     */
    public boolean shouldCacheStatement() {
        return Boolean.TRUE.equals(shouldCacheStatement);
    }

    /**
     * PUBLIC:
     * Flag used to determine if all parts should be cascaded
     */
    public boolean shouldCascadeAllParts() {
        return this.cascadePolicy == CascadeAllParts;
    }

    /**
     * PUBLIC:
     * Mappings should be checked to determined if the current operation should be
     * cascaded to the objects referenced.
     */
    public boolean shouldCascadeByMapping() {
        return this.cascadePolicy == CascadeByMapping;
    }

    /**
     * INTERNAL:
     * Flag used for unit of works cascade policy.
     */
    public boolean shouldCascadeOnlyDependentParts() {
        return this.cascadePolicy == CascadeDependentParts;
    }

    /**
     * PUBLIC:
     * Flag used to determine if any parts should be cascaded
     */
    public boolean shouldCascadeParts() {
        return this.cascadePolicy != NoCascading;
    }

    /**
     * PUBLIC:
     * Flag used to determine if any private parts should be cascaded
     */
    public boolean shouldCascadePrivateParts() {
        return (this.cascadePolicy == CascadePrivateParts) || (this.cascadePolicy == CascadeAllParts);
    }
    
    /**
     * INTERNAL:
     * Flag used to determine if the call needs to be cloned.
     */
    public boolean shouldCloneCall(){
        return shouldCloneCall;
    }

    /**
     * PUBLIC:
     * Local shouldBindAllParameters() should be ignored,
     * Session's shouldBindAllParameters() should be used.
     */
    public boolean shouldIgnoreBindAllParameters() {
        return shouldBindAllParameters == null;
    }

    /**
     * PUBLIC:
     * Local shouldCacheStatement() should be ignored,
     * Session's shouldCacheAllStatements() should be used.
     */
    public boolean shouldIgnoreCacheStatement() {
        return shouldCacheStatement == null;
    }

    /**
     * PUBLIC:
     * Return if the identity map (cache) should be used or not.
     * If not the cache check will be skipped and the result will not be put into the identity map.
     * By default the identity map is always maintained.
     */
    public boolean shouldMaintainCache() {
        return shouldMaintainCache;
    }

    /**
     * PUBLIC:
     * Return if the query should be prepared.
     * EclipseLink automatically prepares queries to generate their SQL only once,
     * one each execution of the query the SQL does not need to be generated again only the arguments need to be translated.
     * This option is provide to disable this optimization as in can cause problems with certain types of queries that require dynamic SQL basd on their arguments.
     * <p>These queries include:
     * <ul>
     * <li> Expressions that make use of 'equal' where the argument value has the potential to be null, this can cause problems on databases that require IS NULL, instead of = NULL.
     * <li> Expressions that make use of 'in' and that use parameter binding, this will cause problems as the in values must be bound individually.
     * </ul>
     */
    public boolean shouldPrepare() {
        return shouldPrepare;
    }

    /**
     * ADVANCED:
     * JPA flag used to control the behavior of the shared cache. This flag
     * specifies the behavior when data is retrieved by the find methods and 
     * by the execution of queries.
     */
    public boolean shouldRetrieveBypassCache() {
        return this.shouldRetrieveBypassCache;
    }
    
    /**
     * ADVANCED:
     * JPA flag used to control the behavior of the shared cache. This flag
     * specifies the behavior when data is read from the database and when
     * data is committed into the database.
     */
    public boolean shouldStoreBypassCache() {
        return this.shouldStoreBypassCache;
    }
    
    /**
     * ADVANCED:
     * The wrapper policy can be enabled on a query.
     */
    public boolean shouldUseWrapperPolicy() {
        return shouldUseWrapperPolicy;
    }

    /**
     * ADVANCED:
     * JPA flag used to control the behavior of the shared cache. This flag
     * specifies the behavior when data is read from the database and when
     * data is committed into the database. Calling this method will set a 
     * store bypass to true.
     * 
     * Note: For a cache store mode of REFRESH, see refreshIdentityMapResult() 
     * from ObjectLevelReadQuery.
     */
    public void storeBypassCache() {
        setShouldStoreBypassCache(true);
    }
    
    public String toString() {
    	String referenceClassString = "";
    	String nameString = "";
    	String queryString = "";
    	if (getReferenceClass() != null) {
    		referenceClassString = "referenceClass=" + getReferenceClass().getSimpleName() + " ";
    	}
    	if ((getName() != null) && (!getName().equals(""))) {
    		nameString = "name=\"" + getName() + "\" ";
    	}
    	if (isSQLCallQuery()) {
    		queryString = "sql=\"" + getSQLString() + "\"";
    	} else if (isJPQLCallQuery()) {
    		queryString = "jpql=\"" + getJPQLString() + "\"";
    	}
    	return getClass().getSimpleName() + "(" + nameString + referenceClassString + queryString + ")";
    }

    /**
     * INTERNAL:
     * TopLink_sessionName_domainClass.  Cached in properties
     */
     public String getDomainClassNounName(String sessionName) {    
        if (getProperty("DMSDomainClassNounName") == null) {
            StringBuffer buffer = new StringBuffer("EclipseLink");
            if (sessionName != null) {
                buffer.append(sessionName);
            }
            if (getReferenceClassName() != null) {
                buffer.append("_");
                buffer.append(getReferenceClassName());
            }        
            setProperty("DMSDomainClassNounName", buffer.toString());
        }
        return (String)getProperty("DMSDomainClassNounName");
     }
        
    /**
     * INTERNAL:
     * TopLink_sessionName_domainClass_queryClass_queryName (if exist).  Cached in properties
     */
    public String getQueryNounName(String sessionName) {    
        if (getProperty("DMSQueryNounName") == null) {
            StringBuffer buffer = new StringBuffer(getDomainClassNounName(sessionName));            
            buffer.append("_");
            buffer.append(getClass().getSimpleName());
            if (getName() != null) {
                buffer.append("_");
                buffer.append(getName());
            }
            setProperty("DMSQueryNounName", buffer.toString());
        }
        return (String)getProperty("DMSQueryNounName");
    }

    /**
     * INTERNAL:
     * TopLink_sessionName_domainClass_queryClass_queryName (if exist)_operationName (if exist).  Cached in properties
     */
    public String getSensorName(String operationName, String sessionName) {    
        if (operationName == null) {
            return getQueryNounName(sessionName);
        }
        
        Hashtable sensorNames = (Hashtable)getProperty("DMSSensorNames");            
        if (sensorNames == null) {
            sensorNames = new Hashtable();
            setProperty("DMSSensorNames", sensorNames);
        }
        Object sensorName = sensorNames.get(operationName);
        if (sensorName == null) {
            StringBuffer buffer = new StringBuffer(getQueryNounName(sessionName));            
            buffer.append("_");
            buffer.append(operationName);
            sensorName = buffer.toString();
            sensorNames.put(operationName, sensorName);
        }
        return (String)sensorName;
    }

    /**
     * ADVANCED:
     * Set if the descriptor requires usage of a native (unwrapped) JDBC connection.
     * This may be required for some Oracle JDBC support when a wrapping DataSource is used.
     */
    public void setIsNativeConnectionRequired(boolean isNativeConnectionRequired) {
        this.isNativeConnectionRequired = isNativeConnectionRequired;
    }

    /**
     * ADVANCED:
     * Return if the descriptor requires usage of a native (unwrapped) JDBC connection.
     * This may be required for some Oracle JDBC support when a wrapping DataSource is used.
     */
    public boolean isNativeConnectionRequired() {
        return isNativeConnectionRequired;
    }

    /**
     * This method is used in combination with redirected queries.  If a redirector is set on the
     * query or there is a default redirector on the Descriptor setting this value to true
     * will force EclipseLink to ignore the redirector during execution.  This setting will be
     * used most often when reexecuting the query within a redirector.
     */
    public boolean getDoNotRedirect() {
        return doNotRedirect;
    }

    /**
     * This method is used in combination with redirected queries.  If a redirector is set on the
     * query or there is a default redirector on the Descriptor setting this value to true
     * will force EclipseLink to ignore the redirector during execution.  This setting will be
     * used most often when reexecuting the query within a redirector.
     */
    public void setDoNotRedirect(boolean doNotRedirect) {
        this.doNotRedirect = doNotRedirect;
    }
}
