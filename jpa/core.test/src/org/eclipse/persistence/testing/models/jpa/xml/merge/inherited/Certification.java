/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  


package org.eclipse.persistence.testing.models.jpa.xml.merge.inherited;

import javax.persistence.*;
import static javax.persistence.CascadeType.*;
import static javax.persistence.GenerationType.*;

public class Certification  {
    private Integer id;
    private String description;
    private BeerConsumer beerConsumer;
    
    public Certification() {}
    
    // Relationship incorrectly defined and overidden by XML
    @ManyToOne
	@JoinColumn(name="WRONG_CONSUMER_ID")  
    public BeerConsumer getBeerConsumer() {
        return beerConsumer;
    }
    
    public String getDescription() {
        return description;    
    }
    
    // This annotation is used for the Id
    @Id
    @GeneratedValue(strategy=TABLE, generator="CERTIFICATION_TABLE_GENERATOR")
	@TableGenerator(
        name="MERGE_CERTIFICATION_TABLE_GENERATOR", 
        table="CMP3_MERGE_BEER_SEQ", 
        pkColumnName="SEQ_NAME", 
        valueColumnName="SEQ_COUNT",
        pkColumnValue="CERTIFICATION_SEQ")
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public void setBeerConsumer(BeerConsumer beerConsumer) {
        this.beerConsumer = beerConsumer;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
