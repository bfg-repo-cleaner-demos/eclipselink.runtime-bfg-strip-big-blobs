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

import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.expressions.*;
import org.eclipse.persistence.testing.models.employee.domain.*;

public class SimpleNotLikeTest extends org.eclipse.persistence.testing.tests.jpql.JPQLTestCase {
    public void setup() {
        Employee emp;
        emp = (Employee)getSomeEmployees().firstElement();

        String partialFirstName = emp.getFirstName().substring(0, 3) + "%";

        ExpressionBuilder builder = new ExpressionBuilder();
        Expression whereClause = builder.get("firstName").notLike(partialFirstName);

        ReadAllQuery raq = new ReadAllQuery();
        raq.setReferenceClass(Employee.class);
        raq.setSelectionCriteria(whereClause);

        setOriginalOject(getSession().executeQuery(raq));
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();

        String ejbqlString = "SELECT OBJECT(emp) FROM Employee emp WHERE emp.firstName NOT LIKE \"" + partialFirstName + "\"";

        setEjbqlString(ejbqlString);

        super.setup();
    }
}