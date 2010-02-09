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
 *     04/24/2009-2.0 Guy Pelletier 
 *       - 270011: JPA 2.0 MappedById support
 *     06/02/2009-2.0 Guy Pelletier 
 *       - 278768: JPA 2.0 Association Override Join Table
 *     06/16/2009-2.0 Guy Pelletier 
 *       - 277039: JPA 2.0 Cache Usage Settings
 *     06/25/2009-2.0 Michael O'Brien 
 *       - 266912: add isMappedSuperclass() helper function in support
 *         of MappedSuperclass handling for the Metamodel API.
 *     08/11/2009-2.0 Michael O'Brien 
 *       - 284147: So we do not add a pseudo PK Field for MappedSuperclasses when
 *         1 or more PK fields already exist on the descriptor. 
 *         Add m_idAccessor map and hasIdAccessor() function.
 *     10/21/2009-2.0 Guy Pelletier 
 *       - 290567: mappedbyid support incomplete
 *     01/22/2010-2.0.1 Guy Pelletier 
 *       - 294361: incorrect generated table for element collection attribute overrides
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata;

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
import org.eclipse.persistence.descriptors.DescriptorEventListener;

import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.descriptors.OptimisticLockingPolicy;

import org.eclipse.persistence.internal.jpa.CMP3Policy;

import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EmbeddableAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EntityAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.CollectionAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.EmbeddedIdAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.IdAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.ManyToManyAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.MappedKeyMapAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.MappingAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.ObjectAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.OneToOneAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.RelationshipAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataClass;
import org.eclipse.persistence.internal.jpa.metadata.accessors.MetadataAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.PropertyMetadata;

import org.eclipse.persistence.internal.jpa.metadata.columns.AssociationOverrideMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.AttributeOverrideMetadata;

import org.eclipse.persistence.internal.jpa.metadata.listeners.EntityListener;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.helper.Helper;

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
    
    private Boolean m_cacheable;
    private Boolean m_usesCascadedOptimisticLocking;
    
    private ClassAccessor m_classAccessor;
    private ClassDescriptor m_descriptor;
    private DatabaseTable m_primaryTable;
    // The embedded id accessor for this descriptor if one exists.
    private EmbeddedIdAccessor m_embeddedIdAccessor;
    
    private List<String> m_idAttributeNames;
    private List<String> m_orderByAttributeNames;
    private List<String> m_idOrderByAttributeNames;
    private List<MetadataDescriptor> m_embeddableDescriptors;
    
    // Holds a list of derived id accessors.
    private List<ObjectAccessor> m_derivedIdAccessors;
    
    private Map<String, String> m_pkClassIDs;
    private Map<String, String> m_genericTypes;
    private Map<String, MappingAccessor> m_accessors;
    private Map<String, IdAccessor> m_idAccessors;
    private Map<String, MappingAccessor> m_primaryKeyAccessors;
    private Map<String, PropertyMetadata> m_properties;
    private Map<String, String> m_pkJoinColumnAssociations;
    private Map<String, AttributeOverrideMetadata> m_attributeOverrides;
    private Map<String, AssociationOverrideMetadata> m_associationOverrides;
    private Map<String, Map<String, MetadataAccessor>> m_biDirectionalManyToManyAccessors;
    
    private MetadataClass m_pkClass;
    private MetadataClass m_javaClass;
    // This is the root descriptor of the inheritance hierarchy. That is, for 
    // the entity that defines the inheritance strategy.
    private MetadataDescriptor m_inheritanceRootDescriptor;
    // This is our immediate parent's descriptor. Which may also be the root. 
    private MetadataDescriptor m_inheritanceParentDescriptor;
    
    // This is the default access type for the class accessor of this 
    // descriptor. The default access type is needed for those embeddables and 
    // mapped superclasses that are 'owned' or rely on this value for their own 
    // processing. It does not reflect an explicit access type.
    private String m_defaultAccess; 
    private String m_defaultSchema;
    private String m_defaultCatalog;
    private String m_existenceChecking;

    /**
     * INTERNAL: 
     */
    public MetadataDescriptor(MetadataClass javaClass) {
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
        m_derivedIdAccessors = new ArrayList<ObjectAccessor>();
        
        m_pkClassIDs = new HashMap<String, String>();
        m_genericTypes = new HashMap<String, String>();
        m_accessors = new HashMap<String, MappingAccessor>();
        m_idAccessors = new HashMap<String, IdAccessor>();
        m_primaryKeyAccessors = new HashMap<String, MappingAccessor>();
        m_properties = new HashMap<String, PropertyMetadata>();
        m_pkJoinColumnAssociations = new HashMap<String, String>();
        m_attributeOverrides = new HashMap<String, AttributeOverrideMetadata>();
        m_associationOverrides = new HashMap<String, AssociationOverrideMetadata>();
        m_biDirectionalManyToManyAccessors = new HashMap<String, Map<String, MetadataAccessor>>();
        
        m_descriptor = new RelationalDescriptor();
        m_descriptor.setAlias("");
        
        // This is the default, set it in case no existence-checking is set.
        m_descriptor.getQueryManager().checkDatabaseForDoesExist();
        
        setJavaClass(javaClass);
    }
    
    /**
     * INTERNAL: 
     */
    public MetadataDescriptor(MetadataClass javaClass, ClassAccessor classAccessor) {
        this(javaClass);
        m_classAccessor = classAccessor;
    }
    
    /**
     * INTERNAL:
     * We must check for null since buildAccessor from ClassAccessor may return
     * a null if ignore default mappings is set to true.</p>
     * If the accessor is an IdAccessor we store it in a separate map for use
     * during MappedSuperclass processing.
     */
    public void addAccessor(MappingAccessor accessor) {
        // Don't bother adding a relationship accessor with a type of
        // ValueHolderInterface. This may be very legacy and no longer needed
        // but for the canonical model processing it's much cleaner if this
        // accessor does not show up in the accessors list. NOTE: processing
        // avoidance of this accessor was previously in in 
        // RelationshipAccessor.processRelationship().
        if (accessor.isRelationship() && ((RelationshipAccessor) accessor).isValueHolderInterface()) {
            return;
        }
        
        m_accessors.put(accessor.getAttributeName(), accessor);
        
        // Store IdAccessors in a separate map for use by hasIdAccessor()
        if (accessor.isId()) {
            m_idAccessors.put(accessor.getAttributeName(), (IdAccessor) accessor);
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
    public void addInternalListener(DescriptorEventListener validationListener) {
        m_descriptor.getEventManager().addinternalListener(validationListener);
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
    public void addGenericType(String genericName, String type) {
        m_genericTypes.put(genericName, type);
    }
    
    /**
     * INTERNAL:
     * We store these to validate the primary class when processing
     * the entity class.
     */
    public void addPKClassId(String attributeName, String type) {
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
     * Add a field representing the primary key or part of a composite primary key
     * to the List of primary key fields on the relational descriptor associated
     * with this metadata descriptor.</p>
     */
    public void addPrimaryKeyField(DatabaseField field, MappingAccessor accessor) {
        m_descriptor.addPrimaryKeyField(field);
        
        // Store the primary primary key mappings based on their field name.
        m_primaryKeyAccessors.put(field.getName(), accessor);
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
            accessor = m_accessors.get(Helper.getAttributeNameFromMethodName(fieldOrPropertyName));
           
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
        String defaultTableName = null;
        if (getProject() != null && getProject().getPersistenceUnitMetadata() != null && getProject().getPersistenceUnitMetadata().isDelimitedIdentifiers()){
            defaultTableName = getAlias();
        } else {
            defaultTableName = getAlias().toUpperCase();
        }
        
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
     * Return the RelationalDescriptor instance associated with this MetadataDescriptor
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
    public List<ObjectAccessor> getDerivedIdAccessors(){
        return m_derivedIdAccessors;
    }
    
    /**
     * INTERNAL:
     * Return the embedded id accessor for this descriptor if one exists.
     */
    public EmbeddedIdAccessor getEmbeddedIdAccessor() {
        return m_embeddedIdAccessor;
    }
    
    /**
     * INTERNAL:
     */
    public String getEmbeddedIdAttributeName() {
        return m_embeddedIdAccessor.getAttributeName();
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
    public String getGenericType(String genericName) {
       return m_genericTypes.get(genericName); 
    }
    
    /**
     * INTERNAL:
     */
    public Map getGenericTypes() {
        return m_genericTypes;
    }
    
    /**
     * INTERNAL:
     */
    public Map<String, IdAccessor> getIdAccessors() {
        return m_idAccessors;
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
    public MetadataClass getJavaClass() {
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
    public MetadataClass getPKClass(){
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
     * Return the primary key mapping for the given field. 
     */
    public MappingAccessor getPrimaryKeyAccessorForField(DatabaseField field) {
        return m_primaryKeyAccessors.get(field.getName());
    }
    
    /**
     * INTERNAL:
     */
    public Map<String, String> getPKClassIDs() {
        return m_pkClassIDs;
    }
    
    /**
     * INTERNAL:
     * Method to return the primary key field name this descriptor metadata. 
     * It assumes there is one.
     */
    public DatabaseField getPrimaryKeyField() {
        return getPrimaryKeyFields().iterator().next();
    }
    
    /**
     * INTERNAL:
     * Method to return the primary key field name for the given descriptor
     * metadata. Assumes there is one.
     */
    public String getPrimaryKeyFieldName() {
        return getPrimaryKeyField().getName();
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
    public boolean hasEmbeddedId() {
        return m_embeddedIdAccessor != null;
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
     * Indicates that a Cacheable annotation or cache element has already been 
     * processed for this descriptor.
     */
    public boolean hasCacheable() {
        return m_cacheable != null;
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
     * Return whether there is an IdAccessor on this descriptor.
     * @return
     */
    public boolean hasIdAccessor() {
        return !m_idAccessors.isEmpty();        
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasMappingForAttributeName(String attributeName) {
        return m_descriptor.getMappingForAttributeName(attributeName) != null;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasPKClass() {
        return m_pkClass != null;
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
     * Indicates that an explicit cacheable value of true has been set for 
     * this descriptor.
     */
    public boolean isCacheableTrue() {
        if (m_cacheable != null) {
            return m_cacheable.booleanValue();
        } else if (isInheritanceSubclass()) {
            return getInheritanceParentDescriptor().isCacheableTrue();
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     * Indicates that an explicit cacheable value of false has been set for 
     * this descriptor.
     */
    public boolean isCacheableFalse() {
        if (m_cacheable != null) {
            return ! m_cacheable.booleanValue();
        } else if (isInheritanceSubclass()) {
            return getInheritanceParentDescriptor().isCacheableFalse();
        }

        return false;
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
     * Return whether the ClassAccessor on this MetadataDescriptor is a MappedSuperclassAccessor.
     * @since EclipseLink 1.2 for the JPA 2.0 Reference Implementation
     */
    public boolean isMappedSuperclass() {
        return getClassAccessor().isMappedSuperclass();
    }
    
    /**
     * INTERNAL:
     */
    public boolean pkClassWasNotValidated() {
        return ! m_pkClassIDs.isEmpty();
    }
    
    /**
     * INTERNAL:
     * Process this descriptors accessors. Some accessors will not be processed
     * right away, instead stored on the project for processing in a later 
     * stage. This method can not and must not be called beyond MetadataProject 
     * stage 2 processing.
     */
    public void processAccessors() {
        for (MappingAccessor accessor : m_accessors.values()) {
            if (! accessor.isProcessed()) {
                // If we a mapped key map accessor with an embeddable as the 
                // key, process that embeddable accessor now.
                if (accessor.isMappedKeyMapAccessor()) {
                    MappedKeyMapAccessor mapAccessor = (MappedKeyMapAccessor) accessor;
                    EmbeddableAccessor mapKeyEmbeddableAccessor = getProject().getEmbeddableAccessor(mapAccessor.getMapKeyClass());
                    
                    if (mapKeyEmbeddableAccessor != null && ! mapKeyEmbeddableAccessor.isProcessed()) {
                        mapKeyEmbeddableAccessor.process();
                    }
                }
                
                // We need to defer the processing of some mappings to stage
                // 3 processing. Accessors are added to different lists since
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
                        // Process the embeddable class now (if it's not already processed)
                        if (! embeddableAccessor.isProcessed()) {
                            embeddableAccessor.process();
                        }
                    
                        // Store this descriptor metadata. It may be needed again 
                        // later on to look up a mappedBy attribute etc.
                        addEmbeddableDescriptor(embeddableAccessor.getDescriptor());
                    
                        // Since association overrides are not allowed on 
                        // embeddedid's we can and must process it right now,
                        // instead of deferring it till after the relationship
                        // accessors have processed.
                        if (accessor.isEmbeddedId() || accessor.isDerivedIdClass()) {
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
                    if (accessor.derivesId()) {
                        m_derivedIdAccessors.add((ObjectAccessor) accessor);
                        getProject().addAccessorWithDerivedId(m_classAccessor);
                    } else {
                        addRelationshipAccessor(accessor);
                    }
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
        
        // Remove the primary key accessor.
        m_primaryKeyAccessors.remove(field.getName());
    }
    
    /**
     * INTERNAL:
     */
    public void setAlias(String alias) {
        m_descriptor.setAlias(alias);
    }
    
    /**
     * INTERNAL:
     * Set the cacheable value of this descriptor.
     */
    public void setCacheable(Boolean cacheable) {
        m_cacheable = cacheable;
    }
    
    /**
     * INTERNAL:
     */
    public void setClassAccessor(ClassAccessor accessor) {
        m_classAccessor = accessor;
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
     * Set the RelationalDescriptor instance associated with this MetadataDescriptor
     */
    public void setDescriptor(ClassDescriptor descriptor) {
        m_descriptor = descriptor;
    }
    
    /**
     * INTERNAL:
     */
    public void setEmbeddedIdAccessor(EmbeddedIdAccessor embeddedIdAccessor) {
        m_embeddedIdAccessor = embeddedIdAccessor;
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
    public void setExistenceChecking(String existenceChecking) {
        m_existenceChecking = existenceChecking;
        
        if (existenceChecking.equals(ExistenceType.CHECK_CACHE.name())) {
            m_descriptor.getQueryManager().checkCacheForDoesExist();
        } else if (existenceChecking.equals(ExistenceType.CHECK_DATABASE.name())) {
            m_descriptor.getQueryManager().checkDatabaseForDoesExist();
        } else if (existenceChecking.equals(ExistenceType.ASSUME_EXISTENCE.name())) {
            m_descriptor.getQueryManager().assumeExistenceForDoesExist();
        } else if (existenceChecking.equals(ExistenceType.ASSUME_NON_EXISTENCE.name())) {
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
    public void setJavaClass(MetadataClass javaClass) {
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
    public void setPKClass(MetadataClass pkClass) {
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
    @Override
    public String toString() {
        return getJavaClassName();
    }
    
    /**
     * INTERNAL:
     */
    public void useNoCache() {
        m_descriptor.setIsIsolated(true);
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
     * pk class were also found on the entity.  This is used for DerivedIds where the
     * Type is only available as a string
     */
    public void validatePKClassId(String attributeName, MetadataClass type) {
        if (m_pkClassIDs.containsKey(attributeName))  {
            String expectedType =  m_pkClassIDs.get(attributeName);
            if (expectedType.equals(type.getName())) {
                m_pkClassIDs.remove(attributeName);
            } else {
                throw ValidationException.invalidCompositePKAttribute(getJavaClass(), getPKClassName(), attributeName, expectedType, null);
            }
        }
    }
}
