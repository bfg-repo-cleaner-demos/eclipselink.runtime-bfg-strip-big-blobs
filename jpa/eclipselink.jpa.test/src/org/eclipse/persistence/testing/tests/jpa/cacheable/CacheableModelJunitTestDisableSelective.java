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
 *     06/16/2009-2.0 Guy Pelletier 
 *       - 277039: JPA 2.0 Cache Usage Settings
 *     07/16/2009-2.0 Guy Pelletier 
 *       - 277039: JPA 2.0 Cache Usage Settings
 *     06/09/2010-2.0.3 Guy Pelletier 
 *       - 313401: shared-cache-mode defaults to NONE when the element value is unrecognized
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.jpa.cacheable;

import java.util.HashMap;
import java.util.List;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import junit.framework.*;

import org.eclipse.persistence.config.CacheUsage;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.ObjectReferenceMapping;
import org.eclipse.persistence.sessions.server.ServerSession;
import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
import org.eclipse.persistence.testing.models.jpa.cacheable.CacheableTableCreator;
import org.eclipse.persistence.testing.models.jpa.cacheable.CacheableTrueEntity;
import org.eclipse.persistence.testing.models.jpa.cacheable.CacheableFalseDetail;
import org.eclipse.persistence.testing.models.jpa.cacheable.CacheableFalseEntity;
import org.eclipse.persistence.testing.models.jpa.cacheable.CacheableProtectedEntity;
import org.eclipse.persistence.testing.models.jpa.cacheable.CacheableForceProtectedEntity;
import org.eclipse.persistence.testing.models.jpa.cacheable.ChildCacheableFalseEntity;
import org.eclipse.persistence.testing.models.jpa.cacheable.SubCacheableFalseEntity;
import org.eclipse.persistence.testing.models.jpa.cacheable.SubCacheableNoneEntity;
import org.eclipse.persistence.testing.models.jpa.cacheable.ForceProtectedEntityWithComposite;
import org.eclipse.persistence.testing.models.jpa.cacheable.ProtectedEmbeddable;
import org.eclipse.persistence.testing.models.jpa.cacheable.ProtectedRelationshipsEntity;
import org.eclipse.persistence.testing.models.jpa.cacheable.SharedEmbeddable;
import org.eclipse.persistence.testing.models.jpa.cacheable.CacheableRelationshipsEntity;
 
/*
 * The test is testing against "MulitPU-4" persistence unit which has <shared-cache-mode> to be DISABLE_SELECTIVE
 */
public class CacheableModelJunitTestDisableSelective extends JUnitTestCase {
    private static int m_cacheableTrueEntity1Id;
    private static int m_cacheableTrueEntity2Id;
    private static int m_childCacheableFalseEntityId;
    private static int m_cacheableForceProtectedEntity1Id;
    private static int m_cacheableProtectedEntityId;
    private static int m_forcedProtectedEntityCompositId;
    private static int m_cacheableRelationshipsEntityId;
    
    public CacheableModelJunitTestDisableSelective() {
        super();
    }
    
    public CacheableModelJunitTestDisableSelective(String name) {
        super(name);
        setPuName("MulitPU-4");
    }
    
    /**
     * Convenience method. 
     */
    public void clearDSCache() {
        super.clearCache("MulitPU-4");
    }
    
    /**
     * Convenience method.
     */
    public void closeEM(EntityManager em) {
        if (em.isOpen()) {
            closeEntityManager(em);
        }
    }
    
    /**
     * Convenience method.
     */
    public EntityManager createDSEntityManager() {
        return super.createEntityManager("MulitPU-4");
    }
    
    /**
     * Convenience method - Executes a straight up find on the EM provided or
     * a new one.
     */
    protected CacheableTrueEntity findCacheableTrueEntity(EntityManager em, int id) {
        if (em == null) {
            EntityManager myEm = createDSEntityManager();
            CacheableTrueEntity entity = myEm.find(CacheableTrueEntity.class, id);
            closeEM(myEm);
            return entity;
        } else {
            return em.find(CacheableTrueEntity.class, id);
        }
    }
    
    /**
     * Convenience method - Executes the select query using retrieve BYPASS
     * and store BYPASS on a new entity manager or the one provided.
     */
    protected CacheableTrueEntity findCacheableTrueEntity_BYPASS_BYPASS(EntityManager em, int id) {
        return findCacheableTrueEntityUsingQuery(em, "findCacheableTrueEntityByPK_BYPASS_BYPASS", id);
    }
    
    /**
     * Convenience method - Executes the select query using retrieve BYPASS
     * and store USE on a new entity manager or the one provided.
     */
    protected CacheableTrueEntity findCacheableTrueEntity_BYPASS_USE(EntityManager em, int id) {
        return findCacheableTrueEntityUsingQuery(em, "findCacheableTrueEntityByPK_RETRIEVE_BYPASS_STORE_USE", id);
    }
    
    /**
     * Convenience method - Executes the select query using retrieve USE
     * and store BYPASS on a new entity manager or the one provided.
     */
    protected CacheableTrueEntity findCacheableTrueEntity_USE_BYPASS(EntityManager em, int id) {
        return findCacheableTrueEntityUsingQuery(em, "findCacheableTrueEntityByPK_RETRIEVE_USE_STORE_BYPASS", id);
    }
    
    /**
     * Convenience method - Executes the select query on a new entity manager 
     * or the one provided.
     */
    protected CacheableTrueEntity findCacheableTrueEntityUsingQuery(EntityManager em, String query, int id) {
        CacheableTrueEntity entity = null;
        
        EntityManager emToUse;
        if (em == null) {
            // Create a new EM ...
            emToUse = createDSEntityManager();
        } else {
            // Use the em provided but do not close it.
            emToUse = em;
        }
        
        try {
            beginTransaction(emToUse);
            entity = (CacheableTrueEntity) emToUse.createNamedQuery(query).setParameter("id", id).getSingleResult();
            commitTransaction(emToUse);
        }  catch (Exception e) {
            fail("Error executing query: " + e);
        } finally {
            if (em == null) {
                closeEM(emToUse);
            }
        }
        
        return entity;
    }
    
    /**
     * Convenience method.
     */
    public ServerSession getDSServerSession() {
        return getPUServerSession("MulitPU-4");
    }
    
    /**
     * Convenience method.
     */
    public ServerSession getPUServerSession(String puName) {
        return JUnitTestCase.getServerSession(puName);
    }
    
    public void setUp() {
        super.setUp();
        clearDSCache();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName("CacheableModelJunitTestDisableSelective");

        if (! JUnitTestCase.isJPA10()) {
            suite.addTest(new CacheableModelJunitTestDisableSelective("testSetup"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testCachingOnDISABLE_SELECTIVE"));
            
            // Test cache retrieve mode of BYPASS and USE through the EM.
            suite.addTest(new CacheableModelJunitTestDisableSelective("testCreateEntities"));
            
            suite.addTest(new CacheableModelJunitTestDisableSelective("testFindWithEMProperties"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testFindWithFindProperties"));
            
            suite.addTest(new CacheableModelJunitTestDisableSelective("testRefreshWithEMProperties"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testRefreshWithRefreshProperties"));
            
            // Test various usage scenarios ..
            suite.addTest(new CacheableModelJunitTestDisableSelective("testRetrieveBYPASSStoreUSE1"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testRetrieveBYPASSStoreUSE2"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testRetrieveUSEStoreBYPASS1"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testRetrieveUSEStoreBYPASS2"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testRetrieveBYPASSStoreBYPASS1"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testRetrieveBYPASSStoreBYPASS2"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testMultipleEMQueries"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testEMPropertiesOnCommit1"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testEMPropertiesOnCommit2"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testInheritanceCacheable"));

            suite.addTest(new CacheableModelJunitTestDisableSelective("testLoadMixedCacheTree"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testIsolatedIsolation"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testProtectedIsolation"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testProtectedCaching"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testReadOnlyTree"));
            
            suite.addTest(new CacheableModelJunitTestDisableSelective("testUpdateForceProtectedBasic"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testUpdateForceProtectedOneToOne"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testUpdateProtectedBasic"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testUpdateProtectedOneToMany"));

            suite.addTest(new CacheableModelJunitTestDisableSelective("testProtectedRelationshipsMetadata"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testForceProtectedFromEmbeddable"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testEmbeddableProtectedCaching"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testUpdateProtectedManyToOne"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testUpdateProtectedManyToMany"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testUpdateProtectedElementCollection"));
            suite.addTest(new CacheableModelJunitTestDisableSelective("testIsolationBeforeEarlyTxBegin"));
        }
        return suite;
    }
    
    /**
     * The setup is done as a test, both to record its failure, and to allow execution in the server.
     */
    public void testSetup() {
        new CacheableTableCreator().replaceTables(JUnitTestCase.getServerSession("MulitPU-4"));
        clearDSCache();
    }
    
    /**
     * Test EM properties on commit.  
     */
    public void testEMPropertiesOnCommit1() {
        EntityManager em1 = createDSEntityManager();
        
        // Find the entity and put it in the cache ..
        CacheableTrueEntity cachedEntity = findCacheableTrueEntity(em1, m_cacheableTrueEntity1Id);
        String staleName = cachedEntity.getName();
        // Create  a new Entity to delete
        beginTransaction(em1);
        CacheableTrueEntity entityToDelete = new CacheableTrueEntity();
        entityToDelete.setName("entityToDelete");
        em1.persist(entityToDelete);
        int entityToDeleteId = entityToDelete.getId();
        commitTransaction(em1);
        
        // No need to clear the EM, just set the new property to BYPASS.
        em1.setProperty(QueryHints.CACHE_STORE_MODE, CacheStoreMode.BYPASS);
        beginTransaction(em1);
        CacheableTrueEntity entity = findCacheableTrueEntity(em1, m_cacheableTrueEntity1Id);
        String updatedName = "testEMPropertiesOnCommit1";
        entity.setName(updatedName);
        CacheableTrueEntity deletedEntity1 = findCacheableTrueEntity(em1, entityToDeleteId);
        em1.remove(deletedEntity1);
        commitTransaction(em1);
        closeEM(em1);
        
        EntityManager em2 = createDSEntityManager();
        CacheableTrueEntity entity2 = findCacheableTrueEntity_USE_BYPASS(em2, m_cacheableTrueEntity1Id);
        assertTrue("The shared cache was updated when the EM property CacheStoreMode = BYPASS", entity2.getName().equals(staleName));
        
        em2.refresh(entity2);
        assertTrue("The entity was not refreshed with the updated name.", entity2.getName().equals(updatedName));
        
        HashMap props = new HashMap();
        props.put(QueryHints.CACHE_RETRIEVE_MODE, CacheRetrieveMode.USE);
        props.put(QueryHints.CACHE_STORE_MODE, CacheStoreMode.BYPASS);
        CacheableTrueEntity deletedEntity2 = (CacheableTrueEntity) em2.find(CacheableTrueEntity.class, entityToDeleteId, props);
        assertTrue("The deleted entity was removed from the cache", deletedEntity2 == null);
        
        deletedEntity2 = em2.find(CacheableTrueEntity.class, entityToDeleteId);
        assertTrue("The deleted entity was removed from the database", deletedEntity2 == null);
        
        closeEM(em2);
    }
    
    /**
     * Test EM properties on commit.  
     */
    public void testEMPropertiesOnCommit2() {
        EntityManager em1 = createDSEntityManager();
        
        // Find the entities and put them in the shared cache ...
        CacheableTrueEntity cachedEntity1 = findCacheableTrueEntity(em1, m_cacheableTrueEntity1Id);
        CacheableTrueEntity cachedEntity2 = findCacheableTrueEntity(em1, m_cacheableTrueEntity2Id);
        String staleName = cachedEntity2.getName();
        
        // No need to clear the EM, just set the new property to BYPASS.
        em1.setProperty(QueryHints.CACHE_STORE_MODE, CacheStoreMode.BYPASS);
        String updatedName = "testEMPropertiesOnCommit2";
        
        beginTransaction(em1);
        // Update entity1 through a query that uses cache store mode USE.
        Query query = em1.createQuery("UPDATE JPA_CACHEABLE_TRUE e SET e.name = :name " + "WHERE e.id = :id ").setParameter("name", updatedName).setParameter("id", m_cacheableTrueEntity1Id);
        query.setHint(QueryHints.CACHE_STORE_MODE, CacheStoreMode.USE);
        query.executeUpdate();
        
        // Update entity2 manually.
        CacheableTrueEntity entity2 = findCacheableTrueEntity(em1, m_cacheableTrueEntity2Id);
        entity2.setName(updatedName);
        commitTransaction(em1);
        closeEM(em1);
        
        // Verify the cache in a separate entity manager.
        EntityManager em2 = createDSEntityManager();
        CacheableTrueEntity entity21 = findCacheableTrueEntity_USE_BYPASS(em2, m_cacheableTrueEntity1Id);
        assertTrue("The shared cache should have been updated", entity21.getName().equals(updatedName));
        
        CacheableTrueEntity entity22 = findCacheableTrueEntity_USE_BYPASS(em2, m_cacheableTrueEntity2Id);
        assertTrue("The shared cache should NOT have been updated", entity22.getName().equals(staleName));
        
        em2.refresh(entity22);
        assertTrue("The entity was not refreshed with the updated name.", entity22.getName().equals(updatedName));
        
        closeEM(em2);
    }
    
    /**
     * Test find using entity manager properties  
     */
    public void testFindWithEMProperties() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) { 
            EntityManager em = createDSEntityManager();
            
            // Put the entity in the UOW and shared cache.
            CacheableTrueEntity cachedEntity = findCacheableTrueEntity(em, m_cacheableTrueEntity1Id);
            
            // Update the entity name in the shared cash through a different EM.
            updateCacheableTrueEntityNameInSharedCache("testCacheRetrieveModeBypassOnFindThroughEMProperties");
            
            // This should pick up the entity from the shared cache
            EntityManager em2 = createDSEntityManager();
            CacheableTrueEntity cachedEntity2 = em2.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id);
            assertTrue("The shared cache was not updated.", cachedEntity2.getName().equals("testCacheRetrieveModeBypassOnFindThroughEMProperties"));
            closeEM(em2);
            
            // This setting should be ignored on a refresh operation ...
            em.setProperty(QueryHints.CACHE_RETRIEVE_MODE, CacheRetrieveMode.USE);
            
            // Set the refresh property.
            em.setProperty(QueryHints.CACHE_STORE_MODE, CacheStoreMode.REFRESH);
            
            // Re-issue the find on the original EM.
            CacheableTrueEntity entity = em.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id);
            assertTrue("CacheableTrueEntity should have been refreshed.", entity.getName().equals("testCacheRetrieveModeBypassOnFindThroughEMProperties"));
            assertTrue("CacheableTrueEntity from UOW should have been refreshed.", cachedEntity.getName().equals(entity.getName()));
            assertTrue("Entity returned should be the same instance from the UOW cache", cachedEntity == entity);
            
            closeEM(em);
        }
    }
    
    /**
     * Test find using find properties  
     */
    public void testFindWithFindProperties() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) {
            EntityManager em = createDSEntityManager();
            
            // Put the entity in the UOW and shared cache.
            CacheableTrueEntity cachedEntity = em.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id);
            
            // Update the entity name, but BYPASS updating the shared cache through a different EM.
            updateCacheableTrueEntityNameAndBypassStore("testCacheRetrieveModeBypassOnFindThroughFindProperties");
            
            // This should pick up the entity from the shared cache (which should not of been updated
            EntityManager em2 = createDSEntityManager();
            CacheableTrueEntity cachedEntity2 = em2.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id);
            assertFalse("The shared cache was updated.", cachedEntity2.getName().equals("testCacheRetrieveModeBypassOnFindThroughFindProperties"));
            closeEM(em2);
            
            // This setting should be ignored on a refresh operation ...
            em.setProperty(QueryHints.CACHE_RETRIEVE_MODE, CacheRetrieveMode.USE);
            
            // Set the refresh property.
            HashMap properties = new HashMap();
            properties.put(QueryHints.CACHE_STORE_MODE, CacheStoreMode.REFRESH);
            
            // Re-issue the find on the original EM.
            CacheableTrueEntity entity = (CacheableTrueEntity) em.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id, properties);
            assertTrue("CacheableTrueEntity should have been refreshed.", entity.getName().equals("testCacheRetrieveModeBypassOnFindThroughFindProperties"));
            assertTrue("CacheableTrueEntity from UOW should have been refreshed.", cachedEntity.getName().equals(entity.getName()));
            assertTrue("Entity returned should be the same instance from the UOW cache", cachedEntity == entity);
            
            closeEM(em);
        }
    }
    
    /**
     * Test refresh using EM properties 
     */
    public void testRefreshWithEMProperties() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) {
            // This will put the entity in the cache.
            EntityManager em = createDSEntityManager();
            CacheableTrueEntity cachedEntity = em.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id);
            // Update the database manually through a different EM
            String updatedName = "testRefreshWithEMProperties";
            updateCacheableTrueEntityNameInSharedCache(updatedName);
           
            // This setting should be ignored on a refresh operation ...
            em.setProperty(QueryHints.CACHE_RETRIEVE_MODE, CacheRetrieveMode.USE);
            beginTransaction(em);
            try{
                cachedEntity = em.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id);
                em.refresh(cachedEntity);
                commitTransaction(em);
            }catch(Exception ex){
                ex.printStackTrace();
                if (isTransactionActive(em)) {
                    rollbackTransaction(em);
                }
            }finally{
                closeEM(em);
            }
            assertTrue("CacheableTrueEntity should have been refreshed.", cachedEntity.getName().equals(updatedName));
        }
        
    }
    
    /**
     * Test refresh using refresh properties. 
     */
    public void testRefreshWithRefreshProperties() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) {
            // This will put the entity in the cache.
            EntityManager em = createDSEntityManager();
            CacheableTrueEntity cachedEntity = em.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id);
            
            // Update the database manually through a different EM
            String updatedName = "testRefreshWithRefreshProperties";
            updateCacheableTrueEntityNameInSharedCache(updatedName);
            
            HashMap properties = new HashMap();
            // This setting should be ignored on a refresh operation ...
            properties.put(QueryHints.CACHE_RETRIEVE_MODE, CacheRetrieveMode.USE);
            beginTransaction(em);
            try{
                cachedEntity = em.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id);
                em.refresh(cachedEntity, properties);
                commitTransaction(em);
            }catch(Exception ex){ 
                if (isTransactionActive(em)) {
                    rollbackTransaction(em);
                }
            }finally{
                closeEM(em);
            }
            assertTrue("CacheableTrueEntity should have been refreshed.", cachedEntity.getName().equals(updatedName));
        }
    }
    
    /**
     * Test: Using named query (when updated object in shared cache)
     * CacheRetrieveMode = BYPASS
     * CacheStoreMode = BYPASS
     */
    public void testRetrieveBYPASSStoreBYPASS1() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) { 
            // Put the entity in the EM UOW and shared cache.
            EntityManager em = createDSEntityManager();
            CacheableTrueEntity cachedEntity = findCacheableTrueEntity(em, m_cacheableTrueEntity1Id);
            
            // Update the entity name in the shared cache through a different EM.
            String updatedName = "testRetrieveBYPASSStoreBYPASS1";
            updateCacheableTrueEntityNameInSharedCache(updatedName);
            
            // Execute find by pk query using Retrieve BYPASS, Store BYPASS on EM.
            // It should return the same entity.
            CacheableTrueEntity entity1 = findCacheableTrueEntity_BYPASS_BYPASS(em, m_cacheableTrueEntity1Id);
            assertTrue("The entity instances must be the same", entity1 == cachedEntity);
            assertTrue("The name should not of been refreshed", entity1.getName().equals(cachedEntity.getName()));
                
            // Issue a find using refresh on EM, should pick up the updated name.
            HashMap properties = new HashMap();
            properties.put(QueryHints.CACHE_STORE_MODE, CacheStoreMode.REFRESH);
            CacheableTrueEntity entity1b = (CacheableTrueEntity) em.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id, properties);
            assertTrue("CacheableTrueEntity should of been refreshed.", entity1b.getName().equals(updatedName));
            closeEM(em);
            
            // On a different EM execute a find by pk using Retrieve BYPASS, 
            // Store BYPASS, we should get the updated name.
            CacheableTrueEntity entity2 = findCacheableTrueEntity_BYPASS_BYPASS(null, m_cacheableTrueEntity1Id);
            assertTrue("CacheableTrueEntity should have been refreshed.", entity2.getName().equals(updatedName));
        }
    }
    
    /**
     * Test: Using named query  (when updated object NOT in shared cache)
     * CacheRetrieveMode = BYPASS
     * CacheStoreMode = BYPASS
     */
    public void testRetrieveBYPASSStoreBYPASS2() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) {
            // Put the entity in the EM UOW and shared cache.
            EntityManager em = createDSEntityManager();
            CacheableTrueEntity cachedEntity = findCacheableTrueEntity(em, m_cacheableTrueEntity1Id);
            
            // Update the entity name in the shared cache through a different EM.
            String updatedName = "testRetrieveBYPASSStoreBYPASS2";
            updateCacheableTrueEntityNameAndBypassStore(updatedName);
            
            // Execute find by pk query using Retrieve BYPASS, Store BYPASS on EM.
            // It should return the same entity.
            CacheableTrueEntity entity1a = findCacheableTrueEntity_BYPASS_BYPASS(em, m_cacheableTrueEntity1Id);
            assertTrue("The entity returned should match the cached instance", entity1a == cachedEntity);
            assertTrue("CacheableTrueEntity should have been refreshed.", entity1a.getName().equals(cachedEntity.getName()));
                
            // On a different EM issue a find (using internal EclipseLink defaults)
            CacheableTrueEntity entity2 = findCacheableTrueEntity(null, m_cacheableTrueEntity1Id);
            assertTrue("CacheableTrueEntity should not of been refreshed.", entity2.getName().equals(entity1a.getName()));
                
            // Issue a find on EM1 using REFRESH.
            HashMap properties = new HashMap();
            properties.put(QueryHints.CACHE_STORE_MODE, CacheStoreMode.REFRESH);
            CacheableTrueEntity entity1b = (CacheableTrueEntity) em.find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id, properties);
            assertTrue("CacheableTrueEntity should be from the shared cache.", entity1b.getName().equals(updatedName));
            
            closeEM(em);
        }
    }
    
    /**
     * Test: Named query using retrieve BYPASS and store USE (when updated object in shared cache).
     */
    public void testRetrieveBYPASSStoreUSE1() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) {
            // Put the entity in the EM UOW and shared cache.
            EntityManager em = createDSEntityManager();
            CacheableTrueEntity cachedEntity = findCacheableTrueEntity(em, m_cacheableTrueEntity1Id);
            
            // Update the entity name in the shared cache through a different EM.
            String updatedName = "testRetrieveBYPASSStoreUSE1";
            updateCacheableTrueEntityNameInSharedCache(updatedName);
            
            // Execute find by pk query using Retrieve BYPASS, Store USE on EM.
            // It should return the same entity.
            CacheableTrueEntity entity1 = findCacheableTrueEntity_BYPASS_USE(em, m_cacheableTrueEntity1Id);
            assertTrue("The entity instances must be the same", entity1 == cachedEntity);
            assertTrue("The name should not of been refreshed", entity1.getName().equals(cachedEntity.getName()));
                
            // Execute find by pk query using Retrieve USE, Store BYPASS on a
            // different EM. The entity returned should have been read from the 
            // shared cache with the updated name.
            CacheableTrueEntity entity2 = findCacheableTrueEntity_USE_BYPASS(null, m_cacheableTrueEntity1Id);
            assertTrue("CacheableTrueEntity should have the name from the shared cache.", entity2.getName().equals(updatedName));
                
            closeEM(em);
        }
    }
    
    /**
     * Test: Using a named query with BYPASS and USE (when updated object not in shared cache)
     */
    public void testRetrieveBYPASSStoreUSE2() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) {
            // Put the entity in the EM UOW and shared cache.
            EntityManager em = createDSEntityManager();
            CacheableTrueEntity cachedEntity = findCacheableTrueEntity(em, m_cacheableTrueEntity1Id);
            
            // Update the database manually through a different EM and bypass
            // updating the shared cache.
            String updatedName = "testRetrieveBYPASSStoreUSE2";
            updateCacheableTrueEntityNameAndBypassStore(updatedName);
            
            // Execute find by pk query using Retrieve BYPASS, Store USE on EM.
            // It should return the same entity. As an optimization in this case,
            // the Store USE will update the shared cache.
            CacheableTrueEntity entity1 = findCacheableTrueEntity_BYPASS_USE(em, m_cacheableTrueEntity1Id);
            assertTrue("The entity instances must be the same", entity1 == cachedEntity);
            assertTrue("The name should not of been refreshed", entity1.getName().equals(cachedEntity.getName()));
            closeEM(em);
            
            // A find on a new EM should return the value from the shared cache 
            // which should have been updated with the Store USE value above
            CacheableTrueEntity entity2 = findCacheableTrueEntity(null, m_cacheableTrueEntity1Id);
            assertTrue("CacheableTrueEntity should have the name from the shared cache.", entity2.getName().equals(updatedName));
        }
    }
    
    /**
     * Test: Named query using retrieve USE and store BYPASS (when updated object in shared cache).
     */
    public void testRetrieveUSEStoreBYPASS1() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) {
            // Put the entity in the UOW and shared cache for EM1
            EntityManager em = createDSEntityManager();
            CacheableTrueEntity cachedEntity = findCacheableTrueEntity(em, m_cacheableTrueEntity1Id);
            
            // Update the entity name in the shared cash through a different EM.
            String updatedName = "testRetrieveUSEStoreBYPASS1";
            updateCacheableTrueEntityNameInSharedCache(updatedName);
            
            // Execute find by pk query using Retrieve USE, Store BYPASS on EM.
            // It should return the same entity.
            CacheableTrueEntity entity1 = findCacheableTrueEntity_USE_BYPASS(em, m_cacheableTrueEntity1Id);
            assertTrue("The entity returned should match the cached instance", entity1 == cachedEntity);
            assertTrue("CacheableTrueEntity should not have been refreshed.", entity1.getName().equals(cachedEntity.getName()));
            closeEM(em);
            
            // Execute a find by pk query using Retrieve USE, Store BYPASS on a 
            // different EM. The entity returned should have been read from the 
            // shared cache with the updated name.
            CacheableTrueEntity entity2 = findCacheableTrueEntity_USE_BYPASS(null, m_cacheableTrueEntity1Id);
            assertTrue("CacheableTrueEntity should have the name from the shared cache.", entity2.getName().equals(updatedName));
        }
    }
    
    /**
     * Test: Named query using retrieve USE and store BYPASS (when updated object NOT in shared cache).
     */
    public void testRetrieveUSEStoreBYPASS2() {
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) {
            // Put the entity in the UOW and shared cache for EM1
            EntityManager em1 = createDSEntityManager();
            CacheableTrueEntity cachedEntity = findCacheableTrueEntity(em1, m_cacheableTrueEntity1Id);
            
            // Update the database manually through a different EM and bypass
            // updating the shared cache.
            String updatedName = "testRetrieveUSEStoreBYPASS2";
            updateCacheableTrueEntityNameAndBypassStore(updatedName);
            
            // Execute find by pk query using Retrieve USE, Store BYPASS on EM.
            // It should return the same entity.
            CacheableTrueEntity entity1 = findCacheableTrueEntity_USE_BYPASS(em1, m_cacheableTrueEntity1Id);
            assertTrue("The entity returned should match the cached instance", entity1 == cachedEntity);
            assertTrue("CacheableTrueEntity should not have been refreshed.", entity1.getName().equals(cachedEntity.getName()));
            closeEM(em1);
            
            // Issue a find on a different EM. The entity should come from the
            // shared cache and have a stale name.
            EntityManager em2 = createDSEntityManager();
            CacheableTrueEntity entity2 = findCacheableTrueEntity(em2, m_cacheableTrueEntity1Id);
            assertTrue("CacheableTrueEntity should have the name from the shared cache.", entity2.getName().equals(entity1.getName()));
                
            // Now refresh the entity, should get the updated name.
            em2.refresh(entity2);
            assertTrue("CacheableTrueEntity should have the name from database.", entity2.getName().equals(updatedName));
            closeEM(em2);
        }
    }
   
    /**
     * Test EM properties on commit.  
     */
    public void testInheritanceCacheable() {
        EntityManager em1 = createDSEntityManager();
        
        beginTransaction(em1);
        CacheableTrueEntity cacheableEntity1 = new CacheableTrueEntity();
        cacheableEntity1.setName("cacheableEntity");
        em1.persist(cacheableEntity1);
        ChildCacheableFalseEntity nonCacheableEntity1 = new ChildCacheableFalseEntity();
        nonCacheableEntity1.setName("nonCacheableEntity");
        em1.persist(nonCacheableEntity1);
        commitTransaction(em1);
        
        closeEM(em1);
        
        EntityManager em2 = createDSEntityManager();
        HashMap props = new HashMap();
        props.put(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheOnly);
        
        CacheableTrueEntity cacheableEntity2 = (CacheableTrueEntity) em2.find(CacheableTrueEntity.class, cacheableEntity1.getId(), props);
        CacheableTrueEntity nonCacheableEntity2a = (CacheableTrueEntity) em2.find(CacheableTrueEntity.class, nonCacheableEntity1.getId(), props);
        ChildCacheableFalseEntity nonCacheableEntity2b = (ChildCacheableFalseEntity) em2.find(ChildCacheableFalseEntity.class, nonCacheableEntity1.getId(), props);
        
        assertFalse("CacheableTrueEntity was not in the cache", cacheableEntity2 == null);
        assertTrue("ChildCacheableFalseEntity was in the cache", nonCacheableEntity2a == null);
        assertTrue("ChildCacheableFalseEntity was in the cache", nonCacheableEntity2b == null);

        closeEM(em2);
    }
    
    public void testMultipleEMQueries() {
        // Get the object in the shared cache.
        CacheableTrueEntity cachedEntity = createDSEntityManager().find(CacheableTrueEntity.class, m_cacheableTrueEntity1Id);
        
        // Update the database manually through a different EM and bypass
        // updating the shared cache.
        String updatedName = "testMultipleEMQueries";
        updateCacheableTrueEntityNameAndBypassStore(updatedName);
        
        // Execute find by pk query using Retrieve USE, Store BYPASS. The entity 
        // returned should have been read from the shared cache with the stale name.
        CacheableTrueEntity entity1 = findCacheableTrueEntity_USE_BYPASS(null, m_cacheableTrueEntity1Id);
        assertFalse("CacheableTrueEntity should not have the updated name.", entity1.getName().equals(updatedName));
            
        // Execute find by pk query using Retrieve BYPASS, Store USE. The entity 
        // returned should have been read from the database and the shared cache 
        // should have been updated.
        CacheableTrueEntity entity2 = findCacheableTrueEntity_BYPASS_USE(null, m_cacheableTrueEntity1Id);
        assertTrue("CacheableTrueEntity should have the updated name.", entity2.getName().equals(updatedName));
            
        // Execute find by pk query using Retrieve USE, Store BYPASS. The entity 
        // returned should have been read from the shared cache with the updated name.
        CacheableTrueEntity entity3 = findCacheableTrueEntity_USE_BYPASS(null, m_cacheableTrueEntity1Id);
        assertTrue("CacheableTrueEntity should have the updated name.", entity3.getName().equals(updatedName));
    }
    
    /**
     * Verifies the cacheable settings when caching (from persistence.xml) is set to DISABLE_SELECTIVE.
     */
    public void testCachingOnDISABLE_SELECTIVE() {
        ServerSession session = getPUServerSession("MulitPU-4");
        ClassDescriptor falseEntityDescriptor = session.getDescriptorForAlias("JPA_CACHEABLE_FALSE");
        assertTrue("CacheableFalseEntity (DISABLE_SELECTIVE) from annotations has caching turned on", usesNoCache(falseEntityDescriptor));
        
        ClassDescriptor trueEntityDescriptor = session.getDescriptorForAlias("JPA_CACHEABLE_TRUE");
        assertFalse("CacheableTrueEntity (DISABLE_SELECTIVE) from annotations has caching turned off", usesNoCache(trueEntityDescriptor));
 
        ClassDescriptor childFalseEntityDescriptor = session.getDescriptorForAlias("JPA_CHILD_CACHEABLE_FALSE");
        assertTrue("ChildCacheableFalseEntity (DISABLE_SELECTIVE) from annotations has caching turned on", usesNoCache(childFalseEntityDescriptor));
        
        ClassDescriptor falseSubEntityDescriptor = session.getDescriptorForAlias("JPA_SUB_CACHEABLE_FALSE");
        assertTrue("SubCacheableFalseEntity (DISABLE_SELECTIVE) from annotations has caching turned on", usesNoCache(falseSubEntityDescriptor));
    
        // Should pick up true from the mapped superclass.
        ClassDescriptor noneSubEntityDescriptor = session.getDescriptorForAlias("JPA_SUB_CACHEABLE_NONE");
        assertFalse("SubCacheableNoneEntity (DISABLE_SELECTIVE) from annotations has caching turned off", usesNoCache(noneSubEntityDescriptor));

        ClassDescriptor xmlFalseEntityDescriptor = session.getDescriptorForAlias("XML_CACHEABLE_FALSE");
        assertTrue("CacheableFalseEntity (DISABLE_SELECTIVE) from XML has caching turned on", usesNoCache(xmlFalseEntityDescriptor));
        
        ClassDescriptor xmlTrueEntityDescriptor = session.getDescriptorForAlias("XML_CACHEABLE_TRUE");
        assertFalse("CacheableTrueEntity (DISABLE_SELECTIVE) from XML has caching turned off", usesNoCache(xmlTrueEntityDescriptor));
        
        ClassDescriptor xmlFalseSubEntityDescriptor = session.getDescriptorForAlias("XML_SUB_CACHEABLE_FALSE");
        assertTrue("SubCacheableFalseEntity (DISABLE_SELECTIVE) from XML has caching turned on", usesNoCache(xmlFalseSubEntityDescriptor));
    
        // Should pick up true from the mapped superclass.
        ClassDescriptor xmlNoneSubEntityDescriptor = session.getDescriptorForAlias("XML_SUB_CACHEABLE_NONE");
        assertFalse("SubCacheableNoneEntity (DISABLE_SELECTIVE) from XML has caching turned off", usesNoCache(xmlNoneSubEntityDescriptor));
    }
    
    public void testCreateEntities() {
        EntityManager em = null;
        
        try {
            em = createEntityManager("MulitPU-4");
            beginTransaction(em);
            
            CacheableTrueEntity cacheableTrueEntity = new CacheableTrueEntity();
            cacheableTrueEntity.setName("testCreateEntities");
            em.persist(cacheableTrueEntity);
            m_cacheableTrueEntity1Id = cacheableTrueEntity.getId();
            CacheableForceProtectedEntity cacheableForceProtectedEntity = new CacheableForceProtectedEntity();
            cacheableForceProtectedEntity.setName("testCreateEntities");
            em.persist(cacheableForceProtectedEntity);
            m_cacheableForceProtectedEntity1Id = cacheableForceProtectedEntity.getId();

            CacheableFalseEntity cacheableFalseEntity = new CacheableFalseEntity();
            em.persist(cacheableFalseEntity);
            
            cacheableForceProtectedEntity.setCacheableFalse(cacheableFalseEntity);
            
            CacheableProtectedEntity cacheableProtectedEntity = new CacheableProtectedEntity();
            em.persist(cacheableProtectedEntity);
            m_cacheableProtectedEntityId = cacheableProtectedEntity.getId();
            cacheableFalseEntity.setProtectedEntity(cacheableProtectedEntity);
            
            CacheableTrueEntity cacheableTrueEntity2 = new CacheableTrueEntity();
            cacheableTrueEntity2.setName("testCreateEntities");
            em.persist(cacheableTrueEntity2);
            m_cacheableTrueEntity2Id = cacheableTrueEntity2.getId();
            
            ChildCacheableFalseEntity childCacheableFalseEntity = new ChildCacheableFalseEntity();
            childCacheableFalseEntity.setName("testCreateEntities");
            em.persist(childCacheableFalseEntity);
            m_childCacheableFalseEntityId = childCacheableFalseEntity.getId();
            
            ForceProtectedEntityWithComposite fpewc = new ForceProtectedEntityWithComposite();
            fpewc.setName("testCreateEntities");
            ProtectedEmbeddable pe = new ProtectedEmbeddable();
            fpewc.setProtectedEmbeddable(pe);
            CacheableFalseEntity cfe = new CacheableFalseEntity();
            pe.setCacheableFalseEntity(cfe);
            SharedEmbeddable se = new SharedEmbeddable();
            fpewc.setSharedEmbeddable(se);
            em.persist(fpewc);
            m_forcedProtectedEntityCompositId = fpewc.getId();
            em.persist(cfe);
            CacheableFalseDetail cacheableFalseDetailEntity = new CacheableFalseDetail();
            em.persist(cacheableFalseDetailEntity);

            CacheableRelationshipsEntity prse = new CacheableRelationshipsEntity();
            prse.setName("Test OneToMany");
            prse.addCacheableFalse(cacheableFalseEntity);
            prse.addCacheableProtected(cacheableProtectedEntity);
            prse.setCacheableFPE(cacheableForceProtectedEntity);
            prse.addCacheableFalseDetail(cacheableFalseDetailEntity);
            prse.addProtectedEmbeddable(pe);
            em.persist(prse);
            m_cacheableRelationshipsEntityId = prse.getId();
            commitTransaction(em);
        } catch (Exception e) {
            fail("Error occurred creating some entities");
        } finally {
            closeEntityManager(em);   
        }
    }
    
    /**
     * Convenience method. This will not update the entity in the shared cache.
     */
    protected void updateCacheableTrueEntityNameAndBypassStore(String name) {
        EntityManager em = createDSEntityManager();
        
        try {
            beginTransaction(em);
            Query query = em.createQuery("UPDATE JPA_CACHEABLE_TRUE e SET e.name = :name " + "WHERE e.id = :id ").setParameter("name", name).setParameter("id", m_cacheableTrueEntity1Id);
            query.setHint(QueryHints.CACHE_STORE_MODE, CacheStoreMode.BYPASS);
            query.executeUpdate();
            commitTransaction(em);
        } catch (Exception e) {
            fail("Error updating the entity through JPQL: " + e);
        } finally {
            closeEM(em);
        }
    }
    
    /**
     * Convenience method. This will update the entity in the shared cache.
     */
    protected void updateCacheableTrueEntityNameInSharedCache(String name) {
        updateCacheableTrueEntityNameInSharedCache(name, null);
    }
    
    /**
     * Convenience method. This will update the entity in the shared cache.
     */
    protected void updateCacheableTrueEntityNameInSharedCache(String name, EntityManager em) {
        EntityManager emToUse;
        
        if (em == null) {
            emToUse = createDSEntityManager();
        } else {
            emToUse = em;
        }
        
        try {
            beginTransaction(emToUse);
            emToUse.createQuery("UPDATE JPA_CACHEABLE_TRUE e SET e.name = :name " + "WHERE e.id = :id ").setParameter("name", name).setParameter("id", m_cacheableTrueEntity1Id).executeUpdate();
            commitTransaction(emToUse);
        } catch (Exception e) {
            fail("Error updating the entity through JPQL: " + e);
        } finally {
            if (em == null) {
                closeEM(emToUse);
            }
        }
    }

    public void testProtectedRelationshipsMetadata(){
        EntityManager em = createDSEntityManager();
        ServerSession session = em.unwrap(ServerSession.class);
        ClassDescriptor descriptor = session.getDescriptor(ProtectedRelationshipsEntity.class);
        for (DatabaseMapping mapping : descriptor.getMappings()){
            if (!mapping.isDirectToFieldMapping()){
                assertTrue("Relationship NONCacheable metadata was not processed correctly", !mapping.isCacheable());
            }
        }
        descriptor = session.getDescriptorForAlias("XML_ROTECTED_RELATIONSHIPS");
        for (DatabaseMapping mapping : descriptor.getMappings()){
            if (!mapping.isDirectToFieldMapping()){
                assertTrue("Relationship NONCacheable metadata was not processed correctly", !mapping.isCacheable());
            }
        }
        closeEM(em);
        
    }

    public void testForceProtectedFromEmbeddable(){
        EntityManager em = createDSEntityManager();
        ClassDescriptor forcedProtectedDescriptor = em.unwrap(ServerSession.class).getDescriptor(ForceProtectedEntityWithComposite.class);
        ClassDescriptor protectedEmbeddableDesc = forcedProtectedDescriptor.getMappingForAttributeName("protectedEmbeddable").getReferenceDescriptor();
        ClassDescriptor sharedEmbeddableDesc = forcedProtectedDescriptor.getMappingForAttributeName("sharedEmbeddable").getReferenceDescriptor();
        assertFalse("Isolation of Entity not altered when embeddable has noncacheable relationship", forcedProtectedDescriptor.isSharedIsolation());
        assertFalse("Isolation of Embeddable not altered when embeddable has noncacheable relationship", protectedEmbeddableDesc.isSharedIsolation());
        assertFalse("Isolation of Embeddable not altered when Parent Entity is Protected", sharedEmbeddableDesc.isSharedIsolation());
    }

    public void testEmbeddableProtectedCaching(){
        //Nested transaction not supported
        if (! isOnServer()) {
            EntityManager em = createDSEntityManager();
            beginTransaction(em);
            try{
                ForceProtectedEntityWithComposite cte = em.find(ForceProtectedEntityWithComposite.class, m_forcedProtectedEntityCompositId);
                CacheableRelationshipsEntity cre = em.find(CacheableRelationshipsEntity.class, m_cacheableRelationshipsEntityId);
                System.out.println("====the size of the collection is 1--" + cre.getProtectedEmbeddables().size());
                ProtectedEmbeddable pe = cte.getProtectedEmbeddable();
            
                ServerSession session = em.unwrap(ServerSession.class);
                closeEM(em);
                ForceProtectedEntityWithComposite cachedCPE = (ForceProtectedEntityWithComposite) session.getIdentityMapAccessor().getFromIdentityMap(cte);
                CacheableRelationshipsEntity cachedCRE = (CacheableRelationshipsEntity) session.getIdentityMapAccessor().getFromIdentityMap(cre);
                assertNotNull("ForceProtectedEntityWithComposite was not found in the cache", cachedCPE);
                assertNotNull("CacheableRelationshipsEntity was not found in the cache", cachedCRE);
                cachedCPE.getProtectedEmbeddable().setName("NewName"+System.currentTimeMillis());
                System.out.println("====the size of the collection is 2--" + cachedCRE.getProtectedEmbeddables().size());
                //follwoing code is commented out due to bug 336651
                //cachedCRE.getProtectedEmbeddables().get(0).setName("NewName"+System.currentTimeMillis());
                em = createDSEntityManager();
                beginTransaction(em);
                ForceProtectedEntityWithComposite managedCPE = em.find(ForceProtectedEntityWithComposite.class, cte.getId());
                CacheableRelationshipsEntity managedCRE = em.find(CacheableRelationshipsEntity.class, cre.getId());
                assertEquals("Cache was not used for Protected Isolation", cachedCPE.getProtectedEmbeddable().getName(),managedCPE.getProtectedEmbeddable().getName());
                //follwoing code is commented out due to bug 336651
                //assertEquals("Cache was not used for Protected Isolation", cachedCRE.getProtectedEmbeddables().get(0).getName(),managedCRE.getProtectedEmbeddables().get(0).getName());
            }finally{
            rollbackTransaction(em);
            closeEM(em);
            }
        }
    }

    public void testUpdateProtectedManyToOne(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        int creID = 0;
        try{
            CacheableForceProtectedEntity cfpe = em.find(CacheableForceProtectedEntity.class, m_cacheableForceProtectedEntity1Id);
            ServerSession session = em.unwrap(ServerSession.class);
            CacheableRelationshipsEntity cre = new CacheableRelationshipsEntity();
            em.persist(cre);
            creID = cre.getId();
            cre.setCacheableFPE(cfpe);
            commitTransaction(em);
            CacheableRelationshipsEntity cachedCRE = (CacheableRelationshipsEntity) session.getIdentityMapAccessor().getFromIdentityMap(cre);
            assertTrue("A protected ManyToOne relationship was not merged into the shared cache", cachedCRE.getCacheableFPE() != null);
            beginTransaction(em);
            cre = em.find(CacheableRelationshipsEntity.class, creID);
            em.remove(cre);
            commitTransaction(em);
        }finally{
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEM(em);
        }
    }

    public void testUpdateProtectedManyToMany(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        int cfdID1 = 0, cfdID2 = 0;
        try{
            CacheableRelationshipsEntity cre = em.find(CacheableRelationshipsEntity.class, m_cacheableRelationshipsEntityId);
            ServerSession session = em.unwrap(ServerSession.class);
            CacheableFalseDetail cfd1 = new CacheableFalseDetail();
            CacheableFalseDetail cfd2 = new CacheableFalseDetail();
            em.persist(cfd1);
            em.persist(cfd2);
            cfdID1 = cfd1.getId();
            cfdID2 = cfd2.getId();
            cre.addCacheableFalseDetail(cfd1);
            cre.addCacheableFalseDetail(cfd2);
            commitTransaction(em);

            CacheableRelationshipsEntity cachedCRE = (CacheableRelationshipsEntity) session.getIdentityMapAccessor().getFromIdentityMap(cre);
            assertTrue("A protected ManyToMany relationship was merged into the shared cache", cachedCRE.getCacheableFalseDetails() == null || cachedCRE.getCacheableFalseDetails().isEmpty());
            beginTransaction(em);
            cre.getCacheableFalseDetails().clear();
            cre = em.find(CacheableRelationshipsEntity.class, m_cacheableRelationshipsEntityId);
            cfd1 = em.find(CacheableFalseDetail.class, cfdID1);
            cfd2 = em.find(CacheableFalseDetail.class, cfdID2);
            cre.removeCacheableFalseDetail(cfd1);
            cre.removeCacheableFalseDetail(cfd2);
            em.remove(cfd1);
            em.remove(cfd2);
            commitTransaction(em);
        }finally{
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEM(em);
        }
    }

    public void testUpdateProtectedElementCollection(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        try{
            CacheableRelationshipsEntity cre = em.find(CacheableRelationshipsEntity.class, m_cacheableRelationshipsEntityId);
            ServerSession session = em.unwrap(ServerSession.class);
            ProtectedEmbeddable cem1 = new ProtectedEmbeddable();
            ProtectedEmbeddable cem2 = new ProtectedEmbeddable();
            cre.addProtectedEmbeddable(cem1);
            cre.addProtectedEmbeddable(cem2);
            commitTransaction(em);

            CacheableRelationshipsEntity cachedCRE = (CacheableRelationshipsEntity) session.getIdentityMapAccessor().getFromIdentityMap(cre);
            assertTrue("A protected ElementCollection relationship was merged into the shared cache", cachedCRE.getProtectedEmbeddables() == null || cachedCRE.getProtectedEmbeddables().isEmpty());
            beginTransaction(em);
            cre.getProtectedEmbeddables().clear();
            commitTransaction(em);
        }finally{
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEM(em);
        }
    }

    public void testIsolationBeforeEarlyTxBegin(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        int cfeID = 0;
        try{
            CacheableForceProtectedEntity cte = em.find(CacheableForceProtectedEntity.class, m_cacheableForceProtectedEntity1Id);
            ServerSession session = em.unwrap(ServerSession.class);
            CacheableProtectedEntity cfe = new CacheableProtectedEntity();
            em.persist(cfe);
            cfeID = cfe.getId();
            cfe.setForcedProtected(cte);
            cte.getCacheableProtecteds().add(cfe);
            em.flush();
            //commitTransaction(em);
            CacheableRelationshipsEntity cre = em.find(CacheableRelationshipsEntity.class, m_cacheableRelationshipsEntityId);
            CacheableRelationshipsEntity cachedCRE = (CacheableRelationshipsEntity) session.getIdentityMapAccessor().getFromIdentityMap(cre);
            assertTrue("A protected OneToMany relationship was merged into the shared cache", cachedCRE.getCacheableFalses() == null || cachedCRE.getCacheableFalses().isEmpty());
            commitTransaction(em);

            beginTransaction(em);
            cte.getCacheableProtecteds().clear();
            cfe.setForcedProtected(null);
            cfe = em.find(CacheableProtectedEntity.class, cfeID);
            em.remove(cfe);
            commitTransaction(em);
        }finally{
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEM(em);
        }
    }

    public void testLoadMixedCacheTree(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        try{
            CacheableForceProtectedEntity cte = em.find(CacheableForceProtectedEntity.class, m_cacheableForceProtectedEntity1Id);
            assertNotNull("Did not load the CacheableTrue Entity", cte);
            CacheableFalseEntity cfe = cte.getCacheableFalse();
            assertNotNull("Did not load the CacheableFalse related Entity", cfe);
            CacheableProtectedEntity cpe = cfe.getProtectedEntity();
            assertNotNull("Did not load the Cacheable Protected related Entity", cpe);
            CacheableRelationshipsEntity cre = em.find(CacheableRelationshipsEntity.class, m_cacheableRelationshipsEntityId);
            assertNotNull("Did not load the CacheableRelationshipsEntity", cre);
            List<CacheableFalseEntity> cacheableFalses = cre.getCacheableFalses();
            assertNotNull("Did not load collections of CacheableRelationshipsEntity related(OneToMany) CacheableFalseEntity", cacheableFalses);
            List<CacheableProtectedEntity> cacheableProtects = cre.getCacheableProtecteds();
            assertNotNull("Did not load collections of CacheableRelationshipsEntity related(OneToMany) CacheableProtectedEntity", cacheableProtects);
            CacheableForceProtectedEntity cfpe = cre.getCacheableFPE();
            assertNotNull("Did not load the CacheableRelationshipsEntity related(ManyToOne) CacheableForceProtectedEntity", cfpe);
            List<CacheableFalseDetail> cacheableFalseDetails = cre.getCacheableFalseDetails();
            assertNotNull("Did not load collections of CacheableRelationshipsEntity related(ManyToMany) CacheableFalseDetail", cacheableFalseDetails);
            List<ProtectedEmbeddable> protectedEmbed = cre.getProtectedEmbeddables();
            assertNotNull("Did not load collections of CacheableRelationshipsEntity related(ElementCollection) ProtectedEmbeddable", protectedEmbed);
    }finally{
        rollbackTransaction(em);
        closeEM(em);
        }
    }
    
    public void testIsolatedIsolation(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        try{
            CacheableForceProtectedEntity cte = em.find(CacheableForceProtectedEntity.class, m_cacheableForceProtectedEntity1Id);
            CacheableFalseEntity cfe = cte.getCacheableFalse();
            assertNull("An isolated Entity was found in the shared cache", em.unwrap(ServerSession.class).getIdentityMapAccessor().getFromIdentityMap(cfe));
            CacheableRelationshipsEntity cre = em.find(CacheableRelationshipsEntity.class, m_cacheableRelationshipsEntityId);
            for (CacheableFalseEntity cfe1 : cre.getCacheableFalses()){
                assertNull("An isolated Entity in many side of OneToMany relationship was found in the shared cache", em.unwrap(ServerSession.class).getIdentityMapAccessor().getFromIdentityMap(cfe1));
            }
            for (CacheableFalseDetail cfde1 : cre.getCacheableFalseDetails()){
                assertNull("An isolated Entity in many side of ManyToMany relationship was found in the shared cache", em.unwrap(ServerSession.class).getIdentityMapAccessor().getFromIdentityMap(cfde1));
            } 
    }finally{
        rollbackTransaction(em);
        closeEM(em);
        }
    }
    
    public void testProtectedIsolation(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        try{
            CacheableForceProtectedEntity cte = em.find(CacheableForceProtectedEntity.class, m_cacheableForceProtectedEntity1Id);
            CacheableFalseEntity cfe = cte.getCacheableFalse();
            CacheableProtectedEntity cpe = cfe.getProtectedEntity();
            ServerSession session = em.unwrap(ServerSession.class);
            assertNull("An protected relationshipwas found in the shared cache", ((CacheableForceProtectedEntity)session.getIdentityMapAccessor().getFromIdentityMap(cte)).getCacheableFalse());
            CacheableRelationshipsEntity cre = em.find(CacheableRelationshipsEntity.class, m_cacheableRelationshipsEntityId);
            for (CacheableProtectedEntity cpe1 : ((CacheableRelationshipsEntity)session.getIdentityMapAccessor().getFromIdentityMap(cre)).getCacheableProtecteds()){
                assertNotNull("An protected relationship in OneToMany was not found in the shared cache", cpe1);
            }
            assertNotNull("An protected relationship in ManyToOne was not found in the shared cache", ((CacheableRelationshipsEntity)session.getIdentityMapAccessor().getFromIdentityMap(cre)).getCacheableFPE());
            for (ProtectedEmbeddable cpe2 : ((CacheableRelationshipsEntity)session.getIdentityMapAccessor().getFromIdentityMap(cre)).getProtectedEmbeddables()){
                assertNotNull("An protected relationship in ElementCollection was not found in the shared cache", cpe2);
            }
        }finally{
        rollbackTransaction(em);
        closeEM(em);
        }
        
    }

    public void testProtectedCaching(){
        //Nested transaction not supported
        if (! isOnServer()) {
            EntityManager em = createDSEntityManager();
            beginTransaction(em);
            try{
                CacheableForceProtectedEntity cte = em.find(CacheableForceProtectedEntity.class, m_cacheableForceProtectedEntity1Id);
                CacheableFalseEntity cfe = cte.getCacheableFalse();
                CacheableProtectedEntity cpe = cfe.getProtectedEntity();
                CacheableRelationshipsEntity cre = em.find(CacheableRelationshipsEntity.class, m_cacheableRelationshipsEntityId);
                List<CacheableProtectedEntity> cacheableProtects = cre.getCacheableProtecteds();
                CacheableForceProtectedEntity cfpe = cre.getCacheableFPE();
                ServerSession session = em.unwrap(ServerSession.class);
                closeEM(em);
                CacheableProtectedEntity cachedCPE = (CacheableProtectedEntity) session.getIdentityMapAccessor().getFromIdentityMap(cpe);
                assertNotNull("CacheableProtectedEntity was not found in the cache", cachedCPE);
                CacheableForceProtectedEntity cachedCFPE = (CacheableForceProtectedEntity) session.getIdentityMapAccessor().getFromIdentityMap(cfpe);
                assertNotNull("CacheableForceProtectedEntity from ManyToOne relationship was not found in the cache", cachedCFPE);
                for (CacheableProtectedEntity cpe1 : cacheableProtects){
                    CacheableProtectedEntity cachedCPE1 = (CacheableProtectedEntity) session.getIdentityMapAccessor().getFromIdentityMap(cpe1);
                    assertNotNull("CacheableProtectedEntity from OneToMany relationship was not found in the cache", cachedCPE1);
                } 
                cachedCPE.setName("NewName"+System.currentTimeMillis());
                cachedCFPE.setName("NewName"+System.currentTimeMillis());
                em = createDSEntityManager();
                beginTransaction(em);
                CacheableProtectedEntity managedCPE = em.find(CacheableProtectedEntity.class, cpe.getId());
                CacheableForceProtectedEntity managedCFPE = em.find(CacheableForceProtectedEntity.class, cfpe.getId());
                assertEquals("Cache was not used for Protected Isolation", cachedCPE.getName(),managedCPE.getName());
                assertEquals("Cache was not used for Protected Isolation", cachedCFPE.getName(),managedCFPE.getName());
            }finally{
            rollbackTransaction(em);
            closeEM(em);
            }
        }
    }

    public void testReadOnlyTree(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        try{
            Query q = em.createQuery("Select c from JPA_CACHEABLE_FORCE_PROTECTED c");
            q.setHint(QueryHints.READ_ONLY, "true");
            CacheableForceProtectedEntity cte = (CacheableForceProtectedEntity) q.getResultList().get(0);
            assertNotNull("Did not load the CacheableTrue Entity", cte);
            CacheableFalseEntity cfe = cte.getCacheableFalse();
            assertNotNull("Did not load the CacheableFalse related Entity", cfe);
            CacheableProtectedEntity cpe = cfe.getProtectedEntity();
            assertNotNull("Did not load the Cacheable Protected related Entity", cpe);
            Query q1 = em.createQuery("Select c from JPA_CACHEABLE_RELATIONSHIPS c");
            q1.setHint(QueryHints.READ_ONLY, "true");
            CacheableRelationshipsEntity cre = (CacheableRelationshipsEntity) q1.getResultList().get(0);
            for (CacheableFalseEntity cfe1 : cre.getCacheableFalses()){
                assertNotNull("Did not load the CacheableFalse related Entity in OneToMany relationship", cfe1);
            }
            for (CacheableProtectedEntity cpe1 : cre.getCacheableProtecteds()){
                assertNotNull("Did not load the CacheableProtected related Entity in OneToMany relationship", cpe1);
            }
            CacheableForceProtectedEntity cfpe = cre.getCacheableFPE();
            assertNotNull("Did not load the Cacheable Force Protected related Entity in ManyToOne relationship", cfpe);
            for (CacheableFalseDetail cfde : cre.getCacheableFalseDetails()){
                assertNotNull("Did not load the CacheableFalse Details related Entity in ManyToMany relationship", cfde);
            }
        }finally{
            rollbackTransaction(em);
            closeEM(em);
        }
    }

    public void testUpdateForceProtectedBasic(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        try{
            CacheableForceProtectedEntity cte = em.find(CacheableForceProtectedEntity.class, m_cacheableForceProtectedEntity1Id);
            String newName = "SomeNewName" + System.currentTimeMillis();
            cte.setName(newName);
            commitTransaction(em);
            ServerSession session = em.unwrap(ServerSession.class);
            CacheableForceProtectedEntity cachedCPE = (CacheableForceProtectedEntity) session.getIdentityMapAccessor().getFromIdentityMap(cte);
            assertEquals("A Basic mapping in a Protected class was not merged into the shared cache.  Expected: "+ newName + " found: "+ cachedCPE.getName(), cachedCPE.getName(), newName);
        }finally{
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEM(em);
        }
    }

    public void testUpdateForceProtectedOneToOne(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        int cfeID = 0;
        try{
            CacheableForceProtectedEntity cte = em.find(CacheableForceProtectedEntity.class, m_cacheableForceProtectedEntity1Id);
            CacheableFalseEntity oldcfe = cte.getCacheableFalse();
            ServerSession session = em.unwrap(ServerSession.class);
            CacheableFalseEntity cfe = new CacheableFalseEntity();
            em.persist(cfe);
            cfeID = cfe.getId();
            cte.setCacheableFalse(cfe);
            commitTransaction(em);
            CacheableForceProtectedEntity cachedCPE = (CacheableForceProtectedEntity) session.getIdentityMapAccessor().getFromIdentityMap(cte);
            assertNull("A protected OneToOne relationship was merged into the shared cache", cachedCPE.getCacheableFalse());
            ObjectReferenceMapping orm = (ObjectReferenceMapping) session.getDescriptor(CacheableForceProtectedEntity.class).getMappingForAttributeName("cacheableFalse");
            Object cacheableFalsefk = session.getIdentityMapAccessorInstance().getCacheKeyForObject(cte).getProtectedForeignKeys().get(orm.getSelectFields().get(0));
            assertEquals("FK update not cached", cfe.getId(), cacheableFalsefk);
            beginTransaction(em);
            cte.setCacheableFalse(oldcfe);
            cfe = em.find(CacheableFalseEntity.class, cfeID);
            em.remove(cfe);
            commitTransaction(em);
        }finally{
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEM(em);
        }
    }

    public void testUpdateProtectedBasic(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        try{
            CacheableProtectedEntity cte = em.find(CacheableProtectedEntity.class, m_cacheableProtectedEntityId);
            ServerSession session = em.unwrap(ServerSession.class);
            String newName = "SomeNewName" + System.currentTimeMillis();
            cte.setName(newName);
            commitTransaction(em);
            CacheableProtectedEntity cachedCPE = (CacheableProtectedEntity) session.getIdentityMapAccessor().getFromIdentityMap(cte);
            assertEquals("A Basic mapping in a Protected class was not merged into the shared cache", newName, cachedCPE.getName());
        }finally{
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEM(em);
        }
    }

    public void testUpdateProtectedOneToMany(){
        EntityManager em = createDSEntityManager();
        beginTransaction(em);
        int cfeID = 0;
        try{
            CacheableForceProtectedEntity cte = em.find(CacheableForceProtectedEntity.class, m_cacheableForceProtectedEntity1Id);
            ServerSession session = em.unwrap(ServerSession.class);
            CacheableProtectedEntity cfe = new CacheableProtectedEntity();
            em.persist(cfe);
            cfeID = cfe.getId();
            cfe.setForcedProtected(cte);
            cte.getCacheableProtecteds().add(cfe);
            commitTransaction(em);

            CacheableForceProtectedEntity cachedCPE = (CacheableForceProtectedEntity) session.getIdentityMapAccessor().getFromIdentityMap(cte);
            assertTrue("A protected OneToMany relationship was not merged into the shared cache", cachedCPE.getCacheableProtecteds() != null && !cachedCPE.getCacheableProtecteds().isEmpty());
            beginTransaction(em);
            cte.getCacheableProtecteds().clear();
            cfe.setForcedProtected(null);
            cfe = em.find(CacheableProtectedEntity.class, cfeID);
            em.remove(cfe);
            commitTransaction(em);
        }finally{
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEM(em);
        }
    }
    
    /**
     * Convenience method.
     */
    private boolean usesNoCache(ClassDescriptor descriptor) {
        return descriptor.isIsolated();
    }
}
