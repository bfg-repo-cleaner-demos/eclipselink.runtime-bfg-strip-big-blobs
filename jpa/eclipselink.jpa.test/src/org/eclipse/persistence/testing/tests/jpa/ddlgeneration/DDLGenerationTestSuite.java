/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle, Frank Schwarz. All rights reserved.
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
 *     01/22/2010-2.0.1 Guy Pelletier 
 *       - 294361: incorrect generated table for element collection attribute overrides
 *     06/14/2010-2.2 Guy Pelletier 
 *       - 264417: Table generation is incorrect for JoinTables in AssociationOverrides
 *     09/15/2010-2.2 Chris Delahunt
 *       - 322233 - AttributeOverrides and AssociationOverride dont change field type info
 *     11/17/2010-2.2.0 Chris Delahunt 
 *       - 214519: Allow appending strings to CREATE TABLE statements
 *     11/23/2010-2.2 Frank Schwarz 
 *       - 328774: TABLE_PER_CLASS-mapped key of a java.util.Map does not work for querying
 *     01/04/2011-2.3 Guy Pelletier 
 *       - 330628: @PrimaryKeyJoinColumn(...) is not working equivalently to @JoinColumn(..., insertable = false, updatable = false)
 *     01/06/2011-2.3 Guy Pelletier
 *       - 312244: can't map optional one-to-one relationship using @PrimaryKeyJoinColumn
 *     01/11/2011-2.3 Guy Pelletier  
 *       - 277079: EmbeddedId's fields are null when using LOB with fetchtype LAZY
 ******************************************************************************/   
package org.eclipse.persistence.testing.tests.jpa.ddlgeneration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.math.BigDecimal;
import java.math.BigInteger;

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
import javax.persistence.TypedQuery;

/**
 * JUnit test case(s) for DDL generation.
 */
public class DDLGenerationTestSuite extends JUnitTestCase {
    // This is the persistence unit name on server as for persistence unit name "ddlGeneration" in J2SE
    private static final String DDL_PU = "MulitPU-1";

    public DDLGenerationTestSuite() {
        super();
    }

    public DDLGenerationTestSuite(String name) {
        super(name);
        setPuName(DDL_PU);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName("DDLGenerationTestSuite");
        suite.addTest(new DDLGenerationTestSuite("testSetup"));
        List<String> tests = new ArrayList<String>();
        tests.add("testDDLPkConstraintErrorIncludingRelationTableColumnName");
        tests.add("testOptionalPrimaryKeyJoinColumnRelationship");
        tests.add("testPrimaryKeyJoinColumns");
        tests.add("testDDLEmbeddableMapKey");
        tests.add("testDDLAttributeOverrides");
        tests.add("testDDLAttributeOverridesOnElementCollection");
        tests.add("testDDLUniqueKeysAsJoinColumns");
        tests.add("testDDLUniqueConstraintsByAnnotations");
        tests.add("testDDLUniqueConstraintsByXML");
        tests.add("testDDLSubclassEmbeddedIdPkColumnsInJoinedStrategy");
        tests.add("testBug241308");
        tests.add("testDDLUnidirectionalOneToMany");
        tests.add("testCascadeMergeOnManagedEntityWithOrderedList");
        tests.add("testDirectCollectionMapping");
        tests.add("testAggregateCollectionMapping");
        tests.add("testOneToManyMapping");
        tests.add("testUnidirectionalOneToManyMapping");
        tests.add("testManyToManyMapping");
        tests.add("testManyToManyWithMultipleJoinColumns");
        tests.add("testEmbeddedManyToMany");
        tests.add("testAssociationOverrideToEmbeddedManyToMany");
        tests.add("testLAZYLOBWithEmbeddedId");
        Collections.sort(tests);
        for (String test : tests) {
            suite.addTest(new DDLGenerationTestSuite(test));
        }
        return suite;
    }

    /**
     * The setup is done as a test, both to record its failure, and to allow execution in the server.
     */
    public void testSetup() {
        // Trigger DDL generation
        EntityManager em = createEntityManager(DDL_PU);
        closeEntityManager(em);
        clearCache(DDL_PU);
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
    
    // Test for bug 312244
    public void testOptionalPrimaryKeyJoinColumnRelationship() {
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        
        try {
            em.persist(new Course());
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }

            fail("Error persisting new course without material failed : " + e);
        } finally {
            closeEntityManager(em);
        }
    }
    
    // Test for bug 330628
    public void testPrimaryKeyJoinColumns() {
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        
        try {
            Country country = new Country();
            country.setName("USA");
            country.setIsoCode("840");
            
            State state = new State();
            state.setCountry(country);
            state.setIsoCode("36");
            state.setName("New York");
            
            Set<State> states = new HashSet<State>();
            states.add(state);
            country.setStates(states);
            
            City city = new City();
            city.setName("Rochester");
            city.setState(state);
            
            Set<City> cities = new HashSet<City>();
            cities.add(city);
            state.setCities(cities);
            
            ZipArea zipArea = new ZipArea();
            zipArea.setCity(city);
            
            Set<ZipArea> zipAreas = new HashSet<ZipArea>();
            zipAreas.add(zipArea);
            city.setZipAreas(zipAreas);
            
            Zip zip = new Zip();
            zip.setCode("14621");
            zip.setCountry(country);
            zip.setZipAreas(zipAreas);
            zipArea.setZip(zip);

            Set<Zip> zips = new HashSet<Zip>();
            country.setZips(zips);
            
            em.persist(country);
            em.persist(state);
            em.persist(city);
            em.persist(zipArea);
            em.persist(zip);
            
            commitTransaction(em);
            
        } catch (RuntimeException e) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }

            fail("Error persisting new country with city, states and zips : " + e);
        } finally {
            closeEntityManager(em);
        }
    }

    // Test for bug 294361
    public void testDDLEmbeddableMapKey() {
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);

        try {
            PropertyRecord propertyRecord = new PropertyRecord();
            
            Zipcode zipCode = new Zipcode();
            zipCode.plusFour = "1234";
            zipCode.zip = "78627";

            Address address = new Address();
            address.city = "Ottawa";
            address.state = "Ontario";
            address.street = "Main";
            address.zipcode = zipCode;

            PropertyInfo propertyInfo = new PropertyInfo();
            propertyInfo.parcelNumber = 1;
            propertyInfo.size = 2;
            propertyInfo.tax = new BigDecimal(10);
            
            propertyRecord.propertyInfos.put(address, propertyInfo);
            
            em.persist(propertyRecord);

            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }

            fail("Error persisting the PropertyRecord : " + e);
        } finally {
            closeEntityManager(em);
        }
    }

    // Test for bug 322233, that optional field info defined in Overrides are used in DDL
    public void testDDLAttributeOverrides() {
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        Exception expectedException = null;
        try {
            Purchase purchase = new Purchase();
            purchase.setFee(new Money());
            em.persist(purchase);

            em.flush();
        } catch (RuntimeException e) {
            //test expects flush to throw an exception because the FEE_AMOUNT field is null
            expectedException = e;
        } finally {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
            closeEntityManager(em);
        }
        this.assertNotNull("Expected an exception persisting null into a field with nullable=false set on an override", expectedException);
    }

    // Test for bug 322233, that optional field info defined in Overrides are used in DDL
    public void testDDLAttributeOverridesOnElementCollection() {
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);
        Exception expectedException = null;
        try {
            PropertyRecord propertyRecord = new PropertyRecord();

            Zipcode zipCode = new Zipcode();
            zipCode.plusFour = "1234";
            zipCode.zip = "78627";

            Address address = new Address();
            address.city = "Ottawa";
            address.state = "Ontario";
            address.street = "Main";
            address.zipcode = zipCode;

            PropertyInfo propertyInfo = new PropertyInfo();
            //propertyInfo.parcelNumber = 1;//Keep this as null, to test the nullable=false setting was used
            propertyInfo.size = 2;
            propertyInfo.tax = new BigDecimal(10);

            propertyRecord.propertyInfos.put(address, propertyInfo);

            em.persist(propertyRecord);
            em.flush();
        } catch (RuntimeException e) {
            //test expects flush to throw an exception because the PARCEL_NUMBER field is null
            expectedException = e;
        } finally {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
            closeEntityManager(em);
        }
        this.assertNotNull("Expected an exception persisting null into a field with nullable=false set on an override", expectedException);
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
            List<Comment<String>> comments = new ArrayList(2);
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

            em.flush();

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

            em.flush();

            clearCache(DDL_PU);
            em.refresh(holder);
            assertTrue(holder.getACMap().get(key) != null);

            holder.getACMap().remove(key);
            em.remove(holder);
            em.remove(key);

            em.flush();

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

            em.flush();

            clearCache(DDL_PU);
            em.refresh(holder);
            holder.getOTMMap().get(key);
            assertTrue(holder.getOTMMap().get(key) != null);

            holder.getOTMMap().remove(key);
            value.setHolder(null);
            em.remove(holder);
            em.remove(value);

            em.flush();

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

            em.flush();

            clearCache(DDL_PU);
            em.refresh(holder);
            assertTrue(holder.getUOTMMap().get(4) != null);

            holder.getUOTMMap().remove(4);
            em.remove(holder);
            em.remove(value);

            em.flush();

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

            em.flush();

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

    // Bug 279784 - Incomplete OUTER JOIN based on JoinTable 
    public void testManyToManyWithMultipleJoinColumns() {
        String prefix = "testManyToManyWithMultipleJoinColumns";
        EntityManager em = createEntityManager(DDL_PU);
        beginTransaction(em);

        // persist and flush c1, cache its pk
        CKeyEntityC c1 = new CKeyEntityC(new CKeyEntityCPK(prefix + "_1"));
        c1.setTempRole(prefix + "_1_Role");
        em.persist(c1);
        // that insures that sequencing has worked 
        em.flush();
        CKeyEntityCPK c1KeyPK = c1.getKey(); 

        // create b1 with the same (seq, code) as c1's (seq, role); add to c1
        ArrayList bs1 = new ArrayList();
        CKeyEntityBPK b1KeyPK = new CKeyEntityBPK(c1KeyPK.seq, c1KeyPK.role);
        CKeyEntityB b1 = new CKeyEntityB(b1KeyPK);
        b1.setUnq1(prefix + "_1_1");
        b1.setUnq2(prefix + "_1_2");
        bs1.add(b1);
        c1.setBs(bs1);
        em.persist(b1);

        // create c2 with the same seq as c1, but with different role
        CKeyEntityC c2 = new CKeyEntityC(new CKeyEntityCPK(prefix + "_2"));
        c2.setTempRole(prefix + "_2_Role");
        CKeyEntityCPK c2KeyPK = c2.getKey(); 
        c2KeyPK.seq = c1KeyPK.seq;

        // create b2 with the same (seq, code) as c2's (seq, role); add to c2
        ArrayList bs2 = new ArrayList();
        CKeyEntityBPK b2KeyPK = new CKeyEntityBPK(c2KeyPK.seq, c2KeyPK.role);
        CKeyEntityB b2 = new CKeyEntityB(b2KeyPK);
        b2.setUnq1(prefix + "_2_1");
        b2.setUnq2(prefix + "_2_2");
        bs2.add(b2);
        c2.setBs(bs2);

        // persist c2
        em.persist(c2);
        em.persist(b2);

        commitTransaction(em);
        closeEntityManager(em);

        clearCache(DDL_PU);
        em = createEntityManager(DDL_PU);
        List<CKeyEntityC> clist = em.createQuery("SELECT c from CKeyEntityC c LEFT OUTER JOIN c.bs bs WHERE c.key = :key").setParameter("key", c1KeyPK).getResultList();
        // verify
        String errorMsg = "";
        int nSize = clist.size();
        if(nSize == 1) {
            // Expected result - nothing to do. Correct sql has been generated (the second ON clause is correct):
            // SELECT t1.C_ROLE, t1.SEQ, t1.ROLE_, t1.A_SEQ, t1.A_L_NAME, t1.A_F_NAME 
            // FROM DDL_CKENTC t1 LEFT OUTER JOIN (DDL_CKENT_C_B t2 JOIN DDL_CKENTB t0 ON ((t0.CODE = t2.B_CODE) AND (t0.SEQ = t2.B_SEQ))) ON ((t2.C_ROLE = t1.C_ROLE) AND (t2.C_SEQ = t1.SEQ)) 
            // WHERE ((t1.SEQ = 1) AND (t1.ROLE_ = testManyToManyWithMultipleJoinColumns_1))

        } else if(nSize == 2) {
            if(clist.get(0) != clist.get(1)) {
                errorMsg = "Read 2 cs, but they are not identical - test problem.";
            } else {
                // That wrong sql was generated before the fix (the second ON clause was incorrect):
                // SELECT t1.C_ROLE, t1.SEQ, t1.ROLE_, t1.A_SEQ, t1.A_L_NAME, t1.A_F_NAME 
                // FROM DDL_CKENTC t1 LEFT OUTER JOIN (DDL_CKENT_C_B t2 JOIN DDL_CKENTB t0 ON ((t0.CODE = t2.B_CODE) AND (t0.SEQ = t2.B_SEQ))) ON (t2.C_SEQ = t1.SEQ) 
                // WHERE ((t1.SEQ = 1) AND (t1.ROLE_ = testManyToManyWithMultipleJoinColumns_1))
                // That caused picking up two CKeyEntityB objects instead of one (because they both have the same SEQ), 
                // outer joining causes return of two identical copies of c1.  
                errorMsg = "Read 2 identical cs instead of one - likely the second on clause was incomplete";
            }
        } else {
            errorMsg = "Read "+nSize+" cs, 1 or 2 were expected - test problem.";
        }

        // clean-up
        beginTransaction(em);
        c1 = em.find(CKeyEntityC.class, c1KeyPK);
        c1.getBs().clear();
        em.remove(c1);
        c2 = em.find(CKeyEntityC.class, c2KeyPK);
        c2.getBs().clear();
        em.remove(c2);
        b1 = em.find(CKeyEntityB.class, b1KeyPK);
        em.remove(b1);
        b2 = em.find(CKeyEntityB.class, b2KeyPK);
        em.remove(b2);
        commitTransaction(em);
        closeEntityManager(em);

        if(errorMsg.length() > 0) {
            fail(errorMsg);
        }
    }

    public void testEmbeddedManyToMany(){
        EntityManager em = createEntityManager(DDL_PU);
        try{
            beginTransaction(em);
            Inventor inventor = new Inventor();
            inventor.setId(1);
            Patent patent = new Patent();
            patent.setId(1);
            PatentCollection patents = new PatentCollection();
            patents.getPatents().add(patent);
            inventor.setPatentCollection(patents);
            em.persist(inventor);
            em.persist(patent);
            em.flush();
            em.clear();
            clearCache(DDL_PU);

            inventor = em.find(Inventor.class, 1);
            assertTrue("Embeddable is null", inventor.getPatentCollection() != null);
            assertFalse("Collection is empty", inventor.getPatentCollection().getPatents().isEmpty());
            assertTrue("Target is incorrect", inventor.getPatentCollection().getPatents().get(0).getId() == 1);
            rollbackTransaction(em);
        } finally {
            closeEntityManager(em);
        }
    }

    /**
     * Fix for bug 264417: Table generation is incorrect for JoinTables in 
     * AssociationOverrides. Problem is that non-owning mappings were processed 
     * before the association overrides in turn causing the multiple table
     * generation (owning side - correct, non-owning side - incorrect)
     * This test was added as well to test the fix, also manual inspection of 
     * the DDL was done.
     */
    public void testAssociationOverrideToEmbeddedManyToMany(){
        EntityManager em = createEntityManager(DDL_PU);

        try {
            beginTransaction(em);
            Employee employee = new Employee();
            PhoneNumber phoneNumber = new PhoneNumber();
            employee.addPhoneNumber(phoneNumber);
            em.persist(employee);
            commitTransaction(em);

            // Force the read to hit the database and make sure the phone number is read back.
            clearCache(DDL_PU);
            em.clear();
            assertNotNull("Unable to read back the phone number", em.find(PhoneNumber.class, phoneNumber.getNumber()));
        } catch (Exception e) {
            fail("An error occurred: " + e.getMessage());
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
    
    // Test for bug 277079
    public void testLAZYLOBWithEmbeddedId() {
        EntityManager em = createEntityManager(DDL_PU);
            
        LobtestPK pk = new LobtestPK();
        
        try {
            beginTransaction(em);
                
            Lobtest lobtest = new Lobtest();
            Byte b1 = new Byte("1");
            Byte b2 = new Byte("2");
            lobtest.setContentdata(new byte[]{b1, b2});

            lobtest.setUuid("123456789");
                
            pk.setDocid("blah");
            pk.setVersionid(new BigInteger(new Long(System.currentTimeMillis()).toString()));
            lobtest.setLobtestPK(pk);

            em.persist(lobtest);
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
    
            fail("Error persisting the lobtest : " + e);
        } finally {
            closeEntityManager(em);
        }
            
        clearCache(DDL_PU);
        em = createEntityManager(DDL_PU);
        
        Lobtest refreshedLobtest = em.find(Lobtest.class, pk);
        assertNotNull("The query returned nothing: ", refreshedLobtest);
        assertNotNull("Doc id from Lobtest was null", refreshedLobtest.getLobtestPK().getDocid());
        assertNotNull("Version id from Lobtest was null", refreshedLobtest.getLobtestPK().getVersionid());
        
        closeEntityManager(em);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
