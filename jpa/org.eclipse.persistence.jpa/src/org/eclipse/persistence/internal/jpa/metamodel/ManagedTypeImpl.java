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
 *     07/06/2009-2.0  mobrien - 266912: Introduce IdentifiableTypeImpl between ManagedTypeImpl
 *       - EntityTypeImpl now inherits from IdentifiableTypeImpl instead of ManagedTypeImpl
 *       - MappedSuperclassTypeImpl now inherits from IdentifiableTypeImpl instead
 *       of implementing IdentifiableType indirectly 
 *       - implement Set<SingularAttribute<? super X, ?>> getSingularAttributes()
 *     07/09/2009-2.0  mobrien - 266912: implement get*Attribute() functionality
 *       - functions throw 2 types of IllegalArgumentExceptions depending on whether
 *         the member is missing or is the wrong type - see design issue #41
 *         http://wiki.eclipse.org/EclipseLink/Development/JPA_2.0/metamodel_api#DI_41:_When_to_throw_IAE_for_missing_member_or_wrong_type_on_get.28.29_call
 *     07/14/2009-2.0  mobrien - 266912: implement getDeclared*() functionality
 *       - Implement 14 functions for ManagedType - see design issue #43
 *         http://wiki.eclipse.org/EclipseLink/Development/JPA_2.0/metamodel_api#DI_43:_20090710:_Implement_getDeclaredX.28.29_methods
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metamodel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.PluralAttribute.CollectionType;

import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.indirection.IndirectSet;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.mappings.CollectionMapping;
import org.eclipse.persistence.mappings.DatabaseMapping;

/**
 * <p>
 * <b>Purpose</b>: Provides the implementation for the ManagedType interface 
 *  of the JPA 2.0 Metamodel API (part of the JSR-317 EJB 3.1 Criteria API)
 * <p>
 * <b>Description</b>:
 *  Instances of the type ManagedType represent entities, mapped superclasses
 *   and embeddable types.
 * 
 * @see javax.persistence.metamodel.ManagedType
 * 
 * @since EclipseLink 2.0 - JPA 2.0
 * @param <X> The represented type.  
 */ 
public abstract class ManagedTypeImpl<X> extends TypeImpl<X> implements ManagedType<X> {

    /** Native RelationalDescriptor that contains all the mappings of this type **/
    private RelationalDescriptor descriptor;

    /** The map of attributes keyed on attribute string name **/
    protected Map<String, Attribute<X,?>> members;

    /** Reference to the metamodel that this managed type belongs to **/
    protected MetamodelImpl metamodel;

    /**
     * INTERNAL:
     * This constructor will create a ManagedType but will not initialize its member mappings.
     * This is accomplished by delayed initialization in MetamodelImpl.initialize()
     * in order that we have access to all types when resolving relationships in mappings.
     * @param metamodel - the metamodel that this managedType is associated with
     * @param descriptor - the RelationalDescriptor that defines this managedType
     */
    protected ManagedTypeImpl(MetamodelImpl metamodel, RelationalDescriptor descriptor) {
        super(descriptor.getJavaClass());
        this.descriptor = descriptor;
        // the metamodel field must be instantiated prior to any *AttributeImpl instantiation which will use the metamodel
        this.metamodel = metamodel;
        // Cache the ManagedType on the descriptor 
        descriptor.setProperty(getClass().getName(), this);
        // Note: Full initialization of the ManagedType occurs during MetamodelImpl.initialize()
        //initialize(); // initialize after all types are instantiated
    }

    /**
     *  Return the attribute of the managed 
     *  type that corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return attribute with given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not present in the managed type     
     */
    public Attribute<X, ?> getAttribute(String name) {
        if(!members.containsKey(name)) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_not_present", 
                    new Object[] { name, this }));
        }
        return members.get(name);
    }
    
    /**
     *  Return the attributes of the managed type.
     */
    public Set<Attribute<? super X, ?>> getAttributes() {
        // We return a new Set instead of directly returning the Collection of values from the members HashMap
        return new HashSet<Attribute<? super X, ?>>(this.members.values());
    }
    

    /**
     *  Return the Collection-valued attribute of the managed type 
     *  that corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return CollectionAttribute of the given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not present in the managed type
     */    
    public CollectionAttribute<? super X, ?> getCollection(String name) {
        // Get the named collection from the set directly
        /*
         * Note: We do not perform type checking on the get(name)
         * If the type is not of the correct Attribute implementation class then
         * a possible CCE will be allowed to propagate to the client.
         * For example if a getCollection() is performed on a ListAttribute a CCE will occur
         */
        CollectionAttribute<? super X, ?> anAttribute = (CollectionAttribute<? super X, ?>)this.members.get(name);
        if(null == anAttribute) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_not_present", 
                    new Object[] { name, this }));
        }
        return anAttribute;
    }
    
    /**
     *  Return the Collection-valued attribute of the managed type 
     *  that corresponds to the specified name and Java element type.
     *  @param name  the name of the represented attribute
     *  @param elementType  the element type of the represented 
     *                      attribute
     *  @return CollectionAttribute of the given name and element
     *          type
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not present in the managed type
     */    
    public <E> CollectionAttribute<? super X, E> getCollection(String name, Class<E> elementType) {
        // We do not use getCollection(name) so that we can catch a possible CCE on the wrong attribute type
        Attribute<? super X, E> anAttribute = (Attribute<? super X, E>)this.members.get(name);
        if(null == anAttribute) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_not_present", 
                    new Object[] { name, this }));
        } else {
            // Throw appropriate IAException if required
            verifyAttributeTypeAndReturnType(anAttribute, elementType, CollectionType.COLLECTION);
        }
        return (CollectionAttribute<? super X, E>)anAttribute;
    }

    /**
     *  Return all collection-valued attributes of the managed type.
     *  @return collection valued attributes
     */
    public Set<PluralAttribute<? super X, ?, ?>> getCollections() {
        // Get all attributes and filter only for PluralAttributes
        Set<Attribute<? super X, ?>> allAttributes = this.getAttributes();
        // Is it better to add to a new Set or remove from an existing Set without a concurrentModificationException
        Set<PluralAttribute<? super X, ?, ?>> pluralAttributes = new HashSet<PluralAttribute<? super X, ?, ?>>();
        for(Iterator<Attribute<? super X, ?>> anIterator = allAttributes.iterator(); anIterator.hasNext();) {
            Attribute<? super X, ?> anAttribute = anIterator.next();
            // Add only CollectionType attributes
            if(anAttribute.isCollection()) {
                pluralAttributes.add((PluralAttribute<? super X, ?, ?>)anAttribute);

            }
        }
        return pluralAttributes;
    }

    /**
     *  Return the declared attribute of the managed
     *  type that corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return attribute with given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not declared in the managed type
     */
    public Attribute<X, ?> getDeclaredAttribute(String name){
        // get the attribute parameterized by <Owning type, return Type> - throw an IAE if not found (no need to check hierarchy)
        // Handles UC1 and UC2
        Attribute<X, ?> anAttribute = getAttribute(name);
        // Check the hierarchy for a declaration in the superclass(s) - keep moving up only when the attribute is not found
        ManagedTypeImpl aManagedSuperType = getManagedSuperType();
        if(null == aManagedSuperType) {
            return anAttribute;
        } else {
            // keep checking the hierarchy but skip this level
            if(aManagedSuperType.hasDeclaredAttribute(name)) {
                // Handles UC4 and UC5 
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                        "metamodel_managed_type_declared_attribute_not_present_but_is_on_superclass",
                        new Object[] { name, this }));
            } else {
                // Handles UC3 (normal case - attribute is not declared on a superclass)
                return anAttribute;
            }
        }
    }

    
    /**
     * All getDeclared*(name, *) function calls require navigation up the superclass tree
     * in order to determine if the member name is declared on the current managedType.<p>
     * If the attribute is found anywhere above on the superclass tree - then throw an IAE.
     *  
        Use Case Partitioning:
            - attribute positioning(none, current, 1st parent, Nth parent)
            - attribute type (right, wrong type)
            - attribute classification for current and parents (Entity, MappedSuperclass, embeddable?, Basic?)
            UC1) Attribute is not found on current attribute (regardless of what is on its' superclasses)
                    - throw IAException
            UC2) Attribute is found on current attribute but is of the wrong type
                    - throw IAException
            UC3) Attribute is found on on current managedType Entity/MappedSuperclass
                    (but not found anywhere on the supertype hierarchy - declared above)
                    In this case we do the reverse - keep checking only when attribute is null
                    - return attribute
            UC4) Attribute is declared on immediate superclass
                    - throw IAException            
            UC5) Attribute is declared on Nth superclass
                    - throw IAException
                
                We use two functions, one public, one a private recursive function.
            If the attribute is not found at the current level or above, or is of the wrong type - throw an IAException
            If the attribute is found then we still need to search to the 
                top of the hierarchy tree to verify it is not declared above 
                - if it is also not found above - return the attribute in this case only                                
     */
    
    /**
     *  Return the attributes declared by the managed type.
     */
    public Set<Attribute<X, ?>> getDeclaredAttributes() {
        // return only the set of attributes declared on this class - not via inheritance
        // Get all attributes and filter only for declared attributes
        Set<Attribute<X, ?>> allAttributes = new HashSet<Attribute<X, ?>>(this.members.values());;
        // Is it better to add to a new Set or remove from an existing Set without a concurrentModificationException
        Set<Attribute<X, ?>> declaredAttributes = new HashSet<Attribute<X, ?>>();
        for(Iterator<Attribute<X, ?>> anIterator = allAttributes.iterator(); anIterator.hasNext();) {
            Attribute<? super X, ?> anAttribute = anIterator.next();
            if(anAttribute.isCollection()) {
                declaredAttributes.add((Attribute<X, ?>)anAttribute);
            }
        }
        return declaredAttributes;
    }

    /**
     *  Return the Collection-valued attribute declared by the 
     *  managed type that corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return declared CollectionAttribute of the given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not declared in the managed type
     */
    public CollectionAttribute<X, ?> getDeclaredCollection(String name) {
        // return only a collection declared on this class - not via inheritance
        // Handles UC1 and UC2
        CollectionAttribute<X, ?> anAttribute = (CollectionAttribute<X, ?>) getCollection(name);
        // The following verification step will throw an appropriate IAException if required (we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;
    }

    /**
     *  Return the Collection-valued attribute declared by the 
     *  managed type that corresponds to the specified name and Java 
     *  element type.
     *  @param name  the name of the represented attribute
     *  @param elementType  the element type of the represented 
     *                      attribute
     *  @return declared CollectionAttribute of the given name and 
     *          element type
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not declared in the managed type
     */
    public <E> CollectionAttribute<X, E> getDeclaredCollection(String name, Class<E> elementType) {
        // return only a collection declared on this class - not via inheritance
        // Handles UC1 and UC2
        CollectionAttribute<X, E> anAttribute = (CollectionAttribute<X, E>) getCollection(name, elementType);
        // The following verification step will throw an appropriate IAException if required (type checking has been done, and we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get, (optionally a type check) and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;
    }

    /**
     *  Return all collection-valued attributes declared by the 
     *  managed type.
     *  @return declared collection valued attributes
     */
    public Set<PluralAttribute<X, ?, ?>> getDeclaredCollections() {
        // It is evident from the fact that we have only getAttributes(), getCollections() and getSingularAttributes() that a Collection is a superset of all Set, List and even Map
        // return only a set of collections declared on this class - not via inheritance
        // Get all collection attribute and filter only on declared ones
        Set<PluralAttribute<? super X, ?, ?>> pluralAttributes = this.getCollections();
        // Is it better to add to a new Set or remove from an existing Set without a concurrentModificationException
        Set<PluralAttribute<X, ?, ?>> declaredAttributes = new HashSet<PluralAttribute<X, ?, ?>>();
        // The set is a copy of the underlying metamodel attribute set - we will remove all SingularAttribute(s)
        for(Iterator<PluralAttribute<? super X, ?, ?>> anIterator = pluralAttributes.iterator(); anIterator.hasNext();) {
            PluralAttribute<? super X, ?, ?> anAttribute = anIterator.next();
            if(((TypeImpl)anAttribute.getElementType()).isManagedType()) {
                // check for declarations in the hierarchy and don't add if declared above
                //if(!((ManagedTypeImpl)anAttribute.getElementType()).hasDeclaredAttribute(anAttribute.getName())) {
                // add attributes that don't have superclasses automatically
                ManagedTypeImpl potentialSuperType = getManagedSuperType();
                if(null == potentialSuperType) {
                    declaredAttributes.add((PluralAttribute<X, ?, ?>)anAttribute);
                } else {
                    // add only if we reach the root without finding another declaration
                    if(!potentialSuperType.hasDeclaredAttribute(anAttribute.getName())) {
                        declaredAttributes.add((PluralAttribute<X, ?, ?>)anAttribute);
                    }
                }
            }
        }
        return declaredAttributes;
    }

    /**
     * INTERNAL:
     * Return an instance of a ManagedType based on the RelationalDescriptor parameter
     * @param metamodel
     * @param descriptor
     * @return
     */
    public static ManagedTypeImpl<?> create(MetamodelImpl metamodel, RelationalDescriptor descriptor) {
        // Get the ManagedType property on the descriptor if it exists
        ManagedTypeImpl<?> managedType = (ManagedTypeImpl<?>) descriptor.getProperty(ManagedTypeImpl.class.getName());

        // Create an Entity, Embeddable or MappedSuperclass
        if (null == managedType) {
            // The descriptor can be one of NORMAL, INTERFACE (not supported), AGGREGATE or AGGREGATE_COLLECTION
            // TODO: handle MappedSuperclass
            if (descriptor.isAggregateDescriptor()) {
                managedType = new EmbeddableTypeImpl(metamodel, descriptor);                
            //} else if (descriptor.isAggregateCollectionDescriptor()) {
            //    managedType = new EntityTypeImpl(metamodel, descriptor);
            } else {
                managedType = new EntityTypeImpl(metamodel, descriptor);
            }
        }

        return managedType;
    }

    /**
     *  Return the List-valued attribute declared by the managed 
     *  type that corresponds to the specified name and Java 
     *  element type.
     *  @param name  the name of the represented attribute
     *  @param elementType  the element type of the represented 
     *                      attribute
     *  @return declared ListAttribute of the given name and 
     *          element type
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not declared in the managed type
     */
    public <E> ListAttribute<X, E> getDeclaredList(String name, Class<E> elementType) {
        // get the attribute parameterized by <Owning type, return Type> - throw an IAE if not found (no need to check hierarchy)
        // Handles UC1 and UC2
        ListAttribute<X, E> anAttribute = (ListAttribute<X, E>) getList(name, elementType);
        // The following verification step will throw an appropriate IAException if required (type checking has been done, and we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get, (optionally a type check) and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;
    }

    /**
     *  Return the List-valued attribute declared by the managed 
     *  type that corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return declared ListAttribute of the given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not declared in the managed type
     */
    public ListAttribute<X, ?> getDeclaredList(String name) {
        // return only a List declared on this class - not via inheritance
        // Handles UC1 and UC2
        ListAttribute<X, ?> anAttribute = (ListAttribute<X, ?>) getList(name);
        // The following verification step will throw an appropriate IAException if required (we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;
    }

    /**
     *  Return the Map-valued attribute of the managed type that
     *  corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return MapAttribute of the given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not present in the managed type
     */
    public MapAttribute<X, ?, ?> getDeclaredMap(String name) {
        // return only a map declared on this class - not via inheritance
        // Handles UC1 and UC2
        MapAttribute<X, ?, ?> anAttribute = (MapAttribute<X, ?, ?>) getMap(name);
        // The following verification step will throw an appropriate IAException if required (we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;
    }

    /**
     *  Return the Map-valued attribute of the managed type that
     *  corresponds to the specified name and Java key and value
     *  types.
     *  @param name  the name of the represented attribute
     *  @param keyType  the key type of the represented attribute
     *  @param valueType  the value type of the represented attribute
     *  @return MapAttribute of the given name and key and value
     *  types
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not present in the managed type
     */
    public <K, V> MapAttribute<X, K, V> getDeclaredMap(String name, Class<K> keyType, Class<V> valueType) {
        // return only a map declared on this class - not via inheritance
        // Handles UC1 and UC2
        MapAttribute<X, K, V> anAttribute = (MapAttribute<X, K, V>) getMap(name, keyType, valueType);
        // The following verification step will throw an appropriate IAException if required (type checking has been done, and we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get, (optionally a type check) and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;

    }

    /**
     *  Return the Set-valued attribute declared by the managed type 
     *  that corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return declared SetAttribute of the given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not declared in the managed type
     */
    public SetAttribute<X, ?> getDeclaredSet(String name) {
        // return only a set declared on this class - not via inheritance
        // Handles UC1 and UC2
        SetAttribute<X, ?> anAttribute = (SetAttribute<X, ?>) getSet(name);
        // The following verification step will throw an appropriate IAException if required (we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;
    }

    /**
     *  Return the Set-valued attribute declared by the managed type 
     *  that corresponds to the specified name and Java element type.
     *  @param name  the name of the represented attribute
     *  @param elementType  the element type of the represented 
     *                      attribute
     *  @return declared SetAttribute of the given name and 
     *          element type
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not declared in the managed type
     */
    public <E> SetAttribute<X, E> getDeclaredSet(String name, Class<E> elementType) {
        // return only a set declared on this class - not via inheritance
        // Handles UC1 and UC2
        SetAttribute<X, E> anAttribute = (SetAttribute<X, E>) getSet(name, elementType);
        // The following verification step will throw an appropriate IAException if required (type checking has been done, and we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get, (optionally a type check) and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;
    }
    
    /**
     *  Return the declared single-valued attribute of the managed
     *  type that corresponds to the specified name in the
     *  represented type.
     *  @param name  the name of the represented attribute
     *  @return declared single-valued attribute of the given 
     *          name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not declared in the managed type
     */
    public SingularAttribute<X, ?> getDeclaredSingularAttribute(String name) {
        // return only a SingularAttribute declared on this class - not via inheritance
        // Handles UC1 and UC2
        SingularAttribute<X, ?> anAttribute = (SingularAttribute<X, ?>) getSingularAttribute(name);
        // The following verification step will throw an appropriate IAException if required (we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;
    }

    /**
     *  Return the declared single-valued attribute of the 
     *  managed type that corresponds to the specified name and Java 
     *  type in the represented type.
     *  @param name  the name of the represented attribute
     *  @param type  the type of the represented attribute
     *  @return declared single-valued attribute of the given 
     *          name and type
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not declared in the managed type
     */
    public <Y> SingularAttribute<X, Y> getDeclaredSingularAttribute(String name, Class<Y> type) {
        // return only a SingularAttribute declared on this class - not via inheritance
        // Handles UC1 and UC2
        SingularAttribute<X, Y> anAttribute = (SingularAttribute<X, Y>) getSingularAttribute(name, type);
        // The following verification step will throw an appropriate IAException if required (type checking has been done, and we can discard the return attribute here)
        getDeclaredAttribute(name);
        // We return an attribute that has passed through both a get, (optionally a type check) and a declared inheritance check
        // all of which would throw an IAException before the return below.
        return anAttribute;
    }

    /**
     *  Return the single-valued attributes declared by the managed
     *  type.
     *  @return declared single-valued attributes
     */
    public Set<SingularAttribute<X, ?>> getDeclaredSingularAttributes() {
        // return the set of SingularAttributes declared on this class - not via inheritance
        // Get all attributes and filter only for declared attributes
        Set<Attribute<X, ?>> allAttributes = new HashSet<Attribute<X, ?>>(this.members.values());;
        // Is it better to add to a new Set or remove from an existing Set without a concurrentModificationException
        Set<SingularAttribute<X, ?>> declaredAttributes = new HashSet<SingularAttribute<X, ?>>();
        for(Iterator<Attribute<X, ?>> anIterator = allAttributes.iterator(); anIterator.hasNext();) {
            Attribute<? super X, ?> anAttribute = anIterator.next();
            if(!anAttribute.isCollection()) {
                declaredAttributes.add((SingularAttribute<X, ?>)anAttribute);
            }
        }
        return declaredAttributes;

    }

    /**
     * INTERNAL:
     * Return the RelationalDescriptor associated with this ManagedType
     * @return
     */
    public RelationalDescriptor getDescriptor() {
        return this.descriptor;
    }

    /**
     *  Return the List-valued attribute of the managed type that
     *  corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return ListAttribute of the given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not present in the managed type
     */
    public ListAttribute<? super X, ?> getList(String name) {
        return getList(name, true);
    }
    
    /**
     * INTERNAL:
     * @param name
     * @param performNullCheck - flag on whether we should be doing an IAException check
     * @return
     */
    protected ListAttribute<? super X, ?> getList(String name, boolean performNullCheck) {
        /*
         * Note: We do not perform type checking on the get(name)
         * If the type is not of the correct Attribute implementation class then
         * a possible CCE will be allowed to propagate to the client.
         */
        ListAttribute<? super X, ?> anAttribute = (ListAttribute<? super X, ?>)this.members.get(name);
        if(performNullCheck && null == anAttribute) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_not_present", 
                    new Object[] { name, this }));
        }
        return anAttribute;
    }

    /**
     * INTERNAL:
     * Perform type checking on the attribute and return types of the named attribute.
     * This function will cause an IllegalArgumentException if any of the passed in types are incorrect.
     * @param anAttribute - the Attribute we are verifying 
     * @param attributeElementType - the java element or basic element type
     * @param aReturnCollectionType - the plural return type
     * @throws IllegalArgumentException if either type is wrong
     * @return void
     */
    private void verifyAttributeTypeAndReturnType(Attribute anAttribute, Class attributeElementType, CollectionType aReturnCollectionType) {
        // Check for plural or singular attribute
        if(anAttribute.isCollection()) {
            // check for CollectionAttribute
            if(((PluralAttribute)anAttribute).getCollectionType().equals(aReturnCollectionType)) {
                // check that the java class is correct (use BindableJavaType not elementType.getJavaType()                
                Class aBindableJavaClass = ((PluralAttribute)anAttribute).getBindableJavaType();
                if(attributeElementType != aBindableJavaClass) {                    
                    throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                        "metamodel_managed_type_attribute_type_incorrect", 
                        new Object[] { anAttribute.getName(), this, attributeElementType, aBindableJavaClass }));
                }
            } else {
                throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_return_type_incorrect", 
                    new Object[] { anAttribute.getName(), this, aReturnCollectionType, 
                            ((PluralAttribute)anAttribute).getCollectionType()}));
            }
        }
    }
    
    /**
     *  Return the List-valued attribute of the managed type that
     *  corresponds to the specified name and Java element type.
     *  @param name  the name of the represented attribute
     *  @param elementType  the element type of the represented 
     *                      attribute
     *  @return ListAttribute of the given name and element type
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not present in the managed type
     */
    public <E> ListAttribute<? super X, E> getList(String name, Class<E> elementType) {
        // We do not use getList(name) so that we can catch a possible CCE on the wrong attribute type
        ListAttribute<? super X, E> anAttribute = (ListAttribute<? super X, E>)this.members.get(name);
        if(null == anAttribute) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_not_present", 
                    new Object[] { name, this }));
        } else {
            // Throw appropriate IAException if required
            verifyAttributeTypeAndReturnType(anAttribute, elementType, CollectionType.LIST);
        }
        return (ListAttribute<? super X, E>)anAttribute;

    }

    /**
     * INTERNAL:
     * Return the ManagedType that represents the superType (superclass) of 
     * the current ManagedType.
     * 
     * @return ManagedType supertype or null if no superclass
     */
    private ManagedTypeImpl getManagedSuperType() {
        // Note this method provides the same functionality of the more specific IdentifiableType.superType but is general to ManagedTypeImpl
        ManagedTypeImpl<?> aSuperType = null;
        // Get the superType if it exists (without using IdentifiableType.superType)
        Class aSuperClass = this.getJavaType().getSuperclass();
        // The superclass for top-level types will be Object - which we will leave as a null supertype on the type
        if(null != aSuperClass && aSuperClass != ClassConstants.OBJECT) {
            // Get the managedType from the metamodel
            aSuperType = (ManagedTypeImpl<?>)this.getMetamodel().type(aSuperClass);            
        }
        return aSuperType;
    }
    
    
    /**
     *  Return the Map-valued attribute of the managed type that
     *  corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return MapAttribute of the given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not present in the managed type
     */
    public MapAttribute<? super X, ?, ?> getMap(String name) {
        /*
         * Note: We do not perform type checking on the get(name)
         * If the type is not of the correct Attribute implementation class then
         * a possible CCE will be allowed to propagate to the client.
         */
        MapAttribute<? super X, ?, ?> anAttribute = (MapAttribute<? super X, ?, ?>)this.members.get(name);
        if(null == anAttribute) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_not_present", 
                    new Object[] { name, this }));
        }
        return anAttribute;
        
    }

    /**
     *  Return the Map-valued attribute of the managed type that
     *  corresponds to the specified name and Java key and value
     *  types.
     *  @param name  the name of the represented attribute
     *  @param keyType  the key type of the represented attribute
     *  @param valueType  the value type of the represented attribute
     *  @return MapAttribute of the given name and key and value
     *  types
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not present in the managed type
     */
    public <K, V> MapAttribute<? super X, K, V> getMap(String name, Class<K> keyType, Class<V> valueType) {
        MapAttribute<? super X, K, V> anAttribute = (MapAttribute<? super X, K, V>)this.getMap(name);
        Class<V> aClass = anAttribute.getElementType().getJavaType();
        if(valueType != aClass) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_type_incorrect", 
                    new Object[] { name, this, valueType, aClass }));
        }
        return anAttribute;
    }
    
    /**
     * INTERNAL:
     * Return the Map of AttributeImpl members keyed by String.
     * @return
     */
    public java.util.Map<String, Attribute<X, ?>> getMembers() {
        return this.members;
    }

    /**
     * INTERNAL:
     * Return the Metamodel that this ManagedType is associated with.
     * @return
     */
    public MetamodelImpl getMetamodel() {
        return this.metamodel;
    }

    /**
     *  Return the Set-valued attribute of the managed type that
     *  corresponds to the specified name.
     *  @param name  the name of the represented attribute
     *  @return SetAttribute of the given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not present in the managed type
     */
    public SetAttribute<? super X, ?> getSet(String name) {
        /*
         * Note: We do not perform type checking on the get(name)
         * If the type is not of the correct Attribute implementation class then
         * a possible CCE will be allowed to propagate to the client.
         */
        SetAttribute<? super X, ?> anAttribute = (SetAttribute<? super X, ?>)this.members.get(name);
        if(null == anAttribute) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_not_present", 
                    new Object[] { name, this }));
        }
        return anAttribute;
    }
    
    /**
     *  Return the Set-valued attribute of the managed type that
     *  corresponds to the specified name and Java element type.
     *  @param name  the name of the represented attribute
     *  @param elementType  the element type of the represented 
     *                      attribute
     *  @return SetAttribute of the given name and element type
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not present in the managed type
     */
    public <E> SetAttribute<? super X, E> getSet(String name, Class<E> elementType) {
        SetAttribute<? super X, E> anAttribute = (SetAttribute<? super X, E>)getSet(name);
        Class<E> aClass = anAttribute.getElementType().getJavaType();
        if(elementType != aClass) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                "metamodel_managed_type_attribute_type_incorrect", 
                new Object[] { name, this, elementType, aClass.getName() }));
        }
        return anAttribute;
    }

    /**
     *  Return the single-valued attribute of the managed type that
     *  corresponds to the specified name in the represented type.
     *  @param name  the name of the represented attribute
     *  @return single-valued attribute with the given name
     *  @throws IllegalArgumentException if attribute of the given
     *          name is not present in the managed type
     */
    public SingularAttribute<? super X, ?> getSingularAttribute(String name) {
        Attribute<X, ?> anAttribute = getMembers().get(name);
        if(null == anAttribute) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                    "metamodel_managed_type_attribute_not_present", 
                    new Object[] { name, this }));
        }
        return (SingularAttribute<? super X, ?>)anAttribute;
    }

    /**
     *  Return the single-valued attribute of the managed 
     *  type that corresponds to the specified name and Java type 
     *  in the represented type.
     *  @param name  the name of the represented attribute
     *  @param type  the type of the represented attribute
     *  @return single-valued attribute with given name and type
     *  @throws IllegalArgumentException if attribute of the given
     *          name and type is not present in the managed type
     */
    public <Y> SingularAttribute<? super X, Y> getSingularAttribute(String name, Class<Y> type) {
        SingularAttribute<? super X, Y> anAttribute = (SingularAttribute<? super X, Y>)getSingularAttribute(name);
        Class<Y> aClass = anAttribute.getType().getJavaType();
        if(type != aClass) {
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage(
                "metamodel_managed_type_attribute_type_incorrect", 
                new Object[] { name, this, type, aClass }));
        }
        return anAttribute;
    }

    /**
     *  Return the single-valued attributes of the managed type.
     *  @return single-valued attributes
     */
    public Set<SingularAttribute<? super X, ?>> getSingularAttributes() {
        // Iterate the members set for attributes of type SingularAttribute
        //Set<SingularAttribute<? super X, ?>> singularAttributeSet = new HashSet<SingularAttribute<? super X, ?>>();
        Set singularAttributeSet = new HashSet<SingularAttribute<? super X, ?>>();
        for(Iterator<Attribute<X, ?>> anIterator = this.members.values().iterator(); anIterator.hasNext();) {
            AttributeImpl<? super X, ?> anAttribute = (AttributeImpl<? super X, ?>)anIterator.next();
            if(!anAttribute.isPlural()) {
                singularAttributeSet.add(anAttribute);
            }
        }
        return singularAttributeSet;
    }
    
    /**
     * INTERNAL:
     * Recursively search the superclass tree of the current managedType
     * for an attribute named "name".<p>
     * This internal function is used exclusively by the getDeclared*() calls on ManagedType objects.<p>
     * This function is type agnostic (Set, List, Map and Collection are treated as attributes)
     * @param attributeName - String name of possible declared attribute search
     * @return
     */
    private boolean hasDeclaredAttribute(String attributeName) {
        Attribute anAttribute = this.getMembers().get(attributeName);
        ManagedTypeImpl<?> aSuperType = getManagedSuperType();        
        
        // Keep searching the superType only when the attribute is not found == null
        if(null == anAttribute && null != aSuperType) {
            // recursive case            
            return aSuperType.hasDeclaredAttribute(attributeName);
        } else {
            // base case
            if(null != anAttribute) { // aSuperType == x (return regardless of whether the superType is null or not)
                return true;
            } else { // anAttribute == null && aSuperType == null
                // we reached the root without finding an attribute declaration
                return false;
            }
        }
    }
    
    /**
     * INTERNAL:
     * Initialize the members of this ManagedType based on the mappings defined on the descriptor.
     * We process the appropriate Map, List, Set, Collection or Object/primitive types.<p>
     * Initialization should occur after all types in the metamodel have been created already.
     * 
     */
    protected void initialize() { // TODO: Check all is*Policy() calls
        /*
         * Issue 1: The hierarchy of the Metamodel API has Collection alongside List, Set and Map.
         *              However, in a normal Java collections framework Collection is an 
         *              abstract superclass of List, Set and Map (with Map not really a Collection).
         *              We therefore need to treat Collection here as a peer of the other "collections".
         */
        this.members = new HashMap<String, Attribute<X, ?>>();

        // Get all mappings on the relationalDescriptor
        for (Iterator<DatabaseMapping> i = getDescriptor().getMappings().iterator(); i.hasNext();) {
            DatabaseMapping mapping = (DatabaseMapping) i.next();
            AttributeImpl<X, ?> member = null;

            // Tie into the collection hierarchy at a lower level
            if (mapping.isCollectionMapping()) {
                // Handle 1:m, n:m collection mappings
                CollectionMapping colMapping = (CollectionMapping) mapping;
                if (colMapping.getContainerPolicy().isMapPolicy()) {
                    // Handle Map type mappings
                    member = new MapAttributeImpl(this, colMapping);
                    // check mapping.attributeAcessor.attributeField.type=Collection
                } else if (colMapping.getContainerPolicy().isListPolicy()) { // TODO: isListPolicy() will return true for IndirectList (a lazy Collection)                    
                    // Handle List type mappings
                    member = new ListAttributeImpl(this, colMapping);
                } else {
                    // Handle Set type mappings (IndirectSet.isAssignableFrom(Set.class) == false)
                    if (colMapping.getContainerPolicy().getContainerClass().isAssignableFrom(Set.class) ||
                            colMapping.getContainerPolicy().getContainerClass().isAssignableFrom(IndirectSet.class)) {
                        member = new SetAttributeImpl(this, colMapping);
                    } else {
                        // Handle Collection type mappings as a default
                        member = new CollectionAttributeImpl(this, colMapping);
                    }
                }
            } else {
                // Handle 1:1 single object and direct mappings
                member = new SingularAttributeImpl(this, mapping);
            }

            this.members.put(mapping.getAttributeName(), member);
        }
    }
    
    /**
     * INTERNAL:
     * Return whether this type is identifiable.
     * This would be EntityType and MappedSuperclassType
     * @return
     */
    @Override
    public boolean isIdentifiableType() {
        return false;
    }

    /**
     * INTERNAL:
     * Return whether this type is identifiable.
     * This would be EmbeddableType as well as EntityType and MappedSuperclassType
     * @return
     */
    @Override
    public boolean isManagedType() {
        return true;
    }
   
    /**
     * Return the string representation of the receiver.
     */
    @Override
    public String toString() {
        return "ManagedTypeImpl[" + getDescriptor() + "]";
    }
}
