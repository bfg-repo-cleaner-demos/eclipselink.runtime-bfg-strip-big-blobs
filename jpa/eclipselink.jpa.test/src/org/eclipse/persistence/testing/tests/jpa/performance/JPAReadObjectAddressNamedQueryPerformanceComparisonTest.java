/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
 package org.eclipse.persistence.testing.tests.jpa.performance;

import javax.persistence.*;
import org.eclipse.persistence.testing.models.performance.*;
import org.eclipse.persistence.testing.framework.*;

/**
 * This test compares the performance of read object Address.
 */
public class JPAReadObjectAddressNamedQueryPerformanceComparisonTest extends PerformanceRegressionTestCase {
    public JPAReadObjectAddressNamedQueryPerformanceComparisonTest() {
        setDescription("This test compares the performance of read object Address using a named query.");
    }

    /**
     * Read address.
     */
    public void test() throws Exception {
        EntityManager manager = createEntityManager();
        manager.getTransaction().begin();
        Query query = manager.createNamedQuery("findAddressByCity");
        query.setParameter("city", "Ottawa");
        Address address = (Address)query.getSingleResult();
        manager.getTransaction().commit();
        manager.close();
    }
}