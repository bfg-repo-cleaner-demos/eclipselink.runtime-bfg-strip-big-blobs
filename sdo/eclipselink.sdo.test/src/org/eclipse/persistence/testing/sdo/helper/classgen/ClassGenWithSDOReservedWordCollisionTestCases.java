/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.sdo.helper.classgen;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import junit.textui.TestRunner;

public class ClassGenWithSDOReservedWordCollisionTestCases extends SDOClassGenTestCases {
    public ClassGenWithSDOReservedWordCollisionTestCases(String name) {
        super(name);
    }

    public static void main(String[] args) {
        String[] arguments = { "-c", "org.eclipse.persistence.testing.sdo.helper.classgen.ClassGenWithSDOReservedWordCollisionTestCases" };
        TestRunner.main(arguments);
    }

    protected List getControlFileNames() {
        ArrayList<String> controlFileNames = new ArrayList<String>();
        controlFileNames.add("Company.java");
        controlFileNames.add("CompanyImpl.java");
        controlFileNames.add("Type.java");
        controlFileNames.add("TypeImpl.java");
        controlFileNames.add("Item.java");
        controlFileNames.add("ItemImpl.java");
        controlFileNames.add("PhoneType.java");
        controlFileNames.add("PhoneTypeImpl.java");
        controlFileNames.add("PurchaseOrder.java");
        controlFileNames.add("PurchaseOrderImpl.java");
        controlFileNames.add("USAddress.java");
        controlFileNames.add("USAddressImpl.java");
        return controlFileNames;
    }

    protected String getSourceFolder() {
        return "./collidesdo";
    }

    protected String getControlSourceFolder() {
        return "./org/eclipse/persistence/testing/sdo/helper/classgen/collidesdo";

    }

    protected String getSchemaName() {
        return "org/eclipse/persistence/testing/sdo/schemas/classgen/CompanyWithSequenceCSWithSDOReservedName.xsd";
    }

  /*  public void testClassGen() {
        compileFiles();
        StringReader reader = new StringReader(xsdString);
        classGenerator.generate(reader, getSourceFolder());
        int numGenerated = classGenerator.getGeneratedBuffers().size();
        assertEquals(getControlFileNames().size() / 2, numGenerated);
        // we cannot compare files at this point because the generated classes are invalid and wont compile
        // we generate warnings until 6067502 is implemented
        //compareFiles(getControlFiles(), getGeneratedFiles(classGenerator.getGeneratedBuffers()));
    }*/
}
