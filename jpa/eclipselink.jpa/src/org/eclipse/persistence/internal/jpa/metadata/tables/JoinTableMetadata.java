/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.tables;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.columns.JoinColumnMetadata;

/**
 * INTERNAL:
 * Object to hold onto table metadata in a TopLink database table.
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public class JoinTableMetadata extends TableMetadata {
	private List<JoinColumnMetadata> m_joinColumns;
	private List<JoinColumnMetadata> m_inverseJoinColumns;
    
    /**
     * INTERNAL:
     */
    public JoinTableMetadata() {}
    
    /**
     * INTERNAL:
     */
    public JoinTableMetadata(JoinTable joinTable, String annotatedElementName) {
    	super(annotatedElementName);
    	
        if (joinTable != null) {
            setName(joinTable.name());
            setSchema(joinTable.schema());
            setCatalog(joinTable.catalog());
            setUniqueConstraints(joinTable.uniqueConstraints());
            setJoinColumns(joinTable.joinColumns());
    		setInverseJoinColumns(joinTable.inverseJoinColumns());
        }
    }
    
    /**
     * INTERNAL: (Override from MetadataTable)
     */
    public String getCatalogContext() {
        return MetadataLogger.JOIN_TABLE_CATALOG;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<JoinColumnMetadata> getInverseJoinColumns() {
        return m_inverseJoinColumns;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<JoinColumnMetadata> getJoinColumns() {
        return m_joinColumns;
    }
    
    /**
     * INTERNAL: (Override from MetadataTable)
     */
    public String getNameContext() {
        return MetadataLogger.JOIN_TABLE_NAME;
    }
    
    /**
     * INTERNAL: (Override from MetadataTable)
     */
    public String getSchemaContext() {
        return MetadataLogger.JOIN_TABLE_SCHEMA;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setInverseJoinColumns(List<JoinColumnMetadata> inverseJoinColumns) {
        m_inverseJoinColumns = inverseJoinColumns;
    }
    
    /**
     * INTERNAL:
     * Called from annotation population.
     */
    protected void setInverseJoinColumns(JoinColumn[] inverseJoinColumns) {
    	m_inverseJoinColumns = new ArrayList<JoinColumnMetadata>();
    	
    	for (JoinColumn inverseJoinColumn : inverseJoinColumns) {
    		m_inverseJoinColumns.add(new JoinColumnMetadata(inverseJoinColumn));
    	}   
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setJoinColumns(List<JoinColumnMetadata> joinColumns) {
    	m_joinColumns = joinColumns;
    }
    
    /**
     * INTERNAL:
     * Called from annotation population.
     */
    protected void setJoinColumns(JoinColumn[] joinColumns) {
    	m_joinColumns = new ArrayList<JoinColumnMetadata>();
    	
    	for (JoinColumn joinColumn : joinColumns) {
    		m_joinColumns.add(new JoinColumnMetadata(joinColumn));
    	}   
    }
}
