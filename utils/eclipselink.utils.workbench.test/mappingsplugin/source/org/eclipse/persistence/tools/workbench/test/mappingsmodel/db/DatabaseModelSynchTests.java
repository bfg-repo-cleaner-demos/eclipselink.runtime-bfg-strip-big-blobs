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

import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

import org.eclipse.persistence.tools.workbench.mappingsmodel.db.MWColumnPair;

/**
 * 
 */
public class DatabaseModelSynchTests
	extends AbstractModelDatabaseTests
{

	public static void main(String[] args) {
		TestRunner.main(new String[] {"-c", DatabaseModelSynchTests.class.getName()});
	}

	public static Test suite() {
		return new TestSuite(DatabaseModelSynchTests.class);
	}

	public DatabaseModelSynchTests(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * remove the field from the table and it should be removed from the
	 * reference also
	 */
	public void testRemoveSourceField() {
		this.table_EMP.removeColumn(this.field_ADDR_ID1_FK);
		boolean match = false;
		for (Iterator stream = this.reference_EMP_ADDR.columnPairs(); stream.hasNext(); ) {
			MWColumnPair pair = (MWColumnPair) stream.next();
			if (pair.getTargetColumn() == this.field_ADDR_ID1) {
				match = true;
				assertNull(pair.getSourceColumn());
			}
		}
		assertTrue(match);
	}

	/**
	 * remove the field from the table and it should be removed from the
	 * reference also
	 */
	public void testRemoveTargetField() {
		this.table_ADDR.removeColumn(this.field_ADDR_ID2);
		boolean match = false;
		for (Iterator stream = this.reference_EMP_ADDR.columnPairs(); stream.hasNext(); ) {
			MWColumnPair pair = (MWColumnPair) stream.next();
			if (pair.getSourceColumn() == this.field_ADDR_ID2_FK) {
				match = true;
				assertNull(pair.getTargetColumn());
			}
		}
		assertTrue(match);
	}

	public void testRemoveTargetTable() {
		this.database.removeTable(this.table_ADDR);
		assertEquals(0, this.reference_EMP_ADDR.columnPairsSize());
		assertNull(this.reference_EMP_ADDR.getTargetTable());
	}

}
