/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.sessions.changesets;

import org.eclipse.persistence.internal.helper.IdentityHashtable;

/**
 * <p>
 * <b>Purpose</b>: This interface defines the API for the changeRecord that maintains the changes made to a collection attribute of
 * an object.
 * <p>
 * <b>Description</b>: The CollectionChangeRecord stores a list of objects removed from the collection and a seperate list of objects
 * added to a collection
 */
public interface CollectionChangeRecord extends ChangeRecord {

    /**
     * ADVANCED:
     * This method returns the IdentityHashtable that contains the added values to the collection
     * and their corresponding ChangeSets.
     * @return java.util.Vector
     */
    public IdentityHashtable getAddObjectList();

    /**
     * ADVANCED:
     * This method returns the IdentityHashtable that contains the removed values from the collection
     * and their corresponding ChangeSets.
     * @return java.util.Vector
     */
    public IdentityHashtable getRemoveObjectList();

    /**
     * ADVANCED:
     * This method returns true if the change set has changes
     * @return boolean
     */
    public boolean hasChanges();
}