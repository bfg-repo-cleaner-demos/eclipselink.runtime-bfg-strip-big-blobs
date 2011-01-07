/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.tests.returning.model;

import org.eclipse.persistence.tools.schemaframework.*;

/**
 * This class was generated by the TopLink table creator generator.
 * It stores the meta-data (tables) that define the database schema.
 * @see org.eclipse.persistence.sessions.factories.TableCreatorClassGenerator
 */
public class ReturningTableCreator extends TableCreator {

    public ReturningTableCreator() {
        setName("ReturningTestProject");

        addTableDefinition(buildRETURNINGTable());
    }

    public TableDefinition buildRETURNINGTable() {
        TableDefinition table = new TableDefinition();
        table.setName("RETURNING");

        FieldDefinition fieldA1 = new FieldDefinition();
        fieldA1.setName("A1");
        fieldA1.setTypeName("NUMBER");
        fieldA1.setSize(20);
        fieldA1.setSubSize(3);
        fieldA1.setIsPrimaryKey(false);
        fieldA1.setIsIdentity(false);
        fieldA1.setUnique(false);
        fieldA1.setShouldAllowNull(true);
        table.addField(fieldA1);

        FieldDefinition fieldA2 = new FieldDefinition();
        fieldA2.setName("A2");
        fieldA2.setTypeName("NUMBER");
        fieldA2.setSize(20);
        fieldA2.setSubSize(3);
        fieldA2.setIsPrimaryKey(false);
        fieldA2.setIsIdentity(false);
        fieldA2.setUnique(false);
        fieldA2.setShouldAllowNull(true);
        table.addField(fieldA2);

        FieldDefinition fieldB1 = new FieldDefinition();
        fieldB1.setName("B1");
        fieldB1.setTypeName("NUMBER");
        fieldB1.setSize(20);
        fieldB1.setSubSize(3);
        fieldB1.setIsPrimaryKey(false);
        fieldB1.setIsIdentity(false);
        fieldB1.setUnique(false);
        fieldB1.setShouldAllowNull(true);
        table.addField(fieldB1);

        FieldDefinition fieldB2 = new FieldDefinition();
        fieldB2.setName("B2");
        fieldB2.setTypeName("NUMBER");
        fieldB2.setSize(20);
        fieldB2.setSubSize(3);
        fieldB2.setIsPrimaryKey(false);
        fieldB2.setIsIdentity(false);
        fieldB2.setUnique(false);
        fieldB2.setShouldAllowNull(true);
        table.addField(fieldB2);

        FieldDefinition fieldC1 = new FieldDefinition();
        fieldC1.setName("C1");
        fieldC1.setTypeName("NUMBER");
        fieldC1.setSize(20);
        fieldC1.setSubSize(3);
        fieldC1.setIsPrimaryKey(false);
        fieldC1.setIsIdentity(false);
        fieldC1.setUnique(false);
        fieldC1.setShouldAllowNull(true);
        table.addField(fieldC1);

        FieldDefinition fieldC2 = new FieldDefinition();
        fieldC2.setName("C2");
        fieldC2.setTypeName("NUMBER");
        fieldC2.setSize(20);
        fieldC2.setSubSize(3);
        fieldC2.setIsPrimaryKey(false);
        fieldC2.setIsIdentity(false);
        fieldC2.setUnique(false);
        fieldC2.setShouldAllowNull(true);
        table.addField(fieldC2);

        FieldDefinition fieldID = new FieldDefinition();
        fieldID.setName("ID");
        fieldID.setTypeName("NUMBER");
        fieldID.setSize(20);
        fieldID.setSubSize(0);
        fieldID.setIsPrimaryKey(true);
        fieldID.setIsIdentity(true);
        fieldID.setUnique(false);
        fieldID.setShouldAllowNull(false);
        table.addField(fieldID);

        return table;
    }

}
