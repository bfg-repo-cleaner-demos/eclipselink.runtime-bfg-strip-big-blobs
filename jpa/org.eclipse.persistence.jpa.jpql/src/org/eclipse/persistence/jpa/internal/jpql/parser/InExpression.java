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

import java.util.Collection;
import java.util.List;
import org.eclipse.persistence.jpa.internal.jpql.WordParser;

/**
 * The <code>state_field_path_expression</code> must have a string, numeric, or enum value. The
 * literal and/or <code>input_parameter</code> values must be like the same abstract schema type of
 * the <code>state_field_path_expression</code> in type.
 * <p>
 * The results of the <code>subquery</code> must be like the same abstract schema type of the
 * <code>state_field_path_expression</code> in type.
 * <p>
 * There must be at least one element in the comma separated list that defines the set of values for
 * the <b>IN</b> expression. If the value of a <code>state_field_path_expression</code> in an
 * <b>IN</b> or <b>NOT IN</b> expression is <b>NULL</b> or unknown, the value of the expression is
 * unknown.
 * <p>
 * JPA 1.0:
 * <div nowrap><b>BNF:</b> <code>in_expression ::= state_field_path_expression [NOT] IN(in_item {, in_item}* | subquery)</code><p>
 * JPA 2.0
 * <div nowrap><b>BNF:</b> <code>in_expression ::= {state_field_path_expression | type_discriminator} [NOT] IN { ( in_item {, in_item}* ) | (subquery) | collection_valued_input_parameter }</code><p>
 * <p>
 * <div nowrap>Example: </code><b>SELECT</b> c <b>FROM</b> Customer c <b>WHERE</b> c.home.city <b>IN</b>(:city)</p>
 * <p>
 * <div nowrap>Example: </code><b>SELECT</b> p <b>FROM</b> Project p <b>WHERE</b> <b>TYPE</b>(p) <b>IN</b>(LargeProject, SmallProject)</p>
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
public final class InExpression extends AbstractExpression {

	/**
	 * The expression before the 'IN' identifier used for identification.
	 */
	private AbstractExpression expression;

	/**
	 * Flag used to determine if the closing parenthesis is present in the query.
	 */
	private boolean hasLeftParenthesis;

	/**
	 * Determines whether this expression is negated or not.
	 */
	private boolean hasNot;

	/**
	 * Flag used to determine if the opening parenthesis is present in the query.
	 */
	private boolean hasRightParenthesis;

	/**
	 * The expression within parenthesis, which can be one or many expressions.
	 */
	private AbstractExpression inItems;

	/**
	 * Creates a new <code>InExpression</code>.
	 *
	 * @param parent The parent of this expression
	 * @param expression The state field path expression that was parsed prior of parsing this
	 * expression
	 */
	InExpression(AbstractExpression parent, AbstractExpression expression) {
		super(parent, IN);

		if (expression != null) {
			this.expression = expression;
			this.expression.setParent(this);
		}
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
		getExpression().accept(visitor);
		getInItems().accept(visitor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void addChildrenTo(Collection<Expression> children) {
		children.add(getExpression());
		children.add(getInItems());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void addOrderedChildrenTo(List<StringExpression> children) {

		// State field path expression or type discriminator
		if (expression != null) {
			children.add(expression);
		}

		// 'NOT'
		if (hasNot) {
			children.add(buildStringExpression(SPACE));
			children.add(buildStringExpression(NOT));
		}
		else if (hasExpression()) {
			children.add(buildStringExpression(SPACE));
		}

		// 'IN'
		children.add(buildStringExpression(IN));

		// '('
		if (hasLeftParenthesis) {
			children.add(buildStringExpression(LEFT_PARENTHESIS));
		}
		else if (hasInItems()) {
			children.add(buildStringExpression(SPACE));
		}

		// In items
		if (inItems != null) {
			children.add(inItems);
		}

		// ')'
		if (hasRightParenthesis) {
			children.add(buildStringExpression(RIGHT_PARENTHESIS));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPQLQueryBNF findQueryBNF(AbstractExpression expression) {

		if (this.expression == expression) {
			return queryBNF(InExpressionExpressionBNF.ID);
		}

		if (inItems.isAncestor(expression)) {
			return queryBNF(InItemBNF.ID);
		}

		return super.findQueryBNF(expression);
	}

	/**
	 * Returns the {@link Expression} that represents the state field path expression or type
	 * discriminator.
	 *
	 * @return The expression that was parsed representing the state field path expression or the
	 * type discriminator
	 */
	public Expression getExpression() {
		if (expression == null) {
			expression = buildNullExpression();
		}
		return expression;
	}

	/**
	 * Returns the identifier for this expression.
	 *
	 * @return Either <b>IS IN</b> or <b>IN</b>
	 */
	public String getIdentifier() {
		return hasNot ? NOT_IN : IN;
	}

	/**
	 * Returns the {@link Expression} that represents the list if items.
	 *
	 * @return The expression that was parsed representing the list of items
	 */
	public Expression getInItems() {
		if (inItems == null) {
			inItems = buildNullExpression();
		}
		return inItems;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPQLQueryBNF getQueryBNF() {
		return queryBNF(InExpressionBNF.ID);
	}

	/**
	 * Determines whether the state field path expression or type discriminator was parsed.
	 *
	 * @return <code>true</code> if the state field path expression or type discriminator was parsed;
	 * <code>false</code> if it was not parsed
	 */
	public boolean hasExpression() {
		return expression != null &&
		      !expression.isNull();
	}

	/**
	 * Determines whether the list of items was parsed.
	 *
	 * @return <code>true</code> if at least one item was parsed; <code>false</code>
	 * otherwise
	 */
	public boolean hasInItems() {
		return inItems != null &&
		      !inItems.isNull();
	}

	/**
	 * Determines whether the open parenthesis was parsed or not.
	 *
	 * @return <code>true</code> if the open parenthesis was present in the string version of the
	 * query; <code>false</code> otherwise
	 */
	public boolean hasLeftParenthesis() {
		return hasLeftParenthesis;
	}

	/**
	 * Determines whether the identifier <b>NOT</b> was parsed.
	 *
	 * @return <code>true</code> if the identifier <b>NOT</b> was parsed; <code>false</code> otherwise
	 */
	public boolean hasNot() {
		return hasNot;
	}

	/**
	 * Determines whether the close parenthesis was parsed or not.
	 *
	 * @return <code>true</code> if the close parenthesis was present in the string version of the
	 * query; <code>false</code> otherwise
	 */
	public boolean hasRightParenthesis() {
		return hasRightParenthesis;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void parse(WordParser wordParser, boolean tolerant) {

		// Parse 'NOT'
		hasNot = wordParser.startsWithIgnoreCase('N');

		if (hasNot) {
			wordParser.moveForward(NOT);
			wordParser.skipLeadingWhitespace();
		}

		// Parse 'IN'
		wordParser.moveForward(IN);

		int count = wordParser.skipLeadingWhitespace();

		// Parse '('
		hasLeftParenthesis = wordParser.startsWith(LEFT_PARENTHESIS);

		if (hasLeftParenthesis) {
			wordParser.moveForward(1);
			count = wordParser.skipLeadingWhitespace();
		}

		// Parse the in items or sub-query
		inItems = parse(
			wordParser,
			queryBNF(InItemBNF.ID),
			tolerant
		);

		if (hasInItems()) {
			count = wordParser.skipLeadingWhitespace();
		}

		// Parse ')'
		hasRightParenthesis = wordParser.startsWith(RIGHT_PARENTHESIS);

		if (hasRightParenthesis) {
			wordParser.moveForward(1);
		}
		else {
			wordParser.moveBackward(count);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void toParsedText(StringBuilder writer, boolean includeVirtual) {

		// State field path expression or type discriminator
		if (hasExpression()) {
			expression.toParsedText(writer, includeVirtual);
		}

		if (hasExpression()) {
			writer.append(SPACE);
		}

		// 'IN'
		if (hasNot) {
			writer.append(NOT);
			writer.append(SPACE);
		}

		// 'IN'
		writer.append(IN);

		// '('
		if (hasLeftParenthesis) {
			writer.append(LEFT_PARENTHESIS);
		}
		else if (hasInItems()) {
			writer.append(SPACE);
		}

		// IN items
		if (hasInItems()) {
			inItems.toParsedText(writer, includeVirtual);
		}

		// ')'
		if (hasRightParenthesis) {
			writer.append(RIGHT_PARENTHESIS);
		}
	}
}