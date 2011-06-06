/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.tests.performance.writing;

import org.eclipse.persistence.sessions.*;
import org.eclipse.persistence.testing.models.performance.Address;
import org.eclipse.persistence.testing.tests.performance.PerformanceTest;

/**
 * This tests the performance of unit of work inserts/deletes.
 * Its purpose is to compare the test result with previous release/label results.
 * It also provides a useful test for profiling performance.
 */
public class InsertDeleteAddressUnitOfWorkTest extends PerformanceTest {
    public InsertDeleteAddressUnitOfWorkTest() {
        setDescription("This tests the performance of unit of work inserts/deletes.");
    }

    /**
     * Insert and delete address.
     */
    public void test() throws Exception {
        UnitOfWork uow = getSession().acquireUnitOfWork();
        Address address = new Address();
        address.setCity("NewCity");
        address.setPostalCode("N5J2N5");
        address.setProvince("ON");
        address.setStreet("1111 Mountain Blvd. Floor 13, suite 1");
        address.setCountry("Canada");
        uow.registerObject(address);
        uow.commit();
        uow = getSession().acquireUnitOfWork();
        uow.deleteObject(address);
        uow.commit();
    }
}
