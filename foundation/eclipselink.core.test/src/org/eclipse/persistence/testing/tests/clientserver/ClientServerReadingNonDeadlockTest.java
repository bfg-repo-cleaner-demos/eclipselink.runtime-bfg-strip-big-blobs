/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.tests.clientserver;

import org.eclipse.persistence.testing.framework.*;
import org.eclipse.persistence.sessions.DatabaseLogin;
import org.eclipse.persistence.exceptions.*;

public class ClientServerReadingNonDeadlockTest extends TestCase {
    protected DatabaseLogin login;
    protected Client2[] policyClients;
    protected Client2[] policyHolderClients;
    protected Server server;
    private static final int CLIENT_NUM = 300;

    public ClientServerReadingNonDeadlockTest() {
        policyClients = new PolicyClientNonLock[CLIENT_NUM];
        policyHolderClients = new PolicyHolderClient[CLIENT_NUM];

        setDescription("The test simulates the deadlock situation when reading the bidirectional objects among multi-thread");
    }

    public void reset() {
        try {
            for (int index2 = 0; index2 < CLIENT_NUM; index2++) {
                policyHolderClients[index2].release();
            }

            for (int index = 0; index < CLIENT_NUM; index++) {
                policyClients[index].release();
            }

            this.server.logout();

            getDatabaseSession().logout();
            getDatabaseSession().login();

        } catch (Exception ex) {
            if (ex instanceof ValidationException) {
                this.verify();
            }
        }
    }

    public void setup() {
        try {
            this.login = (DatabaseLogin)getSession().getLogin().clone();
            this.server = new Server(this.login);
            this.server.serverSession.setSessionLog(getSession().getSessionLog());
            this.server.login();
            this.server.copyDescriptors(getSession());
            for (int index = 0; index < CLIENT_NUM; index++) {
                policyClients[index] = new PolicyClientNonLock(this.server, getSession(), "PolicyClientNonLock" + index);
                policyHolderClients[index] = new PolicyHolderClient(this.server, getSession(), "PolicyHolderClient" + index);
            }
        } catch (Exception ex) {
            if (ex instanceof ValidationException) {
                this.verify();
            }
        }
    }

    public void test() {
        try {
            for (int index = 0; index < CLIENT_NUM; index++) {
                policyClients[index].start();
                policyHolderClients[index].start();
            }

            try {
                for (int index = 0; index < CLIENT_NUM; index++) {
                    policyClients[index].join();
                    policyHolderClients[index].join();
                }
            } catch (InterruptedException exception) {
                TestErrorException testException = new TestErrorException("Client threads are interrupted");
                testException.setInternalException(exception);
                throw testException;
            }
        } catch (Exception ex) {
            if (ex instanceof ValidationException) {
                this.verify();
            }
        }
    }

    public void verify() {
        try {
            if (this.server.errorOccured) {
                throw new TestErrorException("An error occurred on one of the clients, check System.out.");
            }
        } catch (Exception ex) {
            if ((ex instanceof ValidationException) && (((ValidationException)ex).getErrorCode() == 7090)) {
            } else {
                throw new TestErrorException("Error: ", ex);
            }
        }
    }
}
