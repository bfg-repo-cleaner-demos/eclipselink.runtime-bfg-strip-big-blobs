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

package dbws.testing.crud;

// Javase imports
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

// Java extension classes
import javax.wsdl.WSDLException;

// JUnit imports
import org.junit.Test;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

// EclipseLink imports
import org.eclipse.persistence.internal.xr.BaseEntity;
import org.eclipse.persistence.internal.xr.Invocation;
import org.eclipse.persistence.internal.xr.Operation;
import org.eclipse.persistence.oxm.XMLMarshaller;
import org.eclipse.persistence.oxm.XMLUnmarshaller;

// domain imports
import dbws.testing.DBWSTestSuite;

public class CRUDTestSuite extends DBWSTestSuite {

    public static final String DBWS_BUILDER_XML_USERNAME =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<dbws-builder xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">" +
          "<properties>" +
              "<property name=\"projectName\">crud</property>" +
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
            "tableNamePattern=\"crud_table\" " +
            ">" +
            "<sql " +
              "name=\"findByName\" " +
              "returnType=\"crud_tableType\" " +
              "isCollection=\"true\" " +
              ">" +
              "<text><![CDATA[select * from crud_table where name like 'crud%']]></text>" +
            "</sql>" +
          "</table>" +
        "</dbws-builder>";

    public static void main(String[] args) throws IOException, WSDLException {
        buildJar(DBWS_BUILDER_XML_USERNAME, DBWS_BUILDER_XML_PASSWORD, DBWS_BUILDER_XML_URL,
            DBWS_BUILDER_XML_DRIVER, DBWS_BUILDER_XML_PLATFORM, DBWS_BUILDER_XML_MAIN, args[0]);
    }

    // hokey naming convention for test methods to assure order-of-operations
    // w.r.t. insert/update/delete

    @Test
    public void test1_readOne() {
        Invocation invocation = new Invocation("findByPrimaryKey_crud_tableType");
        invocation.setParameter("id", 1);
        Operation op = xrService.getOperation(invocation.getName());
        Object result = op.invoke(xrService, invocation);
        assertNotNull("result is null", result);
        XMLMarshaller marshaller = xrService.getXMLContext().createMarshaller();
        Document doc = xmlPlatform.createDocument();
        marshaller.marshal(result, doc);
        Document controlDoc = xmlParser.parse(new StringReader(CRUD1_CONTROL_DOC));
        assertTrue("control document not same as instance document",
            comparer.isNodeEqual(controlDoc, doc));
    }
    public static final String CRUD1_CONTROL_DOC =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<ns1:crud_tableType xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:ns1=\"urn:crud\"" +
                                       " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
          "<ns1:id>1</ns1:id>" +
          "<ns1:name>crud1</ns1:name>" +
        "</ns1:crud_tableType>";

    @SuppressWarnings("unchecked")
    @Test
    public void test2_readAll() {
        Invocation invocation = new Invocation("findAll_crud_tableType");
        Operation op = xrService.getOperation(invocation.getName());
        Object result = op.invoke(xrService, invocation);
        assertNotNull("result is null", result);
        XMLMarshaller marshaller = xrService.getXMLContext().createMarshaller();
        Document doc = xmlPlatform.createDocument();
        Element ec = doc.createElement("all");
        doc.appendChild(ec);
        for (Object r : (Vector)result) {
            marshaller.marshal(r, ec);
        }
        Document controlDoc = xmlParser.parse(new StringReader(FIND_ALL_CONTROL_DOC));
        assertTrue("control document not same as instance document",
            comparer.isNodeEqual(controlDoc, doc));
    }
    public static final String FIND_ALL_CONTROL_DOC =
    	"<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
    	"<all>" +
    	  "<ns1:crud_tableType xmlns:ns1=\"urn:crud\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
    	    "<ns1:id>1</ns1:id>" +
    	    "<ns1:name>crud1</ns1:name>" +
    	  "</ns1:crud_tableType>" +
    	  "<ns1:crud_tableType xmlns:ns1=\"urn:crud\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
    	    "<ns1:id>2</ns1:id>" +
    	    "<ns1:name>crud2</ns1:name>" +
    	  "</ns1:crud_tableType>" +
    	  "<ns1:crud_tableType xmlns:ns1=\"urn:crud\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
    	    "<ns1:id>3</ns1:id>" +
    	    "<ns1:name>other</ns1:name>" +
    	  "</ns1:crud_tableType>" +
    	"</all>";

    @SuppressWarnings("unchecked")
    @Test
    public void test3_findByName() {
        Invocation invocation = new Invocation("findByName");
        Operation op = xrService.getOperation(invocation.getName());
        Object result = op.invoke(xrService, invocation);
        assertNotNull("result is null", result);
        XMLMarshaller marshaller = xrService.getXMLContext().createMarshaller();
        Document doc = xmlPlatform.createDocument();
        Element ec = doc.createElement("some");
        doc.appendChild(ec);
        for (Object r : (Vector)result) {
            marshaller.marshal(r, ec);
        }
        Document controlDoc = xmlParser.parse(new StringReader(FIND_BY_NAME_CONTROL_DOC));
        assertTrue("control document not same as instance document",
            comparer.isNodeEqual(controlDoc, doc));
    }
    public static final String FIND_BY_NAME_CONTROL_DOC =
    	"<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
    	"<some>" +
    	  "<ns1:crud_tableType xmlns:ns1=\"urn:crud\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
    	    "<ns1:id>1</ns1:id>" +
    	    "<ns1:name>crud1</ns1:name>" +
    	  "</ns1:crud_tableType>" +
    	  "<ns1:crud_tableType xmlns:ns1=\"urn:crud\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
    	    "<ns1:id>2</ns1:id>" +
    	    "<ns1:name>crud2</ns1:name>" +
    	  "</ns1:crud_tableType>" +
    	"</some>";

    @Test
    public void test4_update() {
        XMLUnmarshaller unMarshaller = xrService.getXMLContext().createUnmarshaller();
        Reader reader = new StringReader(CRUD1_CONTROL_DOC);
        InputSource inputSource = new InputSource(reader);
        BaseEntity firstEmp = (BaseEntity)unMarshaller.unmarshal(inputSource);
        firstEmp.set(1, "some other name");
        Invocation invocation = new Invocation("update_crud_tableType");
        invocation.setParameter("theInstance", firstEmp);
        Operation op = xrService.getOperation(invocation.getName());
        op.invoke(xrService, invocation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test5_delete() {
        Invocation invocation = new Invocation("findAll_crud_tableType");
        Operation op = xrService.getOperation(invocation.getName());
        Vector<BaseEntity> result = (Vector<BaseEntity>)op.invoke(xrService, invocation);
        BaseEntity firstEmp = result.firstElement();
        Invocation invocation2 = new Invocation("delete_crud_tableType");
        invocation2.setParameter("theInstance", firstEmp);
        Operation op2 = xrService.getOperation(invocation2.getName());
        op2.invoke(xrService, invocation2);
        Vector<BaseEntity> result2 = (Vector<BaseEntity>)op.invoke(xrService, invocation);
        assertTrue("Wrong number of employees", result2.size() == 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test6_create() {
        XMLUnmarshaller unMarshaller = xrService.getXMLContext().createUnmarshaller();
        Reader reader = new StringReader(CRUD1_CONTROL_DOC);
        InputSource inputSource = new InputSource(reader);
        BaseEntity anotherEmployee = (BaseEntity)unMarshaller.unmarshal(inputSource);
        Invocation invocation = new Invocation("create_crud_tableType");
        invocation.setParameter("theInstance", anotherEmployee);
        Operation op = xrService.getOperation(invocation.getName());
        op.invoke(xrService, invocation);
        Invocation invocation2 = new Invocation("findAll_crud_tableType");
        Operation op2 = xrService.getOperation(invocation2.getName());
        Vector<BaseEntity> result2 = (Vector<BaseEntity>)op2.invoke(xrService, invocation2);
        assertTrue("Wrong number of employees", result2.size() == 3);
    }
}