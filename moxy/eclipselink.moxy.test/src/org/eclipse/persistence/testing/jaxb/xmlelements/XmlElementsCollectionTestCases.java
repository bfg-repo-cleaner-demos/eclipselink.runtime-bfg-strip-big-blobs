/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.jaxb.xmlelements;

import java.util.ArrayList;
import java.util.Calendar;
import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class XmlElementsCollectionTestCases extends JAXBTestCases {

    private final static String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/xmlelements/employee_collection.xml";
    private final static int CONTROL_ID = 10;

    public XmlElementsCollectionTestCases(String name) throws Exception {
        super(name);
        setControlDocument(XML_RESOURCE);        
        Class[] classes = new Class[2];
        classes[0] = EmployeeCollection.class;
        classes[1] = Address.class;
        setClasses(classes);
    }

    protected Object getControlObject() {
        EmployeeCollection employee = new EmployeeCollection();
        employee.id = CONTROL_ID;
        ArrayList choices = new ArrayList();
        choices.add(new Integer(12));
        choices.add("String Value");
        Address addr = new Address();
        addr.city = "Ottawa";
        addr.street = "123 Fake Street";
        choices.add(addr);
        choices.add(new Integer(5));
        employee.choice = choices;
        return employee;
    }
}

