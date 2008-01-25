/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.utility.diff;

import org.eclipse.persistence.tools.workbench.test.utility.diff.model.Employee;
import org.eclipse.persistence.tools.workbench.test.utility.diff.model.SimpleEmployee;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

import org.eclipse.persistence.tools.workbench.utility.diff.Differentiator;
import org.eclipse.persistence.tools.workbench.utility.diff.ReflectiveDifferentiator;



public class SimpleReflectiveDiffTests extends AbstractReflectiveDiffTests {


	public static void main(String[] args) {
		TestRunner.main(new String[] {"-c", SimpleReflectiveDiffTests.class.getName()});
	}

	public static Test suite() {
		return new TestSuite(SimpleReflectiveDiffTests.class);
	}
	
	public SimpleReflectiveDiffTests(String name) {
		super(name);
	}

	protected Differentiator buildDifferentiator() {
		ReflectiveDifferentiator result = new ReflectiveDifferentiator(SimpleEmployee.class);
		result.addKeyFieldNamed("id");
		return result;
	}

	protected Employee buildEmployee(int id, String name) {
		return new SimpleEmployee(id, name);
	}

	protected ReflectiveDifferentiator employeeDifferentiator() {
		return (ReflectiveDifferentiator) this.differentiator;
	}

}
