/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
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
package org.eclipse.persistence.internal.descriptors;


import java.util.Vector;

import org.eclipse.persistence.internal.identitymaps.CacheKey;

/**
 * Define an interface which stores the CacheKey and primary key.
 * This allows us to quickly extract the primary key from an object.
 * 
 * @author  mmacivor
 * @since   10.1.3
 */
public interface PersistenceEntity {
    CacheKey _persistence_getCacheKey();
    void _persistence_setCacheKey(CacheKey key);
    
    Vector _persistence_getPKVector();
    void _persistence_setPKVector(Vector pk);    
}
