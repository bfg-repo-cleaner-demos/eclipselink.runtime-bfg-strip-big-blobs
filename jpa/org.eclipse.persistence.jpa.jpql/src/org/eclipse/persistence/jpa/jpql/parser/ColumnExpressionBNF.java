/*******************************************************************************
 * Copyright (c) 2012 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation
 *
 ******************************************************************************/
package org.eclipse.persistence.jpa.jpql.parser;

/**
 * The query BNF for the EclipseLink's column expression.
 *
 * <div nowrap><b>BNF:</b> <code>column_expression ::= COLUMN('name' {, path}*)</code><p>
 *
 * @version 2.4
 * @since 2.4
 * @author James
 */
@SuppressWarnings("nls")
public final class ColumnExpressionBNF extends JPQLQueryBNF {

	/**
	 * The unique identifier for this {@link ColumnExpressionBNF}.
	 */
	public static final String ID = "column_expression";

	/**
	 * Creates a new <code>ColumnExpressionBNF</code>.
	 */
	public ColumnExpressionBNF() {
		super(ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initialize() {
		super.initialize();
		registerExpressionFactory(ColumnExpressionFactory.ID);
	}
}