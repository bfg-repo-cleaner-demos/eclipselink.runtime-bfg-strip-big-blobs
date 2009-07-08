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
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.converters;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedActionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.jpa.metadata.accessors.mappings.DirectAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;

import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedGetConstructorFor;
import org.eclipse.persistence.internal.security.PrivilegedInvokeConstructor;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.EnumTypeConverter;

/**
 * INTERNAL:
 * Object to hold onto an object type converter metadata.
 * 
 * @author Guy Pelletier
 * @since TopLink 11g
 */
public class ObjectTypeConverterMetadata extends TypeConverterMetadata {
    private List<ConversionValueMetadata> m_conversionValues = new ArrayList<ConversionValueMetadata>();
    private String m_defaultObjectValue;
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public ObjectTypeConverterMetadata() {
        super("<object-type-converter>");
    }
    
    /**
     * INTERNAL:
     */
    public ObjectTypeConverterMetadata(Annotation objectTypeConverter, MetadataAccessibleObject accessibleObject) {
        super(objectTypeConverter, accessibleObject);
        
        for (Annotation conversionValue: (Annotation[]) MetadataHelper.invokeMethod("conversionValues", objectTypeConverter)) {
            m_conversionValues.add(new ConversionValueMetadata(conversionValue, accessibleObject));
           }
        
        m_defaultObjectValue = (String) MetadataHelper.invokeMethod("defaultObjectValue", objectTypeConverter); 
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public boolean equals(Object objectToCompare) {
        if (objectToCompare instanceof ObjectTypeConverterMetadata) {
            ObjectTypeConverterMetadata objectTypeConverter = (ObjectTypeConverterMetadata) objectToCompare;
            
            if (! valuesMatch(getName(), objectTypeConverter.getName())) {
                return false;
            }
            
            if (! valuesMatch(getDataType(), objectTypeConverter.getDataType())) {
                return false;
            }
            
            if (! valuesMatch(getObjectType(), objectTypeConverter.getObjectType())) {
                return false;
            }
            
            if (! valuesMatch(m_conversionValues, objectTypeConverter.getConversionValues())) {
                return false;
            }
            
            return valuesMatch(m_defaultObjectValue, objectTypeConverter.getDefaultObjectValue());
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<ConversionValueMetadata> getConversionValues() {
        return m_conversionValues;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getDefaultObjectValue() {
        return m_defaultObjectValue;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasConversionValues() {
        return ! m_conversionValues.isEmpty();
    }
    
    /**
     * INTERNAL:
     */    
    private Object initObject(Class type, String value, DirectAccessor accessor, boolean isData) {
        if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
            try {
                Constructor constructor = (Constructor) AccessController.doPrivileged(new PrivilegedGetConstructorFor(type, new Class[] {String.class}, false));
                return AccessController.doPrivileged(new PrivilegedInvokeConstructor(constructor, new Object[] {value}));
            } catch (PrivilegedActionException exception) {
                throwInitObjectException(exception, type, value, isData);
            }
        } else {
            try {
                Constructor constructor = PrivilegedAccessHelper.getConstructorFor(type, new Class[] {String.class}, false);
                return PrivilegedAccessHelper.invokeConstructor(constructor, new Object[] {value});
            } catch (Exception exception) {
                throwInitObjectException(exception, type, value, isData);
            }
        }
        
        return null; // keep compiler happy, will never hit.
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void process(DatabaseMapping mapping, DirectAccessor accessor) {
        org.eclipse.persistence.mappings.converters.ObjectTypeConverter converter;
        Class dataType = getDataType(accessor);
        Class objectType = getObjectType(accessor);
        
        if (objectType.isEnum()) {
            // Create an EnumTypeConverter. 
            converter = new EnumTypeConverter(mapping, objectType.getName());
            
            // The object values should be the names of the enum members so 
            // force the objectType to String to ensure the initObject calls 
            // below will work.
            objectType = String.class;
        } else {
            // Create an ObjectTypeConverter.
            converter = new org.eclipse.persistence.mappings.converters.ObjectTypeConverter(mapping);
        }
        
        // Process the conversion values.
        // Hold two-way mappings from the database to the object.
        Map<String, String> dataToObjectValues = new HashMap<String, String>();
        // If a member from m_dataToObjectValues is not in m_objectToDataValues
        // then it will be assumed that it is a one-way mapping.
        Map<String, String> objectToDataValues = new HashMap<String, String>();
   
        if (hasConversionValues()) {
            for (ConversionValueMetadata conversionValue: getConversionValues()) {
                String dataValue = conversionValue.getDataValue();
                String objectValue = conversionValue.getObjectValue();
            
                if (dataToObjectValues.containsKey(dataValue)) {
                    throw ValidationException.multipleObjectValuesForDataValue(accessor.getJavaClass(), getName(), dataValue);
                } else {
                    dataToObjectValues.put(dataValue, objectValue);
                
                    // Only add it if it is a two-way mapping.
                    if (! objectToDataValues.containsKey(objectValue)) {
                        objectToDataValues.put(objectValue, dataValue);
                    }
                }
            }
        }
        
        // Process the data to object mappings. The object and data values
        // should be primitive wrapper types so we can initialize the 
        // conversion values now.
        for (String dataValue : dataToObjectValues.keySet()) {
            String objectValue = dataToObjectValues.get(dataValue);
            
            Object data = initObject(dataType, dataValue, accessor, true);
            Object object = initObject(objectType, objectValue, accessor, false);
            
            if (objectToDataValues.containsKey(objectValue)) {
                // It's a two-way mapping ...
                converter.addConversionValue(data, object);
            } else {
                // It's a one-way mapping ...
                converter.addToAttributeOnlyConversionValue(data, object);
            }
        }
        
        // Process the defaultObjectValue if one is specified.
        if (m_defaultObjectValue != null && ! m_defaultObjectValue.equals("")) {
            converter.setDefaultAttributeValue(initObject(objectType, m_defaultObjectValue, accessor, false));
        }
        
        // Set the converter on the mapping.
        accessor.setConverter(mapping, converter);
    }

    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setConversionValues(List<ConversionValueMetadata> conversionValues) {
        m_conversionValues = conversionValues;
    }
    
    /**
     * INTERNAL:
     * Called from annotation population.
     */
    //protected void setConversionValues(Annotation[] conversionValues) {
        //m_conversionValues = new ArrayList<ConversionValueMetadata>();
        //    
        //for (Annotation conversionValue: conversionValues) {
          //  m_conversionValues.add(new ConversionValueMetadata(conversionValue, getLocation()));
           //}
    //}
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setDefaultObjectValue(String defaultObjectValue) {
        m_defaultObjectValue = defaultObjectValue;
    }
    
    /**
     * INTERNAL:
     */    
    protected void throwInitObjectException(Exception exception, Class type, String value, boolean isData) {
        if (isData) {
            throw ValidationException.errorInstantiatingConversionValueData(getName(), value, type, exception);
        } else {
            throw ValidationException.errorInstantiatingConversionValueObject(getName(), value, type, exception);
        }
    }
}
