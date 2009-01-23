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
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Designates a class whose mapping information is applied 
 * to the entities that inherit from it. A mapped superclass 
 * has no separate table defined for it.  
 *
 * <p> A class designated with the <code>MappedSuperclass</code> 
 * annotation can be mapped in the same way as an entity except that the 
 * mappings will apply only to its subclasses since no table 
 * exists for the mapped superclass itself. When applied to the 
 * subclasses the inherited mappings will apply in the context 
 * of the subclass tables. Mapping information may be overridden 
 * in such subclasses by using the {@link AttributeOverride} and 
 * {@link AssociationOverride} annotations or corresponding XML elements.
 *
 * <pre>
 *    Example: Concrete class as a mapped superclass
 *
 *    &#064;MappedSuperclass
 *    public class Employee {
 *    
 *        &#064;Id protected Integer empId;
 *        &#064;Version protected Integer version;
 *        &#064;ManyToOne &#064;JoinColumn(name="ADDR")
 *        protected Address address;
 *    
 *        public Integer getEmpId() { ... }
 *        public void setEmpId(Integer id) { ... }
 *        public Address getAddress() { ... }
 *        public void setAddress(Address addr) { ... }
 *    }
 *    
 *    // Default table is FTEMPLOYEE table
 *    &#064;Entity
 *    public class FTEmployee extends Employee {
 *    
 *        // Inherited empId field mapped to FTEMPLOYEE.EMPID
 *        // Inherited version field mapped to FTEMPLOYEE.VERSION
 *        // Inherited address field mapped to FTEMPLOYEE.ADDR fk
 *        
 *    
 *    // Defaults to FTEMPLOYEE.SALARY
 *    
 *    protected Integer salary;
 *    
 *    
 *    public FTEmployee() {}
 *    
 *    
 *    public Integer getSalary() { ... }
 *    
 *    public void setSalary(Integer salary) { ... }
 *    }
 *    
 *    &#064;Entity &#064;Table(name="PT_EMP")
 *    &#064;AssociationOverride(name="address", 
 *    
 *    
 *    joincolumns=&#064;JoinColumn(name="ADDR_ID"))
 *    public class PartTimeEmployee extends Employee {
 *    
 *        // Inherited empId field mapped to PT_EMP.EMPID
 *        // Inherited version field mapped to PT_EMP.VERSION
 *        // address field mapping overridden to PT_EMP.ADDR_ID fk
 *        &#064;Column(name="WAGE")
 *        protected Float hourlyWage;
 *    
 *        public PartTimeEmployee() {}
 *    
 *        public Float getHourlyWage() { ... }
 *        public void setHourlyWage(Float wage) { ... }
 *    }
 *
 *    Example: Non-entity superclass
 *
 *    public class Cart {
 *    
 *        // This state is transient
 *        Integer operationCount;
 *    
 *        public Cart() { operationCount = 0; }
 *        public Integer getOperationCount() { return operationCount; }
 *        public void incrementOperationCount() { operationCount++; }
 *    }
 *    
 *    &#064Entity
 *    public class ShoppingCart extends Cart {
 *    
 *        Collection<Item> items = new Vector<Item>();
 *    
 *        public ShoppingCart() { super(); }
 *    
 *    
 *    ...
 *    
 *        &#064OneToMany
 *        public Collection<Item> getItems() { return items; }
 *        public void addItem(Item item) {
 *            items.add(item);
 *            incrementOperationCount();
 *        }
 *    }
 * </pre>
 *
 * @since Java Persistence API 1.0
 */
@Target({TYPE})
@Retention(RUNTIME)

public @interface MappedSuperclass {
}
