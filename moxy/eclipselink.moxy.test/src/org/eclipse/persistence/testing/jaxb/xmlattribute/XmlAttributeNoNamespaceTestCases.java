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
package org.eclipse.persistence.testing.jaxb.xmlattribute;

import java.util.ArrayList;
import java.util.Calendar;
import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class XmlAttributeNoNamespaceTestCases extends JAXBTestCases {

	private final static String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/xmlattribute/employee_nonamespace.xml";
	private final static int CONTROL_ID = 10;

    public XmlAttributeNoNamespaceTestCases(String name) throws Exception {
        super(name);
        setControlDocument(XML_RESOURCE);        
        Class[] classes = new Class[1];
        classes[0] = EmployeeNoNamespace.class;
        setClasses(classes);
    }

    protected Object getControlObject() {
        EmployeeNoNamespace employee = new EmployeeNoNamespace();
		employee.id = CONTROL_ID;
        return employee;
    }
}
