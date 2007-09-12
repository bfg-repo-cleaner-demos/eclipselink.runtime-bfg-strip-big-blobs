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

public interface CustomerType {

   public java.lang.String getFirstName();

   public void setFirstName(java.lang.String value);

   public java.lang.String getLastName();

   public void setLastName(java.lang.String value);

   public defaultPackage.AddressType getAddress();

   public void setAddress(defaultPackage.AddressType value);

   public int getCustomerID();

   public void setCustomerID(int value);

   public java.lang.String getSin();

   public void setSin(java.lang.String value);


}

