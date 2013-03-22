/*******************************************************************************
 * Copyright (c) 2006, 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.persistence.jpa.jpql.parser.AbstractExpression;
import org.eclipse.persistence.jpa.jpql.parser.AbstractExpressionVisitor;
import org.eclipse.persistence.jpa.jpql.parser.AbstractSchemaName;
import org.eclipse.persistence.jpa.jpql.parser.AnonymousExpressionVisitor;
import org.eclipse.persistence.jpa.jpql.parser.CollectionExpression;
import org.eclipse.persistence.jpa.jpql.parser.CollectionMemberDeclaration;
import org.eclipse.persistence.jpa.jpql.parser.CollectionValuedPathExpression;
import org.eclipse.persistence.jpa.jpql.parser.DeleteClause;
import org.eclipse.persistence.jpa.jpql.parser.DeleteStatement;
import org.eclipse.persistence.jpa.jpql.parser.Expression;
import org.eclipse.persistence.jpa.jpql.parser.FromClause;
import org.eclipse.persistence.jpa.jpql.parser.IdentificationVariable;
import org.eclipse.persistence.jpa.jpql.parser.IdentificationVariableDeclaration;
import org.eclipse.persistence.jpa.jpql.parser.JPQLExpression;
import org.eclipse.persistence.jpa.jpql.parser.Join;
import org.eclipse.persistence.jpa.jpql.parser.RangeVariableDeclaration;
import org.eclipse.persistence.jpa.jpql.parser.ResultVariable;
import org.eclipse.persistence.jpa.jpql.parser.SelectClause;
import org.eclipse.persistence.jpa.jpql.parser.SelectStatement;
import org.eclipse.persistence.jpa.jpql.parser.SimpleFromClause;
import org.eclipse.persistence.jpa.jpql.parser.SimpleSelectClause;
import org.eclipse.persistence.jpa.jpql.parser.SimpleSelectStatement;
import org.eclipse.persistence.jpa.jpql.parser.SubExpression;
import org.eclipse.persistence.jpa.jpql.parser.UpdateClause;
import org.eclipse.persistence.jpa.jpql.parser.UpdateStatement;
import org.eclipse.persistence.jpa.jpql.spi.IMapping;
import org.eclipse.persistence.jpa.jpql.spi.IQuery;
import org.eclipse.persistence.jpa.jpql.spi.IType;
import org.eclipse.persistence.jpa.jpql.spi.ITypeDeclaration;

/**
 * This {@link Resolver} is responsible to visit the current query (which is either the top-level
 * query or a subquery) and gathers the information from the declaration clause. For a <b>SELECT</b>
 * or <b>DELETE</b> clause, the information will be retrieved from the <b>FROM</b> clause. For an
 * <code>UDPATE</code> clause, it will be retrieved from the unique identification range variable
 * declaration.
 *
 * @version 2.4
 * @since 2.3
 * @author Pascal Filion
 */
@SuppressWarnings("nls")
public class DeclarationResolver extends Resolver {

	/**
	 * The {@link Declaration Declarations} of the current query that was parsed.
	 */
	private List<Declaration> declarations;

	/**
	 * This visitor is responsible to visit the current query's declaration and populate this
	 * resolver with the list of declarations.
	 */
	private DeclarationVisitor declarationVisitor;

	/**
	 * This visitor is responsible to convert the abstract schema name into a path expression.
	 */
	private QualifyRangeDeclarationVisitor qualifyRangeDeclarationVisitor;

	/**
	 * The context used to query information about the query.
	 */
	private JPQLQueryContext queryContext;

	/**
	 * The identification variable names mapped to their resolvers.
	 */
	private Map<String, Resolver> resolvers;

	/**
	 * The variables identifying the select expressions, if any was defined or an empty set if none
	 * were defined.
	 */
	private Map<IdentificationVariable, String> resultVariables;

	/**
	 *
	 */
	private RootObjectExpressionVisitor rootObjectExpressionVisitor;

	/**
	 * Creates a new <code>DeclarationResolver</code>.
	 *
	 * @param parent The parent resolver if this is used for a subquery or null if it's used for the
	 * top-level query
	 * @param queryContext The context used to query information about the query
	 */
	public DeclarationResolver(DeclarationResolver parent, JPQLQueryContext queryContext) {
		super(parent);
		initialize(queryContext);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void accept(ResolverVisitor visitor) {
	}

	/**
	 * Adds the given {@link Declaration} at the end of the list.
	 *
	 * @param declaration The {@link Declaration} representing a single variable declaration
	 */
	protected final void addDeclaration(Declaration declaration) {
		declarations.add(declaration);
	}

	/**
	 * Registers a range variable declaration that will be used when a JPQL fragment is parsed.
	 *
	 * @param entityName The name of the entity to be accessible with the given variable name
	 * @param variableName The identification variable used to navigate to the entity
	 */
	public void addRangeVariableDeclaration(String entityName, String variableName) {

		// Always make the identification variable be upper case since it's
		// case insensitive, the get will also use upper case
		String internalVariableName = variableName.toUpperCase();

		// Make sure the identification variable was not declared more than once,
		// this could cause issues when trying to resolve it
		if (!resolvers.containsKey(internalVariableName)) {

			// Resolve the expression and map it with the identification variable
			Resolver resolver = new EntityResolver(this, entityName);
			resolver = new IdentificationVariableResolver(resolver, variableName);
			resolvers.put(internalVariableName, resolver);

			Declaration declaration = new Declaration();
			declaration.rangeDeclaration       = true;
			declaration.rootPath               = entityName;
			declaration.identificationVariable = new IdentificationVariable(null, variableName);
			declaration.lockData();
			declarations.add(declaration);
		}
	}

	protected DeclarationVisitor buildDeclarationVisitor() {
		return new DeclarationVisitor();
	}

	protected RootObjectExpressionVisitor buildRootObjectExpressionVisitor() {
		return new RootObjectExpressionVisitor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IType buildType() {
		return getTypeHelper().unknownType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected ITypeDeclaration buildTypeDeclaration() {
		return getTypeHelper().unknownTypeDeclaration();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void checkParent(Resolver parent) {
		// Don't do anything, this is the root
	}

	/**
	 * Converts the given {@link Declaration} from being set as a range variable declaration to
	 * a path expression declaration.
	 * <p>
	 * In this query "<code>UPDATE Employee SET firstName = 'MODIFIED' WHERE (SELECT COUNT(m) FROM
	 * managedEmployees m) > 0</code>" <em>managedEmployees</em> is an unqualified collection-valued
	 * path expression (<code>employee.managedEmployees</code>).
	 *
	 * @param declaration The {@link Declaration} that was parsed to range over an abstract schema
	 * name but is actually ranging over a path expression
	 * @param outerVariableName The identification variable coming from the parent identification
	 * variable declaration
	 */
	public void convertUnqualifiedDeclaration(Declaration declaration, String outerVariableName) {

		QualifyRangeDeclarationVisitor visitor = qualifyRangeDeclarationVisitor();

		try {
			visitor.declaration       = declaration;
			visitor.outerVariableName = outerVariableName;

			declaration.declarationExpression.accept(visitor);
		}
		finally {
			visitor.declaration       = null;
			visitor.outerVariableName = null;
		}
	}

	/**
	 * Disposes the internal data.
	 */
	public void dispose() {
		resolvers      .clear();
		declarations   .clear();
		resultVariables.clear();
	}

	/**
	 * Returns the ordered list of {@link Declaration Declarations}.
	 *
	 * @return The {@link Declaration Declarations} of the current query that was parsed
	 */
	public List<Declaration> getDeclarations() {
		return declarations;
	}

	protected DeclarationVisitor getDeclarationVisitor() {
		if (declarationVisitor == null) {
			declarationVisitor = buildDeclarationVisitor();
		}
		return declarationVisitor;
	}

	/**
	 * Returns the parsed representation of a <b>JOIN</b> and <b>JOIN FETCH</b> that were defined in
	 * the same declaration than the given range identification variable name.
	 *
	 * @param variableName The name of the identification variable that should be used to define an
	 * abstract schema name
	 * @return The <b>JOIN</b> and <b>JOIN FETCH</b> expressions used in the same declaration or an
	 * empty collection if none was defined
	 */
	public Collection<Join> getJoins(String variableName) {
		Collection<Join> joins = getJoinsImp(variableName);
		if (joins.isEmpty() && (getParent() != null)) {
			joins = getParent().getJoinsImp(variableName);
		}
		return joins;
	}

	protected Collection<Join> getJoinsImp(String variableName) {

		for (Declaration declaration : declarations) {
			if (declaration.getVariableName().equalsIgnoreCase(variableName)) {
				return declaration.getJoins();
			}
		}

		return Collections.emptyList();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DeclarationResolver getParent() {
		return (DeclarationResolver) super.getParent();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IQuery getQuery() {
		return queryContext.getQuery();
	}

	/**
	 * Returns the {@link JPQLQueryContext} that is used by this visitor.
	 *
	 * @return The {@link JPQLQueryContext} holding onto the JPQL query and the cached information
	 */
	protected JPQLQueryContext getQueryContext() {
		return queryContext;
	}

	/**
	 * Retrieves the {@link Resolver} mapped with the given identification variable. If the
	 * identification is not defined in the declaration traversed by this resolver, than the search
	 * will traverse the parent hierarchy.
	 *
	 * @param variableName The identification variable that maps a {@link Resolver}
	 * @return The {@link Resolver} mapped with the given identification variable
	 */
	public Resolver getResolver(String variableName) {

		variableName = variableName.toUpperCase();
		Resolver resolver = getResolverImp(variableName);

		if ((resolver == null) && (getParent() != null)) {
			resolver = getParent().getResolver(variableName);
		}

		if (resolver == null) {
			resolver = new NullResolver(this);
			resolvers.put(variableName, resolver);
		}

		return resolver;
	}

	protected Resolver getResolverImp(String variableName) {
		return resolvers.get(variableName);
	}

	/**
	 * Returns the variables that got defined in the select expression. This only applies to JPQL
	 * queries built for JPA 2.0 or later.
	 *
	 * @return The variables identifying the select expressions, if any was defined or an empty set
	 * if none were defined
	 */
	public Set<String> getResultVariables() {
		return new HashSet<String>(resultVariables.values());
	}

	/**
	 * Returns the map of result variables that got used to define a select expression. This only
	 * applies to JPQL queries built for JPA 2.0.
	 *
	 * @return The variables identifying the select expressions, if any was defined or an empty map
	 * if none were defined
	 */
	public Map<IdentificationVariable, String> getResultVariablesMap() {
		return resultVariables;
	}

	protected RootObjectExpressionVisitor getRootObjectExpressionVisitor() {
		if (rootObjectExpressionVisitor == null) {
			rootObjectExpressionVisitor = buildRootObjectExpressionVisitor();
		}
		return rootObjectExpressionVisitor;
	}

	/**
	 * Determines whether the JPQL expression has <b>JOIN</b> expressions.
	 *
	 * @return <code>true</code> if the query or subquery being traversed contains <b>JOIN</b>
	 * expressions; <code>false</code> otherwise
	 */
	public boolean hasJoins() {
		for (Declaration declaration : declarations) {
			if (declaration.hasJoins()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Initializes this <code>DeclarationResolver</code>.
	 *
	 * @param queryContext The context used to query information about the query
	 */
	protected void initialize(JPQLQueryContext queryContext) {
		this.queryContext    = queryContext;
		this.resolvers       = new HashMap<String, Resolver>();
		this.declarations    = new ArrayList<Declaration>();
		this.resultVariables = new HashMap<IdentificationVariable, String>();
	}

	/**
	 * Determines whether the given identification variable is defining a <b>JOIN</b> or <code>IN</code>
	 * expressions for a collection-valued field.
	 *
	 * @param variableName The identification variable to check for what it maps
	 * @return <code>true</code> if the given identification variable maps a collection-valued field
	 * defined in a <code>JOIN</code> or <code>IN</code> expression; <code>false</code> if it's not
	 * defined or it's mapping an abstract schema name
	 */
	public boolean isCollectionIdentificationVariable(String variableName) {
		boolean result = isCollectionIdentificationVariableImp(variableName);
		if (!result && (getParent() != null)) {
			result = getParent().isCollectionIdentificationVariableImp(variableName);
		}
		return result;
	}

	protected boolean isCollectionIdentificationVariableImp(String variableName) {

		for (Declaration declaration : declarations) {

			// Check for a collection member declaration
			if (!declaration.rangeDeclaration &&
			     declaration.getVariableName().equalsIgnoreCase(variableName)) {

				return true;
			}

			// Check the JOIN expressions
			for (String joinIdentificationVariable : declaration.getJoinIdentificationVariables()) {

				if (joinIdentificationVariable.equalsIgnoreCase(variableName)) {

					// Make sure the JOIN expression maps a collection mapping
					Resolver resolver = getResolver(variableName);
					IMapping mapping = (resolver != null) ? resolver.getMapping() : null;
					return (mapping != null) && mapping.isCollection();
				}
			}
		}

		return false;
	}

	/**
	 * Determines whether the given variable name is an identification variable name used to define
	 * an abstract schema name.
	 *
	 * @param variableName The name of the variable to verify if it's defined in a range variable
	 * declaration in the current query or any parent query
	 * @return <code>true</code> if the variable name is mapping an abstract schema name; <code>false</code>
	 * if it's defined in a collection member declaration
	 */
	public boolean isRangeIdentificationVariable(String variableName) {
		boolean result = isRangeIdentificationVariableImp(variableName);
		if (!result && (getParent() != null)) {
			result = getParent().isRangeIdentificationVariableImp(variableName);
		}
		return result;
	}

	protected boolean isRangeIdentificationVariableImp(String variableName) {

		for (Declaration declaration : declarations) {

			if (declaration.rangeDeclaration &&
			    declaration.getVariableName().equalsIgnoreCase(variableName)) {

				return true;
			}
		}

		return false;
	}

	/**
	 * Determines if the given variable is a result variable.
	 *
	 * @param variable The variable to check if it's a result variable
	 * @return <code>true</code> if the given variable is defined as a result variable; <code>false</code>
	 * otherwise
	 */
	public boolean isResultVariable(String variable) {
		return resultVariables.containsValue(variable.toUpperCase());
	}

	/**
	 * Visits the current query (which is either the top-level query or a subquery) and gathers the
	 * information from the declaration clause.
	 *
	 * @param expression The {@link Expression} to visit in order to retrieve the information
	 * contained in the given query's declaration
	 */
	public void populate(Expression expression) {
		expression.accept(getDeclarationVisitor());
	}

	protected QualifyRangeDeclarationVisitor qualifyRangeDeclarationVisitor() {
		if (qualifyRangeDeclarationVisitor == null) {
			qualifyRangeDeclarationVisitor = new QualifyRangeDeclarationVisitor();
		}
		return qualifyRangeDeclarationVisitor;
	}

	/**
	 * Resolves the "root" object represented by the given {@link Expression}. This will also handle
	 * using a subquery in the <code><b>FROM</b></code> clause. This is only support for EclipseLink.
	 *
	 * @param expression
	 * @return
	 */
	protected Resolver resolveRootObject(Expression expression) {
		RootObjectExpressionVisitor visitor = getRootObjectExpressionVisitor();
		try {
			expression.accept(visitor);
			return visitor.resolver;
		}
		finally {
			visitor.resolver = null;
		}
	}

	protected String visitDeclaration(Expression expression, Expression identificationVariable) {

		// Visit the identification variable expression and retrieve the identification variable name
		String variableName = queryContext.literal(
			identificationVariable,
			LiteralType.IDENTIFICATION_VARIABLE
		);

		// If it's not empty, then we can create a Resolver
		if (ExpressionTools.stringIsNotEmpty(variableName)) {

			// Always make the identification variable be upper case since it's
			// case insensitive, the get will also use upper case
			String internalVariableName = variableName.toUpperCase();

			// Make sure the identification variable was not declared more than once,
			// this could cause issues when trying to resolve it
			if (!resolvers.containsKey(internalVariableName)) {

				// Resolve the expression and map it with the identification variable
				Resolver resolver = resolveRootObject(expression);
				resolver = new IdentificationVariableResolver(resolver, variableName);
				resolvers.put(internalVariableName, resolver);
			}

			return variableName;
		}

		return ExpressionTools.EMPTY_STRING;
	}

	/**
	 * A <code>Declaration</code> represents either an identification variable declaration or a
	 * collection member declaration. For a subquery, the declaration can be a derived path expression.
	 */
	public static class Declaration implements JPQLQueryDeclaration {

		/**
		 * Either the range variable declaration if this is a range declaration otherwise the
		 * collection-valued path expression when this is a collection member declaration.
		 */
		protected Expression baseExpression;

		/**
		 * The declaration expression, which is either an {@link IdentificationVariableDeclaration} or
		 * a {@link CollectionMemberDeclaration} when part of a <b>FROM</b> clause, otherwise it's
		 * either the {@link DeleteClause} or the {@link UpdateClause}.
		 */
		protected Expression declarationExpression;

		/**
		 * Determines whether the "root" object is a derived path expression where the identification
		 * variable is declared in the super query, otherwise it's an entity name.
		 */
		protected boolean derived;

		/**
		 * The identification variable used to declare an abstract schema name or a collection-valued
		 * path expression.
		 */
		protected IdentificationVariable identificationVariable;

		/**
		 * The identification variables that are defined in the <b>JOIN</b> expressions.
		 */
		protected Set<String> joinIdentificationVariables;

		/**
		 * The list of <b>JOIN</b> expressions that are declared in the same declaration than the
		 * range variable declaration.
		 */
		protected Map<Join, IdentificationVariable> joins;

		/**
		 * Flag used to determine if this declaration is for a range variable declaration
		 * (<code>true</code>) or for a collection member declaration (<code>false</code>).
		 */
		protected boolean rangeDeclaration;

		/**
		 * The "root" object for objects which may not be reachable by navigation, it is either the
		 * abstract schema name (entity name), a derived path expression (which is only defined in a
		 * subquery) or <code>null</code> if this {@link Declaration} is a collection member declaration.
		 */
		protected String rootPath;

		/**
		 * Adds the given {@link Join} with its identification variable, which can be <code>null</code>.
		 *
		 * @param join
		 * @param identificationVariable
		 */
		protected void addJoin(Join join, IdentificationVariable identificationVariable) {
			if (joins == null) {
				joins = new LinkedHashMap<Join, IdentificationVariable>();
			}
			joins.put(join, identificationVariable);
		}

		protected Set<String> buildJoinIdentificationVariables() {

			Set<String> variables = new HashSet<String>();
			Set<String> upperCaseVariables = new HashSet<String>();

			// Add the non-empty join identification variables
			for (IdentificationVariable identificationVariable : joins.values()) {
				if (identificationVariable != null) {
					String joinVariable = identificationVariable.getText();
					// Make sure the same variable name but with different case is not added more than once
					if ((joinVariable.length() > 0) &&
					    !upperCaseVariables.contains(joinVariable.toUpperCase())) {

						variables.add(joinVariable);
					}
				}
			}

			return Collections.unmodifiableSet(variables);
		}

		protected Map.Entry<Join, String> buildMapEntry(Map.Entry<Join, IdentificationVariable> entry) {
			IdentificationVariable variable = entry.getValue();
			String variableName = (variable != null) ? variable.getText() : ExpressionTools.EMPTY_STRING;
			return new SimpleEntry(entry.getKey(), variableName);
		}

		/**
		 * {@inheritDoc}
		 */
		public Expression getBaseExpression() {
			return baseExpression;
		}

		/**
		 * {@inheritDoc}
		 */
		public Expression getDeclarationExpression() {
			return declarationExpression;
		}

		/**
		 * Returns the <b>JOIN</b> expressions mapped to their identification variables. The set
		 * returns the <b>JOIN</b> expressions in ordered they were parsed.
		 *
		 * @return The <b>JOIN</b> expressions mapped to their identification variables
		 */
		public List<Map.Entry<Join, String>> getJoinEntries() {
			List<Map.Entry<Join, String>> entries = new ArrayList<Map.Entry<Join, String>>();
			for (Map.Entry<Join, IdentificationVariable> entry : joins.entrySet()) {
				entries.add(buildMapEntry(entry));
			}
			return entries;
		}

		/**
		 * Returns the identification variables that are defined in the <b>JOIN</b> expressions.
		 *
		 * @return The identification variables that are defined in the <b>JOIN</b> expressions
		 */
		public Set<String> getJoinIdentificationVariables() {

			if (joinIdentificationVariables == null) {
				if (hasJoins()) {
					joinIdentificationVariables = buildJoinIdentificationVariables();
				}
				else {
					joinIdentificationVariables = Collections.emptySet();
				}
			}

			return joinIdentificationVariables;
		}

		/**
		 * {@inheritDoc}
		 */
		public List<Join> getJoins() {
			return new LinkedList<Join>(joins.keySet());
		}

		/**
		 * Returns the "root" object for objects which may not be reachable by navigation, it is
		 * either the abstract schema name (entity name), a derived path expression (which is only
		 * defined in a subquery) or <code>null</code> if this {@link Declaration} is a collection
		 * member declaration.
		 *
		 * @return The "root" object for objects which may not be reachable by navigation or
		 * <code>null</code> if this {@link Declaration} is a collection member declaration
		 */
		public String getRootPath() {
			return rootPath;
		}

		/**
		 * If {@link #isDerived()} is <code>true</code>, then returns the identification variable used
		 * in the derived path expression that is defined in the superquery, otherwise returns an
		 * empty string.
		 *
		 * @return The identification variable from the superquery if the root path is a derived path
		 * expression
		 */
		public String getSuperqueryVariableName() {
			int index = derived ? rootPath.indexOf('.') : -1;
			return (index > -1) ? rootPath.substring(0, index) : ExpressionTools.EMPTY_STRING;
		}

		/**
		 * Returns the identification variable name that is defining either the abstract schema name
		 * or the collection-valued path expression
		 *
		 * @return The name of the identification variable
		 */
		public String getVariableName() {
			if (identificationVariable == null) {
				return ExpressionTools.EMPTY_STRING;
			}
			return identificationVariable.getText();
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean hasJoins() {
			return !joins.isEmpty();
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isCollection() {
			return !rangeDeclaration;
		}

		/**
		 * Determines whether the "root" object is a derived path expression where the identification
		 * variable is declared in the superquery, otherwise it's an entity name.
		 *
		 * @return <code>true</code> if the root path is a derived path expression; <code>false</code>
		 * otherwise
		 */
		public boolean isDerived() {
			return derived;
		}

		/**
		 * Determines whether this {@link Declaration} represents a range identification variable
		 * declaration, example: "Employee e".
		 *
		 * @return <code>true</code> if the declaration is over an abstract schema name; <code>false</code>
		 * if it's over a collection-valued path expression
		 * @see #isDerived()
		 */
		public boolean isRange() {
			return rangeDeclaration;
		}

		/**
		 * Make sure the list of <b>JOIN</b> expressions and the map of <b>JOIN FETCHS</b> expressions
		 * can not be modified.
		 */
		protected void lockData() {

			if (joins != null) {
				joins = Collections.unmodifiableMap(joins);
			}
			else {
				joins = Collections.emptyMap();
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {

			if (declarationExpression != null) {
				return declarationExpression.toParsedText();
			}

			StringBuilder sb = new StringBuilder();
			sb.append(rootPath);
			sb.append(AbstractExpression.SPACE);
			sb.append(identificationVariable);
			return sb.toString();
		}
	}

	protected class DeclarationVisitor extends AbstractExpressionVisitor {

		/**
		 * This flag is used to determine what to do in {@link #visit(SimpleSelectStatement)}.
		 */
		protected boolean buildingDeclaration;

		/**
		 * Flag used to determine if the {@link IdentificationVariable} to visit is a result variable.
		 */
		protected boolean collectResultVariable;

		/**
		 * The {@link Declaration} being populated.
		 */
		protected Declaration currentDeclaration;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(AbstractSchemaName expression) {
			currentDeclaration.rangeDeclaration = true;
			currentDeclaration.rootPath = expression.getText();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(CollectionExpression expression) {
			expression.acceptChildren(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(CollectionMemberDeclaration expression) {

			Declaration declaration = new Declaration();

			Expression identificationVariable = expression.getIdentificationVariable();
			String variableName = visitDeclaration(expression, identificationVariable);

			if (variableName.length() > 0) {
				declaration.identificationVariable = (IdentificationVariable) identificationVariable;
			}

			declaration.declarationExpression = expression;
			declaration.baseExpression        = expression.getCollectionValuedPathExpression();
			declaration.lockData();
			declarations.add(declaration);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(CollectionValuedPathExpression expression) {
			currentDeclaration.derived = true;
			currentDeclaration.rootPath = expression.toActualText();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(DeleteClause expression) {

			Declaration declaration = new Declaration();
			declaration.declarationExpression = expression;
			declaration.rangeDeclaration = true;
			declarations.add(declaration);

			currentDeclaration = declaration;

			try {
				expression.getRangeVariableDeclaration().accept(this);
			}
			finally {
				currentDeclaration.lockData();
				currentDeclaration = null;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(DeleteStatement expression) {
			expression.getDeleteClause().accept(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(FromClause expression) {
			expression.getDeclaration().accept(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(IdentificationVariable expression) {
			if (collectResultVariable) {
				resultVariables.put(expression, expression.getText().toUpperCase());
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(IdentificationVariableDeclaration expression) {

			Declaration declaration = new Declaration();
			declaration.declarationExpression = expression;
			declaration.baseExpression = expression.getRangeVariableDeclaration();
			declarations.add(declaration);

			currentDeclaration = declaration;

			try {
				declaration.baseExpression.accept(this);
				expression.getJoins().accept(this);
			}
			finally {
				currentDeclaration.lockData();
				currentDeclaration = null;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(Join expression) {

			Expression identificationVariable = expression.getIdentificationVariable();
			String variableName = visitDeclaration(expression, identificationVariable);

			if (variableName.length() > 0) {
				currentDeclaration.addJoin(expression, (IdentificationVariable) identificationVariable);
			}
			else {
				currentDeclaration.addJoin(expression, null);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(JPQLExpression expression) {
			expression.getQueryStatement().accept(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(RangeVariableDeclaration expression) {

			Expression rootObject = expression.getRootObject();

			// Traverse the "root" object
			buildingDeclaration = true;
			rootObject.accept(this);
			buildingDeclaration = false;

			// Identification variable
			Expression identificationVariable = expression.getIdentificationVariable();
			String variableName = visitDeclaration(rootObject, identificationVariable);

			if (variableName.length() > 0) {
				currentDeclaration.identificationVariable = (IdentificationVariable) identificationVariable;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(ResultVariable expression) {
			collectResultVariable = true;
			expression.getResultVariable().accept(this);
			collectResultVariable = false;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(SelectClause expression) {
			expression.getSelectExpression().accept(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(SelectStatement expression) {
			expression.getFromClause().accept(this);
			expression.getSelectClause().accept(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(SimpleFromClause expression) {
			expression.getDeclaration().accept(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(SimpleSelectClause expression) {
			expression.getSelectExpression().accept(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(SimpleSelectStatement expression) {
			expression.getFromClause().accept(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(UpdateClause expression) {

			Declaration declaration = new Declaration();
			declaration.declarationExpression = expression;
			declaration.rangeDeclaration = true;
			declarations.add(declaration);

			currentDeclaration = declaration;

			try {
				expression.getRangeVariableDeclaration().accept(this);
			}
			finally {
				currentDeclaration.lockData();
				currentDeclaration = null;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(UpdateStatement expression) {
			expression.getUpdateClause().accept(this);
		}
	}

	protected class QualifyRangeDeclarationVisitor extends AbstractExpressionVisitor {

		/**
		 * The {@link Declaration} being modified.
		 */
		protected Declaration declaration;

		/**
		 * The identification variable coming from the parent identification variable declaration.
		 */
		protected String outerVariableName;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(CollectionValuedPathExpression expression) {
			declaration.baseExpression = expression;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(IdentificationVariableDeclaration expression) {
			expression.getRangeVariableDeclaration().accept(this);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(RangeVariableDeclaration expression) {

			declaration.rangeDeclaration = false;

			expression.setVirtualIdentificationVariable(outerVariableName, declaration.rootPath);
			expression.getRootObject().accept(this);
		}
	}

	/**
	 * This visitor takes care to support a subquery defined as a "root" object.
	 */
	protected class RootObjectExpressionVisitor extends AnonymousExpressionVisitor {

		/**
		 * The {@link Resolver} of the "root" object.
		 */
		protected Resolver resolver;

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected void visit(Expression expression) {
			resolver = queryContext.getResolver(expression);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(SimpleSelectStatement expression) {
			resolver = new FromSubqueryResolver(
				DeclarationResolver.this,
				DeclarationResolver.this.queryContext,
				expression
			);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void visit(SubExpression expression) {
			expression.getExpression().accept(this);
		}
	}

	/**
	 * A simple implementation of {@link Map.Entry}.
	 */
	private static class SimpleEntry implements Map.Entry<Join, String> {

		/**
		 * The key of this entry.
		 */
		private final Join key;

		/**
		 * The value of this entry.
		 */
		private String value;

		/**
		 * Creates a new <code>SimpleEntry</code>.
		 *
		 * @param key The key of this entry
		 * @param value The value of this entry
		 */
		SimpleEntry(Join key, String value) {
			super();
			this.key   = key;
			this.value = value;
		}

		/**
		 * {@inheritDoc}
		 */
		public Join getKey() {
			return key;
		}

		/**
		 * {@inheritDoc}
		 */
		public String getValue() {
			return value;
		}

		/**
		 * {@inheritDoc}
		 */
		public String setValue(String value) {
			throw new IllegalAccessError("This Map.Entry is read-only");
		}
	}
}