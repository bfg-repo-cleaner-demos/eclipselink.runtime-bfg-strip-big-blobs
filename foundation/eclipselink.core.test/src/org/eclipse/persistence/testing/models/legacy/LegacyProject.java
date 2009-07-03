/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.models.legacy;

import java.util.*;
import org.eclipse.persistence.expressions.*;
import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.mappings.*;

/**
 * TopLink generated Project class.
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
public class LegacyProject extends org.eclipse.persistence.sessions.Project {

    /**
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    public LegacyProject() {
        applyPROJECT();
        applyLOGIN();
        buildComputerDescriptor();
        buildEmployeeDescriptor();
        buildGaurenteedShipmentDescriptor();
        buildInsuredShipmentDescriptor();
        buildOrderDescriptor();
        buildShipmentDescriptor();
    }

    public static void amendEmployeeDescriptor(org.eclipse.persistence.descriptors.RelationalDescriptor descriptor) {
        descriptor.addForeignKeyFieldNameForMultipleTable("LEG_ADD.FIRST_NM", "LEG_EMP.FNAME");
        descriptor.addForeignKeyFieldNameForMultipleTable("LEG_ADD.LNAME", "LEG_EMP.LNAME");
    }

    public static void amendInsuredShipmentDescriptor(org.eclipse.persistence.descriptors.RelationalDescriptor descriptor) {
        descriptor.addForeignKeyFieldNameForMultipleTable("LEG_ISHP.F_NAME", "LEG_SHP.FNAME");
        descriptor.addForeignKeyFieldNameForMultipleTable("LEG_ISHP.L_NAME", "LEG_SHP.LNAME");
        descriptor.addForeignKeyFieldNameForMultipleTable("LEG_ISHP.SHIP_NO", "LEG_SHP.SHIP_NO");

        org.eclipse.persistence.expressions.ExpressionBuilder builder = new org.eclipse.persistence.expressions.ExpressionBuilder();
        descriptor.getDescriptorQueryManager().setMultipleTableJoinExpression((builder.getField("LEG_ISHP.F_NAME").equal(builder.getField("LEG_SHP.FNAME"))).and(builder.getField("LEG_ISHP.L_NAME").equal(builder.getField("LEG_SHP.LNAME"))).and(builder.getField("LEG_ISHP.SHIP_NO").equal(builder.getField("LEG_SHP.SHIP_NO"))));
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void applyLOGIN() {
        org.eclipse.persistence.sessions.DatabaseLogin login = new org.eclipse.persistence.sessions.DatabaseLogin();
        setLogin(login);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void applyPROJECT() {
        setName("Complex Legacy Model");
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildComputerDescriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.legacy.Computer.class);
        Vector vector = new Vector();
        vector.addElement("LEG_COM");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("LEG_COM.CREATE_TS");
        descriptor.addPrimaryKeyFieldName("LEG_COM.CREATE_TSM");

        // SECTION: EVENT MANAGER
        descriptor.getDescriptorEventManager().setPreInsertSelector("prepareForInsert");

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("creationTimestamp");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("LEG_COM.CREATE_TS");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("creationTimestampMillis");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("LEG_COM.CREATE_TSM");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("description");
        directtofieldmapping2.setIsReadOnly(false);
        if (LegacyTables.computerDescriptionFieldName == null) {
            directtofieldmapping2.setFieldName("LEG_COM.DESCRIP");
        } else {
            directtofieldmapping2.setFieldName("LEG_COM." + LegacyTables.computerDescriptionFieldName);
        }

        descriptor.addMapping(directtofieldmapping2);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("employee");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(false);
        onetoonemapping.setReferenceClass(org.eclipse.persistence.testing.models.legacy.Employee.class);
        onetoonemapping.setIsPrivateOwned(false);
        onetoonemapping.addForeignKeyFieldName("LEG_COM.EMP_FNAME", "LEG_EMP.FNAME");
        onetoonemapping.addForeignKeyFieldName("LEG_COM.EMP_LNAME", "LEG_EMP.LNAME");
        descriptor.addMapping(onetoonemapping);
        addDescriptor(descriptor);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildEmployeeDescriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.legacy.Employee.class);
        Vector vector = new Vector();
        vector.addElement("LEG_EMP");
        vector.addElement("LEG_ADD");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("LEG_EMP.FNAME");
        descriptor.addPrimaryKeyFieldName("LEG_EMP.LNAME");

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("address");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("LEG_ADD.ADDR");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("firstName");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("LEG_EMP.FNAME");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("lastName");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setFieldName("LEG_EMP.LNAME");
        descriptor.addMapping(directtofieldmapping2);

        // SECTION: ONETOMANYMAPPING
        org.eclipse.persistence.mappings.OneToManyMapping onetomanymapping = new org.eclipse.persistence.mappings.OneToManyMapping();
        onetomanymapping.setAttributeName("shipments");
        onetomanymapping.setIsReadOnly(false);
        onetomanymapping.setUsesIndirection(false);
        onetomanymapping.setReferenceClass(org.eclipse.persistence.testing.models.legacy.Shipment.class);
        onetomanymapping.setIsPrivateOwned(true);
        onetomanymapping.addTargetForeignKeyFieldName("LEG_SHP.FNAME", "LEG_EMP.FNAME");
        onetomanymapping.addTargetForeignKeyFieldName("LEG_SHP.LNAME", "LEG_EMP.LNAME");
        descriptor.addMapping(onetomanymapping);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("computer");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(false);
        onetoonemapping.setReferenceClass(org.eclipse.persistence.testing.models.legacy.Computer.class);
        onetoonemapping.setIsPrivateOwned(true);
        onetoonemapping.addTargetForeignKeyFieldName("LEG_COM.EMP_FNAME", "LEG_EMP.FNAME");
        onetoonemapping.addTargetForeignKeyFieldName("LEG_COM.EMP_LNAME", "LEG_EMP.LNAME");
        descriptor.addMapping(onetoonemapping);

        org.eclipse.persistence.testing.models.legacy.LegacyProject.amendEmployeeDescriptor(descriptor);
        addDescriptor(descriptor);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildGaurenteedShipmentDescriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.legacy.GaurenteedShipment.class);
        descriptor.getDescriptorInheritancePolicy().setParentClass(org.eclipse.persistence.testing.models.legacy.InsuredShipment.class);
        Vector vector = new Vector();
        vector.addElement("LEG_ISHP");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("LEG_ISHP.F_NAME");
        descriptor.addPrimaryKeyFieldName("LEG_ISHP.L_NAME");
        descriptor.addPrimaryKeyFieldName("LEG_ISHP.SHIP_NO");

        // SECTION: PROPERTIES
        descriptor.getDescriptorInheritancePolicy().setShouldReadSubclasses(true);

        addDescriptor(descriptor);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildInsuredShipmentDescriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.legacy.InsuredShipment.class);
        descriptor.getDescriptorInheritancePolicy().setParentClass(org.eclipse.persistence.testing.models.legacy.Shipment.class);
        Vector vector = new Vector();
        vector.addElement("LEG_ISHP");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("LEG_ISHP.F_NAME");
        descriptor.addPrimaryKeyFieldName("LEG_ISHP.L_NAME");
        descriptor.addPrimaryKeyFieldName("LEG_ISHP.SHIP_NO");

        // SECTION: PROPERTIES
        descriptor.getDescriptorInheritancePolicy().setShouldReadSubclasses(true);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("employeeFirstName");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("LEG_ISHP.F_NAME");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("employeeLastName");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("LEG_ISHP.L_NAME");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("insuranceAmount");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setFieldName("LEG_ISHP.INS_AMT");
        descriptor.addMapping(directtofieldmapping2);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping3 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping3.setAttributeName("shipmentNumber");
        directtofieldmapping3.setIsReadOnly(false);
        directtofieldmapping3.setFieldName("LEG_ISHP.SHIP_NO");
        descriptor.addMapping(directtofieldmapping3);

        org.eclipse.persistence.testing.models.legacy.LegacyProject.amendInsuredShipmentDescriptor(descriptor);
        addDescriptor(descriptor);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildOrderDescriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.legacy.Order.class);
        Vector vector = new Vector();
        vector.addElement("LEG_ORD");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("LEG_ORD.FNAME");
        descriptor.addPrimaryKeyFieldName("LEG_ORD.LNAME");
        descriptor.addPrimaryKeyFieldName("LEG_ORD.ORDER_NO");
        descriptor.addPrimaryKeyFieldName("LEG_ORD.SHIP_NO");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("description");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("LEG_ORD.DESCRIP");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("employeeFirstName");
        directtofieldmapping1.setIsReadOnly(true);
        directtofieldmapping1.setFieldName("LEG_ORD.FNAME");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("employeeLastName");
        directtofieldmapping2.setIsReadOnly(true);
        directtofieldmapping2.setFieldName("LEG_ORD.LNAME");
        descriptor.addMapping(directtofieldmapping2);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping3 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping3.setAttributeName("orderNumber");
        directtofieldmapping3.setIsReadOnly(false);
        directtofieldmapping3.setFieldName("LEG_ORD.ORDER_NO");
        descriptor.addMapping(directtofieldmapping3);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping4 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping4.setAttributeName("shipmentNumber");
        directtofieldmapping4.setIsReadOnly(true);
        directtofieldmapping4.setFieldName("LEG_ORD.SHIP_NO");
        descriptor.addMapping(directtofieldmapping4);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("employee");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(false);
        onetoonemapping.setReferenceClass(org.eclipse.persistence.testing.models.legacy.Employee.class);
        onetoonemapping.setIsPrivateOwned(false);
        onetoonemapping.addForeignKeyFieldName("LEG_ORD.FNAME", "LEG_EMP.FNAME");
        onetoonemapping.addForeignKeyFieldName("LEG_ORD.LNAME", "LEG_EMP.LNAME");
        descriptor.addMapping(onetoonemapping);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping1 = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping1.setAttributeName("shipment");
        onetoonemapping1.setIsReadOnly(false);
        onetoonemapping1.setUsesIndirection(false);
        onetoonemapping1.setReferenceClass(org.eclipse.persistence.testing.models.legacy.Shipment.class);
        onetoonemapping1.setIsPrivateOwned(false);
        onetoonemapping1.addForeignKeyFieldName("LEG_ORD.SHIP_NO", "LEG_SHP.SHIP_NO");
        onetoonemapping1.addTargetForeignKeyFieldName("LEG_SHP.FNAME", "LEG_ORD.FNAME");
        onetoonemapping1.addTargetForeignKeyFieldName("LEG_SHP.LNAME", "LEG_ORD.LNAME");
        descriptor.addMapping(onetoonemapping1);
        addDescriptor(descriptor);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildShipmentDescriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.legacy.Shipment.class);
        Vector vector = new Vector();
        vector.addElement("LEG_SHP");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("LEG_SHP.FNAME");
        descriptor.addPrimaryKeyFieldName("LEG_SHP.LNAME");
        descriptor.addPrimaryKeyFieldName("LEG_SHP.SHIP_NO");

        // SECTION: PROPERTIES
        descriptor.getDescriptorInheritancePolicy().setShouldReadSubclasses(true);
        descriptor.getDescriptorInheritancePolicy().setClassIndicatorFieldName("TYPE");
        descriptor.getDescriptorInheritancePolicy().setShouldUseClassNameAsIndicator(false);
        descriptor.getDescriptorInheritancePolicy().addClassIndicator(org.eclipse.persistence.testing.models.legacy.GaurenteedShipment.class, "Gaurentee");
        descriptor.getDescriptorInheritancePolicy().addClassIndicator(org.eclipse.persistence.testing.models.legacy.InsuredShipment.class, "Insured");
        descriptor.getDescriptorInheritancePolicy().addClassIndicator(org.eclipse.persistence.testing.models.legacy.Shipment.class, "Normal");

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("employeeFirstName");
        directtofieldmapping.setIsReadOnly(true);
        directtofieldmapping.setFieldName("LEG_SHP.FNAME");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("employeeLastName");
        directtofieldmapping1.setIsReadOnly(true);
        directtofieldmapping1.setFieldName("LEG_SHP.LNAME");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("quantityShipped");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setFieldName("LEG_SHP.QUANTITY");
        descriptor.addMapping(directtofieldmapping2);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping3 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping3.setAttributeName("shipmentNumber");
        directtofieldmapping3.setIsReadOnly(false);
        directtofieldmapping3.setFieldName("LEG_SHP.SHIP_NO");
        descriptor.addMapping(directtofieldmapping3);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping4 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping4.setAttributeName("shipMode");
        directtofieldmapping4.setIsReadOnly(false);
        directtofieldmapping4.setFieldName("LEG_SHP.SHP_MODE");
        descriptor.addMapping(directtofieldmapping4);

        // SECTION: ONETOMANYMAPPING
        org.eclipse.persistence.mappings.OneToManyMapping onetomanymapping = new org.eclipse.persistence.mappings.OneToManyMapping();
        onetomanymapping.setAttributeName("orders");
        onetomanymapping.setIsReadOnly(false);
        onetomanymapping.setUsesIndirection(false);
        onetomanymapping.setReferenceClass(org.eclipse.persistence.testing.models.legacy.Order.class);
        onetomanymapping.setIsPrivateOwned(true);
        onetomanymapping.addTargetForeignKeyFieldName("LEG_ORD.LNAME", "LEG_SHP.LNAME");
        onetomanymapping.addTargetForeignKeyFieldName("LEG_ORD.SHIP_NO", "LEG_SHP.SHIP_NO");
        onetomanymapping.addTargetForeignKeyFieldName("LEG_ORD.FNAME", "LEG_SHP.FNAME");
        descriptor.addMapping(onetomanymapping);

        //Fatima's section to add a custom query for the onetomanymapping mapping
        //First create the query object
        ExpressionBuilder myBuilder = new ExpressionBuilder();
        Expression exp = myBuilder.getField("LEG_ORD.SHIP_NO").equal(myBuilder.getParameter("LEG_SHP.SHIP_NO"));
        Expression exp1 = myBuilder.getField("LEG_ORD.ORDER_NO").notEqual(0);
        ReadAllQuery q = new ReadAllQuery();
        q.setReferenceClass(Order.class);
        q.addArgument("shipmentNumber");
        q.setSelectionCriteria(exp.and(exp1));

        OneToManyMapping myMap = (OneToManyMapping)descriptor.getMappingForAttributeName("orders");
        myMap.setCustomSelectionQuery(q);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("employee");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(false);
        onetoonemapping.setReferenceClass(org.eclipse.persistence.testing.models.legacy.Employee.class);
        onetoonemapping.setIsPrivateOwned(false);
        onetoonemapping.addForeignKeyFieldName("LEG_SHP.LNAME", "LEG_EMP.LNAME");
        onetoonemapping.addForeignKeyFieldName("LEG_SHP.FNAME", "LEG_EMP.FNAME");
        descriptor.addMapping(onetoonemapping);
        addDescriptor(descriptor);
    }
}
