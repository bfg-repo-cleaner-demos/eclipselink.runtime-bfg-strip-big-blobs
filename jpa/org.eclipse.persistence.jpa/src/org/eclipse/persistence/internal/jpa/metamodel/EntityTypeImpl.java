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
 *     03/19/2009-2.0  dclarke  - initial API start    
 *     06/30/2009-2.0  mobrien - finish JPA Metadata API modifications in support
 *       of the Metamodel implementation for EclipseLink 2.0 release involving
 *       Map, ElementCollection and Embeddable types on MappedSuperclass descriptors
 *       - 266912: JPA 2.0 Metamodel API (part of the JSR-317 EJB 3.1 Criteria API)  
 *     07/06/2009-2.0  mobrien - 266912: Introduce IdentifiableTypeImpl between ManagedTypeImpl
 *       - EntityTypeImpl now inherits from IdentifiableTypeImpl instead of ManagedTypeImpl
 *       - MappedSuperclassTypeImpl now inherits from IdentifiableTypeImpl instead
 *       of implementing IdentifiableType indirectly
 *     07/16/2009-2.0  mobrien - 266912: implement getIdType() minus full composite key support
 *         http://wiki.eclipse.org/EclipseLink/Development/JPA_2.0/metamodel_api#DI_47:_20090715:_Implement_IdentifiableType.getIdType.28.29_for_composite_keys
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metamodel;

import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Type;

import org.eclipse.persistence.descriptors.CMPPolicy;
import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.internal.jpa.CMP3Policy;
import org.eclipse.persistence.mappings.DatabaseMapping;

/**
 * <p>
 * <b>Purpose</b>: Provides the implementation for the EntityType interface 
 *  of the JPA 2.0 Metamodel API (part of the JSR-317 EJB 3.1 Criteria API)
 * <br>EntityTypeImpl implements the IdentifiableType interface via EntityType
 * <p>
 * <b>Description</b>: 
 *  Instances of the type EntityType represent entity types.
 *   
 * @see javax.persistence.metamodel.EntityType
 * 
 * @since EclipseLink 2.0 - JPA 2.0
 * @param <X> The represented entity type.  
 */ 
public class EntityTypeImpl<X> extends IdentifiableTypeImpl<X> implements EntityType<X> {    
   
    protected EntityTypeImpl(MetamodelImpl metamodel, RelationalDescriptor descriptor) {
        super(metamodel, descriptor);
        // The supertype field will remain uninstantiated until MetamodelImpl.initialize() is complete
    }

    /**
     *  Return the bindable type of the represented object.
     *  @return bindable type
     */ 
    public Bindable.BindableType getBindableType() {
    	return Bindable.BindableType.ENTITY_TYPE;
    }
    
    /**
     * Return the Java type of the represented object.
     * If the bindable type of the object is PLURAL_ATTRIBUTE,
     * the Java element type is returned. If the bindable type is
     * SINGLE_ATTRIBUTE or ENTITY_TYPE, the Java type of the
     * represented entity or attribute is returned.
     * @return Java type
     */
    public Class<X> getBindableJavaType() {
        // In EntityType our BindableType is ENTITY_TYPE - return the java type of the entity
        return this.getJavaType();
    }
    
    /**
     *  Return the type that represents the type of the id.
     *  @return type of id
     */
    @Override
    public Type<?> getIdType() {
        // NOTE: This code is another good reason to abstract out a PKPolicy on the descriptor
        // descriptor.getPrimaryKeyPolicy().getIdClass();
        CMPPolicy cmpPolicy = getDescriptor().getCMPPolicy();
        
        if (null == cmpPolicy) {
            // Composite key support (IE: @EmbeddedId)
            java.util.List<DatabaseMapping> pkMappings = getDescriptor().getObjectBuilder().getPrimaryKeyMappings();
            
            if (pkMappings.size() == 1) {
                Class aClass = pkMappings.get(0).getAttributeClassification(); // null for OneToOneMapping
                // lookup class in our types map
                Type<?> aType = this.metamodel.type(aClass);
                return aType;
            }
        }
        
        // Single Key support using any Java class - built in or user defined
        // There already is an instance of the PKclass on the policy
        if (cmpPolicy instanceof CMP3Policy) {
            // BasicType, EntityType or IdentifiableType are handled here, lookup the class in the types map and create a wrapper if it does not exist yet
            return this.metamodel.getType(((CMP3Policy) cmpPolicy).getPKClass());
        }
        // TODO: Error message for incompatible JPA config
        throw new IllegalStateException("Incompatible persistence configuration");
    }
    
    /**
     *  Return the entity name
     *  @return entity name
     */
    public String getName() {
        return getDescriptor().getAlias();
    }
    
    /**
     *  Return the persistence type.
     *  @return persistence type
     */ 
    public Type.PersistenceType getPersistenceType() {
        return Type.PersistenceType.ENTITY;
    }
}
