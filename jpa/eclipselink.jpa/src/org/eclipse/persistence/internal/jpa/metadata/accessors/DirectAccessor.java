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
package org.eclipse.persistence.internal.jpa.metadata.accessors;

import java.io.Serializable;

import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.Temporal;

import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.jpa.metadata.MetadataConstants;
import org.eclipse.persistence.internal.jpa.metadata.MetadataHelper;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;

import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.columns.ColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.converters.AbstractConverterMetadata;
import org.eclipse.persistence.internal.jpa.metadata.converters.StructConverterMetadata;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.helper.Helper;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.mappings.converters.EnumTypeConverter;
import org.eclipse.persistence.mappings.converters.SerializedObjectConverter;
import org.eclipse.persistence.mappings.converters.TypeConversionConverter;

/**
 * A direct accessor.
 * 
 * Subclasses: BasicAccessor, BasicCollectionAccessor, BasicMapAccessor.
 * 
 * @author Guy Pelletier
 * @since TopLink 11g
 */
public abstract class DirectAccessor extends NonRelationshipAccessor {
	protected final static String DEFAULT_MAP_KEY_COLUMN_SUFFIX = "_KEY";
	
	private Enum m_fetch;
	private Boolean m_optional;
	private Boolean m_lob;
	private Enum m_enumerated;
	private String m_convert;
	private Enum m_temporal;
	
    /**
     * INTERNAL:
     */
    protected DirectAccessor() {}
    
    /**
     * INTERNAL:
     */
    protected DirectAccessor(MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
    	super(accessibleObject, classAccessor);
    }
    
    /**
     * INTERNAL:
     * This is used to return the column for a BasicAccessor. In the case
     * of a BasicCollectionAccessor or BasicMapAccessor, this method should
     * return the value column. NOTE: In the case of a BasicMapAccessor, the
     * key column could be returned instead.
     */
    protected abstract ColumnMetadata getColumn(String loggingCtx);
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getConvert() {
        return m_convert;
    }
    
    /**
     * INTERNAL:
     * Process column details from an @Column or column element into a 
     * ColumnMetadata and return it. This will set correct metadata and log 
     * defaulting messages to the user. It also looks for attribute overrides.
     * 
     * This method will call getColumn() which assumes the subclasses will
     * return the appropriate ColumnMetadata to process based on the context
     * provided.
     * 
     * @See BasicCollectionAccessor and BasicMapAccessor.
     */
    protected DatabaseField getDatabaseField(DatabaseTable defaultTable, String loggingCtx) {
        // Check if we have an attribute override first, otherwise process for 
        // a column (ignoring if for a key column on a basic map)
        ColumnMetadata column;
        if (getDescriptor().hasAttributeOverrideFor(getAttributeName()) && ! loggingCtx.equals(MetadataLogger.MAP_KEY_COLUMN)) {
            column = getDescriptor().getAttributeOverrideFor(getAttributeName()).getColumn();
        } else {
            column = getColumn(loggingCtx);
        }
        
        // Get the actual database field and apply any defaults.
        DatabaseField field = column.getDatabaseField();
        
        // Make sure there is a table name on the field.
        if (field.getTableName().equals("")) {
        	field.setTable(defaultTable);
        }
        
        // Set the correct field name, defaulting and logging when necessary.
        String defaultName = column.getUpperCaseAttributeName();
        
        // If this is for a map key column, append a suffix.
        if (loggingCtx.equals(MetadataLogger.MAP_KEY_COLUMN)) {
            defaultName += DEFAULT_MAP_KEY_COLUMN_SUFFIX;
        }
        
        field.setName(getName(field.getName(), defaultName, loggingCtx));
                    
        return field;
    }
    
    /**
     * INTERNAL:
     */
    public abstract FetchType getDefaultFetchType();
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Enum getEnumerated() {
    	return m_enumerated;
    }
     
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Enum getFetch() {
    	return m_fetch;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Boolean getLob() {
    	return m_lob;
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
    public Enum getTemporal() {
    	return m_temporal;
    }
    
    /**
     * INTERNAL:
     */
    protected boolean hasConvert() {
    	return m_convert != null || isAnnotationPresent(Convert.class);
    }
    
    /**
     * INTERNAL:
	 * Method to check if this basic accessor has an enumerated sub-element.
     */
	public boolean hasEnumerated() {
		return m_enumerated != null || isAnnotationPresent(Enumerated.class);
    }
	
    
    /**
     * INTERNAL:
     * Return true if this accessor represents an BLOB/CLOB mapping.
     */
    public boolean hasLob() {
    	return m_lob != null || isAnnotationPresent(Lob.class);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor is a temporal.
     */
    public boolean hasTemporal() {
    	return m_temporal != null || isAnnotationPresent(Temporal.class);
    }

    /**
     * INTERNAL:
     * Return true if this represents an enum type mapping. Will return true
     * if the accessor's reference class is an enum or if a @Enumerated exists.
     */
    public boolean isEnumerated() {    	
    	if (hasConvert()) {
    		// If we have an @Enumerated with a @Convert, the @Convert takes
    		// precedence and we will ignore the @Enumerated and log a message.
    		if (hasEnumerated()) {
    			getLogger().logWarningMessage(MetadataLogger.IGNORE_ENUMERATED, getJavaClass(), getAnnotatedElement());
    		}
            
    		return false;
    	} else {
    		return hasEnumerated() || MetadataHelper.isValidEnumeratedType(getReferenceClass());
    	}
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor represents a BLOB/CLOB mapping.
     */
    public boolean isLob() {
    	if (hasConvert()) {
			// If we have a @Lob with a @Convert, the @Convert takes precedence 
    		// and we will ignore the @Lob and log a message.
    		if (hasLob()) {
    			getLogger().logWarningMessage(MetadataLogger.IGNORE_LOB, getJavaClass(), getAnnotatedElement());
    		}
    		
    		return false;
    	} else {
    		return hasLob();
    	}
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
     * Return true if this accessor represents a serialized mapping.
     */
    public boolean isSerialized() {
    	if (hasConvert()) {
    		getLogger().logWarningMessage(MetadataLogger.IGNORE_SERIALIZED, getJavaClass(), getAnnotatedElement());
    		return false;
    	} else {
    		return MetadataHelper.isValidSerializedType(getReferenceClass());
    	}
    }
    
    /**
     * INTERNAL:
     * (Overridden in BasicMapAccessor)
     * Return true if this represents a temporal type mapping. Will return true
     * if the accessor's reference class is a temporal type or if a @Temporal 
     * exists.
     */
    public boolean isTemporal() {
    	if (hasConvert()) {
    		// If we have a Temporal specification with a Convert specification, 
    		// the Convert takes precedence and we will ignore the Temporal and 
    		// log a message.
    		if (hasTemporal()) {
    			getLogger().logWarningMessage(MetadataLogger.IGNORE_TEMPORAL, getJavaClass(), getAnnotatedElement());
    		}
            
    		return false;
    	} else {
    		return hasTemporal() || MetadataHelper.isValidTemporalType(getReferenceClass());
    	}
    }
    
    /**
     * INTERNAL: (Overridden in BasicMapAccessor)
     * Process a convert value to apply the specified EclipseLink converter 
     * (Converter, TypeConverter, ObjectTypeConverter) to the given mapping.
     * 
     * This method is called in second stage processing and should only be
     * called on accessors that have a convert value specified.
     */
    public void processConvert() {
    	Object convert = getAnnotation(Convert.class);
    	String convertValue  = (m_convert == null) ? (String)invokeMethod("value", convert, (Object[])null) : m_convert;
    	processConvert(getDescriptor().getMappingForAttributeName(getAttributeName()), convertValue);
    }
    
    /**
     * INTERNAL:
     * Process a Convert annotation or convert element to apply to specified 
     * EclipseLink converter (Converter, TypeConverter, ObjectTypeConverter) 
     * to the given mapping.
     * 
     * This method is called in second stage processing and should only be
     * called on accessors that have a Convert annotation specified.
     */
    protected void processConvert(DatabaseMapping mapping, String converterName) {
        // There is no work to do if the converter's name is "none".
        if (! converterName.equals(MetadataConstants.CONVERT_NONE)) {
            if (converterName.equals(MetadataConstants.CONVERT_SERIALIZED)) {
                processSerialized(mapping);
            } else {
            	AbstractConverterMetadata converter = getProject().getConverter(converterName);
                
            	if (converter == null) {
                	StructConverterMetadata structConverter = getProject().getStructConverter(converterName);
                    if (structConverter == null) {
                    	throw ValidationException.converterNotFound(getJavaClass(), converterName, getAnnotatedElement());
                    } else {
                        structConverter.process(mapping, this);
                    }
                } else {
                	// Process the converter for this mapping.
                	converter.process(mapping, this);
                }
            }
        }
    }
    
    /**
     * INTERNAL: (Overridden in BasicAccessor and BasicMapAccessor)
     * Process an Enumerated annotation. The method may still be called if no 
     * Enumerated annotation has been specified but the accessor's reference 
     * class is a valid enumerated type.
     */
    protected void processEnumerated(DatabaseMapping mapping) {
        // If this accessor is tagged as an enumerated type, validate the
        // reference class.
        if (hasEnumerated()) {
            if (! MetadataHelper.isValidEnumeratedType(getReferenceClass())) {
            	throw ValidationException.invalidTypeForEnumeratedAttribute(mapping.getAttributeName(), getReferenceClass(), getJavaClass());
            }
        }

        Enum enumType;
        if (m_enumerated == null) {
    		Object enumerated = getAnnotation(Enumerated.class);
            
            if (enumerated == null) {
            	enumType = EnumType.ORDINAL;
            } else {
            	enumType = (Enum)invokeMethod("value", enumerated, (Object[])null); 
            }
    	} else {
    		enumType = m_enumerated;
    	}
        
        // Create an EnumTypeConverter and set it on the mapping.
        setConverter(mapping, new EnumTypeConverter(mapping, getReferenceClass(), enumType.equals(EnumType.ORDINAL)));
    }
    
    /**
     * INTERNAL:
     * 
     * Process an @Enumerated, @Lob or @Temporal annotation. Will default
     * a serialized converter if necessary.
     */
    protected void processJPAConverters(DatabaseMapping mapping) {
        // Check for an enum first since it will fall into a serializable 
        // mapping otherwise (Enums are serialized)
    	
        if (isEnumerated()) {
            processEnumerated(mapping);
        } else if (isLob()) {
            processLob(mapping);
        } else if (isTemporal()) {
            processTemporal(mapping);
        } else if (isSerialized()) {
            processSerialized(mapping);
        }
    }
    
    /**
     * INTERNAL: (Overridden in BasicAccessor)
     * 
     * Process a lob specification. The lob must be specified to process and 
     * create a lob type mapping.
     */
    protected void processLob(DatabaseMapping mapping) {
        // Set the field classification type on the mapping based on the
        // referenceClass type.
        if (MetadataHelper.isValidClobType(getReferenceClass())) {
            setFieldClassification(mapping, java.sql.Clob.class);   
            setConverter(mapping, new TypeConversionConverter(mapping));
        } else if (MetadataHelper.isValidBlobType(getReferenceClass())) {
            setFieldClassification(mapping, java.sql.Blob.class);
            setConverter(mapping, new TypeConversionConverter(mapping));
        } else if (Helper.classImplementsInterface(getReferenceClass(), Serializable.class)) {
            setFieldClassification(mapping, java.sql.Blob.class);
            setConverter(mapping, new SerializedObjectConverter(mapping));
        } else {
            // The referenceClass is neither a valid BLOB or CLOB attribute.   
        	throw ValidationException.invalidTypeForLOBAttribute(mapping.getAttributeName(), getReferenceClass(), getJavaClass());
        }
    }
 
    /**
     * INTERNAL:
     * Process a converter for the given mapping. Will look for a converter
     * name from a convert specification.
     */
    protected void processMappingConverter(DatabaseMapping mapping) {
    	if (m_convert == null) {
            Object convert = getAnnotation(Convert.class);
            
            if (convert == null) {
            	processJPAConverters(mapping);
            } else {
            	processMappingConverter(mapping, (String)invokeMethod("value", convert, (Object[])null)); 
            }
    	} else {
    		processMappingConverter(mapping, m_convert);
    	}
    }
    
    /**
     * INTERNAL:
     * Process a convert value which specifies the name of an EclipseLink
     * converter to process with this accessor's mapping. EclipseLink converters 
     * (which are global to the persistent unit) can not be processed till we 
     * have processed all the classes in the persistence unit. So for now, add 
     * this accessor to the project list of convert dependant accessors, and 
     * process it in stage 2, that is, during the project process.
     * 
     * The method will look for an EclipseLink converter first (based on the 
     * converter name provided) and will override any JPA annotations. Log 
     * warnings will be issued for any annotations that are being ignore 
     * because of a Convert override.     
     */
    protected void processMappingConverter(DatabaseMapping mapping, String convertValue) {
        if (convertValue != null && ! convertValue.equals(MetadataConstants.CONVERT_NONE)) {
            // EclipseLink converter specified, defer this accessors converter
            // processing to stage 2 project processing.
            getProject().addConvertAccessor(this);
        } 
        
        // Regardless if we found a convert or not, look for JPA converters. 
        // This ensures two things; 
        // 1 - if no Convert is specified, then any JPA converter that is 
        // specified will be applied (see BasicMapAccessor's override of the
        // method hasConvert()). 
        // 2 - if a convert and a JPA converter are specified, then a log 
        // warning will be issued stating that we are ignoring the JPA 
        // converter.
        processJPAConverters(mapping);
    }
    
    /**
     * INTERNAL:
     * 
     * Process a potential serializable attribute. If the class implements 
     * the Serializable interface then set a SerializedObjectConverter on 
     * the mapping.
     */
    protected void processSerialized(DatabaseMapping mapping) {
        if (Helper.classImplementsInterface(getReferenceClass(), Serializable.class)) {
            SerializedObjectConverter converter = new SerializedObjectConverter(mapping);
            setConverter(mapping, converter);
        } else {
        	throw ValidationException.invalidTypeForSerializedAttribute(mapping.getAttributeName(), getReferenceClass(), getJavaClass());
        }
    }
    
    /**
     * INTERNAL:
     * Process a temporal type accessor.
     */
    protected void processTemporal(DatabaseMapping mapping) {
    	Enum temporalType = null;
    	
    	if (m_temporal == null) {
        	Object temporal = getAnnotation(Temporal.class);
        	
        	if (temporal != null) {
        	    temporalType=(Enum)invokeMethod("value", temporal, (Object[])null);
        	}
        } else {
        	temporalType = m_temporal;
        }
    	
        if (temporalType == null) {
        	// We have a temporal basic, but the temporal type was not
        	// specified. Per the JPA spec we must throw an exception.
        	throw ValidationException.noTemporalTypeSpecified(getAttributeName(), getJavaClass());
        } else {
            if (MetadataHelper.isValidTemporalType(getReferenceClass())) {
                // Set a TypeConversionConverter on the mapping.
                setFieldClassification(mapping, MetadataHelper.getFieldClassification(temporalType));
                setConverter(mapping, new TypeConversionConverter(mapping));
            } else {
            	throw ValidationException.invalidTypeForTemporalAttribute(getAttributeName(), getReferenceClass(), getJavaClass());
            }    
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
     */
    public abstract void setConverter(DatabaseMapping mapping, Converter converter);
    
    /**
     * INTERNAL:
     */
    public abstract void setConverterClassName(DatabaseMapping mapping, String converterClassName);
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setEnumerated(Enum enumerated) {
    	m_enumerated = enumerated;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setFetch(Enum fetch) {
    	m_fetch = fetch;
    }
    
    /**
     * INTERNAL:
     */
    public abstract void setFieldClassification(DatabaseMapping mapping, Class classification);
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setLob(Boolean lob) {
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
    public void setTemporal(Enum temporalType) {
    	m_temporal = temporalType;
    }
        
    /**
     * INTERNAL:
     */
    public boolean usesIndirection() {
    	Enum fetchType = getFetch();
    	
    	if (fetchType == null) {
    		fetchType = getDefaultFetchType();
    	}
    	
        return fetchType.equals(FetchType.LAZY);
    }
}
