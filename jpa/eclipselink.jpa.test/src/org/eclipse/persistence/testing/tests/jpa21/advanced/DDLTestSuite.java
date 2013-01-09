/*******************************************************************************
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     11/22/2012-2.5 Guy Pelletier 
 *       - 389090: JPA 2.1 DDL Generation Support (index metadata support)
 *     12/24/2012-2.5 Guy Pelletier 
 *       - 389090: JPA 2.1 DDL Generation Support
 *     01/08/2013-2.5 Guy Pelletier 
 *       - 389090: JPA 2.1 DDL Generation Support
 *     01/11/2013-2.5 Guy Pelletier 
 *       - 389090: JPA 2.1 DDL Generation Support
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.jpa21.advanced;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.sessions.server.ServerSession;

import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;
import org.eclipse.persistence.testing.framework.junit.JUnitTestCaseHelper;
import org.eclipse.persistence.testing.models.jpa21.advanced.xml.ddl.Organizer;
import org.eclipse.persistence.testing.models.jpa21.advanced.xml.ddl.Race;
import org.eclipse.persistence.testing.models.jpa21.advanced.xml.ddl.Responsibility;
import org.eclipse.persistence.testing.models.jpa21.advanced.xml.ddl.Runner;
import org.eclipse.persistence.testing.models.jpa21.advanced.xml.ddl.RunnerInfo;
import org.eclipse.persistence.testing.models.jpa21.advanced.xml.ddl.RunnerStatus;
import org.eclipse.persistence.testing.models.jpa21.advanced.xml.ddl.Sprinter;
import org.eclipse.persistence.testing.models.jpa21.advanced.enums.Health;
import org.eclipse.persistence.testing.models.jpa21.advanced.enums.Level;
import org.eclipse.persistence.testing.models.jpa21.advanced.enums.RunningStatus;

import junit.framework.TestSuite;
import junit.framework.Test;

public class DDLTestSuite extends JUnitTestCase {
    protected static final String GENERATE_SCHEMA_USE_CONNECTION_PU = "generate-schema-use-connection";
    protected static final String GENERATE_SCHEMA_USE_CONNECTION_DROP_TARGET = "generate-schema-use-connection-drop.jdbc";
    protected static final String GENERATE_SCHEMA_USE_CONNECTION_CREATE_TARGET = "generate-schema-use-connection-create.jdbc";
    
    protected static final String GENERATE_SCHEMA_NO_CONNECTION_PU = "generate-schema-no-connection";
    protected static final String GENERATE_SCHEMA_NO_CONNECTION_DROP_TARGET = "generate-schema-no-connection-drop.jdbc";
    protected static final String GENERATE_SCHEMA_NO_CONNECTION_CREATE_TARGET = "generate-schema-no-connection-create.jdbc";
    
    public DDLTestSuite() {}
    
    public DDLTestSuite(String name) {
        super(name);
    }
    
    @Override
    public void setUp () {
        super.setUp();
        clearCache();
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.setName("DDLTestSuite");

        suite.addTest(ForeignKeyTestSuite.suite());
        suite.addTest(IndexTestSuite.suite());
        
        suite.addTest(new DDLTestSuite("testPersistenceGenerateSchemaUseConnection"));
        suite.addTest(new DDLTestSuite("testPersistenceGenerateSchemaNoConnection"));
        suite.addTest(new DDLTestSuite("testPersistenceGeneratoSchemaDropOnlyScript"));
        
        return suite;
    }
    
    /**
     * Test the generate schema feature from the Persistence API. 
     */
    public void testPersistenceGenerateSchemaUseConnection() {
        Map properties = JUnitTestCaseHelper.getDatabaseProperties(GENERATE_SCHEMA_USE_CONNECTION_PU);
        properties.put(PersistenceUnitProperties.SCHEMA_GENERATION_ACTION, PersistenceUnitProperties.SCHEMA_DROP_AND_CREATE);
        properties.put(PersistenceUnitProperties.SCHEMA_GENERATION_TARGET, PersistenceUnitProperties.SCHEMA_SCRIPTS_GENERATION);
        properties.put(PersistenceUnitProperties.SCHEMA_DROP_SCRIPT_TARGET, GENERATE_SCHEMA_USE_CONNECTION_DROP_TARGET);
        properties.put(PersistenceUnitProperties.SCHEMA_CREATE_SCRIPT_TARGET, GENERATE_SCHEMA_USE_CONNECTION_CREATE_TARGET);
         
        Persistence.generateSchema(GENERATE_SCHEMA_USE_CONNECTION_PU, properties);
    }
    
    /**
     * Test the generate schema feature from the Persistence API.
     * 
     * This test will then further use the PU and make sure the connection
     * occurs and we can persist objects.
     * 
     * All properties are set in code.
     */
    public void testPersistenceGenerateSchemaNoConnection() {
        if (getPlatform().isMySQL()) {
            Map properties = new HashMap();
            properties.put(PersistenceUnitProperties.SESSION_NAME, "generate-schema-no-conn-session");
            properties.put(PersistenceUnitProperties.ORM_SCHEMA_VALIDATION, true);
            properties.put(PersistenceUnitProperties.SCHEMA_DATABASE_PRODUCT_NAME, "MySQL");
            properties.put(PersistenceUnitProperties.SCHEMA_DATABASE_MAJOR_VERSION, "5");
            properties.put(PersistenceUnitProperties.SCHEMA_DATABASE_MINOR_VERSION, "5");
            properties.put(PersistenceUnitProperties.SCHEMA_GENERATION_ACTION, PersistenceUnitProperties.SCHEMA_DROP_AND_CREATE);
            properties.put(PersistenceUnitProperties.SCHEMA_GENERATION_TARGET, PersistenceUnitProperties.SCHEMA_SCRIPTS_GENERATION);
            properties.put(PersistenceUnitProperties.SCHEMA_DROP_SCRIPT_TARGET, GENERATE_SCHEMA_NO_CONNECTION_DROP_TARGET);
            properties.put(PersistenceUnitProperties.SCHEMA_CREATE_SCRIPT_TARGET, GENERATE_SCHEMA_NO_CONNECTION_CREATE_TARGET);
             
            Persistence.generateSchema(GENERATE_SCHEMA_NO_CONNECTION_PU, properties);
            
            // Now create an entity manager and build some objects for this PU using
            // the same session name. Create the schema on the database with the 
            // target scripts built previously.
            properties = JUnitTestCaseHelper.getDatabaseProperties(GENERATE_SCHEMA_NO_CONNECTION_PU);
            properties.put(PersistenceUnitProperties.SESSION_NAME, "generate-schema-no-conn-session");
            properties.put(PersistenceUnitProperties.ORM_SCHEMA_VALIDATION, true);
            properties.put(PersistenceUnitProperties.SCHEMA_DROP_SCRIPT_SOURCE, GENERATE_SCHEMA_NO_CONNECTION_DROP_TARGET);
            properties.put(PersistenceUnitProperties.SCHEMA_CREATE_SCRIPT_SOURCE, GENERATE_SCHEMA_NO_CONNECTION_CREATE_TARGET);
            
            EntityManager em = createEntityManager(GENERATE_SCHEMA_NO_CONNECTION_PU, properties);
    
            try {
                beginTransaction(em);
                    
                Runner runner = new Runner();
                runner.setAge(53);
                runner.setIsFemale();
                runner.setFirstName("Doris");
                runner.setLastName("Day");
                runner.addPersonalBest("10 KM", "47:34");
                runner.addPersonalBest("5", "26:41");
                runner.addAccomplishment("Ran 100KM without stopping", new Date(System.currentTimeMillis()));
                RunnerInfo runnerInfo = new RunnerInfo();
                runnerInfo.setHealth(Health.H);
                runnerInfo.setLevel(Level.A);
                RunnerStatus runnerStatus = new RunnerStatus();
                runnerStatus.setRunningStatus(RunningStatus.D);
                runnerInfo.setStatus(runnerStatus);
                runner.setInfo(runnerInfo);
                  
                Race race = new Race();
                race.setName("The Ultimate Marathon");
                race.addRunner(runner);
                   
                Organizer organizer = new Organizer();
                organizer.setName("Joe Organ");
                organizer.setRace(race);
                  
                Responsibility responsibility = new Responsibility();
                responsibility.setUniqueIdentifier(new Long(System.currentTimeMillis()));
                responsibility.setDescription("Raise funds");
                    
                race.addOrganizer(organizer, responsibility);
                    
                em.persist(race);
                em.persist(organizer);
                em.persist(runner);
                commitTransaction(em);
                        
                // Clear the cache
                em.clear();
                clearCache(GENERATE_SCHEMA_NO_CONNECTION_PU);
            
                Runner runnerRefreshed = em.find(Runner.class, runner.getId());
                assertTrue("The age conversion did not work.", runnerRefreshed.getAge() == 52);
                assertTrue("The embeddable health conversion did not work.", runnerRefreshed.getInfo().getHealth().equals(Health.HEALTHY));
                assertTrue("The embeddable level conversion did not work.", runnerRefreshed.getInfo().getLevel().equals(Level.AMATEUR));
                assertTrue("The nested embeddable running status conversion did not work.", runnerRefreshed.getInfo().getStatus().getRunningStatus().equals(RunningStatus.DOWN_TIME));
                assertTrue("The number of personal bests for this runner is incorrect.", runnerRefreshed.getPersonalBests().size() == 2);
                assertTrue("Distance (map key) conversion did not work.", runnerRefreshed.getPersonalBests().keySet().contains("10K"));
                assertTrue("Distance (map key) conversion did not work.", runnerRefreshed.getPersonalBests().keySet().contains("5K"));
                assertTrue("Time (map value) conversion did not work.", runnerRefreshed.getPersonalBests().values().contains("47:34.0"));
                assertTrue("Time (map value) conversion did not work.", runnerRefreshed.getPersonalBests().values().contains("26:41.0"));
                    
                Race raceRefreshed = em.find(Race.class, race.getId());
                Map<Responsibility, Organizer> organizers = raceRefreshed.getOrganizers();
                assertFalse("No race organizers returned.", organizers.isEmpty());
                assertTrue("More than one race organizer returned.", organizers.size() == 1);
                    
                Responsibility resp = organizers.keySet().iterator().next();
                assertTrue("Responsibility was not uppercased by the converter", resp.getDescription().equals("RAISE FUNDS"));
                    
                for (String accomplishment : runnerRefreshed.getAccomplishments().keySet()) {
                    assertTrue("Accomplishment (map key) conversion did not work.", accomplishment.endsWith("!!!"));
                }
            } catch (RuntimeException e) {
                if (isTransactionActive(em)){
                    rollbackTransaction(em);
                } 
                        
                throw e;
            } finally {
                closeEntityManager(em);
            }
        }
    }
    
    /**
     * Test the generate schema feature from the Persistence API. 
     */
    public void testPersistenceGeneratoSchemaDropOnlyScript() {
        Map properties = new HashMap();
        properties.put(PersistenceUnitProperties.SESSION_NAME, "generate-schema-no-conn-session-drop-only");
        properties.put(PersistenceUnitProperties.ORM_SCHEMA_VALIDATION, true);
        properties.put(PersistenceUnitProperties.SCHEMA_DATABASE_PRODUCT_NAME, "MySQL");
        properties.put(PersistenceUnitProperties.SCHEMA_DATABASE_MAJOR_VERSION, "5");
        properties.put(PersistenceUnitProperties.SCHEMA_DATABASE_MINOR_VERSION, "5");
        properties.put(PersistenceUnitProperties.SCHEMA_GENERATION_ACTION, PersistenceUnitProperties.SCHEMA_DROP);
        properties.put(PersistenceUnitProperties.SCHEMA_GENERATION_TARGET, PersistenceUnitProperties.SCHEMA_SCRIPTS_GENERATION);
        properties.put(PersistenceUnitProperties.SCHEMA_DROP_SCRIPT_TARGET, "generate-schema-no-connection-drop-only.jdbc");

        Persistence.generateSchema(GENERATE_SCHEMA_NO_CONNECTION_PU, properties);
    }
}
