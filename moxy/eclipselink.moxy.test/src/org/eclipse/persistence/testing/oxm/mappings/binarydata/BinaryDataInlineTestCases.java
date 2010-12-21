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
 *     Denise Smith -February 2010 - 2.1 
 ******************************************************************************/
package org.eclipse.persistence.testing.oxm.mappings.binarydata;

import org.eclipse.persistence.testing.oxm.mappings.XMLMappingTestCases;

public class BinaryDataInlineTestCases extends XMLMappingTestCases {
    private final static String XML_RESOURCE = "org/eclipse/persistence/testing/oxm/mappings/binarydata/BinaryDataInline.xml";

	public BinaryDataInlineTestCases(String name) throws Exception {
		super(name);
		setControlDocument(XML_RESOURCE);
        setProject(new BinaryDataInlineProject());
	}
	
    protected Object getControlObject() {
        Employee emp = new Employee(123);
        emp.setPhoto(new byte[]{0,1,2,3});
        emp.setExtraPhoto(new byte[]{0,1,2,3});
        return emp;
    }

}

