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
 *     06/09/2009-2.0 Guy Pelletier 
 *       - 249037: JPA 2.0 persisting list item index
 ******************************************************************************/ 
package org.eclipse.persistence.internal.jpa.metadata.columns;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;

/**
 * INTERNAL:
 * Object to hold onto relation (fk and pk) column metadata in a Eclipselink
 * database field.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 1.2
 */
public class DirectColumnMetadata extends MetadataColumn {
    private Boolean m_nullable;
    private Boolean m_updatable;
    private Boolean m_insertable;
    
    /**
     * INTERNAL:
     */
    public DirectColumnMetadata(MetadataAnnotation directColumn, MetadataAccessibleObject accessibleObject) {
        super(directColumn, accessibleObject);
        
        if (directColumn != null) {
            m_nullable = (Boolean) directColumn.getAttribute("nullable");
            m_updatable = (Boolean) directColumn.getAttribute("updatable");
            m_insertable = (Boolean) directColumn.getAttribute("insertable");
        }
    }

    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    protected DirectColumnMetadata(String xmlElement) {
        super(xmlElement);
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public boolean equals(Object objectToCompare) {
        if (super.equals(objectToCompare) && objectToCompare instanceof DirectColumnMetadata) {
            DirectColumnMetadata directColumn = (DirectColumnMetadata) objectToCompare;
            
            if (! valuesMatch(m_nullable, directColumn.getNullable())) {
                return false;
            }
            
            if (! valuesMatch(m_updatable, directColumn.getUpdatable())) {
                return false;
            }
            
            return valuesMatch(m_insertable, directColumn.getInsertable());
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public DatabaseField getDatabaseField() {
        DatabaseField databaseField = super.getDatabaseField();
            
        databaseField.setNullable(m_nullable == null ? true : m_nullable.booleanValue());
        databaseField.setUpdatable(m_updatable == null ? true : m_updatable.booleanValue());
        databaseField.setInsertable(m_insertable == null ? true : m_insertable.booleanValue());
        
        return databaseField;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Boolean getInsertable() {
        return m_insertable;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Boolean getNullable() {
        return m_nullable;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Boolean getUpdatable() {
        return m_updatable;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setInsertable(Boolean insertable) {
        m_insertable = insertable;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setNullable(Boolean nullable) {
        m_nullable = nullable;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setUpdatable(Boolean updatable) {
        m_updatable = updatable;
    }
}
