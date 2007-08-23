/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.models.readonly;

import java.util.*;
import org.eclipse.persistence.mappings.*;
import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.sessions.DatabaseLogin;

/**
 * TopLink generated Project class.
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
public class ReadOnlyProject extends org.eclipse.persistence.sessions.Project {

    /**
    * <b>WARNING</b>: This code was generated by an automated tool.
    * Any changes will be lost when the code is re-generated
    */
    public ReadOnlyProject() {
        applyPROJECT();
        applyLOGIN();
        buildActorDescriptor();
        buildAddressDescriptor();
        buildCharityDescriptor();
        buildCountryDescriptor();
        buildDefaultReadOnlyTestClassDescriptor();
        buildHollywoodAgentDescriptor();
        buildMovieDescriptor();
        buildPromoterDescriptor();
        buildReadOnlyCharityDescriptor();
        buildReadOnlyHollywoodAgentDescriptor();
        buildStudioDescriptor();
    }

    /**
    * TopLink generated method. 
    * <b>WARNING</b>: This code was generated by an automated tool.
    * Any changes will be lost when the code is re-generated
    */
    protected void applyLOGIN() {
        setLogin(new DatabaseLogin());
    }

    /**
    * TopLink generated method. 
    * <b>WARNING</b>: This code was generated by an automated tool.
    * Any changes will be lost when the code is re-generated
    */
    protected void applyPROJECT() {
        setName("ReadOnlyProject");
    }

    /**
    * TopLink generated method. 
    * <b>WARNING</b>: This code was generated by an automated tool.
    * Any changes will be lost when the code is re-generated
    */
    protected void buildActorDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.readonly.Actor.class);
        Vector vector = new Vector();
        vector.addElement("RO_ACTOR");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("RO_ACTOR.ACT_ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setSequenceNumberName("SEQ");
        descriptor.setSequenceNumberFieldName("ACT_ID");
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("id");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("RO_ACTOR.ACT_ID");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("minimumSalary");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("RO_ACTOR.MIN_SAL");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("name");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setFieldName("RO_ACTOR.ACT_NAME");
        descriptor.addMapping(directtofieldmapping2);

        // SECTION: MANYTOMANYMAPPING
        org.eclipse.persistence.mappings.ManyToManyMapping manytomanymapping = new org.eclipse.persistence.mappings.ManyToManyMapping();
        manytomanymapping.setAttributeName("movies");
        manytomanymapping.setIsReadOnly(true);
        manytomanymapping.setUsesIndirection(false);
        manytomanymapping.setReferenceClass(org.eclipse.persistence.testing.models.readonly.Movie.class);
        manytomanymapping.setIsPrivateOwned(false);
        manytomanymapping.setRelationTableName("ACT_MOV");
        manytomanymapping.addSourceRelationKeyFieldName("ACT_MOV.ACT_ID", "RO_ACTOR.ACT_ID");
        manytomanymapping.addTargetRelationKeyFieldName("ACT_MOV.MOV_ID", "RO_MOVIE.MOV_ID");
        descriptor.addMapping(manytomanymapping);

        // ManyToManyMapping: charities
        ManyToManyMapping charitiesMapping = new ManyToManyMapping();
        charitiesMapping.setAttributeName("charities");
        charitiesMapping.setIsReadOnly(false);
        charitiesMapping.setUsesIndirection(false);
        charitiesMapping.setReferenceClass(ReadOnlyCharity.class);
        charitiesMapping.setIsPrivateOwned(false);
        charitiesMapping.setRelationTableName("ACT_CHA");
        charitiesMapping.setSourceRelationKeyFieldName("ACT_ID");
        charitiesMapping.setTargetRelationKeyFieldName("CHARITY_ID");
        descriptor.addMapping(charitiesMapping);

        // OneToOneMapping: hollywoodAgent
        OneToOneMapping hollywoodAgentMapping = new OneToOneMapping();
        hollywoodAgentMapping.setAttributeName("hollywoodAgent");
        hollywoodAgentMapping.setIsReadOnly(false);
        hollywoodAgentMapping.setUsesIndirection(false);
        hollywoodAgentMapping.setReferenceClass(ReadOnlyHollywoodAgent.class);
        hollywoodAgentMapping.setIsPrivateOwned(false);
        hollywoodAgentMapping.addForeignKeyFieldName("HOLLYWOODAGENT_ID", "HOLLYWOODAGENT_ID");
        descriptor.addMapping(hollywoodAgentMapping);

        addDescriptor(descriptor);
    }

    /**
    * TopLink generated method. 
    * <b>WARNING</b>: This code was generated by an automated tool.
    * Any changes will be lost when the code is re-generated
    */
    protected void buildAddressDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.readonly.Address.class);
        Vector vector = new Vector();
        vector.addElement("RO_ADDR");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("RO_ADDR.ADD_ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setSequenceNumberName("ADD_SEQ");
        descriptor.setSequenceNumberFieldName("ADD_ID");
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("city");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("RO_ADDR.CITY");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("id");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("RO_ADDR.ADD_ID");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("streetAddress");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setFieldName("RO_ADDR.STREET");
        descriptor.addMapping(directtofieldmapping2);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping3 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping3.setAttributeName("zipCode");
        directtofieldmapping3.setIsReadOnly(false);
        directtofieldmapping3.setFieldName("RO_ADDR.ZIP");
        descriptor.addMapping(directtofieldmapping3);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("country");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(false);
        onetoonemapping.setReferenceClass(org.eclipse.persistence.testing.models.readonly.Country.class);
        onetoonemapping.setIsPrivateOwned(false);
        onetoonemapping.addForeignKeyFieldName("RO_ADDR.COUNTRY_ID", "COUNTRY.COUNTRY_ID");
        descriptor.addMapping(onetoonemapping);
        addDescriptor(descriptor);
    }

    protected void buildCharityDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        descriptor.setJavaClass(Charity.class);
        descriptor.setTableName("CHARITY");
        descriptor.addPrimaryKeyFieldName("CHARITY_ID");
        descriptor.setSequenceNumberName("CHARITY_SEQ");
        descriptor.setSequenceNumberFieldName("CHARITY_ID");

        descriptor.addDirectMapping("id", "CHARITY_ID");
        descriptor.addDirectMapping("name", "NAME");
        descriptor.addDirectMapping("donationsRaised", "DONATIONS_RAISED");

        addDescriptor(descriptor);
    }

    /**
     * TopLink generated method. 
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildCountryDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.readonly.Country.class);
        Vector vector = new Vector();
        vector.addElement("COUNTRY");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("COUNTRY.COUNTRY_ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setSequenceNumberName("COUNTRY_SEQ");
        descriptor.setSequenceNumberFieldName("COUNTRY_ID");
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("id");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("COUNTRY.COUNTRY_ID");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("name");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("COUNTRY.NAME");
        descriptor.addMapping(directtofieldmapping1);
        addDescriptor(descriptor);
    }

    protected void buildDefaultReadOnlyTestClassDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.readonly.DefaultReadOnlyTestClass.class);
        descriptor.setTableName("DEFAULT_READ_ONLY");
        descriptor.setPrimaryKeyFieldName("DEFAULT_READ_ONLY.DATA");

        org.eclipse.persistence.mappings.DirectToFieldMapping directToFieldMapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directToFieldMapping.setAttributeName("data");
        directToFieldMapping.setFieldName("DEFAULT_READ_ONLY.DATA");
        directToFieldMapping.setGetMethodName("getData");
        directToFieldMapping.setSetMethodName("setData");
        descriptor.addMapping(directToFieldMapping);

        addDescriptor(descriptor);
        addDefaultReadOnlyClass(org.eclipse.persistence.testing.models.readonly.DefaultReadOnlyTestClass.class);
    }

    protected void buildHollywoodAgentDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        descriptor.setJavaClass(HollywoodAgent.class);
        descriptor.setTableName("HOLLYWOODAGENT");
        descriptor.addPrimaryKeyFieldName("HOLLYWOODAGENT_ID");
        descriptor.setSequenceNumberName("HOLLYWOODAGENT_SEQ");
        descriptor.setSequenceNumberFieldName("HOLLYWOODAGENT_ID");

        descriptor.addDirectMapping("id", "HOLLYWOODAGENT_ID");
        descriptor.addDirectMapping("name", "NAME");
        descriptor.addDirectMapping("numberOfConnections", "CONNECTIONS");

        addDescriptor(descriptor);
    }

    /**
    * TopLink generated method. 
    * <b>WARNING</b>: This code was generated by an automated tool.
    * Any changes will be lost when the code is re-generated
    */
    protected void buildMovieDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.readonly.Movie.class);
        Vector vector = new Vector();
        vector.addElement("RO_MOVIE");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("RO_MOVIE.MOV_ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setSequenceNumberName("MOV_SEQ");
        descriptor.setSequenceNumberFieldName("MOV_ID");
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: AGGREGATEOBJECTMAPPING
        org.eclipse.persistence.mappings.AggregateObjectMapping aggregateobjectmapping = new org.eclipse.persistence.mappings.AggregateObjectMapping();
        aggregateobjectmapping.setAttributeName("studio");
        aggregateobjectmapping.setIsReadOnly(true);
        aggregateobjectmapping.setReferenceClass(org.eclipse.persistence.testing.models.readonly.Studio.class);
        aggregateobjectmapping.setIsNullAllowed(false);
        descriptor.addMapping(aggregateobjectmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("id");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("RO_MOVIE.MOV_ID");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("title");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("RO_MOVIE.TITLE");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: MANYTOMANYMAPPING
        org.eclipse.persistence.mappings.ManyToManyMapping manytomanymapping = new org.eclipse.persistence.mappings.ManyToManyMapping();
        manytomanymapping.setAttributeName("actors");
        manytomanymapping.setIsReadOnly(false);
        manytomanymapping.setUsesIndirection(false);
        manytomanymapping.setReferenceClass(org.eclipse.persistence.testing.models.readonly.Actor.class);
        manytomanymapping.setIsPrivateOwned(true);
        manytomanymapping.setRelationTableName("ACT_MOV");
        manytomanymapping.addSourceRelationKeyFieldName("ACT_MOV.MOV_ID", "RO_MOVIE.MOV_ID");
        manytomanymapping.addTargetRelationKeyFieldName("ACT_MOV.ACT_ID", "RO_ACTOR.ACT_ID");
        descriptor.addMapping(manytomanymapping);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("promoter");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(false);
        onetoonemapping.setReferenceClass(org.eclipse.persistence.testing.models.readonly.Promoter.class);
        onetoonemapping.setIsPrivateOwned(false);
        onetoonemapping.addForeignKeyFieldName("RO_MOVIE.PROMO_ID", "RO_PROMO.PROMO_ID");
        descriptor.addMapping(onetoonemapping);
        addDescriptor(descriptor);
    }

    /**
    * TopLink generated method. 
    * <b>WARNING</b>: This code was generated by an automated tool.
    * Any changes will be lost when the code is re-generated
    */
    protected void buildPromoterDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.readonly.Promoter.class);
        Vector vector = new Vector();
        vector.addElement("RO_PROMO");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("RO_PROMO.PROMO_ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setSequenceNumberName("PROMO_SEQ");
        descriptor.setSequenceNumberFieldName("PROMO_ID");
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("id");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("RO_PROMO.PROMO_ID");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("name");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("RO_PROMO.NAME");
        descriptor.addMapping(directtofieldmapping1);
        addDescriptor(descriptor);
    }

    protected void buildReadOnlyCharityDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        descriptor.setJavaClass(ReadOnlyCharity.class);
        descriptor.setTableName("CHARITY");
        descriptor.addPrimaryKeyFieldName("CHARITY_ID");
        descriptor.setSequenceNumberName("CHARITY_SEQ");
        descriptor.setSequenceNumberFieldName("CHARITY_ID");

        descriptor.addDirectMapping("id", "CHARITY_ID");
        descriptor.addDirectMapping("name", "NAME");

        descriptor.setShouldBeReadOnly(true);

        addDescriptor(descriptor);
    }

    protected void buildReadOnlyHollywoodAgentDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        descriptor.setJavaClass(ReadOnlyHollywoodAgent.class);
        descriptor.setTableName("HOLLYWOODAGENT");
        descriptor.addPrimaryKeyFieldName("HOLLYWOODAGENT_ID");
        descriptor.setSequenceNumberName("HOLLYWOODAGENT_SEQ");
        descriptor.setSequenceNumberFieldName("HOLLYWOODAGENT_ID");

        descriptor.addDirectMapping("id", "HOLLYWOODAGENT_ID");
        descriptor.addDirectMapping("name", "NAME");

        descriptor.setShouldBeReadOnly(true);

        addDescriptor(descriptor);
    }

    /**
    * TopLink generated method. 
    * <b>WARNING</b>: This code was generated by an automated tool.
    * Any changes will be lost when the code is re-generated
    */
    protected void buildStudioDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.readonly.Studio.class);
        Vector vector = new Vector();
        vector.addElement("RO_MOVIE");
        descriptor.setTableNames(vector);

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);
        descriptor.descriptorIsAggregate();

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("name");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("RO_MOVIE.STD_NAME");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("owner");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("RO_MOVIE.STD_OWN");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("address");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(false);
        onetoonemapping.setReferenceClass(org.eclipse.persistence.testing.models.readonly.Address.class);
        onetoonemapping.setIsPrivateOwned(true);
        onetoonemapping.addForeignKeyFieldName("RO_MOVIE.STD_ADD", "RO_ADDR.ADD_ID");
        descriptor.addMapping(onetoonemapping);
        addDescriptor(descriptor);
    }
}