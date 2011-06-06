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
 * The modulo operation finds the remainder of division of one number by another.
 * <p>
 * It takes two integer arguments and returns an integer.
 * <p>
 * <div nowrap><b>BNF:</b> <code>expression ::= MOD(simple_arithmetic_expression, simple_arithmetic_expression)</code><p>
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
public final class ModExpression extends AbstractDoubleEncapsulatedExpression {

	/**
	 * Creates a new <code>ModExpression</code>.
	 *
	 * @param parent The parent of this expression
	 */
	ModExpression(AbstractExpression parent) {
		super(parent);
	}

	/**
	 * {@inheritDoc}
	 */
	public void accept(ExpressionVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPQLQueryBNF getQueryBNF() {
		return queryBNF(FunctionsReturningNumericsBNF.ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPQLQueryBNF parameterExpressionBNF(int index) {
		return queryBNF(SimpleArithmeticExpressionBNF.ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	String parseIdentifier(WordParser wordParser) {
		return MOD;
	}
}