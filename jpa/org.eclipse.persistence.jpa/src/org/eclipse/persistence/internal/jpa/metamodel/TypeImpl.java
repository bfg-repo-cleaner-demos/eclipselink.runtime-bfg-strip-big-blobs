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

import javax.persistence.metamodel.Type;

/**
 * <p>
 * <b>Purpose</b>: Provides the implementation for the Type interface 
 *  of the JPA 2.0 Metamodel API (part of the JSR-317 EJB 3.1 Criteria API)
 * <p>
 * <b>Description</b>:
 * Instances of the type Type represent persistent object 
 * or attribute types.
 * 
 * @see javax.persistence.metamodel.Type 
 * @since EclipseLink 2.0 - JPA 2.0
 *  
 * @param <X>  The type of the represented object or attribute  
 */ 
public abstract class TypeImpl<X> implements Type<X> {
    
    /** The Java Class in use that this Type represents */
    private Class<X> javaClass;
    
    protected TypeImpl(Class<X> javaClass) {
        this.javaClass = javaClass;
    }

    /**
     *  Return the represented Java type.
     *  @return Java type
     */
    public Class<X> getJavaType() {
        return this.javaClass;
    }

    /**
     * INTERNAL:
     * Return whether this type is an Entity (true) or MappedSuperclass (false) or Embeddable (false)
     * @return
     */
    public abstract boolean isEntity();
    
    /**
     * INTERNAL:
     * Return whether this type is identifiable.
     * This would be EntityType and MappedSuperclassType
     * @return
     */
    public abstract boolean isIdentifiableType();

    /**
     * INTERNAL:
     * Return whether this type is identifiable.
     * This would be EmbeddableType as well as EntityType and MappedSuperclassType
     * @return
     */
    public abstract boolean isManagedType();
    
    /**
     * INTERNAL:
     * Return whether this type is an MappedSuperclass (true) or Entity (false) or Embeddable (false)
     * @return
     */
    public abstract boolean isMappedSuperclass();

    /**
     * INTERNAL:
     * Return the string representation of the receiver.
     */
    @Override
    public String toString() {
        StringBuffer aBuffer = new StringBuffer();
        aBuffer.append(this.getClass().getSimpleName());
        aBuffer.append("@");
        aBuffer.append(hashCode());
        if(null != this.getJavaType()) {
            aBuffer.append(":");
            aBuffer.append(this.getJavaType().getSimpleName());
        }
        aBuffer.append(" [ javaType: ");
        aBuffer.append(this.getJavaType());
        toStringHelper(aBuffer);
        aBuffer.append("]");
        return aBuffer.toString();
    }

    /**
     * INTERNAL:
     * Append the partial string representation of the receiver to the StringBuffer.
     */
    protected abstract void toStringHelper(StringBuffer aBuffer);
}
