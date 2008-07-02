/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.models.relationshipmaintenance;

import org.eclipse.persistence.sessions.*;
import org.eclipse.persistence.mappings.*;
import org.eclipse.persistence.descriptors.RelationalDescriptor;

/**
 * This class was generated by the TopLink project class generator.
 * It stores the meta-data (descriptors) that define the TopLink mappings.
 * @see org.eclipse.persistence.sessions.factories.ProjectClassGenerator
 */
public class RelationshipsProject extends org.eclipse.persistence.sessions.Project {
    public RelationshipsProject() {
        setName("Relationships");
        applyLogin();

        addDescriptor(buildCustomerDescriptor());
        addDescriptor(buildFieldLocationDescriptor());
        addDescriptor(buildFieldManagerDescriptor());
        addDescriptor(buildFieldOfficeDescriptor());
        addDescriptor(buildSalesPersonDescriptor());
        addDescriptor(buildEmpDescriptor());
        addDescriptor(buildDeptDescriptor());
        addDescriptor(buildResourceDescriptor());
    }

    public void applyLogin() {
        DatabaseLogin login = new DatabaseLogin();
        setLogin(login);
    }

    public RelationalDescriptor buildCustomerDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(Customer.class);
        descriptor.addTableName("REL_CUSTOMER");
        descriptor.addPrimaryKeyFieldName("REL_CUSTOMER.ID");

        // RelationalDescriptor properties.
        descriptor.useFullIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("REL_CUSTOMER.ID");
        descriptor.setSequenceNumberName("REL_CUSTOMER_SEQ");

        // Query manager.
        descriptor.getQueryManager().checkDatabaseForDoesExist();

        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("REL_CUSTOMER.ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping nameMapping = new DirectToFieldMapping();
        nameMapping.setAttributeName("name");
        nameMapping.setFieldName("REL_CUSTOMER.NAME");
        descriptor.addMapping(nameMapping);

        ManyToManyMapping salespeopleMapping = new ManyToManyMapping();
        salespeopleMapping.setAttributeName("salespeople");
        salespeopleMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.SalesPerson.class);
        salespeopleMapping.useTransparentCollection();
        salespeopleMapping.useCollectionClass(org.eclipse.persistence.indirection.IndirectSet.class);
        salespeopleMapping.readOnly();
        salespeopleMapping.setRelationTableName("SALES_CUST");
        salespeopleMapping.addSourceRelationKeyFieldName("SALES_CUST.CUST_ID", "REL_CUSTOMER.ID");
        salespeopleMapping.addTargetRelationKeyFieldName("SALES_CUST.SALES_ID", "SALESPERSON.ID");
        salespeopleMapping.setRelationshipPartnerAttributeName("customers");
        descriptor.addMapping(salespeopleMapping);

        return descriptor;
    }

    public RelationalDescriptor buildFieldLocationDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.relationshipmaintenance.FieldLocation.class);
        descriptor.addTableName("FIELDLOCATION");
        descriptor.addPrimaryKeyFieldName("FIELDLOCATION.ID");

        // RelationalDescriptor properties.
        descriptor.useFullIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("FIELDLOCATION.ID");
        descriptor.setSequenceNumberName("REL_FIELD_LOC_SEQ");

        // Query manager.
        descriptor.getQueryManager().checkDatabaseForDoesExist();

        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("FIELDLOCATION.ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping cityMapping = new DirectToFieldMapping();
        cityMapping.setAttributeName("city");
        cityMapping.setFieldName("FIELDLOCATION.CITY");
        descriptor.addMapping(cityMapping);

        return descriptor;
    }

    public RelationalDescriptor buildFieldManagerDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.relationshipmaintenance.FieldManager.class);
        descriptor.addTableName("FIELDMANAGER");
        descriptor.addPrimaryKeyFieldName("FIELDMANAGER.ID");

        // RelationalDescriptor properties.
        descriptor.useFullIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("FIELDMANAGER.ID");
        descriptor.setSequenceNumberName("REL_FIELD_MANAGER_SEQ");

        // Query manager.
        descriptor.getQueryManager().checkDatabaseForDoesExist();

        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("FIELDMANAGER.ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping nameMapping = new DirectToFieldMapping();
        nameMapping.setAttributeName("name");
        nameMapping.setFieldName("FIELDMANAGER.NAME");
        descriptor.addMapping(nameMapping);

        OneToOneMapping officeMapping = new OneToOneMapping();
        officeMapping.setAttributeName("office");
        officeMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.FieldOffice.class);
        officeMapping.useBasicIndirection();
        officeMapping.addTargetForeignKeyFieldName("FIELDOFFICE.MANAGER_ID", "FIELDMANAGER.ID");
        officeMapping.setRelationshipPartnerAttributeName("manager");
        descriptor.addMapping(officeMapping);

        return descriptor;
    }

    public RelationalDescriptor buildFieldOfficeDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.relationshipmaintenance.FieldOffice.class);
        descriptor.addTableName("FIELDOFFICE");
        descriptor.addPrimaryKeyFieldName("FIELDOFFICE.ID");

        // RelationalDescriptor properties.
        descriptor.useFullIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("FIELDOFFICE.ID");
        descriptor.setSequenceNumberName("REL_FIELD_OFFICE_SEQ");

        // Query manager.
        descriptor.getQueryManager().checkDatabaseForDoesExist();

        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("FIELDOFFICE.ID");
        descriptor.addMapping(idMapping);

        OneToOneMapping locationMapping = new OneToOneMapping();
        locationMapping.setAttributeName("location");
        locationMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.FieldLocation.class);
        locationMapping.useBasicIndirection();
        locationMapping.addForeignKeyFieldName("FIELDOFFICE.LOCATION_ID", "FIELDLOCATION.ID");
        descriptor.addMapping(locationMapping);

        OneToOneMapping managerMapping = new OneToOneMapping();
        managerMapping.setAttributeName("manager");
        managerMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.FieldManager.class);
        managerMapping.useBasicIndirection();
        managerMapping.addForeignKeyFieldName("FIELDOFFICE.MANAGER_ID", "FIELDMANAGER.ID");
        managerMapping.setRelationshipPartnerAttributeName("office");
        descriptor.addMapping(managerMapping);

        OneToManyMapping salespeopleMapping = new OneToManyMapping();
        salespeopleMapping.setAttributeName("salespeople");
        salespeopleMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.SalesPerson.class);
        salespeopleMapping.useTransparentCollection();
        salespeopleMapping.useCollectionClass(org.eclipse.persistence.indirection.IndirectSet.class);
        salespeopleMapping.addTargetForeignKeyFieldName("SALESPERSON.OFFICE_ID", "FIELDOFFICE.ID");
        salespeopleMapping.setRelationshipPartnerAttributeName("fieldOffice");
        descriptor.addMapping(salespeopleMapping);

        OneToManyMapping resourceMapping = new OneToManyMapping();
        resourceMapping.setAttributeName("resources");
        resourceMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.Resource.class);
        resourceMapping.dontUseIndirection();
        resourceMapping.useCollectionClass(org.eclipse.persistence.indirection.IndirectList.class);
        resourceMapping.addTargetForeignKeyFieldName("REL_RESOURCE.OFFICE_ID", "FIELDOFFICE.ID");
        resourceMapping.setRelationshipPartnerAttributeName("deptno");
        descriptor.addMapping(resourceMapping);

        return descriptor;
    }

    public RelationalDescriptor buildSalesPersonDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.relationshipmaintenance.SalesPerson.class);
        descriptor.addTableName("SALESPERSON");
        descriptor.addPrimaryKeyFieldName("SALESPERSON.ID");

        // RelationalDescriptor properties.
        descriptor.useFullIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("SALESPERSON.ID");
        descriptor.setSequenceNumberName("REL_SALESPERSON_SEQ");

        // Query manager.
        descriptor.getQueryManager().checkDatabaseForDoesExist();

        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("SALESPERSON.ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping nameMapping = new DirectToFieldMapping();
        nameMapping.setAttributeName("name");
        nameMapping.setFieldName("SALESPERSON.NAME");
        descriptor.addMapping(nameMapping);

        OneToOneMapping fieldOfficeMapping = new OneToOneMapping();
        fieldOfficeMapping.setAttributeName("fieldOffice");
        fieldOfficeMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.FieldOffice.class);
        fieldOfficeMapping.useBasicIndirection();
        fieldOfficeMapping.addForeignKeyFieldName("SALESPERSON.OFFICE_ID", "FIELDOFFICE.ID");
        fieldOfficeMapping.setRelationshipPartnerAttributeName("salespeople");
        descriptor.addMapping(fieldOfficeMapping);

        ManyToManyMapping customersMapping = new ManyToManyMapping();
        customersMapping.setAttributeName("customers");
        customersMapping.setReferenceClass(Customer.class);
        customersMapping.useTransparentCollection();
        customersMapping.useCollectionClass(org.eclipse.persistence.indirection.IndirectSet.class);
        customersMapping.setRelationTableName("SALES_CUST");
        customersMapping.addSourceRelationKeyFieldName("SALES_CUST.SALES_ID", "SALESPERSON.ID");
        customersMapping.addTargetRelationKeyFieldName("SALES_CUST.CUST_ID", "REL_CUSTOMER.ID");
        customersMapping.setRelationshipPartnerAttributeName("salespeople");
        descriptor.addMapping(customersMapping);

        return descriptor;
    }

    public RelationalDescriptor buildEmpDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.relationshipmaintenance.Emp.class);
        descriptor.addTableName("REL_EMP");
        descriptor.addPrimaryKeyFieldName("REL_EMP.ID");

        // RelationalDescriptor properties.
        descriptor.useSoftCacheWeakIdentityMap();
        descriptor.setIdentityMapSize(10);

        // Query manager.
        descriptor.getQueryManager().checkDatabaseForDoesExist();

        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("empno");
        idMapping.setFieldName("REL_EMP.ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping nameMapping = new DirectToFieldMapping();
        nameMapping.setAttributeName("ename");
        nameMapping.setFieldName("REL_EMP.NAME");
        descriptor.addMapping(nameMapping);

        OneToOneMapping deptMapping = new OneToOneMapping();
        deptMapping.setAttributeName("deptno");
        deptMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.Dept.class);
        deptMapping.useBasicIndirection();
        deptMapping.addForeignKeyFieldName("REL_EMP.DEPTNO", "REL_DEPT.DEPTNO");
        deptMapping.setRelationshipPartnerAttributeName("empCollection");
        descriptor.addMapping(deptMapping);

        return descriptor;
    }

    public RelationalDescriptor buildDeptDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.relationshipmaintenance.Dept.class);
        descriptor.addTableName("REL_DEPT");
        descriptor.addPrimaryKeyFieldName("REL_DEPT.DEPTNO");

        // RelationalDescriptor properties.
        descriptor.useSoftCacheWeakIdentityMap();
        descriptor.setIdentityMapSize(10);

        // Query manager.
        descriptor.getQueryManager().checkDatabaseForDoesExist();

        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("deptno");
        idMapping.setFieldName("REL_DEPT.DEPTNO");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping nameMapping = new DirectToFieldMapping();
        nameMapping.setAttributeName("dname");
        nameMapping.setFieldName("REL_DEPT.NAME");
        descriptor.addMapping(nameMapping);

        OneToManyMapping empMapping = new OneToManyMapping();
        empMapping.setAttributeName("empCollection");
        empMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.Emp.class);
        empMapping.useTransparentCollection();
        empMapping.useCollectionClass(org.eclipse.persistence.indirection.IndirectList.class);
        empMapping.addTargetForeignKeyFieldName("REL_EMP.DEPTNO", "REL_DEPT.DEPTNO");
        empMapping.setRelationshipPartnerAttributeName("deptno");
        descriptor.addMapping(empMapping);

        return descriptor;
    }

    public RelationalDescriptor buildResourceDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.relationshipmaintenance.Resource.class);
        descriptor.addTableName("REL_RESOURCE");
        descriptor.addPrimaryKeyFieldName("REL_RESOURCE.ID");

        // RelationalDescriptor properties.
        descriptor.useFullIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("REL_RESOURCE.ID");
        descriptor.setSequenceNumberName("REL_RESOURCE_SEQ");

        // Query manager.
        descriptor.getQueryManager().checkDatabaseForDoesExist();

        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("REL_RESOURCE.ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping nameMapping = new DirectToFieldMapping();
        nameMapping.setAttributeName("name");
        nameMapping.setFieldName("REL_RESOURCE.NAME");
        descriptor.addMapping(nameMapping);

        OneToOneMapping officeMapping = new OneToOneMapping();
        officeMapping.setAttributeName("office");
        officeMapping.setReferenceClass(org.eclipse.persistence.testing.models.relationshipmaintenance.FieldOffice.class);
        officeMapping.useBasicIndirection();
        officeMapping.addForeignKeyFieldName("REL_RESOURCE.OFFICE_ID", "FIELDOFFICE.ID");
        descriptor.addMapping(officeMapping);

        return descriptor;
    }
}