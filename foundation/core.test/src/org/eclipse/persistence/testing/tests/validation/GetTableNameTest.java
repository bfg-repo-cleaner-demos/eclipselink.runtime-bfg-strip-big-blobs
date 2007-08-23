/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.validation;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.eclipse.persistence.testing.framework.TestErrorException;


public class GetTableNameTest extends ExceptionTest {
    public GetTableNameTest() {
        super();
        setDescription("This test attempts to call getTableName()");
    }

    protected void setup() {
        expectedException = null;
    }

    public void test() {
        try { //test if getTableName() throws casting exception
            org.eclipse.persistence.testing.models.employee.relational.EmployeeProject project = new org.eclipse.persistence.testing.models.employee.relational.EmployeeProject();
            java.util.Iterator iterator = project.getDescriptors().values().iterator();
            while (iterator.hasNext()) {
                ((ClassDescriptor)iterator.next()).getTableName();
            }
        } catch (EclipseLinkException exception) {
            caughtException = exception;
        }
    }

    protected void verify() {
        if (caughtException != expectedException) {
            throw new TestErrorException("The proper exception was not thrown:\n" + "[CAUGHT] " + caughtException + "\n\n[EXPECTING] " + String.valueOf(expectedException));
        }
    }
}
