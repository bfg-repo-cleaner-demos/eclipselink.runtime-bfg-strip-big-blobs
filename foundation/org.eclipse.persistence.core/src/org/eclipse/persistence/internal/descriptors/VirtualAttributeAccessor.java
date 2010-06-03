/*******************************************************************************
 * Copyright (c) 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     03/29/2010-2.1 Guy Pelletier 
 *       - 267217: Add Named Access Type to EclipseLink-ORM
 ******************************************************************************/  
package org.eclipse.persistence.internal.descriptors;

import org.eclipse.persistence.exceptions.DescriptorException;

public class VirtualAttributeAccessor extends MethodAttributeAccessor {
    /**
     * Gets the value of an instance variable in the object.
     */
    @Override
    public Object getAttributeValueFromObject(Object anObject) throws DescriptorException {
        return getAttributeValueFromObject(anObject, new Object[] {getAttributeName()});
    }
    
    /**
     * Return the method return type. In a name access usage the return type
     * currently must be Object.class.
     */
    @Override
    public Class getGetMethodReturnType() {
        return Object.class;
    }

    /**
     * Return the set method parameter type. In a name access usage there must
     * be an extra String parameter along with the Object.class type so we must
     * check and return a different index from the parameter types.
     */
    @Override
    public Class getSetMethodParameterType() {
        return getSetMethodParameterType(1);
    }
    
    /**
     * Return the set method parameter types.
     */
    @Override
    protected Class[] getSetMethodParameterTypes() { 
        return new Class[] {String.class, getGetMethodReturnType()};
    }
    
    /**
     * Set get and set method after creating these methods by using
     * get and set method names
     */
    @Override
    public void initializeAttributes(Class theJavaClass) throws DescriptorException {
        initializeAttributes(theJavaClass, new Class[] { String.class });
    }
    
    /**
     * Sets the value of the instance variable in the object to the value.
     */
    @Override
    public void setAttributeValueInObject(Object domainObject, Object attributeValue) throws DescriptorException {
        setAttributeValueInObject(domainObject, attributeValue, new Object[] {getAttributeName(), attributeValue});
    }
}
