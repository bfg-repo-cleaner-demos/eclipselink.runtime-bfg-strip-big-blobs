/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.queries.inmemory;

import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.testing.framework.*;

//When query's shouldMaintainCache is false and descriptor's shouldDisableCacheHits is false,
//cache is not checked and a different object is returned.
public class QueryCacheHitDisabledAndDescriptorEnabledTest extends QueryAndDescriptorCacheHitTest {
    public QueryCacheHitDisabledAndDescriptorEnabledTest() {
        setDescription("Test when cache hit is disabled in query and enabled in descriptor, cache is not checked");
    }

    protected void setup() {
        super.setup();
        descriptor.setShouldDisableCacheHits(false);
    }

    protected Object readObject(ReadObjectQuery query) {
        query.setCacheUsage(ObjectLevelReadQuery.DoNotCheckCache);
        query.refreshIdentityMapResult();
        return getSession().executeQuery(query);
    }

    protected void verify() {
        if (((org.eclipse.persistence.testing.models.employee.domain.Employee)objectRead).getFirstName().equals(firstName)) {
            throw new TestErrorException("Object read match but should not.");
        }

        if (tempStream.toString().length() == 0) {
            throw new TestErrorException("The read did not go to the database, but should have");
        }
    }
}