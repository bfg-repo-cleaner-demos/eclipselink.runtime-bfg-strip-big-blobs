/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
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
package org.eclipse.persistence.internal.sessions.coordination.corba.sun;


/**
* org/eclipse/persistence/internal/remotecommand/corba/sun/SunCORBAConnectionHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from rcm.idl
* Tuesday, March 30, 2004 2:00:14 PM EST
*/
public final class SunCORBAConnectionHolder implements org.omg.CORBA.portable.Streamable {
    public org.eclipse.persistence.internal.sessions.coordination.corba.sun.SunCORBAConnection value = null;

    public SunCORBAConnectionHolder() {
    }

    public SunCORBAConnectionHolder(org.eclipse.persistence.internal.sessions.coordination.corba.sun.SunCORBAConnection initialValue) {
        value = initialValue;
    }

    public void _read(org.omg.CORBA.portable.InputStream i) {
        value = org.eclipse.persistence.internal.sessions.coordination.corba.sun.SunCORBAConnectionHelper.read(i);
    }

    public void _write(org.omg.CORBA.portable.OutputStream o) {
        org.eclipse.persistence.internal.sessions.coordination.corba.sun.SunCORBAConnectionHelper.write(o, value);
    }

    public org.omg.CORBA.TypeCode _type() {
        return org.eclipse.persistence.internal.sessions.coordination.corba.sun.SunCORBAConnectionHelper.type();
    }
}