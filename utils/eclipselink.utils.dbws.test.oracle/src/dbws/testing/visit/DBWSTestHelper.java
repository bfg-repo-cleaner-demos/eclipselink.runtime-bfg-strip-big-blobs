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

package dbws.testing.visit;

//javase imports
import java.io.StringWriter;
import org.w3c.dom.Document;

//java eXtension imports
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//EclipseLink imports
import static org.eclipse.persistence.internal.dynamicpersist.BaseEntityClassLoader.COLLECTION_WRAPPER_SUFFIX;

public class DBWSTestHelper {

    public static final String PACKAGE_NAME = "SOMEPACKAGE";

    public static final String PROC1 = "p1";
    public static final String PROC1_TEST = PROC1 + "Test";
    public static final String PROC1_SERVICE = PROC1_TEST + "Service";
    public static final String PROC1_NAMESPACE = "urn:" + PROC1_TEST;
    public static final String PROC1_SERVICE_NAMESPACE = "urn:" + PROC1_SERVICE;
    public static final String PROC1_PORT = PROC1_SERVICE + "Port";

    public static final String PROC2 = "p2";
    public static final String PROC2_TEST = PROC2 + "Test";
    public static final String PROC2_SERVICE = PROC2_TEST + "Service";
    public static final String PROC2_NAMESPACE = "urn:" + PROC2_TEST;
    public static final String PROC2_SERVICE_NAMESPACE = "urn:" + PROC2_SERVICE;
    public static final String PROC2_PORT = PROC2_SERVICE + "Port";

    public static final String PROC3 = "p3";
    public static final String PROC3_TEST = PROC3 + "Test";
    public static final String PROC3_SERVICE = PROC3_TEST + "Service";
    public static final String PROC3_NAMESPACE = "urn:" + PROC3_TEST;
    public static final String PROC3_SERVICE_NAMESPACE = "urn:" + PROC3_SERVICE;
    public static final String PROC3_PORT = PROC3_SERVICE + "Port";

    public static final String PROC4 = "p4";
    public static final String PROC4_TEST = PROC4 + "Test";
    public static final String PROC4_SERVICE = PROC4_TEST + "Service";
    public static final String PROC4_NAMESPACE = "urn:" + PROC4_TEST;
    public static final String PROC4_SERVICE_NAMESPACE = "urn:" + PROC4_SERVICE;
    public static final String PROC4_PORT = PROC4_SERVICE + "Port";

    public static final String PROC5 = "p5";
    public static final String PROC5_TEST = PROC5 + "Test";
    public static final String PROC5_SERVICE = PROC5_TEST + "Service";
    public static final String PROC5_NAMESPACE = "urn:" + PROC5_TEST;
    public static final String PROC5_SERVICE_NAMESPACE = "urn:" + PROC5_SERVICE;
    public static final String PROC5_PORT = PROC5_SERVICE + "Port";

    public static final String PROC6 = "p6";
    public static final String PROC6_TEST = PROC6 + "Test";
    public static final String PROC6_SERVICE = PROC6_TEST + "Service";
    public static final String PROC6_NAMESPACE = "urn:" + PROC6_TEST;
    public static final String PROC6_SERVICE_NAMESPACE = "urn:" + PROC6_SERVICE;
    public static final String PROC6_PORT = PROC6_SERVICE + "Port";

    public static final String PROC7 = "p7";
    public static final String PROC7_TEST = PROC7 + "Test";
    public static final String PROC7_SERVICE = PROC7_TEST + "Service";
    public static final String PROC7_NAMESPACE = "urn:" + PROC7_TEST;
    public static final String PROC7_SERVICE_NAMESPACE = "urn:" + PROC7_SERVICE;
    public static final String PROC7_PORT = PROC7_SERVICE + "Port";

    public static final String TBL1_COMPATIBLETYPE = "SOMEPACKAGE_TBL1";
    public static final String TBL1_DATABASETYPE = "SOMEPACKAGE.TBL1";
    public static final String TBL1_DESCRIPTOR_ALIAS = TBL1_COMPATIBLETYPE.toLowerCase();
    public static final String TBL1_DESCRIPTOR_JAVACLASSNAME = TBL1_DATABASETYPE.toLowerCase() +
        COLLECTION_WRAPPER_SUFFIX;

    public static final String TBL2_COMPATIBLETYPE = "SOMEPACKAGE_TBL2";
    public static final String TBL2_DATABASETYPE = "SOMEPACKAGE.TBL2";
    public static final String TBL2_DESCRIPTOR_ALIAS = TBL2_COMPATIBLETYPE.toLowerCase();
    public static final String TBL2_DESCRIPTOR_JAVACLASSNAME = TBL2_DATABASETYPE.toLowerCase() +
        COLLECTION_WRAPPER_SUFFIX;

    public static final String TBL3_COMPATIBLETYPE = "SOMEPACKAGE_TBL3";
    public static final String TBL3_DATABASETYPE = "SOMEPACKAGE.TBL3";
    public static final String TBL3_DESCRIPTOR_ALIAS = TBL3_COMPATIBLETYPE.toLowerCase();
    public static final String TBL3_DESCRIPTOR_JAVACLASSNAME = TBL3_DATABASETYPE.toLowerCase() +
        COLLECTION_WRAPPER_SUFFIX;

    public static final String TBL4_COMPATIBLETYPE = "SOMEPACKAGE_TBL4";
    public static final String TBL4_DATABASETYPE = "SOMEPACKAGE.TBL4";
    public static final String TBL4_DESCRIPTOR_ALIAS = TBL4_COMPATIBLETYPE.toLowerCase();
    public static final String TBL4_DESCRIPTOR_JAVACLASSNAME = TBL4_DATABASETYPE.toLowerCase() +
        COLLECTION_WRAPPER_SUFFIX;

    public static final String ARECORD_COMPATIBLETYPE = "SOMEPACKAGE_ARECORD";
    public static final String ARECORD_DATABASETYPE = "SOMEPACKAGE.ARECORD";
    public static final String ARECORD_DESCRIPTOR_ALIAS = ARECORD_COMPATIBLETYPE.toLowerCase();
    public static final String ARECORD_DESCRIPTOR_JAVACLASSNAME = ARECORD_DATABASETYPE.toLowerCase();

    public static final String BRECORD_COMPATIBLETYPE = "SOMEPACKAGE_BRECORD";
    public static final String BRECORD_DATABASETYPE = "SOMEPACKAGE.BRECORD";
    public static final String BRECORD_DESCRIPTOR_ALIAS = BRECORD_COMPATIBLETYPE.toLowerCase();
    public static final String BRECORD_DESCRIPTOR_JAVACLASSNAME = BRECORD_DATABASETYPE.toLowerCase();

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