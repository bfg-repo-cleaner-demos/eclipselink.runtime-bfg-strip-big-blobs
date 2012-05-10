/*******************************************************************************
 * Copyright (c) 1998, 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.eclipse.persistence.tools.workbench.mappingsmodel.spi.meta;

/**
 * Interface defining Java metadata required by the
 * TopLink Mapping Workbench.
 * 
 * @see java.lang.reflect.Field
 */
public interface ExternalField extends ExternalMember {

	/**
	 * Returns an ExternalClassDescription object that identifies
	 * the declared type for the field represented by
	 * this ExternalField object.
	 * 
	 * @see java.lang.reflect.Field#getType()
	 */
	ExternalClassDescription getType();

}
