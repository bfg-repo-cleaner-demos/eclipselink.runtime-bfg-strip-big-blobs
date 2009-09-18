/*******************************************************************************
 * Copyright (c) 2008, 2009 Sun Microsystems. All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * 
 * Contributors:
 *     Linda DeMichiel -Java Persistence 2.0 - Proposed Final Draft, Version 2.0 (August 31, 2009)
 *     Specification available from http://jcp.org/en/jsr/detail?id=317
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

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Is applied to a persistent field or property of an entity 
 * class or mapped superclass to denote a composite primary 
 * key that is an embeddable class. The embeddable class 
 * must be annotated as {@link Embeddable}. 
 *
 * <p> Relationship mappings defined within an embedded id class are not supported.
 * 
 * <p> The {@link AttributeOverride} annotation may be used to override
 * the column mappings declared within the embeddable class.
 *
 * <p> There must be only one <code>EmbeddedId</code> annotation and
 * no <code>Id</code> annotation when the <code>EmbeddedId</code> annotation is used.
 *
 * The {@link MappedById} annotation may be used in conjunction
 * with the <code>EmbeddedId</code> annotation to specify a derived
 * primary key.
 * <pre>
 *    Example:
 *
 *    &#064;EmbeddedId
 *    protected EmployeePK empPK;
 * </pre>
 *
 * @see Embeddable
 * @see MappedById
 *
 * @since Java Persistence 1.0
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)

public @interface EmbeddedId {}
