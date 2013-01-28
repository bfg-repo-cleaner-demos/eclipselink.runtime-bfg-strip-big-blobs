/*******************************************************************************
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     02/04/2013-2.5 Guy Pelletier 
 *       - 389090: JPA 2.1 DDL Generation Support
 ******************************************************************************/ 
package org.eclipse.persistence.testing.models.jpa21.advanced.ddl.schema;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name="JPA21_DDL_SHOE", schema="THING")
public class Shoe {
    @Id
    @GeneratedValue
    protected Integer id;
    
    @Column(name="SIZZE")
    protected Integer size;
    
    protected String brand;
    protected String model;
    
    @ManyToOne
    @JoinColumn(
        name="RUNNER_ID",
        foreignKey=@ForeignKey(
            name="Shoes_Runner_Foreign_Key",
            foreignKeyDefinition="FOREIGN KEY (RUNNER_ID) REFERENCES JPA21_DDL_RUNNER (ID)"      
        ) 
    )
    protected Runner runner;
    
    public Shoe() {}

    public String getBrand() {
        return brand;
    }
    
    public Integer getId() {
        return id;
    }
    
    public String getModel() {
        return model;
    }
    
    public Runner getRunner() {
        return runner;
    }
    
    public Integer getSize() {
        return size;
    }    
    
    public void setBrand(String brand) {
        this.brand = brand;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
    
    public void setRunner(Runner runner) {
        this.runner = runner;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
}
