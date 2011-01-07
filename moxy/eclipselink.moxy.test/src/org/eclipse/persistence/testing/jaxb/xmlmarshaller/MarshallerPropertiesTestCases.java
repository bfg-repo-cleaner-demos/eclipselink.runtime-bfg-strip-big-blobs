/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.jaxb.xmlmarshaller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.Document;
import org.eclipse.persistence.testing.oxm.OXTestCase;

public class MarshallerPropertiesTestCases extends OXTestCase {
    private final static String CONTROL_NO_NAMESPACE_XML = "org/eclipse/persistence/testing/oxm/jaxb/Employee_NoNamespaceSchema.xml";
    private final static String CONTROL_NAMESPACE_XML = "org/eclipse/persistence/testing/oxm/jaxb/Employee_NamespaceSchema.xml";
    private final static String CONTROL_BOTH = "org/eclipse/persistence/testing/oxm/jaxb/Employee_BothNamespaceAttr.xml";
    private final static String CONTROL_NONE = "org/eclipse/persistence/testing/oxm/jaxb/Employee_NoSchemaLocation.xml";
    private Marshaller marshaller;
    private DocumentBuilder parser;
    private String contextPath;
    private String originalNNSchemaLocation;
    private String originalSchemaLocation;

    public MarshallerPropertiesTestCases(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        contextPath = System.getProperty("jaxb.test.contextpath", JAXBSAXTestSuite.CONTEXT_PATH);

        JAXBContext jaxbContext = JAXBContext.newInstance(contextPath, getClass().getClassLoader());
        marshaller = jaxbContext.createMarshaller();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setIgnoringElementContentWhitespace(true);
        parser = documentBuilderFactory.newDocumentBuilder();
        originalNNSchemaLocation = (String)marshaller.getProperty("jaxb.noNamespaceSchemaLocation");
        originalSchemaLocation = (String)marshaller.getProperty("jaxb.schemaLocation");
    }

    public void tearDown() throws Exception {
        marshaller.setProperty("jaxb.noNamespaceSchemaLocation", originalNNSchemaLocation);
        marshaller.setProperty("jaxb.schemaLocation", originalSchemaLocation);
    }

    public void testNoNamespaceSchemaLocation() throws Exception {
        Document control = parse(CONTROL_NO_NAMESPACE_XML);
        Document marshalled = parser.newDocument();
        marshaller.setProperty("jaxb.noNamespaceSchemaLocation", "http://www.example.com/Employee.xsd");
        Employee emp = new Employee();
        emp.setID(456);
        marshaller.marshal(emp, marshalled);
        assertXMLIdentical(marshalled, control);
    }

    public void testNamespaceSchemaLocation() throws Exception {
        Document control = parse(CONTROL_NAMESPACE_XML);
        Document marshalled = parser.newDocument();
        marshaller.setProperty("jaxb.schemaLocation", "http://www.example.com/Employee.xsd");
        Employee emp = new Employee();
        emp.setID(456);
        marshaller.marshal(emp, marshalled);
        assertXMLIdentical(control, marshalled);
    }

    public void testBothSchemaLocations() throws Exception {
        Document control = parse(CONTROL_BOTH);
        Document marshalled = parser.newDocument();
        marshaller.setProperty("jaxb.schemaLocation", "http://www.example.com/Employee.xsd");
        marshaller.setProperty("jaxb.noNamespaceSchemaLocation", "file:////ade/mmacivor_toplink10i/tltest/resource/ox/Employee.xsd");
        Employee emp = new Employee();
        emp.setID(456);
        marshaller.marshal(emp, marshalled);
        assertXMLIdentical(control, marshalled);
    }

    public void testNoSchemaLocation() throws Exception {
        Document control = parse(CONTROL_NONE);
        Document marshalled = parser.newDocument();
        Employee emp = new Employee();
        emp.setID(456);
        marshaller.marshal(emp, marshalled);
        assertXMLIdentical(control, marshalled);
    }

    public void testSetNullPropertyException() {
        boolean caughtException = false;
        try {
            marshaller.setProperty(null, null);
        } catch (IllegalArgumentException e) {
            caughtException = true;
        } catch (Exception e) {
        }
        assertTrue("JAXBMarshaller did not throw IllegalArgumentException as expected.", caughtException);
    }

    public void testGetNullPropertyException() {
        boolean caughtException = false;
        try {
            marshaller.getProperty(null);
        } catch (IllegalArgumentException e) {
            caughtException = true;
        } catch (Exception e) {
        }
        assertTrue("JAXBMarshaller did not throw IllegalArgumentException as expected.", caughtException);
    }

    public void testSetInvalidPropertyException() {
        boolean caughtException = false;
        try {
            marshaller.setProperty("thisIsAnInvalidProperty", "thisIsAnInvalidValue");
        } catch (PropertyException e) {
            caughtException = true;
        } catch (Exception e) {
        }
        assertTrue("JAXBMarshaller did not throw PropertyException as expected.", caughtException);
    }

    public void testGetInvalidPropertyException() {
        boolean caughtException = false;
        try {
            marshaller.getProperty("thisIsAnInvalidProperty");
        } catch (PropertyException e) {
            caughtException = true;
        } catch (Exception e) {
        }
        assertTrue("JAXBMarshaller did not throw PropertyException as expected.", caughtException);
    }

    private Document parse(String resource) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resource);
        Document document = parser.parse(stream);
        removeEmptyTextNodes(document);
        return document;
    }
}
