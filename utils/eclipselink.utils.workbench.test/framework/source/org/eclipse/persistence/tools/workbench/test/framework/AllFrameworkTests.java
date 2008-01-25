/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.framework;

import org.eclipse.persistence.tools.workbench.test.framework.resources.AllFrameworkResourcesTests;
import org.eclipse.persistence.tools.workbench.test.framework.ui.tools.AllFrameworkUIToolsTests;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

import org.eclipse.persistence.tools.workbench.utility.ClassTools;


public class AllFrameworkTests {

	public static void main(String[] args) {
		TestRunner.main(new String[] {"-c", AllFrameworkTests.class.getName()});
	}
	
	public static Test suite() {
		return suite(true);
	}
	
	public static Test suite(boolean all) {
		TestSuite suite = new TestSuite(ClassTools.packageNameFor(AllFrameworkTests.class));

		suite.addTest(AllFrameworkResourcesTests.suite());		

		suite.addTest(AbstractApplicationTests.suite());
		suite.addTest(AllFrameworkUIToolsTests.suite());

		return suite;
	}

	private AllFrameworkTests() {
		super();
		throw new UnsupportedOperationException();
	}

}
