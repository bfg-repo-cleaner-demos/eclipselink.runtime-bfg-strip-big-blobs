/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.workbenchintegration.QueryOptions;

import java.util.Vector;

import org.eclipse.persistence.testing.framework.AutoVerifyTestCase;


public class MaxRowsTest extends AutoVerifyTestCase {
    private Vector employees;

    public MaxRowsTest() {
        setDescription("Set up the limit for the maximum number of rows the query returns");
    }

    public void reset() {
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();
    }

    public void setup() {
    }

    public void test() {
        employees = 
                (Vector)getSession().executeQuery("maxRowsQuery", org.eclipse.persistence.testing.models.employee.domain.Employee.class);
    }

    public void verify() {
        if (employees.size() != 4) {
            throw new org.eclipse.persistence.testing.framework.TestErrorException("ReadAllQuery with setMaxRows test failed. Mismatched objects returned");
        }
    }
}
