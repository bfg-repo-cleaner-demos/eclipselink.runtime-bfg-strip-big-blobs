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
 ******************************************************************************/  
package org.eclipse.persistence.mappings;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.*;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.changetracking.*;
import org.eclipse.persistence.internal.descriptors.changetracking.AttributeChangeListener;
import org.eclipse.persistence.internal.descriptors.changetracking.ObjectChangeListener;
import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.expressions.*;
import org.eclipse.persistence.history.*;
import org.eclipse.persistence.indirection.IndirectCollection;
import org.eclipse.persistence.indirection.IndirectList;
import org.eclipse.persistence.internal.databaseaccess.DatasourcePlatform;
import org.eclipse.persistence.internal.databaseaccess.Platform;
import org.eclipse.persistence.internal.descriptors.*;
import org.eclipse.persistence.internal.expressions.*;
import org.eclipse.persistence.internal.helper.*;
import org.eclipse.persistence.internal.identitymaps.*;
import org.eclipse.persistence.internal.queries.*;
import org.eclipse.persistence.internal.sessions.remote.*;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.security.PrivilegedNewInstanceFromClass;
import org.eclipse.persistence.internal.sessions.*;
import org.eclipse.persistence.mappings.converters.*;
import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.sessions.remote.*;
import org.eclipse.persistence.sessions.ObjectCopyingPolicy;
import org.eclipse.persistence.sessions.DatabaseRecord;

/**
 * <p><b>Purpose</b>: This mapping is used to store a collection of simple types (String, Number, Date, etc.)
 * into a single table.  The table must store the value and a foreign key to the source object.
 * A converter can be used if the desired object type and the data type do not match.
 *
 * @see Converter
 * @see ObjectTypeConverter
 * @see TypeConversionConverter
 * @see SerializedObjectConverter
 *
 * @author Sati
 * @since TOPLink/Java 1.0
 */
public class DirectCollectionMapping extends CollectionMapping implements RelationalMapping {

    /** Used for data modification events. */
    protected static final String Delete = "delete";
    protected static final String Insert = "insert";
    protected static final String DeleteAll = "deleteAll";
    protected static final String DeleteAtIndex = "deleteAtIndex";
    protected static final String UpdateAtIndex = "updateAtIndex";

    /** Allows user defined conversion between the object value and the database value. */
    protected Converter valueConverter;
    protected String valueConverterClassName;

    /** Stores the reference table*/
    protected transient DatabaseTable referenceTable;

    /** The direct field name is converted and stored */
    protected transient DatabaseField directField;
    protected transient Vector<DatabaseField> sourceKeyFields;
    protected transient Vector<DatabaseField> referenceKeyFields;

    /** Used for insertion for m-m and dc, not used in 1-m. */
    protected transient DataModifyQuery insertQuery;

    /** Used for deletion when ChangeSets are used */
    protected transient ModifyQuery changeSetDeleteQuery;
    protected transient boolean hasCustomDeleteQuery;
    protected transient boolean hasCustomInsertQuery;
    protected HistoryPolicy historyPolicy;
    
    /** Used (only in case listOrderField != null) to delete object with particular orderFieldValue */
    protected transient ModifyQuery deleteAtIndexQuery;
    /** Used (only in case listOrderField != null) to update orderFieldValue of object with particular orderFieldValue */
    protected transient ModifyQuery updateAtIndexQuery;
    protected transient boolean hasCustomDeleteAtIndexQuery;
    protected transient boolean hasCustomUpdateAtIndexQuery;

    /**
     * PUBLIC:
     * Default constructor.
     */
    public DirectCollectionMapping() {
        this.insertQuery = new DataModifyQuery();
        this.sourceKeyFields = org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance(1);
        this.referenceKeyFields = org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance(1);
        this.selectionQuery = new DirectReadQuery();
        this.hasCustomInsertQuery = false;
        this.isPrivateOwned = true;        
        this.isListOrderFieldSupported = true;
    }

    public boolean isRelationalMapping() {
        return true;
    }

    /**
     * PUBLIC:
     * Return the converter on the mapping.
     * A converter can be used to convert between the direct collection's object value and database value.
     */
    public Converter getValueConverter() {
        return valueConverter;
    }

    /**
     * PUBLIC:
     * Set the converter on the mapping.
     * A converter can be used to convert between the direct collection's object value and database value.
     */
    public void setValueConverter(Converter valueConverter) {
        this.valueConverter = valueConverter;
    }

    /**
     * PUBLIC:
     * Set the converter class name on the mapping. Initialized in 
     * convertClassNamesToClasses.
     * A converter can be used to convert between the direct collection's object value and database value.
     */
    public void setValueConverterClassName(String valueConverterClassName) {
        this.valueConverterClassName = valueConverterClassName;
    }

    /**
     * PUBLIC:
     * Add the reference key field.
     * This is used for composite reference keys.
     * This is the foreign key field in the direct table referencing the primary key of the source object.
     * Both the reference field and the source field that it references must be provided.
     */
    public void addReferenceKeyField(DatabaseField referenceForeignKeyField, DatabaseField sourcePrimaryKeyField) {
        getSourceKeyFields().addElement(sourcePrimaryKeyField);
        getReferenceKeyFields().addElement(referenceForeignKeyField);
    }

    /**
     * PUBLIC:
     * Add the name of the reference key field.
     * This is used for composite reference keys.
     * This is the foreign key field in the direct table referencing the primary key of the source object.
     * Both the reference field name and the name of the source field that it references must be provided.
     */
    public void addReferenceKeyFieldName(String referenceForeignKeyFieldName, String sourcePrimaryKeyFieldName) {
        addReferenceKeyField(new DatabaseField(referenceForeignKeyFieldName), new DatabaseField(sourcePrimaryKeyFieldName));
    }
       
    /**
     * INTERNAL:
     * Clone and prepare the selection query as a nested batch read query.
     * This is used for nested batch reading.
     */
    public ReadQuery prepareNestedBatchQuery(ReadAllQuery query) {
        DataReadQuery batchQuery = new DataReadQuery();
        // Join the query where clause with the mappings,
        // this will cause a join that should bring in all of the target objects.
        ExpressionBuilder builder;
        Expression originalSelectionCriteria = null;

        // 2612538 - the default size of Map (32) is appropriate
        Map clonedExpressions = new IdentityHashMap();
        if (query.getSelectionCriteria() == null) {
            builder = new ExpressionBuilder();
            // S.M.  For flashback.
            if (query.hasAsOfClause()) {
                builder.asOf(query.getAsOfClause());
            }
        } else {
            // For 2729729 must clone the original selection criteria first,
            // otherwise the original query will be corrupted.
            originalSelectionCriteria = query.getSelectionCriteria().copiedVersionFrom(clonedExpressions);
            builder = originalSelectionCriteria.getBuilder();
        }

        Expression twisted = null;
        if (getSelectionQuery().isReadAllQuery()){
            twisted = builder.twist(getSelectionQuery().getSelectionCriteria(), builder);
        } else {
            twisted = builder.twist(getSelectionQuery().getSQLStatement().getWhereClause(), builder);
        }

        // For 2729729, rebuildOn is not needed as the base is still the same.	
        if (originalSelectionCriteria != null) {
            twisted = twisted.and(originalSelectionCriteria);
        }

        if (query.getDescriptor().getQueryManager().getAdditionalJoinExpression() != null) {
            twisted = twisted.and(query.getDescriptor().getQueryManager().getAdditionalJoinExpression().rebuildOn(builder));
        }
        if (getHistoryPolicy() != null) {
            if (query.getSession().getAsOfClause() != null) {
                builder.asOf(query.getSession().getAsOfClause());
            } else if (builder.getAsOfClause() == null) {
                builder.asOf(AsOfClause.NO_CLAUSE);
            }
            twisted = twisted.and(getHistoryPolicy().additionalHistoryExpression(builder));
        }

        SQLSelectStatement batchStatement = new SQLSelectStatement();

        for (Enumeration enumtr = getReferenceKeyFields().elements(); enumtr.hasMoreElements();) {
            batchStatement.addField(builder.getTable(getReferenceTable()).getField((DatabaseField)enumtr.nextElement()));
        }

        batchStatement.addField(builder.getTable(getReferenceTable()).getField(getDirectField()));

        if(listOrderField != null) {
            Expression expField = getListOrderFieldExpression(builder);
            batchStatement.addField(expField);
        }

        batchStatement.setWhereClause(twisted);
        batchQuery.setSQLStatement(batchStatement);
        getContainerPolicy().addAdditionalFieldsToQuery(batchQuery, builder);

        batchStatement.normalize(query.getSession(), getDescriptor(), clonedExpressions);

        return batchQuery;
    }
    
    /**
     * INTERNAL:
     * Clone and prepare the joined direct query.
     * Since direct-collection does not build objects a nest query is not required.
     */
    public ObjectLevelReadQuery prepareNestedJoins(JoinedAttributeManager joinManager, ObjectBuildingQuery baseQuery, AbstractSession session) {
        return null;
    }
    
    /**
     * INTERNAL:
     * Return the value of the field from the row or a value holder on the query to obtain the object.
     */
    protected Object valueFromRowInternalWithJoin(AbstractRecord row, JoinedAttributeManager joinManager, ObjectBuildingQuery sourceQuery, AbstractSession executionSession) throws DatabaseException {

        ContainerPolicy policy = getContainerPolicy();
        Object value = policy.containerInstance();
        ObjectBuilder objectBuilder = getDescriptor().getObjectBuilder();
        // Extract the primary key of the source object, to filter only the joined rows for that object.
        Vector sourceKey = objectBuilder.extractPrimaryKeyFromRow(row, executionSession);
        CacheKey sourceCacheKey = new CacheKey(sourceKey);
        // If the query was using joining, all of the result rows by primary key will have been computed.
        List rows = joinManager.getDataResultsByPrimaryKey().get(sourceCacheKey);
        int size = rows.size();
        
        if(size > 0) {
            // A set of direct values must be maintained to avoid duplicates from multiple 1-m joins.
            Set directValues = new HashSet();
    
            ArrayList directValuesList = null;
            ArrayList<AbstractRecord> targetRows = null;
            boolean shouldAddAll = policy.shouldAddAll();
            if(shouldAddAll) {
                directValuesList = new ArrayList(size);
                targetRows = new ArrayList(size);
            }
            Converter valueConverter = getValueConverter();
            // indicates if collection contains null
            boolean containsNull = false;
            // For each rows, extract the target row and build the target object and add to the collection.
            for (int index = 0; index < size; index++) {
                AbstractRecord sourceRow = (AbstractRecord)rows.get(index);
                AbstractRecord targetRow = sourceRow;            
                // The field for many objects may be in the row,
                // so build the subpartion of the row through the computed values in the query,
                // this also helps the field indexing match.
                targetRow = trimRowForJoin(targetRow, joinManager, executionSession);            
                // Partial object queries must select the primary key of the source and related objects.
                // If the target joined rows in null (outerjoin) means an empty collection.
                Object directValue = targetRow.get(getDirectField());
                if (directValue == null) {
                    if(size==1) {
                        // A null direct value means an empty collection returned as nulls from an outerjoin.
                        return getIndirectionPolicy().valueFromRow(value);
                    } else {
                        containsNull = true;
                    }
                }                        
                // Only build/add the target object once, skip duplicates from multiple 1-m joins.
                if (!directValues.contains(directValue)) {
                    directValues.add(directValue);                            
                    // Allow for value conversion.
                    if (valueConverter != null) {
                        directValue = valueConverter.convertDataValueToObjectValue(directValue, executionSession);
                    }
                    if(shouldAddAll) {
                        directValuesList.add(directValue);
                        targetRows.add(targetRow);
                    } else {
                        policy.addInto(directValue, value, executionSession, targetRow, sourceQuery);
                    }
                }
            }
            if(shouldAddAll) {
                // if collection contains a single element which is null then return an empty collection
                if(!(containsNull && targetRows.size()==1)) {
                    policy.addAll(directValuesList, value, executionSession, targetRows, sourceQuery);
                }
            } else {
                // if collection contains a single element which is null then return an empty collection
                if(containsNull && policy.sizeFor(value)==1) {
                    policy.clear(value);
                }
            }
        }
        return getIndirectionPolicy().valueFromRow(value);
    }
    
    /**
     * INTERNAL:
     * Copy of the attribute of the object.
     * This is NOT used for unit of work but for templatizing an object.
     */
    public void buildCopy(Object copy, Object original, ObjectCopyingPolicy policy) {
        Object attributeValue = getRealCollectionAttributeValueFromObject(original, policy.getSession());
        attributeValue = getContainerPolicy().cloneFor(attributeValue);
        setRealAttributeValueInObject(copy, attributeValue);
    }

    /**
     * INTERNAL:
     * Clone the element, if necessary.
     * DirectCollections hold on to objects that do not have Descriptors
     * (e.g. int, String). These objects do not need to be cloned, unless they use a converter - they
     * are immutable.
     */
    public Object buildElementClone(Object element, Object parent, UnitOfWorkImpl unitOfWork, boolean isExisting) {
        Object cloneValue = element;
        if ((getValueConverter() != null) && getValueConverter().isMutable()) {
            cloneValue = getValueConverter().convertDataValueToObjectValue(getValueConverter().convertObjectValueToDataValue(cloneValue, unitOfWork), unitOfWork);
        }
        return cloneValue;
    }

    /**
     * INTERNAL:
     * Verifies listOrderField's table: it must be reference table.
     * Precondition: listOrderField != null.
     */
    protected void buildListOrderField() {
        if(this.listOrderField.hasTableName()) {
            if(!getReferenceTable().equals(this.listOrderField.getTable())) {
                throw DescriptorException.listOrderFieldTableIsWrong(this.getDescriptor(), this, this.listOrderField.getTable(), getReferenceTable());
            }
        } else {
            this.listOrderField.setTable(getReferenceTable());
        }
        this.listOrderField = getDescriptor().buildField(this.listOrderField, getReferenceTable());
    }
    
    /**
     * INTERNAL:
     * Cascade perform delete through mappings that require the cascade
     */
    public void cascadePerformRemoveIfRequired(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        //as this mapping type references primitive objects this method does not apply
    }
    
    /**
     * INTERNAL:
     * Cascade perform removal of orphaned private owned objects from the UnitOfWorkChangeSet
     */
    public void cascadePerformRemovePrivateOwnedObjectFromChangeSetIfRequired(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        // as this mapping type references primitive objects this method does not apply
    }

    /**
     * INTERNAL:
     * Cascade registerNew for Create through mappings that require the cascade
     */
    public void cascadeRegisterNewIfRequired(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        //as this mapping type references primitive objects this method does not apply
    }
    
    /**
     * INTERNAL:
     * Cascade discover and persist new objects during commit.
     */
    public void cascadeDiscoverAndPersistUnregisteredNewObjects(Object object, Map newObjects, Map unregisteredExistingObjects, Map visitedObjects, UnitOfWorkImpl uow) {
        // Direct mappings do not require any cascading.
    }

    /**
     * INTERNAL:
     * The mapping clones itself to create deep copy.
     */
    public Object clone() {
        DirectCollectionMapping clone = (DirectCollectionMapping)super.clone();

        clone.setSourceKeyFields(cloneFields(getSourceKeyFields()));
        clone.setReferenceKeyFields(cloneFields(getReferenceKeyFields()));
        
        if(this.changeSetDeleteQuery != null) {
            clone.changeSetDeleteQuery = (ModifyQuery)this.changeSetDeleteQuery.clone();
        }
        if(this.deleteAtIndexQuery != null) {
            clone.deleteAtIndexQuery = (ModifyQuery)this.deleteAtIndexQuery.clone();
        }
        if(this.updateAtIndexQuery != null) {
            clone.updateAtIndexQuery = (ModifyQuery)this.updateAtIndexQuery.clone();
        }

        return clone;
    }

    /**
     * INTERNAL:
     * This method is used to calculate the differences between two collections.
     */
    public void compareCollectionsForChange(Object oldCollection, Object newCollection, ChangeRecord changeRecord, AbstractSession session) {
        if(this.listOrderField != null) {
            compareListsForChange((List)oldCollection, (List)newCollection, changeRecord, session);
            return;
        }
        ContainerPolicy cp = getContainerPolicy();
        int numberOfNewNulls = 0;

        HashMap originalKeyValues = new HashMap(10);
        HashMap cloneKeyValues = new HashMap(10);

        if (oldCollection != null) {
            Object backUpIter = cp.iteratorFor(oldCollection);

            while (cp.hasNext(backUpIter)) {// Make a lookup of the objects
                Object secondObject = cp.next(backUpIter, session);

                // For CR#2258/CR#2378 handle null values inserted in a collection.
                if (secondObject == null) {
                    numberOfNewNulls--;
                } else {
                    Integer count = (Integer)originalKeyValues.get(secondObject);
                    if (count == null) {
                        originalKeyValues.put(secondObject, new Integer(1));
                    } else {
                        originalKeyValues.put(secondObject, new Integer(count.intValue() + 1));
                    }
                }
            }
        }

        // should a removal occur this is the original count of objects on the database.
        // this value is used to determine how many objects to re-insert after the delete as a
        // delete will delete all of the objects not just one.
        HashMap databaseCount = (HashMap)originalKeyValues.clone();
        int databaseNullCount = Math.abs(numberOfNewNulls);

        if (newCollection != null) {
            Object cloneIter = cp.iteratorFor(newCollection);

            /* The following code is used to compare objects in a direct collection.
               Because objects in a direct collection are primitives and may be the same object
               the following code must count the number of instances in the collection not just the
               existence of an object.
            */
            while (cp.hasNext(cloneIter)) {//Compare them with the objects from the clone
                Object firstObject = cp.next(cloneIter, session);

                // For CR#2258/CR#2378 handle null values inserted in a collection.
                if (firstObject == null) {
                    numberOfNewNulls++;
                } else {
                    Integer count = (Integer)originalKeyValues.get(firstObject);
                    if (count == null) {//the object was not in the backup
                        Integer cloneCount = (Integer)cloneKeyValues.get(firstObject);

                        //Add it to the additions hashtable
                        if (cloneCount == null) {
                            cloneKeyValues.put(firstObject, new Integer(1));
                        } else {
                            cloneKeyValues.put(firstObject, new Integer(cloneCount.intValue() + 1));
                        }
                    } else if (count.intValue() == 1) {
                        //There is only one object so remove the whole reference
                        originalKeyValues.remove(firstObject);
                    } else {
                        originalKeyValues.put(firstObject, new Integer(count.intValue() - 1));
                    }
                }
            }
        }
        if (cloneKeyValues.isEmpty() && originalKeyValues.isEmpty() && (numberOfNewNulls == 0) && (!changeRecord.getOwner().isNew())) {
            return;
        }
        ((DirectCollectionChangeRecord)changeRecord).addAdditionChange(cloneKeyValues, databaseCount);
        ((DirectCollectionChangeRecord)changeRecord).addRemoveChange(originalKeyValues, databaseCount);
        //For CR#2258, produce a changeRecord which reflects the addition and removal of null values.
        if (numberOfNewNulls != 0) {
            ((DirectCollectionChangeRecord)changeRecord).getCommitAddMap().put(null, new Integer(databaseNullCount));
            if (numberOfNewNulls > 0) {
                ((DirectCollectionChangeRecord)changeRecord).addAdditionChange(null, new Integer(numberOfNewNulls));
            } else {
                numberOfNewNulls *= -1;
                ((DirectCollectionChangeRecord)changeRecord).addRemoveChange(null, new Integer(numberOfNewNulls));
            }
        }
    }

    /**
     * INTERNAL:
     * This method is used to calculate the differences between two Lists.
     */
    public void compareListsForChange(List oldList, List newList, ChangeRecord changeRecord, AbstractSession session) {
        // Maps objects (null included) in newList and oldList to an array of two Sets:
        // the first one contains indexes of the object in oldList, the second - in newList.
        // Contains only the objects for which the set of indexes in newList and oldList are different;
        // only changed indexes appear in the sets (therefore the old index set and new index set don't intersect).
        // Examples:
        //    obj was first (index 0) in oldList; first and second (indexes 0 and 1)in newList: obj -> {{}, {1}};
        //    obj was not in oldList; first in newList: obj -> {null, {0}};
        //    obj was first in oldList; not in newList: obj -> {{0}, null};
        //    obj was first and second in oldList; first in newList: obj -> {{1}, {}};
        // Note the difference between null and empty set:
        //    empty set means there's at least one index (the same in oldList and newList - otherwise it would've been in the set);
        //    null means there's no indexes.
        // That helps during deletion - if we know there is no remaining duplicates for the object to be removed
        // we can delete it without checking its index (which allows delete several duplicates in one sql).
        // Map entry sets with no new and no old indexes removed.
        HashMap changedIndexes = new HashMap(Math.max(oldList.size(), newList.size()));
        
        int nOldSize = 0;
        // for each object in oldList insert all its indexes in oldList into the old indexes set corresponding to each object.
        if (oldList != null) {
            nOldSize = oldList.size();
            for(int i=0; i < nOldSize; i++) {
                Object obj = oldList.get(i);    
                Set[] indexes = (Set[])changedIndexes.get(obj);
                if (indexes == null) {
                    // the first index found for the object.
                    indexes = new Set[]{new HashSet(), null};
                    changedIndexes.put(obj, indexes);
                }
                indexes[0].add(i);
            }
        }

        // helper set to store objects for which entries into changedIndexes has been removed:
        // if an entry for the object is created again, it will have an empty old indexes set (rather than null)
        // to indicate that the object has been on the oldList, too.
        HashSet removedFromChangedIndexes = new HashSet();
        HashSet dummySet = new HashSet(0);
        
        // for each object in newList, for each its index in newList:
        //   if the object has the same index in oldList - remove the index from old indexes set;
        //   if the object doesn't have the same index in oldList - insert the index into new indexes set.
        int nNewSize = 0;
        if (newList != null) {
            nNewSize = newList.size();
            for(int i=0; i < nNewSize; i++) {
                Object obj = newList.get(i);
                Set[] indexes = (Set[])changedIndexes.get(obj);
                if (indexes == null) {
                    // the first index found for the object - or was found and removed before.
                    if(removedFromChangedIndexes.contains(obj)) {
                        // the object also exists in oldList
                        indexes = new Set[]{dummySet, new HashSet()};
                    } else {
                        // the object does not exist in oldList
                        indexes = new Set[]{null, new HashSet()};
                    }
                    changedIndexes.put(obj, indexes);
                    // the object doesn't have this index in oldList - add the index to new indexes set.
                    indexes[1].add(i);
                } else {
                    if(indexes[0] == null || !indexes[0].contains(i)) {
                        // the object doesn't have this index in oldList - add the index to new indexes set.
                        if(indexes[1] == null) {
                            indexes[1] = new HashSet();
                        }
                        indexes[1].add(i);
                    } else {
                        // the object has this index in oldList - remove the index from the old indexes set.
                        indexes[0].remove(i);
                        if(indexes[0].isEmpty()) {
                            // no old indexes left for the object.
                            if(indexes[1] == null || indexes[1].isEmpty()) {
                                // no new indexes left, too - remove the entry for the object.
                                changedIndexes.remove(obj);
                                // store the object in case it has another index on newList 
                                removedFromChangedIndexes.add(obj);
                            }
                        }
                    }
                }
            }
        }

        ((DirectCollectionChangeRecord)changeRecord).setChangedIndexes(changedIndexes);
        ((DirectCollectionChangeRecord)changeRecord).setOldSize(nOldSize);
        ((DirectCollectionChangeRecord)changeRecord).setNewSize(nNewSize);
    }

    /**
     * INTERNAL:
     * This method compares the changes between two direct collections.  Comparisons are made on equality
     * not identity.
     */
    public ChangeRecord compareForChange(Object clone, Object backUp, ObjectChangeSet owner, AbstractSession session) {
        Object cloneAttribute = getAttributeValueFromObject(clone);
        Object backUpAttribute = null;

        if ((cloneAttribute != null) && (!getIndirectionPolicy().objectIsInstantiated(cloneAttribute))) {
            return null;
        }

        Object cloneObjectCollection = getRealCollectionAttributeValueFromObject(clone, session);
        Object backUpCollection = null;

        if (!owner.isNew()) {
            backUpAttribute = getAttributeValueFromObject(backUp);

            if ((backUpAttribute == null) && (cloneAttribute == null)) {
                return null;
            }

            backUpCollection = getRealCollectionAttributeValueFromObject(backUp, session);
        }
        DirectCollectionChangeRecord changeRecord = new DirectCollectionChangeRecord(owner);
        changeRecord.setAttribute(getAttributeName());
        changeRecord.setMapping(this);
        if(this.listOrderField != null) {
            changeRecord.setLatestCollection(cloneObjectCollection);
        }
        compareCollectionsForChange(backUpCollection, cloneObjectCollection, changeRecord, session);
        if (changeRecord.hasChanges()) {
            return changeRecord;
        }
        return null;
    }

    /**
     * INTERNAL:
     * Compare the attributes belonging to this mapping for the objects.
     */
    public boolean compareObjects(Object firstObject, Object secondObject, AbstractSession session) {
        Object firstCollection = getRealCollectionAttributeValueFromObject(firstObject, session);
        Object secondCollection = getRealCollectionAttributeValueFromObject(secondObject, session);
        if(this.listOrderField != null) {
            return compareLists((List)firstCollection, (List)secondCollection);
        }
        ContainerPolicy containerPolicy = getContainerPolicy();

        if (containerPolicy.sizeFor(firstCollection) != containerPolicy.sizeFor(secondCollection)) {
            return false;
        }

        HashMap firstCounter = new HashMap();
        HashMap secondCounter = new HashMap();
        for (Object iter = containerPolicy.iteratorFor(firstCollection);
                 containerPolicy.hasNext(iter);) {
            Object object = containerPolicy.next(iter, session);
            if (firstCounter.containsKey(object)) {
                int count = ((Integer)firstCounter.get(object)).intValue();
                firstCounter.put(object, new Integer(++count));
            } else {
                firstCounter.put(object, new Integer(1));
            }
        }
        for (Object iter = containerPolicy.iteratorFor(secondCollection);
                 containerPolicy.hasNext(iter);) {
            Object object = containerPolicy.next(iter, session);
            if (secondCounter.containsKey(object)) {
                int count = ((Integer)secondCounter.get(object)).intValue();
                secondCounter.put(object, new Integer(++count));
            } else {
                secondCounter.put(object, new Integer(1));
            }
        }
        for (Iterator iterator = firstCounter.keySet().iterator(); iterator.hasNext();) {
            Object object = iterator.next();

            if (!secondCounter.containsKey(object) || (((Integer)secondCounter.get(object)).intValue() != ((Integer)firstCounter.get(object)).intValue())) {
                // containsKey(object) will fail when the objects are arrays.
                boolean found = false;
                
                for (Iterator ii = secondCounter.keySet().iterator(); ii.hasNext();) {
                    Object otherObject = ii.next();
                    if(object == otherObject) {
                        found = true;
                    } else if(object == null || otherObject == null) {
                        found = false;
                    } else {
                        found = Helper.comparePotentialArrays(object, otherObject);
                    }
                        
                    if (found) {
                        iterator.remove();
                        secondCounter.remove(otherObject);
                        break;
                    }
                }
                
                if (!found) {
                    return false;
                }
            } else {
                iterator.remove();
                secondCounter.remove(object);
            }
        }
        if (!firstCounter.isEmpty() || !secondCounter.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Compare two lists. For equality the order of the elements should be the same. 
     * Used only if listOrderField != null
     */
    protected boolean compareLists(List firstList, List secondList) {
        if (firstList.size() != secondList.size()) {
            return false;
        }

        int size = firstList.size();
        for(int i=0; i < size; i++) {
            Object firstObject = firstList.get(i);
            Object secondObject = secondList.get(i);
            if(firstObject != secondObject) {
                if(firstObject==null || secondObject==null) {
                    return false;
                } else {
                    if(!firstObject.equals(secondObject)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * INTERNAL:
     * Convert all the class-name-based settings in this mapping to actual class-based
     * settings
     * This method is implemented by subclasses as necessary.
     * @param classLoader 
     */
    public void convertClassNamesToClasses(ClassLoader classLoader){
        super.convertClassNamesToClasses(classLoader);
        
        if (valueConverter != null) {
            if (valueConverter instanceof TypeConversionConverter){
                ((TypeConversionConverter)valueConverter).convertClassNamesToClasses(classLoader);
            } else if (valueConverter instanceof ObjectTypeConverter) {
                // To avoid 1.5 dependencies with the EnumTypeConverter check
                // against ObjectTypeConverter.
                ((ObjectTypeConverter) valueConverter).convertClassNamesToClasses(classLoader);
            }
        }
        
        // Instantiate any custom converter class
        if (valueConverterClassName != null) {
            Class valueConverterClass;
            Converter valueConverter;
    
            try {
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                    try {
                        valueConverterClass = (Class) AccessController.doPrivileged(new PrivilegedClassForName(valueConverterClassName, true, classLoader));
                    } catch (PrivilegedActionException exception) {
                        throw ValidationException.classNotFoundWhileConvertingClassNames(valueConverterClassName, exception.getException());
                    }
                    
                    try {
                        valueConverter = (Converter) AccessController.doPrivileged(new PrivilegedNewInstanceFromClass(valueConverterClass));
                    } catch (PrivilegedActionException exception) {
                        throw ValidationException.classNotFoundWhileConvertingClassNames(valueConverterClassName, exception.getException());
                    }
                } else {
                    valueConverterClass = org.eclipse.persistence.internal.security.PrivilegedAccessHelper.getClassForName(valueConverterClassName, true, classLoader);
                    valueConverter = (Converter) org.eclipse.persistence.internal.security.PrivilegedAccessHelper.newInstanceFromClass(valueConverterClass);
                }
            } catch (ClassNotFoundException exc) {
                throw ValidationException.classNotFoundWhileConvertingClassNames(valueConverterClassName, exc);
            } catch (Exception e) {
                // Catches IllegalAccessException and InstantiationException
                throw ValidationException.classNotFoundWhileConvertingClassNames(valueConverterClassName, e);
            }
            
            setValueConverter(valueConverter);
        }
    };

    /**
     * INTERNAL:
     * Extract the source primary key value from the reference direct row.
     * Used for batch reading, most following same order and fields as in the mapping.
     */
    protected Vector extractKeyFromTargetRow(AbstractRecord row, AbstractSession session) {
        Vector key = new Vector(getReferenceKeyFields().size());

        for (int index = 0; index < getReferenceKeyFields().size(); index++) {
            DatabaseField relationField = getReferenceKeyFields().elementAt(index);
            DatabaseField sourceField = getSourceKeyFields().elementAt(index);
            Object value = row.get(relationField);

            // Must ensure the classification gets a cache hit.
            try {
                value = session.getDatasourcePlatform().getConversionManager().convertObject(value, getDescriptor().getObjectBuilder().getFieldClassification(sourceField));
            } catch (ConversionException e) {
                throw ConversionException.couldNotBeConverted(this, getDescriptor(), e);
            }

            key.addElement(value);
        }

        return key;
    }

    /**
     * INTERNAL:
     * Extract the primary key value from the source row.
     * Used for batch reading, most following same order and fields as in the mapping.
     */
    protected Vector extractPrimaryKeyFromRow(AbstractRecord row, AbstractSession session) {
        Vector key = new Vector(getSourceKeyFields().size());

        for (Enumeration fieldEnum = getSourceKeyFields().elements(); fieldEnum.hasMoreElements();) {
            DatabaseField field = (DatabaseField)fieldEnum.nextElement();
            Object value = row.get(field);

            // Must ensure the classification to get a cache hit.
            try {
                value = session.getDatasourcePlatform().getConversionManager().convertObject(value, getDescriptor().getObjectBuilder().getFieldClassification(field));
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
        Hashtable referenceDataByKey = null;
        ContainerPolicy mappingContainerPolicy = getContainerPolicy();
        synchronized (query) {
            referenceDataByKey = getBatchReadObjects(query, session);
            mappingContainerPolicy = getContainerPolicy();
            if (referenceDataByKey == null) {
                Vector rows = (Vector)session.executeQuery(query, argumentRow);
                int size = rows.size();
                referenceDataByKey = new Hashtable();
                if(mappingContainerPolicy.shouldAddAll()) {
                    if(size > 0) {
                        HashMap<CacheKey, List[]> referenceDataAndRowsByKey = new HashMap();
                        for (int i=0; i < size; i++) {
                            AbstractRecord referenceRow = (AbstractRecord)rows.get(i);
                            Object referenceValue = referenceRow.get(getDirectField());
                            CacheKey eachReferenceKey = new CacheKey(extractKeyFromTargetRow(referenceRow, session));
        
                            // Allow for value conversion.
                            if (getValueConverter() != null) {
                                referenceValue = getValueConverter().convertDataValueToObjectValue(referenceValue, query.getSession());
                            }
                            List[] valuesAndRows = referenceDataAndRowsByKey.get(eachReferenceKey);
                            if(valuesAndRows == null) {
                                valuesAndRows = new List[]{new ArrayList(), new ArrayList()};
                                referenceDataAndRowsByKey.put(eachReferenceKey, valuesAndRows);
                            }
                            valuesAndRows[0].add(referenceValue);
                            valuesAndRows[1].add(referenceRow);
                        }

                        Iterator<Map.Entry<CacheKey, List[]>> it = referenceDataAndRowsByKey.entrySet().iterator();
                        while(it.hasNext()) {
                            Map.Entry<CacheKey, List[]> entry = it.next();
                            CacheKey eachReferenceKey = entry.getKey();
                            List referenceValues = entry.getValue()[0];
                            List<AbstractRecord> referenceRows = entry.getValue()[1];
                            Object container = mappingContainerPolicy.containerInstance(referenceValues.size());
                            mappingContainerPolicy.addAll(referenceValues, container, query.getSession(), referenceRows, (DataReadQuery)query);
                            referenceDataByKey.put(eachReferenceKey, container);
                        }
                    }
                } else {
                    for (int i=0; i < size; i++) {
                        AbstractRecord referenceRow = (AbstractRecord)rows.get(i);
                        Object referenceValue = referenceRow.get(getDirectField());
                        CacheKey eachReferenceKey = new CacheKey(extractKeyFromTargetRow(referenceRow, session));
    
                        Object container = referenceDataByKey.get(eachReferenceKey);
                        if (container == null) {
                            container = mappingContainerPolicy.containerInstance();
                            referenceDataByKey.put(eachReferenceKey, container);
                        }
    
                        // Allow for value conversion.
                        if (getValueConverter() != null) {
                            referenceValue = getValueConverter().convertDataValueToObjectValue(referenceValue, query.getSession());
                        }
                        mappingContainerPolicy.addInto(referenceValue, container, query.getSession());
                    }
                }
                    
                setBatchReadObjects(referenceDataByKey, query, session);
                query.setSession(null);
            }
        }
        Object result = referenceDataByKey.get(new CacheKey(extractPrimaryKeyFromRow(databaseRow, session)));

        // The source object might not have any target objects
        if (result == null) {
            return mappingContainerPolicy.containerInstance();
        } else {
            return result;
        }
    }

    /**
     * INTERNAL:
     * At this point, we realize we don't have indirection;
     * so we need to replace the reference object(s) with
     * the corresponding object(s) from the remote session.
     *
     * The reference objects for a DirectCollectionMapping
     * are primitives, so they do not need to be replaced.
     */
    public void fixRealObjectReferences(Object object, Map objectInformation, Map processedObjects, ObjectLevelReadQuery query, RemoteSession session) {
        // do nothing
    }

    protected ModifyQuery getDeleteQuery() {
        if (changeSetDeleteQuery == null) {
            changeSetDeleteQuery = new DataModifyQuery();
        }
        return changeSetDeleteQuery;
    }

    protected ModifyQuery getDeleteAtIndexQuery() {
        if (deleteAtIndexQuery == null) {
            deleteAtIndexQuery = new DataModifyQuery();
        }
        return deleteAtIndexQuery;
    }

    protected ModifyQuery getUpdateAtIndexQuery() {
        if (updateAtIndexQuery == null) {
            updateAtIndexQuery = new DataModifyQuery();
        }
        return updateAtIndexQuery;
    }

    /**
     * INTERNAL:
     * Returns the set of fields that should be selected to build this mapping's value(s).
     * This is used by expressions to determine which fields to include in the select clause for non-object expressions.
     */
    public Vector getSelectFields() {
        Vector fields = new NonSynchronizedVector(2);
        fields.add(getDirectField());
        return fields;
    }
        
    /**
     * INTERNAL:
     * Returns the table(s) that should be selected to build this mapping's value(s).
     * This is used by expressions to determine which tables to include in the from clause for non-object expressions.
     */
    public Vector getSelectTables() {
        Vector tables = new NonSynchronizedVector(0);
        tables.add(getReferenceTable());
        return tables;
    }
    
    /**
     * INTERNAL:
     * Return the direct field.
     * This is the field in the direct table to store the values.
     */
    public DatabaseField getDirectField() {
        return directField;
    }

    /**
     * PUBLIC:
     * Returns the name of the field name in the reference table.
     */
    public String getDirectFieldName() {
        if (getDirectField() == null) {
            return null;
        }
        return getDirectField().getQualifiedName();
    }

    protected DataModifyQuery getInsertQuery() {
        return insertQuery;
    }

    /**
     * INTERNAL:
     * Returns the join criteria stored in the mapping selection query. This criteria
     * is used to read reference objects across the tables from the database.
     */
    public Expression getJoinCriteria(QueryKeyExpression exp) {
        if (getHistoryPolicy() != null) {
            Expression result = super.getJoinCriteria(exp);
            Expression historyCriteria = getHistoryPolicy().additionalHistoryExpression(exp);
            if (result != null) {
                return result.and(historyCriteria);
            } else if (historyCriteria != null) {
                return historyCriteria;
            } else {
                return null;
            }
        } else {
            return super.getJoinCriteria(exp);
        }
    }

    /**
     * INTERNAL:
     * return the object on the client corresponding to the specified object.
     * DirectCollections do not have to worry about
     * maintaining object identity.
     */
    public Object getObjectCorrespondingTo(Object object, RemoteSession session, Map objectDescriptors, Map processedObjects, ObjectLevelReadQuery query) {
        return object;
    }

    /**
     * PUBLIC:
     */
    public HistoryPolicy getHistoryPolicy() {
        return historyPolicy;
    }

    /**
     * INTERNAL:
     * Get the container policy from the selection query for this mapping. 
     */
    protected ContainerPolicy getSelectionQueryContainerPolicy() {
        return ((DataReadQuery) getSelectionQuery()).getContainerPolicy();
    }

    /**
     * INTERNAL:
     * This cannot be used with direct collection mappings.
     */
    public Class getReferenceClass() {
        return null;
    }

    public String getReferenceClassName() {
        return null;
    }

    /**
     * INTERNAL:
     * There is none on direct collection.
     */
    public ClassDescriptor getReferenceDescriptor() {
        return null;
    }

    /**
     * INTERNAL:
     * Return the reference key field names associated with the mapping.
     * These are in-order with the sourceKeyFieldNames.
     */
    public Vector getReferenceKeyFieldNames() {
        Vector fieldNames = new Vector(getReferenceKeyFields().size());
        for (Enumeration fieldsEnum = getReferenceKeyFields().elements();
                 fieldsEnum.hasMoreElements();) {
            fieldNames.addElement(((DatabaseField)fieldsEnum.nextElement()).getQualifiedName());
        }

        return fieldNames;
    }

    /**
     * INTERNAL:
     * Return the reference key fields.
     */
    public Vector<DatabaseField> getReferenceKeyFields() {
        return referenceKeyFields;
    }

    /**
     * INTERNAL:
     * Return the direct table.
     * This is the table to store the values.
     */
    public DatabaseTable getReferenceTable() {
        return referenceTable;
    }

    /**
     * PUBLIC:
     * Returns the name of the reference table
     */
    public String getReferenceTableName() {
        if (getReferenceTable() == null) {
            return null;
        }
        return getReferenceTable().getName();
    }

    //This method is added to include table qualifier.

    /**
     * PUBLIC:
     * Returns the qualified name of the reference table.
     */
    public String getReferenceTableQualifiedName() {//CR#2407  
        if (getReferenceTable() == null) {
            return null;
        }
        return getReferenceTable().getQualifiedNameDelimited();
    }

    /**
     * INTERNAL:
     * Return the relationshipPartner mapping for this bi-directional mapping. If the relationshipPartner is null then
     * this is a uni-directional mapping.
     * DirectCollectionMapping can not be part of a bi-directional mapping
     */
    public DatabaseMapping getRelationshipPartner() {
        return null;
    }

    /**
     * PUBLIC:
     * Return the source key field names associated with the mapping.
     * These are in-order with the referenceKeyFieldNames.
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

    protected boolean hasCustomDeleteQuery() {
        return hasCustomDeleteQuery;
    }

    protected boolean hasCustomInsertQuery() {
        return hasCustomInsertQuery;
    }

    protected boolean hasCustomDeleteAtIndexQuery() {
        return hasCustomDeleteAtIndexQuery;
    }

    protected boolean hasCustomUpdateAtIndexQuery() {
        return hasCustomUpdateAtIndexQuery;
    }

    /**
     * INTERNAL:
     * Initialize and validate the mapping properties.
     */
    public void initialize(AbstractSession session) throws DescriptorException {
        if (isKeyForSourceSpecified()) {
            initializeSourceKeys(session);
        } else {
            initializeSourceKeysWithDefaults(session);
        }

        initializeReferenceTable(session);
        initializeReferenceKeys(session);
        initializeDirectField(session);

        if (getReferenceTable().getName().indexOf(' ') != -1) {
            //table names contains a space so needs to be quoted.
            String quoteChar = ((DatasourcePlatform)session.getDatasourcePlatform()).getIdentifierQuoteCharacter();
            //Ensure this tablename hasn't already been quoted.
            if (getReferenceTable().getName().indexOf(quoteChar) == -1) {
                getReferenceTable().setName(quoteChar + getReferenceTable().getName() + quoteChar);
            }
        }
        if(this.listOrderField != null) {
            this.initializeListOrderField(session);
        }
        getContainerPolicy().initialize(session, referenceTable);
        if (!hasCustomSelectionQuery()){
            initOrRebuildSelectQuery();
            selectionQuery.setName(getAttributeName());
            
            if (shouldInitializeSelectionCriteria()) {
                initializeSelectionCriteria(session);
                initializeSelectionStatement(session);
            }
            
            if (!getSelectionQuery().hasSessionName()) {
                getSelectionQuery().setSessionName(session.getName());
            }
        }
        if ((getValueConverter() != null) && (getSelectionQuery() instanceof DirectReadQuery)) {
            ((DirectReadQuery)getSelectionQuery()).setValueConverter(getValueConverter());
        }
        initializeDeleteAllQuery(session);
        initializeDeleteQuery(session);
        initializeInsertQuery(session);
        initializeDeleteAtIndexQuery(session);
        initializeUpdateAtIndexQuery(session);
        if (getHistoryPolicy() != null) {
            getHistoryPolicy().initialize(session);
        }
        if (getValueConverter() != null) {
            getValueConverter().initialize(this, session);
        }
        super.initialize(session);
    }

    /**
     * INTERNAL:
     * Initializes listOrderField. 
     * Precondition: listOrderField != null.
     */
    protected void initializeListOrderField(AbstractSession session) {
        // This method is called twice. The second call (by CollectionMapping.initialize) should be ignored because initialization has been already done.
        if(!getContainerPolicy().isOrderedListPolicy() || ((OrderedListContainerPolicy)getContainerPolicy()).getListOrderField() == null) {
            super.initializeListOrderField(session);
        }
    }
    
    /**
     * Initialize delete all query. This query is used to delete the collection of objects from the
     * reference table.
     */
    protected void initializeDeleteAllQuery(AbstractSession session) {
        if (!getDeleteAllQuery().hasSessionName()) {
            getDeleteAllQuery().setSessionName(session.getName());
        }

        if (hasCustomDeleteAllQuery()) {
            return;
        }

        Expression expression = null;
        Expression subExp1;
        Expression subExp2;
        Expression subExpression;
        Expression builder = new ExpressionBuilder();
        SQLDeleteStatement statement = new SQLDeleteStatement();

        // Construct an expression to delete from the relation table.
        for (int index = 0; index < getReferenceKeyFields().size(); index++) {
            DatabaseField referenceKey = getReferenceKeyFields().elementAt(index);
            DatabaseField sourceKey = getSourceKeyFields().elementAt(index);

            subExp1 = builder.getField(referenceKey);
            subExp2 = builder.getParameter(sourceKey);
            subExpression = subExp1.equal(subExp2);

            if (expression == null) {
                expression = subExpression;
            } else {
                expression = expression.and(subExpression);
            }
        }

        statement.setWhereClause(expression);
        statement.setTable(getReferenceTable());
        getDeleteAllQuery().setSQLStatement(statement);
    }

    protected void initializeDeleteQuery(AbstractSession session) {
        if (!getDeleteQuery().hasSessionName()) {
            getDeleteQuery().setSessionName(session.getName());
        }

        if (hasCustomDeleteQuery()) {
            return;
        }

        SQLDeleteStatement statement = new SQLDeleteStatement();
        ExpressionBuilder builder = new ExpressionBuilder();
        Expression expression = createWhereClauseForDeleteQuery(builder);
        statement.setWhereClause(expression);
        statement.setTable(getReferenceTable());
        getDeleteQuery().setSQLStatement(statement);
    }

    protected void initializeDeleteAtIndexQuery(AbstractSession session) {
        if (!getDeleteAtIndexQuery().hasSessionName()) {
            getDeleteAtIndexQuery().setSessionName(session.getName());
        }

        if (hasCustomDeleteAtIndexQuery()) {
            return;
        }

        SQLDeleteStatement statement = new SQLDeleteStatement();
        ExpressionBuilder builder = new ExpressionBuilder();
        Expression expression = createWhereClauseForDeleteQuery(builder);
        expression = expression.and(builder.getField(this.listOrderField).equal(builder.getParameter(this.listOrderField)));
        statement.setWhereClause(expression);
        statement.setTable(getReferenceTable());
        getDeleteAtIndexQuery().setSQLStatement(statement);
    }

    protected void initializeUpdateAtIndexQuery(AbstractSession session) {
        if (!getUpdateAtIndexQuery().hasSessionName()) {
            getUpdateAtIndexQuery().setSessionName(session.getName());
        }

        if (hasCustomUpdateAtIndexQuery()) {
            return;
        }

        SQLUpdateStatement statement = new SQLUpdateStatement();
        ExpressionBuilder builder = new ExpressionBuilder();
        Expression expression = createWhereClauseForDeleteQuery(builder);
        expression = expression.and(builder.getField(this.listOrderField).equal(builder.getParameter(this.listOrderField)));
        statement.setWhereClause(expression);
        statement.setTable(getReferenceTable());
        AbstractRecord modifyRow = new DatabaseRecord();
        modifyRow.add(listOrderField, null);
        statement.setModifyRow(modifyRow);
        getUpdateAtIndexQuery().setSQLStatement(statement);
    }

    /**
     * INTERNAL:
     * Indicates whether getListOrderFieldExpression method should create field expression on table expression.  
     */
    public boolean shouldUseListOrderFieldTableExpression() {
        return true;
    }
        
    protected Expression createWhereClauseForDeleteQuery(ExpressionBuilder builder) {
        Expression directExp = builder.getField(getDirectField()).equal(builder.getParameter(getDirectField()));
        Expression expression = null;

        // Construct an expression to delete from the relation table.
        for (int index = 0; index < getReferenceKeyFields().size(); index++) {
            DatabaseField referenceKey = getReferenceKeyFields().get(index);
            DatabaseField sourceKey = getSourceKeyFields().get(index);

            Expression subExp1 = builder.getField(referenceKey);
            Expression subExp2 = builder.getParameter(sourceKey);
            Expression subExpression = subExp1.equal(subExp2);

            expression = subExpression.and(expression);
        }
        expression = expression.and(directExp);
        return expression;
    }

    /**
     * The field name on the reference table is initialized and cached.
     */
    protected void initializeDirectField(AbstractSession session) throws DescriptorException {
        if (getDirectField() == null) {
            throw DescriptorException.directFieldNameNotSet(this);
        }

        getDirectField().setTable(getReferenceTable());
        getDirectField().setIndex(0);
    }

    /**
     * Initialize insert query. This query is used to insert the collection of objects into the
     * reference table.
     */
    protected void initializeInsertQuery(AbstractSession session) {
        if (!getInsertQuery().hasSessionName()) {
            getInsertQuery().setSessionName(session.getName());
        }

        if (hasCustomInsertQuery()) {
            return;
        }

        SQLInsertStatement statement = new SQLInsertStatement();
        statement.setTable(getReferenceTable());
        AbstractRecord directRow = new DatabaseRecord();
        for (Enumeration referenceEnum = getReferenceKeyFields().elements();
                 referenceEnum.hasMoreElements();) {
            directRow.put((DatabaseField)referenceEnum.nextElement(), null);
        }
        directRow.put(getDirectField(), null);
        if(listOrderField != null) {
            directRow.put(listOrderField, null);
        }
        statement.setModifyRow(directRow);
        getInsertQuery().setSQLStatement(statement);
        getInsertQuery().setModifyRow(directRow);
    }

    /**
     * There is no reference descriptor
     */
    protected void initializeReferenceDescriptor(AbstractSession session) {
        ;
    }

    /**
     * The reference keys on the reference table are initialized
     */
    protected void initializeReferenceKeys(AbstractSession session) throws DescriptorException {
        if (getReferenceKeyFields().size() == 0) {
            throw DescriptorException.noReferenceKeyIsSpecified(this);
        }

        for (Enumeration referenceEnum = getReferenceKeyFields().elements();
                 referenceEnum.hasMoreElements();) {
            DatabaseField field = (DatabaseField)referenceEnum.nextElement();
            if (field.hasTableName() && (!(field.getTableName().equals(getReferenceTable().getName())))) {
                throw DescriptorException.referenceKeyFieldNotProperlySpecified(field, this);
            }
            field.setTable(getReferenceTable());
        }
    }

    /**
     * Set the table qualifier on the reference table if required
     */
    protected void initializeReferenceTable(AbstractSession session) throws DescriptorException {
        Platform platform = session.getDatasourcePlatform();

        if (getReferenceTable() == null) {
            throw DescriptorException.referenceTableNotSpecified(this);
        }

        if (platform.getTableQualifier().length() > 0) {
            if (getReferenceTable().getTableQualifier().length() == 0) {
                getReferenceTable().setTableQualifier(platform.getTableQualifier());
            }
        }
    }

    protected void initializeSelectionCriteria(AbstractSession session) {
        Expression exp1;
        Expression exp2;
        Expression expression;
        Expression criteria = null;
        Enumeration referenceKeysEnum;
        Enumeration sourceKeysEnum;
        ExpressionBuilder base = new ExpressionBuilder();
        TableExpression table = (TableExpression)base.getTable(getReferenceTable());

        referenceKeysEnum = getReferenceKeyFields().elements();
        sourceKeysEnum = getSourceKeyFields().elements();

        for (; referenceKeysEnum.hasMoreElements();) {
            DatabaseField referenceKey = (DatabaseField)referenceKeysEnum.nextElement();
            DatabaseField sourceKey = (DatabaseField)sourceKeysEnum.nextElement();

            exp1 = table.getField(referenceKey);
            exp2 = base.getParameter(sourceKey);
            expression = exp1.equal(exp2);

            if (criteria == null) {
                criteria = expression;
            } else {
                criteria = expression.and(criteria);
            }
        }

        setSelectionCriteria(criteria);
    }

    /**
     * The selection query is initialized
     */
    protected void initializeSelectionQuery(AbstractSession session) {
        // Nothing required.
    }

    protected void initializeSelectionStatement(AbstractSession session) {
        SQLSelectStatement statement = new SQLSelectStatement();
        statement.addTable(getReferenceTable());
        statement.addField((DatabaseField)getDirectField().clone());
        statement.setWhereClause(getSelectionCriteria());
        if(listOrderField != null) {
            Expression expField = getListOrderFieldExpression(statement.getBuilder());
            statement.addField(expField);
        }
        statement.normalize(session, null);
        getSelectionQuery().setSQLStatement(statement);
    }

    /**
     * The source keys are initialized
     */
    protected void initializeSourceKeys(AbstractSession session) {
        for (int index = 0; index < getSourceKeyFields().size(); index++) {
            DatabaseField field = getDescriptor().buildField(getSourceKeyFields().get(index));
            getSourceKeyFields().set(index, field);
        }
    }

    /**
     * INTERNAL:
     * If a user does not specify the source key then the primary keys of the source table are used.
     */
    protected void initializeSourceKeysWithDefaults(AbstractSession session) {
        List<DatabaseField> primaryKeyFields = getDescriptor().getPrimaryKeyFields();
        for (int index = 0; index < primaryKeyFields.size(); index++) {
            getSourceKeyFields().addElement(primaryKeyFields.get(index));
        }
    }

    /**
     * INTERNAL:
     */
    public boolean isDirectCollectionMapping() {
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
     * Checks if source and target keys are mentioned by the user or not.
     */
    protected boolean isKeyForSourceSpecified() {
        return !getSourceKeyFields().isEmpty();
    }

    /**
     * INTERNAL:
     * Return whether this mapping should be traversed when we are locking
     * @return
     */
    public boolean isLockableMapping(){
        return false;
    }
    
    /**
     * INTERNAL:
     * Direct collection is always private owned.
     */
    public boolean isPrivateOwned() {
        return true;
    }

    /**
     * INTERNAL:
     * Iterate on the attribute value.
     * The value holder has already been processed.
     * PERF: Avoid iteration if not required.
     */
    public void iterateOnRealAttributeValue(DescriptorIterator iterator, Object realAttributeValue) {
        if (iterator.shouldIterateOnPrimitives()) {
            super.iterateOnRealAttributeValue(iterator, realAttributeValue);
        }
    }

    /**
     * INTERNAL:
     * Iterate on the specified element.
     */
    public void iterateOnElement(DescriptorIterator iterator, Object element) {
        iterator.iteratePrimitiveForMapping(element, this);
    }

    /**
     * INTERNAL:
     * Merge changes from the source to the target object.
     * Because this is a collection mapping, values are added to or removed from the
     * collection based on the changeset
     */
    public void mergeChangesIntoObject(Object target, ChangeRecord changeRecord, Object source, MergeManager mergeManager) {
        ContainerPolicy containerPolicy = getContainerPolicy();
        Object valueOfTarget = null;
        AbstractSession session = mergeManager.getSession();

        //collect the changes into a vector
        HashMap addObjects = ((DirectCollectionChangeRecord)changeRecord).getAddObjectMap();
        HashMap removeObjects = ((DirectCollectionChangeRecord)changeRecord).getRemoveObjectMap();

        //Check to see if the target has an instantiated collection
        if ((isAttributeValueInstantiated(target)) && (!changeRecord.getOwner().isNew())) {
            valueOfTarget = getRealCollectionAttributeValueFromObject(target, session);
        } else {
            //if not create an instance of the collection
            valueOfTarget = containerPolicy.containerInstance(addObjects.size());
        }
        if (!isAttributeValueInstantiated(target)) {
            if (mergeManager.shouldMergeChangesIntoDistributedCache()) {
                return;
            }
            for (Object iterator = containerPolicy.iteratorFor(getRealCollectionAttributeValueFromObject(source, session));
                     containerPolicy.hasNext(iterator);) {
                containerPolicy.addInto(containerPolicy.next(iterator, session), valueOfTarget, session);
            }
        } else {
            Object synchronizationTarget = valueOfTarget;
            // For indirect containers the delegate must be synchronized on,
            // not the wrapper as the clone synchs on the delegate, see bug#5685287.
            if (valueOfTarget instanceof IndirectCollection) {
                synchronizationTarget = ((IndirectCollection)valueOfTarget).getDelegateObject();
                if(((DirectCollectionChangeRecord)changeRecord).orderHasBeenRepaired()) {
                    if(valueOfTarget instanceof IndirectList) {
                        ((IndirectList)valueOfTarget).setIsListOrderBrokenInDb(false);
                    }
                }
            }
            synchronized (synchronizationTarget) {
                // Next iterate over the changes and add them to the container
                for (Iterator iterator = addObjects.keySet().iterator(); iterator.hasNext();) {
                    Object object = iterator.next();
                    int objectCount = ((Integer)addObjects.get(object)).intValue();
                    for (int i = 0; i < objectCount; ++i) {
                        if (mergeManager.shouldMergeChangesIntoDistributedCache()) {
                            //bug#4458089 and 4544532- check if collection contains new item before adding during merge into distributed cache					
                            if (!containerPolicy.contains(object, valueOfTarget, session)) {
                                containerPolicy.addInto(object, valueOfTarget, session);
                            }
                        } else {
                            containerPolicy.addInto(object, valueOfTarget, session);
                        }
                    }
                }
                for (Iterator iterator = removeObjects.keySet().iterator(); iterator.hasNext();) {
                    Object object = iterator.next();
                    int objectCount = ((Integer)removeObjects.get(object)).intValue();
                    for (int i = 0; i < objectCount; ++i) {
                        containerPolicy.removeFrom(object, valueOfTarget, session);
                    }
                }
                if(this.listOrderField != null && ((DirectCollectionChangeRecord)changeRecord).getChangedIndexes() == null) {
                    this.compareListsForChange((List)((DirectCollectionChangeRecord)changeRecord).getOriginalCollection(), (List)((DirectCollectionChangeRecord)changeRecord).getLatestCollection(), changeRecord, session);
                }
                
                if(((DirectCollectionChangeRecord)changeRecord).getChangedIndexes() != null) {
                    int oldSize = ((DirectCollectionChangeRecord)changeRecord).getOldSize();
                    int newSize = ((DirectCollectionChangeRecord)changeRecord).getNewSize();
                    int delta = newSize - oldSize;
                    Object newTail[] = null;
                    if(delta > 0) {
                        newTail = new Object[delta];
                    }
                    Iterator<Map.Entry<Object, Set[]>> it = ((DirectCollectionChangeRecord)changeRecord).getChangedIndexes().entrySet().iterator();
                    while(it.hasNext()) {
                        Map.Entry<Object, Set[]> entry = it.next();
                        Object value = entry.getKey();
                        Set[] indexes = entry.getValue();
                        Set indexesAfter = indexes[1];
                        if(indexesAfter != null) {
                            Iterator<Integer> itIndexesAfter = indexesAfter.iterator();
                            while(itIndexesAfter.hasNext()) {
                                int index = itIndexesAfter.next();
                                if(index < oldSize) {
                                    ((List)synchronizationTarget).set(index, value);
                                } else {
                                    newTail[index - oldSize] = value;
                                }
                            }
                        }
                    }
                    if(delta > 0) {
                        for(int i=0; i < delta; i++) {
                            ((List)synchronizationTarget).add(newTail[i]);
                        }
                    } else if(delta < 0) {
                        for(int i=oldSize -1 ; i >= newSize; i--) {
                            ((List)synchronizationTarget).remove(i);
                        }
                    }
                }
            }
        }
        setRealAttributeValueInObject(target, valueOfTarget);
    }

    /**
     * INTERNAL:
     * Merge changes from the source to the target object.
     */
    public void mergeIntoObject(Object target, boolean isTargetUnInitialized, Object source, MergeManager mergeManager) {
        if (isTargetUnInitialized) {
            // This will happen if the target object was removed from the cache before the commit was attempted
            if (mergeManager.shouldMergeWorkingCopyIntoOriginal() && (!isAttributeValueInstantiated(source))) {
                setAttributeValueInObject(target, getIndirectionPolicy().getOriginalIndirectionObject(getAttributeValueFromObject(source), mergeManager.getSession()));
                return;
            }
        }
        if (!shouldMergeCascadeReference(mergeManager)) {
            // This is only going to happen on mergeClone, and we should not attempt to merge the reference
            return;
        }
        if (mergeManager.shouldRefreshRemoteObject() && usesIndirection()) {
            mergeRemoteValueHolder(target, source, mergeManager);
            return;
        }
        if (mergeManager.shouldMergeOriginalIntoWorkingCopy()) {
            if (!isAttributeValueInstantiated(target)) {
                // This will occur when the clone's value has not been instantiated yet and we do not need
                // the refresh that attribute
                return;
            }
        } else if (!isAttributeValueInstantiatedOrChanged(source)) {
            // I am merging from a clone into an original.  No need to do merge if the attribute was never
            // modified
            return;
        }

        ContainerPolicy containerPolicy = getContainerPolicy();
        Object valueOfSource = getRealCollectionAttributeValueFromObject(source, mergeManager.getSession());

        // trigger instantiation of target attribute
        Object valueOfTarget = getRealCollectionAttributeValueFromObject(target, mergeManager.getSession());
        Object newContainer = containerPolicy.containerInstance(containerPolicy.sizeFor(valueOfSource));
        boolean fireChangeEvents = false;
        if ((this.descriptor.getObjectChangePolicy().isObjectChangeTrackingPolicy()) && (target instanceof ChangeTracker) && (((ChangeTracker)target)._persistence_getPropertyChangeListener() != null)) {
            fireChangeEvents = true;
            //Collections may not be indirect list or may have been replaced with user collection.
            Object iterator = containerPolicy.iteratorFor(valueOfTarget);
            while (containerPolicy.hasNext(iterator)) {
                ((ObjectChangeListener)((ChangeTracker)target)._persistence_getPropertyChangeListener()).internalPropertyChange(new CollectionChangeEvent(target, getAttributeName(), valueOfTarget, containerPolicy.next(iterator, mergeManager.getSession()), CollectionChangeEvent.REMOVE));// make the remove change event fire.
            }
            if (newContainer instanceof ChangeTracker) {
                ((ChangeTracker)newContainer)._persistence_setPropertyChangeListener(((ChangeTracker)target)._persistence_getPropertyChangeListener());
            }
            if (valueOfTarget instanceof ChangeTracker) {
                ((ChangeTracker)valueOfTarget)._persistence_setPropertyChangeListener(null);//remove listener 
            }
        }
        valueOfTarget = newContainer;
        for (Object sourceValuesIterator = containerPolicy.iteratorFor(valueOfSource);
                 containerPolicy.hasNext(sourceValuesIterator);) {
            Object sourceValue = containerPolicy.next(sourceValuesIterator, mergeManager.getSession());
            if (fireChangeEvents) {
                //Collections may not be indirect list or may have been replaced with user collection.
                ((ObjectChangeListener)((ChangeTracker)target)._persistence_getPropertyChangeListener()).internalPropertyChange(new CollectionChangeEvent(target, getAttributeName(), valueOfTarget, sourceValue, CollectionChangeEvent.ADD));// make the add change event fire.
            }
            containerPolicy.addInto(sourceValue, valueOfTarget, mergeManager.getSession());
        }
        if (fireChangeEvents && (this.descriptor.getObjectChangePolicy().isAttributeChangeTrackingPolicy())) {
            // check that there were changes, if not then remove the record.
            ObjectChangeSet changeSet = ((AttributeChangeListener)((ChangeTracker)target)._persistence_getPropertyChangeListener()).getObjectChangeSet();
            if (changeSet != null) {
                DirectCollectionChangeRecord changeRecord = (DirectCollectionChangeRecord)changeSet.getChangesForAttributeNamed(getAttributeName());
                if (changeRecord != null) {
                    if (!changeRecord.isDeferred()) {
                        if (!changeRecord.hasChanges()) {
                            changeSet.removeChange(getAttributeName());
                        }
                    } else {
                        // Must reset the latest collection.
                        changeRecord.setLatestCollection(valueOfTarget);
                    }
                }
            }
        }

        // Must re-set variable to allow for set method to re-morph changes if the collection is not being stored directly.
        setRealAttributeValueInObject(target, valueOfTarget);
    }

    /**
     * INTERNAL:
     * Perform the commit event.
     * This is used in the uow to delay data modifications.
     */
    public void performDataModificationEvent(Object[] event, AbstractSession session) throws DatabaseException, DescriptorException {
        // Hey I might actually want to use an inner class here... ok array for now.
        if (event[0] == Delete) {
            session.executeQuery((DataModifyQuery)event[1], (AbstractRecord)event[2]);
            if ((getHistoryPolicy() != null) && getHistoryPolicy().shouldHandleWrites()) {
                getHistoryPolicy().mappingLogicalDelete((DataModifyQuery)event[1], (AbstractRecord)event[2], session);
            }
        } else if (event[0] == Insert) {
            session.executeQuery((DataModifyQuery)event[1], (AbstractRecord)event[2]);
            if ((getHistoryPolicy() != null) && getHistoryPolicy().shouldHandleWrites()) {
                getHistoryPolicy().mappingLogicalInsert((DataModifyQuery)event[1], (AbstractRecord)event[2], session);
            }
        } else if (event[0] == DeleteAll) {
            preDelete((DeleteObjectQuery)event[1]);
        } else if (event[0] == DeleteAtIndex) {
            session.executeQuery((DataModifyQuery)event[1], (AbstractRecord)event[2]);
        } else if (event[0] == UpdateAtIndex) {
            DataModifyQuery updateAtIndexQuery = (DataModifyQuery)((DataModifyQuery)event[1]).clone();
            updateAtIndexQuery.setModifyRow((AbstractRecord)event[3]);
            updateAtIndexQuery.setHasModifyRow(true);
            updateAtIndexQuery.setIsExecutionClone(true);
            session.executeQuery(updateAtIndexQuery, (AbstractRecord)event[2]);
        } else {
            throw DescriptorException.invalidDataModificationEventCode(event[0], this);
        }
    }

    /**
     * INTERNAL:
     * Overridden by mappings that require additional processing of the change record after the record has been calculated.
     */
    @Override
    public void postCalculateChanges(org.eclipse.persistence.sessions.changesets.ChangeRecord changeRecord, UnitOfWorkImpl uow) {
        //no -op for this collection type
    }

    /**
     * INTERNAL:
     * Insert the private owned object.
     */
    public void postInsert(WriteObjectQuery query) throws DatabaseException {
        Object objects;
        AbstractRecord databaseRow = new DatabaseRecord();

        if (isReadOnly()) {
            return;
        }

        objects = getRealCollectionAttributeValueFromObject(query.getObject(), query.getSession());
        ContainerPolicy containerPolicy = getContainerPolicy();
        if (containerPolicy.isEmpty(objects)) {
            return;
        }

        prepareTranslationRow(query.getTranslationRow(), query.getObject(), query.getSession());
        // Extract primary key and value from the source.
        for (int index = 0; index < getReferenceKeyFields().size(); index++) {
            DatabaseField referenceKey = getReferenceKeyFields().get(index);
            DatabaseField sourceKey = getSourceKeyFields().get(index);
            Object sourceKeyValue = query.getTranslationRow().get(sourceKey);
            databaseRow.put(referenceKey, sourceKeyValue);
        }

        int orderIndex = 0;
        // Extract target field and its value. Construct insert statement and execute it
        for (Object iter = containerPolicy.iteratorFor(objects); containerPolicy.hasNext(iter);) {
            Object wrappedObject = containerPolicy.nextEntry(iter, query.getSession());
            Object object = containerPolicy.unwrapIteratorResult(wrappedObject);
            if (getValueConverter() != null) {
                object = getValueConverter().convertObjectValueToDataValue(object, query.getSession());
            }
            databaseRow.put(getDirectField(), object);

            // In the uow data queries are cached until the end of the commit.
            if (query.shouldCascadeOnlyDependentParts()) {
                // Hey I might actually want to use an inner class here... ok array for now.
                Object[] event = new Object[3];
                event[0] = Insert;
                event[1] = getInsertQuery();
                event[2] = databaseRow.clone();
                if(listOrderField != null) {
                    ((AbstractRecord)event[2]).put(listOrderField, orderIndex++);
                }
                query.getSession().getCommitManager().addDataModificationEvent(this, event);
            } else {
                query.getSession().executeQuery(getInsertQuery(), databaseRow);
                if ((getHistoryPolicy() != null) && getHistoryPolicy().shouldHandleWrites()) {
                    getHistoryPolicy().mappingLogicalInsert(getInsertQuery(), databaseRow, query.getSession());
                }
            }
            containerPolicy.propogatePostInsert(query, wrappedObject);
        }
    }

    /**
     * INTERNAL:
     * Update private owned part.
     */
    public void postUpdate(WriteObjectQuery writeQuery) throws DatabaseException {
        if (isReadOnly()) {
            return;
        }

        if (writeQuery.getObjectChangeSet() != null) {
            if(this.listOrderField != null) {
                postUpdateWithChangeSetListOrder(writeQuery);
            } else {
                postUpdateWithChangeSet(writeQuery);
            }
            return;
        }

        // If objects are not instantiated that means they are not changed.
        if (!isAttributeValueInstantiatedOrChanged(writeQuery.getObject())) {
            return;
        }

        if (writeQuery.getSession().isUnitOfWork()) {
            if (compareObjects(writeQuery.getObject(), writeQuery.getBackupClone(), writeQuery.getSession())) {
                return;// Nothing has changed, no work required
            }
        }

        DeleteObjectQuery deleteQuery = new DeleteObjectQuery();
        deleteQuery.setObject(writeQuery.getObject());
        deleteQuery.setSession(writeQuery.getSession());
        deleteQuery.setTranslationRow(writeQuery.getTranslationRow());

        if (writeQuery.shouldCascadeOnlyDependentParts()) {
            // Hey I might actually want to use an inner class here... ok array for now.
            Object[] event = new Object[3];
            event[0] = DeleteAll;
            event[1] = deleteQuery;
            writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
        } else {
            preDelete(deleteQuery);
        }
        postInsert(writeQuery);
    }

    /**
     * INTERNAL:
     * Update private owned part.
     */
    protected void postUpdateWithChangeSet(WriteObjectQuery writeQuery) throws DatabaseException {
        ObjectChangeSet changeSet = writeQuery.getObjectChangeSet();
        DirectCollectionChangeRecord changeRecord = (DirectCollectionChangeRecord)changeSet.getChangesForAttributeNamed(this.getAttributeName());
        if (changeRecord == null) {
            return;
        }
        for (int index = 0; index < getReferenceKeyFields().size(); index++) {
            DatabaseField referenceKey = getReferenceKeyFields().get(index);
            DatabaseField sourceKey = getSourceKeyFields().get(index);
            Object sourceKeyValue = writeQuery.getTranslationRow().get(sourceKey);
            writeQuery.getTranslationRow().put(referenceKey, sourceKeyValue);
        }
        for (Iterator iterator = changeRecord.getRemoveObjectMap().keySet().iterator();
                 iterator.hasNext();) {
            Object object = iterator.next();
            AbstractRecord thisRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
            Object value = object;
            if (getValueConverter() != null) {
                value = getValueConverter().convertObjectValueToDataValue(value, writeQuery.getSession());
            }
            thisRow.add(getDirectField(), value);

            // Hey I might actually want to use an inner class here... ok array for now.
            Object[] event = new Object[3];
            event[0] = Delete;
            event[1] = getDeleteQuery();
            event[2] = thisRow;
            writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
            Integer count = (Integer)changeRecord.getCommitAddMap().get(object);
            if (count != null) {
                for (int counter = count.intValue(); counter > 0; --counter) {
                    thisRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
                    thisRow.add(getDirectField(), value);
                    // Hey I might actually want to use an inner class here... ok array for now.
                    event = new Object[3];
                    event[0] = Insert;
                    event[1] = getInsertQuery();
                    event[2] = thisRow;
                    writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
                }
            }
        }
        for (Iterator iterator = changeRecord.getAddObjectMap().keySet().iterator();
                 iterator.hasNext();) {
            Object object = iterator.next();
            Integer count = (Integer)changeRecord.getAddObjectMap().get(object);
            for (int counter = count.intValue(); counter > 0; --counter) {
            	AbstractRecord thisRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
                Object value = object;
                if (getValueConverter() != null) {
                    value = getValueConverter().convertObjectValueToDataValue(value, writeQuery.getSession());
                }
                thisRow.add(getDirectField(), value);

                // Hey I might actually want to use an inner class here... ok array for now.
                Object[] event = new Object[3];
                event[0] = Insert;
                event[1] = getInsertQuery();
                event[2] = thisRow;
                writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
            }
        }
    }

    /**
     * INTERNAL:
     * Update private owned part.
     */
    protected void postUpdateWithChangeSetListOrder(WriteObjectQuery writeQuery) throws DatabaseException {
        ObjectChangeSet changeSet = writeQuery.getObjectChangeSet();
        DirectCollectionChangeRecord changeRecord = (DirectCollectionChangeRecord)changeSet.getChangesForAttributeNamed(this.getAttributeName());
        if (changeRecord == null) {
            return;
        }
        for (int index = 0; index < getReferenceKeyFields().size(); index++) {
            DatabaseField referenceKey = getReferenceKeyFields().get(index);
            DatabaseField sourceKey = getSourceKeyFields().get(index);
            Object sourceKeyValue = writeQuery.getTranslationRow().get(sourceKey);
            writeQuery.getTranslationRow().put(referenceKey, sourceKeyValue);
        }

        boolean shouldRepairOrder = false;
        if((List)changeRecord.getLatestCollection() instanceof IndirectList) {
            shouldRepairOrder = ((IndirectList)changeRecord.getLatestCollection()).isListOrderBrokenInDb();
        }
        if(shouldRepairOrder) {
            // delete all members of collection
            DeleteObjectQuery deleteQuery = new DeleteObjectQuery();
            deleteQuery.setObject(writeQuery.getObject());
            deleteQuery.setSession(writeQuery.getSession());
            deleteQuery.setTranslationRow(writeQuery.getTranslationRow());
            // Hey I might actually want to use an inner class here... ok array for now.
            Object[] eventDeleteAll = new Object[2];
            eventDeleteAll[0] = DeleteAll;
            eventDeleteAll[1] = deleteQuery;
            writeQuery.getSession().getCommitManager().addDataModificationEvent(this, eventDeleteAll);
            
            // re-insert them back
            for(int i=0; i < ((List)changeRecord.getLatestCollection()).size(); i++) {
                Object value = ((List)changeRecord.getLatestCollection()).get(i);
                if (getValueConverter() != null) {
                    value = getValueConverter().convertObjectValueToDataValue(value, writeQuery.getSession());
                }
                AbstractRecord insertRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
                insertRow.add(getDirectField(), value);
                insertRow.add(this.listOrderField, i);
                // Hey I might actually want to use an inner class here... ok array for now.
                Object[] event = new Object[3];
                event[0] = Insert;
                event[1] = getInsertQuery();
                event[2] = insertRow;
                writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
            }
            
            ((IndirectList)changeRecord.getLatestCollection()).setIsListOrderBrokenInDb(false);
            changeRecord.setOrderHasBeenRepaired(true);
            return;
        }
        
        if(changeRecord.getChangedIndexes() == null) {
            compareListsForChange((List)changeRecord.getOriginalCollection(), (List)changeRecord.getLatestCollection(), changeRecord, writeQuery.getSession());
        }
        
        Iterator<Map.Entry<Object, Set[]>> it = changeRecord.getChangedIndexes().entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<Object, Set[]> entry = it.next();
            Object value = entry.getKey();
            if (getValueConverter() != null) {
                value = getValueConverter().convertObjectValueToDataValue(value, writeQuery.getSession());
            }

            Set[] indexes = entry.getValue();
            Set indexesBefore = indexes[0];
            Set indexesAfter = indexes[1];
            
            if(indexesAfter == null) {
                // All copies of the target object deleted - don't need to verify order field contents.
                AbstractRecord deleteRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
                deleteRow.add(getDirectField(), value);
                // Hey I might actually want to use an inner class here... ok array for now.
                Object[] event = new Object[3];
                event[0] = Delete;
                event[1] = getDeleteQuery();
                event[2] = deleteRow;
                writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
            } else if(indexesAfter.isEmpty()) {
                // Some copies of the target objects should be deleted, some left in the db
                Iterator<Integer> itBefore = indexesBefore.iterator();
                while(itBefore.hasNext()) {
                    AbstractRecord deleteAtIndexRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
                    deleteAtIndexRow.add(getDirectField(), value);
                    deleteAtIndexRow.add(this.listOrderField, itBefore.next());
                    // Hey I might actually want to use an inner class here... ok array for now.
                    Object[] event = new Object[3];
                    event[0] = DeleteAtIndex;
                    event[1] = deleteAtIndexQuery;
                    event[2] = deleteAtIndexRow;
                    writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
                }
            } else {
                if(indexesBefore == null || indexesBefore.isEmpty()) {
                    // insert the object for each index in indexesAfter
                    Iterator<Integer> itAfter = indexesAfter.iterator();
                    while(itAfter.hasNext()) {
                        AbstractRecord insertRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
                        insertRow.add(getDirectField(), value);
                        insertRow.add(this.listOrderField, itAfter.next());
                        // Hey I might actually want to use an inner class here... ok array for now.
                        Object[] event = new Object[3];
                        event[0] = Insert;
                        event[1] = getInsertQuery();
                        event[2] = insertRow;
                        writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
                    }
                } else {
                    Iterator<Integer> itBefore = indexesBefore.iterator();
                    Iterator<Integer> itAfter = indexesAfter.iterator();
                    while(itBefore.hasNext() || itAfter.hasNext()) {
                        if(itBefore.hasNext()) {
                            if(itAfter.hasNext()) {
                                // update the object changing index from indexBefore to indexAfter
                                AbstractRecord updateAtIndexRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
                                updateAtIndexRow.add(getDirectField(), value);
                                updateAtIndexRow.add(this.listOrderField, itBefore.next());
                                // Hey I might actually want to use an inner class here... ok array for now.
                                Object[] event = new Object[4];
                                event[0] = UpdateAtIndex;
                                event[1] = updateAtIndexQuery;
                                event[2] = updateAtIndexRow;
                                DatabaseRecord modifyRow = new DatabaseRecord(1);
                                modifyRow.add(this.listOrderField, itAfter.next());
                                event[3] = modifyRow;
                                writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
                            } else {
                                // delete the object at indexBefore
                                AbstractRecord deleteAtIndexRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
                                deleteAtIndexRow.add(getDirectField(), value);
                                deleteAtIndexRow.add(this.listOrderField, itBefore.next());
                                // Hey I might actually want to use an inner class here... ok array for now.
                                Object[] event = new Object[3];
                                event[0] = DeleteAtIndex;
                                event[1] = deleteAtIndexQuery;
                                event[2] = deleteAtIndexRow;
                                writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
                            }
                        } else {
                            // itAfter.hasNext() must be true
                            // insert the object at indexAfter
                            AbstractRecord insertRow = (AbstractRecord)writeQuery.getTranslationRow().clone();
                            insertRow.add(getDirectField(), value);
                            insertRow.add(this.listOrderField, itAfter.next());
                            // Hey I might actually want to use an inner class here... ok array for now.
                            Object[] event = new Object[3];
                            event[0] = Insert;
                            event[1] = getInsertQuery();
                            event[2] = insertRow;
                            writeQuery.getSession().getCommitManager().addDataModificationEvent(this, event);
                        }
                    }
                }
            }
        }
    }

    /**
     * INTERNAL:
     * Delete private owned part. Which is a collection of objects from the reference table.
     */
    public void preDelete(DeleteObjectQuery query) throws DatabaseException {
        if (isReadOnly()) {
            return;
        }

        prepareTranslationRow(query.getTranslationRow(), query.getObject(), query.getSession());
        query.getSession().executeQuery(getDeleteAllQuery(), query.getTranslationRow());
        if ((getHistoryPolicy() != null) && getHistoryPolicy().shouldHandleWrites()) {
            getHistoryPolicy().mappingLogicalDelete(getDeleteAllQuery(), query.getTranslationRow(), query.getSession());
        }
    }

    /**
     * INTERNAL:
     * The translation row may require additional fields than the primary key if the mapping in not on the primary key.
     */
    protected void prepareTranslationRow(AbstractRecord translationRow, Object object, AbstractSession session) {
        // Make sure that each source key field is in the translation row.
        for (Enumeration sourceFieldsEnum = getSourceKeyFields().elements();
                 sourceFieldsEnum.hasMoreElements();) {
            DatabaseField sourceKey = (DatabaseField)sourceFieldsEnum.nextElement();
            if (!translationRow.containsKey(sourceKey)) {
                Object value = getDescriptor().getObjectBuilder().extractValueFromObjectForField(object, sourceKey, session);
                translationRow.put(sourceKey, value);
            }
        }
    }

    /**
     * INTERNAL:
     * Used by DirectMapMapping to rebuild select query.
     */
    protected void initOrRebuildSelectQuery() {        
        this.selectionQuery.setSQLStatement(new SQLSelectStatement());
    }
    
    /**
     * INTERNAL:
     * Overridden by mappings that require additional processing of the change record after the record has been calculated.
     */
    @Override
    public void recordPrivateOwnedRemovals(Object object, UnitOfWorkImpl uow) {
    }
    /**
     * INTERNAL:
     * Once descriptors are serialized to the remote session. All its mappings and reference descriptors are traversed. Usually
     * mappings are initialized and serialized reference descriptors are replaced with local descriptors if they already exist on the
     * remote session.
     */
    public void remoteInitialization(DistributedSession session) {
        // Remote mappings is initialized here again because while serializing only the uninitialized data is passed
        // as the initialized data is not serializable.
        if (!isRemotelyInitialized()) {
            getAttributeAccessor().initializeAttributes(getDescriptor().getJavaClass());
            remotelyInitialized();
        }
    }

    /**
     * INTERNAL:
     * replace the value holders in the specified reference object(s)
     */
    public Map replaceValueHoldersIn(Object object, RemoteSessionController controller) {
        // do nothing, since direct collections do not hold onto other domain objects
        return null;
    }

    protected void setDeleteQuery(ModifyQuery query) {
        this.changeSetDeleteQuery = query;
    }

    /**
     * PUBLIC:
     * Set the receiver's delete SQL string. This allows the user to override the SQL
     * generated by TopLink, with there own SQL or procedure call. The arguments are
     * translated from the fields of the source row, through replacing the field names
     * marked by '#' with the values for those fields.
     * This SQL is responsible for doing the deletion required by the mapping,
     * such as deletion from join table for M-M.
     * Example, 'delete from RESPONS where EMP_ID = #EMP_ID and DESCRIP = #DESCRIP'.
     */
    public void setDeleteSQLString(String sqlString) {
        DataModifyQuery query = new DataModifyQuery();
        query.setSQLString(sqlString);
        setCustomDeleteQuery(query);
    }

    /**
     * ADVANCED:
     * Configure the mapping to use a container policy.
     * The policy manages the access to the collection.
     */
    public void setContainerPolicy(ContainerPolicy containerPolicy) {
        this.containerPolicy = containerPolicy;
        if (selectionQuery.isDataReadQuery()){
            ((DataReadQuery) getSelectionQuery()).setContainerPolicy(containerPolicy);
        }
    }

    /**
     * PUBLIC:
     * The default delete query for this mapping can be overridden by specifying the new query.
     * This query is responsible for doing the deletion required by the mapping,
     * such as deletion from join table for M-M.  The query should delete a specific row from the
     * DirectCollectionTable bases on the DirectField.
     */
    public void setCustomDeleteQuery(ModifyQuery query) {
        setDeleteQuery(query);
        setHasCustomDeleteQuery(true);
    }

    /**
     * PUBLIC:
     * The default delete by index query for this mapping can be overridden by specifying the new query.
     * This query used (only in case listOrderField != null) to delete object with particular orderFieldValue.
     */
    public void setCustomDeleteAtIndexQuery(ModifyQuery query) {
        this.deleteAtIndexQuery = query;
        hasCustomDeleteAtIndexQuery = true;
    }

    /**
     * PUBLIC:
     * The default insert query for mapping can be overridden by specifying the new query.
     * This query inserts the row into the direct table.
     */
    public void setCustomInsertQuery(DataModifyQuery query) {
        setInsertQuery(query);
        setHasCustomInsertQuery(true);
    }

    /**
     * PUBLIC:
     * The default delete by index query for this mapping can be overridden by specifying the new query.
     * This query used (only in case listOrderField != null) to update orderFieldValue of object with particular orderFieldValue.
     */
    public void setCustomUpdateAtIndexQuery(ModifyQuery query) {
        this.updateAtIndexQuery = query;
        hasCustomUpdateAtIndexQuery = true;
    }

    /**
     * PUBLIC:
     * Set the direct field in the reference table.
     * This is the field that the primitive data value is stored in.
     */
    public void setDirectField(DatabaseField field) {
        directField = field;
    }
    
    /**
     * ADVANCED:
     * Set the class type of the field value.
     * This can be used if field value differs from the object value,
     * has specific typing requirements such as usage of java.sql.Blob or NChar.
     * This must be called after the field name has been set.
     */
    public void setDirectFieldClassification(Class fieldType) {
        getDirectField().setType(fieldType);
    }

    /**
     * PUBLIC:
     * Set the direct field name in the reference table.
     * This is the field that the primitive data value is stored in.
     */
    public void setDirectFieldName(String fieldName) {
        setDirectField(new DatabaseField(fieldName));
    }

    protected void setHasCustomDeleteQuery(boolean bool) {
        hasCustomDeleteQuery = bool;
    }

    protected void setHasCustomInsertQuery(boolean bool) {
        hasCustomInsertQuery = bool;
    }

    protected void setInsertQuery(DataModifyQuery insertQuery) {
        this.insertQuery = insertQuery;
    }

    /**
     * PUBLIC:
     * Set the receiver's insert SQL string. This allows the user to override the SQL
     * generated by TopLink, with there own SQL or procedure call. The arguments are
     * translated from the fields of the source row, through replacing the field names
     * marked by '#' with the values for those fields.
     * This is used to insert an entry into the direct table.
     * <p>Example, 'insert into RESPONS (EMP_ID, RES_DESC) values (#EMP_ID, #RES_DESC)'.
     */
    public void setInsertSQLString(String sqlString) {
        DataModifyQuery query = new DataModifyQuery();
        query.setSQLString(sqlString);
        setCustomInsertQuery(query);
    }

    /**
     * INTERNAL:
     * This cannot be used with direct collection mappings.
     */
    public void setReferenceClass(Class referenceClass) {
        return;
    }

    public void setReferenceClassName(String referenceClassName) {
        return;
    }

    /**
     * PUBLIC:
     * Set the name of the reference key field.
     * This is the foreign key field in the direct table referencing the primary key of the source object.
     * This method is used if the reference key consists of only a single field.
     */
    public void setReferenceKeyFieldName(String fieldName) {
        getReferenceKeyFields().addElement(new DatabaseField(fieldName));
    }

    /**
     * INTERNAL:
     * Set the reference key field names associated with the mapping.
     * These must be in-order with the sourceKeyFieldNames.
     */
    public void setReferenceKeyFieldNames(Vector fieldNames) {
        Vector fields = org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance(fieldNames.size());
        for (Enumeration fieldNamesEnum = fieldNames.elements(); fieldNamesEnum.hasMoreElements();) {
            fields.addElement(new DatabaseField((String)fieldNamesEnum.nextElement()));
        }

        setReferenceKeyFields(fields);
    }

    /**
     * INTERNAL:
     * Set the reference fields.
     */
    public void setReferenceKeyFields(Vector<DatabaseField> aVector) {
        this.referenceKeyFields = aVector;
    }

    /**
     * INTERNAL:
     * Set the reference table.
     */
    public void setReferenceTable(DatabaseTable table) {
        referenceTable = table;
    }

    /**
     * PUBLIC:
     * Sets the selection criteria to be used as a where clause to read
     * reference objects. This criteria is automatically generated by the
     * TopLink if not explicitly specified by the user.
     */
    public void setSelectionCriteria(Expression anExpression) {
        if (getSelectionQuery().isReadAllQuery()){
            ((ReadAllQuery)getSelectionQuery()).setSelectionCriteria(anExpression);
        } else {
            getSelectionQuery().getSQLStatement().setWhereClause(anExpression);
        }
    }
    /**
     * PUBLIC:
     * Set the reference table name.
     */
    public void setReferenceTableName(String tableName) {
        if (tableName == null) {
            setReferenceTable(null);
        } else {
            setReferenceTable(new DatabaseTable(tableName));
        }
    }

    /**
     * INTERNAL:
     * Set the container policy on the selection query for this mapping.
     */
    protected void setSelectionQueryContainerPolicy(ContainerPolicy containerPolicy) {
        ((DataReadQuery) getSelectionQuery()).setContainerPolicy(containerPolicy);
    }
    
    /**
     * PUBLIC:
     */
    public void setHistoryPolicy(HistoryPolicy policy) {
        this.historyPolicy = policy;
        if (policy != null) {
            policy.setMapping(this);
        }
    }

    /**
     * PUBLIC:
     * Set the name of the session to execute the mapping's queries under.
     * This can be used by the session broker to override the default session
     * to be used for the target class.
     */
    public void setSessionName(String name) {
        super.setSessionName(name);
        getInsertQuery().setSessionName(name);
    }

    /**
     * INTERNAL:
     * Set the source key field names associated with the mapping.
     * These must be in-order with the referenceKeyFieldNames.
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
     * Set the source fields.
     */
    public void setSourceKeyFields(Vector<DatabaseField> sourceKeyFields) {
        this.sourceKeyFields = sourceKeyFields;
    }

    /**
     * INTERNAL:
     * Used by AttributeLevelChangeTracking to update a changeRecord with calculated changes
     * as apposed to detected changes.  If an attribute can not be change tracked it's
     * changes can be detected through this process.
     */
    public void calculateDeferredChanges(ChangeRecord changeRecord, AbstractSession session) {
        DirectCollectionChangeRecord collectionRecord = (DirectCollectionChangeRecord)changeRecord;
        // TODO: Handle events that fired after collection was replaced.
        compareCollectionsForChange(collectionRecord.getOriginalCollection(), collectionRecord.getLatestCollection(), collectionRecord, session);
    }

    /**
     * ADVANCED:
     * This method is used to have an object add to a collection once the changeSet is applied
     * The referenceKey parameter should only be used for direct Maps.
     */
    public void simpleAddToCollectionChangeRecord(Object referenceKey, Object objectToAdd, ObjectChangeSet changeSet, AbstractSession session) {
        simpleAddToCollectionChangeRecord(objectToAdd, null, false, changeSet, session);
    }
    protected void simpleAddToCollectionChangeRecord(Object objectToAdd, Integer index, boolean isSet, ObjectChangeSet changeSet, AbstractSession session) {
        DirectCollectionChangeRecord collectionChangeRecord = (DirectCollectionChangeRecord)changeSet.getChangesForAttributeNamed(getAttributeName());
        if (collectionChangeRecord == null) {
            collectionChangeRecord = new DirectCollectionChangeRecord(changeSet);
            collectionChangeRecord.setAttribute(getAttributeName());
            collectionChangeRecord.setMapping(this);
            changeSet.addChange(collectionChangeRecord);
            Object collection = getRealAttributeValueFromObject(changeSet.getUnitOfWorkClone(), session);
            if(this.listOrderField != null) {
                List originalListCopy = new ArrayList((List)collection);
                // collection already contains the added object - to bring it to the original state it should be removed
                if(index == null) {
                    originalListCopy.remove(originalListCopy.size() - 1);
                } else {
                   // intValue() is essential - otherwise invokes remove(Object)
                    originalListCopy.remove(index.intValue());
                }
                collectionChangeRecord.setOriginalCollection(originalListCopy);
                collectionChangeRecord.setLatestCollection(collection);
            } else {
                collectionChangeRecord.storeDatabaseCounts(collection, getContainerPolicy(), session);
                collectionChangeRecord.firstToAddAlreadyInCollection();
            }
        }
        if(!collectionChangeRecord.isDeferred() && this.listOrderField == null) {
            collectionChangeRecord.addAdditionChange(objectToAdd, new Integer(1));
        }
    }

    /**
     * ADVANCED:
     * This method is used to have an object removed from a collection once the changeSet is applied
     * The referenceKey parameter should only be used for direct Maps.
     */
    public void simpleRemoveFromCollectionChangeRecord(Object referenceKey, Object objectToRemove, ObjectChangeSet changeSet, AbstractSession session) {
        simpleRemoveFromCollectionChangeRecord(objectToRemove, null, false, changeSet, session);
    }
    protected void simpleRemoveFromCollectionChangeRecord(Object objectToRemove, Integer index, boolean isSet, ObjectChangeSet changeSet, AbstractSession session) {
        DirectCollectionChangeRecord collectionChangeRecord = (DirectCollectionChangeRecord)changeSet.getChangesForAttributeNamed(getAttributeName());
        if (collectionChangeRecord == null) {
            collectionChangeRecord = new DirectCollectionChangeRecord(changeSet);
            collectionChangeRecord.setAttribute(getAttributeName());
            collectionChangeRecord.setMapping(this);
            changeSet.addChange(collectionChangeRecord);
            Object collection = getRealAttributeValueFromObject(changeSet.getUnitOfWorkClone(), session);
            if(this.listOrderField != null) {
                List originalListCopy = new ArrayList((List)collection);
                // collection already doesn't contain the removed object - to bring it to the original state it should be added or set back.
                // index is not null because IndirectList does remove through indexOf.
                if(isSet) {
                    originalListCopy.set(index, objectToRemove);
                } else {
                    originalListCopy.add(index, objectToRemove);
                }
                collectionChangeRecord.setOriginalCollection(originalListCopy);
                collectionChangeRecord.setLatestCollection(collection);
            } else {
                collectionChangeRecord.storeDatabaseCounts(collection, getContainerPolicy(), session);
                collectionChangeRecord.firstToRemoveAlreadyOutCollection();
                if(isSet) {
                    collectionChangeRecord.firstToAddAlreadyInCollection();
                }
            }
        }
        if(!collectionChangeRecord.isDeferred() && this.listOrderField == null) {
            collectionChangeRecord.addRemoveChange(objectToRemove, new Integer(1));
        }
    }

    /**
     * INTERNAL:
     * Either create a new change record or update with the new value.  This is used
     * by attribute change tracking.
     * Specifically in a collection mapping this will be called when the customer
     * Set a new collection.  In this case we will need to mark the change record
     * with the new and the old versions of the collection.
     * And mark the ObjectChangeSet with the attribute name then when the changes are calculated
     * force a compare on the collections to determine changes.
     */
    public void updateChangeRecord(Object clone, Object newValue, Object oldValue, ObjectChangeSet objectChangeSet, UnitOfWorkImpl uow) {
        DirectCollectionChangeRecord collectionChangeRecord = (DirectCollectionChangeRecord)objectChangeSet.getChangesForAttributeNamed(this.getAttributeName());
        if (collectionChangeRecord == null) {
            collectionChangeRecord = new DirectCollectionChangeRecord(objectChangeSet);
            collectionChangeRecord.setAttribute(getAttributeName());
            collectionChangeRecord.setMapping(this);
            objectChangeSet.addChange(collectionChangeRecord);
        }
        collectionChangeRecord.setIsDeferred(true);
        objectChangeSet.deferredDetectionRequiredOn(getAttributeName());
        if (collectionChangeRecord.getOriginalCollection() == null) {
            collectionChangeRecord.recreateOriginalCollection(oldValue, getContainerPolicy(), uow);
        }
        collectionChangeRecord.setLatestCollection(newValue);
    }
    
    /**
     * INTERNAL:
     * Add or removes a new value and its change set to the collection change record based on the event passed in.  This is used by
     * attribute change tracking.
     */
    public void updateCollectionChangeRecord(CollectionChangeEvent event, ObjectChangeSet changeSet, UnitOfWorkImpl uow) {
        if (event != null ) {
            //Letting the mapping create and add the ChangeSet to the ChangeRecord rather 
            // than the policy, since the policy doesn't know how to handle DirectCollectionChangeRecord.
            // if ordering is to be supported in the future, check how the method in CollectionMapping is implemented
            Object value =  event.getNewValue();
            
            if (event.getChangeType() == CollectionChangeEvent.ADD) {
                simpleAddToCollectionChangeRecord(value, event.getIndex(), event.isSet(), changeSet, uow);
            } else if (event.getChangeType() == CollectionChangeEvent.REMOVE) {
                simpleRemoveFromCollectionChangeRecord(value, event.getIndex(), event.isSet(), changeSet, uow);
            } else {
                throw ValidationException.wrongCollectionChangeEventType(event.getChangeType());
            }
        }
    }

    /**
     * PUBLIC:
     * It is illegal to use a Map as the container of a DirectCollectionMapping. Only
     * Collection containers are supported for DirectCollectionMappings.
     * @see org.eclipse.persistence.mappings.DirectMapMapping
     */
    public void useMapClass(Class concreteClass, String methodName) {
        throw ValidationException.illegalUseOfMapInDirectCollection(this, concreteClass, methodName);
    }

    /**
     * INTERNAL:
     * Return the value of the reference attribute or a value holder.
     * Check whether the mapping's attribute should be optimized through batch and joining.
     * Overridden to support flashback/historical queries.
     */
    public Object valueFromRow(AbstractRecord row, JoinedAttributeManager joinManager, ObjectBuildingQuery sourceQuery, AbstractSession session) throws DatabaseException {
        // if the query uses batch reading, return a special value holder
        // or retrieve the object from the query property.
        if (sourceQuery.isReadAllQuery() && (((ReadAllQuery)sourceQuery).isAttributeBatchRead(getDescriptor(), getAttributeName()) || shouldUseBatchReading())) {
            return batchedValueFromRow(row, (ReadAllQuery)sourceQuery);
        }

        if (shouldUseValueFromRowWithJoin(joinManager, sourceQuery)) {
            return valueFromRowInternalWithJoin(row, joinManager, sourceQuery, session);
        }
        
        ReadQuery targetQuery = getSelectionQuery();

        boolean extendingPessimisticLockScope = isExtendingPessimisticLockScope(sourceQuery) && extendPessimisticLockScope == ExtendPessimisticLockScope.TARGET_QUERY; 
        if ((getHistoryPolicy() != null) || (sourceQuery.getSession().getAsOfClause() != null) || ((sourceQuery.isObjectLevelReadQuery() && ((ObjectLevelReadQuery)sourceQuery).hasAsOfClause()) && (sourceQuery.shouldCascadeAllParts() || (sourceQuery.shouldCascadePrivateParts() && isPrivateOwned()) || (sourceQuery.shouldCascadeByMapping() && this.cascadeRefresh))) || extendingPessimisticLockScope) {
            targetQuery = (ReadQuery)targetQuery.clone();
            // Code copied roughly from initializeSelectionStatement.
            SQLSelectStatement statement = new SQLSelectStatement();
            statement.addTable(getReferenceTable());
            statement.addField((DatabaseField)getDirectField().clone());
            if (isDirectMapMapping()) {
                statement.addField((DatabaseField)((DirectMapMapping)this).getDirectKeyField().clone());
            }
            statement.setWhereClause((Expression)getSelectionCriteria().clone());
            if(sourceQuery.isObjectLevelReadQuery()) {
                statement.getBuilder().asOf(((ObjectLevelReadQuery)sourceQuery).getAsOfClause());
            }
            if(extendingPessimisticLockScope) {
                statement.setLockingClause(new ForUpdateClause(sourceQuery.getLockMode()));
            }
            if (getHistoryPolicy() != null) {
                ExpressionBuilder builder = statement.getBuilder();
                if (sourceQuery.getSession().getAsOfClause() != null) {
                    builder.asOf(sourceQuery.getSession().getAsOfClause());
                } else if (builder.getAsOfClause() == null) {
                    builder.asOf(AsOfClause.NO_CLAUSE);
                }
                Expression temporalExpression = getHistoryPolicy().additionalHistoryExpression(builder);
                statement.setWhereClause(statement.getWhereClause().and(temporalExpression));
                if (builder.hasAsOfClause()) {
                    statement.getTables().set(0, getHistoryPolicy().getHistoricalTables().elementAt(0));
                }
            }
            statement.normalize(sourceQuery.getSession(), null);
            targetQuery.setSQLStatement(statement);
        }

        return getIndirectionPolicy().valueFromQuery(targetQuery, row, sourceQuery.getSession());
    }

    /**
     * INTERNAL:
     * Checks if object is deleted from the database or not.
     */
    public boolean verifyDelete(Object object, AbstractSession session) throws DatabaseException {
        // Row is built for translation
        if (isReadOnly()) {
            return true;
        }

        AbstractRecord row = getDescriptor().getObjectBuilder().buildRowForTranslation(object, session);
        Object value = session.executeQuery(getSelectionQuery(), row);

        return getContainerPolicy().isEmpty(value);
    }

    /**
     * INTERNAL:
     * DirectCollectionMapping contents should not be considered for addition to the UnitOfWork
     * private owned objects list for removal.
     */
    public boolean isCandidateForPrivateOwnedRemoval() {
        return false;
    }
    
    /**
     * INTERNAL
     * Return true if this mapping supports cascaded version optimistic locking.
     */
    public boolean isCascadedLockingSupported() {
        return true;
    }
}
