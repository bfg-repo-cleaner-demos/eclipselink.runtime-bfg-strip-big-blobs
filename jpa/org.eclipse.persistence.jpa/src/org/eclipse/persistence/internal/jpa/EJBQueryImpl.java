/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 *     Zoltan NAGY & tware - updated support for MaxRows
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.LockTimeoutException;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.PersistenceException;
import javax.persistence.PessimisticLockException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.internal.helper.BasicTypeHelperImpl;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.ConversionManager;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.jpa.parsing.JPQLParseTree;
import org.eclipse.persistence.internal.jpa.parsing.jpql.JPQLParser;
import org.eclipse.persistence.internal.jpa.querydef.ParameterExpressionImpl;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.internal.queries.JPQLCallQueryMechanism;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.UnitOfWorkImpl;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.Call;
import org.eclipse.persistence.queries.Cursor;
import org.eclipse.persistence.queries.DataModifyQuery;
import org.eclipse.persistence.queries.DataReadQuery;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.ModifyQuery;
import org.eclipse.persistence.queries.ObjectLevelReadQuery;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;
import org.eclipse.persistence.queries.ReadQuery;
import org.eclipse.persistence.queries.ResultSetMappingQuery;
import org.eclipse.persistence.queries.StoredProcedureCall;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.sessions.Session;

/**
 * Concrete JPA query class. The JPA query wraps a DatabaseQuery which is
 * executed.
 */
public class EJBQueryImpl<X> implements JpaQuery<X> {

    /**
     * Wrapped native query. The query may be {@link #isShared}
     */
    protected DatabaseQuery databaseQuery = null;

    protected EntityManagerImpl entityManager = null;
    protected String queryName = null;
    protected Map<String, Object> parameterValues = null;
    protected Map<Parameter<?>, Parameter<?>> parameters;
    protected int firstResultIndex = -1; // -1 indicates undefined
    protected int maxResults = -1; // -1 indicates undefined

    protected LockModeType lockMode = null;

    /**
     * Stores if the wrapped query is shared, and requires cloning before being
     * changed.
     */
    protected boolean isShared;

    /**
     * Base constructor for EJBQueryImpl. Initializes basic variables.
     */
    protected EJBQueryImpl(EntityManagerImpl entityManager) {
        this.parameterValues = new HashMap<String, Object>();
        this.entityManager = entityManager;
        this.isShared = true;
    }

    /**
     * Create an EJBQueryImpl with a DatabaseQuery.
     */
    public EJBQueryImpl(DatabaseQuery query, EntityManagerImpl entityManager) {
        this(entityManager);
        this.databaseQuery = query;
    }

    /**
     * Build an EJBQueryImpl based on the given jpql string.
     */
    public EJBQueryImpl(String jpql, EntityManagerImpl entityManager) {
        this(jpql, entityManager, false);
    }

    /**
     * Create an EJBQueryImpl with either a query name or an jpql string.
     * 
     * @param isNamedQuery
     *            determines whether to treat the queryDescription as jpql or a
     *            query name.
     */
    public EJBQueryImpl(String queryDescription, EntityManagerImpl entityManager, boolean isNamedQuery) {
        this(entityManager);
        if (isNamedQuery) {
            this.queryName = queryDescription;
        } else {
            if (databaseQuery == null) {
                databaseQuery = buildEJBQLDatabaseQuery(queryDescription, this.entityManager.getServerSession());
            }
        }
    }

    /**
     * Internal method to change the wrapped query to a DataModifyQuery if
     * necessary. When created, the query is created as a DataReadQuery as it is
     * unknown if it is a SELECT or UPDATE. Note that this prevents the original
     * named query from every being prepared.
     */
    protected void setAsSQLModifyQuery() {
        if (getDatabaseQueryInternal().isDataReadQuery()) {
            DataModifyQuery query = new DataModifyQuery();
            query.setIsUserDefined(this.databaseQuery.isUserDefined());
            query.copyFromQuery(this.databaseQuery);
            // Need to clone call, in case was executed as read.
            query.setDatasourceCall((Call) this.databaseQuery.getDatasourceCall().clone());
            this.databaseQuery = query;
        }
    }

    /**
     * Internal method to change the wrapped query to a DataReadQuery if
     * necessary. This should never occur, but could possibly if the same query
     * was executed as executeUpdate() then as getResultList(). Note that the
     * initial conversion to modify would loose any read settings that had been
     * set.
     */
    protected void setAsSQLReadQuery() {
        if (getDatabaseQueryInternal().isDataModifyQuery()) {
            DataReadQuery query = new DataReadQuery();
            query.setResultType(DataReadQuery.AUTO);
            query.setIsUserDefined(databaseQuery.isUserDefined());
            query.copyFromQuery(this.databaseQuery);
            this.databaseQuery = query;
        }
    }

    /**
     * Build a DatabaseQuery from an jpql string.
     * 
     * @param jpql
     * @param session
     *            the session to get the descriptors for this query for.
     * @return a DatabaseQuery representing the given jpql.
     */
    public static DatabaseQuery buildEJBQLDatabaseQuery(String jpql, Session session) {
        return buildEJBQLDatabaseQuery(null, jpql, session, null, null, null);
    }

    /**
     * Build a DatabaseQuery from an JPQL string.
     * 
     * @param jpql
     *            the JPQL string.
     * @param flushOnExecute
     *            flush the unit of work before executing the query.
     * @param session
     *            the session to get the descriptors for this query for.
     * @param hints
     *            a list of hints to be applied to the query.
     * @return a DatabaseQuery representing the given jpql.
     */
    public static DatabaseQuery buildEJBQLDatabaseQuery(String queryName, String jpql, Session session, Enum lockMode, Map<String, Object> hints, ClassLoader classLoader) {
        // PERF: Check if the JPQL has already been parsed.
        // Only allow queries with default properties to be parse cached.
        boolean isCacheable = (queryName == null) && (hints == null);
        DatabaseQuery databaseQuery = null;
        if (isCacheable) {
            databaseQuery = (DatabaseQuery) session.getProject().getJPQLParseCache().get(jpql);
        }
        if ((databaseQuery == null) || (!databaseQuery.isPrepared())) {
            JPQLParseTree parseTree = JPQLParser.buildParseTree(queryName, jpql);
            parseTree.setClassLoader(classLoader);
            databaseQuery = parseTree.createDatabaseQuery();
            databaseQuery.setJPQLString(jpql);
            parseTree.populateQuery(databaseQuery, (AbstractSession) session);
            // If the query uses fetch joins, need to use JPA default of not
            // filtering duplicates.
            if (databaseQuery.isReadAllQuery()) {
                ReadAllQuery readAllQuery = (ReadAllQuery) databaseQuery;
                if (readAllQuery.hasJoining() && (readAllQuery.getDistinctState() == ReadAllQuery.DONT_USE_DISTINCT)) {
                    readAllQuery.setShouldFilterDuplicates(false);
                }
            }
            // Bug#4646580 Add arguments to query.
            parseTree.addParametersToQuery(databaseQuery);

            ((JPQLCallQueryMechanism) databaseQuery.getQueryMechanism()).getJPQLCall().setIsParsed(true);

            // GF#1324 eclipselink.refresh query hint does not cascade
            // cascade by mapping as default for read query
            if (databaseQuery.isReadQuery()) {
                databaseQuery.cascadeByMapping();
            }

            // Apply the lock mode.
            if (lockMode != null && !lockMode.name().equals(ObjectLevelReadQuery.NONE)) {
                if (databaseQuery.isObjectLevelReadQuery()) {
                    // If setting the lock mode returns true, we were unable to
                    // set the lock mode, throw an exception.
                    if (((ObjectLevelReadQuery) databaseQuery).setLockModeType(lockMode.name(), (AbstractSession) session)) {
                        throw new PersistenceException(ExceptionLocalization.buildMessage("ejb30-wrong-lock_called_without_version_locking-index", null));
                    }
                } else {
                    throw new IllegalArgumentException(ExceptionLocalization.buildMessage("invalid_lock_query", (Object[]) null));
                }
            }

            // Apply any query hints.
            databaseQuery = applyHints(hints, databaseQuery, classLoader, (AbstractSession) session);
            if (isCacheable) {
                // Prepare query as hint may cause cloning (but not un-prepare
                // as in read-only).
                databaseQuery.prepareCall(session, new DatabaseRecord());
                session.getProject().getJPQLParseCache().put(jpql, databaseQuery);
            }
        }

        return databaseQuery;
    }

    /**
     * Build a ReadAllQuery from a class and sql string.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(Class resultClass, String sqlString, ClassLoader classLoader, AbstractSession session) {
        return buildSQLDatabaseQuery(resultClass, sqlString, null, classLoader, session);
    }

    /**
     * Build a ReadAllQuery for class and sql string.
     * 
     * @param hints
     *            a list of hints to be applied to the query.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(Class resultClass, String sqlString, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        ReadAllQuery query = new ReadAllQuery(resultClass);
        query.setSQLString(sqlString);
        query.setIsUserDefined(true);

        // apply any query hints
        return applyHints(hints, query, classLoader, session);
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and sql
     * string.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(String sqlResultSetMappingName, String sqlString, ClassLoader classLoader, AbstractSession session) {
        return buildSQLDatabaseQuery(sqlResultSetMappingName, sqlString, null, classLoader, session);
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and sql
     * string.
     * 
     * @param hints
     *            a list of hints to be applied to the query.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(String sqlResultSetMappingName, String sqlString, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        ResultSetMappingQuery query = new ResultSetMappingQuery();
        query.setSQLResultSetMappingName(sqlResultSetMappingName);
        query.setSQLString(sqlString);
        query.setIsUserDefined(true);

        // apply any query hints
        return applyHints(hints, query, classLoader, session);
    }

    /**
     * Build a ReadAllQuery from a class and stored procedure call.
     */
    public static DatabaseQuery buildStoredProcedureQuery(Class resultClass, StoredProcedureCall call, List<String> arguments, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        DatabaseQuery query = new ReadAllQuery(resultClass);
        query.setCall(call);
        query.setIsUserDefined(true);

        // apply any query hints
        query = applyHints(hints, query, classLoader, session);

        // apply any query arguments
        applyArguments(arguments, query);

        return query;
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and a
     * stored procedure call.
     */
    public static DatabaseQuery buildStoredProcedureQuery(StoredProcedureCall call, List<String> arguments, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        DataReadQuery query = new DataReadQuery();
        query.setResultType(DataReadQuery.AUTO);

        query.setCall(call);
        query.setIsUserDefined(true);

        // apply any query hints
        DatabaseQuery hintQuery = applyHints(hints, query, classLoader, session);

        // apply any query arguments
        applyArguments(arguments, hintQuery);

        return hintQuery;
    }

    /**
     * Build a ResultSetMappingQuery from a sql result set mapping name and a
     * stored procedure call.
     */
    public static DatabaseQuery buildStoredProcedureQuery(String sqlResultSetMappingName, StoredProcedureCall call, List<String> arguments, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        ResultSetMappingQuery query = new ResultSetMappingQuery();
        query.setSQLResultSetMappingName(sqlResultSetMappingName);
        query.setCall(call);
        query.setIsUserDefined(true);

        // apply any query hints
        DatabaseQuery hintQuery = applyHints(hints, query, classLoader, session);

        // apply any query arguments
        applyArguments(arguments, hintQuery);

        return hintQuery;
    }

    /**
     * Build a DataReadQuery from a sql string.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(String sqlString, ClassLoader classLoader, AbstractSession session) {
        return buildSQLDatabaseQuery(sqlString, new HashMap<String, Object>(), classLoader, session);
    }

    /**
     * Build a DataReadQuery from a sql string.
     */
    public static DatabaseQuery buildSQLDatabaseQuery(String sqlString, Map<String, Object> hints, ClassLoader classLoader, AbstractSession session) {
        DataReadQuery query = new DataReadQuery();
        query.setResultType(DataReadQuery.AUTO);
        query.setSQLString(sqlString);
        query.setIsUserDefined(true);

        // apply any query hints
        return applyHints(hints, query, classLoader, session);
    }

    /**
     * Execute a ReadQuery by assigning the stored parameter values and running
     * it in the database
     * 
     * @return the results of the query execution
     */
    protected Object executeReadQuery() {
        List parameterValues = processParameters();
        // TODO: the following performFlush() call is a temporary workaround for
        // bug 4752493:
        // CTS: INMEMORY QUERYING IN EJBQUERY BROKEN DUE TO CHANGE TO USE
        // REPORTQUERY.
        // Ideally we should only flush in case the selectionExpression can't be
        // conformed in memory.
        // There are two alternative ways to implement that:
        // 1. Try running the query with conformInUOW flag first - if it fails
        // with
        // QueryException.cannotConformExpression then flush and run the query
        // again -
        // now without conforming.
        // 2. Implement a new isComformable method on Expression which would
        // determine whether the expression
        // could be conformed in memory, flush only in case it returns false.
        // Note that doesConform method currently implemented on Expression
        // requires object(s) to be confirmed as parameter(s).
        // The new isComformable method should not take any objects as
        // parameters,
        // it should return false if there could be such an object that
        // passed to doesConform causes it to throw
        // QueryException.cannotConformExpression -
        // and true otherwise.
        boolean shouldResetConformResultsInUnitOfWork = false;
        if (isFlushModeAUTO()) {
            performPreQueryFlush();
            if (getDatabaseQueryInternal().isObjectLevelReadQuery()) {
                if (((ObjectLevelReadQuery) getDatabaseQueryInternal()).shouldConformResultsInUnitOfWork()) {
                    cloneSharedQuery();
                    ((ObjectLevelReadQuery) getDatabaseQueryInternal()).setCacheUsage(ObjectLevelReadQuery.UseDescriptorSetting);
                    shouldResetConformResultsInUnitOfWork = true;
                }
            }
        }

        // Set a pessimistic locking on the query if specified.
        if (this.lockMode != null) {
            // We need to throw TransactionRequiredException if there is no
            // active transaction
            this.entityManager.checkForTransaction(true);

            // The lock mode setters and getters validate the query type
            // so should be safe to make the casting.
            cloneSharedQuery();

            // Set the lock mode (the session is passed in to do some validation
            // checks)
            // If the return value from the set returns true, it indicates that
            // we were unable to set the lock mode.
            if (((ObjectLevelReadQuery) getDatabaseQueryInternal()).setLockModeType(lockMode.name(), (AbstractSession) getActiveSession())) {
                throw new PersistenceException(ExceptionLocalization.buildMessage("ejb30-wrong-lock_called_without_version_locking-index", null));
            }
        }

        Session session = getActiveSession();
        try {
            // in case it's a user-defined query
            if (getDatabaseQueryInternal().isUserDefined()) {
                // and there is an active transaction
                if (this.entityManager.checkForTransaction(false) != null) {
                    // verify whether uow has begun early transaction
                    if (session.isUnitOfWork() && !((UnitOfWorkImpl) session).wasTransactionBegunPrematurely()) {
                        // uow begins early transaction in case it hasn't
                        // already begun.
                        // TODO: This is not good, it means that no SQL queries
                        // can ever use the cache,
                        // using isUserDefined to mean an SQL query is also
                        // wrong.
                        ((UnitOfWorkImpl) session).beginEarlyTransaction();
                    }
                }
            }

            // Execute the query and return the result.
            return session.executeQuery(getDatabaseQueryInternal(), parameterValues);
        } catch (DatabaseException e) {
            // If we catch a database exception as a result of executing a
            // pessimistic locking query we need to ask the platform which
            // JPA 2.0 locking exception we should throw. It will be either
            // be a PessimisticLockException or a LockTimeoutException (if
            // the query was executed using a wait timeout value)
            if (lockMode != null && lockMode.name().contains(ObjectLevelReadQuery.PESSIMISTIC_)) {
                // ask the platform if it is a lock timeout
                if (session.getPlatform().isLockTimeoutException(e)) {
                    throw new LockTimeoutException(e);
                } else {
                    throw new PessimisticLockException(e);
                }
            } else {
                setRollbackOnly();
                throw e;
            }
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        } finally {
            lockMode = null;

            if (shouldResetConformResultsInUnitOfWork) {
                ((ObjectLevelReadQuery) getDatabaseQueryInternal()).conformResultsInUnitOfWork();
            }
        }
    }

    /**
     * Execute an update or delete statement.
     * 
     * @return the number of entities updated or deleted
     */
    public int executeUpdate() {
        try {
            // bug51411440: need to throw IllegalStateException if query
            // executed on closed em
            entityManager.verifyOpen();
            setAsSQLModifyQuery();
            // bug:4294241, only allow modify queries - UpdateAllQuery preferred
            if (!(getDatabaseQueryInternal() instanceof ModifyQuery)) {
                throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_query_for_execute_update"));
            }

            // need to throw TransactionRequiredException if there is no active
            // transaction
            entityManager.checkForTransaction(true);

            // fix for bug:4288845, did not add the parameters to the query
            List parameterValues = processParameters();
            if (isFlushModeAUTO()) {
                performPreQueryFlush();
            }
            Integer changedRows = (Integer) getActiveSession().executeQuery(databaseQuery, parameterValues);
            return changedRows.intValue();
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Return the wrapped {@link DatabaseQuery} ensuring that if it
     * {@link #isShared} it is cloned before returning to prevent corruption of
     * the query cache.
     * 
     * @see #getDatabaseQueryInternal()
     */
    public DatabaseQuery getDatabaseQuery() {
        cloneSharedQuery();
        return getDatabaseQueryInternal();
    }

    /**
     * INTERNAL: Return the cached database query for this EJBQueryImpl. If the
     * query is a named query and it has not yet been looked up, the query will
     * be looked up and stored as the cached query.
     */
    public DatabaseQuery getDatabaseQueryInternal() {
        if ((this.queryName != null) && (this.databaseQuery == null)) {
            // need error checking and appropriate exception for non-existing
            // query
            this.databaseQuery = this.entityManager.getServerSession().getQuery(this.queryName);
            if (this.databaseQuery != null) {
                if (!this.databaseQuery.isPrepared()) {
                    // prepare the query before cloning, this ensures we do not
                    // have to continually prepare on each usage
                    this.databaseQuery.prepareCall(this.entityManager.getServerSession(), new DatabaseRecord());
                }
            } else {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("unable_to_find_named_query", new Object[] { this.queryName }));
            }

        }
        return this.databaseQuery;
    }

    /**
     * Return the entityManager this query is tied to.
     */
    public JpaEntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Get the current lock mode for the query.
     * 
     * @return lock mode
     * @throws IllegalStateException
     *             if not a Java Persistence query language SELECT query
     */
    public LockModeType getLockMode() {
        try {
            entityManager.verifyOpen();

            if (!getDatabaseQueryInternal().isReadQuery()) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("invalid_lock_query", (Object[]) null));
            }

            return this.lockMode;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Non-standard method to return results of a ReadQuery that uses a Cursor.
     * 
     * @return Cursor on results, either a CursoredStream, or ScrollableCursor
     */
    public Cursor getResultCursor() {
        // bug51411440: need to throw IllegalStateException if query executed on
        // closed em
        entityManager.verifyOpen();
        setAsSQLReadQuery();
        propagateResultProperties();
        // bug:4297903, check container policy class and throw exception if its
        // not the right type
        if (getDatabaseQueryInternal() instanceof ReadAllQuery) {
            if (!((ReadAllQuery) getDatabaseQueryInternal()).getContainerPolicy().isCursorPolicy()) {
                Class containerClass = ((ReadAllQuery) getDatabaseQueryInternal()).getContainerPolicy().getContainerClass();
                throw QueryException.invalidContainerClass(containerClass, Cursor.class);
            }
        } else if (getDatabaseQueryInternal() instanceof ReadObjectQuery) {
            // bug:4300879, no support for ReadObjectQuery if a collection is
            // required
            throw QueryException.incorrectQueryObjectFound(getDatabaseQueryInternal(), ReadAllQuery.class);
        } else if (!(getDatabaseQueryInternal() instanceof ReadQuery)) {
            throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_query_for_get_result_collection"));
        }

        try {
            Object result = executeReadQuery();
            return (Cursor) result;
        } catch (LockTimeoutException e) {
            throw e;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Non-standard method to return results of a ReadQuery that has a
     * containerPolicy that returns objects as a collection rather than a List
     * 
     * @return Collection of results
     */
    public Collection getResultCollection() {
        // bug51411440: need to throw IllegalStateException if query executed on
        // closed em
        entityManager.verifyOpen();
        setAsSQLReadQuery();
        propagateResultProperties();
        // bug:4297903, check container policy class and throw exception if its
        // not the right type
        if (getDatabaseQueryInternal() instanceof ReadAllQuery) {
            Class containerClass = ((ReadAllQuery) getDatabaseQueryInternal()).getContainerPolicy().getContainerClass();
            if (!Helper.classImplementsInterface(containerClass, ClassConstants.Collection_Class)) {
                throw QueryException.invalidContainerClass(containerClass, ClassConstants.Collection_Class);
            }
        } else if (getDatabaseQueryInternal() instanceof ReadObjectQuery) {
            // bug:4300879, no support for ReadObjectQuery if a collection is
            // required
            throw QueryException.incorrectQueryObjectFound(getDatabaseQueryInternal(), ReadAllQuery.class);
        } else if (!(getDatabaseQueryInternal() instanceof ReadQuery)) {
            throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_query_for_get_result_collection"));
        }

        try {
            Object result = executeReadQuery();
            return (Collection) result;
        } catch (LockTimeoutException e) {
            throw e;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Execute the query and return the query results as a List.
     * 
     * @return a list of the results
     */
    public List getResultList() {
        try {
            // bug51411440: need to throw IllegalStateException if query
            // executed on closed em
            entityManager.verifyOpen();
            setAsSQLReadQuery();
            propagateResultProperties();
            // bug:4297903, check container policy class and throw exception if
            // its not the right type
            if (getDatabaseQueryInternal() instanceof ReadAllQuery) {
                Class containerClass = ((ReadAllQuery) getDatabaseQueryInternal()).getContainerPolicy().getContainerClass();
                if (!Helper.classImplementsInterface(containerClass, ClassConstants.List_Class)) {
                    throw QueryException.invalidContainerClass(containerClass, ClassConstants.List_Class);
                }
            } else if (getDatabaseQueryInternal() instanceof ReadObjectQuery) {
                // bug:4300879, handle ReadObjectQuery returning null
                throw QueryException.incorrectQueryObjectFound(getDatabaseQueryInternal(), ReadAllQuery.class);
            } else if (!(getDatabaseQueryInternal() instanceof ReadQuery)) {
                throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_query_for_get_result_list"));
            }
            Object result = executeReadQuery();
            return (List) result;
        } catch (LockTimeoutException e) {
            throw e;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Execute a query that returns a single result.
     * 
     * @return the result
     * @throws javax.persistence.EntityNotFoundException
     *             if there is no result
     * @throws javax.persistence.NonUniqueResultException
     *             if more than one result
     */
    public X getSingleResult() {
        boolean rollbackOnException = true;
        try {
            // bug51411440: need to throw IllegalStateException if query
            // executed on closed em
            entityManager.verifyOpen();
            setAsSQLReadQuery();
            propagateResultProperties();
            // This API is used to return non-List results, so no other
            // validation is done.
            // It could be Cursor or other Collection or Map type.
            if (!(getDatabaseQueryInternal() instanceof ReadQuery)) {
                throw new IllegalStateException(ExceptionLocalization.buildMessage("incorrect_query_for_get_single_result"));
            }
            Object result = executeReadQuery();
            if (result instanceof List) {
                List results = (List) result;
                if (results.isEmpty()) {
                    rollbackOnException = false;
                    throwNoResultException(ExceptionLocalization.buildMessage("no_entities_retrieved_for_get_single_result", (Object[]) null));
                } else if (results.size() > 1) {
                    rollbackOnException = false;
                    throwNonUniqueResultException(ExceptionLocalization.buildMessage("too_many_results_for_get_single_result", (Object[]) null));
                }
                return (X) results.get(0);
            } else {
                if (result == null) {
                    rollbackOnException = false;
                    throwNoResultException(ExceptionLocalization.buildMessage("no_entities_retrieved_for_get_single_result", (Object[]) null));
                }
                return (X) result;
            }
        } catch (LockTimeoutException e) {
            throw e;
        } catch (RuntimeException e) {
            if (rollbackOnException) {
                setRollbackOnly();
            }
            throw e;
        }
    }

    /**
     * Internal method to add the parameters values to the query prior to
     * execution. Returns a list of parameter values in the order the parameters
     * are defined for the databaseQuery.
     */
    protected List<Object> processParameters() {
        DatabaseQuery query = getDatabaseQueryInternal();
        List arguments = query.getArguments();
        if (arguments.isEmpty()) {
            // This occurs for native queries, as the query does not know of its
            // arguments.
            // This may have issues, it is better if the query set its arguments
            // when parsing the SQL.

            arguments = new ArrayList<String>(this.parameterValues.keySet());
            query.setArguments(arguments);
        }
        // now create parameterValues in the same order as the argument list
        int size = arguments.size();
        List<Object> parameterValues = new ArrayList<Object>(size);
        for (int index = 0; index < size; index++) {
            String name = (String) arguments.get(index);
            Object parameter = this.parameterValues.get(name);
            if ((parameter != null) || this.parameterValues.containsKey(name)) {
                parameterValues.add(parameter);
            } else {
                // Error: missing actual parameter value
                throw new IllegalStateException(ExceptionLocalization.buildMessage("missing_parameter_value", new Object[] { name }));
            }
        }
        return parameterValues;
    }

    /**
     * Replace the cached query with the given query.
     */
    public void setDatabaseQuery(DatabaseQuery query) {
        databaseQuery = query;
    }

    /**
     * Set the position of the first result to retrieve.
     * 
     * @param start
     *            position of the first result, numbered from 0
     * @return the same query instance
     */
    public TypedQuery setFirstResult(int startPosition) {
        try {
            entityManager.verifyOpen();
            setFirstResultInternal(startPosition);
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * @see javax.persistence.Query#getFirstResult()
     * @since Java Persistence API 2.0
     */
    public int getFirstResult() {
        return this.firstResultIndex;
    }

    /**
     * Set the position of the first result to retrieve.
     * 
     * @param startPosition
     *            position of the first result, numbered from 0.
     */
    protected void setFirstResultInternal(int startPosition) {
        if (startPosition < 0) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("negative_start_position", (Object[]) null));
        }
        firstResultIndex = startPosition;
    }

    /**
     * Set the flush mode type to be used for the query execution.
     * 
     * @param flushMode
     */
    public TypedQuery setFlushMode(FlushModeType flushMode) {
        try {
            entityManager.verifyOpen();
            if (flushMode == null) {
                getDatabaseQueryInternal().setFlushOnExecute(null);
            } else {
                cloneSharedQuery();
                getDatabaseQueryInternal().setFlushOnExecute(flushMode == FlushModeType.AUTO);
            }
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Set implementation-specific query arguments.
     */
    protected static void applyArguments(List<String> arguments, DatabaseQuery query) {
        for (String argument : arguments) {
            query.addArgument(argument);
        }
    }

    /**
     * Set implementation-specific hints.
     * 
     * @param hints
     *            a list of hints to be applied to the query
     * @param query
     *            the query to apply the hints to
     */
    protected static DatabaseQuery applyHints(Map<String, Object> hints, DatabaseQuery query, ClassLoader classLoader, AbstractSession session) {
        return QueryHintsHandler.apply(hints, query, classLoader, session);
    }

    /**
     * Return a boolean indicating whether a value has been bound to the
     * parameter.
     * 
     * @param param
     *            parameter object
     * @return boolean indicating whether parameter has been bound
     */
    public boolean isBound(Parameter<?> param) {
        if (param == null)
            return false;
        return this.parameterValues.containsKey(param.getName());
    }

    /**
     * Spec. 3.5.2: "FlushMode.AUTO is set on the Query object, or if the flush
     * mode setting for the persistence context is AUTO (the default) and a
     * flush mode setting has not been specified for the Query object, the
     * persistence provider is responsible for ensuring that all updates to the
     * state of all entities in the persistence context which could potentially
     * affect the result of the query are visible to the processing of the
     * query."
     */
    protected boolean isFlushModeAUTO() {
        if (getDatabaseQueryInternal().getFlushOnExecute() != null) {
            return getDatabaseQueryInternal().getFlushOnExecute().booleanValue();
        } else {
            return entityManager.isFlushModeAUTO();
        }
    }

    /**
     * Set an implementation-specific hint. If the hint name is not recognized,
     * it is silently ignored.
     * 
     * @param hintName
     * @param value
     * @return the same query instance
     * @throws IllegalArgumentException
     *             if the second argument is not valid for the implementation
     */
    public TypedQuery setHint(String hintName, Object value) {
        try {
            entityManager.verifyOpen();
            setHintInternal(hintName, value);
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Set an implementation-specific hint. If the hint name is not recognized,
     * it is silently ignored.
     * 
     * @throws IllegalArgumentException
     *             if the second argument is not valid for the implementation.
     */
    protected void setHintInternal(String hintName, Object value) {
        cloneSharedQuery();
        ClassLoader loader = getEntityManager().getServerSession().getLoader();
        DatabaseQuery hintQuery = QueryHintsHandler.apply(hintName, value, getDatabaseQueryInternal(), loader, (AbstractSession) getActiveSession());
        if (hintQuery != null) {
            setDatabaseQuery(hintQuery);
        }
    }

    /**
     * Set the lock mode type to be used for the query execution.
     * 
     * @param lockMode
     * @throws IllegalStateException
     *             if not a Java Persistence query language SELECT query
     */
    public TypedQuery setLockMode(LockModeType lockMode) {
        try {
            entityManager.verifyOpen();

            if (!getDatabaseQueryInternal().isReadQuery()) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("invalid_lock_query", (Object[]) null));
            }

            this.lockMode = lockMode;
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * If the query was from the jpql parse cache it must be cloned before being
     * modified.
     */
    protected void cloneSharedQuery() {
        DatabaseQuery query = getDatabaseQueryInternal();
        if (this.isShared) {
            // Clone to allow setting of hints or other properties without
            // corrupting original query.
            query = (DatabaseQuery) databaseQuery.clone();
            setDatabaseQuery(query);
            this.isShared = false;
        }
    }

    /**
     * Convert the given object to the class represented by the given temporal
     * type.
     * 
     * @return an object representing the given TemporalType.
     */
    protected Object convertTemporalType(Object value, TemporalType type) {
        ConversionManager conversionManager = ((org.eclipse.persistence.internal.sessions.AbstractSession) getEntityManager().getActiveSession()).getDatasourcePlatform().getConversionManager();
        if (type == TemporalType.TIME) {
            return conversionManager.convertObject(value, ClassConstants.TIME);
        } else if (type == TemporalType.TIMESTAMP) {
            return conversionManager.convertObject(value, ClassConstants.TIMESTAMP);
        } else if (type == TemporalType.DATE) {
            return conversionManager.convertObject(value, ClassConstants.SQLDATE);
        }
        return value;
    }

    /**
     * Set the maximum number of results to retrieve.
     * 
     * @param maxResult
     * @return the same query instance
     */
    public TypedQuery setMaxResults(int maxResult) {
        try {
            entityManager.verifyOpen();
            setMaxResultsInternal(maxResult);
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * @see javax.persistence.Query#getMaxResults()
     * @since Java Persistence API 2.0
     */
    public int getMaxResults() {
        return this.maxResults;
    }

    /**
     * Set the maximum number of results to retrieve.
     * 
     * @param maxResult
     */
    public void setMaxResultsInternal(int maxResult) {
        if (maxResult < 0) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("negative_max_result", (Object[]) null));
        }
        this.maxResults = maxResult;
    }

    /**
     * Bind an argument to a named parameter.
     * 
     * @param name
     *            the parameter name
     * @param value
     * @return the same query instance
     */
    public TypedQuery setParameter(String name, Object value) {
        try {
            entityManager.verifyOpen();
            setParameterInternal(name, value, false);
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Bind an instance of java.util.Date to a named parameter.
     * 
     * @param name
     * @param value
     * @param temporalType
     * @return the same query instance
     */
    public TypedQuery setParameter(String name, Date value, TemporalType temporalType) {
        entityManager.verifyOpen();
        return setParameter(name, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an instance of java.util.Calendar to a named parameter.
     * 
     * @param name
     * @param value
     * @param temporalType
     * @return the same query instance
     */
    public TypedQuery setParameter(String name, Calendar value, TemporalType temporalType) {
        entityManager.verifyOpen();
        return setParameter(name, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an argument to a positional parameter.
     * 
     * @param position
     * @param value
     * @return the same query instance
     */
    public TypedQuery setParameter(int position, Object value) {
        try {
            entityManager.verifyOpen();
            setParameterInternal(position, value);
            return this;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Bind an instance of java.util.Date to a positional parameter.
     * 
     * @param position
     * @param value
     * @param temporalType
     * @return the same query instance
     */
    public TypedQuery setParameter(int position, Date value, TemporalType temporalType) {
        entityManager.verifyOpen();
        return setParameter(position, convertTemporalType(value, temporalType));
    }

    /**
     * Bind an instance of java.util.Calendar to a positional parameter.
     * 
     * @param position
     * @param value
     * @param temporalType
     * @return the same query instance
     */
    public TypedQuery setParameter(int position, Calendar value, TemporalType temporalType) {
        entityManager.verifyOpen();
        return setParameter(position, convertTemporalType(value, temporalType));
    }

    /**
     * Configure the firstResult, maxRows and lock mode in the EclipseLink
     * ReadQuery.
     */
    protected void propagateResultProperties() {
        DatabaseQuery databaseQuery = getDatabaseQueryInternal();
        if (databaseQuery.isReadQuery()) {
            ReadQuery readQuery = (ReadQuery) databaseQuery;
            if (maxResults >= 0) {
                cloneSharedQuery();
                readQuery = (ReadQuery) getDatabaseQueryInternal();
                int maxRows = maxResults + ((firstResultIndex >= 0) ? firstResultIndex : 0);
                readQuery.setMaxRows(maxRows);
                maxResults = -1;
            }
            if (firstResultIndex > -1) {
                cloneSharedQuery();
                readQuery = (ReadQuery) getDatabaseQueryInternal();
                readQuery.setFirstResult(firstResultIndex);
                firstResultIndex = -1;
            }
        }
    }

    /**
     * Bind an argument to a named or indexed parameter.
     * 
     * @param name
     *            the parameter name
     * @param value
     *            to bind
     * @param isIndex
     *            defines if index or named
     */
    protected void setParameterInternal(String name, Object value, boolean isIndex) {
        DatabaseQuery query = getDatabaseQueryInternal();
        int index = query.getArguments().indexOf(name);
        if (query.getQueryMechanism().isJPQLCallQueryMechanism()) { // only non
                                                                    // native
                                                                    // queries
            if (index == -1) {
                if (isIndex) {
                    throw new IllegalArgumentException(ExceptionLocalization.buildMessage("ejb30-wrong-argument-index", new Object[] { name, query.getEJBQLString() }));
                } else {
                    throw new IllegalArgumentException(ExceptionLocalization.buildMessage("ejb30-wrong-argument-name", new Object[] { name, query.getEJBQLString() }));
                }
            }
            Class type = query.getArgumentTypes().get(index);
            if (!isValidActualParameter(value, type)) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("ejb30-incorrect-parameter-type", new Object[] { name, value.getClass(), query.getArgumentTypes().get(index), query.getEJBQLString() }));
            }
        }
        this.parameterValues.put(name, value);
    }

    /**
     * Bind an argument to a positional parameter.
     * 
     * @param position
     * @param value
     */
    protected void setParameterInternal(int position, Object value) {
        setParameterInternal(String.valueOf(position), value, true);
    }

    protected boolean isValidActualParameter(Object value, Class parameterType) {
        if (value == null) {
            return true;
        } else {
            return BasicTypeHelperImpl.getInstance().isAssignableFrom(parameterType, value.getClass());
        }
    }

    protected Session getActiveSession() {
        DatabaseQuery query = getDatabaseQueryInternal();
        // PERF: If read-only query, avoid creating unit of work and JTA
        // transaction.
        if (query.isObjectLevelReadQuery() && ((ObjectLevelReadQuery) query).isReadOnly()) {
            return this.entityManager.getReadOnlySession();
        }
        return this.entityManager.getActiveSession();
    }

    protected void performPreQueryFlush() {
        if (this.entityManager.shouldFlushBeforeQuery()) {
            this.entityManager.flush();
        }
    }

    protected void setRollbackOnly() {
        entityManager.setRollbackOnly();
    }

    protected void throwNoResultException(String message) {
        throw new NoResultException(message);
    }

    protected void throwNonUniqueResultException(String message) {
        throw new NonUniqueResultException(message);
    }

    /**
     * @see Query#getFlushMode()
     * @since Java Persistence 2.0
     */
    public FlushModeType getFlushMode() {
        try {
            entityManager.verifyOpen();
            if (getDatabaseQueryInternal().getFlushOnExecute())
                return FlushModeType.AUTO;
            return FlushModeType.COMMIT;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * @see Query#getHints()
     * @since Java Persistence 2.0
     */
    public Map<String, Object> getHints() {
        return (Map<String, Object>) getDatabaseQueryInternal().getProperty(QueryHintsHandler.QUERY_HINT_PROPERTY);
    }

    /**
     * @see Query#getParameter(String, Class)
     * @since Java Persistence 2.0
     */
    public <T> Parameter<T> getParameter(String name, Class<T> type) {
        try {
            entityManager.verifyOpen();
            Parameter param = (Parameter) this.parameters.get(name);
            if (param == null) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NO_PARAMETER_WITH_NAME", new Object[] { name, this.databaseQuery }));

            }
            return param;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * @see Query#getParameter(int, Class)
     * @since Java Persistence 2.0
     */
    public <T> Parameter<T> getParameter(int position, Class<T> type) {
        try {
            entityManager.verifyOpen();
            Parameter param = (Parameter) this.parameters.get(String.valueOf(position));
            if (param == null) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NO_PARAMETER_WITH_INDEX", new Object[] { position, this.databaseQuery }));

            }
            return param;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * @see Query#getParameter(String, Class)
     * @since Java Persistence 2.0
     */
    public Parameter<?> getParameter(String name) {
        try {
            entityManager.verifyOpen();
            Parameter param = (Parameter) this.parameters.get(name);
            if (param == null) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NO_PARAMETER_WITH_NAME", new Object[] { name, this.databaseQuery }));
            }
            return param;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * @see Query#getParameter(int, Class)
     * @since Java Persistence 2.0
     */
    public Parameter<?> getParameter(int position) {
        try {
            entityManager.verifyOpen();
            Parameter param = (Parameter) this.parameters.get(String.valueOf(position));
            if (param == null) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NO_PARAMETER_WITH_INDEX", new Object[] { position, this.databaseQuery }));
            }
            return param;
        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * @see Query#getParameterValue(Parameter)
     * @since Java Persistence 2.0
     */
    public <T> T getParameterValue(Parameter<T> param) {
        if (param == null)
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("PARAMETER_NILL_NOT_FOUND"));
        return (T) this.getParameterValue(param.getName());
    }

    /**
     * Return the value bound to the named parameter.
     * 
     * @param name
     * @return parameter value
     * @throws IllegalStateException
     *             if the parameter has not been been bound
     */
    public Object getParameterValue(String name) {
        try {
            entityManager.verifyOpen();
            if (!this.parameters.containsKey(new ParameterExpressionImpl(null, null, name))) {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NO_PARAMETER_WITH_NAME", new Object[] { name, this.databaseQuery }));
            }
            if (!this.parameterValues.containsKey(name)) { // must check for
                                                           // key. get() would
                                                           // return negative
                                                           // for value == null.
                throw new IllegalStateException(ExceptionLocalization.buildMessage("NO_VALUE_BOUND", new Object[] { name }));
            }
            return this.parameterValues.get(name);

        } catch (RuntimeException e) {
            setRollbackOnly();
            throw e;
        }
    }

    /**
     * Return the value bound to the positional parameter.
     * 
     * @param position
     * @return parameter value
     * @throws IllegalStateException
     *             if the parameter has not been been bound
     */
    public Object getParameterValue(int position) {
        String param = String.valueOf(position);
        if (!this.parameterValues.containsKey(param)) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("position_param_not_found", new Object[] { position }));
        }
        return this.parameterValues.get(param);
    }

    /**
     * @see Query#getParameters()
     * @since Java Persistence 2.0
     */
    public Set<Parameter<?>> getParameters() {
        if (this.parameters == null) {
            DatabaseQuery query = getDatabaseQueryInternal(); // Retrieve named
                                                              // query
            int count = 0;
            if (query.getArguments() != null && !query.getArguments().isEmpty()) {
                this.parameters = new HashMap<Parameter<?>, Parameter<?>>();
                for (String argName : query.getArguments()) {
                    Parameter<?> param = new ParameterExpressionImpl(null, query.getArgumentTypes().get(count), argName);
                    this.parameters.put(param, param);
                    ++count;
                }
            }

        }
        return this.parameters.keySet();
    }

    /**
     * @see Query#getSupportedHints()
     * @since Java Persistence 2.0
     */
    public Set<String> getSupportedHints() {
        return QueryHintsHandler.getSupportedHints();
    }

    /**
     * Set the value of a Parameter object.
     * 
     * @param param
     *            parameter to be set
     * @param value
     *            parameter value
     * @return query instance
     * @throws IllegalArgumentException
     *             if parameter does not correspond to a parameter of the query
     */
    public <T> TypedQuery setParameter(Parameter<T> param, T value) {
        if (param == null)
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NULL_PARAMETER_PASSED_TO_SET_PARAMETER"));
        return this.setParameter(param.getName(), value);
    }

    /**
     * Bind an instance of java.util.Date to a Parameter object.
     * 
     * @param parameter
     *            object
     * @param value
     * @param temporalType
     * @return the same query instance
     * @throws IllegalArgumentException
     *             if position does not correspond to a parameter of the query
     */
    public TypedQuery setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
        if (param == null)
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NULL_PARAMETER_PASSED_TO_SET_PARAMETER"));
        return this.setParameter(param.getName(), value, temporalType);
    }

    /**
     * Bind an instance of java.util.Calendar to a Parameter object.
     * 
     * @param parameter
     * @param value
     * @param temporalType
     * @return the same query instance
     * @throws IllegalArgumentException
     *             if position does not correspond to a parameter of the query
     */
    public TypedQuery setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
        if (param == null)
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("NULL_PARAMETER_PASSED_TO_SET_PARAMETER"));
        return this.setParameter(param.getName(), value, temporalType);
    }

    /**
     * Unwrap the query into the JPA implementation classes/interfaces or the
     * underlying native EclipseLink query.
     * 
     * @see Query#unwrap(Class)
     * @since Java Persistence 2.0
     */
    public <T> T unwrap(Class<T> cls) {
        if (cls.isAssignableFrom(this.getClass())) {
            // unwraps any proxy to Query, JPAQuery or EJBQueryImpl
            return (T) this;
        }
        if (cls.isAssignableFrom(getDatabaseQueryInternal().getClass())) {
            return (T) getDatabaseQueryInternal();
        }

        throw new PersistenceException("Could not unwrap query to: " + cls);
    }

}
