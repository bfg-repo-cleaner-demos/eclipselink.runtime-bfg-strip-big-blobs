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
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files  
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metadata.accessors.objects;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Basic;
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
import org.eclipse.persistence.indirection.ValueHolderInterface;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;

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
    
    private String m_name;
    private Class m_rawClass;
    private Type m_relationType;
    private String m_attributeName;
    private AnnotatedElement m_annotatedElement;
    private HashMap<String, Annotation> m_annotations;
    
    /**
     * INTERNAL:
     * Use this constructor when no logger is needed. That is, there is no
     * need to override or merge ORMetadata. That is, this accessible object
     * will not be tied to an ORMetadata object.
     */
    protected MetadataAnnotatedElement(AnnotatedElement annotatedElement) {
        super(annotatedElement, null);
        setAnnotatedElement(annotatedElement);
    }
    
    /**
     * INTERNAL:
     */
    public MetadataAnnotatedElement(AnnotatedElement annotatedElement, MetadataLogger logger) {
        super(annotatedElement, logger);
        setAnnotatedElement(annotatedElement);
    }
    
    /**
     * INTERNAL:
     */
    public MetadataAnnotatedElement(AnnotatedElement annotatedElement, XMLEntityMappings entityMappings) {
        super(entityMappings.getMappingFile(), entityMappings);
        setAnnotatedElement(annotatedElement);
    }
    
    /**
     * INTERNAL:
     * Return the actual field or method.
     */
    public AnnotatedElement getAnnotatedElement() {
        return m_annotatedElement;
    }

    /**
     * INTERNAL:
     * Return the annotated element for this accessor. Note: This method does 
     * not check against a metadata complete.
     */
    public <T extends Annotation> T getAnnotation(Class annotation) {
        return (T) m_annotations.get(annotation.getName());
    }
    
    /**
     * INTERNAL:
     * Return the annotated element for this accessor.
     */
    public <T extends Annotation> T getAnnotation(Class annotationClass, MetadataDescriptor descriptor) {
        Annotation annotation = m_annotations.get(annotationClass.getName());
        
        if (annotation != null && descriptor.ignoreAnnotations()) {
            getLogger().logWarningMessage(MetadataLogger.IGNORE_ANNOTATION, annotation, m_annotatedElement);
            return null;
        } else {
            return (T) annotation;
        }
    }
    
    /**
     * INTERNAL:
     * Return the annotations of this accessible object.
     */
    public Map<String, Annotation> getAnnotations(){
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
        return descriptor.ignoreAnnotations() ? 0 : m_annotations.size(); 
    }
    
    /**
     * INTERNAL:
     * Return the element of this accessible object.
     */
    public Object getElement() {
        return getAnnotatedElement();
    }
    
    /**
     * INTERNAL:
     * This should only be called for accessor's of type Map. It will return
     * the Map key type if generics are used, null otherwise.
     */
    public Class getMapKeyClass() {
        if (isGenericCollectionType()) {
            // By default, the reference class is equal to the relation
            // class. But if the relation class is a generic we need to 
            // extract and set the actual reference class from the generic. 
            return (Class) ((ParameterizedType) m_relationType).getActualTypeArguments()[0];
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
    public Class getRawClass() {
        if (m_rawClass == null) {
            if (isGenericCollectionType()) {
                // By default, the raw class is equal to the relation
                // class. But if the relation class is a generic we need to 
                // extract and set the actual raw class from the generic. 
                return (Class)(((ParameterizedType) m_relationType).getRawType());
            } else {
                m_rawClass = (Class) m_relationType;
            }
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
     */
    public Class getReferenceClassFromGeneric() {
        if (isGenericCollectionType()) {
            ParameterizedType pType = (ParameterizedType) m_relationType;
            
            if (java.util.Map.class.isAssignableFrom((Class) pType.getRawType())) {
                return (Class) pType.getActualTypeArguments()[1];
            }
            
            return (Class) pType.getActualTypeArguments()[0];
        } else {
            return null;
        }
    }
    
    /**
     * INTERNAL:
     * Return the relation type of this accessible object.
     */
    public Type getRelationType() {
        return m_relationType;
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
     * methods that take a descriptor. 
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
        Annotation annotation = getAnnotation(annotationClass);
        
        if (annotation != null && descriptor.ignoreAnnotations()) {
            getLogger().logWarningMessage(MetadataLogger.IGNORE_ANNOTATION, annotation, m_annotatedElement);
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
     * Return true if this accessor represents an aggregate mapping. True is
     * returned if an Embedded annotation is found or if an Embeddable 
     * annotation is found on the raw/reference class.
     */
    public boolean isEmbedded(MetadataDescriptor descriptor) {
        if (isAnnotationNotPresent(Embedded.class) && isAnnotationNotPresent(EmbeddedId.class)) {
            Class rawClass = getRawClass();
            MetadataClass metadataClass = new MetadataClass(rawClass);
            return (metadataClass.isAnnotationPresent(Embeddable.class) || descriptor.getProject().hasEmbeddable(rawClass));
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
        return m_relationType instanceof ParameterizedType;
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
        if (isAnnotationNotPresent(OneToMany.class)) {
            if (isGenericCollectionType() && isSupportedCollectionClass() && descriptor.getProject().hasEntity(getReferenceClassFromGeneric())) {
                getLogger().logConfigMessage(MetadataLogger.ONE_TO_MANY_MAPPING, m_annotatedElement);
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
        if (isAnnotationNotPresent(OneToOne.class)) {    
            if (descriptor.getProject().hasEntity(getRawClass()) && ! isEmbedded(descriptor)) {
                getLogger().logConfigMessage(MetadataLogger.ONE_TO_ONE_MAPPING, m_annotatedElement);
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
     * Method to return whether a class is a supported Collection. EJB 3.0 spec 
     * currently only supports Collection, Set, List and Map.
     */
    public boolean isSupportedCollectionClass() {
        Class rawClass = getRawClass();
        
        return rawClass.equals(Collection.class) || 
               rawClass.equals(Set.class) || 
               rawClass.equals(List.class) || 
               rawClass.equals(Map.class);
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
     *    ValueHolderInterface.
     */
    public boolean isVariableOneToOne(MetadataDescriptor descriptor) {
        if (isAnnotationNotPresent(VariableOneToOne.class)) {
            if (getRawClass().isInterface() && 
                    ! Map.class.isAssignableFrom(getRawClass()) && 
                    ! Collection.class.isAssignableFrom(getRawClass()) &&
                    ! getRawClass().equals(ValueHolderInterface.class)) {
                getLogger().logConfigMessage(MetadataLogger.VARIABLE_ONE_TO_ONE_MAPPING, m_annotatedElement);
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
     * Set the annotated element for this accessible object.
     * Once the class loader changes, we need to be able to update our
     * classes.
     */
    public void setAnnotatedElement(AnnotatedElement annotatedElement) {
        m_annotatedElement = annotatedElement;

        // For bug210258, the getAnnotation and isAnnotationPresent method will 
        // use the hashmap to determine declared annotation.
        m_annotations = new HashMap<String, Annotation>();
        
        for (Annotation annotation : annotatedElement.getDeclaredAnnotations()) {
            String annotationName = annotation.annotationType().getName();
            if (annotationName.startsWith(JPA_PERSISTENCE_PACKAGE_PREFIX) || annotationName.startsWith(ECLIPSELINK_PERSISTENCE_PACKAGE_PREFIX)) {
                String annotationShortName = annotation.toString().substring(1, annotation.toString().indexOf("("));
                m_annotations.put(annotationShortName, annotation);
            }
        }
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
    
    /**
     * INTERNAL:
     * Set the relation type of this accessible object.
     */
    protected void setRelationType(Type relationType) {
        m_relationType = relationType;
    }
}
