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
 * The {@link StringLiteralExpressionFactory} responsible to parse a sub-query
 * starting with a single quote.
 *
 * @see StringLiteral
 *
 * @version 11.2.0
 * @since 11.0.0
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
final class StringLiteralFactory extends ExpressionFactory
{
	/**
	 * The unique identifier of this {@link StringLiteralFactory}.
	 */
	static final String ID = "string-literal";

	/**
	 * Creates a new <code>StringLiteralFactory</code>.
	 */
	StringLiteralFactory()
	{
		super(ID, Expression.QUOTE);
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
		expression = new StringLiteral(parent);
		expression.parse(wordParser, tolerant);
		return expression;
	}
}