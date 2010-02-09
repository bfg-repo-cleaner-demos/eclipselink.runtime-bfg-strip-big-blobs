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
package org.eclipse.persistence.testing.tests.queries;

import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.queries.DataReadQuery;
import org.eclipse.persistence.testing.framework.*;

/**
 * Test the query timeout feature through using locking.
 */

public class QueryTimeoutTest extends TestCase {
    private boolean limitExceed;

    public QueryTimeoutTest() {
        setDescription("Test the query timeout setting");
        this.limitExceed = false;
    }

    public void test() {
        try {
            DataReadQuery query = new DataReadQuery();
            query.setSQLString("SELECT SUM(e.EMP_ID) from EMPLOYEE e , EMPLOYEE b, EMPLOYEE c, EMPLOYEE d, EMPLOYEE f, EMPLOYEE g, EMPLOYEE h");
            query.setQueryTimeout(1);
            getSession().executeQuery(query);
        } catch (Exception e) {
            if (e instanceof DatabaseException) {
                limitExceed = true;
            }
        }
    }

    public void verify() {
        if (!limitExceed) {
            throw new TestErrorException("Timeout did not occur.");
        }
    }
}
