/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.mappingsmodel.db;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.persistence.tools.workbench.test.models.projects.TestDatabases;
import org.eclipse.persistence.tools.workbench.test.utility.TestTools;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

import org.eclipse.persistence.tools.workbench.mappingsmodel.db.MWDatabase;
import org.eclipse.persistence.tools.workbench.mappingsmodel.db.MWTable;
import org.eclipse.persistence.tools.workbench.mappingsmodel.spi.db.ExternalTableDescription;
import org.eclipse.persistence.tools.workbench.utility.CollectionTools;


// TODO expand beyond Oracle testing (DB2, Sybase, MS Access, etc.)
public class MWDatabaseTests extends AbstractModelDatabaseTests {


	public static void main(String[] args) {
		TestRunner.main(new String[] {"-c", MWDatabaseTests.class.getName()});
	}

	public static Test suite() {
		TestTools.setUpJUnitThreadContextClassLoader();
		return new TestSuite(MWDatabaseTests.class);
	}

	public MWDatabaseTests(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testTableNamed() {
		MWDatabase db = TestDatabases.oracleDatabase();
		MWTable table1 = db.addTable("TEST_TABLE");
		MWTable table2;

		table2 = db.tableNamed("TEST_CATALOG", "TEST_SCHEMA", "TEST_TABLE");
		assertNull("The table was not supposed to be found.", table2);
		table2 = db.tableNamed(null, null, "TEST_TABLE");
		assertEquals("The table was not found.", table1, table2);

		table1.rename("TEST_CATALOG", null, "TEST_TABLE");
		table2 = db.tableNamed("TEST_CATALOG", "TEST_SCHEMA", "TEST_TABLE");
		assertNull("The table was not supposed to be found.", table2);
		table2 = db.tableNamed("TEST_CATALOG", null, "TEST_TABLE");
		assertEquals("The table was not found.", table1, table2);
		
		table1.rename("TEST_CATALOG", "TEST_SCHEMA", "TEST_TABLE");
		table2 = db.tableNamed(null, null, "TEST_TABLE");
		assertNull("The table was not supposed to be found.", table2);
		table2 = db.tableNamed("TEST_CATALOG", "TEST_SCHEMA", "TEST_TABLE");
		assertEquals("The table was not found.", table1, table2);
	}

	public void testLogin() throws Exception {
		this.database.login();
		assertTrue("Not connected to database.", this.database.isConnected());
		this.database.logout();
	}

	public void testLogout() throws Exception {
		this.database.login();
		this.database.logout();
		assertFalse("Database connection was not closed", this.database.isConnected());
	}

	public void testImportTables() throws Exception {
		MWDatabase db = TestDatabases.oracleDatabase();
		try {
			db.login();
			Collection tableTypes = CollectionTools.collection(db.tableTypeNames());
//			System.out.println("table types: " + tableTypes);
			assertTrue("The list of table types does not contain TABLE", tableTypes.contains("TABLE"));
			// Oracle 10g will return 117 tables - just refresh a few...
			db.importQualifiedTablesFor(this.xxx(db.externalTableDescriptions(null, null, null, new String[] {"TABLE"})));
			assertTrue("No tables were added.", db.tablesSize() > 0);
		} finally {
			if (db.isConnected()) {
				db.logout();
			}
		}
	}

	private static final String[] IMPORT_TABLE_NAMES = new String[] {"EMP", "ADDR", "STATE"};

	private Collection xxx(Iterator externalTables) {
		Collection result = new ArrayList();
		while (externalTables.hasNext()) {
			ExternalTableDescription xtd = (ExternalTableDescription) externalTables.next();
			if (CollectionTools.contains(IMPORT_TABLE_NAMES, xtd.getName())) {
				result.add(xtd);
			}
		}
		return result;
	}

	public void testWriteDDLOn() throws Exception {
		Writer writer = new StringWriter();
		this.database.login();
		this.database.writeDDLOn(writer);
		this.database.logout();
		String ddl = writer.toString();
		assertTrue(ddl, ddl.indexOf("CREATE TABLE EMP") != -1);
		assertTrue(ddl, ddl.indexOf("ALTER TABLE EMP ADD CONSTRAINT EMP_ADDR FOREIGN KEY") != -1);
		assertTrue(ddl, ddl.indexOf("ALTER TABLE EMP ADD CONSTRAINT EMP_BOSS FOREIGN KEY") != -1);
		assertTrue(ddl, ddl.indexOf("CREATE TABLE ADDR") != -1);
		assertTrue(ddl, ddl.indexOf("ALTER TABLE ADDR ADD CONSTRAINT ADDR_STATE FOREIGN KEY") != -1);
		assertTrue(ddl, ddl.indexOf("CREATE TABLE STATE") != -1);
	}

	public void testGenerateTables() throws Exception {
		this.database.login();
		this.database.generateTables();
		this.database.logout();
		
		MWDatabase database2 = TestDatabases.oracleDatabase();
		database2.login();
		database2.importUnqualifiedTablesFor(this.matching(database2.externalTableDescriptions(null, null, null, new String[] {"TABLE"})));
		database2.logout();
		assertEquals(this.database.tablesSize(), database2.tablesSize());
		// TODO compare the 2 databases
	}

	private Collection matching(Iterator externalTables) {
		Collection result = new ArrayList();
		while (externalTables.hasNext()) {
			ExternalTableDescription xtd = (ExternalTableDescription) externalTables.next();
			if (this.database.containsTableNamed(null, null, xtd.getName())) {
				result.add(xtd);
			}
		}
		return result;
	}

}
