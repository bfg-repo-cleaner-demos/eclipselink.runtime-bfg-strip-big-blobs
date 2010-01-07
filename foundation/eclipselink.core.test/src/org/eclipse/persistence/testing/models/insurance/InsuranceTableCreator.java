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
package org.eclipse.persistence.testing.models.insurance;

import org.eclipse.persistence.tools.schemaframework.*;

/**
 * This class was generated by the TopLink table creator generator.
 * It stores the meta-data (tables) that define the database schema.
 * @see org.eclipse.persistence.sessions.factories.TableCreatorClassGenerator
 */
public class InsuranceTableCreator extends TableCreator {
    public InsuranceTableCreator() {
        setName("Insurance");

        addTableDefinition(buildCHILDNAMTable());
        addTableDefinition(buildCLAIMTable());
        addTableDefinition(buildHOLDERTable());
        addTableDefinition(buildINS_ADDRTable());
        addTableDefinition(buildINS_PHONETable());
        addTableDefinition(buildPOLICYTable());
        addTableDefinition(buildVHCL_CLMTable());
        addTableDefinition(buildVHCL_POLTable());
    }

    public TableDefinition buildCHILDNAMTable() {
        TableDefinition table = new TableDefinition();
        table.setName("CHILDNAM");

        FieldDefinition fieldHOLDER_ID = new FieldDefinition();
        fieldHOLDER_ID.setName("HOLDER_ID");
        fieldHOLDER_ID.setTypeName("NUMBER");
        fieldHOLDER_ID.setSize(18);
        fieldHOLDER_ID.setSubSize(0);
        fieldHOLDER_ID.setIsPrimaryKey(true);
        fieldHOLDER_ID.setIsIdentity(false);
        fieldHOLDER_ID.setUnique(false);
        fieldHOLDER_ID.setShouldAllowNull(false);
        table.addField(fieldHOLDER_ID);

        FieldDefinition fieldCHILD_NAME = new FieldDefinition();
        fieldCHILD_NAME.setName("CHILD_NAME");
        fieldCHILD_NAME.setTypeName("VARCHAR2");
        fieldCHILD_NAME.setSize(30);
        fieldCHILD_NAME.setSubSize(0);
        fieldCHILD_NAME.setIsPrimaryKey(true);
        fieldCHILD_NAME.setIsIdentity(false);
        fieldCHILD_NAME.setUnique(false);
        fieldCHILD_NAME.setShouldAllowNull(false);
        table.addField(fieldCHILD_NAME);

        ForeignKeyConstraint foreignKeyFK_CHILDNAM_HOLDER_ID = new ForeignKeyConstraint();
        foreignKeyFK_CHILDNAM_HOLDER_ID.setName("CHILD_HOLDER");
        foreignKeyFK_CHILDNAM_HOLDER_ID.setTargetTable("HOLDER");
        foreignKeyFK_CHILDNAM_HOLDER_ID.addSourceField("HOLDER_ID");
        foreignKeyFK_CHILDNAM_HOLDER_ID.addTargetField("SSN");
        table.addForeignKeyConstraint(foreignKeyFK_CHILDNAM_HOLDER_ID);

        return table;
    }

    public TableDefinition buildCLAIMTable() {
        TableDefinition table = new TableDefinition();
        table.setName("CLAIM");

        FieldDefinition fieldCLM_ID = new FieldDefinition();
        fieldCLM_ID.setName("CLM_ID");
        fieldCLM_ID.setTypeName("NUMBER");
        fieldCLM_ID.setSize(18);
        fieldCLM_ID.setSubSize(0);
        fieldCLM_ID.setIsPrimaryKey(true);
        fieldCLM_ID.setIsIdentity(false);
        fieldCLM_ID.setUnique(false);
        fieldCLM_ID.setShouldAllowNull(false);
        table.addField(fieldCLM_ID);

        FieldDefinition fieldPOL_ID = new FieldDefinition();
        fieldPOL_ID.setName("POL_ID");
        fieldPOL_ID.setTypeName("NUMBER");
        fieldPOL_ID.setSize(18);
        fieldPOL_ID.setSubSize(0);
        fieldPOL_ID.setIsPrimaryKey(false);
        fieldPOL_ID.setIsIdentity(false);
        fieldPOL_ID.setUnique(false);
        fieldPOL_ID.setShouldAllowNull(true);
        table.addField(fieldPOL_ID);

        FieldDefinition fieldCLM_TYPE = new FieldDefinition();
        fieldCLM_TYPE.setName("CLM_TYPE");
        fieldCLM_TYPE.setTypeName("VARCHAR2");
        fieldCLM_TYPE.setSize(20);
        fieldCLM_TYPE.setSubSize(0);
        fieldCLM_TYPE.setIsPrimaryKey(false);
        fieldCLM_TYPE.setIsIdentity(false);
        fieldCLM_TYPE.setUnique(false);
        fieldCLM_TYPE.setShouldAllowNull(true);
        table.addField(fieldCLM_TYPE);

        FieldDefinition fieldAMOUNT = new FieldDefinition();
        fieldAMOUNT.setName("AMOUNT");
        fieldAMOUNT.setTypeName("FLOAT");
        fieldAMOUNT.setSize(18);
        fieldAMOUNT.setSubSize(4);
        fieldAMOUNT.setIsPrimaryKey(false);
        fieldAMOUNT.setIsIdentity(false);
        fieldAMOUNT.setUnique(false);
        fieldAMOUNT.setShouldAllowNull(true);
        table.addField(fieldAMOUNT);

        FieldDefinition fieldDISEASE = new FieldDefinition();
        fieldDISEASE.setName("DISEASE");
        fieldDISEASE.setTypeName("VARCHAR2");
        fieldDISEASE.setSize(50);
        fieldDISEASE.setSubSize(0);
        fieldDISEASE.setIsPrimaryKey(false);
        fieldDISEASE.setIsIdentity(false);
        fieldDISEASE.setUnique(false);
        fieldDISEASE.setShouldAllowNull(true);
        table.addField(fieldDISEASE);

        FieldDefinition fieldAREA = new FieldDefinition();
        fieldAREA.setName("AREA");
        fieldAREA.setTypeName("NUMBER");
        fieldAREA.setSize(18);
        fieldAREA.setSubSize(4);
        fieldAREA.setIsPrimaryKey(false);
        fieldAREA.setIsIdentity(false);
        fieldAREA.setUnique(false);
        fieldAREA.setShouldAllowNull(true);
        table.addField(fieldAREA);

        ForeignKeyConstraint foreignKeyFK_CLAIM_POL_ID = new ForeignKeyConstraint();
        foreignKeyFK_CLAIM_POL_ID.setName("CLAIM_POL");
        foreignKeyFK_CLAIM_POL_ID.setTargetTable("POLICY");
        foreignKeyFK_CLAIM_POL_ID.addSourceField("POL_ID");
        foreignKeyFK_CLAIM_POL_ID.addTargetField("POL_ID");
        table.addForeignKeyConstraint(foreignKeyFK_CLAIM_POL_ID);

        return table;
    }

    public TableDefinition buildHOLDERTable() {
        TableDefinition table = new TableDefinition();
        table.setName("HOLDER");

        FieldDefinition fieldSSN = new FieldDefinition();
        fieldSSN.setName("SSN");
        fieldSSN.setTypeName("NUMBER");
        fieldSSN.setSize(18);
        fieldSSN.setSubSize(0);
        fieldSSN.setIsPrimaryKey(true);
        fieldSSN.setIsIdentity(false);
        fieldSSN.setUnique(false);
        fieldSSN.setShouldAllowNull(false);
        table.addField(fieldSSN);

        FieldDefinition fieldF_NAME = new FieldDefinition();
        fieldF_NAME.setName("F_NAME");
        fieldF_NAME.setTypeName("VARCHAR2");
        fieldF_NAME.setSize(20);
        fieldF_NAME.setSubSize(0);
        fieldF_NAME.setIsPrimaryKey(false);
        fieldF_NAME.setIsIdentity(false);
        fieldF_NAME.setUnique(false);
        fieldF_NAME.setShouldAllowNull(true);
        table.addField(fieldF_NAME);

        FieldDefinition fieldL_NAME = new FieldDefinition();
        fieldL_NAME.setName("L_NAME");
        fieldL_NAME.setTypeName("VARCHAR2");
        fieldL_NAME.setSize(20);
        fieldL_NAME.setSubSize(0);
        fieldL_NAME.setIsPrimaryKey(false);
        fieldL_NAME.setIsIdentity(false);
        fieldL_NAME.setUnique(false);
        fieldL_NAME.setShouldAllowNull(true);
        table.addField(fieldL_NAME);

        FieldDefinition fieldSEX = new FieldDefinition();
        fieldSEX.setName("SEX");
        fieldSEX.setTypeName("CHAR");
        fieldSEX.setSize(1);
        fieldSEX.setSubSize(0);
        fieldSEX.setIsPrimaryKey(false);
        fieldSEX.setIsIdentity(false);
        fieldSEX.setUnique(false);
        fieldSEX.setShouldAllowNull(true);
        table.addField(fieldSEX);

        FieldDefinition fieldBDATE = new FieldDefinition();
        fieldBDATE.setName("BDATE");
        fieldBDATE.setTypeName("DATE");
        fieldBDATE.setSize(7);
        fieldBDATE.setSubSize(0);
        fieldBDATE.setIsPrimaryKey(false);
        fieldBDATE.setIsIdentity(false);
        fieldBDATE.setUnique(false);
        fieldBDATE.setShouldAllowNull(true);
        table.addField(fieldBDATE);

        FieldDefinition fieldOCC = new FieldDefinition();
        fieldOCC.setName("OCC");
        fieldOCC.setTypeName("VARCHAR2");
        fieldOCC.setSize(20);
        fieldOCC.setSubSize(0);
        fieldOCC.setIsPrimaryKey(false);
        fieldOCC.setIsIdentity(false);
        fieldOCC.setUnique(false);
        fieldOCC.setShouldAllowNull(true);
        table.addField(fieldOCC);

        return table;
    }

    public TableDefinition buildINS_ADDRTable() {
        TableDefinition table = new TableDefinition();
        table.setName("INS_ADDR");

        FieldDefinition fieldSSN = new FieldDefinition();
        fieldSSN.setName("SSN");
        fieldSSN.setTypeName("NUMBER");
        fieldSSN.setSize(18);
        fieldSSN.setSubSize(0);
        fieldSSN.setIsPrimaryKey(true);
        fieldSSN.setIsIdentity(false);
        fieldSSN.setUnique(false);
        fieldSSN.setShouldAllowNull(false);
        table.addField(fieldSSN);

        FieldDefinition fieldSTREET = new FieldDefinition();
        fieldSTREET.setName("STREET");
        fieldSTREET.setTypeName("VARCHAR2");
        fieldSTREET.setSize(30);
        fieldSTREET.setSubSize(0);
        fieldSTREET.setIsPrimaryKey(false);
        fieldSTREET.setIsIdentity(false);
        fieldSTREET.setUnique(false);
        fieldSTREET.setShouldAllowNull(true);
        table.addField(fieldSTREET);

        FieldDefinition fieldCITY = new FieldDefinition();
        fieldCITY.setName("CITY");
        fieldCITY.setTypeName("VARCHAR2");
        fieldCITY.setSize(25);
        fieldCITY.setSubSize(0);
        fieldCITY.setIsPrimaryKey(false);
        fieldCITY.setIsIdentity(false);
        fieldCITY.setUnique(false);
        fieldCITY.setShouldAllowNull(true);
        table.addField(fieldCITY);

        FieldDefinition fieldSTATE = new FieldDefinition();
        fieldSTATE.setName("STATE");
        fieldSTATE.setTypeName("VARCHAR2");
        fieldSTATE.setSize(2);
        fieldSTATE.setSubSize(0);
        fieldSTATE.setIsPrimaryKey(false);
        fieldSTATE.setIsIdentity(false);
        fieldSTATE.setUnique(false);
        fieldSTATE.setShouldAllowNull(true);
        table.addField(fieldSTATE);

        FieldDefinition fieldCOUNTRY = new FieldDefinition();
        fieldCOUNTRY.setName("COUNTRY");
        fieldCOUNTRY.setTypeName("VARCHAR2");
        fieldCOUNTRY.setSize(20);
        fieldCOUNTRY.setSubSize(0);
        fieldCOUNTRY.setIsPrimaryKey(false);
        fieldCOUNTRY.setIsIdentity(false);
        fieldCOUNTRY.setUnique(false);
        fieldCOUNTRY.setShouldAllowNull(true);
        table.addField(fieldCOUNTRY);

        FieldDefinition fieldZIPCODE = new FieldDefinition();
        fieldZIPCODE.setName("ZIPCODE");
        fieldZIPCODE.setTypeName("VARCHAR2");
        fieldZIPCODE.setSize(10);
        fieldZIPCODE.setSubSize(0);
        fieldZIPCODE.setIsPrimaryKey(false);
        fieldZIPCODE.setIsIdentity(false);
        fieldZIPCODE.setUnique(false);
        fieldZIPCODE.setShouldAllowNull(true);
        table.addField(fieldZIPCODE);

        ForeignKeyConstraint foreignKeyFK_INS_ADDR_SSN = new ForeignKeyConstraint();
        foreignKeyFK_INS_ADDR_SSN.setName("ADDRESS_HOLDER");
        foreignKeyFK_INS_ADDR_SSN.setTargetTable("HOLDER");
        foreignKeyFK_INS_ADDR_SSN.addSourceField("SSN");
        foreignKeyFK_INS_ADDR_SSN.addTargetField("SSN");
        table.addForeignKeyConstraint(foreignKeyFK_INS_ADDR_SSN);

        return table;
    }

    public TableDefinition buildINS_PHONETable() {
        TableDefinition table = new TableDefinition();
        table.setName("INS_PHONE");

        FieldDefinition fieldHOLDER_SSN = new FieldDefinition();
        fieldHOLDER_SSN.setName("HOLDER_SSN");
        fieldHOLDER_SSN.setTypeName("NUMBER");
        fieldHOLDER_SSN.setSize(18);
        fieldHOLDER_SSN.setSubSize(0);
        fieldHOLDER_SSN.setIsPrimaryKey(true);
        fieldHOLDER_SSN.setIsIdentity(false);
        fieldHOLDER_SSN.setUnique(false);
        fieldHOLDER_SSN.setShouldAllowNull(false);
        table.addField(fieldHOLDER_SSN);

        FieldDefinition fieldTYPE = new FieldDefinition();
        fieldTYPE.setName("TYPE");
        fieldTYPE.setTypeName("VARCHAR2");
        fieldTYPE.setSize(10);
        fieldTYPE.setSubSize(0);
        fieldTYPE.setIsPrimaryKey(true);
        fieldTYPE.setIsIdentity(false);
        fieldTYPE.setUnique(false);
        fieldTYPE.setShouldAllowNull(false);
        table.addField(fieldTYPE);

        FieldDefinition fieldAREACODE = new FieldDefinition();
        fieldAREACODE.setName("AREACODE");
        fieldAREACODE.setTypeName("NUMBER");
        fieldAREACODE.setSize(3);
        fieldAREACODE.setSubSize(0);
        fieldAREACODE.setIsPrimaryKey(false);
        fieldAREACODE.setIsIdentity(false);
        fieldAREACODE.setUnique(false);
        fieldAREACODE.setShouldAllowNull(true);
        table.addField(fieldAREACODE);

        FieldDefinition fieldPHONE_NUMBER = new FieldDefinition();
        fieldPHONE_NUMBER.setName("PHONE_NUMBER");
        fieldPHONE_NUMBER.setTypeName("NUMBER");
        fieldPHONE_NUMBER.setSize(10);
        fieldPHONE_NUMBER.setSubSize(0);
        fieldPHONE_NUMBER.setIsPrimaryKey(false);
        fieldPHONE_NUMBER.setIsIdentity(false);
        fieldPHONE_NUMBER.setUnique(false);
        fieldPHONE_NUMBER.setShouldAllowNull(true);
        table.addField(fieldPHONE_NUMBER);

        ForeignKeyConstraint foreignKeyFK_INS_PHONE_HOLDER_SSN = new ForeignKeyConstraint();
        foreignKeyFK_INS_PHONE_HOLDER_SSN.setName("INS_PHONE_HOLDER");
        foreignKeyFK_INS_PHONE_HOLDER_SSN.setTargetTable("HOLDER");
        foreignKeyFK_INS_PHONE_HOLDER_SSN.addSourceField("HOLDER_SSN");
        foreignKeyFK_INS_PHONE_HOLDER_SSN.addTargetField("SSN");
        table.addForeignKeyConstraint(foreignKeyFK_INS_PHONE_HOLDER_SSN);

        return table;
    }

    public TableDefinition buildPOLICYTable() {
        TableDefinition table = new TableDefinition();
        table.setName("POLICY");

        FieldDefinition fieldPOL_ID = new FieldDefinition();
        fieldPOL_ID.setName("POL_ID");
        fieldPOL_ID.setTypeName("NUMBER");
        fieldPOL_ID.setSize(18);
        fieldPOL_ID.setSubSize(0);
        fieldPOL_ID.setIsPrimaryKey(true);
        fieldPOL_ID.setIsIdentity(false);
        fieldPOL_ID.setUnique(false);
        fieldPOL_ID.setShouldAllowNull(false);
        table.addField(fieldPOL_ID);

        FieldDefinition fieldSSN = new FieldDefinition();
        fieldSSN.setName("SSN");
        fieldSSN.setTypeName("NUMBER");
        fieldSSN.setSize(18);
        fieldSSN.setSubSize(0);
        fieldSSN.setIsPrimaryKey(false);
        fieldSSN.setIsIdentity(false);
        fieldSSN.setUnique(false);
        fieldSSN.setShouldAllowNull(true);
        table.addField(fieldSSN);

        FieldDefinition fieldDESCRIPT = new FieldDefinition();
        fieldDESCRIPT.setName("DESCRIPT");
        fieldDESCRIPT.setTypeName("VARCHAR2");
        fieldDESCRIPT.setSize(100);
        fieldDESCRIPT.setSubSize(0);
        fieldDESCRIPT.setIsPrimaryKey(false);
        fieldDESCRIPT.setIsIdentity(false);
        fieldDESCRIPT.setUnique(false);
        fieldDESCRIPT.setShouldAllowNull(true);
        table.addField(fieldDESCRIPT);

        FieldDefinition fieldPOL_TYPE = new FieldDefinition();
        fieldPOL_TYPE.setName("POL_TYPE");
        fieldPOL_TYPE.setTypeName("NUMBER");
        fieldPOL_TYPE.setSize(1);
        fieldPOL_TYPE.setSubSize(0);
        fieldPOL_TYPE.setIsPrimaryKey(false);
        fieldPOL_TYPE.setIsIdentity(false);
        fieldPOL_TYPE.setUnique(false);
        fieldPOL_TYPE.setShouldAllowNull(true);
        table.addField(fieldPOL_TYPE);

        FieldDefinition fieldMAX_COV = new FieldDefinition();
        fieldMAX_COV.setName("MAX_COV");
        fieldMAX_COV.setTypeName("NUMBER");
        fieldMAX_COV.setSize(18);
        fieldMAX_COV.setSubSize(4);
        fieldMAX_COV.setIsPrimaryKey(false);
        fieldMAX_COV.setIsIdentity(false);
        fieldMAX_COV.setUnique(false);
        fieldMAX_COV.setShouldAllowNull(true);
        table.addField(fieldMAX_COV);

        FieldDefinition fieldCOV_RATE = new FieldDefinition();
        fieldCOV_RATE.setName("COV_RATE");
        //NUMBER corresponds to long which is mapped to INTEGER data type in some database such as DB2 and MySQL.  
        //FLOAT should be used which reflect float java type.
        fieldCOV_RATE.setTypeName("FLOAT(16)");
        fieldCOV_RATE.setSize(18);
        fieldCOV_RATE.setSubSize(4);
        fieldCOV_RATE.setIsPrimaryKey(false);
        fieldCOV_RATE.setIsIdentity(false);
        fieldCOV_RATE.setUnique(false);
        fieldCOV_RATE.setShouldAllowNull(true);
        table.addField(fieldCOV_RATE);

        FieldDefinition fieldCNST_DTE = new FieldDefinition();
        fieldCNST_DTE.setName("CNST_DTE");
        fieldCNST_DTE.setTypeName("DATE");
        fieldCNST_DTE.setSize(7);
        fieldCNST_DTE.setSubSize(0);
        fieldCNST_DTE.setIsPrimaryKey(false);
        fieldCNST_DTE.setIsIdentity(false);
        fieldCNST_DTE.setUnique(false);
        fieldCNST_DTE.setShouldAllowNull(true);
        table.addField(fieldCNST_DTE);

        ForeignKeyConstraint foreignKeyFK_POLICY_SSN = new ForeignKeyConstraint();
        foreignKeyFK_POLICY_SSN.setName("POLICY_HOLDER");
        foreignKeyFK_POLICY_SSN.setTargetTable("HOLDER");
        foreignKeyFK_POLICY_SSN.addSourceField("SSN");
        foreignKeyFK_POLICY_SSN.addTargetField("SSN");
        table.addForeignKeyConstraint(foreignKeyFK_POLICY_SSN);

        return table;
    }

    public TableDefinition buildVHCL_CLMTable() {
        TableDefinition table = new TableDefinition();
        table.setName("VHCL_CLM");

        FieldDefinition fieldCLM_ID = new FieldDefinition();
        fieldCLM_ID.setName("CLM_ID");
        fieldCLM_ID.setTypeName("NUMBER");
        fieldCLM_ID.setSize(18);
        fieldCLM_ID.setSubSize(0);
        fieldCLM_ID.setIsPrimaryKey(true);
        fieldCLM_ID.setIsIdentity(false);
        fieldCLM_ID.setUnique(false);
        fieldCLM_ID.setShouldAllowNull(false);
        table.addField(fieldCLM_ID);

        FieldDefinition fieldPART = new FieldDefinition();
        fieldPART.setName("PART");
        fieldPART.setTypeName("VARCHAR2");
        fieldPART.setSize(30);
        fieldPART.setSubSize(0);
        fieldPART.setIsPrimaryKey(false);
        fieldPART.setIsIdentity(false);
        fieldPART.setUnique(false);
        fieldPART.setShouldAllowNull(true);
        table.addField(fieldPART);

        FieldDefinition fieldPART_DESC = new FieldDefinition();
        fieldPART_DESC.setName("PART_DESC");
        fieldPART_DESC.setTypeName("VARCHAR2");
        fieldPART_DESC.setSize(30);
        fieldPART_DESC.setSubSize(0);
        fieldPART_DESC.setIsPrimaryKey(false);
        fieldPART_DESC.setIsIdentity(false);
        fieldPART_DESC.setUnique(false);
        fieldPART_DESC.setShouldAllowNull(true);
        table.addField(fieldPART_DESC);

        ForeignKeyConstraint foreignKeyFK_VHCL_CLM_CLM_ID = new ForeignKeyConstraint();
        foreignKeyFK_VHCL_CLM_CLM_ID.setName("VHCL_CLM_CLAIM");
        foreignKeyFK_VHCL_CLM_CLM_ID.setTargetTable("CLAIM");
        foreignKeyFK_VHCL_CLM_CLM_ID.addSourceField("CLM_ID");
        foreignKeyFK_VHCL_CLM_CLM_ID.addTargetField("CLM_ID");
        table.addForeignKeyConstraint(foreignKeyFK_VHCL_CLM_CLM_ID);

        return table;
    }

    public TableDefinition buildVHCL_POLTable() {
        TableDefinition table = new TableDefinition();
        table.setName("VHCL_POL");

        FieldDefinition fieldPOL_ID = new FieldDefinition();
        fieldPOL_ID.setName("POL_ID");
        fieldPOL_ID.setTypeName("NUMBER");
        fieldPOL_ID.setSize(18);
        fieldPOL_ID.setSubSize(0);
        fieldPOL_ID.setIsPrimaryKey(true);
        fieldPOL_ID.setIsIdentity(false);
        fieldPOL_ID.setUnique(false);
        fieldPOL_ID.setShouldAllowNull(false);
        table.addField(fieldPOL_ID);

        FieldDefinition fieldMODEL = new FieldDefinition();
        fieldMODEL.setName("MODEL");
        fieldMODEL.setTypeName("VARCHAR2");
        fieldMODEL.setSize(30);
        fieldMODEL.setSubSize(0);
        fieldMODEL.setIsPrimaryKey(false);
        fieldMODEL.setIsIdentity(false);
        fieldMODEL.setUnique(false);
        fieldMODEL.setShouldAllowNull(true);
        table.addField(fieldMODEL);

        ForeignKeyConstraint foreignKeyFK_VHCL_POL_POL_ID = new ForeignKeyConstraint();
        foreignKeyFK_VHCL_POL_POL_ID.setName("VHCL_POL_POLICY");
        foreignKeyFK_VHCL_POL_POL_ID.setTargetTable("POLICY");
        foreignKeyFK_VHCL_POL_POL_ID.addSourceField("POL_ID");
        foreignKeyFK_VHCL_POL_POL_ID.addTargetField("POL_ID");
        table.addForeignKeyConstraint(foreignKeyFK_VHCL_POL_POL_ID);

        return table;
    }
}
