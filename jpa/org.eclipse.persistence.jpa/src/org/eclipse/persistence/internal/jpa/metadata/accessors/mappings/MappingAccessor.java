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
 *     06/20/2008-1.0 Guy Pelletier 
 *       - 232975: Failure when attribute type is generic
 *     08/27/2008-1.1 Guy Pelletier 
 *       - 211329: Add sequencing on non-id attribute(s) support to the EclipseLink-ORM.XML Schema
 *     09/23/2008-1.1 Guy Pelletier 
 *       - 241651: JPA 2.0 Access Type support
 *     01/28/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 1)
 *     02/06/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 2)
 *     02/25/2009-2.0 Guy Pelletier 
 *       - 265359: JPA 2.0 Element Collections - Metadata processing portions
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     04/03/2009-2.0 Guy Pelletier
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     04/24/2009-2.0 Guy Pelletier 
 *       - 270011: JPA 2.0 MappedById support
 *     06/02/2009-2.0 Guy Pelletier 
 *       - 278768: JPA 2.0 Association Override Join Table
 *     06/25/2009-2.0 Michael O'Brien 
 *       - 266912: change MappedSuperclass handling in stage2 to pre process accessors
 *          in support of the custom descriptors holding mappings required by the Metamodel. 
 *          We handle undefined parameterized generic types for a MappedSuperclass defined
 *          Map field by returning Void in this case.
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metadata.accessors.mappings;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.FetchType;

import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.JoinFetchType;
import org.eclipse.persistence.annotations.Properties;
import org.eclipse.persistence.annotations.Property;
import org.eclipse.persistence.annotations.ReturnInsert;
import org.eclipse.persistence.annotations.ReturnUpdate;
import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.indirection.TransparentIndirectionPolicy;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataHelper;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.accessors.AccessMethodsMetadata;
import org.eclipse.persistence.internal.jpa.metadata.accessors.MetadataAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.PropertyMetadata;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EmbeddableAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EntityAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataClass;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataMethod;
import org.eclipse.persistence.internal.jpa.metadata.columns.AssociationOverrideMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.AttributeOverrideMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.ColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.JoinColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.converters.AbstractConverterMetadata;
import org.eclipse.persistence.internal.jpa.metadata.converters.ClassInstanceMetadata;
import org.eclipse.persistence.internal.jpa.metadata.converters.EnumeratedMetadata;
import org.eclipse.persistence.internal.jpa.metadata.converters.LobMetadata;
import org.eclipse.persistence.internal.jpa.metadata.converters.SerializedMetadata;
import org.eclipse.persistence.internal.jpa.metadata.converters.TemporalMetadata;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;
import org.eclipse.persistence.internal.queries.CollectionContainerPolicy;
import org.eclipse.persistence.internal.queries.MappedKeyMapContainerPolicy;

import org.eclipse.persistence.mappings.AggregateObjectMapping;
import org.eclipse.persistence.mappings.CollectionMapping;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.DirectMapMapping;
import org.eclipse.persistence.mappings.DirectToFieldMapping;
import org.eclipse.persistence.mappings.EmbeddableMapping;
import org.eclipse.persistence.mappings.ForeignReferenceMapping;
import org.eclipse.persistence.mappings.OneToOneMapping;
import org.eclipse.persistence.mappings.foundation.MapComponentMapping;
import org.eclipse.persistence.mappings.foundation.MapKeyMapping;

/**
 * INTERNAL:
 * An abstract mapping accessor. Holds common metadata for all mappings.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 1.0
 */
public abstract class MappingAccessor extends MetadataAccessor {
    // Reserved converter names
    private static final String CONVERT_NONE = "none";
    private static final String CONVERT_SERIALIZED = "serialized";
    private static final String CONVERT_CLASS_INSTANCE = "class-instance";

    // Used for looking up attribute overrides for a map accessor. 
    protected static final String KEY_DOT_NOTATION = "key.";
    protected static final String VALUE_DOT_NOTATION = "value.";

    private final static String DEFAULT_MAP_KEY_COLUMN_SUFFIX = "_KEY";

    private AccessMethodsMetadata m_accessMethods;
    private ClassAccessor m_classAccessor;
    private DatabaseMapping m_mapping;
    private Map<String, PropertyMetadata> m_properties = new HashMap<String, PropertyMetadata>();

    /**
     * INTERNAL:
     */
    protected MappingAccessor(MetadataAnnotation annotation, MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
        super(annotation, accessibleObject, classAccessor.getDescriptor(), classAccessor.getProject());
        
        // We must keep a reference to the class accessors where this
        // mapping accessor is defined. We need it to determine access types.
        m_classAccessor = classAccessor;
    }
    
    /**
     * INTERNAL:
     */
    protected MappingAccessor(String xmlElement) {
        super(xmlElement);
    }

    /**
     * INTERNAL:
     * Process an attribute override for either an embedded object mapping, or
     * an element collection mapping containing embeddable objects.
     */
    protected void addFieldNameTranslation(EmbeddableMapping embeddableMapping, String overrideName, DatabaseField overrideField, DatabaseMapping aggregatesMapping) {
        DatabaseField aggregatesMappingField = aggregatesMapping.getField();
        
        // If the override field is to an id field, we need to update the 
        // list of primary keys on the owning descriptor. Embeddables can be 
        // shared and different owners may want to override the attribute 
        // with a different column.
        if (getOwningDescriptor().isPrimaryKeyField(aggregatesMappingField)) {
            getOwningDescriptor().removePrimaryKeyField(aggregatesMappingField);
            getOwningDescriptor().addPrimaryKeyField(overrideField, getOwningDescriptor().getPrimaryKeyAccessorForField(aggregatesMappingField));
        }
        
        if (overrideName.indexOf(".") > -1) {
            // Set the nested field name translation on the mapping. In an
            // Embedded case, this call will do the same thing that 
            // addFieldNameTranslation would do, there is no special treatment
            // and is implemented on aggregate object mapping only to satisfy
            // the EmbeddableMapping interface requirements. Nested attribute
            // overrides on an aggregate collection mapping are handled slightly
            // different though.
            embeddableMapping.addNestedFieldNameTranslation(overrideName, overrideField.getQualifiedName(), aggregatesMappingField.getName());
        } else {
            // Set the field name translation on the mapping.
            embeddableMapping.addFieldNameTranslation(overrideField.getQualifiedName(), aggregatesMappingField.getName()); 
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public AccessMethodsMetadata getAccessMethods(){
        return m_accessMethods;
    }
    
    /**
     * INTERNAL:
     * Process the list of association overrides into a map, merging and 
     * overriding any association overrides where necessary with descriptor
     * level association overrides.
     * TODO: This code should look for duplicates within the same list.
     */
    protected Map<String, AssociationOverrideMetadata> getAssociationOverrides(List<AssociationOverrideMetadata> associationOverrides) {
        Map<String, AssociationOverrideMetadata> associationOverridesMap = new HashMap<String, AssociationOverrideMetadata>();
        
        for (AssociationOverrideMetadata associationOverride : associationOverrides) {
            String name = associationOverride.getName();
            
            // An association override from a sub-entity class will name its
            // association override slightly different in that it will have 
            // one extra dot notation at the front. E.G. A mapped superclass 
            // that defines an embedded attribute named 'record' can define 
            // association overrides directly on the mapping, that is, 
            // 'date'. Whereas from an entity class to override 'date' on 
            // 'record', the attribute name will be 'record.date'
            String dotNotationName = getAttributeName() + "." + name;
            if (getClassAccessor().isMappedSuperclass() && getDescriptor().hasAssociationOverrideFor(dotNotationName)) {
                getLogger().logWarningMessage(getLogger().IGNORE_ASSOCIATION_OVERRIDE, name, getAttributeName(), getClassAccessor().getJavaClassName(), getJavaClassName());
                associationOverridesMap.put(name, getDescriptor().getAssociationOverrideFor(dotNotationName));
            } else {
                associationOverridesMap.put(name, associationOverride);
            }
        }
        
        // Now add every other descriptor association override that didn't 
        // override a mapping level one (if we are processing a mapping from
        // a mapped superclass level). We'll check the attribute names match
        // and rip off the extra qualifying when adding it to the override map.
        if (getClassAccessor().isMappedSuperclass()) {
            for (AssociationOverrideMetadata associationOverride : getDescriptor().getAssociationOverrides()) {
                String name = associationOverride.getName();
                String attributeName = name;
                String overrideName = name;
                int indexOfFirstDot = name.indexOf(".");                

                if (indexOfFirstDot > -1) {
                    attributeName = name.substring(0, indexOfFirstDot);
                    overrideName = name.substring(indexOfFirstDot + 1);
                }
                 
                if (attributeName.equals(getAttributeName()) && ! associationOverridesMap.containsKey(attributeName)) {
                    associationOverridesMap.put(overrideName, associationOverride);
                }
            }
        }
        
        return associationOverridesMap;
    }
    
    /**
     * INTERNAL:
     * Return the attribute name for this accessor. This is typically the
     * attribute name on the accessible object (i.e., field or property name),
     * however, if access-methods have been specified, use the name attribute
     * that was specified in XML. (e.g. basic name="sin") and not the property
     * name of the get method from the access-methods specification.
     */
    @Override
    public String getAttributeName() {
        if (m_accessMethods == null) {
            return getAccessibleObject().getAttributeName();
        } else {
            return getName();
        }
    }
    
    /**
     * INTERNAL:
     * Return the attribute override for this accessor.
     */
    protected AttributeOverrideMetadata getAttributeOverride(String loggingCtx) {
        if (loggingCtx.equals(MetadataLogger.MAP_KEY_COLUMN)) {
            return getDescriptor().getAttributeOverrideFor(KEY_DOT_NOTATION + getAttributeName());
        } else if (loggingCtx.equals(MetadataLogger.VALUE_COLUMN)) {
            if (getDescriptor().hasAttributeOverrideFor(VALUE_DOT_NOTATION + getAttributeName())) {
                return getDescriptor().getAttributeOverrideFor(VALUE_DOT_NOTATION + getAttributeName());
            }
        } 
            
        return getDescriptor().getAttributeOverrideFor(getAttributeName());
    }
    
    /**
     * INTERNAL:
     * Process the list of attribute overrides into a map, merging and 
     * overriding any attribute overrides where necessary with descriptor
     * level attribute overrides.
     * TODO: This code should look for duplicates within the same list.
     */
    protected Map<String, AttributeOverrideMetadata> getAttributeOverrides(List<AttributeOverrideMetadata> attributeOverrides) {
        HashMap<String, AttributeOverrideMetadata> attributeOverridesMap = new HashMap<String, AttributeOverrideMetadata>();
        
        for (AttributeOverrideMetadata attributeOverride : attributeOverrides) {
            String name = attributeOverride.getName();
            
            // An attribute override from a sub-entity class will name its
            // attribute override slightly different in that it will have one
            // extra dot notation at the front. E.G. A mapped superclass that
            // defines an embedded attribute named 'record' can define attribute
            // overrides directly on the mapping, that is, 'date'. Whereas
            // from an entity class to override 'date' on 'record', the
            // attribute name will be 'record.date'
            String dotNotationName = getAttributeName() + "." + name;
            if (getClassAccessor().isMappedSuperclass() && getDescriptor().hasAttributeOverrideFor(dotNotationName)) {
                getLogger().logWarningMessage(getLogger().IGNORE_ATTRIBUTE_OVERRIDE, name, getAttributeName(), getClassAccessor().getJavaClassName(), getJavaClassName());
                attributeOverridesMap.put(name, getDescriptor().getAttributeOverrideFor(dotNotationName));
            } else {
                attributeOverridesMap.put(name, attributeOverride);
            }
        }
        
        // Now add every other descriptor association override that didn't 
        // override a mapping level one (if we are processing a mapping from
        // a mapped superclass level). We'll check the attribute names match
        // and rip off the extra qualifying when adding it to the override map.
        if (getClassAccessor().isMappedSuperclass()) {
            for (AttributeOverrideMetadata attributeOverride : getDescriptor().getAttributeOverrides()) {
                String name = attributeOverride.getName();
                String attributeName = name;
                String overrideName = name;
                int indexOfFirstDot = name.indexOf(".");                

                if (indexOfFirstDot > -1) {
                    attributeName = name.substring(0, indexOfFirstDot);
                    overrideName = name.substring(indexOfFirstDot + 1);
                }
                 
                if (attributeName.equals(getAttributeName()) && ! attributeOverridesMap.containsKey(attributeName)) {
                    attributeOverridesMap.put(overrideName, attributeOverride);
                }
            }
        }
        
        return attributeOverridesMap;
    }
    
    /**
     * INTERNAL:
     * Returns the class accessor on which this mapping was defined.
     */
    public ClassAccessor getClassAccessor(){
        return m_classAccessor;
    }
    
    /**
     * INTERNAL:
     * Subclasses should override this method to return the appropriate
     * column for their mapping.
     * @see BasicAccessor
     * @see BasicCollectionAccessor
     * @see BasicMapAccessor
     * @see ElementCollectionAccessor
     * @see CollectionAccessor
     */
    protected ColumnMetadata getColumn(String loggingCtx) {
        return new ColumnMetadata(getAccessibleObject(), getAttributeName());
    }
    
    /**
     *  INTERNAL:
     * Process column metadata details into a database field. This will set 
     * correct metadata and log defaulting messages to the user. It also looks 
     * for an attribute override.
     * 
     * This method will call getColumn() which assumes the subclasses will
     * return the appropriate ColumnMetadata to process based on the context
     * provided.
     * 
     * @See BasicCollectionAccessor and BasicMapAccessor.
     */
    protected DatabaseField getDatabaseField(DatabaseTable defaultTable, String loggingCtx) {
        // Check if we have an attribute override first, otherwise process for a column
        ColumnMetadata column  = hasAttributeOverride(loggingCtx) ? getAttributeOverride(loggingCtx).getColumn() : getColumn(loggingCtx);
        
        // Get the actual database field and apply any defaults.
        DatabaseField field = column.getDatabaseField();
           
        // Make sure there is a table name on the field.
        if (field.getTableName().equals("")) {
            field.setTable(defaultTable);
        }
          
        // Set the correct field name, defaulting and logging when necessary.
        String defaultName = column.getDefaultAttributeName(getProject());
           
        // If this is for a map key column, append a suffix.
        if (loggingCtx.equals(MetadataLogger.MAP_KEY_COLUMN)) {
            defaultName += DEFAULT_MAP_KEY_COLUMN_SUFFIX;
        }
        
           
        field.setName(getName(field.getName(), defaultName, loggingCtx));
        
        if(field.getTable() != null){
            field.getTable().setUseDelimiters(useDelimitedIdentifier());
        }
        field.setUseDelimiters(useDelimitedIdentifier());
                       
        return field;
    }
    
    /**
     * INTERNAL:
     */
    protected String getDefaultFetchType() {
        return FetchType.EAGER.name(); 
    }
    
    /**
     * INTERNAL:
     * Return the default table to hold the foreign key of a MapKey when
     * and Entity is used as the MapKey
     * @return
     */
    protected DatabaseTable getDefaultTableForEntityMapKey(){
        return getReferenceDescriptor().getPrimaryTable();
    }
    
    /**
     * INTERNAL:
     * Return the enumerated metadata for this accessor.
     * @see DirectAccessor
     * @see ElementCollectionAccessor
     * @see CollectionAccessor
     */
    public EnumeratedMetadata getEnumerated(boolean isForMapKey) {
        return null;
    }
    
    /**
     * INTERNAL:
     * Returns the get method name of a method accessor. Note, this method
     * should not be called when processing field access.
     */
    protected String getGetMethodName() {
        if (m_accessMethods != null && m_accessMethods.getGetMethodName() != null) {
            return m_accessMethods.getGetMethodName();
        }
        
        return getAccessibleObjectName();
    }
    
    /**
     * INTERNAL:
     * Return the join columns to use with this mapping accessor. This method 
     * will look for association overrides and use those instead if some are 
     * available. This method will validate the join columns and default
     * any where necessary.
     */
    protected List<JoinColumnMetadata> getJoinColumns(List<JoinColumnMetadata> potentialJoinColumns, MetadataDescriptor descriptor) {
        if (getDescriptor().hasAssociationOverrideFor(getAttributeName())) {
            return getJoinColumnsAndValidate(getDescriptor().getAssociationOverrideFor(getAttributeName()).getJoinColumns(), descriptor);
        } else {
            return getJoinColumnsAndValidate(potentialJoinColumns, descriptor);
        }
    }
    
    /**
     * INTERNAL:
     * This method will validate the join columns and default any where 
     * necessary.
     */    
    protected List<JoinColumnMetadata> getJoinColumnsAndValidate(List<JoinColumnMetadata> joinColumns, MetadataDescriptor descriptor) {
        if (joinColumns.isEmpty()) {
            if (descriptor.hasCompositePrimaryKey()) {
                // Add a default one for each part of the composite primary
                // key. Foreign and primary key to have the same name.
                for (String primaryKeyField : descriptor.getPrimaryKeyFieldNames()) {
                    JoinColumnMetadata joinColumn = new JoinColumnMetadata();
                    joinColumn.setReferencedColumnName(primaryKeyField);
                    joinColumn.setName(primaryKeyField);
                    joinColumns.add(joinColumn);
                }
            } else {
                // Add a default one for the single case, not setting any
                // foreign and primary key names. They will default based
                // on which accessor is using them.
                joinColumns.add(new JoinColumnMetadata());
            }
        } else {
            // Need to update any join columns that use a foreign key name
            // for the primary key name. E.G. User specifies the renamed id
            // field name from a primary key join column as the primary key in
            // an inheritance subclass.
            for (JoinColumnMetadata joinColumn : joinColumns) {
                // Doing this could potentially change a value entered in XML.
                // However, in this case I think that is ok since in theory we 
                // are writing out the correct value that EclipseLink needs to 
                // form valid queries.
                joinColumn.setReferencedColumnName(descriptor.getPrimaryKeyJoinColumnAssociation(joinColumn.getReferencedColumnName()));
            }
        }
        
        if (descriptor.hasCompositePrimaryKey()) {
            // The number of join columns should equal the number of primary key fields.
            if (joinColumns.size() != descriptor.getPrimaryKeyFields().size()) {
                throw ValidationException.incompleteJoinColumnsSpecified(getAnnotatedElement(), getJavaClass());
            }
            
            // All the primary and foreign key field names should be specified.
            for (JoinColumnMetadata joinColumn : joinColumns) {
                if (joinColumn.isPrimaryKeyFieldNotSpecified() || joinColumn.isForeignKeyFieldNotSpecified()) {
                    throw ValidationException.incompleteJoinColumnsSpecified(getAnnotatedElement(), getJavaClass());
                }
            }
        }
        
        return joinColumns;
    }
    
    /**
     * INTERNAL:
     * Return the lob metadata for this accessor.
     * @see DirectAccessor
     */
    public LobMetadata getLob(boolean isForMapKey) {
        return null;
    }
    
    /**
     * INTERNAL:
     * Return the mapping that this accessor is associated to.
     */
    public DatabaseMapping getMapping(){
        return m_mapping;
    }
    
    /**
     * INTERNAL:
     * Return the mapping join fetch type.
     */
    protected int getMappingJoinFetchType(String joinFetchType) {
        if (joinFetchType == null) {
            return ForeignReferenceMapping.NONE;
        } else if (joinFetchType.equals(JoinFetchType.INNER.name())) {
            return ForeignReferenceMapping.INNER_JOIN;
        }

        return ForeignReferenceMapping.OUTER_JOIN;
    }
    
    /**
     * INTERNAL:
     * Return the map key reference class for this accessor if applicable. It 
     * will try to extract a reference class from a generic specification.<p>
     * Parameterized generic keys on a MappedSuperclass will return void.class.<p>  
     * If no generics are used, then it will return void.class. This avoids NPE's 
     * when processing JPA converters that can default (Enumerated and Temporal) 
     * based on the reference class.
     */
    public MetadataClass getMapKeyReferenceClass() {
        if (isMapAccessor()) {
            MetadataClass referenceClass = getAccessibleObject().getMapKeyClass(getDescriptor());
        
            if (referenceClass == null) {
                throw ValidationException.unableToDetermineMapKeyClass(getAttributeName(), getJavaClass());
            }            
        
            /**
             * 266912:  Use of parameterized generic types like Map<X,Y> inherits from class<T> in a MappedSuperclass field
             * will cause referencing issues - as in we are unable to determine the correct type for T.
             * A workaround for this is to detect when we are in this state and return a standard top level class.
             * An invalid class will be of the form MetadataClass.m_name="T" 
             */
            if(this.getClassAccessor().isMappedSuperclass()) {
                // Determine whether we are directly referencing a class or using a parameterized generic reference
                // by trying to load the class and catching any validationException.
                // If we do not get an exception on getClass then the referenceClass.m_name is valid and should be directly returned
                try {
                    MetadataHelper.getClassForName(referenceClass.getName(), getMetadataFactory().getLoader());
                } catch (ValidationException exception) {
                    // Default to Void for parameterized types
                    // Ideally we would need a MetadataClass.isParameterized() to inform us instead.
                    return new MetadataClass(this.getMetadataFactory(), Void.class);
                }                          
            }
            return referenceClass;
        } else {
            return getMetadataFactory().getMetadataClass(void.class.getName());
        }
    }
    
    /**
     * INTERNAL:
     * Return the raw class for this accessor. 
     * Eg. For an accessor with a type of java.util.Collection<Employee>, this 
     * method will return java.util.Collection
     */
    public MetadataClass getRawClass() {
        return getAccessibleObject().getRawClass(getDescriptor());   
    }
    
    /**
     * INTERNAL:
     * Return the mapping accessors associated with the reference descriptor.
     */
    public Collection<MappingAccessor> getReferenceAccessors() {
        return getReferenceDescriptor().getAccessors();
    }
    
    /**
     * INTERNAL: 
     * Return the reference class for this accessor. By default the reference
     * class is the raw class. Some accessors may need to override this
     * method to drill down further. That is, try to extract a reference class
     * from generics.
     */
    public MetadataClass getReferenceClass() {
        return getAccessibleObject().getRawClass(getDescriptor());
    }
    
    /**
     * INTERNAL:
     * Attempts to return a reference class from a generic specification. Note,
     * this method may return null.
     */
    public MetadataClass getReferenceClassFromGeneric() {
        return getAccessibleObject().getReferenceClassFromGeneric(getDescriptor());
    }

    /**
     * INTERNAL:
     * Return the reference class name for this accessor.
     */
    public String getReferenceClassName() {
        return getReferenceClass().getName();
    }
    
    /**
     * INTERNAL:
     * Return the reference descriptors table. By default it is the primary
     * key table off the reference descriptor. Subclasses that care to return
     * a different class should override this method.
     * @see DirectCollectionAccessor
     * @see ManyToManyAccessor
     */
    protected DatabaseTable getReferenceDatabaseTable() {
        return getReferenceDescriptor().getPrimaryKeyTable();
    }
    
    /**
     * INTERNAL:
     * Return the reference metadata descriptor for this accessor.
     */
    public MetadataDescriptor getReferenceDescriptor() {
        ClassAccessor accessor = getProject().getAccessor(getReferenceClassName());
        
        if (accessor == null) {
            throw ValidationException.classNotListedInPersistenceUnit(getReferenceClassName());
        }
        
        return accessor.getDescriptor();
    }
    
    /**
     * INTERNAL:
     * Returns the set method name of a method accessor. Note, this method
     * should not be called when processing field access.
     */
    protected String getSetMethodName() {
        if (m_accessMethods != null && m_accessMethods.getSetMethodName() != null) {
            return m_accessMethods.getSetMethodName();
        }

        return ((MetadataMethod) getAccessibleObject()).getSetMethodName();
    }
    
    /**
     * INTERNAL:
     * Return the temporal metadata for this accessor.
     * @see DirectAccessor
     * @see CollectionAccessor
     */
    public TemporalMetadata getTemporal(boolean isForMapKey) {
        return null;
    }
    
    /**
     * INTERNAL:
     * Return true if we have an attribute override for this accessor.
     */
    protected boolean hasAttributeOverride(String loggingCtx) {
        if (loggingCtx.equals(MetadataLogger.MAP_KEY_COLUMN)) {
            return getDescriptor().hasAttributeOverrideFor(KEY_DOT_NOTATION + getAttributeName());
        } else if (loggingCtx.equals(MetadataLogger.VALUE_COLUMN)) {
            if (getDescriptor().hasAttributeOverrideFor(VALUE_DOT_NOTATION + getAttributeName())) {
                return true;
            }
        } 
            
        return getDescriptor().hasAttributeOverrideFor(getAttributeName());
    }
    
    /**
     * INTERNAL:
     * Method to check if an annotated element has a Column annotation.
     */
    protected boolean hasColumn() {
        return isAnnotationPresent(Column.class);
    }
    
    /**
     * INTERNAL:
     * Method to check if an annotated element has a convert specified. In XML
     * we can restrict where converts can be specified.
     * @see DirectAccessor
     * @see BasicMapAccessor
     * @see ElementCollectionAccessor
     * @see CollectionAccessor
     */
    protected boolean hasConvert(boolean isForMapKey) {
        return isAnnotationPresent(Convert.class);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor has temporal metadata.
     * @see DirectAccessor
     * @see ElementCollectionAccessor
     * @see CollectionAccessor
     */
    protected boolean hasEnumerated(boolean isForMapKey) {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor has lob metadata.
     * @see DirectAccessor
     * @see ElementCollectionAccessor
     * @see CollectionAccessor
     */
    protected boolean hasLob(boolean isForMapKey) {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return whether the map key has been set.
     * @return
     */
    public boolean hasMapKey(){
        return false;
    }
    
    /**
     * INTERNAL:
     * Method to check if this accessor has a ReturnInsert annotation.
     */
    protected boolean hasReturnInsert() {
        return isAnnotationPresent(ReturnInsert.class);
    }
    
    /**
     * INTERNAL:
     * Method to check if this accessor has a ReturnUpdate annotation.
     */
    protected boolean hasReturnUpdate() {
        return isAnnotationPresent(ReturnUpdate.class);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor has temporal metadata.
     * @see DirectAccessor
     * @see ElementCollectionAccessor
     * @see CollectionAccessor
     */
    public boolean hasTemporal(boolean isForMapKey) {
        return false;
    }
    
    /**
     * INTERNAL: 
     * Init an xml mapping accessor with its necessary components. 
     */
    public void initXMLMappingAccessor(ClassAccessor classAccessor) {
        m_classAccessor = classAccessor;
        setEntityMappings(classAccessor.getEntityMappings());
        initXMLAccessor(classAccessor.getDescriptor(), classAccessor.getProject());   
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject, XMLEntityMappings entityMappings) {
        super.initXMLObject(accessibleObject, entityMappings);
        
        // Initialize single objects.
        initXMLObject(m_accessMethods, accessibleObject);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a basic mapping.
     */
    public boolean isBasic() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a basic collection mapping.
     */
    public boolean isBasicCollection() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a basic map mapping.
     */
    public boolean isBasicMap() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true is this accessor is a derived id accessor.
     * @see ObjectAccessor
     */
    public boolean isDerivedId() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor is a derived id class accessor.
     */
    public boolean isDerivedIdClass(){
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a direct collection mapping, 
     * which include basic collection, basic map and element collection 
     * accessors.
     */
    public boolean isDirectCollection() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents an element collection that
     * contains embeddable objects.
     */
    public boolean isDirectEmbeddableCollection() {
        return false;
    }
    
    /** 
     * INTERNAL:
     * Return true if this accessor represents a collection accessor.
     */
    public boolean isCollectionAccessor() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents an aggregate mapping.
     */
    public boolean isEmbedded() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents an aggregate id mapping.
     */
    public boolean isEmbeddedId() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this represents an enum type mapping. Will return true
     * if the accessor's reference class is an enum or if enumerated metadata
     * exists.
     */
    protected boolean isEnumerated(MetadataClass referenceClass, boolean isForMapKey) {
        if (hasConvert(isForMapKey)) {
            // If we have an @Enumerated with a @Convert, the @Convert takes
            // precedence and we will ignore the @Enumerated and log a message.
            if (hasEnumerated(isForMapKey)) {
                getLogger().logWarningMessage(MetadataLogger.IGNORE_ENUMERATED, getJavaClass(), getAnnotatedElement());
            }
            
            return false;
        } else {
            return hasEnumerated(isForMapKey) || EnumeratedMetadata.isValidEnumeratedType(referenceClass);
        }
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor is part of the id.
     */
    public boolean isId(){
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a BLOB/CLOB mapping.
     */
    protected boolean isLob(MetadataClass referenceClass, boolean isForMapKey) {
        if (hasConvert(isForMapKey)) {
            // If we have a Lob specified with a Convert, the Convert takes 
            // precedence and we will ignore the Lob and log a message.
            if (hasLob(isForMapKey)) {
                getLogger().logWarningMessage(MetadataLogger.IGNORE_LOB, getJavaClass(), getAnnotatedElement());
            }
            
            return false;
        } else {
            return hasLob(isForMapKey);
        }
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a m-m relationship.
     */
    public boolean isManyToMany() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a m-1 relationship.
     */
    public boolean isManyToOne() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor uses a Map.
     */
    public boolean isMapAccessor() {
        return getAccessibleObject().isSupportedMapClass(getDescriptor());
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor is a mapped key map accessor.
     */
    public boolean isMappedKeyMapAccessor() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a 1-m relationship.
     */
    public boolean isOneToMany() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a 1-1 relationship.
     */
    public boolean isOneToOne() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Returns true is the given class is primitive wrapper type.
     */
    protected boolean isPrimitiveWrapperClass(MetadataClass cls) {
        return cls.extendsClass(Number.class) ||
            cls.equals(Boolean.class) ||
            cls.equals(Character.class) ||
            cls.equals(String.class) ||
            cls.extendsClass(java.math.BigInteger.class) ||
            cls.extendsClass(java.math.BigDecimal.class) ||
            cls.extendsClass(java.util.Date.class) ||
            cls.extendsClass(java.util.Calendar.class);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor has been processed. If there is a mapping
     * set, we have processed this accessor.
     */
    @Override
    public boolean isProcessed() {
        return m_mapping != null;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor method represents a relationship.
     */
    public boolean isRelationship() {
        return isManyToOne() || isManyToMany() || isOneToMany() || isOneToOne() || isVariableOneToOne();
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a serialized mapping.
     */
    public boolean isSerialized(MetadataClass referenceClass, boolean isForMapKey) {
        if (hasConvert(isForMapKey)) {
            getLogger().logWarningMessage(MetadataLogger.IGNORE_SERIALIZED, getJavaClass(), getAnnotatedElement());
            return false;
        } else {
            return isValidSerializedType(referenceClass);
        }
    }
    
    /**
     * INTERNAL:
     * Return true if this represents a temporal type mapping.
     */
    protected boolean isTemporal(MetadataClass referenceClass, boolean isForMapKey) {
        if (hasConvert(isForMapKey)) {
            // If we have a Temporal specification with a Convert specification, 
            // the Convert takes precedence and we will ignore the Temporal and 
            // log a message.
            if (hasTemporal(isForMapKey)) {
                getLogger().logWarningMessage(MetadataLogger.IGNORE_TEMPORAL, getJavaClass(), getAnnotatedElement());
            }
            
            return false;
        } else {
            return hasTemporal(isForMapKey) || TemporalMetadata.isValidTemporalType(referenceClass);
        }
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a transient mapping.
     */
    public boolean isTransient() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Returns true if the given class is valid for SerializedObjectMapping.
     */
    protected boolean isValidSerializedType(MetadataClass cls) {
        if (cls.isPrimitive()) {
            return false;
        }
        
        if (isPrimitiveWrapperClass(cls)) {    
            return false;
        }   
        
        if (LobMetadata.isValidLobType(cls)) {
            return false;
        }
        
        if (TemporalMetadata.isValidTemporalType(cls)) {
            return false;
        }
     
        return true;   
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a variable one to one mapping.
     */
    public boolean isVariableOneToOne() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Process an association override for either an embedded object mapping, 
     * or a map mapping (element-collection, 1-M and M-M) containing an
     * embeddable object as the value or key.
     * This method should be implemented in those accessors that support 
     * association overrides. An exception is thrown otherwise the association
     * is called against an unsupported accessor/relationship. 
     */
    protected void processAssociationOverride(AssociationOverrideMetadata associationOverride, EmbeddableMapping embeddableMapping, MetadataDescriptor owningDescriptor) {
        throw ValidationException.invalidEmbeddableAttributeForAssociationOverride(getJavaClass(), getAttributeName(), associationOverride.getName(), associationOverride.getLocation()); 
    }
    
    /**
     * INTERNAL:
     * Process the association overrides for the given embeddable mapping which
     * is either an embedded or element collection mapping. Association 
     * overrides are used to specify different keys to a shared mapping.
     */
    protected void processAssociationOverrides(List<AssociationOverrideMetadata> associationOverrides, EmbeddableMapping embeddableMapping, MetadataDescriptor embeddableDescriptor) {
        // Get the processible map of association overrides. This will take dot 
        // notation overrides into consideration (from a sub-entity to a mapped 
        // superclass accessor) and merge the lists. Once the map is returned, 
        // use the map keys as the attribute name and not the name from the 
        // individual association override since they could still contain dot 
        // notation names meaning you will not find their respective mapping 
        // accessor on the embeddable descriptor.
        Map<String, AssociationOverrideMetadata> mergedAssociationOverrides = getAssociationOverrides(associationOverrides);
        
        for (String attributeName : mergedAssociationOverrides.keySet()) {
            AssociationOverrideMetadata associationOverride = mergedAssociationOverrides.get(attributeName);
            // The getAccessorFor call will take care of any sub dot notation 
            // attribute names when looking for the mapping. It will traverse 
            // the embeddable chain. 
            MappingAccessor mappingAccessor = embeddableDescriptor.getAccessorFor(attributeName);
            
            if (mappingAccessor == null) {
                throw ValidationException.embeddableAssociationOverrideNotFound(embeddableDescriptor.getJavaClass(), attributeName, getJavaClass(), getAttributeName());
            } else {
                mappingAccessor.processAssociationOverride(associationOverride, embeddableMapping, getOwningDescriptor());
            }  
        }
    }
    
    /**
     * INTERNAL:
     * Process the attribute overrides for the given embedded mapping. Attribute 
     * overrides are used to apply the correct field name translations of direct 
     * fields. Note an embedded object mapping may be supported as the map key
     * to an element-collection, 1-M and M-M mapping.
     */
    protected void processAttributeOverrides(List<AttributeOverrideMetadata> attributeOverrides, AggregateObjectMapping aggregateObjectMapping, MetadataDescriptor embeddableDescriptor) {
        // Get the processible map of attribute overrides. This will take dot 
        // notation overrides into consideration (from a sub-entity to a mapped 
        // superclass accessor) and merge the lists. Once the map is returned, 
        // use the map keys as the attribute name and not the name from the 
        // individual attribute override since they could still contain dot 
        // notation names meaning you will not find their respective mapping 
        // accessor on the embeddable descriptor.
        Map<String, AttributeOverrideMetadata> mergedAttributeOverrides = getAttributeOverrides(attributeOverrides);
        
        for (String attributeName : mergedAttributeOverrides.keySet()) {
            AttributeOverrideMetadata attributeOverride = mergedAttributeOverrides.get(attributeName);
            // The getMappingForAttributeName call will take care of any sub dot 
            // notation attribute names when looking for the mapping. It will
            // traverse the embeddable chain. 
            DatabaseMapping mapping = embeddableDescriptor.getMappingForAttributeName(attributeName);
                
            if (mapping == null) {
                throw ValidationException.embeddableAttributeOverrideNotFound(embeddableDescriptor.getJavaClass(), attributeName, getJavaClass(), getAttributeName());
            } else if (! mapping.isDirectToFieldMapping()) {
                throw ValidationException.invalidEmbeddableAttributeForAttributeOverride(embeddableDescriptor.getJavaClass(), attributeName, getJavaClass(), getAttributeName());
            } else {
                addFieldNameTranslation(aggregateObjectMapping, attributeName, attributeOverride.getColumn().getDatabaseField(), mapping);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Process the map metadata if this is a valid map accessor. Will return 
     * the map key method name that should be use, null otherwise.
     * @see CollectionAccessor
     * @see ElementCollectionAccessor
     */
    protected void processContainerPolicyAndIndirection(CollectionMapping mapping, String mapKey) {
        if (isMappedKeyMapAccessor()) {
            MappedKeyMapAccessor mapKeyMapAccessor = (MappedKeyMapAccessor) this;
            MetadataClass mapKeyClass = mapKeyMapAccessor.getMapKeyClass();
            if (mapKeyClass != null && (getProject().hasEntity(mapKeyClass) || getProject().hasEmbeddable(mapKeyClass) || mapKeyMapAccessor.getMapKeyColumn() != null)) {
            // TODO: if map key is specified we should throw an exception.
               processMapKeyClass(mapKeyClass, mapping, mapKeyMapAccessor);
            } else {
                // Set the indirection policy on the mapping
                setIndirectionPolicy(mapping, processMapKey(mapKey, mapping), usesIndirection());
            }
        } else if (isMapAccessor()) {
            // Set the indirection policy on the mapping.
            setIndirectionPolicy(mapping, processMapKey(mapKey, mapping), usesIndirection());
        } else {
            // Set the indirection policy on the mapping.
            setIndirectionPolicy(mapping, null, usesIndirection());
        }
    } 
    
    /**
     * INTERNAL:
     * Process a Convert annotation or convert element to apply to specified 
     * EclipseLink converter (Converter, TypeConverter, ObjectTypeConverter) 
     * to the given mapping.
     */
    protected void processConvert(DatabaseMapping mapping, String converterName, MetadataClass referenceClass, boolean isForMapKey) {
        // There is no work to do if the converter's name is "none".
        if (! converterName.equals(CONVERT_NONE)) {
            if (converterName.equals(CONVERT_SERIALIZED)) {
                processSerialized(mapping, referenceClass, isForMapKey);
            } else if (converterName.equals(CONVERT_CLASS_INSTANCE)){
                new ClassInstanceMetadata().process(mapping, this, referenceClass, isForMapKey);
            } else {
                AbstractConverterMetadata converter = getProject().getConverter(converterName);
                
                if (converter == null) {
                    throw ValidationException.converterNotFound(getJavaClass(), converterName, getAnnotatedElement());
                } else {
                    // Process the converter for this mapping.
                    converter.process(mapping, this, referenceClass, isForMapKey);
                }
            }
        }
    }
    
    /**
     * INTERNAL:
     */
    protected DirectToFieldMapping processDirectMapKeyClass(MetadataClass mapKeyClass, MappedKeyMapAccessor mappedKeyMapAccessor) {
        DirectToFieldMapping keyMapping = new DirectToFieldMapping();

        // Get the map key field, defaulting and looking for attribute 
        // overrides. Set the field before applying a converter.
        DatabaseField mapKeyField = getDatabaseField(getReferenceDatabaseTable(), MetadataLogger.MAP_KEY_COLUMN);
        keyMapping.setField(mapKeyField);
        
        // Process a convert key or jpa converter for the map key if specified.
        processMappingKeyConverter(keyMapping, mappedKeyMapAccessor.getMapKeyConvert(), mappedKeyMapAccessor.getMapKeyClass());
        
        keyMapping.setAttributeClassificationName(getMapKeyReferenceClass().getName());
        keyMapping.setDescriptor(getDescriptor().getClassDescriptor());
        
        return keyMapping;
    }
    
    /**
     * INTERNAL:
     */
    protected AggregateObjectMapping processEmbeddableMapKeyClass(MetadataClass mapKeyClass, MappedKeyMapAccessor mappedKeyMapAccessor) {
        AggregateObjectMapping keyMapping = new AggregateObjectMapping();
        keyMapping.setReferenceClassName(mapKeyClass.getName());
        
        // Tell the embeddable accessor to process itself it is hasn't already.
        EmbeddableAccessor mapKeyAccessor = getProject().getEmbeddableAccessor(mapKeyClass);
        if (! mapKeyAccessor.isProcessed()) {
            mapKeyAccessor.process(getReferenceDescriptor());
        }
        
        // Ensure the reference descriptor is marked as an embeddable collection.
        mapKeyAccessor.getDescriptor().setIsEmbeddableCollection();
        
        // Process the attribute overrides for this may key embeddable.
        processAttributeOverrides(mappedKeyMapAccessor.getMapKeyAttributeOverrides(), keyMapping, mapKeyAccessor.getDescriptor());
        
        // Process the association overrides for this may key embeddable.
        processAssociationOverrides(mappedKeyMapAccessor.getMapKeyAssociationOverrides(), keyMapping, mapKeyAccessor.getDescriptor());
        
        keyMapping.setDescriptor(getDescriptor().getClassDescriptor());
        
        return keyMapping;
    }
    
    /**
     * INTERNAL:
     * Process the map key to be an entity class.
     */
    protected OneToOneMapping processEntityMapKeyClass(MetadataClass mapKeyClass, MappedKeyMapAccessor mappedKeyMapAccessor) {
        // Create the one to one map key mapping.
        OneToOneMapping keyMapping = new OneToOneMapping();
        keyMapping.setReferenceClassName(mapKeyClass.getName());
        keyMapping.dontUseIndirection();
        keyMapping.setDescriptor(getDescriptor().getClassDescriptor());
        
        // Process the map key join columns.
        EntityAccessor mapKeyAccessor = getProject().getEntityAccessor(mapKeyClass.getName());
        MetadataDescriptor mapKeyClassDescriptor = mapKeyAccessor.getDescriptor();
        
        // If the pk field (referencedColumnName) is not specified, it 
        // defaults to the primary key of the referenced table.
        String defaultPKFieldName = mapKeyClassDescriptor.getPrimaryKeyFieldName();
        
        // If the fk field (name) is not specified, it defaults to the 
        // concatenation of the following: the name of the referencing 
        // relationship property or field of the referencing entity or 
        // embeddable; "_"; "KEY"
        String defaultFKFieldName = getAttributeName() + DEFAULT_MAP_KEY_COLUMN_SUFFIX;
        
        processOneToOneForeignKeyRelationship(keyMapping, getJoinColumns(mappedKeyMapAccessor.getMapKeyJoinColumns(), mapKeyClassDescriptor), defaultPKFieldName, mapKeyClassDescriptor.getPrimaryTable(), defaultFKFieldName, getDefaultTableForEntityMapKey());

        return keyMapping;
    }
    
    /**
     * INTERNAL:
     * Process an Enumerated setting. The method may still be called if no 
     * Enumerated metadata has been specified but the accessor's reference 
     * class is a valid enumerated type.
     */
    protected void processEnumerated(EnumeratedMetadata enumerated, DatabaseMapping mapping, MetadataClass referenceClass, boolean isForMapKey) {
        if (enumerated == null) {
            // TODO: Log a defaulting message
            enumerated = new EnumeratedMetadata(getAccessibleObject());
        }
        
        enumerated.process(mapping, this, referenceClass, isForMapKey);
    }
    
    /**
     * INTERNAL:
     * Process an Enumerated, Lob, Temporal, MapKeyEnumerated, MapKeyTempora 
     * specification. Will default a serialized converter if necessary. JPA 
     * converters can be applied to basics and to map keys/values of a map 
     * accessor.
     */
    protected void processJPAConverters(DatabaseMapping mapping, MetadataClass referenceClass, boolean isForMapKey) {
        // Check for an enum first since it will fall into a serializable 
        // mapping otherwise (Enums are serialized)
        if (isEnumerated(referenceClass, isForMapKey)) {
            processEnumerated(getEnumerated(isForMapKey), mapping, referenceClass, isForMapKey);
        } else if (isLob(referenceClass, isForMapKey)) {
            processLob(getLob(isForMapKey), mapping, referenceClass, isForMapKey);
        } else if (isTemporal(referenceClass, isForMapKey)) {
            processTemporal(getTemporal(isForMapKey), mapping, referenceClass, isForMapKey);
        } else if (isSerialized(referenceClass, isForMapKey)) {
            processSerialized(mapping, referenceClass, isForMapKey);
        }
    }
    
    /**
     * INTERNAL:
     * Process a lob specification. The lob must be specified to process and 
     * create a lob type mapping.
     */
    protected void processLob(LobMetadata lob, DatabaseMapping mapping, MetadataClass referenceClass, boolean isForMapKey) {
        lob.process(mapping, this, referenceClass, isForMapKey);
    }
    
    /**
     * INTERNAL:
     * Process a map key for a 1-M or M-M mapping. Will return the map key
     * method name that should be use, null otherwise.
     */
    protected String processMapKey(String mapKey, CollectionMapping mapping) {
        MetadataDescriptor referenceDescriptor = getReferenceDescriptor();
        if ((mapKey == null || mapKey.equals("")) && referenceDescriptor.hasCompositePrimaryKey()) {
            // No persistent property or field name has been provided, and the 
            // reference class has a composite primary key class.  Return null,
            // internally, EclipseLink will use an instance of the composite 
            // primary key class as the map key.
            return null;
        } else {
            // A persistent property or field name may have have been provided. 
            // If one has not we will default to the primary key of the reference 
            // class. The primary key cannot be composite at this point.
            String fieldOrPropertyName = getName(mapKey, referenceDescriptor.getIdAttributeName(), getLogger().MAP_KEY_ATTRIBUTE_NAME);
    
            // Look up the referenceAccessor
            MetadataAccessor referenceAccessor = referenceDescriptor.getAccessorFor(fieldOrPropertyName);
            if (referenceAccessor == null) {
                // 266912: relax validation for MappedSuperclass descriptors when the map key is an unresolved generic type
                if(!referenceDescriptor.isMappedSuperclass()) {
                    throw ValidationException.couldNotFindMapKey(fieldOrPropertyName, referenceDescriptor.getJavaClass(), mapping);
                } else {
                    return null;
                }
            }
        
            return referenceAccessor.getAccessibleObjectName();
        }
    }
    
    /**
     * INTERNAL:
     * Process a map key class for the given map accessor.
     */
    protected void processMapKeyClass(MetadataClass mapKeyClass, CollectionMapping mapping, MappedKeyMapAccessor mapAccessor) {
        MapKeyMapping keyMapping;
            
        if (getProject().hasEntity(mapKeyClass)) {
            keyMapping = processEntityMapKeyClass(mapKeyClass, mapAccessor);
        } else if (getProject().hasEmbeddable(mapKeyClass)) {
            keyMapping = processEmbeddableMapKeyClass(mapKeyClass, mapAccessor);
        } else {
            keyMapping = processDirectMapKeyClass(mapKeyClass, mapAccessor);
        }
          
        Class containerClass;
        if (usesIndirection()) {
            containerClass = ClassConstants.IndirectMap_Class;
            mapping.setIndirectionPolicy(new TransparentIndirectionPolicy());
        } else {
            containerClass = java.util.Hashtable.class;
            mapping.dontUseIndirection();
        }

        MappedKeyMapContainerPolicy policy = new MappedKeyMapContainerPolicy(containerClass);
        policy.setKeyMapping(keyMapping);
        policy.setValueMapping((MapComponentMapping) mapping);
        mapping.setContainerPolicy(policy);
    }
    
    /**
     * INTERNAL:
     * Process a convert value which specifies the name of an EclipseLink
     * converter to process with this accessor's mapping.     
     */
    protected void processMappingConverter(DatabaseMapping mapping, String convertValue, MetadataClass referenceClass, boolean isForMapKey) {
        if (convertValue != null && ! convertValue.equals(CONVERT_NONE)) {
            processConvert(mapping, convertValue, referenceClass, isForMapKey);
        } 

        // Regardless if we found a convert or not, look for JPA converters. 
        // This ensures two things; 
        // 1 - if no Convert is specified, then any JPA converter that is 
        // specified will be applied (see BasicMapAccessor's override of the
        // method hasConvert()). 
        // 2 - if a convert and a JPA converter are specified, then a log 
        // warning will be issued stating that we are ignoring the JPA 
        // converter.
        processJPAConverters(mapping, referenceClass, isForMapKey);
    }
    
    /**
     * INTERNAL:
     * Process a convert value which specifies the name of an EclipseLink
     * converter to process with this accessor's mapping key.
     */
    protected void processMappingKeyConverter(DatabaseMapping mapping, String convertValue, MetadataClass referenceClass) {
        processMappingConverter(mapping, convertValue, referenceClass, true); 
    }
    
    /**
     * INTERNAL:
     * Process a convert value which specifies the name of an EclipseLink
     * converter to process with this accessor's mapping.
     */
    protected void processMappingValueConverter(DatabaseMapping mapping, String convertValue, MetadataClass referenceClass) {
        processMappingConverter(mapping, convertValue, referenceClass, false); 
    }
    
    /**
     * INTERNAL:
     * Process the join columns for the owning side of a one to one mapping.
     * The default pk and fk field names are used only with single primary key 
     * entities. The processor should never get as far as to use them with 
     * entities that have a composite primary key (validation exception will be 
     * thrown).
     */
    protected void processOneToOneForeignKeyRelationship(OneToOneMapping mapping, List<JoinColumnMetadata> joinColumns, String defaultPKFieldName, DatabaseTable defaultPKTable, String defaultFKFieldName, DatabaseTable defaultFKTable) {         
        // Add the source foreign key fields to the mapping.
        for (JoinColumnMetadata joinColumn : joinColumns) {
            DatabaseField pkField = joinColumn.getPrimaryKeyField();
            pkField.setName(getName(pkField, defaultPKFieldName, MetadataLogger.PK_COLUMN));
            pkField.setUseDelimiters(useDelimitedIdentifier());
            pkField.setTable(defaultPKTable);
            
            DatabaseField fkField = joinColumn.getForeignKeyField();
            fkField.setName(getName(fkField, defaultFKFieldName, MetadataLogger.FK_COLUMN));
            fkField.setUseDelimiters(useDelimitedIdentifier());
            // Set the table name if one is not already set.
            if (fkField.getTableName().equals("")) {
                fkField.setTable(defaultFKTable);
            }
            
            // Add a source foreign key to the mapping.
            mapping.addForeignKeyField(fkField, pkField);
            
            // If any of the join columns is marked read-only then set the 
            // mapping to be read only.
            if (fkField.isReadOnly()) {
                mapping.setIsReadOnly(true);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Adds properties to the mapping.
     */
    protected void processProperties(DatabaseMapping mapping) {
        // If we were loaded from XML use the properties loaded from there
        // only. Otherwise look for annotations.
        if (loadedFromXML()) {
            for (PropertyMetadata property : getProperties()) {
                processProperty(mapping, property);
            }
        } else {
            // Look for annotations.
            MetadataAnnotation properties = getAnnotation(Properties.class);
            if (properties != null) {
                for (Object property : (Object[]) properties.getAttribute("value")) {
                    processProperty(mapping, new PropertyMetadata((MetadataAnnotation)property, getAccessibleObject()));
                }
            }
            
            MetadataAnnotation property = getAnnotation(Property.class);
            if (property != null) {
                processProperty(mapping, new PropertyMetadata(property, getAccessibleObject()));
            }    
        }
    }
    
    /**
     * INTERNAL:
     * Adds properties to the mapping. They can only come from one place,
     * therefore is we add the same one twice we know to throw an exception.
     */
    protected void processProperty(DatabaseMapping mapping, PropertyMetadata property) {
        if (property.shouldOverride(m_properties.get(property.getName()))) {
            m_properties.put(property.getName(), property);
            mapping.getProperties().put(property.getName(), property.getConvertedValue());
        }
    }
    
    /**
     * INTERNAL:
     * Subclasses should call this method if they want the warning message or
     * override the method if they want/support different behavior.
     * @see BasicAccessor
     */
    protected void processReturnInsert() {
        if (hasReturnInsert()) {
            getLogger().logWarningMessage(MetadataLogger.IGNORE_RETURN_INSERT_ANNOTATION, getAnnotatedElement());
        }
    }
    
    /**
     * INTERNAL:
     * Subclasses should call this method if they want the warning message.
     */
    protected void processReturnInsertAndUpdate() {
        processReturnInsert();
        processReturnUpdate();
    }
    
    /**
     * INTERNAL:
     * Subclasses should call this method if they want the warning message or
     * override the method if they want/support different behavior.
     * @see BasicAccessor
     */
    protected void processReturnUpdate() {
        if (hasReturnUpdate()) {
            getLogger().logWarningMessage(MetadataLogger.IGNORE_RETURN_UPDATE_ANNOTATION, getAnnotatedElement());
        }
    }
    
    /**
     * INTERNAL:
     * Process a potential serializable attribute. If the class implements 
     * the Serializable interface then set a SerializedObjectConverter on 
     * the mapping.
     */
    protected void processSerialized(DatabaseMapping mapping, MetadataClass referenceClass, boolean isForMapKey) {        
        new SerializedMetadata(getAccessibleObject()).process(mapping, this, referenceClass, isForMapKey);
    }
    
    /**
     * INTERNAL:
     * Process a potential serializable attribute. If the class implements 
     * the Serializable interface then set a SerializedObjectConverter on 
     * the mapping.
     */
    protected void processSerialized(DatabaseMapping mapping, MetadataClass referenceClass, MetadataClass classification, boolean isForMapKey) {
        new SerializedMetadata(getAccessibleObject()).process(mapping, this, referenceClass, classification, isForMapKey);
    }
    
    /**
     * INTERNAL:
     * Process a temporal type accessor.
     */
    protected void processTemporal(TemporalMetadata temporal, DatabaseMapping mapping, MetadataClass referenceClass, boolean isForMapKey) {
        if (temporal == null) {
            // We have a temporal type on either a basic mapping or the key to
            // a collection mapping. Since the temporal type was not specified, 
            // per the JPA spec we must throw an exception.
            throw ValidationException.noTemporalTypeSpecified(getAttributeName(), getJavaClass());
        }
        
        temporal.process(mapping, this, referenceClass, isForMapKey);
    }
    
    /**
     * INTERNAL:
     */
    public void setAccessMethods(AccessMethodsMetadata accessMethods){
        m_accessMethods = accessMethods;
    }
    
    /**
     * INTERNAL:
     * Set the getter and setter access methods for this accessor.
     */
    protected void setAccessorMethods(DatabaseMapping mapping) {
        if (usesPropertyAccess(getDescriptor())) {
            mapping.setGetMethodName(getGetMethodName());
            mapping.setSetMethodName(getSetMethodName());
        }
    }
    
    /**
     * INTERNAL:
     * Sets the class accessor for this mapping accessor.
     */
    public void setClassAccessor(ClassAccessor classAccessor) {
        m_classAccessor = classAccessor;
    }
    
    /** 
     * INTERNAL:
     * Set the correct indirection policy on a collection mapping. Method
     * assume that the reference class has been set on the mapping before
     * calling this method.
     */
    protected void setIndirectionPolicy(CollectionMapping mapping, String mapKey, boolean usesIndirection) {
        MetadataClass rawClass = getRawClass();
        
        if (usesIndirection) {            
            if (rawClass.equals(Map.class)) {
                if (mapping.isDirectMapMapping()) {
                    ((DirectMapMapping) mapping).useTransparentMap();
                } else {
                    mapping.useTransparentMap(mapKey);
                }
            } else if (rawClass.equals(List.class)) {
                mapping.useTransparentList();
            } else if (rawClass.equals(Collection.class)) {
                mapping.useTransparentCollection();
            } else if (rawClass.equals(Set.class)) {
                mapping.useTransparentSet();
            } else {
                //bug221577: This should be supported when a transparent indirection class can be set through eclipseLink_orm.xml, or basic indirection is used
                getLogger().logWarningMessage(MetadataLogger.WARNING_INVALID_COLLECTION_USED_ON_LAZY_RELATION, getJavaClass(), getAnnotatedElement(), rawClass);
            }
        } else {
            mapping.dontUseIndirection();
            
            if (rawClass.equals(Map.class)) {
                if (mapping.isDirectMapMapping()) {
                    ((DirectMapMapping) mapping).useMapClass(java.util.Hashtable.class);
                } else {
                    mapping.useMapClass(java.util.Hashtable.class, mapKey);
                }
            } else if (rawClass.equals(Set.class)) {
                // This will cause it to use a CollectionContainerPolicy type
                mapping.useCollectionClass(java.util.HashSet.class);
            } else if (rawClass.equals(List.class)) {
                // This will cause a ListContainerPolicy type to be used or 
                // OrderedListContainerPolicy if ordering is specified.
                mapping.useCollectionClass(java.util.Vector.class);
            } else if (rawClass.equals(Collection.class)) {
                // Force CollectionContainerPolicy type to be used with a 
                // collection implementation.
                mapping.setContainerPolicy(new CollectionContainerPolicy(java.util.Vector.class));
            } else {
                // Use the supplied collection class type if its not an interface
                if (mapKey == null || mapKey.equals("")){
                    mapping.useCollectionClassName(rawClass.getName());
                } else {
                    mapping.useMapClassName(rawClass.getName(), mapKey);
                }
            }
        }
    }
    
    /**
     * INTERNAL:
     * This will do two things:
     * 1 - process any common level metadata for all mappings.
     * 2 - add the mapping to the internal descriptor.
     * 3 - store the actual database mapping associated with this accessor.
     * 
     * Calling this method is a must for all mapping accessors since it will 
     * help to:
     * 1 - determine if the accessor has been processed, and
     * 2 - sub processing will may need access to the mapping to set its 
     *     metadata.
     */
    protected void setMapping(DatabaseMapping mapping) {
        // Before adding the mapping to the descriptor, process the properties
        // for this mapping (if any)
        processProperties(mapping);
        
        // Add the mapping to the class descriptor.
        getDescriptor().getClassDescriptor().addMapping(mapping);
        
        // Keep a reference back to this mapping for quick look up.
        m_mapping = mapping;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public String toString() {
        return getAnnotatedElementName();
    }
    
    /**
     * INTERNAL:
     * @see RelationshipAccessor
     * @see DirectAccessor
     */
    protected boolean usesIndirection() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Returns true if this mapping or class uses property access. In an 
     * inheritance hierarchy, the subclasses inherit their access type from 
     * the parent (unless there is an explicit access setting).
     */
    public boolean usesPropertyAccess(MetadataDescriptor descriptor) {
        if (hasAccess()) {
            return hasPropertyAccess();
        } else {
            return (m_accessMethods == null) ? m_classAccessor.usesPropertyAccess() : true;
        }
    }
}
