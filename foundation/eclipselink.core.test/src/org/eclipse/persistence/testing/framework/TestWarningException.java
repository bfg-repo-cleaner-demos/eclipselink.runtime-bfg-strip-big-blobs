/*******************************************************************************
 * Copyright (c) 1998, 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.eclipse.persistence.testing.framework;


/**
 * <p>
 * <b>Purpose</b>: This exception is raised for the test cases where the test case technically passes
 * but something need to be changed to get it completely passes.
 */
public class TestWarningException extends TestException {
    public TestWarningException(String message) {
        super(message);
    }

    public TestWarningException(String theMessage, Throwable internalException) {
            super(theMessage, internalException);
    }
}
