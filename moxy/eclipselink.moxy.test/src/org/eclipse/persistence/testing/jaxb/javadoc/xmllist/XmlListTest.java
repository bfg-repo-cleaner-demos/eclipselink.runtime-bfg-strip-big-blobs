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
 *     Praba Vijayaratnam - 2.3 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.javadoc.xmllist;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class XmlListTest extends JAXBTestCases{

	private final static String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/javadoc/xmllist/xmllist.xml";
	
	public XmlListTest(String name) throws Exception {
		super(name);
		setControlDocument(XML_RESOURCE);
		Class[] classes = new Class[1];
		classes[0] = Hockey.class;
		setClasses(classes);
	}

	protected Object getControlObject() {

		Hockey example = new Hockey();
		List<String> teams = new ArrayList();
		teams.add("Habs");
		teams.add("Leafs");
		teams.add("Sens");
		teams.add("Flames");
		teams.add("Oilers");
		teams.add("Canucks");
		example.teams = teams;
        return example;
	}


}
