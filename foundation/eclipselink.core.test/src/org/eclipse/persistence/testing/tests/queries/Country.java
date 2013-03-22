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
package org.eclipse.persistence.testing.tests.queries;

import org.eclipse.persistence.tools.schemaframework.*;

public class Country {
    public String name;
    public static Country canada;
    public static Country usa;
    public static Country greece;

    /**
     * This method was created in VisualAge.
     * @return org.eclipse.persistence.testing.tests.queries.Country
     */
    public static Country billingCountry1() {
        Country example = new Country();
        example.setName("USA");
        return example;
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    public static TableDefinition buildCOUNTRYTable() {
        org.eclipse.persistence.tools.schemaframework.TableDefinition tabledefinition = new org.eclipse.persistence.tools.schemaframework.TableDefinition();

        // SECTION: TABLE
        tabledefinition.setName("COUNTRY");

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field.setName("NAME");
        field.setTypeName("VARCHAR");
        field.setShouldAllowNull(false);
        field.setIsPrimaryKey(true);
        field.setUnique(false);
        field.setIsIdentity(false);
        tabledefinition.addField(field);

        return tabledefinition;
    }

    public static Country canada() {
        Country country = new Country();
        country.setName("Canada");
        canada = country;
        return canada;
    }

    public static Country greece() {
        Country country = new Country();
        country.setName("Greece");
        greece = country;
        return greece;
    }

    /**
     * This method was created in VisualAge.
     * @return org.eclipse.persistence.testing.tests.queries.Country
     */
    public void setName(String countryName) {
        name = countryName;
    }

    public static Country usa() {
        Country country = new Country();
        country.setName("USA");
        usa = country;
        return usa;
    }
}
