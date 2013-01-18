/*******************************************************************************
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 *
 ******************************************************************************/
package org.eclipse.persistence.jpa.rs;

import java.util.List;

import javax.persistence.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.eclipse.persistence.internal.jpa.EJBQueryImpl;
import org.eclipse.persistence.internal.queries.ReportItem;
import org.eclipse.persistence.jpa.rs.util.JPARSLogger;
import org.eclipse.persistence.jpa.rs.util.StreamingOutputMarshaller;
import org.eclipse.persistence.jpa.rs.util.list.MultiResultQueryList;
import org.eclipse.persistence.jpa.rs.util.list.MultiResultQueryListItem;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.queries.ReportQuery;

@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Path("/{context}/query/")
public class QueryResource extends AbstractResource {

    @POST
    @Path("{name}")
    public Response namedQueryUpdate(@PathParam("context") String persistenceUnit, @PathParam("name") String name, @Context HttpHeaders hh, @Context UriInfo ui) {
        return namedQueryUpdateInternal(persistenceUnit, name, hh, ui);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Response namedQueryUpdateInternal(String persistenceUnit, String name, HttpHeaders hh, UriInfo ui) {
        PersistenceContext app = getPersistenceFactory().get(persistenceUnit, ui.getBaseUri(), null);
        if (app == null) {
            JPARSLogger.fine("jpars_could_not_find_persistence_context", new Object[] { persistenceUnit });
            return Response.status(Status.NOT_FOUND).build();
        }
        int result = app.queryExecuteUpdate(getMatrixParameters(ui, persistenceUnit), name, getMatrixParameters(ui, name), getQueryParameters(ui));
        JAXBElement jaxbElement = new JAXBElement(new QName(StreamingOutputMarshaller.NO_ROUTE_JAXB_ELEMENT_LABEL), new Integer(result).getClass(), result);
        return Response.ok(new StreamingOutputMarshaller(app, jaxbElement, hh.getAcceptableMediaTypes())).build();
    }
    
    @GET
    @Path("{name}")
    public Response namedQuery(@PathParam("context") String persistenceUnit, @PathParam("name") String name, @Context HttpHeaders hh, @Context UriInfo ui) {
        return namedQueryInternal(persistenceUnit, name, hh, ui);
    }
    
    @SuppressWarnings("unchecked")
    protected Response namedQueryInternal(String persistenceUnit, String name, HttpHeaders hh, UriInfo ui) {
        PersistenceContext app = getPersistenceFactory().get(persistenceUnit, ui.getBaseUri(), null);
        if (app == null) {
            JPARSLogger.fine("jpars_could_not_find_persistence_context", new Object[] { persistenceUnit });
            return Response.status(Status.NOT_FOUND).build();
        }
        Query query = app.buildQuery(getMatrixParameters(ui, persistenceUnit), name, getMatrixParameters(ui, name), getQueryParameters(ui));
        DatabaseQuery dbQuery = ((EJBQueryImpl<?>) query).getDatabaseQuery();
        if (dbQuery instanceof ReportQuery) {
            // simple types selected : select u.name, u.age from employee
            List<ReportItem> reportItems = ((ReportQuery) dbQuery).getItems();
            List<Object[]> queryResults = query.getResultList();
            if ((queryResults != null) && (!queryResults.isEmpty())) {
                MultiResultQueryList list = populateReportQueryResponse(queryResults, reportItems);
                if (list != null) {
                    return Response.ok(new StreamingOutputMarshaller(app, list, hh.getAcceptableMediaTypes())).build();
                } else {
                 // something wrong with the descriptors
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
            }
            return Response.ok(new StreamingOutputMarshaller(app, queryResults, hh.getAcceptableMediaTypes())).build();
        } 
        List<Object> results = query.getResultList();
        return Response.ok(new StreamingOutputMarshaller(app, results, hh.getAcceptableMediaTypes())).build();
    }

    @SuppressWarnings({ "rawtypes" })
    private MultiResultQueryList populateReportQueryResponse(List<Object[]> results, List<ReportItem> reportItems) {
        MultiResultQueryList response = new MultiResultQueryList();
        for (Object result : results) {
            MultiResultQueryListItem queryResultListItem = new MultiResultQueryListItem();
            List<JAXBElement> jaxbFields = createShellJAXBElementList(reportItems, result);
           if (jaxbFields == null) {
                return null;
            }
            queryResultListItem.setFields(jaxbFields);
            response.addItem(queryResultListItem);
        }
        return response;
    }
}
