/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
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
package org.eclipse.persistence.jaxb.compiler;

import org.eclipse.persistence.jaxb.xmlmodel.XmlAccessOrder;
import org.eclipse.persistence.jaxb.xmlmodel.XmlAccessType;
import org.eclipse.persistence.oxm.NamespaceResolver;

/**
 *  INTERNAL:
 *  <p><b>Purpose:</b>To store some information about a schema's target namespace and some additional
 *  information gathered from XmlSchema annotation at the package (namespace) level
 *  <p><b>Responsibilities:</b><ul>
 *  <li>Store target namespace and namespace prefix information for a specific schema</li>
 *  <li>Store some additional Schema information (such as element/attribute form and XmlAccessType)</li>
 *  </ul>
 *  
 *  @see org.eclipse.persistence.jaxb.compiler.AnnotationsProcessor
 *  @author mmacivor
 *  @since Oracle TopLink 11.1.1.0.0
 */

public class NamespaceInfo {
    private String namespace;
    private boolean attributeFormQualified = false;
    private boolean elementFormQualified = false;
    private NamespaceResolver namespaceResolver;
    private XmlAccessType accessType = XmlAccessType.PUBLIC_MEMBER;
    private XmlAccessOrder accessOrder = XmlAccessOrder.UNDEFINED;
    private String location;
    private NamespaceResolver namespaceResolverForDescriptor;
    
    public String getNamespace() {
        return namespace;
    }
    
    public void setNamespace(String ns) {
        this.namespace = ns;
    }
    
    public boolean isAttributeFormQualified() {
        return attributeFormQualified;
    }
    
    public void setAttributeFormQualified(boolean b) {
        attributeFormQualified = b;
    }

    public boolean isElementFormQualified() {
        return elementFormQualified;
    }
    
    public void setElementFormQualified(boolean b) {
        elementFormQualified = b;
    }
    
    public NamespaceResolver getNamespaceResolver() {
        return namespaceResolver;
    }
    
    public void setNamespaceResolver(NamespaceResolver resolver) {
        namespaceResolver = resolver;
    }
    
    public XmlAccessType getAccessType() {
        return accessType;
    }
    
    public void setAccessType(XmlAccessType type) {
        this.accessType = type;
    }

    public XmlAccessOrder getAccessOrder() {
        return accessOrder;
    }
    
    public void setAccessOrder(XmlAccessOrder order) {
        this.accessOrder = order;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    
    public NamespaceResolver getNamespaceResolverForDescriptor() {
        if(this.namespaceResolverForDescriptor == null) {
            this.namespaceResolverForDescriptor = new NamespaceResolver();
            for(String next:this.namespaceResolver.getPrefixesToNamespaces().keySet()) {
                this.namespaceResolverForDescriptor.put(next, this.namespaceResolver.resolveNamespacePrefix(next));
            }
        }
        return this.namespaceResolverForDescriptor;
    }
}
