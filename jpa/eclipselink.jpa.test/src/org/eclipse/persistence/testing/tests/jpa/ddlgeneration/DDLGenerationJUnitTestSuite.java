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
 *     08/20/2008-1.0.1 Nathan Beyer (Cerner) 
 *       - 241308: Primary key is incorrectly assigned to embeddable class 
 *                 field with the same name as the primary key field's name
 *     01/12/2009-1.1 Daniel Lo, Tom Ware, Guy Pelletier
 *       - 247041: Null element inserted in the ArrayList 
 *     07/17/2009 - tware -  added tests for DDL generation of maps
 ******************************************************************************/   
package org.eclipse.persistence.testing.tests.jpa.ddlgeneration;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
import org.eclipse.persistence.testing.models.jpa.ddlgeneration.weaving.Port;
import org.eclipse.persistence.testing.models.jpa.ddlgeneration.weaving.impl.EquipmentDAO;
import org.eclipse.persistence.testing.models.jpa.ddlgeneration.weaving.impl.PortDAO;
import org.eclipse.persistence.testing.models.jpa.ddlgeneration.*;
import org.eclipse.persistence.testing.models.jpa.ddlgeneration.weaving.Equipment;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

/**
 * JUnit test case(s) for DDL generation.
 */
public class DDLGenerationJUnitTestSuite extends JUnitTestCase {
    // the persistence unit name which is used in this test suite
    private static final String DDL_PU = "ddlGeneration";

    public DDLGenerationJUnitTestSuite() {
        super();
    }

    public DDLGenerationJUnitTestSuite(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(DDLGenerationJUnitTestSuite.class);
        
        return suite;
    }
    
    /**
     * The setup is done as a test, both to record its failure, and to allow execution in the server.
     */
    public void testSetup() {
        try{
            // Trigger DDL generation
            //TODO: Let's add a flag which do not disregard DDL generation errors.
            //TODO: This is required to ensure that DDL generation has succeeded.
            EntityManager em = createEntityManager(DDL_PU);
            //em.close();
            clearCache(DDL_PU);
        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    // Test for GF#1392
    // If there is a same name column for the entity and many-to-many table, wrong pk constraint generated.
    public void testDDLPkConstraintErrorIncludingRelationTableColumnName() {
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {

            CKeyEntityC c = new CKeyEntityC(new CKeyEntityCPK("Manager"));
            em.persist(c);

            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            fail("DDL generation may generate wrong Primary Key constraint, thrown:" + e);
        } finally {
            closeEntityManager(em);
        }
    }

    // Test for relationships using candidate(unique) keys
    public void testDDLUniqueKeysAsJoinColumns() {
        CKeyEntityAPK aKey;
        CKeyEntityBPK bKey;
        
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            CKeyEntityA a = new CKeyEntityA("Wonseok", "Kim");
            int seq = (int)System.currentTimeMillis(); // just to get unique value :-)
            CKeyEntityB b = new CKeyEntityB(new CKeyEntityBPK(seq, "B1209"));
            //set unique keys
            b.setUnq1("u0001");
            b.setUnq2("u0002");

            a.setUniqueB(b);
            b.setUniqueA(a);
            
            em.persist(a);
            em.persist(b);

            commitTransaction(em);
            
            aKey = a.getKey();
            bKey = b.getKey();
        } catch (RuntimeException e) {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            throw e;
        } finally {
            closeEntityManager(em);
        }
        //clearCache(DDL_PU);

        em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            CKeyEntityA a = em.find(CKeyEntityA.class, aKey);
            assertNotNull(a);

            CKeyEntityB b = a.getUniqueB();
            assertNotNull(b);

            assertEquals(b.getUnq1(), "u0001");
            assertEquals(b.getUnq2(), "u0002");

            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            throw e;
        } finally {
            closeEntityManager(em);
        }
        //clearCache(DDL_PU);

        em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {

            CKeyEntityB b = em.find(CKeyEntityB.class, bKey);
            assertNotNull(b);

            CKeyEntityA a = b.getUniqueA();
            assertNotNull(a);
            assertEquals(a.getKey(), aKey);

            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            throw e;
        } finally {
            closeEntityManager(em);
        }

    }

    // Test to check if unique constraints are generated correctly
    public void testDDLUniqueConstraintsByAnnotations() {
        if(!getServerSession(DDL_PU).getPlatform().supportsUniqueKeyConstraints()) {
            return;
        }
        UniqueConstraintsEntity1 ucEntity;
        
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            ucEntity = em.find(UniqueConstraintsEntity1.class, 1);
            if(ucEntity == null) {
                ucEntity = new UniqueConstraintsEntity1(1);
                ucEntity.setColumns(1, 1, 1, 1);
                em.persist(ucEntity);
            }
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
            throw e;
        }
        
        beginTransaction(em);
        try {
            ucEntity = new UniqueConstraintsEntity1(2);
            ucEntity.setColumns(1, 2, 2, 2);
            em.persist(ucEntity);
            em.flush();
            
            fail("Unique constraint violation is expected");
        } catch (PersistenceException e) {
            //expected
        } finally {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
        }

        em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            ucEntity = new UniqueConstraintsEntity1(2);
            ucEntity.setColumns(2, 1, 2, 2);
            em.persist(ucEntity);
            em.flush();
            
            fail("Unique constraint violation is expected");
        } catch (PersistenceException e) {
            //expected
        } finally {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
        }
        
        em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            ucEntity = new UniqueConstraintsEntity1(2);
            ucEntity.setColumns(2, 2, 1, 1);
            em.persist(ucEntity);
            em.flush();
            
            fail("Unique constraint violation is expected");
        } catch (PersistenceException e) {
            //expected
        } finally {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
        }

        em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            ucEntity = new UniqueConstraintsEntity1(2);
            ucEntity.setColumns(2, 2, 1, 2);
            em.persist(ucEntity);
            em.flush();
        } catch (PersistenceException e) {
            fail("Unique constraint violation is not expected, thrown:" + e);
        } finally {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
        }
    }
    
    // Test to check if unique constraints are generated correctly
    public void testDDLUniqueConstraintsByXML() {
        if(!getServerSession(DDL_PU).getPlatform().supportsUniqueKeyConstraints()) {
            return;
        }
        UniqueConstraintsEntity2 ucEntity;
        
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            ucEntity = em.find(UniqueConstraintsEntity2.class, 1);
            if(ucEntity == null) {
                ucEntity = new UniqueConstraintsEntity2(1);
                ucEntity.setColumns(1, 1, 1, 1);
                em.persist(ucEntity);
            }
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
            throw e;
        }
        
        beginTransaction(em);
        try {
            ucEntity = new UniqueConstraintsEntity2(2);
            ucEntity.setColumns(1, 2, 2, 2);
            em.persist(ucEntity);
            em.flush();
            
            fail("Unique constraint violation is expected");
        } catch (PersistenceException e) {
            //expected
        } finally {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
        }

        em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            ucEntity = new UniqueConstraintsEntity2(2);
            ucEntity.setColumns(2, 1, 2, 2);
            em.persist(ucEntity);
            em.flush();
            
            fail("Unique constraint violation is expected");
        } catch (PersistenceException e) {
            //expected
        } finally {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
        }
        
        em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            ucEntity = new UniqueConstraintsEntity2(2);
            ucEntity.setColumns(2, 2, 1, 1);
            em.persist(ucEntity);
            em.flush();
            
            fail("Unique constraint violation is expected");
        } catch (PersistenceException e) {
            //expected
        } finally {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
        }

        em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            ucEntity = new UniqueConstraintsEntity2(2);
            ucEntity.setColumns(2, 2, 1, 2);
            em.persist(ucEntity);
            em.flush();
        } catch (PersistenceException e) {
            fail("Unique constraint violation is not expected, thrown:" + e);
        } finally {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            closeEntityManager(em);
        }
    }

    // test if the primary key columns of subclass entity whose root entity has EmbeddedId 
    // are generated properly in joined inheritance strategy
    // Issue: GF#1391
    public void testDDLSubclassEmbeddedIdPkColumnsInJoinedStrategy() {
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        // let's see if a subclass entity is persisted and found well
        try {
            int seq = (int)System.currentTimeMillis(); // just to get unique value :-)
            String code = "B1215";
            CKeyEntityB2 b = new CKeyEntityB2(new CKeyEntityBPK(seq, code));
            //set unique keys
            b.setUnq1(String.valueOf(seq));
            b.setUnq2(String.valueOf(seq));

            em.persist(b);
            em.flush();
            String query = "SELECT b FROM CKeyEntityB2 b WHERE b.key.seq = :seq AND b.key.code = :code";
            Object result = em.createQuery(query).setParameter("seq", seq).setParameter("code", code)
                            .getSingleResult();
            assertNotNull(result);
            
            rollbackTransaction(em);
            
        } catch (RuntimeException e) {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            throw e;
        } finally {
            closeEntityManager(em);
        }
    }

    // Bug 241308 - Primary key is incorrectly assigned to embeddable class 
    // field with the same name as the primary key field's name
    public void testBug241308() {
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
     
        try {
            ThreadInfo threadInfo1 = new ThreadInfo();
            threadInfo1.setId(0);
            threadInfo1.setName("main");
            
            MachineState machineState1 = new MachineState();
            machineState1.setId(0);
            machineState1.setThread(threadInfo1);
            
            em.persist(machineState1);
            
            ThreadInfo threadInfo2 = new ThreadInfo();
            threadInfo2.setId(0);
            threadInfo2.setName("main");
            
            MachineState machineState2 = new MachineState();
            machineState2.setId(1);
            machineState2.setThread(threadInfo2);
            
            em.persist(machineState2);
            
            commitTransaction(em);
            
        } catch (RuntimeException e) {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            throw e;
        } finally {
            closeEntityManager(em);
        }
    }
    
    public void testDDLUnidirectionalOneToMany() {    
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        try {
            // add comments
            long seq = System.currentTimeMillis(); // just to get unique value :-)
            CKeyEntityB b = new CKeyEntityB(new CKeyEntityBPK(seq, "B1210"));
            List<Comment> comments = new ArrayList(2);
            comments.add(new Comment("comment 1"));
            comments.add(new Comment("comment 2"));
            b.setComments(comments);
            //set unique keys
            b.setUnq1("u0003");
            b.setUnq2("u0004");
            em.persist(b);
            commitTransaction(em);
            
            // clean-up
            beginTransaction(em);
            CKeyEntityB b0 = em.find(CKeyEntityB.class, b.getKey());
            em.remove(b0.getComments().get(0));
            em.remove(b0.getComments().get(1));
            em.remove(b0);
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em))
                rollbackTransaction(em);
            throw e;
        } finally {
            closeEntityManager(em);
        }
    }
    
    public void testCascadeMergeOnManagedEntityWithOrderedList() {
        EntityManagerFactory factory = getEntityManagerFactory(DDL_PU);
        
        // Clean up first
        cleanupEquipmentAndPorts(factory);
        
        // Create a piece equipment with one port.
        createEquipment(factory);
            
        // Add two ports to the equipment
        addPorts(factory);
            
        // Fetch the equipment and validate there is no null elements in
        // the ArrayList of Port.
        verifyPorts(factory);
    }
    
    public void testDirectCollectionMapping(){
        EntityManager em = createEntityManager(DDL_PU);
        try{
            beginTransaction(em);
            
            MapHolder holder = new MapHolder();
            holder.setId(1);
            EntityMapKey key = new EntityMapKey();
            key.setId(1);
            holder.getDCMap().put(key, "test1");
            em.persist(holder);
            em.persist(key);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to flush a new ddl-generated DirectCollectionMapping." + e);
            }
            
            clearCache(DDL_PU);
            em.refresh(holder);
            assertTrue(holder.getDCMap().get(key) != null);
            
            holder.getDCMap().remove(key);
            em.remove(holder);
            em.remove(key);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to remove a new ddl-generated DirectCollectionMapping." + e);
            }
            
            rollbackTransaction(em);
        } finally {
            closeEntityManager(em);
        }
    }
    
    public void testAggregateCollectionMapping(){
        EntityManager em = createEntityManager(DDL_PU);
        try{
            beginTransaction(em);
            
            MapHolder holder = new MapHolder();
            holder.setId(2);
            EntityMapKey key = new EntityMapKey();
            key.setId(2);
            AggregateMapValue value = new AggregateMapValue();
            value.setDescription("test2");
            holder.getACMap().put(key, value);
            em.persist(holder);
            em.persist(key);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to flush a new ddl-generated AggregateCollectionMapping." + e);
            }
            
            clearCache(DDL_PU);
            em.refresh(holder);
            assertTrue(holder.getACMap().get(key) != null);
            
            holder.getACMap().remove(key);
            em.remove(holder);
            em.remove(key);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to remove a new ddl-generated AggregateCollectionMapping." + e);
            }
            
            rollbackTransaction(em);
        } finally {
            closeEntityManager(em);
        }
    }
    
    public void testOneToManyMapping(){
        EntityManager em = createEntityManager(DDL_PU);
        try{
            beginTransaction(em);
            
            MapHolder holder = new MapHolder();
            holder.setId(3);
            AggregateMapKey key = new AggregateMapKey();
            key.setDescription("test3");
            EntityMapValueWithBackPointer value = new EntityMapValueWithBackPointer();
            value.setHolder(holder);
            value.setId(3);
            holder.getOTMMap().put(key, value);
            em.persist(holder);
            em.persist(value);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to flush a new ddl-generated OneToManyMapping." + e);
            }
            
            clearCache(DDL_PU);
            em.refresh(holder);
            holder.getOTMMap().get(key);
            assertTrue(holder.getOTMMap().get(key) != null);
            
            holder.getOTMMap().remove(key);
            value.setHolder(null);
            em.remove(holder);
            em.remove(value);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to remove a new ddl-generated OneToManyMapping." + e);
            }
            
            rollbackTransaction(em);
        } finally {
            closeEntityManager(em);
        }
    }
    
    public void testUnidirectionalOneToManyMapping(){
        EntityManager em = createEntityManager(DDL_PU);
        try{
            beginTransaction(em);
            
            MapHolder holder = new MapHolder();
            holder.setId(4);
            EntityMapValue value = new EntityMapValue();
            value.setId(4);
            holder.getUOTMMap().put(4, value);
            em.persist(holder);
            em.persist(value);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to flush a new ddl-generated OneToManyMapping." + e);
            }
            
            clearCache(DDL_PU);
            em.refresh(holder);
            assertTrue(holder.getUOTMMap().get(4) != null);
            
            holder.getUOTMMap().remove(4);
            em.remove(holder);
            em.remove(value);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to remove a new ddl-generated OneToManyMapping."+ e);
            }
            
            rollbackTransaction(em);
        } finally {
            closeEntityManager(em);
        }
    }
    
    public void testManyToManyMapping(){
        EntityManager em = createEntityManager(DDL_PU);
        try{
            beginTransaction(em);
            
            MapHolder holder = new MapHolder();
            holder.setId(5);
            EntityMapKey key = new EntityMapKey();
            key.setId(5);
            MMEntityMapValue value = new MMEntityMapValue();
            value.getHolders().add(holder);
            value.setId(5);
            holder.getMTMMap().put(key, value);
            em.persist(holder);
            em.persist(key);
            em.persist(value);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to flush a new ddl-generated OneToManyMapping."+ e);
            }
            
            clearCache(DDL_PU);
            em.refresh(holder);
            assertTrue(holder.getMTMMap().get(key) != null);
            
            holder.getMTMMap().remove(key);
            value.getHolders().remove(0);
            em.remove(holder);
            em.remove(key);
            em.remove(value);
            
            try{
                em.flush();
            } catch (Exception e){
                e.printStackTrace();
                fail("Caught Exception while trying to remove a new ddl-generated OneToManyMapping." + e);
            }
            
            rollbackTransaction(em);
        } finally {
            closeEntityManager(em);
        }
    }
    
    protected void cleanupEquipmentAndPorts(EntityManagerFactory factory) {
        EntityManager em = null;
        
        try {
            em = factory.createEntityManager();
            beginTransaction(em);
            
            em.createQuery("DELETE FROM PortDAO").executeUpdate();     
            em.createQuery("DELETE FROM EquipmentDAO").executeUpdate();
            
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    protected void createEquipment(EntityManagerFactory factory) {
        EntityManager em = null;

        try {
            em = factory.createEntityManager();
            
            beginTransaction(em);
            
            Equipment eq = new EquipmentDAO();
            eq.setId("eq");
            
            Port port = new PortDAO();
            port.setId("p1");
            port.setPortOrder(0);
            
            eq.addPort(port);
            
            em.persist(eq);
            commitTransaction(em);
        } catch (Exception e) {
            if (em != null && isTransactionActive(em)) {
                rollbackTransaction(em);
            }
            
            fail("En error occurred creating new equipment: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    protected void addPorts(EntityManagerFactory factory) {
        EntityManager em = null;
        
        try {
            em = factory.createEntityManager();
            beginTransaction(em);
            Query query = em.createNamedQuery("Equipment.findEquipmentById");
            query.setParameter("id", "eq");
            Equipment eq = (Equipment) query.getResultList().get(0);
            commitTransaction(em);
            
            em = factory.createEntityManager();
            beginTransaction(em); 
            eq = em.merge(eq);
            
            Port port = new PortDAO();
            port.setId("p2");
            port.setPortOrder(1);
            eq.addPort(port);
            
            port = new PortDAO();
            port.setId("p3");
            port.setPortOrder(2);
            eq.addPort(port);
            
            eq = em.merge(eq);
            commitTransaction(em);
        } catch (Exception e) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
            
            fail("En error occurred adding new ports: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    protected void verifyPorts(EntityManagerFactory factory) {
        EntityManager em = null;
        
        try {
            em = factory.createEntityManager();
            beginTransaction(em);
            Query query = em.createNamedQuery("Equipment.findEquipmentById");
            query.setParameter("id", "eq");
            Equipment eq = (Equipment) query.getResultList().get(0);
            commitTransaction(em);

            for (Port port: eq.getPorts()) {
                if (port == null) {
                    fail("A null PORT was found in the collection of ports.");
                }
            } 
        } catch (Exception e) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
            
            fail("En error occurred fetching the results to verify: " + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
