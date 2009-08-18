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
        Gordon Yorke - VM managed entity detachment
 *     07/16/2009-2.0 Guy Pelletier 
 *       - 277039: JPA 2.0 Cache Usage Settings
 ******************************************************************************/  
package org.eclipse.persistence.internal.sessions;

import java.util.*;

import org.eclipse.persistence.config.FlushClearCache;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.changetracking.AttributeChangeTrackingPolicy;
import org.eclipse.persistence.internal.descriptors.ObjectBuilder;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.OptimisticLockException;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.sessions.IdentityMapAccessor;
import org.eclipse.persistence.sessions.factories.ReferenceMode;


public class RepeatableWriteUnitOfWork extends UnitOfWorkImpl {
    
    /** Used to store the final UnitOfWorkChangeSet for merge into the shared cache */
    protected UnitOfWorkChangeSet cumulativeUOWChangeSet;
    
    /**
     * Used to determine if UnitOfWork should commit and rollback transactions.
     * This is used when an EntityTransaction is controlling the transaction.
     */
    protected boolean shouldTerminateTransaction;
    
    /**
     * Used to determine if we should bypass any merge into the cache. This is
     * a JPA flag and is true when the cacheStoreMode property is set to BYPASS.
     * Otherwise, EclipseLink behaves as it usually would.
     */
    protected boolean shouldStoreBypassCache;
    
    /**
     * The FlashClearCache mode to be used.
     * Initialized by setUnitOfWorkChangeSet method in case it's null;
     * commitAndResume sets this attribute back to null.
     * Relevant only in case call to flush method followed by call to clear method.
     * @see org.eclipse.persistence.config.FlushClearCache
     */
    protected transient String flushClearCache;
    
    /** 
     * Track whether we are already in a flush().
     */
    protected boolean isWithinFlush;
    
    /** Contains classes that should be invalidated in the shared cache on commit.
     * Used only in case fushClearCache == FlushClearCache.DropInvalidate:
     * clear method copies contents of updatedObjectsClasses to this set,
     * adding classes of deleted objects, too;
     * on commit the classes contained here are invalidated in the shared cache
     * and the set is cleared.
     * Relevant only in case call to flush method followed by call to clear method.
     * Works together with flushClearCache.
     */
    protected transient Set<Class> classesToBeInvalidated;
    
    /**
     * Alters the behaviour of the RWUOW commit to function like the UOW with respect to Entity lifecycle
     */
    protected boolean discoverUnregisteredNewObjectsWithoutPersist;
    
    
    public RepeatableWriteUnitOfWork(org.eclipse.persistence.internal.sessions.AbstractSession parentSession, ReferenceMode referenceMode){
        super(parentSession, referenceMode);
        this.shouldTerminateTransaction = true;
        this.shouldNewObjectsBeCached = true;
        this.isWithinFlush = false;
        this.discoverUnregisteredNewObjectsWithoutPersist = false;
    }
    
    /**
     * @return the discoverUnregisteredNewObjectsWithoutPersist
     */
    public boolean shouldDiscoverUnregisteredNewObjectsWithoutPersist() {
        return discoverUnregisteredNewObjectsWithoutPersist;
    }

    /**
     * @param discoverUnregisteredNewObjectsWithoutPersist the discoverUnregisteredNewObjectsWithoutPersist to set
     */
    public void setDiscoverUnregisteredNewObjectsWithoutPersist(boolean discoverUnregisteredNewObjectsWithoutPersist) {
        this.discoverUnregisteredNewObjectsWithoutPersist = discoverUnregisteredNewObjectsWithoutPersist;
    }

    /**
     * INTERNAL:
     * This method will clear all registered objects from this UnitOfWork.
     * If parameter value is 'true' then the cache(s) are cleared, too.
     */
    public void clear(boolean shouldClearCache) {
        super.clear(shouldClearCache);
        if (this.cumulativeUOWChangeSet != null) {            
            if (this.flushClearCache == FlushClearCache.Drop) {
                this.cumulativeUOWChangeSet = null;
                this.unregisteredDeletedObjectsCloneToBackupAndOriginal = null;
            } else if (this.flushClearCache == FlushClearCache.DropInvalidate) {
                // classes of the updated objects should be invalidated in the shared cache on commit.
                Set updatedObjectsClasses = this.cumulativeUOWChangeSet.findUpdatedObjectsClasses();
                if (updatedObjectsClasses != null) {
                    if (this.classesToBeInvalidated == null) {
                        this.classesToBeInvalidated = updatedObjectsClasses;
                    } else {
                        this.classesToBeInvalidated.addAll(updatedObjectsClasses);
                    }
                }
                if ((this.unregisteredDeletedObjectsCloneToBackupAndOriginal != null) && !this.unregisteredDeletedObjectsCloneToBackupAndOriginal.isEmpty()) {
                    if (this.classesToBeInvalidated == null) {
                        this.classesToBeInvalidated = new HashSet<Class>();
                    }
                    Iterator enumDeleted = this.unregisteredDeletedObjectsCloneToBackupAndOriginal.keySet().iterator();
                    // classes of the deleted objects should be invalidated in the shared cache
                    while (enumDeleted.hasNext()) {
                        this.classesToBeInvalidated.add(enumDeleted.next().getClass());
                    }
                }
                this.cumulativeUOWChangeSet = null;
                this.unregisteredDeletedObjectsCloneToBackupAndOriginal = null;
            }
        }
    }
    
    /**
     * INTERNAL:
     * Call this method if the uow will no longer used for committing transactions:
     * all the changes sets will be dereferenced, and (optionally) the cache cleared.
     * If the uow is not released, but rather kept around for ValueHolders, then identity maps shouldn't be cleared:
     * the parameter value should be 'false'. The lifecycle set to Birth so that uow ValueHolder still could be used.
     * Alternatively, if called from release method then everything should go and therefore parameter value should be 'true'.
     * In this case lifecycle won't change - uow.release (optionally) calls this method when it (uow) is already dead.
     * The reason for calling this method from release is to free maximum memory right away:
     * the uow might still be referenced by objects using UOWValueHolders (though they shouldn't be around
     * they still might).
     */
    public void clearForClose(boolean shouldClearCache){
        this.cumulativeUOWChangeSet = null;
        this.unregisteredDeletedObjectsCloneToBackupAndOriginal = null;
        super.clearForClose(shouldClearCache);
    }
    
    /**
     * INTERNAL:
     * Indicates whether clearForClose method should be called by release method.
     */
    public boolean shouldClearForCloseOnRelease() {
        return true;
    }
    
    /**
     * INTERNAL:
     * Returns true if the UOW should bypass any updated to the shared cache
     * during the merge.
     */
    @Override
    public boolean shouldStoreBypassCache() {
        return shouldStoreBypassCache;
    }

    /**
     * Check to see if the descriptor of a superclass can be used to describe this class
     * 
     * By default, in JPA, classes must have specific descriptors to be considered entities
     * In this implementation, we check whether the inheritance policy has been configured to allow
     * superclass descriptors to describe subclasses that do not have a descriptor themselves
     * 
     * @param Class
     * @return ClassDescriptor
     */
    protected ClassDescriptor checkHierarchyForDescriptor(Class theClass){
	    ClassDescriptor descriptor = getDescriptor(theClass.getSuperclass());
	    if (descriptor != null && descriptor.getInheritancePolicy().getDescribesNonPersistentSubclasses()){
	    	return descriptor;
	    }
	    return null;
    }

    /**
     * INTERNAL:
     * Commit the changes to any objects to the parent.
     */
    public void commitRootUnitOfWork() throws DatabaseException, OptimisticLockException {
        commitToDatabaseWithChangeSet(false);
        // unit of work has been committed so it's ok to set the cumulative into the UOW for merge
        if (this.cumulativeUOWChangeSet != null) {
            this.cumulativeUOWChangeSet.mergeUnitOfWorkChangeSet((UnitOfWorkChangeSet)this.getUnitOfWorkChangeSet(), this, true);
            setUnitOfWorkChangeSet(this.cumulativeUOWChangeSet);
        }

        commitTransactionAfterWriteChanges(); // this method will commit the transaction
                                              // and set the transaction flags appropriately

        // Merge after commit	
        mergeChangesIntoParent();
    }

    /**
     * INTERNAL:
     * Traverse the object to find references to objects not registered in this unit of work.
     * Any unregistered new objects found will be persisted or an error will be thrown depending on the mapping's cascade persist.
     * References to deleted objects will also currently cause them to be undeleted.
     */
    public void discoverUnregisteredNewObjects(Map clones, Map newObjects, Map unregisteredExistingObjects, Map visitedObjects) {
        if (this.discoverUnregisteredNewObjectsWithoutPersist){
            super.discoverUnregisteredNewObjects(clones, newObjects, unregisteredExistingObjects, visitedObjects);
        }else{
            for (Iterator clonesEnum = clones.keySet().iterator(); clonesEnum.hasNext(); ) {        
                discoverAndPersistUnregisteredNewObjects(clonesEnum.next(), false, newObjects, unregisteredExistingObjects, visitedObjects);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Has writeChanges() been attempted on this UnitOfWork?  It may have
     * either succeeded or failed but either way the UnitOfWork is in a highly
     * restricted state.
     */
    public boolean isAfterWriteChangesButBeforeCommit() {
        //don't check for writechanges failure.
        return (getLifecycle() == CommitTransactionPending);
    }

    /**
     * INTERNAL:
     * Return if the object has been deleted in this unit of work.
     */
    public boolean isObjectDeleted(Object object) {
        if(super.isObjectDeleted(object)) {
            return true;
        } else {
            if(unregisteredDeletedObjectsCloneToBackupAndOriginal != null) {
                if(unregisteredDeletedObjectsCloneToBackupAndOriginal.containsKey(object)) {
                    return true;
                }
            }
            if (hasObjectsDeletedDuringCommit()) {
                return getObjectsDeletedDuringCommit().containsKey(object);
            } else {
                return false;
            }
        }
    }

    /**
     * INTERNAL:
     * For synchronized units of work, dump SQL to database
     */
    public void issueSQLbeforeCompletion() {

        super.issueSQLbeforeCompletion(false);

        if (this.cumulativeUOWChangeSet != null){
            // unit of work has been committed so it's ok to set the cumulative into the UOW for merge
            this.cumulativeUOWChangeSet.mergeUnitOfWorkChangeSet((UnitOfWorkChangeSet)this.getUnitOfWorkChangeSet(), this, true);
            setUnitOfWorkChangeSet(this.cumulativeUOWChangeSet);
        }

        commitTransactionAfterWriteChanges(); // this method will commit the transaction
                                              // and set the transaction flags appropriately
    }
    
    /**
     * INTERNAL: Merge the changes to all objects to the parent.
     */
    protected void mergeChangesIntoParent() {
        if (this.classesToBeInvalidated != null) {
            // get identityMap of the parent ServerSession
            IdentityMapAccessor accessor = this.getParentIdentityMapSession(null, false, true).getIdentityMapAccessor();
            Iterator<Class> iterator = this.classesToBeInvalidated.iterator();
            while (iterator.hasNext()) {
               accessor.invalidateClass(iterator.next(), false);
            }
            this.classesToBeInvalidated = null;
        }
        super.mergeChangesIntoParent();
    }
    
    /**
     * INTERNAL:
     * Merge the attributes of the clone into the unit of work copy.
     */
    public Object mergeCloneWithReferences(Object rmiClone, MergeManager manager) {
        Object mergedObject = super.mergeCloneWithReferences(rmiClone, manager);
        
        //iterate over new objects, assign sequences and put in the identitymap
        Map  newObjects = manager.getMergedNewObjects();
        if (! newObjects.isEmpty()) {
            Iterator iterator = newObjects.values().iterator();
            while (iterator.hasNext()) {
                Object newObjectClone = iterator.next();
                ClassDescriptor descriptor = getDescriptor(newObjectClone);
                if (assignSequenceNumber(newObjectClone, descriptor) != null) {
                    // Avoid putting the merged object in the cache twice. If
                    // the sequence number has already been assigned then we
                    // don't need to put it in the cache.
                    registerNewObjectInIdentityMap(newObjectClone, null, descriptor);
                }
            }
        }
        
        return mergedObject;
    }

    /**
     * INTERNAL:
     * This method is used internally to update the tracked objects if required
     */
    public void updateChangeTrackersIfRequired(Object objectToWrite, ObjectChangeSet changeSetToWrite, UnitOfWorkImpl uow, ClassDescriptor descriptor) {
        descriptor.getObjectChangePolicy().updateWithChanges(objectToWrite, changeSetToWrite, uow, descriptor);
    }

    /**
     * INTERNAL:
     * This will flush all changes to the database,
     * and create or merge into the cumulativeUOWChangeSet.
     */
    public void writeChanges() {
        // Check for a nested flush and return early if we are in one
        if(this.isWithinFlush()) {
            AbstractSessionLog.getLog().log(SessionLog.WARNING,
                "nested_entity_manager_flush_not_executed_pre_query_changes_may_be_pending", this.getClass().getSimpleName());
            return;
        } 
        
        if (this.unitOfWorkChangeSet == null) {
            this.unitOfWorkChangeSet = new UnitOfWorkChangeSet(this);
        }

        // 256277: stop any nested flushing - there should only be one level 
        this.isWithinFlush = true; // set before calculateChanges as a PrePersist callback may contain a query that requires a pre flush()

        UnitOfWorkChangeSet changeSet = this.unitOfWorkChangeSet;
        // This also discovers unregistered new objects, (which persists them and assign sequence, so no need to assign sequence twice).
        calculateChanges(getCloneMapping(), changeSet, this.discoverUnregisteredNewObjectsWithoutPersist);
        
        // Write those changes to the database.
        if (!changeSet.hasChanges() && !changeSet.hasForcedChanges() && ! this.hasDeletedObjects() && ! this.hasModifyAllQueries()) {
            this.isWithinFlush = false; // clear the flag in the case that we don't have changes
            return;
        }
        try {
            commitToDatabaseWithPreBuiltChangeSet(changeSet, false);
            writesCompleted();
        } catch (RuntimeException exception) {
            clearFlushClearCache();
            setLifecycle(WriteChangesFailed);
            throw exception;
        } finally {
            isWithinFlush = false;  // clear the flag in the case that we have changes          
        }

        if (this.cumulativeUOWChangeSet == null) {
            this.cumulativeUOWChangeSet = changeSet;
        } else {
            // Merge those changes back into the backup clones and the final uowChangeSet.
            this.cumulativeUOWChangeSet.mergeUnitOfWorkChangeSet(changeSet, this, true);
        }

        resumeUnitOfWork();
    }

    /**
     * INTERNAL:
     * Called only by registerNewObjectForPersist method,
     * and only if newObject is not already registered.
     * If newObject is found in
     * unregisteredDeletedObjectsCloneToBackupAndOriginal then it's re-registered,
     * otherwise the superclass method called.
     */
    protected void registerNotRegisteredNewObjectForPersist(Object newObject, ClassDescriptor descriptor) {
        if(unregisteredDeletedObjectsCloneToBackupAndOriginal != null) {
            Object[] backupAndOriginal = (Object[])unregisteredDeletedObjectsCloneToBackupAndOriginal.remove(newObject);
            if(backupAndOriginal != null) {
                // backup
                getCloneMapping().put(newObject, backupAndOriginal[0]);
                // original
                registerNewObjectClone(newObject, backupAndOriginal[1], descriptor);

                // Check if the new objects should be cached.
                registerNewObjectInIdentityMap(newObject, newObject, descriptor);
                
                return;
            }
        }
        super.registerNotRegisteredNewObjectForPersist(newObject, descriptor);
    }

    /**
     * INTERNAL:
     * This is internal to the uow, transactions should not be used explicitly in a uow.
     * The uow shares its parents transactions.
     */
    public void rollbackTransaction() throws DatabaseException {
        if (this.shouldTerminateTransaction || getParent().getTransactionMutex().isNested()){
            super.rollbackTransaction();
        }else{
            //rollback called which means txn failed.
            //but rollback was stopped by entitytransaction which means the
            //transaction will want to call release later.  Make sure release
            //will rollback transaction.
            setWasTransactionBegunPrematurely(true);
        }
    }
    /**
     * INTERNAL:
     * This is internal to the uow, transactions should not be used explicitly in a uow.
     * The uow shares its parents transactions.  Called in JTA this should not set the 
     * transaction to rollback.
     */
    protected void rollbackTransaction(boolean intendedToCommitTransaction) throws DatabaseException {
        rollbackTransaction();
    }
    /**
     * INTERNAL
     * Synchronize the clones and update their backup copies.
     * Called after commit and commit and resume.
     */
    public void synchronizeAndResume() {
        this.cumulativeUOWChangeSet = null;
        this.unregisteredDeletedObjectsCloneToBackupAndOriginal = null;
        super.synchronizeAndResume();
    }
    
    /**
     * INTERNAL:
     * Return if the object was deleted previously (in a flush).
     */
    public boolean wasDeleted(Object original) {
        return getUnregisteredDeletedCloneForOriginal(original) != null;
    }
    
    /**
     * INTERNAL:
     * To avoid putting the original object into the shared cache, and
     * therefore, impede the 'detaching' of the original after commit, a clone 
     * of the original should be registered not the actual original object. 
     * This is a JPA override to traditional EclipseLink behavior.
     */
    @Override
    protected Object cloneAndRegisterNewObject(Object original) {
        ClassDescriptor descriptor = getDescriptor(original);
        //Nested unit of work is not supported for attribute change tracking
        if (isNestedUnitOfWork() && (descriptor.getObjectChangePolicy() instanceof AttributeChangeTrackingPolicy)) {
            throw ValidationException.nestedUOWNotSupportedForAttributeTracking();
        }
        ObjectBuilder builder = descriptor.getObjectBuilder();

        // bug 2612602 create the working copy object.
        Object clone = builder.instantiateWorkingCopyClone(original, this);
        
        // This is the only difference from my superclass. I am building a new
        // original to put in the shared cache.
        Object newOriginal = builder.buildNewInstance();
            
        // Must put in the detached original to clone to resolve circular refs.
        getNewObjectsOriginalToClone().put(original, clone);
        getNewObjectsCloneToOriginal().put(clone, original);
        
        // Must put in clone mapping.
        getCloneMapping().put(clone, clone);

        builder.populateAttributesForClone(original, clone, this);
        // Must reregister in both new objects.
        registerNewObjectClone(clone, newOriginal, descriptor);

        //Build backup clone for DeferredChangeDetectionPolicy or ObjectChangeTrackingPolicy,
        //but not for AttributeChangeTrackingPolicy
        Object backupClone = descriptor.getObjectChangePolicy().buildBackupClone(clone, builder, this);
        getCloneMapping().put(clone, backupClone);// The backup clone must be updated.

        return clone;
    }
    
    /**
     * INTERNAL:
     * Called only by UnitOfWorkIdentityMapAccessor.getAndCloneCacheKeyFromParent method.
     * Return unregisteredDeletedClone corresponding to the passed original, or null
     */
    public Object getUnregisteredDeletedCloneForOriginal(Object original) {
        if (unregisteredDeletedObjectsCloneToBackupAndOriginal != null) {
            Iterator keys = unregisteredDeletedObjectsCloneToBackupAndOriginal.keySet().iterator();
            Iterator values = unregisteredDeletedObjectsCloneToBackupAndOriginal.values().iterator();
            while(keys.hasNext()) {
                Object deletedObjectClone = keys.next();
                Object[] backupAndOriginal = (Object[])values.next();
                Object currentOriginal = backupAndOriginal[1];
                if (original == currentOriginal) {
                    return deletedObjectClone;
                }
            }
        }
        return null;
    }
    
  /**
   * INTERNAL:
   * Wraps the org.eclipse.persistence.exceptions.OptimisticLockException in a  
   * javax.persistence.OptimisticLockException. This conforms to the EJB3 specs
   * @param commitTransaction 
   */
    protected void commitToDatabase(boolean commitTransaction) {
        try {
            super.commitToDatabase(commitTransaction);
        } catch (org.eclipse.persistence.exceptions.OptimisticLockException ole) {
            throw new javax.persistence.OptimisticLockException(ole);
        }
    }

    /**
     * INTERNAL:
     * This is internal to the uow, transactions should not be used explicitly in a uow.
     * The uow shares its parents transactions.
     */
    public void commitTransaction() throws DatabaseException {
        if (this.shouldTerminateTransaction || getParent().getTransactionMutex().isNested()){
            super.commitTransaction();
        }
    }

    public void setShouldStoreByPassCache(boolean shouldStoreBypassCache) {
        this.shouldStoreBypassCache = shouldStoreBypassCache;
    }
    
    public void setShouldTerminateTransaction(boolean shouldTerminateTransaction) {
        this.shouldTerminateTransaction = shouldTerminateTransaction;
    }
    
    /**
     * INTERNAL:
     * Clears invalidation list.
     */
    public void clearFlushClearCache() {
        classesToBeInvalidated = null;
    }

    /**
     * Return the FlashClearCache mode to be used.
     * Relevant only in case call to flush method followed by call to clear method.
     * @see org.eclipse.persistence.config.FlushClearCache
     */
    public String getFlushClearCache() {
        return flushClearCache;
    }

    /**
     * Set the FlashClearCache mode to be used.
     * Relevant only in case call to flush method followed by call to clear method.
     * @see org.eclipse.persistence.config.FlushClearCache
     */
    public void setFlushClearCache(String flushClearCache) {
        this.flushClearCache = flushClearCache;
    }

    /**
     * Return whether we are already performing a flush() call
     * @return
     */
    public boolean isWithinFlush() {
        return isWithinFlush;
    }
}
