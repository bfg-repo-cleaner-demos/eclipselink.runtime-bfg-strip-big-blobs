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
 * The query BNF for a simple select expression.
 *
 * <div nowrap><b>BNF:</b> <code>simple_select_expression ::= single_valued_path_expression |
 * scalar_expression | aggregate_expression | identification_variable</code><p>
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
public final class SimpleSelectExpressionBNF extends JPQLQueryBNF {

	/**
	 * The unique identifier of this BNF rule.
	 */
	public static final String ID = "simple_select_expression";

	/**
	 * Creates a new <code>SimpleSelectExpressionBNF</code>.
	 */
	SimpleSelectExpressionBNF() {
		super(ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	String getFallbackBNFId() {
		// Because none of the child BNFs can identify how to parse the select
		// expression, it will fall back into itself and will use
		// LiteralExpressionFactory. It handles most of the text that does not
		// start with an identifier
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	String getFallbackExpressionFactoryId() {
		return LiteralExpressionFactory.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean handleCollection() {
		// True only to support an invalid query
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void initialize() {
		super.initialize();

		registerChild(SingleValuedPathExpressionBNF.ID);
		registerChild(ScalarExpressionBNF.ID);
		registerChild(AggregateExpressionBNF.ID);
		registerChild(IdentificationVariableBNF.ID);
	}
}