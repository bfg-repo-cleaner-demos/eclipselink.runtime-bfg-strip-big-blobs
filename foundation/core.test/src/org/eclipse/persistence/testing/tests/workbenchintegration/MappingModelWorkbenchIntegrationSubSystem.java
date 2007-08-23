/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.workbenchintegration;

/**
 * Extend the MappingSystem to allow us to compile/read the mapping projects
 * to/from deployment source
 * 
 * @author Guy Pelletier
 */
public class MappingModelWorkbenchIntegrationSubSystem extends MappingModelWorkbenchIntegrationSystem {    
    protected void buildProjects() {
        // Mapping project
        project = WorkbenchIntegrationSystemHelper.buildProjectClass(project, PROJECT_FILE);
        
        // Legacy project.
        legacyProject = WorkbenchIntegrationSystemHelper.buildProjectClass(legacyProject, LEGACY_PROJECT_FILE);

        // Multiple table project.
        multipleTableProject = WorkbenchIntegrationSystemHelper.buildProjectClass(multipleTableProject, MULTIPLE_TABLE_PROJECT_FILE);
        
        // Keyboard project.
        keyboardProject = WorkbenchIntegrationSystemHelper.buildProjectClass(keyboardProject, KEYBOARD_PROJECT_FILE);

        // Bi-directional insert order project.
        bidirectionalProject = WorkbenchIntegrationSystemHelper.buildProjectClass(bidirectionalProject, BIDIRECTIONAL_PROJECT_FILE);
    }
}
