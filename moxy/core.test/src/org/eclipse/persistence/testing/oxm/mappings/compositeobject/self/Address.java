/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.mappings.compositeobject.self;

import java.util.ArrayList;
import java.util.List;

public class Address {
    public String street;
    public String city;
    public List provinces = new ArrayList();

    public boolean equals(Object object) {
        try {
            Address address = (Address)object;
            if (this == address) {
                return true;
            }
            if (null == street) {
                if (null != address.street) {
                    return false;
                }
            } else {
                if (!street.equals(address.street)) {
                    return false;
                }
            }
            if (null == city) {
                if (null != address.city) {
                    return false;
                }
            } else {
                if (!city.equals(address.city)) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Address(street=\"");
        stringBuffer.append(street);
        stringBuffer.append("\" city=\"");
        stringBuffer.append(city);
        stringBuffer.append("\")");
        return stringBuffer.toString();
    }
}