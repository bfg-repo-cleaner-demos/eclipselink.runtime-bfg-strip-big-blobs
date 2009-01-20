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
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.helper;

import java.util.*;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.FetchGroupManager;
import org.eclipse.persistence.descriptors.invalidation.CacheInvalidationPolicy;
import org.eclipse.persistence.exceptions.ConcurrencyException;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.internal.sessions.*;
import org.eclipse.persistence.internal.identitymaps.*;
import org.eclipse.persistence.internal.helper.linkedlist.*;
import org.eclipse.persistence.logging.SessionLog;

/**
 * INTERNAL:
 * <p>
 * <b>Purpose</b>: Acquires all required locks for a particular merge process.
 * Implements a deadlock avoidance algorithm to prevent concurrent merge conflicts
 *
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 * <li> Acquires locks for writing threads.
 * <li> Provides deadlock avoidance behavior.
 * <li> Releases locks for writing threads.
 * </ul>
 *  @author Gordon Yorke
 *  @since 10.0.3
 */
public class WriteLockManager {

    /* This attribute stores the list of threads that have had a problem acquiring locks */
    /*  the first element in this list will be the prevailing thread */
    protected ExposedNodeLinkedList prevailingQueue;

    public WriteLockManager() {
        this.prevailingQueue = new ExposedNodeLinkedList();
    }

    // this will allow us to prevent a readlock thread from looping forever.
    public static int MAXTRIES = 10000;

    /**
     * INTERNAL:
     * This method will return once the object is locked and all non-indirect
     * related objects are also locked.
     */
    public Map acquireLocksForClone(Object objectForClone, ClassDescriptor descriptor, CacheKey cacheKey, AbstractSession session, UnitOfWorkImpl unitOfWork) {
        boolean successful = false;
        IdentityHashMap lockedObjects = new IdentityHashMap();
        IdentityHashMap refreshedObjects = new IdentityHashMap();
        try {
            // if the descriptor has indirection for all mappings then wait as there will be no deadlock risks
            CacheKey toWaitOn = acquireLockAndRelatedLocks(objectForClone, lockedObjects, refreshedObjects, cacheKey, descriptor, session, unitOfWork);
            int tries = 0;
            while (toWaitOn != null) {// loop until we've tried too many times.
                for (Iterator lockedList = lockedObjects.values().iterator(); lockedList.hasNext();) {
                    ((CacheKey)lockedList.next()).releaseReadLock();
                    lockedList.remove();
                }
                synchronized (toWaitOn.getMutex()) {
                    try {
                        if (toWaitOn.isAcquired()) {//last minute check to insure it is still locked.
                            toWaitOn.getMutex().wait();// wait for lock on object to be released
                        }
                    } catch (InterruptedException ex) {
                        // Ignore exception thread should continue.
                    }
                }
                Object waitObject = toWaitOn.getObject();
                // Object may be null for loss of identity.
                if (waitObject != null) {
                    unitOfWork.checkInvalidObject(waitObject, toWaitOn, session.getDescriptor(waitObject));
                    refreshedObjects.put(waitObject, waitObject);
                }
                toWaitOn = acquireLockAndRelatedLocks(objectForClone, lockedObjects, refreshedObjects, cacheKey, descriptor, session, unitOfWork);
                if ((toWaitOn != null) && ((++tries) > MAXTRIES)) {
                    // If we've tried too many times abort.
                    throw ConcurrencyException.maxTriesLockOnCloneExceded(objectForClone);
                }
            }
            successful = true;//successfully acquired all locks
        } finally {
            if (!successful) {//did not acquire locks but we are exiting
                for (Iterator lockedList = lockedObjects.values().iterator(); lockedList.hasNext();) {
                    ((CacheKey)lockedList.next()).releaseReadLock();
                    lockedList.remove();
                }
            }
        }
        return lockedObjects;
    }

    /**
     * INTERNAL:
     * This is a recursive method used to acquire read locks on all objects that
     * will be cloned.  These include all related objects for which there is no
     * indirection.
     * The returned object is the first object that the lock could not be acquired for.
     * The caller must try for exceptions and release locked objects in the case
     * of an exception.
     */
    public CacheKey acquireLockAndRelatedLocks(Object objectForClone, Map lockedObjects, Map refreshedObjects, CacheKey cacheKey, ClassDescriptor descriptor, AbstractSession session, UnitOfWorkImpl unitOfWork) {
        if (!refreshedObjects.containsKey(objectForClone) && this.checkInvalidObject(objectForClone, cacheKey, descriptor, unitOfWork)) {
            return cacheKey;
        }
        // Attempt to get a read-lock, null is returned if cannot be read-locked.
        if (cacheKey.acquireReadLockNoWait()) {
            if (cacheKey.getObject() == null) {
                // This will be the case for deleted objects, NoIdentityMap, and aggregates.
                lockedObjects.put(objectForClone, cacheKey);
            } else {
                objectForClone = cacheKey.getObject();
                if (lockedObjects.containsKey(objectForClone)) {
                    // This is a check for loss of identity, the orignal check in
                    // checkAndLockObject() will shortcircut in the usual case.
                    cacheKey.releaseReadLock();
                    return null;
                }
                // Store locked cachekey for release later.
                lockedObjects.put(objectForClone, cacheKey);
            }
            return traverseRelatedLocks(objectForClone, lockedObjects, refreshedObjects, descriptor, session, unitOfWork);
        } else {
            // Return the cache key that could not be locked.
            return cacheKey;
        }
    }
    
    /**
     * INTERNAL:
     * Check if the object is invalid and should be refreshed, return true, otherwise return false.
     * This is used to ensure that no invalid objects are registered.
     */
    public boolean checkInvalidObject(Object object, CacheKey cacheKey, ClassDescriptor descriptor, UnitOfWorkImpl unitOfWork) {
        if (!unitOfWork.isNestedUnitOfWork() && (cacheKey.getObject() != null)) {
            CacheInvalidationPolicy cachePolicy = descriptor.getCacheInvalidationPolicy();
            // BUG#6671556 refresh invalid objects when accessed in the unit of work.
            return (cachePolicy.shouldRefreshInvalidObjectsInUnitOfWork() && cachePolicy.isInvalidated(cacheKey));
        }
        return false;
    }

    /**
     * INTERNAL:
     * Traverse the object and acquire locks on all related objects.
     */
    public CacheKey traverseRelatedLocks(Object objectForClone, Map lockedObjects, Map refreshedObjects, ClassDescriptor descriptor, AbstractSession session, UnitOfWorkImpl unitOfWork) {
        // If all mappings have indirection short-circuit.
        if (descriptor.shouldAcquireCascadedLocks()) {
            FetchGroupManager fetchGroupManager = descriptor.getFetchGroupManager();
            boolean isPartialObject = (fetchGroupManager != null) && fetchGroupManager.isPartialObject(objectForClone);
            for (Iterator mappings = descriptor.getLockableMappings().iterator();
                     mappings.hasNext();) {
                DatabaseMapping mapping = (DatabaseMapping)mappings.next();
                // Only cascade fetched mappings.
                if (!isPartialObject || (fetchGroupManager.isAttributeFetched(objectForClone, mapping.getAttributeName()))) {
                    // any mapping in this list must not have indirection.
                    Object objectToLock = mapping.getAttributeValueFromObject(objectForClone);
                    if (mapping.isCollectionMapping()) {
                        // Ignore null, means empty.
                        if (objectToLock != null) {
                            ContainerPolicy cp = mapping.getContainerPolicy();
                            Object iterator = cp.iteratorFor(objectToLock);
                            while (cp.hasNext(iterator)) {
                                Object object = cp.next(iterator, session);
                                if (mapping.getReferenceDescriptor().hasWrapperPolicy()) {
                                    object = mapping.getReferenceDescriptor().getWrapperPolicy().unwrapObject(object, session);
                                }
                            CacheKey toWaitOn = checkAndLockObject(object, lockedObjects, refreshedObjects, mapping, session, unitOfWork);
                                if (toWaitOn != null) {
                                    return toWaitOn;
                                }
                            }
                        }
                    } else {
                        if (mapping.getReferenceDescriptor().hasWrapperPolicy()) {
                            objectToLock = mapping.getReferenceDescriptor().getWrapperPolicy().unwrapObject(objectToLock, session);
                        }
                    CacheKey toWaitOn = checkAndLockObject(objectToLock, lockedObjects, refreshedObjects, mapping, session, unitOfWork);
                        if (toWaitOn != null) {
                            return toWaitOn;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * INTERNAL:
     * This method will be the entry point for threads attempting to acquire locks for all objects that have
     * a changeset.  This method will hand off the processing of the deadlock algorithm to other member
     * methods.  The mergeManager must be the active mergemanager for the calling thread.
     * Returns true if all required locks were acquired
     */
    public void acquireRequiredLocks(MergeManager mergeManager, UnitOfWorkChangeSet changeSet) {
        if (!MergeManager.LOCK_ON_MERGE) {//lockOnMerge is a backdoor and not public
            return;
        }
        boolean locksToAcquire = true;
        boolean isForDistributedMerge = false;

        //while that thread has locks to acquire continue to loop.
        try {
            AbstractSession session = mergeManager.getSession();
            if (session.isUnitOfWork()) {
                session = ((UnitOfWorkImpl)session).getParent();
            } else {
                // if the session in the mergemanager is not a unit of work then the
                //merge is of a changeSet into a distributed session.
                isForDistributedMerge = true;
            }
            while (locksToAcquire) {
                //lets assume all locks will be acquired
                locksToAcquire = false;
                //first access the changeSet and begin to acquire locks
                Iterator classIterator = changeSet.getObjectChanges().keySet().iterator();
                while (classIterator.hasNext()) {
                    // Bug 3294426 - objectChanges is now indexed by class name instead of class
                    String objectClassName = (String)classIterator.next();
                    Hashtable changeSetTable = (Hashtable)changeSet.getObjectChanges().get(objectClassName);

                    //the order here does not matter as the deadlock avoidance code will handle any conflicts and maintaining
                    //order would be costly
                    Iterator changeSetIterator = changeSetTable.keySet().iterator();

                    // Perf: Bug 3324418 - Reduce the number of Class.forName() calls
                    Class objectClass = null;
                    while (changeSetIterator.hasNext()) {
                        ObjectChangeSet objectChangeSet = (ObjectChangeSet)changeSetIterator.next();
                        if (objectChangeSet.getCacheKey() == null) {
                            //skip this process as we will be unable to acquire the correct cachekey anyway
                            //this is a new object with identity after write sequencing
                            continue;
                        }
                        if (objectClass == null) {
                            objectClass = objectChangeSet.getClassType(session);
                        }
                        // It would be so much nicer if the change set was keyed by the class instead of class name,
                        // so this could be done once.  We should key on class, and only convert to keying on name when broadcasting changes.
                        ClassDescriptor descriptor = session.getDescriptor(objectClass);
                        // PERF: Do not merge nor lock into the session cache if descriptor set to unit of work isolated.
                        if (descriptor.shouldIsolateObjectsInUnitOfWork()) {
                            break;
                        }
                        CacheKey activeCacheKey = attemptToAcquireLock(objectClass, objectChangeSet.getCacheKey(), session);
                        if (activeCacheKey == null) {
                            //if cacheKey is null then the lock was not available
                            //no need to synchronize this block,because if the check fails then this thread
                            //will just return to the queue until it gets woken up.
                            if (this.prevailingQueue.getFirst() == mergeManager) {
                                //wait on this object until it is free, because this thread is the prevailing thread
                                activeCacheKey = waitOnObjectLock(objectClass, objectChangeSet.getCacheKey(), session);
                                mergeManager.getAcquiredLocks().add(activeCacheKey);
                            } else {
                                //failed to acquire lock, release all acquired locks and place thread on waiting list
                                releaseAllAcquiredLocks(mergeManager);
                                //get cacheKey
                                activeCacheKey = session.getIdentityMapAccessorInstance().getCacheKeyForObjectForLock(objectChangeSet.getCacheKey().getKey(), objectClass, descriptor);
                                if (session.shouldLog(SessionLog.FINER, SessionLog.CACHE)){
                                    Object[] params = new Object[3];
                                    params[0] = objectClass;
                                    params[1] = objectChangeSet.getCacheKey() != null ? objectChangeSet.getCacheKey().getKey() : new Vector();
                                    params[2] = Thread.currentThread().getName();
                                    session.log(SessionLog.FINER, SessionLog.CACHE, "dead_lock_encountered_on_write_no_cachekey", params, null, true);
                                }
                                if (mergeManager.getWriteLockQueued() == null) {
                                    //thread is entering the wait queue for the first time
                                    //set the QueueNode to be the node from the linked list for quick removal upon 
                                    //acquiring all locks
                                    synchronized(this.prevailingQueue) {
                                        mergeManager.setQueueNode(this.prevailingQueue.addLast(mergeManager));
                                    }
                                }

                                //set the cache key on the merge manager for the object that could not be acquired
                                mergeManager.setWriteLockQueued(objectChangeSet.getCacheKey());
                                try {
                                    if (activeCacheKey != null){
                                        //wait on the lock of the object that we couldn't get.
                                        synchronized (activeCacheKey.getMutex()) {
                                            // verify that the cache key is still locked before we wait on it, as
                                            //it may have been releases since we tried to acquire it.
                                            if (activeCacheKey.getMutex().isAcquired() && (activeCacheKey.getMutex().getActiveThread() != Thread.currentThread())) {
                                                activeCacheKey.getMutex().wait();
                                            }
                                        }
                                    }
                                } catch (InterruptedException exception) {
                                    throw org.eclipse.persistence.exceptions.ConcurrencyException.waitWasInterrupted(exception.getMessage());
                                }
                                locksToAcquire = true;
                                //failed to acquire, exit this loop and ensure that the original loop will continue
                                break;
                            }
                        } else {
                            objectChangeSet.setActiveCacheKey(activeCacheKey);
                            mergeManager.getAcquiredLocks().add(activeCacheKey);
                        }
                    }

                    //if a lock failed reset to the beginning
                    if (locksToAcquire) {
                        break;
                    }
                }
            }
        } catch (RuntimeException exception) {
            // if there was an exception then release.
            //must not release in a finally block as release only occurs in this method
            // if there is a problem or all of the locks can not be acquired.
            releaseAllAcquiredLocks(mergeManager);
            throw exception;
        } finally {
            if (mergeManager.getWriteLockQueued() != null) {
                //the merge manager entered the wait queue and must be cleaned up
                synchronized(this.prevailingQueue) {
                    this.prevailingQueue.remove(mergeManager.getQueueNode());
                }
                mergeManager.setWriteLockQueued(null);
            }
        }
    }

	/**
	 * INTERNAL:
	 * This method will be called by a merging thread that is attempting to lock
	 * a new object that was not locked previously.  Unlike the other methods
	 * within this class this method will lock only this object.
	 */
	public Object appendLock(Vector primaryKeys, Object objectToLock, ClassDescriptor descriptor, MergeManager mergeManager, AbstractSession session){
        for (int tries = 0; tries < 1000; ++tries) {  //lets try a fixed number of times
            CacheKey lockedCacheKey = session.getIdentityMapAccessorInstance().acquireLockNoWait(primaryKeys, descriptor.getJavaClass(), true, descriptor);
            if (lockedCacheKey == null){
                //acquire readlock and wait for owning thread to populate cachekey
                //bug 4483312
                lockedCacheKey = session.getIdentityMapAccessorInstance().acquireReadLockOnCacheKey(primaryKeys, descriptor.getJavaClass(), descriptor);
                Object cachedObject = lockedCacheKey.getObject();
                lockedCacheKey.releaseReadLock();
                if (cachedObject == null){
                    session.getSessionLog().log(SessionLog.FINEST, SessionLog.CACHE, "Found null object in identity map on appendLock, retrying");
                    continue;
                }else{
                    return cachedObject;
                }
            }
            if (lockedCacheKey.getObject() == null){
                lockedCacheKey.setObject(objectToLock); // set the object in the cachekey
                // for others to find an prevent cycles
            }
            mergeManager.getAcquiredLocks().add(lockedCacheKey);
            return objectToLock;
        }
        throw ConcurrencyException.maxTriesLockOnMergeExceded(objectToLock); 
	}
	
	/**
     * INTERNAL:
     * This method performs the operations of finding the cacheKey and locking it if possible.
     * Returns True if the lock was acquired, false otherwise
     */
    protected CacheKey attemptToAcquireLock(Class objectClass, CacheKey cacheKey, AbstractSession session) {
        return session.getIdentityMapAccessorInstance().acquireLockNoWait(cacheKey.getKey(), objectClass, true, session.getDescriptor(objectClass));
    }

    /**
     * INTERNAL:
     * Simply check that the object is not already locked then pass it on to the locking method
     */
    protected CacheKey checkAndLockObject(Object objectToLock, Map lockedObjects, Map refreshedObjects, DatabaseMapping mapping, AbstractSession session, UnitOfWorkImpl unitOfWork) {
        //the cachekey should always reference an object otherwise what would we be cloning.
        if ((objectToLock != null) && !lockedObjects.containsKey(objectToLock)) {
            Vector primaryKeysToLock = null;
            ClassDescriptor referenceDescriptor = null;
            if (mapping.getReferenceDescriptor().hasInheritance() || mapping.getReferenceDescriptor().isDescriptorForInterface()) {
                referenceDescriptor = session.getDescriptor(objectToLock);
            } else {
                referenceDescriptor = mapping.getReferenceDescriptor();
            }
            // Need to traverse aggregates, but not lock aggregates directly.
            if (referenceDescriptor.isAggregateDescriptor() || referenceDescriptor.isAggregateCollectionDescriptor()) {
                traverseRelatedLocks(objectToLock, lockedObjects, refreshedObjects, referenceDescriptor, session, unitOfWork);
            } else {
                primaryKeysToLock = referenceDescriptor.getObjectBuilder().extractPrimaryKeyFromObject(objectToLock, session);
                CacheKey cacheKey = session.getIdentityMapAccessorInstance().getCacheKeyForObjectForLock(primaryKeysToLock, objectToLock.getClass(), referenceDescriptor);
                if (cacheKey == null) {
                    // Cache key may be null for no-identity map, missing or deleted object, just create a new one to be locked.
                    cacheKey = new CacheKey(primaryKeysToLock);
                    cacheKey.setReadTime(System.currentTimeMillis());
                }
                CacheKey toWaitOn = acquireLockAndRelatedLocks(objectToLock, lockedObjects, refreshedObjects, cacheKey, referenceDescriptor, session, unitOfWork);
                if (toWaitOn != null) {
                    return toWaitOn;
                }
            }
        }
        return null;
    }

    /**
     * INTERNAL:
     * This method will release all acquired locks
     */
    public void releaseAllAcquiredLocks(MergeManager mergeManager) {
        if (!MergeManager.LOCK_ON_MERGE) {//lockOnMerge is a backdoor and not public
            return;
        }
        Iterator locks = mergeManager.getAcquiredLocks().iterator();
        while (locks.hasNext()) {
            CacheKey cacheKeyToRemove = (CacheKey)locks.next();
            if (cacheKeyToRemove.getObject() == null ){
                cacheKeyToRemove.removeFromOwningMap();
            }
            cacheKeyToRemove.release();
            locks.remove();
        }
    }

    /**
      * INTERNAL:
      * This method performs the operations of finding the cacheKey and locking it if possible.
      * Waits until the lock can be acquired
      */
    protected CacheKey waitOnObjectLock(Class objectClass, CacheKey cacheKey, AbstractSession session) {
        return session.getIdentityMapAccessorInstance().acquireLock(cacheKey.getKey(), objectClass, true, session.getDescriptor(objectClass));
    }
}