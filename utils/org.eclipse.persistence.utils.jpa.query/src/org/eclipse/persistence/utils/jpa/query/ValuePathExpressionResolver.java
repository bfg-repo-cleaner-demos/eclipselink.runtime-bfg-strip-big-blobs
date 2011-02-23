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
package org.eclipse.persistence.utils.jpa.query;

import org.eclipse.persistence.expressions.Expression;

/**
 * The {@link PathExpressionResolver} is responsible to retrieve the {@link Expression} for the
 * value of a {@link java.util.Map Map}.
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 * @author John Bracken
 */
final class ValuePathExpressionResolver extends AbstractPathExpressionResolver {

	/**
	 * Creates a new <code>ValuePathExpressionResolver</code>.
	 *
	 * @param parent The parent resolver responsible for the parent path of the given path
	 */
	ValuePathExpressionResolver(PathExpressionResolver parent) {
		super(parent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ValuePathExpressionResolver clone() {
		return new ValuePathExpressionResolver(getParent().clone());
	}

	/**
	 * {@inheritDoc}
	 */
	public Expression getExpression() {
		return getParentExpression();
	}

	/**
	 * {@inheritDoc}
	 */
	public Expression getExpression(String variableName) {
		return getParent().getExpression(variableName);
	}
}