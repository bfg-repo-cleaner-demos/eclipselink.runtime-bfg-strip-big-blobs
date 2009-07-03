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
package org.eclipse.persistence.testing.tests.jpa.xml.inheritance;

import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import junit.framework.*;

import org.eclipse.persistence.sessions.DatabaseSession;

import org.eclipse.persistence.testing.models.jpa.xml.inheritance.Boat;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.Bus;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.Company;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.SportsCar;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.InheritanceTableCreator;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.InheritanceModelExamples;

import org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.BusListener;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.BusListener2;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.DefaultListener;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.DefaultListener1;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.DefaultListener2;
import org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.DefaultListener3;

import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
 
/**
 * JUnit test case(s) for the TopLink EntityMappingsXMLProcessor.
 */
public class EntityMappingsInheritanceJUnitTestCase extends JUnitTestCase {
    private static Number busId;
    private static Number boatId;
    private static Number sportsCarId;
    
    public EntityMappingsInheritanceJUnitTestCase() {
        super();
    }
    
    public EntityMappingsInheritanceJUnitTestCase(String name) {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite("Inheritance Model");
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testSetup"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testCreateFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testCreateBusFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testCreateNonFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testReadFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testReadNonFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testNamedNativeQueryOnSportsCar"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testUpdateBusFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testUpdateFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testUpdateNonFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testDeleteBusFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testDeleteFueledVehicle"));
        suite.addTest(new EntityMappingsInheritanceJUnitTestCase("testDeleteNonFueledVehicle"));
        
        return suite;
    }
    
    /**
     * The setup is done as a test, both to record its failure, and to allow execution in the server.
     */
    public void testSetup() {
        DatabaseSession session = JUnitTestCase.getServerSession();
        new InheritanceTableCreator().replaceTables(session);
        clearCache();
    }
    
    public void testCreateBusFueledVehicle() {
        int prePersistBusCountBefore = 0;//Bus.PRE_PERSIST_COUNT;
        int postPersistBusCountBefore = 0;//Bus.POST_PERSIST_COUNT;
        
        int prePersistBusListenerCountBefore = BusListener.PRE_PERSIST_COUNT;
        int postPersistBusListenerCountBefore = BusListener.POST_PERSIST_COUNT;
        
        int prePersistBusListener2CountBefore = BusListener2.PRE_PERSIST_COUNT;
        int postPersistBusListener2CountBefore = BusListener2.POST_PERSIST_COUNT;
        
        int prePersistDefaultListenerCountBefore = DefaultListener.PRE_PERSIST_COUNT;
        int postPersistDefaultListenerCountBefore = DefaultListener.POST_PERSIST_COUNT;
        
        EntityManager em = createEntityManager();        
        beginTransaction(em);
        
        Bus bus = new Bus();
        bus.setPassengerCapacity(new Integer(50));
        bus.setFuelCapacity(new Integer(175));
        bus.setDescription("OC Transpo Bus");
        bus.setFuelType("Diesel");
            
        try {
            em.persist(bus);
            busId = bus.getId();
            commitTransaction(em);
        } catch (RuntimeException ex) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            
            closeEntityManager(em);
            throw ex;
        }
        
        assertFalse("The PrePersist callback method on Bus was not called.", prePersistBusCountBefore == bus.pre_persist_count);
        assertFalse("The PostPersist callback method on Bus was not called.", postPersistBusCountBefore == bus.post_persist_count);
        assertFalse("The PrePersist callback method on BusListener was not called.", prePersistBusListenerCountBefore == BusListener.PRE_PERSIST_COUNT);
        assertFalse("The PostPersist callback method on BusListener was not called.", postPersistBusListenerCountBefore == BusListener.POST_PERSIST_COUNT);
        assertFalse("The PrePersist callback method on BusListener2 was not called.", prePersistBusListener2CountBefore == BusListener2.PRE_PERSIST_COUNT);
        assertFalse("The PostPersist callback method on BusListener2 was not called.", postPersistBusListener2CountBefore == BusListener2.POST_PERSIST_COUNT);
        assertFalse("The PrePersist callback method on DefaultListener was not called.", prePersistDefaultListenerCountBefore == DefaultListener.PRE_PERSIST_COUNT);
        assertFalse("The PostPersist callback method on DefaultListener was not called.", postPersistDefaultListenerCountBefore == DefaultListener.POST_PERSIST_COUNT);

        assertTrue("An incorrect number of PrePersist notifications where made for the Bus object.", bus.prePersistCalledListenerCount() == 6);
        assertTrue("An incorrect number of PostPersist notifications where made for the Bus object.", bus.postPersistCalledListenerCount() == 3);

        assertTrue("The PrePersist events were not fired in the correct order.", bus.getPrePersistCalledListenerAt(0) == DefaultListener1.class);
        assertTrue("The PrePersist events were not fired in the correct order.", bus.getPrePersistCalledListenerAt(1) == DefaultListener2.class);
        assertTrue("The PrePersist events were not fired in the correct order.", bus.getPrePersistCalledListenerAt(2) == DefaultListener3.class);

        assertTrue("The PrePersist events were not fired in the correct order.", bus.getPrePersistCalledListenerAt(3) == org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.BusListener2.class);
        assertTrue("The PrePersist events were not fired in the correct order.", bus.getPrePersistCalledListenerAt(4) == org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.BusListener3.class);
        assertTrue("The PrePersist events were not fired in the correct order.", bus.getPrePersistCalledListenerAt(5) == org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.BusListener.class);
        
        assertTrue("The PostPersist events were not fired in the correct order.", bus.getPostPersistCalledListenerAt(0) == org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.BusListener2.class);
        assertTrue("The PostPersist events were not fired in the correct order.", bus.getPostPersistCalledListenerAt(1) == org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.BusListener3.class);
        assertTrue("The PostPersist events were not fired in the correct order.", bus.getPostPersistCalledListenerAt(2) == org.eclipse.persistence.testing.models.jpa.xml.inheritance.listeners.BusListener.class);
    }
    
    public void testCreateFueledVehicle() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        try {
            SportsCar car = (SportsCar) InheritanceModelExamples.sportsCarExample1();
            car.setDescription("Ferrari");
            car.setMaxSpeed(300);
            em.persist(car);
            sportsCarId = car.getId();
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            fail("An exception was caught during create FueledVehicle [SportsCar] operation : " + e.getMessage());
        }finally{
            closeEntityManager(em);
        }

    }

    public void testCreateNonFueledVehicle() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        try {

            Company co = InheritanceModelExamples.companyExample2();
            Boat boat = InheritanceModelExamples.boatExample1(co);
            em.persist(boat);
            boatId = boat.getId();
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            fail("An exception was caught during create NonFueledVehicle [Boat] operation : " + e.getMessage());
        }finally{
            closeEntityManager(em);
        }
    }

    public void testDeleteBusFueledVehicle() {
        int postLoadBusCountBefore = 0;//Bus.POST_LOAD_COUNT;
        int preRemoveBusCountBefore = 0;//Bus.PRE_REMOVE_COUNT;
        int postRemoveBusCountBefore = 0;//Bus.POST_REMOVE_COUNT;

        EntityManager em = createEntityManager();
        beginTransaction(em);
        Bus bus = em.find(Bus.class, busId);
        try {
            em.remove(bus);
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }

            closeEntityManager(em);
            throw e;
        }

        assertTrue("Error deleting FueledVehicle [Bus]", em.find(Bus.class, busId) == null);
        assertFalse("The PostLoad callback method on Bus was not called.", postLoadBusCountBefore == bus.post_load_count);
        assertFalse("The PreRemove callback method on Bus was not called.", preRemoveBusCountBefore == bus.pre_remove_count);
        assertFalse("The PostRemove callback method on Bus was not called.", postRemoveBusCountBefore == bus.post_remove_count);
    }

    public void testDeleteFueledVehicle() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        try {
            em.remove(em.find(SportsCar.class, sportsCarId));
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw e;
        }
        assertTrue("Error deleting FueledVehicle [SportsCar]", em.find(SportsCar.class, sportsCarId) == null);
    }

    public void testDeleteNonFueledVehicle() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        try {
            em.remove(em.find(Boat.class, boatId));
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw e;
        }
        assertTrue("Error deleting NonFueledVehicle [Boat]", em.find(Boat.class, boatId) == null);
    }

    public void testNamedNativeQueryOnSportsCar() {
        Query query = createEntityManager().createNamedQuery("findSQLMaxSpeedForFerrari");
        List results = query.getResultList();
        assertTrue("Failed to return 1 item", (results.size() == 1));

        for (Iterator iterator = results.iterator(); iterator.hasNext(); ){
            Object maxSpeed = iterator.next();
            assertTrue("Failed to return column",(maxSpeed instanceof Number));
            assertTrue("Failed to return correct speed of 300",(((Number)maxSpeed).intValue() == 300));
        }
    }

    public void testReadFueledVehicle() {
        SportsCar car = createEntityManager().find(SportsCar.class, sportsCarId);
        assertTrue("Error reading FueledVehicle [SportsCar]", car.getId() == sportsCarId);
    }

    public void testReadNonFueledVehicle() {
        Boat boat = createEntityManager().find(Boat.class, boatId);
        assertTrue("Error reading NonFueledVehicle [Boat]", boat.getId() == boatId);
    }

    public void testUpdateBusFueledVehicle() {
        int preUpdateBusCountBefore = 0;//Bus.PRE_UPDATE_COUNT;
        int postUpdateBusCountBefore = 0;//Bus.POST_UPDATE_COUNT;

        EntityManager em = createEntityManager();
        beginTransaction(em);

        Bus bus;

        try {
            bus = em.find(Bus.class, busId);
            bus.setDescription("A crappy bus");
            em.merge(bus);
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }

            closeEntityManager(em);
            throw e;
        }

        assertFalse("The PreUpdate callback method on Bus was not called.", preUpdateBusCountBefore == bus.pre_update_count);
        assertFalse("The PostUpdate callback method on Bus was not called.", postUpdateBusCountBefore == bus.post_update_count);

        int postLoadBusCountBefore = bus.post_load_count;
        int postLoadBusCountAfter1;

        beginTransaction(em);
        try {
            bus = em.find(Bus.class, busId);
            // Clear the cache and check that we get a post load (post build internally).
            clearCache();
            em.refresh(bus);
            postLoadBusCountAfter1 = bus.post_load_count;

            // Don't clear the cache and check that we get a post load (post refresh internally).
            em.refresh(bus);
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }

            closeEntityManager(em);
            throw e;
        }

        assertTrue("Error updating FueledVehicle [Bus]", bus.getDescription().equals("A crappy bus"));
        //throw new TestWarningException("This test will not run until bug 262246 is resolved");
        //assertFalse("The PostLoad (on refresh without object in cache) callback method on Bus was not called.", postLoadBusCountBefore == postLoadBusCountAfter1);
        //assertFalse("The PostLoad (on refresh with object in cache) callback method on Bus was not called.", postLoadBusCountAfter1 == bus.post_load_count);
    }

    public void testUpdateFueledVehicle() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        try {
            SportsCar car = em.find(SportsCar.class, sportsCarId);
            car.setDescription("Corvette");
            em.merge(car);
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw e;
        }
        clearCache();
        SportsCar newCar = em.find(SportsCar.class, sportsCarId);
        assertTrue("Error updating FueledVehicle [SportsCar]", newCar.getDescription().equals("Corvette"));
    }

    public void testUpdateNonFueledVehicle() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        try {
            Boat boat = em.find(Boat.class, boatId);
            Company co = boat.getOwner();
            co.setName("XYZ");
            em.merge(boat);
            commitTransaction(em);
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw e;
        }
        clearCache();
        Boat newBoat = em.find(Boat.class, boatId);
        assertTrue("Error updating NonFueledVehicle [Boat]", newBoat.getOwner().getName().equals("XYZ"));
    }

}
