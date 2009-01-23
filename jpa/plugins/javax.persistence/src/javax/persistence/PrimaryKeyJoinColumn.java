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
 * This annotation specifies a primary key column that is used 
 * as a foreign key to join to another table. 
 *
 * <p> It is used to join the primary table of an entity subclass 
 * in the {@link InheritanceType#JOINED JOINED} mapping strategy 
 * to the primary table of its superclass; it is used within a 
 * {@link SecondaryTable} annotation to join a secondary table 
 * to a primary table; and it may be used in a {@link OneToOne} 
 * mapping in which the primary key of the referencing entity 
 * is used as a foreign key to the referenced entity. 
 *
 * <p> If no <code>PrimaryKeyJoinColumn</code> annotation is 
 * specified for a subclass in the {@link InheritanceType#JOINED 
 * JOINED} mapping strategy, the foreign key columns are assumed 
 * to have the same names as the primary key columns of the 
 * primary table of the superclass
 *
 * <pre>
 *
 *    Example: Customer and ValuedCustomer subclass
 *
 *    &#064;Entity
 *    &#064;Table(name="CUST")
 *    &#064;Inheritance(strategy=JOINED)
 *    &#064;DiscriminatorValue("CUST")
 *    public class Customer { ... }
 *    
 *    &#064;Entity
 *    &#064;Table(name="VCUST")
 *    &#064;DiscriminatorValue("VCUST")
 *    &#064;PrimaryKeyJoinColumn(name="CUST_ID")
 *    public class ValuedCustomer extends Customer { ... }
 * </pre>
 *
 * @since Java Persistence API 1.0
 */
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)

public @interface PrimaryKeyJoinColumn {

    /** 
     * The name of the primary key column of the current table.
     * <p> Defaults to the same name as the primary key column 
     * of the primary table of the superclass ({@link 
     * InheritanceType#JOINED JOINED} mapping strategy); the same 
     * name as the primary key column of the primary table 
     * ({@link SecondaryTable} mapping); or the same name as the 
     * primary key column for the table for the referencing entity 
     * ({@link OneToOne} mapping)
     */
    String name() default "";

    /** 
     * (Optional) The name of the primary key column of the table 
     * being joined to.
     * <p> Defaults to the same name as the primary key column 
     * of the primary table of the superclass ({@link 
     * InheritanceType#JOINED JOINED} mapping strategy); the same 
     * name as the primary key column of the primary table 
     * ({@link SecondaryTable} mapping); or the same name as the 
     * primary key column for the table for the referencing entity 
     * ({@link OneToOne} mapping)
     */
    String referencedColumnName() default "";

    /**
     * (Optional) The SQL fragment that is used when generating the 
     * DDL for the column. This should not be specified for a 
     * {@link OneToOne} primary key association.
     * <p> Defaults to the generated SQL to create a column of the 
     * inferred type.
     */
    String columnDefinition() default "";
}
