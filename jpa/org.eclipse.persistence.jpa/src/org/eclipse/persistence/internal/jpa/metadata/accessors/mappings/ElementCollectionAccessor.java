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
 *     01/28/2009-1.1 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 1) 
 ******************************************************************************/ 
package org.eclipse.persistence.internal.jpa.metadata.accessors.mappings;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.MapKey;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;

import org.eclipse.persistence.annotations.ConvertKey;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EmbeddableAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.columns.AssociationOverrideMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.AttributeOverrideMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.ColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.JoinColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.tables.CollectionTableMetadata;
import org.eclipse.persistence.mappings.AggregateCollectionMapping;
import org.eclipse.persistence.mappings.AggregateObjectMapping;
import org.eclipse.persistence.mappings.CollectionMapping;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.DirectCollectionMapping;
import org.eclipse.persistence.mappings.DirectToFieldMapping;
import org.eclipse.persistence.mappings.EmbeddableMapping;
import org.eclipse.persistence.mappings.OneToOneMapping;

/**
 * An element collection accessor.
 * 
 * Used to support DirectCollection, DirectMap, AggregateCollection.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 2.0
 */
public class ElementCollectionAccessor extends DirectCollectionAccessor {
    private Class m_targetClass;
    private Class m_mapKeyClass; // TODO: mapped but not processed.
    private Class m_referenceClass;
    
    private ColumnMetadata m_column;
    private ColumnMetadata m_mapKeyColumn;
    private ColumnMetadata m_orderColumn; // TODO: mapped but not processed.
    private CollectionTableMetadata m_collectionTable;
    
    private List<AssociationOverrideMetadata> m_associationOverrides;
    private List<AttributeOverrideMetadata> m_attributeOverrides;
    private List<JoinColumnMetadata> m_mapKeyJoinColumns; // TODO: mapped but not processed. 
    
    private String m_convertKey;
    private String m_mapKey; // TODO: mapped but not processed.
    private String m_mapKeyClassName; // TODO: mapped but not processed. (See CollectionAccessor)
    private String m_orderBy;
    private String m_targetClassName;
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public ElementCollectionAccessor() {
        super("<element-collection>");
    }
    
    /**
     * INTERNAL:
     */
    public ElementCollectionAccessor(Annotation elementCollection, MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
        super(elementCollection, accessibleObject, classAccessor);
        
        // Set the target class.
        m_targetClass = (Class) MetadataHelper.invokeMethod("targetClass", elementCollection);
        
        // Set the attribute overrides if some are present.
        m_attributeOverrides = new ArrayList<AttributeOverrideMetadata>();
        
        // Set the attribute overrides first if defined.
        if (isAnnotationPresent(AttributeOverrides.class)) {
            for (Annotation attributeOverride : (Annotation[]) MetadataHelper.invokeMethod("value", getAnnotation(AttributeOverrides.class))) {
                m_attributeOverrides.add(new AttributeOverrideMetadata(attributeOverride, accessibleObject));
            }
        }
        
        // Set the single attribute override second if defined.
        if (isAnnotationPresent(AttributeOverride.class)) {
            m_attributeOverrides.add(new AttributeOverrideMetadata(getAnnotation(AttributeOverride.class), accessibleObject));
        }
        
        // Set the association overrides if some are present.
        m_associationOverrides = new ArrayList<AssociationOverrideMetadata>();
        
        if (isAnnotationPresent(AssociationOverrides.class)) {
            for (Annotation associationOverride : (Annotation[]) MetadataHelper.invokeMethod("value", getAnnotation(AssociationOverrides.class))) {
                m_associationOverrides.add(new AssociationOverrideMetadata(associationOverride, accessibleObject));
            }
        }
        
        // Set the single association override second if defined.
        if (isAnnotationPresent(AssociationOverride.class)) {
            m_associationOverrides.add(new AssociationOverrideMetadata(getAnnotation(AssociationOverride.class), accessibleObject));
        }
        
        // Set the column if one if defined.
        if (isAnnotationPresent(Column.class)) {
            m_column = new ColumnMetadata(getAnnotation(Column.class), accessibleObject, getAttributeName());
        }
        
        // Set the collection table if one is defined.
        if (isAnnotationPresent(CollectionTable.class)) {
            m_collectionTable = new CollectionTableMetadata(getAnnotation(CollectionTable.class), accessibleObject, true);
        }
        
        // Set the order if one is present.
        if (isAnnotationPresent(OrderBy.class)) {
            m_orderBy = (String) MetadataHelper.invokeMethod("value", getAnnotation(OrderBy.class));
        }
        
        // Set the map key if one is defined.
        if (isAnnotationPresent(MapKey.class)) {
            m_mapKey = (String) MetadataHelper.invokeMethod("name", getAnnotation(MapKey.class));
        }
        
        // Set the map key class if one is defined.
        if (isAnnotationPresent(MapKeyClass.class)) {
            m_mapKeyClass = (Class) MetadataHelper.invokeMethod("value", getAnnotation(MapKeyClass.class));
        }
        
        // Set the map key column if one is defined.
        if (isAnnotationPresent(MapKeyColumn.class)) {
            m_mapKeyColumn = new ColumnMetadata(getAnnotation(MapKeyColumn.class), accessibleObject, getAttributeName());
        }
        
        // Set the convert key if one is defined.
        if (isAnnotationPresent(ConvertKey.class)) {
            m_convertKey = (String) MetadataHelper.invokeMethod("name", getAnnotation(ConvertKey.class));
        }
        
        // Set the order column if one is defined.
        if (isAnnotationPresent(OrderColumn.class)) {
            m_orderColumn = new ColumnMetadata(getAnnotation(OrderColumn.class), accessibleObject, getAttributeName());
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
    public CollectionTableMetadata getCollectionTable() {
        return m_collectionTable;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public ColumnMetadata getColumn() {
        return m_column;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    protected ColumnMetadata getColumn(String loggingCtx) {
        if (loggingCtx.equals(MetadataLogger.VALUE_COLUMN)) {
            if (m_column == null) {
                // TODO: Log defaulting message.
                m_column = new ColumnMetadata(getAccessibleObject(), getAttributeName());  
            }
            
            return m_column;
        } else {
            // TODO: This is where we need to look at the map key column, map 
            // key class and map key. Map key I guess would have to look up a 
            // mapping by attribute name. Map key class would have to set 
            // different API likely.
            // For now ...
            return m_mapKeyColumn;
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getConvertKey() {
        return m_convertKey;
    }

    /**
     * INTERNAL:
     */
    public EmbeddableAccessor getEmbeddableAccessor() {
        return getProject().getEmbeddableAccessor(getReferenceClass());
    }
    
    /**
     * INTERNAL:
     */
    @Override
    protected String getKeyConverter() {
        return m_convertKey;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public String getMapKey() {
        return m_mapKey;
    }
    
    /**
     * INTERNAL: 
     * TODO: Do we need this method?
     */
    public Class getMapKeyClass() {
        return m_mapKeyClass;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public String getMapKeyClassName() {
        return m_mapKeyClassName;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public ColumnMetadata getMapKeyColumn() {
        return m_mapKeyColumn;
    } 
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public List<JoinColumnMetadata> getMapKeyJoinColumns() {
        return m_mapKeyJoinColumns;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getOrderBy() {
        return m_orderBy; 
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public ColumnMetadata getOrderColumn() {
        return m_orderColumn;
    }
    
    /**
     * INTERNAL:
     * If a targetEntity is specified in metadata, it will be set as the 
     * reference class, otherwise we will look to extract one from generics.
     */
    @Override
    public Class getReferenceClass() {
        if (m_referenceClass == null) {
            m_referenceClass = getTargetClass();
        
            if (m_referenceClass == void.class) {
                // This call will attempt to extract the reference class from generics.
                m_referenceClass = getReferenceClassFromGeneric();
        
                if (m_referenceClass == null) {
                    // Throw an exception. An element collection accessor must 
                    // have a reference class either through generics or a 
                    // specified target class on the mapping metadata.
                    throw ValidationException.unableToDetermineTargetClass(getAttributeName(), getJavaClass());
                } else {
                    // Log the defaulting contextual reference class.
                    getLogger().logConfigMessage(getLogger().ELEMENT_COLLECTION_MAPPING_REFERENCE_CLASS, getAnnotatedElement(), m_referenceClass);
                }
            } 
        }
        
        return m_referenceClass;
    }
    
    /**
     * INTERNAL:
     * In an element collection case, when the collection is not an embeddable
     * collection, there is no knowledge of a reference descriptor and the join 
     * columns should be defaulted based on the owner of the element collection.
     */
    @Override
    public MetadataDescriptor getReferenceDescriptor() {
        if (isDirectEmbeddableCollection()) {
            return getEmbeddableAccessor().getDescriptor();
        } else {
            return getDescriptor();
        }
    }
    
    /**
     * INTERNAL:
     * Return the target class for this accessor.
     */
    protected Class getTargetClass() {
        return m_targetClass;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    protected String getTargetClassName() {
        return m_targetClassName;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject) {
        super.initXMLObject(accessibleObject);
        
        // Initialize lists of ORMetadata objects.
        initXMLObjects(m_attributeOverrides, accessibleObject);
        initXMLObjects(m_associationOverrides, accessibleObject);
        
        // Initialize single objects.
        initXMLObject(m_column, accessibleObject);
        initXMLObject(m_mapKeyColumn, accessibleObject);
        initXMLObject(m_orderColumn, accessibleObject);
        initXMLObject(m_collectionTable, accessibleObject);
        
        // Initialize the any class names we read from XML.
        m_targetClass = initXMLClassName(m_targetClassName);
        m_mapKeyClass = initXMLClassName(m_mapKeyClassName);
    }
    
    /**
     * INTERNAL:
     * Return true if this element collection contains embeddable objects.
     */
    @Override
    public boolean isDirectEmbeddableCollection() {
        return getEmbeddableAccessor() != null;
    }
    
    /**
     * INTERNAL: 
     */
    @Override
    public void process() {
        if (isDirectEmbeddableCollection()) { 
            processDirectEmbeddableCollectionMapping();
        } else if (isValidDirectCollectionType()) {
            processDirectCollectionMapping();
        } else if (isValidDirectMapType()) {
            processDirectMapMapping();
        } else {
            throw ValidationException.invalidTargetClass(getAttributeName(), getJavaClass());
        }
    }
    
    /**
     * INTERNAL:
     * Process the list of association overrides into a map, merging and 
     * overriding any attribute overrides where necessary.
     * TODO: This code should look for duplicates??
     */
    protected Map<String, AssociationOverrideMetadata> processAssociationOverrides() {
        HashMap<String, AssociationOverrideMetadata> associationOverrides = new HashMap<String, AssociationOverrideMetadata>();
        
        for (AssociationOverrideMetadata associationOverride : m_associationOverrides) {
            String name = associationOverride.getName();
            
            if (getClassAccessor().isMappedSuperclass() && getDescriptor().hasAssociationOverrideFor(name)) {
                // TODO: Log an override message.
                associationOverrides.put(name, getDescriptor().getAssociationOverrideFor(name));
            } else {
                associationOverrides.put(name, associationOverride);
            }
        }
        
        // Now add every other descriptor association override that didn't 
        // override a mapping level one (if we are processing a mapping from
        // a mapped superclass level)
        if (getClassAccessor().isMappedSuperclass()) {
            for (AssociationOverrideMetadata associationOverride : getDescriptor().getAssociationOverrides()) {
                String name = associationOverride.getName();
                
                if (! associationOverrides.containsKey(name)) {
                    associationOverrides.put(name, associationOverride);
                }
            }
        }
        
        return associationOverrides;
    }
    
    /**
     * INTERNAL:
     * Process the list of attribute overrides into a map, merging and 
     * overriding any attribute overrides where necessary.
     * TODO: This code should look for duplicates??
     */
    protected Map<String, AttributeOverrideMetadata> processAttributeOverrides() {
        HashMap<String, AttributeOverrideMetadata> attributeOverrides = new HashMap<String, AttributeOverrideMetadata>();
        
        for (AttributeOverrideMetadata attributeOverride : m_attributeOverrides) {
            String name = attributeOverride.getName();
            
            if (getClassAccessor().isMappedSuperclass() && getDescriptor().hasAttributeOverrideFor(name)) {
                // TODO: Log an override message
                attributeOverrides.put(name, getDescriptor().getAttributeOverrideFor(name));
            } else {
                attributeOverrides.put(name, attributeOverride);
            }
        }
        
        // Now add every other descriptor attribute override that didn't 
        // override a mapping level one (if we are processing a mapping from
        // a mapped superclass level)
        if (getClassAccessor().isMappedSuperclass()) {
            for (AttributeOverrideMetadata attributeOverride : getDescriptor().getAttributeOverrides()) {
                String name = attributeOverride.getName();
                
                if (! attributeOverrides.containsKey(name)) {
                    attributeOverrides.put(name, attributeOverride);
                }
            }
        }
        
        return attributeOverrides;
    }
    
    /**
     * INTERNAL:
     * Process a MetadataCollectionTable.
     */
    @Override
    protected void processCollectionTable(CollectionMapping mapping) {
        // Check that we loaded a collection table otherwise default one.        
        if (m_collectionTable == null) {
            // TODO: Log a defaulting message.
            m_collectionTable = new CollectionTableMetadata(getAccessibleObject());
        }
        
        // Process any table defaults and log warning messages.
        processTable(m_collectionTable, getDefaultCollectionTableName());
        
        // Set the reference table on the mapping (only in a direct collection
        // case). For an embeddable collection, the table will be set on the
        // fields.
        if (! isDirectEmbeddableCollection()) {
            ((DirectCollectionMapping) mapping).setReferenceTable(m_collectionTable.getDatabaseTable());
        }
        
        // Add all the joinColumns (reference key fields) to the mapping. Join 
        // column validation is performed in the processJoinColumns call.
        for (JoinColumnMetadata joinColumn : processJoinColumns(m_collectionTable.getJoinColumns())) {
            // The default name is the primary key of the owning entity.
            DatabaseField pkField = joinColumn.getPrimaryKeyField();
            pkField.setName(getName(pkField, getDescriptor().getPrimaryKeyFieldName(), MetadataLogger.PK_COLUMN));
            pkField.setTable(getDescriptor().getPrimaryTable());
                
            // The default name is the primary key of the owning entity.
            DatabaseField fkField = joinColumn.getForeignKeyField();
            fkField.setName(getName(fkField, getDescriptor().getAlias() + "_" + getDescriptor().getPrimaryKeyFieldName(), MetadataLogger.FK_COLUMN));
            fkField.setTable(m_collectionTable.getDatabaseTable());
                
            if (mapping.isDirectCollectionMapping()) {
                // Add the reference key field for the direct collection mapping.
                ((DirectCollectionMapping) mapping).addReferenceKeyField(fkField, pkField);
            } else {
                ((AggregateCollectionMapping) mapping).addTargetForeignKeyField(fkField, pkField);
            }
        }
    }
    
    /**
     * INTERNAL:
     */
    protected void processDirectEmbeddableCollectionMapping() {
        // Get the embeddable accessor from the project.
        EmbeddableAccessor embeddableAccessor = getEmbeddableAccessor();
        
        // Initialize our mapping.
        AggregateCollectionMapping embeddableMapping = new AggregateCollectionMapping();
        
        // Make sure to mark the descriptor as an embeddable collection descriptor.
        embeddableAccessor.getDescriptor().setIsEmbeddableCollection();
        
        // Process common direct collection metadata. This must be done 
        // before any field processing since field processing requires that 
        // the collection table be processed before hand.
        process(embeddableMapping);

        // Set the reference class name.
        embeddableMapping.setReferenceClassName(getReferenceClassName());
        
        // Process the fetch type and set the correct indirection on the mapping.
        setIndirectionPolicy(embeddableMapping, null, usesIndirection());
        
        // Process the mappings from the embeddable to setup the field name 
        // translations. Before we do that lets process the attribute and
        // association overrides that are available to us and that may be used
        // to override any field name translations.
        processMappingsFromEmbeddable(embeddableAccessor, embeddableMapping, processAttributeOverrides(), processAssociationOverrides(), "");
        
        // TODO: if it is an aggregate map, then we need to set the key field
        // and process a converter for it. Value converter would not apply.
        
        // Process properties.
        processProperties(embeddableMapping);
    }
    
    /**
     * INTERNAL:
     */
    protected void processMappingsFromEmbeddable(EmbeddableAccessor embeddableAccessor, EmbeddableMapping embeddableMapping, Map<String, AttributeOverrideMetadata> attributeOverrides, Map<String, AssociationOverrideMetadata> associationOverrides, String dotNotation) {        
        for (DatabaseMapping mapping : (List<DatabaseMapping>) embeddableAccessor.getDescriptor().getMappings()) {
            String overrideName = dotNotation + mapping.getAttributeName();
            
            if (mapping.isDirectToFieldMapping()) {
                AttributeOverrideMetadata attributeOverride = attributeOverrides.get(overrideName);
                
                if (attributeOverride == null) {
                    DatabaseField directFieldClone = (DatabaseField) ((DirectToFieldMapping) mapping).getField().clone();
                    addFieldNameTranslation(embeddableMapping, directFieldClone, m_collectionTable.getDatabaseTable(), mapping);
                } else {
                    addFieldNameTranslation(embeddableMapping, attributeOverride.getOverrideField(), m_collectionTable.getDatabaseTable(), mapping);
                }
            } else if (mapping.isOneToOneMapping()) {
                if (((OneToOneMapping) mapping).isForeignKeyRelationship()) {
                    AssociationOverrideMetadata associationOverride = associationOverrides.get(overrideName);
                    
                    if (associationOverride == null) {
                        for (DatabaseField fkField : ((OneToOneMapping) mapping).getForeignKeyFields()) {
                            DatabaseField collectionTableField = (DatabaseField) fkField.clone();
                            collectionTableField.setTable(m_collectionTable.getDatabaseTable());
                            embeddableMapping.addFieldNameTranslation(collectionTableField.getQualifiedName(), fkField.getName());
                        }
                    } else {
                        processAssociationOverride(associationOverride, embeddableMapping, m_collectionTable.getDatabaseTable(), embeddableAccessor.getDescriptor());
                    }
                } else {
                    // Section 2.6 of the spec states: "An embeddable class (including an embeddable class within 
                    // another embeddable class) contained within an element collection must not contain an element 
                    // collection, nor may it contain a relationship to an entity other than a many-to-one or 
                    // one-to-one relationship. The embeddable class must be on the owning side of such a 
                    // relationship and the relationship must be mapped by a foreign key mapping."
                    throw ValidationException.invalidEmbeddableClassForElementCollection(embeddableAccessor.getJavaClass(), getAttributeName(), getJavaClass(), mapping.getAttributeName());
                }
            } else if (mapping.isAggregateObjectMapping()) {
                EmbeddableAccessor nestedEmbeddableAccessor = getProject().getEmbeddableAccessor(((AggregateObjectMapping) mapping).getReferenceClass());
                nestedEmbeddableAccessor.getDescriptor().setIsEmbeddableCollection();
                processMappingsFromEmbeddable(nestedEmbeddableAccessor, embeddableMapping, attributeOverrides, associationOverrides, overrideName + ".");
            } else {
                // See comment above about section 2.6
                throw ValidationException.invalidEmbeddableClassForElementCollection(embeddableAccessor.getJavaClass(), getAttributeName(), getJavaClass(), mapping.getAttributeName());
            }
        }
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
    public void setCollectionTable(CollectionTableMetadata collectionTable) {
        m_collectionTable = collectionTable;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public void setColumn(ColumnMetadata column) {
        m_column = column;
    }

    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setConvertKey(String convertKey) {
        m_convertKey = convertKey;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public String setMapKey(String mapKey) {
        return m_mapKey;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public void setMapKeyClassName(String mapKeyClassName) {
        m_mapKeyClassName = mapKeyClassName;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public void setMapKeyColumn(ColumnMetadata mapKeyColumn) {
        m_mapKeyColumn = mapKeyColumn;
    } 
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public void setMapKeyJoinColumns(List<JoinColumnMetadata> mapKeyJoinColumns) {
        m_mapKeyJoinColumns = mapKeyJoinColumns;
    }

    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setOrderBy(String orderBy) {
        m_orderBy = orderBy;
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public void setOrderColumn(ColumnMetadata orderColumn) {
        m_orderColumn = orderColumn;
    }

    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    public void setTargetClassName(String targetClassName) {
        m_targetClassName = targetClassName;
    }
}
