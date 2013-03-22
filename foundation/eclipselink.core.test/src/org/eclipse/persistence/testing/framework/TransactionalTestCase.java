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
 * This superclass can be used to inherit transaction setup and reset to ensure the database is not modified.
 */
public abstract class TransactionalTestCase extends AutoVerifyTestCase {
    public static boolean disableTransactions = false;

    protected void setup() {
        if (!disableTransactions) {
            getAbstractSession().beginTransaction();
        }
    }

    public void reset() {
        if (getAbstractSession().isInTransaction() || getSession().isRemoteSession()) {
            if (!disableTransactions) {
                getAbstractSession().rollbackTransaction();
            }
        }
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();
    }
}
