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
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Is used to specify the map key for associations of type 
 * {@link java.util.Map}.
 * 
 * <p> If a persistent field or property other than the primary 
 * key is used as a map key then it is expected to have a 
 * uniqueness constraint associated with it.
 *
 * <pre>
 *
 *    Example 1:
 *
 *    &#064;Entity
 *    public class Department {
 *        ...
 *        &#064;OneToMany(mappedBy="department")
 *        &#064;MapKey(name="empId")
 *        public Map<Integer, Employee> getEmployees() {... }
 *        ...
 *    }
 *
 *    &#064;Entity
 *    public class Employee {
 *        ...
 *        &#064;Id Integer getEmpid() { ... }
 *        &#064;ManyToOne
 *        &#064;JoinColumn(name="dept_id")
 *        public Department getDepartment() { ... }
 *        ...
 *    }
 *
 *    Example 2:
 *
 *    &#064;Entity
 *        public class Department {
 *        ...
 *        &#064;OneToMany(mappedBy="department")
 *        &#064;MapKey(name="empPK")
 *        public Map<EmployeePK, Employee> getEmployees() {... }
 *        ...
 *    }
 *
 *    &#064;Entity
 *        public class Employee {
 *        &#064;EmbeddedId public EmployeePK getEmpPK() { ... }
 *        ...
 *        &#064;ManyToOne
 *        &#064;JoinColumn(name="dept_id")
 *        public Department getDepartment() { ... }
 *        ...
 *    }
 *
 *    &#064;Embeddable
 *    public class EmployeePK {
 *        String name;
 *        Date bday;
 *    }
 * </pre>
 *
 * @since Java Persistence API 1.0
 */
@Target({METHOD, FIELD}) 
@Retention(RUNTIME)

public @interface MapKey {

    /**
     * The name of the persistent field or property of the 
     * associated entity that is used as the map key. If the 
     * name element is not specified, the primary key of the 
     * associated entity is used as the map key. If the 
     * primary key is a composite primary key and is mapped 
     * as {@link IdClass}, an instance of the primary key 
     * class is used as the key.
     */
    String name() default "";
}
