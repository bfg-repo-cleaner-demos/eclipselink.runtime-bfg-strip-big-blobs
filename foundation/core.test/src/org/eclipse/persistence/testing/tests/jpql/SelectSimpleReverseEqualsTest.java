/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.jpql;

import java.util.*;
import org.eclipse.persistence.testing.models.employee.domain.*;

public class SelectSimpleReverseEqualsTest extends org.eclipse.persistence.testing.tests.jpql.JPQLTestCase {
    public void setup() {
        Vector employees = getSession().readAllObjects(Employee.class);

        String ejbqlString = null;
        Employee emp = (Employee)employees.firstElement();
        ejbqlString = "SELECT OBJECT(emp) FROM Employee emp WHERE ";
        ejbqlString = ejbqlString + "\"" + emp.getFirstName() + "\"";
        ejbqlString = ejbqlString + " = emp.firstName";
        setEjbqlString(ejbqlString);
        super.setup();
    }

    public void verify() throws Exception {
        // This method is derived from class org.eclipse.persistence.testing.ejb.EJBQLTesting.EJBQLTestCase
        // to do: code goes here
    }
}