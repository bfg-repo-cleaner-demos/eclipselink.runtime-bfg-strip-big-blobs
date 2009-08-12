/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     07/16/2009-2.0  mobrien - JPA 2.0 Metadata API test model
 *       - 266912: JPA 2.0 Metamodel API (part of the JSR-317 EJB 3.1 Criteria API)  
 ******************************************************************************/  
package org.eclipse.persistence.testing.models.jpa.metamodel;

import static javax.persistence.GenerationType.TABLE;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.GeneratedValue;
import javax.persistence.TableGenerator;

@Embeddable
public class EmbeddedPK implements Serializable {
    // This class is embedded inside a Location Entity
    @GeneratedValue(strategy=TABLE, generator="EMBEDDEDPK_MM_TABLE_GENERATOR")
    @TableGenerator(
        name="EMBEDDEDPK_MM_TABLE_GENERATOR", 
        table="CMP3_MM_EMBEDDEDPK_SEQ", 
        pkColumnName="SEQ_MM_NAME", 
        valueColumnName="SEQ_MM_COUNT",
        pkColumnValue="CUST_MM_SEQ"
    )
    //@Basic(optional = false)
    @Column(name = "GALACTIC_ID", nullable = false)
    private int pk_part1;

    //@Basic(optional = false)
    //@Column(name = "PK_PART2", nullable = false)
    //private int pk_part2;

    public EmbeddedPK() {
    }
}

