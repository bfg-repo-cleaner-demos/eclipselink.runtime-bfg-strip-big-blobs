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
package org.eclipse.persistence.internal.jpa.metadata.accessors.classes;

import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.jpa.metadata.MetadataProject;

/**
 * An embeddable accessor.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 1.0
 */ 
public class EmbeddableAccessor extends ClassAccessor {
	/**
     * INTERNAL:
     */
    public EmbeddableAccessor() {}
    
    /**
     * INTERNAL:
     */
    public EmbeddableAccessor(Class cls, MetadataProject project) {
        super(cls, project);
    }
    
    /**
     * INTERNAL:
     * Process the items of interest on an embeddable class.
     */
    public void process() {
        // If a Cache annotation is present throw an exception.
        if (isAnnotationPresent(Cache.class)) {
            throw ValidationException.cacheNotSupportedWithEmbeddable(getJavaClass());
        } 
        
    	// This accessor represents an embeddable class
        // Process @Customizer
        processCustomizer();
        
        // Process the TopLink converters if specified.
        processConverters();   
        
        // Process the @ChangeTracking
        processChangeTracking();
        
        // Process the accessors on this embeddable.
        processAccessors();
        
        // Process @CopyPolicy
        processCopyPolicy();
    }
}
