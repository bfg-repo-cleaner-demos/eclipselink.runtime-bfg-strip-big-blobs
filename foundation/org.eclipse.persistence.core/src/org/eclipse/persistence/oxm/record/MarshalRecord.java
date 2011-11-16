/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
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
package org.eclipse.persistence.oxm.record;

import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.namespace.QName;

import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.oxm.XMLBinaryDataHelper;
import org.eclipse.persistence.internal.oxm.XPathPredicate;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.oxm.XPathNode;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.XMLMarshalListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * <p>A MarshalRecord encapsulates the marshal target.</p>
 *
 * <p>MarshalRecords are stateful and state changes are triggered by different
 * event notifications, therefore this class is not thread safe.</p>
 *
 * <p>XML document creation will differ depending on the subclass of MarshalRecord
 * used.  For example when NodeRecord is used a child element is created on the
 * openStartElement event, and when the ContentHandlerRecord is used a child
 * element is not created until the closeStartMethod event.</p>
 *
 * @see org.eclipse.persistence.oxm.XMLMarshaller
 */
public abstract class MarshalRecord extends XMLRecord {
    private ArrayList<XPathNode> groupingElements;
    private HashMap positionalNodes;

    public MarshalRecord() {
        super();
    }

    public HashMap getPositionalNodes() {
        if (positionalNodes == null) {
            positionalNodes = new HashMap();
        }
        return positionalNodes;
    }

    public String getLocalName() {
        throw XMLMarshalException.operationNotSupported("getLocalName");
    }

    public String getNamespaceURI() {
        throw XMLMarshalException.operationNotSupported("getNamespaceURI");
    }

    public void clear() {
        throw XMLMarshalException.operationNotSupported("clear");
    }

    public Document getDocument() {
        throw XMLMarshalException.operationNotSupported("getDocument");
    }

    public Element getDOM() {
        throw XMLMarshalException.operationNotSupported("getDOM");
    }

    /**
     * INTERNAL:
     * If an XPathNode does not have an associated NodeValue then add it to the
     * MarshalRecord as a grouping element.
     * @param xPathNode
     */
    public void addGroupingElement(XPathNode xPathNode) {
        if (null == groupingElements) {
            groupingElements = new ArrayList();
        }
        groupingElements.add(xPathNode);
    }

    /**
     *
     * INTERNAL:
     * @param xPathNode
     */
    public void removeGroupingElement(XPathNode xPathNode) {
        if (null != groupingElements) {
            groupingElements.remove(xPathNode);
        }
    }

    public String transformToXML() {
        return null;
    }

    /**
     * INTERNAL:
     * Add the field-value pair to the document.
     */
    public void add(DatabaseField key, Object value) {
        if (null == value) {
            return;
        }
        XMLField xmlField = convertToXMLField(key);
        XPathFragment lastFragment = xmlField.getLastXPathFragment();
        XMLConversionManager xcm = (XMLConversionManager) session.getDatasourcePlatform().getConversionManager();
        if (lastFragment.nameIsText()) {
            String stringValue = (String)xcm.convertObject(value, String.class);
            characters(stringValue);
        } else if (lastFragment.isAttribute()) {
            String stringValue = (String)xcm.convertObject(value, String.class);
            attribute(lastFragment, xmlField.getNamespaceResolver(), stringValue);
        } else {
            element(lastFragment);
        }
    }

    /**
     * INTERNAL:
     * Add the field-value pair to the document.
     */
    public Object put(DatabaseField key, Object value) {
        add(key, value);
        return null;
    }

    /**
     * INTERNAL:
     * Add the namespace declarations to the XML document.
     * @param namespaceResolver The NamespaceResolver contains the namespace
     * prefix and URI pairings that need to be declared.
     */
    public void namespaceDeclarations(NamespaceResolver namespaceResolver) {
        if (namespaceResolver == null) {
            return;
        }
        String namespaceURI = namespaceResolver.getDefaultNamespaceURI();
        if(null != namespaceURI) {
            attribute(XMLConstants.XMLNS_URL, XMLConstants.XMLNS, XMLConstants.XMLNS, namespaceURI);
        }
        if(namespaceResolver.hasPrefixesToNamespaces()) {
            for(Entry<String, String> entry: namespaceResolver.getPrefixesToNamespaces().entrySet()) {
                String namespacePrefix = entry.getKey();
                attribute(XMLConstants.XMLNS_URL, namespacePrefix, XMLConstants.XMLNS + XMLConstants.COLON + namespacePrefix, entry.getValue());
            }
        }
    }

    /**
     * Receive notification that a document is being started.
     * @param encoding The XML document will be encoded using this encoding.
     * @param version This specifies the version of XML.
     */
    public abstract void startDocument(String encoding, String version);

    /**
     * Recieve notification that a document is being ended.
     */
    public abstract void endDocument();

    /**
     * Receive notification that a namespace has been declared.
     * @param prefix The namespace prefix.
     * @param namespaceURI The namespace URI.
     */
    public void startPrefixMapping(String prefix, String namespaceURI) {
    }

    public void startPrefixMappings(NamespaceResolver namespaceResolver) {
        if (namespaceResolver != null && namespaceResolver.hasPrefixesToNamespaces()) {
            for(Entry<String, String> entry: namespaceResolver.getPrefixesToNamespaces().entrySet()) {
                startPrefixMapping(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Receive notification that the scope of this namespace declaration has
     * ended.
     * @param prefix The namespace prefix.
     */
    public void endPrefixMapping(String prefix) {
    }

    public void endPrefixMappings(NamespaceResolver namespaceResolver) {
        if (namespaceResolver != null && namespaceResolver.hasPrefixesToNamespaces()) {
            for(Entry<String, String> entry: namespaceResolver.getPrefixesToNamespaces().entrySet()) {
                endPrefixMapping(entry.getKey());
            }
        }
    }

    /**
     * Receive notification that an element is being started.
     * @param xPathFragment The XPathFragment contains the name and prefix
     * information about the XML element being ended.
     * @param namespaceResolver The NamespaceResolver can be used to resolve the
     * namespace URI for the namespace prefix held by the XPathFragment (if
     * required).
     */
    public void openStartElement(XPathFragment xPathFragment, NamespaceResolver namespaceResolver) {
        this.addPositionalNodes(xPathFragment, namespaceResolver);
    }

    /**
     * Receive notification of an element.
     * @param frag The XPathFragment of the element
     */
    public abstract void element(XPathFragment frag);

    /**
     * Receive notification of an attribute.
     * @param xPathFragment The XPathFragment contains the name and prefix
     * information about the XML element being ended.
     * @param namespaceResolver The NamespaceResolver can be used to resolve the
     * namespace URI for the namespace prefix held by the XPathFragment (if
     * required).
     * @param value This is the complete value for the attribute.
     */
    public abstract void attribute(XPathFragment xPathFragment, NamespaceResolver namespaceResolver, String value);

    /**
     * Receive notification of an attribute.
     * @param namespaceURI The namespace URI, if the attribute is not namespace
     * qualified the value of this parameter wil be null.
     * @param localName The local name of the attribute.
     * @param qName The qualified name of the attribute.
     * @param value This is the complete value for the attribute.
     */
    public abstract void attribute(String namespaceURI, String localName, String qName, String value);

    /**
     * Receive notification that all of the attribute events have occurred for
     * the most recent element that has been started.
     */
    public abstract void closeStartElement();

    /**
     * Receive notification that an element is being ended.
     * @param xPathFragment The XPathFragment contains the name and prefix
     * information about the XML element being ended.
     * @param namespaceResolver The NamespaceResolver can be used to resolve the
     * namespace URI for the namespace prefix held by the XPathFragment (if
     * required).
     */
    public abstract void endElement(XPathFragment xPathFragment, NamespaceResolver namespaceResolver);

    /**
     * Receive notification of character data.
     * @param value This is the entire value of the text node.
     */
    public abstract void characters(String value);

    /**
     * Convert the value if necessary and write out the converted value.
     * Returns true if a value was written, and false if not
     * @since EclipseLink 2.3 
     */    
    public void characters(QName schemaType, Object value, String mimeType, boolean isCDATA){  
        if(mimeType != null) {
            value = XMLBinaryDataHelper.getXMLBinaryDataHelper().getBytesForBinaryValue(//
                    value, marshaller, mimeType).getData();
        }
        if(schemaType != null && XMLConstants.QNAME_QNAME.equals(schemaType)){
            String convertedValue = getStringForQName((QName)value);
            characters(convertedValue);
        }else{
            String convertedValue = ((String) ((XMLConversionManager) session.getDatasourcePlatform().getConversionManager()).convertObject(value, ClassConstants.STRING, schemaType));
            if(isCDATA){
                cdata(convertedValue);        	    
            }else{
                characters(convertedValue);
            }
        }
    }
    
    protected String getStringForQName(QName qName){
        if(null == qName) {
            return null;
        }
        String namespaceURI = qName.getNamespaceURI();
        if(null == namespaceURI || 0 == namespaceURI.length()) {
            if(getNamespaceResolver() != null && getNamespaceResolver().getDefaultNamespaceURI() != null) {
                //need to add a default namespace declaration.
                attribute(XMLConstants.XMLNS_URL, XMLConstants.XMLNS, XMLConstants.XMLNS, namespaceURI);
            }
            return qName.getLocalPart();
        } else {
            NamespaceResolver namespaceResolver = getNamespaceResolver();
            if(namespaceResolver == null){
                throw XMLMarshalException.namespaceResolverNotSpecified(namespaceURI);
            }
            if(namespaceURI.equals(namespaceResolver.getDefaultNamespaceURI())) {
                return qName.getLocalPart();
            }
            String prefix = namespaceResolver.resolveNamespaceURI(namespaceURI);
            if(null == prefix) {
                prefix = namespaceResolver.generatePrefix();
                attribute(XMLConstants.XMLNS_URL, prefix, XMLConstants.XMLNS + XMLConstants.COLON + prefix, namespaceURI);
            }
            return prefix + XMLConstants.COLON + qName.getLocalPart();
        }
    }
    /**
     * Receive notification of character data to be wrapped in a CDATA node.
     * @param value This is the value of the text to be wrapped
     */
    public abstract void cdata(String value);

    /**
     * Receive notification of a node.
     * @param node The Node to be added to the document
     * @param namespaceResolver The NamespaceResolver can be used to resolve the
     * namespace URI/prefix of the node
     */
    public abstract void node(Node node, NamespaceResolver resolver);

    /**
     * INTERNAL:
     * Trigger that the grouping elements should be written.  This is normally
     * done when something like a mapping has a non-null value that is
     * marshalled.
     * @param namespaceResolver The NamespaceResolver can be used to resolve the
     * namespace URI for the namespace prefix held by the XPathFragment (if
     * required).
     */
    public XPathFragment openStartGroupingElements(NamespaceResolver namespaceResolver) {
        if (null == groupingElements) {
            return null;
        }
        XPathFragment xPathFragment = null;
        for (int x = 0, groupingElementsSize = groupingElements.size(); x < groupingElementsSize; x++) {
            XPathNode xPathNode = groupingElements.get(x);
            xPathFragment = xPathNode.getXPathFragment();
            openStartElement(xPathFragment, namespaceResolver);

            predicateAttribute(xPathFragment, namespaceResolver);

            if (x != (groupingElementsSize - 1)) {
                closeStartElement();
            }
        }
        groupingElements = null;
        return xPathFragment;
    }

    public void closeStartGroupingElements(XPathFragment groupingFragment) {
        if (null != groupingFragment) {
            this.closeStartElement();
        }
    }

    protected void addPositionalNodes(XPathFragment xPathFragment, NamespaceResolver namespaceResolver) {
        if (xPathFragment.containsIndex()) {
            String shortName = xPathFragment.getShortName();
            Integer index = (Integer)getPositionalNodes().get(shortName);
            int start;
            if (null == index) {
                start = 1;
            } else {
                start = index.intValue();
            }
            for (int x = start; x < xPathFragment.getIndexValue(); x++) {
                element(xPathFragment);
            }
            getPositionalNodes().put(shortName, xPathFragment.getIndexValue() + 1);
        }
    }

    public void beforeContainmentMarshal(Object child) {
        if(null != marshaller) {
            XMLMarshalListener marshalListener = marshaller.getMarshalListener();
            if(null != marshalListener) {
                marshalListener.beforeMarshal(child);
            }
        }
        setOwningObject(child);
    }

    public void afterContainmentMarshal(Object parent, Object child) {
        if(null != marshaller) {
            XMLMarshalListener marshalListener = marshaller.getMarshalListener();
            if(null != marshalListener) {
                marshalListener.afterMarshal(child);
            }
        }
        setOwningObject(parent);
    }
    
    /**
     * INTERNAL:
     * Returns the list of grouping elements currently stored on the MarshalRecord
     */
    public ArrayList<XPathNode> getGroupingElements() {
        return this.groupingElements;
    }
    
    /**
     * INTERNAL:
     * Sets the list of grouping elements to be marshalled on this record. 
     */
    public void setGroupingElement(ArrayList<XPathNode> elements) {
        this.groupingElements = elements;
    }

    /**
     * Marshal the attribute for the predicate if one was specified. 
     */
    public void predicateAttribute(XPathFragment xPathFragment, NamespaceResolver namespaceResolver) {
        if(null != xPathFragment) {
            XPathPredicate predicate = xPathFragment.getPredicate();
            if(null != predicate) {
                XPathFragment predicateXPathFragment = predicate.getXPathFragment();
                if(predicateXPathFragment.isAttribute()) {
                    attribute(predicateXPathFragment, namespaceResolver, predicate.getValue());
                }
            }
        }
    }

}