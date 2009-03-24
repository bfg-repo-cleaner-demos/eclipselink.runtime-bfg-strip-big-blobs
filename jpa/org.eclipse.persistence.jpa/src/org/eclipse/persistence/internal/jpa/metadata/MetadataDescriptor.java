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
 *     06/20/2008-1.0M9 Guy Pelletier 
 *       - 232975: Failure when attribute type is generic
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
 *     02/25/2009-2.0 Guy Pelletier 
 *       - 265359: JPA 2.0 Element Collections - Metadata processing portions
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.persistence.annotations.ExistenceType;
import org.eclipse.persistence.descriptors.CMPPolicy;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.descriptors.ReturningPolicy;

import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.descriptors.OptimisticLockingPolicy;

import org.eclipse.persistence.internal.jpa.CMP3Policy;

import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EmbeddableAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EntityAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.CollectionAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.ManyToManyAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.MappedKeyMapAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.MappingAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.ObjectAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.OneToOneAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.RelationshipAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataMethod;
import org.eclipse.persistence.internal.jpa.metadata.accessors.MetadataAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.PropertyMetadata;

import org.eclipse.persistence.internal.jpa.metadata.columns.AssociationOverrideMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.AttributeOverrideMetadata;

import org.eclipse.persistence.internal.jpa.metadata.listeners.EntityListener;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;

import org.eclipse.persistence.mappings.DatabaseMapping;

/**
 * INTERNAL:
 * Common metatata descriptor for the annotation and xml processors. This class
 * is a wrap on an actual EclipseLink descriptor.
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public class MetadataDescriptor {
    private Class m_javaClass;
    private ClassAccessor m_classAccessor;
    private ClassDescriptor m_descriptor;
    private DatabaseTable m_primaryTable;
    private Enum m_existenceChecking;
    
    // This is the root descriptor of the inheritance hierarchy. That is, for 
    // the entity that defines the inheritance strategy.
    private MetadataDescriptor m_inheritanceRootDescriptor;
    // This is our immediate parent's descriptor. Which may also be the root. 
    private MetadataDescriptor m_inheritanceParentDescriptor;
    
    private boolean m_isCascadePersist;
    private boolean m_ignoreAnnotations; // XML metadata complete
    private boolean m_ignoreDefaultMappings; // XML exclude default mappings
    private boolean m_hasCache;
    private boolean m_hasCacheInterceptor;
    private boolean m_hasDefaultRedirectors;
    private boolean m_hasChangeTracking;
    private boolean m_hasCustomizer;
    private boolean m_hasReadOnly;
    private boolean m_hasCopyPolicy;
    private Boolean m_usesCascadedOptimisticLocking;
    
    // This is the default access type for the class accessor of this 
    // descriptor. The default access type is needed for those embeddables and 
    // mapped superclasses that are 'owned' or rely on this value for their own 
    // processing. It does not reflect an explicit access type.
    private String m_defaultAccess; 
    private String m_defaultSchema;
    private String m_defaultCatalog;
    private String m_embeddedIdAttributeName;
    
    private List<String> m_idAttributeNames;
    private List<String> m_orderByAttributeNames;
    private List<String> m_idOrderByAttributeNames;
    private List<MetadataDescriptor> m_embeddableDescriptors;
    private List<ObjectAccessor> m_derivedIDAccessors;
    
    private Class m_pkClass;
    private Map<String, Type> m_pkClassIDs;
    private Map<String, Type> m_genericTypes;
    private Map<String, MappingAccessor> m_accessors;
    private Map<String, PropertyMetadata> m_properties;
    private Map<String, String> m_pkJoinColumnAssociations;
    private Map<String, AttributeOverrideMetadata> m_attributeOverrides;
    private Map<String, AssociationOverrideMetadata> m_associationOverrides;
    private Map<String, Map<String, MetadataAccessor>> m_biDirectionalManyToManyAccessors;

    /**
     * INTERNAL: 
     */
    public MetadataDescriptor(Class javaClass) {
        m_defaultAccess = null;
        m_defaultSchema = null;
        m_defaultCatalog = null;
        
        m_inheritanceRootDescriptor = null;
        m_inheritanceParentDescriptor = null;
        
        m_hasCache = false;
        m_hasCacheInterceptor = false;
        m_hasDefaultRedirectors = false;
        m_hasChangeTracking = false;
        m_hasCustomizer = false;
        m_hasReadOnly = false;
        m_hasCopyPolicy = false;
        m_isCascadePersist = false;
        m_ignoreAnnotations = false;
        m_ignoreDefaultMappings = false;
        
        m_idAttributeNames = new ArrayList<String>();
        m_orderByAttributeNames = new ArrayList<String>();
        m_idOrderByAttributeNames = new ArrayList<String>();
        m_embeddableDescriptors = new ArrayList<MetadataDescriptor>();
        
        m_pkClassIDs = new HashMap<String, Type>();
        m_genericTypes = new HashMap<String, Type>();
        m_accessors = new HashMap<String, MappingAccessor>();
        m_properties = new HashMap<String, PropertyMetadata>();
        m_pkJoinColumnAssociations = new HashMap<String, String>();
        m_attributeOverrides = new HashMap<String, AttributeOverrideMetadata>();
        m_associationOverrides = new HashMap<String, AssociationOverrideMetadata>();
        m_biDirectionalManyToManyAccessors = new HashMap<String, Map<String, MetadataAccessor>>();
        
        m_descriptor = new RelationalDescriptor();
        m_descriptor.setAlias("");
        
        // This is the default, set it in case no existence-checking is set.
        m_descriptor.getQueryManager().checkDatabaseForDoesExist();
        
        m_derivedIDAccessors = new ArrayList<ObjectAccessor>();
                
        setJavaClass(javaClass);
    }
    
    /**
     * INTERNAL: 
     */
    public MetadataDescriptor(Class javaClass, ClassAccessor classAccessor) {
        this(javaClass);
        setClassAccessor(classAccessor);
    }
    
    /**
     * INTERNAL:
     * We must check for null since buildAccessor from ClassAccessor may return
     * a null if ignore default mappings is set to true.
     */
    public void addAccessor(MappingAccessor accessor, MetadataDescriptor owningDescriptor) {
        if (accessor != null) {
            m_accessors.put(accessor.getAttributeName(), accessor);
            
            // The actual owning descriptor for this class accessor. In most
            // cases this is the same as our descriptor. However in an
            // embeddable class accessor, it will be the owning entities
            // descriptor. This was introduced to support nesting 
            // embeddables to the nth level.
            accessor.setOwningDescriptor(owningDescriptor);
            
            // Add any converters on this mapping accessor.
            accessor.addConverters();

            // Tell an embeddable accessor to pre-process if it hasn't already.
            preProcessEmbeddableAccessor(accessor.getReferenceClass(), owningDescriptor);
            
            // Tell an embeddable accessor that is a map key to a collection
            // to pre-process itself.
            if (accessor.isMappedKeyMapAccessor()) {
                MappedKeyMapAccessor mapAccessor = (MappedKeyMapAccessor) accessor;
                Class mapKeyClass = mapAccessor.getMapKeyClass();
                
                // If the map key class is not specified, we need to look it 
                // up from the accessor type.
                if (mapKeyClass == null || mapKeyClass.equals(void.class)) {
                    mapKeyClass = accessor.getAccessibleObject().getMapKeyClass(this);
                    
                    if (mapKeyClass == null && mapAccessor.getMapKey() == null) {
                        // We don't have a map key class or map key, throw an exception.
                        throw ValidationException.unableToDetermineMapKeyClass(accessor.getAttributeName(), accessor.getJavaClass());
                    } else {
                        // Set the map key class (note, may still be null)
                        mapAccessor.setMapKeyClass(mapKeyClass);
                    }
                }
                
                // Now pre-process our map key class if it is an embeddable.
                preProcessEmbeddableAccessor(mapKeyClass, owningDescriptor);
            }
        }
    }
    
    /**
     * INTERNAL:
     */
     public void addAssociationOverride(AssociationOverrideMetadata associationOverride) {
        m_associationOverrides.put(associationOverride.getName(), associationOverride);   
     }
    
    /**
     * INTERNAL:
     */
    public void addAttributeOverride(AttributeOverrideMetadata attributeOverride) {
        m_attributeOverrides.put(attributeOverride.getName(), attributeOverride);
    }
    
    /** 
     * INTERNAL:
     */
    public void addDefaultEventListener(EntityListener listener) {
        m_descriptor.getEventManager().addDefaultEventListener(listener);
    }

    /** 
     * INTERNAL:
     */
    public void addDerivedIDAccessor(ObjectAccessor accessor){
        m_derivedIDAccessors.add(accessor);
    }

    /**
     * INTERNAL:
     */
    public void addEmbeddableDescriptor(MetadataDescriptor embeddableDescriptor) {
        m_embeddableDescriptors.add(embeddableDescriptor);
    }

    /**
     * INTERNAL:
     */
    public void addEntityListenerEventListener(EntityListener listener) {
        m_descriptor.getEventManager().addEntityListenerEventListener(listener);
    }

    /**
     * INTERNAL:
     */
    public void addFieldForInsert(DatabaseField field) {
        getReturningPolicy().addFieldForInsert(field);
    }
    
    /**
     * INTERNAL:
     */
    public void addFieldForInsertReturnOnly(DatabaseField field) {
        getReturningPolicy().addFieldForInsertReturnOnly(field);
    }
    
    /**
     * INTERNAL:
     */
    public void addFieldForUpdate(DatabaseField field) {
        getReturningPolicy().addFieldForUpdate(field);
    }
    
    /**
     * INTERNAL:
     */
    public void addIdAttributeName(String idAttributeName) {
        m_idAttributeNames.add(idAttributeName);    
    }
    
    /**
     * INTERNAL:
     */
    public void addForeignKeyFieldForMultipleTable(DatabaseField fkField, DatabaseField pkField) {
        m_descriptor.addForeignKeyFieldForMultipleTable(fkField, pkField);
        m_pkJoinColumnAssociations.put(fkField.getName(), pkField.getName());
    }
    
    /**
     * INTERNAL:
     * Add a generic type for this descriptor.
     */
    public void addGenericType(String genericName, Type type) {
        m_genericTypes.put(genericName, type);
    }
    
    /**
     * INTERNAL:
     * We store these to validate the primary class when processing
     * the entity class.
     */
    public void addPKClassId(String attributeName, Type type) {
        m_pkClassIDs.put(attributeName, type);
    }
    
    /**
     * INTERNAL:
     * Add a property to the descriptor. Will check for an override/ignore case.
     */
    public void addProperty(PropertyMetadata property) {
        if (property.shouldOverride(m_properties.get(property.getName()))) {
            m_properties.put(property.getName(), property);
            m_descriptor.getProperties().put(property.getName(), property.getConvertedValue());
        }
    }
    
    /**
     * INTERNAL:
     */
    public void addPrimaryKeyField(DatabaseField field) {
        m_descriptor.addPrimaryKeyField(field);
    }
    
    /**
      * INTERNAL:
      * Store relationship accessors for later processing and quick look up.
      */
    public void addRelationshipAccessor(MappingAccessor accessor) {
        getProject().addRelationshipAccessor(accessor);
        
        // Store bidirectional ManyToMany relationships so that we may look at 
        // attribute names when defaulting join columns.
        if (accessor.isManyToMany()) {
            String mappedBy = ((ManyToManyAccessor) accessor).getMappedBy();
            
            if (mappedBy != null && ! mappedBy.equals("")) {
                String referenceClassName = ((ManyToManyAccessor) accessor).getReferenceClassName();
                
                // Initialize the map of bi-directional mappings for this class.
                if (! m_biDirectionalManyToManyAccessors.containsKey(referenceClassName)) {
                    m_biDirectionalManyToManyAccessors.put(referenceClassName, new HashMap<String, MetadataAccessor>());
                }
            
                m_biDirectionalManyToManyAccessors.get(referenceClassName).put(mappedBy, accessor);
            }
        }
    }
    
    /**
     * INTERNAL:
     */
    public void addTable(DatabaseTable table) {
        m_descriptor.addTable(table);
    }
    
    /**
     * INTERNAL:
     */
    public boolean excludeSuperclassListeners() {
        return m_descriptor.getEventManager().excludeSuperclassListeners();
    }
    
    /**
     * INTERNAL:
     * This method will first check for an accessor with name equal to 
     * fieldOrPropertyName (that is, assumes it is a field name). If no accessor
     * is found than it assumes fieldOrPropertyName is a property name and 
     * converts it to its corresponding field name and looks for the accessor
     * again. If still no accessor is found and this descriptor represents an
     * inheritance subclass, then traverse up the chain to look for that
     * accessor. Null is returned otherwise.
     */
    public MappingAccessor getAccessorFor(String fieldOrPropertyName) {
        MappingAccessor accessor = m_accessors.get(fieldOrPropertyName);
        
        if (accessor == null) {
            // Perhaps we have a property name ...
            accessor = m_accessors.get(MetadataMethod.getAttributeNameFromMethodName(fieldOrPropertyName));
           
            // If still no accessor and we are an inheritance subclass, check 
            // our parent descriptor. Unless we are within a table per class 
            // strategy in which case, if the mapping doesn't exist within our
            // accessor list, we don't want to deal with it.
            if (accessor == null && isInheritanceSubclass() && ! usesTablePerClassInheritanceStrategy()) {
                accessor = getInheritanceParentDescriptor().getAccessorFor(fieldOrPropertyName);
            }
        }
        
        if (accessor == null) {
            // We didn't find an accessor on our descriptor (or a parent descriptor), 
            // check our aggregate descriptors now.
            for (MetadataDescriptor embeddableDescriptor : m_embeddableDescriptors) {
                // If the attribute name employs the dot notation, rip off the first 
                // bit (up to the first dot and keep burying down the embeddables)
                String subAttributeName = new String(fieldOrPropertyName);
                if (subAttributeName.contains(".")) {
                    subAttributeName = subAttributeName.substring(fieldOrPropertyName.indexOf(".") + 1);
                }
            
                accessor = embeddableDescriptor.getAccessorFor(subAttributeName);
            
                if (accessor != null) {
                    // Found one, stop looking ...
                    return accessor;
                }
            }
        }
        
        return accessor;
    }
    
    /**
     * INTERNAL:
     * Return the collection of mapping accessors for this descriptor.
     */
    public Collection<MappingAccessor> getAccessors() {
        return m_accessors.values();
    }
    
    /**
     * INTERNAL:
     */
    public String getAlias() {
        return m_descriptor.getAlias();
    }
    
    /**
     * INTERNAL:
     */
    public AssociationOverrideMetadata getAssociationOverrideFor(String attributeName) {
        return m_associationOverrides.get(attributeName);
    }
    
    /**
     * INTERNAL:
     */
    public Collection<AssociationOverrideMetadata> getAssociationOverrides() {
        return m_associationOverrides.values();
    }
    
    /**
     * INTERNAL:
     */
    public AttributeOverrideMetadata getAttributeOverrideFor(String attributeName) {
        return m_attributeOverrides.get(attributeName);
    }
    
    /**
     * INTERNAL:
     */
    public Collection<AttributeOverrideMetadata> getAttributeOverrides() {
        return m_attributeOverrides.values();
    }
    
    /**
     * INTERNAL:
     * The default table name is the descriptor alias, unless this descriptor 
     * metadata is an inheritance subclass with a SINGLE_TABLE strategy. Then 
     * it is the table name of the root descriptor metadata.
     */
    public String getDefaultTableName() {
        String defaultTableName = getAlias().toUpperCase();
        
        if (isInheritanceSubclass()) {    
            if (getInheritanceRootDescriptor().usesSingleTableInheritanceStrategy()) {
                defaultTableName = getInheritanceRootDescriptor().getPrimaryTableName();
            }
        }
        
        return defaultTableName;
    }
    
    /**
     * INTERNAL:
     */
    public ClassAccessor getClassAccessor() {
        return m_classAccessor;
    }
    
    /**
     * INTERNAL:
     */
    public ClassDescriptor getClassDescriptor() {
        return m_descriptor;
    }
    
    /**
     * INTERNAL:
     */
    public CMPPolicy getCMPPolicy() {
        return m_descriptor.getCMPPolicy();
    }
    
    /**
     * INTERNAL:
     */
    public String getDefaultAccess() {
        return m_defaultAccess;
    }
    
    /**
     * INTERNAL:
     */
    public String getDefaultCatalog() {
        return m_defaultCatalog;
    }
    
    /**
     * INTERNAL:
     */
    public String getDefaultSchema() {
        return m_defaultSchema;
    }
    
    /**
     * INTERNAL:
     */
    public List<ObjectAccessor> getDerivedIDAccessors(){
        return m_derivedIDAccessors;
    }
    
    /**
     * INTERNAL:
     */
    public String getEmbeddedIdAttributeName() {
        return m_embeddedIdAttributeName;
    }
    
    /**
     * INTERNAL:
     * This method assumes that by calling this method you are certain that
     * the related class accessor to this descriptor is an EntityAccessor.
     * You should not call this method otherwise, @see getClassAccessor()
     */
    public EntityAccessor getEntityAccessor() {
        return (EntityAccessor) m_classAccessor;
    }
    
    /**
     * INTERNAL:
     * Return the type from the generic name.
     */
    public Type getGenericType(String genericName) {
       return m_genericTypes.get(genericName); 
    }
    
    /**
     * INTERNAL:
     * Return the primary key attribute name for this entity.
     */
    public String getIdAttributeName() {
        if (getIdAttributeNames().isEmpty()) {
            if (isInheritanceSubclass()) {
                return getInheritanceRootDescriptor().getIdAttributeName();
            } else {
                return "";
            }
        } else {
            return getIdAttributeNames().get(0);
        }
    }
    
    /**
     * INTERNAL:
     * Return the id attribute names declared on this descriptor metadata.
     */
    public List<String> getIdAttributeNames() {
        return m_idAttributeNames;
    }
    
    /**
     * INTERNAL:
     * Return the primary key attribute names for this entity. If there are no
     * id attribute names set then we are either:
     * 1) an inheritance subclass, get the id attribute names from the root
     *    of the inheritance structure.
     * 2) we have an embedded id. Get the id attribute names from the embedded
     *    descriptor metadata, which is equal the attribute names of all the
     *    direct to field mappings on that descriptor metadata. Currently does
     *    not traverse nested embeddables.
     */
    public List<String> getIdOrderByAttributeNames() {
        if (m_idOrderByAttributeNames.isEmpty()) {
            if (m_idAttributeNames.isEmpty()) {
                if (isInheritanceSubclass()) {  
                    // Get the id attribute names from our root parent.
                    m_idOrderByAttributeNames = getInheritanceRootDescriptor().getIdAttributeNames();
                } else {
                    // We must have a composite primary key as a result of an embedded id.
                    m_idOrderByAttributeNames = getAccessorFor(getEmbeddedIdAttributeName()).getReferenceDescriptor().getOrderByAttributeNames();
                } 
            } else {
                m_idOrderByAttributeNames = m_idAttributeNames;
            }
        }
            
        return m_idOrderByAttributeNames;
    }
    
    
    /**
     * INTERNAL:
     * Assumes hasBidirectionalManyToManyAccessorFor has been called before
     * hand. 
     */
     public MetadataAccessor getBiDirectionalManyToManyAccessor(String className, String attributeName) {
        return m_biDirectionalManyToManyAccessors.get(className).get(attributeName);
    }
    
    /**
     * INTERNAL:
     * This will return the attribute names for all the direct to field mappings 
     * on this descriptor metadata. This method will typically be called when an 
     * embedded or embedded id attribute has been specified as an order by 
     * field
     */
    public List<String> getOrderByAttributeNames() {
        if (m_orderByAttributeNames.isEmpty()) {
            for (DatabaseMapping mapping : getMappings()) {
                if (mapping.isDirectToFieldMapping()) {
                    m_orderByAttributeNames.add(mapping.getAttributeName());
                }
            }
        }
        
        return m_orderByAttributeNames;
    }

    /**
     * INTERNAL:
     */
    public Class getJavaClass() {
        return m_javaClass;
    }
    
    /**
     * INTERNAL:
     */
    public String getJavaClassName() {
        return m_descriptor.getJavaClassName();
    }
    
    /**
     * INTERNAL:
     */
    public MetadataLogger getLogger() {
        return getProject().getLogger();
    }
    
    /** 
     * INTERNAL:
     * Returns the immediate parent's descriptor in the inheritance hierarchy.
     */
    public MetadataDescriptor getInheritanceParentDescriptor() {
        return m_inheritanceParentDescriptor;
    }
    
    /** 
     * INTERNAL:
     * Returns the root descriptor of the inheritance hierarchy, that is, the 
     * one that defines the inheritance strategy.
     */
    public MetadataDescriptor getInheritanceRootDescriptor() {
        return m_inheritanceRootDescriptor;
    }
    
    /**
     * INTERNAL:
     */
    public DatabaseMapping getMappingForAttributeName(String attributeName) {
        return getMappingForAttributeName(attributeName, null);
    } 
    
    /**
     * INTERNAL: 
     * Non-owning mappings that need to look up the owning mapping, should call 
     * this method with their respective accessor to check for circular mappedBy 
     * references. If the referencingAccessor is null, no check will be made.
     */
    public DatabaseMapping getMappingForAttributeName(String attributeName, MetadataAccessor referencingAccessor) {
        // Get accessor will traverse the parent descriptors of an inheritance
        // hierarchy.
        MappingAccessor accessor = getAccessorFor(attributeName);
        
        if (accessor != null) {
            // If the accessor is a relationship accessor than it may or may
            // not have been processed yet. Fast track its processing if it
            // needs to be. The process call will do nothing if it has already
            // been processed.
            if (accessor.isRelationship()) {
                RelationshipAccessor relationshipAccessor = (RelationshipAccessor) accessor;
                
                // Check that we don't have circular mappedBy values which 
                // will cause an infinite loop.
                if (referencingAccessor != null && (relationshipAccessor.isOneToOne() || relationshipAccessor.isCollectionAccessor())) {
                    String mappedBy = null;
                    
                    if (relationshipAccessor.isOneToOne()) {
                        mappedBy = ((OneToOneAccessor) relationshipAccessor).getMappedBy();
                    } else {
                        mappedBy = ((CollectionAccessor) relationshipAccessor).getMappedBy();
                    }
                    
                    if (mappedBy != null && mappedBy.equals(referencingAccessor.getAttributeName())) {
                        throw ValidationException.circularMappedByReferences(referencingAccessor.getJavaClass(), referencingAccessor.getAttributeName(), getJavaClass(), attributeName);
                    }
                }
                
                relationshipAccessor.processRelationship();
            }
            
            // Return the mapping stored on the accessor.
            return accessor.getMapping();
        }
        
        // Found nothing ... return null.
        return null;
    } 
    
    /**
     * INTERNAL:
     */
    public List<DatabaseMapping> getMappings() {
        return m_descriptor.getMappings();
    }

    /**
     * INTERNAL:
     */
    public Class getPKClass(){
        return m_pkClass;
    }
    
    /**
     * INTERNAL:
     */
    public String getPKClassName() {
        String pkClassName = null;
        
        if (m_descriptor.hasCMPPolicy()) {
            pkClassName = ((CMP3Policy) m_descriptor.getCMPPolicy()).getPKClassName();    
        }
        
        return pkClassName;
    }
    
    /**
     * INTERNAL:
     */
    public Map<String, Type> getPKClassIDs() {
        return m_pkClassIDs;
    }
    
    /**
     * INTERNAL:
     * Method to return the primary key field name for the given descriptor
     * metadata. Assumes there is one.
     */
    public String getPrimaryKeyFieldName() {
        return (getPrimaryKeyFields().iterator().next()).getName();
    }
    
    /**
     * INTERNAL:
     * Method to return the primary key field names for the given descriptor
     * metadata. getPrimaryKeyFieldNames() on ClassDescriptor returns qualified
     * names. We don't want that.
     */
    public List<String> getPrimaryKeyFieldNames() {
        List<DatabaseField> primaryKeyFields = getPrimaryKeyFields();
        List<String> primaryKeyFieldNames = new ArrayList<String>(primaryKeyFields.size());
        
        for (DatabaseField primaryKeyField : primaryKeyFields) {
            primaryKeyFieldNames.add(primaryKeyField.getName());
        }
        
        return primaryKeyFieldNames;
    }
    
    /**
     * INTERNAL:
     * Return the primary key fields for this descriptor metadata. If this is
     * an inheritance subclass and it has no primary key fields, then grab the 
     * primary key fields from the root.
     */
    public List<DatabaseField> getPrimaryKeyFields() {
        List<DatabaseField> primaryKeyFields = m_descriptor.getPrimaryKeyFields();
        
        if (primaryKeyFields.isEmpty() && isInheritanceSubclass()) {
            primaryKeyFields = getInheritanceRootDescriptor().getPrimaryKeyFields();
        }
        
        return primaryKeyFields;
    }

    /**
     * INTERNAL:
     * Recursively check the potential chaining of the primary key fields from 
     * a inheritance subclass, all the way to the root of the inheritance 
     * hierarchy.
     */
    public String getPrimaryKeyJoinColumnAssociation(String foreignKeyName) {
        String primaryKeyName = m_pkJoinColumnAssociations.get(foreignKeyName);

        if (primaryKeyName == null || ! isInheritanceSubclass()) {
            return foreignKeyName;
        } else {
            return getInheritanceParentDescriptor().getPrimaryKeyJoinColumnAssociation(primaryKeyName);
        } 
    }
    
    /**
     * INTERNAL:
     * Assumes there is one primary key field set. This method should be called 
     * when qualifying any primary key field (from a join column) for this 
     * descriptor. This method was created because in an inheritance hierarchy 
     * with a joined strategy we can't use getPrimaryTableName() since it would
     * return the wrong table name. From the spec, the primary key must be 
     * defined on the entity that is the root of the entity hierarchy or on a 
     * mapped superclass of the entity hierarchy. The primary key must be 
     * defined exactly once in an entity hierarchy.
     */
    public DatabaseTable getPrimaryKeyTable() {
        return ((getPrimaryKeyFields().iterator().next())).getTable();
    }
    
    /**
     * INTERNAL:
     */
    public DatabaseTable getPrimaryTable() {
        if (m_primaryTable == null && isInheritanceSubclass()) {
            return getInheritanceRootDescriptor().getPrimaryTable();
        } else {
            if (m_descriptor.isAggregateDescriptor()) {
                // Aggregate descriptors don't have tables, just return a 
                // a default empty table.
                return new DatabaseTable();
            }
            
            return m_primaryTable;
        }
    }
    
    /**
     * INTERNAL:
     */
    public String getPrimaryTableName() {
        return getPrimaryTable().getName();
    }

    /**
     * INTERNAL:
     */
    public MetadataProject getProject() {
        return getClassAccessor().getProject();
    }
    
    /**
     * INTERNAL:
     */
    protected ReturningPolicy getReturningPolicy() {
        if (! m_descriptor.hasReturningPolicy()) {
            m_descriptor.setReturningPolicy(new ReturningPolicy());
        }
        
        return m_descriptor.getReturningPolicy();
    }
    
    /**
     * INTERNAL:
     */
    public DatabaseField getSequenceNumberField() {
        return m_descriptor.getSequenceNumberField();
    }
    
    /**
     * INTERNAL:
     * Returns true if we already have (processed) an accessor for the given
     * attribute name.
     */
    public boolean hasAccessorFor(String attributeName) {
        return getAccessorFor(attributeName) != null;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasAssociationOverrideFor(String attributeName) {
        return m_associationOverrides.containsKey(attributeName);
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasAttributeOverrideFor(String attributeName) {
        return m_attributeOverrides.containsKey(attributeName);
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasCompositePrimaryKey() {
        return getPrimaryKeyFields().size() > 1 || getPKClass() != null;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasEmbeddedIdAttribute() {
        return m_embeddedIdAttributeName != null;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasExistenceChecking() {
        return m_existenceChecking != null;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasBiDirectionalManyToManyAccessorFor(String className, String attributeName) {
        if (m_biDirectionalManyToManyAccessors.containsKey(className)) {
            return m_biDirectionalManyToManyAccessors.get(className).containsKey(attributeName);
        }
        
        return false;
    }

    /**
     * INTERNAL:
     * Indicates that a Cache annotation or cache element has already been 
     * processed for this descriptor.
     */
    public boolean hasCache() {
        return m_hasCache;
    }
    
    /**
     * INTERNAL:
     * Indicates that a CacheInterceptor annotation or cacheInterceptor element has already been 
     * processed for this descriptor.
     */
    public boolean hasCacheInterceptor() {
        return m_hasCacheInterceptor;
    }
    
    /**
     * INTERNAL:
     * Indicates that a DefaultRedirectors annotation or default-redirectors element has already been 
     * processed for this descriptor.
     */
    public boolean hasDefaultRedirectors() {
        return m_hasDefaultRedirectors;
    }
    
    /**
     * INTERNAL:
     * Indicates that a Change tracking annotation or change tracking element 
     * has already been processed for this descriptor.
     */
    public boolean hasChangeTracking() {
        return m_hasChangeTracking;
    }
    
    /**
     * INTERNAL:
     * Indicates that a copy Policy annotation or copy policy element 
     * has already been processed for this descriptor.
     */
    public boolean hasCopyPolicy() {
        return m_hasCopyPolicy;
    }

    /**
     * INTERNAL:
     * Indicates that a customizer annotation or customizer element has already 
     * been processed for this descriptor.
     */
    public boolean hasCustomizer() {
        return m_hasCustomizer;
    }
    
    /**
     * INTERNAL:
     * Indicates that a read only annotation or read only element has already 
     * been processed for this descriptor.
     */
    public boolean hasReadOnly() {
        return m_hasReadOnly;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasMappingForAttributeName(String attributeName) {
        return m_descriptor.getMappingForAttributeName(attributeName) != null;
    }
    
    /**
     * INTERNAL:
     * Return true is the descriptor has primary key fields set.
     */
    public boolean hasPrimaryKeyFields() {
        return m_descriptor.getPrimaryKeyFields().size() > 0;
    }
 
    /**
     * INTERNAL:
     * Indicates whether or not annotations should be ignored. However, default 
     * mappings will still be processed unless an exclude-default-mappings 
     * setting is specified.
     * @see ignoreDefaultMappings()
     */
    public boolean ignoreAnnotations() {
        return m_ignoreAnnotations;
    }
    
    /**
     * INTERNAL:
     * Indicates whether or not default mappings should be ignored.
     */
    public boolean ignoreDefaultMappings() {
        return m_ignoreDefaultMappings;
    }
    
    /**
     * INTERNAL:
     * Indicates that cascade-persist should be applied to all relationship 
     * mappings for this entity.
     */
    public boolean isCascadePersist() {
        return m_isCascadePersist;
    }
    
    /**
     * INTERNAL:
     */
    public boolean isEmbeddable() {
        return m_descriptor.isAggregateDescriptor();
    }
    
    /**
     * INTERNAL:
     */
    public boolean isEmbeddableCollection() {
        return m_descriptor.isAggregateCollectionDescriptor();
    }
    
    /**
     * INTERNAL:
     */
    public boolean isInheritanceSubclass() {
        return m_inheritanceParentDescriptor != null;
    }
    
    /**
     * INTERNAL:
     * Method to check if the given field is part of the primary key for
     * this descriptor. It will check against actual field instances and
     * not rely on the equals code logic from DatabaseField since it works
     * as follows:
     *  - ID and ID - returns true
     *  - EMPLOYEE.ID and ID - returns true
     *  - ID and ADDRESS.ID - returns true
     *  - EMPLOYEE.ID and ADDRESS.ID - returns false
     *  Performing a list contains check could incorrectly return a true value
     *  indicating the field is part of the primary key when it actually is not.
     */
    public boolean isPrimaryKeyField(DatabaseField field) {
        for (DatabaseField idField : getPrimaryKeyFields()) {
            if (field == idField) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     */
    public boolean pkClassWasNotValidated() {
        return ! m_pkClassIDs.isEmpty();
    }
    
    /**
     * INTERNAL
     * Pre-process the potential embeddable class if it is indeed an embeddable.
     */
    protected void preProcessEmbeddableAccessor(Class potentialEmbeddableClass, MetadataDescriptor owningDescriptor) {
        if (potentialEmbeddableClass != null) {
            EmbeddableAccessor embeddableAccessor = getProject().getEmbeddableAccessor(potentialEmbeddableClass);
        
            if (embeddableAccessor != null && ! embeddableAccessor.isPreProcessed()) {
                embeddableAccessor.setOwningDescriptor(owningDescriptor);
                embeddableAccessor.preProcess();
            }
        }
    }
    
    /**
     * INTERNAL:
     * Process this descriptors accessors. Some accessors will not be processed
     * right away, instead stored on the project for processing in a later 
     * stage.
     */
    public void processAccessors(MetadataDescriptor owningDescriptor) {
        for (MappingAccessor accessor : m_accessors.values()) {
            if (! accessor.isProcessed()) {
                if (accessor.isDerivedId()){
                    m_derivedIDAccessors.add((ObjectAccessor) accessor);
                    getProject().addAccessorWithDerivedIDs(m_classAccessor);
                }
                
                // We need to defer the processing of some mappings to stage
                // 2 processing. Accessors are added to different lists since
                // the order or processing of those accessors is important.
                // See MetadataProject.processStage2() for more details.
                // Care must be taken in the order of checking here.
                if (accessor.isDirectEmbeddableCollection() || accessor.isEmbedded()) {
                    EmbeddableAccessor embeddableAccessor = getProject().getEmbeddableAccessor(accessor.getReferenceClass());
                    
                    // If there is no embeddable accessor at this point,
                    // something is wrong, throw an exception. Note a direct
                    // embeddable collection can't hit here since we don't build 
                    // a direct embeddable collection if the reference class is 
                    // not an Embeddable.
                    if (embeddableAccessor == null) {
                        throw ValidationException.invalidEmbeddedAttribute(getJavaClass(), accessor.getAttributeName(), accessor.getReferenceClass());
                    } else {
                        // Process the embeddable class now.
                        embeddableAccessor.process(owningDescriptor);
                    
                        // Store this descriptor metadata. It may be needed again 
                        // later on to look up a mappedBy attribute etc.
                        addEmbeddableDescriptor(embeddableAccessor.getDescriptor());
                    
                        // Since association overrides are not allowed on embeddedid
                        // cases (since only direct to field mappings are allowed)
                        // we can process the emebeddedid right now. This will
                        // likely have to change in the future as I could see 
                        // embedded id's allowing foreign key references to be part
                        // of the primary key.
                        if (accessor.isEmbeddedId()) {
                            // Process it right now ...
                            accessor.process();
                        } else {
                            // Otherwise defer it because of association overrides.
                            // We can't process this mapping till all the
                            // relationship mappings have been processed.
                            getProject().addEmbeddableMappingAccessor(accessor);
                        }
                    }
                } else if (accessor.isDirectCollection()) {
                    getProject().addDirectCollectionAccessor(accessor);
                } else if (accessor.isRelationship()) {
                    addRelationshipAccessor(accessor);
                } else {
                    accessor.process();
                }
            }
        }
    }
    
    /**
     * INTERNAL:
     * Remove the following field from the primary key field lists. Presumably,
     * it is not a primary key field or is being replaced with another. See
     * EmbeddedAccessor processAttributeOverride method.
     */
    public void removePrimaryKeyField(DatabaseField field) {
        getPrimaryKeyFields().remove(field);
    }
    
    /**
     * INTERNAL:
     */
    public void setAlias(String alias) {
        m_descriptor.setAlias(alias);
    }
            
    /**
     * INTERNAL:
     */
    public void setClassAccessor(ClassAccessor accessor) {
        m_classAccessor = accessor;
        accessor.setDescriptor(this);
    }
    
    /**
     * INTERNAL:
     */
    public void setDefaultAccess(String defaultAccess) {
        m_defaultAccess = defaultAccess;
    }
    
    /**
     * INTERNAL:
     */
    public void setDefaultCatalog(String defaultCatalog) {
        m_defaultCatalog = defaultCatalog;
    }
    
    /**
     * INTERNAL:
     */
    public void setDefaultSchema(String defaultSchema) {
        m_defaultSchema = defaultSchema;
    }
    
    /**
     * INTERNAL:
     */
    public void setDescriptor(ClassDescriptor descriptor) {
        m_descriptor = descriptor;
    }
    
    /**
     * INTERNAL:
     */
    public void setEmbeddedIdAttributeName(String embeddedIdAttributeName) {
        m_embeddedIdAttributeName = embeddedIdAttributeName;
    }
    
    /** 
     * INTERNAL:
     */
    public void setEntityEventListener(EntityListener listener) {
        m_descriptor.getEventManager().setEntityEventListener(listener);
    }
    
    /**
     * INTERNAL:
     */
    public void setExcludeDefaultListeners(boolean excludeDefaultListeners) {
        m_descriptor.getEventManager().setExcludeDefaultListeners(excludeDefaultListeners);
    }
    
    /**
     * INTERNAL:
     */
    public void setExcludeSuperclassListeners(boolean excludeSuperclassListeners) {
        m_descriptor.getEventManager().setExcludeSuperclassListeners(excludeSuperclassListeners);
    }
    
    /**
     * INTERNAL:
     */
    public void setExistenceChecking(Enum existenceChecking) {
        m_existenceChecking = existenceChecking;
        
        if (existenceChecking.name().equals(ExistenceType.CHECK_CACHE.name())) {
            m_descriptor.getQueryManager().checkCacheForDoesExist();
        } else if (existenceChecking.name().equals(ExistenceType.CHECK_DATABASE.name())) {
            m_descriptor.getQueryManager().checkDatabaseForDoesExist();
        } else if (existenceChecking.name().equals(ExistenceType.ASSUME_EXISTENCE.name())) {
            m_descriptor.getQueryManager().assumeExistenceForDoesExist();
        } else if (existenceChecking.name().equals(ExistenceType.ASSUME_NON_EXISTENCE.name())) {
            m_descriptor.getQueryManager().assumeNonExistenceForDoesExist();
        }
    }
    
    /**
     * INTERNAL:
     * Indicates that we have processed a cache annotation or cache xml element.
     */
    public void setHasCache() {
        m_hasCache = true;
    }
    
    /**
     * INTERNAL:
     * Indicates that we have processed a cache annotation or cache xml element.
     */
    public void setHasCacheInterceptor() {
        m_hasCacheInterceptor = true;
    }
    
    /**
     * INTERNAL:
     * Indicates that we have processed a cache annotation or cache xml element.
     */
    public void setHasDefaultRedirectors() {
        m_hasDefaultRedirectors = true;
    }
    
    /**
     * INTERNAL:
     * Indicates that we have processed a change tracking annotation or change
     * tracking xml element.
     */
    public void setHasChangeTracking() {
        m_hasChangeTracking = true;
    }
    
    /**
     * INTERNAL:
     * Indicates that we have processed a copy policy annotation or copy policy xml element.
     */
    public void setHasCopyPolicy() {
        m_hasCopyPolicy = true;
    }
    
    /**
     * INTERNAL:
     * Indicates that all annotations should be ignored. However, default 
     * mappings will still be processed unless an exclude-default-mappings 
     * setting is specified.
     * @see setIgnoreDefaultMappings()
     */
    public void setIgnoreAnnotations(boolean ignoreAnnotations) {
        m_ignoreAnnotations = ignoreAnnotations;
    }
    
    /**
     * INTERNAL:
     * Indicates that default mappings should be ignored.
     */
    public void setIgnoreDefaultMappings(boolean ignoreDefaultMappings) {
        m_ignoreDefaultMappings = ignoreDefaultMappings;
    }
    
    /**
     * INTERNAL:
     * Set the immediate parent's descriptor of the inheritance hierarchy.
     */
    public void setInheritanceParentDescriptor(MetadataDescriptor inheritanceParentDescriptor) {
        m_inheritanceParentDescriptor = inheritanceParentDescriptor;
    }
    
    /**
     * INTERNAL:
     * Set the root descriptor of the inheritance hierarchy, that is, the one 
     * that defines the inheritance strategy.
     */
    public void setInheritanceRootDescriptor(MetadataDescriptor inheritanceRootDescriptor) {
        m_inheritanceRootDescriptor = inheritanceRootDescriptor;
    }
    
    /**
     * INTERNAL:
     * Indicates that cascade-persist should be added to the set of cascade 
     * values for all relationship mappings.
     */
    public void setIsCascadePersist(boolean isCascadePersist) {
        m_isCascadePersist = isCascadePersist;
    }
    
    /**
     * INTERNAL:
     */
    public void setIsEmbeddable() {
        m_descriptor.descriptorIsAggregate();
    }
    
    /**
     * INTERNAL:
     */
    public void setIsEmbeddableCollection() {
        m_descriptor.descriptorIsAggregateCollection();
    }
    
    /**
     * INTERNAL:
     * Used to set this descriptors java class. 
     */
    public void setJavaClass(Class javaClass) {
        m_javaClass = javaClass;
        m_descriptor.setJavaClassName(javaClass.getName());
        
        // If the javaClass is an interface, add it to the java interface name
        // on the relational descriptor.
        if (javaClass.isInterface()) {
            m_descriptor.setJavaInterfaceName(javaClass.getName());
        }
    }
    
    /**
     * INTERNAL:
     */
    public void setOptimisticLockingPolicy(OptimisticLockingPolicy policy) {
        m_descriptor.setOptimisticLockingPolicy(policy);
    }
    
    /**
     * INTERNAL:
     */
    public void setPKClass(Class pkClass) {
        m_pkClass = pkClass;
        
        CMP3Policy policy = new CMP3Policy();
        policy.setPrimaryKeyClassName(pkClass.getName());
        m_descriptor.setCMPPolicy(policy);
    }
    
    /**
     * INTERNAL:
     */
    public void setPrimaryTable(DatabaseTable primaryTable) {
        addTable(primaryTable);
        m_primaryTable = primaryTable;
    }
    
    /**
     * INTERNAL:
     */
    public void setReadOnly(boolean readOnly) {
        if (readOnly) {
            m_descriptor.setReadOnly();
        }
        
        m_hasReadOnly = true;
    }
    
    /**
     * INTERNAL:
     */
    public void setSequenceNumberField(DatabaseField field) {
        m_descriptor.setSequenceNumberField(field);
    }
    
    /**
     * INTERNAL:
     */
    public void setSequenceNumberName(String name) {
        m_descriptor.setSequenceNumberName(name);
    }
 
    /**
     * INTERNAL:
     */
    public void setUsesCascadedOptimisticLocking(Boolean usesCascadedOptimisticLocking) {
        m_usesCascadedOptimisticLocking = usesCascadedOptimisticLocking;
    }
    
    /**
     * INTERNAL:
     */
    public boolean usesCascadedOptimisticLocking() {
        return m_usesCascadedOptimisticLocking != null && m_usesCascadedOptimisticLocking.booleanValue();
    }
    
    /**
     * INTERNAL:
     * Returns true if this class uses default property access. All access 
     * discovery and processing should have been performed before calling 
     * this method and a default access type should have been set. 
     */
    public boolean usesDefaultPropertyAccess() {
        return m_defaultAccess.equals(MetadataConstants.PROPERTY);
    }
    
    /**
     * INTERNAL:
     */
    public boolean usesOptimisticLocking() {
        return m_descriptor.usesOptimisticLocking();
    }
    
    /**
     * INTERNAL:
     * Indicates if the strategy on the descriptor's inheritance policy is 
     * SINGLE_TABLE. This method must only be called on those descriptors 
     * holding an EntityAccessor. NOTE: Inheritance is currently not supported 
     * on embeddables.
     */
    public boolean usesSingleTableInheritanceStrategy() {
        return ((EntityAccessor) m_classAccessor).getInheritance().usesSingleTableStrategy();
    }
    
    /**
     * INTERNAL:
     * Return true if this descriptor uses a table per class inheritance policy.
     */
    public boolean usesTablePerClassInheritanceStrategy() {
        return m_descriptor.hasTablePerClassPolicy();
    }
    
    /**
     * INTERNAL:
     * Return true if this descriptors class processed OptimisticLocking 
     * meta data of type VERSION_COLUMN.
     */
    public boolean usesVersionColumnOptimisticLocking() {
        // If an optimistic locking metadata of type VERSION_COLUMN was not 
        // specified, then m_usesCascadedOptimisticLocking will be null, that 
        // is, we won't have processed the cascade value.
        return m_usesCascadedOptimisticLocking != null;
    }
    
    /**
     * INTERNAL:
     * This method is used only to validate id fields that were found on a
     * pk class were also found on the entity.
     */
    public void validatePKClassId(String attributeName, Type type) {
        if (m_pkClassIDs.containsKey(attributeName))  {
            Type expectedType =  m_pkClassIDs.get(attributeName);
            
            if (type == expectedType) {
                m_pkClassIDs.remove(attributeName);
            } else {
                throw ValidationException.invalidCompositePKAttribute(getJavaClass(), getPKClassName(), attributeName, expectedType, type);
            }
        }
    }
}
