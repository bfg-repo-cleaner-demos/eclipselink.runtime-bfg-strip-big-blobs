/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.mappings.directcollection.union;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.testing.oxm.mappings.XMLMappingTestCases;
import org.w3c.dom.Document;

public class SimpleUnionNoConversionTestCases extends XMLMappingTestCases {
    private final static String XML_RESOURCE = "org/eclipse/persistence/testing/oxm/mappings/directcollection/union/SimpleUnionNoConversion.xml";
    private final static String CONTROL_ITEM = "ten";
    private final static String CONTROL_FIRST_NAME = "Jane";
    private final static String CONTROL_LAST_NAME = "Doe";

    public SimpleUnionNoConversionTestCases(String name) throws Exception {
        super(name);
        setControlDocument(XML_RESOURCE);
        setProject(new SimpleUnionProject());
    }

    protected Object getControlObject() {
        Person person = new Person();
        person.addItem(CONTROL_ITEM);
        person.setFirstName(CONTROL_FIRST_NAME);
        person.setLastName(CONTROL_LAST_NAME);
        return person;
    }

    public void testXMLToObjectFromInputStream() throws Exception {
        try {
            xmlUnmarshaller.unmarshal(ClassLoader.getSystemResource(XML_RESOURCE));
        } catch (Exception e) {
            handleException(e);
            return;
        }
        fail("no error occurred...expected XMLConversionException");
    }

    public void testXMLToObjectFromURL() throws Exception {
        try {
            xmlUnmarshaller.unmarshal(getControlDocument());
        } catch (Exception e) {
            handleException(e);
            return;
        }
        fail("no error occurred...expected XMLConversionException");
    }

    /*
        public void testXMLToObjectFromDocument() throws Exception {
            try {
                xmlUnmarshaller.unmarshal(getControlDocument());
            } catch (Exception e) {
                handleException(e);
                return;
            }
            fail("no error occurred...expected XMLConversionException");
        }
    */

    public void testUnmarshallerHandler() throws Exception {
        try {
            super.testUnmarshallerHandler();
        } catch (Exception e) {
            handleException(e);
            return;
        }
        fail("no error occurred...expected XMLConversionException");
    }

    private void handleException(Exception e) {
        boolean rightException = (e instanceof ConversionException);
        if (rightException) {
            boolean rightMessage = ((ConversionException)e).getErrorCode() == (ConversionException.COULD_NOT_BE_CONVERTED);
            if (!rightMessage) {
                fail("An incorrect ConversionException occurred");
            }
        } else {
            fail("an invalid Exception occurred, expected XMLConversionException");
        }
    }
}