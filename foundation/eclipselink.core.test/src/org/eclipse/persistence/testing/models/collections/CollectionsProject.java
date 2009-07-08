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
package org.eclipse.persistence.testing.models.collections;

import java.util.*;
import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.mappings.converters.*;

/**
 * TopLink generated Project class.
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
public class CollectionsProject extends org.eclipse.persistence.sessions.Project {

    /**
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    public CollectionsProject() {
        applyPROJECT();
        applyLOGIN();
        buildLocationDescriptor();
        buildMenuDescriptor();
        buildMenuItemDescriptor();
        buildPersonDescriptor();
        buildDinerDescriptor();
        buildWaiterDescriptor();
        buildRestaurantDescriptor();
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
        setName("Collections");
    }

    /**
     * TopLink generated descriptor method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildDinerDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(Diner.class);
        Vector vector = new Vector();
        vector.addElement("COL_PERS");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("COL_PERS.ID");
        descriptor.getInheritancePolicy().setParentClass(org.eclipse.persistence.testing.models.collections.Person.class);

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);

        addDescriptor(descriptor);

    }

    /**
     * TopLink generated descriptor method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildLocationDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(Location.class);
        java.util.Vector vector = new java.util.Vector();
        vector.addElement("COL_LOCA");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("COL_LOCA.ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberName("LOCA_ID");
        descriptor.setSequenceNumberFieldName("ID");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("id");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setGetMethodName("getId");
        directtofieldmapping.setSetMethodName("setId");
        directtofieldmapping.setFieldName("COL_LOCA.ID");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("area");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setGetMethodName("getArea");
        directtofieldmapping1.setSetMethodName("setArea");
        directtofieldmapping1.setFieldName("COL_LOCA.AREA");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("city");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setGetMethodName("getCity");
        directtofieldmapping2.setSetMethodName("setCity");
        directtofieldmapping2.setFieldName("COL_LOCA.CITY");
        descriptor.addMapping(directtofieldmapping2);

        addDescriptor(descriptor);
    }

    /**
     * TopLink generated descriptor method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildMenuDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(Menu.class);
        Vector vector = new Vector();
        vector.addElement("COL_MENU");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("COL_MENU.ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberName("MENU_ID");
        descriptor.setSequenceNumberFieldName("ID");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("id");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setGetMethodName("getId");
        directtofieldmapping.setSetMethodName("setId");
        directtofieldmapping.setFieldName("COL_MENU.ID");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("type");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setGetMethodName("getType");
        directtofieldmapping1.setSetMethodName("setType");
        directtofieldmapping1.setFieldName("COL_MENU.TYPE");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: ONETOMANYMAPPING
        org.eclipse.persistence.mappings.OneToManyMapping onetomanymapping = new org.eclipse.persistence.mappings.OneToManyMapping();
        onetomanymapping.setAttributeName("items");
        onetomanymapping.setIsReadOnly(false);
        onetomanymapping.setUsesIndirection(true);
        onetomanymapping.setGetMethodName("getItemsHolder");
        onetomanymapping.setSetMethodName("setItemsHolder");
        onetomanymapping.setReferenceClass(org.eclipse.persistence.testing.models.collections.MenuItem.class);
        onetomanymapping.setIsPrivateOwned(true);
        onetomanymapping.addTargetForeignKeyFieldName("COL_M_IT.MENU_ID", "COL_MENU.ID");
        onetomanymapping.useCollectionClass(java.util.LinkedList.class);
        descriptor.addMapping(onetomanymapping);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("owner");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(true);
        onetoonemapping.setGetMethodName("getOwnerHolder");
        onetoonemapping.setSetMethodName("setOwnerHolder");
        onetoonemapping.setReferenceClass(Restaurant.class);
        onetoonemapping.setIsPrivateOwned(false);
        onetoonemapping.addForeignKeyFieldName("COL_MENU.REST_ID", "COL_REST.ID");
        descriptor.addMapping(onetoonemapping);

        addDescriptor(descriptor);
    }

    /**
     * TopLink generated descriptor method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildMenuItemDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.collections.MenuItem.class);
        Vector vector = new Vector();
        vector.addElement("COL_M_IT");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("COL_M_IT.ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberName("MENU_ITEM_SEQ");
        descriptor.setSequenceNumberFieldName("ID");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("id");
        directtofieldmapping1.setGetMethodName("getId");
        directtofieldmapping1.setSetMethodName("setId");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("COL_M_IT.ID");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("name");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setGetMethodName("getName");
        directtofieldmapping2.setSetMethodName("setName");
        directtofieldmapping2.setFieldName("COL_M_IT.NAME");
        descriptor.addMapping(directtofieldmapping2);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping3 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping3.setAttributeName("price");
        directtofieldmapping3.setIsReadOnly(false);
        directtofieldmapping3.setGetMethodName("getPrice");
        directtofieldmapping3.setSetMethodName("setPrice");
        directtofieldmapping3.setFieldName("COL_M_IT.PRICE");
        descriptor.addMapping(directtofieldmapping3);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("menu");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(true);
        onetoonemapping.setGetMethodName("getMenuHolder");
        onetoonemapping.setSetMethodName("setMenuHolder");
        onetoonemapping.setReferenceClass(Menu.class);
        onetoonemapping.setIsPrivateOwned(false);
        onetoonemapping.addForeignKeyFieldName("COL_M_IT.MENU_ID", "COL_MENU.ID");
        descriptor.addMapping(onetoonemapping);

        addDescriptor(descriptor);
    }

    protected void buildPersonDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.collections.Person.class);
        java.util.Vector vector = new java.util.Vector();
        vector.addElement("COL_PERS");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("COL_PERS.ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setSequenceNumberName("PERSON_SEQ");
        descriptor.setSequenceNumberFieldName("ID");
        descriptor.getInheritancePolicy().setClassIndicatorFieldName("CLASS");
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);
        descriptor.getInheritancePolicy().addClassIndicator(Diner.class, "D");
        descriptor.getInheritancePolicy().addClassIndicator(org.eclipse.persistence.testing.models.collections.Waiter.class, "W");
        descriptor.getInheritancePolicy().addClassIndicator(org.eclipse.persistence.testing.models.collections.Person.class, "P");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("firstName");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setGetMethodName("getFirstName");
        directtofieldmapping.setSetMethodName("setFirstName");
        directtofieldmapping.setFieldName("COL_PERS.F_NAME");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("id");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setGetMethodName("getId");
        directtofieldmapping1.setSetMethodName("setId");
        directtofieldmapping1.setFieldName("COL_PERS.ID");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("lastName");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setGetMethodName("getLastName");
        directtofieldmapping2.setSetMethodName("setLastName");
        directtofieldmapping2.setFieldName("COL_PERS.L_NAME");
        descriptor.addMapping(directtofieldmapping2);

        addDescriptor(descriptor);
    }

    protected void buildRestaurantDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(Restaurant.class);
        Vector vector = new Vector();
        vector.addElement("COL_REST");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("COL_REST.ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);
        descriptor.setSequenceNumberName("REST_ID");
        descriptor.setSequenceNumberFieldName("ID");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("id");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setGetMethodName("getId");
        directtofieldmapping.setSetMethodName("setId");
        directtofieldmapping.setFieldName("COL_REST.ID");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("name");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setGetMethodName("getName");
        directtofieldmapping1.setSetMethodName("setName");
        directtofieldmapping1.setFieldName("COL_REST.NAME");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: ONETOMANYMAPPING
        org.eclipse.persistence.mappings.OneToManyMapping onetomanymapping = new org.eclipse.persistence.mappings.OneToManyMapping();
        onetomanymapping.setAttributeName("menus");
        onetomanymapping.setIsReadOnly(false);
        onetomanymapping.setUsesIndirection(true);
        onetomanymapping.setGetMethodName("getMenusHolder");
        onetomanymapping.setSetMethodName("setMenusHolder");
        onetomanymapping.setReferenceClass(Menu.class);
        onetomanymapping.setIsPrivateOwned(true);
        onetomanymapping.addTargetForeignKeyFieldName("COL_MENU.REST_ID", "COL_REST.ID");
        onetomanymapping.useMapClass(java.util.Hashtable.class, "getKey");
        descriptor.addMapping(onetomanymapping);

        // SECTION: ONETOMANYMAPPING
        org.eclipse.persistence.mappings.OneToManyMapping onetomanymapping1 = new org.eclipse.persistence.mappings.OneToManyMapping();
        onetomanymapping1.setAttributeName("waiters");
        onetomanymapping1.setIsReadOnly(false);
        onetomanymapping1.setUsesIndirection(false);
        onetomanymapping1.setGetMethodName("getWaiters");
        onetomanymapping1.setSetMethodName("setWaiters");
        onetomanymapping1.setReferenceClass(org.eclipse.persistence.testing.models.collections.Waiter.class);
        onetomanymapping1.setIsPrivateOwned(true);
        onetomanymapping1.addTargetForeignKeyFieldName("COL_PERS.W_RST_ID", "COL_REST.ID");
        onetomanymapping1.useCollectionClass(java.util.ArrayList.class);
        descriptor.addMapping(onetomanymapping1);

        // SECTION: MANYTOMANYMAPPING
        org.eclipse.persistence.mappings.ManyToManyMapping manytomanymapping = new org.eclipse.persistence.mappings.ManyToManyMapping();
        manytomanymapping.setAttributeName("preferredCustomers");
        manytomanymapping.setIsReadOnly(false);
        manytomanymapping.setUsesIndirection(false);
        manytomanymapping.setGetMethodName("getPreferredCustomers");
        manytomanymapping.setSetMethodName("setPreferredCustomers");
        manytomanymapping.setReferenceClass(Diner.class);
        manytomanymapping.setIsPrivateOwned(false);
        manytomanymapping.setRelationTableName("COL_DI_R");
        manytomanymapping.addTargetRelationKeyFieldName("COL_DI_R.DINER_ID", "COL_PERS.ID");
        manytomanymapping.addSourceRelationKeyFieldName("COL_DI_R.REST_ID", "COL_REST.ID");
        manytomanymapping.useMapClass(java.util.Hashtable.class, "getLastName");
        descriptor.addMapping(manytomanymapping);

        // SECTION: MANYTOMANYMAPPING
        org.eclipse.persistence.mappings.ManyToManyMapping manytomanymapping1 = new org.eclipse.persistence.mappings.ManyToManyMapping();
        manytomanymapping1.setAttributeName("locations");
        manytomanymapping1.setIsReadOnly(false);
        manytomanymapping1.setUsesIndirection(false);
        manytomanymapping1.setGetMethodName("getLocations");
        manytomanymapping1.setSetMethodName("setLocations");
        manytomanymapping1.setReferenceClass(Location.class);
        manytomanymapping1.setIsPrivateOwned(false);
        manytomanymapping1.setRelationTableName("COL_R_LO");
        manytomanymapping1.addTargetRelationKeyFieldName("COL_R_LO.LOCA_ID", "COL_LOCA.ID");
        manytomanymapping1.addSourceRelationKeyFieldName("COL_R_LO.REST_ID", "COL_REST.ID");
        manytomanymapping1.useCollectionClass(ArrayList.class);
        descriptor.addMapping(manytomanymapping1);

        // SECTION: MANYTOMANYMAPPING - using TreeSet
        org.eclipse.persistence.mappings.ManyToManyMapping manytomanymapping2 = new org.eclipse.persistence.mappings.ManyToManyMapping();
        manytomanymapping2.setAttributeName("locations2");
        manytomanymapping2.setIsReadOnly(false);
        manytomanymapping2.setUsesIndirection(false);
        manytomanymapping2.setGetMethodName("getLocations2");
        manytomanymapping2.setSetMethodName("setLocations2");
        manytomanymapping2.setReferenceClass(Location.class);
        manytomanymapping2.setIsPrivateOwned(false);
        manytomanymapping2.setRelationTableName("COL_R_LO2");
        manytomanymapping2.addTargetRelationKeyFieldName("COL_R_LO2.LOCA_ID", "COL_LOCA.ID");
        manytomanymapping2.addSourceRelationKeyFieldName("COL_R_LO2.REST_ID", "COL_REST.ID");
        manytomanymapping2.useSortedSetClass(TreeSet.class, Restaurant
                                             .getLocationComparator());
        descriptor.addMapping(manytomanymapping2);

        // SECTION: DIRECTCOLLECTIONMAPPING
        org.eclipse.persistence.mappings.DirectCollectionMapping directcollectionmapping = new org.eclipse.persistence.mappings.DirectCollectionMapping();
        directcollectionmapping.setAttributeName("slogans");
        directcollectionmapping.setUsesIndirection(true);
        directcollectionmapping.setGetMethodName("getSlogansHolder");
        directcollectionmapping.setSetMethodName("setSlogansHolder");
        directcollectionmapping.setDirectFieldName("COL_SLOG.SLOGAN");
        directcollectionmapping.setReferenceTableName("COL_SLOG");
        directcollectionmapping.addReferenceKeyFieldName("COL_SLOG.REST_ID", "COL_REST.ID");
        directcollectionmapping.useCollectionClass(ArrayList.class);
        descriptor.addMapping(directcollectionmapping);

        // Mapping used to test direct-collection converter support.
        org.eclipse.persistence.mappings.DirectCollectionMapping servicesMapping = new org.eclipse.persistence.mappings.DirectCollectionMapping();
        servicesMapping.setAttributeName("services");
        servicesMapping.setUsesIndirection(true);
        servicesMapping.setDirectFieldName("COL_SERVICES.SERVICE");
        servicesMapping.setReferenceTableName("COL_SERVICES");
        servicesMapping.addReferenceKeyFieldName("COL_SERVICES.REST_ID", "COL_REST.ID");
        servicesMapping.useCollectionClass(ArrayList.class);
        ObjectTypeConverter servicesConverter = new ObjectTypeConverter();
        servicesConverter.addConversionValue("AC", "Air Conditioning");
        servicesConverter.addConversionValue("RESV", "Reservations Required");
        servicesConverter.addConversionValue("DRSCD", "Formal Attire");
        servicesMapping.setValueConverter(servicesConverter);
        descriptor.addMapping(servicesMapping);

        // Mapping used to test direct-map converter support.
        org.eclipse.persistence.mappings.DirectMapMapping licensesMapping = new org.eclipse.persistence.mappings.DirectMapMapping();
        licensesMapping.setAttributeName("licenses");
        licensesMapping.setDirectKeyFieldName("COL_LICENSE.LICENSE");
        licensesMapping.setDirectFieldName("COL_LICENSE.STATUS");
        licensesMapping.setReferenceTableName("COL_LICENSE");
        licensesMapping.addReferenceKeyFieldName("COL_LICENSE.REST_ID", "COL_REST.ID");
        licensesMapping.useTransparentMap();
        ObjectTypeConverter licensesKeyConverter = new ObjectTypeConverter();
        licensesKeyConverter.addConversionValue("AL", "Alcohol License");
        licensesKeyConverter.addConversionValue("FD", "Food License");
        licensesKeyConverter.addConversionValue("SM", "Smoking License");
        licensesKeyConverter.addConversionValue("SL", "Site Licence");
        licensesMapping.setKeyConverter(licensesKeyConverter);
        TypeConversionConverter licensesValueConverter = new TypeConversionConverter();
        licensesValueConverter.setObjectClass(Boolean.class);
        licensesValueConverter.setDataClass(Integer.class);
        licensesMapping.setValueConverter(licensesValueConverter);
        descriptor.addMapping(licensesMapping);

        addDescriptor(descriptor);
    }

    /**
     * TopLink generated descriptor method.
     * <b>WARNING</b>: This code was generated by an automated tool.
     * Any changes will be lost when the code is re-generated
     */
    protected void buildWaiterDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.collections.Waiter.class);
        java.util.Vector vector = new java.util.Vector();
        vector.addElement("COL_PERS");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("COL_PERS.ID");
        descriptor.getInheritancePolicy().setParentClass(org.eclipse.persistence.testing.models.collections.Person.class);

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("specialty");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setGetMethodName("getSpecialty");
        directtofieldmapping.setSetMethodName("setSpecialty");
        directtofieldmapping.setFieldName("COL_PERS.SPECIALT");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("employer");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(true);
        onetoonemapping.setGetMethodName("getEmployerHolder");
        onetoonemapping.setSetMethodName("setEmployerHolder");
        onetoonemapping.setReferenceClass(Restaurant.class);
        onetoonemapping.setIsPrivateOwned(false);
        onetoonemapping.addForeignKeyFieldName("COL_PERS.W_RST_ID", "COL_REST.ID");
        descriptor.addMapping(onetoonemapping);

        addDescriptor(descriptor);
    }
}
