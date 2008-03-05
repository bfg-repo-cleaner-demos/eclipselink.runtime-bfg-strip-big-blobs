/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
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
package org.eclipse.persistence.internal.jpa.metadata.queries;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.jpa.metadata.MetadataHelper;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.queries.EJBQLPlaceHolderQuery;

/**
 * INTERNAL:
 * Object to hold onto a named query metadata.
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public class NamedQueryMetadata extends QueryMetadata {
    /**
     * INTERNAL:
     */
    public NamedQueryMetadata() {
    	setLoadedFromXML();
    }

    /**
     * INTERNAL:
     */
    public NamedQueryMetadata(Annotation namedQuery, String javaClassName) {
    	setLoadedFromAnnotation();
        setLocation(javaClassName);
        
        setName((String) invokeMethod("name", namedQuery));
        setQuery((String) invokeMethod("query", namedQuery));
        setHints((Annotation[]) invokeMethod("hints", namedQuery));     
    }
    
    /**
     * INTERNAL:
     */
    public boolean equals(Object objectToCompare) {
    	if (objectToCompare instanceof NamedQueryMetadata) {
    		NamedQueryMetadata namedQuery = (NamedQueryMetadata) objectToCompare;
    		
    		if (! MetadataHelper.valuesMatch(getName(), namedQuery.getName())) {
    			return false;
    		}
    		
    		if (! MetadataHelper.valuesMatch(getQuery(), namedQuery.getQuery())) {
    			return false;
    		}
    		
    		if (getHints().size() != namedQuery.getHints().size()) {
    			return false;
        	} else {
    			for (QueryHintMetadata hint : getHints()) {
        			if (! namedQuery.hasHint(hint)) {
        				return false;
        			}
        		}
    		}
    		
    		return true;
    	}
    	
    	return false;
    }
    
    /**
     * INTERNAL:
     */
    public void process(AbstractSession session) {
        try {
            HashMap<String, String> hints = processQueryHints(session);
            session.addEjbqlPlaceHolderQuery(new EJBQLPlaceHolderQuery(getName(), getQuery(), hints));
        } catch (Exception exception) {
            throw ValidationException.errorProcessingNamedQuery(getClass(), getName(), exception);
        }
    }
}
