/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 *     02/11/2009-1.1 Michael O'Brien 
 *        - 259993: As part 2) During mergeClonesAfterCompletion() 
 *        If the the acquire and release threads are different 
 *        switch back to the stored acquire thread stored on the mergeManager.
 ******************************************************************************/  
package org.eclipse.persistence.internal.sessions;

import java.util.*;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.VersionLockingPolicy;
import org.eclipse.persistence.internal.descriptors.ObjectBuilder;
import org.eclipse.persistence.internal.descriptors.OptimisticLockingPolicy;
import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.internal.helper.linkedlist.LinkedNode;
import org.eclipse.persistence.queries.DoesExistQuery;
import org.eclipse.persistence.sessions.remote.*;
import org.eclipse.persistence.internal.sessions.remote.*;
import org.eclipse.persistence.internal.identitymaps.*;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.sessions.SessionProfiler;

/**
 * <p><b>Purpose</b>:
 * Used to manage the merge of two objects in a unit of work.
 *
 * @author James Sutherland
 * @since TOPLink/Java 1.1
 */
public class MergeManager {

    /** The unit of work merging for. */
    protected AbstractSession session;

    /** Used only while refreshing objects on remote session */
    protected Map objectDescriptors;

    /** Used to unravel recursion. */
    protected Map<AbstractSession, Map<Object, Object>> objectsAlreadyMerged;
    
    /** Used to keep track of merged new objects. */
    protected IdentityHashMap mergedNewObjects;

    /** Used to store the list of locks that this merge manager has acquired for this merge */
    protected ArrayList<CacheKey> acquiredLocks;

    /** If this variable is not null then the mergemanager is waiting on a particular primary key */
    protected Object writeLockQueued;

    /** Stores the node that holds this mergemanager within the WriteLocksManager queue */
    protected LinkedNode queueNode;

    /** Policy that determines merge type (i.e. merge is used for several usages). */
    protected int mergePolicy;
    protected static final int WORKING_COPY_INTO_ORIGINAL = 1;
    protected static final int ORIGINAL_INTO_WORKING_COPY = 2;
    protected static final int CLONE_INTO_WORKING_COPY = 3;
    protected static final int WORKING_COPY_INTO_REMOTE = 4;
    protected static final int REFRESH_REMOTE_OBJECT = 5;
    protected static final int CHANGES_INTO_DISTRIBUTED_CACHE = 6;
    protected static final int CLONE_WITH_REFS_INTO_WORKING_COPY = 7;

    /** Policy that determines how the merge will cascade to its object's parts. */
    protected int cascadePolicy;
    public static final int NO_CASCADE = 1;
    public static final int CASCADE_PRIVATE_PARTS = 2;
    public static final int CASCADE_ALL_PARTS = 3;
    public static final int CASCADE_BY_MAPPING = 4;
    
    /** Backdoor to disable merge locks. */
    public static boolean LOCK_ON_MERGE = true;
    
    /** Stored so that all objects merged by a merge manager can have the same readTime. */
    protected long systemTime = 0;
    
    /** Force cascade merge even if a clone is already registered */
    // GF#1139 Cascade doesn't work when merging managed entity
    protected boolean forceCascade;
    
    /** records that deferred locks have been employed for the merge process */
    protected boolean isTransitionedToDeferredLocks = false;

    /** save the currentThread for later comparison to the activeThread in case they don't match */
    protected Thread lockThread;
    
    public MergeManager(AbstractSession session) {
        this.session = session;
        this.mergedNewObjects = new IdentityHashMap();
        this.objectsAlreadyMerged = new IdentityHashMap();
        this.cascadePolicy = CASCADE_ALL_PARTS;
        this.mergePolicy = WORKING_COPY_INTO_ORIGINAL;
        this.acquiredLocks = new ArrayList<CacheKey>();
    }

    /**
     * Cascade all parts, this is the default for the merge.
     */
    public void cascadeAllParts() {
        setCascadePolicy(CASCADE_ALL_PARTS);
    }

    /**
     * Cascade private parts, this can be used to merge clone when using RMI.
     */
    public void cascadePrivateParts() {
        setCascadePolicy(CASCADE_PRIVATE_PARTS);
    }

    /**
     * Merge only direct parts, this can be used to merge clone when using RMI.
     */
    public void dontCascadeParts() {
        setCascadePolicy(NO_CASCADE);
    }

    public ArrayList<CacheKey> getAcquiredLocks() {
        return this.acquiredLocks;
    }

    public int getCascadePolicy() {
        return cascadePolicy;
    }

    protected int getMergePolicy() {
        return mergePolicy;
    }

    public Map getObjectDescriptors() {
        if (this.objectDescriptors == null) {
            this.objectDescriptors = new IdentityHashMap();
        }
        return this.objectDescriptors;
    }

    public Map getObjectsAlreadyMerged() {
        return objectsAlreadyMerged;
    }

    public Object getObjectToMerge(Object sourceValue, ClassDescriptor descriptor, AbstractSession targetSession) {
        if (shouldMergeOriginalIntoWorkingCopy()) {
            return getTargetVersionOfSourceObject(sourceValue, descriptor, targetSession);
        }

        return sourceValue;
    }

    /**
     * INTENRAL:
     * Used to get the node that this merge manager is stored in, within the WriteLocksManager write lockers queue
     */
    public LinkedNode getQueueNode() {
        return this.queueNode;
    }

    public AbstractSession getSession() {
        return session;
    }

    /**
     * Get the stored value of the current time.  This method lazily initializes
     * so that read times for the same merge manager can all be set to the same read time
     */
    public long getSystemTime() {
        if (systemTime == 0) {
            systemTime = System.currentTimeMillis();
        }
        return systemTime;
    }

    /**
     * Return the corresponding value that should be assigned to the target object for the source object.
     * This value must be local to the targets object space.
     */
    public Object getTargetVersionOfSourceObject(Object source, ClassDescriptor descriptor, AbstractSession targetSession) {
        if (shouldMergeWorkingCopyIntoOriginal()){
            Object original = null;
            CacheKey cacheKey = targetSession.getCacheKeyFromTargetSessionForMerge(source, descriptor.getObjectBuilder(), descriptor, this);
            if (cacheKey != null){
                original = cacheKey.getObject();
            }
            if (original == null){
                original = ((UnitOfWorkImpl) this.session).getOriginalVersionOfObjectOrNull(source, null, descriptor, targetSession);
            }
            // If original does not exist then we must merge the entire object.
            if (original == null){
                original = ((UnitOfWorkImpl) this.session).buildOriginal(source);
                //ensure new original has PKs populated as they may be needed later.
                if (descriptor.getCopyPolicy().buildsNewInstance()){
                    List<DatabaseMapping> pkMappings = descriptor.getObjectBuilder().getPrimaryKeyMappings();
                    for (DatabaseMapping mapping : pkMappings){
                        mapping.buildClone(source, null, original, targetSession);
                    }
                }
            }
            return original;
        }else if (shouldMergeWorkingCopyIntoRemote()) {
            // Target is in uow parent, or original instance for new object.
            return ((UnitOfWorkImpl)this.session).getOriginalVersionOfObject(source);
        } else if (shouldMergeCloneIntoWorkingCopy() || shouldMergeOriginalIntoWorkingCopy() || shouldMergeCloneWithReferencesIntoWorkingCopy()) {
            // Target is clone from uow.
            //make sure we use the register for merge
            //bug 3584343
            return registerObjectForMergeCloneIntoWorkingCopy(source);
        } else if (shouldRefreshRemoteObject()) {
            // Target is in session's cache.
            Object primaryKey = descriptor.getObjectBuilder().extractPrimaryKeyFromObject(source, this.session);
            return this.session.getIdentityMapAccessorInstance().getFromIdentityMap(primaryKey, source.getClass(), descriptor);
        }

        throw ValidationException.invalidMergePolicy();
    }

    /**
     * INTENRAL:
     * Used to get the object that the merge manager is waiting on, in order to acquire locks
     */
    public Object getWriteLockQueued() {
        return this.writeLockQueued;
    }
    
    /**
     * INTERNAL:
     * Will return if the merge process has transitioned the active merge locks to deferred locks for
     * readlock deadlock avoidance.
     */
    public boolean isTransitionedToDeferredLocks(){
        return this.isTransitionedToDeferredLocks;
    }
    
    /**
     * Recursively merge changes in the object dependent on the merge policy.
     * The map is used to resolve recursion.
     */
    public Object mergeChanges(Object object, ObjectChangeSet objectChangeSet, AbstractSession targetSession) throws ValidationException {
        if (object == null) {
            return object;
        }

        // Do not merge read-only objects in a unit of work.
        if (this.session.isClassReadOnly(object.getClass())) {
            return object;
        }

        // Means that object is either already merged or in the process of being merged.	
        if (isAlreadyMerged(object, targetSession)) {
            return object;
        }

        // Put the object to be merged in the set.
        recordMerge(object, object, targetSession);

        Object mergedObject;
        if (shouldMergeWorkingCopyIntoOriginal()) {
            mergedObject = mergeChangesOfWorkingCopyIntoOriginal(object, objectChangeSet);
        } else if (shouldMergeChangesIntoDistributedCache()) {
            mergedObject = mergeChangesIntoDistributedCache(object, objectChangeSet);
        } else if (shouldMergeCloneIntoWorkingCopy() || shouldMergeCloneWithReferencesIntoWorkingCopy()) {
            mergedObject = mergeChangesOfCloneIntoWorkingCopy(object);
        } else if (shouldMergeOriginalIntoWorkingCopy()) {
            mergedObject = mergeChangesOfOriginalIntoWorkingCopy(object);
        } else if (shouldMergeWorkingCopyIntoRemote()) {
            mergedObject = mergeChangesOfWorkingCopyIntoRemote(object);
        } else if (shouldRefreshRemoteObject()) {
            mergedObject = mergeChangesForRefreshingRemoteObject(object);
        } else {
            throw ValidationException.invalidMergePolicy();
        }

        return mergedObject;
    }

    public void recordMerge(Object key, Object value, AbstractSession targetSession) {
        Map sessionMap = this.objectsAlreadyMerged.get(targetSession);
        if (sessionMap == null){
            sessionMap = new IdentityHashMap();
            this.objectsAlreadyMerged.put(targetSession, sessionMap);
        }
        sessionMap.put(key, value);
    }

    public boolean isAlreadyMerged(Object object, AbstractSession targetSession) {
        Map sessionMap = this.objectsAlreadyMerged.get(targetSession);
        if (sessionMap == null){
            return false;
        }
        return sessionMap.containsKey(object);
    }
    
    public Object getMergedObject(Object key, AbstractSession targetSession){
        Map sessionMap = this.objectsAlreadyMerged.get(targetSession);
        if (sessionMap == null){
            return null;
        }
        return sessionMap.get(key);
    }

    /**
     * Recursively merge the RMI clone from the server
     * into the client unit of work working copy.
     * This will only be called if the working copy exists.
     */
    protected Object mergeChangesForRefreshingRemoteObject(Object serverSideDomainObject) {
        ClassDescriptor descriptor = this.session.getDescriptor(serverSideDomainObject);
        Object primaryKey = descriptor.getObjectBuilder().extractPrimaryKeyFromObject(serverSideDomainObject, this.session);
        Object clientSideDomainObject = this.session.getIdentityMapAccessorInstance().getFromIdentityMap(primaryKey, serverSideDomainObject.getClass(), descriptor);
        if (clientSideDomainObject == null) {
            //the referenced object came back as null from the cache.
            ObjectDescriptor objectDescriptor = (ObjectDescriptor)getObjectDescriptors().get(serverSideDomainObject);
            if (objectDescriptor == null){
                //the object must have been added concurently before serialize generate a new ObjectDescriptor on this side
                objectDescriptor = new ObjectDescriptor();
                objectDescriptor.setKey(primaryKey);
                objectDescriptor.setObject(serverSideDomainObject);
                OptimisticLockingPolicy policy = descriptor.getOptimisticLockingPolicy();
                if (policy == null){
                    objectDescriptor.setWriteLockValue(null);
                }else{
                    objectDescriptor.setWriteLockValue(policy.getBaseValue());
                }
            }
            //query is used for the cascade policy only
            org.eclipse.persistence.queries.ObjectLevelReadQuery query = new org.eclipse.persistence.queries.ReadObjectQuery();
            query.setCascadePolicy(this.getCascadePolicy());
            this.session.getIdentityMapAccessorInstance().putInIdentityMap(serverSideDomainObject, primaryKey, objectDescriptor.getWriteLockValue(), objectDescriptor.getReadTime(), descriptor);
            descriptor.getObjectBuilder().fixObjectReferences(serverSideDomainObject, getObjectDescriptors(), this.objectsAlreadyMerged.get(this.session), query, (RemoteSession)this.session);
            clientSideDomainObject = serverSideDomainObject;
        } else {
            // merge into the clientSideDomainObject from the serverSideDomainObject;
            // use clientSideDomainObject as the backup, as anything different should be merged
            descriptor.getObjectBuilder().mergeIntoObject(clientSideDomainObject, false, serverSideDomainObject, this, getSession());
            ObjectDescriptor objectDescriptor = (ObjectDescriptor)getObjectDescriptors().get(serverSideDomainObject);
            if (objectDescriptor == null){
                //the object must have been added concurently before serialize generate a new ObjectDescriptor on this side
                objectDescriptor = new ObjectDescriptor();
                objectDescriptor.setKey(primaryKey);
                objectDescriptor.setObject(serverSideDomainObject);
                OptimisticLockingPolicy policy = descriptor.getOptimisticLockingPolicy();
                if (policy == null){
                    objectDescriptor.setWriteLockValue(null);
                }else{
                    objectDescriptor.setWriteLockValue(policy.getBaseValue());
                }
            }
            CacheKey key = this.session.getIdentityMapAccessorInstance().getCacheKeyForObjectForLock(primaryKey, clientSideDomainObject.getClass(), descriptor);

            // Check for null because when there is NoIdentityMap, CacheKey will be null
            if (key != null) {
                key.setReadTime(objectDescriptor.getReadTime());
            }
            if (descriptor.usesOptimisticLocking()) {
                this.session.getIdentityMapAccessor().updateWriteLockValue(primaryKey, clientSideDomainObject.getClass(), objectDescriptor.getWriteLockValue());
            }
        }
        return clientSideDomainObject;
    }

    /**
     * INTERNAL:
     * Merge the changes to all objects to session's cache.
     */
    public void mergeChangesFromChangeSet(UnitOfWorkChangeSet uowChangeSet) {
        this.session.startOperationProfile(SessionProfiler.DistributedMerge);
        try {
            // Ensure concurrency if cache isolation requires.
            this.session.getIdentityMapAccessorInstance().acquireWriteLock();
            this.session.log(SessionLog.FINER, SessionLog.PROPAGATION, "received_updates_from_remote_server");
            if (this.session.hasEventManager()) {
                this.session.getEventManager().preDistributedMergeUnitOfWorkChangeSet(uowChangeSet);
            }
            // Iterate over each clone and let the object build merge to clones into the originals.
            this.session.getIdentityMapAccessorInstance().getWriteLockManager().acquireRequiredLocks(this, uowChangeSet);
            Iterator objectChangeEnum = uowChangeSet.getAllChangeSets().keySet().iterator();
            while (objectChangeEnum.hasNext()) {
                ObjectChangeSet objectChangeSet = (ObjectChangeSet)objectChangeEnum.next();
                // Don't read the object here.  If it is null then we won't merge it at this stage, unless it
                // is being referenced which will force the load later.
                Object object = objectChangeSet.getTargetVersionOfSourceObject(this, this.session, false);
                if (object != null) {
                    mergeChanges(object, objectChangeSet, this.session);
                    this.session.incrementProfile(SessionProfiler.ChangeSetsProcessed);
                } else if (objectChangeSet.isNew()) {
                    mergeNewObjectIntoCache(objectChangeSet);
                    this.session.incrementProfile(SessionProfiler.ChangeSetsProcessed);
                } else {
                    this.session.incrementProfile(SessionProfiler.ChangeSetsNotProcessed);
                }
            }
            if (uowChangeSet.hasDeletedObjects()) {
                Iterator deletedObjects = uowChangeSet.getDeletedObjects().values().iterator();
                while (deletedObjects.hasNext()) {
                    ObjectChangeSet changeSet = (ObjectChangeSet)deletedObjects.next();
                    changeSet.removeFromIdentityMap(this.session);
                }
            }
        } catch (RuntimeException exception) {
            this.session.handleException(exception);
        } finally {
            this.session.getIdentityMapAccessorInstance().getWriteLockManager().releaseAllAcquiredLocks(this);
            this.session.getIdentityMapAccessorInstance().releaseWriteLock();
            this.session.endOperationProfile(SessionProfiler.DistributedMerge);
            if (this.session.hasEventManager()) {
                this.session.getEventManager().postDistributedMergeUnitOfWorkChangeSet(uowChangeSet);
            }
        }
    }

    /**
     * Merge the changes specified within the changeSet into the cache.
     * The object passed in is the original object from the cache.
     */
    protected Object mergeChangesIntoDistributedCache(Object original, ObjectChangeSet changeSet) {
        AbstractSession session = this.session;
        
        // Determine if the object needs to be registered in the parent's clone mapping,
        // This is required for registered new objects in a nested unit of work.
        Class localClassType = changeSet.getClassType(session);
        ClassDescriptor descriptor = session.getDescriptor(localClassType);

        // Perform invalidation of a cached object (when set on the ChangeSet) to avoid refreshing or merging
        if (changeSet.getSynchronizationType() == ClassDescriptor.INVALIDATE_CHANGED_OBJECTS) {
            session.getIdentityMapAccessorInstance().invalidateObject(changeSet.getId(), localClassType);
            return original;
        }
        
        // If version locking was used, check if the cache version is the correct version, otherwise invalidate,
        // Don't know for no locking, or field locking, so always merge.
        if ((!changeSet.isNew()) && descriptor.usesVersionLocking()) {
            if ((session.getCommandManager() != null) && (session.getCommandManager().getCommandConverter() != null)) {
                // Rebuild the version value from user format i.e the change set was converted to XML
                changeSet.rebuildWriteLockValueFromUserFormat(descriptor, session);
            }
            int difference = descriptor.getOptimisticLockingPolicy().getVersionDifference(changeSet.getInitialWriteLockValue(), original, changeSet.getId(), session);
            
            // Should be = 0 if was a good update, otherwise was already refreshed, or a version change was lost.
            if (difference < 0) {
                // The current version is newer than the one on the remote system, was refreshed already, ignore change.
                session.log(SessionLog.FINEST, SessionLog.PROPAGATION, "change_from_remote_server_older_than_current_version", changeSet.getClassName(), changeSet.getId());
                return original;
            } else if (difference > 0) {
                // If the current version is much older than the remote system, so invalidate the object as a change was missed.
                session.log(SessionLog.FINEST, SessionLog.PROPAGATION, "current_version_much_older_than_change_from_remote_server", changeSet.getClassName(), changeSet.getId());
                session.getIdentityMapAccessorInstance().invalidateObject(changeSet.getId(), localClassType);
                return original;
            }
        }

        // Always merge into the original.
        session.log(SessionLog.FINEST, SessionLog.PROPAGATION, "Merging_from_remote_server", changeSet.getClassName(), changeSet.getId());

        if (changeSet.isNew() || (changeSet.getSynchronizationType() != ClassDescriptor.DO_NOT_SEND_CHANGES)) {
            Object primaryKey = changeSet.getId();

            // PERF: Get the cached cache-key from the change-set.
            CacheKey cacheKey = changeSet.getActiveCacheKey();
            // The cache key should never be null for the new commit locks, but may be depending on the cache isolation level may not be locked,
            // so needs to be re-acquired.
            if (cacheKey == null || !cacheKey.isAcquired()) {                
                // ELBug 355610 - Use appendLock() instead of acquireLock() for transitioning 
                // to deferred locks for new objects in order to avoid the possibility of a deadlock.
                cacheKey = session.getIdentityMapAccessorInstance().getWriteLockManager().appendLock(primaryKey, original, descriptor, this, session);
            }
            descriptor.getObjectBuilder().mergeChangesIntoObject(original, changeSet, null, this, session, false, false);
            if (descriptor.usesOptimisticLocking() && descriptor.getOptimisticLockingPolicy().isStoredInCache()) {
                cacheKey.setWriteLockValue(changeSet.getWriteLockValue());
            }
            cacheKey.setObject(original);
            if (descriptor.getCacheInvalidationPolicy().shouldUpdateReadTimeOnUpdate() || changeSet.isNew()) {
                cacheKey.setReadTime(getSystemTime());
            }
            cacheKey.updateAccess();
        }

        return original;
    }

    /**
     * Recursively merge to rmi clone into the unit of work working copy.
     * The map is used to resolve recursion.
     */
    protected Object mergeChangesOfCloneIntoWorkingCopy(Object rmiClone) {
        Object registeredObject = registerObjectForMergeCloneIntoWorkingCopy(rmiClone);

        if (registeredObject == rmiClone && !shouldForceCascade()) {
            //need to find better better fix.  prevents merging into itself.
            return rmiClone;
        }

        ClassDescriptor descriptor = this.session.getDescriptor(rmiClone);
        try {
            ObjectBuilder builder = descriptor.getObjectBuilder();
            
            if (registeredObject != rmiClone && descriptor.usesVersionLocking() && ! mergedNewObjects.containsKey(registeredObject)) {
                VersionLockingPolicy policy = (VersionLockingPolicy) descriptor.getOptimisticLockingPolicy();
                if (policy.isStoredInObject()) {
                    Object currentValue = builder.extractValueFromObjectForField(registeredObject, policy.getWriteLockField(), session); 
                
                    if (policy.isNewerVersion(currentValue, rmiClone, session.keyFromObject(rmiClone, descriptor), session)) {
                        throw OptimisticLockException.objectChangedSinceLastMerge(rmiClone);
                    }
                }
            }
            
            // Toggle change tracking during the merge.
            descriptor.getObjectChangePolicy().dissableEventProcessing(registeredObject);
            
            boolean cascadeOnly = false;
            if (registeredObject == rmiClone || mergedNewObjects.containsKey(registeredObject)) {    
                // GF#1139 Cascade merge operations to relationship mappings even if already registered
                cascadeOnly = true;
            }
            
            // Merge into the clone from the original and use the clone as 
            // backup as anything different should be merged.
            builder.mergeIntoObject(registeredObject, false, rmiClone, this, this.session, cascadeOnly, false, false);  
        } finally {
            descriptor.getObjectChangePolicy().enableEventProcessing(registeredObject);
        }

        return registeredObject;
    }

    /**
     * Recursively merge to original from its parent into the clone.
     * The map is used to resolve recursion.
     */
    protected Object mergeChangesOfOriginalIntoWorkingCopy(Object clone) {
        ClassDescriptor descriptor = this.session.getDescriptor(clone);

        // Find the original object, if it is not there then do nothing.
        Object primaryKey = descriptor.getObjectBuilder().extractPrimaryKeyFromObject(clone, this.session, true);
        CacheKey parentCacheKey = null;
        if (primaryKey != null){
            parentCacheKey = ((UnitOfWorkImpl)this.session).getParentIdentityMapSession(descriptor, false, false).getIdentityMapAccessorInstance().getCacheKeyForObjectForLock(primaryKey, clone.getClass(), descriptor);
        }
        Object original = null;
        if (parentCacheKey != null){
            original = parentCacheKey.getObject();
        }else{
            if (descriptor.isProtectedIsolation()  && descriptor.hasNoncacheableMappings()){
                this.session.refreshObject(clone);
                return clone;
            }
            original = ((UnitOfWorkImpl)this.session).getOriginalVersionOfObjectOrNull(clone, descriptor);
        }

        if (original == null) {
            return clone;
        }
        
        // Toggle change tracking during the merge.
        descriptor.getObjectChangePolicy().dissableEventProcessing(clone);
        try {
            // This section of code will only be entered if the protected object is being cached in the IsolatedClientSession
            // so there is a fully populated original.
            // Merge into the clone from the original, use clone as backup as anything different should be merged.
            descriptor.getObjectBuilder().mergeIntoObject(clone, false, original, this, session);
        } finally {
            descriptor.getObjectChangePolicy().enableEventProcessing(clone);
        }
        //update the change policies with the refresh
        descriptor.getObjectChangePolicy().revertChanges(clone, descriptor, (UnitOfWorkImpl)this.session, ((UnitOfWorkImpl)this.session).getCloneMapping(), true);
        if (primaryKey == null) {
            return clone;
        }
        
        if (descriptor.usesOptimisticLocking()) {
            descriptor.getOptimisticLockingPolicy().mergeIntoParentCache((UnitOfWorkImpl)this.session, primaryKey, clone);
        }
        CacheKey uowCacheKey = this.session.getIdentityMapAccessorInstance().getCacheKeyForObjectForLock(primaryKey, clone.getClass(), descriptor);

        // Check for null because when there is NoIdentityMap, CacheKey will be null
        if ((parentCacheKey != null) && (uowCacheKey != null)) {
            uowCacheKey.setReadTime(parentCacheKey.getReadTime());
        }

        return clone;
    }

    /**
     * Recursively merge to clone into the original in its parent.
     * The map is used to resolve recursion.
     */
    protected Object mergeChangesOfWorkingCopyIntoOriginal(Object clone, ObjectChangeSet objectChangeSet) {
        UnitOfWorkImpl unitOfWork = (UnitOfWorkImpl)this.session;
        AbstractSession parent = unitOfWork.getParent();
        
        // If the clone is deleted, avoid this merge and simply return the clone.
        if (unitOfWork.isObjectDeleted(clone)) {
            return clone;
        }
        
        // Determine if the object needs to be registered in the parent's clone mapping,
        // This is required for registered new objects in a nested unit of work.
        boolean requiresToRegisterInParent = false;
        if (unitOfWork.isNestedUnitOfWork()) {
            Object originalNewObject = unitOfWork.getOriginalVersionOfNewObject(clone);
            if ((originalNewObject != null) // Check that the object is new.
                     && (!((UnitOfWorkImpl)parent).isCloneNewObject(originalNewObject)) && (!unitOfWork.isUnregisteredNewObjectInParent(originalNewObject))) {
                requiresToRegisterInParent = true;
            }
        }
        ClassDescriptor descriptor = unitOfWork.getDescriptor(clone.getClass());
        AbstractSession parentSession = unitOfWork.getParentIdentityMapSession(descriptor, false, false);
        CacheKey cacheKey = mergeChangesOfWorkingCopyIntoOriginal(clone, objectChangeSet, descriptor, parentSession, unitOfWork);
        Object original = cacheKey.getObject();
        AbstractSession sharedSession = parentSession;
        if (descriptor.isProtectedIsolation()){
            if (parentSession.isIsolatedClientSession()){
                //merge into the shared cache as well.
                sharedSession = parentSession.getParent();
                cacheKey = mergeChangesOfWorkingCopyIntoOriginal(clone, objectChangeSet, descriptor, sharedSession, unitOfWork);
            }else if (!parentSession.isProtectedSession()){
                descriptor.getObjectBuilder().cacheForeignKeyValues(clone, cacheKey, descriptor, sharedSession);
            }
        }

        if (requiresToRegisterInParent) {
            // Can use a new instance as backup and original.
            Object backupClone = descriptor.getObjectBuilder().buildNewInstance();
            Object newInstance = descriptor.getObjectBuilder().buildNewInstance();
            ((UnitOfWorkImpl)parent).registerOriginalNewObjectFromNestedUnitOfWork(original, backupClone, newInstance, descriptor);
        }
        return clone;
}
    
    /**
     * Recursively merge to clone into the original in its parent.
     * The map is used to resolve recursion.
     */
    protected CacheKey mergeChangesOfWorkingCopyIntoOriginal(Object clone, ObjectChangeSet objectChangeSet, ClassDescriptor descriptor, AbstractSession targetSession, UnitOfWorkImpl unitOfWork) {

        ObjectBuilder objectBuilder = descriptor.getObjectBuilder();
        // This always finds an original different from the clone, even if it has to create one.
        // This must be done after special cases have been computed because it registers unregistered new objects.
        // First check the cache key.
        Object original = null;
        CacheKey cacheKey = null;
        if (!targetSession.isClientSession() && objectChangeSet != null) {
            cacheKey = objectChangeSet.getActiveCacheKey();
            if (cacheKey != null) {
                original =  cacheKey.getObject();
            }
        }
        ObjectBuilder builder = descriptor.getObjectBuilder();
        Object implementation = builder.unwrapObject(clone, unitOfWork);
        // If the cache key was missing check the cache.
        if (cacheKey == null) {
            cacheKey = targetSession.getCacheKeyFromTargetSessionForMerge(implementation, builder, descriptor, this);
            if (cacheKey != null){
                original = cacheKey.getObject();
            }
        }
        if (original == null  && !descriptor.getFullyMergeEntity()){
            original = unitOfWork.getOriginalVersionOfObjectOrNull(clone, objectChangeSet, descriptor, targetSession);
            //original was not in cache.  Make sure it is placed in the cache.
            if (original != null){
                if (cacheKey == null){
                    cacheKey = targetSession.getIdentityMapAccessorInstance().getWriteLockManager().appendLock(descriptor.getObjectBuilder().extractPrimaryKeyFromObject(clone, targetSession), original, descriptor, this, targetSession);
                }else{
                    if (cacheKey.getObject() != null){
                        original = cacheKey.getObject();
                    }else{
                        cacheKey.setObject(original);
                    }
                }
            }
        }
        // Always merge into the original.
        try {
            if (original == null || descriptor.getFullyMergeEntity()) {
                // If original does not exist then we must merge the entire object.
                boolean originalWasNull = false;
                if (original == null){
                    original = unitOfWork.buildOriginal(clone);
                    originalWasNull = true;
                }
                if (objectChangeSet == null) {
                    //no changeset so this would not have been locked as part of the 
                    cacheKey = targetSession.getIdentityMapAccessorInstance().getWriteLockManager().appendLock(descriptor.getObjectBuilder().extractPrimaryKeyFromObject(clone, targetSession), original, descriptor, this, targetSession);
                    if (cacheKey.getObject() != null){
                        original = cacheKey.getObject();
                    }else{
                        cacheKey.setObject(original);
                    }
                    objectBuilder.mergeIntoObject(original, true, clone, this, targetSession, false, !descriptor.getCopyPolicy().buildsNewInstance(), originalWasNull);
                } else{
                    cacheKey = targetSession.getIdentityMapAccessorInstance().getWriteLockManager().appendLock(objectChangeSet.getId(), original, descriptor, this, targetSession);
                    if (cacheKey.getObject() != null){
                        original = cacheKey.getObject();
                    }else{
                        cacheKey.setObject(original);
                    }
                    if (!objectChangeSet.isNew()) {
                        objectBuilder.mergeIntoObject(original, true, clone, this, targetSession, false, !descriptor.getCopyPolicy().buildsNewInstance(), false);
                    } else {
                        objectBuilder.mergeChangesIntoObject(original, objectChangeSet, clone, this, targetSession, !descriptor.getCopyPolicy().buildsNewInstance(), true);
                        // PERF: If PersistenceEntity is caching the primary key this must be cleared as the primary key may have changed in new objects.
                        objectBuilder.clearPrimaryKey(original);
                    }
                }
                updateCacheKeyProperties(unitOfWork, cacheKey, original, clone, objectChangeSet, descriptor);
            } else if (objectChangeSet == null) {
                // PERF: If we have no change set if it is existing, then no merging is required.
                // If it is new, then merge the object (normally a new object would have a change set, so this is an odd case.
                if (unitOfWork.isCloneNewObject(clone)) {
                    cacheKey = targetSession.getIdentityMapAccessorInstance().getWriteLockManager().appendLock(descriptor.getObjectBuilder().extractPrimaryKeyFromObject(clone, targetSession), original, descriptor, this, targetSession);
                    objectBuilder.mergeIntoObject(original, true, clone, this, targetSession, false, !descriptor.getCopyPolicy().buildsNewInstance(), true);
                    updateCacheKeyProperties(unitOfWork, cacheKey, original, clone, objectChangeSet, descriptor);
                }
            } else {
                // Regardless if the object is new, old, valid or invalid, merging will ensure there is a stub of an object in the 
                // shared cache for filling in foreign reference relationships. If merge did not occur in some cases (new  objects, garbage 
                // collection objects, object read in a transaction) then no object would be in the shared cache and foreign reference 
                // mappings would be set to null when they should be set to an object.
                if (objectChangeSet.hasChanges()) {
                    //only attempt to invalidate if we would have merged.  This saves us from a potential deadlock on get
                    //writeLockValue when we do not own the lock.
                    if (!objectChangeSet.isNew) {
                        if (objectChangeSet.shouldInvalidateObject(original, targetSession) && (!unitOfWork.isNestedUnitOfWork())) {
                            // Invalidate any object that was marked invalid during the change calculation, even if it was new as multiple flushes 
                            // and custom SQL could still produce invalid new objects.
                            targetSession.getIdentityMapAccessor().invalidateObject(original);
                            // no need to update cacheKey properties here
                        }
                    } else {
                        // PERF: If PersistenceEntity is caching the primary key this must be cleared as the primary key may have changed in new objects.
                        objectBuilder.clearPrimaryKey(original);
                    }
                    //if there are no changes then we just need a reference to the object so skip the merge
                    // saves trying to lock related objects after the fact producing deadlocks
                    objectBuilder.mergeChangesIntoObject(original, objectChangeSet, clone, this, targetSession, false, objectChangeSet.isNew());
                    updateCacheKeyProperties(unitOfWork, cacheKey, original, clone, objectChangeSet, descriptor);
                }
            }
        } catch (QueryException exception) {
            // Ignore validation errors if unit of work validation is suppressed.
            // Also there is a very specific case under EJB wrappering where
            // a related object may have never been accessed in the unit of work context
            // but is still valid, so this error must be ignored.
            if (unitOfWork.shouldPerformNoValidation() || (descriptor.hasWrapperPolicy())) {
                if ((exception.getErrorCode() != QueryException.BACKUP_CLONE_DELETED) && (exception.getErrorCode() != QueryException.BACKUP_CLONE_IS_ORIGINAL_FROM_PARENT) && (exception.getErrorCode() != QueryException.BACKUP_CLONE_IS_ORIGINAL_FROM_SELF)) {
                    throw exception;
                }
                return cacheKey;
            } else {
                throw exception;
            }
        }


        return cacheKey;
    }

    /**
     * Recursively merge changes in the object dependent on the merge policy.
     * This merges changes from a remote unit of work back from the server into
     * the original remote unit of work.  This is meant to merge server-side changes
     * such as sequence numbers, version numbers or events triggered changes.
     */
    public Object mergeChangesOfWorkingCopyIntoRemote(Object clone) throws ValidationException {
        UnitOfWorkImpl unitOfWork = (UnitOfWorkImpl)this.session;

        // This will return the object from the parent unit of work (original unit of work).
        Object original = unitOfWork.getOriginalVersionOfObject(clone);

        // The original is used as the backup to merge everything different from it.
        // This makes this type of merge quite different than the normal unit of work merge.
        ClassDescriptor descriptor = unitOfWork.getDescriptor(clone);
        descriptor.getObjectBuilder().mergeIntoObject(original, false, clone, this, this.session);

        if (((RemoteUnitOfWork)unitOfWork.getParent()).getUnregisteredNewObjectsCache().contains(original)) {
            // Can use a new instance as backup and original.
            Object backupClone = descriptor.getObjectBuilder().buildNewInstance();
            Object newInstance = descriptor.getObjectBuilder().buildNewInstance();
            ((UnitOfWorkImpl)unitOfWork.getParent()).registerOriginalNewObjectFromNestedUnitOfWork(original, backupClone, newInstance, descriptor);
        }

        Object primaryKey = descriptor.getObjectBuilder().extractPrimaryKeyFromObject(clone, unitOfWork);

        // Must ensure the get and put of the cache occur as a single operation.
        // Cache key hold a reference to a concurrency manager which is used for the lock/release operation
        CacheKey cacheKey = unitOfWork.getParent().getIdentityMapAccessorInstance().acquireLock(primaryKey, original.getClass(), descriptor);
        try {
            if (descriptor.usesOptimisticLocking()) {
                cacheKey.setObject(original);
                cacheKey.setWriteLockValue(unitOfWork.getIdentityMapAccessor().getWriteLockValue(original));
            } else {
                // Always put in the parent im for root because it must now be persistent.
                cacheKey.setObject(original);
            }
        } finally {
            cacheKey.updateAccess();
            cacheKey.release();
        }

        return clone;
    }

    /**
     * This can be used by the user for merging clones from RMI into the unit of work.
     */
    public void mergeCloneIntoWorkingCopy() {
        setMergePolicy(CLONE_INTO_WORKING_COPY);
    }

    /**
     * This is used during the merge of dependent objects referencing independent objects, where you want
     * the independent objects merged as well.
     */
    public void mergeCloneWithReferencesIntoWorkingCopy() {
        setMergePolicy(CLONE_WITH_REFS_INTO_WORKING_COPY);
    }

    /**
     * This is used during cache synchronization to merge the changes into the distributed cache.
     */
    public void mergeIntoDistributedCache() {
        setMergePolicy(CHANGES_INTO_DISTRIBUTED_CACHE);
    }

    /**
     * Merge a change set for a new object into the cache.  This method will create a
     * shell for the new object and then merge the changes from the change set into the object.
     * The newly merged object will then be added to the cache.
     */
    public Object mergeNewObjectIntoCache(ObjectChangeSet changeSet) {
        Class localClassType = changeSet.getClassType(session);
        ClassDescriptor descriptor = this.session.getDescriptor(localClassType);
        
        Object newObject = null;
        if (!isAlreadyMerged(changeSet, this.session)) {
            // if we haven't merged this object already then build a new object
            // otherwise leave it as null which will stop the recursion
            newObject = descriptor.getObjectBuilder().buildNewInstance();
            // store the changeset to prevent us from creating this new object again
            recordMerge(changeSet, newObject, this.session);
        } else {
            //we have all ready created the object, must be in a cyclic
            //merge on a new object so get it out of the alreadymerged collection
            newObject = this.objectsAlreadyMerged.get(this.session).get(changeSet);
        }
        mergeChanges(newObject, changeSet, this.session);
        return newObject;
    }

    /**
     * This is used to revert changes to objects, or during refreshes.
     */
    public void mergeOriginalIntoWorkingCopy() {
        setMergePolicy(ORIGINAL_INTO_WORKING_COPY);
    }

    /**
     * This is used during the unit of work commit to merge changes into the parent.
     */
    public void mergeWorkingCopyIntoOriginal() {
        setMergePolicy(WORKING_COPY_INTO_ORIGINAL);
    }

    /**
     * This is used during the unit of work commit to merge changes into the parent.
     */
    public void mergeWorkingCopyIntoRemote() {
        setMergePolicy(WORKING_COPY_INTO_REMOTE);
    }

    /**
     * INTERNAL:
     * This is used to refresh remote session object
     */
    public void refreshRemoteObject() {
        setMergePolicy(REFRESH_REMOTE_OBJECT);
    }

    /**
     * INTERNAL:
     * When merging from a clone when the cache cannot be guaranteed the object must be first read if it is existing
     * and not in the cache. Otherwise no changes will be detected as the original state is missing.
     */
    protected Object registerObjectForMergeCloneIntoWorkingCopy(Object clone) {
        UnitOfWorkImpl unitOfWork = (UnitOfWorkImpl)this.session;
        ClassDescriptor descriptor = unitOfWork.getDescriptor(clone.getClass());
        Object primaryKey = descriptor.getObjectBuilder().extractPrimaryKeyFromObject(clone, unitOfWork, true);

        // Must use the java class as this may be a bean that we are merging and it may not have the same class as the
        // objects in the cache.  As of EJB 2.0.
        Object objectFromCache = null;
        if (primaryKey != null) {
            objectFromCache = unitOfWork.getIdentityMapAccessorInstance().getFromIdentityMap(primaryKey, descriptor.getJavaClass(), false, descriptor);
        }
        if (objectFromCache == null) {
            // Ensure we return the working copy if this has already been registered.
            objectFromCache = unitOfWork.checkIfAlreadyRegistered(clone, descriptor);
        }
        if (objectFromCache != null) {
            // gf830 - merging a removed entity should throw exception.
            if (unitOfWork.isObjectDeleted(objectFromCache)) {
                if (shouldMergeCloneIntoWorkingCopy() || shouldMergeCloneWithReferencesIntoWorkingCopy()) {
                    throw new IllegalArgumentException(ExceptionLocalization.buildMessage("cannot_merge_removed_entity", new Object[] { clone }));
                }
            }            
            return objectFromCache;
        }

        DoesExistQuery existQuery = descriptor.getQueryManager().getDoesExistQuery();
        // Optimize cache option to avoid executing the does exist query.
        if (existQuery.shouldCheckCacheForDoesExist()) {
            checkNewObjectLockVersion(clone, primaryKey, descriptor, unitOfWork);
            Object registeredObject = unitOfWork.internalRegisterObject(clone, descriptor);            
            if (unitOfWork.hasNewObjects() && unitOfWork.getNewObjectsOriginalToClone().containsKey(clone)) {
                this.mergedNewObjects.put(registeredObject, registeredObject);
            }            
            return registeredObject;
        }

        // Check early return to check if it is a new object, i.e. null primary key.
        Boolean doesExist = Boolean.FALSE;
        if (primaryKey != null) {
            doesExist = (Boolean)existQuery.checkEarlyReturn(clone, primaryKey, unitOfWork, null);
        }
        if (doesExist == Boolean.FALSE) {
            checkNewObjectLockVersion(clone, primaryKey, descriptor, unitOfWork);
            Object registeredObject = unitOfWork.internalRegisterObject(clone, descriptor);//should use cloneAndRegisterNewObject to avoid the exist check
            this.mergedNewObjects.put(registeredObject, registeredObject);
            return registeredObject;
        }
    
        // Otherwise it is existing and not in the cache so it must be read.
        Object object = unitOfWork.readObject(clone);
        if (object == null) {
            checkNewObjectLockVersion(clone, primaryKey, descriptor, unitOfWork);
            //bug6180972: avoid internal register's existence check and be sure to put the new object in the mergedNewObjects collection
            object =  unitOfWork.cloneAndRegisterNewObject(clone);
            this.mergedNewObjects.put(object, object);
        }
        return object;
    }

    /**
     * Check if the new object's version has been set, if so, then it was an existing object that was deleted.
     * Raise an error instead of reincarnating the object.
     */
    public void checkNewObjectLockVersion(Object clone, Object primaryKey, ClassDescriptor descriptor, UnitOfWorkImpl unitOfWork) {
        //bug272704: throw an exception if this object is new yet has a version set to avoid merging in deleted objects
        if (descriptor.usesVersionLocking()){
            VersionLockingPolicy policy = (VersionLockingPolicy)descriptor.getOptimisticLockingPolicy();
            Object baseValue = policy.getBaseValue();
            Object objectLockValue = policy.getWriteLockValue(clone, primaryKey, unitOfWork);
            if (policy.isNewerVersion(objectLockValue, baseValue)) {
                throw OptimisticLockException.objectChangedSinceLastMerge(clone);
            }
        }
    }
    
    /**
     * Determine if the object is a registered new object, and that this is a nested unit of work
     * merge into the parent.  In this case private mappings will register the object as being removed.
     */
    public void registerRemovedNewObjectIfRequired(Object removedObject) {
        if (this.session.isUnitOfWork()) {
            UnitOfWorkImpl unitOfWork = (UnitOfWorkImpl)this.session;

            if (shouldMergeWorkingCopyIntoOriginal() && unitOfWork.getParent().isUnitOfWork() && unitOfWork.isCloneNewObject(removedObject)) {
                Object originalVersionOfRemovedObject = unitOfWork.getOriginalVersionOfObject(removedObject);
                unitOfWork.addRemovedObject(originalVersionOfRemovedObject);
            }
        }
    }

    public void setCascadePolicy(int cascadePolicy) {
        this.cascadePolicy = cascadePolicy;
    }

    protected void setMergePolicy(int mergePolicy) {
        this.mergePolicy = mergePolicy;
    }

    public void setForceCascade(boolean forceCascade) {
        this.forceCascade = forceCascade;
    }

    public void setObjectDescriptors(Map objectDescriptors) {
        this.objectDescriptors = objectDescriptors;
    }

    protected void setObjectsAlreadyMerged(Map objectsAlreadyMerged) {
        this.objectsAlreadyMerged = objectsAlreadyMerged;
    }

    /**
     * INTENRAL:
     * Used to set the node that this merge manager is stored in, within the WriteLocksManager write lockers queue
     */
    public void setQueueNode(LinkedNode node) {
        this.queueNode = node;
    }

    protected void setSession(AbstractSession session) {
        this.session = session;
    }

    /**
     * INTENRAL:
     * Used to set the object that the merge manager is waiting on, in order to acquire locks
     * If this value is null then the merge manager is not waiting on any locks.
     */
    public void setWriteLockQueued(Object primaryKey) {
        this.writeLockQueued = primaryKey;
    }

    /**
     * Flag used to determine that the mappings should be checked for
     * cascade requirements.
     */
    public boolean shouldCascadeByMapping() {
        return getCascadePolicy() == CASCADE_BY_MAPPING;
    }

    /**
     * Flag used to determine if all parts should be cascaded
     */
    public boolean shouldCascadeAllParts() {
        return getCascadePolicy() == CASCADE_ALL_PARTS;
    }

    /**
     * Flag used to determine if any parts should be cascaded
     */
    public boolean shouldCascadeParts() {
        return getCascadePolicy() != NO_CASCADE;
    }

    /**
     * Flag used to determine if any private parts should be cascaded
     */
    public boolean shouldCascadePrivateParts() {
        return (getCascadePolicy() == CASCADE_PRIVATE_PARTS) || (getCascadePolicy() == CASCADE_ALL_PARTS);
    }

    /**
     * Refreshes are based on the objects row, so all attributes of the object must be refreshed.
     * However merging from RMI, normally reference are made transient, so should not be merge unless
     * specified.
     */
    public boolean shouldCascadeReferences() {
        return !shouldMergeCloneIntoWorkingCopy();
    }

    /**
     * INTERNAL:
     * This happens when changes from an UnitOfWork is propagated to a distributed class.
     */
    public boolean shouldMergeChangesIntoDistributedCache() {
        return getMergePolicy() == CHANGES_INTO_DISTRIBUTED_CACHE;
    }

    /**
     * This can be used by the user for merging clones from RMI into the unit of work.
     */
    public boolean shouldMergeCloneIntoWorkingCopy() {
        return getMergePolicy() == CLONE_INTO_WORKING_COPY;
    }

    /**
     * This can be used by the user for merging remote EJB objects into the unit of work.
     */
    public boolean shouldMergeCloneWithReferencesIntoWorkingCopy() {
        return getMergePolicy() == CLONE_WITH_REFS_INTO_WORKING_COPY;
    }

    /**
     * This is used to revert changes to objects, or during refreshes.
     */
    public boolean shouldMergeOriginalIntoWorkingCopy() {
        return getMergePolicy() == ORIGINAL_INTO_WORKING_COPY;
    }

    /**
     * This is used during the unit of work commit to merge changes into the parent.
     */
    public boolean shouldMergeWorkingCopyIntoOriginal() {
        return getMergePolicy() == WORKING_COPY_INTO_ORIGINAL;
    }

    /**
     * INTERNAL:
     * This happens when serialized remote unit of work has to be merged with local remote unit of work.
     */
    public boolean shouldMergeWorkingCopyIntoRemote() {
        return getMergePolicy() == WORKING_COPY_INTO_REMOTE;
    }

    /**
     * INTERNAL:
     * This is used to refresh objects on the remote session
     */
    public boolean shouldRefreshRemoteObject() {
        return getMergePolicy() == REFRESH_REMOTE_OBJECT;
    }

    /**
     * This is used to cascade merge even if a clone is already registered. 
     */
    public boolean shouldForceCascade() {
        return forceCascade;
    }
    
    /**
     * INTERNAL:
     * Used to return a map containing new objects found through the 
     * registerObjectForMergeCloneIntoWorkingCopy method.
     * @return Map
     */
    public IdentityHashMap getMergedNewObjects(){
        return mergedNewObjects;
    }
    
    /**
     * INTERNAL:
     * Records that this merge manager has transitioned to use deferred locks during the merge.
     */
    public void transitionToDeferredLocks(){
        this.isTransitionedToDeferredLocks = true;
    }
    
    /**
     * INTERNAL:
     * Update CacheKey properties with new information.  This method is called if this code
     * actually merges
     */
    protected void updateCacheKeyProperties(UnitOfWorkImpl unitOfWork, CacheKey cacheKey, Object original, Object clone, ObjectChangeSet objectChangeSet, ClassDescriptor descriptor){
        if (!unitOfWork.isNestedUnitOfWork()) {
            boolean locked = false;
            // The cache key should never be null for the new commit, but may be for old commit, or depending on the cache isolation level may not be locked,
            // so needs to be re-acquired.
            if (cacheKey == null || !cacheKey.isAcquired()) {
                Object primaryKey = descriptor.getObjectBuilder().extractPrimaryKeyFromObject(original, unitOfWork);
                
                cacheKey = unitOfWork.getParent().getIdentityMapAccessorInstance().acquireLockNoWait(primaryKey, original.getClass(), true, descriptor);
                locked = cacheKey != null;
            }
            if (cacheKey != null){ // only work if we are locked.
                try {
                    if (descriptor.usesOptimisticLocking() && descriptor.getOptimisticLockingPolicy().isStoredInCache()) {
                        cacheKey.setWriteLockValue(unitOfWork.getIdentityMapAccessor().getWriteLockValue(clone));
                    }
                    cacheKey.setObject(original);
                    if (descriptor.getCacheInvalidationPolicy().shouldUpdateReadTimeOnUpdate() || ((objectChangeSet != null) && objectChangeSet.isNew())) {
                        cacheKey.setReadTime(getSystemTime());
                    }
                    cacheKey.updateAccess();
                } finally {
                    if (locked) {
                        cacheKey.release();
                    }
                }
            }
        }
    }

    /**
     * INTERNAL:
     * @return lockThread
     */
    public Thread getLockThread() {
        return lockThread;
    }

    /**
     * INTERNAL:
     * Save the currentThread for later comparison to the activeThread in case they don't match
     * @param lockThread
     */
    public void setLockThread(Thread lockThread) {
        this.lockThread = lockThread;
    }
}
