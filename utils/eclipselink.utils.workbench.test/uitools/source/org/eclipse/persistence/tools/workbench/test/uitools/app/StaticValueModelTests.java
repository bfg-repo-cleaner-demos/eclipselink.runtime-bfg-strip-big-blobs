/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.uitools.app;

import org.eclipse.persistence.tools.workbench.test.utility.TestTools;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

import org.eclipse.persistence.tools.workbench.uitools.app.AbstractReadOnlyPropertyValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.ValueModel;


public class StaticValueModelTests extends TestCase {
	private ValueModel objectHolder;

	public static void main(String[] args) {
		TestRunner.main(new String[] {"-c", StaticValueModelTests.class.getName()});
	}
	
	public static Test suite() {
		return new TestSuite(StaticValueModelTests.class);
	}
	
	public StaticValueModelTests(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		this.objectHolder = this.buildObjectHolder();
	}

	private ValueModel buildObjectHolder() {
		return new AbstractReadOnlyPropertyValueModel() {
			public Object getValue() {
				return "foo";
			}
		};
	}

	protected void tearDown() throws Exception {
		TestTools.clear(this);
		super.tearDown();
	}

	public void testGetValue() {
		assertEquals("foo", this.objectHolder.getValue());
	}

	public void testToString() {
		assertTrue(this.objectHolder.toString().indexOf("foo") >= 0);
	}

}
