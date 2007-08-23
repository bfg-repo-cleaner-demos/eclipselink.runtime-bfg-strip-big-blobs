/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.models.jpa.advanced;

import javax.persistence.*;

/**
 * Local interface for the platinum buyer bean.
 * This is the bean's public/local interface for the clients usage.
 * All locals must extend the javax.ejb.EJBLocalObject.
 * The bean itself does not have to implement the local interface, but must implement all of the methods.
 */
@Entity
@Table(name="CMP3_PBUYER")
@NamedQuery(
	name="findPlatinumBuyer",
	query="SELECT OBJECT(buyer) FROM PlatinumBuyer buyer WHERE buyer.purchases >= :amount"
)
public class PlatinumBuyer extends Buyer {
	private double purchases;

	public double getPurchases() { 
        return purchases; 
    }
    
	public void setPurchases(double purchases) { 
		this.purchases = purchases; 
	}
}
