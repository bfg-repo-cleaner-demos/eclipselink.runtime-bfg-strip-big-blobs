/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.tests.remote.suncorba;

/**
* org/eclipse/persistence/testing/Remote/SunCORBA/CORBAServerManagerHelper.java
* Generated by the IDL-to-Java compiler (portable), version "3.0"
* from CORBAServerManager.idl
* Wednesday, August 23, 2000 1:20:43 PM EDT
*/

abstract public class CORBAServerManagerHelper
{
  private static String  _id = "IDL:org/eclipse/persistence/testing/Remote/SunCORBA/CORBAServerManager:1.0";

  private static org.omg.CORBA.TypeCode __typeCode = null;
  public static org.eclipse.persistence.testing.tests.remote.suncorba.CORBAServerManager extract (org.omg.CORBA.Any a)
  {
	return read (a.create_input_stream ());
  }  
  public static String id ()
  {
	return _id;
  }  
  public static void insert (org.omg.CORBA.Any a, org.eclipse.persistence.testing.tests.remote.suncorba.CORBAServerManager that)
  {
	org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
	a.type (type ());
	write (out, that);
	a.read_value (out.create_input_stream (), type ());
  }  
  public static org.eclipse.persistence.testing.tests.remote.suncorba.CORBAServerManager narrow (org.omg.CORBA.Object obj)
  {
	if (obj == null)
	  return null;
	else if (obj instanceof org.eclipse.persistence.testing.tests.remote.suncorba.CORBAServerManager)
	  return (org.eclipse.persistence.testing.tests.remote.suncorba.CORBAServerManager)obj;
	else if (!obj._is_a (id ()))
	  throw new org.omg.CORBA.BAD_PARAM ();
	else
	{
	  org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
	  return new org.eclipse.persistence.testing.tests.remote.suncorba._CORBAServerManagerStub (delegate);
	}
  }  
  public static org.eclipse.persistence.testing.tests.remote.suncorba.CORBAServerManager read (org.omg.CORBA.portable.InputStream istream)
  {
	return narrow (istream.read_Object (_CORBAServerManagerStub.class));
  }  
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
	if (__typeCode == null)
	{
	  __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (org.eclipse.persistence.testing.tests.remote.suncorba.CORBAServerManagerHelper.id (), "CORBAServerManager");
	}
	return __typeCode;
  }  
  public static void write (org.omg.CORBA.portable.OutputStream ostream, org.eclipse.persistence.testing.tests.remote.suncorba.CORBAServerManager value)
  {
	ostream.write_Object (value);
  }  
}
