/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Gordon Yorke - Initial development
 *
 ******************************************************************************/

package org.eclipse.persistence.internal.jpa.querydef;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Predicate.BooleanOperator;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.Type.PersistenceType;

import org.eclipse.persistence.internal.expressions.ConstantExpression;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.internal.queries.ReportItem;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedGetConstructorFor;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.ObjectLevelReadQuery;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.eclipse.persistence.queries.ReportQuery;

/**
 * <p>
 * <b>Purpose</b>: Contains the implementation of the CriteriaQuery interface of
 * the JPA criteria API.
 * <p>
 * <b>Description</b>: This is the container class for the components that
 * define a query.
 * <p>
 * 
 * @see javax.persistence.criteria CriteriaQuery
 * 
 * @author gyorke
 * @since EclipseLink 1.2
 */
public class CriteriaQueryImpl<T> extends AbstractQueryImpl<T> implements CriteriaQuery<T> {

    protected SelectionImpl<?> selection;
    protected Set<ParameterExpression<?>> parameters;

    protected List<Order> orderBy;
    
    protected List<FromImpl> joins;

    public CriteriaQueryImpl(Metamodel metamodel, ResultType queryResult, Class result, QueryBuilderImpl queryBuilder) {
        super(metamodel, queryResult, queryBuilder, result);
        this.parameters = new HashSet<ParameterExpression<?>>();
    }

    /**
     * Specify the item that is to be returned in the query result. Replaces the
     * previously specified selection, if any.
     * 
     * @param selection
     *            selection specifying the item that is to be returned in the
     *            query result
     * @return the modified query
     */
    public CriteriaQuery<T> select(Selection<? extends T> selection) {
        findRootAndParameters(selection);
        this.selection = (SelectionImpl) selection;
        if (selection.isCompoundSelection()) {
            if (!selection.getJavaType().equals(Tuple.class) && !this.queryResult.equals(ResultType.TUPLE) && !this.queryResult.equals(ResultType.OBJECT_ARRAY)) {
                int size = selection.getCompoundSelectionItems().size();
                if (!discoverResultType(selection.getCompoundSelectionItems().toArray(new Selection[selection.getCompoundSelectionItems().size()]))) {
                    Object[] params = new Object[size + 1];
                    params[0] = this.queryType;
                    int count = 0;
                    for (Selection select : selection.getCompoundSelectionItems()) {
                        params[++count] = select.getJavaType();
                    }
                    throw new IllegalArgumentException(ExceptionLocalization.buildMessage("CRITERIA_NO_CONSTRUCTOR_FOUND_TODO", params));
                }
            }
        } else {
            this.queryType = selection.getJavaType();
            ManagedType type = this.metamodel.type(this.queryType);
            if (type != null && type.getPersistenceType().equals(PersistenceType.ENTITY)) {
                this.queryResult = ResultType.ENTITY;
            } else {
                this.queryResult = ResultType.OTHER;
            }
        }
        return this;
    }

    /**
     * Specify the items that are to be returned in the query result. Replaces
     * the previously specified selection(s), if any.
     * 
     * The type of the result of the query execution depends on the
     * specification of the criteria query object as well as the arguments to
     * the multiselect method as follows:
     * 
     * If the type of the criteria query is CriteriaQuery<Tuple>, a Tuple object
     * corresponding to the arguments of the multiselect method will be
     * instantiated and returned for each row that results from the query
     * execution.
     * 
     * If the type of the criteria query is CriteriaQuery<X> for some
     * user-defined class X, then the arguments to the multiselect method will
     * be passed to the X constructor and an instance of type X will be returned
     * for each row. The IllegalStateException will be thrown if a constructor
     * for the given argument types does not exist.
     * 
     * If the type of the criteria query is CriteriaQuery<X[]> for some class X,
     * an instance of type X[] will be returned for each row. The elements of
     * the array will correspond to the arguments of the multiselect method. The
     * IllegalStateException will be thrown if the arguments to the multiselect
     * method are not of type X.
     * 
     * If the type of the criteria query is CriteriaQuery<Object>, and only a
     * single argument is passed to the multiselect method, an instance of type
     * Object will be returned for each row.
     * 
     * If the type of the criteria query is CriteriaQuery<Object>, and more than
     * one argument is passed to the multiselect method, an instance of type
     * Object[] will be instantiated and returned for each row. The elements of
     * the array will correspond to the arguments to the multiselect method.
     * 
     * @param selections
     *            expressions specifying the items that are to be returned in
     *            the query result
     * @return the modified query
     */
    public CriteriaQuery<T> multiselect(Selection<?>... selections) {
        if (selections == null || selections.length == 0) {
            this.selection = null;
        }
        if (this.queryType.equals(ClassConstants.OBJECT)) {
            if (selections.length > 1) {
                this.queryType = ClassConstants.AOBJECT;
                this.selection = (SelectionImpl) this.queryBuilder.construct(ClassConstants.AOBJECT, selections);
                this.queryResult = ResultType.OBJECT_ARRAY;
            } else {
                ManagedType type = this.metamodel.type(selections[0].getJavaType());
                if (type == null || (!type.getPersistenceType().equals(PersistenceType.ENTITY))) {
                    this.queryResult = ResultType.OTHER;
                } else {
                    this.queryResult = ResultType.ENTITY;
                }
                this.queryType = selections[0].getJavaType();
                this.selection = (SelectionImpl) selections[0];
                if (((InternalSelection)this.selection).isFrom()){
                    ((FromImpl)this.selection).isLeaf = false;
                }
            }
            return this;
        }
        if (selections.length > 1 || !(this.queryType.isAssignableFrom(selections[0].getJavaType()))) {
            if (this.queryResult.equals(ResultType.OTHER) || this.queryResult.equals(ResultType.ENTITY)) {
                if (!discoverResultType(selections)) {
                    Object[] params = new Object[selections.length];
                    params[0] = this.queryType;
                    int count = 0;
                    for (Selection select : selection.getCompoundSelectionItems()) {
                        params[++count] = select.getJavaType();
                    }
                    throw new IllegalArgumentException(ExceptionLocalization.buildMessage("CRITERIA_NO_CONSTRUCTOR_FOUND_TODO", params));
                }
            }
            this.selection = (SelectionImpl) this.queryBuilder.construct(this.queryType, selections);
        } else {
            this.selection = (SelectionImpl) selections[0];
            if (((InternalSelection)this.selection).isFrom()){
                ((FromImpl)this.selection).isLeaf = false;
            }

        }
        // TODO validate primitive return types but a multiselect
        return this;
    }

    /**
     * Specify the items that are to be returned in the query result. Replaces
     * the previously specified selection(s), if any.
     * 
     * The type of the result of the query execution depends on the
     * specification of the criteria query object as well as the arguments to
     * the multiselect method as follows:
     * 
     * If the type of the criteria query is CriteriaQuery<Tuple>, a Tuple object
     * corresponding to the items in the selection list passed to the
     * multiselect method will be instantiated and returned for each row that
     * results from the query execution.
     * 
     * If the type of the criteria query is CriteriaQuery<X> for some
     * user-defined class X, then the items in the selection list passed to the
     * multiselect method will be passed to the X constructor and an instance of
     * type X will be returned for each row. The IllegalStateException will be
     * thrown if a constructor for the given argument types does not exist.
     * 
     * If the type of the criteria query is CriteriaQuery<X[]> for some class X,
     * an instance of type X[] will be returned for each row. The elements of
     * the array will correspond to the items in the selection list passed to
     * the multiselect method. The IllegalStateException will be thrown if the
     * elements in the selection list passed to the multiselect method are not
     * of type X.
     * 
     * If the type of the criteria query is CriteriaQuery<Object>, and the
     * selection list passed to the multiselect method contains only a single
     * item, an instance of type Object will be returned for each row.
     * 
     * If the type of the criteria query is CriteriaQuery<Object>, and the
     * selection list passed to the multiselect method contains more than one
     * item, an instance of type Object[] will be instantiated and returned for
     * each row. The elements of the array will correspond to the items in the
     * selection list passed to the multiselect method.
     * 
     * @param selectionList
     *            list of expressions specifying the items that to be are
     *            returned in the query result
     * @return the modified query
     */
    public CriteriaQuery<T> multiselect(List<Selection<?>> selectionList) {
        if (selectionList == null) {
            this.selection = null;
            return this;
        }
        return this.multiselect(selectionList.toArray(new Selection[selectionList.size()]));
    }

    // override the return type only:
    /**
     * Modify the query to restrict the query result according to the specified
     * boolean expression. Replaces the previously added restriction(s), if any.
     * This method only overrides the return type of the corresponding
     * AbstractQuery method.
     * 
     * @param restriction
     *            a simple or compound boolean expression
     * @return the modified query
     */
    public CriteriaQuery<T> where(Expression<Boolean> restriction) {
        return (CriteriaQuery<T>) super.where(restriction);
    }

    /**
     * Modify the query to restrict the query result according to the
     * conjunction of the specified restriction predicates. Replaces the
     * previously added restriction(s), if any. If no restrictions are
     * specified, any previously added restrictions are simply removed. This
     * method only overrides the return type of the corresponding AbstractQuery
     * method.
     * 
     * @param restrictions
     *            zero or more restriction predicates
     * @return the modified query
     */
    public CriteriaQuery<T> where(Predicate... restrictions) {
        return (CriteriaQuery<T>) super.where(restrictions);
    }

    /**
     * Specify the expressions that are used to form groups over the query
     * results. Replaces the previous specified grouping expressions, if any. If
     * no grouping expressions are specified, any previously added grouping
     * expressions are simply removed. This method only overrides the return
     * type of the corresponding AbstractQuery method.
     * 
     * @param grouping
     *            zero or more grouping expressions
     * @return the modified query
     */
    public CriteriaQuery<T> groupBy(Expression<?>... grouping) {
        super.groupBy(grouping);
        return this;
    }

    /**
     * Specify the expressions that are used to form groups over the query
     * results. Replaces the previous specified grouping expressions, if any. If
     * no grouping expressions are specified, any previously added grouping
     * expressions are simply removed. This method only overrides the return
     * type of the corresponding AbstractQuery method.
     * 
     * @param grouping
     *            list of zero or more grouping expressions
     * @return the modified query
     */
    public CriteriaQuery<T> groupBy(List<Expression<?>> grouping) {
        super.groupBy(grouping);
        return this;
    }

    /**
     * Specify a restriction over the groups of the query. Replaces the previous
     * having restriction(s), if any. This method only overrides the return type
     * of the corresponding AbstractQuery method.
     * 
     * @param restriction
     *            a simple or compound boolean expression
     * @return the modified query
     */
    public CriteriaQuery<T> having(Expression<Boolean> restriction) {
        super.having(restriction);
        return this;
    }

    /**
     * Specify restrictions over the groups of the query according the
     * conjunction of the specified restriction predicates. Replaces the
     * previously added restriction(s), if any. If no restrictions are
     * specified, any previously added restrictions are simply removed. This
     * method only overrides the return type of the corresponding AbstractQuery
     * method.
     * 
     * @param restrictions
     *            zero or more restriction predicates
     * @return the modified query
     */
    public CriteriaQuery<T> having(Predicate... restrictions) {
        super.having(restrictions);
        return this;
    }

    protected void integrateRoot(RootImpl root){
        if (this.roots.isEmpty() && (this.queryResult.equals(ResultType.ENTITY) || this.queryType.equals(ClassConstants.Object_Class))) {
            // this is the first root, set return type and selection and query
            // type
            if (this.selection == null){
                this.selection = root;
                this.queryResult = ResultType.ENTITY;
            }
            this.queryType = root.getJavaType();
        }
        this.roots.add(root);
        
    }

    /**
     * Specify the ordering expressions that are used to order the query
     * results. Replaces the previous ordering expressions, if any. If no
     * ordering expressions are specified, the previous ordering, if any, is
     * simply removed, and results will be returned in no particular order. The
     * left-to-right sequence of the ordering expressions determines the
     * precedence, whereby the leftmost has highest precedence.
     * 
     * @param o
     *            zero or more ordering expressions
     * @return the modified query.
     */
    public CriteriaQuery<T> orderBy(Order... o) {
        this.orderBy = new ArrayList();
        for (Order order : o) {
            this.orderBy.add(order);
        }
        return this;
    }

    /**
     * Specify the ordering expressions that are used to order the query
     * results. Replaces the previous ordering expressions, if any. If no
     * ordering expressions are specified, the previous ordering, if any, is
     * simply removed, and results will be returned in no particular order. The
     * order of the ordering expressions in the list determines the precedence,
     * whereby the first element in the list has highest precedence.
     * 
     * @param o
     *            list of zero or more ordering expressions
     * @return the modified query.
     */
    public CriteriaQuery<T> orderBy(List<Order> o) {
        this.orderBy = o;
        return this;
    }

    public void addParameter(ParameterExpression<?> parameter) {
        this.parameters.add(parameter);
    }

    public boolean discoverResultType(Selection<?>... selections) {
        Class[] constructorArgs = new Class[selections.length];
        int count = 0;
        for (Selection select : selections) {
            constructorArgs[count++] = select.getJavaType();
        }
        try {
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()) {
                AccessController.doPrivileged(new PrivilegedGetConstructorFor(this.queryType, constructorArgs, false));
                this.queryResult = ResultType.CONSTRUCTOR;
                return true;
            } else {
                PrivilegedAccessHelper.getConstructorFor(this.queryType, constructorArgs, false);
                this.queryResult = ResultType.CONSTRUCTOR;
                return true;
            }
        } catch (PrivilegedActionException ex) {
        } catch (NoSuchMethodException e) {
        }
        // did not find constructor, try for partial query
        if (this.queryResult.equals(ResultType.ENTITY)) {
            this.queryResult = ResultType.PARTIAL;
            return true;
        }
        return false;
    }

    /**
     * Specify whether duplicate query results will be eliminated. A true value
     * will cause duplicates to be eliminated. A false value will cause
     * duplicates to be retained. If distinct has not been specified, duplicate
     * results must be retained. This method only overrides the return type of
     * the corresponding AbstractQuery method.
     * 
     * @param distinct
     *            boolean value specifying whether duplicate results must be
     *            eliminated from the query result or whether they must be
     *            retained
     * @return the modified query.
     */
    public CriteriaQuery<T> distinct(boolean distinct) {
        super.distinct(distinct);
        return this;
    }

    public void addJoin(FromImpl from){
        if (this.joins == null){
            this.joins = new ArrayList<FromImpl>();
        }
        this.joins.add(from);
    }
    
    /**
     * Return the ordering expressions in order of precedence.
     * 
     * @return the list of ordering expressions
     */
    public List<Order> getOrderList() {
        throw new UnsupportedOperationException();
    }

    /**
     * Return the parameters of the query
     * 
     * @return the query parameters
     */
    public Set<ParameterExpression<?>> getParameters() {
        return this.parameters;
    }

    /**
     * Return the selection item of the query. This will correspond to the query
     * type.
     * 
     * @return the selection item of the query
     */
    public Selection<T> getSelection() {
        return (Selection<T>) this.selection;
    }

    /**
     * Translates from the criteria query to a EclipseLink Database Query.
     */
    public DatabaseQuery translate() {
        for (Iterator iterator = this.getRoots().iterator(); iterator.hasNext();){
            findJoins((FromImpl)iterator.next());
        }
        
        ObjectLevelReadQuery query = null;
        if (this.queryResult.equals(ResultType.ENTITY)) {
            query = new ReadAllQuery(this.queryType);
            if (this.roots != null && !this.roots.isEmpty()){
                List<org.eclipse.persistence.expressions.Expression> list = ((FromImpl)this.roots.iterator().next()).findJoinFetches();
                for (org.eclipse.persistence.expressions.Expression fetch: list){
                    query.addJoinedAttribute(fetch);
                }
            }
        } else if (this.queryResult.equals(ResultType.PARTIAL)) {
            ReadAllQuery raq = new ReadAllQuery(this.queryType);
            for (Selection selection : this.selection.getCompoundSelectionItems()) {
                raq.addPartialAttribute(((SelectionImpl) selection).currentNode);
            }
            query = raq;
        } else {
            if (this.roots == null || this.roots.isEmpty())
                throw new IllegalStateException(ExceptionLocalization.buildMessage("CRITERIA_NO_ROOT_FOR_COMPOUND_QUERY"));
            ReportQuery reportQuery = null;
            if (this.queryResult.equals(ResultType.CONSTRUCTOR)) {
                reportQuery = new ReportQuery();
                reportQuery.addConstructorReportItem(((ConstructorSelectionImpl) this.selection).translate());
                reportQuery.setShouldReturnSingleAttribute(true);
            } else {
                if (this.queryResult.equals(ResultType.TUPLE)) {
                    reportQuery = new TupleQuery(this.selection == null ? new ArrayList() : this.selection.getCompoundSelectionItems());
                } else {
                    reportQuery = new ReportQuery();
                    reportQuery.setShouldReturnWithoutReportQueryResult(true);
                }
                if (this.selection != null) {
                    if (this.selection.isCompoundSelection()) {
                        for (Selection nested : this.selection.getCompoundSelectionItems()) {
                            if (((SelectionImpl) nested).isCompoundSelection()) {
                                reportQuery.addConstructorReportItem(((ConstructorSelectionImpl) nested).translate());
                            } else {
                                if (((InternalSelection)nested).isFrom()){
                                    reportQuery.addItem(nested.getAlias(), ((SelectionImpl) nested).getCurrentNode(), ((FromImpl)nested).findJoinFetches());
                                }else{
                                    reportQuery.addAttribute(nested.getAlias(), ((SelectionImpl) nested).getCurrentNode(), nested.getJavaType());
                                }
                            }
                        }
                    } else {
                        if (((InternalSelection)selection).isFrom()){
                            reportQuery.addItem(selection.getAlias(), ((SelectionImpl) selection).getCurrentNode(), ((FromImpl)selection).findJoinFetches());
                        }else{
                            reportQuery.addAttribute(selection.getAlias(), ((SelectionImpl) selection).getCurrentNode(), selection.getJavaType());
                        }
                               }
                }
            }
            if (this.where != null && ((InternalSelection) this.where).getCurrentNode() != null) {
                reportQuery.setReferenceClass(((InternalSelection) this.where).getCurrentNode().getBuilder().getQueryClass());
                reportQuery.setExpressionBuilder(((InternalSelection) this.where).getCurrentNode().getBuilder());
            } else {
                Root root = this.getRoots().iterator().next();
                reportQuery.setReferenceClass(root.getJavaType());
                reportQuery.setExpressionBuilder(((RootImpl) root).getCurrentNode().getBuilder());
            }
            query = reportQuery;
            if (this.groupBy != null && !this.groupBy.isEmpty()){
                for (Expression<?> exp : this.groupBy){
                    reportQuery.addGrouping(((InternalSelection)exp).getCurrentNode());
                }
            }
            if (this.havingClause != null){
                reportQuery.setHavingExpression(((InternalSelection)this.havingClause).getCurrentNode());
            }
        }
        for (ParameterExpression<?> parameter : getParameters()) {
            query.addArgument(parameter.getName(), parameter.getJavaType());
        }

        if (this.where != null) {
            if (((InternalExpression)this.where).isPredicate() && ((InternalSelection)this.where).getCurrentNode() == null){
                if (((PredicateImpl)this.where).getOperator() == BooleanOperator.OR){
                    query.setSelectionCriteria(new ConstantExpression(1, query.getExpressionBuilder()).equal(0));
                }
            }else{
                query.setSelectionCriteria(((InternalSelection) this.where).getCurrentNode());
            }
        }
        if (this.joins != null){
            for (FromImpl join : this.joins){
                query.addNonFetchJoinedAttribute(((InternalSelection)join).getCurrentNode());
            }
        }
        if (this.distinct) {
            query.setDistinctState(ObjectLevelReadQuery.USE_DISTINCT);
        } else {
            query.setDistinctState(ObjectLevelReadQuery.DONT_USE_DISTINCT);

        }
        if (this.orderBy != null && !this.orderBy.isEmpty()) {
            for (Order order : this.orderBy) {
                OrderImpl orderImpl = (OrderImpl) order;
                org.eclipse.persistence.expressions.Expression orderExp = ((ExpressionImpl) orderImpl.getExpression()).getCurrentNode();
                if (orderImpl.isAscending()) {
                    orderExp = orderExp.ascending();
                } else {
                    orderExp = orderExp.descending();
                }
                query.addOrdering(orderExp);
            }
        }

        return query;
    }

}
