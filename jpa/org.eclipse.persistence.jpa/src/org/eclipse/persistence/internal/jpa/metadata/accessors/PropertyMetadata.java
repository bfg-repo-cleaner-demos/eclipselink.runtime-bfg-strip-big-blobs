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
 *     Andrei Ilitchev (Oracle), April 8, 2008 
 *        - New file introduced for bug 217168.
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors;

import java.lang.annotation.Annotation;

import org.eclipse.persistence.internal.helper.ConversionManager;
import org.eclipse.persistence.internal.jpa.metadata.ORMetadata;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;

/**
 * INTERNAL:
 * PropertyMetadata. Each mapping may be assigned user-defined properties.
 * 
 * @author Andrei Ilitchev
 * @since EclipseLink 1.0 
 */
public class PropertyMetadata extends ORMetadata {
    private String m_name;
    private String m_value;
    private Class m_valueType;
    private String m_valueTypeName;
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public PropertyMetadata() {
        super("<property>");
    }
    
    /**
     * INTERNAL:
     */
    public PropertyMetadata(Annotation property, MetadataAccessibleObject accessibleObject) {
        super(property, accessibleObject);
        
        m_name = (String) MetadataHelper.invokeMethod("name", property);
        m_value = (String) MetadataHelper.invokeMethod("value", property);
        m_valueType = (Class) MetadataHelper.invokeMethod("valueType", property);
    }
    
    /**
     * INTERNAL:
     * To satisfy the abstract getIdentifier() method from ORMetadata.
     */
    @Override
    public String getIdentifier() {
        return getName();
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
    public String getValue() {
        return m_value;
    }
    
    /**
     * INTERNAL:
     */
    public Class getValueType() {
        return m_valueType;
    }

    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getValueTypeName() {
        return m_valueTypeName;
    }
    
    /**
     * INTERNAL:
     */
    public Object getConvertedValue() {
        if(m_valueType.equals(void.class) || m_valueType.equals(String.class)) {
            return m_value;
        } else {
            return ConversionManager.getDefaultManager().convertObject(m_value, m_valueType);
        }
    }

    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject) {
        super.initXMLObject(accessibleObject);

        m_valueType = initXMLClassName(m_valueTypeName);
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
    public void setValue(String value) {
        m_value = value;
    }
    
    /**
     * INTERNAL:
     */
    public void setValueType(Class valueType) {
        m_valueType = valueType;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setValueTypeName(String valueTypeName) {
        m_valueTypeName = valueTypeName;
    }
}
