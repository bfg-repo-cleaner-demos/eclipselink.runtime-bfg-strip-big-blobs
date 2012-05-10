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
package org.eclipse.persistence.jpa.tests.jpql.parser;

import org.junit.Test;

@SuppressWarnings("nls")
public final class ResultVariableTest extends JPQLParserTest {

	@Test
	public void testBuildExpression_01() {

		String query = "SELECT e AS n FROM Employee e";

		ExpressionTester selectStatement = selectStatement(
			select(resultVariableAs(variable("e"), "n")),
			from("Employee", "e")
		);

		testQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_02() {

		String query = "SELECT e n FROM Employee e";

		ExpressionTester selectStatement = selectStatement(
			select(resultVariable(variable("e"), "n")),
			from("Employee", "e")
		);

		testQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_03() {

		String query = "SELECT AVG(e.age) AS g FROM Employee e";

		ExpressionTester selectStatement = selectStatement(
			select(resultVariableAs(avg("e.age"), "g")),
			from("Employee", "e")
		);

		testQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_04() {

		String query = "SELECT AVG(e.age) g FROM Employee e";

		ExpressionTester selectStatement = selectStatement(
			select(resultVariable(avg("e.age"), "g")),
			from("Employee", "e")
		);

		testQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_05() {

		String query = "SELECT AVG(e.age) + 2 AS g FROM Employee e";

		ExpressionTester selectStatement = selectStatement(
			select(resultVariableAs(avg("e.age").add(numeric(2)), "g")),
			from("Employee", "e")
		);

		testQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_06() {

		String query = "SELECT AVG(e.age) + 2 AS g FROM Employee e";

		ExpressionTester selectStatement = selectStatement(
			select(resultVariableAs(avg("e.age").add(numeric(2)), "g")),
			from("Employee", "e")
		);

		testQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_07() {

		String query = "SELECT AVG(e.age) AS g, e.name AS n FROM Employee e";

		ExpressionTester selectStatement = selectStatement(
			select(
				resultVariableAs(avg("e.age"), "g"),
				resultVariableAs(path("e.name"), "n")
			),
			from("Employee", "e")
		);

		testQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_08() {

		String query = "SELECT AVG(e.age) g, e.name n FROM Employee e";

		ExpressionTester selectStatement = selectStatement(
			select(
				resultVariable(avg("e.age"), "g"),
				resultVariable(path("e.name"), "n")
			),
			from("Employee", "e")
		);

		testQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_09() {

		String query = "SELECT AVG(e.age) AS";

		ResultVariableTester resultVariable = resultVariableAs(avg(path("e.age")), nullExpression());
		resultVariable.hasSpaceAfterAs = false;

		ExpressionTester selectStatement = selectStatement(
			select(resultVariable),
			nullExpression()
		);

		testInvalidQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_10() {

		String query = "SELECT AVG(e.age) AS ";

		ExpressionTester selectStatement = selectStatement(
			select(resultVariableAs(avg(path("e.age")), nullExpression())),
			nullExpression()
		);

		testInvalidQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_11() {

		String query = "SELECT AS";

		ResultVariableTester resultVariable = resultVariableAs(nullExpression(), nullExpression());
		resultVariable.hasSpaceAfterAs = false;

		ExpressionTester selectStatement = selectStatement(
			select(resultVariable),
			nullExpression()
		);

		testInvalidQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_12() {

		String query = "SELECT AS ";

		ExpressionTester selectStatement = selectStatement(
			select(resultVariableAs(nullExpression(), nullExpression())),
			nullExpression()
		);

		testInvalidQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_13() {

		String query = "SELECT AS n";

		ExpressionTester selectStatement = selectStatement(
			select(resultVariableAs(nullExpression(), "n")),
			nullExpression()
		);

		testInvalidQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_14() {

		String query = "SELECT e AS emp FROM Employee e ORDER BY emp";

		SelectStatementTester selectStatement = selectStatement(
			select(resultVariableAs(variable("e"), "emp")),
			from("Employee", "e"),
			nullExpression(),
			nullExpression(),
			nullExpression(),
			orderBy(orderByItem(variable("emp")))
		);

		testQuery(query, selectStatement);
	}

	@Test
	public void testBuildExpression_15() {

		String query = "SELECT e.name, AVG(e.age) AS age FROM Employee e ORDER BY age";

		SelectStatementTester selectStatement = selectStatement(
			select(path("e.name"), resultVariableAs(avg("e.age"), "age")),
			from("Employee", "e"),
			nullExpression(),
			nullExpression(),
			nullExpression(),
			orderBy(orderByItem(variable("age")))
		);

		testQuery(query, selectStatement);
	}
}