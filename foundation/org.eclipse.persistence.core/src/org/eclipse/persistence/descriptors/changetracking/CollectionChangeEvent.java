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
package org.eclipse.persistence.descriptors.changetracking;

import java.beans.PropertyChangeEvent;

/**
 * <p>
 * <b>Purpose</b>: Define a change event for collection types.
 * <p>
 * <b>Description</b>: For any object that wishes to use either object change tracking or
 * attribute change tracking, its collection attributes need to fire CollectionChangeEvent
 * in the add or remove methods.
 * <p>
 * <b>Responsibilities</b>: Create a CollectionChangeEvent for an object
 * <ul>
 * </ul>
 */
public class CollectionChangeEvent extends PropertyChangeEvent {
    public static int ADD = 0;
    public static int REMOVE = 1;

    /**
     * INTERNAL:
     * Change type is either add or remove
     */
    protected int changeType;
    
    /**
     * INTERNAL:
     * index is the location of the change in the collection
     */
    protected Integer index;

    /**
     * PUBLIC:
     * Create a CollectionChangeEvent for an object based on the property name, old value, new value
     * and change type (add or remove)
     */
    public CollectionChangeEvent(Object collectionOwner, String propertyName, Object collectionChanged, Object elementChanged, int changeType) {
        this(collectionOwner, propertyName, collectionChanged, elementChanged, changeType, (Integer)null);
    }
    
    /**
     * PUBLIC:
     * Create a CollectionChangeEvent for an object based on the property name, old value, new value, 
     * change type (add or remove) and the index where the object is/was in the collection (list)
     */
    public CollectionChangeEvent(Object collectionOwner, String propertyName, Object collectionChanged, Object elementChanged, int changeType, Integer index) {
        super(collectionOwner, propertyName, collectionChanged, elementChanged);
        this.changeType = changeType;
        this.index = index;
    }

    /**
     * INTERNAL:
     * Return the change type
     */
    public int getChangeType() {
        return changeType;
    }
    
    /**
     * INTERNAL:
     * Return the index of the change in the collection
     */
    public Integer getIndex() {
        return index;
    }

    /**
     * INTERNAL:
     * Set the index of the change in the collection
     */
    public void setIndex(Integer index) {
        this.index = index;
    }
}
