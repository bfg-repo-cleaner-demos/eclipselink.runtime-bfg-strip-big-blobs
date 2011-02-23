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
package org.eclipse.persistence.utils.jpa.query.parser;

import java.util.Collection;
import java.util.List;

/**
 * The <b>GROUP BY</b> construct enables the aggregation of values according to the properties of an
 * entity class.
 * <p>
 * <div nowrap><b>BNF:</b> <code>groupby_clause ::= GROUP BY groupby_item {, groupby_item}*</code><p>
 *
 * @version 11.2.0
 * @since 11.0.0
 * @author Pascal Filion
 */
public final class GroupByClause extends AbstractExpression {

	/**
	 * The unique group by item or the collection of group by items.
	 */
	private AbstractExpression groupByItems;

	/**
	 * Determines whether a whitespace was parsed after <b>GROUP BY</b>.
	 */
	private boolean hasSpace;

	/**
	 * Creates a new <code>GroupByClause</code>.
	 *
	 * @param parent The parent of this expression
	 */
	GroupByClause(AbstractExpression parent) {
		super(parent, GROUP_BY);
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
	public void acceptChildren(ExpressionVisitor visitor) {
		getGroupByItems().accept(visitor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void addChildrenTo(Collection<Expression> children) {
		children.add(getGroupByItems());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void addOrderedChildrenTo(List<StringExpression> children) {

		children.add(buildStringExpression(getText()));

		if (hasSpace) {
			children.add(buildStringExpression(SPACE));
		}

		if (groupByItems != null) {
			children.add(groupByItems);
		}
	}

	/**
	 * Returns the {@link Expression} that represents the list of group by items if any was parsed.
	 *
	 * @return The expression that was parsed representing the list of items
	 */
	public Expression getGroupByItems() {
		if (groupByItems == null) {
			groupByItems = buildNullExpression();
		}
		return groupByItems;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	JPQLQueryBNF getQueryBNF() {
		return queryBNF(GroupByClauseBNF.ID);
	}

	/**
	 * Determines whether the list of items was parsed.
	 *
	 * @return <code>true</code> if at least one item was parsed; <code>false</code> otherwise
	 */
	public boolean hasGroupByItems() {
		return groupByItems != null &&
		      !groupByItems.isNull();
	}

	/**
	 * Determines whether a whitespace was found after <b>GROUP BY</b>.
	 *
	 * @return <code>true</code> if there was a whitespace after <b>GROUP BY</b>; <code>false</code>
	 * otherwise
	 */
	public boolean hasSpaceAfterGroupBy() {
		return hasSpace;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void parse(WordParser wordParser, boolean tolerant) {

		// Parse 'GROUP BY'
		wordParser.moveForward(GROUP_BY);

		hasSpace = wordParser.skipLeadingWhitespace() > 0;

		// Group by items
//		if (tolerant) {
			groupByItems = parse(
				wordParser,
				queryBNF(GroupByItemBNF.ID),
				tolerant
			);
//		}
//		else {
//			groupByItems = parseWithFactory(
//				wordParser,
//				queryBNF(GroupByItemBNF.ID),
//				expressionFactory(LiteralExpressionFactory.ID),
//				tolerant
//			);
//		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void toParsedText(StringBuilder writer) {

		// 'GROUP BY'
		writer.append(getText());

		if (hasSpace) {
			writer.append(SPACE);
		}

		// Group by items
		if (groupByItems != null) {
			groupByItems.toParsedText(writer);
		}
	}
}