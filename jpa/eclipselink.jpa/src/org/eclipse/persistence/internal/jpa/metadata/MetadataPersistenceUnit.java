/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata;

import org.eclipse.persistence.internal.jpa.metadata.xml.XMLConstants;

/**
 * Metadata object to hold persistence unit information.
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public class MetadataPersistenceUnit  {
    protected String m_access;
    protected String m_schema;
    protected String m_catalog;
    protected String m_conflict;
    protected boolean m_isCascadePersist;
    protected boolean m_isMetadataComplete;
    
    /**
     * INTERNAL:
     */
    public MetadataPersistenceUnit() {
        m_access = "";
        m_schema = "";
        m_catalog = "";
        m_isCascadePersist = false;
        m_isMetadataComplete = false;
    }
    
    /**
     * INTERNAL:
     * If equals returns false, call getConflict() for a finer grain reason why.
     */
    public boolean equals(Object objectToCompare) {
        MetadataPersistenceUnit persistenceUnit = (MetadataPersistenceUnit) objectToCompare;
            
        if (! persistenceUnit.getAccess().equals(getAccess())) {
            m_conflict = XMLConstants.ACCESS;
            return false;
        }
            
        if (! persistenceUnit.getCatalog().equals(getCatalog())) {
            m_conflict = XMLConstants.CATALOG;
            return false;
        }
            
        if (! persistenceUnit.getSchema().equals(getSchema())) {
            m_conflict = XMLConstants.SCHEMA;
            return false;
        }
            
        if (persistenceUnit.isCascadePersist() != isCascadePersist()) {
            m_conflict = XMLConstants.CASCADE_PERSIST;
            return false;
        }
                
        if (persistenceUnit.isMetadataComplete() != isMetadataComplete()) {
            m_conflict = XMLConstants.METADATA_COMPLETE;
            return false;
        }
        
        return true;
    }
    
    /**
     * INTERNAL:
     */
    public String getAccess() {
       return m_access; 
    }
    
    /**
     * INTERNAL:
     */
    public String getCatalog() {
       return m_catalog; 
    }
    
    /**
     * INTERNAL:
     * Calling this method after an equals call that returns false will give
     * you the conflicting metadata.
     */
    public String getConflict() {
       return m_conflict;
    }
    
    /**
     * INTERNAL:
     */
    public String getSchema() {
       return m_schema; 
    }
    
    /**
     * INTERNAL:
     */
    public boolean isCascadePersist() {
        return m_isCascadePersist;
    }
    
    /**
     * INTERNAL:
     */
    public boolean isMetadataComplete() {
        return m_isMetadataComplete;
    }
    
    /**
     * INTERNAL:
     */
    public void setAccess(String access) {
       m_access = access; 
    }
    
    /**
     * INTERNAL:
     */
    public void setCatalog(String catalog) {
       m_catalog = catalog;
    }
    
    /**
     * INTERNAL:
     */
    public void setIsCascadePersist(boolean isCascadePersist) {
        m_isCascadePersist = isCascadePersist;
    }
    
    /**
     * INTERNAL:
     */
    public void setIsMetadataComplete(boolean isMetadataComplete) {
        m_isMetadataComplete = isMetadataComplete;
    }
    
    /**
     * INTERNAL:
     */
    public void setSchema(String schema) {
       m_schema = schema;
    }
}
