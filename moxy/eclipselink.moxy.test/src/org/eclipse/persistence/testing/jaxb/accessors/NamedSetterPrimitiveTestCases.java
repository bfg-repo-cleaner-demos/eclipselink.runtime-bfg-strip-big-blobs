/*******************************************************************************
* Copyright (c) 2010 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
* which accompanies this distribution.
* The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
* and the Eclipse Distribution License is available at
* http://www.eclipse.org/org/documents/edl-v10.php.
*
* Contributors:
*     Denise Smith
******************************************************************************/
package org.eclipse.persistence.testing.jaxb.accessors;

import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class NamedSetterPrimitiveTestCases extends JAXBTestCases {

    private static final String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/accessors/address.xml";
    private static final String XML_WRITE_RESOURCE = "org/eclipse/persistence/testing/jaxb/accessors/address_write.xml";    
	
	public NamedSetterPrimitiveTestCases(String name) throws Exception {
		super(name);
		setControlDocument(XML_RESOURCE);
		setWriteControlDocument(XML_WRITE_RESOURCE);
		setClasses(new Class[]{Address.class});
	}

	protected Object getControlObject() {
		
		Address addr = new Address();
		addr.setId("10");
		addr.setCity("Ottawa");
		addr.setStreetNumber(123);
		return addr;
	}
	
	public Object getReadControlObject() {
		
		Address addr = new Address();
		addr.setId("10");
		addr.setCity("Ottawa");
		addr.setStreetNumber(123);
		return addr;
	}
	
	public void testRoundTrip(){}
}