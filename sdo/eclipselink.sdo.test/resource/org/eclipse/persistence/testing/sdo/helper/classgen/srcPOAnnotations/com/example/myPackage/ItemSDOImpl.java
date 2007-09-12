/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package com.example.myPackage;

import org.eclipse.persistence.sdo.SDODataObject;

public class ItemSDOImpl extends SDODataObject implements ItemSDO {

   public static final int START_PROPERTY_INDEX = 0;

   public static final int END_PROPERTY_INDEX = START_PROPERTY_INDEX + 6;

   public ItemSDOImpl() {}

   public java.lang.String getPorder() {
      return getString(START_PROPERTY_INDEX + 0);
   }

   public void setPorder(java.lang.String value) {
      set(START_PROPERTY_INDEX + 0 , value);
   }

   public java.lang.String getProductName() {
      return getString(START_PROPERTY_INDEX + 1);
   }

   public void setProductName(java.lang.String value) {
      set(START_PROPERTY_INDEX + 1 , value);
   }

   public java.math.BigInteger getQuantity() {
      return getBigInteger(START_PROPERTY_INDEX + 2);
   }

   public void setQuantity(java.math.BigInteger value) {
      set(START_PROPERTY_INDEX + 2 , value);
   }

   public com.example.myPackage.SKU getPartNumSDO() {
      return (com.example.myPackage.SKU)get(START_PROPERTY_INDEX + 3);
   }

   public void setPartNumSDO(com.example.myPackage.SKU value) {
      set(START_PROPERTY_INDEX + 3 , value);
   }

   public java.math.BigDecimal getUSPrice() {
      return getBigDecimal(START_PROPERTY_INDEX + 4);
   }

   public void setUSPrice(java.math.BigDecimal value) {
      set(START_PROPERTY_INDEX + 4 , value);
   }

   public java.lang.String getComment() {
      return getString(START_PROPERTY_INDEX + 5);
   }

   public void setComment(java.lang.String value) {
      set(START_PROPERTY_INDEX + 5 , value);
   }

   public java.lang.String getShipDate() {
      return getString(START_PROPERTY_INDEX + 6);
   }

   public void setShipDate(java.lang.String value) {
      set(START_PROPERTY_INDEX + 6 , value);
   }


}

