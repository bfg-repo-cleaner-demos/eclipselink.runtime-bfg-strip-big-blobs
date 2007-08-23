/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/
package org.eclipse.persistence.testing.sdo.helper.xmlhelper.loadandsave;

import commonj.sdo.DataObject;
import commonj.sdo.Type;
import commonj.sdo.helper.XMLDocument;
import java.util.List;
import junit.textui.TestRunner;
import org.eclipse.persistence.sdo.SDOConstants;

public class LoadAndSaveNillableIsSetNodeNullPolicyFalseTestCases extends
		LoadAndSaveTestCases {
	public LoadAndSaveNillableIsSetNodeNullPolicyFalseTestCases(String name) {
		super(name);
	}

	public void setUp() {
		super.setUp();
	}

	protected String getSchemaName() {
		return "./org/eclipse/persistence/testing/sdo/schemas/DirectIsSetNodeNullPolicyFalseElement.xsd";
	}

	protected String getControlFileName() {
		return "./org/eclipse/persistence/testing/sdo/helper/xmlhelper/nillable/DirectIsSetNodeNullPolicyFalseElement.xml";
	}

	protected String getControlWriteFileName() {
		return "./org/eclipse/persistence/testing/sdo/helper/xmlhelper/nillable/DirectIsSetNodeNullPolicyFalseElement.xml";
	}

	protected String getNoSchemaControlWriteFileName() {
		return "./org/eclipse/persistence/testing/sdo/helper/xmlhelper/nillable/DirectIsSetNodeNullPolicyFalseElementNoSchema.xml";
	}

	protected String getNoSchemaControlFileName() {
		//return "./org/eclipse/persistence/testing/sdo/helper/xmlhelper/nillable/DirectOptionalNodeNullPolicyElementNoSchema.xml";
		return "./org/eclipse/persistence/testing/sdo/helper/xmlhelper/nillable/DirectIsSetNodeNullPolicyFalseElementNoSchema.xml";		
	}

	protected String getControlRootURI() {
		return "http://www.example.org";
	}

	protected String getControlRootName() {
		return "employee";
	}

	protected String getRootInterfaceName() {
		return "EmployeeType";
	}

	protected void verifyAfterLoad(XMLDocument doc) {
		super.verifyAfterLoad(doc);
		Object value = doc.getRootObject().get("id");
		boolean isSet = doc.getRootObject().isSet("id");
		assertNull(value);
		// 20070615 changed because Direct now uses IsSetOptionalNNP not
		// OptionalNNP
		assertFalse(isSet);

		value = doc.getRootObject().get("task");
		isSet = doc.getRootObject().isSet("task");
		assertTrue(value instanceof List);
		assertTrue(((List) value).size() == 0);
		assertFalse(isSet);

		value = doc.getRootObject().get("last-name");
		isSet = doc.getRootObject().isSet("last-name");
		assertNotNull(value);
		assertTrue(isSet);

		value = doc.getRootObject().get("address");
		isSet = doc.getRootObject().isSet("address");
		assertNull(value);
		assertTrue(isSet);
	}

	public void registerTypes() {
		Type stringType = typeHelper.getType("commonj.sdo", "String");
		Type intType = typeHelper.getType("commonj.sdo", "Int");

		// ADDRESS TYYPE
		DataObject addressTypeDO = dataFactory.create("commonj.sdo", "Type");
		addressTypeDO.set("uri", getControlRootURI());
		addressTypeDO.set("name", "AddressType");

		DataObject addressCityProperty = addProperty(addressTypeDO, "city",
				stringType, false, false, true);
		addressCityProperty.set("nullable", false);
		Type addressType = typeHelper.define(addressTypeDO);

		// create a new Type for Customers
		DataObject customerType = dataFactory.create("commonj.sdo", "Type");
		customerType.set("uri", getControlRootURI());
		customerType.set("name", "EmployeeType");
		// customerType.set("name", "Employee");
		DataObject idProp = addProperty(customerType, "id", stringType, false,
				false, true);
		idProp.set("nullable", false);
		addProperty(customerType, "task", stringType, false, true, true);
		addProperty(customerType, "last-name", stringType, false, false, true);
		DataObject addressProp = addProperty(customerType, "address",
				addressType, true, false, true);
		addressProp.set("nullable", false);

		// now define the Customer type so that customers can be made
		Type customerSDOType = typeHelper.define(customerType);

		DataObject propDO = dataFactory.create(SDOConstants.SDO_PROPERTY);
		propDO.set("name", getControlRootName());
		propDO.set("type", customerSDOType);
		typeHelper.defineOpenContentProperty(getControlRootURI(), propDO);
	}

	public static void main(String[] args) {
		String[] arguments = {
				"-c",
				"org.eclipse.persistence.testing.sdo.helper.xmlhelper.loadandsave.LoadAndSaveNillableIsSetNodeNullPolicyFalseTestCases" };
		TestRunner.main(arguments);
	}
}