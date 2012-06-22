/*******************************************************************************
 * Copyright (c) 2006, 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.eclipse.persistence.jpa.internal.jpql.parser;

/**
 * The query BNF for the order by clause.
 *
 * <div nowrap><b>BNF:</b> <code>orderby_clause ::= ORDER BY orderby_item {, orderby_item}*</code><p>
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
public final class OrderByClauseBNF extends JPQLQueryBNF {

	/**
	 * The unique identifier of this BNF rule.
	 */
	public static final String ID = "orderby_clause";

	/**
	 * Creates a new <code>OrderByClauseBNF</code>.
	 */
	OrderByClauseBNF() {
		super(ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean handleCollection() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void initialize() {
		super.initialize();
		registerExpressionFactory(OrderByClauseFactory.ID);
	}
}