/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available athttp://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle
 *
 ******************************************************************************/
package org.eclipse.persistence.utils.jpa.query.parser;

/**
 * The query BNF for the select clause.
 *
 * <div nowrap><b>BNF:</b> <code>select_clause ::= SELECT [DISTINCT] select_item {, select_item}*</code><p>
 *
 * @version 11.2.0
 * @since 11.2.0
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
final class SelectClauseBNF extends JPQLQueryBNF
{
	/**
	 * The unique identifier of this BNF rule.
	 */
	static final String ID = "select_clause";

	/**
	 * Creates a new <code>SelectClauseBNF</code>.
	 */
	SelectClauseBNF()
	{
		super(ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	boolean handleCollection()
	{
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void initialize()
	{
		registerExpressionFactory(SelectClauseFactory.ID);
	}
}