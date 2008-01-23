/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  


package org.eclipse.persistence.testing.models.jpa.inheritance;

import java.io.*;
import javax.persistence.*;

@Entity
@Table(name="CMP3_TIRE")
@DiscriminatorValue("Performance")
public class PerformanceTireInfo extends TireInfo implements Serializable {
    protected Integer speedrating;

    public PerformanceTireInfo() {}

	@Column(name="SPEEDRATING")
    public Integer getSpeedRating() {
        return this.speedrating;
    }

    public void setSpeedRating(Integer rating) {
        this.speedrating = rating;
    }

}