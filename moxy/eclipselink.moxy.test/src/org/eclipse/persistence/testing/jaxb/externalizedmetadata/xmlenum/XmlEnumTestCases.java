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
 * dmccann - January 11/2010 - 2.0 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlenum;

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


/**
 * Tests XmlEnum via eclipselink-oxm.xml
 *
 */
public class XmlEnumTestCases extends ExternalizedMetadataTestCases {
    private static final String CONTEXT_PATH = "org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlenum";
    private static final String PATH = "org/eclipse/persistence/testing/jaxb/externalizedmetadata/xmlenum/";
    
    /**
     * This is the preferred (and only) constructor.
     * 
     * @param name
     */
    public XmlEnumTestCases(String name) {
        super(name);
    }

    /**
     * Tests @XmlEnum schema generation via eclipselink-oxm.xml.
     * 
     * Positive test.
     */
    public void testXmlEnumSchemaGen() {
        String metadataFile = PATH + "eclipselink-oxm-game.xml";
        InputStream iStream = loader.getResourceAsStream(metadataFile);
        if (iStream == null) {
            fail("Couldn't load metadata file [" + metadataFile + "]");
        }

        MySchemaOutputResolver outputResolver = generateSchema(new Class[] { Game.class }, CONTEXT_PATH, iStream, 1);
        // validate schema
        String controlSchema = PATH + "game-schema.xsd";
        compareSchemas(outputResolver.schemaFiles.get(EMPTY_NAMESPACE), new File(controlSchema));
    }
    
    /**
     * Tests @XmlEnum via eclipselink-oxm.xml.
     * 
     * Positive test.
     */
    public void testXmlEnumUnmarshalThenMarshal() {
        // load XML metadata
        String metadataFile = PATH + "eclipselink-oxm-game.xml";
        InputStream iStream = loader.getResourceAsStream(metadataFile);
        if (iStream == null) {
            fail("Couldn't load metadata file [" + metadataFile + "]");
        }

        HashMap<String, Source> metadataSourceMap = new HashMap<String, Source>();
        metadataSourceMap.put(CONTEXT_PATH, new StreamSource(iStream));
        Map<String, Map<String, Source>> properties = new HashMap<String, Map<String, Source>>();
        properties.put(JAXBContextFactory.ECLIPSELINK_OXM_XML_KEY, metadataSourceMap);

        // create context
        JAXBContext jCtx = null;
        try {
            jCtx = (JAXBContext) JAXBContextFactory.createContext(new Class[] { Game.class }, properties);
        } catch (JAXBException e1) {
            e1.printStackTrace();
            fail("JAXBContext creation failed.");
        }
        
        // load instance doc
        String src = PATH + "game.xml";
        InputStream iDocStream = loader.getResourceAsStream(src);
        if (iDocStream == null) {
            fail("Couldn't load instance doc [" + src + "]");
        }

        // setup control objects
        Game gameCtrl = new Game();
        gameCtrl.card = Card.DIAMONDS;
        gameCtrl.coin = Coin.DIME;
        
        // unmarshal
        Game gameObj = null;
        Unmarshaller unmarshaller = jCtx.createUnmarshaller();
        try {
            gameObj = (Game) unmarshaller.unmarshal(iDocStream);
            assertFalse("Unmarshalled object is null.", gameObj == null);
            assertTrue("Unmarshal failed - object is not equal to control Game", gameObj.equals(gameCtrl));
        } catch (javax.xml.bind.JAXBException e) {
            e.printStackTrace();
            fail("Unmarshal operation failed.");
        }

        Document testDoc = parser.newDocument();
        Document ctrlDoc = parser.newDocument();
        try {
            ctrlDoc = getControlDocument(src);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An unexpected exception occurred loading control document [" + src + "].");
        }

        // marshal
        Marshaller marshaller = jCtx.createMarshaller();
        try {
            marshaller.marshal(gameCtrl, testDoc);
            assertTrue("Document comparison failed unxepectedly: ", compareDocuments(ctrlDoc, testDoc));
        } catch (JAXBException e) {
            e.printStackTrace();
            fail("Unmarshal operation failed.");
        }
    }
}
