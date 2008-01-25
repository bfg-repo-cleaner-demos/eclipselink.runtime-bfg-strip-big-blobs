/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.mappingsmodel.meta;

import java.util.EventListener;

/**
 * An "ExternalClassLoadFailure" event gets fired whenever an "external"
 * class repository has problems loading the metadata needed to populate
 * an "external" class.
 */

public interface ExternalClassLoadFailureListener 
	extends EventListener 
{
    /**
     * This method gets called when an "external" class repository
     * has problems loading the metadata needed to populate an "external" class.
     * 
     * @param e An ExternalClassLoadFailureEvent object describing the
     * event source, the "external" class that did not load, and the cause of
     * the failure.
     */
    void externalClassLoadFailure(ExternalClassLoadFailureEvent e);
}