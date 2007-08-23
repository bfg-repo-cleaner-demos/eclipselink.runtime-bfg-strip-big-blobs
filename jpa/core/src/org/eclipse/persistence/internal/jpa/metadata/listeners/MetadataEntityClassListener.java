/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.listeners;

import java.lang.reflect.Method;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.exceptions.ValidationException;

/**
 * A callback listener for those entities that define callback methods. 
 * Callback methods on an entity must be signatureless, hence, this class 
 * overrides behavior from CBListener.
 * 
 * @author Guy Pelletier
 * @since TopLink 10.1.3/EJB 3.0 Preview
 */
public class MetadataEntityClassListener extends MetadataEntityListener {
    /**
     * INTERNAL: 
     */
    public MetadataEntityClassListener(Class entityClass) {
        super(entityClass);
    }
    
    /**
     * INTERNAL: 
     * For entity classes listener methods, they need to override listeners 
     * from mapped superclasses for the same method. So we need to override 
     * this method and make the override check instead of it throwing an
     * exception for multiple lifecycle methods for the same event.
     */
    public void addEventMethod(String event, Method method) {
        if (! hasOverriddenEventMethod(method, event)) {
            super.addEventMethod(event, method);
        }
    }
    
    /**
     * INTERNAL:
     */
    public Class getListenerClass() {
        return getEntityClass();
    }
    
    /**
     * INTERNAL: 
     */
    protected void invokeMethod(String event, DescriptorEvent descriptorEvent) {
        Object[] objectList = {};
        invokeMethod(getEventMethod(event), descriptorEvent.getObject(), objectList, descriptorEvent);
    }
    
    /**
     * INTERNAL:
     */
     public boolean isEntityClassListener() {
        return true;
    }
    
    /**
     * INTERNAL:
     */
    protected void validateMethod(Method method) {
        if (method.getParameterTypes().length > 0) {
            throw ValidationException.invalidEntityCallbackMethodArguments(getEntityClass(), method.getName());
        } else {
            // So far so good, now check the method modifiers.
            validateMethodModifiers(method);
        }
    }
}
