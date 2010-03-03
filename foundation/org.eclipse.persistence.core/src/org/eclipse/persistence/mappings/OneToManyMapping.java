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
 ******************************************************************************/  
package org.eclipse.persistence.mappings;

import java.util.*;

import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.expressions.*;
import org.eclipse.persistence.internal.helper.*;
import org.eclipse.persistence.internal.identitymaps.*;
import org.eclipse.persistence.internal.queries.*;
import org.eclipse.persistence.internal.sessions.*;
import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.internal.descriptors.CascadeLockingPolicy;
import org.eclipse.persistence.internal.descriptors.ObjectBuilder;
import org.eclipse.persistence.internal.expressions.SQLUpdateStatement;
import org.eclipse.persistence.mappings.foundation.MapComponentMapping;

/**
 * <p><b>Purpose</b>: This mapping is used to represent the
 * typical RDBMS relationship between a single
 * source object and collection of target objects; where,
 * on the database, the target objects have references
 * (foreign keys) to the source object.
 *
 * @author Sati
 * @since TOPLink/Java 1.0
 */
public class OneToManyMapping extends CollectionMapping implements RelationalMapping, MapComponentMapping {

    /** Used for data modification events. */
    protected static final String PostInsert = "postInsert";
    protected static final String ObjectRemoved = "objectRemoved";
    protected static final String ObjectAdded = "objectAdded";
    
    /** The target foreign key fields that reference the sourceKeyFields. */
    protected transient Vector<DatabaseField> targetForeignKeyFields;

    /** The (typically primary) source key fields that are referenced by the targetForeignKeyFields. */
    protected transient Vector<DatabaseField> sourceKeyFields;

    /** This maps the target foreign key fields to the corresponding (primary) source key fields. */
    protected transient Map<DatabaseField, DatabaseField> targetForeignKeysToSourceKeys;
    
    /** This maps the (primary) source key fields to the corresponding target foreign key fields. */
    protected transient Map<DatabaseField, DatabaseField> sourceKeysToTargetForeignKeys;

    /** All targetForeignKeyFields should have the same table.
     *  Used only in case data modification events required.
     **/
    protected transient DatabaseTable targetForeignKeyTable;

    /** Primary keys of targetForeignKeyTable:
     *  the same as referenceDescriptor().getPrimaryKeyFields() in case the table is default table of reference descriptor;
     *  otherwise contains secondary table's primary key fields in the same order as default table primary keys mapped to them.  
     *  Used only in case data modification events required.
     **/
    protected transient List<DatabaseField> targetPrimaryKeyFields;

    /** 
     * Query used to update a single target row setting its foreign key to point to the source. 
     * Run once for each target added to the source.
     * Example: 
     *   for Employee with managedEmployees attribute mapped with UnidirectionalOneToMany
     *   the query looks like:
     *   UPDATE EMPLOYEE SET MANAGER_ID = 1 WHERE (EMP_ID = 2)
     *   where 1 is id of the source, and 2 is the id of the target to be added.  
     *  Used only in case data modification events required.
     **/
    protected transient DataModifyQuery addTargetQuery;
    protected transient boolean hasCustomAddTargetQuery;
    
    /** 
     * Query used to update a single target row changing its foreign key value from the one pointing to the source to null. 
     * Run once for each target removed from the source. 
     * Example: 
     *   for Employee with managedEmployees attribute mapped with UnidirectionalOneToMany
     *   the query looks like:
     *   UPDATE EMPLOYEE SET MANAGER_ID = null WHERE ((MANAGER_ID = 1) AND (EMP_ID = 2))
     *   where 1 is id of the source, and 2 is the id of the target to be removed.  
     *  Used only in case data modification events required.
     **/
    protected transient DataModifyQuery removeTargetQuery;
    protected transient boolean hasCustomRemoveTargetQuery;

    /** 
     * Query used to update all target rows changing target foreign key value from the one pointing to the source to null. 
     * Run before the source object is deleted.
     * Example: 
     *   for Employee with managedEmployees attribute mapped with UnidirectionalOneToMany
     *   the query looks like:
     *   UPDATE EMPLOYEE SET MANAGER_ID = null WHERE (MANAGER_ID = 1)
     *   where 1 is id of the source to be deleted.  
     *  Used only in case data modification events required.
     **/
    protected transient DataModifyQuery removeAllTargetsQuery;
    protected transient boolean hasCustomRemoveAllTargetsQuery;
    
    /**
     * PUBLIC:
     * Default constructor.
     */
    public OneToManyMapping() {
        super();

        this.targetForeignKeysToSourceKeys = new HashMap(2);
        this.sourceKeysToTargetForeignKeys = new HashMap(2);
        
        this.sourceKeyFields = org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance(1);
        this.targetForeignKeyFields = org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance(1);

        this.deleteAllQuery = new DeleteAllQuery();
        this.removeTargetQuery = new DataModifyQuery();
        this.removeAllTargetsQuery = new DataModifyQuery();
        
        this.isListOrderFieldSupported = true;
    }

    /**
     * INTERNAL:
     */
    public boolean isRelationalMapping() {
        return true;
    }

    /**
     * INTERNAL:
     * Add the associated fields to the appropriate collections.
     */
    public void addTargetForeignKeyField(DatabaseField targetForeignKeyField, DatabaseField sourceKeyField) {
        getTargetForeignKeyFields().addElement(targetForeignKeyField);
        getSourceKeyFields().addElement(sourceKeyField);
    }

    /**
     * PUBLIC:
     * Define the target foreign key relationship in the one-to-many mapping.
     * This method is used for composite target foreign key relationships.
     * That is, the target object's table has multiple foreign key fields
     * that are references to
     * the source object's (typically primary) key fields.
     * Both the target foreign key field name and the corresponding
     * source primary key field name must be specified.
     * Because the target object's table must store a foreign key to the source table,
     * the target object must map that foreign key, this is normally done through a
     * one-to-one mapping back-reference. Other options include:
     * <ul>
     * <li> use a DirectToFieldMapping and maintain the
     * foreign key fields directly in the target
     * <li> use a ManyToManyMapping
     * <li> use an AggregateCollectionMapping
     * </ul>
     * @see DirectToFieldMapping
     * @see ManyToManyMapping
     * @see AggregateCollectionMapping
     */
    public void addTargetForeignKeyFieldName(String targetForeignKeyFieldName, String sourceKeyFieldName) {
        addTargetForeignKeyField(new DatabaseField(targetForeignKeyFieldName), new DatabaseField(sourceKeyFieldName));
    }

    /**
     * INTERNAL:
     * Verifies listOrderField's table: it must be the same table that contains all target foreign keys.
     * Precondition: listOrderField != null.
     */
    protected void buildListOrderField() {
        if(this.listOrderField.hasTableName()) {
            if(!this.targetForeignKeyTable.equals(this.listOrderField.getTable())) {
                throw DescriptorException.listOrderFieldTableIsWrong(this.getDescriptor(), this, this.listOrderField.getTable(), this.targetForeignKeyTable);
            }
        } else {
            listOrderField.setTable(this.targetForeignKeyTable);
        }
        this.listOrderField = this.getReferenceDescriptor().buildField(this.listOrderField, this.targetForeignKeyTable);
    }
    
    /**
     * The selection criteria are created with target foreign keys and source "primary" keys.
     * These criteria are then used to read the target records from the table.
     * These criteria are also used as the default "delete all" criteria.
     *
     * CR#3922 - This method is almost the same as buildSelectionCriteria() the difference
     * is that TargetForeignKeysToSourceKeys contains more information after login then SourceKeyFields
     * contains before login.
     */
    protected Expression buildDefaultSelectionCriteriaAndAddFieldsToQuery() {
        Expression selectionCriteria = null;
        Expression builder = new ExpressionBuilder();

        for (Iterator keys = getTargetForeignKeysToSourceKeys().keySet().iterator();
                 keys.hasNext();) {
            DatabaseField targetForeignKey = (DatabaseField)keys.next();
            DatabaseField sourceKey = getTargetForeignKeysToSourceKeys().get(targetForeignKey);

            Expression partialSelectionCriteria = builder.getField(targetForeignKey).equal(builder.getParameter(sourceKey));
            selectionCriteria = partialSelectionCriteria.and(selectionCriteria);
        }
        getContainerPolicy().addAdditionalFieldsToQuery(getSelectionQuery(), builder);

        return selectionCriteria;
    }
    
    /**
     * This method would allow customers to get the potential selection criteria for a mapping
     * prior to initialization.  This would allow them to more easily create an amendment method
     * that would amend the SQL for the join.
     *
     * CR#3922 - This method is almost the same as buildDefaultSelectionCriteria() the difference
     * is that TargetForeignKeysToSourceKeys contains more information after login then SourceKeyFields
     * contains before login.
     */
    public Expression buildSelectionCriteria() {
        //CR3922	
        Expression selectionCriteria = null;
        Expression builder = new ExpressionBuilder();

        Enumeration sourceKeys = getSourceKeyFields().elements();
        for (Enumeration targetForeignKeys = getTargetForeignKeyFields().elements();
                 targetForeignKeys.hasMoreElements();) {
            DatabaseField targetForeignKey = (DatabaseField)targetForeignKeys.nextElement();
            DatabaseField sourceKey = (DatabaseField)sourceKeys.nextElement();
            Expression partialSelectionCriteria = builder.getField(targetForeignKey).equal(builder.getParameter(sourceKey));
            selectionCriteria = partialSelectionCriteria.and(selectionCriteria);
        }
        return selectionCriteria;
    }

    /**
     * INTERNAL:
     * Clone the appropriate attributes.
     */
    public Object clone() {
        OneToManyMapping clone = (OneToManyMapping)super.clone();
        clone.setTargetForeignKeysToSourceKeys(new HashMap(getTargetForeignKeysToSourceKeys()));
        
        if (addTargetQuery != null){
            clone.addTargetQuery = (DataModifyQuery) this.addTargetQuery.clone();
        }
        clone.removeTargetQuery = (DataModifyQuery) this.removeTargetQuery.clone();
        clone.removeAllTargetsQuery = (DataModifyQuery) this.removeAllTargetsQuery.clone();
        
        return clone;
    }

    /**
     * INTERNAL
     * Called when a DatabaseMapping is used to map the key in a collection.  Returns the key.
     */
    public Object createMapComponentFromRow(AbstractRecord dbRow, ObjectBuildingQuery query, AbstractSession session){
        return session.executeQuery(getSelectionQuery(), dbRow);
    }
    
    /**
     * Delete all the reference objects with a single query.
     */
    protected void deleteAll(DeleteObjectQuery query) throws DatabaseException {
        Object attribute = getAttributeAccessor().getAttributeValueFromObject(query.getObject()); 
        if(usesIndirection()) {
           if(attribute == null || !getIndirectionPolicy().objectIsInstantiated(attribute)) {
               // An empty Vector indicates to DeleteAllQuery that no objects should be removed from cache
               ((DeleteAllQuery)getDeleteAllQuery()).executeDeleteAll(query.getSession().getSessionForClass(getReferenceClass()), query.getTranslationRow(), new Vector(0));
               return;
           }
        }
        Object referenceObjects = this.getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());
        ((DeleteAllQuery)getDeleteAllQuery()).executeDeleteAll(query.getSession().getSessionForClass(getReferenceClass()), query.getTranslationRow(), getContainerPolicy().vectorFor(referenceObjects, query.getSession()));
     }

    /**
     *    This method will make sure that all the records privately owned by this mapping are
     * actually removed. If such records are found then those are all read and removed one
     * by one along with their privately owned parts.
     */
    protected void deleteReferenceObjectsLeftOnDatabase(DeleteObjectQuery query) throws DatabaseException, OptimisticLockException {
        Object objects = readPrivateOwnedForObject(query);

        // Delete all these object one by one.
        ContainerPolicy cp = getContainerPolicy();
        for (Object iter = cp.iteratorFor(objects); cp.hasNext(iter);) {
            query.getSession().deleteObject(cp.next(iter, query.getSession()));
        }
    }

    /**
     * We need to execute the batch query and store the
     * results in a hash table keyed by the (primary) keys
     * of the source objects.
     */
    protected Map executeBatchQuery(DatabaseQuery query, AbstractSession session, AbstractRecord row) {
        ContainerPolicy mappingCP = getContainerPolicy();
        ContainerPolicy queryCP = ((ReadAllQuery)query).getContainerPolicy();
        Object queryResult = null;

        queryResult = session.executeQuery(query, row);

        Map batchedObjects = new Hashtable(queryCP.sizeFor(queryResult));

        for (Object iter = queryCP.iteratorFor(queryResult); queryCP.hasNext(iter);) {
            Object eachReferenceObject = queryCP.next(iter, session);
            Object eachForeignKey = extractForeignKeyFromReferenceObject(eachReferenceObject, session);

            Object container = batchedObjects.get(eachForeignKey);
            if (container == null) {
                container = mappingCP.containerInstance();
                batchedObjects.put(eachForeignKey, container);
            }
            mappingCP.addInto(eachReferenceObject, container, session);
        }
        return batchedObjects;
    }

    /**
     * Extract the foreign key value from the reference object.
     * Used for batch reading. Keep the fields in the same order
     * as in the targetForeignKeysToSourceKeys map.
     */
    protected Object extractForeignKeyFromReferenceObject(Object object, AbstractSession session) {
        int size = this.sourceKeyFields.size();
        Object[] foreignKey = new Object[size];
        ConversionManager conversionManager = session.getDatasourcePlatform().getConversionManager();
        ObjectBuilder builder = getReferenceDescriptor().getObjectBuilder();

        for (int index = 0; index < size; index++) {
            DatabaseField sourceField = this.sourceKeyFields.get(index);
            DatabaseField targetField = this.targetForeignKeyFields.get(index);
            if (object == null) {
                foreignKey[index] = null;
            } else {
                Object value = builder.extractValueFromObjectForField(object, targetField, session);

                //CR:somenewsgroupbug need to ensure source and target types match.
                try {
                    value = conversionManager.convertObject(value, builder.getFieldClassification(sourceField));
                } catch (ConversionException e) {
                    throw ConversionException.couldNotBeConverted(this, getDescriptor(), e);
                }

                foreignKey[index] = value;
            }
        }
        return new CacheId(foreignKey);
    }

    /**
     * INTERNAL:
     * Extract the source primary key value from the target row.
     * Used for batch reading, most following same order and fields as in the mapping.
     */
    protected Object extractKeyFromTargetRow(AbstractRecord row, AbstractSession session) {
        int size = this.sourceKeyFields.size();
        Object[] key = new Object[size];
        ConversionManager conversionManager = session.getDatasourcePlatform().getConversionManager();

        for (int index = 0; index < size; index++) {
            DatabaseField targetField = this.targetForeignKeyFields.get(index);
            DatabaseField sourceField = this.sourceKeyFields.get(index);
            Object value = row.get(targetField);

            // Must ensure the classification gets a cache hit.
            try {
                value = conversionManager.convertObject(value, sourceField.getType());
            } catch (ConversionException e) {
                throw ConversionException.couldNotBeConverted(this, getDescriptor(), e);
            }

            key[index] = value;
        }

        return new CacheId(key);
    }

    /**
     * Extract the key field values from the specified row.
     * Used for batch reading. Keep the fields in the same order
     * as in the targetForeignKeysToSourceKeys hashtable.
     */
    protected Object extractPrimaryKeyFromRow(AbstractRecord row, AbstractSession session) {
        int size = this.sourceKeyFields.size();
        Object[] key = new Object[size];
        ConversionManager conversionManager = session.getDatasourcePlatform().getConversionManager();

        for (int index = 0; index < size; index++) {
            DatabaseField sourceField = this.sourceKeyFields.get(index);
            Object value = row.get(sourceField);

            // Must ensure the classification to get a cache hit.
            try {
                value = conversionManager.convertObject(value, sourceField.getType());
            } catch (ConversionException e) {
                throw ConversionException.couldNotBeConverted(this, this.descriptor, e);
            }

            key[index] = value;
        }
        
        return new CacheId(key);
    }

    /**
     * INTERNAL:
     * Extract the value from the batch optimized query.
     */
    @Override
    public Object extractResultFromBatchQuery(DatabaseQuery query, AbstractRecord row, AbstractSession session, AbstractRecord argumentRow) {
        if (((ReadAllQuery)query).shouldIncludeData()) {
            return super.extractResultFromBatchQuery(query, row, session, argumentRow);
        }
        //this can be null, because either one exists in the query or it will be created
        Map<Object, Object> batchedObjects = null;
        synchronized (query) {
            batchedObjects = getBatchReadObjects(query, session);
            if (batchedObjects == null) {
                batchedObjects = executeBatchQuery(query, session, argumentRow);
                setBatchReadObjects(batchedObjects, query, session);
                query.setSession(null);
            }
        }
        Object result = batchedObjects.get(extractPrimaryKeyFromRow(row, session));

        // The source object might not have any target objects
        if (result == null) {
            return getContainerPolicy().containerInstance();
        } else {
            return result;
        }
    }

    /**
     * PUBLIC:
     * Return the source key field names associated with the mapping.
     * These are in-order with the targetForeignKeyFieldNames.
     */
    public Vector getSourceKeyFieldNames() {
        Vector fieldNames = new Vector(getSourceKeyFields().size());
        for (Enumeration fieldsEnum = getSourceKeyFields().elements();
                 fieldsEnum.hasMoreElements();) {
            fieldNames.addElement(((DatabaseField)fieldsEnum.nextElement()).getQualifiedName());
        }

        return fieldNames;
    }

    /**
     * INTERNAL:
     * Return the source key fields.
     */
    public Vector<DatabaseField> getSourceKeyFields() {
        return sourceKeyFields;
    }
    
    /**
     * INTERNAL:
     * Return the source/target key fields.
     */
    public Map<DatabaseField, DatabaseField> getSourceKeysToTargetForeignKeys() {
        return sourceKeysToTargetForeignKeys;
    }

    /**
     * INTERNAL:
     * Primary keys of targetForeignKeyTable.
     */
    public List<DatabaseField> getTargetPrimaryKeyFields() {
        return this.targetPrimaryKeyFields;
    }
    
    /**
     * INTERNAL:
     * Return the target foreign key field names associated with the mapping.
     * These are in-order with the targetForeignKeyFieldNames.
     */
    public Vector getTargetForeignKeyFieldNames() {
        Vector fieldNames = new Vector(getTargetForeignKeyFields().size());
        for (Enumeration fieldsEnum = getTargetForeignKeyFields().elements();
                 fieldsEnum.hasMoreElements();) {
            fieldNames.addElement(((DatabaseField)fieldsEnum.nextElement()).getQualifiedName());
        }

        return fieldNames;
    }

    /**
     * INTERNAL:
     * Return the target foreign key fields.
     */
    public Vector<DatabaseField> getTargetForeignKeyFields() {
        return targetForeignKeyFields;
    }

    /**
     * INTERNAL:
     * Return the target/source key fields.
     */
    public Map<DatabaseField, DatabaseField> getTargetForeignKeysToSourceKeys() {
        return targetForeignKeysToSourceKeys;
    }

    /**
     * INTERNAL:
     * Maintain for backward compatibility.
     * This is 'public' so StoredProcedureGenerator
     * does not have to use the custom query expressions.
     */
    public Map getTargetForeignKeyToSourceKeys() {
        return getTargetForeignKeysToSourceKeys();
    }

    /**
     * INTERNAL:
     * Return whether the mapping has any inverse constraint dependencies,
     * such as foreign keys and join tables.
     */
    public boolean hasInverseConstraintDependency() {
        return true;
    }

    /**
     * INTERNAL:
     * Initialize the mapping.
     */
    public void initialize(AbstractSession session) throws DescriptorException {
        super.initialize(session);

        getContainerPolicy().initialize(session, getReferenceDescriptor().getDefaultTable());
        if (shouldInitializeSelectionCriteria()) {
            setSelectionCriteria(buildDefaultSelectionCriteriaAndAddFieldsToQuery());
        }

        initializeDeleteAllQuery();
        
        if(requiresDataModificationEvents() || getContainerPolicy().requiresDataModificationEvents()) {
            initializeAddTargetQuery(session);
            initializeRemoveTargetQuery(session);
            initializeRemoveAllTargetsQuery(session);
        }
        
        if (getReferenceDescriptor() != null && getReferenceDescriptor().hasTablePerClassPolicy()) {
            // This will do nothing if we have already prepared for this 
            // source mapping or if the source mapping does not require
            // any special prepare logic.
            getReferenceDescriptor().getTablePerClassPolicy().prepareChildrenSelectionQuery(this, session);              
        }
    }

    /**
     * INTERNAL:
     * Initialize addTargetQuery.
     */
    protected void initializeAddTargetQuery(AbstractSession session) {
        AbstractRecord modifyRow = createModifyRowForAddTargetQuery();
        if(modifyRow.isEmpty()) {
            return;
        }
        addTargetQuery = new DataModifyQuery();
        
        if (!addTargetQuery.hasSessionName()) {
            addTargetQuery.setSessionName(session.getName());
        }
        if (hasCustomAddTargetQuery) {
            return;
        }
        
        // all fields in modifyRow must have the same table
        DatabaseTable table = ((DatabaseField)modifyRow.getFields().get(0)).getTable();
        
        // Build where clause expression.
        Expression whereClause = null;
        Expression builder = new ExpressionBuilder();

        int size = targetPrimaryKeyFields.size();
        for (int index = 0; index < size; index++) {
            DatabaseField targetPrimaryKey = targetPrimaryKeyFields.get(index);
            Expression expression = builder.getField(targetPrimaryKey).equal(builder.getParameter(targetPrimaryKey));
            whereClause = expression.and(whereClause);
        }

        SQLUpdateStatement statement = new SQLUpdateStatement();
        statement.setTable(table);
        statement.setWhereClause(whereClause);
        statement.setModifyRow(modifyRow);
        addTargetQuery.setSQLStatement(statement);
    }

    /**
     * INTERNAL:
     */
    protected AbstractRecord createModifyRowForAddTargetQuery() {
        AbstractRecord modifyRow = new DatabaseRecord();
        containerPolicy.addFieldsForMapKey(modifyRow);
        if(listOrderField != null) {
            modifyRow.add(listOrderField, null);
        }
        return modifyRow;
    }
    
    /**
     * INTERNAL:
     * Initialize changeOrderTargetQuery.
     */
    protected void initializeChangeOrderTargetQuery(AbstractSession session) {
        boolean hasChangeOrderTargetQuery = changeOrderTargetQuery != null;
        if(!hasChangeOrderTargetQuery) {
            changeOrderTargetQuery = new DataModifyQuery();
        }
        
        changeOrderTargetQuery = new DataModifyQuery();
        if (!changeOrderTargetQuery.hasSessionName()) {
            changeOrderTargetQuery.setSessionName(session.getName());
        }
        if (hasChangeOrderTargetQuery) {
            return;
        }

        DatabaseTable table = this.listOrderField.getTable();
        
        // Build where clause expression.
        Expression whereClause = null;
        Expression builder = new ExpressionBuilder();

        int size = targetPrimaryKeyFields.size();
        for (int index = 0; index < size; index++) {
            DatabaseField targetPrimaryKey = targetPrimaryKeyFields.get(index);
            Expression expression = builder.getField(targetPrimaryKey).equal(builder.getParameter(targetPrimaryKey));
            whereClause = expression.and(whereClause);
        }

        AbstractRecord modifyRow = new DatabaseRecord();
        modifyRow.add(this.listOrderField, null);

        SQLUpdateStatement statement = new SQLUpdateStatement();
        statement.setTable(table);
        statement.setWhereClause(whereClause);
        statement.setModifyRow(modifyRow);
        changeOrderTargetQuery.setSQLStatement(statement);
    }

    /**
     * Initialize the delete all query.
     * This query is used to delete the collection of objects from the
     * database.
     */
    protected void initializeDeleteAllQuery() {
        ((DeleteAllQuery)getDeleteAllQuery()).setReferenceClass(getReferenceClass());
        if (!hasCustomDeleteAllQuery()) {
            // the selection criteria are re-used by the delete all query
            if (getSelectionCriteria() == null) {
                getDeleteAllQuery().setSelectionCriteria(buildDefaultSelectionCriteriaAndAddFieldsToQuery());
            } else {
                getDeleteAllQuery().setSelectionCriteria(getSelectionCriteria());
            }
        }
    }

    /**
     * INTERNAL:
     * Initialize targetForeignKeyTable and initializeTargetPrimaryKeyFields.
     * This method should be called after initializeTargetForeignKeysToSourceKeys method,
     * which creates targetForeignKeyFields (guaranteed to be not empty in case 
     * requiresDataModificationEvents method returns true - the only case for the method to be called).
     */
    protected void initializeTargetPrimaryKeyFields() {
        // all target foreign key fields must have the same table.
        int size = getTargetForeignKeyFields().size();
        HashSet<DatabaseTable> tables = new HashSet();
        for(int i=0; i < size; i++) {
            tables.add(getTargetForeignKeyFields().get(i).getTable());
        }
        if(tables.size() == 1) {
            this.targetForeignKeyTable = getTargetForeignKeyFields().get(0).getTable();
        } else {
            // multiple foreign key tables - throw exception.
            throw DescriptorException.multipleTargetForeignKeyTables(this.getDescriptor(), this, tables);
        }
        
        List defaultTablePrimaryKeyFields = getReferenceDescriptor().getPrimaryKeyFields(); 
        if(this.targetForeignKeyTable.equals(getReferenceDescriptor().getDefaultTable())) {
            this.targetPrimaryKeyFields = defaultTablePrimaryKeyFields;
        } else {
            int sizePk = defaultTablePrimaryKeyFields.size();
            this.targetPrimaryKeyFields = new ArrayList();
            for(int i=0; i < sizePk; i++) {
                this.targetPrimaryKeyFields.add(null);
            }
            Map<DatabaseField, DatabaseField> map = getReferenceDescriptor().getAdditionalTablePrimaryKeyFields().get(this.targetForeignKeyTable);
            Iterator<Map.Entry<DatabaseField, DatabaseField>> it = map.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry<DatabaseField, DatabaseField> entry = it.next();
                DatabaseField sourceField = entry.getKey();
                DatabaseField targetField = entry.getValue();
                DatabaseField additionalTableField;
                DatabaseField defaultTableField;
                if(sourceField.getTable().equals(this.targetForeignKeyTable)) {
                    additionalTableField = sourceField;
                    defaultTableField = targetField;
                } else {
                    defaultTableField = sourceField;
                    additionalTableField = targetField;
                }
                int index = defaultTablePrimaryKeyFields.indexOf(defaultTableField);
                getReferenceDescriptor().buildField(additionalTableField, this.targetForeignKeyTable);
                this.targetPrimaryKeyFields.set(index, additionalTableField);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Initialize removeTargetQuery.
     */
    protected void initializeRemoveTargetQuery(AbstractSession session) {
        if (!removeTargetQuery.hasSessionName()) {
            removeTargetQuery.setSessionName(session.getName());
        }
        if (hasCustomRemoveTargetQuery) {
            return;
        }

        // All targetForeignKeys should have the same table 
        DatabaseTable table = targetForeignKeyFields.get(0).getTable();
        
        // Build where clause expression.
        Expression whereClause = null;
        Expression builder = new ExpressionBuilder();

        int size = targetPrimaryKeyFields.size();
        for (int index = 0; index < size; index++) {
            DatabaseField targetPrimaryKey = targetPrimaryKeyFields.get(index);
            Expression expression = builder.getField(targetPrimaryKey).equal(builder.getParameter(targetPrimaryKey));
            whereClause = expression.and(whereClause);
        }

        AbstractRecord modifyRow = new DatabaseRecord();
        if(shouldRemoveTargetQueryModifyTargetForeignKey()) {
            size = targetForeignKeyFields.size();
            for (int index = 0; index < size; index++) {
                DatabaseField targetForeignKey = targetForeignKeyFields.get(index);
                modifyRow.put(targetForeignKey, null);
                Expression expression = builder.getField(targetForeignKey).equal(builder.getParameter(targetForeignKey));
                whereClause = expression.and(whereClause);
            }
        }
        if(listOrderField != null) {
            modifyRow.add(listOrderField, null);
        }

        SQLUpdateStatement statement = new SQLUpdateStatement();
        statement.setTable(table);
        statement.setWhereClause(whereClause);
        statement.setModifyRow(modifyRow);
        removeTargetQuery.setSQLStatement(statement);
    }

    /**
     * Initialize and set the descriptor for the referenced class in this mapping.
     * Added here initialization of target foreign keys and target primary keys so that they are ready when
     * CollectionMapping.initialize initializes listOrderField.
     */
    protected void initializeReferenceDescriptor(AbstractSession session) throws DescriptorException {
        super.initializeReferenceDescriptor(session);
        if (!isSourceKeySpecified()) {
            // sourceKeyFields will be empty when #setTargetForeignKeyFieldName() is used
            setSourceKeyFields(org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance(getDescriptor().getPrimaryKeyFields()));
        }
        initializeTargetForeignKeysToSourceKeys();
        if(requiresDataModificationEvents() || getContainerPolicy().requiresDataModificationEvents()) {
            initializeTargetPrimaryKeyFields();
        }
    }
    
    /**
     * INTERNAL:
     * Initialize removeAllTargetsQuery.
     */
    protected void initializeRemoveAllTargetsQuery(AbstractSession session) {
        if (!removeAllTargetsQuery.hasSessionName()) {
            removeAllTargetsQuery.setSessionName(session.getName());
        }
        if (hasCustomRemoveAllTargetsQuery) {
            return;
        }

        // All targetForeignKeys should have the same table 
        DatabaseTable table = targetForeignKeyFields.get(0).getTable();
        
        // Build where clause expression.
        Expression whereClause = null;
        Expression builder = new ExpressionBuilder();

        AbstractRecord modifyRow = new DatabaseRecord();
        int size = targetForeignKeyFields.size();
        for (int index = 0; index < size; index++) {
            DatabaseField targetForeignKey = targetForeignKeyFields.get(index);
            if(shouldRemoveTargetQueryModifyTargetForeignKey()) {
                modifyRow.put(targetForeignKey, null);
            }
            Expression expression = builder.getField(targetForeignKey).equal(builder.getParameter(targetForeignKey));
            whereClause = expression.and(whereClause);
        }
        if(this.listOrderField != null) {
            // targetForeignKeys and listOrderField should have the same table
            modifyRow.add(this.listOrderField, null);
        }

        SQLUpdateStatement statement = new SQLUpdateStatement();
        statement.setTable(table);
        statement.setWhereClause(whereClause);
        statement.setModifyRow(modifyRow);
        removeAllTargetsQuery.setSQLStatement(statement);
    }
    
    /**
     * Verify, munge, and hash the target foreign keys and source keys.
     */
    protected void initializeTargetForeignKeysToSourceKeys() throws DescriptorException {
        if (getTargetForeignKeyFields().isEmpty()) {
            if (shouldInitializeSelectionCriteria() || requiresDataModificationEvents() || getContainerPolicy().requiresDataModificationEvents()) {
                throw DescriptorException.noTargetForeignKeysSpecified(this);
            } else {
                // if they have specified selection criteria, the keys do not need to be specified
                return;
            }
        }

        if (getTargetForeignKeyFields().size() != getSourceKeyFields().size()) {
            throw DescriptorException.targetForeignKeysSizeMismatch(this);
        }

        for (int index = 0; index < getTargetForeignKeyFields().size(); index++) {
            DatabaseField field = getReferenceDescriptor().buildField(getTargetForeignKeyFields().get(index));
            getTargetForeignKeyFields().set(index, field);
        }

        for (int index = 0; index < getSourceKeyFields().size(); index++) {
            DatabaseField field = getDescriptor().buildField(getSourceKeyFields().get(index));
            getSourceKeyFields().set(index, field);
        }

        Iterator<DatabaseField> targetForeignKeys = getTargetForeignKeyFields().iterator();
        Iterator<DatabaseField> sourceKeys = getSourceKeyFields().iterator();
        while (targetForeignKeys.hasNext()) {
            DatabaseField targetForeignKey = targetForeignKeys.next();
            DatabaseField sourcePrimaryKey = sourceKeys.next();
            getTargetForeignKeysToSourceKeys().put(targetForeignKey, sourcePrimaryKey);
            getSourceKeysToTargetForeignKeys().put(sourcePrimaryKey, targetForeignKey);
        }
    }

    /**
     * INTERNAL:
     */
    public boolean isOneToManyMapping() {
        return true;
    }

    /**
     * Return whether the source key is specified.
     * It will be empty when #setTargetForeignKeyFieldName(String) is used.
     */
    protected boolean isSourceKeySpecified() {
        return !getSourceKeyFields().isEmpty();
    }

    /**
     * INTERNAL:
     * An object was added to the collection during an update, insert it if private.
     */
    protected void objectAddedDuringUpdate(ObjectLevelModifyQuery query, Object objectAdded, ObjectChangeSet changeSet, Map extraData) throws DatabaseException, OptimisticLockException {
        // First insert/update object.
        super.objectAddedDuringUpdate(query, objectAdded, changeSet, extraData);

        if (requiresDataModificationEvents() || containerPolicy.requiresDataModificationEvents()){
            // In the uow data queries are cached until the end of the commit.
            if (query.shouldCascadeOnlyDependentParts()) {
                // Hey I might actually want to use an inner class here... ok array for now.
                Object[] event = new Object[4];
                event[0] = ObjectAdded;
                event[1] = query;
                event[2] = objectAdded;
                event[3] = extraData;
                query.getSession().getCommitManager().addDataModificationEvent(this, event);
            } else {
                updateTargetForeignKeyPostUpdateSource_ObjectAdded(query, objectAdded, extraData);
            }
        }
    }

    /**
     * INTERNAL:
     * An object was removed to the collection during an update, delete it if private.
     */
    protected void objectRemovedDuringUpdate(ObjectLevelModifyQuery query, Object objectDeleted, Map extraData) throws DatabaseException, OptimisticLockException {
        if(!isPrivateOwned()) {
            if (requiresDataModificationEvents() || containerPolicy.requiresDataModificationEvents()){
                // In the uow data queries are cached until the end of the commit.
                if (query.shouldCascadeOnlyDependentParts()) {
                    // Hey I might actually want to use an inner class here... ok array for now.
                    Object[] event = new Object[3];
                    event[0] = ObjectRemoved;
                    event[1] = query;
                    event[2] = objectDeleted;
                    query.getSession().getCommitManager().addDataModificationEvent(this, event);
                } else {
                    updateTargetForeignKeyPostUpdateSource_ObjectRemoved(query, objectDeleted);
                }
            }
        }

        // Delete object after join entry is delete if private.
        super.objectRemovedDuringUpdate(query, objectDeleted, extraData);
    }

    
    /**
     * INTERNAL:
     * Perform the commit event.
     * This is used in the uow to delay data modifications.
     */
    public void performDataModificationEvent(Object[] event, AbstractSession session) throws DatabaseException, DescriptorException {
        // Hey I might actually want to use an inner class here... ok array for now.
        if (event[0] == PostInsert) {
            updateTargetRowPostInsertSource((WriteObjectQuery)event[1]);
        } else if (event[0] == ObjectRemoved) {
            updateTargetForeignKeyPostUpdateSource_ObjectRemoved((WriteObjectQuery)event[1], event[2]);
        } else if (event[0] == ObjectAdded) {
            updateTargetForeignKeyPostUpdateSource_ObjectAdded((WriteObjectQuery)event[1], event[2], (Map)event[3]);
        } else {
            throw DescriptorException.invalidDataModificationEventCode(event[0], this);
        }
    }
    
    /**
     * INTERNAL:
     * Insert the reference objects.
     */
    public void postInsert(WriteObjectQuery query) throws DatabaseException, OptimisticLockException {
        if (isReadOnly()) {
            return;
        }

        if (shouldObjectModifyCascadeToParts(query) && !query.shouldCascadeOnlyDependentParts()) {
            Object objects = getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());
            // insert each object one by one
            ContainerPolicy cp = getContainerPolicy();
            for (Object iter = cp.iteratorFor(objects); cp.hasNext(iter);) {
                Object wrappedObject = cp.nextEntry(iter, query.getSession());
                Object object = cp.unwrapIteratorResult(wrappedObject);
                if (isPrivateOwned()) {
                    // no need to set changeSet as insert is a straight copy
                    InsertObjectQuery insertQuery = new InsertObjectQuery();
                    insertQuery.setIsExecutionClone(true);
                    insertQuery.setObject(object);
                    insertQuery.setCascadePolicy(query.getCascadePolicy());
                    query.getSession().executeQuery(insertQuery);
                } else {
                    // This will happen in a cascaded query.
                    // This is done only for persistence by reachability and is not required if the targets are in the queue anyway
                    // Avoid cycles by checking commit manager, this is allowed because there is no dependency.
                    if (!query.getSession().getCommitManager().isCommitInPreModify(object)) {
                        WriteObjectQuery writeQuery = new WriteObjectQuery();
                        writeQuery.setIsExecutionClone(true);
                        writeQuery.setObject(object);
                        writeQuery.setCascadePolicy(query.getCascadePolicy());
                        query.getSession().executeQuery(writeQuery);
                    }
                }
                cp.propogatePostInsert(query, wrappedObject);
            }
        }
        if (requiresDataModificationEvents() || getContainerPolicy().requiresDataModificationEvents()){
            // only cascade dependents in UOW
            if (query.shouldCascadeOnlyDependentParts()) {
                if (!isReadOnly() && (requiresDataModificationEvents() || containerPolicy.shouldUpdateForeignKeysPostInsert())) {
                    // Hey I might actually want to use an inner class here... ok array for now.
                    Object[] event = new Object[2];
                    event[0] = PostInsert;
                    event[1] = query;
                    query.getSession().getCommitManager().addDataModificationEvent(this, event);
                }
            } else {
                if (!isReadOnly() && (requiresDataModificationEvents() || containerPolicy.shouldUpdateForeignKeysPostInsert())){
                    updateTargetRowPostInsertSource(query);
                }
            }
        }
    }

    /**
     * INTERNAL:
     * Update the reference objects.
     */
    @Override
    public void postUpdate(WriteObjectQuery query) throws DatabaseException, OptimisticLockException {
        if (isReadOnly()) {
            return;
        }
        
        if (!requiresDataModificationEvents() && !shouldObjectModifyCascadeToParts(query)){
            return;
        }
       
        // if the target objects are not instantiated, they could not have been changed....
        if (!isAttributeValueInstantiatedOrChanged(query.getObject())) {
            return;
        }
    
        // manage objects added and removed from the collection
        Object objectsInMemory = getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());
        Object objectsInDB = readPrivateOwnedForObject(query);
    
        compareObjectsAndWrite(objectsInDB, objectsInMemory, query);
    }

    /**
     * INTERNAL:
     * Add additional fields
     */
    @Override
    protected void postPrepareNestedBatchQuery(ReadQuery batchQuery, ObjectLevelReadQuery query) {
        ReadAllQuery mappingBatchQuery = (ReadAllQuery)batchQuery;
        mappingBatchQuery.setShouldIncludeData(getSelectionQueryContainerPolicy().shouldAddAll());
    }

    /**
     * INTERNAL:
     * Delete the reference objects.
     */
    @Override
    public void preDelete(DeleteObjectQuery query) throws DatabaseException, OptimisticLockException {
        if (!shouldObjectModifyCascadeToParts(query)) {
            if(this.listOrderField != null) {
                updateTargetRowPreDeleteSource(query);
            }
            return;
        }

        Object objects = getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());

        ContainerPolicy cp = getContainerPolicy();

        // if privately-owned parts have their privately-owned sub-parts, delete them one by one;
        // else delete everything in one shot
        if (mustDeleteReferenceObjectsOneByOne()) {
            for (Object iter = cp.iteratorFor(objects); cp.hasNext(iter);) {
                Object wrappedObject = cp.nextEntry(iter, query.getSession());
                Object object = cp.unwrapIteratorResult(wrappedObject);
                DeleteObjectQuery deleteQuery = new DeleteObjectQuery();
                deleteQuery.setIsExecutionClone(true);
                deleteQuery.setObject(object);
                deleteQuery.setCascadePolicy(query.getCascadePolicy());
                query.getSession().executeQuery(deleteQuery);
                containerPolicy.propogatePreDelete(deleteQuery, wrappedObject);
            }
            if (!query.getSession().isUnitOfWork()) {
                // This deletes any objects on the database, as the collection in memory may have been changed.
                // This is not required for unit of work, as the update would have already deleted these objects,
                // and the backup copy will include the same objects causing double deletes.
                deleteReferenceObjectsLeftOnDatabase(query);
            }
        } else {
            deleteAll(query);
        }
    }
    
    /**
     * Prepare a cascade locking policy.
     */
    public void prepareCascadeLockingPolicy() {
        CascadeLockingPolicy policy = new CascadeLockingPolicy(getDescriptor(), getReferenceDescriptor());
        policy.setQueryKeyFields(getSourceKeysToTargetForeignKeys());
        getReferenceDescriptor().addCascadeLockingPolicy(policy);
    }

    /**
     * INTERNAL:
     * Returns whether this mapping uses data modification events to complete its writes
     * @see UnidirectionalOneToManyMapping
     * @return
     */
    public boolean requiresDataModificationEvents(){
        return this.listOrderField != null;
    }
    
    /**
     * PUBLIC:
     * The default add target query for mapping can be overridden by specifying the new query.
     * This query must set new value to target foreign key.
     */
    public void setCustomAddTargetQuery(DataModifyQuery query) {
        addTargetQuery = query;
        hasCustomAddTargetQuery = true;
    }


    /**
     * PUBLIC:
     */
    public void setAddTargetSQLString(String sqlString) {
        DataModifyQuery query = new DataModifyQuery();
        query.setSQLString(sqlString);
        setCustomAddTargetQuery(query);
    }
    
    /**
     * PUBLIC:
     * The default remove target query for mapping can be overridden by specifying the new query.
     * In case target foreign key references the source, this query must set target foreign key to null.
     */
    public void setCustomRemoveTargetQuery(DataModifyQuery query) {
        removeTargetQuery = query;
        hasCustomRemoveTargetQuery = true;
    }

    /**
     * PUBLIC:
     * The default remove all targets query for mapping can be overridden by specifying the new query.
     * This query must set all target foreign keys that reference the source to null.  
     */
    public void setCustomRemoveAllTargetsQuery(DataModifyQuery query) {
        removeAllTargetsQuery = query;
        hasCustomRemoveAllTargetsQuery = true;
    }
    
    /**
     * PUBLIC:
     * Set the SQL string used by the mapping to delete the target objects.
     * This allows the developer to override the SQL
     * generated by TopLink with a custom SQL statement or procedure call.
     * The arguments are
     * translated from the fields of the source row, by replacing the field names
     * marked by '#' with the values for those fields at execution time.
     * A one-to-many mapping will only use this delete all optimization if the target objects
     * can be deleted in a single SQL call. This is possible when the target objects
     * are in a single table, do not using locking, do not contain other privately-owned
     * parts, do not read subclasses, etc.
     * <p>
     * Example: "delete from PHONE where OWNER_ID = #EMPLOYEE_ID"
     */
    public void setDeleteAllSQLString(String sqlString) {
        DeleteAllQuery query = new DeleteAllQuery();
        query.setSQLString(sqlString);
        setCustomDeleteAllQuery(query);
    }
    

    /**
     * PUBLIC:
     * Set the name of the session to execute the mapping's queries under.
     * This can be used by the session broker to override the default session
     * to be used for the target class.
     */
    public void setSessionName(String name) {
        super.setSessionName(name);
        if (addTargetQuery != null){
            addTargetQuery.setSessionName(name);
        }
        removeTargetQuery.setSessionName(name);
        removeAllTargetsQuery.setSessionName(name);
    }

    /**
     * INTERNAL:
     * Set the source key field names associated with the mapping.
     * These must be in-order with the targetForeignKeyFieldNames.
     */
    public void setSourceKeyFieldNames(Vector fieldNames) {
        Vector fields = org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance(fieldNames.size());
        for (Enumeration fieldNamesEnum = fieldNames.elements(); fieldNamesEnum.hasMoreElements();) {
            fields.addElement(new DatabaseField((String)fieldNamesEnum.nextElement()));
        }

        setSourceKeyFields(fields);
    }

    /**
     * INTERNAL:
     * Set the source key fields.
     */
    public void setSourceKeyFields(Vector<DatabaseField> sourceKeyFields) {
        this.sourceKeyFields = sourceKeyFields;
    }

    /**
     * PUBLIC:
     * Define the target foreign key relationship in the one-to-many mapping.
     * This method can be used when the foreign and primary keys
     * have only a single field each.
     * (Use #addTargetForeignKeyFieldName(String, String)
     * for "composite" keys.)
     * Only the target foreign key field name is specified and the source
     * (primary) key field is
     * assumed to be the primary key of the source object.
     * Because the target object's table must store a foreign key to the source table,
     * the target object must map that foreign key, this is normally done through a
     * one-to-one mapping back-reference. Other options include:
     * <ul>
     * <li> use a DirectToFieldMapping and maintain the
     * foreign key fields directly in the target
     * <li> use a ManyToManyMapping
     * <li> use an AggregateCollectionMapping
     * </ul>
     * @see DirectToFieldMapping
     * @see ManyToManyMapping
     * @see AggregateCollectionMapping
     */
    public void setTargetForeignKeyFieldName(String targetForeignKeyFieldName) {
        getTargetForeignKeyFields().addElement(new DatabaseField(targetForeignKeyFieldName));
    }

    /**
     * PUBLIC:
     * Define the target foreign key relationship in the one-to-many mapping.
     * This method is used for composite target foreign key relationships.
     * That is, the target object's table has multiple foreign key fields to
     * the source object's (typically primary) key fields.
     * Both the target foreign key field names and the corresponding source primary
     * key field names must be specified.
     */
    public void setTargetForeignKeyFieldNames(String[] targetForeignKeyFieldNames, String[] sourceKeyFieldNames) {
        if (targetForeignKeyFieldNames.length != sourceKeyFieldNames.length) {
            throw DescriptorException.targetForeignKeysSizeMismatch(this);
        }
        for (int i = 0; i < targetForeignKeyFieldNames.length; i++) {
            addTargetForeignKeyFieldName(targetForeignKeyFieldNames[i], sourceKeyFieldNames[i]);
        }
    }

    /**
     * INTERNAL:
     * Set the target key field names associated with the mapping.
     * These must be in-order with the sourceKeyFieldNames.
     */
    public void setTargetForeignKeyFieldNames(Vector fieldNames) {
        Vector fields = org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance(fieldNames.size());
        for (Enumeration fieldNamesEnum = fieldNames.elements(); fieldNamesEnum.hasMoreElements();) {
            fields.addElement(new DatabaseField((String)fieldNamesEnum.nextElement()));
        }

        setTargetForeignKeyFields(fields);
    }

    /**
     * INTERNAL:
     * Set the target fields.
     */
    public void setTargetForeignKeyFields(Vector<DatabaseField> targetForeignKeyFields) {
        this.targetForeignKeyFields = targetForeignKeyFields;
    }

    /**
     * INTERNAL:
     * Set the target fields.
     */
    protected void setTargetForeignKeysToSourceKeys(Map<DatabaseField, DatabaseField> targetForeignKeysToSourceKeys) {
        this.targetForeignKeysToSourceKeys = targetForeignKeysToSourceKeys;
    }

    /**
     * Return whether any process leading to object modification
     * should also affect its parts.
     * Used by write, insert, update, and delete.
     */
    protected boolean shouldObjectModifyCascadeToParts(ObjectLevelModifyQuery query) {
        if (isReadOnly()) {
            return false;
        }

        if (isPrivateOwned()) {
            return true;
        }
        
        if (containerPolicy.isMappedKeyMapPolicy() && containerPolicy.requiresDataModificationEvents()){
            return true;
        }

        return query.shouldCascadeAllParts();
    }    
    
    /**
     * INTERNAL
     * If it's not a map then target foreign key has been already modified (set to null).
     */
    protected boolean shouldRemoveTargetQueryModifyTargetForeignKey() {
        return containerPolicy.isMapPolicy();
    }
    
    /**
     * INTERNAL
     * Return true if this mapping supports cascaded version optimistic locking.
     */
    public boolean isCascadedLockingSupported() {
        return true;
    }
    
    /**
     * INTERNAL:
     * Return if this mapping support joining.
     */
    public boolean isJoiningSupported() {
        return true;
    }

    /**
     * INTERNAL:
     * Update target foreign keys after a new source was inserted. This follows following steps.
     */
    public void updateTargetRowPostInsertSource(WriteObjectQuery query) throws DatabaseException {
        if (isReadOnly() || addTargetQuery == null) {
            return;
        }

        ContainerPolicy cp = getContainerPolicy();
        Object objects = getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());
        if (cp.isEmpty(objects)) {
            return;
        }

        prepareTranslationRow(query.getTranslationRow(), query.getObject(), query.getSession());

        AbstractRecord keyRow = buildKeyRowForTargetUpdate(query);
        
        // Extract target field and its value. Construct insert statement and execute it
        int size = targetPrimaryKeyFields.size();
        int objectIndex = 0;
        for (Object iter = cp.iteratorFor(objects); cp.hasNext(iter);) {
            AbstractRecord databaseRow = new DatabaseRecord();
            databaseRow.mergeFrom(keyRow);
            Object wrappedObject = cp.nextEntry(iter, query.getSession());
            Object object = cp.unwrapIteratorResult(wrappedObject);
            for(int index = 0; index < size; index++) {
                DatabaseField targetPrimaryKey = targetPrimaryKeyFields.get(index);
                Object targetKeyValue = getReferenceDescriptor().getObjectBuilder().extractValueFromObjectForField(object, targetPrimaryKey, query.getSession());
                databaseRow.put(targetPrimaryKey, targetKeyValue);
            }
            ContainerPolicy.copyMapDataToRow(cp.getKeyMappingDataForWriteQuery(wrappedObject, query.getSession()), databaseRow);
            if(listOrderField != null) {
                databaseRow.put(listOrderField, objectIndex++);
            }
            query.getSession().executeQuery(addTargetQuery, databaseRow);
        }
    }
    
    protected AbstractRecord buildKeyRowForTargetUpdate(ObjectLevelModifyQuery query){
        return new DatabaseRecord();
    }
    
    /**
     * INTERNAL:
     * Update target foreign key after a target object was added to the source. This follows following steps.
     * <p>- Extract primary key and its value from the source object.
     * <p>- Extract target key and its value from the target object.
     * <p>- Construct an update statement with above fields and values for target table.
     * <p>- execute the statement.
     */
    public void updateTargetForeignKeyPostUpdateSource_ObjectAdded(ObjectLevelModifyQuery query, Object objectAdded, Map extraData) throws DatabaseException {
        if (isReadOnly() || addTargetQuery == null) {
            return;
        }

        ContainerPolicy cp = getContainerPolicy();
        prepareTranslationRow(query.getTranslationRow(), query.getObject(), query.getSession());
        AbstractRecord databaseRow = buildKeyRowForTargetUpdate(query);

        // Extract target field and its value. Construct insert statement and execute it
        int size = targetPrimaryKeyFields.size();
        for (int index = 0; index < size; index++) {
            DatabaseField targetPrimaryKey = targetPrimaryKeyFields.get(index);
            Object targetKeyValue = getReferenceDescriptor().getObjectBuilder().extractValueFromObjectForField(cp.unwrapIteratorResult(objectAdded), targetPrimaryKey, query.getSession());
            databaseRow.put(targetPrimaryKey, targetKeyValue);
        }
  
        ContainerPolicy.copyMapDataToRow(cp.getKeyMappingDataForWriteQuery(objectAdded, query.getSession()), databaseRow);
        if(listOrderField != null && extraData != null) {
            databaseRow.put(listOrderField, extraData.get(listOrderField));
        }

        query.getSession().executeQuery(addTargetQuery, databaseRow);
    }

    /**
     * INTERNAL:
     * Update target foreign key after a target object was removed from the source. This follows following steps.
     * <p>- Extract primary key and its value from the source object.
     * <p>- Extract target key and its value from the target object.
     * <p>- Construct an update statement with above fields and values for target table.
     * <p>- execute the statement.
     */
    public void updateTargetForeignKeyPostUpdateSource_ObjectRemoved(ObjectLevelModifyQuery query, Object objectRemoved) throws DatabaseException {
        if (this.isReadOnly) {
            return;
        }
        AbstractSession session = query.getSession();
        prepareTranslationRow(query.getTranslationRow(), query.getObject(), session);
        AbstractRecord translationRow = new DatabaseRecord();

        // Extract primary key and value from the source (use translation row).
        int size = this.sourceKeyFields.size();
        AbstractRecord modifyRow = new DatabaseRecord(size);
        for (int index = 0; index < size; index++) {
            DatabaseField sourceKey = this.sourceKeyFields.get(index);
            DatabaseField targetForeignKey = this.targetForeignKeyFields.get(index);
            Object sourceKeyValue = query.getTranslationRow().get(sourceKey);
            translationRow.add(targetForeignKey, sourceKeyValue);
            // Need to set this value to null in the modify row.
            modifyRow.add(targetForeignKey, null);
        }
        if(listOrderField != null) {
            modifyRow.add(listOrderField, null);
        }

        ContainerPolicy cp = getContainerPolicy();
        // Extract target field and its value from the object.
        size = targetPrimaryKeyFields.size();
        for (int index = 0; index < size; index++) {
            DatabaseField targetPrimaryKey = targetPrimaryKeyFields.get(index);
            Object targetKeyValue = getReferenceDescriptor().getObjectBuilder().extractValueFromObjectForField(cp.unwrapIteratorResult(objectRemoved), targetPrimaryKey, session);
            translationRow.add(targetPrimaryKey, targetKeyValue);
        }
        // Need a different modify row than translation row, as the same field has different values in each.
        DataModifyQuery removeQuery = (DataModifyQuery)this.removeTargetQuery.clone();
        removeQuery.setModifyRow(modifyRow);
        removeQuery.setHasModifyRow(true);
        removeQuery.setIsExecutionClone(true);
        session.executeQuery(removeQuery, translationRow);
    }
    
    /**
     * INTERNAL:
     * Update target foreign key after a target object was removed from the source. This follows following steps.
     * <p>- Extract primary key and its value from the source object.
     * <p>- Extract target key and its value from the target object.
     * <p>- Construct an update statement with above fields and values for target table.
     * <p>- execute the statement.
     */
    public void updateTargetRowPreDeleteSource(ObjectLevelModifyQuery query) throws DatabaseException {
        if (this.isReadOnly) {
            return;
        }

        // Extract primary key and value from the source.
        int size = this.sourceKeyFields.size();
        AbstractRecord translationRow = new DatabaseRecord(size);
        AbstractRecord modifyRow = new DatabaseRecord(size);
        for (int index = 0; index < size; index++) {
            DatabaseField sourceKey = this.sourceKeyFields.get(index);
            DatabaseField targetForeignKey = this.targetForeignKeyFields.get(index);
            Object sourceKeyValue = query.getTranslationRow().get(sourceKey);
            translationRow.add(targetForeignKey, sourceKeyValue);
            // Need to set this value to null in the modify row.
            modifyRow.add(targetForeignKey, null);
        }
        if(listOrderField != null) {
            modifyRow.add(listOrderField, null);
        }

        // Need a different modify row than translation row, as the same field has different values in each.
        DataModifyQuery removeQuery = (DataModifyQuery)this.removeAllTargetsQuery.clone();
        removeQuery.setModifyRow(modifyRow);
        removeQuery.setHasModifyRow(true);
        removeQuery.setIsExecutionClone(true);
        query.getSession().executeQuery(removeQuery, translationRow);
    }
    
    /**
     * INTERNAL:
     * Used to verify whether the specified object is deleted or not.
     */
    public boolean verifyDelete(Object object, AbstractSession session) throws DatabaseException {
        if (this.isPrivateOwned()) {
            Object objects = this.getRealCollectionAttributeValueFromObject(object, session);

            ContainerPolicy containerPolicy = getContainerPolicy();
            for (Object iter = containerPolicy.iteratorFor(objects); containerPolicy.hasNext(iter);) {
                if (!session.verifyDelete(containerPolicy.next(iter, session))) {
                    return false;
                }
            }
        }
        return true;
    }
}
