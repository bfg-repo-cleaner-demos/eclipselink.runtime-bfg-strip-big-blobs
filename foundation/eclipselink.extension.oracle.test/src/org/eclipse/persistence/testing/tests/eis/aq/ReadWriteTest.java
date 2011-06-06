/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.tests.eis.aq;

import java.math.BigDecimal;
import java.util.*;
import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.eis.interactions.*;
import org.eclipse.persistence.testing.models.order.*;
import org.eclipse.persistence.testing.framework.TestCase;

/**
 * Testing reading and writing. Testing reading and writing the order model mapped to XML messages.
 */
public class ReadWriteTest extends TestCase {
    public ReadWriteTest() {
        setName("ReadWriteTest");
        setDescription("Testing reading and writing the order model mapped to XML messages.");
    }

    public void test() throws Exception {
        Address address = new Address();
        address.addressee = "Bob Jones";
        address.street = "123 lane";
        address.city = "Ottawa";
        address.state = "Ont";
        address.country = "Canada";
        address.zipCode = "K2C4A4";

        List lineItems = new ArrayList();

        LineItem line = new LineItem();
        line.lineNumber = 1;
        line.itemName = "Wheels";
        line.itemPrice = new BigDecimal("35.99");
        line.quantity = 50;
        lineItems.add(line);

        line = new LineItem();
        line.lineNumber = 2;
        line.itemName = "Axles";
        line.itemPrice = new BigDecimal("135.99");
        line.quantity = 25;
        lineItems.add(line);

        Order order = new Order();
        order.orderedBy = "Tom Jones";
        order.address = address;
        order.lineItems = lineItems;

        XMLInteraction insertCall = new XMLInteraction();
        insertCall.setProperty("queue", "raw_order_queue");
        insertCall.setProperty("schema", "aquser");
        insertCall.setProperty("operation", "enqueue");
        InsertObjectQuery insert = new InsertObjectQuery(order);
        insert.setCall(insertCall);
        getSession().executeQuery(insert);

        XMLInteraction readCall = new XMLInteraction();
        readCall.setProperty("queue", "raw_order_queue");
        readCall.setProperty("schema", "aquser");
        readCall.setProperty("operation", "dequeue");

        getSession().getIdentityMapAccessor().initializeIdentityMaps();

        getSession().logMessage(getSession().readObject(org.eclipse.persistence.testing.models.order.Order.class, readCall).toString());
    }
}
