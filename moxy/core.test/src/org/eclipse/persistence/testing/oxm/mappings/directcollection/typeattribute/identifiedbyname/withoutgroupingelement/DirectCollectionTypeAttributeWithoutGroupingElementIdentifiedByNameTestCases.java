/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.mappings.directcollection.typeattribute.identifiedbyname.withoutgroupingelement;

import java.util.Vector;

import org.eclipse.persistence.testing.oxm.mappings.XMLMappingTestCases;
import org.eclipse.persistence.testing.oxm.mappings.directcollection.Employee;

public class DirectCollectionTypeAttributeWithoutGroupingElementIdentifiedByNameTestCases extends XMLMappingTestCases {

  private final static String XML_RESOURCE = "org/eclipse/persistence/testing/oxm/mappings/directcollection/typeattribute/identifiedbyname/withoutgroupingelement/DirectCollectionTypeAttributeWithoutGroupingElementIdentifiedByName.xml";
  private final static int CONTROL_ID = 123;
	private final static String CONTROL_RESPONSIBILITY1 = "make the coffee";
  private final static String CONTROL_RESPONSIBILITY2 = "do the dishes";
  private final static String CONTROL_RESPONSIBILITY3 = "take out the garbage";

  public DirectCollectionTypeAttributeWithoutGroupingElementIdentifiedByNameTestCases(String name) throws Exception {
    super(name);
    setControlDocument(XML_RESOURCE);
		setProject(new DirectCollectionTypeAttributeWithoutGroupingElementIdentifiedByNameProject());
  }

  protected Object getControlObject() {
		Vector responsibilities = new Vector();
		responsibilities.addElement(CONTROL_RESPONSIBILITY1);
		responsibilities.addElement(CONTROL_RESPONSIBILITY2);
		responsibilities.addElement(CONTROL_RESPONSIBILITY3);

    Employee employee = new Employee();
    employee.setID(CONTROL_ID);
		employee.setResponsibilities(responsibilities);
    return employee;
  }

}
