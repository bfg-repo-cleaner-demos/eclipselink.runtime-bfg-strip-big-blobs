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
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.identitymaps;

import java.util.*;

import org.eclipse.persistence.descriptors.ClassDescriptor;

/**
 * <p><b>Purpose</b>: A SoftIdentityMap holds all objects referenced by the application.
 * The soft identity map is similar to the weak identity map except for the fact that it allows
 * only garbage collects when memory is low.
 * <p><b>Responsibilities</b>:<ul>
 * <li> Guarantees identity.
 * <li> Allows garbage collection when memory is low.
 * </ul>
 * @author James Sutherland
 * @since TopLink 11g
 */
public class SoftIdentityMap extends WeakIdentityMap {

    public SoftIdentityMap(int size, ClassDescriptor descriptor) {
        super(size, descriptor);
        this.cleanupCount = 0;
        this.cleanupSize = size;
    }

    public CacheKey createCacheKey(Vector primaryKey, Object object, Object writeLockValue, long readTime) {
        return new SoftCacheKey(primaryKey, object, writeLockValue, readTime);
    }
}
