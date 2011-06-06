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
package org.eclipse.persistence.testing.tests.simplepojoclient;

import java.math.BigDecimal;

/**
 *  ###  Generated by TopLink Workbench 10.1.3.0.0 - Mon Apr 11 13:49:33 EDT 2005.  ###
 * 
 * This class has no zero argument constructor.
 * This source code generation mechanism uses the 
 *  zero argument constructor to initialize instance variables.
 * In order for this class to be used by TopLink,
 *  please make sure that its instance variables are initialized properly.
 */
public class

PojoEmployee {

    private BigDecimal empId;
    private String fName;
    private String gender;
    private String lName;
    private BigDecimal managerId;

    public BigDecimal getEmpId() {
        return this.empId;
    }

    public String getFName() {
        return this.fName;
    }

    public String getGender() {
        return this.gender;
    }

    public String getLName() {
        return this.lName;
    }

    public BigDecimal getManagerId() {
        return this.managerId;
    }

    public void setEmpId(BigDecimal empId) {
        this.empId = empId;
    }

    public void setFName(String fName) {
        this.fName = fName;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setLName(String lName) {
        this.lName = lName;
    }

    public void setManagerId(BigDecimal managerId) {
        this.managerId = managerId;
    }

}
