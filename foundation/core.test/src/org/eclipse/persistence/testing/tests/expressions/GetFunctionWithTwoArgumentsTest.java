/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.expressions;

import org.eclipse.persistence.testing.framework.*;
import org.eclipse.persistence.expressions.*;

public class GetFunctionWithTwoArgumentsTest extends ReadAllExpressionTest {
    public GetFunctionWithTwoArgumentsTest(Class referenceClass, int originalObjectsSize) {
        super(referenceClass, originalObjectsSize);
    }

    public void setup() {
        if (this.getSession().getLogin().getPlatform().isOracle() || getSession().getLogin().getPlatform().isMySQL()) {
            ExpressionOperator op = getSession().getLogin().getPlatform().getOperator(ExpressionOperator.Concat);
            String concatFunctionName;

            ExpressionBuilder emp = new ExpressionBuilder();

            //expression = emp.get("firstName").getFunction(concatFunctionName, " is cool!").equal("Sarah" + " is cool!");
            expression = emp.get("firstName").getFunction("CONCAT", " is cool!").equal("Sarah" + " is cool!");
            getQuery(true).setSelectionCriteria(expression);

            super.setup();
        } else {
            throw new TestWarningException("This test can only be done on Oracle");
        }
    }
}