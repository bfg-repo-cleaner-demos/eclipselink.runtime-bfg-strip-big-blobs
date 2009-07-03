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
package org.eclipse.persistence.internal.queries;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.*;
import java.lang.reflect.*;

import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.internal.helper.*;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.ObjectBuildingQuery;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.security.PrivilegedMethodInvoker;
import org.eclipse.persistence.internal.security.PrivilegedGetValueFromField;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.CollectionChangeRecord;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.internal.sessions.UnitOfWorkChangeSet;
import org.eclipse.persistence.internal.sessions.UnitOfWorkImpl;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.changetracking.MapChangeEvent;
import org.eclipse.persistence.descriptors.changetracking.CollectionChangeEvent;
import org.eclipse.persistence.mappings.Association;
import org.eclipse.persistence.mappings.CollectionMapping;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.foundation.AbstractDirectMapping;
import org.eclipse.persistence.mappings.querykeys.QueryKey;

/**
 * <p><b>Purpose</b>: A MapContainerPolicy is ContainerPolicy whose container class
 * implements the Map interface.
 * <p>
 * <p><b>Responsibilities</b>:
 * Provide the functionality to operate on an instance of a Map.
 *
 * @see ContainerPolicy
 * @see CollectionContainerPolicy
 */
public class MapContainerPolicy extends InterfaceContainerPolicy {
    /** The Method which is called on each value added to the Map and whose result is used as key to the value. */
    protected String keyName;

    protected String elementClassName;
    protected Class elementClass;
    protected transient Field keyField;
    protected transient Method keyMethod;

    /**
     * INTERNAL:
     * Construct a new policy.
     */
    public MapContainerPolicy() {
        super();
    }

    /**
     * INTERNAL:
     * Construct a new policy for the specified class.
     */
    public MapContainerPolicy(Class containerClass) {
        super(containerClass);
    }

    /**
     * INTERNAL:
     * Construct a new policy for the specified class name.
     */
    public MapContainerPolicy(String containerClassName) {
        super(containerClassName);
    }

    /**
     * Prepare and validate.
     * Set the element class.
     */
    public void prepare(DatabaseQuery query, AbstractSession session) throws QueryException {
        if ((getElementClass() == null) && (query.getDescriptor() != null)) {
            setElementClass(query.getDescriptor().getJavaClass());
        }
        super.prepare(query, session);
    }
    
    /**
     * INTERNAL:
     * Add element to that implements the Map interface
     * use the row to compute the key
     */ 
    public boolean addInto(Object element, Object container, AbstractSession session, AbstractRecord dbRow, ObjectBuildingQuery query) {
        return addInto(null, element, container, session);
    }
    
    /**
     * INTERNAL:
     * Add element to container.
     * This is used to add to a collection independent of JDK 1.1 and 1.2.
     * The session may be required to wrap for the wrapper policy.
     * Return whether the container changed
     */
    public boolean addInto(Object element, Object container, AbstractSession session){
        if (element instanceof Map.Entry){
            return addInto(((Map.Entry)element).getKey(), ((Map.Entry)element).getValue(), container, session);
        } else {
            return super.addInto(element, container, session);
        }
    }
    
    /**
     * INTERNAL:
     * Add element into container which implements the Map interface.
     */
    public boolean addInto(Object key, Object element, Object container, AbstractSession session) {
        Object wrapped = element;
        if (hasElementDescriptor()) {
            wrapped = getElementDescriptor().getObjectBuilder().wrapObject(element, session);
        }
        try {
            if (key != null) {
                return ((Map)container).put(key, wrapped) != null;
            } else if (isKeyAvailableFromElement()){
                Object keyFromElement = keyFrom(element, session);
                
                try {
                    Object result = ((Map)container).put(keyFromElement, wrapped);
                    return null != result;
                } catch (NullPointerException e) { 
                    // If the container Map is a concrete type that does not 
                    // allow null keys then throw a QueryException. Note, a 
                    // HashMap permits null keys.
                    if (keyFromElement == null) {
                        // TreeMap, HashTable, SortedMap do not permit null keys 
                        throw QueryException.mapKeyIsNull(element, container);
                    } else {
                        // We got a null pointer exception for some other reason
                        // so re-throw the exception.
                        throw e;
                    }
                }
            } else {
                throw QueryException.cannotAddElementWithoutKeyToMap(element);
            }
        } catch (ClassCastException ex1) {
            throw QueryException.mapKeyNotComparable(key, container);
        }
    }

    /**
     * INTERNAL:
     * This method is used to add the next value from an iterator built using ContainerPolicy's iteratorFor() method
     * into the toCollection.  Since this ContainerPolicy represents a Map, the key and the value are extracted and added
     * 
     */
    public void addNextValueFromIteratorInto(Object valuesIterator, Object parent, Object toCollection, CollectionMapping mapping, UnitOfWorkImpl unitOfWork, boolean isExisting) {
        Map.Entry entry = ((MapContainerPolicyIterator)valuesIterator).next();
        Object clonedKey = buildCloneForKey(entry.getKey(), parent, unitOfWork, isExisting);
        Object clonedValue = buildCloneForValue(entry.getValue(), parent, mapping, unitOfWork, isExisting);
        // add the object to the uow list of private owned objects if it is a candidate and the
        // uow should discover new objects
        if (mapping.isCandidateForPrivateOwnedRemoval() && unitOfWork.shouldDiscoverNewObjects()) {
            if (clonedValue != null && unitOfWork.isObjectNew(clonedValue)) { 
                unitOfWork.addPrivateOwnedObject(mapping, clonedValue);
            }
        }
        addInto(clonedKey, clonedValue, toCollection, unitOfWork);
    }

    /**
     * INTERNAL:
     * Return an object representing an entry in the collection represented by this container policy
     * This method will returns an Association containing the key and the value for a Map
     */
    public Object buildCollectionEntry(Object objectAdded, ObjectChangeSet changeSet){
        return new Association(changeSet.getNewKey(), objectAdded);
    }
    
    /**
     * INTERNAL:
     * Ensure the new key is set for the change set for a new map object
     */
    public void buildChangeSetForNewObjectInCollection(Object object, ClassDescriptor referenceDescriptor, UnitOfWorkChangeSet uowChangeSet, AbstractSession session){
        ObjectChangeSet changeSet = referenceDescriptor.getObjectBuilder().createObjectChangeSet(((Map.Entry)object).getValue(), uowChangeSet, session);
        Object key = ((Map.Entry)object).getKey();
        changeSet.setNewKey(key);
    }
    
    /**
     * Build a clone for the value in a mapping.
     * @param value
     * @param mapping
     * @param uow
     * @param isExisting
     * @return
     */
    protected Object buildCloneForValue(Object value, Object parent, CollectionMapping mapping, UnitOfWorkImpl uow, boolean isExisting){
        return mapping.buildElementClone(value, parent, uow, isExisting);
        
    }
    
    /**
     * INTERNAL:
     * Remove all the elements from container.
     */
    public void clear(Object container) {
        try {
            ((Map)container).clear();
        } catch (UnsupportedOperationException ex) {
            throw QueryException.methodNotValid(container, "clear()");
        }
    }

    /**
     * INTERNAL:
     * Return true if keys are the same in the source as the backup.  False otherwise
     * in the case of readonly compare against the original
     */
    public boolean compareKeys(Object sourceValue, AbstractSession session) {
        if (((UnitOfWorkImpl)session).isClassReadOnly(sourceValue.getClass())) {
            return true;
        }
        Object backUpVersion = ((UnitOfWorkImpl)session).getBackupClone(sourceValue, getElementDescriptor());
        return (keyFrom(backUpVersion, session).equals(keyFrom(sourceValue, session)));
    }

    /**
     * INTERNAL:
     * Return the true if element exists in container.
     * @return boolean true if container 'contains' element
     */
    protected boolean contains(Object element, Object container) {
        return ((Map)container).containsValue(element);
    }
    
    /**
     * INTERNAL:
     * Convert all the class-name-based settings in this ContainerPolicy to 
     * actual class-based settings. This method is used when converting a 
     * project that has been built with class names to a project with classes.
     * @param classLoader 
     */
    public void convertClassNamesToClasses(ClassLoader classLoader){
        super.convertClassNamesToClasses(classLoader);
        
        if (elementClassName == null){
            return;
        }
        
        try {
            Class elementClass = null;
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                try {
                    elementClass = (Class)AccessController.doPrivileged(new PrivilegedClassForName(elementClassName, true, classLoader));
                } catch (PrivilegedActionException exception) {
                    throw ValidationException.classNotFoundWhileConvertingClassNames(containerClassName, exception.getException());
                }
            } else {
                elementClass = org.eclipse.persistence.internal.security.PrivilegedAccessHelper.getClassForName(elementClassName, true, classLoader);
            }
            setElementClass(elementClass);
        } catch (ClassNotFoundException exc){
            throw ValidationException.classNotFoundWhileConvertingClassNames(containerClassName, exc);
        }
    }

    /**
     * INTERNAL:
     * Create a query key that links to the map key.  MapContainerPolicy does not have a specific mapping for the
     * key, so return null;
     * @return
     */
    public QueryKey createQueryKeyForMapKey(){
        return null;
    }
    
    /**
     * INTERNAL:
     * Return all the fields in the key.  MapContainerPolicy gets it fields from the reference descriptor
     * of the provided mappings.  It uses its keyName to lookup the appropriate mapping and returns the fields from
     * that mapping
     * @return
     */
    public List<DatabaseField> getAllFieldsForMapKey(CollectionMapping baseMapping){
        if (baseMapping == null){
            return null;
        }
        ClassDescriptor descriptor = baseMapping.getReferenceDescriptor();
        DatabaseMapping mapping = descriptor.getMappingForAttributeName(Helper.getAttributeNameFromMethodName(keyName));
        return mapping.getFields();
    }
    
    /**
     * INTERNAL:
     * Return the DatabaseField that represents the key in a DirectMapMapping.   MapContainerPolicy gets it fields from the reference descriptor
     * of the provided mappings.  It uses its keyName to lookup the appropriate mapping and returns the field from
     * that mapping
     * @return
     */
    public DatabaseField getDirectKeyField(CollectionMapping baseMapping){
        if (baseMapping == null){
            return null;
        }
        ClassDescriptor descriptor = baseMapping.getReferenceDescriptor();
        DatabaseMapping mapping = descriptor.getMappingForAttributeName(Helper.getAttributeNameFromMethodName(keyName));
        if (mapping.isAbstractDirectMapping()){
            return ((AbstractDirectMapping)mapping).getField();
        }
        return null;
    }
    
    /**
     * INTERNAL:
     * Returns the element class which defines the map key.
     */
    public Class getElementClass() {
        return elementClass;
    }
    
    /**
     * INTERNAL:
     * Returns the element class name which defines the map key.
     */
    public String getElementClassName() {
        return elementClassName;
    }

    /**
     * INTERNAL:
     */
    public Class getInterfaceType() {
        return ClassConstants.Map_Class;
    }

    /**
     * INTERNAL:
     * Returns the key name which will return the value of the key to be used 
     * in the container.
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * INTERNAL:
     * Return the type of the map key, this will be overridden by container policies that allow maps
     * @return
     */
    public Object getKeyType(){
        initializeKey();
        if (keyField != null){
            return keyField.getType();
        } else if (keyMethod != null){
            return keyMethod.getReturnType();
        }
        return null;
    }
    
    /**
     * INTERNAL
     * Yes this is a MapPolicy
     */
    public boolean isMapPolicy() {
        return true;
    }
    
    
    /**
     * MapContainerPolicy is for mappings where the key is stored in actual
     * element.
     * @return
     */
    protected boolean isKeyAvailableFromElement(){
        return true;
    }
    
    /**
     * INTERNAL:
     * Return whether a map key this container policy represents is an attribute
     * @return
     */
    public boolean isMapKeyAttribute(){
        if (elementDescriptor != null){
            DatabaseMapping mapping = elementDescriptor.getMappingForAttributeName(Helper.getAttributeNameFromMethodName(keyName));
            if (mapping != null){
                return mapping.isDirectToFieldMapping();
            }
            
        }
        initializeKey();
        if (keyField != null){
            if (keyField.getClass().isPrimitive()){
                return true;
            }
        } else if (keyMethod != null){
            if (keyMethod.getClass().isPrimitive()){
                return true;
            }
        }
        return false;
    }
    
    /**
     * INTERNAL:
     * Return whether the iterator has more objects.
     * The iterator is the one returned from #iteratorFor().
     *
     * @see ContainerPolicy#iteratorFor(java.lang.Object)
     */
    public boolean hasNext(Object iterator){
        return ((MapContainerPolicyIterator)iterator).hasNext();
    }
    /**
     * INTERNAL:
     * Set the keyMethod or keyField based on the keyName
     */
    protected void initializeKey(){
        // Should only run through this once ...
        if (keyName != null && keyMethod == null && keyField == null) {
            try {
                keyMethod = Helper.getDeclaredMethod(elementClass, keyName, (Class[]) null);
            } catch (NoSuchMethodException ex) {
                try {
                    keyField = Helper.getField(elementClass, keyName);
                } catch (NoSuchFieldException e) {
                    throw ValidationException.mapKeyNotDeclaredInItemClass(keyName, elementClass);    
                }
            }
        }
    }
    
    /**
     * INTERNAL:
     * Return an Iterator for the given container.
     */
    public Object iteratorFor(Object container) {
        return new MapContainerPolicyIterator((Map)container);
    }
    
    /**
     * INTERNAL:
     * Return the key for the specified element.
     */
    public Object keyFrom(Object element, AbstractSession session) {
        initializeKey();
        Object keyElement = element;
        if (hasElementDescriptor()) {
            keyElement = getElementDescriptor().getObjectBuilder().unwrapObject(element, session);
        }
        if (keyMethod != null) {
            try {              
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                    try {
                        return AccessController.doPrivileged(new PrivilegedMethodInvoker(keyMethod, keyElement, (Object[])null));
                    } catch (PrivilegedActionException exception) {
                        Exception throwableException = exception.getException();
                        if (throwableException instanceof IllegalAccessException) {
                            throw QueryException.cannotAccessMethodOnObject(keyMethod, keyElement);
                        } else {
                            throw QueryException.calledMethodThrewException(keyMethod, keyElement, throwableException);
                        }
                    }
                } else {
                    return PrivilegedAccessHelper.invokeMethod(keyMethod, keyElement, (Object[])null);
                }
            } catch (IllegalAccessException e) {
                throw QueryException.cannotAccessMethodOnObject(keyMethod, keyElement);
            } catch (InvocationTargetException exception) {
                throw QueryException.calledMethodThrewException(keyMethod, keyElement, exception);
            }
        } else if (keyField != null) {
            try {
                if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                    try {
                        return AccessController.doPrivileged(new PrivilegedGetValueFromField(keyField, keyElement));
                    } catch (PrivilegedActionException exception) {
                        throw QueryException.cannotAccessFieldOnObject(keyField, keyElement);
                    }
                } else {
                    return org.eclipse.persistence.internal.security.PrivilegedAccessHelper.getValueFromField(keyField, keyElement);
                }
            } catch (IllegalAccessException e) {
                throw QueryException.cannotAccessFieldOnObject(keyField, keyElement);
            }
        } else {
            // If we get this far I think it is safe to assume we have
            // an element descriptor.
            return getElementDescriptor().getCMPPolicy().createPrimaryKeyInstance(keyElement, session);
        }
    }
    

    /**
     * Get the key from the passed in Map.Entry
     * @param entry
     * @return
     */
    public Object keyFromEntry(Object entry){
        if (entry instanceof Map.Entry){
            return ((Map.Entry)entry).getKey();
        }
        return null;
    }
    
    public Object keyFromIterator(Object iterator){
        return ((MapContainerPolicyIterator)iterator).getCurrentKey();
    }
    
    /**
     * INTERNAL:
     * Return the next object on the queue. The iterator is the one
     * returned from #iteratorFor().
     *
     * @see ContainerPolicy#iteratorFor(java.lang.Object)
     */
    protected Object next(Object iterator){
        return ((MapContainerPolicyIterator)iterator).next().getValue();
    }

    /**
     * INTERNAL:
     * Return the next object on the queue. The iterator is the one
     * returned from #iteratorFor().
     * 
     * This will return a MapEntry to allow use of the key
     *
     * @see ContainerPolicy#iteratorFor(java.lang.Object)
     * @see MapContainerPolicy.unwrapIteratorResult(Object object)
     */
    public Object nextEntry(Object iterator){
        return ((MapContainerPolicyIterator)iterator).next();
    }
    
    /**
     * INTERNAL:
     * Return the next object on the queue. The iterator is the one
     * returned from #iteratorFor().
     * 
     * This will return a MapEntry to allow use of the key
     *
     * @see ContainerPolicy#iteratorFor(Object iterator, AbstractSession session)
     * @see MapContainerPolicy.unwrapIteratorResult(Object object)
     */
    public Object nextEntry(Object iterator, AbstractSession session) {
        Map.Entry next = (Map.Entry)nextEntry(iterator);
        Object object = next.getValue();
        if (hasElementDescriptor()) {
            object = getElementDescriptor().getObjectBuilder().unwrapObject(object, session);
        }
        Object key = next.getKey();
        key = unwrapKey(key, session);
        next = new Association(next.getKey(), object);
        return next;
    }
    
    /**
     * INTERNAL: 
     * MapContainerPolicy's iterator iterates on the Entries of a Map.
     * This method returns the object from the iterator
     * 
     * @see MapContainerPolicy.nextWrapped(Object iterator)
     */
    public Object unwrapElement(Object object){
        if (object instanceof Association){
            return ((Association)object).getValue();
        } else {
            return object;
        }
    }

    /**
     * INTERNAL: 
     * MapContainerPolicy's iterator iterates on the Entries of a Map.
     * This method returns the object from the iterator
     * 
     * @see MapContainerPolicy.nextWrapped(Object iterator)
     */
    public Object unwrapIteratorResult(Object object){
        if (object instanceof Map.Entry){
            return ((Map.Entry)object).getValue();
        } else {
            return object;
        }
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
        return key;
    }
    
    /**
     * This method is used to bridge the behavior between Attribute Change Tracking and
     * deferred change tracking with respect to adding the same instance multiple times.
     * Each ContainerPolicy type will implement specific behavior for the collection 
     * type it is wrapping.  These methods are only valid for collections containing object references
     */
    public void recordUpdateToCollectionInChangeRecord(CollectionChangeEvent event, ObjectChangeSet changeSet, CollectionChangeRecord collectionChangeRecord){
        
        Object key = null;
        //This is to allow non-MapChangeEvent.  Not sure how one could get here, but wasn't willing to remove the chance that it could
        if (event.getClass().equals(ClassConstants.MapChangeEvent_Class)){
            key = ((MapChangeEvent)event).getKey();
        }
        if (event.getChangeType() == CollectionChangeEvent.ADD) {
            recordAddToCollectionInChangeRecord(changeSet, collectionChangeRecord);
            changeSet.setNewKey(key);
        } else if (event.getChangeType() == MapChangeEvent.REMOVE) {
            recordRemoveFromCollectionInChangeRecord(changeSet, collectionChangeRecord);
            changeSet.setOldKey(key);
        } else {
            throw ValidationException.wrongCollectionChangeEventType(event.getChangeType());
        }
    }

    /**
     * INTERNAL:
     * Remove element from container which implements the Map interface.
     */
    public boolean removeFrom(Object key, Object element, Object container, AbstractSession session) {
        try {
            Object returnValue = null;
            if (key != null) {
                returnValue = ((Map)container).remove(key);
            } else {
                returnValue = ((Map)container).remove(keyFrom(element, session));
            }
            if (returnValue == null) {
                return false;
            } else {
                return true;
            }
        } catch (UnsupportedOperationException ex) {
            throw QueryException.methodNotValid(container, "remove(Object element)");
        }
    }

    /**
     * INTERNAL:
     * Remove element from container which implements the Map interface.
     */
    public boolean removeFromWithIdentity(Object element, Object container, AbstractSession session) {
        boolean found = false;
        Vector knownKeys = new Vector(1);
        try {
            Iterator iterator = ((Map)container).keySet().iterator();
            while (iterator.hasNext()) {
                Object key = iterator.next();
                if (((Map)container).get(key) == element) {
                    knownKeys.addElement(key);
                    found = true;
                }
            }
            if (found) {
                for (int index = 0; index < knownKeys.size(); ++index) {
                    ((Map)container).remove(knownKeys.elementAt(index));
                }
            }
            return found;
        } catch (UnsupportedOperationException ex) {
            throw QueryException.methodNotValid(container, "remove(Object element)");
        }
    }

    /**
     * INTERNAL:
     * Sets the element class which defines the method.
     */
    public void setElementClass(Class elementClass) {
        if (elementClass != null) {
            elementClassName = elementClass.getName();
        }
        
        this.elementClass = elementClass;
    }

    /**
     * INTERNAL:
     * Validate the container type.
     */
    public boolean isValidContainer(Object container) {
        // PERF: Use instanceof which is inlined, not isAssignable which is very inefficent.
        return container instanceof Map;
    }

    /**
     * INTERNAL:
     * Sets the key name to be used to generate the key in a Map type container 
     * class. The key name, may be the name of a field or method.
     */
    public void setKeyName(String keyName, String elementClassName) {
        // The key name and class name must be held as the policy is used 
        // directly from the mapping.
        this.keyName = keyName;
        this.elementClassName = elementClassName;
    }

    /**
     * INTERNAL:
     * Sets the key name to be used to generate the key in a Map type container 
     * class. The key name, may be the name of a field or method.
     * An instance of the class is provided in the case when the descriptor is being 
     * built in code.
     */
    public void setKeyName(String keyName, Class elementClass) {
        // The key name and class name must be held as the policy is used 
        // directly from the mapping.
        this.keyName = keyName;
        this.elementClass = elementClass;
    }

    /**
     * INTERNAL:
     * Sets the key name to be used to generate the key in a Map type container 
     * class. The key name, maybe the name of a field or method.
     */
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }
    
    /**
     * INTERNAL:
     * Sets the Method to be used to generate the key in a Map type container class.
     */
    public void setKeyMethod(String keyMethodName, Class elementClass) {
        this.setKeyName(keyMethodName, elementClass);
    }

    /**
     * INTERNAL:
     * Sets the Method to be used to generate the key in a Map type container class.
     */
    public void setKeyMethod(String keyMethodName, String elementClassName) {
        this.setKeyName(keyMethodName, elementClassName);
    }

    /**
     * INTERNAL:
     * Sets the Method to be used to generate the key in a Map type container class.
     */
    public void setKeyMethodName(String keyMethodName) {
        this.setKeyName(keyMethodName);
    }

    /**
     * INTERNAL:
     * Return the size of container.
     */
    public int sizeFor(Object container) {
        return ((Map)container).size();
    }

    /**
     * INTERNAL:
     * If the key has changed, remove the element and add it back into the target.
     */
    public void validateElementAndRehashIfRequired(Object sourceValue, Object targetMap, AbstractSession session, Object targetVersionOfSource) {
        if (session.isUnitOfWork()) {
            //this must be a unit of work at this point
            Object backupValue = ((UnitOfWorkImpl)session).getBackupClone(sourceValue, getElementDescriptor());
            if (!keyFrom(backupValue, session).equals(keyFrom(sourceValue, session))) {
                //the key has been changed.  Remove the old value and put back the new one
                removeFrom(backupValue, targetMap, session);
                addInto(targetVersionOfSource, targetMap, session);
            }
        }
    }
    
    
    public Object valueFromIterator(Object iterator){
        return ((MapContainerPolicyIterator)iterator).getCurrentValue();
    }
    
    /**
     * INTERNAL:
     * This inner class is used to iterate through the Map.Entry s of a Map.
     * It maintains a pointer to the current entry to allow access to the key and the value.
     * @author tware
     */
    public static class MapContainerPolicyIterator implements Iterator {
        
        private Iterator iterator;
        private Map.Entry currentEntry;
        
        public MapContainerPolicyIterator(Map container) {
            this.iterator = container.entrySet().iterator();
        }
        
        public boolean hasNext() {
            return iterator.hasNext();
        }
        
        public Map.Entry next() {
            currentEntry = (Map.Entry)iterator.next();
            return currentEntry;
        }
        
        public Object getCurrentKey() {
            if (currentEntry != null) {
                return currentEntry.getKey();
            }
            return null;
        }

        public Object getCurrentValue() {
            if (currentEntry != null) {
                return currentEntry.getValue();
            }
            return null;
        }

        public void remove() {
            this.iterator.remove();
        }
    }
    
}
