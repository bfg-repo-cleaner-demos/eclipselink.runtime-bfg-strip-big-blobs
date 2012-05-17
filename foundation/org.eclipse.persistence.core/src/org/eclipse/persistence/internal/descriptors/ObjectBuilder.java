/*******************************************************************************
 * Copyright (c) 1998, 2012 Oracle and/or its affiliates. All rights reserved.
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
 *     04/01/2011-2.3 Guy Pelletier 
 *       - 337323: Multi-tenant with shared schema support (part 2)
 *     09/09/2011-2.3.1 Guy Pelletier 
 *       - 356197: Add new VPD type to MultitenantType
 *     11/10/2011-2.4 Guy Pelletier 
 *       - 357474: Address primaryKey option from tenant discriminator column
 ******************************************************************************/  
package org.eclipse.persistence.internal.descriptors;

import java.io.*;
import java.util.*;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.eclipse.persistence.annotations.BatchFetchType;
import org.eclipse.persistence.annotations.CacheKeyType;
import org.eclipse.persistence.annotations.IdValidation;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventManager;
import org.eclipse.persistence.descriptors.FetchGroupManager;
import org.eclipse.persistence.descriptors.changetracking.ChangeTracker;
import org.eclipse.persistence.descriptors.changetracking.ObjectChangePolicy;
import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.expressions.*;
import org.eclipse.persistence.internal.databaseaccess.DatabaseAccessor;
import org.eclipse.persistence.internal.databaseaccess.DatabasePlatform;
import org.eclipse.persistence.internal.databaseaccess.Platform;
import org.eclipse.persistence.internal.expressions.*;
import org.eclipse.persistence.internal.helper.*;
import org.eclipse.persistence.internal.identitymaps.*;
import org.eclipse.persistence.internal.indirection.ProxyIndirectionPolicy;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.internal.queries.EntityFetchGroup;
import org.eclipse.persistence.internal.queries.JoinedAttributeManager;
import org.eclipse.persistence.internal.sessions.*;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.mappings.*;
import org.eclipse.persistence.mappings.DatabaseMapping.WriteType;
import org.eclipse.persistence.mappings.foundation.*;
import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.mappings.querykeys.*;
import org.eclipse.persistence.sessions.remote.*;
import org.eclipse.persistence.sessions.CopyGroup;
import org.eclipse.persistence.sessions.SessionProfiler;
import org.eclipse.persistence.sessions.DatabaseRecord;

/**
 * <p><b>Purpose</b>: Object builder is one of the behavior class attached to descriptor.
 * It is responsible for building objects, rows, and extracting primary keys from
 * the object and the rows.
 *
 * @author Sati
 * @since TOPLink/Java 1.0
 */
public class ObjectBuilder implements Cloneable, Serializable {
    protected ClassDescriptor descriptor;
    /** Mappings keyed by attribute name. */
    protected Map<String, DatabaseMapping> mappingsByAttribute;
    /** Mappings keyed by database field. */
    protected Map<DatabaseField, DatabaseMapping> mappingsByField;
    /** List of read-only mappings using a database field. */
    protected Map<DatabaseField, List<DatabaseMapping>> readOnlyMappingsByField;
    /** Used to maintain identity on the field objects. Ensure they get the correct index/type. */
    protected Map<DatabaseField, DatabaseField> fieldsMap;
    /** Mapping for the primary key fields. */
    protected List<DatabaseMapping> primaryKeyMappings;
    /** The types for the primary key fields, in same order as descriptor's primary key fields. */
    protected List<Class> primaryKeyClassifications;
    /** All mapping other than primary key mappings. */
    protected transient List<DatabaseMapping> nonPrimaryKeyMappings;
    /** Expression for querying an object by primary key. */
    protected transient Expression primaryKeyExpression;
    /** PERF: Cache mapping that use joining. */
    protected List<DatabaseMapping> joinedAttributes;
    /** PERF: Cache mapping that use batch fetching. */
    protected List<DatabaseMapping> batchFetchedAttributes;
    /** PERF: Cache mapping that use batch fetching. */
    protected boolean hasInBatchFetchedAttribute;
    /** PERF: Cache mappings that require cloning. */
    protected List<DatabaseMapping> cloningMappings;    
    /** PERF: Cache mappings that are eager loaded. */
    protected List<DatabaseMapping> eagerMappings;
    /** PERF: Cache relationship mappings. */
    protected List<DatabaseMapping> relationshipMappings;
    /** PERF: Cache if is a simple mapping, all direct. */
    protected boolean isSimple;
    /** PERF: Cache if has a wrapper policy. */
    protected boolean hasWrapperPolicy;
    /** PERF: Cache sequence mappings. */
    protected AbstractDirectMapping sequenceMapping;
    /** indicates whether part of primary key is unmapped - may happen only in case AggregateObject or AggregateCollection descriptor. */
    protected boolean mayHaveNullInPrimaryKey;
    /** attribute name corresponding to optimistic lock field, set only if optimistic locking is used */
    protected String lockAttribute; 

    public ObjectBuilder(ClassDescriptor descriptor) {
        this.descriptor = descriptor;
        initialize(descriptor);
    }

    protected void initialize(ClassDescriptor descriptor) {
        this.mappingsByField = new HashMap(20);
        this.readOnlyMappingsByField = new HashMap(10);
        this.mappingsByAttribute = new HashMap(20);
        this.fieldsMap = new HashMap(20);
        this.primaryKeyMappings = new ArrayList(5);
        this.nonPrimaryKeyMappings = new ArrayList(10);
        this.cloningMappings = new ArrayList(10);
        this.eagerMappings = new ArrayList(5);
        this.relationshipMappings = new ArrayList(5);
    }

    /**
     * Create a new row/record for the object builder.
     * This allows subclasses to define different record types.
     */
    public AbstractRecord createRecord(AbstractSession session) {
        return new DatabaseRecord();
    }

    /**
     * Create a new row/record for the object builder.
     * This allows subclasses to define different record types.
     */
    public AbstractRecord createRecord(int size, AbstractSession session) {
        return new DatabaseRecord(size);
    }

    /**
     * Create a new row/record for the object builder. This allows subclasses to
     * define different record types.  This will typically be called when a 
     * record will be used for temporarily holding on to primary key fields.
     */
    protected AbstractRecord createRecordForPKExtraction(int size, AbstractSession session) {
        return createRecord(size, session);
    }

    /**
     * Add the primary key and its value to the Record for all the non default tables.
     * This method is used while writing into the multiple tables.
     */
    public void addPrimaryKeyForNonDefaultTable(AbstractRecord databaseRow) {
        // this method has been revised so it calls addPrimaryKeyForNonDefaultTable(AbstractRecord, Object, Session) is similar.
        // the session and object are null in this case.
        addPrimaryKeyForNonDefaultTable(databaseRow, null, null);
    }

    /**
     * Add the primary key and its value to the Record for all the non default tables.
     * This method is used while writing into the multiple tables.
     */
    public void addPrimaryKeyForNonDefaultTable(AbstractRecord databaseRow, Object object, AbstractSession session) {
        if (!this.descriptor.hasMultipleTables()) {
            return;
        }
        List<DatabaseTable> tables = this.descriptor.getTables();
        int size = tables.size();

        // Skip first table.
        for (int index = 1; index < size; index++) {
            DatabaseTable table = tables.get(index);
            Map keyMapping = this.descriptor.getAdditionalTablePrimaryKeyFields().get(table);

            // Loop over the additionalTablePK fields and add the PK info for the table. The join might
            // be between a fk in the source table and pk in secondary table.
            if (keyMapping != null) {
                Iterator primaryKeyFieldEnum = keyMapping.keySet().iterator();
                Iterator secondaryKeyFieldEnum = keyMapping.values().iterator();
                while (primaryKeyFieldEnum.hasNext()) {
                    DatabaseField primaryKeyField = (DatabaseField)primaryKeyFieldEnum.next();
                    DatabaseField secondaryKeyField = (DatabaseField)secondaryKeyFieldEnum.next();
                    Object primaryValue = databaseRow.getIndicatingNoEntry(primaryKeyField);

                    // normally the primary key has a value, however if the multiple tables were joined by a foreign
                    // key the foreign key has a value.
                    if ((primaryValue == AbstractRecord.noEntry)) {
                        if (object != null) {
                            DatabaseMapping mapping = getMappingForField(secondaryKeyField);
                            if (mapping == null) {
                                throw DescriptorException.missingMappingForField(secondaryKeyField, this.descriptor);
                            }
                            mapping.writeFromObjectIntoRow(object, databaseRow, session, WriteType.UNDEFINED);
                        }
                        databaseRow.put(primaryKeyField, databaseRow.get(secondaryKeyField));
                    } else {
                        databaseRow.put(secondaryKeyField, primaryValue);
                    }
                }
            }
        }
    }
    
    /**
     * Clear any primary key cache data in the object.
     */
    public void clearPrimaryKey(Object object) {
        // PERF: If PersistenceEntity is caching the primary key this must be cleared as the primary key has changed.
        if (object instanceof PersistenceEntity) {
            ((PersistenceEntity)object)._persistence_setId(null);
        }
    }
  
    /**
     * Assign the fields in the row back into the object.
     * This is used by returning, as well as events and version locking.
     * If not null changeSet must correspond to object. changeSet is updated with all of the field values in the row.
     */
    public void assignReturnRow(Object object, AbstractSession writeSession, AbstractRecord row, ObjectChangeSet changeSet) throws DatabaseException {
        writeSession.log(SessionLog.FINEST, SessionLog.QUERY, "assign_return_row", row);

        // Require a query context to read into an object.
        ReadObjectQuery query = new ReadObjectQuery();
        query.setSession(writeSession);

        // To avoid processing the same mapping twice,
        // maintain Collection of mappings already used.
        HashSet handledMappings = null;
        int size = row.size();
        if (size > 1) {
            handledMappings = new HashSet(size);
        }
        List fields = row.getFields();
        for (int index = 0; index < size; index++) {
            DatabaseField field = (DatabaseField)fields.get(index);
            assignReturnValueForField(object, query, row, field, handledMappings, changeSet);
        }
    }

    /**
     * Assign the field value from the row to the object for all the mappings using field (read or write).
     * If not null changeSet must correspond to object. changeSet is updated with all of the field values in the row.
     */
    public void assignReturnValueForField(Object object, ReadObjectQuery query, AbstractRecord row, DatabaseField field, Collection handledMappings, ObjectChangeSet changeSet) {
        DatabaseMapping mapping = getMappingForField(field);
        if (mapping != null) {
            assignReturnValueToMapping(object,  query, row, field, mapping, handledMappings, changeSet);
        }
        List readOnlyMappings = getReadOnlyMappingsForField(field);
        if (readOnlyMappings != null) {
            int size = readOnlyMappings.size();
            for (int index = 0; index < size; index++) {
                mapping = (DatabaseMapping)readOnlyMappings.get(index);
                assignReturnValueToMapping(object, query, row, field, mapping, handledMappings, changeSet);
            }
        }
    }

    /**
     * INTERNAL:
     * Assign values from objectRow to the object through the mapping.
     * If not null changeSet must correspond to object. changeSet is updated with all of the field values in the row.
     */
    protected void assignReturnValueToMapping(Object object, ReadObjectQuery query, AbstractRecord row, DatabaseField field, DatabaseMapping mapping, Collection handledMappings, ObjectChangeSet changeSet) {
        if ((handledMappings != null) && handledMappings.contains(mapping)) {
            return;
        }
        if (mapping.isAbstractDirectMapping()) {
            if(changeSet != null && (!changeSet.isNew() || (query.getDescriptor() != null && query.getDescriptor().shouldUseFullChangeSetsForNewObjects()))) {
                DirectToFieldChangeRecord changeRecord = (DirectToFieldChangeRecord)changeSet.getChangesForAttributeNamed(mapping.getAttributeName());
                Object oldAttributeValue = null;
                if (changeRecord == null) {
                    oldAttributeValue = mapping.getAttributeValueFromObject(object);
                }
                
                //use null cachekey to ensure we build directly into the attribute
                Object attributeValue = mapping.readFromRowIntoObject(row, null, object, null, query, query.getSession(), true);

                if (changeRecord == null) {
                    // Don't use ObjectChangeSet.updateChangeRecordForAttributeWithMappedObject to avoid unnecessary conversion - attributeValue is already converted.
                    changeRecord = (DirectToFieldChangeRecord)((AbstractDirectMapping)mapping).internalBuildChangeRecord(attributeValue, oldAttributeValue, changeSet);
                    changeSet.addChange(changeRecord);
                } else {
                    changeRecord.setNewValue(attributeValue);
                }
            } else {
                mapping.readFromRowIntoObject(row, null, object, null, query, query.getSession(), true);
            }
        } else if (mapping.isAggregateObjectMapping()) {
            ((AggregateObjectMapping)mapping).readFromReturnRowIntoObject(row, object, query, handledMappings, changeSet);
        } else if (mapping.isTransformationMapping()) {
            ((AbstractTransformationMapping)mapping).readFromReturnRowIntoObject(row, object, query, handledMappings, changeSet);
        } else {
            query.getSession().log(SessionLog.FINEST, SessionLog.QUERY, "field_for_unsupported_mapping_returned", field, this.descriptor);
        }
    }

    /**
     * INTERNAL:
     * Update the object primary key by fetching a new sequence number from the accessor.
     * This assume the uses sequence numbers check has already been done.
     * @return the sequence value or null if not assigned.
     * @exception  DatabaseException - an error has occurred on the database.
     */
    public Object assignSequenceNumber(Object object, AbstractSession writeSession) throws DatabaseException {
        return assignSequenceNumber(object, writeSession, null);
    }

    /**
     * INTERNAL:
     * Update the writeQuery's object primary key by fetching a new sequence number from the accessor.
     * This assume the uses sequence numbers check has already been done.
     * Adds the assigned sequence value to writeQuery's modify row.
     * If object has a changeSet then sets sequence value into change set as an Id
     * adds it also to object's change set in a ChangeRecord if required.
     * @return the sequence value or null if not assigned.
     * @exception  DatabaseException - an error has occurred on the database.
     */
    public Object assignSequenceNumber(WriteObjectQuery writeQuery) throws DatabaseException {
        return assignSequenceNumber(writeQuery.getObject(), writeQuery.getSession(), writeQuery);
    }
    
    /**
     * INTERNAL:
     * Update the object primary key by fetching a new sequence number from the accessor.
     * This assume the uses sequence numbers check has already been done.
     * Adds the assigned sequence value to writeQuery's modify row.
     * If object has a changeSet then sets sequence value into change set as an Id
     * adds it also to object's change set in a ChangeRecord if required.
     * @return the sequence value or null if not assigned.
     * @exception  DatabaseException - an error has occurred on the database.
     */
    protected Object assignSequenceNumber(Object object, AbstractSession writeSession, WriteObjectQuery writeQuery) throws DatabaseException {
        DatabaseField sequenceNumberField = this.descriptor.getSequenceNumberField();
        Object existingValue = null;
        if (this.sequenceMapping != null) {
            existingValue = this.sequenceMapping.getAttributeValueFromObject(object);
        } else {
            existingValue = getBaseValueForField(sequenceNumberField, object);
        }
        
        // PERF: The (internal) support for letting the sequence decide this was removed,
        // as anything other than primitive should allow null and default as such.
        Object sequenceValue;
        int index = this.descriptor.getPrimaryKeyFields().indexOf(sequenceNumberField);
        if (isPrimaryKeyComponentInvalid(existingValue, index) || this.descriptor.getSequence().shouldAlwaysOverrideExistingValue()) {
            sequenceValue = writeSession.getSequencing().getNextValue(this.descriptor.getJavaClass());
        } else {
            return null;
        }

        // Check that the value is not null, this occurs on any databases using IDENTITY type sequencing.
        if (sequenceValue == null) {
            return null;
        }
        
        writeSession.log(SessionLog.FINEST, SessionLog.SEQUENCING, "assign_sequence", sequenceValue, object);
        Object convertedSequenceValue = null;
        if (this.sequenceMapping != null) {
            convertedSequenceValue = this.sequenceMapping.getObjectValue(sequenceValue, writeSession);
            this.sequenceMapping.setAttributeValueInObject(object, convertedSequenceValue);
        } else {
            // Now add the value to the object, this gets ugly.
            AbstractRecord tempRow = createRecord(1, writeSession);
            tempRow.put(sequenceNumberField, sequenceValue);
    
            // Require a query context to read into an object.
            ReadObjectQuery query = new ReadObjectQuery();
            query.setSession(writeSession);
            DatabaseMapping mapping = getBaseMappingForField(sequenceNumberField);
            Object sequenceIntoObject = getParentObjectForField(sequenceNumberField, object);
    
            // The following method will return the converted value for the sequence.
            convertedSequenceValue = mapping.readFromRowIntoObject(tempRow, null, sequenceIntoObject, null, query, writeSession, true);
        }
        
        // PERF: If PersistenceEntity is caching the primary key this must be cleared as the primary key has changed.
        clearPrimaryKey(object);

        if (writeQuery != null) {
            Object primaryKey = extractPrimaryKeyFromObject(object, writeSession);
            writeQuery.setPrimaryKey(primaryKey);
            AbstractRecord modifyRow = writeQuery.getModifyRow();
            // Update the row.
            modifyRow.put(sequenceNumberField, sequenceValue);
            if (descriptor.hasMultipleTables()) {
                addPrimaryKeyForNonDefaultTable(modifyRow, object, writeSession);
            }
            // Update the changeSet if there is one.
            if (writeSession.isUnitOfWork()) {
                ObjectChangeSet objectChangeSet = writeQuery.getObjectChangeSet();
                if ((objectChangeSet == null) && (((UnitOfWorkImpl)writeSession).getUnitOfWorkChangeSet() != null)) {
                    objectChangeSet = (ObjectChangeSet)((UnitOfWorkImpl)writeSession).getUnitOfWorkChangeSet().getObjectChangeSetForClone(object);
                }
                if (objectChangeSet != null) {
                    // objectChangeSet.isNew() == true
                    if (writeQuery.getDescriptor().shouldUseFullChangeSetsForNewObjects()) {
                        if (this.sequenceMapping != null) {
                            // Don't use ObjectChangeSet.updateChangeRecordForAttribute to avoid unnecessary conversion - convertedSequenceValue is already converted. 
                            String attributeName = this.sequenceMapping.getAttributeName(); 
                            DirectToFieldChangeRecord changeRecord = (DirectToFieldChangeRecord)objectChangeSet.getChangesForAttributeNamed(attributeName);
                            if (changeRecord == null) {
                                changeRecord = new DirectToFieldChangeRecord(objectChangeSet);
                                changeRecord.setAttribute(attributeName);
                                changeRecord.setMapping(this.sequenceMapping);
                                objectChangeSet.addChange(changeRecord);
                            }
                            changeRecord.setNewValue(convertedSequenceValue);
                        } else {
                            ChangeRecord changeRecord = getBaseChangeRecordForField(objectChangeSet, object, sequenceNumberField, writeSession);
                            if (changeRecord.getMapping().isDirectCollectionMapping()) {
                                // assign converted value to the attribute
                                ((DirectToFieldChangeRecord)changeRecord).setNewValue(convertedSequenceValue);
                            } else if (changeRecord.getMapping().isTransformationMapping()) {
                                // put original (not converted) value into the record.
                                ((TransformationMappingChangeRecord)changeRecord).getRecord().put(sequenceNumberField, sequenceValue);
                            }
                        }
                    }
                    objectChangeSet.setId(primaryKey);
                }
            }
        }
        
        return convertedSequenceValue;
    }

    /**
     * Each mapping is recursed to assign values from the Record to the attributes in the domain object.
     */
    public void buildAttributesIntoObject(Object domainObject, CacheKey cacheKey, AbstractRecord databaseRow, ObjectBuildingQuery query, JoinedAttributeManager joinManager, boolean forRefresh, AbstractSession targetSession) throws DatabaseException {

        // PERF: Avoid synchronized enumerator as is concurrency bottleneck.
        List mappings = this.descriptor.getMappings();

        // PERF: Cache if all mappings should be read.
        boolean readAllMappings = query.shouldReadAllMappings();
        boolean isTargetProtected = targetSession.isProtectedSession();
        int size = mappings.size();
        for (int index = 0; index < size; index++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);
            if (readAllMappings || query.shouldReadMapping(mapping)) {
                mapping.readFromRowIntoObject(databaseRow, joinManager, domainObject, cacheKey, query, targetSession, isTargetProtected);
            }
        }

        // PERF: Avoid events if no listeners.
        if (this.descriptor.hasEventManager()) {
            DescriptorEventManager descriptorEventManager = this.descriptor.getDescriptorEventManager();
            if(descriptorEventManager.hasAnyEventListeners()) {
                // Need to run post build or refresh selector, currently check with the query for this,
                // I'm not sure which should be called it case of refresh building a new object, currently refresh is used...
                org.eclipse.persistence.descriptors.DescriptorEvent event = new org.eclipse.persistence.descriptors.DescriptorEvent(domainObject);
                event.setQuery(query);
                event.setSession(query.getSession());
                event.setRecord(databaseRow);
                if (forRefresh) {
                    //this method can be called from different places within TopLink.  We may be
                    //executing refresh query but building the object not refreshing so we must
                    //throw the appropriate event.
                    //bug 3325315
                    event.setEventCode(DescriptorEventManager.PostRefreshEvent);
                } else {
                    event.setEventCode(DescriptorEventManager.PostBuildEvent);
                }
                descriptorEventManager.executeEvent(event);
            }
        }
    }

    /**
     * Returns the backup clone of the specified object. This is called only from unit of work.
     * The clone sent as parameter is always a working copy from the unit of work.
     */
    public Object buildBackupClone(Object clone, UnitOfWorkImpl unitOfWork) {
        // The copy policy builds clone	.
        ClassDescriptor descriptor = this.descriptor;
        Object backup = descriptor.getCopyPolicy().buildClone(clone, unitOfWork);

        // PERF: Avoid synchronized enumerator as is concurrency bottleneck.
        List mappings = getCloningMappings();
        int size = mappings.size();
        if (descriptor.hasFetchGroupManager() && descriptor.getFetchGroupManager().isPartialObject(clone)) {
            FetchGroupManager fetchGroupManager = descriptor.getFetchGroupManager();
            for (int index = 0; index < size; index++) {
                DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);
                if (fetchGroupManager.isAttributeFetched(clone, mapping.getAttributeName())) {
                    mapping.buildBackupClone(clone, backup, unitOfWork);
                }
            }
        } else {
            for (int index = 0; index < size; index++) {
                ((DatabaseMapping)mappings.get(index)).buildBackupClone(clone, backup, unitOfWork);
            }
        }

        return backup;
    }

    /**
     * Build and return the expression to use as the where clause to delete an object.
     * The row is passed to allow the version number to be extracted from it.
     */
    public Expression buildDeleteExpression(DatabaseTable table, AbstractRecord row) {
        if (this.descriptor.usesOptimisticLocking() && (this.descriptor.getTables().firstElement().equals(table))) {
            return this.descriptor.getOptimisticLockingPolicy().buildDeleteExpression(table, primaryKeyExpression, row);
        } else {
            return buildPrimaryKeyExpression(table);
        }
    }

    /**
     * INTERNAL:
     * This method is used when Query By Example is used.  Going through the mappings one by one, this method
     * calls the specific buildExpression method corresponding to the type of mapping.  It then generates a
     * complete Expression by joining the individual Expressions.
     */
    public Expression buildExpressionFromExample(Object queryObject, QueryByExamplePolicy policy, Expression expressionBuilder, Map processedObjects, AbstractSession session) {
        if (processedObjects.containsKey(queryObject)) {
            //this object has already been queried on
            return null;
        }

        processedObjects.put(queryObject, queryObject);
        Expression expression = null;

        // PERF: Avoid synchronized enumerator as is concurrency bottleneck.
        List mappings = this.descriptor.getMappings();
        for (int index = 0; index < mappings.size(); index++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);
            if (expression == null) {
                expression = mapping.buildExpression(queryObject, policy, expressionBuilder, processedObjects, session);
            } else {
                expression = expression.and(mapping.buildExpression(queryObject, policy, expressionBuilder, processedObjects, session));
            }
        }

        return expression;
    }

    /**
     * Return a new instance of the receiver's javaClass.
     */
    public Object buildNewInstance() {
        return this.descriptor.getInstantiationPolicy().buildNewInstance();
    }
    
    /**
     * Return an instance of the receivers javaClass. Set the attributes of an instance
     * from the values stored in the database row.
     */
    public Object buildObject(ObjectLevelReadQuery query, AbstractRecord databaseRow) throws DatabaseException, QueryException {
        // PERF: Avoid lazy init of join manager if no joining.
        JoinedAttributeManager joinManager = null;
        if (query.hasJoining()) {
            joinManager = query.getJoinedAttributeManager();
        }
        return buildObject(query, databaseRow, joinManager);
    }

    /**
     * Return an instance of the receivers javaClass. Set the attributes of an instance
     * from the values stored in the database row.
     */
    public Object buildObject(ObjectBuildingQuery query, AbstractRecord databaseRow, JoinedAttributeManager joinManager) throws DatabaseException, QueryException {
        // Profile object building.
        AbstractSession session = query.getSession();
        session.startOperationProfile(SessionProfiler.ObjectBuilding, query, SessionProfiler.ALL);
        Object domainObject = null;
        try {
            Object primaryKey = extractPrimaryKeyFromRow(databaseRow, session);
            // Check for null primary key, this is not allowed.
            if ((primaryKey == null) && (!query.hasPartialAttributeExpressions()) && (!this.descriptor.isAggregateCollectionDescriptor())) {
                //BUG 3168689: EJBQL: "Select Distinct s.customer from SpouseBean s"
                //BUG 3168699: EJBQL: "Select s.customer from SpouseBean s where s.id = '6'"
                //If we return either a single null, or a Collection containing at least 
                //one null, then we want the nulls returned/included if the indicated 
                //property is set in the query. (As opposed to throwing an Exception).
                if (query.shouldBuildNullForNullPk()) {
                    return null;
                } else {
                    throw QueryException.nullPrimaryKeyInBuildingObject(query, databaseRow);
                }
            }
            ClassDescriptor concreteDescriptor = this.descriptor;
            if (concreteDescriptor.hasInheritance() && concreteDescriptor.getInheritancePolicy().shouldReadSubclasses()) {
                Class classValue = concreteDescriptor.getInheritancePolicy().classFromRow(databaseRow, session);
                concreteDescriptor = concreteDescriptor.getInheritancePolicy().getDescriptor(classValue);
                if ((concreteDescriptor == null) && query.hasPartialAttributeExpressions()) {
                    concreteDescriptor = this.descriptor;
                }
                if (concreteDescriptor == null) {
                    throw QueryException.noDescriptorForClassFromInheritancePolicy(query, classValue);
                }
            }
            if (session.isUnitOfWork()) {
                // Do not wrap yet if in UnitOfWork, as there is still much more
                // processing ahead.
                domainObject = buildObjectInUnitOfWork(query, joinManager, databaseRow, (UnitOfWorkImpl)session, primaryKey, concreteDescriptor);
            } else {
                domainObject = buildObject(false, query, databaseRow, session, primaryKey, concreteDescriptor, joinManager);
                if (query.shouldCacheQueryResults()) {
                    query.cacheResult(domainObject);
                }

                // wrap the object if the query requires it.
                if (query.shouldUseWrapperPolicy()) {
                    domainObject = concreteDescriptor.getObjectBuilder().wrapObject(domainObject, session);
                }
            }
        } finally {
            session.endOperationProfile(SessionProfiler.ObjectBuilding, query, SessionProfiler.ALL);
        }
        return domainObject;
    }


    /**
     * Force instantiation to any eager mappings.
     */
    public void instantiateEagerMappings(Object object, AbstractSession session) {
        // Force instantiation to eager mappings.
        if (!this.eagerMappings.isEmpty()) {
            FetchGroup fetchGroup = null;
            FetchGroupManager fetchGroupManager = this.descriptor.getFetchGroupManager();
            if (fetchGroupManager != null) {
                fetchGroup = fetchGroupManager.getObjectFetchGroup(object);
            }
            int size = this.eagerMappings.size();
            for (int index = 0; index < size; index++) {
                DatabaseMapping mapping = this.eagerMappings.get(index);
                if (fetchGroup == null || fetchGroup.containsAttributeInternal(mapping.getAttributeName())) {
                    mapping.instantiateAttribute(object, session);
                }
            }
        }
    }
    
    /**
     * For executing all reads on the UnitOfWork, the session when building
     * objects from rows will now be the UnitOfWork.  Useful if the rows were
     * read via a dirty write connection and we want to avoid putting uncommitted
     * data in the global cache.
     * <p>
     * Decides whether to call either buildWorkingCopyCloneFromRow (bypassing
     * shared cache) or buildWorkingCopyCloneNormally (placing the result in the
     * shared cache).
     */
    protected Object buildObjectInUnitOfWork(ObjectBuildingQuery query, JoinedAttributeManager joinManager, AbstractRecord databaseRow, UnitOfWorkImpl unitOfWork, Object primaryKey, ClassDescriptor concreteDescriptor) throws DatabaseException, QueryException {
        // When in transaction we are reading via the write connection
        // and so do not want to corrupt the shared cache with dirty objects.
        // Hence we build and refresh clones directly from the database row.
        // PERF: Allow the session cached to still be used after early transaction if isolation setting has been set.
        if (!concreteDescriptor.shouldUseSessionCacheInUnitOfWorkEarlyTransaction()) {
            if (((unitOfWork.hasCommitManager() && unitOfWork.getCommitManager().isActive())
                    || unitOfWork.wasTransactionBegunPrematurely()
                    || concreteDescriptor.getCachePolicy().shouldIsolateObjectsInUnitOfWork()
                    || concreteDescriptor.shouldIsolateProtectedObjectsInUnitOfWork()
                    || query.shouldStoreBypassCache())
                        && (!unitOfWork.isClassReadOnly(concreteDescriptor.getJavaClass(), concreteDescriptor))) {
                // It is easier to switch once to the correct builder here.
                return concreteDescriptor.getObjectBuilder().buildWorkingCopyCloneFromRow(query, joinManager, databaseRow, unitOfWork, primaryKey);
            }
        }

        return buildWorkingCopyCloneNormally(query, databaseRow, unitOfWork, primaryKey, concreteDescriptor, joinManager);
    }

    /**
     * buildWorkingCopyCloneFromRow is an alternative to this which is the
     * normal behavior.
     * A row is read from the database, an original is built/refreshed/returned
     * from the shared cache, and the original is registered/conformed/reverted
     * in the UnitOfWork.
     * <p>
     * This default behavior is only safe when the query is executed on a read
     * connection, otherwise uncommitted data might get loaded into the shared
     * cache.
     * <p>
     * Represents the way TopLink has always worked.
     */
    protected Object buildWorkingCopyCloneNormally(ObjectBuildingQuery query, AbstractRecord databaseRow, UnitOfWorkImpl unitOfWork, Object primaryKey, ClassDescriptor concreteDescriptor, JoinedAttributeManager joinManager) throws DatabaseException, QueryException {
        // First check local unit of work cache.
        CacheKey unitOfWorkCacheKey = unitOfWork.getIdentityMapAccessorInstance().acquireLock(primaryKey, concreteDescriptor.getJavaClass(), concreteDescriptor);
        Object clone = unitOfWorkCacheKey.getObject();
        boolean found = clone != null;
        Object original = null;
        try {
            // Only check parent cache if not in unit of work, or if a refresh is required.
            if (!found || query.shouldRefreshIdentityMapResult()
                    || query.shouldCacheQueryResults() // Need to build original to cache it.
                    || query.shouldRetrieveBypassCache()
                    || (concreteDescriptor.hasFetchGroupManager() && concreteDescriptor.getFetchGroupManager().isPartialObject(clone))) {
                // This is normal case when we are not in transaction.
                // Pass the query off to the parent.  Let it build the object and
                // cache it normally, then register/refresh it.
                AbstractSession session = unitOfWork.getParentIdentityMapSession(query);
                
                // forwarding queries to different sessions is now as simple as setting
                // the session on the query.
                query.setSession(session);
                if (session.isUnitOfWork()) {
                    // If a nested unit of work, recurse.
                    original = buildObjectInUnitOfWork(query, joinManager, databaseRow, (UnitOfWorkImpl)session, primaryKey, concreteDescriptor);
                    //GFBug#404  Pass in joinManager or not based on if shouldCascadeCloneToJoinedRelationship is set to true 
                    if (unitOfWork.shouldCascadeCloneToJoinedRelationship()) {
                        return query.registerIndividualResult(original, primaryKey, unitOfWork, joinManager, concreteDescriptor);
                    } else {
                        return query.registerIndividualResult(original, primaryKey, unitOfWork, null, concreteDescriptor);            
                    }
                } else {
                    // PERF: This optimizes the normal case to avoid duplicate cache access.
                    CacheKey parentCacheKey = (CacheKey)buildObject(true, query, databaseRow, session, primaryKey, concreteDescriptor, joinManager);
                    original = parentCacheKey.getObject();
                    if (query.shouldCacheQueryResults()) {
                        query.cacheResult(original);
                    }
                    // PERF: Do not register nor process read-only.
                    if (unitOfWork.isClassReadOnly(original.getClass(), concreteDescriptor)) {
                        // There is an obscure case where they object could be read-only and pessimistic.
                        // Record clone if referenced class has pessimistic locking policy.
                        query.recordCloneForPessimisticLocking(original, unitOfWork);
                        return original;
                    }
                    if (!query.isRegisteringResults()) {
                        return original;
                    }
                    if (clone == null) {
                        clone = unitOfWork.cloneAndRegisterObject(original, parentCacheKey, unitOfWorkCacheKey, concreteDescriptor);
                        // TODO-dclarke: At this point the clones do not have their fetch-group specified
                        // relationship attributes loaded
                    }
                    //bug3659327
                    //fetch group manager control fetch group support
                    if (concreteDescriptor.hasFetchGroupManager()) {
                        //if the object is already registered in uow, but it's partially fetched (fetch group case)     
                        if (concreteDescriptor.getFetchGroupManager().shouldWriteInto(original, clone)) {
                            //there might be cases when reverting/refreshing clone is needed.
                            concreteDescriptor.getFetchGroupManager().writePartialIntoClones(original, clone, unitOfWork);
                        }
                    }
                }
            }
            query.postRegisterIndividualResult(clone, original, primaryKey, unitOfWork, joinManager, concreteDescriptor);
        } finally {
            unitOfWorkCacheKey.release();
            query.setSession(unitOfWork);
        }
        return clone;
    }

    /**
     * Return an instance of the receivers javaClass. Set the attributes of an instance
     * from the values stored in the database row.
     */
    protected Object buildObject(boolean returnCacheKey, ObjectBuildingQuery query, AbstractRecord databaseRow, AbstractSession session, Object primaryKey, ClassDescriptor concreteDescriptor, JoinedAttributeManager joinManager) throws DatabaseException, QueryException {
        boolean isProtected = concreteDescriptor.getCachePolicy().isProtectedIsolation();
        if (isProtected && session.isIsolatedClientSession()){
            return buildProtectedObject(returnCacheKey, query, databaseRow, session, primaryKey, concreteDescriptor, joinManager);
        }
        Object domainObject = null;
        
        // Cache key is used for object locking.
        CacheKey cacheKey = null;
        // Keep track if we actually built/refresh the object.
        boolean cacheHit = true;
        boolean domainWasMissing = true;
        try {
            // Check if the objects exists in the identity map.
            if (query.shouldMaintainCache() && (! query.shouldRetrieveBypassCache() || ! query.shouldStoreBypassCache())) {
                cacheKey = session.retrieveCacheKey(primaryKey, concreteDescriptor, joinManager, query.requiresDeferredLocks());
                domainObject = cacheKey.getObject();
                domainWasMissing = domainObject == null;
            }

            if (domainWasMissing || query.shouldRetrieveBypassCache()) {
                cacheHit = false;
                if (domainObject == null || query.shouldStoreBypassCache()) {
                    if (query.isReadObjectQuery() && ((ReadObjectQuery)query).shouldLoadResultIntoSelectionObject()) {
                        domainObject = ((ReadObjectQuery)query).getSelectionObject();
                    } else {
                        domainObject = concreteDescriptor.getObjectBuilder().buildNewInstance();
                    }
                }

                // The object must be registered before building its attributes to resolve circular dependencies.
                if (query.shouldMaintainCache() && ! query.shouldStoreBypassCache()){
                    if (domainWasMissing) {  // may have build a new domain even though there is one in the cache
                        cacheKey.setObject(domainObject);
                    }
                    copyQueryInfoToCacheKey(cacheKey, query, databaseRow, session, concreteDescriptor);
                } else if (cacheKey == null || (domainWasMissing && query.shouldRetrieveBypassCache())) {
                    cacheKey = new CacheKey(primaryKey);
                    cacheKey.setObject(domainObject);
                }

                concreteDescriptor.getObjectBuilder().buildAttributesIntoObject(domainObject, cacheKey, databaseRow, query, joinManager, false, session);
                if (isProtected && (cacheKey != null)) {
                    cacheForeignKeyValues(databaseRow, cacheKey, session);
                }
                if (query.shouldMaintainCache() && ! query.shouldStoreBypassCache()) {
                    // Set the fetch group to the domain object, after built.
                    if ((query.getEntityFetchGroup() != null) && concreteDescriptor.hasFetchGroupManager()) {
                        query.getEntityFetchGroup().setOnEntity(domainObject, session);
                    }
                }
                // PERF: Cache the primary key and cache key if implements PersistenceEntity.
                if (domainObject instanceof PersistenceEntity) {
                    updateCachedAttributes((PersistenceEntity) domainObject, cacheKey, primaryKey);
                }
            } else {
                if (query.isReadObjectQuery() && ((ReadObjectQuery)query).shouldLoadResultIntoSelectionObject()) {
                    copyInto(domainObject, ((ReadObjectQuery)query).getSelectionObject());
                    domainObject = ((ReadObjectQuery)query).getSelectionObject();
                }

                //check if the cached object has been invalidated
                boolean isInvalidated = concreteDescriptor.getCacheInvalidationPolicy().isInvalidated(cacheKey, query.getExecutionTime());

                //CR #4365 - Queryid comparison used to prevent infinite recursion on refresh object cascade all
                //if the concurrency manager is locked by the merge process then no refresh is required.
                // bug # 3388383 If this thread does not have the active lock then someone is building the object so in order to maintain data integrity this thread will not
                // fight to overwrite the object ( this also will avoid potential deadlock situations
                if ((cacheKey.getMutex().getActiveThread() == Thread.currentThread()) && ((query.shouldRefreshIdentityMapResult() || concreteDescriptor.shouldAlwaysRefreshCache() || isInvalidated ) && ((cacheKey.getLastUpdatedQueryId() != query.getQueryId()) && !cacheKey.getMutex().isLockedByMergeManager()))) {
                    cacheHit = refreshObjectIfRequired(concreteDescriptor, cacheKey, cacheKey.getObject(), query, joinManager, databaseRow, session, false);
                } else if (concreteDescriptor.hasFetchGroupManager() && (concreteDescriptor.getFetchGroupManager().isPartialObject(domainObject) && (!concreteDescriptor.getFetchGroupManager().isObjectValidForFetchGroup(domainObject, query.getEntityFetchGroup())))) {
                    cacheHit = false;
                    // The fetched object is not sufficient for the fetch group of the query 
                    // refresh attributes of the query's fetch group.
                    concreteDescriptor.getFetchGroupManager().unionEntityFetchGroupIntoObject(domainObject, query.getEntityFetchGroup(), session);
                    concreteDescriptor.getObjectBuilder().buildAttributesIntoObject(domainObject, cacheKey, databaseRow, query, joinManager, false, session);
                    if (cacheKey != null){
                        cacheForeignKeyValues(databaseRow, cacheKey, session);
                    }
                }
                // 3655915: a query with join/batch'ing that gets a cache hit
                // may require some attributes' valueholders to be re-built.
                else if (joinManager != null && joinManager.hasJoinedAttributeExpressions()) { //some queries like ObjRel do not support joining
                    loadJoinedAttributes(concreteDescriptor, domainObject, cacheKey, databaseRow, joinManager, query, false);
                } else if (query.isReadAllQuery() && ((ReadAllQuery)query).hasBatchReadAttributes()) {
                    loadBatchReadAttributes(concreteDescriptor, domainObject, cacheKey, databaseRow, query, joinManager, false);
                }
            }
        } finally {
            if (query.shouldMaintainCache() && (cacheKey != null)) {
                // bug 2681401:
                // in case of exception (for instance, thrown by buildNewInstance())
                // cacheKey.getObject() may be null.
                if (cacheKey.getObject() != null) {
                    cacheKey.updateAccess();
                }

                // PERF: Only use deferred locking if required.
                if (query.requiresDeferredLocks()) {
                    cacheKey.releaseDeferredLock();
                } else {
                    cacheKey.release();
                }
            }
        }
        if (!cacheHit) {
            concreteDescriptor.getObjectBuilder().instantiateEagerMappings(domainObject, session);
            if (query.shouldMaintainCache() && (cacheKey != null)) {
                concreteDescriptor.getCachePolicy().indexObjectInCache(cacheKey, databaseRow, domainObject, concreteDescriptor, session, !domainWasMissing);
            }
        }
        
        if (returnCacheKey) {
            return cacheKey;
        } else {
            return domainObject;
        }
    }

    /**
     * Return an instance of the receivers javaClass. Set the attributes of an instance
     * from the values stored in the database row.
     */
    protected Object buildProtectedObject(boolean returnCacheKey, ObjectBuildingQuery query, AbstractRecord databaseRow, AbstractSession session, Object primaryKey, ClassDescriptor concreteDescriptor, JoinedAttributeManager joinManager) throws DatabaseException, QueryException {
        Object cachedObject = null;
        Object protectedObject = null;
        
        // Cache key is used for object locking.
        CacheKey cacheKey = null;
        CacheKey sharedCacheKey = null;
        
        // Keep track if we actually built/refresh the object.
        boolean cacheHit = true;
        try {
            // Check if the objects exists in the identity map.
            if (query.shouldMaintainCache() &&  (!query.shouldRetrieveBypassCache() || !query.shouldStoreBypassCache())) {
                cacheKey = session.retrieveCacheKey(primaryKey, concreteDescriptor, joinManager, query.requiresDeferredLocks());
                protectedObject = cacheKey.getObject();
            }
            
            if (protectedObject == null || query.shouldRetrieveBypassCache()) {
                cacheHit = false;
                boolean domainWasMissing = protectedObject == null;
                if (protectedObject == null || query.shouldStoreBypassCache()){
                    if (query.isReadObjectQuery() && ((ReadObjectQuery)query).shouldLoadResultIntoSelectionObject()) {
                        protectedObject = ((ReadObjectQuery)query).getSelectionObject();
                    } else {
                        protectedObject = concreteDescriptor.getObjectBuilder().buildNewInstance();
                    }
                }
                // The object must be registered before building its attributes to resolve circular dependencies.
                // The object must be registered before building its attributes to resolve circular dependencies.
                if (query.shouldMaintainCache() && ! query.shouldStoreBypassCache()){
                    if (domainWasMissing) {  // may have build a new domain even though there is one in the cache
                        cacheKey.setObject(protectedObject);
                    }
                    copyQueryInfoToCacheKey(cacheKey, query, databaseRow, session, concreteDescriptor);
                }else if (cacheKey == null || (domainWasMissing && query.shouldRetrieveBypassCache())){
                    cacheKey = new CacheKey(primaryKey);
                    cacheKey.setObject(protectedObject);
                }

                
                // The object must be registered before building its attributes to resolve circular dependencies.
                if (query.shouldMaintainCache() && ! query.shouldStoreBypassCache()) {
                    sharedCacheKey = session.getParent().retrieveCacheKey(primaryKey, concreteDescriptor, joinManager, query.requiresDeferredLocks());
                    if (sharedCacheKey.getObject() == null){
                        sharedCacheKey = (CacheKey) buildObject(true, query, databaseRow, session.getParent(), primaryKey, concreteDescriptor, joinManager);
                        cachedObject = sharedCacheKey.getObject();
                    }
                }
                concreteDescriptor.getObjectBuilder().buildAttributesIntoObject(protectedObject, sharedCacheKey, databaseRow, query, joinManager, false, session);
                
                //if !protected the returned object and the domain object are the same.
                if (query.shouldMaintainCache() && ! query.shouldStoreBypassCache()) {
                    // Set the fetch group to the domain object, after built.
                    if ((query.getEntityFetchGroup() != null) && concreteDescriptor.hasFetchGroupManager()) {
                        query.getEntityFetchGroup().setOnEntity(protectedObject, session);
                    }
                }
                // PERF: Cache the primary key and cache key if implements PersistenceEntity.
                if (protectedObject instanceof PersistenceEntity) {
                    ((PersistenceEntity)protectedObject)._persistence_setId(primaryKey);
                }
            } else {                
                if (query.isReadObjectQuery() && ((ReadObjectQuery)query).shouldLoadResultIntoSelectionObject()) {
                    copyInto(protectedObject, ((ReadObjectQuery)query).getSelectionObject());
                    protectedObject = ((ReadObjectQuery)query).getSelectionObject();
                }
                sharedCacheKey = session.getParent().retrieveCacheKey(primaryKey, concreteDescriptor, joinManager, query.requiresDeferredLocks());
                cachedObject = sharedCacheKey.getObject();
                if (cachedObject == null){
                    sharedCacheKey = (CacheKey) buildObject(true, query, databaseRow, session.getParent(), primaryKey, concreteDescriptor, joinManager);
                    cachedObject = sharedCacheKey.getObject();
                }

                //check if the cached object has been invalidated
                boolean isInvalidated = concreteDescriptor.getCacheInvalidationPolicy().isInvalidated(sharedCacheKey, query.getExecutionTime());

                //CR #4365 - Queryid comparison used to prevent infinite recursion on refresh object cascade all
                //if the concurrency manager is locked by the merge process then no refresh is required.
                // bug # 3388383 If this thread does not have the active lock then someone is building the object so in order to maintain data integrity this thread will not
                // fight to overwrite the object ( this also will avoid potential deadlock situations
                if ((sharedCacheKey.getMutex().getActiveThread() == Thread.currentThread()) && ((query.shouldRefreshIdentityMapResult() || concreteDescriptor.shouldAlwaysRefreshCache() || isInvalidated) && ((sharedCacheKey.getLastUpdatedQueryId() != query.getQueryId()) && !sharedCacheKey.getMutex().isLockedByMergeManager()))) {
                    
                    //need to refresh. shared cache instance
                    cacheHit = refreshObjectIfRequired(concreteDescriptor, sharedCacheKey, cachedObject, query, joinManager, databaseRow, session.getParent(), true);
                    //shared cache was refreshed and a refresh has been requested so lets refresh the protected object as well
                    refreshObjectIfRequired(concreteDescriptor, sharedCacheKey, protectedObject, query, joinManager, databaseRow, session, true);
                } else if (concreteDescriptor.hasFetchGroupManager() && (concreteDescriptor.getFetchGroupManager().isPartialObject(protectedObject) && (!concreteDescriptor.getFetchGroupManager().isObjectValidForFetchGroup(protectedObject, query.getEntityFetchGroup())))) {
                    cacheHit = false;
                    // The fetched object is not sufficient for the fetch group of the query 
                    // refresh attributes of the query's fetch group.
                    concreteDescriptor.getFetchGroupManager().unionEntityFetchGroupIntoObject(protectedObject, query.getEntityFetchGroup(), session);
                    concreteDescriptor.getObjectBuilder().buildAttributesIntoObject(protectedObject, sharedCacheKey, databaseRow, query, joinManager, false, session);
                }
                // 3655915: a query with join/batch'ing that gets a cache hit
                // may require some attributes' valueholders to be re-built.
                else if (joinManager != null && joinManager.hasJoinedAttributeExpressions()) { //some queries like ObjRel do not support joining
                    loadJoinedAttributes(concreteDescriptor, cachedObject, sharedCacheKey, databaseRow, joinManager, query, false);
                    loadJoinedAttributes(concreteDescriptor, protectedObject, sharedCacheKey, databaseRow, joinManager, query, true);
                } else if (query.isReadAllQuery() && ((ReadAllQuery)query).hasBatchReadAttributes()) {
                    loadBatchReadAttributes(concreteDescriptor, cachedObject, sharedCacheKey, databaseRow, query, joinManager, false);
                    loadBatchReadAttributes(concreteDescriptor, protectedObject, sharedCacheKey, databaseRow, query, joinManager, true);
                }
            }
        } finally {
            if (query.shouldMaintainCache()){
                if (cacheKey != null) {
                    // bug 2681401:
                    // in case of exception (for instance, thrown by buildNewInstance())
                    // cacheKey.getObject() may be null.
                    if (cacheKey.getObject() != null) {
                        cacheKey.updateAccess();
                    }

                    // PERF: Only use deferred locking if required.
                    if (query.requiresDeferredLocks()) {
                        cacheKey.releaseDeferredLock();
                    } else {
                        cacheKey.release();
                    }
                }
                if (sharedCacheKey != null) {
                    // bug 2681401:
                    // in case of exception (for instance, thrown by buildNewInstance())
                    // sharedCacheKey() may be null.
                    if (sharedCacheKey.getObject() != null) {
                        sharedCacheKey.updateAccess();
                    }

                    // PERF: Only use deferred locking if required.
                    if (query.requiresDeferredLocks()) {
                        sharedCacheKey.releaseDeferredLock();
                    } else {
                        sharedCacheKey.release();
                    }
                }
            }
        }
        if (!cacheHit) {
            concreteDescriptor.getObjectBuilder().instantiateEagerMappings(protectedObject, session);
        }
        
        if (returnCacheKey) {
            return cacheKey;
        } else {
            return protectedObject;
        }
    }

    /**
     * Clean up the cached object data and only revert the fetch group data back to the cached object.
     */
    private void revertFetchGroupData(Object domainObject, ClassDescriptor concreteDescriptor, CacheKey cacheKey, ObjectBuildingQuery query, JoinedAttributeManager joinManager, AbstractRecord databaseRow, AbstractSession session, boolean targetIsProtected) {
        //the cached object is either invalidated, or staled as the version is newer, or a refresh is explicitly set on the query.
        //clean all data of the cache object.
        concreteDescriptor.getFetchGroupManager().reset(domainObject);
        //set fetch group reference to the cached object
        concreteDescriptor.getFetchGroupManager().setObjectFetchGroup(domainObject, query.getEntityFetchGroup(), session);
        // Bug 276362 - set the CacheKey's read time (to re-validate the CacheKey) before buildAttributesIntoObject is called
        cacheKey.setReadTime(query.getExecutionTime());
        //read in the fetch group data only
        concreteDescriptor.getObjectBuilder().buildAttributesIntoObject(domainObject, cacheKey, databaseRow, query, joinManager, false, session);
        //set refresh on fetch group
        concreteDescriptor.getFetchGroupManager().setRefreshOnFetchGroupToObject(domainObject, (query.shouldRefreshIdentityMapResult() || concreteDescriptor.shouldAlwaysRefreshCache()));
        //set query id to prevent infinite recursion on refresh object cascade all
        cacheKey.setLastUpdatedQueryId(query.getQueryId());
        //register the object into the IM and set the write lock object if applied.
        if (concreteDescriptor.usesOptimisticLocking()) {
            OptimisticLockingPolicy policy = concreteDescriptor.getOptimisticLockingPolicy();
            cacheKey.setWriteLockValue(policy.getValueToPutInCache(databaseRow, session));
        }
    }

    /**
     * Return a container which contains the instances of the receivers javaClass.
     * Set the fields of the instance to the values stored in the database rows.
     */
    public Object buildObjectsInto(ReadAllQuery query, List databaseRows, Object domainObjects) throws DatabaseException {
        int size = databaseRows.size();
        if (size > 0) {
            AbstractSession session = query.getSession();
            // PERF: Avoid lazy init of join manager if no joining.
            JoinedAttributeManager joinManager = null;
            if (query.hasJoining()) {
                joinManager = query.getJoinedAttributeManager();
            }
            ContainerPolicy policy = query.getContainerPolicy();
            if (policy.shouldAddAll()) {
                List domainObjectsIn = new ArrayList(size);
                List<AbstractRecord> databaseRowsIn = new ArrayList(size);
                for (int index = 0; index < size; index++) {
                    AbstractRecord databaseRow = (AbstractRecord)databaseRows.get(index);
                    // PERF: 1-m joining nulls out duplicate rows.
                    if (databaseRow != null) {
                        domainObjectsIn.add(buildObject(query, databaseRow, joinManager));
                        databaseRowsIn.add(databaseRow);
                    }
                }
                policy.addAll(domainObjectsIn, domainObjects, session, databaseRowsIn, query, (CacheKey)null, true);
            } else {
                boolean quickAdd = (domainObjects instanceof Collection) && !this.hasWrapperPolicy;
                for (int index = 0; index < size; index++) {
                    AbstractRecord databaseRow = (AbstractRecord)databaseRows.get(index);
                    // PERF: 1-m joining nulls out duplicate rows.
                    if (databaseRow != null) {
                        Object domainObject = buildObject(query, databaseRow, joinManager);
                        if (quickAdd) {
                            ((Collection)domainObjects).add(domainObject);
                        } else {
                            policy.addInto(domainObject, domainObjects, session, databaseRow, query, (CacheKey)null, true);
                        }
                    }
                }
            }
        }
        return domainObjects;
    }

    /**
     * Build the primary key expression for the secondary table.
     */
    public Expression buildPrimaryKeyExpression(DatabaseTable table) throws DescriptorException {
        if (this.descriptor.getTables().firstElement().equals(table)) {
            return getPrimaryKeyExpression();
        }

        Map keyMapping = this.descriptor.getAdditionalTablePrimaryKeyFields().get(table);
        if (keyMapping == null) {
            throw DescriptorException.multipleTablePrimaryKeyNotSpecified(this.descriptor);
        }

        ExpressionBuilder builder = new ExpressionBuilder();
        Expression expression = null;
        for (Iterator primaryKeyEnum = keyMapping.values().iterator(); primaryKeyEnum.hasNext();) {
            DatabaseField field = (DatabaseField)primaryKeyEnum.next();
            expression = (builder.getField(field).equal(builder.getParameter(field))).and(expression);
        }

        return expression;
    }

    /**
     * Build the primary key expression from the specified primary key values.
     */
    public Expression buildPrimaryKeyExpressionFromKeys(Object primaryKey, AbstractSession session) {
        Expression builder = new ExpressionBuilder();
        List<DatabaseField> primaryKeyFields = this.descriptor.getPrimaryKeyFields();

        if (this.descriptor.getCachePolicy().getCacheKeyType() == CacheKeyType.ID_VALUE) {
            return builder.getField(primaryKeyFields.get(0)).equal(primaryKey);
        }
        Expression expression = null;
        int size = primaryKeyFields.size();
        Object[] primaryKeyValues = null;
        if (primaryKey == null) {
            primaryKeyValues = new Object[size];
        } else {
            primaryKeyValues = ((CacheId)primaryKey).getPrimaryKey();
        }
        for (int index = 0; index < size; index++) {
            Object value = primaryKeyValues[index];
            DatabaseField field = primaryKeyFields.get(index);
            if (value != null) {
                Expression subExpression = builder.getField(field).equal(value);
                expression = subExpression.and(expression);
            }
        }

        return expression;
    }

    /**
     * Build the primary key expression from the specified domain object.
     */
    public Expression buildPrimaryKeyExpressionFromObject(Object domainObject, AbstractSession session) {
        return buildPrimaryKeyExpressionFromKeys(extractPrimaryKeyFromObject(domainObject, session), session);
    }

    /**
     * Build the row representation of an object.
     */
    public AbstractRecord buildRow(Object object, AbstractSession session, WriteType writeType) {
        return buildRow(createRecord(session), object, session, writeType);
    }

    /**
     * Build the row representation of an object.
     */
    public AbstractRecord buildRow(AbstractRecord databaseRow, Object object, AbstractSession session, WriteType writeType) {
        // PERF: Avoid synchronized enumerator as is concurrency bottleneck.
        List mappings = this.descriptor.getMappings();
        int mappingsSize = mappings.size();
        for (int index = 0; index < mappingsSize; index++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);
            mapping.writeFromObjectIntoRow(object, databaseRow, session, writeType);
        }

        // If this descriptor is involved in inheritance add the class type.
        if (this.descriptor.hasInheritance()) {
            this.descriptor.getInheritancePolicy().addClassIndicatorFieldToRow(databaseRow);
        }

        // If this descriptor has multiple tables then we need to append the primary keys for 
        // the non default tables.
        if (this.descriptor.hasMultipleTables() && !this.descriptor.isAggregateDescriptor()) {
            addPrimaryKeyForNonDefaultTable(databaseRow, object, session);
        }

        // If the session uses multi-tenancy, add the tenant id field.
        if (getDescriptor().hasMultitenantPolicy()) {
            getDescriptor().getMultitenantPolicy().addFieldsToRow(databaseRow, session);            
        }
        
        return databaseRow;
    }
    /**
     * Build the row representation of the object for update. The row built does not
     * contain entries for uninstantiated attributes.
     */
    public AbstractRecord buildRowForShallowInsert(Object object, AbstractSession session) {
        return buildRowForShallowInsert(createRecord(session), object, session);
    }

    /**
     * Build the row representation of the object for update. The row built does not
     * contain entries for uninstantiated attributes.
     */
    public AbstractRecord buildRowForShallowInsert(AbstractRecord databaseRow, Object object, AbstractSession session) {
        // PERF: Avoid synchronized enumerator as is concurrency bottleneck.
        List mappings = this.descriptor.getMappings();
        int mappingsSize = mappings.size();
        for (int index = 0; index < mappingsSize; index++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);
            mapping.writeFromObjectIntoRowForShallowInsert(object, databaseRow, session);
        }

        // If this descriptor is involved in inheritance add the class type.
        if (this.descriptor.hasInheritance()) {
            this.descriptor.getInheritancePolicy().addClassIndicatorFieldToRow(databaseRow);
        }

        // If this descriptor has multiple tables then we need to append the primary keys for 
        // the non default tables.
        if (!this.descriptor.isAggregateDescriptor()) {
            addPrimaryKeyForNonDefaultTable(databaseRow, object, session);
        }

        // If the session uses multi-tenancy, add the tenant id field.
        if (getDescriptor().hasMultitenantPolicy()) {
            getDescriptor().getMultitenantPolicy().addFieldsToRow(databaseRow, session);            
        }
        
        return databaseRow;
    }


    /**
     * Build the row representation of an object.
     * This is only used for aggregates.
     */
    public AbstractRecord buildRowWithChangeSet(AbstractRecord databaseRow, ObjectChangeSet objectChangeSet, AbstractSession session, WriteType writeType) {
        List<ChangeRecord> changes = (List)objectChangeSet.getChanges();
        int size = changes.size();
        for (int index = 0; index < size; index++) {
            ChangeRecord changeRecord = changes.get(index);
            DatabaseMapping mapping = changeRecord.getMapping();
            mapping.writeFromObjectIntoRowWithChangeRecord(changeRecord, databaseRow, session, writeType);
        }

        // If this descriptor is involved in inheritance add the class type.
        if (this.descriptor.hasInheritance()) {
            this.descriptor.getInheritancePolicy().addClassIndicatorFieldToRow(databaseRow);
        }

        // If the session uses multi-tenancy, add the tenant id field.
        if (getDescriptor().hasMultitenantPolicy()) {
            getDescriptor().getMultitenantPolicy().addFieldsToRow(databaseRow, session);            
        }
        
        return databaseRow;
    }

    /**
     * Build the row representation of an object. The row built is used only for translations
     * for the expressions in the expression framework.
     */
    public AbstractRecord buildRowForTranslation(Object object, AbstractSession session) {
        AbstractRecord databaseRow = createRecord(session);
        List<DatabaseMapping> primaryKeyMappings = getPrimaryKeyMappings();
        int size = primaryKeyMappings.size();
        for (int index = 0; index < size; index++) {
            DatabaseMapping mapping = primaryKeyMappings.get(index);
            if (mapping != null) {
                mapping.writeFromObjectIntoRow(object, databaseRow, session, WriteType.UNDEFINED);
            }
        }

        // If this descriptor has multiple tables then we need to append the primary keys for 
        // the non default tables, this is require for m-m, dc defined in the Builder that prefixes the wrong table name.
        // Ideally the mappings should take part in building the translation row so they can add required values.
        if (this.descriptor.hasMultipleTables()) {
            addPrimaryKeyForNonDefaultTable(databaseRow, object, session);
        }

        return databaseRow;
    }

    /**
     * Build the row representation of the object for update. The row built does not
     * contain entries for unchanged attributes.
     */
    public AbstractRecord buildRowForUpdate(WriteObjectQuery query) {
        AbstractRecord databaseRow = createRecord(query.getSession());
        return buildRowForUpdate(databaseRow, query);
    }
    
    /**
     * Build into the row representation of the object for update. The row does not
     * contain entries for unchanged attributes.
     */
    public AbstractRecord buildRowForUpdate(AbstractRecord databaseRow, WriteObjectQuery query) {
        for (Iterator mappings = getNonPrimaryKeyMappings().iterator(); mappings.hasNext();) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.next();
            mapping.writeFromObjectIntoRowForUpdate(query, databaseRow);
        }

        // If this descriptor is involved in inheritance and is an Aggregate, add the class type.
        // Added Nov 8, 2000 Mostly by PWK but also JED
        // Prs 24801
        // Modified  Dec 11, 2000 TGW with assistance from PWK
        // Prs 27554
        if (this.descriptor.hasInheritance() && this.descriptor.isAggregateDescriptor()) {
            if (query.getObject() != null) {
                if (query.getBackupClone() == null) {
                    this.descriptor.getInheritancePolicy().addClassIndicatorFieldToRow(databaseRow);
                } else {
                    if (!query.getObject().getClass().equals(query.getBackupClone().getClass())) {
                        this.descriptor.getInheritancePolicy().addClassIndicatorFieldToRow(databaseRow);
                    }
                }
            }
        }

        // If the session uses multi-tenancy, add the tenant id field.
        if (getDescriptor().hasMultitenantPolicy()) {
            getDescriptor().getMultitenantPolicy().addFieldsToRow(databaseRow, query.getExecutionSession());            
        }
        
        return databaseRow;
    }

    /**
     * Build the row representation of the object for update. The row built does not
     * contain entries for uninstantiated attributes.
     */
    public AbstractRecord buildRowForUpdateWithChangeSet(WriteObjectQuery query) {
        AbstractRecord databaseRow = createRecord(query.getSession());
        AbstractSession session = query.getSession();
        List changes = query.getObjectChangeSet().getChanges();
        int size = changes.size();
        for (int index = 0; index < size; index++) {
            ChangeRecord changeRecord = (ChangeRecord)changes.get(index);
            DatabaseMapping mapping = changeRecord.getMapping();
            mapping.writeFromObjectIntoRowWithChangeRecord(changeRecord, databaseRow, session, WriteType.UPDATE); 
        }

        return databaseRow;
    }

    /**
     * Build the row representation of an object.
     */
    public AbstractRecord buildRowForWhereClause(ObjectLevelModifyQuery query) {
        AbstractRecord databaseRow = createRecord(query.getSession());
        
        // EL bug 319759
        if (query.isUpdateObjectQuery()) {
            query.setShouldValidateUpdateCallCacheUse(true);
        }

        for (Iterator mappings = this.descriptor.getMappings().iterator();
                 mappings.hasNext();) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.next();
            mapping.writeFromObjectIntoRowForWhereClause(query, databaseRow);
        }

        // If this descriptor has multiple tables then we need to append the primary keys for 
        // the non default tables.
        if (!this.descriptor.isAggregateDescriptor()) {
            addPrimaryKeyForNonDefaultTable(databaseRow);
        }

        return databaseRow;
    }

    /**
     * Build the row from the primary key values.
     */
    public AbstractRecord writeIntoRowFromPrimaryKeyValues(AbstractRecord row, Object primaryKey, AbstractSession session, boolean convert) {
        List<DatabaseField> primaryKeyFields = this.descriptor.getPrimaryKeyFields();
        if (this.descriptor.getCachePolicy().getCacheKeyType() == CacheKeyType.ID_VALUE) {
            DatabaseField field = primaryKeyFields.get(0);
            Object value = primaryKey;
            value = session.getPlatform(this.descriptor.getJavaClass()).getConversionManager().convertObject(value, field.getType());
            row.put(field, value);
            return row;
        }
        int size = primaryKeyFields.size();
        Object[] primaryKeyValues = ((CacheId)primaryKey).getPrimaryKey();
        for (int index = 0; index < size; index++) {
            DatabaseField field = primaryKeyFields.get(index);
            Object value = primaryKeyValues[index];
            value = session.getPlatform(this.descriptor.getJavaClass()).getConversionManager().convertObject(value, field.getType());
            row.put(field, value);
        }

        return row;
    }

    /**
     * Build the row from the primary key values.
     */
    public AbstractRecord buildRowFromPrimaryKeyValues(Object key, AbstractSession session) {
        AbstractRecord databaseRow = createRecord(this.descriptor.getPrimaryKeyFields().size(), session);
        return writeIntoRowFromPrimaryKeyValues(databaseRow, key, session, true);
    }

    /**
     * Build the row of all of the fields used for insertion.
     */
    public AbstractRecord buildTemplateInsertRow(AbstractSession session) {
        AbstractRecord databaseRow = createRecord(session);
        buildTemplateInsertRow(session, databaseRow);
        return databaseRow;
    }

    public void buildTemplateInsertRow(AbstractSession session, AbstractRecord databaseRow) {
        for (Iterator mappings = this.descriptor.getMappings().iterator();
                 mappings.hasNext();) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.next();
            mapping.writeInsertFieldsIntoRow(databaseRow, session);
        }

        // If this descriptor is involved in inheritance add the class type.
        if (this.descriptor.hasInheritance()) {
            this.descriptor.getInheritancePolicy().addClassIndicatorFieldToInsertRow(databaseRow);
        }

        // If this descriptor has multiple tables then we need to append the primary keys for 
        // the non default tables.
        if (!this.descriptor.isAggregateDescriptor()) {
            addPrimaryKeyForNonDefaultTable(databaseRow);
        }

        if (this.descriptor.usesOptimisticLocking()) {
            this.descriptor.getOptimisticLockingPolicy().addLockFieldsToUpdateRow(databaseRow, session);
        }

        // If the session uses multi-tenancy, add the tenant id field.
        if (getDescriptor().hasMultitenantPolicy()) {
            getDescriptor().getMultitenantPolicy().addFieldsToRow(databaseRow, session);            
        }
        
        // remove any fields from the databaseRow
        trimFieldsForInsert(session, databaseRow);
    }
        
    /**
     * INTERNAL
     * Remove a potential sequence number field and invoke the ReturningPolicy trimModifyRowsForInsert method
     */
    public void trimFieldsForInsert(AbstractSession session, AbstractRecord databaseRow) {
        ClassDescriptor descriptor = this.descriptor;
        if (descriptor.usesSequenceNumbers() && descriptor.getSequence().shouldAcquireValueAfterInsert()) {
            databaseRow.remove(descriptor.getSequenceNumberField());
        }
        if (descriptor.hasReturningPolicy()) {
            descriptor.getReturningPolicy().trimModifyRowForInsert(databaseRow);
        }
    }

    /**
     * Build the row representation of the object for update. The row built does not
     * contain entries for uninstantiated attributes.
     */
    public AbstractRecord buildTemplateUpdateRow(AbstractSession session) {
        AbstractRecord databaseRow = createRecord(session);

        for (Iterator mappings = getNonPrimaryKeyMappings().iterator();
                 mappings.hasNext();) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.next();
            mapping.writeUpdateFieldsIntoRow(databaseRow, session);
        }

        if (this.descriptor.usesOptimisticLocking()) {
            this.descriptor.getOptimisticLockingPolicy().addLockFieldsToUpdateRow(databaseRow, session);
        }

        return databaseRow;
    }

    /**
     * Build and return the expression to use as the where clause to an update object.
     * The row is passed to allow the version number to be extracted from it.
     */
    public Expression buildUpdateExpression(DatabaseTable table, AbstractRecord transactionRow, AbstractRecord modifyRow) {
        // Only the first table must use the lock check.
        Expression primaryKeyExpression = buildPrimaryKeyExpression(table);
        if (this.descriptor.usesOptimisticLocking()) {
            return this.descriptor.getOptimisticLockingPolicy().buildUpdateExpression(table, primaryKeyExpression, transactionRow, modifyRow);
        } else {
            return primaryKeyExpression;
        }
    }

    /**
     * INTERNAL:
     * Build just the primary key mappings into the object.
     */
    public void buildPrimaryKeyAttributesIntoObject(Object original, AbstractRecord databaseRow, ObjectBuildingQuery query, AbstractSession session) throws DatabaseException, QueryException {
        // PERF: Avoid synchronized enumerator as is concurrency bottleneck.
        List mappings = this.primaryKeyMappings;
        int mappingsSize = mappings.size();
        for (int i = 0; i < mappingsSize; i++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(i);
            mapping.buildShallowOriginalFromRow(databaseRow, original, null, query, session);
        }
    }

    /**
     * INTERNAL:
     * For reading through the write connection when in transaction,
     * We need a partially populated original, so that we
     * can build a clone using the copy policy, even though we can't
     * put this original in the shared cache yet; just build a
     * shallow original (i.e. just enough to copy over the primary
     * key and some direct attributes) and keep it on the UOW.
     */
    public void buildAttributesIntoShallowObject(Object original, AbstractRecord databaseRow, ObjectBuildingQuery query) throws DatabaseException, QueryException {
        AbstractSession executionSession = query.getSession().getExecutionSession(query);

        // PERF: Avoid synchronized enumerator as is concurrency bottleneck.
        List pkMappings = getPrimaryKeyMappings();
        int mappingsSize = pkMappings.size();
        for (int i = 0; i < mappingsSize; i++) {
            DatabaseMapping mapping = (DatabaseMapping)pkMappings.get(i);

            //if (query.shouldReadMapping(mapping)) {
            if (!mapping.isAbstractColumnMapping()) {
                mapping.buildShallowOriginalFromRow(databaseRow, original, null, query, executionSession);
            }
        }
        List mappings = this.descriptor.getMappings();
        mappingsSize = mappings.size();
        for (int i = 0; i < mappingsSize; i++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(i);

            //if (query.shouldReadMapping(mapping)) {
            if (mapping.isAbstractColumnMapping()) {
                mapping.buildShallowOriginalFromRow(databaseRow, original, null, query, executionSession);
            }
        }
    }

    /**
     * INTERNAL:
     * For reading through the write connection when in transaction,
     * populate the clone directly from the database row.
     */
    public void buildAttributesIntoWorkingCopyClone(Object clone, CacheKey sharedCacheKey, ObjectBuildingQuery query, JoinedAttributeManager joinManager, AbstractRecord databaseRow, UnitOfWorkImpl unitOfWork, boolean forRefresh) throws DatabaseException, QueryException {
        // PERF: Cache if all mappings should be read.
        boolean readAllMappings = query.shouldReadAllMappings();
        List mappings = this.descriptor.getMappings();
        int size = mappings.size();
        for (int index = 0; index < size; index++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);
            if (readAllMappings || query.shouldReadMapping(mapping)) {
                mapping.buildCloneFromRow(databaseRow, joinManager, clone, sharedCacheKey, query, unitOfWork, unitOfWork);
            }
        }

        // PERF: Avoid events if no listeners.
        if (this.descriptor.getEventManager().hasAnyEventListeners()) {
            // Need to run post build or refresh selector, currently check with the query for this,
            // I'm not sure which should be called it case of refresh building a new object, currently refresh is used...
            DescriptorEvent event = new DescriptorEvent(clone);
            event.setQuery(query);
            event.setSession(unitOfWork);
            event.setDescriptor(this.descriptor);
            event.setRecord(databaseRow);
            if (forRefresh) {
                event.setEventCode(DescriptorEventManager.PostRefreshEvent);
            } else {
                event.setEventCode(DescriptorEventManager.PostBuildEvent);
                //fire a postBuildEvent then the postCloneEvent
                unitOfWork.deferEvent(event);
               
                event = new DescriptorEvent(clone);
                event.setQuery(query);
                event.setSession(unitOfWork);
                event.setDescriptor(this.descriptor);
                event.setRecord(databaseRow);
                //bug 259404: ensure postClone is called for objects built directly into the UnitOfWork
                //in this case, the original is the clone
                event.setOriginalObject(clone);
                event.setEventCode(DescriptorEventManager.PostCloneEvent);
            }
            unitOfWork.deferEvent(event);
            
            
        }
    }

    /**
     * INTERNAL:
     * Builds a working copy clone directly from the database row.
     * This is the key method that allows us to execute queries against a
     * UnitOfWork while in transaction and not cache the results in the shared
     * cache.  This is because we might violate transaction isolation by
     * putting uncommitted versions of objects in the shared cache.
     */
    protected Object buildWorkingCopyCloneFromRow(ObjectBuildingQuery query, JoinedAttributeManager joinManager, AbstractRecord databaseRow, UnitOfWorkImpl unitOfWork, Object primaryKey) throws DatabaseException, QueryException {
        ClassDescriptor descriptor = this.descriptor;

        // If the clone already exists then it may only need to be refreshed or returned.
        // We call directly on the identity map to avoid going to the parent,
        // registering if found, and wrapping the result.
        // Acquire or create the cache key as is need once the object is build anyway.
        CacheKey unitOfWorkCacheKey = unitOfWork.getIdentityMapAccessorInstance().getIdentityMapManager().acquireLock(primaryKey, descriptor.getJavaClass(), false, descriptor);
        Object workingClone = unitOfWorkCacheKey.getObject();
        try {
            // If there is a clone, and it is not a refresh then just return it.
            boolean wasAClone = workingClone != null;
            boolean isARefresh = query.shouldRefreshIdentityMapResult() || (query.isLockQuery() && (!wasAClone || !query.isClonePessimisticLocked(workingClone, unitOfWork)));
            // Also need to refresh if the clone is a partial object and query requires more than its fetch group.
            if (wasAClone && descriptor.hasFetchGroupManager() && (descriptor.getFetchGroupManager().isPartialObject(workingClone) && (!descriptor.getFetchGroupManager().isObjectValidForFetchGroup(workingClone, query.getEntityFetchGroup())))) {
                isARefresh = true;
            }
            if (wasAClone && (!isARefresh)) {
                return workingClone;
            }
    
            boolean wasAnOriginal = false;
            boolean isIsolated = descriptor.getCachePolicy().shouldIsolateObjectsInUnitOfWork()
                    || (descriptor.shouldIsolateObjectsInUnitOfWorkEarlyTransaction() && unitOfWork.wasTransactionBegunPrematurely());

            Object original = null;    
            CacheKey originalCacheKey = null;
            // If not refreshing can get the object from the cache.
            if ((!isARefresh) && (!isIsolated) && !query.shouldRetrieveBypassCache() && !unitOfWork.shouldReadFromDB() && (!unitOfWork.shouldForceReadFromDB(query, primaryKey))) {
                AbstractSession session = unitOfWork.getParentIdentityMapSession(query);            
                originalCacheKey = session.getIdentityMapAccessorInstance().getCacheKeyForObject(primaryKey, descriptor.getJavaClass(), descriptor, false);
                if (originalCacheKey != null) {
                    // PERF: Read-lock is not required on object as unit of work will acquire this on clone and object cannot gc and object identity is maintained.
                    original = originalCacheKey.getObject();
                    wasAnOriginal = original != null;
                    // If the original is invalid or always refresh then need to refresh.
                    isARefresh = wasAnOriginal && (descriptor.shouldAlwaysRefreshCache() || descriptor.getCacheInvalidationPolicy().isInvalidated(originalCacheKey, query.getExecutionTime()));
                    // Otherwise can just register the cached original object and return it.
                    if (wasAnOriginal && (!isARefresh)){
                        if (descriptor.getCachePolicy().isSharedIsolation() || !descriptor.shouldIsolateProtectedObjectsInUnitOfWork()) {
                            // using shared isolation and the original is from the shared cache
                            // or using protected isolation and isolated client sessions
                            return unitOfWork.cloneAndRegisterObject(original, originalCacheKey, unitOfWorkCacheKey, descriptor);
                        }
                    }
                }
            }
    
            if (!wasAClone) {
                // This code is copied from UnitOfWork.cloneAndRegisterObject.  Unlike
                // that method we don't need to lock the shared cache, because
                // are not building off of an original in the shared cache.
                // The copy policy is easier to invoke if we have an original.
                if (wasAnOriginal && !query.shouldRetrieveBypassCache()) {
                    workingClone = instantiateWorkingCopyClone(original, unitOfWork);
                    // intentionally put nothing in clones to originals, unless really was one.
                    unitOfWork.getCloneToOriginals().put(workingClone, original);
                } else {
                    // What happens if a copy policy is defined is not pleasant.
                    workingClone = instantiateWorkingCopyCloneFromRow(databaseRow, query, primaryKey, unitOfWork);
                }
    
                // This must be registered before it is built to avoid cycles.
                // The version and read is set below in copyQueryInfoToCacheKey.
                unitOfWorkCacheKey.setObject(workingClone);
                
                // This must be registered before it is built to avoid cycles.
                unitOfWork.getCloneMapping().put(workingClone, workingClone);
            }
    
            // Must avoid infinite loops while refreshing.
            if (wasAClone && (unitOfWorkCacheKey.getLastUpdatedQueryId() >= query.getQueryId())) {
                return workingClone;
            }
            copyQueryInfoToCacheKey(unitOfWorkCacheKey, query, databaseRow, unitOfWork, descriptor);
            
            ObjectChangePolicy policy = descriptor.getObjectChangePolicy();
            // If it was a clone the change listener must be cleared after.
            if (!wasAClone) {
                // The change listener must be set before building the clone as aggregate/collections need the listener.
                policy.setChangeListener(workingClone, unitOfWork, descriptor);
            }
    
            // Turn it 'off' to prevent unwanted events.
            policy.dissableEventProcessing(workingClone);
            // Set fetch group before building object if a refresh to avoid fetching during building.
            FetchGroupManager fetchGroupManager = this.descriptor.getFetchGroupManager();
            if (isARefresh && fetchGroupManager != null) {
                fetchGroupManager.setObjectFetchGroup(workingClone, query.getExecutionFetchGroup(), unitOfWork);
            }
            if (descriptor.getCachePolicy().isProtectedIsolation() && !isIsolated && !query.shouldStoreBypassCache()){
                // we are at this point because we have isolated protected entities to the UnitOfWork
                // we should ensure that we populate the cache as well.
                originalCacheKey = (CacheKey) buildObject(true, query, databaseRow, unitOfWork.getParentIdentityMapSession(descriptor, false, true), primaryKey, descriptor, joinManager);
            }
            //If we are unable to access the shared cache because of any of the above settings at this point
            // the cachekey will be null so the attribute building will not be able to access the shared cache.
            if (isARefresh){
                //if we need to refresh the UOW then remove the cache key and the clone will be rebuilt not using any of the 
                //cache.  This should be updated to force the buildAttributesIntoWorkingCopyClone to refresh the objects 
                originalCacheKey = null;
            }
            // Build/refresh the clone from the row.
            buildAttributesIntoWorkingCopyClone(workingClone, originalCacheKey, query, joinManager, databaseRow, unitOfWork, wasAClone);
            // Set fetch group after building object if not a refresh to avoid checking fetch during building.           
            if ((!isARefresh) && fetchGroupManager != null) {
                fetchGroupManager.setObjectFetchGroup(workingClone, query.getExecutionFetchGroup(), unitOfWork);
            }
            Object backupClone = policy.buildBackupClone(workingClone, this, unitOfWork);
    
            // If it was a clone the change listener must be cleared.
            if (wasAClone) {
                policy.clearChanges(workingClone, unitOfWork, descriptor, isARefresh);
            }
            policy.enableEventProcessing(workingClone);
            unitOfWork.getCloneMapping().put(workingClone, backupClone);
            query.recordCloneForPessimisticLocking(workingClone, unitOfWork);
            // PERF: Cache the primary key if implements PersistenceEntity.
            if (workingClone instanceof PersistenceEntity) {
                ((PersistenceEntity)workingClone)._persistence_setId(primaryKey);
            }
        } finally {
            unitOfWorkCacheKey.release();            
        }
        instantiateEagerMappings(workingClone, unitOfWork);

        return workingClone;
    }
    
    /**
     * INTERNAL:
     * Builds a working copy clone directly from a result set.
     * PERF: This method is optimized for a specific case of building objects
     * so can avoid many of the normal checks, only queries that have this criteria
     * can use this method of building objects.
     */
    public Object buildObjectFromResultSet(ObjectBuildingQuery query, JoinedAttributeManager joinManager, ResultSet resultSet, AbstractSession executionSession, DatabaseAccessor accessor, ResultSetMetaData metaData, DatabasePlatform platform) throws SQLException {
        ClassDescriptor descriptor = this.descriptor;
        DatabaseMapping primaryKeyMapping = this.primaryKeyMappings.get(0);
        Object primaryKey = primaryKeyMapping.valueFromResultSet(resultSet, query, executionSession, accessor, metaData, 1, platform);

        UnitOfWorkImpl unitOfWork = null;
        AbstractSession session = executionSession;
        boolean isolated = !descriptor.getCachePolicy().isSharedIsolation();
        if (session.isUnitOfWork()) {
            unitOfWork = (UnitOfWorkImpl)executionSession;
        }
        
        CacheKey cacheKey = session.getIdentityMapAccessorInstance().getIdentityMapManager().acquireLock(primaryKey, descriptor.getJavaClass(), false, descriptor);
        CacheKey parentCacheKey = null;
        Object object = cacheKey.getObject();
        try {
            // Found locally in the unit of work, or session query and found in the session.
            if (object != null) {
                return object;
            }
            if ((unitOfWork != null) && !isolated) {
                // Need to lookup in the session.
                session = unitOfWork.getParentIdentityMapSession(query);
                parentCacheKey = session.getIdentityMapAccessorInstance().getIdentityMapManager().acquireLock(primaryKey, descriptor.getJavaClass(), false, descriptor);
                object = parentCacheKey.getObject();
            }
            // If the object is not in the cache, it needs to be built, this is building in the unit of work if isolated.
            if (object == null) {
                object = buildNewInstance();
                if (unitOfWork == null) {
                    cacheKey.setObject(object);                    
                } else {
                    if (isolated) {
                        cacheKey.setObject(object); 
                        unitOfWork.getCloneMapping().put(object, object);
                    } else {
                        parentCacheKey.setObject(object);                        
                    }
                }
                
                primaryKeyMapping.setAttributeValueInObject(object, primaryKey);
                List mappings = descriptor.getMappings();            
                int size = mappings.size();
                for (int index = 1; index < size; index++) {
                    DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);
                    mapping.readFromResultSetIntoObject(resultSet, object, query, session, accessor, metaData, index + 1, platform);
                }
                
                ((PersistenceEntity)object)._persistence_setId(primaryKey);
                if ((unitOfWork != null) && isolated) {
                    ObjectChangePolicy policy = descriptor.getObjectChangePolicy();
                    policy.setChangeListener(object, unitOfWork, descriptor);
                }
            }
            if ((unitOfWork != null) && !isolated) {
                // Need to clone the object in the unit of work.
                Object clone = instantiateWorkingCopyClone(object, unitOfWork);
                ((PersistenceEntity)clone)._persistence_setId(cacheKey.getKey());
                unitOfWork.getCloneMapping().put(clone, clone);
                unitOfWork.getCloneToOriginals().put(clone, object);
                cacheKey.setObject(clone);
                ObjectChangePolicy policy = descriptor.getObjectChangePolicy();
                policy.setChangeListener(clone, unitOfWork, descriptor);
                object = clone;
            }
        } finally {
            cacheKey.release();
            if (parentCacheKey != null) {
                parentCacheKey.release();
            }
        }

        return object;
    }

    /**
     * Returns a clone of itself.
     */
    public Object clone() {
        ObjectBuilder objectBuilder = null;
        try {
            objectBuilder = (ObjectBuilder)super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new InternalError(exception.toString());
        }
        // Only the shallow copy is created. The entries never change in these data structures
        objectBuilder.setMappingsByAttribute(new HashMap(getMappingsByAttribute()));
        objectBuilder.setMappingsByField(new HashMap(getMappingsByField()));
        objectBuilder.setFieldsMap(new HashMap(getFieldsMap()));
        objectBuilder.setReadOnlyMappingsByField(new HashMap(getReadOnlyMappingsByField()));
        objectBuilder.setPrimaryKeyMappings(new ArrayList(getPrimaryKeyMappings()));
        objectBuilder.setNonPrimaryKeyMappings(new ArrayList(getNonPrimaryKeyMappings()));
        objectBuilder.cloningMappings = new ArrayList(this.cloningMappings);
        objectBuilder.eagerMappings = new ArrayList(this.eagerMappings);
        objectBuilder.relationshipMappings = new ArrayList(this.relationshipMappings);
        
        return objectBuilder;
    }

    /**
     * INTERNAL:
     * This method is used by the UnitOfWork to cascade registration of new objects.
     * It may raise exceptions as described in the EJB3 specification
     */
    public void cascadePerformRemove(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        // PERF: Only process relationships.
        if (!this.isSimple) {
            List<DatabaseMapping> mappings = this.relationshipMappings;
            for (int index = 0; index < mappings.size(); index++) {
                DatabaseMapping mapping = mappings.get(index);
                mapping.cascadePerformRemoveIfRequired(object, uow, visitedObjects);
            }
        }
    }
    
    /**
     * INTERNAL:
     * This method is used to iterate over the specified object's mappings and cascade
     * remove orphaned private owned objects from the UnitOfWorkChangeSet and IdentityMap.
     */
    public void cascadePerformRemovePrivateOwnedObjectFromChangeSet(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        if (object != null && !this.isSimple) {
            for (DatabaseMapping mapping : this.relationshipMappings) {
                // only cascade into private owned mappings
                if (mapping.isPrivateOwned()) {
                    mapping.cascadePerformRemovePrivateOwnedObjectFromChangeSetIfRequired(object, uow, visitedObjects);
                }
            }
        }
    }

    /**
     * INTERNAL: 
     * This method is used to store the FK values used for this mapping in the cachekey.
     * This is used when the mapping is protected but we have retrieved the fk values and will cache
     * them for use when the entity is cloned.
     */
    public void cacheForeignKeyValues(AbstractRecord databaseRecord, CacheKey cacheKey, AbstractSession session) {
        Set<DatabaseField> foreignKeys = this.descriptor.getForeignKeyValuesForCaching();
        if (foreignKeys.isEmpty()) {
            return;
        }
        DatabaseRecord cacheRecord = new DatabaseRecord(foreignKeys.size());
        for (DatabaseField field : foreignKeys) {
            cacheRecord.put(field, databaseRecord.get(field));
        }
        cacheKey.setProtectedForeignKeys(cacheRecord);
        
    }

    /**
     * INTERNAL: 
     * This method is used to store the FK values used for this mapping in the cachekey.
     * This is used when the mapping is protected but we have retrieved the fk values and will cache
     * them for use when the entity is cloned.
     */
    public void cacheForeignKeyValues(Object source, CacheKey cacheKey, ClassDescriptor descriptor, AbstractSession session) {
        Set<DatabaseField> foreignKeys = this.descriptor.getForeignKeyValuesForCaching();
        if (foreignKeys.isEmpty()) {
            return;
        }
        DatabaseRecord cacheRecord = new DatabaseRecord(foreignKeys.size());
        for (DatabaseField field : foreignKeys) {
            cacheRecord.put(field, extractValueFromObjectForField(source, field, session));
        }
        cacheKey.setProtectedForeignKeys(cacheRecord);
    }

    /**
     * INTERNAL:
     * Cascade discover and persist new objects during commit.
     * It may raise exceptions as described in the EJB3 specification
     */
    public void cascadeDiscoverAndPersistUnregisteredNewObjects(Object object, Map newObjects, Map unregisteredExistingObjects, Map visitedObjects, UnitOfWorkImpl uow, Set cascadeErrors) {
        // PERF: Only process relationships.
        if (!this.isSimple) {
            List<DatabaseMapping> mappings = this.relationshipMappings;
            int size = mappings.size();
            FetchGroupManager fetchGroupManager = descriptor.getFetchGroupManager();
            // Only cascade fetched mappings.
            if ((fetchGroupManager != null) && fetchGroupManager.isPartialObject(object)) {
                for (int index = 0; index < size; index++) {
                    DatabaseMapping mapping = mappings.get(index);
                    if (fetchGroupManager.isAttributeFetched(object, mapping.getAttributeName())) {
                        mapping.cascadeDiscoverAndPersistUnregisteredNewObjects(object, newObjects, unregisteredExistingObjects, visitedObjects, uow, cascadeErrors);
                    }
                }
            } else {
                for (int index = 0; index < size; index++) {
                    DatabaseMapping mapping = mappings.get(index);
                    mapping.cascadeDiscoverAndPersistUnregisteredNewObjects(object, newObjects, unregisteredExistingObjects, visitedObjects, uow, cascadeErrors);
                }
            }
        }
    }
    
    /**
     * INTERNAL:
     * This method is used by the UnitOfWork to cascade registration of new objects.
     * It may raise exceptions as described in the EJB3 specification
     */
    public void cascadeRegisterNewForCreate(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        // PERF: Only process relationships.
        if (!this.isSimple) {
            List<DatabaseMapping> mappings = this.relationshipMappings;
            int size = mappings.size();
            FetchGroupManager fetchGroupManager = this.descriptor.getFetchGroupManager();
            // Only cascade fetched mappings.
            if ((fetchGroupManager != null) && fetchGroupManager.isPartialObject(object)) {
                for (int index = 0; index < size; index++) {
                    DatabaseMapping mapping = mappings.get(index);
                    if (fetchGroupManager.isAttributeFetched(object, mapping.getAttributeName())) {
                        mapping.cascadeRegisterNewIfRequired(object, uow, visitedObjects);
                    }
                }
            } else {
                for (int index = 0; index < size; index++) {
                    DatabaseMapping mapping = mappings.get(index);
                    mapping.cascadeRegisterNewIfRequired(object, uow, visitedObjects);
                }
            }
        }
        // Allow persist to set the partitioning connection.
        if (this.descriptor.getPartitioningPolicy() != null) {
            this.descriptor.getPartitioningPolicy().partitionPersist(uow.getParent(), object, this.descriptor);
        }
    }

    /**
     * INTERNAL:
     * This method creates a records change set for a particular object.
     * It should only be used by aggregates.
     * @return ObjectChangeSet
     */
    public ObjectChangeSet compareForChange(Object clone, Object backUp, UnitOfWorkChangeSet changeSet, AbstractSession session) {
        // delegate the change comparison to this objects ObjectChangePolicy - TGW
        return descriptor.getObjectChangePolicy().calculateChanges(clone, backUp, backUp == null, changeSet, ((UnitOfWorkImpl)session), this.descriptor, true);
    }

    /**
     * Compares the two specified objects
     */
    public boolean compareObjects(Object firstObject, Object secondObject, AbstractSession session) {
        // PERF: Avoid iterator.
        List mappings = this.descriptor.getMappings();
        for (int index = 0; index < mappings.size(); index++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);

            if (!mapping.compareObjects(firstObject, secondObject, session)) {
                Object firstValue = mapping.getAttributeValueFromObject(firstObject);
                Object secondValue = mapping.getAttributeValueFromObject(secondObject);
                session.log(SessionLog.FINEST, SessionLog.QUERY, "compare_failed", mapping, firstValue, secondValue);
                return false;
            }
        }

        return true;
    }

    /**
     * Copy each attribute from one object into the other.
     */
    public void copyInto(Object source, Object target, boolean cloneOneToOneValueHolders) {
        // PERF: Avoid iterator.
        List mappings = this.descriptor.getMappings();
        for (int index = 0; index < mappings.size(); index++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);
            Object value = null;
            if (cloneOneToOneValueHolders && mapping.isForeignReferenceMapping()){
                value = ((ForeignReferenceMapping)mapping).getAttributeValueWithClonedValueHolders(source);
            } else {
                value = mapping.getAttributeValueFromObject(source);
            }
            mapping.setAttributeValueInObject(target, value);
        }
    }
    
    /**
     * Copy each attribute from one object into the other.
     */
    public void copyInto(Object source, Object target) {
        copyInto(source, target, false);
    }

    /**
     * Return a copy of the object.
     * This is NOT used for unit of work but for templatizing an object.
     * The depth and primary key reseting are passed in.
     */
    public Object copyObject(Object original, CopyGroup copyGroup) {
        Object copy = copyGroup.getCopies().get(original);
        if (copyGroup.shouldCascadeTree()) {
            FetchGroupManager fetchGroupManager = this.descriptor.getFetchGroupManager();
            if (fetchGroupManager != null) {
                // empty copy group means all the attributes should be copied - don't alter it.
                if (copyGroup.hasItems()) {
                    // by default add primary key attribute(s) if not already in the group
                    if (!copyGroup.shouldResetPrimaryKey()) {
                        for (DatabaseMapping mapping : this.primaryKeyMappings) {
                            String name = mapping.getAttributeName();
                            if (!copyGroup.containsAttributeInternal(name)) {
                               copyGroup.addAttribute(name); 
                            }
                        }
                    } else {
                        for (DatabaseMapping mapping : this.primaryKeyMappings) {
                            if (mapping.isForeignReferenceMapping()) {
                                String name = mapping.getAttributeName();
                                if (!copyGroup.containsAttributeInternal(name)) {
                                   copyGroup.addAttribute(name); 
                                }
                            }
                        }
                    }
                    
                    // by default version attribute if not already in the group
                    if (!copyGroup.shouldResetVersion()) {
                        if (this.lockAttribute != null) {
                            if (!copyGroup.containsAttributeInternal(this.lockAttribute)) {
                               copyGroup.addAttribute(this.lockAttribute); 
                            }
                        }
                    }
                    
                    FetchGroup fetchGroup = fetchGroupManager.getObjectFetchGroup(original);
                    if (fetchGroup != null) {
                        if (!fetchGroup.getAttributeNames().containsAll(copyGroup.getAttributeNames())) {
                            // trigger fetch group if it does not contain all attributes of the copy group.
                            fetchGroup.onUnfetchedAttribute((FetchGroupTracker)original, null);
                        }
                    }
                }

                // Entity fetch group currently set on copyObject
                EntityFetchGroup existingEntityFetchGroup = null;
                if (copy != null) {
                    Object[] copyArray = (Object[])copy;
                    // copy of the original
                    copy = copyArray[0];
                    // A set of CopyGroups that have visited.
                    Set<CopyGroup> visitedCopyGroups = (Set<CopyGroup>)copyArray[1];
                    
                    if(visitedCopyGroups.contains(copyGroup)) {
                        // original has been already visited with this copyGroup - leave
                        return copy;
                    } else {
                        visitedCopyGroups.add(copyGroup);
                    }
                    
                    existingEntityFetchGroup = fetchGroupManager.getObjectEntityFetchGroup(copy);
                }
                
                // Entity fetch group that will be assigned to copyObject
                EntityFetchGroup newEntityFetchGroup = null;
                
                // Attributes to be visited - only reference mappings will be visited.
                // If null then all attributes should be visited.
                Set<String> attributesToVisit = copyGroup.getAttributeNames();
                // Attributes to be copied
                Set<String> attributesToCopy = attributesToVisit;
                boolean shouldCopyAllAttributes = false;
                boolean shouldAssignNewEntityFetchGroup = false;
                if(copy != null && existingEntityFetchGroup == null) {
                    // all attributes have been already copied
                    attributesToCopy = null;
                } else {
                    // Entity fetch group corresponding to copyPolicy.
                    // Note that empty, or null, or containing all arguments attributesToCopy
                    // results in copyGroupFetchGroup = null;
                    EntityFetchGroup copyGroupEntityFetchGroup = fetchGroupManager.getEntityFetchGroup(attributesToCopy);
                    if(copyGroupEntityFetchGroup == null) {
                        // all attributes will be copied
                        shouldCopyAllAttributes = true;
                    }

                    if(copy != null) {
                        if(copyGroupEntityFetchGroup != null) {
                            if(!copyGroup.shouldResetPrimaryKey()) {
                                if(!existingEntityFetchGroup.getAttributeNames().containsAll(attributesToCopy)) {
                                    // Entity fetch group that will be assigned to copy object
                                    newEntityFetchGroup = fetchGroupManager.flatUnionFetchGroups(existingEntityFetchGroup, copyGroupEntityFetchGroup);
                                    shouldAssignNewEntityFetchGroup = true;
                                }
                            }
                            attributesToCopy = new HashSet(attributesToCopy);
                            attributesToCopy.removeAll(existingEntityFetchGroup.getAttributeNames());
                        }
                    } else {
                        // copy does not exist - create it
                        copy = copyGroup.getSession().getDescriptor(original).getObjectBuilder().buildNewInstance();
                        Set<CopyGroup> visitedCopyGroups = new HashSet();
                        visitedCopyGroups.add(copyGroup);
                        copyGroup.getCopies().put(original, new Object[]{copy, visitedCopyGroups});
                        if(!copyGroup.shouldResetPrimaryKey()) {
                            newEntityFetchGroup = copyGroupEntityFetchGroup;
                            shouldAssignNewEntityFetchGroup = true;
                        }
                    }
                }
                if(shouldAssignNewEntityFetchGroup) {
                    fetchGroupManager.setObjectFetchGroup(copy, newEntityFetchGroup, null);
                }

                for (DatabaseMapping mapping : getDescriptor().getMappings()) {
                    String name = mapping.getAttributeName();
                    boolean shouldCopy = shouldCopyAllAttributes || (attributesToCopy != null && attributesToCopy.contains(name));
                    boolean shouldVisit = attributesToVisit == null || attributesToVisit.contains(name);
                    if(shouldCopy || shouldVisit) {
                        boolean isVisiting = false;
                        // unless it's a reference mapping pass copyGroup - just to carry the session.
                        CopyGroup mappingCopyGroup = copyGroup;
                        if(mapping.isForeignReferenceMapping()) {
                            ForeignReferenceMapping frMapping = (ForeignReferenceMapping)mapping;
                            ClassDescriptor referenceDescriptor = frMapping.getReferenceDescriptor();
                            if(referenceDescriptor != null) {
                                isVisiting = true;
                                mappingCopyGroup = copyGroup.getGroup(name);
                                if(mappingCopyGroup == null) {
                                    FetchGroupManager referenceFetchGroupManager = referenceDescriptor.getFetchGroupManager();
                                    if(referenceFetchGroupManager != null) {
                                        EntityFetchGroup nonReferenceEntityFetchGroup = referenceFetchGroupManager.getNonReferenceEntityFetchGroup(copyGroup.shouldResetPrimaryKey(), copyGroup.shouldResetVersion());
                                        if(nonReferenceEntityFetchGroup != null) {
                                            mappingCopyGroup = nonReferenceEntityFetchGroup.toCopyGroup();
                                        } else {
                                            // null nonReferenceEntityFetchGroup is equivalent to containing all attributes:
                                            // create a new empty CopyGroup.
                                            mappingCopyGroup = new CopyGroup();
                                            mappingCopyGroup.shouldCascadeTree();
                                        }
                                    } else {
                                        // TODO: would that work?
                                        mappingCopyGroup = new CopyGroup();
                                        mappingCopyGroup.dontCascade();
                                        isVisiting = false;
                                    }
                                    mappingCopyGroup.setCopies(copyGroup.getCopies());
                                    mappingCopyGroup.setShouldResetPrimaryKey(copyGroup.shouldResetPrimaryKey());
                                    mappingCopyGroup.setShouldResetVersion(copyGroup.shouldResetVersion());
                                }
                                mappingCopyGroup.setSession(copyGroup.getSession());
                            }
                        } else if (mapping.isAggregateObjectMapping()) {
                            mappingCopyGroup = new CopyGroup();
                        }
                        if(shouldCopy || isVisiting) {
                            // TODO: optimization: (even when isVisiting == true) redefine buildCopy to take shouldCopy and don't copy if not required.
                            mapping.buildCopy(copy, original, mappingCopyGroup);
                        }
                    }
                }
            } else {
                // fetchGroupManager == null
                // TODO
            }

        } else {
            // ! copyGroup.shouldCascadeTree()
            if (copy != null) {
                return copy;
            }
    
            copy = instantiateClone(original, copyGroup.getSession());
            copyGroup.getCopies().put(original, copy);
    
            // PERF: Avoid synchronized enumerator as is concurrency bottleneck.
            List mappings = getCloningMappings();
            int size = mappings.size();
            for (int index = 0; index < size; index++) {
                ((DatabaseMapping)mappings.get(index)).buildCopy(copy, original, copyGroup);
            }
    
            if (copyGroup.shouldResetPrimaryKey() && (!(this.descriptor.isDescriptorTypeAggregate()))) {
                // Do not reset if any of the keys is mapped through a 1-1, i.e. back reference id has already changed.
                boolean hasOneToOne = false;
                List primaryKeyMappings = getPrimaryKeyMappings();
                size = primaryKeyMappings.size();
                for (int index = 0; index < size; index++) {
                    if (((DatabaseMapping)primaryKeyMappings.get(index)).isOneToOneMapping()) {
                        hasOneToOne = true;
                    }
                }
                if (!hasOneToOne) {
                    for (int index = 0; index < size; index++) {
                        DatabaseMapping mapping = (DatabaseMapping)primaryKeyMappings.get(index);
    
                        // Only null out direct mappings, as others will be nulled in the respective objects.
                        if (mapping.isAbstractColumnMapping()) {
                            Object nullValue = ((AbstractColumnMapping)mapping).getObjectValue(null, copyGroup.getSession());
                            mapping.setAttributeValueInObject(copy, nullValue);
                        } else if (mapping.isTransformationMapping()) {
                            mapping.setAttributeValueInObject(copy, null);
                        }
                    }
                }
            }
    
            // PERF: Avoid events if no listeners.
            if (this.descriptor.getEventManager().hasAnyEventListeners()) {
                org.eclipse.persistence.descriptors.DescriptorEvent event = new org.eclipse.persistence.descriptors.DescriptorEvent(copy);
                event.setSession(copyGroup.getSession());
                event.setOriginalObject(original);
                event.setEventCode(DescriptorEventManager.PostCloneEvent);
                this.descriptor.getEventManager().executeEvent(event);
            }
        }

        return copy;
    }

    /**
     * INTERNAL:
     * Used by the ObjectBuilder to create an ObjectChangeSet for the specified clone object.
     * @return ObjectChangeSet the newly created changeSet representing the clone object
     * @param clone the object to convert to a changeSet.
     * @param uowChangeSet the owner of this changeSet.
     */
    public ObjectChangeSet createObjectChangeSet(Object clone, UnitOfWorkChangeSet uowChangeSet, AbstractSession session) {
        boolean isNew = ((UnitOfWorkImpl)session).isCloneNewObject(clone);
        return createObjectChangeSet(clone, uowChangeSet, isNew, session);
    }

    /**
     * INTERNAL:
     * Used by the ObjectBuilder to create an ObjectChangeSet for the specified clone object.
     * @return ObjectChangeSet the newly created changeSet representing the clone object
     * @param clone the object to convert to a changeSet.
     * @param uowChangeSet the owner of this changeSet.
     * @param isNew signifies if the clone object is a new object.
     */
    public ObjectChangeSet createObjectChangeSet(Object clone, UnitOfWorkChangeSet uowChangeSet, boolean isNew, AbstractSession session) {
        return createObjectChangeSet(clone, uowChangeSet, isNew, false, session);
    }

    /**
     * INTERNAL:
     * Used by the ObjectBuilder to create an ObjectChangeSet for the specified clone object.
     * @return ObjectChangeSet the newly created changeSet representing the clone object
     * @param clone the object to convert to a changeSet.
     * @param uowChangeSet the owner of this changeSet.
     * @param isNew signifies if the clone object is a new object.
     * @param assignPrimaryKeyIfExisting signifies if the primary key of the change set should be updated if existing.
     */
    public ObjectChangeSet createObjectChangeSet(Object clone, UnitOfWorkChangeSet uowChangeSet, boolean isNew, boolean assignPrimaryKeyIfExisting, AbstractSession session) {
        ObjectChangeSet changes = (ObjectChangeSet)uowChangeSet.getObjectChangeSetForClone(clone);
        if (changes == null) {
            if (this.descriptor.isAggregateDescriptor()) {
                changes = new AggregateObjectChangeSet(new CacheId(new Object[0]), this.descriptor, clone, uowChangeSet, isNew);
            } else {
                changes = new ObjectChangeSet(extractPrimaryKeyFromObject(clone, session, true), this.descriptor, clone, uowChangeSet, isNew);
            }
            changes.setIsAggregate(this.descriptor.isDescriptorTypeAggregate());
            uowChangeSet.addObjectChangeSetForIdentity(changes, clone);
        } else{
            if (isNew && !changes.isNew()) {
                //this is an unregistered new object that we found during change calc
                //or change listener update.  Let's switch it to be new.
                changes.setIsNew(isNew);
            }
            if (assignPrimaryKeyIfExisting) {
                if (!changes.isAggregate()) {
                    // If creating a new change set for a new object, the original change set (from change tracking) may have not had the primary key.
                    Object primaryKey = extractPrimaryKeyFromObject(clone, session, true);
                    if (primaryKey != null) {
                        changes.setId(primaryKey);
                    }
                }
           }
        }
        return changes;
    }
        
    /**
     * Creates and stores primary key expression.
     */
    public void createPrimaryKeyExpression(AbstractSession session) {
        Expression expression = null;
        Expression builder = new ExpressionBuilder();
        Expression subExp1;
        Expression subExp2;
        Expression subExpression;
        List primaryKeyFields = this.descriptor.getPrimaryKeyFields();

        if(null != primaryKeyFields) {
            for (int index = 0; index < primaryKeyFields.size(); index++) {
                DatabaseField primaryKeyField = (DatabaseField)primaryKeyFields.get(index);
                subExp1 = builder.getField(primaryKeyField);
                subExp2 = builder.getParameter(primaryKeyField);
                subExpression = subExp1.equal(subExp2);

                if (expression == null) {
                    expression = subExpression;
                } else {
                    expression = expression.and(subExpression);
                }
            }
        }

        setPrimaryKeyExpression(expression);
    }
    
    /**
     * Return the row with primary keys and their values from the given expression.
     */
    public Object extractPrimaryKeyFromExpression(boolean requiresExactMatch, Expression expression, AbstractRecord translationRow, AbstractSession session) {
        AbstractRecord primaryKeyRow = createRecord(getPrimaryKeyMappings().size(), session);

        expression.getBuilder().setSession(session.getRootSession(null));
        // Get all the field & values from expression.
        boolean isValid = expression.extractPrimaryKeyValues(requiresExactMatch, this.descriptor, primaryKeyRow, translationRow);
        if (requiresExactMatch && (!isValid)) {
            return null;
        }

        // Check that the sizes match.
        if (primaryKeyRow.size() != this.descriptor.getPrimaryKeyFields().size()) {
            return null;
        }

        Object primaryKey = extractPrimaryKeyFromRow(primaryKeyRow, session);
        if ((primaryKey == null) && isValid) {
            return InvalidObject.instance;
        }
        return primaryKey;
    }
    
    /**
     * Return if the expression is by primary key.
     */
    public boolean isPrimaryKeyExpression(boolean requiresExactMatch, Expression expression, AbstractSession session) {
        expression.getBuilder().setSession(session.getRootSession(null));
        List<DatabaseField> keyFields = this.descriptor.getPrimaryKeyFields();
        int size = keyFields.size();
        Set<DatabaseField> fields = new HashSet(size);
        boolean isValid = expression.extractFields(requiresExactMatch, true, this.descriptor, keyFields, fields);
        if (requiresExactMatch && (!isValid)) {
            return false;
        }
        // Check that the sizes match.
        if (fields.size() != size) {
            return false;
        }
        return true;
    }

    /**
     * Extract primary key attribute values from the domainObject.
     */
    public Object extractPrimaryKeyFromObject(Object domainObject, AbstractSession session) {
        return extractPrimaryKeyFromObject(domainObject, session, false);
    }
    
    /**
     * Extract primary key attribute values from the domainObject.
     */
    public Object extractPrimaryKeyFromObject(Object domainObject, AbstractSession session, boolean shouldReturnNullIfNull) {
        if (domainObject == null) {
            return null;
        }
        // Avoid using the cached id for XML, as the relational descriptor may be different than the xml one.
        boolean isPersistenceEntity = (domainObject instanceof PersistenceEntity) && (!isXMLObjectBuilder());
        if (isPersistenceEntity) {
            Object primaryKey = ((PersistenceEntity)domainObject)._persistence_getId();
            if (primaryKey != null) {
                return primaryKey;
            }
        }
        ClassDescriptor descriptor = this.descriptor;
        boolean isNull = false;
        // Allow for inheritance, the concrete descriptor must always be used.
        if (descriptor.hasInheritance() && (domainObject.getClass() != descriptor.getJavaClass()) && (!domainObject.getClass().getSuperclass().equals(descriptor.getJavaClass()))) {
            return session.getDescriptor(domainObject).getObjectBuilder().extractPrimaryKeyFromObject(domainObject, session, shouldReturnNullIfNull);
        }
        
        CacheKeyType cacheKeyType = descriptor.getCachePolicy().getCacheKeyType();
        List<DatabaseField> primaryKeyFields = descriptor.getPrimaryKeyFields();        
        Object[] primaryKeyValues = null;
        if (cacheKeyType != CacheKeyType.ID_VALUE) {
            primaryKeyValues = new Object[primaryKeyFields.size()];
        }
        List<DatabaseMapping> mappings = getPrimaryKeyMappings();
        int size = mappings.size();
        // PERF: optimize simple case of direct mapped singleton primary key.
        if (descriptor.hasSimplePrimaryKey()) {
            // PERF: use index not enumeration.
            for (int index = 0; index < size; index++) {
                AbstractColumnMapping mapping = (AbstractColumnMapping)mappings.get(index);
                Object keyValue = mapping.valueFromObject(domainObject, primaryKeyFields.get(index), session);
                if (isPrimaryKeyComponentInvalid(keyValue, index)) {
                    if (shouldReturnNullIfNull) {
                        return null;
                    }
                    isNull = true;
                }
                if (cacheKeyType == CacheKeyType.ID_VALUE) {
                    if (isPersistenceEntity && (!isNull)) {
                        ((PersistenceEntity)domainObject)._persistence_setId(keyValue);
                    }
                    return keyValue;
                } else {
                    primaryKeyValues[index] = keyValue;
                }
            }
        } else {
            AbstractRecord databaseRow = createRecordForPKExtraction(size, session);
            // PERF: use index not enumeration			
            for (int index = 0; index < size; index++) {
                DatabaseMapping mapping = mappings.get(index);
                // Primary key mapping may be null for aggregate collection.
                if (mapping != null) {
                    mapping.writeFromObjectIntoRow(domainObject, databaseRow, session, WriteType.UNDEFINED);
                }
            }
            List<Class> primaryKeyClassifications = getPrimaryKeyClassifications();
            Platform platform = session.getPlatform(domainObject.getClass());
            // PERF: use index not enumeration
            for (int index = 0; index < size; index++) {
                // Ensure that the type extracted from the object is the same type as in the descriptor,
                // the main reason for this is that 1-1 can optimize on vh by getting from the row as the row-type.
                Class classification = primaryKeyClassifications.get(index);
                Object value = databaseRow.get(primaryKeyFields.get(index));
                if (isPrimaryKeyComponentInvalid(value, index)) {
                    if (shouldReturnNullIfNull) {
                        return null;
                    }
                    isNull = true;
                }
                value = platform.convertObject(value, classification);
                if (cacheKeyType == CacheKeyType.ID_VALUE) {
                    if (isPersistenceEntity && (!isNull)) {
                        ((PersistenceEntity)domainObject)._persistence_setId(value);
                    }
                    return value;
                } else {
                    primaryKeyValues[index] = value;
                }
            }
        }
        CacheId id = new CacheId(primaryKeyValues);
        if (isPersistenceEntity && (!isNull)) {
            ((PersistenceEntity)domainObject)._persistence_setId(id);
        }
        return id;
    }

    /**
     * Extract primary key values from the specified row.
     * null is returned if the row does not contain the key.
     */
    public Object extractPrimaryKeyFromRow(AbstractRecord databaseRow, AbstractSession session) {
        List<DatabaseField> primaryKeyFields = this.descriptor.getPrimaryKeyFields();
        if(null == primaryKeyFields) {
            return null;
        }
        List<Class> primaryKeyClassifications = getPrimaryKeyClassifications();
        int size = primaryKeyFields.size();
        Object[] primaryKeyValues = null;
        CacheKeyType cacheKeyType = this.descriptor.getCachePolicy().getCacheKeyType();
        if (cacheKeyType != CacheKeyType.ID_VALUE) {
            primaryKeyValues = new Object[size];
        }
        int numberOfNulls = 0;

        // PERF: use index not enumeration
        for (int index = 0; index < size; index++) {
            DatabaseField field = primaryKeyFields.get(index);

            // Ensure that the type extracted from the row is the same type as in the object.
            Class classification = primaryKeyClassifications.get(index);
            Object value = databaseRow.get(field);
            if (value != null) {
                if (value.getClass() != classification) {
                    value = session.getPlatform(this.descriptor.getJavaClass()).convertObject(value, classification);
                }
                if (cacheKeyType == CacheKeyType.ID_VALUE) {
                    return value;
                }
                primaryKeyValues[index] = value;
            } else {
                if (this.mayHaveNullInPrimaryKey) {
                    numberOfNulls++;
                    if (numberOfNulls < size) {
                        primaryKeyValues[index] = null;
                    } else {
                        // Must have some non null elements. If all elements are null return null.
                        return null;
                    }
                } else {
                    return null;
                }
            }
        }

        return new CacheId(primaryKeyValues);
    }

    /**
     * Return the row with primary keys and their values from the given expression.
     */
    public AbstractRecord extractPrimaryKeyRowFromExpression(Expression expression, AbstractRecord translationRow, AbstractSession session) {
        AbstractRecord primaryKeyRow = createRecord(getPrimaryKeyMappings().size(), session);

        expression.getBuilder().setSession(session.getRootSession(null));
        // Get all the field & values from expression	
        boolean isValid = expression.extractPrimaryKeyValues(true, this.descriptor, primaryKeyRow, translationRow);
        if (!isValid) {
            return null;
        }

        // Check that the sizes match up 
        if (primaryKeyRow.size() != this.descriptor.getPrimaryKeyFields().size()) {
            return null;
        }

        return primaryKeyRow;
    }

    /**
     * Return the row from the given expression.
     */
    public AbstractRecord extractRowFromExpression(Expression expression, AbstractRecord translationRow, AbstractSession session) {
        AbstractRecord record = createRecord(session);

        expression.getBuilder().setSession(session.getRootSession(null));
        // Get all the field & values from expression   
        boolean isValid = expression.extractValues(false, false, this.descriptor, record, translationRow);
        if (!isValid) {
            return null;
        }

        return record;
    }

    /**
     * Extract primary key attribute values from the domainObject.
     */
    public AbstractRecord extractPrimaryKeyRowFromObject(Object domainObject, AbstractSession session) {
        AbstractRecord databaseRow = createRecord(getPrimaryKeyMappings().size(), session);

        // PERF: use index not enumeration.
        for (int index = 0; index < getPrimaryKeyMappings().size(); index++) {
            getPrimaryKeyMappings().get(index).writeFromObjectIntoRow(domainObject, databaseRow, session, WriteType.UNDEFINED);
        }

        // PERF: optimize simple primary key case, no need to remap.
        if (this.descriptor.hasSimplePrimaryKey()) {
            return databaseRow;
        }
        AbstractRecord primaryKeyRow = createRecord(getPrimaryKeyMappings().size(), session);
        List primaryKeyFields = this.descriptor.getPrimaryKeyFields();
        for (int index = 0; index < primaryKeyFields.size(); index++) {
            // Ensure that the type extracted from the object is the same type as in the descriptor,
            // the main reason for this is that 1-1 can optimize on vh by getting from the row as the row-type.
            Class classification = getPrimaryKeyClassifications().get(index);
            DatabaseField field = (DatabaseField)primaryKeyFields.get(index);
            Object value = databaseRow.get(field);
            primaryKeyRow.put(field, session.getPlatform(domainObject.getClass()).convertObject(value, classification));
        }

        return primaryKeyRow;
    }

    /**
     * Extract the value of the primary key attribute from the specified object.
     */
    public Object extractValueFromObjectForField(Object domainObject, DatabaseField field, AbstractSession session) throws DescriptorException {
        // Allow for inheritance, the concrete descriptor must always be used.
        ClassDescriptor descriptor = null;//this variable will be assigned in the final

        if (this.descriptor.hasInheritance() && (domainObject.getClass() != this.descriptor.getJavaClass()) && ((descriptor = session.getDescriptor(domainObject)).getJavaClass() != this.descriptor.getJavaClass())) {
            if(descriptor.isAggregateCollectionDescriptor()) {
                descriptor = this.descriptor.getInheritancePolicy().getDescriptor(descriptor.getJavaClass());
            }
            return descriptor.getObjectBuilder().extractValueFromObjectForField(domainObject, field, session);
        } else {
            DatabaseMapping mapping = getMappingForField(field);
            if (mapping == null) {
                throw DescriptorException.missingMappingForField(field, this.descriptor);
            }

            return mapping.valueFromObject(domainObject, field, session);
        }
    }
    /**
     * INTERNAL:
     * An object has been serialized from the server to the client.
     * Replace the transient attributes of the remote value holders
     * with client-side objects.
     */
    public void fixObjectReferences(Object object, Map objectDescriptors, Map processedObjects, ObjectLevelReadQuery query, RemoteSession session) {
        // PERF: Only process relationships.
        if (!this.isSimple) {
            List<DatabaseMapping> mappings = this.relationshipMappings;
            for (int index = 0; index < mappings.size(); index++) {
                mappings.get(index).fixObjectReferences(object, objectDescriptors, processedObjects, query, session);
            }
        }
    }

    /**
     * Return the base ChangeRecord for the given DatabaseField.
     * The object and all its relevant aggregates must exist.
     * The returned ChangeRecord is 
     * either DirectToFieldChangeRecord or TransformationMappingChangeRecord,
     * or null.
     */
    public ChangeRecord getBaseChangeRecordForField(ObjectChangeSet objectChangeSet, Object object, DatabaseField databaseField, AbstractSession session) {
        DatabaseMapping mapping = getMappingForField(databaseField);

        // Drill down through the mappings until we get the direct mapping to the databaseField.    
        while (mapping.isAggregateObjectMapping()) {
            String attributeName = mapping.getAttributeName();
            Object aggregate = mapping.getAttributeValueFromObject(object);
            ClassDescriptor referenceDescriptor = ((AggregateObjectMapping)mapping).getReferenceDescriptor();
            AggregateChangeRecord aggregateChangeRecord = (AggregateChangeRecord)objectChangeSet.getChangesForAttributeNamed(attributeName);
            if (aggregateChangeRecord == null) {
                aggregateChangeRecord = new AggregateChangeRecord(objectChangeSet);
                aggregateChangeRecord.setAttribute(attributeName);
                aggregateChangeRecord.setMapping(mapping);
                objectChangeSet.addChange(aggregateChangeRecord);
            }
            ObjectChangeSet aggregateChangeSet = (ObjectChangeSet)aggregateChangeRecord.getChangedObject();
            if (aggregateChangeSet == null) {
                aggregateChangeSet = referenceDescriptor.getObjectBuilder().createObjectChangeSet(aggregate, (UnitOfWorkChangeSet)objectChangeSet.getUOWChangeSet(), session);
                aggregateChangeRecord.setChangedObject(aggregateChangeSet);
            }

            mapping = referenceDescriptor.getObjectBuilder().getMappingForField(databaseField);
            objectChangeSet = aggregateChangeSet;
            object = aggregate;
        }
        
        String attributeName = mapping.getAttributeName();
        if (mapping.isAbstractDirectMapping()) {
            DirectToFieldChangeRecord changeRecord = (DirectToFieldChangeRecord)objectChangeSet.getChangesForAttributeNamed(attributeName);
            if (changeRecord == null) {
                changeRecord = new DirectToFieldChangeRecord(objectChangeSet);
                changeRecord.setAttribute(attributeName);
                changeRecord.setMapping(mapping);
                objectChangeSet.addChange(changeRecord);
            }
            return changeRecord;
        } else if (mapping.isTransformationMapping()) {
            TransformationMappingChangeRecord changeRecord = (TransformationMappingChangeRecord)objectChangeSet.getChangesForAttributeNamed(attributeName);
            if (changeRecord == null) {
                changeRecord = new TransformationMappingChangeRecord(objectChangeSet);
                changeRecord.setAttribute(attributeName);
                changeRecord.setMapping(mapping);
                objectChangeSet.addChange(changeRecord);
            }
            return changeRecord;
        } else {
            session.log(SessionLog.FINEST, SessionLog.QUERY, "field_for_unsupported_mapping_returned", databaseField, getDescriptor());
            return null;
        }
    }

    /**
     * Return the base mapping for the given DatabaseField.
     */
    public DatabaseMapping getBaseMappingForField(DatabaseField databaseField) {
        DatabaseMapping mapping = getMappingForField(databaseField);

        // Drill down through the mappings until we get the direct mapping to the databaseField.	
        while ((mapping != null) && mapping.isAggregateObjectMapping()) {
            mapping = ((AggregateObjectMapping)mapping).getReferenceDescriptor().getObjectBuilder().getMappingForField(databaseField);
        }
        return mapping;
    }

    /**
     * Return the base value that is mapped to for given field.
     */
    public Object getBaseValueForField(DatabaseField databaseField, Object domainObject) {
        Object valueIntoObject = domainObject;
        DatabaseMapping mapping = getMappingForField(databaseField);

        // Drill down through the aggregate mappings to get to the direct to field mapping.
        while (mapping.isAggregateObjectMapping()) {
            valueIntoObject = mapping.getAttributeValueFromObject(valueIntoObject);
            mapping = ((AggregateMapping)mapping).getReferenceDescriptor().getObjectBuilder().getMappingForField(databaseField);
        }
        return mapping.getAttributeValueFromObject(valueIntoObject);
    }

    /**
     * Return the descriptor
     */
    public ClassDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * INTERNAL:
     * Return the classification for the field contained in the mapping.
     * This is used to convert the row value to a consistent java value.
     */
    public Class getFieldClassification(DatabaseField fieldToClassify) throws DescriptorException {
        DatabaseMapping mapping = getMappingForField(fieldToClassify);
        if (mapping == null) {
            // Means that the mapping is read-only or the classification is unknown,
            // this is normally not an issue as the classification is only really used for primary keys
            // and only when the database type can be different and not polymorphic than the object type.
            return null;
        }

        return mapping.getFieldClassification(fieldToClassify);
    }

    /**
     * Return the field used for the query key name.
     */
    public DatabaseField getFieldForQueryKeyName(String name) {
        QueryKey key = this.descriptor.getQueryKeyNamed(name);
        if (key == null) {
            DatabaseMapping mapping = getMappingForAttributeName(name);
            if (mapping == null) {
                return null;
            }
            if (mapping.getFields().isEmpty()) {
                return null;
            }
            return mapping.getFields().get(0);
        }
        if (key.isDirectQueryKey()) {
            return ((DirectQueryKey)key).getField();
        }
        return null;
    }

    /**
     * Return the fields map.
     * Used to maintain identity on the field objects. Ensure they get the correct index/type.
     */
    public Map<DatabaseField, DatabaseField> getFieldsMap() {
        return fieldsMap;
    }

    /**
     * Return the fields map.
     * Used to maintain identity on the field objects. Ensure they get the correct index/type.
     */
    protected void setFieldsMap(Map fieldsMap) {
        this.fieldsMap = fieldsMap;
    }
    
    /**
     * PERF:
     * Return all mappings that require cloning.
     * This allows for simple directs to be avoided when using clone copying.
     */
    public List<DatabaseMapping> getCloningMappings() {
        return cloningMappings;
    }

    /**
     * PERF:
     * Return if the descriptor has no complex mappings, all direct.
     */
    public boolean isSimple() {
        return isSimple;
    }
    
    /**
     * PERF:
     * Return all relationship mappings.
     */
    public List<DatabaseMapping> getRelationshipMappings() {
        return relationshipMappings;
    }
    
    /**
     * PERF:
     * Return all mappings that are eager loaded (but use indirection).
     * This allows for eager mappings to still benefit from indirection for locking and change tracking.
     */
    public List<DatabaseMapping> getEagerMappings() {
        return eagerMappings;
    }

    /**
     * Answers the attributes which are always joined to the original query on reads.
     */
    public List<DatabaseMapping> getJoinedAttributes() {
        return joinedAttributes;
    }

    /**
     * Return the mappings that are always batch fetched.
     */
    public List<DatabaseMapping> getBatchFetchedAttributes() {
        return this.batchFetchedAttributes;
    }

    /**
     * PERF:
     * Return the sequence mapping.
     */
    public AbstractDirectMapping getSequenceMapping() {
        return sequenceMapping;
    }

    /**
     * PERF:
     * Set the sequence mapping.
     */
    public void setSequenceMapping(AbstractDirectMapping sequenceMapping) {
        this.sequenceMapping = sequenceMapping;
    }
    
    /**
     * Answers if any attributes are to be joined / returned in the same select
     * statement.
     */
    public boolean hasJoinedAttributes() {
        return (this.joinedAttributes != null);
    }
    
    /**
     * Return is any mappings are always batch fetched.
     */
    public boolean hasBatchFetchedAttributes() {
        return (this.batchFetchedAttributes != null);
    }
    
    /**
     * Return is any mappings are always batch fetched using IN.
     */
    public boolean hasInBatchFetchedAttribute() {
        return this.hasInBatchFetchedAttribute;
    }
    
    /**
     * Set if any mappings are always batch fetched using IN.
     */
    public void setHasInBatchFetchedAttribute(boolean hasInBatchFetchedAttribute) {
        this.hasInBatchFetchedAttribute = hasInBatchFetchedAttribute;
    }

    /**
     * Return the mapping for the specified attribute name.
     */
    public DatabaseMapping getMappingForAttributeName(String name) {
        return getMappingsByAttribute().get(name);
    }

    /**
     * Return al the mapping for the specified field.
     */
    public DatabaseMapping getMappingForField(DatabaseField field) {
        return getMappingsByField().get(field);
    }

    /**
     * Return all the read-only mapping for the specified field.
     */
    public List<DatabaseMapping> getReadOnlyMappingsForField(DatabaseField field) {
        return getReadOnlyMappingsByField().get(field);
    }

    /**
     * Return all the mapping to attribute associations
     */
    protected Map<String, DatabaseMapping> getMappingsByAttribute() {
        return mappingsByAttribute;
    }

    /**
     * INTERNAL:
     * Return all the mapping to field associations
     */
    public Map<DatabaseField, DatabaseMapping> getMappingsByField() {
        return mappingsByField;
    }

    /**
     * INTERNAL:
     * Return all the read-only mapping to field associations
     */
    public Map<DatabaseField, List<DatabaseMapping>> getReadOnlyMappingsByField() {
        return readOnlyMappingsByField;
    }

    /**
     * Return the non primary key mappings.
     */
    protected List<DatabaseMapping> getNonPrimaryKeyMappings() {
        return nonPrimaryKeyMappings;
    }

    /**
     * Return the base value that is mapped to for given field.
     */
    public Object getParentObjectForField(DatabaseField databaseField, Object domainObject) {
        Object valueIntoObject = domainObject;
        DatabaseMapping mapping = getMappingForField(databaseField);

        // Drill down through the aggregate mappings to get to the direct to field mapping.
        while (mapping.isAggregateObjectMapping()) {
            valueIntoObject = mapping.getAttributeValueFromObject(valueIntoObject);
            mapping = ((AggregateMapping)mapping).getReferenceDescriptor().getObjectBuilder().getMappingForField(databaseField);
        }
        return valueIntoObject;
    }

    /**
     * Return primary key classifications.
     * These are used to ensure a consistent type for the pk values.
     */
    public List<Class> getPrimaryKeyClassifications() {
        if (primaryKeyClassifications == null) {
            List primaryKeyFields = this.descriptor.getPrimaryKeyFields();
            if(null == primaryKeyFields) {
                return Collections.emptyList();
            }
            List<Class> classifications = new ArrayList(primaryKeyFields.size());

            for (int index = 0; index < primaryKeyFields.size(); index++) {
                if (getPrimaryKeyMappings().size() < (index + 1)) { // Check for failed initialization to avoid cascaded errors.
                    classifications.add(null);
                } else {
                    DatabaseMapping mapping = getPrimaryKeyMappings().get(index);
                    DatabaseField field = (DatabaseField)primaryKeyFields.get(index);
                    if (mapping != null) {
                        classifications.add(Helper.getObjectClass(mapping.getFieldClassification(field)));
                    } else {
                        classifications.add(null);
                    }
                }
            }
            primaryKeyClassifications = classifications;
        }
        return primaryKeyClassifications;
    }

    /**
     * Return the primary key expression
     */
    public Expression getPrimaryKeyExpression() {
        return primaryKeyExpression;
    }

    /**
     * Return primary key mappings.
     */
    public List<DatabaseMapping> getPrimaryKeyMappings() {
        return primaryKeyMappings;
    }

    /**
     * INTERNAL: return a database field based on a query key name
     */
    public DatabaseField getTargetFieldForQueryKeyName(String queryKeyName) {
        DatabaseMapping mapping = getMappingForAttributeName(queryKeyName);
        if ((mapping != null) && mapping.isAbstractColumnMapping()) {
            return ((AbstractColumnMapping)mapping).getField();
        }

        //mapping is either null or not direct to field.
        //check query keys
        QueryKey queryKey = this.descriptor.getQueryKeyNamed(queryKeyName);
        if ((queryKey != null) && queryKey.isDirectQueryKey()) {
            return ((DirectQueryKey)queryKey).getField();
        }

        //nothing found
        return null;
    }

    /**
     * Cache all the mappings by their attribute and fields.
     */
    public void initialize(AbstractSession session) throws DescriptorException {
        getMappingsByField().clear();
        getReadOnlyMappingsByField().clear();
        getMappingsByAttribute().clear();
        getCloningMappings().clear();
        getEagerMappings().clear();
        getRelationshipMappings().clear();

        for (Enumeration mappings = this.descriptor.getMappings().elements();
                 mappings.hasMoreElements();) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.nextElement();

            // Add attribute to mapping association
            if (!mapping.isWriteOnly()) {
                getMappingsByAttribute().put(mapping.getAttributeName(), mapping);
            }
            // Cache mappings that require cloning.
            if (mapping.isCloningRequired()) {
                getCloningMappings().add(mapping);
            }
            // Cache eager mappings.
            if (mapping.isForeignReferenceMapping() && ((ForeignReferenceMapping)mapping).usesIndirection() && (!mapping.isLazy())) {
                getEagerMappings().add(mapping);
            }
            
            if (mapping.getReferenceDescriptor() != null && mapping.isCollectionMapping()){
                // only process writable mappings on the defining class in the case of inheritance
                if (getDescriptor() == mapping.getDescriptor()){
                    ((ContainerMapping)mapping).getContainerPolicy().processAdditionalWritableMapKeyFields(session);
                }
            }
            
            // Cache relationship mappings.
            if (!mapping.isAbstractColumnMapping()) {
                getRelationshipMappings().add(mapping);
            }

            // Add field to mapping association
            for (DatabaseField field : mapping.getFields()) {

                if (mapping.isReadOnly()) {
                    List readOnlyMappings = getReadOnlyMappingsByField().get(field);
    
                    if (readOnlyMappings == null) {
                        readOnlyMappings = new ArrayList();
                        getReadOnlyMappingsByField().put(field, readOnlyMappings);
                    }
    
                    readOnlyMappings.add(mapping);
                } else {
                    if (mapping.isAggregateObjectMapping()) {
                        // For Embeddable class, we need to test read-only 
                        // status of individual fields in the embeddable.
                        ObjectBuilder aggregateObjectBuilder = ((AggregateObjectMapping)mapping).getReferenceDescriptor().getObjectBuilder();
                        
                        // Look in the non-read-only fields mapping
                        DatabaseMapping aggregatedFieldMapping = aggregateObjectBuilder.getMappingForField(field);
    
                        if (aggregatedFieldMapping == null) { // mapping must be read-only
                            List readOnlyMappings = getReadOnlyMappingsByField().get(field);
                            
                            if (readOnlyMappings == null) {
                                readOnlyMappings = new ArrayList();
                                getReadOnlyMappingsByField().put(field, readOnlyMappings);
                            }
    
                            readOnlyMappings.add(mapping);
                        } else {
                            getMappingsByField().put(field, mapping);
                        }
                    } else { // Not an embeddable mapping
                        if (getMappingsByField().containsKey(field) || mapping.getDescriptor().getAdditionalWritableMapKeyFields().contains(field)) {  
                            session.getIntegrityChecker().handleError(DescriptorException.multipleWriteMappingsForField(field.toString(), mapping));
                        } else {
                            getMappingsByField().put(field, mapping);
                        }
                    }
                }
            }
        }
        this.isSimple = getRelationshipMappings().isEmpty();

        initializePrimaryKey(session);
        initializeJoinedAttributes();
        initializeBatchFetchedAttributes();
        
        if (this.descriptor.usesSequenceNumbers()) {
            DatabaseMapping sequenceMapping = getMappingForField(this.descriptor.getSequenceNumberField());
            if ((sequenceMapping != null) && sequenceMapping.isDirectToFieldMapping()) {
                setSequenceMapping((AbstractDirectMapping)sequenceMapping);
            }
        }
        if(this.descriptor.usesOptimisticLocking()) {
            DatabaseField lockField = this.descriptor.getOptimisticLockingPolicy().getWriteLockField();
            if (lockField != null) {
                DatabaseMapping lockMapping = getDescriptor().getObjectBuilder().getMappingForField(lockField);
                if (lockMapping != null) {
                    this.lockAttribute = lockMapping.getAttributeName();
                }
            }
        }

    }
    
    public boolean isPrimaryKeyComponentInvalid(Object keyValue, int index) {
        IdValidation idValidation;
        if (index < 0) {
            idValidation = this.descriptor.getIdValidation();
        } else {        
            idValidation = this.descriptor.getPrimaryKeyIdValidations().get(index);
        }
        if (idValidation == IdValidation.ZERO) {
            return keyValue == null || Helper.isEquivalentToNull(keyValue); 
        } else if (idValidation == IdValidation.NULL) {
            return keyValue == null;
        } else if (idValidation == IdValidation.NEGATIVE) {
            return keyValue == null || Helper.isNumberNegativeOrZero(keyValue);
        } else {
            // idValidation == IdValidation.NONE
            return false;
        }
    }
    
    public void recordPrivateOwnedRemovals(Object object, UnitOfWorkImpl uow, boolean initialPass) {
        if (!this.descriptor.isDescriptorTypeAggregate()){
            if (!initialPass && uow.getDeletedObjects().containsKey(object)){
                return;
            }
            uow.getDeletedObjects().put(object, object);
        }
        if (this.descriptor.hasMappingsPostCalculateChanges()){
            for (DatabaseMapping mapping : this.descriptor.getMappingsPostCalculateChanges()){
                mapping.recordPrivateOwnedRemovals(object, uow);
            }
        }
    }

    /**
     * INTERNAL:
     * Post initializations after mappings are initialized.
     */
    public void postInitialize(AbstractSession session) throws DescriptorException {
        // PERF: Cache if needs to unwrap to optimize unwrapping.
        this.hasWrapperPolicy = this.descriptor.hasWrapperPolicy() || session.getProject().hasProxyIndirection();
    }
    
    /**
     * INTERNAL:
     * Iterates through all one to one mappings and checks if any of them use joining.
     * <p>
     * By caching the result query execution in the case where there are no joined
     * attributes can be improved.
     */
    public void initializeJoinedAttributes() {
        // For concurrency don't worry about doing this work twice, just make sure
        // if it happens don't add the same joined attributes twice.
        List<DatabaseMapping> joinedAttributes = null;
        List mappings = this.descriptor.getMappings();
        for (int i = 0; i < mappings.size(); i++) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.get(i);
            if (mapping.isForeignReferenceMapping() && ((ForeignReferenceMapping)mapping).isJoinFetched()) {
                if (joinedAttributes == null) {
                    joinedAttributes = new ArrayList();
                }
                joinedAttributes.add(mapping);
            }
        }
        this.joinedAttributes = joinedAttributes;
    }
    
    /**
     * INTERNAL:
     * Iterates through all one to one mappings and checks if any of them use batch fetching.
     * <p>
     * By caching the result query execution in the case where there are no batch fetched
     * attributes can be improved.
     */
    public void initializeBatchFetchedAttributes() {
        List<DatabaseMapping> batchedAttributes = null;
        for (DatabaseMapping mapping : this.descriptor.getMappings()) {
            if (mapping.isForeignReferenceMapping() && ((ForeignReferenceMapping)mapping).shouldUseBatchReading()) {
                if (batchedAttributes == null) {
                    batchedAttributes = new ArrayList();
                }
                batchedAttributes.add(mapping);
                if (((ForeignReferenceMapping)mapping).getBatchFetchType() == BatchFetchType.IN) {
                    this.hasInBatchFetchedAttribute = true;
                }
            } else if (mapping.isAggregateObjectMapping()) {
                if (mapping.getReferenceDescriptor().getObjectBuilder().hasInBatchFetchedAttribute()) {
                    this.hasInBatchFetchedAttribute = true;                    
                }
            }
        }
        this.batchFetchedAttributes = batchedAttributes;
        
        if (this.hasInBatchFetchedAttribute && this.descriptor.hasInheritance()) {
            ClassDescriptor parent = this.descriptor.getInheritancePolicy().getParentDescriptor();
            while (parent != null) {
                parent.getObjectBuilder().setHasInBatchFetchedAttribute(true);
                parent = parent.getInheritancePolicy().getParentDescriptor();                
            }
        }
    }

    /**
     * Initialize a cache key.  Called by buildObject and now also by
     * buildWorkingCopyCloneFromRow.
     */
    protected void copyQueryInfoToCacheKey(CacheKey cacheKey, ObjectBuildingQuery query, AbstractRecord databaseRow, AbstractSession session, ClassDescriptor concreteDescriptor) {
        //CR #4365 - used to prevent infinite recursion on refresh object cascade all
        cacheKey.setLastUpdatedQueryId(query.getQueryId());

        if (concreteDescriptor.usesOptimisticLocking()) {
            OptimisticLockingPolicy policy = concreteDescriptor.getOptimisticLockingPolicy();
            Object cacheValue = policy.getValueToPutInCache(databaseRow, session);

            //register the object into the IM and set the write lock object
            cacheKey.setWriteLockValue(cacheValue);
        }
        cacheKey.setReadTime(query.getExecutionTime());
    }

    /**
     * Cache primary key and non primary key mappings.
     */
    public void initializePrimaryKey(AbstractSession session) throws DescriptorException {
        List primaryKeyFields = this.descriptor.getPrimaryKeyFields();
        if ((null == primaryKeyFields || primaryKeyFields.isEmpty()) && getDescriptor().isAggregateCollectionDescriptor()) {
            // populate primaryKeys with all mapped fields found in the main table.
            DatabaseTable defaultTable = getDescriptor().getDefaultTable();
            Iterator<DatabaseField> it = getDescriptor().getFields().iterator();
            while(it.hasNext()) {
                DatabaseField field = it.next();
                if(field.getTable().equals(defaultTable) && getMappingsByField().containsKey(field)) {
                    primaryKeyFields.add(field);
                }
            }
            List<DatabaseField> additionalFields = this.descriptor.getAdditionalAggregateCollectionKeyFields();
            for(int i=0; i < additionalFields.size(); i++) {
                DatabaseField additionalField = additionalFields.get(i); 
                if(!primaryKeyFields.contains(additionalField)) {
                    primaryKeyFields.add(additionalField);
                }
            }
        }
        createPrimaryKeyExpression(session);

        if(null != primaryKeyMappings) {
            primaryKeyMappings.clear();
        }
        if(null != nonPrimaryKeyMappings) {
            nonPrimaryKeyMappings.clear();
        }

        // This must be before because the secondary table primary key fields are registered after
        for (Iterator fields = getMappingsByField().keySet().iterator(); fields.hasNext();) {
            DatabaseField field = (DatabaseField)fields.next();
            if (null ==primaryKeyFields || !primaryKeyFields.contains(field)) {
                DatabaseMapping mapping = getMappingForField(field);
                if (nonPrimaryKeyMappings != null && !getNonPrimaryKeyMappings().contains(mapping)) {
                    getNonPrimaryKeyMappings().add(mapping);
                }
            }
        }

        if(null != primaryKeyFields) {
            for (int index = 0; index < primaryKeyFields.size(); index++) {
                DatabaseField primaryKeyField = (DatabaseField)primaryKeyFields.get(index);
                DatabaseMapping mapping = getMappingForField(primaryKeyField);
    
                if (mapping == null) {
                    if(this.descriptor.isDescriptorTypeAggregate()) {
                        this.mayHaveNullInPrimaryKey = true;
                    } else {
                        throw DescriptorException.noMappingForPrimaryKey(primaryKeyField, this.descriptor);
                    }
                }
    
                getPrimaryKeyMappings().add(mapping);
                if (mapping != null) {
                    mapping.setIsPrimaryKeyMapping(true);
                }
    
                // Use the same mapping to map the additional table primary key fields.
                // This is required if someone tries to map to one of these fields.
                if (this.descriptor.hasMultipleTables() && (mapping != null)) {
                    for (Map keyMapping : this.descriptor.getAdditionalTablePrimaryKeyFields().values()) {
                        DatabaseField secondaryField = (DatabaseField) keyMapping.get(primaryKeyField);
    
                        // This can be null in the custom multiple join case
                        if (secondaryField != null) {
                            getMappingsByField().put(secondaryField, mapping);
    
                            if (mapping.isAggregateObjectMapping()) {
                                // GF#1153,1391
                                // If AggregateObjectMapping contain primary keys and the descriptor has multiple tables
                                // AggregateObjectMapping should know the the primary key join columns (secondaryField here)
                                // to handle some cases properly
                                ((AggregateObjectMapping) mapping).addPrimaryKeyJoinField(primaryKeyField, secondaryField);
                            }
                        }
                    }
                }
            }
        }

        // PERF: compute if primary key is mapped through direct mappings,
        // to allow fast extraction.
        boolean hasSimplePrimaryKey = true;
        if(null != primaryKeyMappings) {
            for (int index = 0; index < getPrimaryKeyMappings().size(); index++) {
                DatabaseMapping mapping = getPrimaryKeyMappings().get(index);
    
                // Primary key mapping may be null for aggregate collection.
                if ((mapping == null) || (!mapping.isAbstractColumnMapping())) {
                    hasSimplePrimaryKey = false;
                    break;
                }
            }
        }
        this.descriptor.setHasSimplePrimaryKey(hasSimplePrimaryKey);

        // Set id validation, zero is allowed for composite primary keys.
        boolean wasIdValidationSet = true;
        if (this.descriptor.getIdValidation() == null) {
            wasIdValidationSet = false;
            List<DatabaseField> descriptorPrimaryKeyFields = this.descriptor.getPrimaryKeyFields();
            if (descriptorPrimaryKeyFields != null && descriptorPrimaryKeyFields.size() > 1) {
                this.descriptor.setIdValidation(IdValidation.NULL);
            } else {
                this.descriptor.setIdValidation(IdValidation.ZERO);
            }
        }
        // Initialize id validation per field, default sequence to allowing zero.
        // This defaults to allowing zero for the other fields.
        if (this.descriptor.getPrimaryKeyFields() != null && this.descriptor.getPrimaryKeyIdValidations() == null) {
            this.descriptor.setPrimaryKeyIdValidations(new ArrayList(this.descriptor.getPrimaryKeyFields().size()));
            for (DatabaseField field : this.descriptor.getPrimaryKeyFields()) {
                if (!wasIdValidationSet && this.descriptor.usesSequenceNumbers() && field.equals(this.descriptor.getSequenceNumberField())) {
                    this.descriptor.getPrimaryKeyIdValidations().add(IdValidation.ZERO);
                } else {
                    this.descriptor.getPrimaryKeyIdValidations().add(this.descriptor.getIdValidation());
                }
            }
        }
    }

    /**
     * Returns the clone of the specified object. This is called only from unit of work.
     * This only instantiates the clone instance, it does not clone the attributes,
     * this allows the stub of the clone to be registered before cloning its parts.
     */
    public Object instantiateClone(Object domainObject, AbstractSession session) {
        Object clone = this.descriptor.getCopyPolicy().buildClone(domainObject, session);
        // Clear change tracker.
        if (clone instanceof ChangeTracker) {
            ((ChangeTracker)clone)._persistence_setPropertyChangeListener(null);
        }
        if(clone instanceof FetchGroupTracker) {
            ((FetchGroupTracker)clone)._persistence_setFetchGroup(null);
            ((FetchGroupTracker)clone)._persistence_setSession(null);
        }
        clearPrimaryKey(clone);
        return clone;
    }

    /**
     * Returns the clone of the specified object. This is called only from unit of work.
     * The domainObject sent as parameter is always a copy from the parent of unit of work.
     * bug 2612602 make a call to build a working clone.  This will in turn call the copy policy
     * to make a working clone.  This allows for lighter and heavier clones to
     * be created based on their use.
     * this allows the stub of the clone to be registered before cloning its parts.
     */
    public Object instantiateWorkingCopyClone(Object domainObject, AbstractSession session) {
        return this.descriptor.getCopyPolicy().buildWorkingCopyClone(domainObject, session);
    }

    /**
     * It is now possible to build working copy clones directly from rows.
     * <p>An intermediary original is no longer needed.
     * <p>This has ramifications to the copy policy and cmp, for clones are
     * no longer built via cloning.
     * <p>Instead the copy policy must in some cases not copy at all.
     * this allows the stub of the clone to be registered before cloning its parts.
     */
    public Object instantiateWorkingCopyCloneFromRow(AbstractRecord row, ObjectBuildingQuery query, Object primaryKey, UnitOfWorkImpl unitOfWork) {
        return this.descriptor.getCopyPolicy().buildWorkingCopyCloneFromRow(row, query, primaryKey, unitOfWork);
    }

    public boolean isPrimaryKeyMapping(DatabaseMapping mapping) {
        return getPrimaryKeyMappings().contains(mapping);
    }

    /**
     * INTERNAL:
     * Perform the iteration operation on the objects attributes through the mappings.
     */
    public void iterate(DescriptorIterator iterator) {
        List<DatabaseMapping> mappings;
        // Only iterate on relationships if required.
        if (iterator.shouldIterateOnPrimitives()) {
            mappings = this.descriptor.getMappings();
        } else {
            // PERF: Only process relationships.
            if (this.isSimple) {
                return;
            }
            mappings = this.relationshipMappings;
        }
        int mappingsSize = mappings.size();
        for (int index = 0; index < mappingsSize; index++) {
            mappings.get(index).iterate(iterator);
        }
    }
    
    /**
     * INTERNAL:
     * Merge changes between the objects, this merge algorithm is dependent on the merge manager.
     */
    public void mergeChangesIntoObject(Object target, ObjectChangeSet changeSet, Object source, MergeManager mergeManager, AbstractSession targetSession) {
        mergeChangesIntoObject(target, changeSet, source, mergeManager, targetSession, false, false);
    }
    
    /**
     * INTERNAL:
     * Merge changes between the objects, this merge algorithm is dependent on the merge manager.
     */
    public void mergeChangesIntoObject(Object target, ObjectChangeSet changeSet, Object source, MergeManager mergeManager, AbstractSession targetSession, boolean isTargetCloneOfOriginal, boolean shouldMergeFetchGroup) {
        // PERF: Just merge the object for new objects, as the change set is not populated.
        if ((source != null) && changeSet.isNew() && (!this.descriptor.shouldUseFullChangeSetsForNewObjects())) {
            mergeIntoObject(target,  changeSet, true, source, mergeManager, targetSession, false, isTargetCloneOfOriginal, shouldMergeFetchGroup);
        } else {
            List changes = changeSet.getChanges();
            int size = changes.size();
            for (int index = 0; index < size; index++) {
                ChangeRecord record = (ChangeRecord)changes.get(index);
                //cr 4236, use ObjectBuilder getMappingForAttributeName not the Descriptor one because the
                // ObjectBuilder method is much more efficient.
                DatabaseMapping mapping = getMappingForAttributeName(record.getAttribute());
                mapping.mergeChangesIntoObject(target, record, source, mergeManager, targetSession);                
            }
            // PERF: Avoid events if no listeners.
            // Event is already raised in mergeIntoObject, avoid calling twice.
            if (this.descriptor.getEventManager().hasAnyEventListeners()) {
                DescriptorEvent event = new DescriptorEvent(target);
                event.setSession(mergeManager.getSession());
                event.setOriginalObject(source);
                event.setChangeSet(changeSet);
                event.setEventCode(DescriptorEventManager.PostMergeEvent);
                this.descriptor.getEventManager().executeEvent(event);
            }
        }
        this.descriptor.getCachePolicy().indexObjectInCache(changeSet, target, this.descriptor, targetSession);
    }

    /**
     * INTERNAL:
     * Merge the contents of one object into another, this merge algorithm is dependent on the merge manager.
     * This merge also prevents the extra step of calculating the changes when it is not required.
     */
    public void mergeIntoObject(Object target, boolean isUnInitialized, Object source, MergeManager mergeManager, AbstractSession targetSession) {
        mergeIntoObject(target, null, isUnInitialized, source, mergeManager, targetSession, false, false, false);
    }
    
    /**
     * INTERNAL:
     * Merge the contents of one object into another, this merge algorithm is dependent on the merge manager.
     * This merge also prevents the extra step of calculating the changes when it is not required.
     * If 'cascadeOnly' is true, only foreign reference mappings are merged.
     * If 'isTargetCloneOfOriginal' then the target was create through a shallow clone of the source, so merge basics is not required.
     */
    public void mergeIntoObject(Object target, ObjectChangeSet changeSet, boolean isUnInitialized, Object source, MergeManager mergeManager, AbstractSession targetSession, boolean cascadeOnly, boolean isTargetCloneOfOriginal, boolean shouldMergeFetchGroup) {
        // cascadeOnly is introduced to optimize merge 
        // for GF#1139 Cascade merge operations to relationship mappings even if already registered
        
        FetchGroup sourceFetchGroup = null;
        FetchGroup targetFetchGroup = null;
        if(this.descriptor.hasFetchGroupManager()) {
            sourceFetchGroup = this.descriptor.getFetchGroupManager().getObjectFetchGroup(source);
            targetFetchGroup = this.descriptor.getFetchGroupManager().getObjectFetchGroup(target);
            if(targetFetchGroup != null) {
                if(!targetFetchGroup.isSupersetOf(sourceFetchGroup)) {
                    targetFetchGroup.onUnfetchedAttribute((FetchGroupTracker)target, null);
                }
            } else if (shouldMergeFetchGroup && sourceFetchGroup != null){
                    this.descriptor.getFetchGroupManager().setObjectFetchGroup(target, sourceFetchGroup, targetSession);
            }
        }
        // PERF: Avoid synchronized enumerator as is concurrency bottleneck.
        List<DatabaseMapping> mappings = this.descriptor.getMappings();
        int size = mappings.size();
        for (int index = 0; index < size; index++) {
            DatabaseMapping mapping = mappings.get(index);
            if (((!cascadeOnly && !isTargetCloneOfOriginal) 
                    || (cascadeOnly && mapping.isForeignReferenceMapping())
                    || (isTargetCloneOfOriginal && mapping.isCloningRequired()))
                  && (sourceFetchGroup == null || sourceFetchGroup.containsAttributeInternal(mapping.getAttributeName()))) {
                mapping.mergeIntoObject(target, isUnInitialized, source, mergeManager, targetSession);
            }
        }

        // PERF: Avoid events if no listeners.
        if (this.descriptor.getEventManager().hasAnyEventListeners()) {
            DescriptorEvent event = new DescriptorEvent(target);
            event.setSession(mergeManager.getSession());
            event.setOriginalObject(source);
            event.setChangeSet(changeSet);
            event.setEventCode(DescriptorEventManager.PostMergeEvent);
            this.descriptor.getEventManager().executeEvent(event);
        }
    }

    /**
     * Clones the attributes of the specified object. This is called only from unit of work.
     * The domainObject sent as parameter is always a copy from the parent of unit of work.
     */
    public void populateAttributesForClone(Object original, CacheKey cacheKey, Object clone, Integer refreshCascade, AbstractSession cloningSession) {
        List mappings = getCloningMappings();
        int size = mappings.size();
        if (this.descriptor.hasFetchGroupManager() && this.descriptor.getFetchGroupManager().isPartialObject(original)) {
            FetchGroupManager fetchGroupManager = this.descriptor.getFetchGroupManager();
            for (int index = 0; index < size; index++) {
                DatabaseMapping mapping = (DatabaseMapping)mappings.get(index);
                if (fetchGroupManager.isAttributeFetched(original, mapping.getAttributeName())) {
                    mapping.buildClone(original, cacheKey, clone, refreshCascade, cloningSession);
                }
            }
        } else {
            for (int index = 0; index < size; index++) {
                ((DatabaseMapping)mappings.get(index)).buildClone(original, cacheKey, clone, refreshCascade, cloningSession);
            }
        }

        // PERF: Avoid events if no listeners.
        if (this.descriptor.getEventManager().hasAnyEventListeners()) {
            DescriptorEvent event = new DescriptorEvent(clone);
            event.setSession(cloningSession);
            event.setOriginalObject(original);
            event.setDescriptor(descriptor);
            event.setEventCode(DescriptorEventManager.PostCloneEvent);
            cloningSession.deferEvent(event);
        }
    }
    
    protected void loadBatchReadAttributes(ClassDescriptor concreteDescriptor, Object sourceObject, CacheKey cacheKey, AbstractRecord databaseRow, ObjectBuildingQuery query, JoinedAttributeManager joinManager, boolean isTargetProtected){
        List batchExpressions = ((ReadAllQuery)query).getBatchReadAttributeExpressions();
        int size = batchExpressions.size();
        for (int index = 0; index < size; index++) {
            QueryKeyExpression queryKeyExpression = (QueryKeyExpression)batchExpressions.get(index);
            // Only worry about immediate attributes.
            if (queryKeyExpression.getBaseExpression().isExpressionBuilder()) {
                DatabaseMapping mapping = getMappingForAttributeName(queryKeyExpression.getName());

                if (mapping == null) {
                    throw ValidationException.missingMappingForAttribute(concreteDescriptor, queryKeyExpression.getName(), this.toString());
                } else {
                    // Bug 4230655 - do not replace instantiated valueholders.
                    Object attributeValue = mapping.getAttributeValueFromObject(sourceObject);
                    if ((attributeValue != null) && mapping.isForeignReferenceMapping() && ((ForeignReferenceMapping)mapping).usesIndirection() && (!((ForeignReferenceMapping)mapping).getIndirectionPolicy().objectIsInstantiated(attributeValue))) {
                        mapping.readFromRowIntoObject(databaseRow, joinManager, sourceObject, cacheKey, query, query.getExecutionSession(),isTargetProtected);
                    }
                }
            }
        }
    }

    protected void loadJoinedAttributes(ClassDescriptor concreteDescriptor, Object sourceObject, CacheKey cacheKey, AbstractRecord databaseRow, JoinedAttributeManager joinManager, ObjectBuildingQuery query, boolean isTargetProtected){
        List joinExpressions = joinManager.getJoinedAttributeExpressions();
        int size = joinExpressions.size();
        for (int index = 0; index < size; index++) {
            QueryKeyExpression queryKeyExpression = (QueryKeyExpression)joinExpressions.get(index);
            QueryKeyExpression baseExpression = (QueryKeyExpression)joinManager.getJoinedAttributes().get(index);
            DatabaseMapping mapping = joinManager.getJoinedAttributeMappings().get(index);
            
            // Only worry about immediate (excluding aggregates) foreign reference mapping attributes.
            if (queryKeyExpression == baseExpression) {
                //get the intermediate objects between this expression node and the base builder
                Object intermediateValue = joinManager.getValueFromObjectForExpression(query.getExecutionSession(), sourceObject, (ObjectExpression)baseExpression.getBaseExpression());
                if (mapping == null) {
                    throw ValidationException.missingMappingForAttribute(concreteDescriptor, queryKeyExpression.getName(), toString());
                } else {
                    // Bug 4230655 - do not replace instantiated valueholders.
                    Object attributeValue = mapping.getAttributeValueFromObject(intermediateValue);
                    if ((attributeValue != null) && mapping.isForeignReferenceMapping() && ((ForeignReferenceMapping)mapping).usesIndirection() && (!((ForeignReferenceMapping)mapping).getIndirectionPolicy().objectIsInstantiated(attributeValue))) {
                        mapping.readFromRowIntoObject(databaseRow, joinManager, intermediateValue, cacheKey, query, query.getExecutionSession(), isTargetProtected);
                    }
                }
            }
        }
    }
    /**
     * This method is called when a cached Entity needs to be refreshed
     */
    protected boolean refreshObjectIfRequired(ClassDescriptor concreteDescriptor, CacheKey cacheKey, Object domainObject, ObjectBuildingQuery query, JoinedAttributeManager joinManager, AbstractRecord databaseRow, AbstractSession session, boolean targetIsProtected){
        boolean cacheHit = true;
        //cached object might be partially fetched, only refresh the fetch group attributes of the query if
        //the cached partial object is not invalidated and does not contain all data for the fetch group.   
        if (concreteDescriptor.hasFetchGroupManager() && concreteDescriptor.getFetchGroupManager().isPartialObject(domainObject)) {
            cacheHit = false;
            //only ObjectLevelReadQuery and above support partial objects
            revertFetchGroupData(domainObject, concreteDescriptor, cacheKey, (query), joinManager, databaseRow, session, targetIsProtected);
        } else {
            boolean refreshRequired = true;
            if (concreteDescriptor.usesOptimisticLocking()) {
                OptimisticLockingPolicy policy = concreteDescriptor.getOptimisticLockingPolicy();
                Object cacheValue = policy.getValueToPutInCache(databaseRow, session);
                if (concreteDescriptor.getCachePolicy().shouldOnlyRefreshCacheIfNewerVersion()) {
                    refreshRequired = policy.isNewerVersion(databaseRow, domainObject, cacheKey.getKey(), session);
                    if (!refreshRequired) {
                        cacheKey.setReadTime(query.getExecutionTime());
                    }
                }
                if (refreshRequired) {
                    // Update the write lock value.
                    cacheKey.setWriteLockValue(cacheValue);
                }
            }
            if (refreshRequired) {
                cacheHit = false;
                // CR #4365 - used to prevent infinite recursion on refresh object cascade all.
                cacheKey.setLastUpdatedQueryId(query.getQueryId());
                // Bug 276362 - set the CacheKey's read time (re-validating the CacheKey) before buildAttributesIntoObject is called
                cacheKey.setReadTime(query.getExecutionTime());
                concreteDescriptor.getObjectBuilder().buildAttributesIntoObject(domainObject, cacheKey, databaseRow, query, joinManager, true, session);
            }
        }
        return cacheHit;
    }

    /**
     * Rehash any maps based on fields.
     * This is used to clone descriptors for aggregates, which hammer field names,
     * it is probably better not to hammer the field name and this should be refactored.
     */
    public void rehashFieldDependancies(AbstractSession session) {
        setMappingsByField(Helper.rehashMap(getMappingsByField()));
        setReadOnlyMappingsByField(Helper.rehashMap(getReadOnlyMappingsByField()));
        setFieldsMap(Helper.rehashMap(getFieldsMap()));
        setPrimaryKeyMappings(new ArrayList(2));
        setNonPrimaryKeyMappings(new ArrayList(2));
        initializePrimaryKey(session);
    }

    /**
     * Set the descriptor.
     */
    public void setDescriptor(ClassDescriptor aDescriptor) {
        descriptor = aDescriptor;
    }

    /**
     * All the mappings and their respective attribute associations are cached for performance improvement.
     */
    protected void setMappingsByAttribute(Map<String, DatabaseMapping> theAttributeMappings) {
        mappingsByAttribute = theAttributeMappings;
    }

    /**
     * INTERNAL:
     * All the mappings and their respective field associations are cached for performance improvement.
     */
    public void setMappingsByField(Map<DatabaseField, DatabaseMapping> theFieldMappings) {
        mappingsByField = theFieldMappings;
    }

    /**
     * INTERNAL:
     * All the read-only mappings and their respective field associations are cached for performance improvement.
     */
    public void setReadOnlyMappingsByField(Map<DatabaseField, List<DatabaseMapping>> theReadOnlyFieldMappings) {
        readOnlyMappingsByField = theReadOnlyFieldMappings;
    }

    /**
     * The non primary key mappings are cached to improve performance.
     */
    protected void setNonPrimaryKeyMappings(List<DatabaseMapping> theNonPrimaryKeyMappings) {
        nonPrimaryKeyMappings = theNonPrimaryKeyMappings;
    }

    /**
     * INTERNAL:
     * Set primary key classifications.
     * These are used to ensure a consistent type for the pk values.
     */
    public void setPrimaryKeyClassifications(List<Class> primaryKeyClassifications) {
        this.primaryKeyClassifications = primaryKeyClassifications;
    }

    /**
     * The primary key expression is cached to improve performance.
     */
    public void setPrimaryKeyExpression(Expression criteria) {
        primaryKeyExpression = criteria;
    }

    /**
     * The primary key mappings are cached to improve performance.
     */
    protected void setPrimaryKeyMappings(List<DatabaseMapping> thePrimaryKeyMappings) {
        primaryKeyMappings = thePrimaryKeyMappings;
    }

    public String toString() {
        return Helper.getShortClassName(getClass()) + "(" + this.descriptor.toString() + ")";
    }

    /**
     * Unwrap the object if required.
     * This is used for the wrapper policy support and EJB.
     */
    public Object unwrapObject(Object proxy, AbstractSession session) {
        if (!this.hasWrapperPolicy) {
            return proxy;
        }
        if (proxy == null) {
            return null;
        }
        // PERF: Using direct variable access.

        // Check if already unwrapped.
        if ((!this.descriptor.hasWrapperPolicy()) || (this.descriptor.getJavaClass() == proxy.getClass()) || (!this.descriptor.getWrapperPolicy().isWrapped(proxy))) {
            if (session.getProject().hasProxyIndirection()) {
                //Bug#3947714  Check and trigger the proxy here
                return ProxyIndirectionPolicy.getValueFromProxy(proxy);
            }
            return proxy;
        }

        // Allow for inheritance, the concrete wrapper must always be used.
        if (this.descriptor.hasInheritance() && (this.descriptor.getInheritancePolicy().hasChildren())) {
            ClassDescriptor descriptor = session.getDescriptor(proxy);
            if (descriptor != this.descriptor) {
                return descriptor.getObjectBuilder().unwrapObject(proxy, session);
            }
        }

        return this.descriptor.getWrapperPolicy().unwrapObject(proxy, session);        
    }

    /**
     * INTERNAL:
     * Used to updated any attributes that may be cached on a woven entity
     */
    public void updateCachedAttributes(PersistenceEntity persistenceEntity, CacheKey cacheKey, Object primaryKey){
        persistenceEntity._persistence_setCacheKey(cacheKey);
        persistenceEntity._persistence_setId(primaryKey);
    }
    
    /**
     * Validates the object builder. This is done once the object builder initialized and descriptor
     * fires this validation.
     */
    public void validate(AbstractSession session) throws DescriptorException {
        if (this.descriptor.usesSequenceNumbers()) {
            if (getMappingForField(this.descriptor.getSequenceNumberField()) == null) {
                throw DescriptorException.mappingForSequenceNumberField(this.descriptor);
            }
        }
    }

    /**
     * Verify that an object has been deleted from the database.
     * An object can span multiple tables.  A query is performed on each of
     * these tables using the primary key values of the object as the selection
     * criteria.  If the query returns a result then the object has not been
     * deleted from the database.  If no result is returned then each of the
     * mappings is asked to verify that the object has been deleted. If all mappings
     * answer true then the result is true.
     */
    public boolean verifyDelete(Object object, AbstractSession session) {
        AbstractRecord translationRow = buildRowForTranslation(object, session);

        // If a call is used generated SQL cannot be executed, the call must be used.
        if ((this.descriptor.getQueryManager().getReadObjectQuery() != null) && this.descriptor.getQueryManager().getReadObjectQuery().isCallQuery()) {
            Object result = session.readObject(object);
            if (result != null) {
                return false;
            }
        } else {
            for (Enumeration tables = this.descriptor.getTables().elements();
                     tables.hasMoreElements();) {
                DatabaseTable table = (DatabaseTable)tables.nextElement();

                SQLSelectStatement sqlStatement = new SQLSelectStatement();
                sqlStatement.addTable(table);
                if (table == this.descriptor.getTables().firstElement()) {
                    sqlStatement.setWhereClause((Expression)getPrimaryKeyExpression().clone());
                } else {
                    sqlStatement.setWhereClause(buildPrimaryKeyExpression(table));
                }
                DatabaseField all = new DatabaseField("*");
                all.setTable(table);
                sqlStatement.addField(all);
                sqlStatement.normalize(session, null);

                DataReadQuery dataReadQuery = new DataReadQuery();
                dataReadQuery.setSQLStatement(sqlStatement);
                dataReadQuery.setSessionName(this.descriptor.getSessionName());

                // execute the query and check if there is a valid result
                List queryResults = (List)session.executeQuery(dataReadQuery, translationRow);
                if (!queryResults.isEmpty()) {
                    return false;
                }
            }
        }

        // now ask each of the mappings to verify that the object has been deleted.
        for (Enumeration mappings = this.descriptor.getMappings().elements();
                 mappings.hasMoreElements();) {
            DatabaseMapping mapping = (DatabaseMapping)mappings.nextElement();

            if (!mapping.verifyDelete(object, session)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Return if the descriptor has a wrapper policy.
     * Cache for performance.
     */
    public boolean hasWrapperPolicy() {
        return hasWrapperPolicy;
    }

    /**
     * Set if the descriptor has a wrapper policy.
     * Cached for performance.
     */
    public void setHasWrapperPolicy(boolean hasWrapperPolicy) {
        this.hasWrapperPolicy = hasWrapperPolicy;
    }
    
    /**
     * Wrap the object if required.
     * This is used for the wrapper policy support and EJB.
     */
    public Object wrapObject(Object implementation, AbstractSession session) {
        if (!this.hasWrapperPolicy) {
            return implementation;
        }
        if (implementation == null) {
            return null;
        }
        // PERF: Using direct variable access.
        
        // Check if already wrapped.
        if ((!this.descriptor.hasWrapperPolicy()) || this.descriptor.getWrapperPolicy().isWrapped(implementation)) {
            return implementation;
        }

        // Allow for inheritance, the concrete wrapper must always be used.
        if (this.descriptor.hasInheritance() && this.descriptor.getInheritancePolicy().hasChildren() && (implementation.getClass() != this.descriptor.getJavaClass())) {
            ClassDescriptor descriptor = session.getDescriptor(implementation);
            if (descriptor != this.descriptor) {
                return descriptor.getObjectBuilder().wrapObject(implementation, session);
            }
        }
        return this.descriptor.getWrapperPolicy().wrapObject(implementation, session);
    }
    
    public boolean isXMLObjectBuilder() {
        return false;
    }
    
    public String getLockAttribute() {
        return this.lockAttribute;
    }
}
