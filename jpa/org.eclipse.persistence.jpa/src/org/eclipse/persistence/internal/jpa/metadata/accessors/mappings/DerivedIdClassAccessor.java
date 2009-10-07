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
 *     04/24/2009-2.0 Guy Pelletier 
 *       - 270011: JPA 2.0 MappedById support
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.mappings;

import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotatedElement;

/**
 * A derived id class accessor is found within an entity's embedded id class
 * and is a reference back to a parents id class.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 1.2
 */
public class DerivedIdClassAccessor extends EmbeddedAccessor {
    /**
     * INTERNAL:
     * Constructor called from an Embedabble IdClass.
     */
    public DerivedIdClassAccessor(MetadataAnnotatedElement accessibleObject, ClassAccessor classAccessor) {
        super(null, accessibleObject, classAccessor);
    }
    
    /**
     * INTERNAL:
     * Return true if this accessor is a derived id class accessor. That is,
     * an EmbeddedId that has a portion of its accessors mapped through an
     * owning entities ManyToOne relationship using a MapsId.
     */
    @Override
    public boolean isDerivedIdClass(){
        return true;
    }
}
