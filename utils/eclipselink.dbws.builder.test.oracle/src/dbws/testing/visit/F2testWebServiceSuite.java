/*******************************************************************************
 * Copyright (c) 2011, 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * dmccann - 2.3 - Initial implementation
 ******************************************************************************/
package dbws.testing.visit;

//javase imports
import java.io.IOException;
import java.io.StringReader;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

//java eXtension imports
import javax.annotation.PostConstruct;
import javax.wsdl.WSDLException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;
import static javax.xml.ws.Service.Mode.MESSAGE;

//JUnit4 imports
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

//EclipseLink imports
import org.eclipse.persistence.tools.dbws.PLSQLProcedureOperationModel;

//test imports
import static dbws.testing.visit.DBWSTestHelper.PACKAGE_NAME;
import static dbws.testing.visit.DBWSTestHelper.FUNC2;
import static dbws.testing.visit.DBWSTestHelper.FUNC2_NAMESPACE;
import static dbws.testing.visit.DBWSTestHelper.FUNC2_SERVICE_NAMESPACE;
import static dbws.testing.visit.DBWSTestHelper.FUNC2_PORT;
import static dbws.testing.visit.DBWSTestHelper.FUNC2_SERVICE;
import static dbws.testing.visit.DBWSTestHelper.FUNC2_TEST;

@WebServiceProvider(
    targetNamespace = FUNC2_SERVICE_NAMESPACE,
    serviceName = FUNC2_SERVICE,
    portName = FUNC2_PORT
)
@ServiceMode(MESSAGE)
public class F2testWebServiceSuite extends WebServiceTestSuite implements Provider<SOAPMessage> {

    static final String ENDPOINT_ADDRESS = "http://localhost:9999/" + FUNC2_TEST;

    @BeforeClass
    public static void setUp() throws WSDLException {
        builder.setProjectName(FUNC2_TEST);
        builder.setTargetNamespace(FUNC2_NAMESPACE);
        PLSQLProcedureOperationModel p5Model = new PLSQLProcedureOperationModel();
        p5Model.setName(FUNC2_TEST);
        builder.getSchema().getNamespaceResolver().put("ns1", FUNC2_NAMESPACE);
        p5Model.setReturnType("ns1:SOMEPACKAGE_TBL2");
        p5Model.setCatalogPattern(PACKAGE_NAME);
        p5Model.setProcedurePattern(FUNC2);
        builder.getOperations().add(p5Model);
        serviceSetup(ENDPOINT_ADDRESS, new F2testWebServiceSuite());
    }

    @PostConstruct
    public void init() {
        super.init();
    }

    static final String REQUEST_MSG =
        "<env:Envelope xmlns:env=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
          "<env:Header/>" +
          "<env:Body>" +
            "<srvc:" + FUNC2_TEST + " xmlns:srvc=\"" + FUNC2_SERVICE_NAMESPACE + "\" xmlns:ns1=\"" + FUNC2_NAMESPACE + "\">" +
              "<srvc:OLD>" +
                "<ns1:item>3000.00</ns1:item>" +
                "<ns1:item>4050.01</ns1:item>" +
                "<ns1:item>6000.07</ns1:item>" +
              "</srvc:OLD>" +
              "<srvc:SIMPLARRAY>" +
                "<ns1:item>" + "this" + "</ns1:item>" +
                "<ns1:item>" + "is" + "</ns1:item>" +
                "<ns1:item>" + "a" + "</ns1:item>" +
            "</srvc:SIMPLARRAY>" +
            "</srvc:" + FUNC2_TEST + ">" +
          "</env:Body>" +
        "</env:Envelope>";

    @Test
    public void f2Test() throws SOAPException, IOException, SAXException,
        ParserConfigurationException, TransformerException {
        MessageFactory factory = MessageFactory.newInstance();
        SOAPMessage request = factory.createMessage();
        SOAPPart part = request.getSOAPPart();
        DOMSource domSource = new DOMSource(
            getDocumentBuilder().parse(new InputSource(new StringReader(REQUEST_MSG))));
        part.setContent(domSource);
        Dispatch<SOAPMessage> dispatch = testService.createDispatch(portQName, SOAPMessage.class,
            Service.Mode.MESSAGE);
        SOAPMessage response = null;
        try {
            response = dispatch.invoke(request);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (response != null) {
            Source src = response.getSOAPPart().getContent();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            DOMResult result = new DOMResult();
            transformer.transform(src, result);
            Document resultDoc = (Document)result.getNode();
            Document controlDoc = xmlParser.parse(new StringReader(TEST_RESPONSE));
            assertTrue("control document not same as instance document",
                comparer.isNodeEqual(controlDoc, resultDoc));
        } else {
            fail("No response was returned");
        }
    }

    static final String TEST_RESPONSE =
        "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
          "<SOAP-ENV:Header/>" +
          "<SOAP-ENV:Body>" +
            "<srvc:" + FUNC2_TEST + "Response xmlns=\"" + FUNC2_NAMESPACE + "\" xmlns:srvc=\"" + FUNC2_SERVICE_NAMESPACE + "\">" +
              "<srvc:result>" +
                "<SOMEPACKAGE_TBL2>" +
                    "<item>3000</item>" +
                    "<item>4050.01</item>" +
                    "<item>6000.07</item>" +
                "</SOMEPACKAGE_TBL2>" +
              "</srvc:result>" +
            "</srvc:" + FUNC2_TEST + "Response>" +
          "</SOAP-ENV:Body>" +
        "</SOAP-ENV:Envelope>";
}