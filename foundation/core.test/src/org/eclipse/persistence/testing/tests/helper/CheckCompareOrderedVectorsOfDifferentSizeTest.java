/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.helper;

import java.util.Vector;
import org.eclipse.persistence.internal.helper.*;
import org.eclipse.persistence.testing.framework.*;

public class CheckCompareOrderedVectorsOfDifferentSizeTest extends AutoVerifyTestCase {
    Exception e;
    Vector v1;
    Vector v2;
    boolean test1ResultIsTrue = false;

    public CheckCompareOrderedVectorsOfDifferentSizeTest() {
        setDescription("Test of Helper.compareOrderedVectors(Vector vector1, Vector vector2) when vectors are of different size.");
    }

    public void reset() {
        v1 = null;
        v2 = null;
    }

    public void setup() {
        v1 = new Vector();
        v1.addElement(new Integer(1));
        v1.addElement(new Integer(2));
        v2 = new Vector();
        v2.addElement(new Integer(3));
    }

    public void test() {
        try {
            test1ResultIsTrue = Helper.compareOrderedVectors(v1, v2);

        } catch (Exception e) {
            this.e = e;
            throw new TestErrorException("An exception should not have been thrown when checking if vectors are of different size.");
        }
    }

    public void verify() {
        if (test1ResultIsTrue) {
            throw new TestErrorException("Helper.compareOrderedVectors(v1, v2) does not recognize that Vectors are of different size.");
        }
        if (e != null) {
            throw new TestErrorException("An exception should not have been thrown when checking if vectors are of different size: " + e.toString());
        }
    }
}