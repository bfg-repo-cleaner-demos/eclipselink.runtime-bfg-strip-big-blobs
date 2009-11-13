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
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     06/02/2009-2.0 Guy Pelletier 
 *       - 278768: JPA 2.0 Association Override Join Table
 *     09/29/2009-2.0 Guy Pelletier 
 *       - 282553: JPA 2.0 JoinTable support for OneToOne and ManyToOne
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.mappings;

import org.eclipse.persistence.annotations.CollectionTable;
import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.helper.DatabaseField;

import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;

import org.eclipse.persistence.internal.jpa.metadata.columns.ColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.PrimaryKeyJoinColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.PrimaryKeyJoinColumnsMetadata;

import org.eclipse.persistence.internal.jpa.metadata.tables.CollectionTableMetadata;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;

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
    public BasicCollectionAccessor(MetadataAnnotation basicCollection, MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
        super(basicCollection, accessibleObject, classAccessor);

        // Must check, BasicMapAccessor calls this constructor ...
        if (basicCollection != null) {
            m_valueColumn = new ColumnMetadata((MetadataAnnotation) basicCollection.getAttribute("valueColumn"), accessibleObject);
        }
        
        // Set the collection table if one is present.
        if (isAnnotationPresent(CollectionTable.class)) {
            setCollectionTable(new CollectionTableMetadata(getAnnotation(CollectionTable.class), accessibleObject, false));
        }
    }
    
    /**
     * INTERNAL:
     * Method ignores logging context. Can't be anything but the value 
     * column for a BasicCollection annotation. Used with the BasicMap 
     * annotation however.
     */
    @Override
    protected ColumnMetadata getColumn(String loggingCtx) {
        if (loggingCtx.equals(MetadataLogger.VALUE_COLUMN)) {
            return m_valueColumn == null ? super.getColumn(loggingCtx) : m_valueColumn;
        } else {
            return super.getColumn(loggingCtx);
        }
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
    public void initXMLObject(MetadataAccessibleObject accessibleObject, XMLEntityMappings entityMappings) {
        super.initXMLObject(accessibleObject, entityMappings);
    
        // Initialize single ORMetadata objects.
        initXMLObject(m_valueColumn, accessibleObject);
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
        super.processCollectionTable(mapping);
        
        // Add all the primaryKeyJoinColumns (reference key fields) to the 
        // mapping. Primary key join column validation is performed in the
        // processPrimaryKeyJoinColumns call.
        for (PrimaryKeyJoinColumnMetadata primaryKeyJoinColumn : processPrimaryKeyJoinColumns(new PrimaryKeyJoinColumnsMetadata(getCollectionTable().getPrimaryKeyJoinColumns()))) {
            // The default name is the primary key of the owning entity.
            DatabaseField pkField = primaryKeyJoinColumn.getPrimaryKeyField();
            setFieldName(pkField, getOwningDescriptor().getPrimaryKeyFieldName(), MetadataLogger.PK_COLUMN);
            pkField.setTable(getDescriptor().getPrimaryTable());
            
            // The default name is the primary key of the owning entity.
            DatabaseField fkField = primaryKeyJoinColumn.getForeignKeyField();
            setFieldName(fkField, getOwningDescriptor().getPrimaryKeyFieldName(), MetadataLogger.FK_COLUMN);
            fkField.setTable(getReferenceDatabaseTable());
            
            // Add the reference key field for the direct collection mapping.
            ((DirectCollectionMapping) mapping).addReferenceKeyField(fkField, pkField);
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    protected void setValueColumn(ColumnMetadata valueColumn) {
        m_valueColumn = valueColumn;
    }
}
