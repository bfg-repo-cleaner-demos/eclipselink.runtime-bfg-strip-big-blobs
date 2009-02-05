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
 *     tware - initial implementation
 *     tware - implemenation of basic CRUD functionality
 ******************************************************************************/
package org.eclipse.persistence.internal.queries;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.CollectionChangeRecord;
import org.eclipse.persistence.internal.sessions.MergeManager;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.internal.sessions.UnitOfWorkChangeSet;
import org.eclipse.persistence.internal.sessions.UnitOfWorkImpl;
import org.eclipse.persistence.mappings.Association;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.DirectMapMapping;
import org.eclipse.persistence.mappings.DirectToFieldMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.mappings.foundation.MapKeyMapping;
import org.eclipse.persistence.mappings.foundation.MapComponentMapping;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.ObjectBuildingQuery;
import org.eclipse.persistence.queries.ReadQuery;

/**
 * A MappedKeyMapContainerPolicy should be used for mappings to implementers of Map.
 * It differs from MapContainerPolicy by allowing the MapKey to be an otherwise unmapped
 * column in a table rather than a mapped element of the value in the map.
 * 
 * This container policy holds a reference to a KeyMapping that will be used to construct the key
 * from the database and a reference to its owner which creates the value for the map.
 * 
 * The key of the map can be any implementer of MapKeyMapping and the data representing the
 * key can either be stored in the target table of the value mapping, or in a collection table that
 * associates the source to the target.   The data can either be everything necessary to compose the
 * key, or foreign keys that allow the key to be retrieved
 * 
 * @see MapContainerPolicy
 * @see MapKeyMapping
 * @see MapComponentMapping
 * 
 * @author tware
 *
 */
public class MappedKeyMapContainerPolicy extends MapContainerPolicy implements DirectMapUsableContainerPolicy {
    
    protected transient MapKeyMapping keyMapping;

    protected transient MapComponentMapping valueMapping;
    
    public DatabaseQuery keyQuery;
    
    /**
     * INTERNAL:
     * Construct a new policy.
     */
    public MappedKeyMapContainerPolicy() {
        super();
    }

    /**
     * INTERNAL:
     * Construct a new policy for the specified class.
     */
    public MappedKeyMapContainerPolicy(Class containerClass) {
        super(containerClass);
    }

    /**
     * INTERNAL:
     * Construct a new policy for the specified class name.
     */
    public MappedKeyMapContainerPolicy(String containerClassName) {
        super(containerClassName);
    }
    
    /**
     * INTERNAL:
     * Called when the selection query is being initialize to add the fields for the key to the query
     */
    public void addAdditionalFieldsToQuery(ReadQuery selectionQuery, Expression baseExpression){
        keyMapping.addAdditionalFieldsToQuery(selectionQuery, baseExpression);
    }
    
    /**
     * INTERNAL:
     * Add any non-Foreign-key data from an Object describe by a MapKeyMapping to a database row
     * This is typically used in write queries to ensure all the data stored in the collection table is included
     * in the query.
     * @param object
     * @param databaseRow
     * @param session
     */
    public Map getKeyMappingDataForWriteQuery(Object object, AbstractSession session){
        Object keyValue = ((Map.Entry)object).getKey();
        return keyMapping.extractIdentityFieldsForQuery(keyValue, session);
    }

    /**
     * INTERNAL:
     * Called when the insert query is being initialized to ensure the fields for the key are in the insert query
     * 
     * @see MappedKeyMapContainerPolicy
     */
    public void addFieldsForMapKey(AbstractRecord joinRow){
        keyMapping.addFieldsForMapKey(joinRow);
    }
    
    /**
     * INTERNAL:
     * Add element to container.
     * This is used to add to a collection independent of JDK 1.1 and 1.2.
     * The session may be required to wrap for the wrapper policy.
     * Return whether the container changed
     */
    public boolean addInto(Object element, Object container, AbstractSession session){
        Object key = null;
        Object value = null;
        if (element instanceof AbstractRecord) {
            AbstractRecord record = (AbstractRecord)element;
            key = keyMapping.createMapComponentFromRow(record, null, session);
            
            value = valueMapping.createMapComponentFromRow(record, null, session);
            return addInto(key, value, container, session);
        } else if (element instanceof Association){
            Association record = (Association)element;
            key = record.getKey();
            value = record.getValue();
            return addInto(key, value, container, session);
        }
        return super.addInto(element, container, session);
    }

    /**
     * INTERNAL:
     * Add element to that implements the Map interface
     * use the row to compute the key
     */ 
    public boolean addInto(Object element, Object container, AbstractSession session, AbstractRecord dbRow, ObjectBuildingQuery query) {
        Object key = null;
        Object value = null;
        
        // we are a direct collection mapping.  This means the key will be element and the value will come
        // from dbRow
        if ((valueMapping != null) && (((DatabaseMapping)valueMapping).isDirectCollectionMapping()) && (session.getDescriptor(element.getClass()) != null)){
            key = element;
            value = valueMapping.createMapComponentFromRow(dbRow, null, session);
        } else if (keyMapping != null){
            value = element;
            try{
                key = keyMapping.createMapComponentFromRow(dbRow, query, session);
            } catch (Exception e){
                throw QueryException.exceptionWhileReadingMapKey(element, e);
            }
        }
        return addInto(key, value, container, session);
    }

    
    
    /**
     * Build a clone for the key of a Map represented by this container policy 
     * @param key
     * @param uow
     * @param isExisting
     * @return
     */
    protected Object buildCloneForKey(Object key, UnitOfWorkImpl uow, boolean isExisting){
        return keyMapping.buildElementClone(key, uow, isExisting);

    }
    
    /**
     * INTERNAL:
     * Certain key mappings favor different types of selection query.  Return the appropriate
     * type of selectionQuery
     * @return
     */
    public ReadQuery buildSelectionQueryForDirectCollectionMapping(){
        ReadQuery query = keyMapping.buildSelectionQueryForDirectCollectionKeyMapping(this);
        return query;
    }
        
        /**
         * INTERNAL:
         * Return a container populated with the contents of the specified Vector.
         */
        public Object buildContainerFromVector(Vector vector, DatabaseQuery query, AbstractSession session) {
            Object container = containerInstance(vector.size());
            int size = vector.size();
            for (int index = 0; index < size; index++) {
                Object element = vector.get(index);
                if (element instanceof AbstractRecord  && query.isObjectBuildingQuery()){
                    Object key = null;
                    key = keyMapping.createMapComponentFromRow((AbstractRecord)element, (ObjectBuildingQuery)query, session);
                    Object value = ((AbstractRecord)element).getValues().elementAt(1);
                    addInto(key, value, container, session);
                } else {
                    addInto(vector.get(index), container, session);
                }
            }
            return container;
        }

        /**
         * Extract the key for the map from the provided row
         * @param row
         * @param query
         * @param session
         * @return
         */
        public Object buildKey(AbstractRecord row, ObjectBuildingQuery query, AbstractSession session){
            return keyMapping.createMapComponentFromRow(row, query, session);
        }
        
        /**
         * INTERNAL:
         * This method is used to calculate the differences between two collections.
         */
        public void compareCollectionsForChange(Object oldCollection, Object newCollection, CollectionChangeRecord changeRecord, AbstractSession session, ClassDescriptor referenceDescriptor) {
            // 2612538 - the default size of Map (32) is appropriate
            IdentityHashMap originalKeyValues = new IdentityHashMap();
            IdentityHashMap cloneKeyValues = new IdentityHashMap();

            // Collect the values from the oldCollection.
            if (oldCollection != null) {
                Object backUpIter = iteratorFor(oldCollection);
                
                while (hasNext(backUpIter)) {
                    Map.Entry entry = (Map.Entry)nextEntry(backUpIter, session);
                    // CR2378 null check to prevent a null pointer exception - XC
                    if (entry != null) {
                        originalKeyValues.put(entry.getValue(), entry);
                    }
                }
            }
            
            if (newCollection != null){
                // Collect the objects from the new Collection.
                Object cloneIter = iteratorFor(newCollection);
                
                while (hasNext(cloneIter)) {
                    Map.Entry wrappedFirstObject = (Map.Entry)nextEntry(cloneIter, session);
                    Object firstObject = wrappedFirstObject.getValue();
                    // CR2378 null check to prevent a null pointer exception - XC
                    // If value is null then nothing can be done with it.
                    if (firstObject != null) {
                        if (originalKeyValues.containsKey(firstObject)) {
                            originalKeyValues.remove(firstObject);
                        } else {
                            // Place it in the add collection
                            buildChangeSetForNewObjectInCollection(wrappedFirstObject, referenceDescriptor, (UnitOfWorkChangeSet) changeRecord.getOwner().getUOWChangeSet(), session);
                            cloneKeyValues.put(firstObject, firstObject);
                        }
                    }
                }
            }
            Iterator originalKeyValuesIterator = originalKeyValues.keySet().iterator();
            while (originalKeyValuesIterator.hasNext()){
                Object object = originalKeyValuesIterator.next();
                ObjectChangeSet changeSet = referenceDescriptor.getObjectBuilder().createObjectChangeSet(object, (UnitOfWorkChangeSet) changeRecord.getOwner().getUOWChangeSet(), session);
                Map.Entry entry = (Map.Entry)originalKeyValues.get(object);
                changeSet.setOldKey(entry.getKey());
            }
            changeRecord.addAdditionChange(cloneKeyValues, (UnitOfWorkChangeSet) changeRecord.getOwner().getUOWChangeSet(), session);
            changeRecord.addRemoveChange(originalKeyValues, (UnitOfWorkChangeSet) changeRecord.getOwner().getUOWChangeSet(), session);
        }
        
        
        /**
         * INTERNAL:
         * Return true if keys are the same.  False otherwise
         */
        public boolean compareContainers(Object firstObjectMap, Object secondObjectMap) {
            if (sizeFor(firstObjectMap) != sizeFor(secondObjectMap)) {
                return false;
            }

            for (Object firstIterator = iteratorFor(firstObjectMap); hasNext(firstIterator);) {
                Map.Entry entry = (Map.Entry)nextEntry(firstIterator);
                Object key = entry.getKey();
                if (!((Map)firstObjectMap).get(key).equals(((Map)secondObjectMap).get(key))) {
                    return false;
                }
            }
            return true;
        }
        
    /**
     * INTERNAL:
     * Return true if keys are the same in the source as the backup.  False otherwise
     * in the case of readonly compare against the original
     */
    public boolean compareKeys(Object sourceValue, AbstractSession session) {
        // Key is not stored in the object, only in the Map and the DB
        // As a result, a change in the object will not change how this object is hashed
        if (keyMapping != null){
            return true;
        }
        return super.compareKeys(sourceValue, session);
    }
    
    /**
     * INTERNAL:
     * This method will actually potentially wrap an object in two ways.  It will first wrap the object
     * based on the referenceDescriptor's wrapper policy.  It will also potentially do some wrapping based
     * on what is required by the container policy.
     * 
     * @see MappedKeyMapContainerPolicy
     * @param wrappedObject
     * @param referenceDescriptor
     * @param mergeManager
     * @return
     */
    public Object createWrappedObjectFromExistingWrappedObject(Object wrappedObject, ClassDescriptor referenceDescriptor, MergeManager mergeManager){
        Object key = ((Map.Entry)wrappedObject).getKey();
        key = keyMapping.getTargetVersionOfSourceObject(key, mergeManager);
        key = keyMapping.wrapKey(key, mergeManager.getSession());
        Object value = referenceDescriptor.getObjectBuilder().wrapObject(mergeManager.getTargetVersionOfSourceObject(unwrapIteratorResult(wrappedObject)), mergeManager.getSession());
        return new Association(key, value);
    }
    
    /**
     * INTERNAL:
     * Convert all the class-name-based settings in this ContainerPolicy to actual class-based
     * settings
     * This method is implemented by subclasses as necessary.
     * @param classLoader 
     */
    public void convertClassNamesToClasses(ClassLoader classLoader){
        ((DatabaseMapping)keyMapping).convertClassNamesToClasses(classLoader);
    }
    
    /**
     * INTERNAL:
     * Used when objects are added or removed during an update.
     * This method returns either the clone from the ChangeSet or a packaged
     * version of it that contains things like map keys
     * @return
     */
    public Object getCloneDataFromChangeSet(ObjectChangeSet changeSet){
        Object key = changeSet.getNewKey();
        if (key == null){
            key = changeSet.getOldKey();
        }
        return new Association(key ,changeSet.getUnitOfWorkClone());
    }
    
    
    /**
     * INTERNAL:
     * Return the DatabaseField that represents the key in a DirectMapMapping.  If the
     * keyMapping is not a DirectMapping, this will return null
     * @return
     */
    public DatabaseField getDirectKeyField(){
        if (((DatabaseMapping)keyMapping).isDirectToFieldMapping()){
            return ((DirectToFieldMapping)keyMapping).getField();
        }
        return null;
    }
    
    /**
     * INTERNAL:
     * Return the fields that make up the identity of the mapped object.  For mappings with
     * a primary key, it will be the set of fields in the primary key.  For mappings without
     * a primary key it will likely be all the fields
     * @return
     */
    public List<DatabaseField> getIdentityFieldsForMapKey(){
        return keyMapping.getIdentityFieldsForMapKey();
    }
    
    /**
     * INTERNAL:
     * Get the Converter for the key of this mapping if one exists
     * @return
     */
    public Converter getKeyConverter(){
        if (((DatabaseMapping)keyMapping).isDirectToFieldMapping()){
            return ((DirectToFieldMapping)keyMapping).getConverter();
        }
        return null;
    }
    
    public MapKeyMapping getKeyMapping(){
        return keyMapping;
    }

    /**
     * INTERNAL:
     * Some map keys must be obtained from the database.  This query is used to obtain the key
     * @param keyQuery
     */
    public DatabaseQuery getKeyQuery(){
        return keyQuery;
    }
    

    public MapComponentMapping getValueMapping(){
        return valueMapping;
    }

    
    /**
     * INTERNAL:
     * Initialize the key mapping
     */
    public void initialize(AbstractSession session, DatabaseTable keyTable){
        getKeyMapping().preinitializeMapKey(keyTable);
        ((DatabaseMapping)keyMapping).initialize(session);
    }
    
    /**
     * CollectionTableMapContainerPolicy is for mappings where the key is stored in a table separately from the map
     * element.
     * @return
     */
    protected boolean isKeyAvailableFromElement(){
        return false;
    }
    
    public boolean isMappedKeyMapPolicy(){
        return true;
    }
    
    
    /**
     * INTERNAL:
     * Returns whether this ContainerPolicy requires data modification events when
     * objects are added or deleted during update
     * @return
     */
    public boolean requiresDataModificationEvents(){
        return true;
    }
    
    /**
     * INTERNAL:
     * Return the key for the specified element.
     */
    public Object keyFrom(Object element, AbstractSession session) {
        // key is mapped to the database table and not the object and therefore cannot be extracted from the object
        if (keyMapping != null){
            return null;
        }
        return super.keyFrom(element, session);
    }

    /**
     * INTERNAL:
     * Set the DatabaseField that will represent the key in a DirectMapMapping
     * @param keyField
     * @param descriptor
     */
    public void setKeyField(DatabaseField keyField, ClassDescriptor descriptor){
        if (keyMapping == null){
            DirectToFieldMapping newKeyMapping = new DirectToFieldMapping();
            newKeyMapping.setField(keyField);
            newKeyMapping.setDescriptor(descriptor);
            setKeyMapping(newKeyMapping);
        }
        if (((DatabaseMapping)keyMapping).isDirectToFieldMapping()){
            ((DirectToFieldMapping)keyMapping).setField(keyField);;
        }
    }
    
    /**
     * INTERNAL:
     * Used during initialization of DirectMapMapping.  Sets the descriptor associated with
     * the key
     * @param descriptor
     */
    public void setDescriptorForKeyMapping(ClassDescriptor descriptor){
        ((DatabaseMapping)keyMapping).setDescriptor(descriptor);
    }
    
    /**
     * INTERNAL:
     * Set a converter on the KeyField of a DirectCollectionMapping
     * @param keyConverter
     * @param mapping
     */
    public void setKeyConverter(Converter keyConverter, DirectMapMapping mapping){
        if (((DatabaseMapping)keyMapping).isDirectToFieldMapping()){
            ((DirectToFieldMapping)keyMapping).setConverter(keyConverter);
        } else {
            throw DescriptorException.cannotSetConverterForNonDirectMapping(mapping.getDescriptor(), mapping, keyConverter.getClass().getName());
        }
    }
    
    /**
     * INTERNAL:
     * Set the name of the class to be used as a converter for the key of a DirectMapMaping
     * @param keyConverterClassName
     * @param mapping
     */
    public void setKeyConverterClassName(String keyConverterClassName, DirectMapMapping mapping){
        if (((DatabaseMapping)keyMapping).isDirectToFieldMapping()){
            ((DirectToFieldMapping)keyMapping).setConverterClassName(keyConverterClassName);
        } else {
            throw DescriptorException.cannotSetConverterForNonDirectMapping(mapping.getDescriptor(), mapping, keyConverterClassName);
        }

    }
    
    public void setKeyMapping(MapKeyMapping mapping){
        this.keyMapping = mapping;
    }
    
    /**
     * INTERNAL:
     * Some map keys must be obtained from the database.  This query is used to obtain the key
     * @param keyQuery
     */
    public void setKeyQuery(DatabaseQuery keyQuery){
        this.keyQuery = keyQuery;
    }
    
    /**
     * INTERNAL:
     * Used during initialization of DirectMapMapping to make the ContainerPolicy aware of the 
     * DatabaseField used for the value portion of the mapping
     * 
     * This method is a no-op for MappedKeyMapContainerPolicy since it does not require that information
     * 
     * @param directField
     * @param valueConverter
     */
    public void setValueField(DatabaseField field, Converter converter) {
    }
    
    public void setValueMapping(MapComponentMapping mapping){
        this.valueMapping = mapping;
    }
    
    /**
     * INTERNAL:
     * Certain types of container policies require an extra update statement after a relationship
     * is inserted.  Return whether this update statement is required
     * @return
     */
    public boolean shouldUpdateForeignKeysPostInsert(){
        return true;
    }
    
    /**
     * INTERNAL:
     * Allow the key to be unwrapped.  This will be overridden by container policies that
     * allow keys that are entities
     * 
     * @see MappedKeyMapContainerPolicy
     * @param key
     * @param session
     * @return
     */
    public Object unwrapKey(Object key, AbstractSession session){
        return keyMapping.unwrapKey(key, session);
    }
}
