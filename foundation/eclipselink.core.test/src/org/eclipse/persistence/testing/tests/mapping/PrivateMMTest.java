/*******************************************************************************
 * Copyright (c) 1998, 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.eclipse.persistence.testing.tests.mapping;

import java.util.*;
import org.eclipse.persistence.testing.framework.*;
import org.eclipse.persistence.testing.framework.DeleteObjectTest;
import org.eclipse.persistence.testing.models.mapping.*;
import org.eclipse.persistence.sessions.*;

/**
 * This type was generated by a SmartGuide.
 * @author Load Builder
 */
public class PrivateMMTest extends DeleteObjectTest {
    Employee employeeBeingDeleted;
    Vector phoneNumbers;
    public UnitOfWork unitOfWork;
    Vector shipments;
    Vector managedEmployees;
    Employee manager;

    /**
     * Private1MTest constructor comment.
     * @param originalObject java.lang.Object
     */
    public PrivateMMTest(Object originalObject) {
        super(originalObject);
    }

    public void reset() {
        super.reset();
        getSession().getIdentityMapAccessor().initializeIdentityMaps();
    }

    protected void setup() {
        super.setup();

        unitOfWork = getSession().acquireUnitOfWork();
        employeeBeingDeleted = (Employee)(unitOfWork.registerObject(originalObject));
        managedEmployees = employeeBeingDeleted.getManagedEmployees();
        phoneNumbers = employeeBeingDeleted.getPhoneNumbers();
        shipments = employeeBeingDeleted.getShipments();
        manager = employeeBeingDeleted.getManager();

        if (manager != null) {
            manager.removeManagedEmployee(employeeBeingDeleted);
        }

        Enumeration employeeEnum = managedEmployees.elements();
        while (employeeEnum.hasMoreElements()) {
            ((Employee)employeeEnum.nextElement()).setManager(null);
        }

        Enumeration shipmentEnum = shipments.elements();
        while (shipmentEnum.hasMoreElements()) {
            ((Shipment)shipmentEnum.nextElement()).removeEmployee(employeeBeingDeleted);
        }
    }

    protected void test() {
        unitOfWork.deleteObject(employeeBeingDeleted);
        unitOfWork.commit();
    }

    protected void verify() {
        super.verify();
        Enumeration enumtr = phoneNumbers.elements();
        while (enumtr.hasMoreElements()) {
            if (!(verifyDelete(enumtr.nextElement()))) {
                throw new TestErrorException("Private parts were not deleted along with " + employeeBeingDeleted);
            }
        }
    }
}
