/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.jaxb.xmlschematype;

import java.util.ArrayList;
import java.util.Calendar;
import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class XmlSchemaTypeDateTestCases extends JAXBTestCases {

    private final static String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/xmlschematype/employee_date.xml";

    public XmlSchemaTypeDateTestCases(String name) throws Exception {
        super(name);
        setControlDocument(XML_RESOURCE);        
        Class[] classes = new Class[1];
        classes[0] = EmployeeDate.class;
        setClasses(classes);
    }

    protected Object getControlObject() {
        EmployeeDate employee = new EmployeeDate();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2005,04,24);
      
        employee.startDate = cal;
                
        return employee;
    }
}