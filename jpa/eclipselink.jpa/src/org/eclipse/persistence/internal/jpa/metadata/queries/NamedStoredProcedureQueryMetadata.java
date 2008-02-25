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
package org.eclipse.persistence.internal.jpa.metadata.queries;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.persistence.annotations.NamedStoredProcedureQuery;
import org.eclipse.persistence.annotations.StoredProcedureParameter;

/**
 * INTERNAL:
 * Object to hold onto a named stored procedure query.
 * 
 * @author Guy Pelletier
 * @since TopLink 11g
 */
public class NamedStoredProcedureQueryMetadata extends NamedNativeQueryMetadata  {
	private boolean m_returnsResultSet;
	private List<StoredProcedureParameterMetadata> m_procedureParameters;
	private String m_procedureName;
	
    /**
     * INTERNAL:
     */
    public NamedStoredProcedureQueryMetadata() {
    	this.setLoadedFromXML();
    	// Will need an XML context ignore string when XML support is added.
    }
    
    /**
     * INTERNAL:
     */
    public NamedStoredProcedureQueryMetadata(Object namedStoredProcedureQuery, Class javaClass) {
        
    	m_procedureName = (String)MetadataHelper.invokeMethod("procedureName", namedStoredProcedureQuery, (Object[])null);
    	
    	setLoadedFromAnnotation();
    	setLocation(javaClass.getName());
        setName((String)MetadataHelper.invokeMethod("name", namedStoredProcedureQuery, (Object[])null));
        setHints((Object[])MetadataHelper.invokeMethod("hints", namedStoredProcedureQuery, (Object[])null));
        setResultClass((Class)MetadataHelper.invokeMethod("resultClass", namedStoredProcedureQuery, (Object[])null));
        setResultSetMapping((String)MetadataHelper.invokeMethod("resultSetMapping", namedStoredProcedureQuery, (Object[])null));
        setReturnsResultSet((Boolean)MetadataHelper.invokeMethod("returnsResultSet", namedStoredProcedureQuery, (Object[])null));
        setProcedureParameters((Object[])MetadataHelper.invokeMethod("procedureParameters", namedStoredProcedureQuery, (Object[])null));
    }
   
    /**
     * INTERNAL:
     */
    public String getProcedureName() {
        return m_procedureName;
    }
    
    /**
     * INTERNAL:
     */
    public List<StoredProcedureParameterMetadata> getProcedureParameters() {
        return m_procedureParameters;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasProcedureParameters() {
        return ! m_procedureParameters.isEmpty();
    }
    
    /**
     * INTERNAL:
     */
    public boolean returnsResultSet() {
        return m_returnsResultSet;
    }
    
    /**
     * INTERNAL:
     */
    public void setProcedureName(String procedureName) {
        m_procedureName = procedureName;
    }
    
    /**
     * INTERNAL:
     */
    public void setProcedureParameters(Object[] procedureParameters) {
        m_procedureParameters = new ArrayList<StoredProcedureParameterMetadata>();
        for (Object storedProcedureParameter : procedureParameters) {
           m_procedureParameters.add(new StoredProcedureParameterMetadata(storedProcedureParameter));
        }
    }
    
    /**
     * INTERNAL:
     */
    public void setReturnsResultSet(boolean returnsResultSet) {
        m_returnsResultSet = returnsResultSet;
    }
}
