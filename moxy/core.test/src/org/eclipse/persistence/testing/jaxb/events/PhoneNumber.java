/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.jaxb.events;

import javax.xml.bind.annotation.*;

public class PhoneNumber {
    @XmlValue
    public String number;
    
    public boolean equals(Object obj) {
        if(!(obj instanceof PhoneNumber)) {
            return false;
        }
        String objNum = ((PhoneNumber)obj).number;
        return objNum == number || (objNum != null && number != null && objNum.equals(number));
    }        
}