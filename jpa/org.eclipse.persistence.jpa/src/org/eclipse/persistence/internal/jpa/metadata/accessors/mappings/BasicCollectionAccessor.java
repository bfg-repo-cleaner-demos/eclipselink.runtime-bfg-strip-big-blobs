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
 *     06/20/2008-1.0 Guy Pelletier 
 *       - 232975: Failure when attribute type is generic
 *     01/28/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 1)
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.mappings;

import java.lang.annotation.Annotation;

import org.eclipse.persistence.annotations.CollectionTable;
import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;

import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;

import org.eclipse.persistence.internal.jpa.metadata.columns.ColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.PrimaryKeyJoinColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.PrimaryKeyJoinColumnsMetadata;

import org.eclipse.persistence.internal.jpa.metadata.tables.CollectionTableMetadata;

import org.eclipse.persistence.mappings.CollectionMapping;
import org.eclipse.persistence.mappings.DirectCollectionMapping;

/**
 * INTERNAL:
 * A basic collection accessor.
 * 
 * @author Guy Pelletier
 * @since TopLink 11g
 */
public class BasicCollectionAccessor extends DirectCollectionAccessor {
    private ColumnMetadata m_valueColumn;
    private CollectionTableMetadata m_collectionTable;
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public BasicCollectionAccessor() {
        super("<basic-collection>");
    }
    
    /**
     * INTERNAL:
     */
    protected BasicCollectionAccessor(String xmlElement) {
        super(xmlElement);
    }
    
    /**
     * INTERNAL:
     */
    public BasicCollectionAccessor(Annotation basicCollection, MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
        super(basicCollection, accessibleObject, classAccessor);

        // Must check, BasicMapAccessor calls this constructor ...
        if (basicCollection != null) {
            m_valueColumn = new ColumnMetadata((Annotation) MetadataHelper.invokeMethod("valueColumn", basicCollection), accessibleObject, getAttributeName());
        }
        
        // Set the collection table if one is present.
        if (isAnnotationPresent(CollectionTable.class)) {
            m_collectionTable = new CollectionTableMetadata(getAnnotation(CollectionTable.class), accessibleObject, false);
        }
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    protected CollectionTableMetadata getCollectionTable() {
        return m_collectionTable;
    }
    
    /**
     * INTERNAL:
     * Method ignores logging context. Can't be anything but the value 
     * column for a BasicCollection annotation. Used with the BasicMap 
     * annotation however.
     */
    @Override
    protected ColumnMetadata getColumn(String loggingCtx) {
        return (m_valueColumn == null) ? new ColumnMetadata(getAccessibleObject(), getAttributeName()) : m_valueColumn;  
    }
    
    /**
     * INTERNAL:
     * Process column metadata details and resolve any generic specifications.
     */
    @Override
    protected DatabaseField getDatabaseField(DatabaseTable defaultTable, String loggingCtx) {
        DatabaseField field = super.getDatabaseField(defaultTable, loggingCtx);
        
        // To correctly resolve the generics at runtime, we need to set the 
        // field type.
        if (getAccessibleObject().isGenericCollectionType()) {
            field.setType(getReferenceClass());
        }
                    
        return field;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    protected String getDefaultCollectionTableName() {
        if (m_valueColumn != null && m_valueColumn.getTable() != null && !m_valueColumn.getTable().equals("")) {
            return m_valueColumn.getTable();
        } else {
            return super.getDefaultCollectionTableName();
        }
    }
    
    /**
     * INTERNAL:
     * A basic collection can not return a key converter value so return
     * null in this case.
     */
    @Override
    protected String getKeyConverter() {
        return null;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public ColumnMetadata getValueColumn() {
        return m_valueColumn;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject) {
        super.initXMLObject(accessibleObject);
    
        // Initialize single ORMetadata objects.
        initXMLObject(m_valueColumn, accessibleObject);
        
        // Initialize lists of ORMetadata objects.
        initXMLObject(m_collectionTable, accessibleObject);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a basic collection mapping.
     */
    @Override
    public boolean isBasicCollection() {
        return true;
    }
    
    /**
     * INTERNAL:
     */
    public void process() {
        if (isValidDirectCollectionType()) {
            processDirectCollectionMapping();
        } else {
            throw ValidationException.invalidTypeForBasicCollectionAttribute(getAttributeName(), getRawClass(), getJavaClass());
        }
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
        
        // Set the reference table on the mapping.
        ((DirectCollectionMapping) mapping).setReferenceTable(m_collectionTable.getDatabaseTable());
        
        // Add all the primaryKeyJoinColumns (reference key fields) to the 
        // mapping. Primary key join column validation is performed in the
        // processPrimaryKeyJoinColumns call.
        for (PrimaryKeyJoinColumnMetadata primaryKeyJoinColumn : processPrimaryKeyJoinColumns(new PrimaryKeyJoinColumnsMetadata(m_collectionTable.getPrimaryKeyJoinColumns()))) {
            // The default name is the primary key of the owning entity.
            DatabaseField pkField = primaryKeyJoinColumn.getPrimaryKeyField();
            pkField.setName(getName(pkField, getOwningDescriptor().getPrimaryKeyFieldName(), MetadataLogger.PK_COLUMN));
            pkField.setTable(getDescriptor().getPrimaryTable());
            
            // The default name is the primary key of the owning entity.
            DatabaseField fkField = primaryKeyJoinColumn.getForeignKeyField();
            fkField.setName(getName(fkField, getOwningDescriptor().getPrimaryKeyFieldName(), MetadataLogger.FK_COLUMN));
            fkField.setTable(m_collectionTable.getDatabaseTable());
            
            // Add the reference key field for the direct collection mapping.
            ((DirectCollectionMapping) mapping).addReferenceKeyField(fkField, pkField);
        }
    }
    
    /**
     * INTERNAL: 
     * Used for OX mapping.
     */
    protected void setCollectionTable(CollectionTableMetadata collectionTable) {
        m_collectionTable = collectionTable;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    protected void setValueColumn(ColumnMetadata valueColumn) {
        m_valueColumn = valueColumn;
    }
}
