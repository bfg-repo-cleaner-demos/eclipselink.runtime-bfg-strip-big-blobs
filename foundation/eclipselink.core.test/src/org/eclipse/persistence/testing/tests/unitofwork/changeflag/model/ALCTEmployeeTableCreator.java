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
package org.eclipse.persistence.testing.tests.unitofwork.changeflag.model;

/**
 * TopLink generated Project class. 
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
public class ALCTEmployeeTableCreator extends org.eclipse.persistence.tools.schemaframework.TableCreator {

    /**
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    public ALCTEmployeeTableCreator() {
        applyPROJECT();
        buildEMPLOYEETable();
    }

    /**
     * TopLink generated method. 
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void applyPROJECT() {
        setName("ALCTEmployee");
    }

    /**
     * TopLink generated method. 
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildEMPLOYEETable() {
        org.eclipse.persistence.tools.schemaframework.TableDefinition tabledefinition = 
            new org.eclipse.persistence.tools.schemaframework.TableDefinition();


        // SECTION: TABLE
        tabledefinition.setName("ALCTEMPLOYEE");

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field = 
            new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field.setName("EMP_ID");
        field.setTypeName("NUMERIC");
        field.setSize(15);
        field.setShouldAllowNull(false);
        field.setIsPrimaryKey(true);
        field.setUnique(false);
        field.setIsIdentity(true);
        tabledefinition.addField(field);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field1 = 
            new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field1.setName("F_NAME");
        field1.setTypeName("VARCHAR");
        field1.setSize(40);
        field1.setShouldAllowNull(true);
        field1.setIsPrimaryKey(false);
        field1.setUnique(false);
        field1.setIsIdentity(false);
        tabledefinition.addField(field1);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field2 = 
            new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field2.setName("L_NAME");
        field2.setTypeName("VARCHAR");
        field2.setSize(40);
        field2.setShouldAllowNull(true);
        field2.setIsPrimaryKey(false);
        field2.setUnique(false);
        field2.setIsIdentity(false);
        tabledefinition.addField(field2);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field3 = 
            new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field3.setName("START_DATE");
        field3.setTypeName("DATE");
        field3.setSize(23);
        field3.setShouldAllowNull(true);
        field3.setIsPrimaryKey(false);
        field3.setUnique(false);
        field3.setIsIdentity(false);
        tabledefinition.addField(field3);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field4 = 
            new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field4.setName("END_DATE");
        field4.setTypeName("DATE");
        field4.setSize(23);
        field4.setShouldAllowNull(true);
        field4.setIsPrimaryKey(false);
        field4.setUnique(false);
        field4.setIsIdentity(false);
        tabledefinition.addField(field4);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field7 = 
            new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field7.setName("GENDER");
        field7.setTypeName("VARCHAR");
        field7.setSize(1);
        field7.setShouldAllowNull(true);
        field7.setIsPrimaryKey(false);
        field7.setUnique(false);
        field7.setIsIdentity(false);
        tabledefinition.addField(field7);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field10 = 
            new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field10.setName("VERSION");
        field10.setTypeName("NUMERIC");
        field10.setSize(15);
        field10.setShouldAllowNull(true);
        field10.setIsPrimaryKey(false);
        field10.setUnique(false);
        field10.setIsIdentity(false);
        tabledefinition.addField(field10);
        addTableDefinition(tabledefinition);
    }
}
