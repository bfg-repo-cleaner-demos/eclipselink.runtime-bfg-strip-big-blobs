/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors;

import java.util.Map;
import java.util.List;
import java.util.StringTokenizer;

import javax.persistence.FetchType;
import javax.persistence.JoinTable;
import javax.persistence.MapKey;
import javax.persistence.OrderBy;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.helper.DatabaseField;

import org.eclipse.persistence.internal.jpa.metadata.accessors.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;

import org.eclipse.persistence.internal.jpa.metadata.columns.JoinColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.JoinColumnsMetadata;

import org.eclipse.persistence.internal.jpa.metadata.MetadataConstants;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataHelper;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;

import org.eclipse.persistence.internal.jpa.metadata.tables.JoinTableMetadata;

import org.eclipse.persistence.mappings.CollectionMapping;
import org.eclipse.persistence.mappings.ManyToManyMapping;

/**
 * An annotation defined relational collections accessor.
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public abstract class CollectionAccessor extends RelationshipAccessor {
	private String m_mapKey;
	private String m_orderBy;
	private JoinTableMetadata m_joinTable;
	
	/**
     * INTERNAL:
     */
    protected CollectionAccessor() {}
    
	/**
     * INTERNAL:
     */
    protected CollectionAccessor(MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
        super(accessibleObject, classAccessor);
    }
    
    /**
     * INTERNAL:
     * 
     * Add the relation key fields to a many to many mapping.
     */
    protected void addManyToManyRelationKeyFields(JoinColumnsMetadata joinColumns, ManyToManyMapping mapping, String defaultFieldName, MetadataDescriptor descriptor, boolean isSource) {
        // Set the right context level.
        String PK_CTX, FK_CTX;
        if (isSource) {
            PK_CTX = MetadataLogger.SOURCE_PK_COLUMN;
            FK_CTX = MetadataLogger.SOURCE_FK_COLUMN;
        } else {
            PK_CTX = MetadataLogger.TARGET_PK_COLUMN;
            FK_CTX = MetadataLogger.TARGET_FK_COLUMN;
        }
        
        for (JoinColumnMetadata joinColumn : processJoinColumns(joinColumns, descriptor)) {
            // If the pk field (referencedColumnName) is not specified, it 
            // defaults to the primary key of the referenced table.
            String defaultPKFieldName = descriptor.getPrimaryKeyFieldName();
            DatabaseField pkField = joinColumn.getPrimaryKeyField();
            pkField.setName(getName(pkField, defaultPKFieldName, PK_CTX));
            pkField.setTable(descriptor.getPrimaryKeyTable());
            
            // If the fk field (name) is not specified, it defaults to the 
            // name of the referencing relationship property or field of the 
            // referencing entity + "_" + the name of the referenced primary 
            // key column. If there is no such referencing relationship 
            // property or field in the entity (i.e., a join table is used), 
            // the join column name is formed as the concatenation of the 
            // following: the name of the entity + "_" + the name of the 
            // referenced primary key column.
            DatabaseField fkField = joinColumn.getForeignKeyField();
            String defaultFKFieldName = defaultFieldName + "_" + defaultPKFieldName;
            fkField.setName(getName(fkField, defaultFKFieldName, FK_CTX));
            // Target table name here is the join table name.
            // If the user had specified a different table name in the join
            // column, it is igored. Perhaps an error or warning should be
            // fired off.
            fkField.setTable(mapping.getRelationTable());
            
            // Add a target relation key to the mapping.
            if (isSource) {
                mapping.addSourceRelationKeyField(fkField, pkField);
            } else {
                mapping.addTargetRelationKeyField(fkField, pkField);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Return the default fetch type for a collection mapping.
     */
    public FetchType getDefaultFetchType() {
    	return FetchType.LAZY;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public JoinTableMetadata getJoinTable() {
    	return m_joinTable;
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
     * Used for OX mapping.
     */
    public String getOrderBy() {
    	return m_orderBy; 
    }
    
    /**
     * INTERNAL: (Override from MetadataAccessor)
     * If a targetEntity is specified in metadata, it will be set as the 
     * reference class, otherwise we will look to extract one from generics.
     */
    public Class getReferenceClass() {
        if (m_referenceClass == null) {
            m_referenceClass = getTargetEntity();
        
            if (m_referenceClass == void.class) {
                // This call will attempt to extract the reference class from generics.
                m_referenceClass = getReferenceClassFromGeneric();
        
                if (m_referenceClass == null) {
                    // Throw an exception. A relationship accessor must have a 
                    // reference class either through generics or a specified
                    // target entity on the mapping metadata.
                	throw ValidationException.unableToDetermineTargetEntity(getAttributeName(), getJavaClass());
                } else {
                    // Log the defaulting contextual reference class.
                    getLogger().logConfigMessage(getLoggingContext(), getAnnotatedElement(), m_referenceClass);
                }
            } 
        }
        
        return m_referenceClass;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor uses a Map.
     */
    public boolean isMapCollectionAccessor() {
        return getRawClass().equals(Map.class);
    }
    
    /**
     * INTERNAL:
     * This process should do any common validation processing of collection 
     * accessors.
     */
    public void process() {
    	// Validate the collection type.
		if (! MetadataHelper.isSupportedCollectionClass(getRawClass())) {
			throw ValidationException.invalidCollectionTypeForRelationship(getJavaClass(), getRawClass(), getAttributeName());
		}
    }
    
    /**
     * INTERNAL:
     */
    protected void process(CollectionMapping mapping) {
        mapping.setIsReadOnly(false);
        mapping.setIsPrivateOwned(hasPrivateOwned());
        mapping.setJoinFetch(getMappingJoinFetchType());
        mapping.setAttributeName(getAttributeName());
        mapping.setReferenceClassName(getReferenceClassName());
        
        // Will check for PROPERTY access
        setAccessorMethods(mapping);

        // Process the cascade types.
        processCascadeTypes(mapping);
        
        // Process an OrderBy if there is one.
        processOrderBy(mapping);
        
        // Process a MapKey if there is one.
        String mapKey = processMapKey(mapping);
        
        // Set the correct indirection on the collection mapping.
        // ** Note the reference class or reference class name needs to be set 
        // on the mapping before setting the indirection policy.
        setIndirectionPolicy(mapping, mapKey, usesIndirection());
        
        // Process a @ReturnInsert and @ReturnUpdate (to log a warning message)
        processReturnInsertAndUpdate();
    }
    
    /**
     * INTERNAL:
     * Process a MetadataJoinTable.
     */
    protected void processJoinTable(ManyToManyMapping mapping) {
    	JoinTableMetadata joinTable;
       	
    	if (m_joinTable == null) {
    		// Look for a JoinTable annotation.
       		JoinTable jTable = getAnnotation(JoinTable.class);
            joinTable = new JoinTableMetadata(jTable, getAnnotatedElementName());
    	} else {
    		// Use the join table specified in XML.
    		m_joinTable.setLocation(getAnnotatedElementName());
    		joinTable = m_joinTable;
    	}
       	
        // Build the default table name
        String defaultName = getDescriptor().getPrimaryTableName() + "_" + getReferenceDescriptor().getPrimaryTableName();
        
        // Process any table defaults and log warning messages.
        processTable(joinTable, defaultName);
        
        // Set the table on the mapping.
        mapping.setRelationTable(joinTable.getDatabaseTable());
        
        // Add all the joinColumns (source foreign keys) to the mapping.
        String defaultSourceFieldName;
        if (getReferenceDescriptor().hasBiDirectionalManyToManyAccessorFor(getJavaClassName(), getAttributeName())) {
            defaultSourceFieldName = getReferenceDescriptor().getBiDirectionalManyToManyAccessor(getJavaClassName(), getAttributeName()).getAttributeName();
        } else {
            defaultSourceFieldName = getDescriptor().getAlias();
        }
        addManyToManyRelationKeyFields(new JoinColumnsMetadata(joinTable.getJoinColumns()), mapping, defaultSourceFieldName, getDescriptor(), true);
        
        // Add all the inverseJoinColumns (target foreign keys) to the mapping.
        String defaultTargetFieldName = getAttributeName();
        addManyToManyRelationKeyFields(new JoinColumnsMetadata(joinTable.getInverseJoinColumns()), mapping, defaultTargetFieldName, getReferenceDescriptor(), false);
    }
    
    /**
     * INTERNAL:
     * Process a MapKey for a 1-M or M-M mapping. Will return the map key
     * method name that should be use, null otherwise.
     */
    protected String processMapKey(CollectionMapping mapping) {
        String mapKeyMethod = null;
        
        if (isMapCollectionAccessor()) {
            MetadataDescriptor referenceDescriptor = getReferenceDescriptor();
            String mapKeyValue = "";
            
            if (m_mapKey == null) {
            	// Look for an annotation.
            	if (isAnnotationPresent(MapKey.class)) {
                    MapKey mapKey = getAnnotation(MapKey.class);
                    mapKeyValue = mapKey.name();
                }
            } else {
            	// Use the value specified in XML.
                mapKeyValue = m_mapKey;
            }
            
            if (mapKeyValue.equals("") && referenceDescriptor.hasCompositePrimaryKey()) {
                // No persistent property or field name has been provided, and
                // the reference class has a composite primary key class. Let
                // it fall through to return null for the map key. Internally,
                // TopLink will use an instance of the composite primary key
                // class as the map key.
            } else {
                // A persistent property or field name may have have been 
                // provided. If one has not we will default to the primary
                // key of the reference class. The primary key cannot be 
                // composite at this point.
                String fieldOrPropertyName = getName(mapKeyValue, referenceDescriptor.getIdAttributeName(), getLogger().MAP_KEY_ATTRIBUTE_NAME);
    
                // Look up the referenceAccessor
                MetadataAccessor referenceAccessor = referenceDescriptor.getAccessorFor(fieldOrPropertyName);
        
                if (referenceAccessor == null) {
                	throw ValidationException.couldNotFindMapKey(fieldOrPropertyName, referenceDescriptor.getJavaClass(), mapping);
                }
        
                mapKeyMethod = referenceAccessor.getAccessibleObjectName();
            }
        }
        
        return mapKeyMethod;
    }
    
    /**
     * INTERNAL:
     * Process an order by value (if specified) for the given collection 
     * mapping. Order by specifies the ordering of the elements of a collection 
     * valued association at the point when the association is retrieved.
     * 
     * The syntax of the value ordering element is an orderby_list, as follows:
     * 
     * orderby_list ::= orderby_item [, orderby_item]*
     * orderby_item ::= property_or_field_name [ASC | DESC]
     * 
     * When ASC or DESC is not specified, ASC is assumed.
     * 
     * If the ordering element is not specified, ordering by the primary key
     * of the associated entity is assumed.
     * 
     * The property or field name must correspond to that of a persistent
     * property or field of the associated class. The properties or fields 
     * used in the ordering must correspond to columns for which comparison
     * operators are supported.
     */
    protected void processOrderBy(CollectionMapping mapping) {
        String orderByValue;
        
    	if (m_orderBy == null) {
    		// Look for an OrderBy annotation.
        	OrderBy orderBy = getAnnotation(OrderBy.class);
            orderByValue = (orderBy == null) ? null : orderBy.value();
        } else {
        	// Used the value specified in XML.
            orderByValue = m_orderBy;
        } 
    	
        if (orderByValue != null) {
            MetadataDescriptor referenceDescriptor = getReferenceDescriptor();
            
            if (orderByValue.equals("")) {
                // Default to the primary key field name(s).
                List<String> orderByAttributes = referenceDescriptor.getIdOrderByAttributeNames();
            
                if (referenceDescriptor.hasEmbeddedIdAttribute()) {
                    String embeddedIdAttributeName = referenceDescriptor.getEmbeddedIdAttributeName();
                
                    for (String orderByAttribute : orderByAttributes) {
                        mapping.addAggregateOrderBy(embeddedIdAttributeName, orderByAttribute, false);
                    }
                } else {
                    for (String orderByAttribute : orderByAttributes) {
                        mapping.addOrderBy(orderByAttribute, false);
                    }
                }
            } else {
                StringTokenizer commaTokenizer = new StringTokenizer(orderByValue, ",");
            
                while (commaTokenizer.hasMoreTokens()) {
                    StringTokenizer spaceTokenizer = new StringTokenizer(commaTokenizer.nextToken());
                    String propertyOrFieldName = spaceTokenizer.nextToken();
                    MetadataAccessor referenceAccessor = referenceDescriptor.getAccessorFor(propertyOrFieldName);
                
                    if (referenceAccessor == null) {
                    	throw ValidationException.invalidOrderByValue(propertyOrFieldName, referenceDescriptor.getJavaClass(), getAccessibleObjectName(), getJavaClass());
                    }

                    String attributeName = referenceAccessor.getAttributeName();                    
                    String ordering = (spaceTokenizer.hasMoreTokens()) ? spaceTokenizer.nextToken() : MetadataConstants.ASCENDING;

                    if (referenceAccessor.isEmbedded()) {
                        for (String orderByAttributeName : referenceDescriptor.getOrderByAttributeNames()) {
                            mapping.addAggregateOrderBy(attributeName, orderByAttributeName, ordering.equals(MetadataConstants.DESCENDING));        
                        }
                    } else {
                        mapping.addOrderBy(attributeName, ordering.equals(MetadataConstants.DESCENDING));    
                    }
                }
            }
        }
    } 
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setJoinTable(JoinTableMetadata joinTable) {
    	m_joinTable = joinTable;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setMapKey(String mapKey) {
    	m_mapKey = mapKey;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setOrderBy(String orderBy) {
    	m_orderBy = orderBy;
    }
}
