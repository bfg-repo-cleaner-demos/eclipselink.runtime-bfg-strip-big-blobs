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
 * This {@link FromClauseFactory} creates a new {@link FromClause} when the
 * portion of the query to parse starts with <b>FROM</b>.
 *
 * @see FromClause
 *
 * @version 11.2.0
 * @since 11.0.0
 * @author Pascal Filion
 */
final class FromClauseFactory extends ExpressionFactory
{
	/**
	 * The unique identifier of this {@link FromClauseFactory}.
	 */
	static final String ID = Expression.FROM;

	/**
	 * Creates a new <code>FromClauseFactory</code>.
	 */
	FromClauseFactory()
	{
		super(ID, Expression.FROM);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	AbstractExpression buildExpression(AbstractExpression parent,
	                                   WordParser wordParser,
	                                   String word,
	                                   JPQLQueryBNF queryBNF,
	                                   AbstractExpression expression,
	                                   boolean tolerant)
	{
		expression = new FromClause(parent);
		expression.parse(wordParser, tolerant);
		return expression;
	}
}