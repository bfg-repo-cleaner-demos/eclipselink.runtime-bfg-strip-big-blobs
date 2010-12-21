/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
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

package org.eclipse.persistence.testing.models.jpa.partitioned;

public class PhoneNumberPK {
    public EmployeePK owner;

    public String type;

    public PhoneNumberPK() {
    }
    
    public EmployeePK getOwner() {
        return owner;
    }

    public void setOwner(EmployeePK owner) {
        this.owner = owner;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * equals: Answer true if the ids are equal
     */
    public boolean equals(Object pk) {
        if (!(pk instanceof PhoneNumberPK)) {
            return false;
        }
        return ((PhoneNumberPK)pk).owner.equals(this.owner) && ((PhoneNumberPK)pk).type.equals(this.type);
    }
}
