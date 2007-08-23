/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/
package org.eclipse.persistence.testing.models.jpa.fieldaccess.advanced;

import javax.persistence.*;

import static javax.persistence.GenerationType.*;
import static javax.persistence.CascadeType.*;

@Entity(name="Woman")
@Table(name="CMP3_FA_WOMAN")
public class Woman {
	@Id
    @GeneratedValue(strategy=SEQUENCE, generator="FA_WOMAN_SEQ_GENERATOR")
	@SequenceGenerator(name="FA_WOMAN_SEQ_GENERATOR", sequenceName="WOMAN_SEQ")
    private Integer id;
	@OneToOne(mappedBy="woman")
    private PartnerLink partnerLink;

	public Woman() {}
    
    
	public Integer getId() { 
        return id; 
    }
       
	public PartnerLink getPartnerLink() { 
        return partnerLink; 
    }
    
	public void setId(Integer id) { 
        this.id = id; 
    }
    
    public void setPartnerLink(PartnerLink partnerLink) { 
        this.partnerLink = partnerLink; 
    }
}
