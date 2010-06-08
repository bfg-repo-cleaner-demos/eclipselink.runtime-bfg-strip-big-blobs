/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.tests.nchar;

import org.eclipse.persistence.testing.framework.TestSystem;
import org.eclipse.persistence.sessions.DatabaseSession;

/**
 * <b>Purpose</b>: To define a LOB testing system behavior.
 * <p><b>Responsibilities</b>:	<ul>
 * <li> Login and return an initialize database session.
 * <li> Create the database.
 * </ul>
 */
public class NcharTestSystem extends TestSystem {

    public NcharTestSystem() {
        project = new CharNcharProject();
    }

    public void addDescriptors(DatabaseSession session) {
        if (project == null) {
            project = new CharNcharProject();
        }

        session.addDescriptors(project);
    }

    public void createTables(DatabaseSession session) {
        new CharNcharTableCreator().replaceTables(session);
    }

}
