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
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 *     05/23/2008-1.0M8 Guy Pelletier 
 *       - 211330: Add attributes-complete support to the EclipseLink-ORM.XML Schema
 *     06/20/2008-1.0 Guy Pelletier 
 *       - 232975: Failure when attribute type is generic
 *     09/23/2008-1.1 Guy Pelletier 
 *       - 241651: JPA 2.0 Access Type support
 *     10/01/2008-1.1 Guy Pelletier 
 *       - 249329: To remain JPA 1.0 compliant, any new JPA 2.0 annotations should be referenced by name
 *     02/06/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 2)
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     04/24/2009-2.0 Guy Pelletier 
 *       - 270011: JPA 2.0 MappedById support
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metadata.accessors.objects;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Version;

import org.eclipse.persistence.annotations.BasicCollection;
import org.eclipse.persistence.annotations.BasicMap;
import org.eclipse.persistence.annotations.ReadTransformer;
import org.eclipse.persistence.annotations.VariableOneToOne;
import org.eclipse.persistence.annotations.WriteTransformer;
import org.eclipse.persistence.annotations.WriteTransformers;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.indirection.ValueHolderInterface;
import org.eclipse.persistence.internal.jpa.metadata.MetadataConstants;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
//import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;

/**
 * INTERNAL:
 * Parent object that is used to hold onto a valid JPA decorated method
 * field or class.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 1.0
 */
public class MetadataAnnotatedElement extends MetadataAccessibleObject {
    public static final String JPA_PERSISTENCE_PACKAGE_PREFIX = "javax.persistence";
    public static final String ECLIPSELINK_PERSISTENCE_PACKAGE_PREFIX = "org.eclipse.persistence.annotations";
    
    /** The name of the element, i.e. class name, field name, method name. */
    private String m_name;
    /** Defines elements modifiers, i.e. private/static/transient. */
    private int modifiers;
    /** Used to cache the type metadata class, but cannot be cached in the case of generics. */
    private MetadataClass m_rawClass;
    /**
     * Defines the generic types of the elements type.
     * This is null if no generics are used.
     * This is a list of class/type names from the class/field/method signature.
     * The size of the list varries depending on how many generics are present,
     * i.e.
     * - Map<String, Long> -> ["java.util.Map", "java.lang.String", "java.lang.Long"]
     * - Beverage<T> extends Object -> [T, :, java.lang.Object, java.lang.Object]
     */
    private List<String> genericType;
    /** Defines the field type, or method return type class name. */
    private String type;
    /** Defines the attribute name of a field, or property name of a method. */
    private String m_attributeName;
    /** Stores any annotations defined for the element, keyed by annotation name. */
    private Map<String, MetadataAnnotation> m_annotations = new HashMap<String, MetadataAnnotation>();
    
    /**
     * INTERNAL:
     */
    public MetadataAnnotatedElement(MetadataLogger logger) {
        super(logger);
    }

    /**
     * INTERNAL:
     * Return the annotated element for this accessor. Note: This method does 
     * not check against a metadata complete.
     */
    public MetadataAnnotation getAnnotation(Class annotation) {
        if (m_annotations == null) {
            return null;
        }
        return m_annotations.get(annotation.getName());
    }
    
    /**
     * INTERNAL:
     * Return the annotated element for this accessor.
     */
    public MetadataAnnotation getAnnotation(String annotationClassName, MetadataDescriptor descriptor) {
        if (m_annotations == null) {
            return null;
        }
        MetadataAnnotation annotation = m_annotations.get(annotationClassName);
        
        if (annotation != null && descriptor.ignoreAnnotations()) {
            getLogger().logWarningMessage(MetadataLogger.IGNORE_ANNOTATION, annotation, this);
            return null;
        } else {
            return annotation;
        }
    }
    
    /**
     * INTERNAL:
     * Return the annotations of this accessible object.
     */
    public Map<String, MetadataAnnotation> getAnnotations(){
        return m_annotations;
    }
    
    /**
     * INTERNAL:
     */
    public String getAttributeName() {
        return m_attributeName;
    }
    
    /**
     * INTERNAL:
     */
    protected int getDeclaredAnnotationsCount(MetadataDescriptor descriptor) {
        if (descriptor.ignoreAnnotations() || (m_annotations == null)) {
            return 0;
        }
        return m_annotations.size(); 
    }
    
    /**
     * INTERNAL:
     * This should only be called for accessor's of type Map. It will return
     * the map key type if generics are used, null otherwise.
     */
    public MetadataClass getMapKeyClass(MetadataDescriptor descriptor) {
        if (isGenericCollectionType()) {
            // The Map key may be a generic itself, or just the class value.
            String type = descriptor.getGenericType(this.genericType.get(2));
            if (type != null) {
                return MetadataFactory.getClassMetadata(type);
            }
            return MetadataFactory.getClassMetadata(this.genericType.get(1));
        } else {
            return null;
        }
    }
    
    /**
     * INTERNAL:
     */
    public String getName() {
        return m_name;
    }
    
    /**
     * INTERNAL:
     * Return the raw class for this accessible object. E.g. For an 
     * accessible object with a type of java.util.Collection<Employee>, this 
     * method will return java.util.Collection. 
     * @See getReferenceClassFromGeneric() to get Employee.class back.
     */
    public MetadataClass getRawClass(MetadataDescriptor descriptor) {
        if (m_rawClass == null) {
            if (isGenericType()) {
                String type = descriptor.getGenericType(getGenericType().get(0));
                if (type == null) {
                    return MetadataFactory.getClassMetadata("java.lang.String");
                }
                return MetadataFactory.getClassMetadata(type);
            }
            return MetadataFactory.getClassMetadata(getType());
        }
        
        return m_rawClass;    
    }
    
    /**
     * INTERNAL:
     * Return the reference class from the generic specification on this 
     * accessible object.
     * Here is what you will get back from this method given the following
     * scenarios:
     * 1 - public Collection<String> getTasks() => String.class
     * 2 - public Map<String, Integer> getTasks() => Integer.class
     * 3 - public Employee getEmployee() => null
     * 4 - public Collection getTasks() => null
     * 5 - public Map getTasks() => null
     * 6 - public Collection<byte[]> getAudio() => byte[].class
     * TODO: we don't handle multiple levels of generics too well (or at all really :-( )
     */
    public MetadataClass getReferenceClassFromGeneric(MetadataDescriptor descriptor) {
        if (isGenericCollectionType()) {
            // TODO: This is guessing, need to be more logical.
            // Collection<String> -> [Collection, String], get element class.
            String elementClass = this.genericType.get(1);
            // If size is greater than 4 then assume it is a double generic Map,
            // Map<T, Phone> -> [Map, T, Z, T, X]
            if (this.genericType.size() > 4) {
                elementClass = this.genericType.get(4);
            } else if (this.genericType.size() > 3) {
                // If size is greater than 3 then assume it is a generic Map,
                // Map<T, Phone> -> [Map, T, Z, Phone]
                elementClass = this.genericType.get(3);
            } else if (this.genericType.size() > 2) {
                // If size is greater than 2 then assume it is a Map,
                // Map<String, Phone> -> [Map, String, Phone]
                elementClass = this.genericType.get(2);
            }
            if (elementClass.length() == 1) {
                // Assume is a generic type variable, find real type.
                elementClass = descriptor.getGenericType(elementClass);
            }            
            return MetadataFactory.getClassMetadata(elementClass);
        } else {
            return null;
        }
    }
    
    /**
     * INTERNAL:
     * Return true if this accessible object has 1 or more declared 
     * persistence annotations.
     */
    public boolean hasDeclaredAnnotations(MetadataDescriptor descriptor) {
        return getDeclaredAnnotationsCount(descriptor) > 0;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessible object has 2 or more declared 
     * persistence annotations.
     */
    public boolean hasMoreThanOneDeclaredAnnotation(MetadataDescriptor descriptor) {
        return getDeclaredAnnotationsCount(descriptor) > 1;
    }
    
    /** 
     * INTERNAL:
     * Indicates whether the specified annotation is actually not present on 
     * this accessible object. Used for defaulting. Need this check since the
     * isAnnotationPresent calls can return a false when true because of the
     * meta-data complete feature.
     */
    public boolean isAnnotationNotPresent(Class annotation) {
        return ! isAnnotationPresent(annotation);
   }
    
    /** 
     * INTERNAL:
     * Indicates whether the specified annotation is present on this accessible
     * object. NOTE: Calling this method directly does not take any metadata
     * complete flag into consideration. Look at the other isAnnotationPresent
     * methods that takes a descriptor. 
     */
    public boolean isAnnotationPresent(Class annotation) {
        return getAnnotation(annotation) != null;
    }
    
    /** 
     * INTERNAL:
     * Indicates whether the specified annotation is present on java class
     * for the given descriptor metadata. 
     */
    public boolean isAnnotationPresent(Class annotationClass, MetadataDescriptor descriptor) {
        MetadataAnnotation annotation = getAnnotation(annotationClass);
        
        if (annotation != null && descriptor.ignoreAnnotations()) {
            getLogger().logWarningMessage(MetadataLogger.IGNORE_ANNOTATION, annotation, this);
            return false;
        } else {
            return annotation != null;
        }
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a basic mapping.
     */
    public boolean isBasic(MetadataDescriptor descriptor) {
        return isAnnotationPresent(Basic.class, descriptor) ||
               isAnnotationPresent(Lob.class, descriptor) ||
               isAnnotationPresent(Temporal.class, descriptor) ||
               isAnnotationPresent(Enumerated.class, descriptor);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a basic collection mapping.
     */
    public boolean isBasicCollection(MetadataDescriptor descriptor) {
        return isAnnotationPresent(BasicCollection.class, descriptor);
    }
    
    /**
     * INTERNAL: 
     * Return true if this accessor represents a basic collection mapping.
     */
    public boolean isBasicMap(MetadataDescriptor descriptor) {
        return isAnnotationPresent(BasicMap.class, descriptor);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents an id mapping.
     */
    public boolean isDerivedId(MetadataDescriptor descriptor) {
        return isId(descriptor) && (isOneToOne(descriptor) || isManyToOne(descriptor));
    }
    
    /**
     * INTERNAL: 
     * Return true if this accessor represents an element collection mapping.
     */
    public boolean isElementCollection(MetadataDescriptor descriptor) {
        return isAnnotationPresent(ElementCollection.class, descriptor);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents an aggregate mapping. True is
     * returned if an Embedded annotation is found or if an Embeddable 
     * annotation is found on the raw/reference class.
     */
    public boolean isEmbedded(MetadataDescriptor descriptor) {
        if (isAnnotationNotPresent(Embedded.class) && isAnnotationNotPresent(EmbeddedId.class)) {
            MetadataClass rawClass = getRawClass(descriptor);
            return (rawClass.isAnnotationPresent(Embeddable.class) || descriptor.getProject().hasEmbeddable(rawClass));
        } else {
            // Still need to make the call since we may need to ignore it
            // because of meta-data complete.
            return isAnnotationPresent(Embedded.class, descriptor);
        }
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents an aggregate id mapping.
     */
    public boolean isEmbeddedId(MetadataDescriptor descriptor) {
        return isAnnotationPresent(EmbeddedId.class, descriptor);
    }
    
    /**
     * INTERNAL:
     * Method to return whether a collection type is a generic.
     */
    public boolean isGenericCollectionType() {
        return (this.genericType != null) && (this.genericType.size() > 1);
    }
    
    /**
     * INTERNAL:
     * Method to return whether a type is a generic.
     */
    public boolean isGenericType() {
        return (this.genericType != null)
                    && (this.genericType.size() > 1)
                    && (this.genericType.get(0).length() == 1);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents an id mapping.
     */
    public boolean isId(MetadataDescriptor descriptor) {
        return isAnnotationPresent(Id.class, descriptor);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents an id mapping.
     */
    public boolean isDerivedIdClass(MetadataDescriptor descriptor) {
        return descriptor.isEmbeddable() && descriptor.getProject().isIdClass(getRawClass(descriptor));
    }
    
    /**
     * INTERNAL:
     * Return true if this field accessor represents a m-m relationship.
     */
    public boolean isManyToMany(MetadataDescriptor descriptor) {
        return isAnnotationPresent(ManyToMany.class, descriptor);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a m-1 relationship.
     */
    public boolean isManyToOne(MetadataDescriptor descriptor) {
        return isAnnotationPresent(ManyToOne.class, descriptor);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a 1-m relationship.
     */
    public boolean isOneToMany(MetadataDescriptor descriptor) {
        if (isAnnotationNotPresent(OneToMany.class) && ! descriptor.ignoreDefaultMappings()) {
            if (isGenericCollectionType() && isSupportedToManyCollectionClass(descriptor) && descriptor.getProject().hasEntity(getReferenceClassFromGeneric(descriptor))) {
                getLogger().logConfigMessage(MetadataLogger.ONE_TO_MANY_MAPPING, this);
                return true;
            }
            
            return false;
        } else {
            // Still need to make the call since we may need to ignore it
            // because of meta-data complete.
            return isAnnotationPresent(OneToMany.class, descriptor);
        }
    }
    
    /**
     * INTERNAL: 
     * Return true if this accessor represents a 1-1 relationship.
     */
    public boolean isOneToOne(MetadataDescriptor descriptor) {        
        if (isAnnotationNotPresent(OneToOne.class) && ! descriptor.ignoreDefaultMappings()) {    
            if (descriptor.getProject().hasEntity(getRawClass(descriptor)) && ! isEmbedded(descriptor)) {
                getLogger().logConfigMessage(MetadataLogger.ONE_TO_ONE_MAPPING, this);
                return true;
            } else {
                return false;
            }
        } else {
            // Still need to make the call since we may need to ignore it
            // because of meta-data complete.
            return isAnnotationPresent(OneToOne.class, descriptor);
        }
    }
    
    /**
     * INTERNAL:
     * Method to return whether a class is a supported direct collection class.
     * Changed in 221577 to allow types assignable to Collection, Set, List and 
     * Map rather than strict equality.
     */
    public boolean isSupportedCollectionClass(MetadataDescriptor descriptor) {
        MetadataClass rawClass = getRawClass(descriptor);
        
        return rawClass.isCollection();
    }
    
    /**
     * INTERNAL:
     * Method to return whether a class is a supported direct map collection
     * class. Changed in 221577 to allow types assignable to Collection, Set, 
     * List and Map rather than strict equality.
     */
    public boolean isSupportedMapClass(MetadataDescriptor descriptor) {
        return getRawClass(descriptor).isMap(); 
    }
    
    /**
     * INTERNAL:
     * Method to return whether a class is a supported one to many collection
     * class.  
     */
    public boolean isSupportedToManyCollectionClass(MetadataDescriptor descriptor) {
        return isSupportedCollectionClass(descriptor) || isSupportedMapClass(descriptor);
    }

    /**
     * INTERNAL:
     * Return true if this accessor represents an transformation mapping.
     */
    public boolean isTransformation(MetadataDescriptor descriptor) {
        return isAnnotationPresent(ReadTransformer.class, descriptor) ||
               isAnnotationPresent(WriteTransformers.class, descriptor) ||
               isAnnotationPresent(WriteTransformer.class, descriptor);
    }
    
    /**
     * INTERNAL:
     * When processing the inverse accessors to an explicit access setting,
     * their must be an Access(FIELD) or Access(PROPERTY) present for the
     * element to be processed. Otherwise, it is ignored.
     */
    protected boolean isValidPersistenceElement(boolean mustBeExplicit, String explicitType, MetadataDescriptor descriptor) {
        if (mustBeExplicit) {
            MetadataAnnotation annotation = getAnnotation(MetadataConstants.ACCESS_ANNOTATION, descriptor);
            
            if (annotation == null) {
                return false;
            } else {
                String access = (String)annotation.getAttribute("value");
                
                if (! access.equals(explicitType)) {
                    throw ValidationException.invalidExplicitAccessTypeSpecified(this, descriptor.getJavaClass(), explicitType);
                }
            }
        }
        
        return true;
    }
    
    /**
     * INTERNAL:
     * Return true if the modifiers are not transient, static or abstract.
     */
    protected boolean isValidPersistenceElement(int modifiers) {
        return ! (Modifier.isTransient(modifiers) || Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers));
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a variable 1-1 relationship.
     * The method will return true if one of the following conditions is met:
     *  - There is a VariableOneToOne annotation present, or
     *  - The raw class is an interface and not a collection or map, nor a 
     *    ValueHolderInterface (and an exclude default mappings flag is not set)
     */
    public boolean isVariableOneToOne(MetadataDescriptor descriptor) {
        if (isAnnotationNotPresent(VariableOneToOne.class) && ! descriptor.ignoreDefaultMappings()) {
            if (getRawClass(descriptor).isInterface() && 
                    ! getRawClass(descriptor).isMap() && 
                    ! getRawClass(descriptor).isCollection() &&
                    ! getRawClass(descriptor).extendsInterface(ValueHolderInterface.class)) {
                getLogger().logConfigMessage(MetadataLogger.VARIABLE_ONE_TO_ONE_MAPPING, this);
                return true;
            }
            
            return false;
        } else {
            // Still need to make the call since we may need to ignore it
            // because of meta-data complete.
            return isAnnotationPresent(VariableOneToOne.class, descriptor);
        }
    }
    
    /**
     * INTERNAL: 
     * Return true if this accessor represents a version mapping.
     */
    public boolean isVersion(MetadataDescriptor descriptor) {
        return isAnnotationPresent(Version.class, descriptor);
    }
    
    /**
     * INTERNAL:
     */
    protected void setAttributeName(String attributeName) {
        m_attributeName = attributeName;
    }
    
    /**
     * INTERNAL:
     */
    protected void setName(String name) {
        m_name = name;
    }

    public int getModifiers() {
        return modifiers;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }
    
    public int hashCode() {
        return getName().hashCode();
    }
    
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (getName() == null) {
            return ((MetadataAnnotatedElement)object).getName() == null;
        }
        return (object.getClass() == getClass())
                && getName().equals(((MetadataAnnotatedElement)object).getName());
    }
    
    public String toString() {
        String className = getClass().getSimpleName();
        return className.substring("Metadata".length(), className.length()).toLowerCase() + " " + getName();
    }

    public List<String> getGenericType() {
        return genericType;
    }

    public void setGenericType(List<String> genericType) {
        this.genericType = genericType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
