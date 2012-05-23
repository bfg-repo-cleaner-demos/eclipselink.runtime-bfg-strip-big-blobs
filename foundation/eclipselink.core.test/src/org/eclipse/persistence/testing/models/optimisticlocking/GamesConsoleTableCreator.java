/*******************************************************************************
 * Copyright (c) 1998, 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     dminsky - initial API and implementation
 ******************************************************************************/  
package org.eclipse.persistence.testing.models.optimisticlocking;

import org.eclipse.persistence.tools.schemaframework.*;

/**
 * This class was generated by the TopLink table creator generator.
 * It stores the meta-data (tables) that define the database schema.
 * @see org.eclipse.persistence.sessions.factories.TableCreatorClassGenerator
 */

public class GamesConsoleTableCreator extends org.eclipse.persistence.tools.schemaframework.TableCreator {

public GamesConsoleTableCreator() {
    setName("GamesConsoleSystem");
    
    addTableDefinition(buildOL_CONTROLLERTable());
    addTableDefinition(buildOL_CAMERATable());
    addTableDefinition(buildOL_CONSOLETable());
    addTableDefinition(buildOL_GAMERTable());
    addTableDefinition(buildOL_CONSOLE_OL_GAMERTable());
    addTableDefinition(buildOL_SKILLTable());
}

public TableDefinition buildOL_CAMERATable() {
    TableDefinition table = new TableDefinition();
    table.setName("OL_CAMERA");
    
    FieldDefinition fieldDESCRIPTION = new FieldDefinition();
    fieldDESCRIPTION.setName("DESCRIPTION");
    fieldDESCRIPTION.setTypeName("VARCHAR2");
    fieldDESCRIPTION.setSize(32);
    fieldDESCRIPTION.setSubSize(0);
    fieldDESCRIPTION.setIsPrimaryKey(false);
    fieldDESCRIPTION.setIsIdentity(false);
    fieldDESCRIPTION.setUnique(false);
    fieldDESCRIPTION.setShouldAllowNull(true);
    table.addField(fieldDESCRIPTION);
    
    FieldDefinition fieldID = new FieldDefinition();
    fieldID.setName("ID");
    fieldID.setTypeName("NUMBER");
    fieldID.setSize(0);
    fieldID.setSubSize(0);
    fieldID.setIsPrimaryKey(true);
    fieldID.setIsIdentity(false);
    fieldID.setUnique(false);
    fieldID.setShouldAllowNull(false);
    table.addField(fieldID);
    
    FieldDefinition fieldNAME = new FieldDefinition();
    fieldNAME.setName("NAME");
    fieldNAME.setTypeName("VARCHAR2");
    fieldNAME.setSize(32);
    fieldNAME.setSubSize(0);
    fieldNAME.setIsPrimaryKey(false);
    fieldNAME.setIsIdentity(false);
    fieldNAME.setUnique(false);
    fieldNAME.setShouldAllowNull(true);
    table.addField(fieldNAME);
    
    FieldDefinition fieldUPDATED = new FieldDefinition();
    fieldUPDATED.setName("UPDATED");
    fieldUPDATED.setTypeName("TIMESTAMP");
    fieldUPDATED.setSize(0);
    fieldUPDATED.setSubSize(0);
    fieldUPDATED.setIsPrimaryKey(false);
    fieldUPDATED.setIsIdentity(false);
    fieldUPDATED.setUnique(false);
    fieldUPDATED.setShouldAllowNull(true);
    table.addField(fieldUPDATED);
    
    FieldDefinition fieldVERSION = new FieldDefinition();
    fieldVERSION.setName("VERSION");
    fieldVERSION.setTypeName("NUMBER");
    fieldVERSION.setSize(0);
    fieldVERSION.setSubSize(0);
    fieldVERSION.setIsPrimaryKey(false);
    fieldVERSION.setIsIdentity(false);
    fieldVERSION.setUnique(false);
    fieldVERSION.setShouldAllowNull(true);
    table.addField(fieldVERSION);
    
    return table;
}

public TableDefinition buildOL_CONSOLETable() {
    TableDefinition table = new TableDefinition();
    table.setName("OL_CONSOLE");
    
    FieldDefinition fieldCAMERA_ID = new FieldDefinition();
    fieldCAMERA_ID.setName("CAMERA_ID");
    fieldCAMERA_ID.setTypeName("NUMBER");
    fieldCAMERA_ID.setSize(0);
    fieldCAMERA_ID.setSubSize(0);
    fieldCAMERA_ID.setIsPrimaryKey(false);
    fieldCAMERA_ID.setIsIdentity(false);
    fieldCAMERA_ID.setUnique(false);
    fieldCAMERA_ID.setShouldAllowNull(true);
    table.addField(fieldCAMERA_ID);
    
    FieldDefinition fieldDESCRIPTION = new FieldDefinition();
    fieldDESCRIPTION.setName("DESCRIPTION");
    fieldDESCRIPTION.setTypeName("VARCHAR2");
    fieldDESCRIPTION.setSize(32);
    fieldDESCRIPTION.setSubSize(0);
    fieldDESCRIPTION.setIsPrimaryKey(false);
    fieldDESCRIPTION.setIsIdentity(false);
    fieldDESCRIPTION.setUnique(false);
    fieldDESCRIPTION.setShouldAllowNull(true);
    table.addField(fieldDESCRIPTION);
    
    FieldDefinition fieldID = new FieldDefinition();
    fieldID.setName("ID");
    fieldID.setTypeName("NUMBER");
    fieldID.setSize(0);
    fieldID.setSubSize(0);
    fieldID.setIsPrimaryKey(true);
    fieldID.setIsIdentity(false);
    fieldID.setUnique(false);
    fieldID.setShouldAllowNull(false);
    table.addField(fieldID);
    
    FieldDefinition fieldNAME = new FieldDefinition();
    fieldNAME.setName("NAME");
    fieldNAME.setTypeName("VARCHAR2");
    fieldNAME.setSize(32);
    fieldNAME.setSubSize(0);
    fieldNAME.setIsPrimaryKey(false);
    fieldNAME.setIsIdentity(false);
    fieldNAME.setUnique(false);
    fieldNAME.setShouldAllowNull(true);
    table.addField(fieldNAME);
    
    FieldDefinition fieldPSU_ON = new FieldDefinition();
    fieldPSU_ON.setName("PSU_ON");
    fieldPSU_ON.setTypeName("CHAR");
    fieldPSU_ON.setSize(0);
    fieldPSU_ON.setSubSize(0);
    fieldPSU_ON.setIsPrimaryKey(false);
    fieldPSU_ON.setIsIdentity(false);
    fieldPSU_ON.setUnique(false);
    fieldPSU_ON.setShouldAllowNull(true);
    table.addField(fieldPSU_ON);
    
    FieldDefinition fieldPSU_SERIAL = new FieldDefinition();
    fieldPSU_SERIAL.setName("PSU_SERIAL");
    fieldPSU_SERIAL.setTypeName("VARCHAR2");
    fieldPSU_SERIAL.setSize(32);
    fieldPSU_SERIAL.setSubSize(0);
    fieldPSU_SERIAL.setIsPrimaryKey(false);
    fieldPSU_SERIAL.setIsIdentity(false);
    fieldPSU_SERIAL.setUnique(false);
    fieldPSU_SERIAL.setShouldAllowNull(true);
    table.addField(fieldPSU_SERIAL);
    
    FieldDefinition fieldUPDATED = new FieldDefinition();
    fieldUPDATED.setName("UPDATED");
    fieldUPDATED.setTypeName("TIMESTAMP");
    fieldUPDATED.setSize(0);
    fieldUPDATED.setSubSize(0);
    fieldUPDATED.setIsPrimaryKey(false);
    fieldUPDATED.setIsIdentity(false);
    fieldUPDATED.setUnique(false);
    fieldUPDATED.setShouldAllowNull(true);
    table.addField(fieldUPDATED);
    
    FieldDefinition fieldVERSION = new FieldDefinition();
    fieldVERSION.setName("VERSION");
    fieldVERSION.setTypeName("NUMBER");
    fieldVERSION.setSize(0);
    fieldVERSION.setSubSize(0);
    fieldVERSION.setIsPrimaryKey(false);
    fieldVERSION.setIsIdentity(false);
    fieldVERSION.setUnique(false);
    fieldVERSION.setShouldAllowNull(true);
    table.addField(fieldVERSION);
    
    ForeignKeyConstraint foreignKeyOL_CONSOLE_OL_CAMERA = new ForeignKeyConstraint();
    foreignKeyOL_CONSOLE_OL_CAMERA.setName("OL_CONSOLE_OL_CAMERA");
    foreignKeyOL_CONSOLE_OL_CAMERA.setTargetTable("OL_CAMERA");
    foreignKeyOL_CONSOLE_OL_CAMERA.addSourceField("CAMERA_ID");
    foreignKeyOL_CONSOLE_OL_CAMERA.addTargetField("ID");
    table.addForeignKeyConstraint(foreignKeyOL_CONSOLE_OL_CAMERA);
    
    return table;
}

public TableDefinition buildOL_CONSOLE_OL_GAMERTable() {
    TableDefinition table = new TableDefinition();
    table.setName("OL_CONSOLE_OL_GAMER");
    
    FieldDefinition fieldCONSOLE_ID = new FieldDefinition();
    fieldCONSOLE_ID.setName("CONSOLE_ID");
    fieldCONSOLE_ID.setTypeName("NUMBER");
    fieldCONSOLE_ID.setSize(0);
    fieldCONSOLE_ID.setSubSize(0);
    fieldCONSOLE_ID.setIsPrimaryKey(false);
    fieldCONSOLE_ID.setIsIdentity(false);
    fieldCONSOLE_ID.setUnique(false);
    fieldCONSOLE_ID.setShouldAllowNull(false);
    table.addField(fieldCONSOLE_ID);
    
    FieldDefinition fieldGAMER_ID = new FieldDefinition();
    fieldGAMER_ID.setName("GAMER_ID");
    fieldGAMER_ID.setTypeName("NUMBER");
    fieldGAMER_ID.setSize(0);
    fieldGAMER_ID.setSubSize(0);
    fieldGAMER_ID.setIsPrimaryKey(false);
    fieldGAMER_ID.setIsIdentity(false);
    fieldGAMER_ID.setUnique(false);
    fieldGAMER_ID.setShouldAllowNull(false);
    table.addField(fieldGAMER_ID);
    
    return table;
}

public TableDefinition buildOL_CONTROLLERTable() {
    TableDefinition table = new TableDefinition();
    table.setName("OL_CONTROLLER");
    
    FieldDefinition fieldDESCRIPTION = new FieldDefinition();
    fieldDESCRIPTION.setName("DESCRIPTION");
    fieldDESCRIPTION.setTypeName("VARCHAR2");
    fieldDESCRIPTION.setSize(32);
    fieldDESCRIPTION.setSubSize(0);
    fieldDESCRIPTION.setIsPrimaryKey(false);
    fieldDESCRIPTION.setIsIdentity(false);
    fieldDESCRIPTION.setUnique(false);
    fieldDESCRIPTION.setShouldAllowNull(true);
    table.addField(fieldDESCRIPTION);
    
    FieldDefinition fieldCONSOLE_ID = new FieldDefinition();
    fieldCONSOLE_ID.setName("CONSOLE_ID");
    fieldCONSOLE_ID.setTypeName("NUMBER");
    fieldCONSOLE_ID.setSize(0);
    fieldCONSOLE_ID.setSubSize(0);
    fieldCONSOLE_ID.setIsPrimaryKey(false);
    fieldCONSOLE_ID.setIsIdentity(false);
    fieldCONSOLE_ID.setUnique(false);
    fieldCONSOLE_ID.setShouldAllowNull(true);
    table.addField(fieldCONSOLE_ID);
    
    FieldDefinition fieldID = new FieldDefinition();
    fieldID.setName("ID");
    fieldID.setTypeName("NUMBER");
    fieldID.setSize(0);
    fieldID.setSubSize(0);
    fieldID.setIsPrimaryKey(true);
    fieldID.setIsIdentity(false);
    fieldID.setUnique(false);
    fieldID.setShouldAllowNull(false);
    table.addField(fieldID);
    
    FieldDefinition fieldNAME = new FieldDefinition();
    fieldNAME.setName("NAME");
    fieldNAME.setTypeName("VARCHAR2");
    fieldNAME.setSize(32);
    fieldNAME.setSubSize(0);
    fieldNAME.setIsPrimaryKey(false);
    fieldNAME.setIsIdentity(false);
    fieldNAME.setUnique(false);
    fieldNAME.setShouldAllowNull(true);
    table.addField(fieldNAME);
    
    FieldDefinition fieldUPDATED = new FieldDefinition();
    fieldUPDATED.setName("UPDATED");
    fieldUPDATED.setTypeName("TIMESTAMP");
    fieldUPDATED.setSize(0);
    fieldUPDATED.setSubSize(0);
    fieldUPDATED.setIsPrimaryKey(false);
    fieldUPDATED.setIsIdentity(false);
    fieldUPDATED.setUnique(false);
    fieldUPDATED.setShouldAllowNull(true);
    table.addField(fieldUPDATED);
    
    FieldDefinition fieldVERSION = new FieldDefinition();
    fieldVERSION.setName("VERSION");
    fieldVERSION.setTypeName("NUMBER");
    fieldVERSION.setSize(0);
    fieldVERSION.setSubSize(0);
    fieldVERSION.setIsPrimaryKey(false);
    fieldVERSION.setIsIdentity(false);
    fieldVERSION.setUnique(false);
    fieldVERSION.setShouldAllowNull(true);
    table.addField(fieldVERSION);
    
    ForeignKeyConstraint foreignKeyOL_CONTROLLER_OL_CONSOLE = new ForeignKeyConstraint();
    foreignKeyOL_CONTROLLER_OL_CONSOLE.setName("OL_CONTROLLER_OL_CONSOLE");
    foreignKeyOL_CONTROLLER_OL_CONSOLE.setTargetTable("OL_CONSOLE");
    foreignKeyOL_CONTROLLER_OL_CONSOLE.addSourceField("CONSOLE_ID");
    foreignKeyOL_CONTROLLER_OL_CONSOLE.addTargetField("ID");
    table.addForeignKeyConstraint(foreignKeyOL_CONTROLLER_OL_CONSOLE);
    
    return table;
}

public TableDefinition buildOL_GAMERTable() {
    TableDefinition table = new TableDefinition();
    table.setName("OL_GAMER");
    
    FieldDefinition fieldID = new FieldDefinition();
    fieldID.setName("ID");
    fieldID.setTypeName("NUMBER");
    fieldID.setSize(0);
    fieldID.setSubSize(0);
    fieldID.setIsPrimaryKey(true);
    fieldID.setIsIdentity(false);
    fieldID.setUnique(false);
    fieldID.setShouldAllowNull(false);
    table.addField(fieldID);
    
    FieldDefinition fieldNAME = new FieldDefinition();
    fieldNAME.setName("NAME");
    fieldNAME.setTypeName("VARCHAR2");
    fieldNAME.setSize(32);
    fieldNAME.setSubSize(0);
    fieldNAME.setIsPrimaryKey(false);
    fieldNAME.setIsIdentity(false);
    fieldNAME.setUnique(false);
    fieldNAME.setShouldAllowNull(true);
    table.addField(fieldNAME);
    
    FieldDefinition fieldDESCRIPTION = new FieldDefinition();
    fieldDESCRIPTION.setName("DESCRIPTION");
    fieldDESCRIPTION.setTypeName("VARCHAR2");
    fieldDESCRIPTION.setSize(32);
    fieldDESCRIPTION.setSubSize(0);
    fieldDESCRIPTION.setIsPrimaryKey(false);
    fieldDESCRIPTION.setIsIdentity(false);
    fieldDESCRIPTION.setUnique(false);
    fieldDESCRIPTION.setShouldAllowNull(true);
    table.addField(fieldDESCRIPTION);
    
    FieldDefinition fieldVERSION = new FieldDefinition();
    fieldVERSION.setName("VERSION");
    fieldVERSION.setTypeName("NUMBER");
    fieldVERSION.setSize(0);
    fieldVERSION.setSubSize(0);
    fieldVERSION.setIsPrimaryKey(false);
    fieldVERSION.setIsIdentity(false);
    fieldVERSION.setUnique(false);
    fieldVERSION.setShouldAllowNull(true);
    table.addField(fieldVERSION);
    
    FieldDefinition fieldUPDATED = new FieldDefinition();
    fieldUPDATED.setName("UPDATED");
    fieldUPDATED.setTypeName("TIMESTAMP");
    fieldUPDATED.setSize(0);
    fieldUPDATED.setSubSize(0);
    fieldUPDATED.setIsPrimaryKey(false);
    fieldUPDATED.setIsIdentity(false);
    fieldUPDATED.setUnique(false);
    fieldUPDATED.setShouldAllowNull(true);
    table.addField(fieldUPDATED);
    
    FieldDefinition fieldSKILL_INDICATOR = new FieldDefinition();
    fieldSKILL_INDICATOR.setName("SKILL_INDICATOR");
    fieldSKILL_INDICATOR.setTypeName("VARCHAR2");
    fieldSKILL_INDICATOR.setSize(32);
    fieldSKILL_INDICATOR.setSubSize(0);
    fieldSKILL_INDICATOR.setIsPrimaryKey(false);
    fieldSKILL_INDICATOR.setIsIdentity(false);
    fieldSKILL_INDICATOR.setUnique(false);
    fieldSKILL_INDICATOR.setShouldAllowNull(true);
    table.addField(fieldSKILL_INDICATOR);
    
    FieldDefinition fieldSKILL_ID = new FieldDefinition();
    fieldSKILL_ID.setName("SKILL_ID");
    fieldSKILL_ID.setTypeName("NUMBER");
    fieldSKILL_ID.setSize(0);
    fieldSKILL_ID.setSubSize(0);
    fieldSKILL_ID.setIsPrimaryKey(false);
    fieldSKILL_ID.setIsIdentity(false);
    fieldSKILL_ID.setUnique(false);
    fieldSKILL_ID.setShouldAllowNull(true);
    table.addField(fieldSKILL_ID);
    
    ForeignKeyConstraint foreignKeyOL_GAMER_OL_SKILL = new ForeignKeyConstraint();
    foreignKeyOL_GAMER_OL_SKILL.setName("OL_GAMER_OL_SKILL");
    foreignKeyOL_GAMER_OL_SKILL.setTargetTable("OL_SKILL");
    foreignKeyOL_GAMER_OL_SKILL.addSourceField("SKILL_ID");
    foreignKeyOL_GAMER_OL_SKILL.addTargetField("ID");
    table.addForeignKeyConstraint(foreignKeyOL_GAMER_OL_SKILL);
    
    return table;
}

public TableDefinition buildOL_SKILLTable() {
    TableDefinition table = new TableDefinition();
    table.setName("OL_SKILL");
    
    FieldDefinition fieldID = new FieldDefinition();
    fieldID.setName("ID");
    fieldID.setTypeName("NUMBER");
    fieldID.setSize(0);
    fieldID.setSubSize(0);
    fieldID.setIsPrimaryKey(true);
    fieldID.setIsIdentity(false);
    fieldID.setUnique(false);
    fieldID.setShouldAllowNull(false);
    table.addField(fieldID);
    
    FieldDefinition fieldNAME = new FieldDefinition();
    fieldNAME.setName("NAME");
    fieldNAME.setTypeName("VARCHAR2");
    fieldNAME.setSize(32);
    fieldNAME.setSubSize(0);
    fieldNAME.setIsPrimaryKey(false);
    fieldNAME.setIsIdentity(false);
    fieldNAME.setUnique(false);
    fieldNAME.setShouldAllowNull(true);
    table.addField(fieldNAME);
    
    FieldDefinition fieldVERSION = new FieldDefinition();
    fieldVERSION.setName("VERSION");
    fieldVERSION.setTypeName("NUMBER");
    fieldVERSION.setSize(0);
    fieldVERSION.setSubSize(0);
    fieldVERSION.setIsPrimaryKey(false);
    fieldVERSION.setIsIdentity(false);
    fieldVERSION.setUnique(false);
    fieldVERSION.setShouldAllowNull(true);
    table.addField(fieldVERSION);
    
    FieldDefinition fieldDESCRIPTION = new FieldDefinition();
    fieldDESCRIPTION.setName("DESCRIPTION");
    fieldDESCRIPTION.setTypeName("VARCHAR2");
    fieldDESCRIPTION.setSize(32);
    fieldDESCRIPTION.setSubSize(0);
    fieldDESCRIPTION.setIsPrimaryKey(false);
    fieldDESCRIPTION.setIsIdentity(false);
    fieldDESCRIPTION.setUnique(false);
    fieldDESCRIPTION.setShouldAllowNull(true);
    table.addField(fieldDESCRIPTION);
    
    FieldDefinition fieldUPDATED = new FieldDefinition();
    fieldUPDATED.setName("UPDATED");
    fieldUPDATED.setTypeName("TIMESTAMP");
    fieldUPDATED.setSize(0);
    fieldUPDATED.setSubSize(0);
    fieldUPDATED.setIsPrimaryKey(false);
    fieldUPDATED.setIsIdentity(false);
    fieldUPDATED.setUnique(false);
    fieldUPDATED.setShouldAllowNull(true);
    table.addField(fieldUPDATED);
    
    return table;
}

}
