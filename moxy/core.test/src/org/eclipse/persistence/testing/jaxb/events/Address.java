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

public class Address {
    public String street;
    
    public boolean equals(Object obj) {
        if(!(obj instanceof Address)) {
            return false;
        }
        String objStreet = ((Address)obj).street;
        return objStreet == street || (objStreet != null && street != null && objStreet.equals(street));
    }    
}