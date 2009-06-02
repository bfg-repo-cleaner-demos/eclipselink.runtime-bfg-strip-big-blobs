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
* dmccann - May 21/2009 - 2.0 - Initial implementation
******************************************************************************/
package org.eclipse.persistence.testing.jaxb.schemagen.customizedmapping.xmlelementwrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.persistence.jaxb.JAXBContext;
import org.eclipse.persistence.oxm.XMLConstants;

import junit.framework.TestCase;

public class SchemaGenXmlElementWrapperTestCases  extends TestCase {
    static String tmpdir;

    public SchemaGenXmlElementWrapperTestCases(String name) throws Exception {
        super(name);
        tmpdir = (System.getenv("T_WORK") == null ? "" : (System.getenv("T_WORK") + "/"));
    }
    
    public void testElementWrapper() {
        MySchemaOutputResolver outputResolver = new MySchemaOutputResolver();
        try {
            Class[] classes = new Class[]{ MyClassThree.class }; 
            JAXBContext context = (org.eclipse.persistence.jaxb.JAXBContext) org.eclipse.persistence.jaxb.JAXBContextFactory.createContext(classes, null);
            context.generateSchema(outputResolver);
        } catch (Exception ex) {
            fail("Schema generation failed unexpectedly: " + ex.toString());
        }

        assertTrue("No schemas were generated", outputResolver.schemaFiles.size() > 0);
        assertTrue("More than one shcema was generated unxepectedly", outputResolver.schemaFiles.size() == 1);

        try {
            SchemaFactory sFact = SchemaFactory.newInstance(XMLConstants.SCHEMA_URL);
            Schema theSchema = sFact.newSchema(outputResolver.schemaFiles.get(0));
            Validator validator = theSchema.newValidator();
            String src = "org/eclipse/persistence/testing/jaxb/schemagen/customizedmapping/xmlelementwrapper/root3.xml";
            StreamSource ss = new StreamSource(new File(src)); 
            validator.validate(ss);
        } catch (Exception ex) {
            fail("Schema validation failed unexpectedly: " + ex.toString());
        }
    }

    /**
     * If the element wrapper has a namespace that is not ##default and not the target
     * namespace an element reference should generated 
     */
    public void testElementWrapperRef() {
        MySchemaOutputResolver outputResolver = new MySchemaOutputResolver();
        SchemaFactory sFact = SchemaFactory.newInstance(XMLConstants.SCHEMA_URL);
        Schema theSchema;
        Validator validator;
        String src = "org/eclipse/persistence/testing/jaxb/schemagen/customizedmapping/xmlelementwrapper/root.xml";
        StreamSource ss = new StreamSource(new File(src)); 
        try {
            Class[] classes = new Class[]{ MyClassOne.class, MyClassTwo.class }; 
            JAXBContext context = (org.eclipse.persistence.jaxb.JAXBContext) org.eclipse.persistence.jaxb.JAXBContextFactory.createContext(classes, null);
            context.generateSchema(outputResolver);
        } catch (Exception ex) {
            fail("Schema generation failed unexpectedly: " + ex.toString());
        }

        assertTrue("No schemas were generated", outputResolver.schemaFiles.size() > 0);
        assertTrue("More than two schemas were generated unxepectedly", outputResolver.schemaFiles.size() == 2);
        
        try {
            theSchema = sFact.newSchema(outputResolver.schemaFiles.get(0));
            validator = theSchema.newValidator();
            validator.validate(ss);
        } catch (Exception ex) {
            // validation may have failed due to map ordering differences between VMs
            try {
                theSchema = sFact.newSchema(outputResolver.schemaFiles.get(1));
                validator = theSchema.newValidator();
                validator.validate(ss);
            } catch (Exception x) {
                fail("Schema validation failed unexpectedly: " + ex.toString() + ";" + x.toString());
            }
        }
    }
    
    /**
     * Exception case - @XmlElementWrapper must be on collection property
     */
    public void testInvalidElementWrapper() {
        MySchemaOutputResolver outputResolver = new MySchemaOutputResolver();
        boolean exception = false;
        try {
            Class[] classes = new Class[]{ MyInvalidClass.class }; 
            JAXBContext context = (org.eclipse.persistence.jaxb.JAXBContext) org.eclipse.persistence.jaxb.JAXBContextFactory.createContext(classes, null);
            context.generateSchema(outputResolver);
        } catch (Exception ex) {
            exception = true;
        }
        assertTrue("An error did not occur as expected", exception);
    }

    class MySchemaOutputResolver extends SchemaOutputResolver {
        // keep a list of processed schemas for the validation phase of the test(s)
        public List<File> schemaFiles;
        
        public MySchemaOutputResolver() {
            schemaFiles = new ArrayList<File>();
        }
        
        public Result createOutput(String namespaceURI, String suggestedFileName) throws IOException {
            File schemaFile = new File(tmpdir + suggestedFileName);
            schemaFiles.add(schemaFile);
            return new StreamResult(schemaFile);
        }
    }
}
