/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Denise Smith - February 2010 - 2.1
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.inheritance.typetests.compositecollection;

import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.mappings.XMLCompositeCollectionMapping;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.testing.oxm.inheritance.typetests.CanadianAddress;
import org.eclipse.persistence.testing.oxm.mappings.XMLMappingTestCases;

public class CanadianAddressAsRefClassTestCases extends XMLMappingTestCases {
    private static final String READ_DOC = "org/eclipse/persistence/testing/oxm/inheritance/typetests/employee_with_address_cdnaddressnoxsi.xml";
    
    public CanadianAddressAsRefClassTestCases(String name) throws Exception {
        super(name);
        Project p = new COMCollectionTypeProject();
        ((XMLCompositeCollectionMapping)p.getDescriptor(Employee.class).getMappingForAttributeName("addresses")).setReferenceClass(CanadianAddress.class);
        ((XMLField)((XMLCompositeCollectionMapping)p.getDescriptor(Employee.class).getMappingForAttributeName("addresses")).getField()).setLeafElementType(null);
            
        setProject(p);
        setControlDocument(READ_DOC);
    }

    public Object getControlObject() {
        Employee emp = new Employee();
        ArrayList adds = new ArrayList();
        CanadianAddress cadd = new CanadianAddress();
        cadd.setId("123");
		cadd.setStreet("1 A Street");
		cadd.setPostalCode("A1B 2C3");
        adds.add(cadd);
     
        cadd = new CanadianAddress();
        cadd.setId("456");
        cadd.setStreet("2 A Street");
        cadd.setPostalCode("A1B 2C3");
        adds.add(cadd);
     
        emp.setAddresses(adds);
        return emp;
    }
}