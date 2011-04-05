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

/**
 * The query BNF for the <b>TYPE</b> expression.
 *
 * <div nowrap><b>BNF:</b> <code>entity_type_expression ::= type_discriminator |
 *                                                          entity_type_literal |
 *                                                          input_parameter</code><p>
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
public final class EntityTypeExpressionBNF extends JPQLQueryBNF {

	/**
	 * The unique identifier of this BNF rule.
	 */
	public static final String ID = "entity_type_expression";

	/**
	 * Creates a new <code>EntityTypeExpressionBNF</code>.
	 */
	EntityTypeExpressionBNF() {
		super(ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	String getFallbackBNFId() {
		return EntityTypeLiteralBNF.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void initialize() {
		super.initialize();

		registerChild(TypeExpressionBNF.ID);
		registerChild(InputParameterBNF.ID);
		registerChild(EntityTypeLiteralBNF.ID);
	}
}