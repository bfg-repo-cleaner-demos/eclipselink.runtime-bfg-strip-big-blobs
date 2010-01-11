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
package org.eclipse.persistence.internal.oxm;

import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.mappings.XMLChoiceObjectMapping;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.oxm.mappings.XMLCompositeObjectMapping;
import org.eclipse.persistence.oxm.mappings.XMLMapping;
import org.eclipse.persistence.oxm.record.MarshalRecord;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.oxm.XMLRoot;

import org.xml.sax.Attributes;

/**
 * INTERNAL:
 * <p><b>Purpose</b>: This is how the XML Choice Collection Mapping is 
 * handled when used with the TreeObjectBuilder.</p> 
 * @author mmacivor
 */
public class XMLChoiceObjectMappingNodeValue extends NodeValue implements NullCapableValue {
    private NodeValue choiceElementNodeValue;
    private XMLChoiceObjectMapping xmlChoiceMapping;
    //The first node value of the choice will be registered as a null capable value. If any
    //of the choice elements get hit, this needs to be removed as a null value.
    private XMLChoiceObjectMappingNodeValue nullCapableNodeValue;
    private XMLField xmlField;
    
    public XMLChoiceObjectMappingNodeValue(XMLChoiceObjectMapping mapping, XMLField xmlField) {
        this.xmlChoiceMapping = mapping;
        this.xmlField = xmlField;
        initializeNodeValue();
    }
    
    public boolean isOwningNode(XPathFragment xPathFragment) {
        return choiceElementNodeValue.isOwningNode(xPathFragment);
    }
    
    public void initializeNodeValue() {
        XMLMapping xmlMapping = xmlChoiceMapping.getChoiceElementMappings().get(xmlField);
        if(xmlMapping instanceof XMLDirectMapping) {
            choiceElementNodeValue = new XMLDirectMappingNodeValue((XMLDirectMapping)xmlMapping);
        } else {
            choiceElementNodeValue = new XMLCompositeObjectMappingNodeValue((XMLCompositeObjectMapping)xmlMapping);
        }
    }
    
    public void setNullCapableNodeValue(XMLChoiceObjectMappingNodeValue nodeValue) {
        this.nullCapableNodeValue = nodeValue;
    }
    
    public void setNullValue(Object object, Session session) {
        xmlChoiceMapping.setAttributeValueInObject(object, null);
    }

    public boolean marshal(XPathFragment xPathFragment, MarshalRecord marshalRecord, Object object, AbstractSession session, NamespaceResolver namespaceResolver) {
        Object value = xmlChoiceMapping.getAttributeValueFromObject(object);
        if(value instanceof XMLRoot) {
        	XMLRoot root = (XMLRoot)value;
        	XPathFragment fragment = this.xmlField.getXPathFragment();
        	while(fragment != null && !fragment.nameIsText) {
        		if(fragment.getNextFragment() == null || fragment.getHasText()) {
        			if(fragment.getLocalName().equals(root.getLocalName())) {
    					String fragUri = fragment.getNamespaceURI();
    					String namespaceUri = root.getNamespaceURI();
    					if((namespaceUri == null && fragUri == null) || (namespaceUri != null && fragUri != null && namespaceUri.equals(fragUri))) {
    						return this.choiceElementNodeValue.marshal(xPathFragment, marshalRecord, object, session, namespaceResolver);
    					}
        			}
        		}
        		fragment = fragment.getNextFragment();
        	}
        } else {
        	if(value != null && xmlChoiceMapping.getClassToFieldMappings().get(value.getClass()) == this.xmlField) {
        		return this.choiceElementNodeValue.marshal(xPathFragment, marshalRecord, object, session, namespaceResolver);
        	}
        }
        return false;
    }
    
    public void endElement(XPathFragment xPathFragment, UnmarshalRecord unmarshalRecord) {
        unmarshalRecord.removeNullCapableValue(this.nullCapableNodeValue);
        this.choiceElementNodeValue.endElement(xPathFragment, unmarshalRecord);
    }
    
    public boolean startElement(XPathFragment xPathFragment, UnmarshalRecord unmarshalRecord, Attributes atts) {
        unmarshalRecord.removeNullCapableValue(this.nullCapableNodeValue);
        return this.choiceElementNodeValue.startElement(xPathFragment, unmarshalRecord, atts);
    }
    
    public void setXPathNode(XPathNode xPathNode) {
        super.setXPathNode(xPathNode);
        this.choiceElementNodeValue.setXPathNode(xPathNode);
    }    
    
    
}
