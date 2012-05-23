/*******************************************************************************
 * Copyright (c) 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Matt MacIvor - 2.4
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.sun.prefixmapper;

import org.eclipse.persistence.jaxb.JAXBMarshaller;
import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class NonELPrefixMapperTestCases extends JAXBTestCases {

    private static final String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/prefixmapper/employee.xml";
    
    public NonELPrefixMapperTestCases(String name) throws Exception {
        super(name);
        setClasses(new Class[]{Employee.class, Person.class});
        jaxbMarshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new NonELPrefixMapperImpl());
        setControlDocument(XML_RESOURCE);
    }

    @Override
    protected Object getControlObject() {
        Employee emp = new Employee();
        emp.firstName = "John";
        emp.lastName = "Doe";
        emp.address = "123 Fake Street";
        emp.employeeId = 4321;
        
        emp.manager = new Employee();
        emp.manager.firstName = "Bob";
        emp.manager.lastName = "Jones";
        emp.manager.address = "234 Some Street";
        ((Employee)emp.manager).employeeId = 9876;
        return emp;
    }
}
