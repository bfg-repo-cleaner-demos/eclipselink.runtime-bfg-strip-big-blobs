/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.mapping;

import org.eclipse.persistence.testing.framework.*;
import org.eclipse.persistence.testing.models.mapping.*;

public class TwoParametersTransformationMappingTest extends WriteObjectTest {
	public Address address;

	public TwoParametersTransformationMappingTest(Object originalObject) {
		super(originalObject);
	}

	public Address getAddress() {
		return address;
	}

	protected void verify() {
		getSession().getIdentityMapAccessor().initializeIdentityMaps();
		this.objectFromDatabase = getSession().executeQuery(this.query);
		this.address = (Address) this.objectFromDatabase;
		if (getAddress().employeeRows == null) {
			throw new TestErrorException("The 2 parameter transformation mapping has failed");
		}
	}
}
