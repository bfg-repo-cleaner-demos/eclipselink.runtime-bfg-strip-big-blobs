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

package dbws.testing;

//javase imports
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import org.w3c.dom.Document;

//Java extension imports
import javax.wsdl.WSDLException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//JUnit imports
import org.junit.BeforeClass;

//EclipseLink imports
import org.eclipse.persistence.internal.xr.XRServiceAdapter;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLUnmarshaller;
import org.eclipse.persistence.platform.xml.XMLComparer;
import org.eclipse.persistence.platform.xml.XMLParser;
import org.eclipse.persistence.platform.xml.XMLPlatform;
import org.eclipse.persistence.platform.xml.XMLPlatformFactory;
import org.eclipse.persistence.tools.dbws.DBWSBuilder;
import org.eclipse.persistence.tools.dbws.DBWSBuilderModel;
import org.eclipse.persistence.tools.dbws.DBWSBuilderModelProject;
import org.eclipse.persistence.tools.dbws.JarArchiver;
import org.eclipse.persistence.tools.dbws.XRPackager;
import static org.eclipse.persistence.tools.dbws.DBWSPackager.ArchiveUse.ignore;

// domain-specific (testing) imports

public class DBWSTestSuite {

    public final static String DATABASE_USERNAME_KEY = "db.user";
    public final static String DATABASE_PASSWORD_KEY = "db.pwd";
    public final static String DATABASE_URL_KEY = "db.url";
    public final static String DATABASE_DRIVER_KEY = "db.driver";
    public final static String DATABASE_PLATFORM_KEY = "db.platform";
    public final static String DEFAULT_DATABASE_USERNAME = "MNORMAN";
    public final static String DEFAULT_DATABASE_PASSWORD = "password";
    public final static String DEFAULT_DATABASE_URL = "jdbc:mysql://tlsvrdb4.ca.oracle.com/" +
        DEFAULT_DATABASE_USERNAME;
    public final static String DEFAULT_DATABASE_DRIVER = "com.mysql.jdbc.Driver";
    public final static String DEFAULT_DATABASE_PLATFORM =
        "org.eclipse.persistence.platform.database.MySQLPlatform";

    public static XMLComparer comparer = new XMLComparer();
    public static XMLPlatform xmlPlatform = XMLPlatformFactory.getInstance().getXMLPlatform();
    public static XMLParser xmlParser = xmlPlatform.newXMLParser();

    // test fixture(s)
    public static XRServiceAdapter xrService = null;

    public static void buildJar(String builderXMLUserPortion, String builderXMLPasswordPortion,
        String builderXMLUrlPortion, String builderXMLDriverPortion,
        String builderXMLPlatformPortion, String builderXMLMainPortion, String jarName)
        throws IOException, WSDLException {

        String username = System.getProperty(DATABASE_USERNAME_KEY, DEFAULT_DATABASE_USERNAME);
        String password = System.getProperty(DATABASE_PASSWORD_KEY, DEFAULT_DATABASE_PASSWORD);
        String url = System.getProperty(DATABASE_URL_KEY, DEFAULT_DATABASE_URL);
        String driver = System.getProperty(DATABASE_DRIVER_KEY, DEFAULT_DATABASE_DRIVER);
        String platform = System.getProperty(DATABASE_PLATFORM_KEY, DEFAULT_DATABASE_PLATFORM);
        String builderString = builderXMLUserPortion + username + builderXMLPasswordPortion +
        password + builderXMLUrlPortion + url + builderXMLDriverPortion + driver +
        builderXMLPlatformPortion + platform + builderXMLMainPortion;
        buildJar(builderString, jarName);
    }

    public static void buildJar(String builderString, String jarName) throws WSDLException {
        XMLContext context = new XMLContext(new DBWSBuilderModelProject());
        XMLUnmarshaller unmarshaller = context.createUnmarshaller();
        DBWSBuilderModel builderModel =
            (DBWSBuilderModel)unmarshaller.unmarshal(new StringReader(builderString));
        DBWSBuilder dbwsBuilder = new DBWSBuilder();
        dbwsBuilder.quiet = true;
        dbwsBuilder.properties = builderModel.properties;
        dbwsBuilder.operations = builderModel.operations;
        XRPackager xrPackager = new XRPackager();
        xrPackager.setArchiveUse(ignore);
        xrPackager.setArchiver(new JarArchiver(xrPackager));
        xrPackager.setArchiveFilename("dbws" + jarName + ".jar");
        xrPackager.setStageDir(new File("."));
        xrPackager.setSessionsFileName(dbwsBuilder.getSessionsFileName());
        xrPackager.setDBWSBuilder(dbwsBuilder);
        dbwsBuilder.setPackager(xrPackager);
        dbwsBuilder.start();
    }

    @BeforeClass
    public static void setUpDBWSService() {
        TestDBWSFactory serviceFactory = new TestDBWSFactory();
        xrService = serviceFactory.buildService();
    }

    public static String documentToString(Document doc) {
        DOMSource domSource = new DOMSource(doc);
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty("indent", "yes");
            transformer.transform(domSource, result);
            return stringWriter.toString();
        } catch (Exception e) {
            // e.printStackTrace();
            return "<empty/>";
        }
    }
}
