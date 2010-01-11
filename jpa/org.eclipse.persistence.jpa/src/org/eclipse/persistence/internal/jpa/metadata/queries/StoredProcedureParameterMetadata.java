/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
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
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.queries;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.persistence.annotations.Direction;
import org.eclipse.persistence.internal.jpa.metadata.ORMetadata;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.queries.StoredProcedureCall;

/**
 * INTERNAL:
 * Object to hold onto a stored procedure parameter metadata.
 * 
 * @author Guy Pelletier
 * @since TopLink 11g
 */
public class StoredProcedureParameterMetadata extends ORMetadata {
    private Class m_type;
    private Enum m_direction;
    private Integer m_jdbcType;
    private String m_jdbcTypeName;
    private String m_name;
    private String m_queryParameter;
    private String m_typeName;
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public StoredProcedureParameterMetadata() {
        super("<stored-procedure-parameter>");
    }
    
    /**
     * INTERNAL:
     */
    public StoredProcedureParameterMetadata(Annotation storedProcedureParameter, MetadataAccessibleObject accessibleObject) {
        super(storedProcedureParameter, accessibleObject);
        
        m_direction = (Enum) MetadataHelper.invokeMethod("direction", storedProcedureParameter);
        m_name = (String) MetadataHelper.invokeMethod("name", storedProcedureParameter);
        m_queryParameter = (String) MetadataHelper.invokeMethod("queryParameter", storedProcedureParameter); 
        m_type = (Class) MetadataHelper.invokeMethod("type", storedProcedureParameter);
        m_jdbcType = (Integer) MetadataHelper.invokeMethod("jdbcType", storedProcedureParameter);
        m_jdbcTypeName = (String) MetadataHelper.invokeMethod("jdbcTypeName", storedProcedureParameter);
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public boolean equals(Object objectToCompare) {
        if (objectToCompare instanceof StoredProcedureParameterMetadata) {
            StoredProcedureParameterMetadata parameter = (StoredProcedureParameterMetadata) objectToCompare;
            
            if (! valuesMatch(m_type, parameter.getType())) {
                return false;
            }
            
            if (! valuesMatch(m_direction, parameter.getDirection())) {
                return false;
            }
            
            if (! valuesMatch(m_jdbcType, parameter.getJdbcType())) {
                return false;
            }

            if (! valuesMatch(m_jdbcTypeName, parameter.getJdbcTypeName())) {
                return false;
            }
            
            if (! valuesMatch(m_name, parameter.getName())) {
                return false;
            }
            
            return valuesMatch(m_queryParameter, parameter.getQueryParameter());
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Enum getDirection() {
        return m_direction;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Integer getJdbcType() {
        return m_jdbcType;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getJdbcTypeName() {
        return m_jdbcTypeName;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getName() {
        return m_name;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getQueryParameter() {
        return m_queryParameter;
    }
    
    /**
     * INTERNAL:
     */
    public Class getType() {
        return m_type;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getTypeName() {
        return m_typeName;
    }
    
    /**
     * INTERNAL:
     */
    protected boolean hasJdbcType() {
        return m_jdbcType != null && m_jdbcType.equals(-1);
    }
    
    /**
     * INTERNAL:
     */
    protected boolean hasJdbcTypeName() {
        return m_jdbcTypeName != null && ! m_jdbcTypeName.equals("");
    }
    
    /**
     * INTERNAL:
     */
    protected boolean hasType() {
        return m_type != void.class;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject) {
        super.initXMLObject(accessibleObject);
    
        m_type = initXMLClassName(m_typeName);
    }
    
    /**
     * INTERNAL:
     */
    public List<String> process(StoredProcedureCall call) {
        List<String> queryArguments = new ArrayList<String>();
                    
        // Process the procedure parameter name, defaults to the 
        // argument field name.
        // TODO: Log a message when defaulting.
        String procedureParameterName = m_name.equals("") ? m_queryParameter : m_name;
                        
        // Process the parameter direction
        if (m_direction == null || m_direction.name().equals(Direction.IN.name())) {
            // TODO: Log a defaulting message if m_direction is null.
            if (hasType()) {
                call.addNamedArgument(procedureParameterName, m_queryParameter, m_type);
            } else if (hasJdbcType() && hasJdbcTypeName()) {
                call.addNamedArgument(procedureParameterName, m_queryParameter, m_jdbcType, m_jdbcTypeName);
            } else if (hasJdbcType()) {
                call.addNamedArgument(procedureParameterName, m_queryParameter, m_jdbcType);
            } else {
                call.addNamedArgument(procedureParameterName, m_queryParameter);
            }
                            
            queryArguments.add(m_queryParameter);
        } else if (m_direction.name().equals(Direction.OUT.name())) {
            if (hasType()) {
                call.addNamedOutputArgument(procedureParameterName, m_queryParameter, m_type);
            } else if (hasJdbcType() && hasJdbcTypeName()) {
                call.addNamedOutputArgument(procedureParameterName, m_queryParameter, m_jdbcType, m_jdbcTypeName);
            } else if (hasJdbcType()) {
                call.addNamedOutputArgument(procedureParameterName, m_queryParameter, m_jdbcType);
            } else {
                call.addNamedOutputArgument(procedureParameterName, m_queryParameter);
            }
        } else if (m_direction.name().equals(Direction.IN_OUT.name())) {
            if (hasType()) {
                call.addNamedInOutputArgument(procedureParameterName, m_queryParameter, m_queryParameter, m_type);
            } else if (hasJdbcType() && hasJdbcTypeName()) {
                call.addNamedInOutputArgument(procedureParameterName, m_queryParameter, m_queryParameter, m_jdbcType, m_jdbcTypeName);
            } else if (hasJdbcType()) {
                call.addNamedInOutputArgument(procedureParameterName, m_queryParameter, m_queryParameter, m_jdbcType);
            } else {
                call.addNamedInOutputArgument(procedureParameterName, m_queryParameter);
            }
                            
            queryArguments.add(m_queryParameter);
        } else if (m_direction.name().equals(Direction.OUT_CURSOR.name())) {
            call.useNamedCursorOutputAsResultSet(m_queryParameter);
        }
      
        return queryArguments;    
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setDirection(Enum direction) {
        m_direction = direction;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setJdbcType(Integer jdbcType) {
        m_jdbcType = jdbcType;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setJdbcTypeName(String jdbcTypeName) {
        m_jdbcTypeName = jdbcTypeName;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setName(String name) {
        m_name = name;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setQueryParameter(String queryParameter) {
        m_queryParameter = queryParameter;
    }
    
    /**
     * INTERNAL:
     */
    public void setType(Class type) {
        m_type = type;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setTypeName(String typeName) {
        m_typeName = typeName;
    }
}
