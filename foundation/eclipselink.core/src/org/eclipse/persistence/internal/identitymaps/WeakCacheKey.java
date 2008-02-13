/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.identitymaps;

import java.lang.ref.*;
import java.util.Vector;

/**
 * <p><b>Purpose</b>: Container class for storing objects in an IdentityMap.
 * The weak cache key uses a weak reference to allow garbage collection of its object.
 * The cache key itself however will remain and thus should cleaned up every now and then.
 * <p><b>Responsibilities</b>:<ul>
 * <li> Hold key and object.
 * <li> Maintain and update the current writeLockValue.
 * </ul>
 * @since TOPLink/Java 1.0
 */
public class WeakCacheKey extends CacheKey {

    /** Reference is maintained weak to allow garbage collection */
    protected Reference reference;

    /**
     * Initialize the newly allocated instance of this class.
     * @param primaryKey contains values extracted from the object
     * @param writeLockValue is the write lock value, null if optimistic locking not being used for this object.
     * @param readTime the time EclipseLInk read the cache key
     */
    public WeakCacheKey(Vector primaryKey, Object object, Object writeLockValue, long readTime) {
        super(primaryKey, object, writeLockValue, readTime);
    }

    public Object getObject() {
        if (this.reference == null) {
            return null;
        }
        return this.reference.get();
    }

    public void setObject(Object object) {
        this.reference = new WeakReference(object);
    }
}