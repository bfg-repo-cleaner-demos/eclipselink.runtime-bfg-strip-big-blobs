/*******************************************************************************
 * Copyright (c) 2006, 2011 Oracle. All rights reserved.
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

import org.eclipse.persistence.jpa.internal.jpql.WordParser;
import org.eclipse.persistence.jpa.jpql.spi.IJPAVersion;

/**
 * This {@link ResultVariableFactory} creates a new {@link ResultVariable} when the portion of the
 * query to parse starts with or without <b>AS</b>.
 *
 * @see ResultVariable
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
final class ResultVariableFactory extends ExpressionFactory {

	/**
	 * The unique identifier of this {@link ResultVariableFactory}.
	 */
	static final String ID = "result_variable";

	/**
	 * Creates a new <code>ResultVariableFactory</code>.
	 */
	ResultVariableFactory() {
		super(ID, Expression.AS);
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
	                                   boolean tolerant) {

		// There was already an expression parsed, we'll assume it's a valid
		// expression and it will have an identification variable
		if (((expression != null) || word.equalsIgnoreCase(Expression.AS)) &&
		    isSupported(parent) &&
		    (word.indexOf(".") == -1))
		{
			ResultVariable resultVariable = new ResultVariable(parent, expression);
			resultVariable.parse(wordParser, tolerant);
			return resultVariable;
		}

		// The word is a JPQL identifier, lets try to parse the query using the factory so
		// the invalid portion can be properly validated and possibly the rest of the query
		// can be parsed correctly
		if (tolerant && AbstractExpression.isIdentifier(word)) {
			ExpressionFactory factory = AbstractExpression.expressionFactoryForIdentifier(word);
			if (factory != null) {
				return factory.buildExpression(parent, wordParser, word, queryBNF, expression, tolerant);
			}
		}

		// Use the default factory
		ExpressionFactory factory = AbstractExpression.expressionFactory(LiteralExpressionFactory.ID);
		return factory.buildExpression(parent, wordParser, word, queryBNF, expression, tolerant);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	IJPAVersion getVersion() {
		// We return JPA 1.0 because the check will be done in the build method
		// otherwise identification variable, state field path expression won't
		// be parsed
		return IJPAVersion.VERSION_1_0;
	}

	private boolean isSupported(AbstractExpression parent) {
		return parent.getJPAVersion().isNewerThanOrEqual(IJPAVersion.VERSION_2_0);
	}
}