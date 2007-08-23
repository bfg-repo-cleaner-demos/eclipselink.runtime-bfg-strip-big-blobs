/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.jaxb.xmlattribute;

import javax.xml.bind.annotation.*;
import java.util.*;

@XmlRootElement(name="employee-data")
public class EmployeeCollection
{
	@XmlList
    @XmlAttribute(name="id")
	public java.util.Collection ids;

	public String toString()
	{
		return "EMPLOYEE: " + ids;
	}

	public boolean equals(Object object) {
		EmployeeCollection emp = ((EmployeeCollection)object);
		if(emp.ids == null && ids == null) {
			return true;
		} else if(emp.ids == null || ids == null)
		{
			return false;
		}
		if(emp.ids.size() != ids.size()) {
			return false;
		}
		Iterator ids1 = emp.ids.iterator();
		Iterator ids2 = ids.iterator();
		while(ids1.hasNext()) {
			if(!(ids1.next().equals(ids2.next()))) {
				System.out.println("returning false");
				return false;
			}
		}
		return true;
	}
};
