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
package org.eclipse.persistence.jpa.jpql;

import org.eclipse.persistence.jpa.jpql.ContentAssistProposals.ClassType;
import org.eclipse.persistence.jpa.jpql.parser.AbstractEclipseLinkExpressionVisitor;
import org.eclipse.persistence.jpa.jpql.parser.AbstractFromClause;
import org.eclipse.persistence.jpa.jpql.parser.AbstractPathExpression;
import org.eclipse.persistence.jpa.jpql.parser.AbstractSelectStatement;
import org.eclipse.persistence.jpa.jpql.parser.AsOfClause;
import org.eclipse.persistence.jpa.jpql.parser.CastExpression;
import org.eclipse.persistence.jpa.jpql.parser.ConnectByClause;
import org.eclipse.persistence.jpa.jpql.parser.DatabaseType;
import org.eclipse.persistence.jpa.jpql.parser.EclipseLinkExpressionVisitor;
import org.eclipse.persistence.jpa.jpql.parser.Expression;
import org.eclipse.persistence.jpa.jpql.parser.ExtractExpression;
import org.eclipse.persistence.jpa.jpql.parser.GroupByClause;
import org.eclipse.persistence.jpa.jpql.parser.HavingClause;
import org.eclipse.persistence.jpa.jpql.parser.HierarchicalQueryClause;
import org.eclipse.persistence.jpa.jpql.parser.OrderByClause;
import org.eclipse.persistence.jpa.jpql.parser.OrderByItem;
import org.eclipse.persistence.jpa.jpql.parser.OrderByItem.Ordering;
import org.eclipse.persistence.jpa.jpql.parser.OrderSiblingsByClause;
import org.eclipse.persistence.jpa.jpql.parser.PatternValueBNF;
import org.eclipse.persistence.jpa.jpql.parser.RangeVariableDeclaration;
import org.eclipse.persistence.jpa.jpql.parser.RegexpExpression;
import org.eclipse.persistence.jpa.jpql.parser.SelectStatement;
import org.eclipse.persistence.jpa.jpql.parser.StartWithClause;
import org.eclipse.persistence.jpa.jpql.parser.TableExpression;
import org.eclipse.persistence.jpa.jpql.parser.TableVariableDeclaration;
import org.eclipse.persistence.jpa.jpql.parser.UnionClause;
import org.eclipse.persistence.jpa.jpql.parser.WhereClause;
import static org.eclipse.persistence.jpa.jpql.parser.AbstractExpression.*;
import static org.eclipse.persistence.jpa.jpql.parser.Expression.*;

/**
 * This extension over the default content assist visitor adds the additional support EclipseLink
 * provides.
 * <p>
 * Provisional API: This interface is part of an interim API that is still under development and
 * expected to change significantly before reaching stability. It is available at this early stage
 * to solicit feedback from pioneering adopters on the understanding that any code that uses this
 * API will almost certainly be broken (repeatedly) as the API evolves.
 *
 * @version 2.5
 * @since 2.4
 * @author Pascal Filion
 */
@SuppressWarnings("unused") // unused used for the import statement: see bug 330740
public class EclipseLinkContentAssistVisitor extends AbstractContentAssistVisitor
                                             implements EclipseLinkExpressionVisitor {

	/**
	 * Creates a new <code>EclipseLinkContentAssistVisitor</code>.
	 *
	 * @param queryContext The context used to query information about the query
	 * @exception NullPointerException The {@link JPQLQueryContext} cannot be <code>null</code>
	 */
	public EclipseLinkContentAssistVisitor(JPQLQueryContext queryContext) {
		super(queryContext);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AcceptableTypeVisitor buildAcceptableTypeVisitor() {
		return new AcceptableTypeVisitor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AppendableExpressionVisitor buildAppendableExpressionVisitor() {
		return new AppendableExpressionVisitor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ConditionalExpressionCompletenessVisitor buildConditionalExpressionCompletenessVisitor() {
		return new ConditionalExpressionCompletenessVisitor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected FromClauseCollectionHelper buildFromClauseCollectionHelper() {
		return new FromClauseCollectionHelper();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected FromClauseSelectStatementHelper buildFromClauseSelectStatementHelper() {
		return new FromClauseSelectStatementHelper();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected GroupByClauseCollectionHelper buildGroupByClauseCollectionHelper() {
		return new GroupByClauseCollectionHelper();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected GroupByClauseSelectStatementHelper buildGroupByClauseSelectStatementHelper() {
		return new GroupByClauseSelectStatementHelper();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HavingClauseSelectStatementHelper buildHavingClauseSelectStatementHelper() {
		return new HavingClauseSelectStatementHelper();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected OrderByClauseSelectStatementHelper buildOrderByClauseSelectStatementHelper() {
		return new OrderByClauseSelectStatementHelper();
	}

	protected TableExpressionVisitor buildTableExpressionVisitor() {
		return new TableExpressionVisitor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected TrailingCompletenessVisitor buildTrailingCompleteness() {
		return new TrailingCompletenessVisitor();
	}

	protected UnionClauseSelectStatementHelper buildUnionClauseSelectStatementHelper() {
		return new UnionClauseSelectStatementHelper();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected WhereClauseSelectStatementHelper buildWhereClauseSelectStatementHelper() {
		return new WhereClauseSelectStatementHelper();
	}

	protected TableExpressionVisitor getTableExpressionVisitor() {
		TableExpressionVisitor visitor = getHelper(TableExpressionVisitor.class);
		if (visitor == null) {
			visitor = buildTableExpressionVisitor();
			registerHelper(TableExpressionVisitor.class, visitor);
		}
		return visitor;
	}

	protected String getTableName(String variableName) {

		Declaration declaration = queryContext.getDeclaration(variableName);

		if ((declaration != null) &&
		    (declaration.baseExpression != null) &&
		     isTableExpression(declaration.baseExpression)) {

	   	return queryContext.literal(declaration.baseExpression, LiteralType.STRING_LITERAL);
		}

		return null;
	}

	protected UnionClauseSelectStatementHelper getUnionClauseSelectStatementHelper() {
		UnionClauseSelectStatementHelper helper = getHelper(UnionClauseSelectStatementHelper.class);
		if (helper == null) {
			helper = buildUnionClauseSelectStatementHelper();
			registerHelper(UnionClauseSelectStatementHelper.class, helper);
		}
		return helper;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isJoinFetchIdentifiable() {
		EclipseLinkVersion version = EclipseLinkVersion.value(getGrammar().getProviderVersion());
		return version.isNewerThanOrEqual(EclipseLinkVersion.VERSION_2_4);
	}

	protected boolean isTableExpression(Expression expression) {
		TableExpressionVisitor visitor = getTableExpressionVisitor();
		try {
			visitor.expression = expression;
			expression.accept(visitor);
			return visitor.valid;
		}
		finally {
			visitor.valid = false;
			visitor.expression = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(AsOfClause expression) {
		super.visit(expression);
		// TODO
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(CastExpression expression) {
		super.visit(expression);
		int position = getPosition(expression) - corrections.peek();
		String identifier = expression.getIdentifier();

		// Within CAST
		if (isPositionWithin(position, identifier)) {
			proposals.addIdentifier(identifier);
		}
		// After "CAST("
		else if (expression.hasLeftParenthesis()) {
			int length = identifier.length() + 1 /* '(' */;

			// Right after "CAST("
			if (position == length) {
				addAllIdentificationVariables();
				addAllFunctions(expression.encapsulatedExpressionBNF());
			}
			else if (expression.hasExpression()) {
				Expression scalarExpression = expression.getExpression();

				if (isComplete(scalarExpression)) {
					length += length(scalarExpression);

					if (expression.hasSpaceAfterExpression()) {
						length++;

						// Right before "AS" or database type
						if (position == length) {
							addAllAggregates(expression.encapsulatedExpressionBNF());
							proposals.addIdentifier(AS);
						}
						// Within "AS"
						else if (isPositionWithin(position, length, AS)) {
							proposals.addIdentifier(AS);
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(ConnectByClause expression) {
		super.visit(expression);
		// TODO: 2.5
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(DatabaseType expression) {
		super.visit(expression);
		// Nothing to do, this is database specific
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(ExtractExpression expression) {
		super.visit(expression);
		int position = getPosition(expression) - corrections.peek();
		String identifier = expression.getIdentifier();

		// Within "EXTRACT"
		if (isPositionWithin(position, identifier)) {
			proposals.addIdentifier(identifier);
		}
		// After "EXTRACT("
		else if (expression.hasLeftParenthesis()) {
			int length = identifier.length() + 1 /* '(' */;

			// Right after "EXTRACT("
			if (position == length) {
				// Nothing to do, unless we show basic date parts
			}

			if (expression.hasDatePart()) {
				String datePart = expression.getDatePart();

				// Within "<date part>"
				if (isPositionWithin(position, length, datePart)) {
					// Nothing to do, unless we show basic date parts
				}

				length += datePart.length();

				// After "<date part> "
				if (expression.hasSpaceAfterDatePart()) {
					length++;

					// Right before "FROM"
					if (position == length) {
						addIdentifier(FROM);

						// Only add the scalar expression's functions if it is not specified
						// or the FROM identifier is not present
						if (!expression.hasExpression() || !expression.hasFrom()) {
							addAllIdentificationVariables();
							addAllFunctions(expression.encapsulatedExpressionBNF());
						}
					}
				}
			}

			if (expression.hasFrom()) {

				// Within "FROM"
				if (isPositionWithin(position, length, FROM)) {
					proposals.addIdentifier(FROM);

					// Only add the scalar expression's functions if it is not specified
					if (!expression.hasExpression()) {
						addAllIdentificationVariables();
						addAllFunctions(expression.encapsulatedExpressionBNF());
					}
				}

				length += 4 /* FROM */;

				if (expression.hasSpaceAfterFrom()) {
					length++;
				}

				// Right after "FROM "
				if (position == length) {
					addAllIdentificationVariables();
					addAllFunctions(expression.encapsulatedExpressionBNF());
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(HierarchicalQueryClause expression) {
		super.visit(expression);
		// TODO: 2.5
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(OrderByItem expression) {
		super.visit(expression);
		int position = getPosition(expression) - corrections.peek();

		// After the order by item
		if (expression.hasExpression()) {
			int length = length(expression.getExpression());

			if (expression.hasSpaceAfterExpression()) {
				length++;

				// Right after the order by item
				if (position == length) {

					// Only add "NULLS FIRST" and "NULLS LAST" if the ordering is not specified
					if (expression.getOrdering() == Ordering.DEFAULT) {
						proposals.addIdentifier(NULLS_FIRST);
						proposals.addIdentifier(NULLS_LAST);
					}
				}
				else {
					length += expression.getActualOrdering().length();

					if (position > length) {
						if (expression.hasSpaceAfterOrdering()) {
							length += SPACE_LENGTH;

							// Right before "NULLS FIRST" or "NULLS LAST"
							if (position == length) {
								proposals.addIdentifier(NULLS_FIRST);
								proposals.addIdentifier(NULLS_LAST);
							}
							else {
								String nullOrdering = expression.getActualNullOrdering();

								// Within "NULLS FIRST" or "NULLS LAST"
								if (isPositionWithin(position, length, nullOrdering)) {
									proposals.addIdentifier(NULLS_FIRST);
									proposals.addIdentifier(NULLS_LAST);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(OrderSiblingsByClause expression) {
		if (!isLocked(expression)) {
			super.visit(expression);
			visitCollectionExpression(expression, ORDER_SIBLINGS_BY, getOrderByClauseCollectionHelper());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void visit(RangeVariableDeclaration expression) {
		super.visit(expression);
		int position = getPosition(expression) - corrections.peek();

		// Within the entity name or fully qualified class name
		if (expression.hasRootObject()) {
			Expression rootObject = expression.getRootObject();
			int length = length(rootObject);

			// Right after "<abstract schema name> "
			if ((position >= 0) && (position <= length)) {
				String root = rootObject.toActualText();
				proposals.setClassNamePrefix(root.substring(0, position), ClassType.INSTANTIABLE);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(RegexpExpression expression) {
		super.visit(expression);
		int position = getPosition(expression) - corrections.peek();
		int length = 0;

		if (expression.hasStringExpression()) {
			length += length(expression.getStringExpression());

			if (expression.hasSpaceAfterStringExpression()) {
				length += SPACE_LENGTH;
			}
		}

		// Within "REGEXP"
		if (isPositionWithin(position, length, REGEXP)) {
			proposals.addIdentifier(REGEXP);
		}
		// After "REGEXP"
		else {
			length += 6 /* REGEXP */;

			// After "REGEXP "
			if (expression.hasSpaceAfterIdentifier()) {
				length += SPACE_LENGTH;

				// Right after "REGEXP "
				addAllIdentificationVariables();
				addAllFunctions(PatternValueBNF.ID);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(StartWithClause expression) {
		if (!isLocked(expression)) {
			super.visit(expression);
			visitClause(expression, START_WITH, expression.hasSpaceAfterIdentifier(), abstractConditionalClauseHelper());
			visitCompoundableExpression(expression);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(TableExpression expression) {
		super.visit(expression);
		int position = getPosition(expression);

		// Within "TABLE"
		if (isPositionWithin(position, TABLE)) {
			proposals.addIdentifier(TABLE);
		}
		// After '('
		else if (expression.hasLeftParenthesis()) {
			int length = TABLE.length() + SPACE_LENGTH;

			// Right after '('
			if (position == length) {
				proposals.setTableNamePrefix(ExpressionTools.EMPTY_STRING);
			}
			else {
				Expression nameExpression = expression.getExpression();
				String tableName = queryContext.literal(nameExpression, LiteralType.STRING_LITERAL);

				if (tableName.length() == 0) {
					tableName = queryContext.literal(nameExpression, LiteralType.IDENTIFICATION_VARIABLE);
				}

				int tableNameLength = tableName.length();

				// Within the string literal representing the table name
				if ((position > length) && (position <= length + tableNameLength)) {
					String prefix = tableName.substring(0, position - length);
					prefix = ExpressionTools.unquote(prefix);
					proposals.setTableNamePrefix(prefix);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(TableVariableDeclaration expression) {
		super.visit(expression);

		TableExpression tableExpression = expression.getTableExpression();
		int position = getPosition(expression) - corrections.peek();
		int length = length(tableExpression);

		// After "TABLE()"
		if (expression.hasSpaceAfterTableExpression()) {
			length += SPACE_LENGTH;

			// Right after "TABLE() "
			if (isPositionWithin(position, length, AS)) {
				addIdentifier(AS);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void visit(UnionClause expression) {
//		super.visit(expression);
		int position = getPosition(expression) - corrections.peek();
		String identifier = expression.getIdentifier();

		// Within <identifier>
		if (isPositionWithin(position, identifier)) {
			proposals.addIdentifier(EXCEPT);
			proposals.addIdentifier(INTERSECT);
			proposals.addIdentifier(UNION);
		}
		// After "<identifier> "
		else if (expression.hasSpaceAfterIdentifier()) {
			int length = identifier.length() + SPACE_LENGTH;

			// Right after "<identifier> "
			if (position == length) {
				proposals.addIdentifier(ALL);

				if (!expression.hasAll()) {
					addIdentifier(SELECT);
				}
			}
			// Within "ALL"
			else if (isPositionWithin(position, length, ALL)) {
				addIdentifier(ALL);
			}
			else {
				if ((position == length) && !expression.hasAll()) {
					proposals.addIdentifier(SELECT);
				}
				else {

					if (expression.hasAll()) {
						length += 3 /* ALL */;
					}

					// After "ALL "
					if (expression.hasSpaceAfterAll()) {
						length += SPACE_LENGTH;

						// Right after "ALL "
						if (position == length) {
							proposals.addIdentifier(SELECT);
						}
					}
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void visitThirdPartyPathExpression(AbstractPathExpression expression,
	                                             String variableName) {

		// Check to see if a column name can be resolved
		int position = getPosition(expression);
		String text = expression.toActualText();
		int dotIndex = text.indexOf(DOT);
		int secondDotIndex = (dotIndex > -1) ? text.indexOf(DOT, dotIndex + 1) : -1;

		// The cursor position is after the first dot and either there is no second dot or the
		// position is before the second dot, which means a table name and column names could
		// potentially be resolved
		if ((secondDotIndex == -1) || (position < secondDotIndex)) {
			String tableName = getTableName(variableName);

			if (tableName != ExpressionTools.EMPTY_STRING) {
				tableName = ExpressionTools.unquote(tableName);
				proposals.setTableName(tableName, text.substring(dotIndex + 1, position));
			}
		}
	}

	protected class AcceptableTypeVisitor extends AbstractContentAssistVisitor.AcceptableTypeVisitor {
	}

	protected class AppendableExpressionVisitor extends AbstractContentAssistVisitor.AppendableExpressionVisitor
	                                            implements EclipseLinkExpressionVisitor {

		/**
		 * {@inheritDoc}
		 */
		public void visit(AsOfClause expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(CastExpression expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(ConnectByClause expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(DatabaseType expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(ExtractExpression expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(HierarchicalQueryClause expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(OrderSiblingsByClause expression) {
			clauseOfItems = true;
			super.visit(expression);
			clauseOfItems = false;
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(RegexpExpression expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(StartWithClause expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(TableExpression expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(TableVariableDeclaration expression) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(UnionClause expression) {
		}
	}

	protected class ConditionalExpressionCompletenessVisitor extends AbstractContentAssistVisitor.ConditionalExpressionCompletenessVisitor
	                                                         implements EclipseLinkExpressionVisitor {

		/**
		 * {@inheritDoc}
		 */
		public void visit(AsOfClause expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(CastExpression expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(ConnectByClause expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(DatabaseType expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(ExtractExpression expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(HierarchicalQueryClause expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(OrderSiblingsByClause expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(RegexpExpression expression) {
			complete = expression.hasPatternValue();
			if (complete) {
				complete = isComplete(expression.getPatternValue());
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(StartWithClause expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(TableExpression expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(TableVariableDeclaration expression) {
			// Not part of a conditional expression
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(UnionClause expression) {
			// Not part of a conditional expression
		}
	}

	protected class FromClauseCollectionHelper extends AbstractContentAssistVisitor.FromClauseCollectionHelper {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addProposals(AbstractFromClause expression, int index) {
			super.addProposals(expression, index);
			proposals.setClassNamePrefix(ExpressionTools.EMPTY_STRING, ClassType.INSTANTIABLE);
		}
	}

	protected class FromClauseSelectStatementHelper extends AbstractContentAssistVisitor.FromClauseSelectStatementHelper {

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void addClauseIdentifierProposals(SelectStatement expression) {

			super.addClauseIdentifierProposals(expression);

			if (!expression.hasWhereClause()) {
				if (!expression.hasGroupByClause()) {
					if (!expression.hasHavingClause()) {
						if (!expression.hasOrderByClause()) {
							addIdentifier(UNION);
							addIdentifier(EXCEPT);
							addIdentifier(INTERSECT);
						}
					}
				}
			}
		}
	}

	protected class GroupByClauseCollectionHelper extends AbstractContentAssistVisitor.GroupByClauseCollectionHelper {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void addAtTheEndOfChild(GroupByClause expression, Expression child, int index) {
//			addIdentifier(EXCEPT);
//			addIdentifier(INTERSECT);
//			addIdentifier(UNION);
		}
	}

	protected class GroupByClauseSelectStatementHelper extends AbstractContentAssistVisitor.GroupByClauseSelectStatementHelper {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void appendNextClauseProposals(SelectStatement expression,
		                                      GroupByClause clause,
		                                      int position,
		                                      boolean complete) {

			super.appendNextClauseProposals(expression, clause, position, complete);

			if (complete || isGroupByComplete(clause)) {
				if (!expression.hasHavingClause()) {
					if (!expression.hasOrderByClause()) {
						addIdentifier(EXCEPT);
						addIdentifier(INTERSECT);
						addIdentifier(UNION);
					}
				}
			}
		}
	}

	protected class HavingClauseSelectStatementHelper extends AbstractContentAssistVisitor.HavingClauseSelectStatementHelper {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void appendNextClauseProposals(SelectStatement expression,
		                                      HavingClause clause,
		                                      int position,
		                                      boolean complete) {

			super.appendNextClauseProposals(expression, clause, position, complete);

			if (complete || isAppendable(clause)) {
				if (!expression.hasOrderByClause()) {
					addIdentifier(EXCEPT);
					addIdentifier(INTERSECT);
					addIdentifier(UNION);
				}
			}
		}
	}

	protected class OrderByClauseSelectStatementHelper extends AbstractContentAssistVisitor.OrderByClauseSelectStatementHelper {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void appendNextClauseProposals(SelectStatement expression,
		                                      OrderByClause clause,
		                                      int position,
		                                      boolean complete) {

			super.appendNextClauseProposals(expression, clause, position, complete);

			if (complete || isAppendable(clause)) {
				addIdentifier(EXCEPT);
				addIdentifier(INTERSECT);
				addIdentifier(UNION);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public UnionClauseSelectStatementHelper getNextHelper() {
			return getUnionClauseSelectStatementHelper();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean hasSpaceAfterClause(SelectStatement expression) {
			return expression.hasSpaceBeforeUnion();
		}
	}

	protected class TableExpressionVisitor extends AbstractEclipseLinkExpressionVisitor {

		/**
		 * The {@link Expression} being visited.
		 */
		protected Expression expression;

		/**
		 * <code>true</code> if the {@link Expression} being visited is a {@link TableExpression}.
		 */
		protected boolean valid;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(TableExpression expression) {
			valid = (this.expression == expression);
		}
	}

	protected class TrailingCompletenessVisitor extends AbstractContentAssistVisitor.TrailingCompletenessVisitor
	                                            implements EclipseLinkExpressionVisitor {

		/**
		 * {@inheritDoc}
		 */
		public void visit(AsOfClause expression) {

			complete = expression.hasExpression();

			if (complete) {
				expression.getExpression().accept(this);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(CastExpression expression) {
			complete = expression.hasRightParenthesis();
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(ConnectByClause expression) {
			complete = expression.hasExpression();
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(DatabaseType expression) {
			// Always complete since it's a single word
		}


		/**
		 * {@inheritDoc}
		 */
		public void visit(ExtractExpression expression) {
			complete = expression.hasRightParenthesis();
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(HierarchicalQueryClause expression) {

			if (expression.hasOrderSiblingsByClause()) {
				expression.getOrderSiblingsByClause().accept(this);
			}
			else {
				complete = expression.hasConnectByClause();

				if (complete) {
					expression.getConnectByClause().accept(this);
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(OrderSiblingsByClause expression) {

			complete = expression.hasOrderByItems();

			if (complete) {
				expression.getOrderByItems().accept(this);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(RegexpExpression expression) {
			complete = expression.hasPatternValue();
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(StartWithClause expression) {

			complete = expression.hasConditionalExpression();

			if (complete) {
				expression.getConditionalExpression().accept(this);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(TableExpression expression) {
			complete = expression.hasRightParenthesis();
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(TableVariableDeclaration expression) {
			complete = expression.hasIdentificationVariable();
		}

		/**
		 * {@inheritDoc}
		 */
		public void visit(UnionClause expression) {

			complete = expression.hasQuery();

			if (complete) {
				expression.getQuery().accept(this);
			}
		}
	}

	protected class UnionClauseSelectStatementHelper implements SelectStatementHelper<SelectStatement, UnionClause> {

		/**
		 * {@inheritDoc}
		 */
		public void addClauseProposal() {
			addIdentifier(EXCEPT);
			addIdentifier(INTERSECT);
			addIdentifier(UNION);
		}

		/**
		 * {@inheritDoc}
		 */
		public void appendNextClauseProposals(SelectStatement expression,
		                                      UnionClause clause,
		                                      int position,
		                                      boolean complete) {

			addIdentifier(EXCEPT);
			addIdentifier(INTERSECT);
			addIdentifier(UNION);
		}

		/**
		 * {@inheritDoc}
		 */
		public UnionClause getClause(SelectStatement expression) {
			return (UnionClause) expression.getUnionClauses();
		}

		/**
		 * {@inheritDoc}
		 */
		public Expression getClauseExpression(UnionClause clause) {
			return clause.getQuery();
		}

		/**
		 * {@inheritDoc}
		 */
		public SelectStatementHelper<? extends AbstractSelectStatement, ? extends Expression> getNextHelper() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public AbstractContentAssistVisitor.OrderByClauseSelectStatementHelper getPreviousHelper() {
			return getOrderByClauseSelectStatementHelper();
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean hasClause(SelectStatement expression) {
			return expression.hasUnionClauses();
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean hasClauseExpression(UnionClause clause) {
			return clause.hasQuery();
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean hasSpaceAfterClause(SelectStatement expression) {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean hasSpaceBeforeClause(SelectStatement expression) {
			return expression.hasSpaceBeforeUnion();
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isClauseExpressionComplete(Expression expression) {
			return false;
		}
	}

	protected class WhereClauseSelectStatementHelper extends AbstractContentAssistVisitor.WhereClauseSelectStatementHelper {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void appendNextClauseProposals(SelectStatement expression,
		                                      WhereClause clause,
		                                      int position,
		                                      boolean complete) {


			super.appendNextClauseProposals(expression, clause, position, complete);

			if (complete || isAppendable(clause)) {
				if (!expression.hasGroupByClause()) {
					if (!expression.hasHavingClause()) {
						if (!expression.hasOrderByClause()) {
							addIdentifier(EXCEPT);
							addIdentifier(INTERSECT);
							addIdentifier(UNION);
						}
					}
				}
			}
		}
	}
}