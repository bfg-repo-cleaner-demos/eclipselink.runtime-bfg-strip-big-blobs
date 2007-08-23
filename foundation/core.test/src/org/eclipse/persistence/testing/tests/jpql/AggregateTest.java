/*
 * Created by IntelliJ IDEA.
 * User: jon.driscoll
 * Date: Aug 13, 2002
 * Time: 10:34:39 AM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.eclipse.persistence.testing.tests.jpql;

import org.eclipse.persistence.testing.framework.TestSuite;
import org.eclipse.persistence.testing.framework.TestErrorException;
import org.eclipse.persistence.queries.ReportQuery;
import org.eclipse.persistence.testing.models.employee.domain.Employee;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.expressions.Expression;
import java.util.Collection;
import java.util.Iterator;
import java.math.BigDecimal;

public class AggregateTest extends JPQLTestCase {
    private ReportQuery reportQuery;
    private Collection originalResults;

    private static AggregateTest getNewTestCaseNamed(String testName, String ejbql, Class referenceClass) {
        AggregateTest test = new AggregateTest();

        test.setName(testName);
        test.setEjbqlString(ejbql);
        test.useReportQuery();
        test.setReferenceClass(referenceClass);

        return test;
    }

    private static ReportQuery getNewReportQueryForTest(AggregateTest test) {
        ReportQuery rq = new ReportQuery();

        rq.setReferenceClass(test.getReferenceClass());
        rq.returnSingleAttribute();
        rq.dontRetrievePrimaryKeys();

        test.setReportQuery(rq);

        return rq;
    }

    public static JPQLTestCase getSimpleAvgTest() {
        String ejbql = "SELECT AVG(DISTINCT emp.salary) FROM Employee emp";

        AggregateTest test = getNewTestCaseNamed("AVG Test", ejbql, Employee.class);

        ReportQuery rq = getNewReportQueryForTest(test);
        rq.addAverage("salary", Double.class);

        return test;
    }

    public static JPQLTestCase getSimpleCountTest() {
        String ejbql;
        ejbql = "SELECT COUNT(emp) FROM Employee emp";

        AggregateTest test = getNewTestCaseNamed("COUNT Test", ejbql, Employee.class);

        ReportQuery rq = getNewReportQueryForTest(test);
        rq.addCount("id", Long.class);

        return test;
    }

    public static JPQLTestCase getSimpleMaxTest() {
        String ejbql = "SELECT MAX(DISTINCT emp.salary) FROM Employee emp";
        AggregateTest test = getNewTestCaseNamed("MAX Test", ejbql, Employee.class);

        ReportQuery rq = getNewReportQueryForTest(test);
    	ExpressionBuilder builder = rq.getExpressionBuilder();
    	Expression exp = builder.get("salary").distinct();
        rq.addMaximum("salary", exp);

        return test;
    }

    public static JPQLTestCase getSimpleMinTest() {
        String ejbql = "SELECT MIN(DISTINCT emp.salary) FROM Employee emp";
        AggregateTest test = getNewTestCaseNamed("MIN Test", ejbql, Employee.class);

        ReportQuery rq = getNewReportQueryForTest(test);
    	ExpressionBuilder builder = rq.getExpressionBuilder();
    	Expression exp = builder.get("salary").distinct();
        rq.addMinimum("salary", exp);


        return test;
    }

    public static JPQLTestCase getSimpleSumTest() {
        String ejbql = "SELECT SUM(DISTINCT emp.salary) FROM Employee emp";
        AggregateTest test = getNewTestCaseNamed("SUM Test", ejbql, Employee.class);

        ReportQuery rq = getNewReportQueryForTest(test);
        rq.addSum("salary", Long.class);

        return test;
    }

    private static AggregateTest makeTestComplex(AggregateTest test) {
        test.setName("Complex " + test.getName());
        test.setEjbqlString(test.getEjbqlString() + " WHERE emp.lastName = \"Smith\"");

        ExpressionBuilder employee = test.getReportQuery().getExpressionBuilder();
        Expression whereClause = employee.get("lastName").equal("Smith");

        test.getReportQuery().setSelectionCriteria(whereClause);

        return test;
    }

    public static JPQLTestCase getComplexAvgTest() {
        return makeTestComplex((AggregateTest)getSimpleAvgTest());
    }

    public static JPQLTestCase getComplexCountTest() {
        return makeTestComplex((AggregateTest)getSimpleCountTest());
    }

    public static JPQLTestCase getComplexDistinctCountTest() {
        String ejbql;
        ejbql = "SELECT COUNT (DISTINCT emp.lastName) FROM Employee emp";

        AggregateTest test = getNewTestCaseNamed("Complex DISTINCT COUNT Test", ejbql, Employee.class);

        ReportQuery rq = getNewReportQueryForTest(test);
        rq.addCount("lastName", new ExpressionBuilder().get("lastName").distinct(), Long.class);

        return test;
    }

    public static JPQLTestCase getComplexMaxTest() {
        return makeTestComplex((AggregateTest)getSimpleMaxTest());
    }

    public static JPQLTestCase getComplexMinTest() {
        return makeTestComplex((AggregateTest)getSimpleMinTest());
    }

    public static JPQLTestCase getComplexSumTest() {
        return makeTestComplex((AggregateTest)getSimpleSumTest());
    }

    public static TestSuite getSimpleAggregateTestSuite() {
        TestSuite suite = new TestSuite();
        suite.setName("Simple Aggregate Test Suite");

        suite.addTest(getSimpleAvgTest());
        suite.addTest(getSimpleCountTest());
        suite.addTest(getSimpleMaxTest());
        suite.addTest(getSimpleMinTest());
        suite.addTest(getSimpleSumTest());

        return suite;
    }

    public static TestSuite getComplexAggregateTestSuit() {
        TestSuite suite = new TestSuite();
        suite.setName("Complex Aggregate Test Suite");

        suite.addTest(getComplexAvgTest());
        suite.addTest(getComplexCountTest());
        suite.addTest(getComplexDistinctCountTest());
        suite.addTest(getComplexMaxTest());
        suite.addTest(getComplexMinTest());
        suite.addTest(getComplexSumTest());

        return suite;
    }

    public void setup() {
        setOriginalResults((Collection)getSession().executeQuery(getReportQuery()));
    }

    public void reset() {
        super.reset();
    }

    public void verify() throws Exception {
        Iterator originalObjects = getOriginalResults().iterator();
        Iterator returnedObjects = ((Collection)getReturnedObjects()).iterator();

        while (originalObjects.hasNext()) {
            Object original = originalObjects.next();
            Object returned = returnedObjects.next();
            if (!(original.equals(returned))) {
                throw new TestErrorException(getName() + " Verify Failed");
            }
        }
    }

    // Accessors
    public ReportQuery getReportQuery() {
        return reportQuery;
    }

    public void setReportQuery(ReportQuery reportQuery) {
        this.reportQuery = reportQuery;
    }

    public Collection getOriginalResults() {
        return originalResults;
    }

    public void setOriginalResults(Collection originalResults) {
        this.originalResults = originalResults;
    }
}
