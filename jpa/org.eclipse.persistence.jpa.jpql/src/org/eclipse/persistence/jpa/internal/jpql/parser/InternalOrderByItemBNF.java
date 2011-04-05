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
 * The query BNF for an order by item.
 * <p>
 * JPA 1.0: <div nowrap><b>BNF:</b> <code>orderby_item ::= state_field_path_expression [ ASC | DESC ]</code>
 * <p>
 * JPA 2.0 <div nowrap><b>BNF:</b> <code>orderby_item ::= state_field_path_expression | result_variable [ ASC | DESC ]</code><p>
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
public final class InternalOrderByItemBNF extends JPQLQueryBNF {

	/**
	 * The unique identifier for this <code>InternalOrderByItemBNF</code>.
	 */
	public static final String ID = "internal_orderby_item";

	/**
	 * Creates a new <code>InternalOrderByItemBNF</code>.
	 */
	InternalOrderByItemBNF() {
		super(ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	String getFallbackBNFId() {
		return ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	String getFallbackExpressionFactoryId() {
		return InternalOrderByItemFactory.ID;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void initialize() {
		super.initialize();

		registerChild(IdentificationVariableBNF.ID);
		registerChild(StateFieldPathExpressionBNF.ID);

		// EclipseLink extension over the JPQL grammar
		registerChild(ScalarExpressionBNF.ID);
	}
}