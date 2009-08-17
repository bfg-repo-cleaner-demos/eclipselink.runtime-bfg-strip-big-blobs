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
 *     Guy Pelletier - initial API and implementation from Oracle TopLink
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
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     04/03/2009-2.0 Guy Pelletier
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     04/24/2009-2.0 Guy Pelletier 
 *       - 270011: JPA 2.0 MappedById support
 *     04/30/2009-2.0 Michael O'Brien 
 *       - 266912: JPA 2.0 Metamodel API (part of Criteria API)
 *     06/16/2009-2.0 Guy Pelletier 
 *       - 277039: JPA 2.0 Cache Usage Settings 
 *     06/25/2009-2.0 Michael O'Brien 
 *       - 266912: change MappedSuperclass handling in stage2 to pre process accessors
 *          in support of the custom descriptors holding mappings required by the Metamodel. 
 *          processAccessType() is now public and is overridden in the superclass 
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.classes;

import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.Helper;

import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataClass;
import org.eclipse.persistence.internal.jpa.metadata.columns.AssociationOverrideMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.AttributeOverrideMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.DiscriminatorColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.PrimaryKeyJoinColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.PrimaryKeyJoinColumnsMetadata;

import org.eclipse.persistence.internal.jpa.metadata.inheritance.InheritanceMetadata;
import org.eclipse.persistence.internal.jpa.metadata.listeners.EntityClassListenerMetadata;
import org.eclipse.persistence.internal.jpa.metadata.listeners.EntityListenerMetadata;

import org.eclipse.persistence.internal.jpa.metadata.MetadataConstants;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.MetadataProject;
import org.eclipse.persistence.internal.jpa.metadata.ORMetadata;

import org.eclipse.persistence.internal.jpa.metadata.queries.NamedNativeQueryMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.NamedQueryMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.NamedStoredProcedureQueryMetadata;
import org.eclipse.persistence.internal.jpa.metadata.queries.SQLResultSetMappingMetadata;
import org.eclipse.persistence.internal.jpa.metadata.sequencing.SequenceGeneratorMetadata;
import org.eclipse.persistence.internal.jpa.metadata.sequencing.TableGeneratorMetadata;
import org.eclipse.persistence.internal.jpa.metadata.tables.SecondaryTableMetadata;
import org.eclipse.persistence.internal.jpa.metadata.tables.TableMetadata;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;

/**
 * An entity accessor.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 1.0
 */
public class EntityAccessor extends MappedSuperclassAccessor {
    private ArrayList<MappedSuperclassAccessor> m_mappedSuperclasses = new ArrayList<MappedSuperclassAccessor>();
    private DiscriminatorColumnMetadata m_discriminatorColumn;
    private InheritanceMetadata m_inheritance;
    
    // TODO: Since we allow the annotations of these to be defined on a
    // mapped superclass, we should change the schema to allow that as well
    // and move these attributes up to MappedSuperclass. And remove their 
    // respective processing methods from this class.
    private List<AssociationOverrideMetadata> m_associationOverrides = new ArrayList<AssociationOverrideMetadata>();
    private List<AttributeOverrideMetadata> m_attributeOverrides = new ArrayList<AttributeOverrideMetadata>();
    private List<NamedQueryMetadata> m_namedQueries = new ArrayList<NamedQueryMetadata>();
    private List<NamedNativeQueryMetadata> m_namedNativeQueries = new ArrayList<NamedNativeQueryMetadata>();
    private List<NamedStoredProcedureQueryMetadata> m_namedStoredProcedureQueries = new ArrayList<NamedStoredProcedureQueryMetadata>();
    private List<SQLResultSetMappingMetadata> m_sqlResultSetMappings = new ArrayList<SQLResultSetMappingMetadata>();
    private List<SecondaryTableMetadata> m_secondaryTables = new ArrayList<SecondaryTableMetadata>();
    private List<PrimaryKeyJoinColumnMetadata> m_primaryKeyJoinColumns = new ArrayList<PrimaryKeyJoinColumnMetadata>();
    
    private SequenceGeneratorMetadata m_sequenceGenerator;
 
    private String m_discriminatorValue;
    private String m_entityName;
    
    private TableGeneratorMetadata m_tableGenerator;
    private TableMetadata m_table;
    
    /**
     * INTERNAL:
     */
    public EntityAccessor() {
        super("<entity>");
    }
    
    /**
     * INTERNAL:
     */
    public EntityAccessor(MetadataAnnotation annotation, MetadataClass cls, MetadataProject project) {
        super(annotation, cls, project);
    }
    
    /**
     * INTERNAL:
     * Add multiple fields to the descriptor. Called from either Inheritance 
     * or SecondaryTable context.
     */
    protected void addMultipleTableKeyFields(PrimaryKeyJoinColumnsMetadata primaryKeyJoinColumns, DatabaseTable sourceTable, DatabaseTable targetTable, String PK_CTX, String FK_CTX) {
        // ProcessPrimaryKeyJoinColumns will validate the primary key join
        // columns passed in and will return a list of 
        // PrimaryKeyJoinColumnMetadata.
        for (PrimaryKeyJoinColumnMetadata primaryKeyJoinColumn : processPrimaryKeyJoinColumns(primaryKeyJoinColumns)) {
            // In an inheritance case this call will return the pk field on the
            // root class of the inheritance hierarchy. Otherwise in a secondary
            // table case it's the primary key field name off our own descriptor.
            String defaultPKFieldName = getDescriptor().getPrimaryKeyFieldName();

            DatabaseField pkField = primaryKeyJoinColumn.getPrimaryKeyField();
            pkField.setName(getName(pkField, defaultPKFieldName, PK_CTX));
            pkField.setUseDelimiters(useDelimitedIdentifier());
            pkField.setTable(sourceTable);
            
            DatabaseField fkField = primaryKeyJoinColumn.getForeignKeyField();
            fkField.setName(getName(fkField, pkField.getName(), FK_CTX));
            fkField.setUseDelimiters(useDelimitedIdentifier());
            fkField.setTable(targetTable);

            // Add the foreign key field to the descriptor.
            getDescriptor().addForeignKeyFieldForMultipleTable(fkField, pkField);
        }
    }
    
    /**
     * INTERNAL:
     * Add mapped superclass accessors to inheriting entities.
     * Add new descriptors for these mapped superclasses to the core project.
     */
    protected void addPotentialMappedSuperclass(MetadataClass metadataClass, boolean addMappedSuperclassAccessors) {
        // Get the mappedSuperclass that was stored previously on the project
        MappedSuperclassAccessor accessor = getProject().getMappedSuperclass(metadataClass);

        if (accessor == null) {
            // If the mapped superclass was not defined in XML then check 
            // for a MappedSuperclass annotation unless the 
            // addMappedSuperclassAccessors flag is false, meaning we are
            // pre-processing for the canonical model and any and all 
            // mapped superclasses should have been discovered and we need
            // not investigate this class further.
            if (addMappedSuperclassAccessors) {
                if (metadataClass.isAnnotationPresent(MappedSuperclass.class)) {
                    m_mappedSuperclasses.add(new MappedSuperclassAccessor(metadataClass.getAnnotation(MappedSuperclass.class), metadataClass, getDescriptor()));
                    // 266912: process and store mappedSuperclass descriptors on the project for later use by the Metamodel API
                    MappedSuperclassAccessor msAccessor = new MappedSuperclassAccessor(metadataClass.getAnnotation(MappedSuperclass.class), metadataClass, getProject());
                    // process what type of access is on the superclass (in case inheriting members differ in their access type)
                    getProject().addMetamodelMappedSuperclass(metadataClass, msAccessor);
                }
            }
        } else {
            // For the canonical model pre-processing we do not need to do any
            // of the reloading (cloning) that we require for the regular
            // metadata processing. Therefore, just add the mapped superclass
            // directly leaving its current descriptor as is. When a mapped
            // superclass accessor is reloaded for a sub entity, its descriptor 
            // is set to that entity's descriptor.
            if (addMappedSuperclassAccessors) {
                // Reload the accessor from XML to get our own instance not already on the project
                MappedSuperclassAccessor msAccessor = reloadMappedSuperclass(accessor, getDescriptor());
                m_mappedSuperclasses.add(msAccessor);
                // 266912: process and store mappedSuperclass descriptors on the project for later use by the Metamodel API
                // Note: we must again reload our accessor from XML or we will be sharing instances of the descriptor
                getProject().addMetamodelMappedSuperclass(metadataClass, reloadMappedSuperclass(accessor,  new MetadataDescriptor(metadataClass)));
            } else {
                m_mappedSuperclasses.add(accessor);
            }
        }
    }
    
    /**
     * INTERNAL:
     */
    protected void clearMappedSuperclassesAndInheritanceParents() {
        // Re-initialize the mapped superclass list if it's not empty.
        if (! m_mappedSuperclasses.isEmpty()) {
            m_mappedSuperclasses.clear();
        }
        
        // Null out the inheritance parent and root descriptor before we start
        // since they will be recalculated and used to determine when to stop
        // looking for mapped superclasses.
        getDescriptor().setInheritanceParentDescriptor(null);
        getDescriptor().setInheritanceRootDescriptor(null);
    }
    
    /**
     * INTERNAL:
     * Build a list of classes that are decorated with a MappedSuperclass
     * annotation or that are tagged as a mapped-superclass in an XML document.
     * 
     * This method will also do a couple other things as well since we are
     * traversing the parent classes:
     *  - Build a map of generic types specified and will be used to resolve 
     *    actual class types for mappings.
     *  - Will discover and set the inheritance parent and root descriptors
     *    if this entity is part of an inheritance hierarchy.
     *  - save mapped-superclass descriptors on the project for later use
     *    by the Metamodel API
     * 
     * Note: The list is rebuilt every time this method is called since
     * it is called both during pre-deploy and deploy where the class loader
     * dependencies change.
     */
    protected void discoverMappedSuperclassesAndInheritanceParents(boolean addMappedSuperclassAccessors) {
        // Clear out any previous mapped superclasses and inheritance parents
        // that were discovered.
        clearMappedSuperclassesAndInheritanceParents();
        
        EntityAccessor currentAccessor = this;
        MetadataClass parent = getJavaClass().getSuperclass();
        List<String> genericTypes = getJavaClass().getGenericType();
        
        // We keep a list of potential subclass accessors to ensure they 
        // have their root parent descriptor set correctly.
        List<EntityAccessor> subclassAccessors = new ArrayList<EntityAccessor>();
        subclassAccessors.add(currentAccessor);
        
        if ((parent != null) && !parent.isObject()) {
            while ((parent != null) && !parent.isObject()) {
                EntityAccessor parentAccessor = getProject().getEntityAccessor(parent.getName());
            
                // We found a parent entity.
                if (parentAccessor != null) {
                    // Set the immediate parent's descriptor to the current descriptor.
                    currentAccessor.getDescriptor().setInheritanceParentDescriptor(parentAccessor.getDescriptor());
                
                    // Update the current accessor.
                    currentAccessor = parentAccessor;
                
                    // Clear out any previous mapped superclasses and inheritance 
                    // parents that were discovered. We're going to re-discover
                    // them now.
                    currentAccessor.clearMappedSuperclassesAndInheritanceParents();
                
                    // If we found an entity with inheritance metadata, set the 
                    // root descriptor on its subclasses.
                    if (currentAccessor.hasInheritance()) {
                        for (EntityAccessor subclassAccessor : subclassAccessors) {
                            subclassAccessor.getDescriptor().setInheritanceRootDescriptor(currentAccessor.getDescriptor());
                        }
                    
                        // Clear the subclass list, we'll keep looking but the 
                        // inheritance strategy may have changed so we need to 
                        // build a new list of subclasses.
                        subclassAccessors.clear();
                    }
                
                    // Add the descriptor to the subclass list
                    subclassAccessors.add(currentAccessor);
                
                    // Resolve any generic types from the generic parent onto the
                    // current descriptor.
                    currentAccessor.resolveGenericTypes(genericTypes, parent);                
                } else {
                    // Might be a mapped superclass, check and add as needed.
                    currentAccessor.addPotentialMappedSuperclass(parent, addMappedSuperclassAccessors);
                
                    // Resolve any generic types from the generic parent onto the
                    // current descriptor.
                    currentAccessor.resolveGenericTypes(genericTypes, parent);
                }
            
                // Get the next parent and keep processing ...
                genericTypes = parent.getGenericType();
                parent = parent.getSuperclass();
            }
        } else {
            // Resolve any generic types we have (we may be an inheritance root).
            currentAccessor.resolveGenericTypes(genericTypes, parent);
        }
        
        // Set our root descriptor of the inheritance hierarchy on all the 
        // subclass descriptors. The root may not have explicit inheritance 
        // metadata therefore, make the current descriptor of the entity 
        // hierarchy the root.
        if (! subclassAccessors.isEmpty()) {
            for (EntityAccessor subclassAccessor : subclassAccessors) {
                if (subclassAccessor != currentAccessor) {
                    subclassAccessor.getDescriptor().setInheritanceRootDescriptor(currentAccessor.getDescriptor());
                }
            } 
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<AssociationOverrideMetadata> getAssociationOverrides() {
        return m_associationOverrides;
    } 
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<AttributeOverrideMetadata> getAttributeOverrides() {
        return m_attributeOverrides;
    } 
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public DiscriminatorColumnMetadata getDiscriminatorColumn() {
        return m_discriminatorColumn;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getDiscriminatorValue() {
        return m_discriminatorValue;
    }
 
    /**
     * INTERNAL:
     * Process a discriminator value to set the class indicator on the root 
     * descriptor of the inheritance hierarchy. 
     * 
     * If there is no discriminator value, the class indicator defaults to 
     * the class name.
     */
    public String getDiscriminatorValueOrNull() {
        if (! Modifier.isAbstract(getJavaClass().getModifiers())) {
            // Add the indicator to the inheritance root class' descriptor. The
            // default is the short class name.
            if (m_discriminatorValue == null) {
                MetadataAnnotation discriminatorValue = getAnnotation(DiscriminatorValue.class);
                
                if (discriminatorValue == null) {
                    return Helper.getShortClassName(getJavaClassName());
                } else {
                    return (String) discriminatorValue.getAttribute("value"); 
                }
            } else {
                return m_discriminatorValue;
            }  
        }
        
        return null;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getEntityName() {
        return m_entityName;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public InheritanceMetadata getInheritance() {
        return m_inheritance;
    }
    
    /**
     * INTERNAL:
     * Return the mapped superclasses associated with this entity accessor.
     * A call to discoverMappedSuperclassesAndInheritanceParents() should be
     * made before calling this method. 
     * @see process()
     */
    public ArrayList<MappedSuperclassAccessor> getMappedSuperclasses() {
        return m_mappedSuperclasses;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<NamedNativeQueryMetadata> getNamedNativeQueries() {
        return m_namedNativeQueries;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<NamedQueryMetadata> getNamedQueries() {
        return m_namedQueries;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<NamedStoredProcedureQueryMetadata> getNamedStoredProcedureQueries() {
        return m_namedStoredProcedureQueries;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */    
    public List<PrimaryKeyJoinColumnMetadata> getPrimaryKeyJoinColumns() {
        return m_primaryKeyJoinColumns;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<SecondaryTableMetadata> getSecondaryTables() {
        return m_secondaryTables;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public SequenceGeneratorMetadata getSequenceGenerator() {
        return m_sequenceGenerator;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<SQLResultSetMappingMetadata> getSqlResultSetMappings() {
        return m_sqlResultSetMappings;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public TableMetadata getTable() {
        return m_table;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public TableGeneratorMetadata getTableGenerator() {
        return m_tableGenerator;
    }
    
    /**
     * INTERNAL: 
     * Return true if this class has an inheritance specifications.
     */
    public boolean hasInheritance() {
        if (m_inheritance == null) {
            return isAnnotationPresent(Inheritance.class);
        } else {
            return true;
        }
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject, XMLEntityMappings entityMappings) {
        super.initXMLObject(accessibleObject, entityMappings);
        
        // Initialize single objects.
        initXMLObject(m_inheritance, accessibleObject);
        initXMLObject(m_discriminatorColumn, accessibleObject);
        initXMLObject(m_sequenceGenerator, accessibleObject);
        initXMLObject(m_tableGenerator, accessibleObject);
        initXMLObject(m_table, accessibleObject);
        
        // Initialize lists of objects.
        initXMLObjects(m_associationOverrides, accessibleObject);
        initXMLObjects(m_attributeOverrides, accessibleObject);
        initXMLObjects(m_namedQueries, accessibleObject);
        initXMLObjects(m_namedNativeQueries, accessibleObject);
        initXMLObjects(m_namedStoredProcedureQueries, accessibleObject);
        initXMLObjects(m_sqlResultSetMappings, accessibleObject);
        initXMLObjects(m_secondaryTables, accessibleObject);
        initXMLObjects(m_primaryKeyJoinColumns, accessibleObject);
    }
    
    /** 
     * INTERNAL:
     * Return true if this accessor represents an entity class.
     */
    @Override
    public boolean isEntityAccessor() {
        return true;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public boolean isMappedSuperclass() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Entity level merging details.
     */
    @Override
    public void merge(ORMetadata metadata) {
        super.merge(metadata);
        
        EntityAccessor accessor = (EntityAccessor) metadata;
        
        // Simple object merging.
        m_entityName = (String) mergeSimpleObjects(m_entityName, accessor.getEntityName(), accessor, "@name");
        m_discriminatorValue = (String) mergeSimpleObjects(m_discriminatorValue, accessor.getDiscriminatorValue(), accessor, "<discriminator-value>");
        
        // ORMetadata object merging.
        m_discriminatorColumn = (DiscriminatorColumnMetadata) mergeORObjects(m_discriminatorColumn, accessor.getDiscriminatorColumn());
        m_inheritance = (InheritanceMetadata) mergeORObjects(m_inheritance, accessor.getInheritance());
        m_sequenceGenerator = (SequenceGeneratorMetadata) mergeORObjects(m_sequenceGenerator, accessor.getSequenceGenerator());
        m_tableGenerator = (TableGeneratorMetadata) mergeORObjects(m_tableGenerator, accessor.getTableGenerator());
        m_table = (TableMetadata) mergeORObjects(m_table, accessor.getTable());
        
        // ORMetadata list merging. 
        m_namedQueries = mergeORObjectLists(m_namedQueries, accessor.getNamedQueries());
        m_namedNativeQueries = mergeORObjectLists(m_namedNativeQueries, accessor.getNamedNativeQueries());
        m_namedStoredProcedureQueries = mergeORObjectLists(m_namedStoredProcedureQueries, accessor.getNamedStoredProcedureQueries());
        m_sqlResultSetMappings = mergeORObjectLists(m_sqlResultSetMappings, accessor.getSqlResultSetMappings());
        m_associationOverrides = mergeORObjectLists(m_associationOverrides, accessor.getAssociationOverrides());
        m_attributeOverrides = mergeORObjectLists(m_attributeOverrides, accessor.getAttributeOverrides());
        m_secondaryTables = mergeORObjectLists(m_secondaryTables, accessor.getSecondaryTables());
        m_primaryKeyJoinColumns = mergeORObjectLists(m_primaryKeyJoinColumns, accessor.getPrimaryKeyJoinColumns());
    }
    
    /**
     * INTERNAL:
     * Pre-process the items of interest on an entity class. The order of 
     * processing is important, care must be taken if changes must be made.
     */
    @Override
    public void preProcess() {
        preProcess(true);
    }
    
    /**
     * INTERNAL:
     * Pre-process the items of interest on an entity class. The order of 
     * processing is important, care must be taken if changes must be made.
     */
    public void preProcess(boolean addMappedSuperclassAccessors) {
        setIsPreProcessed();
        
        // If we are not already an inheritance subclass (meaning we were not
        // discovered through a subclass entity discovery) then perform
        // the discovery process before processing any further. We traverse
        // the chain upwards and we can not guarantee which entity will be
        // processed first in an inheritance hierarchy.
        if (! getDescriptor().isInheritanceSubclass()) {
            discoverMappedSuperclassesAndInheritanceParents(addMappedSuperclassAccessors);
        }
        
        // If we are an inheritance subclass process out root first.
        if (getDescriptor().isInheritanceSubclass()) {
            // Ensure our parent accessors are processed first. Top->down.
            // An inheritance subclass can no longer blindly inherit a default
            // access type from the root of the inheritance hierarchy since
            // that root may explicitly set an access type which applies only
            // to itself. The first entity in the hierarchy (going down) that
            // does not specify an explicit type will be used to determine
            // the default access type.
            EntityAccessor parentAccessor = (EntityAccessor) getDescriptor().getInheritanceParentDescriptor().getClassAccessor();
            if (! parentAccessor.isPreProcessed()) {
                parentAccessor.preProcess(addMappedSuperclassAccessors);
            }
        }
        
        // Process the correct access type before any other processing.
        processAccessType();
        
        // Process the entity specifics now, metadata complete, exclude default
        // mappings etc. which we need before proceed further and start looking
        // for annotations.
        processEntity();
        
        // Add any id class definition to the project.
        initIdClass();
        
        // Process a cacheable setting.
        processCacheable();
        
        // For the canonical model generation we do not want to gather the
        // accessors from the mapped superclasses nor pre-process them. They
        // will be pre-processed on their own.
        if (addMappedSuperclassAccessors) {
            // Pre-process our mapped superclass accessors, this will add their
            // accessors and converters and further look for a cacheable setting.
            for (MappedSuperclassAccessor mappedSuperclass : m_mappedSuperclasses) {
                mappedSuperclass.preProcess();
            }
        }
        
        // Finally, add our immediate accessors and converters.
        addAccessors();
        addConverters();
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void preProcessForCanonicalModel() {
        preProcess(false);
    }
    
    /**
     * INTERNAL:
     * Process the items of interest on an entity class. The order of processing 
     * is important, care must be taken if changes must be made.
     */
    @Override
    public void process() {
        setIsProcessed();
        
        // If we are an inheritance subclass process out root first.
        if (getDescriptor().isInheritanceSubclass()) {
            ClassAccessor parentAccessor = getDescriptor().getInheritanceParentDescriptor().getClassAccessor();
            if (! parentAccessor.isProcessed()) {
                parentAccessor.process();
            }
        }
            
        // Process the Table and Inheritance metadata.
        processTableAndInheritance();
        
        // Process the common class level attributes that an entity or mapped 
        // superclass may define. This needs to be done before processing
        // the mapped superclasses. We want to be able to grab the metadata off 
        // the actual entity class first because it needs to override any 
        // settings from the mapped superclass and may need to log a warning.
        processClassMetadata();
        
        // Process the MappedSuperclass(es) metadata now. There may be
        // several MappedSuperclasses for any given Entity.
        for (MappedSuperclassAccessor mappedSuperclass : m_mappedSuperclasses) {
            // All this is going to do is call processClassMetadata.
            mappedSuperclass.process();
        }
        
        // Finally, process the accessors on this entity (and all those from
        // super classes that apply to us).
        processAccessors();
    }
    
    /**
     * INTERNAL:
     * Process the association override metadata specified on an entity or 
     * mapped superclass. Once the association overrides are processed from
     * XML process the association overrides from annotations. This order of
     * processing must be maintained.
     */
    @Override
    protected void processAssociationOverrides() {
        // Process the XML association override elements first.
        for (AssociationOverrideMetadata associationOverride : m_associationOverrides) {
            // Process the association override.
            processAssociationOverride(associationOverride);
        }
        
        // Call the super class to look for annotations.
        super.processAssociationOverrides();
    }
    
    /**
     * INTERNAL:
     * Process the attribute override metadata specified on an entity or 
     * mapped superclass. Once the attribute overrides are processed from
     * XML process the attribute overrides from annotations. This order of 
     * processing must be maintained.
     */
    @Override
    protected void processAttributeOverrides() {
        // Process the XML attribute overrides first.
        for (AttributeOverrideMetadata attributeOverride : m_attributeOverrides) {
            // Process the attribute override.
            processAttributeOverride(attributeOverride);
        }
        
        super.processAttributeOverrides();
    }
    
    /**
     * INTERNAL:
     * Process a caching metadata for this entity accessor logging ignore
     * warnings where necessary.
     */
    @Override
    protected void processCaching() {
        if (getProject().isCacheAll()) {
           if (getDescriptor().isCacheableFalse()) {
               // The persistence unit has an ALL caching type and the user 
               // specified Cacheable(false). Log a warning message that it is 
               // being ignored. If Cacheable(true) was specified then it is 
               // redundant and we just carry on.
               getLogger().logWarningMessage(MetadataLogger.IGNORE_CACHEABLE_FALSE, getJavaClass());
           }
                
           // Process the cache metadata.
           processCachingMetadata();
        } else if (getProject().isCacheNone()) {
            if (getDescriptor().isCacheableTrue()) {
                // The persistence unit has a NONE caching type and the the user
                // specified Cacheable(true). Log a warning message that it is being 
                // ignored. If Cacheable(false) was specified then it is redundant 
                // and we just carry on.
                getLogger().logWarningMessage(MetadataLogger.IGNORE_CACHEABLE_TRUE, getJavaClass());
            }
                
            // Turn off the cache.
            getDescriptor().useNoCache();
        } else if (getProject().isCacheEnableSelective()) {
            if (getDescriptor().isCacheableTrue()) {
                // ENABLE_SELECTIVE and Cacheable(true), process the cache metadata.
                processCachingMetadata();
            } else {
                // ENABLE_SELECTIVE and Cacheable(false) or no setting, turn off the cache.
                getDescriptor().useNoCache();
            }
        } else if (getProject().isCacheDisableSelective()) {
            if (getDescriptor().isCacheableFalse()) {
                // DISABLE_SELECTIVE and Cacheable(false), turn off cache.
                getDescriptor().useNoCache();
            } else {
                // DISABLE_SELECTIVE and Cacheable(true) or no setting, process the cache metadata.
                processCachingMetadata();
            }
        }
    }
    
    /**
     * INTERNAL:
     * Allows for processing DerivedIds.  All referenced accessors are processed
     * first to ensure the necessary fields are set before this derivedId is processed 
     */
    @Override
    public void processDerivedIDs(HashSet<ClassAccessor> processing, HashSet<ClassAccessor> processed) {
        if (hasDerivedId() && !processed.contains(this)){
            super.processDerivedIDs(processing, processed);
            
            // Validate we found a primary key.
            validatePrimaryKey();
                
            // Primary key has been validated, let's process those items that
            // depend on it now.
                
            // Process the SecondaryTable(s) metadata.
            processSecondaryTables();
        }
    }
    
    /**
     * INTERNAL:
     * Process the discriminator column metadata (defaulting is necessary),
     * and return the EclipseLink database field.
     */
    public DatabaseField processDiscriminatorColumn() {
        MetadataAnnotation discriminatorColumn = getAnnotation(DiscriminatorColumn.class);
        
        if (m_discriminatorColumn == null) {
            m_discriminatorColumn = new DiscriminatorColumnMetadata(discriminatorColumn, getAccessibleObject()); 
        } else {
            if (isAnnotationPresent(DiscriminatorColumn.class)) {
                getLogger().logWarningMessage(MetadataLogger.OVERRIDE_ANNOTATION_WITH_XML, discriminatorColumn, getJavaClassName(), getLocation());
            }
        }
        
        return m_discriminatorColumn.process(getDescriptor(), getAnnotatedElementName(), MetadataLogger.INHERITANCE_DISCRIMINATOR_COLUMN);
    }
    
    /**
     * INTERNAL:
     * Process the accessors for the given class.
     * If we are within a TABLE_PER_CLASS inheritance hierarchy, our parent
     * accessors will already have been added at this point. 
     * @see InheritanceMetadata process()
     */
    @Override
    public void processAccessors() {
        // Process our accessors, and then perform the necessary validation
        // checks for this entity.
        super.processAccessors();

        // Validate the optimistic locking setting.
        validateOptimisticLocking();
            
        // Check that we have a simple pk. If it is a derived id, this will be 
        // run in a second pass
        if (! hasDerivedId()){
            // Validate we found a primary key.
            validatePrimaryKey();
                
            // Primary key has been validated, let's process those items that
            // depend on it now.
                
            // Process the SecondaryTable(s) metadata.
            processSecondaryTables();
        }
    }
    
    /**
     * INTERNAL:
     * Figure out the access type for this entity. It works as follows:
     * 1 - check for an explicit access type specification
     * 2 - check our inheritance parents (ignoring explicit specifications)
     * 3 - check our mapped superclasses (ignoring explicit specifications) for 
     *     the location of annotations
     * 4 - check the entity itself for the location of annotations
     * 5 - check for an xml default from a persistence-unit-metadata-defaults or 
     *     entity-mappings setting.    
     * 6 - we have exhausted our search, default to FIELD.
     */
    public void processAccessType() {
        // 266912: this function has been partially overridden in the MappedSuperclassAccessor parent
        // do not call the superclass method in MappedSuperclassAccessor
        // Step 1 - Check for an explicit setting.
        String explicitAccessType = getAccess(); 
        
        // Step 2, regardless if there is an explicit access type we still
        // want to determine the default access type for this entity since
        // any embeddable, mapped superclass or id class that depends on it
        // will have it available. 
        String defaultAccessType = null;
        
        // 1 - Check the access types from our parents if we are an inheritance 
        // sub-class. Inheritance hierarchies are processed top->down so our 
        // first parent that does not define an explicit access type will have 
        // the type we would want to default to.
        if (getDescriptor().isInheritanceSubclass()) {
            MetadataDescriptor parent = getDescriptor().getInheritanceParentDescriptor();
            while (parent != null) {
                if (! parent.getClassAccessor().hasAccess()) {
                    defaultAccessType = parent.getDefaultAccess();
                    break;
                }
                    
                parent = parent.getInheritanceParentDescriptor();
            }
        }
        
        // 2 - If there is no inheritance or no inheritance parent that does not 
        // explicitly define an access type. Let's check this entities mapped 
        // superclasses now.
        if (defaultAccessType == null) {
            for (MappedSuperclassAccessor mappedSuperclass : m_mappedSuperclasses) {
                if (! mappedSuperclass.hasAccess()) {
                    if (havePersistenceFieldAnnotationsDefined(mappedSuperclass.getJavaClass().getFields().values())) {
                        defaultAccessType = MetadataConstants.FIELD;
                    } else if (havePersistenceMethodAnnotationsDefined(mappedSuperclass.getJavaClass().getMethods().values())) {
                        defaultAccessType = MetadataConstants.PROPERTY;
                    }
                        
                    break;
                }
            }
            
            // 3 - If there are no mapped superclasses or no mapped superclasses 
            // without an explicit access type. Check where the annotations are 
            // defined on this entity class. 
            if (defaultAccessType == null) {    
                if (havePersistenceFieldAnnotationsDefined(getJavaClass().getFields().values())) {
                    defaultAccessType = MetadataConstants.FIELD;
                } else if (havePersistenceMethodAnnotationsDefined(getJavaClass().getMethods().values())) {
                    defaultAccessType = MetadataConstants.PROPERTY;
                } else {
                    // 4 - If there are no annotations defined on either the
                    // fields or properties, check for an xml default from
                    // persistence-unit-metadata-defaults or entity-mappings.
                    if (getDescriptor().getDefaultAccess() != null) {
                        defaultAccessType = getDescriptor().getDefaultAccess();
                    } else {
                        // 5 - We've exhausted our search, set the access type
                        // to FIELD.
                        defaultAccessType = MetadataConstants.FIELD;
                    }
                }
            }
        } 
        
        // Finally set the default access type on the descriptor and log a 
        // message to the user if we are defaulting the access type for this
        // entity to use that default.
        getDescriptor().setDefaultAccess(defaultAccessType);
        
        if (explicitAccessType == null) {
            getLogger().logConfigMessage(MetadataLogger.ACCESS_TYPE, defaultAccessType, getJavaClass());
        }
    }
        
    /**
     * INTERNAL:
     * Process the entity metadata.
     */
    protected void processEntity() {
        // Set a metadata complete flag if specified on the entity class or a 
        // mapped superclass.
        if (getMetadataComplete() != null) {
            getDescriptor().setIgnoreAnnotations(isMetadataComplete());
        } else {
            for (MappedSuperclassAccessor mappedSuperclass : m_mappedSuperclasses) {
                if (mappedSuperclass.getMetadataComplete() != null) {
                    getDescriptor().setIgnoreAnnotations(mappedSuperclass.isMetadataComplete());
                    break; // stop searching.
                }
            }
        }
        
        // Set an exclude default mappings flag if specified on the entity class
        // or a mapped superclass.
        if (getExcludeDefaultMappings() != null) {
            getDescriptor().setIgnoreDefaultMappings(excludeDefaultMappings());
        } else {
            for (MappedSuperclassAccessor mappedSuperclass : m_mappedSuperclasses) {
                if (mappedSuperclass.getExcludeDefaultMappings() != null) {
                    getDescriptor().setIgnoreDefaultMappings(mappedSuperclass.excludeDefaultMappings());
                    break; // stop searching.
                }
            }
        }
        
        // Process the entity name (alias) and default if necessary.
        if (m_entityName == null) {
            m_entityName = (getAnnotation(Entity.class) == null) ? "" : (String) getAnnotation(Entity.class).getAttributeString("name");
        }
            
        if (m_entityName.equals("")) {
            m_entityName = Helper.getShortClassName(getJavaClassName());
            getLogger().logConfigMessage(MetadataLogger.ALIAS, getDescriptor(), m_entityName);
        }

        getProject().addAlias(m_entityName, getDescriptor());
    }
    
    /**
     * INTERNAL:
     * Process the Inheritance metadata for a root of an inheritance 
     * hierarchy. One may or may not be specified for the entity class that is 
     * the root of the entity class hierarchy, so we need to default in this 
     * case. This method should only be called on the root of the inheritance 
     * hierarchy.
     */
    protected void processInheritance() {
        // Process the inheritance metadata first. Create one if one does not 
        // exist.
        if (m_inheritance == null) {
            m_inheritance = new InheritanceMetadata(getAnnotation(Inheritance.class), getAccessibleObject());
        }
        
        m_inheritance.process(getDescriptor());
    }
    
    /**
     * INTERNAL:
     * 
     * Process the inheritance metadata for an inheritance subclass. The
     * parent descriptor must be provided.
     */
    public void processInheritancePrimaryKeyJoinColumns() {
        PrimaryKeyJoinColumnsMetadata pkJoinColumns;
        
        if (m_primaryKeyJoinColumns.isEmpty()) {
            // Look for annotations.
            MetadataAnnotation primaryKeyJoinColumn = getAnnotation(PrimaryKeyJoinColumn.class);
            MetadataAnnotation primaryKeyJoinColumns = getAnnotation(PrimaryKeyJoinColumns.class);
                
            pkJoinColumns = new PrimaryKeyJoinColumnsMetadata(primaryKeyJoinColumns, primaryKeyJoinColumn, getAccessibleObject());
        } else {
            // Used what is specified in XML.
            pkJoinColumns = new PrimaryKeyJoinColumnsMetadata(m_primaryKeyJoinColumns);
        }
        
        addMultipleTableKeyFields(pkJoinColumns, getDescriptor().getPrimaryKeyTable(), getDescriptor().getPrimaryTable(), MetadataLogger.INHERITANCE_PK_COLUMN, MetadataLogger.INHERITANCE_FK_COLUMN);
    }
    
    /**
     * INTERNAL:
     * Process the listeners for this entity.
     */
    public void processListeners(ClassLoader loader) {
        // Step 1 - process the default listeners that were defined in XML.
        // Default listeners process the class for additional lifecycle 
        // callback methods that are decorated with annotations.
        // NOTE: We add the default listeners regardless if the exclude default 
        // listeners flag is set. This allows the user to change the exclude 
        // flag at runtime and have the default listeners available to them.
        for (EntityListenerMetadata defaultListener : getProject().getDefaultListeners()) {
            // We need to clone the default listeners. Can't re-use the 
            // same one for all the entities in the persistence unit.
            ((EntityListenerMetadata) defaultListener.clone()).process(getDescriptor(), loader, true);
        }

        // Step 2 - process the entity listeners that are defined on the entity 
        // class and mapped superclasses (taking metadata-complete into 
        // consideration). Go through the mapped superclasses first, top->down 
        // only if the exclude superclass listeners flag is not set.
        discoverMappedSuperclassesAndInheritanceParents(true);
        
        if (! getDescriptor().excludeSuperclassListeners()) {
            int mappedSuperclassesSize = m_mappedSuperclasses.size();
            
            for (int i = mappedSuperclassesSize - 1; i >= 0; i--) {
                m_mappedSuperclasses.get(i).processEntityListeners(loader);
            }
        }
        
        processEntityListeners(loader); 
        
        // Step 3 - process the entity class for lifecycle callback methods. Go
        // through the mapped superclasses as well.
        new EntityClassListenerMetadata(this).process(m_mappedSuperclasses, loader);
    }
    
    /**
     * INTERNAL:
     * Process/collect the named native queries on this accessor and add them
     * to the project for later processing.
     */
    @Override
    protected void processNamedNativeQueries() {
        // Process the XML named native queries first.
        for (NamedNativeQueryMetadata namedNativeQuery : m_namedNativeQueries) {
            getProject().addQuery(namedNativeQuery);
        }
        
        // Process the named native query annotations second.
        super.processNamedNativeQueries();
    }
    
    /**
     * INTERNAL:
     * Process/collect the named queries on this accessor and add them to the 
     * project for later processing.
     */
    @Override
    protected void processNamedQueries() {
        // Process the XML named queries first.
        for (NamedQueryMetadata namedQuery : m_namedQueries) {
            getProject().addQuery(namedQuery);
        }
        
        // Process the named query annotations second.
        super.processNamedQueries();
    }
    
    /**
     * INTERNAL:
     * Process/collect the named stored procedure queries on this accessor and 
     * add them to the project for later processing.
     */
    @Override
    protected void processNamedStoredProcedureQueries() {
        // Process the XML named queries first.
        for (NamedStoredProcedureQueryMetadata namedStoredProcedureQuery : m_namedStoredProcedureQueries) {
            getProject().addQuery(namedStoredProcedureQuery);
        }
        
        // Process the named stored procedure query annotations second.
        super.processNamedStoredProcedureQueries();
    }
    
    /**
     * INTERNAL:
     * Process a MetadataSecondaryTable. Do all the table name defaulting and 
     * set the correct, fully qualified name on the TopLink DatabaseTable.
     */
    protected void processSecondaryTable(SecondaryTableMetadata secondaryTable) {
        // Process any table defaults and log warning messages.
        processTable(secondaryTable, secondaryTable.getName());
        
        // Add the table to the descriptor.
        getDescriptor().addTable(secondaryTable.getDatabaseTable());
        
        // Get the primary key join column(s) and add the multiple table key fields.
        addMultipleTableKeyFields(new PrimaryKeyJoinColumnsMetadata(secondaryTable.getPrimaryKeyJoinColumns()), getDescriptor().getPrimaryTable(), secondaryTable.getDatabaseTable(), MetadataLogger.SECONDARY_TABLE_PK_COLUMN, MetadataLogger.SECONDARY_TABLE_FK_COLUMN);
    }
    
    /**
     * INTERNAL:
     * Process secondary-table(s) for a given entity.
     */
    protected void processSecondaryTables() {
        MetadataAnnotation secondaryTable = getAnnotation(SecondaryTable.class);
        MetadataAnnotation secondaryTables = getAnnotation(SecondaryTables.class);
        
        if (m_secondaryTables.isEmpty()) {
            // Look for a SecondaryTables annotation.
            if (secondaryTables != null) {
                for (Object table : (Object[]) secondaryTables.getAttributeArray("value")) { 
                    processSecondaryTable(new SecondaryTableMetadata((MetadataAnnotation)table, getAccessibleObject()));
                }
            } else {
                // Look for a SecondaryTable annotation
                if (secondaryTable != null) {    
                    processSecondaryTable(new SecondaryTableMetadata(secondaryTable, getAccessibleObject()));
                }
            }
        } else {
            if (secondaryTable != null) {
                getLogger().logWarningMessage(MetadataLogger.OVERRIDE_ANNOTATION_WITH_XML, secondaryTable, getJavaClassName(), getLocation());
            }
            
            if (secondaryTables != null) {
                getLogger().logWarningMessage(MetadataLogger.OVERRIDE_ANNOTATION_WITH_XML, secondaryTables, getJavaClassName(), getLocation());
            }
            
            for (SecondaryTableMetadata table : m_secondaryTables) {
                processSecondaryTable(table);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Process the sequence generator metadata and add it to the project. 
     */
    @Override
    protected void processSequenceGenerator() {
        // Process the xml defined sequence generator first.
        if (m_sequenceGenerator != null) {
            getProject().addSequenceGenerator(m_sequenceGenerator, getDescriptor().getDefaultCatalog(), getDescriptor().getDefaultSchema());
        }
        
        // Process the annotation defined sequence generator second.
        super.processSequenceGenerator();
    }
    
    /**
     * INTERNAL:
     * Process the sql result set mappings for the given class which could be 
     * an entity or a mapped superclass.
     */
    @Override
    protected void processSqlResultSetMappings() {
        // Process the XML sql result set mapping elements first.
        for (SQLResultSetMappingMetadata sqlResultSetMapping : m_sqlResultSetMappings) {
            getProject().addSQLResultSetMapping(sqlResultSetMapping);
        }
        
        // Process the sql result set mapping query annotations second.
        super.processSqlResultSetMappings();
    }
    
    /**
     * INTERNAL:
     * Process table information for the given metadata descriptor.
     */
    protected void processTable() {
        MetadataAnnotation table = getAnnotation(Table.class);
        
        if (m_table == null) {
            // Check for a table annotation. If no annotation is defined, the 
            // table will default.
            processTable(new TableMetadata(table, getAccessibleObject()));
        } else {
            if (table != null) {
                getLogger().logWarningMessage(MetadataLogger.OVERRIDE_ANNOTATION_WITH_XML, table, getJavaClassName(), getLocation());
            }

            processTable(m_table);                
        }
    }
    
    /**
     * INTERNAL:
     * Process a MetadataTable. Do all the table name defaulting and set the
     * correct, fully qualified name on the TopLink DatabaseTable.
     */
    protected void processTable(TableMetadata table) {
        // Process any table defaults and log warning messages.
        processTable(table, getDescriptor().getDefaultTableName());
        
        // Set the table on the descriptor.
        getDescriptor().setPrimaryTable(table.getDatabaseTable());
    }
    
    /**
     * INTERNAL:
     * Process any inheritance specifics. NOTE: Inheritance hierarchies are
     * always processed from the top down so it is safe to assume that all
     * our parents have already been processed fully. The only exception being
     * when a root accessor doesn't know they are a root (defaulting case). In
     * this case we'll tell the root accessor to process the inheritance 
     * metadata befor continuing with our own processing.
     */
    protected void processTableAndInheritance() {
        // If we are an inheritance subclass, ensure our root is processed 
        // first since it has information its subclasses depend on.
        if (getDescriptor().isInheritanceSubclass()) {
            MetadataDescriptor rootDescriptor = getDescriptor().getInheritanceRootDescriptor();
            EntityAccessor rootAccessor = (EntityAccessor) rootDescriptor.getClassAccessor();
            
            // An entity who didn't know they were the root of an inheritance 
            // hierarchy (that is, does not define inheritance metadata) must 
            // process and default the inheritance metadata.
            if (! rootAccessor.hasInheritance()) {    
                rootAccessor.processInheritance();
            }
            
            // Process the table metadata for this descriptor if either there
            // is an inheritance specification (we're a new root) or if we are
            // part of a joined or table per class inheritance strategy.
            if (hasInheritance() || ! rootDescriptor.usesSingleTableInheritanceStrategy()) {
                processTable();
            }
            
            // If this entity has inheritance metadata as well, then the 
            // inheritance strategy is mixed and we need to process the 
            // inheritance metadata to ensure this entity's subclasses process 
            // correctly.
            if (hasInheritance()) {
                // Process the inheritance metadata.
                processInheritance();    
            } else {                
                // Process the inheritance details using the inheritance 
                // metadata from our parent. This will put the right 
                // inheritance policy on our descriptor.
                rootAccessor.getInheritance().process(getDescriptor());
            }
        } else {
            // Process the table metadata if there is some, otherwise default.
            processTable();
            
            // If we have inheritance metadata, then process it now. If we are 
            // an inheritance root class that doesn't know it, a subclass will 
            // force this processing to occur.
            if (hasInheritance()) {
                processInheritance();
            }
        }
    }
    
    /**
     * INTERNAL:
     * Process the table generator metadata and add it to the project. 
     */
    @Override
    protected void processTableGenerator() {
        // Process the xml defined table generator first.
        if (m_tableGenerator != null) {
            getProject().addTableGenerator(m_tableGenerator, getDescriptor().getDefaultCatalog(), getDescriptor().getDefaultSchema());
        }
        
        // Process the annotation defined table generator second.
        super.processTableGenerator();
    } 
    
    /**
     * INTERNAL:
     * This method resolves generic types. Resolving generic types will be
     * the responsibility of the metadata factory since each factory could have 
     * its own means to do so and not respect a generic format on the metadata
     * objects.
     */
    protected void resolveGenericTypes(List<String> genericTypes, MetadataClass parent) {
        getMetadataFactory().resolveGenericTypes(getJavaClass(), genericTypes, parent, getDescriptor());
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setAssociationOverrides(List<AssociationOverrideMetadata> associationOverrides) {
        m_associationOverrides = associationOverrides;
    } 
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setAttributeOverrides(List<AttributeOverrideMetadata> attributeOverrides) {
        m_attributeOverrides = attributeOverrides;
    } 
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setDiscriminatorColumn(DiscriminatorColumnMetadata discriminatorColumn) {
        m_discriminatorColumn = discriminatorColumn;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setDiscriminatorValue(String discriminatorValue) {
        m_discriminatorValue = discriminatorValue;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setEntityName(String entityName) {
        m_entityName = entityName;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setInheritance(InheritanceMetadata inheritance) {
        m_inheritance = inheritance;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public void setPrimaryKeyJoinColumns(List<PrimaryKeyJoinColumnMetadata> primaryKeyJoinColumns) {
        m_primaryKeyJoinColumns = primaryKeyJoinColumns;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setNamedNativeQueries(List<NamedNativeQueryMetadata> namedNativeQueries) {
        m_namedNativeQueries = namedNativeQueries;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setNamedQueries(List<NamedQueryMetadata> namedQueries) {
        m_namedQueries = namedQueries;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setNamedStoredProcedureQueries(List<NamedStoredProcedureQueryMetadata> namedStoredProcedureQueries) {
        m_namedStoredProcedureQueries = namedStoredProcedureQueries;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setSecondaryTables(List<SecondaryTableMetadata> secondaryTables) {
        m_secondaryTables = secondaryTables;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setSequenceGenerator(SequenceGeneratorMetadata sequenceGenerator) {
        m_sequenceGenerator = sequenceGenerator;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setSqlResultSetMappings(List<SQLResultSetMappingMetadata> sqlResultSetMappings) {
        m_sqlResultSetMappings = sqlResultSetMappings;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setTable(TableMetadata table) {
        m_table = table;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setTableGenerator(TableGeneratorMetadata tableGenerator) {
        m_tableGenerator = tableGenerator;
    }
    
    /**
     * INTERNAL: 
     * Validate a OptimisticLocking(type=VERSION_COLUMN) setting. That is,
     * validate that we found a version field. If not, throw an exception.
     */
    protected void validateOptimisticLocking() {
        if (getDescriptor().usesVersionColumnOptimisticLocking() && ! getDescriptor().usesOptimisticLocking()) {
            throw ValidationException.optimisticLockingVersionElementNotSpecified(getJavaClass());
        }
    }
    
    /**
     * INTERNAL:
     * 
     * Call this method after a primary key should have been found.
     */
    protected void validatePrimaryKey() {
        // If this descriptor has a composite primary key, check that all 
        // our composite primary key attributes were validated. 
        if (getDescriptor().hasCompositePrimaryKey()) {
            if (getDescriptor().pkClassWasNotValidated()) {
                throw ValidationException.invalidCompositePKSpecification(getJavaClass(), getDescriptor().getPKClassName());
            }
        } else {
            // Descriptor has a single primary key. Validate an id 
            // attribute was found, unless we are an inheritance subclass
            // or an aggregate descriptor.
            if (! getDescriptor().hasPrimaryKeyFields() && ! getDescriptor().isInheritanceSubclass()) {
                throw ValidationException.noPrimaryKeyAnnotationsFound(getJavaClass());
            }
        }  
    }
}
