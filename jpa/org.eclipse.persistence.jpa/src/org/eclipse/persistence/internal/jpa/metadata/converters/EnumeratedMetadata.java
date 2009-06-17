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
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.converters;

import javax.persistence.EnumType;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.EnumTypeConverter;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.MappingAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataClass;

/**
 * INTERNAL:
 * Abstract converter class that parents both the JPA and Eclipselink 
 * converters.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 2.0
 */
public class EnumeratedMetadata extends MetadataConverter {
    private String m_enumerated;
    
    /**
     * INTERNAL:
     * Used for defaulting case.
     */
    public EnumeratedMetadata() {
        m_enumerated = EnumType.ORDINAL.name();
    }
    
    /**
     * INTERNAL:
     */
    public EnumeratedMetadata(String xmlElement) {
        super(xmlElement);
    }
    
    /**
     * Used for defaulting.
     */
    public EnumeratedMetadata(MetadataAccessibleObject accessibleObject) {
        super(accessibleObject);
    }
    
    /**
     * INTERNAL:
     */
    public EnumeratedMetadata(MetadataAnnotation enumerated, MetadataAccessibleObject accessibleObject) {
        super(enumerated, accessibleObject);
        
        m_enumerated = (String) enumerated.getAttribute("value");
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getEnumerated() {
        return m_enumerated;
    }
    
    /**
     * INTERNAL:
     * Return true if the given class is a valid enum type.
     */
    public static boolean isValidEnumeratedType(MetadataClass cls) {
        return cls.isEnum();    
    }
    
    /**
     * INTERNAL:
     * Every converter needs to be able to process themselves.
     */
    public void process(DatabaseMapping mapping, MappingAccessor accessor, MetadataClass referenceClass, boolean isForMapKey) {
        // Create an EnumTypeConverter and set it on the mapping.
        if (! EnumeratedMetadata.isValidEnumeratedType(referenceClass)) {
            throw ValidationException.invalidTypeForEnumeratedAttribute(mapping.getAttributeName(), referenceClass, accessor.getJavaClass());
        }
        boolean isOrdinal = true;
        if (m_enumerated != null) {
            isOrdinal = m_enumerated.equals(EnumType.ORDINAL.name());
        }
        setConverter(mapping, new EnumTypeConverter(mapping, getJavaClass(referenceClass), isOrdinal), isForMapKey);
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setEnumerated(String enumerated) {
        m_enumerated = enumerated;
    }
}
