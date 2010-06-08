/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
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
package org.eclipse.persistence.internal.helper;

import java.util.*;

/**
 *  INTERNAL:
 *  Interface which abstracts the version of the JDK we are on.
 *  This should only implement methods that are dependent on JDK version
 *  The implementers should implement the minimum amount of functionality required to
 *  allow support of multiple versions of the JDK.
 *  @see JDK15Platform
 *  @see JavaPlatform
 *  @author Tom Ware
 */
public interface JDKPlatform {

    /**
     * Conforming queries with LIKE will act differently in different JDKs.
     */
    Boolean conformLike(Object left, Object right);


    /**
     * Get a concurrent Map that allow concurrent gets but block on put.
     */
    Map getConcurrentMap();

}
