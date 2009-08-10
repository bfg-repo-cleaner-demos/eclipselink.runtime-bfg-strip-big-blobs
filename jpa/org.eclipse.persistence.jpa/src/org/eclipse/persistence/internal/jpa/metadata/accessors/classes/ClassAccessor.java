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
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 *     05/23/2008-1.0M8 Guy Pelletier 
 *       - 211330: Add attributes-complete support to the EclipseLink-ORM.XML Schema
 *     05/30/2008-1.0M8 Guy Pelletier 
 *       - 230213: ValidationException when mapping to attribute in MappedSuperClass
 *     07/15/2008-1.0.1 Guy Pelletier 
 *       - 240679: MappedSuperclass Id not picked when on get() method accessor
 *     09/23/2008-1.1 Guy Pelletier 
 *       - 241651: JPA 2.0 Access Type support
 *     10/01/2008-1.1 Guy Pelletier 
 *       - 249329: To remain JPA 1.0 compliant, any new JPA 2.0 annotations should be referenced by name
 *     12/12/2008-1.1 Guy Pelletier 
 *       - 249860: Implement table per class inheritance support.
 *     01/28/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 1)
 *     02/06/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 2)
 *     02/26/2009-2.0 Guy Pelletier 
 *       - 264001: dot notation for mapped-by and order-by
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     04/03/2009-2.0 Guy Pelletier
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     04/24/2009-2.0 Guy Pelletier 
 *       - 270011: JPA 2.0 MappedById support
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.classes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.eclipse.persistence.annotations.BasicCollection;
import org.eclipse.persistence.annotations.BasicMap;
import org.eclipse.persistence.annotations.ChangeTracking;
import org.eclipse.persistence.annotations.Customizer;
import org.eclipse.persistence.annotations.CopyPolicy;
import org.eclipse.persistence.annotations.InstantiationCopyPolicy;
import org.eclipse.persistence.annotations.CloneCopyPolicy;
import org.eclipse.persistence.annotations.Properties;
import org.eclipse.persistence.annotations.Property;
import org.eclipse.persistence.annotations.Transformation;
import org.eclipse.persistence.annotations.VariableOneToOne;

import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.jpa.metadata.accessors.PropertyMetadata;

import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.ElementCollectionAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.EmbeddedIdAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.DerivedIdClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.ManyToManyAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.ManyToOneAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.BasicAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.BasicCollectionAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.BasicMapAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.EmbeddedAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.IdAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.MappedKeyMapAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.MappingAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.OneToManyAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.OneToOneAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.TransformationAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.VariableOneToOneAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.VersionAccessor;

import org.eclipse.persistence.internal.jpa.metadata.accessors.MetadataAccessor;

import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotatedElement;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataClass;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataField;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataMethod;

import org.eclipse.persistence.internal.jpa.metadata.changetracking.ChangeTrackingMetadata;

import org.eclipse.persistence.internal.jpa.metadata.copypolicy.CopyPolicyMetadata;
import org.eclipse.persistence.internal.jpa.metadata.copypolicy.CustomCopyPolicyMetadata;
import org.eclipse.persistence.internal.jpa.metadata.copypolicy.InstantiationCopyPolicyMetadata;
import org.eclipse.persistence.internal.jpa.metadata.copypolicy.CloneCopyPolicyMetadata;

import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;

import org.eclipse.persistence.internal.jpa.metadata.MetadataConstants;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.MetadataProject;
import org.eclipse.persistence.internal.jpa.metadata.ORMetadata;

import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.ObjectAccessor;

/**
 * INTERNAL:
 * A abstract class accessor. Holds common metadata for entities, embeddables
 * and mapped superclasses.
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public abstract class ClassAccessor extends MetadataAccessor {
    private boolean m_isPreProcessed = false;
    private boolean m_isProcessed = false;
    
    private Boolean m_excludeDefaultMappings;
    private Boolean m_metadataComplete;
    
    private ChangeTrackingMetadata m_changeTracking;
    private MetadataClass m_customizerClass;
    
    // Various copy policies. Represented individually to facilitate XML writing.
    private CloneCopyPolicyMetadata m_cloneCopyPolicy;
    private CustomCopyPolicyMetadata m_customCopyPolicy;
    private InstantiationCopyPolicyMetadata m_instantiationCopyPolicy;
    
    private String m_className;
    private String m_customizerClassName;
    private String m_description;
    
    private XMLAttributes m_attributes;
    
    /**
     * INTERNAL:
     */
    protected ClassAccessor(String xmlElement) {
        super(xmlElement);
    }
    
    /**
     * INTERNAL:
     */
    public ClassAccessor(MetadataAnnotation annotation, MetadataClass cls, MetadataProject project) {
        super(annotation, cls, new MetadataDescriptor(cls), project);
        
        // Set the class accessor reference on the descriptor.
        getDescriptor().setClassAccessor(this);
    }
    
    /**
     * INTERNAL:
     * Called from MappedSuperclassAccessor. We want to avoid setting the
     * class accessor on the descriptor to be the MappedSuperclassAccessor.
     */
    protected ClassAccessor(MetadataAnnotation annotation, MetadataClass cls, MetadataDescriptor descriptor) {    
        super(annotation, cls, descriptor, descriptor.getProject());
    }
    
    /**
     * INTERNAL:
     * Add the accessor to the descriptor
     */
    protected void addAccessor(MappingAccessor accessor) {
        if (accessor != null) {
            // The actual owning descriptor for this class accessor. In most
            // cases this is the same as our descriptor. However in an
            // embeddable class accessor, it will be the owning entities
            // descriptor. This was introduced to support nesting 
            // embeddables to the nth level.
            accessor.setOwningDescriptor(getOwningDescriptor());
            
            // Add any converters on this mapping accessor.
            accessor.addConverters();
            
            // Add any embeddedid references to the list of id 'used' classes.
            // That is @IdClass and @EmbeddedId reference classes.
            if (accessor.isEmbeddedId()) {
                getProject().addIdClass(accessor.getReferenceClassName());
            }

            // Add the embeddable accessor to the project. In the case of
            // pre-processing, if we are an embeddable accessor the nested 
            // embeddable will be pre-processed now.
            addPotentialEmbeddableAccessor(accessor.getReferenceClass());
            
            // Tell an embeddable accessor that is a map key to a collection
            // to pre-process itself.
            if (accessor.isMappedKeyMapAccessor()) {
                MappedKeyMapAccessor mapAccessor = (MappedKeyMapAccessor) accessor;
                MetadataClass mapKeyClass = mapAccessor.getMapKeyClass();
                
                // If the map key class is not specified, we need to look it 
                // up from the accessor type.
                if (mapKeyClass == null || mapKeyClass.equals(void.class)) {
                    mapKeyClass = accessor.getAccessibleObject().getMapKeyClass(getDescriptor());
                    
                    if (mapKeyClass == null && mapAccessor.getMapKey() == null) {
                        // We don't have a map key class or map key, throw an exception.
                        throw ValidationException.unableToDetermineMapKeyClass(accessor.getAttributeName(), accessor.getJavaClass());
                    } else {
                        // Set the map key class (note, may still be null)
                        mapAccessor.setMapKeyClass(mapKeyClass);
                    }
                }
                
                // Add the embeddable accessor to the project. In the case of
                // pre-processing, if we are an embeddable accessor the nested 
                // embeddable will be pre-processed now.
                addPotentialEmbeddableAccessor(mapKeyClass);
            }
         
            // Add the accessor to the descriptor.
            getDescriptor().addAccessor(accessor);
        }
    }
    
    /**
     * INTERNAL:
     * Add the accessors from this class accessors java class to the descriptor
     * tied to this class accessor. This method is called for every class
     * accessor and is also called from parent class accessors to each of its
     * subclasses of a TABLE_PER_CLASS inhertiance strategy.
     */
    public void addAccessors() {      
        if (m_attributes != null) {
            for (MappingAccessor accessor : m_attributes.getAccessors()) {
                // Load the accessible object from the class.
                MetadataAccessibleObject accessibleObject = null;
                
                // We must init all xml mapping accessors with a reference
                // of their owning class accessor. The mapping accessors 
                // require metatata information from them to ensure they 
                // process themselves correctly.
                accessor.initXMLMappingAccessor(this);
                
                if (accessor.usesPropertyAccess(getDescriptor())) {
                    if (accessor.getAccessMethods() != null) {
                        // Can't rely on MappingAccessor's getGetMethodName 
                        // methods as they could result in NPE if 
                        // accessibleObject isn't set first
                        String getMethodName = accessor.getAccessMethods().getGetMethodName();
                        MetadataMethod getMethod = getJavaClass().getMethod(getMethodName, new String[]{});
                        String setMethodName = accessor.getAccessMethods().getSetMethodName();
                        MetadataMethod setMethod = getJavaClass().getMethod(setMethodName, Arrays.asList(new String[]{getMethod.getReturnType()}));
                        getMethod.setSetMethod(setMethod);
                        accessibleObject = getMethod;
                    } else {
                        MetadataMethod method = getJavaClass().getMethodForPropertyName(accessor.getName());

                        if (method == null) {
                            throw ValidationException.invalidPropertyForClass(accessor.getName(), getJavaClass());
                        } else {
                            // True will force an exception to be thrown if it 
                            // is not a valid method. However, if it is a
                            // transient accessor, don't validate it and just 
                            // let it through.
                            if (accessor.isTransient() || method.isValidPersistenceMethod(getDescriptor(), true)) {    
                                accessibleObject = method;
                            }
                        }  
                    }
                } else {
                    MetadataField field = getJavaClass().getField(accessor.getName());
                
                    if (field == null) {
                        throw ValidationException.invalidFieldForClass(accessor.getName(), getJavaClass());
                    } else {
                        // True will force an exception to be thrown if it is 
                        // not a valid field. However, if it is a transient 
                        // accessor, don't validate it and just let it through.
                        if (accessor.isTransient() || field.isValidPersistenceField(getDescriptor(), true)) {
                            accessibleObject = field;
                        }
                    }
                }
                
                // Initialize the accessor with its real accessible object now,
                // that is a field or method since it will currently hold a 
                // reference to its owning class' accessible object.
                accessor.initXMLObject(accessibleObject, getEntityMappings());
                
                // It's now safe to init the correct access type for this
                // mapping accessor since we now have set the actual accessible
                // object for this mapping accessor. Note: the initAccess call
                // was originally in initXMLObject, but with the current
                // processing setup that isn't valid since mapping accessors
                // have their accessible object 'faked' out for xml merging
                // purposes during XMLAttributes initXMLObject call. Doing the
                // access initialization there could cause one of two problems: 
                // Firstly, an incorrect access type setting and secondly and
                // more importantly, a null pointer exception (bug 264596) since
                // our descriptor hasn't been set.
                accessor.initAccess();
                
                // Add the accessor to the descriptor's list
                addAccessor(accessor);
            }
        }
        
        // Process the fields or methods on the class for annotations.
        if (usesPropertyAccess()) {
            addAccessorMethods(false);
        } else {
            addAccessorFields(false);
        }
    }
    
    /**
     * INTERNAL:
     * Create mappings from the fields directly. If the mustBeExplicit flag
     * is true, then we are processing the inverse of an explicit access
     * setting and for a field to be processed it must have a Access(FIELD) 
     * setting.
     */
    protected void addAccessorFields(boolean processingInverse) {
        for (MetadataField metadataField : getJavaClass().getFields().values()) {
            if (metadataField.isAnnotationPresent(Transient.class, getDescriptor())) {
                if (metadataField.hasMoreThanOneDeclaredAnnotation(getDescriptor())) {
                    throw ValidationException.mappingAnnotationsAppliedToTransientAttribute(metadataField);
                }
            } else {
                // The is valid check will throw an exception if needed.
                if (metadataField.isValidPersistenceField(processingInverse, getDescriptor())) {
                    // If the accessor already exists, it may have come from XML 
                    // or because of an explicit access type setting. E.G. 
                    // Access type is property and we processed the access 
                    // methods for this field, however the field has been tagged 
                    // as access field. We must therefore overwrite the previous 
                    // accessor with this explicit one.
                    if (! getDescriptor().hasAccessorFor(metadataField.getAttributeName()) || (getDescriptor().hasAccessorFor(metadataField.getAttributeName()) && processingInverse)) {
                        addAccessor(buildAccessor(metadataField));
                    }
                }
            }
        }
        
        // If we have an explicit access setting we must process the inverse
        // for those accessors that have an Access(PROPERTY) setting.
        if (hasAccess() && ! processingInverse) {
            addAccessorMethods(true);
        }  
    }
    
    /**
     * INTERNAL:
     * Create mappings via the class properties. If the mustBeExplicit flag
     * is true, then we are processing the inverse of an explicit access
     * setting and for a field to be processed it must have a Access(PROPERTY) 
     * setting.
     */
    protected void addAccessorMethods(boolean processingInverse) {
        for (MetadataMethod metadataMethod : getJavaClass().getMethods().values()) {
            if ( metadataMethod.isAnnotationPresent(Transient.class, getDescriptor())) {    
                if (metadataMethod.hasMoreThanOneDeclaredAnnotation(getDescriptor())) {
                    throw ValidationException.mappingAnnotationsAppliedToTransientAttribute(metadataMethod);
                }
            } else {
                // The is valid check will throw an exception if needed.
                if (metadataMethod.isValidPersistenceMethod(processingInverse, getDescriptor())) {
                    // If the accessor already exists, it may have come from XML 
                    // or because of an explicit access type setting. E.G. 
                    // Access type is field however the user indicated the we 
                    // should use its access methods. We must therefore 
                    // overwrite the previous accessor with this explicit one.
                    if (! getDescriptor().hasAccessorFor(metadataMethod.getAttributeName()) || (getDescriptor().hasAccessorFor(metadataMethod.getAttributeName()) && processingInverse)) {
                        addAccessor(buildAccessor(metadataMethod));
                    }
                }
            }
        }
        
        // If we have an explicit access setting we must process the inverse
        // for those accessors that have an Access(FIELD)setting. 
        if (hasAccess() && ! processingInverse) {
            addAccessorFields(true);
        }  
    }
    
    /**
     * INTERNAL
     * Add an embeddable class to the embeddable accessor list if it is
     * indeed an embeddable. This method is overridden in EmbeddableAccessor
     * and is called during pre-process. At the entity level all we want to do
     * is set the owning descriptor. Any nested embeddables will be discovered
     * and pre-processed when pre-processing the known list of embeddables.
     * @see MetadataProject processStage1()
     */
    protected void addPotentialEmbeddableAccessor(MetadataClass potentialEmbeddableClass) {
        if (potentialEmbeddableClass != null) {
            EmbeddableAccessor embeddableAccessor = getProject().getEmbeddableAccessor(potentialEmbeddableClass);
        
            if (embeddableAccessor != null) {
                embeddableAccessor.setOwningDescriptor(getOwningDescriptor());
                getProject().addRootEmbeddableAccessor(embeddableAccessor);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Create and return the appropriate accessor based on the accessible 
     * object given. Order of checking is important, careful when modifying
     * or adding, check what the isXyz call does to determine if the accessor
     * is of type xyz.
     */
    protected MappingAccessor buildAccessor(MetadataAnnotatedElement accessibleObject) {
        if (accessibleObject.isBasicCollection(getDescriptor())) {
            return new BasicCollectionAccessor(accessibleObject.getAnnotation(BasicCollection.class), accessibleObject, this);
        } else if (accessibleObject.isBasicMap(getDescriptor())) {
            return new BasicMapAccessor(accessibleObject.getAnnotation(BasicMap.class), accessibleObject, this);
        } else if (accessibleObject.isElementCollection(getDescriptor())) {
            return new ElementCollectionAccessor(accessibleObject.getAnnotation(ElementCollection.class), accessibleObject, this);
        } else if (accessibleObject.isVersion(getDescriptor())) {
            return new VersionAccessor(accessibleObject.getAnnotation(Version.class), accessibleObject, this);
        } else if (accessibleObject.isId(getDescriptor()) && ! accessibleObject.isDerivedId(getDescriptor())) {
            return new IdAccessor(accessibleObject.getAnnotation(Id.class), accessibleObject, this);
        } else if (accessibleObject.isDerivedIdClass(getDescriptor())) {
            return new DerivedIdClassAccessor(accessibleObject, this);
        } else if (accessibleObject.isBasic(getDescriptor())) {
            return new BasicAccessor(accessibleObject.getAnnotation(Basic.class), accessibleObject, this);
        } else if (accessibleObject.isEmbedded(getDescriptor())) {
            return new EmbeddedAccessor(accessibleObject.getAnnotation(Embedded.class), accessibleObject, this);
        } else if (accessibleObject.isEmbeddedId(getDescriptor())) {
            return new EmbeddedIdAccessor(accessibleObject.getAnnotation(EmbeddedId.class), accessibleObject, this);
        } else if (accessibleObject.isTransformation(getDescriptor())) { 
            return new TransformationAccessor(accessibleObject.getAnnotation(Transformation.class), accessibleObject, this);
        } else if (accessibleObject.isManyToMany(getDescriptor())) {
            return new ManyToManyAccessor(accessibleObject.getAnnotation(ManyToMany.class), accessibleObject, this);
        } else if (accessibleObject.isManyToOne(getDescriptor())) {
            return new ManyToOneAccessor(accessibleObject.getAnnotation(ManyToOne.class), accessibleObject, this);
        } else if (accessibleObject.isOneToMany(getDescriptor())) {
            // A OneToMany can default and doesn't require an annotation to be present.
            return new OneToManyAccessor(accessibleObject.getAnnotation(OneToMany.class), accessibleObject, this);
        } else if (accessibleObject.isOneToOne(getDescriptor())) {
            // A OneToOne can default and doesn't require an annotation to be present.
            return new OneToOneAccessor(accessibleObject.getAnnotation(OneToOne.class), accessibleObject, this);
        } else if (accessibleObject.isVariableOneToOne(getDescriptor())) {
            // A VariableOneToOne can default and doesn't require an annotation to be present.
            return new VariableOneToOneAccessor(accessibleObject.getAnnotation(VariableOneToOne.class), accessibleObject, this);
        } else if (getDescriptor().ignoreDefaultMappings()) {
            return null;
        } else {
            // Default case (everything else falls into a Basic)
            return new BasicAccessor(accessibleObject.getAnnotation(Basic.class), accessibleObject, this);
        }
    }
    
    /**
     * INTERNAL:
     */
    public boolean excludeDefaultMappings() {
        return m_excludeDefaultMappings != null && m_excludeDefaultMappings;
    }
    
    /**
     * INTERNAL:
     * Return the access type of this accessor. Assumes all access processing
     * has been performed before calling this method.
     */
    public String getAccessType() {
        if (hasAccess()) {    
            return super.getAccess();
        } else {
            return getDescriptor().getDefaultAccess();
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public XMLAttributes getAttributes() {
        return m_attributes;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public ChangeTrackingMetadata getChangeTracking() {
        return m_changeTracking;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getClassName() {
        return m_className;
    }
    
    /**
     * INTERNAL:
     */
    public CopyPolicyMetadata getCopyPolicy(){
        if (m_cloneCopyPolicy != null){
            return m_cloneCopyPolicy;
        } else if (m_instantiationCopyPolicy != null){
            return m_instantiationCopyPolicy;
        } else {
            return m_customCopyPolicy;
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping
     */
    public CloneCopyPolicyMetadata getCloneCopyPolicy(){
        return m_cloneCopyPolicy;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping
     */
    public CustomCopyPolicyMetadata getCustomCopyPolicy(){
        return m_customCopyPolicy;
    }
    
    /**
     * INTERNAL:
     */
    public MetadataClass getCustomizerClass() {
        return m_customizerClass;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getCustomizerClassName() {
        return m_customizerClassName;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getDescription() {
        return m_description;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Boolean getExcludeDefaultMappings() {
        return m_excludeDefaultMappings;
    }
    
    /**
     * INTERNAL:
     * To satisfy the abstract getIdentifier() method from ORMetadata.
     */
    @Override
    public String getIdentifier() {
        return getJavaClassName();
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping
     */
    public InstantiationCopyPolicyMetadata getInstantiationCopyPolicy(){
        return m_instantiationCopyPolicy;
    }
    
    /**
     * INTERNAL:
     * Return the java class that defines this accessor. It may be an
     * entity, embeddable or mapped superclass.
     */
    @Override
    public MetadataClass getJavaClass() {
        return (MetadataClass) getAnnotatedElement();
    }
    
    /**
     * INTERNAL:
     * Return the java class name that defines this accessor. It may be an
     * entity, embeddable or mapped superclass.
     */
    @Override
    public String getJavaClassName() {
        return getJavaClass().getName();
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Boolean getMetadataComplete() {
        return m_metadataComplete;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasDerivedId(){
        return ! getDescriptor().getDerivedIDAccessors().isEmpty();
    }
    
    /**
     * INTERNAL:
     */
    protected boolean havePersistenceFieldAnnotationsDefined(Collection<MetadataField> fields) {
        for (MetadataField field : fields) {            
            if (field.hasDeclaredAnnotations(getDescriptor())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     */
    protected boolean havePersistenceMethodAnnotationsDefined(Collection<MetadataMethod> methods) {
        for (MetadataMethod method : methods) {            
            if (method.hasDeclaredAnnotations(getDescriptor())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     * Return whether this ClassAccessor is a MappedSuperclassAccessor
     */
    public boolean isMappedSuperclass() {
        return false;
    }
    
    /**
     * INTERNAL:
     */
    public boolean isMetadataComplete() {
        return m_metadataComplete != null && m_metadataComplete;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor has been pre-processed.
     */
    public boolean isPreProcessed() {
        return m_isPreProcessed;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor has been processed.
     */
    public boolean isProcessed() {
        return m_isProcessed;
    }
    
    /**
     * INTERNAL: 
     * This method should be subclassed in those methods that need to do 
     * extra initialization.
     */
    public void initXMLClassAccessor(MetadataAccessibleObject accessibleObject, MetadataDescriptor descriptor, MetadataProject project, XMLEntityMappings entityMappings) {
        initXMLAccessor(descriptor, project);
        initXMLObject(accessibleObject, entityMappings);
        
        // Since the the descriptor, project and accessible object are all 
        // available at this point, it is now safe to initialize our access
        // type.
        initAccess();
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject, XMLEntityMappings entityMappings) {
        super.initXMLObject(accessibleObject, entityMappings);
        
        // Initialize single objects.
        initXMLObject(m_changeTracking, accessibleObject);
        initXMLObject(m_cloneCopyPolicy, accessibleObject);
        initXMLObject(m_customCopyPolicy, accessibleObject);
        initXMLObject(m_instantiationCopyPolicy, accessibleObject);
        initXMLObject(m_attributes, accessibleObject);
        
        // Initialize simple class objects.
        m_customizerClass = initXMLClassName(m_customizerClassName);
    }
    
    /** 
     * INTERNAL:
     * Return true if this accessor represents a class.
     */
    public boolean isClassAccessor() {
        return true;
    }
    
    /** 
     * INTERNAL:
     * Return true if this accessor represents an embeddable class.
     */
    public boolean isEmbeddableAccessor() {
        return false;
    }
    
    /** 
     * INTERNAL:
     * Return true if this accessor represents an entity class.
     */
    public boolean isEntityAccessor() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Generic class level merging details for entities, mapped superclasses
     * and embeddables.
     */
    @Override
    public void merge(ORMetadata metadata) {
        super.merge(metadata);
        
        ClassAccessor accessor = (ClassAccessor) metadata;
        
        // Simple object merging.
        m_customizerClass = (MetadataClass) mergeSimpleObjects(m_customizerClass, accessor.getCustomizerClass(), accessor, "<customizer>");
        m_description = (String) mergeSimpleObjects(m_description, accessor.getDescription(), accessor, "<description>");
        m_metadataComplete = (Boolean) mergeSimpleObjects(m_metadataComplete, accessor.getMetadataComplete(), accessor, "@metadata-complete");
        m_excludeDefaultMappings = (Boolean) mergeSimpleObjects(m_excludeDefaultMappings, accessor.getExcludeDefaultMappings(), accessor, "@exclude-default-mappings");
        
        // ORMetadata object merging.        
        m_cloneCopyPolicy = (CloneCopyPolicyMetadata) mergeORObjects(m_cloneCopyPolicy, accessor.getCloneCopyPolicy());
        m_customCopyPolicy = (CustomCopyPolicyMetadata) mergeORObjects(m_customCopyPolicy, accessor.getCustomCopyPolicy());
        m_instantiationCopyPolicy = (InstantiationCopyPolicyMetadata) mergeORObjects(m_instantiationCopyPolicy, accessor.getInstantiationCopyPolicy());
        m_changeTracking = (ChangeTrackingMetadata) mergeORObjects(m_changeTracking, accessor.getChangeTracking());
        
        // ORObjects that merge further ...
        if (m_attributes == null) {
            m_attributes = accessor.getAttributes();
        } else {
            m_attributes.merge(accessor.getAttributes());
        }
    }
    
    /**
     * INTERNAL: 
     */
    public abstract void preProcess();
    
    /**
     * INTERNAL:
     */
    public abstract void preProcessForCanonicalModel();
    
    /**
     * INTERNAL: 
     * Implemented by EntityAccessor, EmbeddableAccessor and 
     * MappedSuperclassAccessor. They must call this method to set the
     * isProcessed flag.
     */
    @Override
    public abstract void process();
    
    /**
     * INTERNAL:
     * Process the accessors for the given class.
     */
    public void processAccessors() {
        // Now tell the descriptor to process its accessors.
        getDescriptor().processAccessors(getOwningDescriptor());
    }
    
    /**
     * INTERNAL:
     * Process the change tracking setting for this accessor.
     */
    protected void processChangeTracking() {
        MetadataAnnotation changeTracking = getAnnotation(ChangeTracking.class);
        
        if (m_changeTracking != null || changeTracking != null) {
            if (getDescriptor().hasChangeTracking()) {    
                // We must be processing a mapped superclass setting for an
                // entity that has its own change tracking setting. Ignore it 
                // and log a warning.
                getLogger().logWarningMessage(MetadataLogger.IGNORE_MAPPED_SUPERCLASS_CHANGE_TRACKING, getDescriptor().getJavaClass(), getJavaClass());
            } else {
                if (m_changeTracking == null) {
                    new ChangeTrackingMetadata(changeTracking, getAccessibleObject()).process(getDescriptor());
                } else {
                    if (changeTracking != null) {
                        getLogger().logWarningMessage(MetadataLogger.OVERRIDE_ANNOTATION_WITH_XML, changeTracking, getJavaClassName(), getLocation());
                    }
                    
                    m_changeTracking.process(getDescriptor());
                }
            }
        }   
    }
    
    /**
     * INTERNAL:
     */
    protected void processCopyPolicy(){
        MetadataAnnotation copyPolicy = getAnnotation(CopyPolicy.class);
        MetadataAnnotation instantiationCopyPolicy = getAnnotation(InstantiationCopyPolicy.class);
        MetadataAnnotation cloneCopyPolicy = getAnnotation(CloneCopyPolicy.class);

        if (getCopyPolicy() != null || copyPolicy != null || instantiationCopyPolicy != null || cloneCopyPolicy != null) {
            if (getDescriptor().hasCopyPolicy()){
                // We must be processing a mapped superclass ...
                getLogger().logWarningMessage(MetadataLogger.IGNORE_MAPPED_SUPERCLASS_COPY_POLICY, getDescriptor().getJavaClass(), getJavaClass());
            }
            
            if (getCopyPolicy() == null) {
                // Look at the annotations.
                if (copyPolicy != null) {
                    if (instantiationCopyPolicy != null || cloneCopyPolicy != null) {
                        throw ValidationException.multipleCopyPolicyAnnotationsOnSameClass(getJavaClassName());
                    }

                    new CustomCopyPolicyMetadata(copyPolicy, getAccessibleObject()).process(getDescriptor());
                }
                
                if (instantiationCopyPolicy != null){
                    if (cloneCopyPolicy != null) {
                        throw ValidationException.multipleCopyPolicyAnnotationsOnSameClass(getJavaClassName());
                    }
                    
                    new InstantiationCopyPolicyMetadata(instantiationCopyPolicy, getAccessibleObject()).process(getDescriptor());
                }
                
                if (cloneCopyPolicy != null){
                    new CloneCopyPolicyMetadata(cloneCopyPolicy, getAccessibleObject()).process(getDescriptor());
                }
                
            } else {
                // We have a copy policy specified in XML.
                if (copyPolicy != null) {
                    getLogger().logWarningMessage(MetadataLogger.OVERRIDE_ANNOTATION_WITH_XML, copyPolicy, getJavaClassName(), getLocation());
                }
                
                if (instantiationCopyPolicy != null) {
                    getLogger().logWarningMessage(MetadataLogger.OVERRIDE_ANNOTATION_WITH_XML, instantiationCopyPolicy, getJavaClassName(), getLocation());
                }
                
                if (cloneCopyPolicy != null) {
                    getLogger().logWarningMessage(MetadataLogger.OVERRIDE_ANNOTATION_WITH_XML, cloneCopyPolicy, getJavaClassName(), getLocation());
                }
                
                getCopyPolicy().process(getDescriptor());
            }
        }
    }
    
    /**
     * INTERNAL:
     */
    protected void processCustomizer() {
        MetadataAnnotation customizer = getAnnotation(Customizer.class);
        
        if ((m_customizerClass != null && ! m_customizerClass.equals(void.class)) || customizer != null) {
            if (getDescriptor().hasCustomizer()) {
                // We must be processing a mapped superclass and its subclass
                // override the customizer class, that is, defined its own. Log 
                // a warning that we are ignoring the Customizer metadata on the 
                // mapped superclass for the descriptor's java class.
                getLogger().logWarningMessage(MetadataLogger.IGNORE_MAPPED_SUPERCLASS_CUSTOMIZER, getDescriptor().getJavaClass(), getJavaClass());
            } else {
                if (m_customizerClass == null || m_customizerClass.equals(void.class)) { 
                    // Use the annotation value.
                    m_customizerClass = getMetadataClass((String)customizer.getAttribute("value"));
                } else {
                    // Use the xml value and log a message if necessary.
                    if (customizer != null) {
                        getLogger().logWarningMessage(MetadataLogger.OVERRIDE_ANNOTATION_WITH_XML, customizer, getJavaClassName(), getLocation());
                    }
                }
                
                getProject().addAccessorWithCustomizer(this);
            }
        }
    }

    /**
     * INTERNAL:
     * Allows for processing DerivedIds. All referenced accessors are processed
     * first to ensure the necessary fields are set before this derivedId is 
     * processed 
     */
    public void processDerivedIDs(HashSet<ClassAccessor> processing, HashSet<ClassAccessor> processed) {
        if (hasDerivedId() && ! processed.contains(this)) {
            if (processing.contains(this)){
                //we have a circular pk reference problem
                throw ValidationException.idRelationshipCircularReference(processing);
            }
            
            processing.add(this);
            
            for (ObjectAccessor accessor : getDescriptor().getDerivedIDAccessors()) {
                accessor.processKey(processing, processed);
            }
            
            processing.remove(this);
            processed.add(this);
        }
    }

    /**
     * INTERNAL:
     * Adds properties to the descriptor.
     */
    protected void processProperties() {        
        // Add the XML properties first.
        for (PropertyMetadata property : getProperties()) {
            getDescriptor().addProperty(property);
        }

        // Now add the properties defined in annotations.
        MetadataAnnotation properties = getAnnotation(Properties.class);
        if (properties != null) {
            for (Object property : (Object[]) properties.getAttributeArray("value")) {
                getDescriptor().addProperty(new PropertyMetadata((MetadataAnnotation)property, getAccessibleObject()));
            }
        }
        
        MetadataAnnotation property = getAnnotation(Property.class);
        if (property != null) {
            getDescriptor().addProperty(new PropertyMetadata(property, getAccessibleObject()));
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setAttributes(XMLAttributes attributes) {
        m_attributes = attributes;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public void setChangeTracking(ChangeTrackingMetadata changeTracking) {
        m_changeTracking = changeTracking;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setClassName(String className) {
        m_className = className;
    }
    
    /**
     * INTERNAL:
     * set the copy policy metadata
     */
    public void setCloneCopyPolicy(CloneCopyPolicyMetadata copyPolicy){
        m_cloneCopyPolicy = copyPolicy;
    }
    
    /**
     * INTERNAL:
     * set the copy policy metadata
     */
    public void setCustomCopyPolicy(CustomCopyPolicyMetadata copyPolicy){
        m_customCopyPolicy = copyPolicy;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setCustomizerClassName(String customizerClassName) {
        m_customizerClassName = customizerClassName;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setDescription(String description) {
        m_description = description;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setExcludeDefaultMappings(Boolean excludeDefaultMappings) {
        m_excludeDefaultMappings = excludeDefaultMappings;
    }
    
    /**
     * INTERNAL:
     * set the copy policy metadata
     */
    public void setInstantiationCopyPolicy(InstantiationCopyPolicyMetadata copyPolicy){
        m_instantiationCopyPolicy = copyPolicy;
    }
    
    /**
     * INTERNAL:
     */
    protected void setIsPreProcessed() {
        m_isPreProcessed = true;    
    }
    
    /**
     * INTERNAL:
     */
    protected void setIsProcessed() {
        m_isProcessed = true;    
    }
    
    /**
     * INTERNAL:
     * Set the java class for this accessor. This is currently called after
     * the class loader has changed and we are adding entity listeners.
     */
    public void setJavaClass(MetadataClass cls) {
        setAccessibleObject(cls);
        getDescriptor().setJavaClass(cls);
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setMetadataComplete(Boolean metadataComplete) {
        m_metadataComplete = metadataComplete;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public String toString() {
        return getJavaClassName();
    }
    
    /**
     * INTERNAL:
     * Returns true if this class uses property access. It will first check for 
     * an explicit access type specification, otherwise will use the default 
     * access as specified on the descriptor for this accessor since we may be 
     * processing a mapped superclass.
     */
    public boolean usesPropertyAccess() {
        return getAccessType().equals(MetadataConstants.PROPERTY);
    }
}
