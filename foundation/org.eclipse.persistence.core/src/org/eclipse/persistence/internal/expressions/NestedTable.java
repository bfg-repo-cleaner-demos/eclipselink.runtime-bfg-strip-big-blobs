/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
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
package org.eclipse.persistence.internal.expressions;

import org.eclipse.persistence.internal.helper.*;
import org.eclipse.persistence.mappings.*;

public class NestedTable extends DatabaseTable {
    //the handle of the queryKey
    private QueryKeyExpression queryKeyExpression;

    public NestedTable() {
        super();
    }

    public NestedTable(QueryKeyExpression queryKeyExpression) {
        super();
        this.queryKeyExpression = queryKeyExpression;
        name = (queryKeyExpression.getMapping().getDescriptor().getTables().firstElement()).getName();
        tableQualifier = (queryKeyExpression.getMapping().getDescriptor().getTables().firstElement()).getQualifiedName();
    }

    /**
     * INTRENAL:
     */
    public String getQualifiedName() {
        if (qualifiedName == null) {
            // Print nested table using the TABLE function.
            DatabaseMapping mapping = queryKeyExpression.getMapping();
            DatabaseTable nestedTable = mapping.getDescriptor().getTables().firstElement();
            DatabaseTable tableAlias = queryKeyExpression.getBaseExpression().aliasForTable(nestedTable);
            DatabaseTable nestedTableAlias = queryKeyExpression.aliasForTable(this);

            StringBuffer name = new StringBuffer();
            name.append("TABLE(");
            name.append(tableAlias.getName());
            name.append(".");
            name.append(mapping.getField().getName());
            name.append(")");

            qualifiedName = name.toString();
        }

        return qualifiedName;
    }

    public QueryKeyExpression getQuerykeyExpression() {
        return queryKeyExpression;
    }

    public void setQuerykeyExpression(QueryKeyExpression queryKeyExpression) {
        this.queryKeyExpression = queryKeyExpression;
    }
}