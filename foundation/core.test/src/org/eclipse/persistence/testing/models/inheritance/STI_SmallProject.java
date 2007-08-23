/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.models.inheritance;

/**
 * STI stands for Single Table Inheritance.
 * STI_SmallProject is a concrete subclass of STI_Project which adds no additional attributes.
 * When the PROJ_TYPE is set to 'S' in the STI_PROJECT table a STI_SmallProject is instantiated.
 * No table definition is required and the descriptor is very simple.
 */
public class STI_SmallProject extends STI_Project {

    /**
     * Print the SmallProject's information.
     */
    public String toString() {
        java.io.StringWriter writer = new java.io.StringWriter();

        writer.write("STI_SmallProject: ");
        writer.write(getName());
        writer.write(" ");
        writer.write(getDescription());
        writer.write("");
        return writer.toString();
    }
}
