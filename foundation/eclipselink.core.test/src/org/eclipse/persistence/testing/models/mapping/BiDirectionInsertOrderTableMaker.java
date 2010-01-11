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
package org.eclipse.persistence.testing.models.mapping;

/**
 * TopLink generated Project class.
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
public class BiDirectionInsertOrderTableMaker extends org.eclipse.persistence.tools.schemaframework.TableCreator {

    /**
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    public BiDirectionInsertOrderTableMaker() {
        applyPROJECT();
        buildBABYTable();
        buildBABY_MONIOTRTable();
        buildCRIBTable();
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void applyPROJECT() {
        setName("insertOrder");
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildBABY_MONIOTRTable() {
        org.eclipse.persistence.tools.schemaframework.TableDefinition tabledefinition = new org.eclipse.persistence.tools.schemaframework.TableDefinition();

        // SECTION: TABLE
        tabledefinition.setName("BABY_MONIOTR");

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field.setName("ID");
        field.setTypeName("NUMERIC");
        field.setSize(10);
        field.setShouldAllowNull(false);
        field.setIsPrimaryKey(true);
        field.setUnique(false);
        field.setIsIdentity(true);
        tabledefinition.addField(field);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field1 = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field1.setName("BRAND_NAME");
        field1.setTypeName("VARCHAR");
        field1.setSize(20);
        field1.setShouldAllowNull(true);
        field1.setIsPrimaryKey(false);
        field1.setUnique(false);
        field1.setIsIdentity(false);
        tabledefinition.addField(field1);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field2 = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field2.setName("BABY_ID");
        field2.setTypeName("NUMBER");
        field2.setSize(10);
        field2.setShouldAllowNull(true);
        field2.setIsPrimaryKey(false);
        field2.setUnique(false);
        field2.setIsIdentity(false);
        field2.setForeignKeyFieldName("BABY.ID");
        tabledefinition.addField(field2);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field3 = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field3.setName("CRIB_ID");
        field3.setTypeName("NUMBER");
        field3.setSize(10);
        field3.setShouldAllowNull(true);
        field3.setIsPrimaryKey(false);
        field3.setUnique(false);
        field3.setIsIdentity(false);
        field3.setForeignKeyFieldName("CRIB.ID");
        tabledefinition.addField(field3);
        addTableDefinition(tabledefinition);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildBABYTable() {
        org.eclipse.persistence.tools.schemaframework.TableDefinition tabledefinition = new org.eclipse.persistence.tools.schemaframework.TableDefinition();

        // SECTION: TABLE
        tabledefinition.setName("BABY");

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field.setName("NAME");
        field.setTypeName("VARCHAR");
        field.setSize(20);
        field.setShouldAllowNull(true);
        field.setIsPrimaryKey(false);
        field.setUnique(false);
        field.setIsIdentity(false);
        tabledefinition.addField(field);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field1 = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field1.setName("MONITOR_ID");
        field1.setTypeName("NUMBER");
        field1.setSize(10);
        field1.setShouldAllowNull(true);
        field1.setIsPrimaryKey(false);
        field1.setUnique(false);
        field1.setIsIdentity(false);
        field1.setForeignKeyFieldName("BABY_MONIOTR.ID");
        tabledefinition.addField(field1);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field2 = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field2.setName("CRIB_ID");
        field2.setTypeName("NUMBER");
        field2.setSize(10);
        field2.setShouldAllowNull(true);
        field2.setIsPrimaryKey(false);
        field2.setUnique(false);
        field2.setIsIdentity(false);
        field2.setForeignKeyFieldName("CRIB.ID");
        tabledefinition.addField(field2);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field3 = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field3.setName("ID");
        field3.setTypeName("NUMERIC");
        field3.setSize(10);
        field3.setShouldAllowNull(false);
        field3.setIsPrimaryKey(true);
        field3.setUnique(false);
        field3.setIsIdentity(true);
        tabledefinition.addField(field3);
        addTableDefinition(tabledefinition);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildCRIBTable() {
        org.eclipse.persistence.tools.schemaframework.TableDefinition tabledefinition = new org.eclipse.persistence.tools.schemaframework.TableDefinition();

        // SECTION: TABLE
        tabledefinition.setName("CRIB");

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field.setName("ID");
        field.setTypeName("NUMERIC");
        field.setSize(10);
        field.setShouldAllowNull(false);
        field.setIsPrimaryKey(true);
        field.setUnique(false);
        field.setIsIdentity(true);
        tabledefinition.addField(field);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field1 = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field1.setName("COLOR");
        field1.setTypeName("VARCHAR");
        field1.setSize(20);
        field1.setShouldAllowNull(true);
        field1.setIsPrimaryKey(false);
        field1.setUnique(false);
        field1.setIsIdentity(false);
        tabledefinition.addField(field1);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field2 = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field2.setName("MONITOR_ID");
        field2.setTypeName("NUMBER");
        field2.setSize(10);
        field2.setShouldAllowNull(true);
        field2.setIsPrimaryKey(false);
        field2.setUnique(false);
        field2.setIsIdentity(false);
        field2.setForeignKeyFieldName("BABY_MONIOTR.ID");
        tabledefinition.addField(field2);

        // SECTION: FIELD
        org.eclipse.persistence.tools.schemaframework.FieldDefinition field3 = new org.eclipse.persistence.tools.schemaframework.FieldDefinition();
        field3.setName("BABY_ID");
        field3.setTypeName("NUMBER");
        field3.setSize(10);
        field3.setShouldAllowNull(true);
        field3.setIsPrimaryKey(false);
        field3.setUnique(false);
        field3.setIsIdentity(false);
        field3.setForeignKeyFieldName("BABY.ID");
        tabledefinition.addField(field3);
        addTableDefinition(tabledefinition);
    }
}
