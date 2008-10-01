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


package org.eclipse.persistence.testing.tests.jpa.inherited;

import javax.persistence.EntityManager;

import junit.framework.*;
import junit.extensions.TestSetup;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
import org.eclipse.persistence.testing.models.jpa.inherited.Beer;
import org.eclipse.persistence.testing.models.jpa.inherited.Alpine;
import org.eclipse.persistence.testing.models.jpa.inherited.BeerConsumer;
import org.eclipse.persistence.testing.models.jpa.inherited.SerialNumber;
import org.eclipse.persistence.testing.models.jpa.inherited.InheritedTableManager;
 
@SuppressWarnings("deprecation")
public class InheritedCallbacksJunitTest extends JUnitTestCase {
    private static Integer m_Id;
    
    public InheritedCallbacksJunitTest() {
        super();
    }
    
    public InheritedCallbacksJunitTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName("InheritedCallbacksJunitTest");
        suite.addTest(new InheritedCallbacksJunitTest("testSetup"));
        suite.addTest(new InheritedCallbacksJunitTest("testPreAndPostPersistAlpine"));
        suite.addTest(new InheritedCallbacksJunitTest("testPrePersistAlpineOnMerge"));
        suite.addTest(new InheritedCallbacksJunitTest("testPreAndPostPersistBeerConsumer"));
        suite.addTest(new InheritedCallbacksJunitTest("testPostLoadOnFind"));
        suite.addTest(new InheritedCallbacksJunitTest("testPostLoadOnRefresh"));
        suite.addTest(new InheritedCallbacksJunitTest("testPreAndPostUpdate"));
        suite.addTest(new InheritedCallbacksJunitTest("testPreAndPostRemove"));

        return suite;
    }
    
    /**
     * The setup is done as a test, both to record its failure, and to allow execution in the server.
     */
    public void testSetup() {
        new InheritedTableManager().replaceTables(JUnitTestCase.getServerSession());
        clearCache();
    }
    
    public void testPreAndPostPersistAlpine() {
        int beerPrePersistCount = Beer.BEER_PRE_PERSIST_COUNT;
        int alpinePrePersistCount = Alpine.ALPINE_PRE_PERSIST_COUNT;
        
        Alpine alpine = null;
        EntityManager em = createEntityManager();
        
        try {
            beginTransaction(em);
            SerialNumber serialNumber = new SerialNumber();
            em.persist(serialNumber);
            alpine = new Alpine(serialNumber);
            alpine.setBestBeforeDate(new java.util.Date(2007, 8, 17));
            alpine.setAlcoholContent(5.0);
            
            em.persist(alpine);
            commitTransaction(em);
        } catch (RuntimeException ex) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw ex;
        }
        
        verifyNotCalled(beerPrePersistCount, Beer.BEER_PRE_PERSIST_COUNT, "PrePersist");
        verifyCalled(alpinePrePersistCount, Alpine.ALPINE_PRE_PERSIST_COUNT, "PrePersist");
    }
    
    public void testPrePersistAlpineOnMerge() {
        int beerPrePersistCount = Beer.BEER_PRE_PERSIST_COUNT;
        int alpinePrePersistCount = Alpine.ALPINE_PRE_PERSIST_COUNT;
        
        Alpine alpine = null;
        EntityManager em = createEntityManager();
        
        try {
            beginTransaction(em);
            SerialNumber serialNumber = new SerialNumber();
            em.persist(serialNumber);
            alpine = new Alpine(serialNumber);
            alpine.setBestBeforeDate(new java.util.Date(2007, 8, 17));
            alpine.setAlcoholContent(5.0);
            
            alpine.setClassification(Alpine.Classification.NONE);
            
            Alpine mergedAlpine = em.merge(alpine);
        
            verifyNotCalled(beerPrePersistCount, Beer.BEER_PRE_PERSIST_COUNT, "PrePersist");
            verifyCalled(alpinePrePersistCount, Alpine.ALPINE_PRE_PERSIST_COUNT, "PrePersist");
            assertTrue("The merged alpine classification was not updated from the PrePersist lifecycle method", mergedAlpine.getClassification() == Alpine.Classification.STRONG);
            
            commitTransaction(em);
        } catch (RuntimeException ex) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw ex;
        }
    }
    
    public void testPreAndPostPersistBeerConsumer() {
        BeerConsumer beerConsumer = null;
        EntityManager em = createEntityManager();
        try {
            beginTransaction(em);
            beerConsumer = new BeerConsumer();
            beerConsumer.setName("A consumer to delete eventually");
            em.persist(beerConsumer);
            m_Id = beerConsumer.getId();
        
            commitTransaction(em);
        }catch (RuntimeException ex){
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw ex;
        }
        verifyCalled(0, beerConsumer.pre_persist_count, "PrePersist");
        verifyCalled(0, beerConsumer.post_persist_count, "PostPersist");
    }
    
    public void testPostLoadOnFind() {
        BeerConsumer beerConsumer = createEntityManager().find(BeerConsumer.class, m_Id);
        
        verifyCalled(0, beerConsumer.post_load_count, "PostLoad");
    }
    
    public void testPostLoadOnRefresh() {
        BeerConsumer beerConsumer = null;
        EntityManager em = createEntityManager();
        beginTransaction(em);

        try {
            beerConsumer = em.find(BeerConsumer.class, m_Id);
            em.refresh(beerConsumer);
            
            commitTransaction(em);
        }catch (RuntimeException ex){
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw ex;
        }
        
        verifyCalled(0, beerConsumer.post_load_count, "PostLoad");
    }
    
    public void testPreAndPostUpdate() {
        BeerConsumer beerConsumer = null;
        int count1, count2 = 0;
        EntityManager em = createEntityManager();
        beginTransaction(em);

        try {
            beerConsumer = em.find(BeerConsumer.class, m_Id);
            count1 = beerConsumer.pre_update_count;
            beerConsumer.setName("An updated name");
            count2 = beerConsumer.post_update_count;
            commitTransaction(em);    
        }catch (RuntimeException ex){
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw ex;
        }
        
        verifyCalled(count1, beerConsumer.pre_update_count, "PreUpdate");
        verifyCalled(count2, beerConsumer.post_update_count, "PostUpdate");
    }
    
    public void testPreAndPostRemove() {
        BeerConsumer beerConsumer = null;
        int count1, count2 = 0;
        EntityManager em = createEntityManager();
        beginTransaction(em);

        try {
            beerConsumer = em.find(BeerConsumer.class, m_Id);
            count1 = beerConsumer.pre_remove_count;
            em.remove(beerConsumer);
            count2 = beerConsumer.post_remove_count;
            commitTransaction(em);    
        }catch (RuntimeException ex){
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            closeEntityManager(em);
            throw ex;
        }
        
        verifyCalled(count1, beerConsumer.pre_remove_count, "PreRemove");
        verifyCalled(count2, beerConsumer.post_remove_count, "PostRemove");
    }
    
    public void verifyCalled(int countBefore, int countAfter, String callback) {
        assertFalse("The callback method [" + callback + "] was not called", countBefore == countAfter);
        assertTrue("The callback method [" + callback + "] was called more than once", countAfter == (countBefore + 1));
    }
    
    public void verifyNotCalled(int countBefore, int countAfter, String callback) {
        assertTrue("The callback method [" + callback + "] was called.", countBefore == countAfter);
    }
}
