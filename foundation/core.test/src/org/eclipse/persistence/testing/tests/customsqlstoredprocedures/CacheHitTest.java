/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.customsqlstoredprocedures;

import org.eclipse.persistence.testing.framework.*;

/**
 * Test selecting using an object's primary key to ensure that it does not go to the databaase.
 */
public class CacheHitTest extends org.eclipse.persistence.testing.tests.queries.inmemory.CacheHitTest {
    public CacheHitTest() {
        super();
    }

    public CacheHitTest(Object originalObject) {
        super(originalObject);
    }

    protected void setup() {
        if (!((getSession().getLogin().getPlatform().isOracle()) || (getSession().getLogin().getPlatform().isSQLServer()) || (getSession().getLogin().getPlatform().isSybase()) || (getSession().getLogin().getPlatform().isMySQL()))) {
            throw new TestWarningException("This test is only valid for Database Systems supporting StoredProcedures");
        }
        super.setup();
    }
}