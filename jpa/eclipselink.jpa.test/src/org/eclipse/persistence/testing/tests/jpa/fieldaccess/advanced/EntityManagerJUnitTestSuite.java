/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  


package org.eclipse.persistence.testing.tests.jpa.fieldaccess.advanced;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.Vector;
import java.util.Iterator;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;
import javax.persistence.OptimisticLockException;
import javax.persistence.RollbackException;

import junit.extensions.TestSetup;
import junit.framework.*;

import org.eclipse.persistence.jpa.config.CacheUsage;
import org.eclipse.persistence.jpa.config.PessimisticLock;
import org.eclipse.persistence.jpa.config.PersistenceUnitProperties;
import org.eclipse.persistence.jpa.config.EclipseLinkQueryHints;
import org.eclipse.persistence.internal.jpa.EJBQueryImpl;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.queries.ObjectLevelReadQuery;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.tools.schemaframework.SchemaManager;
import org.eclipse.persistence.sessions.server.ReadConnectionPool;
import org.eclipse.persistence.sessions.server.ServerSession;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.tools.schemaframework.OracleSequenceDefinition;
import org.eclipse.persistence.exceptions.QueryException;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.changetracking.ChangeTracker;
import org.eclipse.persistence.internal.descriptors.PersistenceEntity;
import org.eclipse.persistence.internal.jpa.EntityManagerImpl;
import org.eclipse.persistence.internal.weaving.PersistenceWeaved;
import org.eclipse.persistence.internal.weaving.PersistenceWeavedLazy;
import org.eclipse.persistence.queries.FetchGroupTracker;

import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
import org.eclipse.persistence.testing.framework.junit.JUnitTestCaseHelper;
import org.eclipse.persistence.testing.models.jpa.fieldaccess.advanced.*;

/**
 * Test the EntityManager API using the advanced model.
 */
public class EntityManagerJUnitTestSuite extends JUnitTestCase {
        
    public EntityManagerJUnitTestSuite() {
        super();
    }
    
    public EntityManagerJUnitTestSuite(String name) {
        super(name);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(EntityManagerJUnitTestSuite.class);

        return new TestSetup(suite) {
            protected void setUp(){
                SchemaManager schemaManager = new SchemaManager(JUnitTestCase.getServerSession("fieldaccess"));
                new AdvancedTableCreator().replaceTables(JUnitTestCase.getServerSession("fieldaccess"), schemaManager);
            }

            protected void tearDown() {
                clearCache("fieldaccess");
            }
        };
    }
    
    // JUnit framework will automatically execute all methods starting with test...    
    public void testRefreshNotManaged() {
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        Employee emp = new Employee();
        emp.setFirstName("testRefreshNotManaged");
        try {
            em.refresh(emp);
            fail("entityManager.refresh(notManagedObject) didn't throw exception");
        } catch (IllegalArgumentException illegalArgumentException) {
            // expected behaviour
        } catch (Exception exception ) {
            fail("entityManager.refresh(notManagedObject) threw a wrong exception: " + exception.getMessage());
        } finally {
            em.getTransaction().rollback();
            em.close();
        }
    }

    public void testRefreshRemoved() {
        // find an existing or create a new Employee
        String firstName = "testRefreshRemoved";
        Employee emp;
        EntityManager em = createEntityManager("fieldaccess");
        List result = em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.firstName = '"+firstName+"'").getResultList();
        if(!result.isEmpty()) {
            emp = (Employee)result.get(0);
        } else {
            emp = new Employee();
            emp.setFirstName(firstName);
            // persist the Employee
            try{
                em.getTransaction().begin();
                em.persist(emp);
                em.getTransaction().commit();
            }catch (RuntimeException ex){
                if (em.getTransaction().isActive()){
                    em.getTransaction().rollback();
                }
                throw ex;
            }
        }
        
        try{
            em.getTransaction().begin();
            emp = em.find(Employee.class, emp.getId());
            
            // delete the Employee from the db
            em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();

            // refresh the Employee - should fail with EntityNotFoundException
            em.refresh(emp);
            fail("entityManager.refresh(removedObject) didn't throw exception");
        } catch (EntityNotFoundException entityNotFoundException) {
            em.getTransaction().rollback();
            // expected behaviour
        } catch (Exception exception ) {
            em.getTransaction().rollback();
            fail("entityManager.refresh(removedObject) threw a wrong exception: " + exception.getMessage());
        }
    }

    public void testCacheUsage() {
        EntityManager em = createEntityManager("fieldaccess");
        Employee emp = new Employee();
        emp.setFirstName("Mark");
        // persist the Employee
        try {
            em.getTransaction().begin();
            em.persist(emp);
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        }
    	clearCache("fieldaccess");
        // Create new entity manager to avoid extended uow of work cache hits.
        em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        List result = em.createQuery("SELECT OBJECT(e) FROM Employee e").getResultList();
        em.getTransaction().commit();
        Object obj = ((org.eclipse.persistence.jpa.JpaEntityManager)em).getServerSession().getIdentityMapAccessor().getFromIdentityMap(result.get(0));
        assertTrue("Failed to load the object into the shared cache when there were no changes in the UOW", obj != null);
        try{
            em.getTransaction().begin();
            emp = em.find(Employee.class, emp.getId());
            em.remove(emp);
            em.getTransaction().commit();
        } catch (RuntimeException exception) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw exception;
        }
    }
    
    public void testContainsRemoved() {
        // find an existing or create a new Employee
        String firstName = "testContainsRemoved";
        Employee emp;
        EntityManager em = createEntityManager("fieldaccess");
        List result = em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.firstName = '"+firstName+"'").getResultList();
        if(!result.isEmpty()) {
            emp = (Employee)result.get(0);
        } else {
            emp = new Employee();
            emp.setFirstName(firstName);
            // persist the Employee
            try{
                em.getTransaction().begin();
                em.persist(emp);
                em.getTransaction().commit();
            }catch (RuntimeException ex){
                if (em.getTransaction().isActive()){
                    em.getTransaction().rollback();
                }
                throw ex;
            }
        }
        
        boolean containsRemoved = true;
        try{
            em.getTransaction().begin();
            emp = em.find(Employee.class, emp.getId());
            em.remove(emp);
            containsRemoved = em.contains(emp);
            em.getTransaction().commit();
        }catch (RuntimeException t){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw t;
        }
        
        assertFalse("entityManager.contains(removedObject)==true ", containsRemoved);
    }

    public void testFlushModeEmAutoQueryCommit() {
        internalTestFlushMode(FlushModeType.AUTO, FlushModeType.COMMIT);
    }
    
    public void testFlushModeEmAuto() {
        internalTestFlushMode(FlushModeType.AUTO, null);
    }
    
    public void testFlushModeEmAutoQueryAuto() {
        internalTestFlushMode(FlushModeType.AUTO, FlushModeType.AUTO);
    }
    
    public void testFlushModeEmCommitQueryCommit() {
        internalTestFlushMode(FlushModeType.COMMIT, FlushModeType.COMMIT);
    }
    
    public void testFlushModeEmCommit() {
        internalTestFlushMode(FlushModeType.COMMIT, null);
    }
    
    public void testFlushModeEmCommitQueryAuto() {
        internalTestFlushMode(FlushModeType.COMMIT, FlushModeType.AUTO);
    }
    
    public void internalTestFlushMode(FlushModeType emFlushMode, FlushModeType queryFlushMode) {
        // create a new Employee
        String firstName = "testFlushMode";

        // make sure no Employee with the specified firstName exists.
        EntityManager em = createEntityManager("fieldaccess");
        try{
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw ex;
        }
        clearCache("fieldaccess");
        
        Employee emp;
        Query query = em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.firstName like '"+firstName+"'");
        if(queryFlushMode != null) {
            query.setFlushMode(queryFlushMode);
        }
        FlushModeType emFlushModeOriginal = em.getFlushMode();
        em.setFlushMode(emFlushMode);

        // create a new Employee
        emp = new Employee();
        emp.setFirstName(firstName);
        boolean flushed = true;
        Employee result = null;
        try{
            em.getTransaction().begin();
            em.persist(emp);
            result = (Employee) query.getSingleResult();
        } catch (javax.persistence.NoResultException ex) {
            // failed to flush to database
            flushed = false;
        } finally {
            em.getTransaction().rollback();
            em.setFlushMode(emFlushModeOriginal);
        }
        
        boolean shouldHaveFlushed;
        if(queryFlushMode != null) {
            shouldHaveFlushed = queryFlushMode == FlushModeType.AUTO;
        } else {
            shouldHaveFlushed = emFlushMode == FlushModeType.AUTO;
        }
        if(shouldHaveFlushed != flushed) {
            if(flushed) {
                fail("Flushed to database");
            } else {
                fail("Failed to flush to database");
            }
        }
        
    }

    public void testFlushModeOnUpdateQuery() {
        // find an existing or create a new Employee
        String firstName = "testFlushModeOnUpdateQuery";
        Employee emp;
        EntityManager em = createEntityManager("fieldaccess");
        Query readQuery = em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.phoneNumbers IS EMPTY and e.firstName like '"+firstName+"'");
        Query updateQuery = em.createQuery("UPDATE Employee e set e.salary = 100 where e.firstName like '" + firstName + "'");
        updateQuery.setFlushMode(FlushModeType.AUTO);
        emp = new Employee();
        emp.setFirstName(firstName);
        try{
            try{
                em.getTransaction().begin();
                em.persist(emp);
                updateQuery.executeUpdate();
                Employee result = (Employee) readQuery.getSingleResult();
            }catch (javax.persistence.EntityNotFoundException ex){
                em.getTransaction().rollback();
                fail("Failed to flush to database");
            }
            em.refresh(emp);
            assertTrue("Failed to flush to Database", emp.getSalary() == 100);
            em.remove(emp);
            em.getTransaction().commit();
        }catch(RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw ex;
        }
    }

    public void testSetRollbackOnly(){
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try{
            Employee emp = new Employee();
            emp.setFirstName("Bob");
            emp.setLastName("Fisher");
            em.persist(emp);
            emp = new Employee();
            emp.setFirstName("Anthony");
            emp.setLastName("Walace");
            em.persist(emp);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        clearCache("fieldaccess");
        EntityManager localEm = createEntityManager("fieldaccess");
        localEm.getTransaction().begin();
        List result = localEm.createQuery("SELECT e FROM Employee e").getResultList();
        Employee emp = (Employee)result.get(0);
        Employee emp2 = (Employee)result.get(1);
        String newName = ""+System.currentTimeMillis();
        emp2.setFirstName(newName);
        localEm.flush();
        emp2.setLastName("Whatever");
        emp2.setVersion(0);
        try{
            localEm.flush();
        }catch (Exception ex){
            localEm.clear(); //prevent the flush again
            String eName = (String)localEm.createQuery("SELECT e.firstName FROM Employee e where e.id = " + emp2.getId()).getSingleResult();
            assertTrue("Failed to keep txn open for set RollbackOnly", eName.equals(newName));
        }
        try{
            assertTrue("Failed to mark txn rollback only", localEm.getTransaction().getRollbackOnly());
        }finally{
            try{
                localEm.getTransaction().commit();
            }catch (RollbackException ex){
                return;    
            }catch (RuntimeException ex){
                localEm.getTransaction().rollback();
                throw ex;
                
            }
        }
        fail("Failed to throw rollback exception");
    }
    
    public void testSubString() {
        // find an existing or create a new Employee
        String firstName = "testSubString";
        Employee emp;
        EntityManager em = createEntityManager("fieldaccess");
        List result = em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.firstName = '"+firstName+"'").getResultList();
        if(!result.isEmpty()) {
            emp = (Employee)result.get(0);
        } else {
            emp = new Employee();
            emp.setFirstName(firstName);
            // persist the Employee
            try{
                em.getTransaction().begin();
                em.persist(emp);
                em.getTransaction().commit();
            }catch (RuntimeException ex){
                if (em.getTransaction().isActive()){
                    em.getTransaction().rollback();
                }
                throw ex;
            }
        }
        
        int firstIndex = 1;
        int lastIndex = firstName.length();
        List employees = em.createQuery("SELECT object(e) FROM Employee e where e.firstName = substring(:p1, :p2, :p3)").
            setParameter("p1", firstName).
            setParameter("p2", new Integer(firstIndex)).
            setParameter("p3", new Integer(lastIndex)).
            getResultList();
            
        // clean up
        try{
            em.getTransaction().begin();
            em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw ex;
        }

        assertFalse("employees.isEmpty()==true ", employees.isEmpty());
    }
    
    public void testDatabaseSyncNewObject() {
        EntityManager em = createEntityManager("fieldaccess");

        em.getTransaction().begin();

        try{
            Project project = new LargeProject();
            em.persist(project);
            project.setName("Blah");
            project.setTeamLeader(new Employee());
            project.getTeamLeader().addProject(project);
            em.flush();
        }catch (IllegalStateException ex){
            em.getTransaction().rollback();
            return;
        }
        
        fail("Failed to throw illegal argument when finding unregistered new object cascading on database sync");

    }

    public void testTransactionRequired() {
        String firstName = "testTransactionRequired";
        Employee emp = new Employee();
        emp.setFirstName(firstName);
        
        String noException = "";
        String wrongException = "";
        
        try {
            createEntityManager("fieldaccess").flush();
            noException = noException + " flush;";
        } catch (TransactionRequiredException transactionRequiredException) {
            // expected behaviour
        } catch (RuntimeException ex) {
            wrongException = wrongException + " flush: " + ex.getMessage() +";";
        }

        String errorMsg = "";
        if(noException.length() > 0) {
            errorMsg = "No exception thrown: " + noException;
        }
        if(wrongException.length() > 0) {
            if(errorMsg.length() > 0) {
                errorMsg = errorMsg + " ";
            }
            errorMsg = errorMsg + "Wrong exception thrown: " + wrongException;
        }
        if(errorMsg.length() > 0) {
            fail(errorMsg);
        }
    }
    
    public void testIdentityInsideTransaction() {
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        
        Query query = em.createQuery("SELECT e FROM PhoneNumber e");
        List<PhoneNumber> phoneNumbers = query.getResultList();
        for (PhoneNumber phoneNumber : phoneNumbers) {
            Employee emp = phoneNumber.getOwner();
            Collection<PhoneNumber> numbers = emp.getPhoneNumbers();
            assertTrue(numbers.contains(phoneNumber));
        }
        
        em.getTransaction().commit();
        em.close();
    }

    public void testIdentityOutsideTransaction() {
        EntityManager em = createEntityManager("fieldaccess");
        
        Query query = em.createQuery("SELECT e FROM PhoneNumber e");
        List<PhoneNumber> phoneNumbers = query.getResultList();
        for (PhoneNumber phoneNumber : phoneNumbers) {
            Employee emp = phoneNumber.getOwner();
            Collection<PhoneNumber> numbers = emp.getPhoneNumbers();
            assertTrue(numbers.contains(phoneNumber));
        }
        
        em.close();
    } 
    
    public void testIgnoreRemovedObjectsOnDatabaseSync() {
        EntityManager em = createEntityManager("fieldaccess");
        Query phoneQuery = em.createQuery("Select p from PhoneNumber p where p.owner.lastName like 'Dow%'");
        Query empQuery = em.createQuery("Select e FROM Employee e where e.lastName like 'Dow%'");

        em.getTransaction().begin();
        //--setup
        try{
            Employee emp = new Employee();
            emp.setLastName("Dowder");
            PhoneNumber phone = new PhoneNumber("work", "613", "5555555");
            emp.addPhoneNumber(phone);
            phone = new PhoneNumber("home", "613", "4444444");
            emp.addPhoneNumber(phone);
            Address address = new Address("SomeStreet", "somecity", "province", "country", "postalcode");
            emp.setAddress(address);
            em.persist(emp);
            em.flush();
    
            emp = new Employee();
            emp.setLastName("Dows");
            phone = new PhoneNumber("work", "613", "2222222");
            emp.addPhoneNumber(phone);
            phone = new PhoneNumber("home", "613", "1111111");
            emp.addPhoneNumber(phone);
            address = new Address("street1", "city1", "province1", "country1", "postalcode1");
            emp.setAddress(address);
            em.persist(emp);
            em.flush();
            //--end setup
    
            List<Employee> emps = empQuery.getResultList();
    
            List phones = phoneQuery.getResultList();
            for (Iterator iterator = phones.iterator(); iterator.hasNext();){
                em.remove(iterator.next());
            }
            em.flush();
            
            for (Iterator<Employee> iterator = emps.iterator(); iterator.hasNext();){
                em.remove(iterator.next());
            }
        }catch (RuntimeException ex){
            em.getTransaction().rollback();
            throw ex;
        }
        try{
            em.flush();
        }catch (IllegalStateException ex){
            em.getTransaction().rollback();
            em.close();
            em = createEntityManager("fieldaccess");
            em.getTransaction().begin();
            try{
                phoneQuery = em.createQuery("Select p from PhoneNumber p where p.owner.lastName like 'Dow%'");
                empQuery = em.createQuery("Select e FROM Employee e where e.lastName like 'Dow%'");
                List<Employee> emps =  empQuery.getResultList();
                List phones = phoneQuery.getResultList();
                for (Iterator iterator = phones.iterator(); iterator.hasNext();){
                    em.remove(iterator.next());
                }
                for (Iterator<Employee> iterator = emps.iterator(); iterator.hasNext();){
                    em.remove(iterator.next());
                }
                em.getTransaction().commit();
            }catch (RuntimeException re){
                if (em.getTransaction().isActive()){
                    em.getTransaction().rollback();
                }
                throw re;
            }
            fail("Failed to ignore the removedobject when cascading on database sync");
        }
        
        em.getTransaction().commit();
    }
    
    public void testREADLock(){
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        Employee employee = null;
        try{
            employee = new Employee();
            employee.setFirstName("Mark");
            employee.setLastName("Madsen");
            em.persist(employee);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        EntityManager em2 = createEntityManager("fieldaccess");
        Exception optimisticLockException = null;
       
        em.getTransaction().begin();
        try{
            employee = em.find(Employee.class, employee.getId());
            em.lock(employee, LockModeType.READ);
            em2.getTransaction().begin();
            try{
                Employee employee2 = em2.find(Employee.class, employee.getId());
                employee2.setFirstName("Michael");
                em2.getTransaction().commit();
                em2.close();
            }catch (RuntimeException ex){
                em2.getTransaction().rollback();
                em2.close();
                throw ex;
            }
        
            try{
                em.flush();
            } catch (PersistenceException exception) {
                if (exception instanceof OptimisticLockException){
                    optimisticLockException = exception;
                }else{
                    throw exception;
                }
            }
            em.getTransaction().rollback();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        em.getTransaction().begin();
        try{
            employee = em.find(Employee.class, employee.getId());
            em.remove(employee);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        
        if (optimisticLockException == null){
            fail("Proper exception not thrown when EntityManager.lock(object, READ) is used.");
        }
    }
    
    // test for bug 4676587: 
    // CTS: AFTER A REMOVE THEN A PERSIST ON THE SAME ENTITY, CONTAINS RETURNS FALSE
    // The test performs persist, remove, persist sequence on a single object
    // in different "flavours":
    // doTransaction - the first persist happens in a separate transaction;
    // doFirstFlush - perform flush after the first persist;
    // doSecondFlush - perform flush after the remove;
    // doThirdFlush - perform flush after the second persist;
    // doRollback - rollbacks transaction that contains remove and the second persist.
    public void testPersistRemoved() {
        // create an Employee
        String firstName = "testPesistRemoved";
        Employee emp = new Employee();
        emp.setFirstName(firstName);

        // make sure no Employee with the specified firstName exists.
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try{
            em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }

        String errorMsg = "";
        for (int i=0; i < 32; i++) {
            int j = i;
            boolean doRollback = j % 2 == 0;
            j = j/2;
            boolean doThirdFlush = j % 2 == 0;
            j = j/2;
            boolean doSecondFlush = j % 2 == 0;
            j = j/2;
            boolean doFirstFlush = j % 2 == 0;
            j = j/2;
            boolean doTransaction = j % 2 == 0;
            if(doTransaction && doFirstFlush) {
                continue;
            }
            String msg = "";
            if(doTransaction) {
                msg = "Transaction ";
            }
            if(doFirstFlush) {
                msg = msg + "firstFlush ";
            }
            if(doSecondFlush) {
                msg = msg + "secondFlush ";
            }
            if(doThirdFlush) {
                msg = msg + "thirdFlush ";
            }
            if(doRollback) {
                msg = msg + "RolledBack ";
            }

            String localErrorMsg = msg;
            boolean exceptionWasThrown = false;
            Integer empId = null;
            em.getTransaction().begin();            
            try {
                emp = new Employee();
                emp.setFirstName(firstName);

                // persist the Employee
                em.persist(emp);
                if(doTransaction) {
                    em.getTransaction().commit();
                    empId = emp.getId();
                    em.getTransaction().begin();
                } else {
                    if(doFirstFlush) {
                        em.flush();
                    }
                }
        
                if(doTransaction) {
                    emp = em.find(Employee.class, empId);
                }
                // remove the Employee
                em.remove(emp);
                if(doSecondFlush) {
                    em.flush();
                }
        
                // persist the Employee
                em.persist(emp);
                if(doThirdFlush) {
                    em.flush();
                }
            } catch (RuntimeException ex) {
                em.getTransaction().rollback();
                localErrorMsg = localErrorMsg + " " + ex.getMessage() + ";";
                exceptionWasThrown = true;
            }
        
            boolean employeeShouldExist = doTransaction || !doRollback;
            boolean employeeExists = false;
            try{
                if(!exceptionWasThrown) {
                    if(doRollback) {
                        em.getTransaction().rollback();
                    } else {
                        em.getTransaction().commit();
                    }
                    
                    if(doTransaction) {
                        Employee employeeReadFromCache = (Employee)em.find(Employee.class, empId);
                        if(employeeReadFromCache == null) {
                            localErrorMsg = localErrorMsg + " employeeReadFromCache == null;";
                        }
                    }
                    
                    List resultList = em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.firstName = '"+firstName+"'").getResultList();
                    employeeExists = resultList.size() > 0;
                    
                    if(employeeShouldExist) {
                        if(resultList.size() > 1) {
                            localErrorMsg = localErrorMsg + " resultList.size() > 1";
                        }
                        if(!employeeExists) {
                            localErrorMsg = localErrorMsg + " employeeReadFromDB == null;";
                        }
                    } else {
                        if(resultList.size() > 0) {
                            localErrorMsg = localErrorMsg + " employeeReadFromDB != null;";
                        }
                    }
                }
            }catch (RuntimeException ex){
                if (em.getTransaction().isActive()){
                    em.getTransaction().rollback();
                }
                throw ex;
            }
            
            // clean up
            if(employeeExists || exceptionWasThrown) {
                em = createEntityManager("fieldaccess");
                em.getTransaction().begin();
                try{
                    em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
                    em.getTransaction().commit();
                }catch (RuntimeException ex){
                    em.getTransaction().rollback();
                    throw ex;
                }
            }
            
            if(!msg.equals(localErrorMsg)) {
                errorMsg = errorMsg + "i="+Integer.toString(i)+": "+ localErrorMsg + " ";
            }
        }
        if(errorMsg.length() > 0) {
            fail(errorMsg);
        }
    }

    public void testPersistManagedException(){       
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        Employee emp = new Employee();
        em.persist(emp);
        em.flush();
        Integer id = emp.getId();
        emp = new Employee();
        emp.setId(id);
        boolean caughtException = false;
        try{
            em.persist(emp);
        } catch (EntityExistsException e){
            caughtException = true;
        }
        emp = em.find(Employee.class, id);
        em.remove(emp);
        em.getTransaction().rollback();
        assertTrue("EntityExistsException was not thrown for an existing Employee.", caughtException);
    }
    
    public void testPersistManagedNoException(){       
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        Employee emp = new Employee();
        em.persist(emp);
        em.flush();
        Integer id = emp.getId();
        Address address = new Address();
        emp.setAddress(address);
        boolean caughtException = false;
        try{
            em.persist(emp);
        } catch (EntityExistsException e){
            caughtException = true;
        }
        emp = em.find(Employee.class, id);
        em.remove(emp);
        em.getTransaction().commit();
        assertFalse("EntityExistsException was thrown for a registered Employee.", caughtException);
    }

    // test for bug 4676587: 
    // CTS: AFTER A REMOVE THEN A PERSIST ON THE SAME ENTITY, CONTAINS RETURNS FALSE
    public void testRemoveFlushPersistContains() {
        // create an Employee
        String firstName = "testRemoveFlushPersistContains";
        Employee emp = new Employee();
        emp.setFirstName(firstName);

        // make sure no Employee with the specified firstName exists.
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try{
            em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }

        // persist
        em.getTransaction().begin();
        try{
            em.persist(emp);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        // remove, flush, persist, contains
        boolean contains = false;
        em.getTransaction().begin();
        try{
            emp = em.find(Employee.class, emp.getId());
            em.remove(emp); 
            em.flush(); 
            em.persist(emp); 
            contains = em.contains(emp);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        
        // clean up
        em.getTransaction().begin();
        try{
            em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        
        assertTrue("contains==false", contains);
    }
    
    // test for bug 4742161: 
    // CTS: OBJECTS REMOVED AND THEN FLUSHED ARE RETURNED BY QUERIES AFTER THE FLUSH
    public void testRemoveFlushFind() {
        // create an Employee
        String firstName = "testRemoveFlushFind";
        Employee emp = new Employee();
        emp.setFirstName(firstName);

        // make sure no Employee with the specified firstName exists.
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try{
            em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }

        // persist
        em.getTransaction().begin();
        try{
            em.persist(emp);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        
        // remove, flush, persist, contains
        boolean foundAfterFlush = true;
        boolean foundBeforeFlush = true;
        em.getTransaction().begin();
        try{
            emp = em.find(Employee.class, emp.getId());
            em.remove(emp); 
            Employee empFound = em.find(Employee.class, emp.getId());
            foundBeforeFlush = empFound != null;
            em.flush(); 
            empFound = em.find(Employee.class, emp.getId());
             foundAfterFlush = empFound != null;
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        
        // clean up
        em.getTransaction().begin();
        try{
            em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        
        assertFalse("removed object found", foundBeforeFlush);
        assertFalse("removed object found after flush", foundAfterFlush);
    }
    
    // test for bug 4681287: 
    // CTS: EXCEPTION EXPECTED ON FIND() IF PK PASSED IN != ATTRIBUTE TYPE
    public void testFindWithWrongTypePk() {
        EntityManager em = createEntityManager("fieldaccess");
        try {
            em.find(Employee.class, "1");
        } catch (IllegalArgumentException ilEx) {
            return;
        } catch (Exception ex) {
            fail("Wrong exception thrown: " + ex.getMessage());
            return;
        }finally{
            em.close();
        }
        fail("No exception thrown");
    }
    
    //test for gf721 - IllegalArgumentException expected for null PK
    public void testFindWithNullPk() {
        EntityManager em = createEntityManager("fieldaccess");
        try {
            em.find(Employee.class, null);
        } catch (IllegalArgumentException iae) {
            return;
        } catch (Exception e) {
            fail("Wrong exception type thrown: " + e.getClass());
        }finally{
            em.close();
        }
        fail("No exception thrown when null PK used in find operation.");
    }

    public void testCheckVersionOnMerge() {
        Employee employee = new Employee();
        employee.setFirstName("Marc");
        
        EntityManager em = createEntityManager("fieldaccess");
        try{
            em.getTransaction().begin();
            em.persist(employee);
            em.getTransaction().commit();
            em.clear();
            em.getTransaction().begin();
            Employee empClone = (Employee) em.find(Employee.class, employee.getId());
            empClone.setFirstName("Guy");
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            fail("Exception caught during test setup " + ex);
        }
        
        try {
            em.getTransaction().begin();
            em.merge(employee);
            em.getTransaction().commit();
        } catch (OptimisticLockException e) {
            em.getTransaction().rollback();
            em.close();
            return;
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            fail("Wrong exception thrown: " + ex.getMessage());
        }
            
        fail("No exception thrown");
    }
    
    public void testClear(){
        Employee employee = new Employee();
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try{
            em.persist(employee);
            em.getTransaction().commit();
            em.clear();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        boolean cleared = !em.contains(employee);
        em.close();
        assertTrue("EntityManager not properly cleared", cleared);
    }
    
    public void testClearWithFlush(){
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try{
            Employee emp = new Employee();
            emp.setFirstName("Douglas");
            emp.setLastName("McRae");
            em.persist(emp);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        clearCache("fieldaccess");
        EntityManager localEm = createEntityManager("fieldaccess");
        localEm.getTransaction().begin();
        Employee emp = null;
        String originalName = "";
        boolean cleared, updated, reset = false;
        try{
            Query query = localEm.createQuery("Select e FROM Employee e where e.firstName is not null");
            emp = (Employee)query.getResultList().get(0);
            originalName = emp.getFirstName();
            emp.setFirstName("Bobster");
            localEm.flush();
            localEm.clear();
            //this test is testing the cache not the database
            localEm.getTransaction().commit();
            cleared = !localEm.contains(emp);
            emp = localEm.find(Employee.class, emp.getId());
            updated = emp.getFirstName().equals("Bobster");
            localEm.close();
        }catch (RuntimeException ex){
            localEm.getTransaction().rollback();
            localEm.close();
            throw ex;
        }finally{
            //now clean up
            localEm = createEntityManager("fieldaccess");
            localEm.getTransaction().begin();
            emp = localEm.find(Employee.class, emp.getId());
            emp.setFirstName(originalName);
            localEm.getTransaction().commit();
            emp = localEm.find(Employee.class, emp.getId());
            reset = emp.getFirstName().equals(originalName);
            localEm.close();
        }
        assertTrue("EntityManager not properly cleared", cleared);
        assertTrue("flushed data not merged", updated);
        assertTrue("unable to reset", reset);
    }
    
    public void testClearInTransaction(){
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try{
            Employee emp = new Employee();
            emp.setFirstName("Tommy");
            emp.setLastName("Marsh");
            em.persist(emp);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        clearCache("fieldaccess");
        EntityManager localEm = createEntityManager("fieldaccess");
        localEm.getTransaction().begin();
        Employee emp = null;
        String originalName = "";
        try{
            Query query = localEm.createQuery("Select e FROM Employee e where e.firstName is not null");
            emp = (Employee)query.getResultList().get(0);
            originalName = emp.getFirstName();
            emp.setFirstName("Bobster");
            localEm.clear();
            localEm.getTransaction().commit();
        }catch (RuntimeException ex){
            localEm.getTransaction().rollback();
            localEm.close();
            throw ex;
        }
        boolean cleared = !localEm.contains(emp);
        emp = localEm.find(Employee.class, emp.getId());
        localEm.close();
        assertTrue("EntityManager not properly cleared", cleared);
        assertTrue("Employee was updated although EM was cleared", emp.getFirstName().equals(originalName));
    }
    
    public void testExtendedPersistenceContext() {
        String firstName = "testExtendedPersistenceContext";
        int originalSalary = 0;

        Employee empNew = new Employee();
        empNew.setFirstName(firstName);
        empNew.setLastName("new");
        empNew.setSalary(originalSalary);
        
        Employee empToBeRemoved = new Employee();
        empToBeRemoved.setFirstName(firstName);
        empToBeRemoved.setLastName("toBeRemoved");
        empToBeRemoved.setSalary(originalSalary);
        
        Employee empToBeRefreshed = new Employee();
        empToBeRefreshed.setFirstName(firstName);
        empToBeRefreshed.setLastName("toBeRefreshed");
        empToBeRefreshed.setSalary(originalSalary);
        
        Employee empToBeMerged = new Employee();
        empToBeMerged.setFirstName(firstName);
        empToBeMerged.setLastName("toBeMerged");
        empToBeMerged.setSalary(originalSalary);
        
        // setup: make sure no Employee with the specified firstName exists and create the existing employees.
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try{
            em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
            em.getTransaction().commit();
            em.getTransaction().begin();
            em.persist(empToBeRemoved);
            em.persist(empToBeRefreshed);
            em.persist(empToBeMerged);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        em.close();
        clearCache("fieldaccess");
        
        // create entityManager with extended Persistence Context.
        em = createEntityManager("fieldaccess");
        
        try {
            // first test
            // without starting transaction persist, remove, refresh, merge
    
            em.persist(empNew);
            
            Employee empToBeRemovedExtended = em.find(Employee.class, empToBeRemoved.getId());
            em.remove(empToBeRemovedExtended);
            
            Employee empToBeRefreshedExtended = em.find(Employee.class, empToBeRefreshed.getId());
            int newSalary = 100;
            // Use another EntityManager to alter empToBeRefreshed in the db
            em.getTransaction().begin();
            empToBeRefreshed = em.find(Employee.class, empToBeRefreshed.getId());
            empToBeRefreshed.setSalary(newSalary);
            em.getTransaction().commit();
            // now refesh
            em.refresh(empToBeRefreshedExtended);
    
            Employee empToBeMergedExtended = em.find(Employee.class, empToBeMerged.getId());
            // alter empToBeRefreshed
            empToBeMerged.setSalary(newSalary);
            // now merge
            em.merge(empToBeMerged);
    
            // begin and commit transaction
            em.getTransaction().begin();
            em.getTransaction().commit();
            
            // verify objects are correct in the PersistenceContext after transaction
            if(!em.contains(empNew)) {
                fail("empNew gone from extended PersistenceContext after transaction committed");
            }
            if(em.contains(empToBeRemovedExtended)) {
                fail("empToBeRemovedExtended still in extended PersistenceContext after transaction committed");
            }
            if(!em.contains(empToBeRefreshedExtended)) {
                fail("empToBeRefreshedExtended gone from extended PersistenceContext after transaction committed");
            } else if(empToBeRefreshedExtended.getSalary() != newSalary) {
                fail("empToBeRefreshedExtended still has the original salary after transaction committed");
            }
            if(!em.contains(empToBeMergedExtended)) {
                fail("empToBeMergedExtended gone from extended PersistenceContext after transaction committed");
            } else if(empToBeMergedExtended.getSalary() != newSalary) {
                fail("empToBeMergedExtended still has the original salary after transaction committed");
            }
    
            // verify objects are correct in the db after transaction
            clearCache("fieldaccess");
            Employee empNewFound = em.find(Employee.class, empNew.getId());
            if(empNewFound == null) {
                fail("empNew not in the db after transaction committed");
            }
            Employee empToBeRemovedFound = em.find(Employee.class, empToBeRemoved.getId());
            if(empToBeRemovedFound != null) {
                fail("empToBeRemoved is still in the db after transaction committed");
            }
            Employee empToBeRefreshedFound = em.find(Employee.class, empToBeRefreshed.getId());
            if(empToBeRefreshedFound == null) {
                fail("empToBeRefreshed not in the db after transaction committed");
            } else if(empToBeRefreshedFound.getSalary() != newSalary) {
                fail("empToBeRefreshed still has the original salary in the db after transaction committed");
            }
            Employee empToBeMergedFound = em.find(Employee.class, empToBeMerged.getId());
            if(empToBeMergedFound == null) {
                fail("empToBeMerged not in the db after transaction committed");
            } else if(empToBeMergedFound.getSalary() != newSalary) {
                fail("empToBeMerged still has the original salary in the db after transaction committed");
            }
    
            // second test
            // without starting transaction persist, remove, refresh, merge for the second time:
            // now return to the original state of the objects:
            // remove empNew, persist empToBeRemoved, set empToBeRefreshed and empToBeMerged the original salary.
            
            em.persist(empToBeRemoved);
            em.remove(empNew);
            
            // Use another EntityManager to alter empToBeRefreshed in the db
            em.getTransaction().begin();
            empToBeRefreshed = em.find(Employee.class, empToBeRefreshed.getId());
            empToBeRefreshed.setSalary(originalSalary);
            em.getTransaction().commit();
            // now refesh
            em.refresh(empToBeRefreshedExtended);
    
            // alter empToBeRefreshedFound - can't use empToBeRefreshed here because of its older version().
            empToBeMergedFound.setSalary(originalSalary);
            // now merge
            em.merge(empToBeMergedFound);
    
            // begin and commit the second transaction
            em.getTransaction().begin();
            em.getTransaction().commit();
            
            // verify objects are correct in the PersistenceContext
            if(em.contains(empNew)) {
                fail("empNew not gone from extended PersistenceContext after the second transaction committed");
            }
            if(!em.contains(empToBeRemoved)) {
                fail("empToBeRemoved is not in extended PersistenceContext after the second transaction committed");
            }
            if(!em.contains(empToBeRefreshedExtended)) {
                fail("empToBeRefreshedExtended gone from extended PersistenceContext after the second transaction committed");
            } else if(empToBeRefreshedExtended.getSalary() != originalSalary) {
                fail("empToBeRefreshedExtended still doesn't have the original salary after the second transaction committed");
            }
            if(!em.contains(empToBeMergedExtended)) {
                fail("empToBeMergedExtended gone from extended PersistenceContext after the second transaction committed");
            } else if(empToBeMergedExtended.getSalary() != originalSalary) {
                fail("empToBeMergedExtended doesn't have the original salary after the second transaction committed");
            }
    
            // verify objects are correct in the db
            clearCache("fieldaccess");
            Employee empNewFound2 = em.find(Employee.class, empNew.getId());
            if(empNewFound2 != null) {
                fail("empNew still in the db after the second transaction committed");
            }
            Employee empToBeRemovedFound2 = em.find(Employee.class, empToBeRemoved.getId());
            if(empToBeRemovedFound2 == null) {
                fail("empToBeRemoved is not in the db after the second transaction committed");
            }
            Employee empToBeRefreshedFound2 = em.find(Employee.class, empToBeRefreshed.getId());
            if(empToBeRefreshedFound2 == null) {
                fail("empToBeRefreshed not in the db after the second transaction committed");
            } else if(empToBeRefreshedFound2.getSalary() != originalSalary) {
                fail("empToBeRefreshed doesn't have the original salary in the db after the second transaction committed");
            }
            Employee empToBeMergedFound2 = em.find(Employee.class, empToBeMerged.getId());
            if(empToBeMergedFound2 == null) {
                fail("empToBeMerged not in the db after the second transaction committed");
            } else if(empToBeMergedFound2.getSalary() != originalSalary) {
                fail("empToBeMerged doesn't have the original salary in the db after the second transaction committed");
            }
            
            // third test
            // without starting transaction persist, remove, refresh, merge
            // The same as the first test - but now we'll rollback.
            // The objects should be detached.
    
            em.getTransaction().begin();
            em.persist(empNew);
            em.remove(empToBeRemoved);
            
            // Use another EntityManager to alter empToBeRefreshed in the db
            EntityManager em2 = createEntityManager("fieldaccess");
            em2.getTransaction().begin();
            try{
                empToBeRefreshed = em2.find(Employee.class, empToBeRefreshed.getId());
                empToBeRefreshed.setSalary(newSalary);
                em2.getTransaction().commit();
            }catch (RuntimeException ex){
                if (em2.getTransaction().isActive()){
                    em2.getTransaction().rollback();
                }
                throw ex;
            }finally{
                em2.close();
            }
            // now refesh
            em.refresh(empToBeRefreshedExtended);
    
            // alter empToBeRefreshed
            empToBeMergedFound2.setSalary(newSalary);
            // now merge
            em.merge(empToBeMergedFound2);
    
            // flush and ROLLBACK the third transaction
            em.flush();
            em.getTransaction().rollback();
            
            // verify objects are correct in the PersistenceContext after the third transaction rolled back
            if(em.contains(empNew)) {
                fail("empNew is still in extended PersistenceContext after the third transaction rolled back");
            }
            if(em.contains(empToBeRemoved)) {
                fail("empToBeRemoved is still in extended PersistenceContext after the third transaction rolled back");
            }
            if(em.contains(empToBeRefreshedExtended)) {
                fail("empToBeRefreshedExtended is still in extended PersistenceContext after the third transaction rolled back");
            } else if(empToBeRefreshedExtended.getSalary() != newSalary) {
                fail("empToBeRefreshedExtended still has the original salary after third transaction rolled back");
            }
            if(em.contains(empToBeMergedExtended)) {
                fail("empToBeMergedExtended is still in extended PersistenceContext after the third transaction rolled back");
            } else if(empToBeMergedExtended.getSalary() != newSalary) {
                fail("empToBeMergedExtended still has the original salary after third transaction rolled back");
            }
    
            // verify objects are correct in the db after the third transaction rolled back
            clearCache("fieldaccess");
            Employee empNewFound3 = em.find(Employee.class, empNew.getId());
            if(empNewFound3 != null) {
                fail("empNew is in the db after the third transaction rolled back");
            }
            Employee empToBeRemovedFound3 = em.find(Employee.class, empToBeRemoved.getId());
            if(empToBeRemovedFound3 == null) {
                fail("empToBeRemoved not in the db after the third transaction rolled back");
            }
            Employee empToBeRefreshedFound3 = em.find(Employee.class, empToBeRefreshed.getId());
            if(empToBeRefreshedFound3 == null) {
                fail("empToBeRefreshed not in the db after the third transaction rolled back");
            } else if(empToBeRefreshedFound3.getSalary() != newSalary) {
                fail("empToBeRefreshed has the original salary in the db after the third transaction rolled back");
            }
            Employee empToBeMergedFound3 = em.find(Employee.class, empToBeMerged.getId());
            if(empToBeMergedFound3 == null) {
                fail("empToBeMerged not in the db after the third transaction rolled back");
            } else if(empToBeMergedFound3.getSalary() != originalSalary) {
                fail("empToBeMerged still doesn't have the original salary in the db after the third transaction rolled back");
            }
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }
    
    public void testReadTransactionIsolation_CustomUpdate() {
        internalTestReadTransactionIsolation(false, false, false, false);
    }
    public void testReadTransactionIsolation_CustomUpdate_Flush() {
        internalTestReadTransactionIsolation(false, false, false, true);
    }
    public void testReadTransactionIsolation_CustomUpdate_Refresh() {
        internalTestReadTransactionIsolation(false, false, true, false);
    }
    public void testReadTransactionIsolation_CustomUpdate_Refresh_Flush() {
        internalTestReadTransactionIsolation(false, false, true, true);
    }
    public void testReadTransactionIsolation_UpdateAll() {
        internalTestReadTransactionIsolation(false, true, false, false);
    }
    public void testReadTransactionIsolation_UpdateAll_Flush() {
        internalTestReadTransactionIsolation(false, true, false, true);
    }
    public void testReadTransactionIsolation_UpdateAll_Refresh() {
        internalTestReadTransactionIsolation(false, true, true, false);
    }
    public void testReadTransactionIsolation_UpdateAll_Refresh_Flush() {
        internalTestReadTransactionIsolation(false, true, true, true);
    }
    public void testReadTransactionIsolation_OriginalInCache_CustomUpdate() {
        internalTestReadTransactionIsolation(true, false, false, false);
    }
    public void testReadTransactionIsolation_OriginalInCache_CustomUpdate_Flush() {
        internalTestReadTransactionIsolation(true, false, false, true);
    }
    public void testReadTransactionIsolation_OriginalInCache_CustomUpdate_Refresh() {
        internalTestReadTransactionIsolation(true, false, true, false);
    }
    public void testReadTransactionIsolation_OriginalInCache_CustomUpdate_Refresh_Flush() {
        internalTestReadTransactionIsolation(true, false, true, true);
    }
    public void testReadTransactionIsolation_OriginalInCache_UpdateAll() {
        internalTestReadTransactionIsolation(true, true, false, false);
    }
    public void testReadTransactionIsolation_OriginalInCache_UpdateAll_Flush() {
        internalTestReadTransactionIsolation(true, true, false, true);
    }
    public void testReadTransactionIsolation_OriginalInCache_UpdateAll_Refresh() {
        internalTestReadTransactionIsolation(true, true, true, false);
    }
    public void testReadTransactionIsolation_OriginalInCache_UpdateAll_Refresh_Flush() {
        internalTestReadTransactionIsolation(true, true, true, true);
    }
    
    protected void internalTestReadTransactionIsolation(boolean shouldOriginalBeInParentCache, boolean shouldUpdateAll, boolean shouldRefresh, boolean shouldFlush) {
        //setup
        String firstName = "testReadTransactionIsolation";
        
        // make sure no Employee with the specified firstName exists.
        EntityManager em = createEntityManager("fieldaccess");
        Query deleteQuery = em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'");        
        em.getTransaction().begin();
        try{
            deleteQuery.executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        clearCache("fieldaccess");
        em.clear();
        
        // create and persist the object
        String lastNameOriginal = "Original";
        int salaryOriginal = 0;
        Employee employee = new Employee();
        employee.setFirstName(firstName);
        employee.setLastName(lastNameOriginal);
        employee.setSalary(salaryOriginal);
        em.getTransaction().begin();
        try{
            em.persist(employee);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        
        if(!shouldOriginalBeInParentCache) {
            clearCache("fieldaccess");
        }
        em.clear();
        
        Query selectQuery = em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.firstName = '"+firstName+"'");
        Employee employeeUOW = null;

        int salaryNew = 100;
        String lastNameNew = "New";

        em.getTransaction().begin();
        
        try{
            if(shouldRefresh) {
                String lastNameAlternative = "Alternative";
                int salaryAlternative = 50;
                employeeUOW = (Employee)selectQuery.getSingleResult();
                employeeUOW.setLastName(lastNameAlternative);
                employeeUOW.setSalary(salaryAlternative);
            }
        
            int nUpdated;
            if(shouldUpdateAll) {
                nUpdated = em.createQuery("UPDATE Employee e set e.lastName = '" + lastNameNew + "' where e.firstName like '" + firstName + "'").setFlushMode(FlushModeType.AUTO).executeUpdate();
            } else {
                nUpdated = em.createNativeQuery("UPDATE CMP3_FA_EMPLOYEE SET L_NAME = '" + lastNameNew + "', VERSION = VERSION + 1 WHERE F_NAME LIKE '" + firstName + "'").setFlushMode(FlushModeType.AUTO).executeUpdate();
            }
            assertTrue("nUpdated=="+ nUpdated +"; 1 was expected", nUpdated == 1);
    
            if(shouldFlush) {
                selectQuery.setFlushMode(FlushModeType.AUTO);
            } else {
                selectQuery.setFlushMode(FlushModeType.COMMIT);
            }
    
            if(shouldRefresh) {
                selectQuery.setHint("eclipselink.refresh", Boolean.TRUE);
                employeeUOW = (Employee)selectQuery.getSingleResult();
                selectQuery.setHint("eclipselink.refresh", Boolean.FALSE);
            } else {
                employeeUOW = (Employee)selectQuery.getSingleResult();
            }
            assertTrue("employeeUOW.getLastName()=="+ employeeUOW.getLastName() +"; " + lastNameNew + " was expected", employeeUOW.getLastName().equals(lastNameNew));
    
            employeeUOW.setSalary(salaryNew);
    
            employeeUOW = (Employee)selectQuery.getSingleResult();
            assertTrue("employeeUOW.getSalary()=="+ employeeUOW.getSalary() +"; " + salaryNew + " was expected", employeeUOW.getSalary() == salaryNew);
                    
            em.getTransaction().commit();
        }catch (Throwable ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            if (Error.class.isAssignableFrom(ex.getClass())){
                throw (Error)ex;
            }else{
                throw (RuntimeException)ex;
            }
        }
        
        Employee employeeFoundAfterTransaction = em.find(Employee.class, employeeUOW.getId());
        assertTrue("employeeFoundAfterTransaction().getLastName()=="+ employeeFoundAfterTransaction.getLastName() +"; " + lastNameNew + " was expected", employeeFoundAfterTransaction.getLastName().equals(lastNameNew));
        assertTrue("employeeFoundAfterTransaction().getSalary()=="+ employeeFoundAfterTransaction.getSalary() +"; " + salaryNew + " was expected", employeeFoundAfterTransaction.getSalary() == salaryNew);
    
        // clean up
        em.getTransaction().begin();
        try{
            deleteQuery.executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        clearCache("fieldaccess");
        em.close();
    }

    // test for bug 4755392: 
    // AFTER DELETEALL OBJECT STILL DEEMED EXISTING
    public void testFindDeleteAllPersist() {
        String firstName = "testFindDeleteAllPersist";

        // create Employees        
        Employee empWithAddress = new Employee();
        empWithAddress.setFirstName(firstName);
        empWithAddress.setLastName("WithAddress");
        empWithAddress.setAddress(new Address());

        Employee empWithoutAddress = new Employee();
        empWithoutAddress.setFirstName(firstName);
        empWithoutAddress.setLastName("WithoutAddress");

        EntityManager em = createEntityManager("fieldaccess");
        Query deleteQuery = em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'");

        // make sure no Employee with the specified firstName exists.
        em.getTransaction().begin();
        try{
            deleteQuery.executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }

        // persist
        em.getTransaction().begin();
        try{
            em.persist(empWithAddress);
            em.persist(empWithoutAddress);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        
        // clear cache
        clearCache("fieldaccess");
        em.clear();
        
        // Find both to bring into the cache, delete empWithoutAddress.
        // Because the address VH is not triggered both objects should be invalidated.
        em.getTransaction().begin();
        try{
            Employee empWithAddressFound = em.find(Employee.class, empWithAddress.getId());
            Employee empWithoutAddressFound = em.find(Employee.class, empWithoutAddress.getId());
            int nDeleted = em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"' and e.address IS NULL").executeUpdate();
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        // we can no longer rely on the query above to clear the Employee from the persistence context.
        // Clearling the context to allow us to proceed.
        em.clear();
        // persist new empWithoutAddress - the one that has been deleted from the db.
        em.getTransaction().begin();
        try{
            Employee newEmpWithoutAddress = new Employee();
            newEmpWithoutAddress.setFirstName(firstName);
            newEmpWithoutAddress.setLastName("newWithoutAddress");
            newEmpWithoutAddress.setId(empWithoutAddress.getId());
            em.persist(newEmpWithoutAddress);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }

        // persist new empWithAddress - the one still in the db.
        em.getTransaction().begin();
        try{
            Employee newEmpWithAddress = new Employee();
            newEmpWithAddress.setFirstName(firstName);
            newEmpWithAddress.setLastName("newWithAddress");
            newEmpWithAddress.setId(empWithAddress.getId());
            em.persist(newEmpWithAddress);
            fail("EntityExistsException was expected");
        } catch (EntityExistsException ex) {
            // "cant_persist_detatched_object" - ignore the expected exception
        } finally {
            em.getTransaction().rollback();
        }

        // clean up
        em.getTransaction().begin();
        deleteQuery.executeUpdate();
        em.getTransaction().commit();
    }
    
    public void testWRITELock(){
        EntityManager em = createEntityManager("fieldaccess");
        Employee employee = new Employee();
        employee.setFirstName("Mark");
        employee.setLastName("Madsen");
        em.getTransaction().begin();
        try{
            em.persist(employee);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }

        EntityManager em2 = createEntityManager("fieldaccess");
        Exception optimisticLockException = null;
        
        em.getTransaction().begin();
        try{
            employee = em.find(Employee.class, employee.getId());
            em.lock(employee, LockModeType.WRITE);

            em2.getTransaction().begin();
            try{
                Employee employee2 = em2.find(Employee.class, employee.getId());
                employee2.setFirstName("Michael");
                em2.getTransaction().commit();
            }catch (RuntimeException ex){
                em2.getTransaction().rollback();
                em2.close();
                throw ex;
            }
            
            em.getTransaction().commit();
        } catch (RollbackException exception) {
            if (exception.getCause() instanceof OptimisticLockException){
                optimisticLockException = exception;
            }
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        em.getTransaction().begin();
        try{
            employee = em.find(Employee.class, employee.getId());
            em.remove(employee);
            em.getTransaction().commit();
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
            throw ex;
        }
        
        if (optimisticLockException == null){
            fail("Proper exception not thrown when EntityManager.lock(object, WRITE) is used.");
        }
    }

    /**This test case uses the "negative2" PU defined in the persistence.xml*/     
    public void testEMFWrapValidationException() 
    {
        EntityManagerFactory factory = null;
        try {
            factory = Persistence.createEntityManagerFactory("negative2", JUnitTestCaseHelper.getDatabaseProperties());
            EntityManager em = factory.createEntityManager();
        } catch (javax.persistence.PersistenceException e)  {
            // Ignore - it's expected exception type
        } finally {
            factory.close();
        }
    }
    
    /**
     * At the time this test case was added, the problem it was designed to test for would cause a failure during deployement 
     * and therefore this tests case would likely always pass if there is a successful deployment.
     * But it is anticipated that that may not always be the case and therefore we are adding a test case 
     */
    public void testEMDefaultTxType() 
    {
        EntityManagerFactory factory = null;
        try {
            factory = Persistence.createEntityManagerFactory("negative1", JUnitTestCaseHelper.getDatabaseProperties());
            EntityManager em = factory.createEntityManager();
        } catch (Exception e)  {   
            fail("Exception caught while creating EM with no \"transaction-type\" specified in persistence.xml");        
        } finally {
            factory.close();
        }
        Assert.assertTrue(true);        
    }
    
    public void testPersistOnNonEntity()
    {
        boolean testPass = false;
        Object nonEntity = new Object();
        EntityManager em = createEntityManager("fieldaccess");
        
        try {
            em.persist(nonEntity);
        } catch (IllegalArgumentException e) {
            testPass = true;
        }
        Assert.assertTrue(testPass);
    }

    public void testClose() {
        EntityManager em = createEntityManager("fieldaccess");
        if(!em.isOpen()) {
            fail("Created EntityManager is not open");
        }
        em.close();
        if(em.isOpen()) {
            fail("Closed EntityManager is still open");
        }
    }

    public void testBeginTransactionClose() {
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try{
            em.close();
            if(em.isOpen()) {
                fail("Closed EntityManager is still open before transaction complete");
            }
        }catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            if(em.isOpen()) {
                em.close();
            }
            throw ex;
        }

        em.getTransaction().rollback();
        if(em.isOpen()) {
            fail("Closed EntityManager is still open after transaction rollback");
        }
    }

    public void testBeginTransactionCloseCommitTransaction() {
        String firstName = "testBeginTrCloseCommitTr";
        EntityManager em = createEntityManager("fieldaccess");

        // make sure there is no employees with this firstName
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
        em.getTransaction().commit();
        
        // create a new Employee
        Employee employee = new Employee();
        employee.setFirstName(firstName);        
        
        // persist the new Employee and close the entity manager
        em.getTransaction().begin();
        try{
            em.persist(employee);
            em.close();
            if(em.isOpen()) {
                fail("Closed EntityManager is still open before transaction complete");
            }
        }catch (RuntimeException ex){
            em.getTransaction().rollback();
            if(em.isOpen()) {
                em.close();
            }
            throw ex;
        }
        em.getTransaction().commit();
        
        if(em.isOpen()) {
            fail("Closed EntityManager is still open after transaction commit");
        }
        
        // verify that the employee has been persisted
        em = createEntityManager("fieldaccess");
        RuntimeException exception = null;
        try {
            Employee persistedEmployee = (Employee)em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.firstName = '"+firstName+"'").getSingleResult();
        } catch (RuntimeException runtimeException) {
            exception = runtimeException;
        }

        // clean up
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
        em.getTransaction().commit();
        
        if(exception != null) {
            if(exception instanceof EntityNotFoundException) {
                fail("object has not been persisted");
            } else {
                // unexpected exception - rethrow.
                throw exception;
            }
        }
    }

    // The test removed because we moved back to binding literals 
    // on platforms other than DB2 and Derby
/*    public void testDontBindLiteral() {
        EntityManager em = createEntityManager("fieldaccess");
        
        Query controlQuery = em.createQuery("SELECT OBJECT(p) FROM SmallProject p WHERE p.name = CONCAT(:param1, :param2)");
        controlQuery.setParameter("param1", "A").setParameter("param2", "B");
        List controlResults = controlQuery.getResultList();
        int nControlParams = ((ExpressionQueryMechanism)((EJBQueryImpl)controlQuery).getDatabaseQuery().getQueryMechanism()).getCall().getParameters().size();
        if(nControlParams != 2) {
            fail("controlQuery has wrong number of parameters = "+nControlParams+"; 2 is expected");
        }

        Query query = em.createQuery("SELECT OBJECT(p) FROM SmallProject p WHERE p.name = CONCAT('A', 'B')");
        List results = query.getResultList();
        int nParams = ((ExpressionQueryMechanism)((EJBQueryImpl)query).getDatabaseQuery().getQueryMechanism()).getCall().getParameters().size();
        if(nParams > 0) {
            fail("Query processed literals as parameters");
        }
        
        em.close();
    }*/
    
    public void testPersistenceProperties() {
        EntityManager em = createEntityManager("fieldaccess");
        ServerSession ss = ((org.eclipse.persistence.internal.jpa.EntityManagerImpl)em).getServerSession();
        
        // these properties were set in persistence unit
        // and overridden in CMP3TestModel.setup - the values should be overridden.
        
        boolean isReadShared = (ss.getReadConnectionPool() instanceof ReadConnectionPool);
        if(isReadShared != Boolean.parseBoolean((String)JUnitTestCaseHelper.propertiesMap.get(PersistenceUnitProperties.JDBC_READ_CONNECTIONS_SHARED))) {
            fail("isReadShared is wrong");
        }
        
        int writeMin = ss.getDefaultConnectionPool().getMinNumberOfConnections();
        if(writeMin != Integer.parseInt((String)JUnitTestCaseHelper.propertiesMap.get(PersistenceUnitProperties.JDBC_WRITE_CONNECTIONS_MIN))) {
            fail("writeMin is wrong");
        }
        
        int writeMax = ss.getDefaultConnectionPool().getMaxNumberOfConnections();
        if(writeMax != Integer.parseInt((String)JUnitTestCaseHelper.propertiesMap.get(PersistenceUnitProperties.JDBC_WRITE_CONNECTIONS_MAX))) {
            fail("writeMax is wrong");
        }

        int readMin = ss.getReadConnectionPool().getMinNumberOfConnections();
        if(readMin != Integer.parseInt((String)JUnitTestCaseHelper.propertiesMap.get(PersistenceUnitProperties.JDBC_READ_CONNECTIONS_MIN))) {
            fail("readMin is wrong");
        }

        int readMax = ss.getReadConnectionPool().getMaxNumberOfConnections();
        if(readMax != Integer.parseInt((String)JUnitTestCaseHelper.propertiesMap.get(PersistenceUnitProperties.JDBC_READ_CONNECTIONS_MAX))) {
            fail("readMax is wrong");
        }
        
        // these properties were set in persistence unit - the values should be the same as in persistence.xml
        /*
			<property name="eclipselink.session-name" value="default-session"/>
			<property name="eclipselink.cache.size.default" value="500"/>
			<property name="eclipselink.cache.size.Employee" value="550"/>
			<property name="eclipselink.cache.size.org.eclipse.persistence.testing.models.jpa.advanced.Address" value="555"/>
			<property name="eclipselink.cache.type.default" value="Full"/>
			<property name="eclipselink.cache.type.Employee" value="Weak"/>
			<property name="eclipselink.cache.type.org.eclipse.persistence.testing.models.jpa.advanced.Address" value="HardWeak"/>
			<property name="eclipselink.session.customizer" value="org.eclipse.persistence.testing.models.jpa.advanced.Customizer"/>
			<property name="eclipselink.descriptor.customizer.Employee" value="org.eclipse.persistence.testing.models.jpa.advanced.Customizer"/>
			<property name="eclipselink.descriptor.customizer.org.eclipse.persistence.testing.models.jpa.advanced.Address" value="org.eclipse.persistence.testing.models.jpa.advanced.Customizer"/>
        */
        
        String sessionName = ss.getName();
        
        int defaultCacheSize = ss.getDescriptor(Project.class).getIdentityMapSize();
        if(defaultCacheSize != 500) {
            fail("defaultCacheSize is wrong");
        }
        
        int employeeCacheSize = ss.getDescriptor(Employee.class).getIdentityMapSize();
        if(employeeCacheSize != 550) {
            fail("employeeCacheSize is wrong");
        }
        
        int addressCacheSize = ss.getDescriptor(Address.class).getIdentityMapSize();
        if(addressCacheSize != 555) {
            fail("addressCacheSize is wrong");
        }
        
        Class defaultCacheType = ss.getDescriptor(Project.class).getIdentityMapClass();
        if(! Helper.getShortClassName(defaultCacheType).equals("FullIdentityMap")) {
            fail("defaultCacheType is wrong");
        }
        
        Class employeeCacheType = ss.getDescriptor(Employee.class).getIdentityMapClass();
        if(! Helper.getShortClassName(employeeCacheType).equals("WeakIdentityMap")) {
            fail("employeeCacheType is wrong");
        }
        
        Class addressCacheType = ss.getDescriptor(Address.class).getIdentityMapClass();
        if(! Helper.getShortClassName(addressCacheType).equals("HardCacheWeakIdentityMap")) {
            fail("addressCacheType is wrong");
        }
        
        int numSessionCalls = Customizer.getNumberOfCallsForSession(ss.getName());
        if(numSessionCalls == 0) {
            fail("session customizer hasn't been called");
        }
        
        int numProjectCalls = Customizer.getNumberOfCallsForClass(Project.class.getName());
        if(numProjectCalls > 0) {
            fail("Project customizer has been called");
        }
        
        int numEmployeeCalls = Customizer.getNumberOfCallsForClass(Employee.class.getName());
        if(numEmployeeCalls == 0) {
            fail("Employee customizer hasn't been called");
        }
        
        int numAddressCalls = Customizer.getNumberOfCallsForClass(Address.class.getName());
        if(numAddressCalls == 0) {
            fail("Address customizer hasn't been called");
        }
        
        em.close();
    }

    public void testMultipleFactories() {
        getEntityManagerFactory("fieldaccess");
        closeEntityManagerFactory();
        boolean isOpen = getEntityManagerFactory("fieldaccess").isOpen();
        if(!isOpen) {
            fail("Close factory 1; open factory 2 - it's not open");
        } else {
            // Get entity manager just to login back the session, then close em
            getEntityManagerFactory("fieldaccess").createEntityManager().close();
        }
    }
    
    public void testParallelMultipleFactories() {
        EntityManagerFactory factory1 =  Persistence.createEntityManagerFactory("fieldaccess", JUnitTestCaseHelper.getDatabaseProperties());
        factory1.createEntityManager();
        EntityManagerFactory factory2 =  Persistence.createEntityManagerFactory("fieldaccess", JUnitTestCaseHelper.getDatabaseProperties());
        factory2.createEntityManager();
        factory1.close();
        if(factory1.isOpen()) {
            fail("after factory1.close() factory1 is not closed");
        }
        if(!factory2.isOpen()) {
            fail("after factory1.close() factory2 is closed");
        }
        factory2.close();
        if(factory2.isOpen()) {
            fail("after factory2.close() factory2 is not closed");
        }
        EntityManagerFactory factory3 =  Persistence.createEntityManagerFactory("fieldaccess", JUnitTestCaseHelper.getDatabaseProperties());
        if(!factory3.isOpen()) {
            fail("factory3 is closed");
        }
        factory3.createEntityManager();
        factory3.close();
        if(factory3.isOpen()) {
            fail("after factory3.close() factory3 is open");
        }
    }
    
    public void testQueryHints() {
        EntityManager em = createEntityManager("fieldaccess");
        Query query = em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.firstName = 'testQueryHints'");
        ObjectLevelReadQuery olrQuery = (ObjectLevelReadQuery)((EJBQueryImpl)query).getDatabaseQuery();
        
        // binding
        // original state = default state
        assertTrue(olrQuery.shouldIgnoreBindAllParameters());
        // set boolean true
        query.setHint(EclipseLinkQueryHints.BIND_PARAMETERS, true);
        // Parse cached query may be cloned when hint set, so re-get.
        olrQuery = (ObjectLevelReadQuery)((EJBQueryImpl)query).getDatabaseQuery();
        assertTrue("Binding not set.", olrQuery.shouldBindAllParameters());
        // reset to original state
        query.setHint(EclipseLinkQueryHints.BIND_PARAMETERS, "");
        assertTrue("Binding not set.", olrQuery.shouldIgnoreBindAllParameters());
        // set "false"
        query.setHint(EclipseLinkQueryHints.BIND_PARAMETERS, "false");
        assertFalse("Binding not set.", olrQuery.shouldBindAllParameters());
        // reset to the original state
        query.setHint(EclipseLinkQueryHints.BIND_PARAMETERS, "");
        assertTrue("Binding not set.", olrQuery.shouldIgnoreBindAllParameters());
        
        // cache usage
        query.setHint(EclipseLinkQueryHints.CACHE_USAGE, CacheUsage.DoNotCheckCache);
        assertTrue("Cache usage not set.", olrQuery.getCacheUsage()==ObjectLevelReadQuery.DoNotCheckCache);
        query.setHint(EclipseLinkQueryHints.CACHE_USAGE, CacheUsage.CheckCacheOnly);
        assertTrue("Cache usage not set.", olrQuery.shouldCheckCacheOnly());
        query.setHint(EclipseLinkQueryHints.CACHE_USAGE, CacheUsage.ConformResultsInUnitOfWork);
        assertTrue("Cache usage not set.", olrQuery.shouldConformResultsInUnitOfWork());
        // reset to the original state
        query.setHint(EclipseLinkQueryHints.CACHE_USAGE, "");
        assertTrue("Cache usage not set.", olrQuery.shouldCheckDescriptorForCacheUsage());
        
        // pessimistic lock
        query.setHint(EclipseLinkQueryHints.PESSIMISTIC_LOCK, PessimisticLock.Lock);
        assertTrue("Lock not set.", olrQuery.getLockMode()==ObjectLevelReadQuery.LOCK);
        query.setHint(EclipseLinkQueryHints.PESSIMISTIC_LOCK, PessimisticLock.NoLock);
        assertTrue("Lock not set.", olrQuery.getLockMode()==ObjectLevelReadQuery.NO_LOCK);
        query.setHint(EclipseLinkQueryHints.PESSIMISTIC_LOCK, PessimisticLock.LockNoWait);
        assertTrue("Lock not set.", olrQuery.getLockMode()==ObjectLevelReadQuery.LOCK_NOWAIT);
        // default state
        query.setHint(EclipseLinkQueryHints.PESSIMISTIC_LOCK, "");
        assertTrue("Lock not set.", olrQuery.getLockMode()==ObjectLevelReadQuery.NO_LOCK);
        
        //refresh
        // set to original state - don't refresh.
        // the previously run LOCK and LOCK_NOWAIT have swithed it to true
        query.setHint(EclipseLinkQueryHints.REFRESH, false);
        assertFalse("Refresh not set.", olrQuery.shouldRefreshIdentityMapResult());
        // set boolean true
        query.setHint(EclipseLinkQueryHints.REFRESH, true);
        assertTrue("Refresh not set.", olrQuery.shouldRefreshIdentityMapResult());
        // set "false"
        query.setHint(EclipseLinkQueryHints.REFRESH, "false");
        assertFalse("Refresh not set.", olrQuery.shouldRefreshIdentityMapResult());
        // set Boolean.TRUE
        query.setHint(EclipseLinkQueryHints.REFRESH, Boolean.TRUE);
        assertTrue("Refresh not set.", olrQuery.shouldRefreshIdentityMapResult());
        // reset to original state
        query.setHint(EclipseLinkQueryHints.REFRESH, "");
        assertFalse("Refresh not set.", olrQuery.shouldRefreshIdentityMapResult());
        
        query.setHint(EclipseLinkQueryHints.RETURN_SHARED, "false");
        assertFalse("Read-only not set.", olrQuery.isReadOnly()); 
        
        query.setHint(EclipseLinkQueryHints.RETURN_SHARED, Boolean.TRUE);
        assertTrue("Read-only not set.", olrQuery.isReadOnly());
        
        query.setHint(EclipseLinkQueryHints.RETURN_SHARED, Boolean.FALSE);
        assertFalse("Read-only not set.", olrQuery.isReadOnly());
        
        query.setHint(EclipseLinkQueryHints.JDBC_TIMEOUT, new Integer(100));
        assertTrue("Timeout not set.", olrQuery.getQueryTimeout() == 100);
        
        query.setHint(EclipseLinkQueryHints.JDBC_FETCH_SIZE, new Integer(101));
        assertTrue("Fetch-size not set.", olrQuery.getFetchSize() == 101);
        
        query.setHint(EclipseLinkQueryHints.JDBC_MAX_ROWS, new Integer(103));
        assertTrue("Max-rows not set.", olrQuery.getMaxRows() == 103); 
        
        query.setHint(EclipseLinkQueryHints.RESULT_COLLECTION_TYPE, java.util.ArrayList.class);
        assertTrue("ArrayList not set.", ((ReadAllQuery)olrQuery).getContainerPolicy().getContainerClassName().equals(java.util.ArrayList.class.getName())); 

        query.setHint(EclipseLinkQueryHints.RESULT_COLLECTION_TYPE, "java.util.Vector");
        assertTrue("Vector not set.", ((ReadAllQuery)olrQuery).getContainerPolicy().getContainerClassName().equals(java.util.Vector.class.getName())); 

        em.close();
    }
    
    /**
     * This test ensures that the eclipselink.batch query hint works.
     * It tests two things. 
     * 
     * 1. That the batch read attribute is properly added to the queyr
     * 2. That the query will execute
     * 
     * It does not do any verification that the batch reading feature actually works.  That is
     * left for the batch reading testing to do.
     */
    public void testBatchQueryHint(){
        int id1 = 0;
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        
        Employee manager = new Employee();
        manager.setFirstName("Marvin");
        manager.setLastName("Malone");
        PhoneNumber number = new PhoneNumber("cell", "613", "888-8888");
        manager.addPhoneNumber(number);
        number = new PhoneNumber("home", "613", "888-8880");
        manager.addPhoneNumber(number);
        em.persist(manager);
        id1 = manager.getId();
        
        Employee emp = new Employee();
        emp.setFirstName("Melvin");
        emp.setLastName("Malone");
        emp.setManager(manager);
        manager.addManagedEmployee(emp);
        number = new PhoneNumber("cell", "613", "888-9888");
        emp.addPhoneNumber(number);
        number = new PhoneNumber("home", "613", "888-0880");
        emp.addPhoneNumber(number);
        em.persist(emp);
        
        emp = new Employee();
        emp.setFirstName("David");
        emp.setLastName("Malone");
        emp.setManager(manager);
        manager.addManagedEmployee(emp);
        number = new PhoneNumber("cell", "613", "888-9988");
        emp.addPhoneNumber(number);
        number = new PhoneNumber("home", "613", "888-0980");
        emp.addPhoneNumber(number);
        em.persist(emp);
        
        em.getTransaction().commit();
        em.clear();

        org.eclipse.persistence.jpa.JpaQuery query = (org.eclipse.persistence.jpa.JpaQuery)em.createQuery("SELECT e FROM Employee e WHERE e.lastName = 'Malone' order by e.firstName");
        query.setHint(EclipseLinkQueryHints.BATCH, "e.phoneNumbers");
        query.setHint(EclipseLinkQueryHints.BATCH, "e.manager.phoneNumbers");
        
        ReadAllQuery raq = (ReadAllQuery)query.getDatabaseQuery();
        List expressions = raq.getBatchReadAttributeExpressions();
        assertTrue(expressions.size() == 2);
        Expression exp = (Expression)expressions.get(0);
        assertTrue(exp.isQueryKeyExpression());
        assertTrue(exp.getName().equals("phoneNumbers"));
        exp = (Expression)expressions.get(1);
        assertTrue(exp.isQueryKeyExpression());
        assertTrue(exp.getName().equals("phoneNumbers"));

        List resultList = query.getResultList();
        emp = (Employee)resultList.get(0);
        emp.getPhoneNumbers().hashCode();
        
        emp.getManager().getPhoneNumbers().hashCode();

        emp = (Employee)resultList.get(1);
        emp.getPhoneNumbers().hashCode();
        
        em.getTransaction().begin();
        emp = em.find(Employee.class, id1);
        Iterator it = emp.getManagedEmployees().iterator();
        while (it.hasNext()){
            Employee managedEmp = (Employee)it.next();
            it.remove();
            managedEmp.setManager(null);
            em.remove(managedEmp);
        }
        em.remove(emp);
        em.getTransaction().commit();
    }
    
    /**
     * This test ensures that the toplink.fetch query hint works.
     * It tests two things. 
     * 
     * 1. That the jined attribute is properly added to the query
     * 2. That the query will execute
     * 
     * It does not do any verification that the joining feature actually works.  That is
     * left for the joining testing to do.
     */
    public void testFetchQueryHint(){
        int id1 = 0;
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        
        Employee manager = new Employee();
        manager.setFirstName("Marvin");
        manager.setLastName("Malone");
        PhoneNumber number = new PhoneNumber("cell", "613", "888-8888");
        manager.addPhoneNumber(number);
        number = new PhoneNumber("home", "613", "888-8880");
        manager.addPhoneNumber(number);
        em.persist(manager);
        id1 = manager.getId();
        
        Employee emp = new Employee();
        emp.setFirstName("Melvin");
        emp.setLastName("Malone");
        emp.setManager(manager);
        manager.addManagedEmployee(emp);
        number = new PhoneNumber("cell", "613", "888-9888");
        emp.addPhoneNumber(number);
        number = new PhoneNumber("home", "613", "888-0880");
        emp.addPhoneNumber(number);
        em.persist(emp);
        
        emp = new Employee();
        emp.setFirstName("David");
        emp.setLastName("Malone");
        emp.setManager(manager);
        manager.addManagedEmployee(emp);
        number = new PhoneNumber("cell", "613", "888-9988");
        emp.addPhoneNumber(number);
        number = new PhoneNumber("home", "613", "888-0980");
        emp.addPhoneNumber(number);
        em.persist(emp);
        
        em.getTransaction().commit();
        em.clear();

        org.eclipse.persistence.jpa.JpaQuery query = (org.eclipse.persistence.jpa.JpaQuery)em.createQuery("SELECT e FROM Employee e WHERE e.lastName = 'Malone' order by e.firstName");
        query.setHint(EclipseLinkQueryHints.FETCH, "e.manager");
        ReadAllQuery raq = (ReadAllQuery)query.getDatabaseQuery();
        List expressions = raq.getJoinedAttributeExpressions();
        assertTrue(expressions.size() == 1);
        Expression exp = (Expression)expressions.get(0);
        assertTrue(exp.getName().equals("manager"));       
        query.setHint(EclipseLinkQueryHints.FETCH, "e.manager.phoneNumbers");
        assertTrue(expressions.size() == 2);

        List resultList = query.getResultList();
        emp = (Employee)resultList.get(0);
        
        em.getTransaction().begin();
        emp = em.find(Employee.class, id1);
        Iterator it = emp.getManagedEmployees().iterator();
        while (it.hasNext()){
            Employee managedEmp = (Employee)it.next();
            it.remove();
            managedEmp.setManager(null);
            em.remove(managedEmp);
        }
        em.remove(emp);

        em.getTransaction().commit();
    }
    
    /**
     * Test that the proper exception is thrown when an incorrect batch or fetch query hint is set on the queyr.
     */
    public void testIncorrectBatchQueryHint(){
        EntityManager em = createEntityManager("fieldaccess");
        QueryException exception = null;
        try{
            Query query = em.createQuery("SELECT e FROM Employee e WHERE e.lastName = 'Malone' order by e.firstName");
            query.setHint(EclipseLinkQueryHints.BATCH, "e");
        } catch (QueryException exc){
            exception = exc;
        }
        assertNotNull("No exception was thrown on an incorrect BATCH query hint.", exception);
        assertTrue("Incorrect Exception thrown", exception.getErrorCode() == QueryException.QUERY_HINT_DID_NOT_CONTAIN_ENOUGH_TOKENS);
        exception = null;
        try{
            Query query = em.createQuery("SELECT e FROM Employee e WHERE e.lastName = 'Malone' order by e.firstName");
            query.setHint(EclipseLinkQueryHints.BATCH, "e.abcdef");
        } catch (QueryException exc){
            exception = exc;
        }
        assertNotNull("No exception was thrown on an incorrect BATCH query hint.", exception);
        assertTrue("Incorrect Exception thrown", exception.getErrorCode() == QueryException.QUERY_HINT_NAVIGATED_NON_EXISTANT_RELATIONSHIP);

        exception = null;
        try{
            Query query = em.createQuery("SELECT e FROM Employee e WHERE e.lastName = 'Malone' order by e.firstName");
            query.setHint(EclipseLinkQueryHints.BATCH, "e.firstName");
        } catch (QueryException exc){
            exception = exc;
        }
        assertNotNull("No exception was thrown when an incorrect relationship was navigated in a BATCH query hint.", exception);
        assertTrue("Incorrect Exception thrown", exception.getErrorCode() == QueryException.QUERY_HINT_NAVIGATED_ILLEGAL_RELATIONSHIP);

        exception = null;
        try{
            Query query = em.createQuery("SELECT e FROM Employee e WHERE e.lastName = 'Malone' order by e.firstName");
            query.setHint(EclipseLinkQueryHints.FETCH, "e");
        } catch (QueryException exc){
            exception = exc;
        }
        assertNotNull("No exception was thrown on an incorrect FETCH query hint.", exception);
        assertTrue("Incorrect Exception thrown", exception.getErrorCode() == QueryException.QUERY_HINT_DID_NOT_CONTAIN_ENOUGH_TOKENS);

        exception = null;
        try{
            Query query = em.createQuery("SELECT e FROM Employee e WHERE e.lastName = 'Malone' order by e.firstName");
            query.setHint(EclipseLinkQueryHints.FETCH, "e.abcdef");
        } catch (QueryException exc){
            exception = exc;
        }
        assertNotNull("No exception was thrown on an incorrect FETCH query hint.", exception);
        assertTrue("Incorrect Exception thrown", exception.getErrorCode() == QueryException.QUERY_HINT_NAVIGATED_NON_EXISTANT_RELATIONSHIP);

        exception = null;
        try{
            Query query = em.createQuery("SELECT e FROM Employee e WHERE e.lastName = 'Malone' order by e.firstName");
            query.setHint(EclipseLinkQueryHints.FETCH, "e.firstName");
        } catch (QueryException exc){
            exception = exc;
        }
        assertNotNull("No exception was thrown when an incorrect relationship was navigated in a FETCH query hint.", exception);
        assertTrue("Incorrect Exception thrown", exception.getErrorCode() == QueryException.QUERY_HINT_NAVIGATED_ILLEGAL_RELATIONSHIP);

    }
    
    /*
     * Bug51411440: need to throw IllegalStateException if query executed on closed em
     */
    public void testQueryOnClosedEM() {
        boolean exceptionWasThrown = false;
        EntityManager em = createEntityManager("fieldaccess");
        Query q =  em.createQuery("SELECT e FROM Employee e ");
        em.close();
        if(em.isOpen()) {
            fail("Closed EntityManager is still open");
        }
        try{
            q.getResultList();
        }catch(java.lang.IllegalStateException e){
            exceptionWasThrown=true;
        }
        if (!exceptionWasThrown){
            fail("Query on Closed EntityManager did not throw an exception");
        }
    }
    
    public void testNullifyAddressIn() {
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
            em.createQuery("UPDATE Employee e SET e.address = null WHERE e.address.country IN ('Canada', 'US')").executeUpdate();
        } finally {
            em.getTransaction().rollback();
            em.close();
        }
    }

    //test for bug 5234283: WRONG =* SQL FOR LEFT JOIN ON DERBY AND DB2 PLATFORMS
    public void testLeftJoinOneToOneQuery() {
        EntityManager em = createEntityManager("fieldaccess");
        List results = em.createQuery("SELECT a FROM Employee e LEFT JOIN e.address a").getResultList();
        em.close();
    }

    // test for GlassFish bug 711 - throw a descriptive exception when an uninstantiated valueholder is serialized and then accessed
    public void testSerializedLazy(){
        org.eclipse.persistence.jpa.JpaEntityManager em = (org.eclipse.persistence.jpa.JpaEntityManager) createEntityManager("fieldaccess");      
       
        em.getTransaction().begin();
        
        Employee emp = new Employee();
        emp.setFirstName("Owen");
        emp.setLastName("Hargreaves");
		emp.setId(40);
        Address address = new Address();
        address.setCity("Munich");
        emp.setAddress(address);
        em.persist(emp);
        em.flush();
        em.getTransaction().commit();
        em.close();
        clearCache("fieldaccess");
        em = (org.eclipse.persistence.jpa.JpaEntityManager) createEntityManager("fieldaccess");
        String ejbqlString = "SELECT e FROM Employee e WHERE e.firstName = 'Owen' and e.lastName = 'Hargreaves'";
        List result = em.createQuery(ejbqlString).getResultList();
        emp = (Employee)result.get(0);
        Exception exception = null;
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(byteStream);
                
            stream.writeObject(emp);
            stream.flush();
            byte arr[] = byteStream.toByteArray();
            ByteArrayInputStream inByteStream = new ByteArrayInputStream(arr);
            ObjectInputStream inObjStream = new ObjectInputStream(inByteStream);

            emp = (Employee) inObjStream.readObject();
            emp.getAddress();
        } catch (ValidationException e) {
            if (e.getErrorCode() == ValidationException.INSTANTIATING_VALUEHOLDER_WITH_NULL_SESSION){
                exception = e;
            } else {
                fail("An unexpected exception was thrown while testing serialization of ValueHolders: " + e.toString());
            }
        } catch (Exception e){
            fail("An unexpected exception was thrown while testing serialization of ValueHolders: " + e.toString());
        }

        // Only throw error if weaving was used.
        if (System.getProperty("TEST_NO_WEAVING") == null) {
            assertNotNull("The correct exception was not thrown while traversing an uninstantiated lazy relationship on a serialized object: " + exception, exception);
        }
        em.getTransaction().begin();
        emp = em.find(Employee.class, emp.getId());
        em.remove(emp);
        em.getTransaction().commit();
    }
    
    //test for bug 5170395: GET THE SEQUENCING EXCEPTION WHEN RUNNING FOR THE FIRST TIME ON A CLEAR SCHEMA
    public void testOracleSequenceDefinition() {
        EntityManager em = createEntityManager("fieldaccess");
        ServerSession ss = ((org.eclipse.persistence.internal.jpa.EntityManagerImpl)em).getServerSession();
        if(!ss.getLogin().getPlatform().isOracle()) {
            // Oracle platform is required for this test
            em.close();
            return;
        }
        String seqName = "testOracleSequenceDefinition";
        try {
            // first param is preallocationSize, second is startValue
            // both should be positive
            internalTestOracleSequenceDefinition(10, 1, seqName, em);
            internalTestOracleSequenceDefinition(10, 5, seqName, em);
            internalTestOracleSequenceDefinition(10, 15, seqName, em);
        } finally {
            em.close();
        }
    }

    protected void internalTestOracleSequenceDefinition(int preallocationSize, int startValue, String seqName, EntityManager em) {
        OracleSequenceDefinition def = new OracleSequenceDefinition(seqName, preallocationSize, startValue);
        // create sequence
        String createStr = def.buildCreationWriter(null, new StringWriter()).toString();
        em.createNativeQuery(createStr).executeUpdate();
        try {
            // select first value
            int firstSelectedValue = ((Number)((List)em.createNativeQuery("SELECT "+seqName+".NEXTVAL FROM DUAL").getSingleResult()).get(0)).intValue();

            // calculated the first sequence value to be used by TopLink
            int firstSequenceValue = firstSelectedValue - preallocationSize + 1;

            if(firstSequenceValue != startValue) {
                fail(seqName + " sequence with preallocationSize = "+preallocationSize+" and startValue = " + startValue + " produced wrong firstSequenceValue =" + firstSequenceValue);
            }
        } finally {
            // drop sequence
            String dropStr = def.buildDeletionWriter(null, new StringWriter()).toString();
            em.createNativeQuery(dropStr).executeUpdate();
        }
    }
    
    public void testMergeDetachedObject() {
        // Step 1 - read a department and clear the cache.
        clearCache("fieldaccess");
        EntityManager em = createEntityManager("fieldaccess");
        EJBQueryImpl query = (EJBQueryImpl) em.createNamedQuery("findAllSQLDepartments");
        Collection departments = query.getResultCollection();
        
        Department detachedDepartment;
        
        // This test seems to get called twice. Once with departments populated
        // and a second time with the department table empty.
        if (departments.isEmpty()) {
            em.getTransaction().begin();
            detachedDepartment = new Department();
            detachedDepartment.setName("Department X");
            em.persist(detachedDepartment);
            em.getTransaction().commit();
        } else {
            detachedDepartment = (Department) departments.iterator().next();
        }
        
        em.close();
        clearCache("fieldaccess");
        
        // Step 2 - create a new em, create a new employee with the 
        // detached department and then query the departments again.
        em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        
        Employee emp = new Employee();
        emp.setFirstName("Crazy");
        emp.setLastName("Kid");
		emp.setId(41);
        emp.setDepartment(detachedDepartment);
            
        em.persist(emp);
        em.getTransaction().commit();
        
        try {
            ((EJBQueryImpl) em.createNamedQuery("findAllSQLDepartments")).getResultCollection();
        } catch (NullPointerException e) {
            assertTrue("The detached department caused a null pointer on the query execution.", false);
        }
        
        em.close();
    }
    
    //bug gf830 - attempting to merge a removed entity should throw an IllegalArgumentException
    public void testMergeRemovedObject() {
    	//create an Employee
        Employee emp = new Employee();
        emp.setFirstName("testMergeRemovedObjectEmployee");
		emp.setId(42);
  
        //persist the Employee
        EntityManager em = createEntityManager("fieldaccess");
        try{
            em.getTransaction().begin();
            em.persist(emp);
            em.getTransaction().commit();
        }catch (RuntimeException re){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw re;
        }
        
        em.getTransaction().begin();
        em.remove(emp); 	//attempt to remove the Employee
        try{  
            em.merge(emp);	//then attempt to merge the Employee
            fail("No exception thrown when merging a removed entity is attempted.");
        }catch (IllegalArgumentException iae){
        	//expected
        }catch (Exception e) {
        	fail("Wrong exception type thrown: " + e.getClass());
        }finally {
            em.getTransaction().rollback();
            
            //clean up - ensure removal of employee
            em.getTransaction().begin();	
            em.remove(em.find(Employee.class, emp.getId())); 
            em.getTransaction().commit();
            em.close();
        } 
    }
    
    //merge(null) should throw IllegalArgumentException
    public void testMergeNull(){
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
        	em.merge(null);
        }catch (IllegalArgumentException iae){
        	return;
        }catch (Exception e) {
        	fail("Wrong exception type thrown: " + e.getClass());
        }finally {
            em.getTransaction().rollback();
            em.close();
        }
        fail("No exception thrown when entityManager.merge(null) attempted.");        
    }
    
    //persist(null) should throw IllegalArgumentException
    public void testPersistNull(){
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
        	em.persist(null);
        }catch (IllegalArgumentException iae){
        	return;
        }catch (Exception e) {
        	fail("Wrong exception type thrown: " + e.getClass());
        }finally {
            em.getTransaction().rollback();
            em.close();
        }
        fail("No exception thrown when entityManager.persist(null) attempted.");        
    }
    
    //contains(null) should throw IllegalArgumentException
    public void testContainsNull(){
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
        	em.contains(null);
        }catch (IllegalArgumentException iae){
        	return;
        }catch (Exception e) {
        	fail("Wrong exception type thrown: " + e.getClass());
        }finally {
            em.getTransaction().rollback();
            em.close();
        }
        fail("No exception thrown when entityManager.contains(null) attempted.");        
    }

	//bug gf732 - removing null entity should throw an IllegalArgumentException
    public void testRemoveNull(){
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
        	em.remove(null);
        }catch (IllegalArgumentException iae){
        	return;
        }catch (Exception e) {
        	fail("Wrong exception type thrown: " + e.getClass());
        }finally {
            em.getTransaction().rollback();
            em.close();
        }
        fail("No exception thrown when entityManager.remove(null) attempted.");        
    }
    
    //Glassfish bug 702 - prevent primary key updates
    public void testPrimaryKeyUpdate() {
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();

        Employee emp = new Employee();
        emp.setFirstName("Groucho");
        emp.setLastName("Marx");
        em.persist(emp);

        Integer id = emp.getId();
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        emp.setId(id + 1);
        
        try {
            em.getTransaction().commit();
        } catch (PersistenceException pe) {
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }

            if (pe.getCause() instanceof ValidationException) {
                ValidationException ve = (ValidationException) pe.getCause();
                if (ve.getErrorCode() == ValidationException.PRIMARY_KEY_UPDATE_DISALLOWED) {
                    return;
                } else {
                    fail("Wrong error code for ValidationException: " + ve.getErrorCode());
                }
            } else {
                fail("ValiationException expected, thrown: " + pe.getCause());
            }
        } catch (Exception e) {
            fail("Wrong exception type thrown: " + e.getClass());
        } finally {
            em.close();
        }
        fail("No exception thrown when primary key update attempted.");        
    }
    
    //Glassfish bug 702 - prevent primary key updates, same value is ok
    public void testPrimaryKeyUpdateSameValue() {
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();

        Employee emp = new Employee();
        emp.setFirstName("Harpo");
        emp.setLastName("Marx");
        em.persist(emp);

        Integer id = emp.getId();
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        emp.setId(id);
        
        try {
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }

            fail("Unexpected exception thrown: " + e.getClass());
        } finally {
            em.close();
        }
    }

    //Glassfish bug 702 - prevent primary key updates, overlapping PK/FK
    public void testPrimaryKeyUpdatePKFK() {
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();

        Employee emp = new Employee();
        emp.setFirstName("Groucho");
        emp.setLastName("Marx");
        em.persist(emp);

        Employee emp2 = new Employee();
        emp2.setFirstName("Harpo");
        emp2.setLastName("Marx");
        em.persist(emp2);

        PhoneNumber phone = new PhoneNumber("home", "415", "0007");
        phone.setOwner(emp);
        em.persist(phone);
        em.getTransaction().commit();
        
        em.getTransaction().begin();
        phone.setOwner(emp2);
        
        try {
            em.getTransaction().commit();
        } catch (PersistenceException pe) {
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }

            if (pe.getCause() instanceof ValidationException) {
                ValidationException ve = (ValidationException) pe.getCause();
                if (ve.getErrorCode() == ValidationException.PRIMARY_KEY_UPDATE_DISALLOWED) {
                    return;
                } else {
                    fail("Wrong error code for ValidationException: " + ve.getErrorCode());
                }
            } else {
                fail("ValiationException expected, thrown: " + pe.getCause());
            }
        } catch (Exception e) {
            fail("Wrong exception type thrown: " + e.getClass());
        } finally {
            em.close();
        }
        fail("No exception thrown when primary key update attempted.");        
    }
    
    // Test cascade merge on a detached entity
    public void testCascadeMergeDetached() {
        // setup
        Project p1 = new Project();
        p1.setName("Project1");
        Project p2 = new Project();
        p1.setName("Project2");
        Employee e1 = new Employee();
        e1.setFirstName("Employee1");
        Employee e2 = new Employee();
        e2.setFirstName("Employee2");
        
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
            em.persist(p1);
            em.persist(p2);
            em.persist(e1);
            em.persist(e2);

            em.getTransaction().commit();
        } catch (RuntimeException re){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw re;
        }
        em.close();
        // end of setup

        //p1,p2,e1,e2 are detached
        
        // associate relationships
        //p1 -> e1 (one-to-one)
        p1.setTeamLeader(e1);
        //e1 -> e2 (one-to-many)
        e1.addManagedEmployee(e2);
        //e2 -> p2 (many-to-many)
        e2.addProject(p2);
        p2.addTeamMember(e2);

        em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
            Project mp1 = em.merge(p1); // cascade merge
            assertTrue(em.contains(mp1));
            assertTrue("Managed instance and detached instance must not be same", mp1 != p1);
            
            Employee me1 = mp1.getTeamLeader();
            assertTrue("Cascade merge failed", em.contains(me1));
            assertTrue("Managed instance and detached instance must not be same", me1 != e1);
            
            Employee me2 = me1.getManagedEmployees().iterator().next();
            assertTrue("Cascade merge failed", em.contains(me2));
            assertTrue("Managed instance and detached instance must not be same", me2 != e2);

            Project mp2 = me2.getProjects().iterator().next();
            assertTrue("Cascade merge failed", em.contains(mp2));
            assertTrue("Managed instance and detached instance must not be same", mp2 != p2);

            em.getTransaction().commit();
        } catch (RuntimeException re){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw re;
        }
        em.close();
    }

    // Test cascade merge on a managed entity
    // Test for GF#1139 - Cascade doesn't work when merging managed entity
    public void testCascadeMergeManaged() {
        // setup
        Project p1 = new Project();
        p1.setName("Project1");
        Project p2 = new Project();
        p1.setName("Project2");
        Employee e1 = new Employee();
        e1.setFirstName("Employee1");
        Employee e2 = new Employee();
        e2.setFirstName("Employee2");
            
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
            em.persist(p1);
            em.persist(p2);
            em.persist(e1);
            em.persist(e2);

            em.getTransaction().commit();
        } catch (RuntimeException re){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw re;
        }
        em.close();
        // end of setup
        
        //p1,p2,e1,e2 are detached
        em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
            Project mp1 = em.merge(p1);
            assertTrue(em.contains(mp1));
            assertTrue("Managed instance and detached instance must not be same", mp1 != p1);

            //p1 -> e1 (one-to-one)
            mp1.setTeamLeader(e1);
            mp1 = em.merge(mp1); // merge again - trigger cascade merge

            Employee me1 = mp1.getTeamLeader();
            assertTrue("Cascade merge failed", em.contains(me1));
            assertTrue("Managed instance and detached instance must not be same", me1 != e1);

            //e1 -> e2 (one-to-many)
            me1.addManagedEmployee(e2);
            me1 = em.merge(me1); // merge again - trigger cascade merge
            
            Employee me2 = me1.getManagedEmployees().iterator().next();
            assertTrue("Cascade merge failed", em.contains(me2));
            assertTrue("Managed instance and detached instance must not be same", me2 != e2);

            //e2 -> p2 (many-to-many)
            me2.addProject(p2);
            p2.addTeamMember(me2);
            me2 = em.merge(me2); // merge again - trigger cascade merge

            Project mp2 = me2.getProjects().iterator().next();
            assertTrue("Cascade merge failed", em.contains(mp2));
            assertTrue("Managed instance and detached instance must not be same", mp2 != p2);

            em.getTransaction().commit();
        } catch (RuntimeException re){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw re;
        }
        em.close();
    }

    //Glassfish bug 1021 - allow cascading persist operation to non-entities
    public void testCascadePersistToNonEntitySubclass() {
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();

        Employee emp = new Employee();
        emp.setFirstName("Albert");
        emp.setLastName("Einstein");

        SuperLargeProject s1 = new SuperLargeProject("Super 1");
        Collection projects = new ArrayList();
        projects.add(s1);
        emp.setProjects(projects);
        em.persist(emp);

        try {
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            fail("Persist operation was not cascaded to related non-entity, thrown: " + e);
        } finally {
            em.close();
        }
    }

    /**
     * Bug 801
     * Test to ensure when property access is used and the underlying variable is changed the change
     * is correctly reflected in the database
     * 
     * In this test we test making the change before the object is managed
     */
    public void testInitializeFieldForPropertyAccess(){
        Employee employee = new Employee();
        employee.setFirstName("Andy");
        employee.setLastName("Dufresne");
        Address address = new Address();
        address.setCity("Shawshank");
        employee.setAddressField(address);
        
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        em.persist(employee);
        try{
            em.getTransaction().commit();
        } catch (RuntimeException e){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw e;
        }
        int id = employee.getId();
        
        clearCache("fieldaccess");
        
        employee = (Employee)em.find(Employee.class, new Integer(id));
        address = employee.getAddress();
        
        assertTrue("The address was not persisted.", employee.getAddress() != null);
        assertTrue("The address was not correctly persisted.", employee.getAddress().getCity().equals("Shawshank"));
        
        em.getTransaction().begin();
        employee.setAddress(null);
        em.remove(address);
        em.remove(employee);
        em.getTransaction().commit();
    
    }

    /**
     * Bug 801
     * Test to ensure when property access is used and the underlying variable is changed the change
     * is correctly reflected in the database
     * 
     * In this test we test making the change after the object is managed
     */
    public void testSetFieldForPropertyAccess(){       
        EntityManager em = createEntityManager("fieldaccess");
        
        Employee employee = new Employee();
        employee.setFirstName("Andy");
        employee.setLastName("Dufresne");
        Address address = new Address();
        address.setCity("Shawshank");
        employee.setAddress(address);
        
        em.getTransaction().begin();
        em.persist(employee);
        em.getTransaction().commit();
        int id = employee.getId();
        int addressId = address.getId();
        
        em.getTransaction().begin();
        employee = (Employee)em.find(Employee.class, new Integer(id));
        employee.getAddress();

        address = new Address();
        address.setCity("Metropolis");
        employee.setAddressField(address);
        try{
            em.getTransaction().commit();
        } catch (RuntimeException e){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw e;
        }
        
        clearCache("fieldaccess");
        
        employee = (Employee)em.find(Employee.class, new Integer(id));
        address = employee.getAddress();

        assertTrue("The address was not persisted.", employee.getAddress() != null);
        assertTrue("The address was not correctly persisted.", employee.getAddress().getCity().equals("Metropolis"));
    
        Address initialAddress = (Address)em.find(Address.class, new Integer(addressId));
        em.getTransaction().begin();
        employee.setAddress(null);
        em.remove(address);
        em.remove(employee);
        em.remove(initialAddress);
        em.getTransaction().commit();
    }

    /**
     * Bug 801
     * Test to ensure when property access is used and the underlying variable is changed the change
     * is correctly reflected in the database
     * 
     * In this test we test making the change after the object is refreshed
     */
    public void testSetFieldForPropertyAccessWithRefresh(){       
        EntityManager em = createEntityManager("fieldaccess");
        
        Employee employee = new Employee();
        employee.setFirstName("Andy");
        employee.setLastName("Dufresne");
        Address address = new Address();
        address.setCity("Shawshank");
        employee.setAddress(address);
        
        em.getTransaction().begin();
        em.persist(employee);
        em.getTransaction().commit();
        int id = employee.getId();
        int addressId = address.getId();
        
        em.getTransaction().begin();
        em.refresh(employee);
        employee.getAddress();

        address = new Address();
        address.setCity("Metropolis");
        employee.setAddressField(address);
        try{
            em.getTransaction().commit();
        } catch (RuntimeException e){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw e;
        }
        
        clearCache("fieldaccess");
        
        employee = (Employee)em.find(Employee.class, new Integer(id));
        address = employee.getAddress();

        assertTrue("The address was not persisted.", employee.getAddress() != null);
        assertTrue("The address was not correctly persisted.", employee.getAddress().getCity().equals("Metropolis"));
    
        Address initialAddress = (Address)em.find(Address.class, new Integer(addressId));
        em.getTransaction().begin();
        employee.setAddress(null);
        em.remove(address);
        em.remove(employee);
        em.remove(initialAddress);
        em.getTransaction().commit();
    }
    
    /**
     * Bug 801
     * Test to ensure when property access is used and the underlying variable is changed the change
     * is correctly reflected in the database
     * 
     * In this test we test making the change when an existing object is read into a new EM
     */
    public void testSetFieldForPropertyAccessWithNewEM(){       
        EntityManager em = createEntityManager("fieldaccess");
        
        Employee employee = new Employee();
        employee.setFirstName("Andy");
        employee.setLastName("Dufresne");
        Address address = new Address();
        address.setCity("Shawshank");
        employee.setAddress(address);
        
        em.getTransaction().begin();
        em.persist(employee);
        em.getTransaction().commit();
        int id = employee.getId();
        int addressId = address.getId();
        
        em = createEntityManager("fieldaccess");
        
        em.getTransaction().begin();
        employee = em.find(Employee.class, new Integer(id));
        employee.getAddress();

        address = new Address();
        address.setCity("Metropolis");
        employee.setAddressField(address);
        try{
            em.getTransaction().commit();
        } catch (RuntimeException e){
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw e;
        }
        
        clearCache("fieldaccess");
        
        employee = (Employee)em.find(Employee.class, new Integer(id));
        address = employee.getAddress();

        assertTrue("The address was not persisted.", employee.getAddress() != null);
        assertTrue("The address was not correctly persisted.", employee.getAddress().getCity().equals("Metropolis"));
        
        Address initialAddress = (Address)em.find(Address.class, new Integer(addressId));
        em.getTransaction().begin();
        employee.setAddress(null);
        em.remove(address);
        em.remove(employee);
        em.remove(initialAddress);
        em.getTransaction().commit();
    }
    	//bug gf674 - EJBQL delete query with IS NULL in WHERE clause produces wrong sql
     public void testDeleteAllPhonesWithNullOwner() {
         EntityManager em = createEntityManager("fieldaccess");
         em.getTransaction().begin();
         try {
         	em.createQuery("DELETE FROM PhoneNumber ph WHERE ph.owner IS NULL").executeUpdate();
         } catch (Exception e) {
         	fail("Exception thrown: " + e.getClass());
         } finally {
            if (em.getTransaction().isActive()){
                 em.getTransaction().rollback();
            }
             em.close();
         }
     }
     public void testDeleteAllProjectsWithNullTeamLeader() {
         internalDeleteAllProjectsWithNullTeamLeader("Project");
     }
     public void testDeleteAllSmallProjectsWithNullTeamLeader() {
         internalDeleteAllProjectsWithNullTeamLeader("SmallProject");
     }
     public void testDeleteAllLargeProjectsWithNullTeamLeader() {
         internalDeleteAllProjectsWithNullTeamLeader("LargeProject");
     }
     protected void internalDeleteAllProjectsWithNullTeamLeader(String className) {
         String name = "testDeleteAllProjectsWithNull";
         
         // setup
         SmallProject sp = new SmallProject();
         sp.setName(name);
         LargeProject lp = new LargeProject();
         lp.setName(name);
         EntityManager em = createEntityManager("fieldaccess");
         try {
             em.getTransaction().begin();
             // make sure there are no pre-existing objects with this name
           	em.createQuery("DELETE FROM "+className+" p WHERE p.name = '"+name+"'").executeUpdate();
             em.persist(sp);
             em.persist(lp);
             em.getTransaction().commit();
         } catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                 em.getTransaction().rollback();
            }
             throw ex;
         } finally {
             em.close();
         }
                 
         // test
         em = createEntityManager("fieldaccess");
         em.getTransaction().begin();
         try {
         	em.createQuery("DELETE FROM "+className+" p WHERE p.name = '"+name+"' AND p.teamLeader IS NULL").executeUpdate();
             em.getTransaction().commit();
         } catch (RuntimeException e) {
            if (em.getTransaction().isActive()){
                 em.getTransaction().rollback();
            }
            throw e;
         } finally {
             em.close();
         }

         // verify
         String error = null;
         em = createEntityManager("fieldaccess");
         List result = em.createQuery("SELECT OBJECT(p) FROM Project p WHERE p.name = '"+name+"'").getResultList();
         if(result.isEmpty()) {
             if(!className.equals("Project")) {
                 error = "Target Class " + className +": no objects left";
             }
         } else {
             if(result.size() > 1) {
                 error = "Target Class " + className +": too many objects left: " + result.size();
             } else {
                 Project p = (Project)result.get(0);
                 if(p.getClass().getName().endsWith(className)) {
                     error = "Target Class " + className +": object of wrong type left: " + p.getClass().getName();
                 }
             }
         }

         // clean up
         try {
             em.getTransaction().begin();
             // make sure there are no pre-existing objects with this name
           	em.createQuery("DELETE FROM "+className+" p WHERE p.name = '"+name+"'").executeUpdate();
             em.getTransaction().commit();
         } catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                 em.getTransaction().rollback();
            }
             throw ex;
         } finally {
             em.close();
         }
         
         if(error != null) {
             fail(error);
         }
     }
    
    // gf1408: DeleteAll and UpdateAll queries broken on some db platforms;
    // gf1451: Complex updates to null using temporary storage do not work on Derby;
    // gf1860: TopLink provides too few values.
    // The tests forces the use of temporary storage to test null assignment to an integer field
    // on all platforms.
    public void testUpdateUsingTempStorage() {
        internalUpdateUsingTempStorage(false);
    }
    public void testUpdateUsingTempStorageWithParameter() {
        internalUpdateUsingTempStorage(true);
    }
    protected void internalUpdateUsingTempStorage(boolean useParameter) {
        String firstName = "testUpdateUsingTempStorage";
        int n = 3;
        
        // setup
        EntityManager em = createEntityManager("fieldaccess");
        try {
            em.getTransaction().begin();
            // make sure there are no pre-existing objects with this name
           	em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
           	em.createQuery("DELETE FROM Address a WHERE a.country = '"+firstName+"'").executeUpdate();
            // populate Employees
            for(int i=1; i<=n; i++) {
                Employee emp = new Employee();
                emp.setFirstName(firstName);
                emp.setLastName(Integer.toString(i));
                emp.setSalary(i*100);
                emp.setRoomNumber(i);
                
                Address address = new Address();
                address.setCountry(firstName);
                address.setCity(Integer.toString(i));
                
                emp.setAddress(address);

                em.persist(emp);
            }
            em.getTransaction().commit();
        } catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                 em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
                
        // test
        em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        int nUpdated = 0;
        try {
            if(useParameter) {
                nUpdated = em.createQuery("UPDATE Employee e set e.salary = e.roomNumber, e.roomNumber = e.salary, e.address = :address where e.firstName = '" + firstName + "'").setParameter("address", null).executeUpdate();
            } else {
                nUpdated = em.createQuery("UPDATE Employee e set e.salary = e.roomNumber, e.roomNumber = e.salary, e.address = null where e.firstName = '" + firstName + "'").executeUpdate();
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()){
                 em.getTransaction().rollback();
            }
        	fail("Exception thrown: " + e.getClass());
        } finally {
            em.close();
        }

        // verify
        String error = null;
        em = createEntityManager("fieldaccess");
        List result = em.createQuery("SELECT OBJECT(e) FROM Employee e WHERE e.firstName = '"+firstName+"'").getResultList();
        em.close();
        int nReadBack = result.size();
        if(n != nUpdated) {
            error = "n = "+n+", but nUpdated ="+nUpdated+";";
        }
        if(n != nReadBack) {
            error = " n = "+n+", but nReadBack ="+nReadBack+";";
        }
        for(int i=0; i<nReadBack; i++) {
            Employee emp = (Employee)result.get(i);
            if(emp.getAddress() != null) {
                error = " Employee "+emp.getLastName()+" still has address;";
            }
            int ind = Integer.valueOf(emp.getLastName()).intValue();
            if(emp.getSalary() != ind) {
                error = " Employee "+emp.getLastName()+" has wrong salary "+emp.getSalary()+";";
            }
            if(emp.getRoomNumber() != ind*100) {
                error = " Employee "+emp.getLastName()+" has wrong roomNumber "+emp.getRoomNumber()+";";
            }
        }

        // clean up
        em = createEntityManager("fieldaccess");
        try {
            em.getTransaction().begin();
            // make sure there are no objects left with this name
          	em.createQuery("DELETE FROM Employee e WHERE e.firstName = '"+firstName+"'").executeUpdate();
           	em.createQuery("DELETE FROM Address a WHERE a.country = '"+firstName+"'").executeUpdate();
            em.getTransaction().commit();
        } catch (RuntimeException ex){
            if (em.getTransaction().isActive()){
                 em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
        
        if(error != null) {
            fail(error);
        }
    }

    protected void createProjectsWithName(String name, Employee teamLeader) {
        EntityManager em = createEntityManager("fieldaccess");
        try {
            em.getTransaction().begin();

            SmallProject sp = new SmallProject();
            sp.setName(name);

            LargeProject lp = new LargeProject();
            lp.setName(name);

            em.persist(sp);
            em.persist(lp);
            
            if(teamLeader != null) {
                SmallProject sp2 = new SmallProject();
                sp2.setName(name);
                sp2.setTeamLeader(teamLeader);
    
                LargeProject lp2 = new LargeProject();
                lp2.setName(name);
                lp2.setTeamLeader(teamLeader);
    
                em.persist(sp2);
                em.persist(lp2);   
            }

            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            if(em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    protected void deleteProjectsWithName(String name) {
        EntityManager em = createEntityManager("fieldaccess");
        try {
            em.getTransaction().begin();

          	em.createQuery("DELETE FROM Project p WHERE p.name = '"+name+"'").executeUpdate();
            
            em.getTransaction().commit();
        } catch (RuntimeException ex) {
            if(em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void testUpdateAllSmallProjects() {
        internalTestUpdateAllProjects(SmallProject.class);
    }
    public void testUpdateAllLargeProjects() {
        internalTestUpdateAllProjects(LargeProject.class);
    }
    public void testUpdateAllProjects() {
        internalTestUpdateAllProjects(Project.class);
    }
    protected void internalTestUpdateAllProjects(Class cls) {
        String className = Helper.getShortClassName(cls);
        String name = "testUpdateAllProjects";
        String newName = "testUpdateAllProjectsNEW";
        HashMap map = null;
        boolean ok = false;
        
        try {
            // setup
            // populate Projects - necessary only if no SmallProject and/or LargeProject objects already exist.
            createProjectsWithName(name, null);
            // save the original names of projects: will set them back in cleanup
            // to restore the original state.
            EntityManager em = createEntityManager("fieldaccess");
            List projects = em.createQuery("SELECT OBJECT(p) FROM Project p").getResultList();
            map = new HashMap(projects.size());
            for(int i=0; i<projects.size(); i++) {
                Project p = (Project)projects.get(i);
                map.put(p.getId(), p.getName());
            }        
    
            // test
            em.getTransaction().begin();
            try {
                em.createQuery("UPDATE "+className+" p set p.name = '"+newName+"'").executeUpdate();
                em.getTransaction().commit();
            } catch (RuntimeException ex) {
                if(em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw ex;
            } finally {
                em.close();
            }
            
            // verify
            em = createEntityManager("fieldaccess");
            String errorMsg = "";
            projects = em.createQuery("SELECT OBJECT(p) FROM Project p").getResultList();
            for(int i=0; i<projects.size(); i++) {
                Project p = (Project)projects.get(i);
                String readName = p.getName();
                if(cls.isInstance(p)) {
                    if(!newName.equals(readName)) {
                        errorMsg = errorMsg + "haven't updated name: " + p + "; ";
                    }
                } else {
                    if(newName.equals(readName)) {
                        errorMsg = errorMsg + "have updated name: " + p + "; ";
                    }
                }
            }
            em.close();

            if(errorMsg.length() > 0) {
                fail(errorMsg);
            } else {
                ok = true;
            }
        } finally {
            // clean-up
            try {
                if(map != null) {
                    EntityManager em = createEntityManager("fieldaccess");
                    List projects = em.createQuery("SELECT OBJECT(p) FROM Project p").getResultList();
                    em.getTransaction().begin();
                    try {
                        for(int i=0; i<projects.size(); i++) {
                            Project p = (Project)projects.get(i);
                            String oldName = (String)map.get(((Project)projects.get(i)).getId());
                            p.setName(oldName);
                        }
                        em.getTransaction().commit();
                    } catch (RuntimeException ex) {
                        if(em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        throw ex;
                    } finally {
                        em.close();
                    }
                }
                // delete projects that createProjectsWithName has created in setup
                deleteProjectsWithName(name);
            } catch (RuntimeException ex) {
                // eat clean-up exception in case the test failed
                if(ok) {
                    throw ex;
                }
            }
        }        
    }
    
    public void testUpdateAllSmallProjectsWithName() {
        internalTestUpdateAllProjectsWithName(SmallProject.class);
    }
    public void testUpdateAllLargeProjectsWithName() {
        internalTestUpdateAllProjectsWithName(LargeProject.class);
    }
    public void testUpdateAllProjectsWithName() {
        internalTestUpdateAllProjectsWithName(Project.class);
    }
    protected void internalTestUpdateAllProjectsWithName(Class cls) {
        String className = Helper.getShortClassName(cls);
        String name = "testUpdateAllProjects";
        String newName = "testUpdateAllProjectsNEW";
        boolean ok = false;
        
        try {
            // setup
            // make sure no projects with the specified names exist
            deleteProjectsWithName(name);
            deleteProjectsWithName(newName);
            // populate Projects
            createProjectsWithName(name, null);
    
            // test
            EntityManager em = createEntityManager("fieldaccess");
            em.getTransaction().begin();
            try {
                em.createQuery("UPDATE "+className+" p set p.name = '"+newName+"' WHERE p.name = '"+name+"'").executeUpdate();
                em.getTransaction().commit();
            } catch (RuntimeException ex) {
                if(em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw ex;
            } finally {
                em.close();
            }
            
            // verify
            em = createEntityManager("fieldaccess");
            String errorMsg = "";
            List projects = em.createQuery("SELECT OBJECT(p) FROM Project p WHERE p.name = '"+newName+"' OR p.name = '"+name+"'").getResultList();
            for(int i=0; i<projects.size(); i++) {
                Project p = (Project)projects.get(i);
                String readName = p.getName();
                if(cls.isInstance(p)) {
                    if(!readName.equals(newName)) {
                        errorMsg = errorMsg + "haven't updated name: " + p + "; ";
                    }
                } else {
                    if(readName.equals(newName)) {
                        errorMsg = errorMsg + "have updated name: " + p + "; ";
                    }
                }
            }
            em.close();
            
            if(errorMsg.length() > 0) {
                fail(errorMsg);
            } else {
                ok = true;
            }
        } finally {
            // clean-up
            // make sure no projects with the specified names left
            try {
                deleteProjectsWithName(name);
                deleteProjectsWithName(newName);
            } catch (RuntimeException ex) {
                // eat clean-up exception in case the test failed
                if(ok) {
                    throw ex;
                }
            }
        }
    }
    
    public void testUpdateAllSmallProjectsWithNullTeamLeader() {
        internalTestUpdateAllProjectsWithNullTeamLeader(SmallProject.class);
    }
    public void testUpdateAllLargeProjectsWithNullTeamLeader() {
        internalTestUpdateAllProjectsWithNullTeamLeader(LargeProject.class);
    }
    public void testUpdateAllProjectsWithNullTeamLeader() {
        internalTestUpdateAllProjectsWithNullTeamLeader(Project.class);
    }
    protected void internalTestUpdateAllProjectsWithNullTeamLeader(Class cls) {
        String className = Helper.getShortClassName(cls);
        String name = "testUpdateAllProjects";
        String newName = "testUpdateAllProjectsNEW";
        Employee empTemp = null;
        boolean ok = false;
        
        try {
            // setup
            // make sure no projects with the specified names exist
            deleteProjectsWithName(name);
            deleteProjectsWithName(newName);
            EntityManager em = createEntityManager("fieldaccess");
            Employee emp = null;
            List employees = em.createQuery("SELECT OBJECT(e) FROM Employee e").getResultList();
            if(employees.size() > 0) {
                emp = (Employee)employees.get(0);
            } else {
                em.getTransaction().begin();
                try {
                    emp = new Employee();
                    emp.setFirstName(name);
                    emp.setLastName("TeamLeader");
                    em.persist(emp);
                    em.getTransaction().commit();
                    empTemp = emp;
                } catch (RuntimeException ex) {
                    if(em.getTransaction().isActive()) {
                        em.getTransaction().rollback();
                    }
                    em.close();
                    throw ex;
                }
            }
            em.close();
            // populate Projects
            createProjectsWithName(name, emp);
    
            // test
            em = createEntityManager("fieldaccess");
            em.getTransaction().begin();
            try {
                em.createQuery("UPDATE "+className+" p set p.name = '"+newName+"' WHERE p.name = '"+name+"' AND p.teamLeader IS NULL").executeUpdate();
                em.getTransaction().commit();
            } catch (RuntimeException ex) {
                if(em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw ex;
            } finally {
                em.close();
            }
            
            // verify
            em = createEntityManager("fieldaccess");
            String errorMsg = "";
            List projects = em.createQuery("SELECT OBJECT(p) FROM Project p WHERE p.name = '"+newName+"' OR p.name = '"+name+"'").getResultList();
            for(int i=0; i<projects.size(); i++) {
                Project p = (Project)projects.get(i);
                String readName = p.getName();
                if(cls.isInstance(p) && p.getTeamLeader()==null) {
                    if(!readName.equals(newName)) {
                        errorMsg = errorMsg + "haven't updated name: " + p + "; ";
                    }
                } else {
                    if(readName.equals(newName)) {
                        errorMsg = errorMsg + "have updated name: " + p + "; ";
                    }
                }
            }
            em.close();
            
            if(errorMsg.length() > 0) {
                fail(errorMsg);
            } else {
                ok = true;
            }
        } finally {
            // clean-up
            // make sure no projects with the specified names exist
            try {
                deleteProjectsWithName(name);
                deleteProjectsWithName(newName);
                if(empTemp != null) {
                    EntityManager em = createEntityManager("fieldaccess");
                    em.getTransaction().begin();
                    try {
                        em.createQuery("DELETE FROM Employee e WHERE e.id = '"+empTemp.getId()+"'").executeUpdate();
                        em.getTransaction().commit();
                    } catch (RuntimeException ex) {
                        if(em.getTransaction().isActive()) {
                            em.getTransaction().rollback();
                        }
                        throw ex;
                    } finally {
                        em.close();
                    }
                }
            } catch (RuntimeException ex) {
                // eat clean-up exception in case the test failed
                if(ok) {
                    throw ex;
                }
            }
        }
    }
    
    public void testRollbackOnlyOnException() {
        EntityManager em = createEntityManager("fieldaccess");
        em.getTransaction().begin();
        try {
            Employee emp = em.find(Employee.class, "");
            fail("IllegalArgumentException has not been thrown");
        } catch(IllegalArgumentException ex) {
            assertTrue("Transaction is not roll back only", em.getTransaction().getRollbackOnly());
        } finally {
            em.getTransaction().rollback();
            em.close();
        }
    }

    public void testClosedEmShouldThrowException() {
        EntityManager em = createEntityManager("fieldaccess");
        em.close();
        String errorMsg = "";

        try {
            em.clear();
            errorMsg = errorMsg + "; em.clear() didn't throw exception";
        } catch(IllegalStateException ise) {
            // expected
        } catch(RuntimeException ex) {
            errorMsg = errorMsg + "; em.clear() threw wrong exception: " + ex.getMessage();
        }
        try {
            em.close();
            errorMsg = errorMsg + "; em.close() didn't throw exception";
        } catch(IllegalStateException ise) {
            // expected
        } catch(RuntimeException ex) {
            errorMsg = errorMsg + "; em.close() threw wrong exception: " + ex.getMessage();
        }
        try {
            em.contains(null);
            errorMsg = errorMsg + "; em.contains() didn't throw exception";
        } catch(IllegalStateException ise) {
            // expected
        } catch(RuntimeException ex) {
            errorMsg = errorMsg + "; em.contains threw() wrong exception: " + ex.getMessage();
        }
        try {
            em.getDelegate();
            errorMsg = errorMsg + "; em.getDelegate() didn't throw exception";
        } catch(IllegalStateException ise) {
            // expected
        } catch(RuntimeException ex) {
            errorMsg = errorMsg + "; em.getDelegate() threw wrong exception: " + ex.getMessage();
        }
        try {
            em.getReference(Employee.class, new Integer(1));
            errorMsg = errorMsg + "; em.getReference() didn't throw exception";
        } catch(IllegalStateException ise) {
            // expected
        } catch(RuntimeException ex) {
            errorMsg = errorMsg + "; em.getReference() threw wrong exception: " + ex.getMessage();
        }
        try {
            em.joinTransaction();
            errorMsg = errorMsg + "; em.joinTransaction() didn't throw exception";
        } catch(IllegalStateException ise) {
            // expected
        } catch(RuntimeException ex) {
            errorMsg = errorMsg + "; em.joinTransaction() threw wrong exception: " + ex.getMessage();
        }
        try {
            em.lock(null, null);
            errorMsg = errorMsg + "; em.lock() didn't throw exception";
        } catch(IllegalStateException ise) {
            // expected
        } catch(RuntimeException ex) {
            errorMsg = errorMsg + "; em.lock() threw wrong exception: " + ex.getMessage();
        }
        
        if(errorMsg.length() > 0) {
            fail(errorMsg);
        }
    }
    
    //gf 1217 - Ensure join table defaults correctly when 'mappedby' not specified
    public void testOneToManyDefaultJoinTableName() {
        Department dept  = new Department();
        Employee manager = new Employee();
        dept.addManager(manager);
        
        EntityManager em = createEntityManager("fieldaccess");
        try {
            em.getTransaction().begin();
            em.persist(dept);
            em.getTransaction().commit();
        }catch (RuntimeException e) {
            throw e;
        }finally {
            if (em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
    
    // gf1732
    public void testMultipleEntityManagerFactories() {
        // close the original factory
        closeEntityManagerFactory();
        // create the new one - not yet deployed
        EntityManagerFactory factory1 =  getEntityManagerFactory("fieldaccess");
        // create the second one
        EntityManagerFactory factory2 =  Persistence.createEntityManagerFactory("fieldaccess", JUnitTestCaseHelper.getDatabaseProperties());
        // deploy
        factory2.createEntityManager();
        // close
        factory2.close();

        try {
            // now try to getEM from the first one - this used to throw exception
            factory1.createEntityManager();
            // don't close factory1 if all is well
        } catch (PersistenceException ex) {
            fail("factory1.createEM threw exception: " + ex.getMessage());
            factory1.close();
        }
    }

    // gf2074: EM.clear throws NPE
    public void testClearEntityManagerWithoutPersistenceContext() {
        EntityManager em = createEntityManager("fieldaccess");
        try {
            em.clear();
        }finally {
            em.close();
        }
    }
    
    // Used by testClearEntityManagerWithoutPersistenceContextSimulateJTA().
    // At first tried to use JTATransactionController class, but that introduced dependencies 
    // on javax.transaction package (and therefore failed in gf entity persistence tests).
    static class DummyExternalTransactionController extends org.eclipse.persistence.transaction.AbstractTransactionController {
        public boolean isRolledBack_impl(Object status){return false;}
        protected void registerSynchronization_impl(org.eclipse.persistence.transaction.AbstractSynchronizationListener listener, Object txn) throws Exception{}
        protected Object getTransaction_impl() throws Exception {return null;}
        protected Object getTransactionKey_impl(Object transaction) throws Exception {return null;}
        protected Object getTransactionStatus_impl() throws Exception {return null;}
        protected void beginTransaction_impl() throws Exception{}
        protected void commitTransaction_impl() throws Exception{}
        protected void rollbackTransaction_impl() throws Exception{}
        protected void markTransactionForRollback_impl() throws Exception{}
        protected boolean canBeginTransaction_impl(Object status){return false;}
        protected boolean canCommitTransaction_impl(Object status){return false;}
        protected boolean canRollbackTransaction_impl(Object status){return false;}
        protected boolean canIssueSQLToDatabase_impl(Object status){return false;}
        protected boolean canMergeUnitOfWork_impl(Object status){return false;}
        protected String statusToString_impl(Object status){return "";}
    }
    // gf2074: EM.clear throws NPE (JTA case)
    public void testClearEntityManagerWithoutPersistenceContextSimulateJTA() {
        EntityManager em = createEntityManager("fieldaccess");
        ServerSession ss = ((org.eclipse.persistence.jpa.JpaEntityManager)em).getServerSession();
        em.close();
        // in non-JTA case session doesn't have external transaction controller
        boolean hasExternalTransactionController = ss.hasExternalTransactionController();
        if(!hasExternalTransactionController) {
            // simulate JTA case
            ss.setExternalTransactionController(new DummyExternalTransactionController());
        }
        try {
            testClearEntityManagerWithoutPersistenceContext();
        }finally {
            if(!hasExternalTransactionController) {
                // remove the temporary set TransactionController
                ss.setExternalTransactionController(null);
            }
        }
    }
    
    public void testDescriptorNamedQuery(){
        ReadAllQuery query = new ReadAllQuery(Employee.class);
        ExpressionBuilder builder = new ExpressionBuilder();
        Expression exp = builder.get("firstName").equal(builder.getParameter("fName"));
        exp = exp.and(builder.get("lastName").equal(builder.getParameter("lName")));
        query.setSelectionCriteria(exp);
        query.addArgument("fName", String.class);
        query.addArgument("lName", String.class);
        
        EntityManager em = createEntityManager("fieldaccess");
        Session session = ((EntityManagerImpl)em.getDelegate()).getServerSession();
        ClassDescriptor descriptor = session.getDescriptor(Employee.class);
        descriptor.getQueryManager().addQuery("findByFNameLName", query);

        em.getTransaction().begin();
        try {
            Employee emp = new Employee();
            emp.setFirstName("Melvin");
            emp.setLastName("Malone");
            em.persist(emp);
            em.flush();
            
            Query ejbQuery = ((org.eclipse.persistence.jpa.JpaEntityManager)em).createDescriptorNamedQuery("findByFNameLName", Employee.class);
            
            List results = ejbQuery.setParameter("fName", "Melvin").setParameter("lName", "Malone").getResultList();
            
            assertTrue(results.size() == 1);
            emp = (Employee)results.get(0);
            assertTrue(emp.getFirstName().equals("Melvin"));
            assertTrue(emp.getLastName().equals("Malone"));
        } finally {
            em.getTransaction().rollback();
            em.close();
        }
        
        descriptor.getQueryManager().removeQuery("findByFNameLName");
    }
    
    public void testDescriptorNamedQueryForMultipleQueries(){
        ReadAllQuery query = new ReadAllQuery(Employee.class);
        ExpressionBuilder builder = new ExpressionBuilder();
        Expression exp = builder.get("firstName").equal(builder.getParameter("fName"));
        exp = exp.and(builder.get("lastName").equal(builder.getParameter("lName")));
        query.setSelectionCriteria(exp);
        query.addArgument("fName", String.class);
        query.addArgument("lName", String.class);
        
        ReadAllQuery query2 = new ReadAllQuery(Employee.class);

        
        EntityManager em = createEntityManager("fieldaccess");
        Session session = ((EntityManagerImpl)em.getDelegate()).getServerSession();
        ClassDescriptor descriptor = session.getDescriptor(Employee.class);
        descriptor.getQueryManager().addQuery("findEmployees", query);
        descriptor.getQueryManager().addQuery("findEmployees", query2);

        em.getTransaction().begin();
        try {
        Employee emp = new Employee();
            emp.setFirstName("Melvin");
            emp.setLastName("Malone");
            em.persist(emp);
            em.flush();
    
            Vector args = new Vector(2);
            args.addElement(String.class);
            args.addElement(String.class);
            Query ejbQuery = ((org.eclipse.persistence.jpa.JpaEntityManager)em).createDescriptorNamedQuery("findEmployees", Employee.class, args);
            
            List results = ejbQuery.setParameter("fName", "Melvin").setParameter("lName", "Malone").getResultList();
            
            assertTrue(results.size() == 1);
            emp = (Employee)results.get(0);
            assertTrue(emp.getFirstName().equals("Melvin"));
            assertTrue(emp.getLastName().equals("Malone"));
        } finally {
            em.getTransaction().rollback();
            em.close();
        }
        
        descriptor.getQueryManager().removeQuery("findEmployees");
    }
    
    // GF 2621
    public void testDoubleMerge(){
        EntityManager em = createEntityManager("fieldaccess");
        
        Employee employee = new Employee();
        employee.setId(44);
        employee.setVersion(0);
        employee.setFirstName("Alfie");

        Employee employee2 = new Employee();
        employee2.setId(44);
        employee2.setVersion(0);
        employee2.setFirstName("Phillip");        
        
        try {
            em.getTransaction().begin();
            em.merge(employee);
            em.merge(employee2);
            em.flush();
        } catch (PersistenceException e){
            fail("A double merge of an object with the same key, caused two inserts instead of one.");
        } finally {
            em.getTransaction().rollback();
        }
    }

    
    /**
     * Test that all of the classes in the advanced model were weaved as expected.
     */
    public void testWeaving() {
        // Only test if weaving was on, test runs without weaving must set this system property.
        if (System.getProperty("TEST_NO_WEAVING") == null) {
            internalTestWeaving(new Employee(), true, true);
            internalTestWeaving(new FormerEmployment(), true, false);
            internalTestWeaving(new Address(), true, false);
            internalTestWeaving(new PhoneNumber(), true, false);
            internalTestWeaving(new EmploymentPeriod(), true, false);
            internalTestWeaving(new Buyer(), false, false);  // field-locking
            internalTestWeaving(new GoldBuyer(), false, false);  // field-locking
            internalTestWeaving(new PlatinumBuyer(), false, false);  // field-locking
            internalTestWeaving(new Department(), false, false);  // eager 1-m
            internalTestWeaving(new Golfer(), true, false);
            internalTestWeaving(new GolferPK(), true, false);
            internalTestWeaving(new SmallProject(), true, false);
            internalTestWeaving(new LargeProject(), true, false);
            internalTestWeaving(new SuperLargeProject(), true, false);
            internalTestWeaving(new Man(), true, false);
            internalTestWeaving(new Woman(), true, false);
            internalTestWeaving(new Vegetable(), false, false);  // serialized
            internalTestWeaving(new VegetablePK(), false, false);
            internalTestWeaving(new WorldRank(), true, false);
            internalTestWeaving(new Equipment(), true, false);
            internalTestWeaving(new EquipmentCode(), true, false);
            internalTestWeaving(new PartnerLink(), true, false);
        }
    }
    
    /**
     * Test that the object was weaved.
     */
    public void internalTestWeaving(Object object, boolean changeTracking, boolean indirection) {
        if (!(object instanceof PersistenceWeaved)) {
            fail("Object not weaved:" + object);
        }
        if (indirection && (!(object instanceof PersistenceWeavedLazy))) {
            fail("Object not weaved for indirection:" + object);
        }
        if (changeTracking && (!(object instanceof ChangeTracker))) {
            fail("Object not weaved for ChangeTracker:" + object);
        }
        ClassDescriptor descriptor = getServerSession("fieldaccess").getDescriptor(object);
        if (!descriptor.isAggregateDescriptor()) {
            if (changeTracking != descriptor.getObjectChangePolicy().isAttributeChangeTrackingPolicy()) {
                fail("Descriptor not set to use change tracking policy correctly:" + object);
            }
            if (!(object instanceof PersistenceEntity)) {
                fail("Object not weaved for PersistenceEntity:" + object);
            }
            if (!(object instanceof FetchGroupTracker)) {
                fail("Object not weaved for FetchGroupTracker:" + object);
            }
        }
    }
}
