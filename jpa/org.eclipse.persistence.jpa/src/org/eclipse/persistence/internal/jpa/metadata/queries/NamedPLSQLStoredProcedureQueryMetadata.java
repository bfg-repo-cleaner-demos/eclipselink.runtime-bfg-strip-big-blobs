/*******************************************************************************
 * Copyright (c) 1998, 2012 Oracle and/or its affiliates. All rights reserved.
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
 *     03/24/2011-2.3 Guy Pelletier 
 *       - 337323: Multi-tenant with shared schema support (part 1)
 *     02/08/2012-2.4 Guy Pelletier 
 *       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
 *     06/20/2012-2.5 Guy Pelletier 
 *       - 350487: JPA 2.1 Specification defined support for Stored Procedure Calls
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.queries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.persistence.internal.jpa.StoredProcedureQueryImpl;
import org.eclipse.persistence.internal.jpa.metadata.accessors.MetadataAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.platform.database.oracle.plsql.PLSQLStoredProcedureCall;

/**
 * INTERNAL:
 * Object to hold onto a named PLSQL stored procedure query.
 * 
 * Key notes:
 * - any metadata mapped from XML to this class must be compared in the
 *   equals method.
 * - all metadata mapped from XML should be initialized in the initXMLObject 
 *   method.
 * - when loading from annotations, the constructor accepts the metadata
 *   accessor this metadata was loaded from. Used it to look up any 
 *   'companion' annotation needed for processing.
 * - methods should be preserved in alphabetical order.
 * 
 * @author James Sutherland
 * @since EclipseLink 2.3
 */
public class NamedPLSQLStoredProcedureQueryMetadata extends NamedNativeQueryMetadata {

    private List<PLSQLParameterMetadata> m_parameters = new ArrayList<PLSQLParameterMetadata>();
    private String m_procedureName;
    
    /**
     * INTERNAL:
     * Used for XML loading.
     */
    public NamedPLSQLStoredProcedureQueryMetadata() {
        super("<named-plsql-stored-procedure-query>");
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public NamedPLSQLStoredProcedureQueryMetadata(String elementName) {
        super(elementName);
    }
    
    /**
     * INTERNAL:
     */
    public NamedPLSQLStoredProcedureQueryMetadata(MetadataAnnotation namedStoredProcedureQuery, MetadataAccessor accessor) {
        super(namedStoredProcedureQuery, accessor);
         
        for (Object storedProcedureParameter : (Object[]) namedStoredProcedureQuery.getAttributeArray("parameters")) {
           m_parameters.add(new PLSQLParameterMetadata((MetadataAnnotation)storedProcedureParameter, accessor));
        }
        
        m_procedureName = (String) namedStoredProcedureQuery.getAttribute("procedureName");
    }
   
    /**
     * INTERNAL:
     */
    @Override
    public boolean equals(Object objectToCompare) {
        if (super.equals(objectToCompare) && objectToCompare instanceof NamedPLSQLStoredProcedureQueryMetadata) {
            NamedPLSQLStoredProcedureQueryMetadata query = (NamedPLSQLStoredProcedureQueryMetadata) objectToCompare;
                        
            if (! valuesMatch(m_parameters, query.getParameters())) {
                return false;
            }
            
            return valuesMatch(m_procedureName, query.getProcedureName());
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getProcedureName() {
        return m_procedureName;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<PLSQLParameterMetadata> getParameters() {
        return m_parameters;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject, XMLEntityMappings entityMappings) {
        super.initXMLObject(accessibleObject, entityMappings);
        
        // Initialize parameters ...
        initXMLObjects(m_parameters, accessibleObject);
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void process(AbstractSession session) {
        // Build the stored procedure call.
        PLSQLStoredProcedureCall call = new PLSQLStoredProcedureCall();
        
        // Process the stored procedure parameters.
        for (PLSQLParameterMetadata parameter : m_parameters) {
            parameter.process(call, false);
        }
        
        // Process the procedure name.
        call.setProcedureName(m_procedureName);
                
        // Process the query hints.
        Map<String, Object> hints = processQueryHints(session);
        
        // Process the result class.
        if (getResultClass().isVoid()) {
            if (hasResultSetMapping(session)) {
                session.addQuery(getName(), StoredProcedureQueryImpl.buildStoredProcedureQuery(getResultSetMapping(), call, hints, getLoader(), session));
            } else {
                // Neither a resultClass or resultSetMapping is specified so place in a temp query on the session
                session.addQuery(getName(), StoredProcedureQueryImpl.buildStoredProcedureQuery(call, hints, getLoader(), session));
            }
        } else {
            session.addQuery(getName(), StoredProcedureQueryImpl.buildStoredProcedureQuery(getJavaClass(getResultClass()), call, hints, getLoader(), session));
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setProcedureName(String procedureName) {
        m_procedureName = procedureName;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setParameters(List<PLSQLParameterMetadata> parameters) {
        m_parameters = parameters;
    }
}
