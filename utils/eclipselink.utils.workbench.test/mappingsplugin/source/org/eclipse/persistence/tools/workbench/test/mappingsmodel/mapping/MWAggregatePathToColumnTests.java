/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.mappingsmodel.mapping;

import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.persistence.tools.workbench.mappingsmodel.mapping.relational.MWAggregatePathToColumn;
import org.eclipse.persistence.tools.workbench.utility.CollectionTools;

public class MWAggregatePathToColumnTests extends TestCase {

	public static void main(String[] args) {
		junit.swingui.TestRunner.main(new String[] {"-c", MWAggregatePathToColumnTests.class.getName()});
	}
	
	public static Test suite() {
		return new TestSuite(MWAggregatePathToColumnTests.class);
	}
	
	public MWAggregatePathToColumnTests(String name) {
		super(name);
	}
	
	public static Collection associationsInPersonAddressMapping() {
		return CollectionTools.sort(MWAggregateMappingTests.personAddressMapping().pathsToFields());
	}

	public void testGetFullDescription() {
		MWAggregatePathToColumn association = (MWAggregatePathToColumn) associationsInPersonAddressMapping().iterator().next();
		String tersePathDescription = association.getPathDescription() + association.getAggregateRuntimeFieldNameGenerator().fieldNameForRuntime();
		assertEquals("Address.cityDIRECT", tersePathDescription);
	}
	
	public void testGetPathDescription() {
		MWAggregatePathToColumn association = (MWAggregatePathToColumn) associationsInPersonAddressMapping().iterator().next();
		String tersePathDescription = association.getPathDescription();
		assertEquals("Address.city", tersePathDescription);
	}
}
