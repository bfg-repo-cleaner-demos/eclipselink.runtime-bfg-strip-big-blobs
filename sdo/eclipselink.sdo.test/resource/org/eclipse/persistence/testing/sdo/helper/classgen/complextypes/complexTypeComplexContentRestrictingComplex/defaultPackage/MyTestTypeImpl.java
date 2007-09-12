/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package defaultPackage;

import org.eclipse.persistence.sdo.SDODataObject;

public class MyTestTypeImpl extends defaultPackage.TestTypeImpl implements MyTestType {

   public static final int START_PROPERTY_INDEX = defaultPackage.TestTypeImpl.END_PROPERTY_INDEX + 1;

   public static final int END_PROPERTY_INDEX = START_PROPERTY_INDEX + 1;

   public MyTestTypeImpl() {}

   public java.lang.String getMyTest1() {
      return getString(START_PROPERTY_INDEX + 0);
   }

   public void setMyTest1(java.lang.String value) {
      set(START_PROPERTY_INDEX + 0 , value);
   }

   public java.lang.String getMyTest2() {
      return getString(START_PROPERTY_INDEX + 1);
   }

   public void setMyTest2(java.lang.String value) {
      set(START_PROPERTY_INDEX + 1 , value);
   }


}

