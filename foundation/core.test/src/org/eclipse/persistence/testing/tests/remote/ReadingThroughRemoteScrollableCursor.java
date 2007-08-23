/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.remote;

import org.eclipse.persistence.testing.framework.*;
import org.eclipse.persistence.sessions.*;
import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.testing.models.employee.domain.*;

public class ReadingThroughRemoteScrollableCursor extends TestCase {

    public ReadingThroughRemoteScrollableCursor() {
        setDescription("ReadingThroughRemoteScrollableCursor");
    }

    public void reset() {
        getAbstractSession().rollbackTransaction();
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();
    }

    public void setup() {
        getAbstractSession().beginTransaction();
    }

    public void test() {
        UnitOfWork uow = getSession().acquireUnitOfWork();
        ReadAllQuery query = new ReadAllQuery();
        query.setReferenceClass(Employee.class);
        query.useScrollableCursor();
        ScrollableCursor cursor = (ScrollableCursor)getSession().executeQuery(query);
        Employee emp = (Employee)cursor.next();
        emp = (Employee)uow.registerObject(emp);
        emp.setFirstName("Sami");
        uow.commit();
    }
}
