/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.proxyauthentication;

import java.util.Properties;

import org.eclipse.persistence.sessions.*;
import org.eclipse.persistence.sessions.server.*;

/**
 * ProxyAuthentication_FS: 4.3.1.1.	Realizations of Main use case:
 * �ServerSession uses main connection; each ClientSession uses its own proxy connection�. (see 3.1.2.1)
 * All we have to do is to make sure that proxy properties are found in the login,
 * which is used to connect ClientSession�s write connection.
 * There are three alternative approaches to realize the use case.
 * C.  Each proxy authentication is mapped to a separate external connection pool � proxy properties set into pool�s login.
 * Proxy properties could be set either before ClientSession is created (useEvent=false);
 * or in postAcquireClientSession event (useEvent=true).
 */
public class ExternalConnectionPoolTestCase extends ProxyAuthenticationConnectionTestCase {

    boolean useEvent;

    public ExternalConnectionPoolTestCase(Properties proxyProperties, boolean useEvent) {
        super(proxyProperties);
        this.useEvent = useEvent;
        String suffix;
        if (useEvent) {
            suffix = " proxy setup in Event";
        } else {
            suffix = " proxy setup before ClientSession created";
        }
        setName(getName() + suffix);
    }

    protected void proxySetup() {
        if (useEvent) {
            // Use this approach if ClientSession created with default connection policy:
            // cs = serverSession.acquireClientSession();
            listener = new SessionEventAdapter() {
                        public void postAcquireClientSession(SessionEvent event) {
                            ClientSession cs = (ClientSession)event.getSession();
                            ConnectionPolicy policy = (ConnectionPolicy)cs.getConnectionPolicy().clone();
                            cs.setConnectionPolicy(policy);
                            proxyConnectionPoolSetup(cs.getConnectionPolicy(), cs.getParent());
                        }
                    };
        } else {
            // The most natural way of realizing this use case.
            // The custom connection policy is used to create ClientSession:
            // cs = serverSession.acquireClientSession(connectionPolicy);
            ServerSession ss = (ServerSession)getServerSession();
            // clone because the policy will be changed (pool name will be altered)
            connectionPolicy = (ConnectionPolicy)ss.getDefaultConnectionPolicy().clone();
            proxyConnectionPoolSetup(connectionPolicy, ss);
        }
    }

    protected void proxyConnectionPoolSetup(ConnectionPolicy policy, ServerSession ss) {
        // The ClientSession will connect  using the pool with the same mane as proxy user
        String proxyUser = proxyProperties.getProperty("PROXY_USER_NAME");
        policy.setPoolName(proxyUser);
        // if the pool doesn�t exist � create and start up it
        ConnectionPool pool = ss.getConnectionPool(proxyUser);
        if (pool == null) {
            // Clone serverSession�s login � the clone will be used by the new connection pool
            Login login = (Login)ss.getLogin().clone();
            // set proxy properties in the login
            addProxyPropertiesToLogin(login);
            // create the new pool    
            pool = new ExternalConnectionPool(proxyUser, login, ss);
            ss.getConnectionPools().put(proxyUser, pool);
            // start it up
            pool.startUp();
        }
    }

    public void reset() {
        super.reset();
        String proxyUser = proxyProperties.getProperty("PROXY_USER_NAME");
        ServerSession ss = (ServerSession)getServerSession();
        ConnectionPool pool = ss.getConnectionPool(proxyUser);
        if (pool != null) {
            pool.shutDown();
            ss.getConnectionPools().remove(proxyUser);
        }
    }
}
