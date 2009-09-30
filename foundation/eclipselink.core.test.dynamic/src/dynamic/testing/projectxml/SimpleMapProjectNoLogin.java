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
 *     dclarke - Dynamic Persistence INCUBATION - Enhancement 200045
 *               http://wiki.eclipse.org/EclipseLink/Development/Dynamic
 *     mnorman - tweaks to work from Ant command-line,
 *               et database properties from System, etc.
 *     
 * This code is being developed under INCUBATION and is not currently included 
 * in the automated EclipseLink build. The API in this code may change, or 
 * may never be included in the product. Please provide feedback through mailing 
 * lists or the bug database.
 ******************************************************************************/
package dynamic.testing.projectxml;

//javase imports
import java.io.IOException;

//JUnit4 imports
import org.junit.BeforeClass;
import static junit.framework.Assert.fail;

//EclipseLink imports
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.dynamic.DynamicHelper;
import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.dynamic.DynamicTypeBuilder;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.tools.schemaframework.DynamicSchemaManager;

//domain-specific (testing) imports
import static dynamic.testing.DynamicTestingHelper.createLogin;
import static dynamic.testing.DynamicTestingHelper.logLevel;

/*
 * Test cases verifying the use of simple-map-project-no-login.xml 
 */
public class SimpleMapProjectNoLogin extends SimpleMapProject {

    static final String PROJECT_XML = "dynamic/testing/simple-map-project-no-login.xml";
    
    @BeforeClass
    public static void setUp() {
        DynamicClassLoader dcl = new DynamicClassLoader(SimpleMapProjectNoLogin.class.getClassLoader());
        Project project = null;
        try {
            project = DynamicTypeBuilder.loadDynamicProject(PROJECT_XML, createLogin(), dcl);
        }
        catch (IOException e) {
            //e.printStackTrace();
            fail("cannot find simple-map-project-no-login.xml");
        }
        session = project.createDatabaseSession();
        if (SessionLog.OFF == logLevel) {
            session.dontLogMessages();
        }
        else {
            session.setLogLevel(logLevel); 
        }
        dynamicHelper = new DynamicHelper(session);
        session.login();
        new DynamicSchemaManager(session).createTables(new DynamicType[0]);
    }

}