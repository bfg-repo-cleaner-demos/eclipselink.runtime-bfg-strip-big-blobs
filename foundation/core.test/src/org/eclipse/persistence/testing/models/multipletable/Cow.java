/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.models.multipletable;


/**
 * A cow object uses mutliple table foreign key.
 *
 * @auther Guy Pelletier
 * @version 1.0
 * @date June 17, 2005
 */
public class Cow {
    protected int cowId;
    protected int calfCount;
    protected int calfCountId;
    protected String name;

    public Cow() {
    }

    public int getCalfCount() {
        return this.calfCount;
    }

    public int getCalfCountId() {
        return calfCountId;
    }

    public int getCowId() {
        return this.cowId;
    }

    public String getName() {
        return this.name;
    }

    public void setCalfCount(int calfCount) {
        this.calfCount = calfCount;
    }

    public void setCowId(int cowId) {
        this.cowId = cowId;
    }

    public void setName(String name) {
        this.name = name;
    }
}