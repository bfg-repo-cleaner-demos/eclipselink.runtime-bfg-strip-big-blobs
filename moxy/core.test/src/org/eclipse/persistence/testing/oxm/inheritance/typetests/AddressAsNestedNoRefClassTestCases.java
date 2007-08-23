/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.inheritance.typetests;

import org.eclipse.persistence.oxm.mappings.XMLCompositeObjectMapping;
import org.eclipse.persistence.sessions.Project;

public class AddressAsNestedNoRefClassTestCases extends AddressAsNestedTestCases {
    public AddressAsNestedNoRefClassTestCases(String name) throws Exception {
        super(name);
        Project p = new TypeProject();
        ((XMLCompositeObjectMapping)p.getDescriptor(Employee.class).getMappingForAttributeName("address")).setReferenceClass(null);
        ((XMLCompositeObjectMapping)p.getDescriptor(Employee.class).getMappingForAttributeName("address")).setReferenceClassName(null);
        setProject(p);
    }

    public static void main(String[] args) {
        String[] arguments = { "-c", "org.eclipse.persistence.testing.oxm.inheritance.typetests.AddressAsNestedNoRefClassTestCases" };
        junit.textui.TestRunner.main(arguments);
    }
}