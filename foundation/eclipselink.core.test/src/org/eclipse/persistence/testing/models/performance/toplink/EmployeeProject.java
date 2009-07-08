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
package org.eclipse.persistence.testing.models.performance.toplink;

import org.eclipse.persistence.sessions.*;
import org.eclipse.persistence.descriptors.*;
import org.eclipse.persistence.mappings.*;

/**
 * This class was generated by the TopLink project class generator.
 * It stores the meta-data (descriptors) that define the TopLink mappings.
 * ## OracleAS TopLink - 10g (10.1.3 ) (Build 040713) ##
 * @see org.eclipse.persistence.sessions.factories.ProjectClassGenerator
 */
public class EmployeeProject extends org.eclipse.persistence.sessions.Project {
    public EmployeeProject() {
        setName("Employee");
        applyLogin();

        addDescriptor(buildAddressDescriptor());
        addDescriptor(buildEmployeeDescriptor());
        addDescriptor(buildEmploymentPeriodDescriptor());
        addDescriptor(buildLargeProjectDescriptor());
        addDescriptor(buildPhoneNumberDescriptor());
        addDescriptor(buildProjectDescriptor());
        addDescriptor(buildSmallProjectDescriptor());
    }

    public void applyLogin() {
        DatabaseLogin login = new DatabaseLogin();
        setDatasourceLogin(login);
    }

    public ClassDescriptor buildAddressDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.performance.Address.class);
        descriptor.addTableName("ADDRESS");
        descriptor.addPrimaryKeyFieldName("ADDRESS.ADDRESS_ID");

        // Descriptor Properties.
        descriptor.useSoftCacheWeakIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("ADDRESS.ADDRESS_ID");
        descriptor.setSequenceNumberName("ADDRESS_SEQ");
        descriptor.setAlias("Address");

        // Mappings.
        DirectToFieldMapping cityMapping = new DirectToFieldMapping();
        cityMapping.setAttributeName("city");
        cityMapping.setFieldName("ADDRESS.CITY");
        descriptor.addMapping(cityMapping);

        DirectToFieldMapping countryMapping = new DirectToFieldMapping();
        countryMapping.setAttributeName("country");
        countryMapping.setFieldName("ADDRESS.COUNTRY");
        descriptor.addMapping(countryMapping);

        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("ADDRESS.ADDRESS_ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping postalCodeMapping = new DirectToFieldMapping();
        postalCodeMapping.setAttributeName("postalCode");
        postalCodeMapping.setFieldName("ADDRESS.P_CODE");
        descriptor.addMapping(postalCodeMapping);

        DirectToFieldMapping provinceMapping = new DirectToFieldMapping();
        provinceMapping.setAttributeName("province");
        provinceMapping.setFieldName("ADDRESS.PROVINCE");
        descriptor.addMapping(provinceMapping);

        DirectToFieldMapping streetMapping = new DirectToFieldMapping();
        streetMapping.setAttributeName("street");
        streetMapping.setFieldName("ADDRESS.STREET");
        descriptor.addMapping(streetMapping);

        return descriptor;
    }

    public ClassDescriptor buildEmployeeDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(Employee.class);
        descriptor.addTableName("EMPLOYEE");
        descriptor.addPrimaryKeyFieldName("EMPLOYEE.EMP_ID");

        // Descriptor Properties.
        descriptor.useSoftCacheWeakIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("EMPLOYEE.EMP_ID");
        descriptor.setSequenceNumberName("EMP_SEQ");
        VersionLockingPolicy lockingPolicy = new VersionLockingPolicy();
        lockingPolicy.setWriteLockFieldName("EMPLOYEE.VERSION");
        lockingPolicy.storeInObject();
        descriptor.setOptimisticLockingPolicy(lockingPolicy);
        descriptor.setAlias("Employee");

        // Mappings.
        DirectToFieldMapping firstNameMapping = new DirectToFieldMapping();
        firstNameMapping.setAttributeName("firstName");
        firstNameMapping.setFieldName("EMPLOYEE.F_NAME");
        firstNameMapping.setNullValue("");
        descriptor.addMapping(firstNameMapping);

        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("EMPLOYEE.EMP_ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping versionMapping = new DirectToFieldMapping();
        versionMapping.setAttributeName("version");
        versionMapping.setFieldName("EMPLOYEE.VERSION");
        descriptor.addMapping(versionMapping);

        DirectToFieldMapping lastNameMapping = new DirectToFieldMapping();
        lastNameMapping.setAttributeName("lastName");
        lastNameMapping.setFieldName("EMPLOYEE.L_NAME");
        lastNameMapping.setNullValue("");
        descriptor.addMapping(lastNameMapping);

        DirectToFieldMapping salaryMapping = new DirectToFieldMapping();
        salaryMapping.setAttributeName("salary");
        salaryMapping.setFieldName("EMPLOYEE.SALARY");
        descriptor.addMapping(salaryMapping);

        DirectToFieldMapping genderMapping = new DirectToFieldMapping();
        genderMapping.setAttributeName("gender");
        genderMapping.setFieldName("EMPLOYEE.GENDER");
        descriptor.addMapping(genderMapping);

        AggregateObjectMapping periodMapping = new AggregateObjectMapping();
        periodMapping.setAttributeName("period");
        periodMapping.setReferenceClass(org.eclipse.persistence.testing.models.performance.EmploymentPeriod.class);
        periodMapping.setIsNullAllowed(true);
        periodMapping.addFieldNameTranslation("EMPLOYEE.END_DATE", "endDate->DIRECT");
        periodMapping.addFieldNameTranslation("EMPLOYEE.START_DATE", "startDate->DIRECT");
        descriptor.addMapping(periodMapping);

        OneToOneMapping addressMapping = new OneToOneMapping();
        addressMapping.setAttributeName("address");
        addressMapping.setGetMethodName("getAddressHolder");
        addressMapping.setSetMethodName("setAddressHolder");
        addressMapping.setReferenceClass(org.eclipse.persistence.testing.models.performance.Address.class);
        addressMapping.useBasicIndirection();
        addressMapping.privateOwnedRelationship();
        addressMapping.addForeignKeyFieldName("EMPLOYEE.ADDR_ID", "ADDRESS.ADDRESS_ID");
        descriptor.addMapping(addressMapping);

        OneToOneMapping managerMapping = new OneToOneMapping();
        managerMapping.setAttributeName("manager");
        managerMapping.setGetMethodName("getManagerHolder");
        managerMapping.setSetMethodName("setManagerHolder");
        managerMapping.setReferenceClass(Employee.class);
        managerMapping.useBasicIndirection();
        managerMapping.addForeignKeyFieldName("EMPLOYEE.MANAGER_ID", "EMPLOYEE.EMP_ID");
        descriptor.addMapping(managerMapping);

        OneToManyMapping managedEmployeesMapping = new OneToManyMapping();
        managedEmployeesMapping.setAttributeName("managedEmployees");
        managedEmployeesMapping.setReferenceClass(Employee.class);
        managedEmployeesMapping.useTransparentSet();
        managedEmployeesMapping.addTargetForeignKeyFieldName("EMPLOYEE.MANAGER_ID", "EMPLOYEE.EMP_ID");
        descriptor.addMapping(managedEmployeesMapping);

        OneToManyMapping phoneNumbersMapping = new OneToManyMapping();
        phoneNumbersMapping.setAttributeName("phoneNumbers");
        phoneNumbersMapping.setReferenceClass(PhoneNumber.class);
        phoneNumbersMapping.useTransparentSet();
        phoneNumbersMapping.privateOwnedRelationship();
        phoneNumbersMapping.addTargetForeignKeyFieldName("PHONE.EMP_ID", "EMPLOYEE.EMP_ID");
        descriptor.addMapping(phoneNumbersMapping);

        ManyToManyMapping projectsMapping = new ManyToManyMapping();
        projectsMapping.setAttributeName("projects");
        projectsMapping.setReferenceClass(Project.class);
        projectsMapping.useTransparentSet();
        projectsMapping.setRelationTableName("PROJ_EMP");
        projectsMapping.addSourceRelationKeyFieldName("PROJ_EMP.EMP_ID", "EMPLOYEE.EMP_ID");
        projectsMapping.addTargetRelationKeyFieldName("PROJ_EMP.PROJ_ID", "PROJECT.PROJ_ID");
        descriptor.addMapping(projectsMapping);

        return descriptor;
    }

    public ClassDescriptor buildEmploymentPeriodDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.descriptorIsAggregate();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.performance.EmploymentPeriod.class);

        // Descriptor Properties.
        descriptor.setAlias("EmploymentPeriod");

        // Mappings.
        DirectToFieldMapping endDateMapping = new DirectToFieldMapping();
        endDateMapping.setAttributeName("endDate");
        endDateMapping.setFieldName("endDate->DIRECT");
        descriptor.addMapping(endDateMapping);

        DirectToFieldMapping startDateMapping = new DirectToFieldMapping();
        startDateMapping.setAttributeName("startDate");
        startDateMapping.setFieldName("startDate->DIRECT");
        descriptor.addMapping(startDateMapping);

        return descriptor;
    }

    public ClassDescriptor buildLargeProjectDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(LargeProject.class);
        descriptor.addTableName("LPROJECT");

        // Inheritance Properties.
        descriptor.getInheritancePolicy().setParentClass(Project.class);

        // Descriptor Properties.
        descriptor.setAlias("LargeProject");

        // Mappings.
        DirectToFieldMapping budgetMapping = new DirectToFieldMapping();
        budgetMapping.setAttributeName("budget");
        budgetMapping.setFieldName("LPROJECT.BUDGET");
        descriptor.addMapping(budgetMapping);

        DirectToFieldMapping milestoneVersionMapping = new DirectToFieldMapping();
        milestoneVersionMapping.setAttributeName("milestoneVersion");
        milestoneVersionMapping.setFieldName("LPROJECT.MILESTONE");
        descriptor.addMapping(milestoneVersionMapping);

        return descriptor;
    }

    public ClassDescriptor buildPhoneNumberDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(PhoneNumber.class);
        descriptor.addTableName("PHONE");
        descriptor.addPrimaryKeyFieldName("PHONE.PHONE_ID");

        // Descriptor Properties.
        descriptor.useSoftCacheWeakIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("PHONE.PHONE_ID");
        descriptor.setSequenceNumberName("PHONE_SEQ");
        descriptor.setAlias("PhoneNumber");

        // Query keys.
        descriptor.addDirectQueryKey("id", "PHONE.EMP_ID");

        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("PHONE.PHONE_ID");
        descriptor.addMapping(idMapping);
        
        DirectToFieldMapping areaCodeMapping = new DirectToFieldMapping();
        areaCodeMapping.setAttributeName("areaCode");
        areaCodeMapping.setFieldName("PHONE.AREA_CODE");
        descriptor.addMapping(areaCodeMapping);

        DirectToFieldMapping numberMapping = new DirectToFieldMapping();
        numberMapping.setAttributeName("number");
        numberMapping.setFieldName("PHONE.P_NUMBER");
        descriptor.addMapping(numberMapping);

        DirectToFieldMapping typeMapping = new DirectToFieldMapping();
        typeMapping.setAttributeName("type");
        typeMapping.setFieldName("PHONE.TYPE");
        descriptor.addMapping(typeMapping);

        OneToOneMapping ownerMapping = new OneToOneMapping();
        ownerMapping.setAttributeName("ownerHolder");
        ownerMapping.setReferenceClass(Employee.class);
        ownerMapping.useBasicIndirection();
        ownerMapping.addForeignKeyFieldName("PHONE.EMP_ID", "EMPLOYEE.EMP_ID");
        descriptor.addMapping(ownerMapping);

        return descriptor;
    }

    public ClassDescriptor buildProjectDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(Project.class);
        descriptor.addTableName("PROJECT");
        descriptor.addPrimaryKeyFieldName("PROJECT.PROJ_ID");

        // Inheritance Properties.
        descriptor.getInheritancePolicy().setClassIndicatorFieldName("PROJECT.PROJ_TYPE");
        descriptor.getInheritancePolicy().addClassIndicator(SmallProject.class, "S");
        descriptor.getInheritancePolicy().addClassIndicator(LargeProject.class, "L");

        // Descriptor Properties.
        descriptor.useSoftCacheWeakIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("PROJECT.PROJ_ID");
        descriptor.setSequenceNumberName("PROJ_SEQ");
        VersionLockingPolicy lockingPolicy = new VersionLockingPolicy();
        lockingPolicy.setWriteLockFieldName("PROJECT.VERSION");
        lockingPolicy.storeInObject();
        descriptor.setOptimisticLockingPolicy(lockingPolicy);
        descriptor.setAlias("Project");

        // Mappings.
        DirectToFieldMapping descriptionMapping = new DirectToFieldMapping();
        descriptionMapping.setAttributeName("description");
        descriptionMapping.setFieldName("PROJECT.DESCRIP");
        descriptionMapping.setNullValue("");
        descriptor.addMapping(descriptionMapping);

        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("PROJECT.PROJ_ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping versionMapping = new DirectToFieldMapping();
        versionMapping.setAttributeName("version");
        versionMapping.setFieldName("PROJECT.VERSION");
        descriptor.addMapping(versionMapping);

        DirectToFieldMapping nameMapping = new DirectToFieldMapping();
        nameMapping.setAttributeName("name");
        nameMapping.setFieldName("PROJECT.PROJ_NAME");
        nameMapping.setNullValue("");
        descriptor.addMapping(nameMapping);

        OneToOneMapping teamLeaderMapping = new OneToOneMapping();
        teamLeaderMapping.setAttributeName("teamLeaderHolder");
        teamLeaderMapping.setReferenceClass(Employee.class);
        teamLeaderMapping.useBasicIndirection();
        teamLeaderMapping.addForeignKeyFieldName("PROJECT.LEADER_ID", "EMPLOYEE.EMP_ID");
        descriptor.addMapping(teamLeaderMapping);

        return descriptor;
    }

    public ClassDescriptor buildSmallProjectDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(SmallProject.class);
        descriptor.addTableName("SPROJECT");

        // Inheritance Properties.
        descriptor.getInheritancePolicy().setParentClass(Project.class);

        // Descriptor Properties.
        descriptor.setAlias("SmallProject");

        return descriptor;
    }
}
