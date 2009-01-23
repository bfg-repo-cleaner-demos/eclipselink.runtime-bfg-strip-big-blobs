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
 * The API for this class and its comments are derived from the JPA 2.0 specification 
 * which is developed under the Java Community Process (JSR 317) and is copyright 
 * Sun Microsystems, Inc. 
 *
 * Contributors:
 *     pkrogh -        Java Persistence API 2.0 Public Draft
 *                     Specification and licensing terms available from
 *                     http://jcp.org/en/jsr/detail?id=317
 *
 * EARLY ACCESS - PUBLIC DRAFT
 * This is an implementation of an early-draft specification developed under the 
 * Java Community Process (JCP) and is made available for testing and evaluation 
 * purposes only. The code is not compatible with any specification of the JCP.
 ******************************************************************************/
package javax.persistence;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This annotation defines a primary key generator that may be referenced by
 * name when a generator element is specified for the {@link GeneratedValue}
 * annotation. A sequence generator may be specified on the entity class or on
 * the primary key field or property. The scope of the generator name is global
 * to the persistence unit (across all generator types).
 * 
 * <pre>
 *   Example:
 *    @SequenceGenerator(name=&quot;EMP_SEQ&quot;, allocationSize=25)
 * </pre>
 * 
 * @since Java Persistence API 1.0
 */
@Target( { TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface SequenceGenerator {

	/**
	 * (Required) A unique generator name that can be referenced by one or more
	 * classes to be the generator for primary key values.
	 */
	String name();

	/**
	 * (Optional) The name of the database sequence object from which to obtain
	 * primary key values.
	 * <p>
	 * Defaults to a provider-chosen value.
	 */
	String sequenceName() default "";

	/** (Optional) The catalog of the sequence generator. 
	 * @since Java Persistence API 2.0
	 */
	String catalog() default "";

	/** (Optional) The schema of the sequence generator. 
	 * @since Java Persistence API 2.0
	 */
	String schema() default "";

	/**
	 * (Optional) The value from which the sequence object is to start
	 * generating.
	 */
	int initialValue() default 1;

	/**
	 * (Optional) The amount to increment by when allocating sequence numbers
	 * from the sequence.
	 */
	int allocationSize() default 50;
}
