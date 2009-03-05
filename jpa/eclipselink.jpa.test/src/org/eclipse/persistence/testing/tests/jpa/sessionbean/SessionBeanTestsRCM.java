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
*     pvijayaratnam - cache coordination test implementation
 ******************************************************************************/  
 package org.eclipse.persistence.testing.tests.jpa.sessionbean;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.rmi.PortableRemoteObject;

import junit.framework.*;

import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
import org.eclipse.persistence.testing.models.jpa.fieldaccess.advanced.Employee;
import org.eclipse.persistence.testing.models.jpa.fieldaccess.advanced.Address;
import org.eclipse.persistence.testing.models.jpa.fieldaccess.advanced.AdvancedTableCreator;
import org.eclipse.persistence.testing.models.jpa.sessionbean.EmployeeService;

/**
 * EJB 3 SessionBean tests.
 * Testing using EclipseLink JPA in a JEE EJB 3 SessionBean environment.
 * These tests can only be run with a server.
 */
public class SessionBeanTestsRCM extends JUnitTestCase {
    protected EmployeeService service;
	private String wlsUserName;
	private String wlsPassword;
	private String server1Url;
	private String server2Url;
	private String server3Url;
	int empId = 0;
	private Employee employeeCached = null; 	
	
    public SessionBeanTestsRCM() {
        super();
    }

    public SessionBeanTestsRCM(String name) {
        super(name);
    }

    public SessionBeanTestsRCM(String name, boolean shouldRunTestOnServer) {
        super(name);

	   this.getClass().getResource("weblogic.properties");
	    wlsUserName = System.getProperty("server.user");
	    wlsPassword = System.getProperty("server.pwd");
	    server1Url = System.getProperty("rcm.wls.server1.url");    
	    server2Url = System.getProperty("rcm.wls.server2.url");
	    server3Url = System.getProperty("rcm.wls.server3.url");
    }
    
    public static Test suite() {

	    
        TestSuite suite = new TestSuite("SessionBeanTestsRCM");

	suite.addTest(new SessionBeanTestsRCM("testSetupRcmOnServer2", true));
	suite.addTest(new SessionBeanTestsRCM("testSetupForDeleteOnServer2", true));
	
	suite.addTest(new SessionBeanTestsRCM("testSetupRcmOnServer1", true));
	suite.addTest(new SessionBeanTestsRCM("testPerformDeleteOnServer1", true));
	
	//suite.addTest(new SessionBeanTestsRCM("testReadFromServer2AfterUpdateNonJPQL", true));
        suite.addTest(new SessionBeanTestsRCM("testConfirmUpdateOnServer2", true));
        suite.addTest(new SessionBeanTestsRCM("testConfirmDeleteOnServer2", true));
        return suite;
    }
    
    /**
     * The setup is done as a test, both to record its failure, and to alow execution in the server2.
     */
    public void testSetupRcmOnServer2() throws Exception {
	    new AdvancedTableCreator().replaceTables(JUnitTestCase.getServerSession("sessionbean"));
    }

    public EmployeeService getEmployeeService(String url) throws Exception {
        if (service == null) {
            Properties properties = new Properties();
            if (url != null) {
                properties.put("java.naming.provider.url", url);
            }
            Context context = new InitialContext(properties);
    
            try {
                service = (EmployeeService) PortableRemoteObject.narrow(context.lookup("java:comp/env/ejb/EmployeeService"), EmployeeService.class);
            } catch (NameNotFoundException notFoundException) {
                try {
                    service = (EmployeeService) PortableRemoteObject.narrow(context.lookup("ejb/EmployeeService"), EmployeeService.class);
                } catch (NameNotFoundException notFoundException2) {
                    try {
                        // WLS likes this one.
                        service = (EmployeeService) PortableRemoteObject.narrow(context.lookup("EmployeeService#org.eclipse.persistence.testing.models.jpa.sessionbean.EmployeeService"), EmployeeService.class);
                    } catch (NameNotFoundException notFoundException3) {
                        try {
                             //jboss likes this one
                             service = (EmployeeService) PortableRemoteObject.narrow(context.lookup("EmployeeService/remote"), EmployeeService.class);
                        } catch (NameNotFoundException notFoundException4) {
                             throw new Error("All lookups failed.", notFoundException);
                        }
                    }
                }
            }
        }
        return service;
    }    
   

    /* CacheCoordination Delete Test Setup on Server2:
    *  This test insert an employee record, which will be deleted later using Server1
    */
   public void testSetupForDeleteOnServer2() throws Exception {  
       	/* Create an Employee record using Server2 */
        Employee employee = new Employee();
        employee.setFirstName("Jane2");
        employee.setLastName("Doe2");
	employee.setAddress(new Address());
        employee.getAddress().setCity("Ottawa2");
	Employee manager = new Employee();
        manager.setFirstName("John2");
        manager.setLastName("Done2");
        employee.setManager(manager);

        int empID = getEmployeeService(server2Url).insert(employee); 
       
       try{    
        employee = getEmployeeService(server2Url).findById(empID);
	if(employee == null){
        	
          fail("Server2 CacheCoordination Setup Failure: New employee added from Server2 is not found in cache or DB.");
        }
       } catch(Exception e){
	       throw new Error("Following Emp ID not found: " + empID); 
       }      
   }  

    /**
     * CacheCoordination Update Test setup on Server1:
     * This test insert an employee record, which is then updated using Server1. Later on,  this update will be verified on Server2.
     * The setup is done as a test, both to record its failure, and to alow execution in the server1.
     */
    public void testSetupRcmOnServer1() throws Exception {

       	/* Create an Employee record in Server1 */
        Employee employee = new Employee();
        employee.setFirstName("Jane1");
        employee.setLastName("Doe1");
        employee.setAddress(new Address());
        employee.getAddress().setCity("Ottawa1");
	Employee manager = new Employee();
        manager.setFirstName("John1");
        manager.setLastName("Done1");
        employee.setManager(manager);
	
        empId = getEmployeeService(server1Url).insert(employee);	
	
	/* read Employee from cache and/or DB */
	Employee jane1 = (Employee) getEmployeeService(server1Url).findById(empId);
	/* update employee on Server1 */
        jane1.setLastName("LastNameUpdatedOnServer1");
        jane1.getAddress().setCity("newCity");
        getEmployeeService(server1Url).update(jane1);

        if (!jane1.getLastName().equals("LastNameUpdatedOnServer1")) {
		fail("UpdateTest Setup on Server1 failed");
        } 
	
    }
 
     /**
     * CacheCoordination Test setup for Delete on Server1:
     * Find employee created on Server2, then delete it using Server1.
     */
    public void testPerformDeleteOnServer1() throws Exception {
	
	List result = getEmployeeService(server1Url).findByFirstName("Jane2");
	int count = 0;
	for(Iterator i = result.iterator(); i.hasNext();){
		 employeeCached = (Employee) i.next();
	}

	if(employeeCached == null){
        	
          fail("Perform Delete Test failed: New employee was not found in distributed cache to delete");
        }
	
        getEmployeeService(server1Url).delete(employeeCached);   	
    }
    
       
    /* CacheCoordination Test - Verify that Object Update done on Server1 is sync with Server2 thru cache:
    *  This test uses JPQL to read object on Server2.
    */
   public void testConfirmUpdateOnServer2() throws Exception {  
        
        
        /* verify updates are in sync: read Employee from using Server2 URL */
	   
	List result = getEmployeeService(server2Url).findByFirstName("Jane1");
	int count = 0;
	for(Iterator i = result.iterator(); i.hasNext();){
		 employeeCached = (Employee) i.next();
	}
	if(employeeCached == null){
        	
          fail("Object Update Test verification failed: New employee was not found in distributed cache");
        }
        if (!employeeCached.getLastName().equals("LastNameUpdatedOnServer1")) {
		fail("Object Update Test verification failed: Changes from server1 is not seen by server2 from distributed cache");
        } 


   }  
     

   
   /* CacheCoordination Test - Verify that Object Delete done on Server1 is sync with Server2 thru cache:
    *    This test uses JPQL to read object on Server2.
    */
   public void testConfirmDeleteOnServer2() throws Exception {  
      
        /* verify deletes are in sync: read Employee from using Server2 URL */
	   
	List result = getEmployeeService(server2Url).findByFirstName("Jane2");
	int count = 0;
	for(Iterator i = result.iterator(); i.hasNext();){
		 employeeCached = (Employee) i.next();
	}
	if(!( employeeCached == null)){
        	
          fail("Object Delete Test verification failed: employee was not removed from cache as expected" );
        } 

   }   

    /* CacheCoordination Test - Verify changes are on cache and read onto Server2:
    *
    */
/*
   public void testReadFromServer2AfterUpdateNonJPQL() throws Exception {  
        
        
        // read Employee from using Server2 URL 
       ExpressionBuilder employees = new ExpressionBuilder();
       Expression expression = employees.get("firstName").equal("Jane1");
       ReadObjectQuery query = new ReadObjectQuery(Employee.class, expression);            
        
        // verify updates are in sync
	
	query.checkCacheOnly();
	//empID = getEmpID();
        Employee employeeCached = (Employee) (this.getServerSession("sessionbean")).executeQuery(query); 

	if(employeeCached == null){
        	
          fail("Object_Exist_Test: New employee was not added to distributed cache: (SERVER2_URL) "+ (this.getServerSession("sessionbean")).getName() );
        }
        if (!employeeCached.getLastName().equals("LastNameUpdatedOnServer1")) {
		fail("Object_Update_Test: Changes from server1 is not seen by server2 from distributed cache");
        } 

        
   }
*/   

}

