/*******************************************************************************
* Copyright (c) 1998, 2008 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
* which accompanies this distribution.
* The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
* and the Eclipse Distribution License is available at
* http://www.eclipse.org/org/documents/edl-v10.php.
*
* Contributors:
* mmacivor - April 25/2008 - 1.0M8 - Initial implementation
******************************************************************************/
package org.eclipse.persistence.testing.jaxb.simpledocument;

import org.eclipse.persistence.testing.jaxb.JAXBTestCases;
import javax.xml.bind.JAXBElement;

/**
 * Tests mapping a simple document containing a single base64 element to a Byte Array
 * @author mmacivor
 *
 */
public class SimpleDocumentByteArrayTestCases extends JAXBTestCases {
		private final static String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/simpledocument/bytearrayroot.xml";

	    public SimpleDocumentByteArrayTestCases(String name) throws Exception {
	        super(name);
	        setControlDocument(XML_RESOURCE);        
	        Class[] classes = new Class[1];
	        classes[0] = ObjectFactory.class;
	        setClasses(classes);
	    }

	    protected Object getControlObject() {
	    	JAXBElement value = new ObjectFactory().createBase64Root();
	    	value.setValue(new Byte[]{new Byte((byte)1), new Byte((byte)2), new Byte((byte)3), new Byte((byte)4), new Byte((byte)5), new Byte((byte)6), new Byte((byte)7)});
	    	return value;      
	    }
	    
	    public void compareJAXBElementObjects(JAXBElement controlObj, JAXBElement testObj) {
	        assertEquals(controlObj.getName().getLocalPart(), testObj.getName().getLocalPart());
	        assertEquals(controlObj.getName().getNamespaceURI(), testObj.getName().getNamespaceURI());
	        Byte[] controlBytes = (Byte[])controlObj.getValue();
	        Byte[] testBytes = (Byte[])testObj.getValue();
	        assertEquals(controlBytes.length, testBytes.length);
	        for(int i = 0; i < controlBytes.length; i++) {
	        	assertEquals(controlBytes[i].byteValue(), testBytes[i].byteValue());
	        }

	    }
	    
}
