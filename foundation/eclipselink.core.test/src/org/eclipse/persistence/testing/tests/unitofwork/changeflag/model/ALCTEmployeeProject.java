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

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.descriptors.VersionLockingPolicy;
import org.eclipse.persistence.descriptors.changetracking.AttributeChangeTrackingPolicy;
import org.eclipse.persistence.mappings.AggregateObjectMapping;
import org.eclipse.persistence.mappings.DirectToFieldMapping;
import org.eclipse.persistence.mappings.converters.ObjectTypeConverter;
import org.eclipse.persistence.sessions.DatabaseLogin;


/**
 * This class was generated by the TopLink project class generator.
 * It stores the meta-data (descriptors) that define the TopLink mappings.
 * ## OracleAS TopLink - 10g (10.1.3 ) (Build 040713) ##
 * @see org.eclipse.persistence.sessions.factories.ProjectClassGenerator
 */
public class

ALCTEmployeeProject extends org.eclipse.persistence.sessions.Project {

    public ALCTEmployeeProject() {
        setName("Employee");
        applyLogin();

        addDescriptor(buildEmployeeDescriptor());
        addDescriptor(buildEmploymentPeriodDescriptor());
    }

    public void applyLogin() {
        DatabaseLogin login = new DatabaseLogin();
        login.usePlatform(new org.eclipse.persistence.platform.database.OraclePlatform());
        login.setDriverClassName("oracle.jdbc.OracleDriver");
        login.setConnectionString("jdbc:oracle:thin:@localhost:1521:orcl");
        login.setUserName("scott");
        login.setEncryptedPassword("3E20F8982C53F4ABA825E30206EC8ADE");

        // Configuration Properties.
        login.setShouldBindAllParameters(false);
        login.setShouldCacheAllStatements(false);
        login.setUsesByteArrayBinding(true);
        login.setUsesStringBinding(false);
        if (login.shouldUseByteArrayBinding()) { // Can only be used with binding.
            login.setUsesStreamsForBinding(false);
        }
        login.setShouldForceFieldNamesToUpperCase(false);
        login.setShouldOptimizeDataConversion(true);
        login.setShouldTrimStrings(true);
        login.setUsesBatchWriting(false);
        if (login.shouldUseBatchWriting()) { // Can only be used with batch writing.
            login.setUsesJDBCBatchWriting(true);
        }
        login.setUsesExternalConnectionPooling(false);
        login.setUsesExternalTransactionController(false);

        setDatasourceLogin(login);
    }

    public ClassDescriptor buildEmployeeDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(ALCTEmployee.class);
        descriptor.addTableName("ALCTEMPLOYEE");
        descriptor.addPrimaryKeyFieldName("ALCTEMPLOYEE.EMP_ID");

        // Descriptor Properties.
        descriptor.useSoftCacheWeakIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.useRemoteSoftCacheWeakIdentityMap();
        descriptor.setRemoteIdentityMapSize(100);
        descriptor.setSequenceNumberFieldName("ALCTEMPLOYEE.EMP_ID");
        descriptor.setSequenceNumberName("EMP_SEQ");
        VersionLockingPolicy lockingPolicy = new VersionLockingPolicy();
        lockingPolicy.setWriteLockFieldName("ALCTEMPLOYEE.VERSION");
        descriptor.setOptimisticLockingPolicy(lockingPolicy);
        descriptor.setAlias("ALCTEmployee");

        //Change Tracking
        descriptor.setObjectChangePolicy(new AttributeChangeTrackingPolicy());

        // Cache Invalidation Policy

        // Query Manager.
        descriptor.getQueryManager().checkCacheForDoesExist();
        descriptor.getQueryManager().setQueryTimeout(0);
        // Named Queries.


        // Event Manager.

        // Mappings.
        DirectToFieldMapping firstNameMapping = new DirectToFieldMapping();
        firstNameMapping.setAttributeName("firstName");
        firstNameMapping.setFieldName("ALCTEMPLOYEE.F_NAME");
        firstNameMapping.setNullValue("");
        descriptor.addMapping(firstNameMapping);

        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setFieldName("ALCTEMPLOYEE.EMP_ID");
        descriptor.addMapping(idMapping);

        DirectToFieldMapping lastNameMapping = new DirectToFieldMapping();
        lastNameMapping.setAttributeName("lastName");
        lastNameMapping.setFieldName("ALCTEMPLOYEE.L_NAME");
        lastNameMapping.setNullValue("");
        descriptor.addMapping(lastNameMapping);

        DirectToFieldMapping genderMapping = new DirectToFieldMapping();
        genderMapping.setAttributeName("gender");
        genderMapping.setFieldName("ALCTEMPLOYEE.GENDER");
        ObjectTypeConverter genderMappingConverter = new ObjectTypeConverter();
        genderMappingConverter.addConversionValue("F", "Female");
        genderMappingConverter.addConversionValue("M", "Male");
        genderMapping.setConverter(genderMappingConverter);
        descriptor.addMapping(genderMapping);

        AggregateObjectMapping periodMapping = new AggregateObjectMapping();
        periodMapping.setAttributeName("period");
        periodMapping.setReferenceClass(ALCTEmploymentPeriod.class);
        periodMapping.setIsNullAllowed(true);
        periodMapping.addFieldNameTranslation("ALCTEMPLOYEE.END_DATE", "endDate->DIRECT");
        periodMapping.addFieldNameTranslation("ALCTEMPLOYEE.START_DATE", "startDate->DIRECT");
        descriptor.addMapping(periodMapping);

        return descriptor;
    }

    public ClassDescriptor buildEmploymentPeriodDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.descriptorIsAggregate();
        descriptor.setJavaClass(ALCTEmploymentPeriod.class);

        // Descriptor Properties.
        descriptor.setAlias("EmploymentPeriod");

        //Change Tracking
        descriptor.setObjectChangePolicy(new AttributeChangeTrackingPolicy());

        // Cache Invalidation Policy

        // Query Manager.
        // Named Queries.


        // Event Manager.

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

}
