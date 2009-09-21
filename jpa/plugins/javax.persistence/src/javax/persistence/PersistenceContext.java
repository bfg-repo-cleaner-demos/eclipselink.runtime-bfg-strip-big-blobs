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
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Expresses a dependency on a container-managed {@link EntityManager} and its
 * associated persistence context.
 *
 * @since Java Persistence 1.0
 */

@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface PersistenceContext {

    /**
     * (Optional) The name by which the entity manager is to be accessed in the 
     * environment referencing context; not needed when dependency 
     * injection is used.
     */
    String name() default "";

    /**
     * (Optional) The name of the persistence unit as defined in the
     * <code>persistence.xml</code> file. If the <code>unitName</code> element is
     * specified, the persistence unit for the entity manager that is
     * accessible in JNDI must have the same name.
     */
    String unitName() default "";

    /**
     * (Optional) Specifies whether a transaction-scoped persistence context 
     * or an extended persistence context is to be used.
     */
    PersistenceContextType type() default PersistenceContextType.TRANSACTION;

    /**
     * (Optional) Properties for the container or persistence
     * provider.  Vendor specific properties may be included in this
     * set of properties.  Properties that are not recognized by
     * a vendor are ignored.  
     */ 
    PersistenceProperty[] properties() default {};
}
