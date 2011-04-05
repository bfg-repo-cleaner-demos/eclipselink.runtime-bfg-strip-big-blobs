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

/**
 * This {@link UpdateItemStateFieldPathExpressionFactory} is meant to handle the parsing of a
 * portion of the query when it's expected to be a state field path. By default a word without a dot
 * would be parsed as an identification variable but for the left side of the update item assignment,
 * a single word is a state field path expression.
 *
 * @see StateFieldPathExpression
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
final class UpdateItemStateFieldPathExpressionFactory extends AbstractLiteralExpressionFactory {

	/**
	 * The unique identifier of this {@link StateFieldPathExpressionFactory}.
	 */
	static final String ID = "update-item-state-field-path";

	/**
	 * Creates a new <code>UpdateItemStateFieldPathExpressionFactory</code>.
	 */
	UpdateItemStateFieldPathExpressionFactory() {
		super(ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	AbstractExpression buildExpression(AbstractExpression parent,
	                                   WordParser wordParser,
	                                   String word,
	                                   AbstractExpression expression,
	                                   boolean tolerant) {

		if (tolerant && AbstractExpression.isIdentifier(word)) {
			return null;
		}

		expression = new StateFieldPathExpression(parent, word);
		expression.parse(wordParser, tolerant);
		return expression;
	}
}