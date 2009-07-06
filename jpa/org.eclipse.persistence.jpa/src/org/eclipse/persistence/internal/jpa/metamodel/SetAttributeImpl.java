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
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metamodel;

import java.util.Set;

import javax.persistence.PersistenceException;
import javax.persistence.metamodel.SetAttribute;

import org.eclipse.persistence.mappings.CollectionMapping;

/**
 * <p>
 * <b>Purpose</b>: Provides the implementation for the SetAttribute interface 
 *  of the JPA 2.0 Metamodel API (part of the JSR-317 EJB 3.1 Criteria API)
 * <p>
 * <b>Description</b>: 
 * 
 * @see javax.persistence.metamodel.SetAttribute 
 * @since EclipseLink 2.0 - JPA 2.0
 *  
 */ 
public class SetAttributeImpl<X, V> extends PluralAttributeImpl<X, java.util.Set<V>, V> implements SetAttribute<X, V> {

    protected SetAttributeImpl(ManagedTypeImpl<X> managedType, CollectionMapping mapping) {
        super(managedType, mapping);
    }

    public Class<V> getBindableJavaType() {
        throw new PersistenceException("Not Yet Implemented");
    }

    public CollectionType getCollectionType() {
        return CollectionType.SET;
    }
    
    public Class<Set<V>> getJavaType() {
        return this.getMapping().getAttributeClassification();
    }
    
    @Override
    public boolean isAttribute() {
    	throw new PersistenceException("Not Yet Implemented");
    }
    
    public String toString() {
        return "SetAttributeImpl[" + getMapping() + "]";
    }

    
}
