/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.models.order;


/**
 * Model address class, maps to ADDRESS record.
 */
public class Address {
    public String addressee;
    public String street;
    public String city;
    public String state;
    public String country;
    public String zipCode;

    public String toString() {
        return "Address(" + addressee + ", " + street + ", " + city + ", " + state + ", " + country + ", " + zipCode + ")";
    }
}