/*******************************************************************************
 * Copyright (c) 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.queries;

import java.util.Map;

import org.eclipse.persistence.internal.jpa.EJBQueryImpl;
import org.eclipse.persistence.internal.jpa.metadata.MetadataProject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.MetadataAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.platform.database.oracle.plsql.PLSQLStoredFunctionCall;

/**
 * INTERNAL:
 * Object to hold onto a named PLSQL stored function query.
 * 
 * @author James
 * @since EclipseLink 2.3
 */
public class NamedPLSQLStoredFunctionQueryMetadata extends NamedPLSQLStoredProcedureQueryMetadata {
    private PLSQLParameterMetadata returnParameter;

    public NamedPLSQLStoredFunctionQueryMetadata() {
        super("<named-plsql-stored-function-query>");
    }

    public NamedPLSQLStoredFunctionQueryMetadata(MetadataAnnotation namedStoredProcedureQuery, MetadataAccessor accessor) {
        super(namedStoredProcedureQuery, accessor);
         
        this.returnParameter = new PLSQLParameterMetadata((MetadataAnnotation)namedStoredProcedureQuery.getAttribute("returnParameter"), accessor);
        
        setProcedureName((String) namedStoredProcedureQuery.getAttribute("functionName"));
    }

    @Override
    public boolean equals(Object objectToCompare) {
        if (super.equals(objectToCompare) && objectToCompare instanceof NamedPLSQLStoredFunctionQueryMetadata) {
            NamedPLSQLStoredFunctionQueryMetadata query = (NamedPLSQLStoredFunctionQueryMetadata) objectToCompare;                        
            
            return valuesMatch(this.returnParameter, query.getReturnParameter());
        }
        
        return false;
    }

    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject, XMLEntityMappings entityMappings) {
        super.initXMLObject(accessibleObject, entityMappings);
        
        // Initialize parameters ...
        initXMLObject(this.returnParameter, accessibleObject);
    }

    @Override
    public void process(AbstractSession session, ClassLoader loader, MetadataProject project) {
        // Build the stored procedure call.
        PLSQLStoredFunctionCall call = new PLSQLStoredFunctionCall();
        
        // Process the stored procedure parameters.
        for (PLSQLParameterMetadata parameter : getParameters()) {
            parameter.process(call, project, false);
        }
        
        if (getReturnParameter() != null) {
            getReturnParameter().process(call, project, true);
        }
        
        // Process the procedure name.
        call.setProcedureName(getProcedureName());
                
        // Process the query hints.
        Map<String, Object> hints = processQueryHints(session);
        
        // Process the result class.
        if (getResultClass().isVoid()) {
            if (hasResultSetMapping(session)) {
                session.addQuery(getName(), EJBQueryImpl.buildStoredProcedureQuery(getResultSetMapping(), call, hints, loader, session));
            } else {
                // Neither a resultClass or resultSetMapping is specified so place in a temp query on the session
                session.addQuery(getName(), EJBQueryImpl.buildStoredProcedureQuery(call, hints, loader, session));
            }
        } else {
            session.addQuery(getName(), EJBQueryImpl.buildStoredProcedureQuery(MetadataHelper.getClassForName(getResultClass().getName(), loader), call, hints, loader, session));
        }
    }

    public PLSQLParameterMetadata getReturnParameter() {
        return returnParameter;
    }

    public void setReturnParameter(PLSQLParameterMetadata returnParameter) {
        this.returnParameter = returnParameter;
    }
}
