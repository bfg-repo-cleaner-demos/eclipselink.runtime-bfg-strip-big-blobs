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
 *     09/23/2008-1.1 Guy Pelletier 
 *       - 241651: JPA 2.0 Access Type support
 *     01/28/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 1)
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.mappings;

import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.eclipse.persistence.annotations.Convert;

import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.converters.EnumeratedMetadata;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.converters.LobMetadata;
import org.eclipse.persistence.internal.jpa.metadata.converters.TemporalMetadata;

/**
 * A direct accessor.
 * 
 * Subclasses: BasicAccessor, BasicCollectionAccessor, BasicMapAccessor.
 * 
 * @author Guy Pelletier
 * @since TopLink 11g
 */
public abstract class DirectAccessor extends MappingAccessor {
    private Boolean m_optional;
    private String m_fetch;
    private EnumeratedMetadata m_enumerated;
    private LobMetadata m_lob;
    private String m_convert;
    private TemporalMetadata m_temporal;
    
    /**
     * INTERNAL:
     */
    protected DirectAccessor(String xmlElement) {
        super(xmlElement);
    }
    
    /**
     * INTERNAL:
     */
    protected DirectAccessor(MetadataAnnotation annotation, MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
        super(annotation, accessibleObject, classAccessor);
        
        // Set the lob if one is present.
        if (isAnnotationPresent(Lob.class)) {
            m_lob = new LobMetadata(getAnnotation(Lob.class), getAccessibleObject());
        }
        
        // Set the enumerated if one is present.
        if (isAnnotationPresent(Enumerated.class)) {
            m_enumerated = new EnumeratedMetadata(getAnnotation(Enumerated.class), getAccessibleObject());
        }
        
        // Set the temporal type if one is present.
        if (isAnnotationPresent(Temporal.class)) {
            m_temporal = new TemporalMetadata(getAnnotation(Temporal.class), getAccessibleObject());
        }
        
        // Set the convert value if one is present.
        if (isAnnotationPresent(Convert.class)) {
            m_convert = (String) getAnnotation(Convert.class).getAttribute("value");
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getConvert() {
        return m_convert;
    }
    
    /**
     * INTERNAL:
     */
    public abstract String getDefaultFetchType();
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public EnumeratedMetadata getEnumerated() {
        return m_enumerated;
    }
     
    /**
     * INTERNAL:
     * Return the enumerated metadata for this accessor.
     */
    @Override
    public EnumeratedMetadata getEnumerated(boolean isForMapKey) {
        return getEnumerated();
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getFetch() {
        return m_fetch;
    }
    
    /**
     * INTERNAL:
     * Return the field classification for the given temporal type.
     */
    protected Class getFieldClassification(Enum type) {
        if (type.name().equals(TemporalType.DATE.name())){
            return java.sql.Date.class;
        } else if(type.name().equals(TemporalType.TIME.name())){
            return java.sql.Time.class;
        } else if(type.name().equals(TemporalType.TIMESTAMP.name())){
            return java.sql.Timestamp.class;
        } else {
            return null;
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public LobMetadata getLob() {
        return m_lob;
    }
    
    /**
     * INTERNAL:
     * Return the lob metadata for this accessor.
     */
    public LobMetadata getLob(boolean isForMapKey) {
        if (isForMapKey) {
            return super.getLob(isForMapKey);
        } else {
            return getLob();
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Boolean getOptional() {
        return m_optional;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public TemporalMetadata getTemporal() {
        return m_temporal;
    }
    
    /**
     * INTERNAL:
     * Return the temporal metadata for this accessor.
     */
    @Override
    public TemporalMetadata getTemporal(boolean isForMapKey) {
        return getTemporal();
    }
    
    /**
     * INTERNAL:
     */
    @Override
    protected boolean hasConvert(boolean isForMapKey) {
        return m_convert != null;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor has enumerated metadata.
     */
    @Override
    public boolean hasEnumerated(boolean isForMapKey) {
        return m_enumerated != null;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor has lob metadata.
     */
    @Override
    public boolean hasLob(boolean isForMapKey) {
        return m_lob != null;
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor has temporal metadata.
     */
    @Override
    public boolean hasTemporal(boolean isForMapKey) {
        return m_temporal != null;
    }
    
    /**
     * INTERNAL:
     */
    public boolean isOptional() {
        if (m_optional == null) {
            return true;
        } else {
            return m_optional.booleanValue();
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setConvert(String convert) {
        m_convert = convert;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setEnumerated(EnumeratedMetadata enumerated) {
        m_enumerated = enumerated;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setFetch(String fetch) {
        m_fetch = fetch;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setLob(LobMetadata lob) {
        m_lob = lob;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setOptional(Boolean optional) {
        m_optional = optional;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setTemporal(TemporalMetadata temporalType) {
        m_temporal = temporalType;
    }
        
    /**
     * INTERNAL:
     */
    @Override
    protected boolean usesIndirection() {
        String fetchType = getFetch();
        
        if (fetchType == null) {
            fetchType = getDefaultFetchType();
        }
        
        return fetchType.equals(FetchType.LAZY.name());
    }
}
