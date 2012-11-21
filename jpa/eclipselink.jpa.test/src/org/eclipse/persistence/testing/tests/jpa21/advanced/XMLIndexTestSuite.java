/*******************************************************************************
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     11/22/2012-2.5 Guy Pelletier 
 *       - 389090: JPA 2.1 DDL Generation Support (index metadata support)
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.jpa21.advanced;

import javax.persistence.EntityManager;

import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
import org.eclipse.persistence.testing.models.jpa21.advanced.xml.ddl.Fork;
import org.eclipse.persistence.testing.models.jpa21.advanced.xml.ddl.ForkUser;

import junit.framework.TestSuite;
import junit.framework.Test;

public class XMLIndexTestSuite extends JUnitTestCase {
    protected static final String XML_DDL_PU = "xml-ddl";
    
    public XMLIndexTestSuite() {}
    
    public XMLIndexTestSuite(String name) {
        super(name);
    }
    
    @Override
    public void setUp () {
        super.setUp();
        clearCache();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName("XMLIndexTestSuite");
        
        suite.addTest(new XMLIndexTestSuite("testDDLPersistenceUnit"));
        
        return suite;
    }
    
    /**
     * Create some forks and users to make sure we don't have any metadata
     * mapping issues.
     */
    public void testDDLPersistenceUnit() {
        EntityManager em = createEntityManager(XML_DDL_PU);
            
        try {
            beginTransaction(em);
            
            Fork fork1 = new Fork();
            fork1.setColor("Black");
            fork1.setStyle("Carving");
            fork1.setPrice(2.99);
            fork1.setRental(0.99);
            em.persist(fork1);
            
            Fork fork2 = new Fork();
            fork2.setColor("Gray");
            fork2.setStyle("Cheese");
            fork2.setPrice(7.99);
            fork2.setRental(1.99);
            em.persist(fork2);
            
            Fork fork3 = new Fork();
            fork3.setColor("Brushed Nickel");
            fork3.setStyle("Chip");
            fork3.setPrice(7.99);
            fork3.setRental(1.99);
            em.persist(fork3);
            
            ForkUser forkUser1 = new ForkUser();
            forkUser1.setName("User1");
            forkUser1.addFork(fork1);
            forkUser1.addFork(fork2);
            forkUser1.addFork(fork3);
            fork1.addUser(forkUser1);
            fork2.addUser(forkUser1);
            fork3.addUser(forkUser1);
            em.persist(forkUser1);
            
            ForkUser forkUser2 = new ForkUser();
            forkUser2.setName("User2");
            forkUser2.addFork(fork2);
            forkUser2.addFork(fork3);;
            fork2.addUser(forkUser2);
            fork3.addUser(forkUser2);
            em.persist(forkUser2);
            
            ForkUser forkUser3 = new ForkUser();
            forkUser3.setName("User3");
            forkUser3.addFork(fork1);
            forkUser3.addFork(fork3);;
            fork1.addUser(forkUser3);
            fork3.addUser(forkUser3);
            em.persist(forkUser3);
                        
            commitTransaction(em);
            
        } catch (RuntimeException e) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
                
            throw e;
        } finally {
            closeEntityManager(em);
        }
    }
}