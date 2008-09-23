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
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 *     09/23/2008-1.1 Guy Pelletier 
 *       - 241651: JPA 2.0 Access Type support
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.objects;

import java.lang.reflect.Field;

import javax.persistence.AccessType;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;

/**
 * INTERNAL:
 * An object to hold onto a valid EJB 3.0 decorated field.
 * 
 * @author Guy Pelletier
 * @since TopLink 10.1.3/EJB 3.0 Preview
 */
public class MetadataField extends MetadataAnnotatedElement {    
    /**
     * INTERNAL:
     */
    public MetadataField(Field field, MetadataLogger logger) {
        super(field, logger);
        
        setName(field.getName());
        setAttributeName(field.getName());
        setRelationType(field.getGenericType());
    }
    
    /**
     * INTERNAL:
     */
    public MetadataField(Field field, XMLEntityMappings entityMappings) {
        super(field, entityMappings);
        
        setName(field.getName());
        setAttributeName(field.getName());
        setRelationType(field.getGenericType());
    }
    
    /**
     * INTERNAL:
     */
    protected Field getField() {
        return (Field) getAnnotatedElement();
    }
    
    /**
     * INTERNAL:
     * Return true is this field is a valid persistence field. This method
     * will validate against any declared annotations on the field. If the 
     * mustBeExplicit flag is true, then we are processing the inverse of an 
     * explicit access setting and the field must have an Access(FIELD) 
     * setting to be processed. Otherwise, it is ignored.
     */
    public boolean isValidPersistenceField(boolean mustBeExplicit, MetadataDescriptor descriptor) {
        if (isValidPersistenceElement(mustBeExplicit, AccessType.FIELD, descriptor)) {
            return isValidPersistenceField(descriptor, hasDeclaredAnnotations(descriptor)); 
        }

        return false;
    }
    
    /**
     * INTERNAL:
     * Return true is this field is a valid persistence field. User decorated
     * is used to indicate that the field either had persistence annotations
     * defined on it or that it was specified in XML.
     */
    public boolean isValidPersistenceField(MetadataDescriptor descriptor, boolean userDecorated) {
        if (! isValidPersistenceElement(getField().getModifiers())) {
            if (userDecorated) {
                throw ValidationException.mappingMetadataAppliedToInvalidAttribute(getField(), descriptor.getJavaClass());
            }
            
            return false;
        }
        
        return true;
    }
}
