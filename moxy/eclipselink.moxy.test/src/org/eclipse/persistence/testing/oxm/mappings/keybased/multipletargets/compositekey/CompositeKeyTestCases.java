/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.oxm.mappings.keybased.multipletargets.compositekey;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.persistence.testing.oxm.mappings.keybased.multipletargets.compositekey.attributekey.CompositeAttributeKeysTestCases;
import org.eclipse.persistence.testing.oxm.mappings.keybased.multipletargets.compositekey.nestedattributekey.NestedAttributeKeyTestCases;

public class CompositeKeyTestCases extends TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite("Multiple targets with composite key test suite");
        suite.addTestSuite(CompositeAttributeKeysTestCases.class);
        suite.addTestSuite(NestedAttributeKeyTestCases.class);
        return suite;
    }

    public static void main(String[] args) {
        String[] arguments = { "-c", "org.eclipse.persistence.testing.oxm.mappings.keybased.multipletargets.compositekey.CompositeKeyTestCases"};
        junit.textui.TestRunner.main(arguments);
    }
}
