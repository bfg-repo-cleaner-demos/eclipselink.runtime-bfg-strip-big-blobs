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
 *     rbarkhouse - 2010-03-04 12:22:11 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.dynamic;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.jaxb.DynamicJAXBContext;
import org.eclipse.persistence.jaxb.DynamicJAXBContextFactory;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.sessions.Project;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class DynamicJAXBFromXSDTestCases extends TestCase {

    private static final String RESOURCE_DIR = "org/eclipse/persistence/testing/jaxb/dynamic/";

    // Schema files used to test each annotation
    private static final String XMLSCHEMA_QUALIFIED = RESOURCE_DIR + "xmlschema-qualified.xsd";
    private static final String XMLSCHEMA_UNQUALIFIED = RESOURCE_DIR +  "xmlschema-unqualified.xsd";
    private static final String XMLSCHEMA_DEFAULTS = RESOURCE_DIR + "xmlschema-defaults.xsd";
    private static final String XMLSEEALSO = RESOURCE_DIR + "xmlseealso.xsd";
    private static final String XMLROOTELEMENT = RESOURCE_DIR + "xmlrootelement.xsd";
    private static final String XMLTYPE = RESOURCE_DIR + "xmltype.xsd";
    private static final String XMLATTRIBUTE = RESOURCE_DIR + "xmlattribute.xsd";
    private static final String XMLELEMENT = RESOURCE_DIR + "xmlelement.xsd";

    private static final String PACKAGE = "mynamespace";
    private static final String PERSON = "Person";
    private static final String EMPLOYEE = "Employee";

    DynamicJAXBContext jaxbContext;

    NamespaceResolver nsResolver;

    public DynamicJAXBFromXSDTestCases(String name) throws Exception {
        super(name);

        nsResolver = new NamespaceResolver();
        nsResolver.put("ns0", "myNamespace");
        nsResolver.put("xsi", XMLConstants.SCHEMA_INSTANCE_URL);
    }

    public void testXmlSchemaQualified() throws Exception {
        // <xs:schema targetNamespace="myNamespace" xmlns:xs="http://www.w3.org/2001/XMLSchema"
        //      attributeFormDefault="qualified" elementFormDefault="qualified">

        InputStream inputStream = ClassLoader.getSystemResourceAsStream(XMLSCHEMA_QUALIFIED);
        jaxbContext = DynamicJAXBContextFactory.createContextFromXSD(inputStream, nsResolver, null, null);

        DynamicEntity person = jaxbContext.newDynamicEntity(PACKAGE + "." + PERSON);
        assertNotNull("Could not create Dynamic Entity.", person);

        person.set("id", 456);
        person.set("name", "Bob Dobbs");

        Document marshalDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        jaxbContext.createMarshaller().marshal(person, marshalDoc);

        // Make sure "targetNamespace" was interpreted properly.
        Node node = marshalDoc.getChildNodes().item(0);
        assertEquals("Target Namespace was not set as expected.", "myNamespace", node.getNamespaceURI());

        // Make sure "elementFormDefault" was interpreted properly.
        // elementFormDefault=qualified, so the root node, the
        // root node's attribute, and the child node should all have a prefix.
        assertNotNull("Root node did not have namespace prefix as expected.", node.getPrefix());

        Node attr = node.getAttributes().item(0);
        assertNotNull("Attribute did not have namespace prefix as expected.", attr.getPrefix());

        Node childNode = node.getChildNodes().item(0);
        assertNotNull("Child node did not have namespace prefix as expected.", childNode.getPrefix());
    }

    public void testXmlSchemaUnqualified() throws Exception {
        // <xs:schema targetNamespace="myNamespace" xmlns:xs="http://www.w3.org/2001/XMLSchema"
        //      attributeFormDefault="unqualified" elementFormDefault="unqualified">

        InputStream inputStream = ClassLoader.getSystemResourceAsStream(XMLSCHEMA_UNQUALIFIED);
        jaxbContext = DynamicJAXBContextFactory.createContextFromXSD(inputStream, nsResolver, null, null);

        DynamicEntity person = jaxbContext.newDynamicEntity(PACKAGE + "." + PERSON);
        assertNotNull("Could not create Dynamic Entity.", person);

        person.set("id", 456);
        person.set("name", "Bob Dobbs");

        Document marshalDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        jaxbContext.createMarshaller().marshal(person, marshalDoc);

        // Make sure "targetNamespace" was interpreted properly.
        Node node = marshalDoc.getChildNodes().item(0);
        assertEquals("Target Namespace was not set as expected.", "myNamespace", node.getNamespaceURI());

        // Make sure "elementFormDefault" was interpreted properly.
        // elementFormDefault=unqualified, so the root node should have a prefix
        // but the root node's attribute and child node should not.
        assertNotNull("Root node did not have namespace prefix as expected.", node.getPrefix());

        Node attr = node.getAttributes().item(0);
        assertNull("Attribute should not have namespace prefix.", attr.getPrefix());

        Node childNode = node.getChildNodes().item(0);
        assertNull("Child node should not have namespace prefix.", childNode.getPrefix());
    }

    public void testXmlSchemaDefaults() throws Exception {
        // <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">

        InputStream inputStream = ClassLoader.getSystemResourceAsStream(XMLSCHEMA_DEFAULTS);
        jaxbContext = DynamicJAXBContextFactory.createContextFromXSD(inputStream, nsResolver, null, null);

        DynamicEntity person = jaxbContext.newDynamicEntity(PERSON);
        assertNotNull("Could not create Dynamic Entity.", person);

        person.set("id", 456);
        person.set("name", "Bob Dobbs");

        Document marshalDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        jaxbContext.createMarshaller().marshal(person, marshalDoc);

        // "targetNamespace" should be null by default
        Node node = marshalDoc.getChildNodes().item(0);
        assertNull("Target Namespace was not null as expected.", node.getNamespaceURI());

        // Make sure "elementFormDefault" was interpreted properly.
        // When unset, no namespace qualification is done.
        assertNull("Root node should not have namespace prefix.", node.getPrefix());

        Node attr = node.getAttributes().item(0);
        assertNull("Attribute should not have namespace prefix.", attr.getPrefix());

        Node childNode = node.getChildNodes().item(0);
        assertNull("Child node should not have namespace prefix.", childNode.getPrefix());
    }

    public void testXmlSeeAlso() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(XMLSEEALSO);
        jaxbContext = DynamicJAXBContextFactory.createContextFromXSD(inputStream, nsResolver, null, null);

        DynamicEntity person = jaxbContext.newDynamicEntity(PACKAGE + "." + EMPLOYEE);
        assertNotNull("Could not create Dynamic Entity.", person);

        person.set("name", "Bob Dobbs");
        person.set("employeeId", "CA2472");

        Document marshalDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        jaxbContext.createMarshaller().marshal(person, marshalDoc);

        // Nothing to really test, XmlSeeAlso isn't represented in an instance doc.
    }

    public void testXmlRootElement() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(XMLROOTELEMENT);
        jaxbContext = DynamicJAXBContextFactory.createContextFromXSD(inputStream, nsResolver, null, null);

        DynamicEntity person = jaxbContext.newDynamicEntity(PACKAGE + "." + PERSON);
        assertNotNull("Could not create Dynamic Entity.", person);

        person.set("name", "Bob Dobbs");

        Document marshalDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        jaxbContext.createMarshaller().marshal(person, marshalDoc);

        Node node = marshalDoc.getChildNodes().item(0);

        assertEquals("Root element was not 'individuo' as expected.", "individuo", node.getLocalName());
    }

    public void testXmlType() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(XMLTYPE);
        jaxbContext = DynamicJAXBContextFactory.createContextFromXSD(inputStream, nsResolver, null, null);

        DynamicEntity person = jaxbContext.newDynamicEntity(PACKAGE + "." + PERSON);
        assertNotNull("Could not create Dynamic Entity.", person);

        person.set("email", "bdobbs@subgenius.com");
        person.set("lastName", "Dobbs");
        person.set("id", 678);
        person.set("phoneNumber", "212-555-8282");
        person.set("firstName", "Bob");

        Document marshalDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        jaxbContext.createMarshaller().marshal(person, marshalDoc);

        // Test that XmlType.propOrder was interpreted properly
        Node node = marshalDoc.getDocumentElement().getChildNodes().item(0);
        assertNotNull("Node was null.", node);
        assertEquals("Unexpected node.", "id", node.getLocalName());

        node = marshalDoc.getDocumentElement().getChildNodes().item(1);
        assertNotNull("Node was null.", node);
        assertEquals("Unexpected node.", "first-name", node.getLocalName());

        node = marshalDoc.getDocumentElement().getChildNodes().item(2);
        assertNotNull("Node was null.", node);
        assertEquals("Unexpected node.", "last-name", node.getLocalName());

        node = marshalDoc.getDocumentElement().getChildNodes().item(3);
        assertNotNull("Node was null.", node);
        assertEquals("Unexpected node.", "phone-number", node.getLocalName());

        node = marshalDoc.getDocumentElement().getChildNodes().item(4);
        assertNotNull("Node was null.", node);
        assertEquals("Unexpected node.", "email", node.getLocalName());
    }

    public void testXmlAttribute() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(XMLATTRIBUTE);
        jaxbContext = DynamicJAXBContextFactory.createContextFromXSD(inputStream, nsResolver, null, null);

        DynamicEntity person = jaxbContext.newDynamicEntity(PACKAGE + "." + PERSON);
        assertNotNull("Could not create Dynamic Entity.", person);

        person.set("id", 777);

        Document marshalDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        jaxbContext.createMarshaller().marshal(person, marshalDoc);

        Node node = marshalDoc.getChildNodes().item(0);

        if (node.getAttributes() == null || node.getAttributes().getNamedItem("id") == null) {
            fail("Attribute not present.");
        }
    }

    public void testXmlElement() throws Exception {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(XMLELEMENT);
        jaxbContext = DynamicJAXBContextFactory.createContextFromXSD(inputStream, nsResolver, null, null);

        DynamicEntity person = jaxbContext.newDynamicEntity(PACKAGE + "." + PERSON);
        assertNotNull("Could not create Dynamic Entity.", person);

        person.set("type", "O+");

        Document marshalDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        jaxbContext.createMarshaller().marshal(person, marshalDoc);

        Node node = marshalDoc.getDocumentElement();

        assertNotNull("Element not present.", node.getChildNodes());

        String elemName = node.getChildNodes().item(0).getNodeName();
        assertEquals("Element not present.", "type", elemName);
    }

}