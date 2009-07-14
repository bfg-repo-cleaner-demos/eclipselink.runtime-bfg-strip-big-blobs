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
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.sessions;

import java.util.Map;

import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.sessions.server.*;
import org.eclipse.persistence.queries.*;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.AbstractRecord;

/**
 * Provides isolation support by allowing a client session
 * to have a local cache of the subset of the classes.
 * This can be used to avoid caching frequently changing data,
 * or for security or VPD purposes.
 */
public class IsolatedClientSession extends ClientSession {
    public IsolatedClientSession(ServerSession parent, ConnectionPolicy connectionPolicy) {
        super(parent, connectionPolicy);
    }

    public IsolatedClientSession(ServerSession parent, ConnectionPolicy connectionPolicy, Map properties) {
        super(parent, connectionPolicy, properties);
    }

    /**
    * INTERNAL:
    * Set up the IdentityMapManager.  This method allows subclasses of Session to override
    * the default IdentityMapManager functionality.
    */
    public void initializeIdentityMapAccessor() {
        this.identityMapAccessor = new IsolatedClientSessionIdentityMapAccessor(this);
    }

    /**
    * INTERNAL:
    * Helper method to calculate whether to execute this query locally or send
    * it to the server session.
    */
    protected boolean shouldExecuteLocally(DatabaseQuery query) {
        if (isIsolatedQuery(query)) {
            return true;
        }
        return isInTransaction();
    }

    /**
    * INTERNAL: Answers if this query is an isolated query and must be
    * executed locally.
    */
    protected boolean isIsolatedQuery(DatabaseQuery query) {
        query.checkDescriptor(this);
        if (query.isDataModifyQuery() || query.isDataReadQuery() || ((query.getDescriptor() != null) && query.getDescriptor().isIsolated()) || (query.isObjectBuildingQuery() && ((ObjectBuildingQuery)query).shouldUseExclusiveConnection())) {
            // For CR#4334 if in transaction stay on client session.
            // That way client's write accessor will be used for all queries.
            // This is to preserve transaction isolation levels.
            // also if this is an isolated class and we are in an isolated session
            //load locally.
            return true;
        }
        return false;

    }

    /**
    * INTERNAL:
    * Gets the next link in the chain of sessions followed by a query's check
    * early return, the chain of sessions with identity maps all the way up to
    * the root session.
    * <p>
    * Used for session broker which delegates to registered sessions, or UnitOfWork
    * which checks parent identity map also.
    * @param canReturnSelf true when method calls itself.  If the path
    * starting at <code>this</code> is acceptable.  Sometimes true if want to
    * move to the first valid session, i.e. executing on ClientSession when really
    * should be on ServerSession.
    * @param terminalOnly return the session we will execute the call on, not
    * the next step towards it.
    * @return this if there is no next link in the chain
    */
    public AbstractSession getParentIdentityMapSession(DatabaseQuery query, boolean canReturnSelf, boolean terminalOnly) {
        if ((query != null) && isIsolatedQuery(query)) {
            return this;
        } else {
            return getParent().getParentIdentityMapSession(query, canReturnSelf, terminalOnly);
        }
    }

    /**
    * INTERNAL:
    * Gets the session which this query will be executed on.
    * Generally will be called immediately before the call is translated,
    * which is immediately before session.executeCall.
    * <p>
    * Since the execution session also knows the correct datasource platform
    * to execute on, it is often used in the mappings where the platform is
    * needed for type conversion, or where calls are translated.
    * <p>
    * Is also the session with the accessor.  Will return a ClientSession if
    * it is in transaction and has a write connection.
    * @return a session with a live accessor
    * @param query may store session name or reference class for brokers case
    */
    public AbstractSession getExecutionSession(DatabaseQuery query) {
        if (shouldExecuteLocally(query)) {
            return this;
        } else {
            return getParent().getExecutionSession(query);
        }
    }

    /**
    * INTERNAL:
    * Isolated sessions must forward call execution to its parent, unless in a transaction.
    * This is required as isolated sessions are always the execution session for isolated classes.
    */
    public Object executeCall(Call call, AbstractRecord translationRow, DatabaseQuery query) throws DatabaseException {
        if (isInTransaction()) {
            return super.executeCall(call, translationRow, query);
        }
        return getParent().executeCall(call, translationRow, query);
    }

    /**
     * PUBLIC:
     * Return if this session is an isolated client session.
     */
    public boolean isIsolatedClientSession() {
        return true;
    }    
}
