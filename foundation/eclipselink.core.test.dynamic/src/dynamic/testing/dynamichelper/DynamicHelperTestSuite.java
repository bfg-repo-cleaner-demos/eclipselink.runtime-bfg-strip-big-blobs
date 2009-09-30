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
 *     dclarke - Dynamic Persistence INCUBATION - Enhancement 200045
 *     			 http://wiki.eclipse.org/EclipseLink/Development/Dynamic
 *     mnorman - tweaks to work from Ant command-line,
 *               et database properties from System, etc.
 *     
 * This code is being developed under INCUBATION and is not currently included 
 * in the automated EclipseLink build. The API in this code may change, or 
 * may never be included in the product. Please provide feedback through mailing 
 * lists or the bug database.
 ******************************************************************************/
package dynamic.testing.dynamichelper;

//javase imports
import java.util.List;

//JUnit4 imports
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

//EclipseLink imports
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.dynamic.DynamicHelper;
import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.dynamic.DynamicTypeBuilder;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReadObjectQuery;
import org.eclipse.persistence.queries.ReportQuery;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.sessions.UnitOfWork;

//domain-specific (testing) inmports
import static dynamic.testing.DynamicTestingHelper.createEmptySession;
import static dynamic.testing.DynamicTestingHelper.createSession;

/**
 * Set of tests verifying that the DynamicHelper functions as expected.
 */
public class DynamicHelperTestSuite {

    //test fixtures
    static DatabaseSession session = null;
    static DynamicHelper dynamicHelper = null;
    @BeforeClass
    public static void setUp() {
        session = createSession();
        dynamicHelper = new DynamicHelper(session);
        DynamicClassLoader dcl = dynamicHelper.getDynamicClassLoader(); 
        Class<?> empClass = dcl.createDynamicClass("dynamichelper.Employee");
        DynamicTypeBuilder typeBuilder = new DynamicTypeBuilder(empClass, null, "D_EMPLOYEE");
        typeBuilder.setPrimaryKeyFields("EMP_ID");
        typeBuilder.addDirectMapping("id", int.class, "EMP_ID");
        typeBuilder.addDirectMapping("firstName", String.class, "F_NAME");
        typeBuilder.addDirectMapping("lastName", String.class, "L_NAME");
        dynamicHelper.addTypes(true, false, typeBuilder.getType());
        DynamicType empType = new DynamicHelper(session).getType("Employee");
        assertNotNull("No type found for Employee", empType);
        
        //Populate table with a single Employee
        DynamicEntity e1 = empType.newDynamicEntity();
        e1.set("id", 1);
        e1.set("firstName", "Mike");
        e1.set("lastName", "Norman");
        UnitOfWork uow = session.acquireUnitOfWork();
        uow.registerNewObject(e1);
        uow.commit();
        session.getIdentityMapAccessor().initializeAllIdentityMaps();
    }

    @AfterClass
    public static void tearDown() {
        session.executeNonSelectingSQL("DROP TABLE D_EMPLOYEE");
        session.logout();
        session = null;
        dynamicHelper = null;
    }

    @Test
    public void createQuery_ValidReadObjectQuery() throws Exception {
        ReadObjectQuery query = dynamicHelper.newReadObjectQuery("Employee");
        assertNotNull(query);
        query.setSelectionCriteria(query.getExpressionBuilder().get("id").equal(1L));

        DynamicEntity emp = (DynamicEntity) session.executeQuery(query);
        assertNotNull(emp);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void createQuery_ValidReadAllQuery() throws Exception {
        ReadAllQuery query = dynamicHelper.newReadAllQuery("Employee");
        assertNotNull(query);

        List<DynamicEntity> emps = (List<DynamicEntity>) session.executeQuery(query);
        assertNotNull(emps);
    }

    @Test
    public void createQuery_ValidReportQuery() throws Exception {
        ReportQuery query = dynamicHelper.newReportQuery("Employee", new ExpressionBuilder());
        assertNotNull(query);
        query.addCount();
        query.setShouldReturnSingleValue(true);

        Number count = (Number) session.executeQuery(query);
        assertNotNull(count);
        assertEquals(1L, count.longValue());
    }

    @Test(expected=IllegalArgumentException.class)
    public void nullArgs() {
        new DynamicHelper(createEmptySession()).newReadAllQuery(null);
    }

}