/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.utility.string;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

public class BestFirstPartialStringComparatorEngineTests
	extends ExhaustivePartialStringComparatorEngineTests
{

	public static void main(String[] args) {
		TestRunner.main(new String[] {"-c", BestFirstPartialStringComparatorEngineTests.class.getName()});
	}
	
	public static Test suite() {
		return new TestSuite(BestFirstPartialStringComparatorEngineTests.class);
	}
	
	public BestFirstPartialStringComparatorEngineTests(String name) {
		super(name);
	}

}
