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
package org.eclipse.persistence.jpa.tests.internal.jpql.parser;

import org.eclipse.persistence.jpa.internal.jpql.parser.CollectionExpression;
import org.eclipse.persistence.jpa.internal.jpql.parser.Expression;
import org.eclipse.persistence.jpa.internal.jpql.parser.IdentificationVariable;
import org.eclipse.persistence.jpa.internal.jpql.parser.JPQLExpression;
import org.eclipse.persistence.jpa.internal.jpql.parser.NullExpression;
import org.eclipse.persistence.jpa.internal.jpql.parser.ObjectExpression;
import org.eclipse.persistence.jpa.internal.jpql.parser.SelectClause;
import org.eclipse.persistence.jpa.internal.jpql.parser.SelectStatement;
import org.junit.Test;

import static org.junit.Assert.*;

@SuppressWarnings("nls")
public final class ObjectExpressionTest extends AbstractJPQLTest {
	@Test
	public void testBuildExpression_01() {
		String query = "SELECT OBJECT(e) FROM Employee e";
		JPQLExpression jpqlExpression = JPQLQueryBuilder.buildQuery(query);

		// SelectStatement
		Expression expression = jpqlExpression.getQueryStatement();
		assertTrue(expression instanceof SelectStatement);
		SelectStatement selectStatement = (SelectStatement) expression;

		// SelectClause
		expression = selectStatement.getSelectClause();
		assertTrue(expression instanceof SelectClause);
		SelectClause selectClause = (SelectClause) expression;

		// ObjectExpression
		expression = selectClause.getSelectExpression();
		assertTrue(expression instanceof ObjectExpression);
		ObjectExpression objectExpression = (ObjectExpression) expression;

		assertTrue(objectExpression.hasLeftParenthesis());
		assertTrue(objectExpression.hasRightParenthesis());

		// IdentificationVariable
		expression = objectExpression.getExpression();
		assertTrue(expression instanceof IdentificationVariable);
		IdentificationVariable identificationVariable = (IdentificationVariable) expression;

		assertEquals("e", identificationVariable.toParsedText());
	}

	@Test
	public void testBuildExpression_02() {
		String query = "SELECT OBJECT FROM Employee e";
		JPQLExpression jpqlExpression = JPQLQueryBuilder.buildQuery(query);

		// SelectStatement
		Expression expression = jpqlExpression.getQueryStatement();
		assertTrue(expression instanceof SelectStatement);
		SelectStatement selectStatement = (SelectStatement) expression;

		// SelectClause
		expression = selectStatement.getSelectClause();
		assertTrue(expression instanceof SelectClause);
		SelectClause selectClause = (SelectClause) expression;

		// ObjectExpression
		expression = selectClause.getSelectExpression();
		assertTrue(expression instanceof ObjectExpression);
		ObjectExpression objectExpression = (ObjectExpression) expression;

		assertFalse(objectExpression.hasLeftParenthesis());
		assertFalse(objectExpression.hasRightParenthesis());

		// NullExpression
		expression = objectExpression.getExpression();
		assertTrue(expression instanceof NullExpression);
	}

	@Test
	public void testBuildExpression_03() {
		String query = "SELECT OBJECT( FROM Employee e";
		JPQLExpression jpqlExpression = JPQLQueryBuilder.buildQuery(query);

		// SelectStatement
		Expression expression = jpqlExpression.getQueryStatement();
		assertTrue(expression instanceof SelectStatement);
		SelectStatement selectStatement = (SelectStatement) expression;

		// SelectClause
		expression = selectStatement.getSelectClause();
		assertTrue(expression instanceof SelectClause);
		SelectClause selectClause = (SelectClause) expression;

		// ObjectExpression
		expression = selectClause.getSelectExpression();
		assertTrue(expression instanceof ObjectExpression);
		ObjectExpression objectExpression = (ObjectExpression) expression;

		assertTrue (objectExpression.hasLeftParenthesis());
		assertFalse(objectExpression.hasRightParenthesis());

		// NullExpression
		expression = objectExpression.getExpression();
		assertTrue(expression instanceof NullExpression);
	}

	@Test
	public void testBuildExpression_04() {
		String query = "SELECT OBJECT() FROM Employee e";
		JPQLExpression jpqlExpression = JPQLQueryBuilder.buildQuery(query);

		// SelectStatement
		Expression expression = jpqlExpression.getQueryStatement();
		assertTrue(expression instanceof SelectStatement);
		SelectStatement selectStatement = (SelectStatement) expression;

		// SelectClause
		expression = selectStatement.getSelectClause();
		assertTrue(expression instanceof SelectClause);
		SelectClause selectClause = (SelectClause) expression;

		// ObjectExpression
		expression = selectClause.getSelectExpression();
		assertTrue(expression instanceof ObjectExpression);
		ObjectExpression objectExpression = (ObjectExpression) expression;

		assertTrue(objectExpression.hasLeftParenthesis());
		assertTrue(objectExpression.hasRightParenthesis());

		// NullExpression
		expression = objectExpression.getExpression();
		assertTrue(expression instanceof NullExpression);
	}

	@Test
	public void testBuildExpression_05() {
		String query = "SELECT OBJECT, e FROM Employee e";
		JPQLExpression jpqlExpression = JPQLQueryBuilder.buildQuery(query);

		// SelectStatement
		Expression expression = jpqlExpression.getQueryStatement();
		assertTrue(expression instanceof SelectStatement);
		SelectStatement selectStatement = (SelectStatement) expression;

		// SelectClause
		expression = selectStatement.getSelectClause();
		assertTrue(expression instanceof SelectClause);
		SelectClause selectClause = (SelectClause) expression;

		// CollectionExpression
		expression = selectClause.getSelectExpression();
		assertTrue(expression instanceof CollectionExpression);
		CollectionExpression collectionExpression = (CollectionExpression) expression;

		assertEquals(2, collectionExpression.childrenSize());

		// ObjectExpression
		expression = collectionExpression.getChild(0);
		assertTrue(expression instanceof ObjectExpression);
		ObjectExpression objectExpression = (ObjectExpression) expression;

		assertFalse(objectExpression.hasLeftParenthesis());
		assertFalse(objectExpression.hasRightParenthesis());

		// NullExpression
		expression = objectExpression.getExpression();
		assertTrue(expression instanceof NullExpression);
	}

	@Test
	public void testBuildExpression_06() {
		String query = "SELECT OBJECT, e FROM Employee e";
		JPQLExpression jpqlExpression = JPQLQueryBuilder.buildQuery(query);

		// SelectStatement
		Expression expression = jpqlExpression.getQueryStatement();
		assertTrue(expression instanceof SelectStatement);
		SelectStatement selectStatement = (SelectStatement) expression;

		// SelectClause
		expression = selectStatement.getSelectClause();
		assertTrue(expression instanceof SelectClause);
		SelectClause selectClause = (SelectClause) expression;

		// CollectionExpression
		expression = selectClause.getSelectExpression();
		assertTrue(expression instanceof CollectionExpression);
		CollectionExpression collectionExpression = (CollectionExpression) expression;

		assertEquals(2, collectionExpression.childrenSize());

		// ObjectExpression
		expression = collectionExpression.getChild(0);
		assertTrue(expression instanceof ObjectExpression);
		ObjectExpression objectExpression = (ObjectExpression) expression;

		assertFalse(objectExpression.hasLeftParenthesis());
		assertFalse(objectExpression.hasRightParenthesis());

		// NullExpression
		expression = objectExpression.getExpression();
		assertTrue(expression instanceof NullExpression);
	}
}