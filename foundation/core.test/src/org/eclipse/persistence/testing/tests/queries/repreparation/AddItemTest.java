/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.queries.repreparation;

import org.eclipse.persistence.testing.framework.AutoVerifyTestCase;
import org.eclipse.persistence.testing.models.employee.domain.*;
import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.expressions.*;
import java.util.Vector;

public class AddItemTest extends AutoVerifyTestCase {
    ReportQuery reportQuery;
    Vector results;

    public AddItemTest() {
        setDescription("Test if SQL is reprepared the second time");
    }

    public void reset() {
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();
    }

    public void setup() {
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();

        reportQuery = new ReportQuery(new ExpressionBuilder());
        reportQuery.setReferenceClass(Employee.class);
        reportQuery.addAttribute("gender");
        results = (Vector)getSession().executeQuery(reportQuery);
    }

    public void test() {
        reportQuery.addGrouping("gender");
        reportQuery.addItem("salary", reportQuery.getExpressionBuilder().get("salary").average());
        results = (Vector)getSession().executeQuery(reportQuery);
    }

    public void verify() {
        if (!reportQuery.getCall().getSQLString().equals("SELECT t0.GENDER, AVG(t1.SALARY) FROM EMPLOYEE t0, SALARY t1 WHERE (t1.EMP_ID = t0.EMP_ID) GROUP BY t0.GENDER")) {
            throw new org.eclipse.persistence.testing.framework.TestErrorException("AddItemTest failed.");
        }
    }
}