/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
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
package org.eclipse.persistence.oxm;

/**
 * <p>An implementation of XMLMarshalListener can be set on an XMLMarshaller to provide additional
 * behaviour during marshal operations.</p>
 */
public interface XMLMarshalListener {	

    /**
     * This event will be called before an object is marshalled.
     *
     * @param target The object that will be marshalled.
     */
    public void beforeMarshal(Object target);
    
    /**
     * This event  will be called after an object is marshalled.
     *
     * @param target The object that was marshalled.
     */
    public void afterMarshal(Object target);
}
