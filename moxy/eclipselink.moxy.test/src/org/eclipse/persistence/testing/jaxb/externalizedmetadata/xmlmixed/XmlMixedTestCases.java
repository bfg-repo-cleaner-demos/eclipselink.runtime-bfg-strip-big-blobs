/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * dmccann - November 04/2009 - 2.0 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlmixed;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.persistence.jaxb.JAXBContext;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.ExternalizedMetadataTestCases;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests XmlMixed via eclipselink-oxm.xml
 *
 */
public class XmlMixedTestCases extends ExternalizedMetadataTestCases {
    private boolean shouldGenerateSchema = true;
    private MySchemaOutputResolver outputResolver; 
    private static final String CONTEXT_PATH = "org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlmixed";
    private static final String PATH = "org/eclipse/persistence/testing/jaxb/externalizedmetadata/xmlmixed/";
    
    /**
     * This is the preferred (and only) constructor.
     * 
     * @param name
     */
    public XmlMixedTestCases(String name) {
        super(name);
        outputResolver = new MySchemaOutputResolver();
    }

    private void doSchemaGeneration() {
        if (shouldGenerateSchema) {
            outputResolver = generateSchema(CONTEXT_PATH, PATH, 1);
            // validate schema
            String controlSchema = PATH + "schema.xsd";
            compareSchemas(outputResolver.schemaFiles.get(EMPTY_NAMESPACE), new File(controlSchema));
            shouldGenerateSchema = false;
        }
    }    
    
    /**
     * Tests @XmlMixed override via eclipselink-oxm.xml.  
     * 
     * Positive test.
     */
    public void testXmlMixed() {
        doSchemaGeneration();
        String src = PATH + "employee.xml";
        String result = validateAgainstSchema(src, EMPTY_NAMESPACE, outputResolver);
        assertTrue("Schema validation failed unxepectedly: " + result, result == null);
    }
    
    /**
     * 
     * Positive test.
     */
    public void testXmlMixedUnmarshal() {
        String metadataFile = PATH + "eclipselink-oxm.xml";
        InputStream iStream = loader.getResourceAsStream(metadataFile);
        if (iStream == null) {
            fail("Couldn't load metadata file [" + metadataFile + "]");
        }
        HashMap<String, Source> metadataSourceMap = new HashMap<String, Source>();
        metadataSourceMap.put(CONTEXT_PATH, new StreamSource(iStream));
        Map<String, Map<String, Source>> properties = new HashMap<String, Map<String, Source>>();
        properties.put(JAXBContextFactory.ECLIPSELINK_OXM_XML_KEY, metadataSourceMap);

        JAXBContext jaxbContext = null;
        try {
            jaxbContext = (JAXBContext) JAXBContextFactory.createContext(new Class[] { Employee.class }, properties);
        } catch (JAXBException e1) {
            e1.printStackTrace();
            fail("JAXBContext creation failed");
        }

        // test unmarshal
        Employee emp = null;
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        try {
            String src = PATH + "employee.xml";
            emp = (Employee) unmarshaller.unmarshal(getControlDocument(src));
            assertNotNull("The Employee object is null after unmarshal.", emp);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An unexpected exception occurred unmarshalling the document.");
        }

        assertNotNull("The Employee did not umnmarshal correctly: 'stuff' is null.", emp.stuff);
        assertTrue("The Employee did not umnmarshal correctly: expected 'stuff' size of [3] but was [" + emp.stuff.size() + "]", emp.stuff.size() == 3);
        assertTrue("The Employee did not umnmarshal correctly: expected 'stuff.0' to be instanceof [String] but was [" + emp.stuff.get(0) + "]", emp.stuff.get(0) instanceof String);
        assertTrue("The Employee did not umnmarshal correctly: expected 'stuff.1' to be instanceof [Element] but was [" + emp.stuff.get(1) + "]", emp.stuff.get(1) instanceof Element);
        assertTrue("The Employee did not umnmarshal correctly: expected 'stuff.2' to be instanceof [String] but was [" + emp.stuff.get(2) + "]", emp.stuff.get(2) instanceof String);
    }
}
