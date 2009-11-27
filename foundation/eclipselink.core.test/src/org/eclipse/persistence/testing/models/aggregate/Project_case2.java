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
package org.eclipse.persistence.testing.models.aggregate;

import java.util.*;

/**
 * TopLink generated Project class.
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
public class Project_case2 extends org.eclipse.persistence.sessions.Project {

    /**
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    public Project_case2() {
        applyPROJECT();
        applyLOGIN();
        buildAddress1Descriptor();
        buildEmployee1Descriptor();
        buildHomeAddressDescriptor();
        buildWorkingAddressDescriptor();
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void applyLOGIN() {
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void applyPROJECT() {
        setName("case2");
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildAddress1Descriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.aggregate.Address1.class);
        Vector vector = new Vector();
        descriptor.descriptorIsAggregate();
        descriptor.getDescriptorInheritancePolicy().setShouldReadSubclasses(true);
        descriptor.getDescriptorInheritancePolicy().setClassIndicatorFieldName("TYPE");
        descriptor.getDescriptorInheritancePolicy().setShouldUseClassNameAsIndicator(false);
        descriptor.getDescriptorInheritancePolicy().addClassIndicator(org.eclipse.persistence.testing.models.aggregate.WorkingAddress.class, "W");
        descriptor.getDescriptorInheritancePolicy().addClassIndicator(org.eclipse.persistence.testing.models.aggregate.HomeAddress.class, "H");

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("buildingNumber");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setGetMethodName("getBuildingNumber");
        directtofieldmapping1.setSetMethodName("setBuildingNumber");
        directtofieldmapping1.setFieldName("Employee1.BUILDING_NUMBER");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("city");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setGetMethodName("getCity");
        directtofieldmapping2.setSetMethodName("setCity");
        directtofieldmapping2.setFieldName("Employee1.CITY");
        descriptor.addMapping(directtofieldmapping2);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping3 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping3.setAttributeName("country");
        directtofieldmapping3.setIsReadOnly(false);
        directtofieldmapping3.setGetMethodName("getCountry");
        directtofieldmapping3.setSetMethodName("setCountry");
        directtofieldmapping3.setFieldName("Employee1.COUNTRY");
        descriptor.addMapping(directtofieldmapping3);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping4 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping4.setAttributeName("postalCode");
        directtofieldmapping4.setIsReadOnly(false);
        directtofieldmapping4.setGetMethodName("getPostalCode");
        directtofieldmapping4.setSetMethodName("setPostalCode");
        directtofieldmapping4.setFieldName("Employee1.POSTAL_CODE");
        descriptor.addMapping(directtofieldmapping4);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping5 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping5.setAttributeName("streetName");
        directtofieldmapping5.setIsReadOnly(false);
        directtofieldmapping5.setGetMethodName("getStreetName");
        directtofieldmapping5.setSetMethodName("setStreetName");
        directtofieldmapping5.setFieldName("Employee1.STREET_NAME");
        descriptor.addMapping(directtofieldmapping5);
        addDescriptor(descriptor);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildEmployee1Descriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.aggregate.Employee1.class);
        Vector vector = new Vector();
        vector.addElement("Employee1");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("Employee1.id");

        // SECTION: AGGREGATEOBJECTMAPPING
        org.eclipse.persistence.mappings.AggregateObjectMapping aggregateobjectmapping = new org.eclipse.persistence.mappings.AggregateObjectMapping();
        aggregateobjectmapping.setAttributeName("address");
        aggregateobjectmapping.setIsReadOnly(false);
        aggregateobjectmapping.setGetMethodName("getAddress");
        aggregateobjectmapping.setSetMethodName("setAddress");
        aggregateobjectmapping.setReferenceClass(org.eclipse.persistence.testing.models.aggregate.Address1.class);
        aggregateobjectmapping.setIsNullAllowed(false);
        descriptor.addMapping(aggregateobjectmapping);

        // SECTION: AGGREGATEOBJECTMAPPING
        org.eclipse.persistence.mappings.AggregateObjectMapping aggregateobjectmapping2 = new org.eclipse.persistence.mappings.AggregateObjectMapping();
        aggregateobjectmapping2.setAttributeName("businessAddress");
        aggregateobjectmapping2.setReferenceClass(org.eclipse.persistence.testing.models.aggregate.Address1.class);
        aggregateobjectmapping2.setIsNullAllowed(false);
        aggregateobjectmapping.addFieldNameTranslation("Employee1.BTYPE", "Employee1.TYPE");
        aggregateobjectmapping.addFieldNameTranslation("Employee1.BBUILDING_NUMBER", "Employee1.BUILDING_NUMBER");
        aggregateobjectmapping.addFieldNameTranslation("Employee1.BCITY", "Employee1.CITY");
        aggregateobjectmapping.addFieldNameTranslation("Employee1.BCOUNTRY", "Employee1.COUNTRY");
        aggregateobjectmapping.addFieldNameTranslation("Employee1.BPOSTAL_CODE", "Employee1.POSTAL_CODE");
        aggregateobjectmapping.addFieldNameTranslation("Employee1.BSTREET_NAME", "Employee1.STREET_NAME");
        aggregateobjectmapping.addFieldNameTranslation("Employee1.BAPARTMENT_NUMBER", "Employee1.APARTMENT_NUMBER");
        descriptor.addMapping(aggregateobjectmapping2);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("id");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setGetMethodName("getId");
        directtofieldmapping.setSetMethodName("setId");
        directtofieldmapping.setFieldName("Employee1.id");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("name");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setGetMethodName("getName");
        directtofieldmapping1.setSetMethodName("setName");
        directtofieldmapping1.setFieldName("Employee1.Name");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("salary");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setGetMethodName("getSalary");
        directtofieldmapping2.setSetMethodName("setSalary");
        directtofieldmapping2.setFieldName("Employee1.salary");
        descriptor.addMapping(directtofieldmapping2);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping3 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping3.setAttributeName("status");
        directtofieldmapping3.setIsReadOnly(false);
        directtofieldmapping3.setGetMethodName("getStatus");
        directtofieldmapping3.setSetMethodName("setStatus");
        directtofieldmapping3.setFieldName("Employee1.status");
        descriptor.addMapping(directtofieldmapping3);
        addDescriptor(descriptor);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildHomeAddressDescriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.aggregate.HomeAddress.class);
        descriptor.getDescriptorInheritancePolicy().setParentClass(org.eclipse.persistence.testing.models.aggregate.Address1.class);

        // SECTION: PROPERTIES
        descriptor.descriptorIsAggregate();
        descriptor.getDescriptorInheritancePolicy().setShouldReadSubclasses(true);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("apartmentNumber");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setGetMethodName("getApartmentNumber");
        directtofieldmapping.setSetMethodName("setApartmentNumber");
        directtofieldmapping.setFieldName("Employee1.APARTMENT_NUMBER");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");
        addDescriptor(descriptor);
    }

    /**
     * TopLink generated method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildWorkingAddressDescriptor() {
        org.eclipse.persistence.descriptors.RelationalDescriptor descriptor = new org.eclipse.persistence.descriptors.RelationalDescriptor();

        descriptor.setJavaClass(org.eclipse.persistence.testing.models.aggregate.WorkingAddress.class);
        descriptor.getDescriptorInheritancePolicy().setParentClass(org.eclipse.persistence.testing.models.aggregate.Address1.class);
        descriptor.getDescriptorInheritancePolicy().setShouldReadSubclasses(true);
        descriptor.descriptorIsAggregate();
        
        addDescriptor(descriptor);
    }
}
