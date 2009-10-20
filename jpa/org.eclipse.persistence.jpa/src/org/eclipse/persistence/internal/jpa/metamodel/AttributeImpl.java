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
 *     03/19/2009-2.0  dclarke  - initial API start    
 *     06/30/2009-2.0  mobrien - finish JPA Metadata API modifications in support
 *       of the Metamodel implementation for EclipseLink 2.0 release involving
 *       Map, ElementCollection and Embeddable types on MappedSuperclass descriptors
 *       - 266912: JPA 2.0 Metamodel API (part of the JSR-317 EJB 3.1 Criteria API)  
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metamodel;

import java.io.Serializable;
import java.lang.reflect.Member;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.internal.descriptors.InstanceVariableAttributeAccessor;
import org.eclipse.persistence.internal.descriptors.MethodAttributeAccessor;
import org.eclipse.persistence.mappings.AttributeAccessor;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.OneToOneMapping;
import org.eclipse.persistence.mappings.VariableOneToOneMapping;

/**
 * <p>
 * <b>Purpose</b>: Provides the implementation for the Attribute interface 
 *  of the JPA 2.0 Metamodel API (part of the JSR-317 EJB 3.1 Criteria API)
 * <p>
 * <b>Description</b>:
 * An attribute of a Java type  
 * 
 * @see javax.persistence.metamodel.Attribute
 * 
 * @since EclipseLink 1.2 - JPA 2.0
 * @param <X> The represented type that contains the attribute
 * @param <T> The type of the represented attribute
 *  
 */ 
public abstract class AttributeImpl<X, T> implements Attribute<X, T>, Serializable {

    /** the ManagedType associated with this attribute **/
    private ManagedTypeImpl<X> managedType;

    /** The databaseMapping associated with this attribute **/
    private DatabaseMapping mapping;
    

    /**
     * INTERNAL:
     * 
     * @param managedType
     * @param mapping
     */
    protected AttributeImpl(ManagedTypeImpl<X> managedType, DatabaseMapping mapping) {
        this.mapping = mapping;
        // Cache this Attribute on the mapping
        this.mapping.setProperty(getClass().getName(), this);
        this.managedType = managedType;
    }

    /**
     *  Return the managed type representing the type in which 
     *  the attribute was declared.
     *  @return declaring type
     */
    public ManagedType<X> getDeclaringType() {
        return getManagedTypeImpl();
    }

    /**
     * INTERNAL:
     * Return the Descriptor associated with this attribute 
     * @return
     */
    protected RelationalDescriptor getDescriptor() {
        return getManagedTypeImpl().getDescriptor();
    }

    /**
     * Return the java.lang.reflect.Member for the represented attribute. 
     * In the case of property access the get method will be returned
     * 
     * @return corresponding java.lang.reflect.Member
     */
    public Member getJavaMember() {
        AttributeAccessor accessor = getMapping().getAttributeAccessor();

        if (accessor.isMethodAttributeAccessor()) {
            return ((MethodAttributeAccessor) accessor).getGetMethod();
        }

        Member aMember = ((InstanceVariableAttributeAccessor) accessor).getAttributeField();
        // For primitive and basic types - we should not return null - the attributeAccessor on the MappedSuperclass is not initialized - see
        // http://wiki.eclipse.org/EclipseLink/Development/JPA_2.0/metamodel_api#DI_95:_20091017:_Attribute.getJavaMember.28.29_returns_null_for_a_BasicType_on_a_MappedSuperclass_because_of_an_uninitialized_accessor
        // MappedSuperclasses need special handling to get their type from an inheriting subclass
        // Note: This code does not handle attribute overrides on any entity subclass tree - use descriptor initialization instead
        if(null == aMember) {
            if(this.getManagedTypeImpl().isMappedSuperclass()) {
                // get inheriting subtype member (without handling @override annotations)                
                AttributeImpl inheritingTypeMember = ((MappedSuperclassTypeImpl)this.getManagedTypeImpl())
                    .getMemberFromInheritingType(mapping.getAttributeName());
                // Verify we have an attributeAccessor
                aMember = ((InstanceVariableAttributeAccessor)inheritingTypeMember.getMapping()
                        .getAttributeAccessor()).getAttributeField();
            }
        }
        return aMember;
    }

    /**
     *  Return the Java type of the represented attribute.
     *  @return Java type
     */
    public abstract Class<T> getJavaType();

    /**
     * INTERNAL:
     * Return the managed type representing the type in which the member was
     * declared.
     * @return
     */
    public ManagedTypeImpl<X> getManagedTypeImpl() {
        return this.managedType;
    }

    /**
     * INTERNAL:
     * Return the databaseMapping that represents the type
     * @return
     */
    protected DatabaseMapping getMapping() {
        return this.mapping;
    }
    
    /**
     * INTERNAL: 
     * Return the concrete metamodel that this attribute is associated with.
     * @return MetamodelImpl
     */
    protected MetamodelImpl getMetamodel() {
        return this.managedType.getMetamodel();
    }
    
    /**
     * Return the name of the attribute.
     * @return name
     */
    public String getName() {
        return this.getMapping().getAttributeName();
    }

    /**
     *  Return the persistent attribute type for the attribute.
     *  @return persistent attribute type
     */
    public javax.persistence.metamodel.Attribute.PersistentAttributeType getPersistentAttributeType() {
        /**
         * process the following mappings
         * MANY_TO_ONE
         * ONE_TO_ONE
         * BASIC
         * EMBEDDED
         * MANY_TO_MANY
         * ONE_TO_MANY
         * ELEMENT_COLLECTION
         */
        if (getMapping().isDirectToFieldMapping()) {
            return PersistentAttributeType.BASIC;
        }
        if (getMapping().isAggregateObjectMapping()) {
            return PersistentAttributeType.EMBEDDED;
        }
        if (getMapping().isOneToManyMapping()) {
            return PersistentAttributeType.ONE_TO_MANY;
        }
        if (getMapping().isOneToOneMapping()) {
            return PersistentAttributeType.ONE_TO_ONE;
        }
        // Internally we treat m:1 as 1:1 - place this before the 1:1 check
        // http://wiki.eclipse.org/EclipseLink/Development/JPA_2.0/metamodel_api#DI_96:_20091019:_Attribute.getPersistentAttributeType.28.29_treats_OneToMany_the_same_as_OneToOne
        if (getMapping().isRelationalMapping() && 
                (getMapping() instanceof OneToOneMapping || getMapping() instanceof VariableOneToOneMapping)) {
            return PersistentAttributeType.MANY_TO_ONE;
        }
        // Test coverage required
        if (getMapping().isManyToManyMapping()) {
            return PersistentAttributeType.MANY_TO_MANY;
        }
        // Test coverage required
        return PersistentAttributeType.ELEMENT_COLLECTION;
    }
    
    /**
     *  Is the attribute an association.
     *  @return whether boolean indicating whether attribute 
     *          corresponds to an association
     */
    public boolean isAssociation() {
        // Only a ReferenceMapping that extends ObjectReferenceMapping returns true
        return getMapping().isReferenceMapping();
    }

    /**
     *  Is the attribute collection-valued.
     *  @return boolean indicating whether attribute is 
     *          collection-valued.<p>
     *  This will be true for the mappings CollectionMapping, AbstractCompositeCollectionMapping, 
     *  AbstractCompositeDirectCollectionMapping and their subclasses         
     *          
     */
    public boolean isCollection() {        
        return getMapping().isCollectionMapping();
    }
    
    /**
     * INTERNAL:
     * Return whether the attribute is plural or singular
     * @return
     */
    public abstract boolean isPlural();
}
