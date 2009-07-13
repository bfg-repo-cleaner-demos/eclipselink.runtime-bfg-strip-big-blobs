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
 *     tware - testing for delimited identifiers in JPA 2.0
 ******************************************************************************/  

package org.eclipse.persistence.testing.tests.jpa.delimited;

import java.sql.Date;
import java.util.List;

import javax.persistence.EntityManager;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
import org.eclipse.persistence.testing.models.jpa.delimited.*;

/**
 * Test the EntityManager API using a model that uses delimited-identifiers
 */
public class DelimitedPUTestSuite extends JUnitTestCase {
    
    private static Employee emp = null;
    private static Address addr = null;
    private static PhoneNumber pn = null;
    private static Employee emp2 = null;
    private static LargeProject lproj = null;
    private static SmallProject sproj = null;
    
    public DelimitedPUTestSuite() {
        super();
    }
    
    public DelimitedPUTestSuite(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName("DelimitedPUTestSuite");
        suite.addTest(new DelimitedPUTestSuite("testPopulate"));
        suite.addTest(new DelimitedPUTestSuite("testReadEmployee"));
        suite.addTest(new DelimitedPUTestSuite("testNativeQuery"));
        suite.addTest(new DelimitedPUTestSuite("testUpdateEmployee"));
        
        return suite;
    }

    public void testPopulate(){
        // Temporarily disable testing on MySQL until build scripts can be updated to 
        if (getServerSession("delimited").getDatasourcePlatform().isMySQL()){
            return;
        }
        EntityManager em = createEntityManager("delimited");
        beginTransaction(em);
        
        createEmployee();
        createAddress();
        createPhoneNumber();
        createEmployee2();
        createLargeProject();
        createSmallProject();
        
        addr.getEmployees().add(emp);
        emp.setAddress(addr);
        
        emp.addPhoneNumber(pn);
        pn.setOwner(emp);
        
        emp.addManagedEmployee(emp2);
        emp2.setManager(emp);
        
        lproj.setTeamLeader(emp);
        emp.addProject(lproj);
        lproj.addTeamMember(emp2);
        emp2.addProject(lproj);
        
        sproj.setTeamLeader(emp2);
        emp2.addProject(sproj);

        em.persist(emp);
        em.persist(addr);
        em.persist(pn);
        em.persist(emp2);
        em.persist(lproj);
        em.persist(sproj);

        commitTransaction(em);
        em.refresh(pn);
        
        clearCache("delimited");
    }
    
    public void testReadEmployee() {
        // Temporarily disable testing on MySQL until build scripts can be updated to 
        if (getServerSession("delimited").getDatasourcePlatform().isMySQL()){
            return;
        }
        
        EntityManager em = createEntityManager("delimited");

        Employee returnedEmp = (Employee)em.createQuery("select e from Employee e where e.firstName = 'Del' and e.lastName = 'Imited'").getSingleResult();
        Assert.assertTrue("testCreateEmployee emp not properly persisted", getServerSession("delimited").compareObjects(emp, returnedEmp));

        Employee returnedWorker = (Employee)em.createQuery("select e from Employee e where e.firstName = 'Art' and e.lastName = 'Vandeleigh'").getSingleResult();
        Assert.assertTrue("testCreateEmployee emp2 not properly persisted", getServerSession("delimited").compareObjects(emp2, returnedWorker));
    }
    
    public void testNativeQuery(){
        // Temporarily disable testing on MySQL until build scripts can be updated to 
        if (getServerSession("delimited").getDatasourcePlatform().isMySQL()){
            return;
        }
        
        clearCache("delimited");
        EntityManager em = createEntityManager("delimited");
        List result = em.createNamedQuery("findAllSQLEmployees").getResultList();
        Assert.assertTrue("testNativeQuery did not return result ", result.size() == 2);
    }
    
    
    public void testUpdateEmployee() {
        // Temporarily disable testing on MySQL until build scripts can be updated to 
        if (getServerSession("delimited").getDatasourcePlatform().isMySQL()){
            return;
        }
        
        EntityManager em = createEntityManager("delimited");
        em.getTransaction().begin();
        Employee returnedEmp = (Employee)em.createQuery("select e from Employee e where e.firstName = 'Del' and e.lastName = 'Imited'").getSingleResult();

        returnedEmp.setFirstName("Redel");
        PhoneNumber pn = new PhoneNumber();
        pn.setType("home");
        pn.setAreaCode("123");
        returnedEmp.addPhoneNumber(pn);
        returnedEmp.getAddress().setCity("Reident");
        em.flush();
        clearCache("delimited");
        returnedEmp = em.find(Employee.class, returnedEmp.getId());
        Assert.assertTrue("testUpdateEmployee did not properly update firstName", returnedEmp.getFirstName().equals("Redel"));
        Assert.assertTrue("testUpdateEmployee did not properly update address", returnedEmp.getAddress().getCity().equals("Reident"));
        Assert.assertTrue("testUpdateEmployee did not properly add phone number", returnedEmp.getPhoneNumbers().size() == 2);
        em.getTransaction().rollback();
    }
    
    private static Employee createEmployee(){
        emp = new Employee();
        emp.setFirstName("Del");
        emp.setLastName("Imited");
        emp.setFemale();
        emp.addResponsibility("Supervise projects");
        emp.addResponsibility("Delimit Identifiers");
        Date startDate = Date.valueOf("2009-06-01");
        Date endDate = Date.valueOf("2009-08-01");
        EmploymentPeriod period = new EmploymentPeriod(startDate, endDate);
        emp.setPeriod(period);
        return emp;
    }
    
    private static Employee createEmployee2(){
        emp2 = new Employee();
        emp2.setFirstName("Art");
        emp2.setLastName("Vandeleigh");
        emp2.setMale();
        return emp2;
    }
    
    private static Address createAddress(){
        addr = new Address();
        addr.setCity("Ident");
        addr.setCountry("Ifier");
        addr.setPostalCode("A0A1B1");
        addr.setProvince("Delimitia");
        addr.setStreet("Del St.");
        return addr;
    }
    
    private static PhoneNumber createPhoneNumber(){
        pn = new PhoneNumber();
        pn.setAreaCode("709");
        pn.setNumber("5551234");
        pn.setType("work");
        return pn;
    }
    
    private static LargeProject createLargeProject(){
        lproj = new LargeProject();
        lproj.setBudget(10000000);
        lproj.setDescription("Allow delimited identifiers in persistence.xml");
        lproj.setName("PUDefaults");
        return lproj;
    }
    
    private static SmallProject createSmallProject(){
        sproj = new SmallProject();
        sproj.setDescription("Allow delimited identifiers in annotations");
        sproj.setName("Annotations");
        return sproj;
    }
    
}
