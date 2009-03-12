/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     ailitchev - Uni-directional OneToMany
 ******************************************************************************/  
package org.eclipse.persistence.mappings;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.exceptions.OptimisticLockException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.internal.descriptors.CascadeLockingPolicy;
import org.eclipse.persistence.internal.expressions.SQLUpdateStatement;
import org.eclipse.persistence.internal.helper.ConversionManager;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.identitymaps.CacheKey;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.ChangeRecord;
import org.eclipse.persistence.internal.sessions.CollectionChangeRecord;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.internal.sessions.UnitOfWorkChangeSet;
import org.eclipse.persistence.queries.ComplexQueryResult;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.DeleteObjectQuery;
import org.eclipse.persistence.queries.ObjectLevelModifyQuery;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadQuery;
import org.eclipse.persistence.sessions.DatabaseRecord;

/**
 * <p><b>Purpose</b>: UnidirectionalOneToManyMapping doesn't have 1:1 back reference mapping.
 *
 * @author Andrei Ilitchev
 * @since Eclipselink 1.1
 */
public class UnidirectionalOneToManyMapping extends OneToManyMapping {
    /**
     * Indicates whether target's optimistic locking value should be incremented on
     * target being added to / removed from a source. 
     **/
    protected transient boolean shouldIncrementTargetLockValueOnAddOrRemoveTarget;

    /**
     * Indicates whether target's optimistic locking value should be incremented on
     * the source deletion. 
     * Note that if the flag is set to true then the indirection will be triggered on
     * source delete - in order to verify all targets' versions.
     **/
    protected transient boolean shouldIncrementTargetLockValueOnDeleteSource;

    /**
     * PUBLIC:
     * Default constructor.
     */
    public UnidirectionalOneToManyMapping() {
        super();
        this.shouldIncrementTargetLockValueOnAddOrRemoveTarget = true;
        this.shouldIncrementTargetLockValueOnDeleteSource = true;
    }
    

    /**
     * INTERNAL:
     * Build a row containing the keys for use in the query that updates the row for the
     * target object during an insert or update
     */
    protected AbstractRecord buildKeyRowForTargetUpdate(ObjectLevelModifyQuery query){
       AbstractRecord keyRow = new DatabaseRecord();
       // Extract primary key and value from the source.
       int size = sourceKeyFields.size();
       for (int index = 0; index < size; index++) {
           DatabaseField sourceKey = sourceKeyFields.get(index);
           DatabaseField targetForeignKey = targetForeignKeyFields.get(index);
           Object sourceKeyValue = query.getTranslationRow().get(sourceKey);
           keyRow.put(targetForeignKey, sourceKeyValue);
       }
       return keyRow;
   }
    
    /**
     * INTERNAL:
     * This method is used to create a change record from comparing two collections
     * @return org.eclipse.persistence.internal.sessions.ChangeRecord
     */
    public ChangeRecord compareForChange(Object clone, Object backUp, ObjectChangeSet owner, AbstractSession session) {
        ChangeRecord record = super.compareForChange(clone, backUp, owner, session);
        if(record != null && getReferenceDescriptor().getOptimisticLockingPolicy() != null) {
            postCalculateChanges(record, session);
        }
        return record;
    }
    
    /**
     * INTERNAL:
     * Extract the primary key value from the source row.
     * Used for batch reading, most following same order and fields as in the mapping.
     */
    protected Vector extractPrimaryKeyFromRow(AbstractRecord row, AbstractSession session) {
        int size = sourceKeyFields.size();
        Vector key = new Vector(size);
        ConversionManager conversionManager = session.getDatasourcePlatform().getConversionManager();

        for (int index=0; index < size; index++) {
            DatabaseField field = sourceKeyFields.get(index);
            Object value = row.get(field);

            // Must ensure the classification gets a cache hit.
            try {
                value = conversionManager.convertObject(value, field.getType());
            } catch (ConversionException e) {
                throw ConversionException.couldNotBeConverted(this, getDescriptor(), e);
            }

            key.addElement(value);
        }

        return key;
    }

    /**
     * INTERNAL:
     * Extract the source primary key value from the target row.
     * Used for batch reading, most following same order and fields as in the mapping.
     */
    protected Vector extractSourceKeyFromRow(AbstractRecord row, AbstractSession session) {
        int size = sourceKeyFields.size();
        Vector key = new Vector(size);
        ConversionManager conversionManager = session.getDatasourcePlatform().getConversionManager();

        for (int index = 0; index < size; index++) {
            DatabaseField targetField = targetForeignKeyFields.get(index);
            DatabaseField sourceField = sourceKeyFields.get(index);
            Object value = row.get(targetField);

            // Must ensure the classification gets a cache hit.
            try {
                value = conversionManager.convertObject(value, sourceField.getType());
            } catch (ConversionException e) {
                throw ConversionException.couldNotBeConverted(this, getDescriptor(), e);
            }

            key.addElement(value);
        }

        return key;
    }

    /**
     * INTERNAL:
     * Extract the value from the batch optimized query.
     */
    public Object extractResultFromBatchQuery(DatabaseQuery query, AbstractRecord databaseRow, AbstractSession session, AbstractRecord argumentRow) {
        //this can be null, because either one exists in the query or it will be created
        Hashtable referenceObjectsByKey = null;
        synchronized (query) {
            referenceObjectsByKey = getBatchReadObjects(query, session);
            if (referenceObjectsByKey == null) {
                ReadAllQuery batchQuery = (ReadAllQuery)query;
                ComplexQueryResult complexResult = (ComplexQueryResult)session.executeQuery(batchQuery, argumentRow);
                // Batch query created in ForeignReferenceMapping.prepareNestedBatchQuery without specifying container policy - uses ListContainerPolicy by default.
                List results = (List)complexResult.getResult();
                referenceObjectsByKey = new Hashtable();
                List rows = (List)complexResult.getData();
                int size = results.size();
                for (int index = 0; index < size; index++) {
                    Object eachReferenceObject = results.get(index);
                    CacheKey eachReferenceKey = new CacheKey(extractSourceKeyFromRow((AbstractRecord)rows.get(index), session));
                    if (!referenceObjectsByKey.containsKey(eachReferenceKey)) {
                        referenceObjectsByKey.put(eachReferenceKey, containerPolicy.containerInstance());
                    }
                    containerPolicy.addInto(eachReferenceObject, referenceObjectsByKey.get(eachReferenceKey), session);
                }
                setBatchReadObjects(referenceObjectsByKey, query, session);
            }
        }
        Object result = referenceObjectsByKey.get(new CacheKey(extractPrimaryKeyFromRow(databaseRow, session)));

        // The source object might not have any target objects
        if (result == null) {
            return containerPolicy.containerInstance();
        } else {
            return result;
        }
    }

    /**
     * INTERNAL:
     */
    public boolean isUnidirectionalOneToManyMapping() {
        return true;
    }
    
    /**
     * INTERNAL:
     * Initialize the mapping.
     */
    public void initialize(AbstractSession session) throws DescriptorException {
        super.initialize(session);
        if(getReferenceDescriptor().getOptimisticLockingPolicy() != null) {
            if(shouldIncrementTargetLockValueOnAddOrRemoveTarget) {
                descriptor.addMappingsPostCalculateChanges(this);
            }
            if(shouldIncrementTargetLockValueOnDeleteSource && !isPrivateOwned) {
                descriptor.addMappingsPostCalculateChangesOnDeleted(this);
            }
        }
    }
    
    /**
     * Initialize the type of the target foreign key, as it will be null as it is not mapped in the target.
     */
    public void postInitialize(AbstractSession session) {
        super.postInitialize(session);
        Iterator<DatabaseField> targetForeignKeys = getTargetForeignKeyFields().iterator();
        Iterator<DatabaseField> sourceKeys = getSourceKeyFields().iterator();
        while (targetForeignKeys.hasNext()) {
            DatabaseField targetForeignKey = targetForeignKeys.next();
            DatabaseField sourcePrimaryKey = sourceKeys.next();
            if (targetForeignKey.getType() == null) {
                targetForeignKey.setType(getDescriptor().getObjectBuilder().getMappingForField(sourcePrimaryKey).getFieldClassification(sourcePrimaryKey));
            }
        }
    }

    /**
     * INTERNAL:
     * Initialize addTargetQuery.
     */
    protected void initializeAddTargetQuery(AbstractSession session) {
        if (!addTargetQuery.hasSessionName()) {
            addTargetQuery.setSessionName(session.getName());
        }
        if (hasCustomAddTargetQuery) {
            return;
        }

        // Build where clause expression.
        Expression whereClause = null;
        Expression builder = new ExpressionBuilder();

        List<DatabaseField> targetPrimaryKeyFields = getReferenceDescriptor().getPrimaryKeyFields();
        int size = targetPrimaryKeyFields.size();
        for (int index = 0; index < size; index++) {
            DatabaseField targetPrimaryKey = targetPrimaryKeyFields.get(index);
            Expression expression = builder.getField(targetPrimaryKey).equal(builder.getParameter(targetPrimaryKey));
            whereClause = expression.and(whereClause);
        }

        AbstractRecord modifyRow = new DatabaseRecord();
        size = targetForeignKeyFields.size();
        for (int index = 0; index < size; index++) {
            DatabaseField targetForeignKey = targetForeignKeyFields.get(index);
            modifyRow.put(targetForeignKey, null);
        }
        containerPolicy.addFieldsForMapKey(modifyRow);
        
        SQLUpdateStatement statement = new SQLUpdateStatement();
        statement.setTable(getReferenceDescriptor().getDefaultTable());
        statement.setWhereClause(whereClause);
        statement.setModifyRow(modifyRow);
        addTargetQuery.setSQLStatement(statement);
    }

    /**
     * INTERNAL:
     * Delete the reference objects.
     */
    public void preDelete(DeleteObjectQuery query) throws DatabaseException, OptimisticLockException {
        if (shouldObjectModifyCascadeToParts(query)) {
            super.preDelete(query);
        } else {
            updateTargetRowPreDeleteSource(query);
            if (getContainerPolicy().propagatesEventsToCollection()){
                Object queryObject = query.getObject();
                Object values = getAttributeValueFromObject(queryObject);
                Object iterator = containerPolicy.iteratorFor(values);
                while (containerPolicy.hasNext(iterator)){
                    Object wrappedObject = containerPolicy.nextEntry(iterator, query.getSession());
                    containerPolicy.propogatePreDelete(query, wrappedObject);
                }
            }
        }

    }
    
    /**
     * Prepare a cascade locking policy.
     */
    public void prepareCascadeLockingPolicy() {
        CascadeLockingPolicy policy = new CascadeLockingPolicy(getDescriptor(), getReferenceDescriptor());
        policy.setQueryKeyFields(getSourceKeysToTargetForeignKeys());
        policy.setShouldHandleUnmappedFields(true);
        getReferenceDescriptor().addCascadeLockingPolicy(policy);
    }

    /**
     * INTERNAL:
     * Overridden by mappings that require additional processing of the change record after the record has been calculated.
     */
    public void postCalculateChanges(org.eclipse.persistence.sessions.changesets.ChangeRecord changeRecord, AbstractSession session) {
        // targets are added to and/or removed to/from the source.
        CollectionChangeRecord collectionChangeRecord = (CollectionChangeRecord)changeRecord;
        Iterator it = collectionChangeRecord.getAddObjectList().values().iterator();
        while(it.hasNext()) {
            ObjectChangeSet change = (ObjectChangeSet)it.next();
            if(!change.hasChanges()) {
                change.setShouldModifyVersionField(Boolean.TRUE);
                ((org.eclipse.persistence.internal.sessions.UnitOfWorkChangeSet)change.getUOWChangeSet()).addObjectChangeSet(change, session, false);
            }
        }
        // in the mapping is privately owned then the target will be deleted - no need to modify target version.
        if(!isPrivateOwned()) {
            it = collectionChangeRecord.getRemoveObjectList().values().iterator();
            while(it.hasNext()) {
                ObjectChangeSet change = (ObjectChangeSet)it.next(); 
                if(!change.hasChanges()) {
                    change.setShouldModifyVersionField(Boolean.TRUE);
                    ((org.eclipse.persistence.internal.sessions.UnitOfWorkChangeSet)change.getUOWChangeSet()).addObjectChangeSet(change, session, false);
                }
            }
        }
    }

    /**
     * INTERNAL:
     * Overridden by mappings that require objects to be deleted contribute to change set creation.
     */
    public void postCalculateChangesOnDeleted(Object deletedObject, UnitOfWorkChangeSet uowChangeSet, AbstractSession session) {        
        // the source is deleted:
        // trigger the indirection - we have to get optimistic lock exception
        // in case another thread has updated one of the targets:
        // triggered indirection caches the target with the old version,
        // then the version update waits until the other thread (which is locking the version field) commits,
        // then the version update is executed and it throws optimistic lock exception.
        Object col = getRealCollectionAttributeValueFromObject(deletedObject, session);
        if (col != null) {
            Object iterator = containerPolicy.iteratorFor(col);
            while (containerPolicy.hasNext(iterator)) {
                Object target = containerPolicy.next(iterator, session);
                ObjectChangeSet change = referenceDescriptor.getObjectBuilder().createObjectChangeSet(target, uowChangeSet, session);
                if (!change.hasChanges()) {
                    change.setShouldModifyVersionField(Boolean.TRUE);
                    ((org.eclipse.persistence.internal.sessions.UnitOfWorkChangeSet)change.getUOWChangeSet()).addObjectChangeSet(change, session, false);
                }
            }
        }
    }

    /**
     * INTERNAL:
     * Add additional fields
     */
    protected void postPrepareNestedBatchQuery(ReadQuery batchQuery, ReadAllQuery query) {
        ReadAllQuery mappingBatchQuery = (ReadAllQuery)batchQuery;
        mappingBatchQuery.setShouldIncludeData(true);
        int size = targetForeignKeyFields.size();
        for(int i=0; i < size; i++) {
            mappingBatchQuery.addAdditionalField(targetForeignKeyFields.get(i));
        }        
    }

    /**
     * INTERNAL:
     * The translation row may require additional fields than the primary key if the mapping in not on the primary key.
     */
    protected void prepareTranslationRow(AbstractRecord translationRow, Object object, AbstractSession session) {
        // Make sure that each source key field is in the translation row.
        int size = sourceKeyFields.size();
        for(int i=0; i < size; i++) {
            DatabaseField sourceKey = sourceKeyFields.get(i);
            if (!translationRow.containsKey(sourceKey)) {
                Object value = getDescriptor().getObjectBuilder().extractValueFromObjectForField(object, sourceKey, session);
                translationRow.put(sourceKey, value);
            }
        }
    }


    /**
     * INTERNAL:
     * UnidirectionalOneToManyMapping performs some events after INSERT/UPDATE to maintain the keys
     * @return
     */
    @Override
    public boolean requiresDataModificationEvents(){
        return true;
    }

    /**
     * PUBLIC:
     * Set value that indicates whether target's optimistic locking value should be incremented on
     * target being added to / removed from a source (default value is true).
     **/
    public void setShouldIncrementTargetLockValueOnAddOrRemoveTarget(boolean shouldIncrementTargetLockValueOnAddOrRemoveTarget) {
        this.shouldIncrementTargetLockValueOnAddOrRemoveTarget = shouldIncrementTargetLockValueOnAddOrRemoveTarget;
    }

    /**
     * PUBLIC:
     * Set value that indicates whether target's optimistic locking value should be incremented on
     * the source deletion (default value is true).
     **/
    public void setShouldIncrementTargetLockValueOnDeleteSource(boolean shouldIncrementTargetLockValueOnDeleteSource) {
        this.shouldIncrementTargetLockValueOnDeleteSource = shouldIncrementTargetLockValueOnDeleteSource;
    }

    /**
     * PUBLIC:
     * Indicates whether target's optimistic locking value should be incremented on
     * target being added to / removed from a source (default value is true).
     **/
    public boolean shouldIncrementTargetLockValueOnAddOrRemoveTarget() {
        return shouldIncrementTargetLockValueOnAddOrRemoveTarget;
    }

    /**
     * PUBLIC:
     * Indicates whether target's optimistic locking value should be incremented on
     * the source deletion (default value is true). 
     **/
    public boolean shouldIncrementTargetLockValueOnDeleteSource() {
        return shouldIncrementTargetLockValueOnDeleteSource;
    }
    

}
