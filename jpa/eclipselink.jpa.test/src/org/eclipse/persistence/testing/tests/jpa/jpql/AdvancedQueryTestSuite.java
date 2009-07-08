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

package org.eclipse.persistence.testing.tests.jpa.jpql;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;
import javax.persistence.EntityManager;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.persistence.config.CacheUsage;
import org.eclipse.persistence.config.QueryHints;
import org.eclipse.persistence.config.QueryType;
import org.eclipse.persistence.config.ResultSetConcurrency;
import org.eclipse.persistence.config.ResultSetType;
import org.eclipse.persistence.config.ResultType;
import org.eclipse.persistence.descriptors.invalidation.DailyCacheInvalidationPolicy;
import org.eclipse.persistence.descriptors.invalidation.TimeToLiveCacheInvalidationPolicy;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.jpa.JpaQuery;
import org.eclipse.persistence.queries.Cursor;
import org.eclipse.persistence.queries.ReadQuery;
import org.eclipse.persistence.queries.ScrollableCursor;
import org.eclipse.persistence.sessions.DatabaseSession;

import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
import org.eclipse.persistence.testing.framework.QuerySQLTracker;
import org.eclipse.persistence.testing.models.jpa.advanced.Employee;
import org.eclipse.persistence.testing.models.jpa.advanced.EmployeePopulator;
import org.eclipse.persistence.testing.models.jpa.advanced.AdvancedTableCreator;


/**
 * <p>
 * <b>Purpose</b>: Test advanced JPA Query functionality.
 * <p>
 * <b>Description</b>: This tests query hints, caching and query optimization.
 * <p>
 */
public class AdvancedQueryTestSuite extends JUnitTestCase {

    static JUnitDomainObjectComparer comparer; //the global comparer object used in all tests

    public AdvancedQueryTestSuite() {
        super();
    }

    public AdvancedQueryTestSuite(String name) {
        super(name);
    }

    // This method is run at the start of EVERY test case method.

    public void setUp() {

    }

    // This method is run at the end of EVERY test case method.

    public void tearDown() {
        clearCache();
    }

    //This suite contains all tests contained in this class

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName("AdvancedQueryTestSuite");
        suite.addTest(new AdvancedQueryTestSuite("testSetup"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryCacheFirstCacheHits"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryCacheOnlyCacheHits"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryCacheOnlyCacheHitsOnSession"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryPrimaryKeyCacheHits"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryExactPrimaryKeyCacheHits"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryTypeCacheHits"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryCache"));
/* KERNEL-SRG-TEMP
        suite.addTest(new AdvancedQueryTestSuite("testQueryREADLock"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryWRITELock"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryOPTIMISTICLock"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryOPTIMISTIC_FORCE_INCREMENTLock"));
        // Temporary removal of JPA 2.0 dependency
        //suite.addTest(new AdvancedQueryTestSuite("testQueryPESSIMISTICLock"));
        suite.addTest(new AdvancedQueryTestSuite("testQueryPESSIMISTIC_FORCE_INCREMENTLock"));
        // Temporary removal of JPA 2.0 dependency
        //suite.addTest(new AdvancedQueryTestSuite("testQueryPESSIMISTICTIMEOUTLock"));
*/        
        suite.addTest(new AdvancedQueryTestSuite("testObjectResultType"));
        suite.addTest(new AdvancedQueryTestSuite("testNativeResultType"));
        suite.addTest(new AdvancedQueryTestSuite("testCursors"));
        suite.addTest(new AdvancedQueryTestSuite("testFetchGroups"));
        suite.addTest(new AdvancedQueryTestSuite("testMultipleNamedJoinFetchs"));
        
        return suite;
    }
    
    /**
     * The setup is done as a test, both to record its failure, and to allow execution in the server.
     */
    public void testSetup() {
        clearCache();
        DatabaseSession session = JUnitTestCase.getServerSession();
        //create a new EmployeePopulator
        EmployeePopulator employeePopulator = new EmployeePopulator();
        new AdvancedTableCreator().replaceTables(session);
        //initialize the global comparer object
        comparer = new JUnitDomainObjectComparer();
        //set the session for the comparer to use
        comparer.setSession((AbstractSession)session.getActiveSession());
        //Populate the tables
        employeePopulator.buildExamples();
        //Persist the examples in the database
        employeePopulator.persistExample(session);
    }
    
    /**
     * Test that a cache hit will occur on a primary key query.
     */
    public void testQueryPrimaryKeyCacheHits() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        QuerySQLTracker counter = null;
        try {
            // Load an employee into the cache.  
            Query query = em.createQuery("Select employee from Employee employee");
            List result = query.getResultList();
            Employee employee = (Employee)result.get(0);

            // Count SQL.
            counter = new QuerySQLTracker(getServerSession());
            // Query by primary key.
            query = em.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheByPrimaryKey);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            Employee queryResult = (Employee)query.getSingleResult();
            if (queryResult != employee) {
                fail("Employees are not equal: " + employee + ", " + queryResult);
            }
            if (counter.getSqlStatements().size() > 0) {
                fail("Cache hit do not occur: " + counter.getSqlStatements());
            }
        } finally {
            rollbackTransaction(em);
            if (counter != null) {
                counter.remove();
            }
        }
    }

    /**
     * Test that a cache hit will occur on a primary key query.
     */
    public void testQueryTypeCacheHits() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        QuerySQLTracker counter = null;
        try {
            // Load an employee into the cache.  
            Query query = em.createQuery("Select employee from Employee employee");
            List result = query.getResultList();
            Employee employee = (Employee)result.get(0);

            // Count SQL.
            counter = new QuerySQLTracker(getServerSession());
            // Query by primary key.
            query = em.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setHint(QueryHints.QUERY_TYPE, QueryType.ReadObject);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            Employee queryResult = (Employee)query.getSingleResult();
            if (queryResult != employee) {
                fail("Employees are not equal: " + employee + ", " + queryResult);
            }
            if (counter.getSqlStatements().size() > 0) {
                fail("Cache hit do not occur: " + counter.getSqlStatements());
            }
        } finally {
            rollbackTransaction(em);
            if (counter != null) {
                counter.remove();
            }
        }
    }

    /**
     * Test fetch groups.
     */
    public void testFetchGroups() {
        if (!isWeavingEnabled()) {
            return;
        }
        EntityManager em = createEntityManager();
        beginTransaction(em);
        QuerySQLTracker counter = null;
        try {
            // Load an employee into the cache.  
            Query query = em.createQuery("Select employee from Employee employee");
            List result = query.getResultList();
            Employee employee = (Employee)result.get(0);

            rollbackTransaction(em);
            closeEntityManager(em);
            clearCache();
            em = createEntityManager();
            beginTransaction(em);
            
            // Count SQL.
            counter = new QuerySQLTracker(getServerSession());
            // Query by primary key.
            query = em.createQuery("Select employee from Employee employee where employee.id = :id");
            query.setHint(QueryHints.FETCH_GROUP_ATTRIBUTE, "firstName");
            query.setHint(QueryHints.FETCH_GROUP_ATTRIBUTE, "lastName");
            query.setParameter("id", employee.getId());
            Employee queryResult = (Employee)query.getSingleResult();
            if (counter.getSqlStatements().size() != 1) {
                fail("More than fetch group selected: " + counter.getSqlStatements());
            }
            queryResult.getGender();
            if (counter.getSqlStatements().size() != 2) {
                fail("Access to unfetch did not cause fetch: " + counter.getSqlStatements());
            }
            verifyObject(employee);
        } finally {
            rollbackTransaction(em);
            closeEntityManager(em);
            if (counter != null) {
                counter.remove();
            }
        }
    }

    /**
     * Test multiple fetch joining from named queries.
     */
    public void testMultipleNamedJoinFetchs() {
        if (!isWeavingEnabled()) {
            return;
        }
        EntityManager em = createEntityManager();
        beginTransaction(em);
        QuerySQLTracker counter = null;
        try {
            clearCache();
            // Count SQL.
            counter = new QuerySQLTracker(getServerSession());
            Query query = em.createNamedQuery("findAllEmployeesJoinAddressPhones");
            List<Employee> result = query.getResultList();
            if (counter.getSqlStatements().size() != 1) {
                fail("More than join fetches selected: " + counter.getSqlStatements());
            }
            for (Employee each : result) {
                each.getAddress().getCity();
                each.getPhoneNumbers().size();
            }
            if (counter.getSqlStatements().size() != 1) {
                fail("Join fetches triggered query: " + counter.getSqlStatements());
            }
        } finally {
            rollbackTransaction(em);
            closeEntityManager(em);
            if (counter != null) {
                counter.remove();
            }
        }
    }
    
    /**
     * Test cursored queries.
     */
    public void testCursors() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        try {
            // Test cusored stream.
            Query query = em.createQuery("Select employee from Employee employee");
            query.setHint(QueryHints.CURSOR, true);
            query.setHint(QueryHints.CURSOR_INITIAL_SIZE, 2);
            query.setHint(QueryHints.CURSOR_PAGE_SIZE, 5);
            query.setHint(QueryHints.CURSOR_SIZE, "Select count(*) from CMP3_EMPLOYEE");
            Cursor cursor = (Cursor)query.getSingleResult();
            cursor.nextElement();
            cursor.size();
            cursor.close();
            
            // Test cursor result API.
            JpaQuery jpaQuery = (JpaQuery)((EntityManager)em.getDelegate()).createQuery("Select employee from Employee employee");
            jpaQuery.setHint(QueryHints.CURSOR, true);
            cursor = jpaQuery.getResultCursor();
            cursor.nextElement();
            cursor.size();
            cursor.close();
            
            // Test scrollable cursor.
            jpaQuery = (JpaQuery)((EntityManager)em.getDelegate()).createQuery("Select employee from Employee employee");
            jpaQuery.setHint(QueryHints.SCROLLABLE_CURSOR, true);
            jpaQuery.setHint(QueryHints.RESULT_SET_CONCURRENCY, ResultSetConcurrency.ReadOnly);
            jpaQuery.setHint(QueryHints.RESULT_SET_TYPE, ResultSetType.DEFAULT);
            ScrollableCursor scrollableCursor = (ScrollableCursor)jpaQuery.getResultCursor();
            scrollableCursor.next();
            scrollableCursor.close();
            
        } finally {
            rollbackTransaction(em);
            closeEntityManager(em);
        }
    }

    /**
     * Test the result type of various queries.
     */
    public void testObjectResultType() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        try {
            // Load an employee into the cache.  
            Query query = em.createQuery("Select employee from Employee employee");
            List result = query.getResultList();
            Employee employee = (Employee)result.get(0);

            // Test multi object, as an array.
            query = em.createQuery("Select employee, employee.address, employee.id from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            Object[] arrayResult = (Object[])query.getSingleResult();
            if ((arrayResult.length != 3) && (arrayResult[0] != employee) || (arrayResult[1] != employee.getAddress()) || (!arrayResult[2].equals(employee.getId()))) {
                fail("Array result not correct: " + arrayResult);
            }
            List listResult = query.getResultList();
            arrayResult = (Object[])listResult.get(0);
            if ((arrayResult.length != 3) || (arrayResult[0] != employee) || (arrayResult[1] != employee.getAddress()) || (!arrayResult[2].equals(employee.getId()))) {
                fail("Array result not correct: " + arrayResult);
            }
            
            // Test single object, as an array.
            query = em.createQuery("Select employee.id from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setHint(QueryHints.RESULT_TYPE, ResultType.Array);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            arrayResult = (Object[])query.getSingleResult();
            if ((arrayResult.length != 1) || (!arrayResult[0].equals(employee.getId()))) {
                fail("Array result not correct: " + arrayResult);
            }
            listResult = query.getResultList();
            arrayResult = (Object[])listResult.get(0);
            if ((arrayResult.length != 1) || (!arrayResult[0].equals(employee.getId()))) {
                fail("Array result not correct: " + arrayResult);
            }
            
            // Test multi object, as a Map.
            query = em.createQuery("Select employee, employee.address, employee.id from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setHint(QueryHints.RESULT_TYPE, ResultType.Map);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            Map mapResult = (Map)query.getSingleResult();
            if ((mapResult.size() != 3) ||(mapResult.get("employee") != employee) || (mapResult.get("address") != employee.getAddress()) || (!mapResult.get("id").equals(employee.getId()))) {
                fail("Map result not correct: " + mapResult);
            }
            listResult = query.getResultList();
            mapResult = (Map)listResult.get(0);
            if ((mapResult.size() != 3) ||(mapResult.get("employee") != employee) || (mapResult.get("address") != employee.getAddress()) || (!mapResult.get("id").equals(employee.getId()))) {
                fail("Map result not correct: " + mapResult);
            }
            
            // Test single object, as a Map.
            query = em.createQuery("Select employee.id from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setHint(QueryHints.RESULT_TYPE, ResultType.Map);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            mapResult = (Map)query.getSingleResult();
            if ((mapResult.size() != 1) || (!mapResult.get("id").equals(employee.getId()))) {
                fail("Map result not correct: " + mapResult);
            }
            listResult = query.getResultList();
            mapResult = (Map)listResult.get(0);
            if ((mapResult.size() != 1) || (!mapResult.get("id").equals(employee.getId()))) {
                fail("Map result not correct: " + mapResult);
            }
            
            // Test single object, as an array.
            query = em.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setHint(QueryHints.QUERY_TYPE, QueryType.Report);
            query.setHint(QueryHints.RESULT_TYPE, ResultType.Array);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            arrayResult = (Object[])query.getSingleResult();
            if (arrayResult[0] != employee) {
                fail("Array result not correct: " + arrayResult);
            }
            
            // Test single object, as value.
            query = em.createQuery("Select employee.id from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            Object valueResult = query.getSingleResult();
            if (! valueResult.equals(employee.getId())) {
                fail("Value result not correct: " + valueResult);
            }
            listResult = query.getResultList();
            valueResult = listResult.get(0);
            if (! valueResult.equals(employee.getId())) {
                fail("Value result not correct: " + valueResult);
            }
            
            // Test multi object, as value.
            query = em.createQuery("Select employee.id, employee.firstName from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setHint(QueryHints.RESULT_TYPE, ResultType.Value);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            valueResult = query.getSingleResult();
            if (! valueResult.equals(employee.getId())) {
                fail("Value result not correct: " + valueResult);
            }
            
            // Test single object, as attribute.
            query = em.createQuery("Select employee.id from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setHint(QueryHints.RESULT_TYPE, ResultType.Attribute);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            valueResult = query.getSingleResult();
            if (! valueResult.equals(employee.getId())) {
                fail("Value result not correct: " + valueResult);
            }
            listResult = query.getResultList();
            valueResult = listResult.get(0);
            if (! valueResult.equals(employee.getId())) {
                fail("Value result not correct: " + valueResult);
            }
        } finally {
            rollbackTransaction(em);
        }
    }

    /**
     * Test the result type of various native queries.
     */
    public void testNativeResultType() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        try {
            // Load an employee into the cache.  
            Query query = em.createNativeQuery("Select * from CMP3_EMPLOYEE employee", Employee.class);
            List result = query.getResultList();
            Employee employee = (Employee)result.get(0);

            // Test multi object, as an array.
            query = em.createNativeQuery("Select employee.F_NAME, employee.EMP_ID from CMP3_EMPLOYEE employee where employee.EMP_ID = ? and employee.F_NAME = ?");
            query.setParameter(1, employee.getId());
            query.setParameter(2, employee.getFirstName());
            Object[] arrayResult = (Object[])query.getSingleResult();
            if ((arrayResult.length != 2) || (!arrayResult[0].equals(employee.getFirstName())) && (!arrayResult[1].equals(employee.getId()))) {
                fail("Array result not correct: " + arrayResult);
            }
            List listResult = query.getResultList();
            arrayResult = (Object[])listResult.get(0);
            if ((arrayResult.length != 2) || (!arrayResult[0].equals(employee.getFirstName())) && (!arrayResult[1].equals(employee.getId()))) {
                fail("Array result not correct: " + arrayResult);
            }
            
            // Test single object, as an array.
            query = em.createNativeQuery("Select employee.EMP_ID from CMP3_EMPLOYEE employee where employee.EMP_ID = ? and employee.F_NAME = ?");
            query.setHint(QueryHints.RESULT_TYPE, ResultType.Array);
            query.setParameter(1, employee.getId());
            query.setParameter(2, employee.getFirstName());
            arrayResult = (Object[])query.getSingleResult();
            if ((arrayResult.length != 1) || (!new Integer(((Number)arrayResult[0]).intValue()).equals(employee.getId()))) {
                fail("Array result not correct: " + arrayResult);
            }
            listResult = query.getResultList();
            arrayResult = (Object[])listResult.get(0);
            if ((arrayResult.length != 1) || (!new Integer(((Number)arrayResult[0]).intValue()).equals(employee.getId()))) {
                fail("Array result not correct: " + arrayResult);
            }
            
            // Test multi object, as a Map.
            query = em.createNativeQuery("Select employee.F_NAME, employee.EMP_ID from CMP3_EMPLOYEE employee where employee.EMP_ID = ? and employee.F_NAME = ?");
            query.setHint(QueryHints.RESULT_TYPE, ResultType.Map);
            query.setParameter(1, employee.getId());
            query.setParameter(2, employee.getFirstName());
            Map mapResult = (Map)query.getSingleResult();
            if ((mapResult.size() != 2) || (!mapResult.get("F_NAME").equals(employee.getFirstName())) || (!(new Integer(((Number)mapResult.get("EMP_ID")).intValue())).equals(employee.getId()))) {
                fail("Map result not correct: " + mapResult);
            }
            listResult = query.getResultList();
            mapResult = (Map)listResult.get(0);
            if ((mapResult.size() != 2) || (!mapResult.get("F_NAME").equals(employee.getFirstName())) || (!(new Integer(((Number)mapResult.get("EMP_ID")).intValue())).equals(employee.getId()))) {
                fail("Map result not correct: " + mapResult);
            }
            
            // Test single object, as a Map.
            query = em.createNativeQuery("Select employee.EMP_ID from CMP3_EMPLOYEE employee where employee.EMP_ID = ? and employee.F_NAME = ?");
            query.setHint(QueryHints.RESULT_TYPE, ResultType.Map);
            query.setParameter(1, employee.getId());
            query.setParameter(2, employee.getFirstName());
            mapResult = (Map)query.getSingleResult();
            if ((mapResult.size() != 1) || (!(new Integer(((Number)mapResult.get("EMP_ID")).intValue())).equals(employee.getId()))) {
                fail("Map result not correct: " + mapResult);
            }
            listResult = query.getResultList();
            mapResult = (Map)listResult.get(0);
            if ((mapResult.size() != 1) || (!(new Integer(((Number)mapResult.get("EMP_ID")).intValue())).equals(employee.getId()))) {
                fail("Map result not correct: " + mapResult);
            }
            
            // Test single object, as value.
            query = em.createNativeQuery("Select employee.EMP_ID from CMP3_EMPLOYEE employee where employee.EMP_ID = ? and employee.F_NAME = ?");
            query.setParameter(1, employee.getId());
            query.setParameter(2, employee.getFirstName());
            Object valueResult = query.getSingleResult();
            if (!(new Integer(((Number)valueResult).intValue())).equals(employee.getId())) {
                fail("Value result not correct: " + valueResult);
            }
            listResult = query.getResultList();
            valueResult = listResult.get(0);
            if (!(new Integer(((Number)valueResult).intValue())).equals(employee.getId())) {
                fail("Value result not correct: " + valueResult);
            }
        } finally {
            rollbackTransaction(em);
        }
    }

    /**
     * Test that a cache hit will occur on a primary key query.
     */
    public void testQueryExactPrimaryKeyCacheHits() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        QuerySQLTracker counter = null;
        try {
            // Load an employee into the cache.  
            Query query = em.createQuery("Select employee from Employee employee");
            List result = query.getResultList();
            Employee employee = (Employee)result.get(0);

            // Count SQL.
            counter = new QuerySQLTracker(getServerSession());
            // Query by primary key.
            query = em.createQuery("Select employee from Employee employee where employee.id = :id");
            query.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheByExactPrimaryKey);
            query.setParameter("id", employee.getId());
            Employee queryResult = (Employee)query.getSingleResult();
            if (queryResult != employee) {
                fail("Employees are not equal: " + employee + ", " + queryResult);
            }
            if (counter.getSqlStatements().size() > 0) {
                fail("Cache hit do not occur: " + counter.getSqlStatements());
            }
        } finally {
            rollbackTransaction(em);
            if (counter != null) {
                counter.remove();
            }
        }
    }

    /**
     * Test that a cache hit will occur on a query.
     */
    public void testQueryCacheFirstCacheHits() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        QuerySQLTracker counter = null;
        try {
            // Load an employee into the cache.  
            Query query = em.createQuery("Select employee from Employee employee");
            List result = query.getResultList();
            Employee employee = (Employee)result.get(result.size() - 1);

            // Count SQL.
            counter = new QuerySQLTracker(getServerSession());
            // Query by primary key.
            query = em.createQuery("Select employee from Employee employee where employee.firstName = :firstName");
            query.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            query.setParameter("firstName", employee.getFirstName());
            Employee queryResult = (Employee)query.getSingleResult();
            if (!queryResult.getFirstName().equals(employee.getFirstName())) {
                fail("Employees are not equal: " + employee + ", " + queryResult);
            }
            if (counter.getSqlStatements().size() > 0) {
                fail("Cache hit do not occur: " + counter.getSqlStatements());
            }
        } finally {
            rollbackTransaction(em);
            if (counter != null) {
                counter.remove();
            }
        }
    }

    /**
     * Test that a cache hit will occur on a query.
     */
    public void testQueryCacheOnlyCacheHits() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        QuerySQLTracker counter = null;
        try {
            // Load an employee into the cache.  
            Query query = em.createQuery("Select employee from Employee employee");
            List result = query.getResultList();
            Employee employee = (Employee)result.get(result.size() - 1);

            // Count SQL.
            counter = new QuerySQLTracker(getServerSession());
            // Query by primary key.
            query = em.createQuery("Select employee from Employee employee where employee.firstName = :firstName");
            query.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheOnly);
            query.setParameter("firstName", employee.getFirstName());
            // Test that list works as well.
            query.getResultList();
            if (counter.getSqlStatements().size() > 0) {
                fail("Cache hit do not occur: " + counter.getSqlStatements());
            }
        } finally {
            rollbackTransaction(em);
            if (counter != null) {
                counter.remove();
            }
        }
    }
    
    /**
     * Test that a cache hit will occur on a query when the object is not in the unit of work/em.
     */
    public void testQueryCacheOnlyCacheHitsOnSession() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        QuerySQLTracker counter = null;
        try {
            // Load an employee into the cache.  
            Query query = em.createQuery("Select employee from Employee employee");
            List result = query.getResultList();
            Employee employee = (Employee)result.get(result.size() - 1);

            // Count SQL.
            counter = new QuerySQLTracker(getServerSession());
            // Query by primary key.
            rollbackTransaction(em);
            closeEntityManager(em);
            em = createEntityManager();
            beginTransaction(em);
            query = em.createQuery("Select employee from Employee employee where employee.id = :id");
            query.setHint(QueryHints.QUERY_TYPE, QueryType.ReadObject);
            query.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheOnly);
            query.setParameter("id", employee.getId());
            if (query.getSingleResult() == null) {
                fail("Query did not check session cache.");
            }
            if (counter.getSqlStatements().size() > 0) {
                fail("Cache hit do not occur: " + counter.getSqlStatements());
            }
            rollbackTransaction(em);
            closeEntityManager(em);
            em = createEntityManager();
            beginTransaction(em);
            query = em.createQuery("Select employee from Employee employee where employee.id = :id");
            query.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheOnly);
            query.setParameter("id", employee.getId());
            if (query.getResultList().size() != 1) {
                fail("Query did not check session cache.");
            }
            if (counter.getSqlStatements().size() > 0) {
                fail("Cache hit do not occur: " + counter.getSqlStatements());
            }
        } finally {
            if (counter != null) {
                counter.remove();
            }
            rollbackTransaction(em);
            closeEntityManager(em);
        }
    }
    
    /**
     * Test the query cache.
     */
    public void testQueryCache() {
        EntityManager em = createEntityManager();
        beginTransaction(em);
        QuerySQLTracker counter = null;
        try {
            // Load an employee into the cache.  
            JpaQuery jpaQuery = (JpaQuery)((EntityManager)em.getDelegate()).createNamedQuery("CachedAllEmployees");
            List result = jpaQuery.getResultList();
            ReadQuery readQuery = (ReadQuery)jpaQuery.getDatabaseQuery();
            if (readQuery.getQueryResultsCachePolicy() == null) {
                fail("Query cache not set.");
            }
            if (readQuery.getQueryResultsCachePolicy().getMaximumCachedResults() != 200) {
                fail("Query cache size not set.");
            }
            if (!(readQuery.getQueryResultsCachePolicy().getCacheInvalidationPolicy() instanceof TimeToLiveCacheInvalidationPolicy)) {
                fail("Query cache invalidation not set.");
            }
            if (((TimeToLiveCacheInvalidationPolicy)readQuery.getQueryResultsCachePolicy().getCacheInvalidationPolicy()).getTimeToLive() != 5000) {
                fail("Query cache invalidation time not set.");
            }
            
            jpaQuery = (JpaQuery)((EntityManager)em.getDelegate()).createNamedQuery("CachedTimeOfDayAllEmployees");
            readQuery = (ReadQuery)jpaQuery.getDatabaseQuery();
            if (readQuery.getQueryResultsCachePolicy() == null) {
                fail("Query cache not set.");
            }
            if (readQuery.getQueryResultsCachePolicy().getMaximumCachedResults() != 200) {
                fail("Query cache size not set.");
            }
            if (!(readQuery.getQueryResultsCachePolicy().getCacheInvalidationPolicy() instanceof DailyCacheInvalidationPolicy)) {
                fail("Query cache invalidation not set.");
            }
            Calendar calendar = ((DailyCacheInvalidationPolicy)readQuery.getQueryResultsCachePolicy().getCacheInvalidationPolicy()).getExpiryTime();
            if ((calendar.get(Calendar.HOUR_OF_DAY) != 23 )
                    && (calendar.get(Calendar.MINUTE) != 59)
                    && (calendar.get(Calendar.SECOND) != 59)) {
                fail("Query cache invalidation time not set.");
            }

            // Count SQL.
            counter = new QuerySQLTracker(getServerSession());
            // Query by primary key.
            Query query = em.createNamedQuery("CachedAllEmployees");
            if (result.size() != query.getResultList().size()) {
                fail("List result size is not correct.");
            }
            if (counter.getSqlStatements().size() > 0) {
                fail("Query cache was not used: " + counter.getSqlStatements());
            }
        } finally {
            rollbackTransaction(em);
            if (counter != null) {
                counter.remove();
            }
        }
    }
    
    /* // KERNEL_SRG_TEMP       
    public void testQueryREADLock(){
        // Cannot create parallel entity managers in the server.
        if (isOnServer()) {
            return;
        }
        
        // Load an employee into the cache.
        EntityManager em = createEntityManager();
        List result = em.createQuery("Select employee from Employee employee").getResultList();
        Employee employee = (Employee) result.get(0);
        Exception optimisticLockException = null;
       
        try {
            beginTransaction(em);
            
            // Query by primary key.
            Query query = em.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setLockMode(LockModeType.READ);
            query.setHint(QueryHints.REFRESH, true);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            Employee queryResult = (Employee) query.getSingleResult();
            queryResult.toString();
            
            EntityManager em2 = createEntityManager();
            
            try {
                beginTransaction(em2);
                Employee employee2 = em2.find(Employee.class, employee.getId());
                employee2.setFirstName("Read");
                commitTransaction(em2);
            } catch (RuntimeException ex) {
                rollbackTransaction(em2);
                throw ex;
            } finally {
                closeEntityManager(em2);
            }
        
            try {
                em.flush();
            } catch (PersistenceException exception) {
                if (exception instanceof OptimisticLockException) {
                    optimisticLockException = exception;
                } else {
                    throw exception;
                }
            }
            
            rollbackTransaction(em);
        } catch (RuntimeException ex) {
            if (isTransactionActive(em)){
                rollbackTransaction(em);
            }
            
            throw ex;
        } finally {
            closeEntityManager(em);
        }
        
        assertFalse("Proper exception not thrown when Query with LockModeType.READ is used.", optimisticLockException == null);
    }
    
    public void testQueryWRITELock(){
        // Cannot create parallel transactions.
        if (isOnServer()) {
            return;
        }

        // Load an employee into the cache.
        EntityManager em = createEntityManager();
        List result = em.createQuery("Select employee from Employee employee").getResultList();
        Employee employee = (Employee) result.get(0);
        Exception optimisticLockException = null;
        
        try {
            beginTransaction(em);
            
            // Query by primary key.
            Query query = em.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName");
            query.setLockMode(LockModeType.WRITE);
            query.setHint(QueryHints.REFRESH, true);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            Employee queryResult = (Employee) query.getSingleResult();
        
            EntityManager em2 = createEntityManager();
            
            try {
                beginTransaction(em2);
                
                Employee employee2 = em2.find(Employee.class, queryResult.getId());
                employee2.setFirstName("Write");
                commitTransaction(em2);
            } catch (RuntimeException ex) {
                rollbackTransaction(em2);
                closeEntityManager(em2);
                throw ex;
            }
            
            commitTransaction(em);
        } catch (RollbackException exception) {
            if (exception.getCause() instanceof OptimisticLockException){
                optimisticLockException = exception;
            }
        } catch (RuntimeException ex) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
            
            closeEntityManager(em);
            
            throw ex;
        }

        assertFalse("Proper exception not thrown when Query with LockModeType.WRITE is used.", optimisticLockException == null);
    }
    
    public void testQueryOPTIMISTICLock(){
        // Cannot create parallel entity managers in the server.
        if (! isOnServer()) {
            // Load an employee into the cache.
            EntityManager em = createEntityManager();
            List result = em.createQuery("Select employee from Employee employee").getResultList();
            Employee employee = (Employee) result.get(0);
            Exception optimisticLockException = null;
           
            try {
                beginTransaction(em);
                
                // Query by primary key.
                Query query = em.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName");
                query.setLockMode(LockModeType.OPTIMISTIC);
                query.setHint(QueryHints.REFRESH, true);
                query.setParameter("id", employee.getId());
                query.setParameter("firstName", employee.getFirstName());
                Employee queryResult = (Employee) query.getSingleResult();
                queryResult.toString();
            
                EntityManager em2 = createEntityManager();
                
                try {
                    beginTransaction(em2);
                    Employee employee2 = em2.find(Employee.class, employee.getId());
                    employee2.setFirstName("Optimistic");
                    commitTransaction(em2);
                } catch (RuntimeException ex) {
                    rollbackTransaction(em2);
                    throw ex;
                } finally {
                    closeEntityManager(em2);
                }
            
                try {
                    em.flush();
                } catch (PersistenceException exception) {
                    if (exception instanceof OptimisticLockException) {
                        optimisticLockException = exception;
                    } else {
                        throw exception;
                    }
                }
                
                rollbackTransaction(em);
            } catch (RuntimeException ex) {
                if (isTransactionActive(em)){
                    rollbackTransaction(em);
                }
                
                throw ex;
            } finally {
                closeEntityManager(em);
            }
            
            assertFalse("Proper exception not thrown when Query with LockModeType.READ is used.", optimisticLockException == null);
        }
    }
    
    public void testQueryOPTIMISTIC_FORCE_INCREMENTLock(){
        // Cannot create parallel transactions.
        if (! isOnServer()) {
            // Load an employee into the cache.
            EntityManager em = createEntityManager();
            List result = em.createQuery("Select employee from Employee employee").getResultList();
            Employee employee = (Employee) result.get(0);
            Exception optimisticLockException = null;
            
            try {
                beginTransaction(em);
                
                // Query by primary key.
                Query query = em.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName");
                query.setLockMode(LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                query.setHint(QueryHints.REFRESH, true);
                query.setParameter("id", employee.getId());
                query.setParameter("firstName", employee.getFirstName());
                Employee queryResult = (Employee) query.getSingleResult();
            
                EntityManager em2 = createEntityManager();
                
                try {
                    beginTransaction(em2);
                    
                    Employee employee2 = em2.find(Employee.class, queryResult.getId());
                    employee2.setFirstName("OptimisticForceIncrement");
                    commitTransaction(em2);
                } catch (RuntimeException ex) {
                    rollbackTransaction(em2);
                    closeEntityManager(em2);
                    throw ex;
                }
                
                commitTransaction(em);
            } catch (RollbackException exception) {
                if (exception.getCause() instanceof OptimisticLockException){
                    optimisticLockException = exception;
                }
            } catch (RuntimeException ex) {
                if (isTransactionActive(em)) {
                    rollbackTransaction(em);
                }
                
                closeEntityManager(em);
                
                throw ex;
            }
    
            assertFalse("Proper exception not thrown when Query with LockModeType.WRITE is used.", optimisticLockException == null);
        }
    }
    
    public void testQueryPESSIMISTICLock() {
        ServerSession session = JUnitTestCase.getServerSession();
        
        // Cannot create parallel entity managers in the server.
        if (! isOnServer() && ! session.getPlatform().isMySQL() && ! session.getPlatform().isTimesTen()) {
            EntityManager em = createEntityManager();
            Exception pessimisticLockException = null;
        
            try {
                beginTransaction(em);
            
                // Find all the departments and lock them.
                List employees = em.createQuery("Select employee from Employee employee").setLockMode(LockModeType.PESSIMISTIC).getResultList();
                Employee employee = (Employee) employees.get(0);
                employee.setFirstName("New Pessimistic Employee");
            
                EntityManager em2 = createEntityManager();
            
                try {
                    beginTransaction(em2);
                
                    Employee employee2 = em2.find(Employee.class, employee.getId());
                    HashMap properties = new HashMap();
                    properties.put(QueryHints.PESSIMISTIC_LOCK_TIMEOUT, 0);
                    em2.lock(employee2, LockModeType.PESSIMISTIC, properties);
                    employee2.setFirstName("Invalid Lock Employee");
                    
                    commitTransaction(em2);
                } catch (PersistenceException ex) {
                    if (ex instanceof javax.persistence.PessimisticLockException) {
                        pessimisticLockException = ex;
                    } else {
                        throw ex;
                    } 
                } finally {
                    closeEntityManager(em2);
                }
                
                commitTransaction(em);
            } catch (RuntimeException ex) {
                if (isTransactionActive(em)) {
                    rollbackTransaction(em);
                }
                
                throw ex;
            } finally {
                closeEntityManager(em);
            }
        
            assertFalse("Proper exception not thrown when Query with LockModeType.PESSIMISTIC is used.", pessimisticLockException == null);
        }
    }
    
    public void testQueryPESSIMISTIC_FORCE_INCREMENTLock() {        
        Employee employee = null;
        Integer version1;
        
        EntityManager em = createEntityManager();
        beginTransaction(em);
        
        try {
            employee = new Employee();
            employee.setFirstName("Guillaume");
            employee.setLastName("Aujet");
            em.persist(employee);
            commitTransaction(em);
        } catch (RuntimeException ex) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
         
            closeEntityManager(em);
            throw ex;
        }
        
        version1 = employee.getVersion();
        
        try {
            beginTransaction(em);
            Query query = em.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName").setLockMode(LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            query.setHint(QueryHints.REFRESH, true);
            query.setParameter("id", employee.getId());
            query.setParameter("firstName", employee.getFirstName());
            Employee queryResult = (Employee) query.getSingleResult();
            queryResult.setLastName("Auger");
            commitTransaction(em);
            
            assertTrue("The version was not updated on the pessimistic lock.", version1.intValue() < employee.getVersion().intValue());
        } catch (RuntimeException ex) {
            if (isTransactionActive(em)) {
                rollbackTransaction(em);
            }
            
            throw ex;
        } finally {
            closeEntityManager(em);
        }
    }
    
    public void testQueryPESSIMISTICTIMEOUTLock() {
        ServerSession session = JUnitTestCase.getServerSession();
        
        // Cannot create parallel entity managers in the server.
        if (! isOnServer() && ! session.getPlatform().isMySQL() && ! session.getPlatform().isTimesTen()) {
            EntityManager em = createEntityManager();
            List result = em.createQuery("Select employee from Employee employee").getResultList();
            Employee employee = (Employee) result.get(0);
            Exception lockTimeOutException = null;
           
            try {
                beginTransaction(em);
                
                // Query by primary key.
                Query query = em.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName");
                query.setLockMode(LockModeType.PESSIMISTIC);
                query.setHint(QueryHints.REFRESH, true);
                query.setParameter("id", employee.getId());
                query.setParameter("firstName", employee.getFirstName());
                Employee queryResult = (Employee) query.getSingleResult();
                queryResult.toString();
            
                EntityManager em2 = createEntityManager();
            
                try {
                    beginTransaction(em2);
                
                    // Query by primary key.
                    Query query2 = em2.createQuery("Select employee from Employee employee where employee.id = :id and employee.firstName = :firstName");
                    query2.setLockMode(LockModeType.PESSIMISTIC);
                    query2.setHint(QueryHints.REFRESH, true);
                    query2.setHint(QueryHints.PESSIMISTIC_LOCK_TIMEOUT, 5);
                    query2.setParameter("id", employee.getId());
                    query2.setParameter("firstName", employee.getFirstName());
                    Employee employee2 = (Employee) query2.getSingleResult();
                    employee2.setFirstName("Invalid Lock Employee");
                    commitTransaction(em2);
                } catch (PersistenceException ex) {
                    if (ex instanceof javax.persistence.LockTimeoutException) {
                        lockTimeOutException = ex;
                    } else {
                        throw ex;
                    } 
                } finally {
                    closeEntityManager(em2);
                }
                
                commitTransaction(em);
            } catch (RuntimeException ex) {
                if (isTransactionActive(em)) {
                    rollbackTransaction(em);
                }
                
                throw ex;
            } finally {
                closeEntityManager(em);
            }
        
            assertFalse("Proper exception not thrown when Query with LockModeType.PESSIMISTIC is used.", lockTimeOutException == null);
        }
    } */
}
