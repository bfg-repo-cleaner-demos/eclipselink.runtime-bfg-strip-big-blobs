/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
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
 *     02/06/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 2)
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     04/24/2009-2.0 Guy Pelletier 
 *       - 270011: JPA 2.0 MappedById support
 *     11/02/2009-2.0 Michael O'Brien
 *       - 266912: JPA 2.0 Metamodel support for m:1 as m:m in DI 96
 *     04/27/2010-2.1 Guy Pelletier 
 *       - 309856: MappedSuperclasses from XML are not being initialized properly
 *     06/14/2010-2.2 Guy Pelletier 
 *       - 264417: Table generation is incorrect for JoinTables in AssociationOverrides
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.mappings;

import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;

import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;

import org.eclipse.persistence.mappings.ManyToOneMapping;

/**
 * INTERNAL:
 * A many to one relationship accessor.
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public class ManyToOneAccessor extends ObjectAccessor {
    // Note: Any metadata mapped from XML to this class must be compared in the equals method.
    
    /**
     * INTERNAL:
     */
    public ManyToOneAccessor() {
        super("<many-to-one>");
    }
    
    /**
     * INTERNAL:
     */
    public ManyToOneAccessor(MetadataAnnotation manyToOne, MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
        super(manyToOne, accessibleObject, classAccessor);
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public boolean equals(Object objectToCompare) {
        return super.equals(objectToCompare) && objectToCompare instanceof ManyToOneAccessor;
    }
    
    /**
     * INTERNAL:
     * Return the logging context for this accessor.
     */
    protected String getLoggingContext() {
        return MetadataLogger.MANY_TO_ONE_MAPPING_REFERENCE_CLASS;
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public boolean isManyToOne() {
        return true;
    }
    
    /**
     * INTERNAL: 
     * A PrivateOwned setting on a ManyToOne is ignored. A log warning is
     * issued.
     */
    @Override
    public boolean isPrivateOwned() {
        if (super.isPrivateOwned()) {
            getLogger().logWarningMessage(MetadataLogger.IGNORE_PRIVATE_OWNED_ANNOTATION, this);
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     * Process a many to one setting into an EclipseLink OneToOneMapping.
     */
    public void process() {
        super.process();
        
        // Initialize our mapping now with what we found.
        ManyToOneMapping mapping = initManyToOneMapping();
                
        // Process the owning keys for this mapping.
        processOwningMappingKeys(mapping);
    }
}
