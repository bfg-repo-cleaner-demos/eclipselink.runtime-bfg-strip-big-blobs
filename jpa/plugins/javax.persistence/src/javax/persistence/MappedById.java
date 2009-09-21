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
 * Is used to designate a <code>ManyToOne</code> or
 * <code>OneToOne</code> relationship attribute that corresponds to an
 * {@link EmbeddedId} primary key, an attribute within an
 * <code>EmbeddedId</code> primary key, or a simple primary key of the
 * parent entity. The <code>value</code> element specifies the
 * attribute within a composite key to which the relationship
 * attribute corresponds. If the entity's primary key is of the same
 * Java type as the primary key of the entity referenced by the
 * relationship, the value attribute is not specified.
 * 
 * <pre>
 *    Example:
 *
 *    // parent entity has simple primary key
 *
 *    &#064;Entity
 *    public class Employee {
 *       &#064;Id long empId;
 *       String name;
 *       ...
 *    } 
 *
 *    // dependent entity uses EmbeddedId for composite key
 *
 *    &#064;Embeddable
 *    public class DependentId {
 *       String name;
 *       long empid;   // corresponds to PK type of Employee
 *    }
 *
 *    &#064;Entity
 *    public class Dependent {
 *       &#064;EmbeddedId DependentId id;
 *        ...
 *       &#064;MappedById("empid")  //  maps to empid attribute of embedded id
 *       &#064;ManyToOne Employee emp;
 *    }
 * </pre>
 *
 * @since Java Persistence 2.0
 */
@Target( { METHOD, FIELD })
@Retention(RUNTIME)
public @interface MappedById {

    /**
     * (Optional) The name of the attribute within the composite key
     * to which the relationship attribute corresponds.  If not
     * supplied, the relationship is mapped by the entity�s primary
     * key.
     */
   String value() default ""; }
