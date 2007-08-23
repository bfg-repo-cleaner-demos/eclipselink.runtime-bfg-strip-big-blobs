/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.events;

import org.eclipse.persistence.sessions.*;
import org.eclipse.persistence.testing.models.events.Customer;
import org.eclipse.persistence.testing.framework.*;

public class ObjectChangeSetEventTest extends EventHookTestCase {
    public Customer customer;

    public void setup() {
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();
        beginTransaction();
        UnitOfWork uow = getSession().acquireUnitOfWork();
        this.customer = Customer.example1();
        uow.registerObject(this.customer);
        uow.commit();
    }

    public void test() {
        UnitOfWork uow = getSession().acquireUnitOfWork();
        Customer cloneCustomer = (Customer)uow.readObject(this.customer);
        cloneCustomer.preWrite = true;
        uow.commit();
    }

    public void verify() {
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();
        Customer originalCustomer = (Customer)getSession().readObject(this.customer);
        if (!originalCustomer.name.equals("PreWrite")) {
            throw new TestErrorException("Failed to cause update when attribute was changed in the pre-write event");
        }
    }
}