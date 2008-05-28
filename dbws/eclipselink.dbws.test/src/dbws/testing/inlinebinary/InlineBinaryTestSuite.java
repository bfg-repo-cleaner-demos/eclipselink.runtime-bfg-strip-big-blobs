/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Mike Norman - May 2008, created DBWS test package
 ******************************************************************************/

package dbws.testing.inlinebinary;

//Javase imports
import java.io.IOException;
import java.io.StringReader;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

//Java extension imports
import javax.wsdl.WSDLException;

//JUnit imports
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

//EclipseLink imports
import org.eclipse.persistence.internal.xr.Invocation;
import org.eclipse.persistence.internal.xr.Operation;
import org.eclipse.persistence.oxm.XMLMarshaller;

//domain-specific imports
import dbws.testing.DBWSTestSuite;

public class InlineBinaryTestSuite extends DBWSTestSuite {

    public static final String DBWS_BUILDER_XML_USERNAME =
     "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
     "<dbws-builder xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">" +
       "<properties>" +
           "<property name=\"projectName\">inlinebinary</property>" +
           "<property name=\"logLevel\">off</property>" +
           "<property name=\"username\">";
    public static final String DBWS_BUILDER_XML_PASSWORD =
           "</property><property name=\"password\">";
    public static final String DBWS_BUILDER_XML_URL =
           "</property><property name=\"url\">";
    public static final String DBWS_BUILDER_XML_DRIVER =
           "</property><property name=\"driver\">";
    public static final String DBWS_BUILDER_XML_PLATFORM =
           "</property><property name=\"platformClassname\">";
    public static final String DBWS_BUILDER_XML_MAIN =
           "</property>" +
       "</properties>" +
       "<table " +
          "schemaPattern=\"%\" " +
         "tableNamePattern=\"inlinebinary\" " +
       "/>" +
     "</dbws-builder>";

    public static void main(String[] args) throws IOException, WSDLException {
		buildJar(DBWS_BUILDER_XML_USERNAME, DBWS_BUILDER_XML_PASSWORD, DBWS_BUILDER_XML_URL,
		    DBWS_BUILDER_XML_DRIVER, DBWS_BUILDER_XML_PLATFORM, DBWS_BUILDER_XML_MAIN, args[0]);
	}

    @SuppressWarnings("unchecked")
	@Test
    public void findAll() {
        Invocation invocation = new Invocation("findAll_inlinebinary");
        Operation op = xrService.getOperation(invocation.getName());
        Object result = op.invoke(xrService, invocation);
        assertNotNull("result is null", result);
        XMLMarshaller marshaller = xrService.getXMLContext().createMarshaller();
        Document doc = xmlPlatform.createDocument();
        Element ec = doc.createElement("inlinebinary-collection");
        doc.appendChild(ec);
        for (Object r : (Vector)result) {
            marshaller.marshal(r, ec);
        }
        Document controlDoc = xmlParser.parse(new StringReader(INLINEBINARY_COLLECTION_XML));
        assertTrue("control document not same as instance document", 
            comparer.isNodeEqual(controlDoc, doc));
    }
    public static final String INLINEBINARY_COLLECTION_XML =
    	"<?xml version = '1.0' encoding = 'UTF-8'?>" +
    	"<inlinebinary-collection>" +
			"<ns1:inlinebinary xmlns:ns1=\"urn:inlinebinary\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"<ns1:id>1</ns1:id>" +
				"<ns1:name>one</ns1:name>" +
				"<ns1:b xsi:type=\"xsd:base64Binary\">rO0ABXVyAAJbQqzzF/gGCFTgAgAAeHAAAAAPAQEBAQEBAQEBAQEBAQEB</ns1:b>" +
			"</ns1:inlinebinary>" +
			"<ns1:inlinebinary xmlns:ns1=\"urn:inlinebinary\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"<ns1:id>2</ns1:id>" +
				"<ns1:name>two</ns1:name>" +
				"<ns1:b xsi:type=\"xsd:base64Binary\">rO0ABXVyAAJbQqzzF/gGCFTgAgAAeHAAAAAPAgICAgICAgICAgICAgIC</ns1:b>" +
			"</ns1:inlinebinary>" +
			"<ns1:inlinebinary xmlns:ns1=\"urn:inlinebinary\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"<ns1:id>3</ns1:id>" +
				"<ns1:name>three</ns1:name>" +
				"<ns1:b xsi:type=\"xsd:base64Binary\">rO0ABXVyAAJbQqzzF/gGCFTgAgAAeHAAAAAPAwMDAwMDAwMDAwMDAwMD</ns1:b>" +
			"</ns1:inlinebinary>" +
		"</inlinebinary-collection>";
}