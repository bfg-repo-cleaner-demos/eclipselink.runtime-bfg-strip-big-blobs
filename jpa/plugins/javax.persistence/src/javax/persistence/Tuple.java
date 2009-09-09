/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     gyorke - Java Persistence 2.0 - Post Proposed Final Draft (March 13, 2009) Updates
 *               Specification available from http://jcp.org/en/jsr/detail?id=317
 *
 * Java(TM) Persistence API, Version 2.0 - EARLY ACCESS
 * This is an implementation of an early-draft specification developed under the 
 * Java Community Process (JCP).  The code is untested and presumed not to be a  
 * compatible implementation of JSR 317: Java(TM) Persistence API, Version 2.0.   
 * We encourage you to migrate to an implementation of the Java(TM) Persistence 
 * API, Version 2.0 Specification that has been tested and verified to be compatible 
 * as soon as such an implementation is available, and we encourage you to retain 
 * this notice in any implementation of Java(TM) Persistence API, Version 2.0 
 * Specification that you distribute.
 ******************************************************************************/
package javax.persistence;

import java.util.List;

/**
 * Interface for extracting the elements of a query result tuple.
 */
public interface Tuple {

    /**
     * Get the value of the specified tuple element.
     * @param tupleElement  tuple element
     * @return value of tuple element
     * @throws IllegalArgumentException if tuple element
     *         does not correspond to an element in the
     *         query result tuple
     */
    <X> X get(TupleElement<X> tupleElement);

    /**
     * Get the value of the tuple element to which the
     * specified alias has been assigned.
     * @param alias  alias assigned to tuple element
     * @param type of the tuple element
     * @return value of the tuple element
     * @throws IllegalArgumentException if alias
     *         does not correspond to an element in the
     *         query result tuple or element cannot be
     *         assigned to the specified type
     */
    <X> X get(String alias, Class<X> type); 

    /**
     * Get the value of the tuple element to which the
     * specified alias has been assigned.
     * @param alias  alias assigned to tuple element
     * @return value of the tuple element
     * @throws IllegalArgumentException if alias
     *         does not correspond to an element in the
     *         query result tuple
     */
    Object get(String alias); 

    /**
     * Get the value of the element at the specified
     * position in the result tuple. The first position is 0.
     * @param i  position in result tuple
     * @param type  type of the tuple element
     * @return value of the tuple element
     * @throws IllegalArgumentException if i exceeds
     *         length of result tuple  or element cannot be
     *         assigned to the specified type
     */
    <X> X get(int i, Class<X> type);

    /**
     * Get the value of the element at the specified
     * position in the result tuple. The first position is 0.
     * @param i  position in result tuple
     * @return value of the tuple element
     * @throws IllegalArgumentException if i exceeds
     *         length of result tuple
     */
    Object get(int i);

    /**
     * Return the values of the result tuple elements as an array.
     * @return tuple element values
     */
    Object[] toArray();

    /**
     * Return the tuple elements
     * @return tuple elements
     */
    List<TupleElement<?>> getElements();
}
