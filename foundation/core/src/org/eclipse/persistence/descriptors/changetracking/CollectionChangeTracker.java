/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.descriptors.changetracking;

/**
 * <p>
 * <b>Purpose</b>: Define an interface for any collection that wishes to use attribute change track.
 * <p>
 * <b>Description</b>: Build a bridge between an object and its PropertyChangeListener.  Which will be
 * The listener of the parent object.
 * <p>
 */
public interface CollectionChangeTracker extends ChangeTracker{

    /**
     * PUBLIC:
     * Return the Attribute name this collection is mapped under.
     */
    public String getTopLinkAttributeName();

    /**
     * PUBLIC:
     * Set the Attribute name this collection is mapped under.
     */
    public void setTopLinkAttributeName(String attributeName);
}