/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.mappingsmodel.spi.meta.classloader;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

import org.eclipse.persistence.tools.workbench.utility.ClassTools;

/**
 * decentralize test creation code
 */
public class AllModelSPIMetaClassLoaderTests {

	public static void main(String[] args) {
		TestRunner.main(new String[] {"-c", AllModelSPIMetaClassLoaderTests.class.getName()});
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(ClassTools.packageNameFor(AllModelSPIMetaClassLoaderTests.class));

		suite.addTest(CLExternalClassRepositoryTests.suite());
		suite.addTest(CLExternalClassTests.suite());
		suite.addTest(CLExternalConstructorTests.suite());
		suite.addTest(CLExternalFieldTests.suite());
		suite.addTest(CLExternalMethodTests.suite());
		suite.addTest(CLExternalClassDescriptionTests.suite());

		return suite;
	}

	/**
	 * suppress instantiation
	 */
	private AllModelSPIMetaClassLoaderTests() {
		super();
		throw new UnsupportedOperationException();
	}

}
