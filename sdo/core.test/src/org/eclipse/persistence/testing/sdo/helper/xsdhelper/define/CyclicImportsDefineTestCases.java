/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.sdo.helper.xsdhelper.define;

import commonj.sdo.DataObject;
import commonj.sdo.Type;
import commonj.sdo.helper.XMLDocument;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import junit.textui.TestRunner;
import org.eclipse.persistence.sdo.SDOConstants;
import org.eclipse.persistence.sdo.SDOProperty;
import org.eclipse.persistence.sdo.SDOType;
import org.eclipse.persistence.sdo.helper.SDOXSDHelper;

public class CyclicImportsDefineTestCases extends XSDHelperDefineTestCases {
    public CyclicImportsDefineTestCases(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(CyclicImportsDefineTestCases.class);
    }

    public String getSchemaToDefine() {
        //return "org/eclipse/persistence/testing/sdo/helper/xsdhelper/generate/ImportsWithNamespaces.xsd";
        return "org/eclipse/persistence/testing/sdo/helper/xsdhelper/generate/Cyclic1.xsd";
    }

    protected String getSchemaLocation() {
        // return "file:./org/eclipse/persistence/testing/sdo/helper/xsdhelper/generate/";
        return "file:./org/eclipse/persistence/testing/sdo/helper/xsdhelper/generate/";
    }

    public void testDefine() {
        //String xsdSchema = getSchema(getSchemaToDefine());
        InputStream is = getSchemaInputStream(getSchemaToDefine());
        CyclicSchemaResolver schemaResolver = new CyclicSchemaResolver();
        schemaResolver.setBaseSchemaLocation(getSchemaLocation());
        Source xsdSource = new StreamSource(is);
        List types = ((SDOXSDHelper)xsdHelper).define(xsdSource, schemaResolver);

        //List types = xsdHelper.define(xsdSchema, getSchemaLocation());
        log("\nExpected:\n");
        List controlTypes = getControlTypes();
        log(controlTypes);

        log("\nActual:\n");
        log(types);

        compare(getControlTypes(), types);

        try {
            FileInputStream inStream = new FileInputStream("org/eclipse/persistence/testing/sdo/helper/xsdhelper/generate/cyclic.xml");
            XMLDocument theDoc = xmlHelper.load(inStream);
            assertNotNull(theDoc);
            assertNotNull(theDoc.getRootObject());
            DataObject shipToDO = theDoc.getRootObject().getDataObject("shipTo");
            DataObject billToDo = theDoc.getRootObject().getDataObject("billTo");
            assertNotNull(shipToDO);
            assertNotNull(billToDo);
            DataObject shipToPhoneData = shipToDO.getDataObject("thePhone");
            assertNotNull(shipToPhoneData);
            assertEquals("1234567", shipToPhoneData.getString("number"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("An error occurred during xmlhelper.load");
        }
    }

    public List getControlTypes() {
        List types = new ArrayList();
        String uri = "my.uri";
        String uri2 = "my.uri2";

        Type stringType = typeHelper.getType("commonj.sdo", "String");
        Type intType = typeHelper.getType("commonj.sdo", "Int");

        /****QUANTITY TYPE*****/
        SDOType quantityType = new SDOType(uri, "quantityType");
        quantityType.setDataType(true);
        quantityType.setInstanceClassName("java.lang.String");
        quantityType.addBaseType((SDOType)intType);

        /****SKU TYPE*****/
        SDOType SKUType = new SDOType(uri, "SKU");
        SKUType.setInstanceClassName("java.lang.String");
        SKUType.setDataType(true);
        SKUType.addBaseType((SDOType)stringType);

        /****PHONE TYPE*****/
        SDOType phoneType = new SDOType(uri, "PhoneType");
        phoneType.setDataType(false);
        phoneType.setInstanceClassName("defaultPackage.PhoneType");

        SDOProperty numberProp = new SDOProperty(aHelperContext);
        numberProp.setName("number");
        numberProp.setXsdLocalName("number");
        numberProp.setXsd(true);
        numberProp.setType(stringType);
        phoneType.addDeclaredProperty(numberProp);

        /****ADDRESS TYPE*****/

        //ADDRESS TYPE
        SDOType USaddrType = new SDOType(uri2, "USAddress");
        USaddrType.setDataType(false);
        USaddrType.setInstanceClassName("defaultPackage.USAddress");

        SDOProperty streetProp = new SDOProperty(aHelperContext);
        streetProp.setName("street");
        streetProp.setXsd(true);
        streetProp.setXsdLocalName("street");
        streetProp.setType(stringType);
        USaddrType.addDeclaredProperty(streetProp);

        SDOProperty cityProp = new SDOProperty(aHelperContext);
        cityProp.setName("city");
        cityProp.setXsdLocalName("city");
        cityProp.setType(stringType);
        cityProp.setXsd(true);
        USaddrType.addDeclaredProperty(cityProp);

        SDOProperty quantityProp = new SDOProperty(aHelperContext);
        quantityProp.setName("quantity");
        quantityProp.setXsdLocalName("quantity");
        //quantityProp.setType(quantityType);
        quantityProp.setType(stringType);
        quantityProp.setXsd(true);
        USaddrType.addDeclaredProperty(quantityProp);

        SDOProperty partNumProp = new SDOProperty(aHelperContext);
        partNumProp.setName("partNum");
        partNumProp.setXsdLocalName("partNum");
        partNumProp.setType(SKUType);
        partNumProp.setXsd(true);
        USaddrType.addDeclaredProperty(partNumProp);

        SDOProperty phoneProp = new SDOProperty(aHelperContext);
        phoneProp.setName("thePhone");
        phoneProp.setXsdLocalName("thePhone");
        phoneProp.setType(phoneType);
        phoneProp.setXsd(true);
        phoneProp.setInstanceProperty(SDOConstants.XMLELEMENT_PROPERTY, Boolean.TRUE);
        USaddrType.addDeclaredProperty(phoneProp);

        /****PURCHASEORDER TYPE*****/
        SDOProperty shipToProp = new SDOProperty(aHelperContext);
        shipToProp.setName("shipTo");
        shipToProp.setXsdLocalName("shipTo");
        shipToProp.setContainment(true);
        shipToProp.setType(USaddrType);
        shipToProp.setXsd(true);

        SDOProperty billToProp = new SDOProperty(aHelperContext);
        billToProp.setName("billTo");
        billToProp.setXsdLocalName("billTo");
        billToProp.setContainment(true);
        billToProp.setType(USaddrType);
        billToProp.setXsd(true);

        SDOType POtype = new SDOType(uri, "PurchaseOrder");
        POtype.setInstanceClassName("defaultPackage.PurchaseOrder");
        POtype.setDataType(false);
        POtype.addDeclaredProperty(shipToProp);
        POtype.addDeclaredProperty(billToProp);
        //POtype.addDeclaredProperty(quantityProp);
        //POtype.addDeclaredProperty(partNumProp);
        types.add(POtype);
        types.add(phoneType);
        types.add(USaddrType);
        //types.add(quantityType);
        types.add(SKUType);
        return types;
    }
}