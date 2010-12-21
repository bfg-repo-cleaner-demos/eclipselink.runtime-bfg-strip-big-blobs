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
package org.eclipse.persistence.sessions.remote.rmi.iiop;


// Tie class generated by rmic, do not edit.
// Contents subject to change without notice.
import org.eclipse.persistence.internal.sessions.remote.Transporter;
import java.rmi.Remote;
import javax.rmi.CORBA.Tie;
import org.omg.CORBA.BAD_OPERATION;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.ResponseHandler;
import org.omg.CORBA.portable.UnknownException;
import org.omg.CORBA_2_3.portable.ObjectImpl;

/**
 * INTERNAL:
 */
public class _RMIRemoteSessionControllerDispatcher_Tie extends ObjectImpl implements Tie {
    private RMIRemoteSessionControllerDispatcher target = null;
    private static final String[] _type_ids = { "RMI:org.eclipse.persistence.sessions.remote.rmi.iiop.RMIRemoteSessionController:0000000000000000" };

    public String[] _ids() {
        return _type_ids;
    }

    public OutputStream _invoke(String method, InputStream _in, ResponseHandler reply) throws SystemException {
        try {
            org.omg.CORBA_2_3.portable.InputStream in = (org.omg.CORBA_2_3.portable.InputStream)_in;
            switch (method.length()) {
            case 10:
                if (method.equals("_get_login")) {
                    Transporter result = target.getLogin();
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 12:
                if (method.equals("executeQuery")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.executeQuery(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 13:
                if (method.equals("getDescriptor")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.getDescriptor(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 14:
                if (method.equals("processCommand")) {
                    Transporter arg0 = (Transporter)in.read_value(Transporter.class);
                    Transporter result = target.processCommand(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    out.write_value(result, Transporter.class);
                    return out;
                }
            case 16:
                if (method.equals("beginTransaction")) {
                    Transporter result = target.beginTransaction();
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 17:
                if (method.equals("commitTransaction")) {
                    Transporter result = target.commitTransaction();
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("executeNamedQuery")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter arg1 = readTransporter(in);
                    Transporter arg2 = readTransporter(in);
                    Transporter result = target.executeNamedQuery(arg0, arg1, arg2);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 18:
                if (method.equals("cursoredStreamSize")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.cursoredStreamSize(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 19:
                if (method.equals("cursorSelectObjects")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.cursorSelectObjects(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("cursoredStreamClose")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.cursoredStreamClose(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("rollbackTransaction")) {
                    Transporter result = target.rollbackTransaction();
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 20:
                if (method.equals("commitRootUnitOfWork")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.commitRootUnitOfWork(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("scrollableCursorLast")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorLast(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("scrollableCursorSize")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorSize(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 21:
                if (method.equals("scrollableCursorClose")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorClose(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("scrollableCursorFirst")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorFirst(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 22:
                if (method.equals("cursoredStreamNextPage")) {
                    Transporter arg0 = readTransporter(in);
                    int arg1 = in.read_long();
                    Transporter result = target.cursoredStreamNextPage(arg0, arg1);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("getSequenceNumberNamed")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.getSequenceNumberNamed(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("scrollableCursorIsLast")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorIsLast(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 23:
                if (method.equals("scrollableCursorIsFirst")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorIsFirst(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 24:
                if (method.equals("scrollableCursorAbsolute")) {
                    Transporter arg0 = readTransporter(in);
                    int arg1 = in.read_long();
                    Transporter result = target.scrollableCursorAbsolute(arg0, arg1);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("scrollableCursorRelative")) {
                    Transporter arg0 = readTransporter(in);
                    int arg1 = in.read_long();
                    Transporter result = target.scrollableCursorRelative(arg0, arg1);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 25:
                if (method.equals("scrollableCursorAfterLast")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorAfterLast(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 26:
                if (method.equals("scrollableCursorNextObject")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorNextObject(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("getDefaultReadOnlyClasses")) {
                    Transporter result = target.getDefaultReadOnlyClasses();
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 27:
                if (method.equals("scrollableCursorBeforeFirst")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorBeforeFirst(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                } else if (method.equals("scrollableCursorIsAfterLast")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorIsAfterLast(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 28:
                if (method.equals("scrollableCursorCurrentIndex")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorCurrentIndex(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 29:
                if (method.equals("scrollableCursorIsBeforeFirst")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorIsBeforeFirst(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 30:
                if (method.equals("scrollableCursorPreviousObject")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.scrollableCursorPreviousObject(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 36:
                if (method.equals("instantiateRemoteValueHolderOnServer")) {
                    Transporter arg0 = readTransporter(in);
                    Transporter result = target.instantiateRemoteValueHolderOnServer(arg0);
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            case 37:
                if (method.equals("initializeIdentityMapsOnServerSession")) {
                    Transporter result = target.initializeIdentityMapsOnServerSession();
                    org.omg.CORBA_2_3.portable.OutputStream out = (org.omg.CORBA_2_3.portable.OutputStream)reply.createReply();
                    writeTransporter(result, out);
                    return out;
                }
            }
            throw new BAD_OPERATION();
        } catch (SystemException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new UnknownException(ex);
        }
    }

    public void deactivate() {
        _orb().disconnect(this);
        _set_delegate(null);
        target = null;
    }

    public Remote getTarget() {
        return target;
    }

    public ORB orb() {
        return _orb();
    }

    public void orb(ORB orb) {
        orb.connect(this);
    }

    /**
     * Insert the method's description here.
     * Creation date: (8/15/00 9:07:19 AM)
     * @return org.eclipse.persistence.internal.sessions.remote.Transporter
     * @param stream org.omg.CORBA_2_3.portable.InputStream
     */
    public static org.eclipse.persistence.internal.sessions.remote.Transporter readTransporter(org.omg.CORBA.portable.InputStream stream) {
        int length = stream.read_ulong();

        byte[] bytes = new byte[length];
        stream.read_octet_array(bytes, 0, length);

        java.io.ByteArrayInputStream byteIn = new java.io.ByteArrayInputStream(bytes);
        org.eclipse.persistence.internal.sessions.remote.Transporter transporter = null;
        if (bytes.length == 0) {
            return null;
        }
        try {
            java.io.ObjectInputStream objectIn = new java.io.ObjectInputStream(byteIn);
            transporter = (org.eclipse.persistence.internal.sessions.remote.Transporter)objectIn.readObject();
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }

        return transporter;
    }

    public void setTarget(Remote target) {
        this.target = (RMIRemoteSessionControllerDispatcher)target;
    }

    public org.omg.CORBA.Object thisObject() {
        return this;
    }

    /**
     * Insert the method's description here.
     * Creation date: (8/15/00 9:05:39 AM)
     * @param stream org.omg.CORBA_2_3.portable.OutputStream
     * @param transporter org.eclipse.persistence.internal.sessions.remote.Transporter
     */
    public static void writeTransporter(org.eclipse.persistence.internal.sessions.remote.Transporter transporter, org.omg.CORBA.portable.OutputStream stream) {
        java.io.ByteArrayOutputStream byteOut = new java.io.ByteArrayOutputStream();
        try {
            java.io.ObjectOutputStream objectOut = new java.io.ObjectOutputStream(byteOut);
            objectOut.writeObject(transporter);
            //		objectOut.writeBoolean(wasOperationSuccessful);
            //		objectOut.writeObject(query);
            //		objectOut.writeObject(object);
            //		objectOut.writeObject(objectDescriptors);
            objectOut.flush();
            stream.write_ulong(byteOut.size());
            stream.write_octet_array(byteOut.toByteArray(), 0, byteOut.size());
        } catch (java.io.IOException exception) {
        }
    }
}
