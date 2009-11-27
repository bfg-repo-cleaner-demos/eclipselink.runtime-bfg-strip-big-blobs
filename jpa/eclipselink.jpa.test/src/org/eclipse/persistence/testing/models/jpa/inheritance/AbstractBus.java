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
package org.eclipse.persistence.testing.models.jpa.inheritance;

import javax.persistence.PrePersist;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractBus extends FueledVehicle {
    public static int PRE_PERSIST_COUNT = 0;

	@PrePersist
	private void prePersist() {
        PRE_PERSIST_COUNT++;
	}
}
