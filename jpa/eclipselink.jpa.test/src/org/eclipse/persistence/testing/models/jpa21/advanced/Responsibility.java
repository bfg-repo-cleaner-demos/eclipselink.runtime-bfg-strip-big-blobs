/*******************************************************************************
 * Copyright (c) 2012, 2013 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     11/28/2012-2.5 Guy Pelletier 
 *       - 374688: JPA 2.1 Converter support
 ******************************************************************************/ 
package org.eclipse.persistence.testing.models.jpa21.advanced;

import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.GeneratedValue;

@Embeddable
public class Responsibility {
    public Long uniqueIdentifier;
    public String description;
    
    public Responsibility() {}
    
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Responsibility) {
            Responsibility r = (Responsibility) obj;
            
            return (r.getDescription().equals(getDescription()) && (r.getUniqueIdentifier().equals(getUniqueIdentifier()))); 
        }
        
        return false;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Long getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    @Override
    public int hashCode() {
        if (uniqueIdentifier != null) {
            return uniqueIdentifier.intValue();
        }
        
        return -1;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setUniqueIdentifier(Long uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
    }
}
