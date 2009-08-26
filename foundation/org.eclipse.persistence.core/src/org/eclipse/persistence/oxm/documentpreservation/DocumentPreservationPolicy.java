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
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.oxm.documentpreservation;

import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.mappings.XMLMapping;

import org.w3c.dom.Node;

/**
 * <b>Purpose:</b>Provides an interface for customizing how Documents are
 * preserved.
 * <p><b>Responsibilities:</b><ul>
 * <li>Access objects from the cache based on node</li>
 * <li>Access nodes from the cache based on Object</li>
 * <li>Add objects and nodes to the cache</li>
 * <li>Allow the configuration of how nodes are added into the preserved doc</li>
 * 
 * @author mmacivor
 * @since Oracle TopLink 11g
 */
public abstract class DocumentPreservationPolicy {
    private NodeOrderingPolicy nodeOrderingPolicy;
    
    /**
     * PUBLIC:
     * Sets the NodeOrderingPolicy to be used by this DocumentPreservationPolicy
     * when adding new elements into a cached XML Document.
     * @see AppendNewElementsOrderingPolicy
     * @see IgnoreNewElementsOrderingPolicy
     * @see RelativePositionNodeOrderingPolicy
     */
    public void setNodeOrderingPolicy(NodeOrderingPolicy policy) {
        this.nodeOrderingPolicy = policy;
    }
    public NodeOrderingPolicy getNodeOrderingPolicy() {
        return nodeOrderingPolicy;
    }
    
    public abstract void addObjectToCache(Object obj, Node node);
    
    public abstract void addObjectToCache(Object obj, Node node, XMLMapping selfRecordMapping);
        
    public abstract Node getNodeForObject(Object obj);
    
    public abstract Object getObjectForNode(Node node);
    
    public abstract Object getObjectForNode(Node node, XMLMapping selfRecordMapping);
    
    public abstract boolean shouldPreserveDocument();
    
    public void initialize(XMLContext context) {
    }

}
