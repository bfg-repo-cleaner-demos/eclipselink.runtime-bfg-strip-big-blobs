/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Denise Smith - Initial implementation June 12, 2009
 ******************************************************************************/  
package org.eclipse.persistence.internal.jaxb.many;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.mappings.AttributeAccessor;

/**
* AttributeAccessor used in conjunction with an XMLCompositeDirectCollectionMapping 
* or XMLCompositeCollectionMapping to enable support for mapping to Maps in JAXB  
*/
public class MapValueAttributeAccessor extends AttributeAccessor {

    private AttributeAccessor nestedAccessor;
    private String mapClassName;
    private Class mapClass;
    private ContainerPolicy containerPolicy;
    private Class generatedEntryClass;
    
    public MapValueAttributeAccessor(AttributeAccessor nestedAccessor, ContainerPolicy cp, Class generatedEntryClass, String mapClassName) {
        this.nestedAccessor = nestedAccessor;
        this.mapClassName = mapClassName;
        this.containerPolicy = cp;
        this.generatedEntryClass = generatedEntryClass;
    }   
    
    public Object getAttributeValueFromObject(Object object)throws DescriptorException {
        Object value = nestedAccessor.getAttributeValueFromObject(object);
        if(null == value) {
            return null;
        }
    
        Object results = containerPolicy.containerInstance(((Map)value).size());            
        Iterator iter =  ((Map)value).keySet().iterator();
        while(iter.hasNext()){
            Object nextKey = iter.next();
            Object nextValue = ((Map)value).get(nextKey);
            MapEntry nextEntry;
            try {
                nextEntry = (MapEntry)generatedEntryClass.newInstance();
            } catch (Exception e) {     
                return null;
            } 
            nextEntry.setKey(nextKey);
            nextEntry.setValue(nextValue);
            containerPolicy.addInto(nextEntry, results, null);
        }
        return results; 
    }
    
    public void setAttributeValueInObject(Object object, Object value) throws DescriptorException {
        
        Map mapValue = null;;
        try{
            mapValue = (Map)mapClass.newInstance();
        } catch (InstantiationException e) {
            throw XMLMarshalException.unmarshalException(e);
        } catch (IllegalAccessException e) {
            throw XMLMarshalException.unmarshalException(e);
        }
        Object iterator = containerPolicy.iteratorFor(value);
        while(containerPolicy.hasNext(iterator)){
            Object next = containerPolicy.next(iterator, null);
            Object nextKey = ((MapEntry)next).getKey();
            Object nextValue = ((MapEntry)next).getValue(); 
            mapValue.put(nextKey, nextValue);
        }
        nestedAccessor.setAttributeValueInObject(object, mapValue);         
    }
        
    public void initializeAttributes(Class theJavaClass) throws DescriptorException {
        nestedAccessor.initializeAttributes(theJavaClass);
        
        if(mapClassName.equals("java.util.Map")){
            mapClassName = "java.util.HashMap";
        } else if(mapClassName.equals("java.util.concurrent.ConcurrentMap")){
            mapClassName = "java.util.concurrent.ConcurrentHashMap";
        } else if(mapClassName.equals("java.util.SortedMap")){
            mapClassName = "java.util.TreeMap";
        }
      
        try{            
            mapClass =PrivilegedAccessHelper.getClassForName(mapClassName);
        }catch (ClassNotFoundException e){
            throw XMLMarshalException.unmarshalException(e);
        }     
    }

}
