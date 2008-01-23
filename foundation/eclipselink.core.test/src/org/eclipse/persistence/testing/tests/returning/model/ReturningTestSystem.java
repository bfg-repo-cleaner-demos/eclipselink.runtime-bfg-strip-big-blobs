/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.returning.model;

import org.eclipse.persistence.testing.framework.TestSystem;
import org.eclipse.persistence.sessions.*;

/**
 * <b>Purpose</b>: To define a Returning testing system behavior.
 * <p><b>Responsibilities</b>:	<ul>
 * <li> Login and return an initialize database session.
 * <li> Create the database.
 * </ul>
 */
public class ReturningTestSystem extends TestSystem {

    public ReturningTestSystem() {
        super();
    }

    public void addDescriptors(DatabaseSession session) {
        if (project == null) {
            project = new ReturningProject();
        }
        session.addDescriptors(project);
    }

    public void createTables(DatabaseSession session) {
        new ReturningTableCreator().replaceTables(session);
    }

}
