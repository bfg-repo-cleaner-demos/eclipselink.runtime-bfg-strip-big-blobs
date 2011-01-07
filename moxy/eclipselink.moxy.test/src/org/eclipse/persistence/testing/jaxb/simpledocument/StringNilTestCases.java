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
 * mmacivor - April 8th/2010 - 2.1 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.simpledocument;

import javax.xml.bind.JAXBElement;

import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class StringNilTestCases extends JAXBTestCases {
    private final static String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/simpledocument/string_nil.xml";

    private final static String XML_WRITE_RESOURCE = "org/eclipse/persistence/testing/jaxb/simpledocument/string_empty.xml";
    public StringNilTestCases(String name) throws Exception {
        super(name);
        setControlDocument(XML_RESOURCE);        
        //setWriteControlDocument(XML_WRITE_RESOURCE);
        Class[] classes = new Class[1];
        classes[0] = StringObjectFactory.class;
        setClasses(classes);
    }

    protected Object getControlObject() {
        JAXBElement value = new StringObjectFactory().createStringRoot();
        value.setValue(null);
        value.setNil(true);
        return value;      
    }
    
    
}
