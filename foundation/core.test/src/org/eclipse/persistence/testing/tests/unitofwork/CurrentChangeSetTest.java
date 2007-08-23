/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.unitofwork;

import org.eclipse.persistence.sessions.UnitOfWork;
import org.eclipse.persistence.testing.framework.AutoVerifyTestCase;
import org.eclipse.persistence.testing.framework.TestErrorException;
import org.eclipse.persistence.testing.models.employee.domain.Employee;


// Bug 5744009 - Make sure new objects are included in the current change set.
public class CurrentChangeSetTest extends AutoVerifyTestCase {
    int countBeforeCommit;
    int countAfterCommit;

    public void reset() {
        if (getAbstractSession().isInTransaction()) {
            getAbstractSession().rollbackTransaction();
            getSession().getIdentityMapAccessor().initializeAllIdentityMaps();
        }
    }

    public void setup() {
        getAbstractSession().beginTransaction();
        countBeforeCommit = 0;
        countAfterCommit = 0;
    }

    public void test() {
        UnitOfWork uow = getSession().acquireUnitOfWork();
        Employee emp = (Employee)uow.registerObject(new Employee());
        countBeforeCommit = uow.getCurrentChanges().getAllChangeSets().size();
        uow.commit();
        countAfterCommit = uow.getCurrentChanges().getAllChangeSets().size();
    }

    public void verify() {
        if (countBeforeCommit == 0) {
            throw new TestErrorException("Invalid number of change sets before commit [" + countBeforeCommit + 
                                         "]. ");
        }

        if (countAfterCommit != 1) {
            throw new TestErrorException("Invalid number of change sets after commit [" + countAfterCommit + 
                                         "]. ");
        }
    }
}
