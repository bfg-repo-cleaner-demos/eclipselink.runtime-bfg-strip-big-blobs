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
 package org.eclipse.persistence.testing.tests.jpa.performance;

import javax.persistence.*;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.testing.models.performance.*;
import org.eclipse.persistence.testing.framework.*;

/**
 * This test compares the performance of inserting Employee.
 */
public class JPAInsertEmployeePerformanceComparisonTest extends PerformanceRegressionTestCase {
    public JPAInsertEmployeePerformanceComparisonTest() {
        setDescription("This test compares the performance of insert Employee.");
    }

    /**
     * Delete all employees.
     */
    public void reset() {
        getSession().executeNonSelectingSQL("Delete from PHONE where P_NUMBER = '9991111'");
        getSession().executeNonSelectingSQL("Delete from EMPLOYEE where F_NAME = 'NewGuy'");
        getSession().executeNonSelectingSQL("Delete from ADDRESS where STREET = 'Hasting Perf'");
        //getSession().getIdentityMapAccessor().initializeIdentityMaps();
        //HibernatePerformanceComparisonModel.getSessionFactory().evict(Address.class);
        //HibernatePerformanceComparisonModel.getSessionFactory().evict(PhoneNumber.class);
        //HibernatePerformanceComparisonModel.getSessionFactory().evict(Employee.class);
    }

    /**
     * Insert employee.
     */
    public void test() throws Exception {
        EntityManager manager = createEntityManager();
        manager.getTransaction().begin();
        Employee employee = new Employee();
        employee.setFirstName("NewGuy");
        employee.setLastName("Smith");

        EmploymentPeriod employmentPeriod = new EmploymentPeriod();
        java.sql.Date startDate = Helper.dateFromString("1901-12-31");
        java.sql.Date endDate = Helper.dateFromString("1970-01-01");
        employmentPeriod.setEndDate(startDate);
        employmentPeriod.setStartDate(endDate);
        employee.setPeriod(employmentPeriod);

        Address address = new Address();
        address.setCity("Ottawa");
        address.setStreet("Hastings Perf");
        address.setProvince("ONT");
        employee.setAddress(address);

        PhoneNumber phone = new PhoneNumber();
        phone.setType("home");
        phone.setAreaCode("613");
        phone.setNumber("9991111");
        employee.addPhoneNumber(phone);

        phone = new PhoneNumber();
        phone.setType("fax");
        phone.setAreaCode("613");
        phone.setNumber("9991111");
        employee.addPhoneNumber(phone);

        manager.persist(employee);
        manager.getTransaction().commit();
        manager.close();
    }
}