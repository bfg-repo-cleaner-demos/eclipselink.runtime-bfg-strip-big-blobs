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
package org.eclipse.persistence.testing.oxm.inheritance.typetests.compositecollection;

import org.eclipse.persistence.testing.oxm.inheritance.typetests.ContactMethod;
import org.eclipse.persistence.testing.oxm.mappings.XMLWithJSONMappingTestCases;

import java.util.ArrayList;

public class ContactsAsNestedTestCases extends XMLWithJSONMappingTestCases {
    private static final String READ_DOC = "org/eclipse/persistence/testing/oxm/inheritance/typetests/customer_with_contacts_noxsi.xml";
    private static final String JSON_READ_DOC = "org/eclipse/persistence/testing/oxm/inheritance/typetests/customer_with_contacts_noxsi.json";
    
    public ContactsAsNestedTestCases(String name) throws Exception {
        super(name);
        setProject(new COMCollectionTypeProject());
        setControlDocument(READ_DOC);
        setControlJSON(JSON_READ_DOC);

    }

    public Object getControlObject() {
		Customer cust = new Customer();
		ArrayList contacts = new ArrayList();
        ContactMethod cm = new ContactMethod();
        cm.setId("123");
		contacts.add(cm);
        cm = new ContactMethod();
        cm.setId("456");
		contacts.add(cm);
        cm = new ContactMethod();
        cm.setId("789");
		contacts.add(cm);
		cust.setContactMethods(contacts);
        return cust;
    }

    public static void main(String[] args) {
        String[] arguments = { "-c", "org.eclipse.persistence.testing.oxm.inheritance.typetests.compositecollection.ContactsAsNestedTestCases" };
        junit.textui.TestRunner.main(arguments);
    }
}
