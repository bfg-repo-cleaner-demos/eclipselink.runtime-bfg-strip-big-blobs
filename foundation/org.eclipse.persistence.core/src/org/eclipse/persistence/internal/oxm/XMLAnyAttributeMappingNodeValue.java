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
package org.eclipse.persistence.internal.oxm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import org.eclipse.persistence.internal.oxm.record.MarshalContext;
import org.eclipse.persistence.internal.queries.DirectMapContainerPolicy;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.mappings.XMLAnyAttributeMapping;
import org.eclipse.persistence.oxm.record.MarshalRecord;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;

/**
 * INTERNAL:
 * <p><b>Purpose</b>: This is how the XML Any Attribute Mapping is handled when
 * used with the TreeObjectBuilder.</p>
 */

public class XMLAnyAttributeMappingNodeValue extends MappingNodeValue implements ContainerValue {
    private XMLAnyAttributeMapping xmlAnyAttributeMapping;

    public XMLAnyAttributeMappingNodeValue(XMLAnyAttributeMapping xmlAnyAttributeMapping) {
        super();
        this.xmlAnyAttributeMapping = xmlAnyAttributeMapping;
    }

    public boolean isOwningNode(XPathFragment xPathFragment) {
        return xPathFragment == null;
    }

    public boolean marshal(XPathFragment xPathFragment, MarshalRecord marshalRecord, Object object, AbstractSession session, NamespaceResolver namespaceResolver) {
        if (xmlAnyAttributeMapping.isReadOnly()) {
            return false;
        }
        Object collection = xmlAnyAttributeMapping.getAttributeValueFromObject(object);
        if (collection == null) {
            return false;
        }
        DirectMapContainerPolicy cp = getContainerPolicy();
        Object iter = cp.iteratorFor(collection);
        if (!cp.hasNext(iter)) {
            return false;
        }
        XPathFragment groupingElements = marshalRecord.openStartGroupingElements(namespaceResolver);
        List extraNamespaces = new ArrayList();
        NamespaceResolver nr = marshalRecord.getNamespaceResolver();
        while (cp.hasNext(iter)) {
            Map.Entry entry = (Map.Entry)cp.nextEntry(iter, session);
            Object key = entry.getKey();
            if (key instanceof QName) {
                QName name = (QName) key;
                String value = entry.getValue().toString();

                String qualifiedName = name.getLocalPart();
                if (nr != null) {
                    String prefix = nr.resolveNamespaceURI(name.getNamespaceURI());
                    if ((prefix != null) && prefix.length() > 0) {
                        qualifiedName = prefix + XMLConstants.COLON+ qualifiedName;
                    } else if (name.getNamespaceURI() != null && name.getNamespaceURI().length() > 0) {
                        String generatedPrefix = nr.generatePrefix();
                        qualifiedName = generatedPrefix + XMLConstants.COLON + qualifiedName;
                        nr.put(generatedPrefix, name.getNamespaceURI());
                        extraNamespaces.add(generatedPrefix);
                        marshalRecord.attribute(XMLConstants.XMLNS_URL, generatedPrefix, XMLConstants.XMLNS + XMLConstants.COLON + generatedPrefix, name.getNamespaceURI());
                    }
                }
                marshalRecord.attribute(name.getNamespaceURI(), name.getLocalPart(), qualifiedName, value);
            }
        }

        for (int i = 0; i < extraNamespaces.size(); i++) {
            marshalRecord.getNamespaceResolver().removeNamespace((String) extraNamespaces.get(i));
        }
        marshalRecord.closeStartGroupingElements(groupingElements);
        return true;
    }

    public void attribute(UnmarshalRecord unmarshalRecord, String namespaceURI, String localName, String value) {
        DirectMapContainerPolicy cp = (DirectMapContainerPolicy) xmlAnyAttributeMapping.getContainerPolicy();
        Object containerInstance = unmarshalRecord.getContainerInstance(this);
            
        boolean includeAttribute = true;
        if(!xmlAnyAttributeMapping.isNamespaceDeclarationIncluded() && XMLConstants.XMLNS_URL.equals(namespaceURI)){
            includeAttribute = false;               
        }else if(!xmlAnyAttributeMapping.isSchemaInstanceIncluded() && XMLConstants.SCHEMA_INSTANCE_URL.equals(namespaceURI)){
            includeAttribute = false;               
        }
                    
        if(includeAttribute){
            QName key = new QName(namespaceURI, localName);            
            cp.addInto(key, value, containerInstance, unmarshalRecord.getSession());
        }          
    }

    public Object getContainerInstance() {
        return xmlAnyAttributeMapping.getContainerPolicy().containerInstance();
    }

    public void setContainerInstance(Object object, Object container) {
        xmlAnyAttributeMapping.setAttributeValueInObject(object, container);
    }

    public DirectMapContainerPolicy getContainerPolicy() {
        return (DirectMapContainerPolicy) xmlAnyAttributeMapping.getContainerPolicy();
    }

    public boolean isContainerValue() {
        return true;
    }

    public boolean marshalSingleValue(XPathFragment xPathFragment, MarshalRecord marshalRecord, Object object, Object value, AbstractSession session, NamespaceResolver namespaceResolver, MarshalContext marshalContext) {
        return true;
    }

    public XMLAnyAttributeMapping getMapping() {
        return xmlAnyAttributeMapping;
    }

    public boolean getReuseContainer() {
        return getMapping().getReuseContainer();
    }

}
