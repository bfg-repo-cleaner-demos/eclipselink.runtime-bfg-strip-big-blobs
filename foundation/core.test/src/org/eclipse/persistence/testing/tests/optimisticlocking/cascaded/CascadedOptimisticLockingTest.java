/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.optimisticlocking.cascaded;

import org.eclipse.persistence.testing.framework.*;
import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;

/**
 * Root test case for the cascaded optimistic locking tests.
 * 
 * @auther Guy Pelletier
 * @version 1.0 June 2/05
 */
public class CascadedOptimisticLockingTest extends AutoVerifyTestCase {
    protected int m_id;
    protected int m_originalVersion;
    public CascadedOptimisticLockingTest() {}

    public void reset()  {	
        getAbstractSession().rollbackTransaction();
        getSession().getIdentityMapAccessor().initializeIdentityMaps();
    }

    protected void setup()  {	
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();
        getAbstractSession().beginTransaction();
    }

    protected void verify()  {
        Expression readExpression = new ExpressionBuilder().get("id").equal(m_id);
        
        Bar bar = (Bar) getSession().readObject(Bar.class, readExpression);
        
        if (bar.getVersion() <= m_originalVersion) {
            throw new TestErrorException("The parent version lock field was not updated in the cache.");   
        }
        
        // Clear the cache and check the database value now.
        getSession().getIdentityMapAccessor().initializeIdentityMaps();
        
        bar = (Bar) getSession().readObject(Bar.class, readExpression);
        
        if (bar.getVersion() <= m_originalVersion) {
            throw new TestErrorException("The parent version lock field was not updated in the database.");   
        }
    }
}
