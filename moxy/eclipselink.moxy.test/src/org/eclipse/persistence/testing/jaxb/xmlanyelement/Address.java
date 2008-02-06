/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.jaxb.xmlanyelement;
import javax.xml.bind.annotation.*;

@XmlRootElement(name="address")
public class Address {

	public String street;
	public String city;
	public String country;
	
	public boolean equals(Object obj) {
		if(!(obj instanceof Address)) {
			return false;
		}
		Address addr = (Address)obj;
		return addr.street.equals(street) && addr.city.equals(city) && addr.country.equals(country);
	}

}
