/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.sequencing;

import org.eclipse.persistence.internal.databaseaccess.Accessor;

/**
 * <p>
 * <b>Purpose</b>: Define interface for getting separate sequencing connection(s)
 * <p>
 * <b>Description</b>:
 * <p>
 * <b>Responsibilities</b>:
 * <ul>
 * <li> Used by SequencingManager only, to obtain separate sequencing connection(s)
 * </ul>
 * @see SequencingManager
 */
interface SequencingConnectionHandler extends SequencingLogInOut {
    public Accessor acquireAccessor();

    public void releaseAccessor(Accessor accessor);
}