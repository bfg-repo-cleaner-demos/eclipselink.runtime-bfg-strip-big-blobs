/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.models.weaving;

import javax.persistence.*;
import static javax.persistence.GenerationType.*;
import static javax.persistence.CascadeType.*;
import static javax.persistence.FetchType.*;

@Entity(name="OrderBean")
@Table(name="CMP3_ORDER")
@NamedQuery(
	name="findAllOrdersByItem",
	query="SELECT OBJECT(theorder) FROM OrderBean theorder WHERE theorder.item.itemId = :id"
)
public class Order implements java.io.Serializable {

	private Integer orderId;
	private int version;
	private Item item;
	private int quantity;
	private String shippingAddress;
	private Customer customer;
	
	public Order() {
	}

	@Id
    @GeneratedValue(strategy=TABLE, generator="ORDER_TABLE_GENERATOR")
	@TableGenerator(
        name="ORDER_TABLE_GENERATOR", 
        table="CMP3_CUSTOMER_SEQ", 
        pkColumnName="SEQ_NAME", 
        valueColumnName="SEQ_COUNT",
        pkColumnValue="ORDER_SEQ"
    )
	@Column(name="ORDER_ID")
	public Integer getOrderId() { 
        return orderId; 
    }
    
	public void setOrderId(Integer id) { 
        this.orderId = id; 
    }

	@Version
	@Column(name="ORDER_VERSION")
	protected int getVersion() { 
        return version; 
    }
    
	protected void setVersion(int version) { 
        this.version = version; 
    }

	@OneToOne(cascade=PERSIST, fetch=LAZY)
	@JoinColumn(name="ITEM_ID", referencedColumnName="ITEM_ID")
	public Item getItem() { 
        return item; 
    }
    
	public void setItem(Item item) { 
        this.item = item; 
    }

	public int getQuantity() { 
        return quantity; 
    }
    
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	@Column(name="SHIP_ADDR")	
	public String getShippingAddress() { 
        return shippingAddress; 
    }
    
	public void setShippingAddress(String shippingAddress) {
		this.shippingAddress = shippingAddress;
	}

	@ManyToOne(fetch=LAZY)
	@JoinColumn(name="CUST_ID")
	public Customer getCustomer() { 
        return customer; 
    }
    
	public void setCustomer(Customer customer) { 
        this.customer = customer; 
    }
}
