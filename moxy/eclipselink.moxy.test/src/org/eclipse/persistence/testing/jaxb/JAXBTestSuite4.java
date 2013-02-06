/*******************************************************************************
 * Copyright (c) 1998, 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb;

import org.eclipse.persistence.testing.jaxb.collections.CollectionsTestSuite;
import org.eclipse.persistence.testing.jaxb.defaultvalue.DefaultValueTestSuite;
import org.eclipse.persistence.testing.jaxb.employee.JAXBEmployeeNSTestCases;
import org.eclipse.persistence.testing.jaxb.employee.JAXBEmployeeNoWrapperTestCases;
import org.eclipse.persistence.testing.jaxb.employee.JAXBEmployeeTestCases;
import org.eclipse.persistence.testing.jaxb.jaxbfragment.JAXBFragmentTestCases;
import org.eclipse.persistence.testing.jaxb.map.MapElementWrapperExternalTestCases;
import org.eclipse.persistence.testing.jaxb.map.MapElementWrapperTestCases;
import org.eclipse.persistence.testing.jaxb.map.MapNamespaceBarTestCases;
import org.eclipse.persistence.testing.jaxb.map.MapNamespaceFooTestCases;
import org.eclipse.persistence.testing.jaxb.map.MapTestCases;
import org.eclipse.persistence.testing.jaxb.refresh.RefreshTestSuite;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class JAXBTestSuite4 extends TestCase {
	
	public static Test suite() {
	    TestSuite suite = new TestSuite("JAXB20 Compiler Test Suite4");
	    suite.addTest(SunCompatibilityTestSuite.suite());
	    suite.addTest(RefreshTestSuite.suite());
        suite.addTest(org.eclipse.persistence.testing.jaxb.substitution.SubstitutionTestSuite.suite());
        suite.addTest(org.eclipse.persistence.testing.jaxb.innerclasses.InnerClassTestSuite.suite());
        suite.addTest(CollectionsTestSuite.suite());
        suite.addTest(DefaultValueTestSuite.suite());
	    
	    suite.addTestSuite(JAXBEmployeeTestCases.class);
	    suite.addTestSuite(JAXBEmployeeNoWrapperTestCases.class);
	    suite.addTestSuite(JAXBEmployeeNSTestCases.class);
	    suite.addTestSuite(JAXBFragmentTestCases.class);
	    suite.addTest(org.eclipse.persistence.testing.jaxb.eventhandler.EventHandlerTestSuite.suite());
	        
	    suite.addTestSuite(MapTestCases.class);
	    suite.addTestSuite(MapElementWrapperTestCases.class);
	    suite.addTestSuite(MapElementWrapperExternalTestCases.class);
	    suite.addTestSuite(MapNamespaceBarTestCases.class);
	    suite.addTestSuite(MapNamespaceFooTestCases.class);
	    return suite;	    
	}
	
	  public static void main(String[] args) {
	        String[] arguments = { "-c", "org.eclipse.persistence.testing.jaxb.JAXBTestSuite4" };
	        junit.textui.TestRunner.main(arguments);
	    }
}
