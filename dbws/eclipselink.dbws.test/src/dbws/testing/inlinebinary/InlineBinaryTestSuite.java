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
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

//EclipseLink imports
import org.eclipse.persistence.internal.xr.Invocation;
import org.eclipse.persistence.internal.xr.Operation;
import org.eclipse.persistence.internal.xr.XRServiceAdapter;
import org.eclipse.persistence.oxm.XMLMarshaller;

//domain-specific imports
import dbws.testing.TestDBWSFactory;
import static dbws.testing.TestDBWSFactory.buildJar;
import static dbws.testing.TestDBWSFactory.comparer;
import static dbws.testing.TestDBWSFactory.DATABASE_DRIVER_KEY;
import static dbws.testing.TestDBWSFactory.DATABASE_PASSWORD_KEY;
import static dbws.testing.TestDBWSFactory.DATABASE_PLATFORM_KEY;
import static dbws.testing.TestDBWSFactory.DATABASE_URL_KEY;
import static dbws.testing.TestDBWSFactory.DATABASE_USERNAME_KEY;
import static dbws.testing.TestDBWSFactory.DEFAULT_DATABASE_DRIVER;
import static dbws.testing.TestDBWSFactory.DEFAULT_DATABASE_PASSWORD;
import static dbws.testing.TestDBWSFactory.DEFAULT_DATABASE_PLATFORM;
import static dbws.testing.TestDBWSFactory.DEFAULT_DATABASE_URL;
import static dbws.testing.TestDBWSFactory.DEFAULT_DATABASE_USERNAME;
import static dbws.testing.TestDBWSFactory.xmlParser;
import static dbws.testing.TestDBWSFactory.xmlPlatform;

public class InlineBinaryTestSuite {

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
         "tableNamePattern=\"inlinebinary\" " +
       "/>" +
     "</dbws-builder>";

    public static void main(String[] args) throws IOException, WSDLException {
		String username = System.getProperty(DATABASE_USERNAME_KEY, DEFAULT_DATABASE_USERNAME);
		String password = System.getProperty(DATABASE_PASSWORD_KEY, DEFAULT_DATABASE_PASSWORD);
		String url = System.getProperty(DATABASE_URL_KEY, DEFAULT_DATABASE_URL);
		String driver = System.getProperty(DATABASE_DRIVER_KEY, DEFAULT_DATABASE_DRIVER);
		String platform = System.getProperty(DATABASE_PLATFORM_KEY, DEFAULT_DATABASE_PLATFORM);
		
		String builderString = DBWS_BUILDER_XML_USERNAME + username + DBWS_BUILDER_XML_PASSWORD +
		password + DBWS_BUILDER_XML_URL + url + DBWS_BUILDER_XML_DRIVER + driver +
		DBWS_BUILDER_XML_PLATFORM + platform + DBWS_BUILDER_XML_MAIN;
		
		buildJar(builderString, "InlineBinary");
	}

	// test fixture(s)
    static XRServiceAdapter xrService = null;
    @BeforeClass
    public static void setUpDBWSService() {
    	TestDBWSFactory serviceFactory = new TestDBWSFactory();
    	xrService = serviceFactory.buildService();
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