/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.sdo.helper.xmlhelper.loadandsave.changesummary;

import commonj.sdo.DataObject;
import commonj.sdo.Type;
import org.eclipse.persistence.sdo.SDOConstants;
import org.eclipse.persistence.sdo.SDOProperty;
import org.eclipse.persistence.testing.sdo.helper.xmlhelper.loadandsave.LoadAndSaveTestCases;

public abstract class ChangeSummaryRootLoadAndSaveTestCases extends LoadAndSaveTestCases {
    public ChangeSummaryRootLoadAndSaveTestCases(String name) {
        super(name);
    }

    protected String getSchemaName() {
        return "./org/eclipse/persistence/testing/sdo/schemas/Team_cs_on_root.xsd";
    }

    protected String getControlRootURI() {
        return "http://www.example.org";
    }

    protected String getControlRootName() {
        return "root";
    }
    
      protected String getRootInterfaceName() {
        return "Team";
    }

    protected void registerTypes() {
        Type stringType = typeHelper.getType("commonj.sdo", "String");
        Type employeeType = registerEmployeeType();

        // create a new Type for Customers
        DataObject teamType = dataFactory.create("commonj.sdo", "Type");

        SDOProperty prop = (SDOProperty)teamType.getType().getProperty("uri");
        teamType.set(prop, getControlRootURI());

        prop = (SDOProperty)teamType.getType().getProperty("name");
        teamType.set(prop, "team");

        addProperty(teamType, "name", stringType, true, false, true);
        DataObject managerProp = addProperty(teamType, "manager", employeeType, true, false, true);
        DataObject myChangeSummaryProp = addProperty(teamType, "myChangeSummary", SDOConstants.SDO_CHANGESUMMARY, true, false, true);

        Type teamSDOType = typeHelper.define(teamType);

        DataObject propDO = dataFactory.create(SDOConstants.SDO_PROPERTY);
        propDO.set("name", getControlRootName());
        propDO.set("type", teamSDOType);
        typeHelper.defineOpenContentProperty(getControlRootURI(), propDO);
    }

    private Type registerEmployeeType() {
        Type stringType = typeHelper.getType("commonj.sdo", "String");
        Type addressType = registerAddressType();
        Type phoneType = registerPhoneType();

        // create a new Type for addressType
        DataObject employeeType = dataFactory.create("commonj.sdo", "Type");

        SDOProperty prop = (SDOProperty)employeeType.getType().getProperty("uri");
        employeeType.set(prop, getControlRootURI());

        prop = (SDOProperty)employeeType.getType().getProperty("name");
        employeeType.set(prop, "Employee");

        addProperty(employeeType, "id", stringType, false, false, true);
        addProperty(employeeType, "name", stringType, false, false, true);
        addProperty(employeeType, "previousLastNames", stringType, false, true, true);        
        addProperty(employeeType, "address", addressType, true, false, true);
        DataObject phoneProp = addProperty(employeeType, "phone", phoneType, true, true, true);

        return typeHelper.define(employeeType);
    }

    private Type registerPhoneType() {
        Type stringType = typeHelper.getType("commonj.sdo", "String");

        // create a new Type for phoneType
        DataObject phoneType = dataFactory.create("commonj.sdo", "Type");

        SDOProperty prop = (SDOProperty)phoneType.getType().getProperty("uri");
        phoneType.set(prop, getControlRootURI());

        prop = (SDOProperty)phoneType.getType().getProperty("name");
        phoneType.set(prop, "Phone");

        addProperty(phoneType, "number", stringType, false, false, true);

        return typeHelper.define(phoneType);
    }

    private Type registerAddressType() {
        Type yardType = registerYardType();
        Type stringType = typeHelper.getType("commonj.sdo", "String");

        // create a new Type for addressType
        DataObject addressType = dataFactory.create("commonj.sdo", "Type");

        SDOProperty prop = (SDOProperty)addressType.getType().getProperty("uri");
        addressType.set(prop, getControlRootURI());

        prop = (SDOProperty)addressType.getType().getProperty("name");
        addressType.set(prop, "Address");

        addProperty(addressType, "street", stringType, false, false, true);
        addProperty(addressType, "city", stringType, false, false, true);
        addProperty(addressType, "province", stringType, false, false, true);
        addProperty(addressType, "yard", yardType, true, false, true);

        return typeHelper.define(addressType);
    }

    private Type registerYardType() {
        // create a new Type for addressType
        DataObject yardType = dataFactory.create("commonj.sdo", "Type");

        SDOProperty prop = (SDOProperty)yardType.getType().getProperty("uri");
        yardType.set(prop, getControlRootURI());
        yardType.set("name", "Yard");

        addProperty(yardType, "squarefootage", SDOConstants.SDO_STRING, false, false, true);
        addProperty(yardType, "length", SDOConstants.SDO_STRING, false, false, true);
        addProperty(yardType, "width", SDOConstants.SDO_STRING, false, false, true);

        return typeHelper.define(yardType);
    }
}