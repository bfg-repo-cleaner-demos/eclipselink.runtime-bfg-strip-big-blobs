/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.mappingsmodel.query;

import java.util.Iterator;

import org.eclipse.persistence.tools.workbench.test.mappingsmodel.MappingsModelTestTools;
import org.eclipse.persistence.tools.workbench.test.mappingsmodel.ModelProblemsTestCase;
import org.eclipse.persistence.tools.workbench.test.models.projects.QueryProject;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.persistence.tools.workbench.mappingsmodel.ProblemConstants;
import org.eclipse.persistence.tools.workbench.mappingsmodel.descriptor.InterfaceDescriptorCreationException;
import org.eclipse.persistence.tools.workbench.mappingsmodel.descriptor.relational.MWTableDescriptor;
import org.eclipse.persistence.tools.workbench.mappingsmodel.mapping.MWMapping;
import org.eclipse.persistence.tools.workbench.mappingsmodel.meta.MWClass;
import org.eclipse.persistence.tools.workbench.mappingsmodel.project.MWProject;
import org.eclipse.persistence.tools.workbench.mappingsmodel.project.relational.MWRelationalProject;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.MWQuery;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.MWQueryManager;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.MWQueryParameter;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWAbstractRelationalReadQuery;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWAutoGeneratedQueryFormat;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWEJBQLQueryFormat;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWExpressionQueryFormat;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWRelationalQuery;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWRelationalReadAllQuery;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWRelationalReadQuery;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWReportQuery;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWStringQueryFormat;
import org.eclipse.persistence.tools.workbench.utility.TriStateBoolean;

public class MWQueryTests extends ModelProblemsTestCase
{

	public static void main(String[] args) 
	{
		junit.swingui.TestRunner.main(new String[] {"-c", MWQueryTests.class.getName()});
	}
	
	public static Test suite() 
	{
		return new TestSuite(MWQueryTests.class);
	}

	public MWQueryTests(String name)
	{
		super(name);
	}

	public void testDefaultQueryFormat()
	{
		MWProject project = new MWRelationalProject("test-project", MappingsModelTestTools.buildSPIManager(), null);
//		project.getEJBPolicy().setEjbPersistenceType(MWProjectEjbPolicy.EJB_1_1_CMP_PROPERTY);
		
		MWClass type = project.typeNamed("test-class");
		MWTableDescriptor desc;
		try {
			desc = (MWTableDescriptor) project.addDescriptorForType(type);
		} catch (InterfaceDescriptorCreationException e) {
			throw new RuntimeException(e);
		}
		
//		desc.addDescriptorEjbPolicy();

		MWQueryManager qm = desc.getQueryManager();

		MWRelationalReadQuery query = (MWRelationalReadQuery) qm.addReadObjectQuery("findByPrimaryKey");		
		assertTrue("QueryFormat did not default to AutoGeneratedQueryFormat", MWAutoGeneratedQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		query = (MWRelationalReadQuery) qm.addReadObjectQuery("findAll");
		assertTrue("QueryFormat did not default to AutoGeneratedQueryFormat", MWAutoGeneratedQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		query = (MWRelationalReadQuery) qm.addReadObjectQuery("findOneBySql");
		assertTrue("QueryFormat did not default to AutoGeneratedQueryFormat", MWAutoGeneratedQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		query = (MWRelationalReadQuery) qm.addReadObjectQuery("findManyBySql");
		assertTrue("QueryFormat did not default to AutoGeneratedQueryFormat", MWAutoGeneratedQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		query = (MWRelationalReadQuery) qm.addReadObjectQuery("findOneByEjbql");
		assertTrue("QueryFormat did not default to AutoGeneratedQueryFormat", MWAutoGeneratedQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		query = (MWRelationalReadQuery) qm.addReadObjectQuery("findManyByEjbql");
		assertTrue("QueryFormat did not default to AutoGeneratedQueryFormat", MWAutoGeneratedQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		query = (MWRelationalReadQuery) qm.addReadObjectQuery("findOneByQuery");
		assertTrue("QueryFormat did not default to AutoGeneratedQueryFormat", MWAutoGeneratedQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		query = (MWRelationalReadQuery) qm.addReadObjectQuery("findManyByQuery");
		assertTrue("QueryFormat did not default to AutoGeneratedQueryFormat", MWAutoGeneratedQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));


		query = (MWRelationalReadQuery) qm.addReadObjectQuery("findCurrentEmployees");
		assertTrue("QueryFormat did not default to EJBQLQueryFormat", MWEJBQLQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		assertTrue("EJBQLQueryFormat should have a null expression", query.getQueryFormat().getExpression() == null);

		query =(MWRelationalReadQuery)  qm.addReadObjectQuery("whatever");
		assertTrue("QueryFormat did not default to ExpressionQueryFormat", MWExpressionQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		assertTrue("ExpressionQueryFormat should have an empty query string", query.getQueryFormat().getQueryString().equals(""));
	}
	
	public void testGetSetMethods()
	{
		MWAbstractRelationalReadQuery query = this.buildTestRelationalQuery();

		query.setCacheUsage(MWRelationalReadQuery.CONFORM_RESULTS_IN_UNIT_OF_WORK);
		query.setLocking(MWQuery.LOCK_NOWAIT);
		query.setBindAllParameters(new TriStateBoolean(true));
		query.setCacheStatement(new TriStateBoolean(true));
		query.setMaintainCache(false);
		query.setRefreshIdentityMapResult(true);
		query.setCacheQueryResults(true);
		query.setDistinctState(MWQuery.USE_DISTINCT);
		query.setInMemoryQueryIndirectionPolicy(MWRelationalReadQuery.TRIGGER_INDIRECTION);
        query.setQueryFormatType(MWRelationalQuery.SQL_FORMAT);
		((MWStringQueryFormat) query.getQueryFormat()).setQueryString("queryString");

		assertTrue("Cache usage was not updated", query.getCacheUsage().getMWModelOption() == MWRelationalReadQuery.CONFORM_RESULTS_IN_UNIT_OF_WORK);
		assertTrue("Lock mode was not updated", query.getLocking().getMWModelOption() == MWQuery.LOCK_NOWAIT);
		assertTrue("BindAllParameters not updated", query.isBindAllParameters().booleanValue());
		assertTrue("CacheStatement was not updated", query.isCacheStatement().booleanValue());
		assertTrue("MaintainCache was not updated", query.isMaintainCache() == false);
		assertTrue("RefreshIdentityMapResult was not updated", query.isRefreshIdentityMapResult());
		assertTrue("RefreshRemoteIdentityMapResult was not updated", query.isRefreshRemoteIdentityMapResult());
		assertTrue("StringQueryFormat was not updated", MWStringQueryFormat.class.isAssignableFrom(query.getQueryFormat().getClass()));
		assertTrue("CacheQueryResults was not updated", query.isCacheQueryResults());
		assertTrue("DistinctState was not updated", query.getDistinctState().getMWModelOption() == MWQuery.USE_DISTINCT);
		assertTrue("InMemoryQueryIndirectionPolicy was not updated", query.getInMemoryQueryIndirectionPolicy().getMWModelOption() == MWRelationalReadQuery.TRIGGER_INDIRECTION);


		//testing that setting RefreshIdentityMapResult also set RefreshRemoteIdentityMapResult, but not vice versa
		query.setRefreshRemoteIdentityMapResult(false);
		assertTrue("RefreshRemoteIdentityMapResult was not set to false", !query.isRefreshRemoteIdentityMapResult());
		
		query.setRefreshRemoteIdentityMapResult(true);
		query.setRefreshIdentityMapResult(false);
		assertTrue("RefreshRemoteIdentityMapResult was not set to true as a result of setting RefreshIdentityMapResult to true", !query.isRefreshRemoteIdentityMapResult());
	}

	public void testParameterList()
	{
		MWRelationalReadQuery query = this.buildTestQuery();

		MWQueryParameter parameter1 = query.addParameter(query.getProject().typeNamed("java.lang.String"));
		parameter1.setName("FirstParameter");
		MWQueryParameter parameter2 = query.addParameter(query.getProject().typeNamed("java.lang.String"));
		parameter2.setName("SecondParameter");

		assertTrue("The parameters were not added", query.parametersSize() == 2);

		Iterator iter = query.parameters();
		int index = 0;
		while (iter.hasNext())
		{
			Object parameter = iter.next();

			if (index == 0)
				assertTrue("The order was not kept", parameter == parameter1);
			else if (index == 1)
				assertTrue("The order was not kept", parameter == parameter2);

			index++;
		}

		query.removeParameter(parameter1);
		assertTrue("Parameter1 was not removed", query.parametersSize() == 1);
	}
	
	//TODO morphing tests up next
	public void testMorphingFromReportQueryToReadObjectQuery() {
	
		//morph a reportQuery to a readObjectQuery
		//check the following settings:
			//parameters
			//selectionCriteria
			//bindAllParameters
			//cacheStatement
			//		
		
				
		//morph a readAllQuery to a readObjectQuery
		
		//morph a readObjectQuery to a readAllQuery
	}
	
	public void testMorphingFromReportQueryToReadAllQuery() {
		//check the following settings:
		//ordering attributes	
	}
	
	public void testMorphingFromReadObjectQueryToReportQuery() {
	}
	
	public void testMorphingFromReadAllQueryToReportQuery() {
	}
	
	public void testMorphingFromReadAllQueryToReadObjectQuery() {
	}
	
	public void testMorphingFromReadObjectQueryToReadAllQuery() {
	}
	
	//test morphing eis queries
	
	private MWRelationalReadQuery buildTestQuery() {
		MWProject project = new MWRelationalProject("test-project", MappingsModelTestTools.buildSPIManager(), null);
		MWClass type = project.typeNamed("test-class");
		MWTableDescriptor desc;
		try {
			desc = (MWTableDescriptor) project.addDescriptorForType(type);
		} catch (InterfaceDescriptorCreationException e) {
			throw new RuntimeException(e);
		}

		MWQueryManager qm = desc.getQueryManager();
		MWRelationalReadQuery query = (MWRelationalReadQuery) qm.addReadObjectQuery("test-query");
		return query;
	}
	
	private MWRelationalReadAllQuery buildTestRelationalQuery() {
		MWProject project = new MWRelationalProject("test-project", MappingsModelTestTools.buildSPIManager(), null);
		MWClass type = project.typeNamed("test-class");
		MWTableDescriptor desc;
		try {
			desc = (MWTableDescriptor) project.addDescriptorForType(type);
		} catch (InterfaceDescriptorCreationException e) {
			throw new RuntimeException(e);
		}

		MWQueryManager qm = desc.getQueryManager();
		MWRelationalReadQuery query = (MWRelationalReadQuery) qm.addReadAllQuery("test-query");
		return (MWRelationalReadAllQuery) query;
	}
	
    
    
    public void testReportQueryAttributeValidProblem() {
        String errorNumber = ProblemConstants.QUERYABLE_NOT_VALID_FOR_REPORT_QUERY_ATTRIBUTE;        
        
        QueryProject queryProject = new QueryProject();
        MWReportQuery reportQuery = (MWReportQuery) queryProject.getEmployeeDescriptor().getQueryManager().queryWithSignature("reportQuery()");
   
        assertFalse(hasProblem(errorNumber, reportQuery));
        
        MWMapping mapping = queryProject.getEmployeeDescriptor().mappingNamed("firstName");
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        
        mapping = mapping.asMWOneToManyMapping();        
        assertTrue(mapping.isTraversableForReportQueryAttribute());
       assertTrue(hasProblem(errorNumber, reportQuery));
       
        mapping = mapping.asMWDirectMapping();       
        assertTrue(mapping.isTraversableForReportQueryAttribute());
       assertFalse(hasProblem(errorNumber, reportQuery));
 
        
        mapping = mapping.asMWAggregateMapping();        
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertTrue(hasProblem(errorNumber, reportQuery));

        mapping = mapping.asMWDirectMapping();       
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertFalse(hasProblem(errorNumber, reportQuery));
        
        
        mapping = mapping.asMWOneToManyMapping();        
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertTrue(hasProblem(errorNumber, reportQuery));

        mapping = mapping.asMWDirectMapping();       
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertFalse(hasProblem(errorNumber, reportQuery));
        
        
        mapping = mapping.asMWOneToOneMapping();        
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertTrue(hasProblem(errorNumber, reportQuery));

        mapping = mapping.asMWDirectMapping();       
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertFalse(hasProblem(errorNumber, reportQuery));

        
        mapping = mapping.asMWVariableOneToOneMapping();        
        assertFalse(mapping.isTraversableForReportQueryAttribute());
        assertTrue(hasProblem(errorNumber, reportQuery));

        mapping = mapping.asMWTypeConversionMapping();       
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertFalse(hasProblem(errorNumber, reportQuery));

        
        mapping = (MWMapping) mapping.asMWDirectCollectionMapping();        
        assertFalse(mapping.isTraversableForReportQueryAttribute());
        assertTrue(hasProblem(errorNumber, reportQuery));

        mapping = mapping.asMWSerializedMapping();       
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertFalse(hasProblem(errorNumber, reportQuery));

        
        mapping = (MWMapping) mapping.asMWDirectMapMapping();        
        assertFalse(mapping.isTraversableForReportQueryAttribute());
        assertTrue(hasProblem(errorNumber, reportQuery));

        mapping = mapping.asMWObjectTypeMapping();       
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertFalse(hasProblem(errorNumber, reportQuery));
        
        
        mapping = mapping.asMWTransformationMapping();        
        assertFalse(mapping.isTraversableForReportQueryAttribute());
        assertTrue(hasProblem(errorNumber, reportQuery));

        mapping = mapping.asMWDirectMapping();       
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertFalse(hasProblem(errorNumber, reportQuery));
        
        
        mapping = mapping.asMWDirectToXmlTypeMapping();        
        assertFalse(mapping.isTraversableForReportQueryAttribute());
        assertTrue(hasProblem(errorNumber, reportQuery));

        mapping = mapping.asMWDirectMapping();       
        assertTrue(mapping.isTraversableForReportQueryAttribute());
        assertFalse(hasProblem(errorNumber, reportQuery));   

        queryProject.getEmployeeDescriptor().removeMapping(mapping);
        assertTrue(hasProblem(ProblemConstants.QUERYABLE_NULL_FOR_REPORT_ITEM, reportQuery));
    }
    
    public void testReportQueryGroupingAttributeValidProblem() {
        String errorNumber = ProblemConstants.QUERYABLE_NULL_FOR_GROUPING_ITEM;        
        
        QueryProject queryProject = new QueryProject();
        MWReportQuery reportQuery = (MWReportQuery) queryProject.getEmployeeDescriptor().getQueryManager().queryWithSignature("reportQuery2()");
   
        assertFalse(hasProblem(errorNumber, reportQuery));
        
        MWMapping mapping = queryProject.getEmployeeDescriptor().mappingNamed("lastName");
        queryProject.getEmployeeDescriptor().removeMapping(mapping);
        assertTrue(hasProblem(errorNumber, reportQuery));
    
    }

    
    public void testReadAllQueryOrderingAttributeValidProblem() {
        String errorNumber = ProblemConstants.QUERYABLE_NOT_VALID_FOR_READ_ALL_QUERY_ORDERING_ITEM;        
        
        QueryProject queryProject = new QueryProject();
        MWRelationalReadAllQuery query = (MWRelationalReadAllQuery) queryProject.getEmployeeDescriptor().getQueryManager().queryWithSignature("myQuery4(java.lang.Integer)");
   
        assertFalse(hasProblem(errorNumber, query));
        
        MWMapping mapping = queryProject.getEmployeeDescriptor().mappingNamed("lastName");
        assertTrue(mapping.isTraversableForReadAllQueryOrderable());
        
        mapping = mapping.asMWOneToManyMapping();        
        assertTrue(hasProblem(errorNumber, query));
        assertFalse(mapping.isTraversableForReadAllQueryOrderable());
      
        mapping = mapping.asMWDirectMapping();       
        assertFalse(hasProblem(errorNumber, query));
 
        
        mapping = mapping.asMWAggregateMapping();        
        assertTrue(mapping.isTraversableForReadAllQueryOrderable());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWDirectMapping();       
        assertFalse(hasProblem(errorNumber, query)); 
        
        
        mapping = mapping.asMWOneToOneMapping();        
        assertTrue(mapping.isTraversableForReadAllQueryOrderable());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWDirectMapping();       
        assertFalse(hasProblem(errorNumber, query));

        
        mapping = mapping.asMWVariableOneToOneMapping();        
        assertFalse(mapping.isTraversableForReadAllQueryOrderable());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWTypeConversionMapping();       
        assertTrue(mapping.isTraversableForReadAllQueryOrderable());
        assertFalse(hasProblem(errorNumber, query));

        
        mapping = (MWMapping) mapping.asMWDirectCollectionMapping();        
        assertFalse(mapping.isTraversableForReadAllQueryOrderable());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWSerializedMapping();       
        assertTrue(mapping.isTraversableForReadAllQueryOrderable());
        assertFalse(hasProblem(errorNumber, query));

        
        mapping = (MWMapping) mapping.asMWDirectMapMapping();        
        assertFalse(mapping.isTraversableForReadAllQueryOrderable());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWObjectTypeMapping();       
        assertTrue(mapping.isTraversableForReadAllQueryOrderable());
        assertFalse(hasProblem(errorNumber, query));
        
        
        mapping = mapping.asMWTransformationMapping();        
        assertFalse(mapping.isTraversableForReadAllQueryOrderable());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWDirectMapping();       
        assertFalse(hasProblem(errorNumber, query));

    
        
        mapping = mapping.asMWDirectToXmlTypeMapping();        
        assertFalse(mapping.isTraversableForReadAllQueryOrderable());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWDirectMapping();       
        assertFalse(hasProblem(errorNumber, query));
       
        queryProject.getEmployeeDescriptor().removeMapping(mapping);
        assertTrue(hasProblem(ProblemConstants.QUERYABLE_NULL_FOR_ORDERING_ITEM, query));
    }

    public void testReadAllQueryBatchReadAttributeValidProblem() {
        String errorNumber = ProblemConstants.QUERYABLE_NOT_VALID_FOR_READ_ALL_QUERY_BATCH_READ_ITEM;        
        
        QueryProject queryProject = new QueryProject();
        MWRelationalReadAllQuery query = (MWRelationalReadAllQuery) queryProject.getEmployeeDescriptor().getQueryManager().queryWithSignature("myQuery4(java.lang.Integer)");
   
        assertFalse(hasProblem(errorNumber, query));
        
        MWMapping mapping = queryProject.getEmployeeDescriptor().mappingNamed("phoneNumbers");
        assertTrue(mapping.isTraversableForBatchReadAttribute());
        
        mapping = mapping.asMWDirectMapping();        
        assertTrue(hasProblem(errorNumber, query));
        assertFalse(mapping.isTraversableForBatchReadAttribute());
      
        mapping = mapping.asMWOneToManyMapping();       
        assertTrue(mapping.isTraversableForBatchReadAttribute());
        assertFalse(hasProblem(errorNumber, query));
 
        
        mapping = mapping.asMWAggregateMapping();        
        assertTrue(mapping.isTraversableForBatchReadAttribute());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWOneToManyMapping();       
        assertTrue(mapping.isTraversableForBatchReadAttribute());
        assertFalse(hasProblem(errorNumber, query)); 
        
        
        mapping = mapping.asMWOneToOneMapping();        
        assertTrue(mapping.isTraversableForBatchReadAttribute());
        assertFalse(hasProblem(errorNumber, query));

        mapping = mapping.asMWOneToManyMapping();       
        assertFalse(hasProblem(errorNumber, query));

        
        mapping = mapping.asMWVariableOneToOneMapping();        
        assertFalse(mapping.isTraversableForBatchReadAttribute());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWTypeConversionMapping();       
        assertFalse(mapping.isTraversableForBatchReadAttribute());
        assertTrue(hasProblem(errorNumber, query));

        
        mapping = (MWMapping) mapping.asMWDirectCollectionMapping();        
        assertTrue(mapping.isTraversableForBatchReadAttribute());
        assertFalse(hasProblem(errorNumber, query));

        mapping = mapping.asMWSerializedMapping();       
        assertFalse(mapping.isTraversableForBatchReadAttribute());
        assertTrue(hasProblem(errorNumber, query));

        
        mapping = (MWMapping) mapping.asMWDirectMapMapping();        
        assertTrue(mapping.isTraversableForBatchReadAttribute());
        assertFalse(hasProblem(errorNumber, query));

        mapping = mapping.asMWObjectTypeMapping();       
        assertFalse(mapping.isTraversableForBatchReadAttribute());
        assertTrue(hasProblem(errorNumber, query));
        
        
        mapping = mapping.asMWTransformationMapping();        
        assertFalse(mapping.isTraversableForBatchReadAttribute());
        assertTrue(hasProblem(errorNumber, query));
  
        
        mapping = mapping.asMWDirectToXmlTypeMapping();        
        assertFalse(mapping.isTraversableForBatchReadAttribute());
        assertTrue(hasProblem(errorNumber, query));
       
        queryProject.getEmployeeDescriptor().removeMapping(mapping);
        assertTrue(hasProblem(ProblemConstants.QUERYABLE_NULL_FOR_BATCH_READ_ITEM, query));
   }

    public void testReadAllQueryJoinedAttributeValidProblem() {
        String errorNumber = ProblemConstants.QUERYABLE_NOT_VALID_FOR_READ_QUERY_JOINED_READ_ITEM;        
        
        QueryProject queryProject = new QueryProject();
        MWRelationalReadAllQuery query = (MWRelationalReadAllQuery) queryProject.getEmployeeDescriptor().getQueryManager().queryWithSignature("myQuery4(java.lang.Integer)");
   
        assertFalse(hasProblem(errorNumber, query));
        
        MWMapping mapping = queryProject.getEmployeeDescriptor().mappingNamed("manager");
        assertTrue(mapping.isTraversableForJoinedAttribute());
        
        mapping = mapping.asMWDirectMapping();        
        assertFalse(mapping.isTraversableForJoinedAttribute());
        assertTrue(hasProblem(errorNumber, query));
      
        mapping = mapping.asMWOneToManyMapping();       
        assertTrue(mapping.isTraversableForJoinedAttribute());
        assertFalse(hasProblem(errorNumber, query));
 
        
        mapping = mapping.asMWAggregateMapping();        
        assertTrue(mapping.isTraversableForJoinedAttribute());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWOneToManyMapping();       
        assertTrue(mapping.isTraversableForJoinedAttribute());
        assertFalse(hasProblem(errorNumber, query)); 
        
        
        mapping = mapping.asMWOneToOneMapping();        
        assertTrue(mapping.isTraversableForJoinedAttribute());
        assertFalse(hasProblem(errorNumber, query));

        
        mapping = mapping.asMWVariableOneToOneMapping();        
        assertFalse(mapping.isTraversableForJoinedAttribute());
        assertTrue(hasProblem(errorNumber, query));

        mapping = mapping.asMWTypeConversionMapping();       
        assertFalse(mapping.isTraversableForJoinedAttribute());
        assertTrue(hasProblem(errorNumber, query));

        
        mapping = (MWMapping) mapping.asMWDirectCollectionMapping();        
        assertTrue(mapping.isTraversableForJoinedAttribute());
        assertFalse(hasProblem(errorNumber, query));

        mapping = mapping.asMWSerializedMapping();       
        assertFalse(mapping.isTraversableForJoinedAttribute());
        assertTrue(hasProblem(errorNumber, query));

        
        mapping = (MWMapping) mapping.asMWDirectMapMapping();        
        assertTrue(mapping.isTraversableForJoinedAttribute());
        assertFalse(hasProblem(errorNumber, query));

        mapping = mapping.asMWObjectTypeMapping();       
        assertFalse(mapping.isTraversableForJoinedAttribute());
        assertTrue(hasProblem(errorNumber, query));
        
        
        mapping = mapping.asMWTransformationMapping();        
        assertFalse(mapping.isTraversableForJoinedAttribute());
        assertTrue(hasProblem(errorNumber, query));
  
        
        mapping = mapping.asMWDirectToXmlTypeMapping();        
        assertFalse(mapping.isTraversableForJoinedAttribute());
        assertTrue(hasProblem(errorNumber, query));
       
        queryProject.getEmployeeDescriptor().removeMapping(mapping);
        assertTrue(hasProblem(ProblemConstants.QUERYABLE_NULL_FOR_JOINED_ITEM, query));
    }  
    
    public void testChangeReportQueryFormatToSqlFormat() {
        QueryProject queryProject = new QueryProject();
        MWReportQuery reportQuery = (MWReportQuery) queryProject.getEmployeeDescriptor().getQueryManager().queryWithSignature("reportQuery2()");
        reportQuery.addAttributeItem("lastName", queryProject.getEmployeeDescriptor().mappingNamed("lastName"));
        
        assertTrue("reportQuery2() has one ordering item chosen", reportQuery.orderingItemsSize() == 1);
        assertTrue("reportQuery2() has one grouping item chosen", reportQuery.groupingItemsSize() == 1);
        reportQuery.setQueryFormatType(MWRelationalQuery.SQL_FORMAT);
        
        assertTrue("reportQuery2() ordering items have been cleared", reportQuery.orderingItemsSize() == 0);
        assertTrue("reportQuery2() grouping items have been cleared", reportQuery.groupingItemsSize() == 0);
        assertTrue("reportQuery2() attribute items should NOT have been cleared", reportQuery.attributeItemsSize() == 1);
             
        
        try {
            reportQuery.addOrderingItem(queryProject.getEmployeeDescriptor().mappingNamed("firstName"));
        } catch(IllegalStateException e) {
            assertTrue(true);
        }
        assertTrue("No ordering item should have been added", reportQuery.orderingItemsSize() == 0);
 
    
        try {
            reportQuery.addGroupingItem(queryProject.getEmployeeDescriptor().mappingNamed("firstName"));
        } catch(IllegalStateException e) {
            assertTrue(true);
        }
        assertTrue("No grouping item should have been added", reportQuery.groupingItemsSize() == 0);
    }
    
    public void testChangeReportQueryFormatToEjbqlFormat() {
        QueryProject queryProject = new QueryProject();
        MWReportQuery reportQuery = (MWReportQuery) queryProject.getEmployeeDescriptor().getQueryManager().queryWithSignature("reportQuery2()");
        reportQuery.addAttributeItem("lastName", queryProject.getEmployeeDescriptor().mappingNamed("lastName"));
        
        assertTrue("reportQuery2() should have 1 ordering item chosen", reportQuery.orderingItemsSize() == 1);
        assertTrue("reportQuery2() should have 1 grouping item chosen", reportQuery.groupingItemsSize() == 1);
        assertTrue("reportQuery2() should have 1 attribute item chosen", reportQuery.attributeItemsSize() == 1);
        reportQuery.setQueryFormatType(MWRelationalQuery.EJBQL_FORMAT);
        
        assertTrue("reportQuery2() ordering items should have been cleared", reportQuery.orderingItemsSize() == 0);
        assertTrue("reportQuery2() grouping items should have been cleared", reportQuery.groupingItemsSize() == 0);
        assertTrue("reportQuery2() attribute items should have been cleared", reportQuery.attributeItemsSize() == 0);
              
        
        try {
            reportQuery.addOrderingItem(queryProject.getEmployeeDescriptor().mappingNamed("firstName"));
        } catch(IllegalStateException e) {
            assertTrue(true);
        }
        assertTrue("No ordering item should have been added", reportQuery.orderingItemsSize() == 0);
 
    
        try {
            reportQuery.addGroupingItem(queryProject.getEmployeeDescriptor().mappingNamed("firstName"));
        } catch(IllegalStateException e) {
            assertTrue(true);
        }
        assertTrue("No grouping item should have been added", reportQuery.groupingItemsSize() == 0);
        
        try {
            reportQuery.addAttributeItem("firstName", queryProject.getEmployeeDescriptor().mappingNamed("firstName"));
        } catch(IllegalStateException e) {
            assertTrue(true);
        }
        assertTrue("No attribute item should have been added", reportQuery.attributeItemsSize() == 0);
    }
    
    public void testChangeReadAllQueryFormatToEjbqlFormat() {
        QueryProject queryProject = new QueryProject();
        MWRelationalReadAllQuery readAllQuery = (MWRelationalReadAllQuery) queryProject.getEmployeeDescriptor().getQueryManager().queryWithSignature("myQuery4(java.lang.Integer)");
        
        assertTrue("reportQuery2() should have 1 ordering item chosen", readAllQuery.orderingItemsSize() == 2);
        assertTrue("reportQuery2() should have 1 batch read item chosen", readAllQuery.batchReadItemsSize() == 1);
        readAllQuery.setQueryFormatType(MWRelationalQuery.EJBQL_FORMAT);
        
        assertTrue("reportQuery2() ordering items should have been cleared", readAllQuery.orderingItemsSize() == 0);
        assertTrue("reportQuery2() batchread items should NOT have been cleared", readAllQuery.batchReadItemsSize() == 1);
              
        
        try {
            readAllQuery.addOrderingItem(queryProject.getEmployeeDescriptor().mappingNamed("firstName"));
        } catch(IllegalStateException e) {
            assertTrue(true);
        }
        assertTrue("No ordering item should have been added", readAllQuery.orderingItemsSize() == 0);
 
    
        readAllQuery.addBatchReadItem(queryProject.getEmployeeDescriptor().mappingNamed("phoneNumbers"));
        assertTrue("batchread item should have been added", readAllQuery.batchReadItemsSize() == 2);
                
    }
    
    public void testChangeReadAllQueryFormatToSqlFormat() {
        QueryProject queryProject = new QueryProject();
        MWRelationalReadAllQuery readAllQuery = (MWRelationalReadAllQuery) queryProject.getEmployeeDescriptor().getQueryManager().queryWithSignature("myQuery4(java.lang.Integer)");
        
        assertTrue("reportQuery2() has one ordering item chosen", readAllQuery.orderingItemsSize() == 2);
        assertTrue("reportQuery2() has one batchread item chosen", readAllQuery.batchReadItemsSize() == 1);
        readAllQuery.setQueryFormatType(MWRelationalQuery.SQL_FORMAT);
        
        assertTrue("reportQuery2() ordering items have been cleared", readAllQuery.orderingItemsSize() == 0);
        assertTrue("reportQuery2() batchread items have been cleared", readAllQuery.batchReadItemsSize() == 0);
              
        
        try {
            readAllQuery.addOrderingItem(queryProject.getEmployeeDescriptor().mappingNamed("firstName"));
        } catch(IllegalStateException e) {
            assertTrue(true);
        }
        assertTrue("No ordering item should have been added", readAllQuery.orderingItemsSize() == 0);
 
    
        try {
            readAllQuery.addBatchReadItem(queryProject.getEmployeeDescriptor().mappingNamed("phoneNumbers"));
        } catch(IllegalStateException e) {
            assertTrue(true);
        }
        assertTrue("No batchread item should have been added", readAllQuery.batchReadItemsSize() == 0);
            
    }

}