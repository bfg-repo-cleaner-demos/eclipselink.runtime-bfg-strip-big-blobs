/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/
package org.eclipse.persistence.testing.models.jpa.advanced;

import java.io.Serializable;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="CMP3_VEGETABLE")
public class Vegetable implements Serializable {
    private VegetablePK id;       
    private double cost;
    private String[] tags;
    
    public Vegetable() {}

    public boolean equals(Object otherVegetable) {
        if (otherVegetable instanceof Vegetable) {
            return getId().equals(((Vegetable) otherVegetable).getId());
        }
        
        return false;
    }

    public double getCost() {
        return cost;
    }
    
    @EmbeddedId
    @AttributeOverrides({
        @AttributeOverride(name="name", column=@Column(name="vegetable_name")),
        @AttributeOverride(name="color", column=@Column(name="vegetable_color"))
    })
    public VegetablePK getId() {
        return id;
    }
    
    public int hashCode() {
        int hash = 0;
        hash += (this.getId() != null ? this.getId().hashCode() : 0);
        return hash;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
    
    public void setId(VegetablePK id) {
        this.id = id;
    }
    
    public String[] getTags() {
        return this.tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }
    
    public String toString() {
        return "Vegetable[id=" + getId() + "]";
    }
}
