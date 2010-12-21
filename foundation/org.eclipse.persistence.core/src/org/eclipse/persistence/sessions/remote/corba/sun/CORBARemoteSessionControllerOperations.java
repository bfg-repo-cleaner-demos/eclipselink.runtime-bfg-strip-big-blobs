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
package org.eclipse.persistence.sessions.remote.corba.sun;


/**
 * INTERNAL:
* org/eclipse/persistence/remote/corba/sun/CORBARemoteSessionControllerOperations.java
* Generated by the IDL-to-Java compiler (portable), version "3.0"
* from CorbaRemoteSessionControllerSun.idl
* Monday, November 19, 2001 1:51:44 o'clock PM EST
*/
public interface CORBARemoteSessionControllerOperations {
    org.eclipse.persistence.internal.sessions.remote.Transporter getLogin();

    org.eclipse.persistence.internal.sessions.remote.Transporter getDefaultReadOnlyClasses();

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorCurrentIndex(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter commitRootUnitOfWork(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorAbsolute(org.eclipse.persistence.internal.sessions.remote.Transporter arg0, int arg1);

    org.eclipse.persistence.internal.sessions.remote.Transporter cursoredStreamNextPage(org.eclipse.persistence.internal.sessions.remote.Transporter arg0, int arg1);

    org.eclipse.persistence.internal.sessions.remote.Transporter executeQuery(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorFirst(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorAfterLast(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter cursoredStreamClose(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter getSequenceNumberNamed(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorClose(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter processCommand(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter cursorSelectObjects(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorLast(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter executeNamedQuery(org.eclipse.persistence.internal.sessions.remote.Transporter arg0, org.eclipse.persistence.internal.sessions.remote.Transporter arg1, org.eclipse.persistence.internal.sessions.remote.Transporter arg2);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorBeforeFirst(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorIsBeforeFirst(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter beginTransaction();

    org.eclipse.persistence.internal.sessions.remote.Transporter initializeIdentityMapsOnServerSession();

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorIsLast(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorSize(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorIsFirst(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter getDescriptor(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter cursoredStreamSize(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorRelative(org.eclipse.persistence.internal.sessions.remote.Transporter arg0, int arg1);

    org.eclipse.persistence.internal.sessions.remote.Transporter commitTransaction();

    org.eclipse.persistence.internal.sessions.remote.Transporter rollbackTransaction();

    org.eclipse.persistence.internal.sessions.remote.Transporter instantiateRemoteValueHolderOnServer(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorNextObject(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorPreviousObject(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);

    org.eclipse.persistence.internal.sessions.remote.Transporter scrollableCursorIsAfterLast(org.eclipse.persistence.internal.sessions.remote.Transporter arg0);
}// interface CORBARemoteSessionControllerOperations
