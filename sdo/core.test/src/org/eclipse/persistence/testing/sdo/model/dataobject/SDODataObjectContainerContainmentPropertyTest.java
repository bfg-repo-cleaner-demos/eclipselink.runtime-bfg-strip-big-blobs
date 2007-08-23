/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.sdo.model.dataobject;

public class SDODataObjectContainerContainmentPropertyTest extends SDODataObjectContainerContainmentPropertyTestCases {
    public SDODataObjectContainerContainmentPropertyTest(String name) {
        super(name);
    }

    // purpose: test if a dataobject's cotainer is changed if other dataobject
    // reset its containment property's value to this dataobject, e.g.
    // Before, b contains c, then
    public void testContainerContainmentPropertyAfterReset() {
        this.assertEquals(dataObject_b, dataObject_c.getContainer());
        dataObject_a.set(property_a, dataObject_c);
        this.assertFalse(dataObject_c.getContainer().equals(dataObject_b));
    }

    // purpose: test if a dataobject's cotainer is changed if other dataobject
    // reset its containment property's value to this dataobject, e.g.
    // Before, b contains c, then
    public void testContainerContainmentPropertyAfterUnset() {
        this.assertEquals(dataObject_a, dataObject_b.getContainer());
        dataObject_a.unset(property_a);
        this.assertNull(dataObject_b.getContainer());
    }
}