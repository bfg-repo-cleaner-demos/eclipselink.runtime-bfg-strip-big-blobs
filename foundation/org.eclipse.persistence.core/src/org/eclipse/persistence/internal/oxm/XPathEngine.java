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
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.xml.namespace.QName;
import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.oxm.documentpreservation.NoDocumentPreservationPolicy;
import org.eclipse.persistence.internal.oxm.documentpreservation.XMLBinderPolicy;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.XMLUnionField;
import org.eclipse.persistence.oxm.documentpreservation.DocumentPreservationPolicy;
import org.eclipse.persistence.oxm.record.XMLEntry;
import org.eclipse.persistence.oxm.record.XMLRecord;
import org.eclipse.persistence.platform.xml.XMLNodeList;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * INTERNAL:
 * <p><b>Purpose</b>: Utility class for creating and removing XML nodes using
 * XPath expressions.</p>
 * @author    Rick Barkhouse - rick.barkhouse@oracle.com
 * @since    OracleAS TopLink 10<i>g</i> (10.0.3), 03/11/2003 10:21:42
 */
public class XPathEngine {
    private static XPathEngine instance = null;
    private UnmarshalXPathEngine unmarshalXPathEngine;
    private DocumentPreservationPolicy noDocPresPolicy = new NoDocumentPreservationPolicy();//handles xpath engine calls without a policy
    private DocumentPreservationPolicy xmlBinderPolicy = new XMLBinderPolicy();//used for adding new elements to a collection.

    /**
    * Return the <code>XPathEngine</code> singleton.
    */
    public static XPathEngine getInstance() {
        if (instance == null) {
            instance = new XPathEngine();
        }
        return instance;
    }

    private XPathEngine() {
        super();
        unmarshalXPathEngine = new UnmarshalXPathEngine();
    }

    /**
    * Create the node path specified by <code>xpathString</code> under <code>element</code>.
    * This method also supports creating attributes and indexed elements using the appropriate
    * XPath syntax ('<code>@</code>' and '<code>[ ]</code>' respectively).
    *
    * @param xmlField XMLField containing xpath expression representing the node path to create
    * @param element Root element under which to create path
    *
    * @return The last <code>XMLNode</code> in the path
    *
    * @exception org.eclipse.persistence.oxm.exceptions.XMLMarshalException Thrown if passed an invalid XPath string
    */
    public Node create(XMLField xmlField, Node element, AbstractSession session) throws XMLMarshalException {
        return create(xmlField, element, this, session);
    }

    public Node create(XMLField xmlField, Node element, Object value, AbstractSession session) {
        return create(xmlField, element, value, null, noDocPresPolicy, session);
    }

    /**
    * Create the node path specified by <code>xpathString</code> under <code>element</code>
    * and initialize the leaf node with <code>value</code>.
    * This method also supports creating attributes and integer-indexed elements using the
    * appropriate XPath syntax ('<code>@</code>' and '<code>[ ]</code>' respectively).
    *
    * @param xmlField XMLField containing xpath expression representing the node path to create
    * @param element Root element under which to create path
    * @param value Initial value for the leaf node (should not be a list)
    *
    * @return The last <code>XMLNode</code> in the path
    *
    * @exception org.eclipse.persistence.oxm.exceptions.XMLMarshalException Thrown if passed an invalid XPath string
    */
    public Node create(XMLField xmlField, Node element, Object value, XMLField lastUpdated, DocumentPreservationPolicy docPresPolicy, AbstractSession session) throws XMLMarshalException {
        if (null == value) {
            return null;
        }
        if (docPresPolicy == null) {
            //EIS case and others
            docPresPolicy = this.noDocPresPolicy;
        }
        XPathFragment fragment = xmlField.getXPathFragment();
        if (fragment.getNextFragment() == null) {
            if (fragment.nameIsText()) {
                Object textValue = getValueToWrite(value, xmlField, session);
                if (textValue instanceof String) {
                    if (xmlField.isTypedTextField()) {
                        XMLNodeList createdElements = new XMLNodeList();
                        createdElements.add(element);
                        addTypeAttributes(createdElements, xmlField, value, resolveNamespacePrefixForURI(XMLConstants.SCHEMA_INSTANCE_URL, getNamespaceResolverForField(xmlField)));
                    }
                    return addText(xmlField, element, (String)textValue);
                }
                return null;
            }
        }
        NodeList created = createCollection(xmlField, element, value, lastUpdated, docPresPolicy, session);

        if ((created == null) || (created.getLength() == 0)) {
            return null;
        }

        return created.item(0);
    }

    public void create(List<XMLField> xmlFields, Node contextNode, List<XMLEntry> values, XMLField lastUpdatedField, DocumentPreservationPolicy docPresPolicy, AbstractSession session) {
        List itemsToWrite = new ArrayList();
        for(int i = 0, size = values.size(); i < size; i++) {
            XMLEntry nextEntry = values.get(i);
            itemsToWrite.add(nextEntry.getValue());
            if(i == (values.size() -1) || values.get(i+1).getXMLField() != nextEntry.getXMLField()) {
                create(nextEntry.getXMLField(), contextNode, itemsToWrite, lastUpdatedField, docPresPolicy, session);
                itemsToWrite = new ArrayList();
                lastUpdatedField = nextEntry.getXMLField();
            }
        }
    }



    /**
    * Create the node path specified by <code>xpathString</code> under <code>element</code>
    * and initialize the leaf node with <code>value</code>.
    * This method also supports creating attributes and integer-indexed elements using the
    * appropriate XPath syntax ('<code>@</code>' and '<code>[ ]</code>' respectively).
    *
    * @param xmlField XMLField containing xpath expression representing the node path to create
    * @param element Root element under which to create path
    * @param value Initial value for the leaf node (this can be a value or a collection of values)
    *
    * @return The last <code>XMLNode</code> in the path
    *
    * @exception org.eclipse.persistence.oxm.exceptions.XMLMarshalException Thrown if passed an invalid XPath string
    */
    private NodeList createCollection(XMLField xmlField, Node element, Object value, XMLField lastUpdated, DocumentPreservationPolicy docPresPolicy, AbstractSession session) throws XMLMarshalException {
        XMLNodeList createdElements = new XMLNodeList();

        //CR:### If the value is null, then the node(s) must not be created.
        if ((value == null) || (value instanceof Collection && (((Collection)value).size() == 0))) {
            return createdElements;
        }
        Node nextElement = element;
        Element sibling = null;
        XPathFragment siblingFragment = null;
        if(lastUpdated != null) {
            siblingFragment = lastUpdated.getXPathFragment();
        }
        if ((lastUpdated != null) && !siblingFragment.isAttribute() && !siblingFragment.nameIsText()) {
            //find the sibling element.
            NodeList nodes = unmarshalXPathEngine.selectElementNodes(element, siblingFragment, getNamespaceResolverForField(lastUpdated));
            if (nodes.getLength() > 0) {
                sibling = (Element)nodes.item(nodes.getLength() - 1);
            }
        }
        NodeList elements;
        XPathFragment next = xmlField.getXPathFragment();
        while (next != null) {
            if (next.isAttribute()) {
                addAttribute(next, xmlField, nextElement, value, session);
            } else if (next.containsIndex()) {
                // If we are creating multiple nodes from this XPath, assume the value is for the last node.
                boolean hasMore = !(next.getHasText() || (next.getNextFragment() == null));

                if (hasMore) {
                    nextElement = addIndexedElement(next, xmlField, nextElement, this, !hasMore, session);
                } else {
                    Object valueToWrite = getValueToWrite(value, xmlField, session);
                    nextElement = addIndexedElement(next, xmlField, nextElement, valueToWrite, !hasMore, session);
                    createdElements.add(nextElement);
                }
            } else {
                boolean hasMore = !(next.getHasText() || (next.getNextFragment() == null));

                if (hasMore) {
                    elements = addElements(next, xmlField, nextElement, this, !hasMore, sibling, docPresPolicy, session);
                } else {
                    XPathFragment nextFragment = next.getNextFragment();
                    if ((nextFragment != null) && nextFragment.isAttribute() && !(value instanceof List)) {
                        elements = addElements(next, xmlField, nextElement, this, hasMore, sibling, docPresPolicy, session);
                    } else {
                        Object valueToWrite = getValueToWrite(value, xmlField, session);
                        elements = addElements(next, xmlField, nextElement, valueToWrite, !hasMore, sibling, docPresPolicy, session);
                        createdElements.addAll(elements);
                    }
                }

                nextElement = elements.item(elements.getLength() - 1);
            }
            if(siblingFragment != null && sibling != null && siblingFragment.equals(next)) {
                //if the sibling shares a grouping element, update the sibling
                siblingFragment = siblingFragment.getNextFragment();
                if ((siblingFragment != null) && !siblingFragment.isAttribute() && !siblingFragment.nameIsText()) {
                    //find the sibling element.
                    NodeList nodes = unmarshalXPathEngine.selectElementNodes(nextElement, siblingFragment, getNamespaceResolverForField(lastUpdated));
                    if (nodes.getLength() > 0) {
                        sibling = (Element)nodes.item(nodes.getLength() - 1);
                    } else {
                        sibling = null;
                    }
                } else {
                    sibling = null;
                }
            } else {
                sibling = null;
            }

            next = next.getNextFragment();
            if ((next != null) && next.nameIsText()) {
                next = null;
            }
        }

        if (xmlField.isTypedTextField()) {
            addTypeAttributes(createdElements, xmlField, value, resolveNamespacePrefixForURI(XMLConstants.SCHEMA_INSTANCE_URL, getNamespaceResolverForField(xmlField)));
        }

        return createdElements;
    }

    private Object getNonNodeValueToWrite(Object value, XMLField xmlField, AbstractSession session) {
        if (this == value) {
            return this;
        }

        QName schemaType = null;
        if(xmlField.getLeafElementType() != null){
            schemaType = xmlField.getLeafElementType();
        }else if (xmlField.isUnionField()) {
            return getValueToWriteForUnion((XMLUnionField)xmlField, value, session);
        }else if (xmlField.isTypedTextField()) {
            schemaType = xmlField.getXMLType(value.getClass());
        }else if (xmlField.getSchemaType() != null) {
            schemaType = xmlField.getSchemaType();
        }

        if (value instanceof List) {
            if (xmlField.usesSingleNode()) {
                String returnString = XMLConstants.EMPTY_STRING;
                for (int i = 0; i < ((List)value).size(); i++) {
                    Object nextItem = ((List)value).get(i);

                    String nextConvertedItem = null;
                    if(schemaType != null && schemaType.equals(XMLConstants.QNAME_QNAME)){
                        nextConvertedItem = getStringForQName((QName)nextItem, getNamespaceResolverForField(xmlField));
                    }else{
                    	nextConvertedItem = (String) ((XMLConversionManager)session.getDatasourcePlatform().getConversionManager()).convertObject(nextItem, ClassConstants.STRING, schemaType);
                    }
                    returnString += nextConvertedItem;
                    if (i < (((List)value).size() - 1)) {
                        returnString += " ";
                    }
                }
                return returnString;
            } else {
                ArrayList items = new ArrayList(((List)value).size());
                for (int index = 0; index < ((List)value).size(); index++) {
                    Object nextItem = ((List)value).get(index);
                    if (nextItem instanceof Node || nextItem == XMLRecord.NIL) {
                        items.add(nextItem);
                    } else {
                        if(schemaType != null && schemaType.equals(XMLConstants.QNAME_QNAME)){
                            String nextConvertedItem = getStringForQName((QName)nextItem, getNamespaceResolverForField(xmlField));
                            items.add(nextConvertedItem);
                        }else{
                            String nextConvertedItem = (String) ((XMLConversionManager)session.getDatasourcePlatform().getConversionManager()).convertObject(nextItem, ClassConstants.STRING, schemaType);
                            items.add(nextConvertedItem);
                        }
                    }
                }
                return items;
            }
        } else {
            if(schemaType != null && schemaType.equals(XMLConstants.QNAME_QNAME)){
                return getStringForQName((QName)value, getNamespaceResolverForField(xmlField));
            }
            return ((XMLConversionManager)session.getDatasourcePlatform().getConversionManager()).convertObject(value, ClassConstants.STRING, schemaType);
        }
    }

    private Object getValueToWrite(Object value, XMLField xmlField, AbstractSession session) {
        if (value instanceof Node) {
            return value;
        }

        return getNonNodeValueToWrite(value, xmlField, session);
    }

    private String getSingleValueToWriteForUnion(XMLUnionField xmlField, Object value, AbstractSession session) {
        ArrayList schemaTypes = xmlField.getSchemaTypes();
        QName schemaType = null;
        for (int i = 0; i < schemaTypes.size(); i++) {
            QName nextQName = (QName)(xmlField).getSchemaTypes().get(i);
            try {
                if (nextQName != null) {
                    Class javaClass = xmlField.getJavaClass(nextQName);
                    value = ((XMLConversionManager)session.getDatasourcePlatform().getConversionManager()).convertObject(value, javaClass, nextQName);
                    schemaType = nextQName;
                    break;
                }
            } catch (ConversionException ce) {
                if (i == (schemaTypes.size() - 1)) {
                    schemaType = nextQName;
                }
            }
        }
        return (String) ((XMLConversionManager)session.getDatasourcePlatform().getConversionManager()).convertObject(value, ClassConstants.STRING, schemaType);
    }

    private Object getValueToWriteForUnion(XMLUnionField xmlField, Object value, AbstractSession session) {
        if (value instanceof List) {
            if (xmlField.usesSingleNode()) {
                String returnString = XMLConstants.EMPTY_STRING;
                Object next = null;
                for (int i = 0; i < ((List)value).size(); i++) {
                    next = ((List)value).get(i);
                    returnString += getSingleValueToWriteForUnion(xmlField, next, session);
                    if (i < (((List)value).size() - 1)) {
                        returnString += " ";
                    }
                }
                return returnString;
            } else {
                ArrayList items = new ArrayList(((List)value).size());
                Object next = null;
                for (int i = 0; i < ((List)value).size(); i++) {
                    next = ((List)value).get(i);
                    items.add(getSingleValueToWriteForUnion(xmlField, next, session));
                }
                return items;
            }
        } else {
            return getSingleValueToWriteForUnion(xmlField, value, session);
        }
    }

    /**
    * Add a new indexed <code>element</code> to the <code>parent</code> element.
    * Will overwrite if an element already exists at that position.  Currently only supports
    * integer indices.
    *
    * @param xpathString element and index to create (in the form 'element[index]')
    * @param namespaceResolover namespaceResolover of the element being created
    * @param parent Parent element
    * @param schemaType schemaType for the new node
    * @param value Value for the new node
    * @param forceCreate If true, create a new element even if one with the same name currently exists
    *
    * @return The <code>XMLElement</code> that was created/found
    *
    * @exception org.eclipse.persistence.oxm.exceptions.XMLMarshalException Thrown if passed an invalid XPath string
    */
    private Node addIndexedElement(XPathFragment fragment, XMLField xmlField, Node parent, Object value, boolean forceCreate, AbstractSession session) throws XMLMarshalException {
        String element = fragment.getShortName();

        int index = fragment.getIndexValue();
        if (index < 0) {
            throw XMLMarshalException.invalidXPathIndexString(fragment.getXPath());
        }

        Node existingElement;
        NamespaceResolver namespaceResolver = getNamespaceResolverForField(xmlField);
        for (int i = 1; i < index; i++) {
            XMLField field = new XMLField(element + "[" + i + "]");
            field.setNamespaceResolver(namespaceResolver);
            existingElement = (Node)unmarshalXPathEngine.selectSingleNode(parent, field, namespaceResolver);
            if (existingElement == null) {
                addElement(new XPathFragment(element), xmlField, parent, this, true, session);
            }
        }
        XMLField field = new XMLField(fragment.getXPath());
        field.setNamespaceResolver(namespaceResolver);
        existingElement = (Node)unmarshalXPathEngine.selectSingleNode(parent, field, namespaceResolver);
        if (existingElement == null) {
            return addElement(new XPathFragment(element), field, parent, value, true, session);
        }

        if ((existingElement != null) && !forceCreate) {
            return existingElement;
        }

        String namespace = resolveNamespacePrefix(fragment, namespaceResolver);
        Element elementToReturn = parent.getOwnerDocument().createElementNS(namespace, element);

        if ((value != this) && (value != null)) {
            if (value instanceof String) {
                addText(xmlField, elementToReturn, (String)value);
            }
        }
        parent.replaceChild(elementToReturn, existingElement);
        return elementToReturn;
    }

    /**
    * Add a new <code>element</code> to the <code>parent</code> element.  If an element with
    * this name already exists, return it (unless <code>forceCreate</code> is <code>true</code>).
    *
    * @param element Name of element to create
    * @param parent Parent element
    * @param value Value for the new node
    * @param forceCreate If true, create a new element even if one with the same name currently exists
    *
    * @return The <code>XMLElement</code> that was created/found
    */
    private Node addElement(XPathFragment fragment, XMLField xmlField, Node parent, Object value, boolean forceCreate, AbstractSession session) {
        return addElement(fragment, xmlField, parent, null, value, forceCreate, session);
    }

    private Node addElement(XPathFragment fragment, XMLField xmlField, Node parent, QName schemaType, Object value, boolean forceCreate, AbstractSession session) {
        NodeList list = addElements(fragment, xmlField, parent, value, forceCreate, null, noDocPresPolicy, session);
        if (list.getLength() > 0) {
            return list.item(0);
        }
        return null;
    }

    /**
    * Add a new <code>element</code> to the <code>parent</code> element.  If an element with
    * this name already exists, return it (unless <code>forceCreate</code> is <code>true</code>).
    *
    * @param fragment Name of element to create
    * @param namespace namespace of element to create
    * @param parent Parent element
    * @param schemaType schemaType of element to create
    * @param value Value for the new node
    * @param forceCreate If true, create a new element even if one with the same name currently exists

    * @return The <code>NodeList</code> that was created/found
    */
    private NodeList addElements(XPathFragment fragment, XMLField xmlField, Node parent, Object value, boolean forceCreate, Element sibling, DocumentPreservationPolicy docPresPolicy, AbstractSession session) {
        if (!forceCreate) {
            NodeList nodes = unmarshalXPathEngine.selectElementNodes(parent, fragment, getNamespaceResolverForField(xmlField));
            if (nodes.getLength() > 0) {
                return nodes;
            }
        }

        XMLNodeList elementsToReturn = new XMLNodeList();

        if (value == this) {
            String namespace = resolveNamespacePrefix(fragment, getNamespaceResolverForField(xmlField));
            Element newElement = parent.getOwnerDocument().createElementNS(namespace, fragment.getShortName());
            elementsToReturn.add(newElement);
            docPresPolicy.getNodeOrderingPolicy().appendNode(parent, newElement, sibling);

        } else if (value == null) {
            elementsToReturn.add(parent);
        } else {
            // Value may be a direct value, node, or list of values.
            if (value instanceof List) {
                List values = (List)value;
                for (int index = 0; index < values.size(); index++) {
                    Element newElement = null;

                    if (values.get(index) != XMLRecord.NIL) {
                        newElement = (Element) createElement(parent, fragment, xmlField, values.get(index), session);
                    } else {
                        newElement = (Element) createElement(parent, fragment, xmlField, XMLConstants.EMPTY_STRING, session);
                        newElement.setAttributeNS(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_INSTANCE_PREFIX + ":" + XMLConstants.SCHEMA_NIL_ATTRIBUTE, XMLConstants.BOOLEAN_STRING_TRUE);
                    }

                    docPresPolicy.getNodeOrderingPolicy().appendNode(parent, newElement, sibling);
                    elementsToReturn.add(newElement);
                    sibling = newElement;
                }
            } else {
                Element newElement = (Element)createElement(parent, fragment, xmlField, value, session);
                docPresPolicy.getNodeOrderingPolicy().appendNode(parent, newElement, sibling);
                elementsToReturn.add(newElement);
            }
        }
        return elementsToReturn;
    }

    /**
    * Creates a new Element and appends a value to an element.
    *
    * @param parent Element which will own the newly created element
    * @param elementName tag name for the new element
    * @param value Object to add
    */
    private Node createElement(Node parent, XPathFragment fragment, XMLField xmlField, Object value, AbstractSession session) {
        if (value == null) {
            return parent;
        }
        if (value instanceof Node) {
            return createElement(parent, fragment, getNamespaceResolverForField(xmlField), (Node)value);
        }
        Element element = null;
        if (parent.getOwnerDocument() == null) {
            element = ((Document)parent).getDocumentElement();
        } else {
            String namespace = resolveNamespacePrefix(fragment, getNamespaceResolverForField(xmlField));
            element = parent.getOwnerDocument().createElementNS(namespace, fragment.getXPath());
            if (fragment.isGeneratedPrefix()) {
                element.setAttributeNS(XMLConstants.XMLNS_URL, XMLConstants.XMLNS + XMLConstants.COLON + fragment.getPrefix(), fragment.getNamespaceURI());
            }
        }

        XPathFragment nextFragment = fragment.getNextFragment();
        if ((nextFragment != null) && nextFragment.isAttribute()) {
            addAttribute(nextFragment, xmlField, element, value, session);
        } else if (value instanceof String && ((String)value).length() > 0) {
            addText(xmlField, element, (String)value);
        }
        return element;
    }

    public Element createUnownedElement(Node parent, XMLField xmlField) {
        XPathFragment lastFragment = xmlField.getXPathFragment();
        while (lastFragment.getNextFragment() != null) {
            lastFragment = lastFragment.getNextFragment();
        }
        String nodeName = lastFragment.getShortName();
        String namespace = resolveNamespacePrefix(lastFragment, getNamespaceResolverForField(xmlField));

        Element elem = parent.getOwnerDocument().createElementNS(namespace, nodeName);
        if (lastFragment.isGeneratedPrefix()) {
            elem.setAttributeNS(XMLConstants.XMLNS_URL, XMLConstants.XMLNS + XMLConstants.COLON + lastFragment.getPrefix(), lastFragment.getNamespaceURI());
        }

        return elem;
    }

    /**
    * Adds a type attribute on an element, the value of the attribute is determined
    * by performing a lookup in the SimpleTypeTranslator to find the Schema type
    * for the value.
    *
    * @param elements NodeList which will have a type attribute added to them
    * @param simpleTypeTranslator SimpleTypeTranslator to perform lookup in
    * @param value Object to base the lookup on
    * @param schemaInstancePrefix the prefix representing the schema instance namespace
    */
    private void addTypeAttributes(NodeList elements, XMLField field, Object value, String schemaInstancePrefix) {
        NamespaceResolver namespaceResolver = getNamespaceResolverForField(field);
        if (!field.isTypedTextField()) {
            return;
        }

        List values;
        if (value instanceof List) {
            values = (List)value;
        } else {
            values = new ArrayList();
            values.add(value);
        }

        int size = elements.getLength();
        int valuesSize = values.size();

        if (size != valuesSize) {
            return;
        }

        Node next = null;
        for (int i = 0; i < size; i++) {
            next = elements.item(i);
            if (next.getNodeType() == Node.ELEMENT_NODE) {
                Class valueClass = values.get(i).getClass();
                if(valueClass != ClassConstants.STRING){
                    QName qname = field.getXMLType(valueClass);
                    if (qname != null) {
                        if (null == schemaInstancePrefix) {
                            schemaInstancePrefix = namespaceResolver.generatePrefix(XMLConstants.SCHEMA_INSTANCE_PREFIX);
                            ((Element)next).setAttributeNS(XMLConstants.XMLNS_URL, XMLConstants.XMLNS + XMLConstants.COLON + schemaInstancePrefix, XMLConstants.SCHEMA_INSTANCE_URL);
                        }

                        String type;
                        String prefix = this.resolveNamespacePrefixForURI(qname.getNamespaceURI(), namespaceResolver);
                        if (prefix == null || prefix.length() == 0) {
                            if(qname.getNamespaceURI().equals(XMLConstants.SCHEMA_URL)){
                            	prefix = namespaceResolver.generatePrefix(XMLConstants.SCHEMA_PREFIX);
                            }else{
                            	prefix = namespaceResolver.generatePrefix();
                            }
                            ((Element)next).setAttributeNS(XMLConstants.XMLNS_URL, XMLConstants.XMLNS + XMLConstants.COLON + prefix, qname.getNamespaceURI());
                        }
                        type = prefix + XMLConstants.COLON + qname.getLocalPart();
                        ((Element)next).setAttributeNS(XMLConstants.SCHEMA_INSTANCE_URL, schemaInstancePrefix + XMLConstants.COLON + XMLConstants.SCHEMA_TYPE_ATTRIBUTE, type);
                    }
                }
            }
        }
    }

    /**
    * Creates a new Element and appends a value to an element.
    *
    * @param parent Element which will own the newly created element
    * @param elementName tag name for the new element
    * @param value Node to add
    *
    */
    private Node createElement(Node parent, XPathFragment fragment, NamespaceResolver namespaceResolver, Node value) {
        String elementName = fragment.getXPath();

        // The case of the parent being the document (element is the root) needs to be handled.
        // Document have no owner document, but are the document.
        Document document = parent.getOwnerDocument();
        if ((document == null) && (parent.getNodeType() == Node.DOCUMENT_NODE)) {
            document = (Document)parent;
        }

        String nodeUri = value.getNamespaceURI();
        String nodeName = value.getLocalName();

        //String fragUri = resolveNamespacePrefix(fragment, namespaceResolver);
        String fragUri = fragment.getNamespaceURI();
        String fragName = fragment.getLocalName();

        if ((nodeName != null) && nodeName.equals(fragName) && (((nodeUri != null) && nodeUri.equals(fragUri)) || ((nodeUri == null) && (fragUri == null)))) {
            if (document != value.getOwnerDocument()) {
                return document.importNode(value, true);
            }
            return value;
        } else {
            // Need to reset the node name.
            String namespace = resolveNamespacePrefix(fragment, namespaceResolver);
            Element clone = document.createElementNS(namespace, elementName);
            NamedNodeMap attributes = value.getAttributes();
            int attributesLength = attributes.getLength();

            for (int index = 0; index < attributesLength; index++) {
                Node attribute = document.importNode(attributes.item(index), true);
                clone.setAttributeNode((Attr)attribute);
            }

            NodeList elements = value.getChildNodes();
            int elementsLength = elements.getLength();
            for (int index = 0; index < elementsLength; index++) {
                Node attribute = document.importNode(elements.item(index), true);
                clone.appendChild(attribute);
            }
            return clone;
        }
    }

    /**
    * Add a new attribute to an element.  If the attribute already exists, return the element.
    *
    * @param attributeName Name of the attribute to add
    * @param parent Element to create the attribute on
    * @param value Value for the new attribute
    *
    * @return The <code>XMLElement</code> that the attribute was added to (same as the <code>parent</code> parameter).
    */
    private Node addAttribute(XPathFragment attributeFragment, XMLField xmlField, Node parent, Object value, AbstractSession session) {
        Object valueToWrite = null;

        if (!(parent instanceof Element)) {
            return parent;
        }
        Element parentElement = (Element)parent;
        if (value instanceof Node) {
            if (((Node)value).getNodeType() == Node.ATTRIBUTE_NODE) {
                Attr attr = (Attr)value;
                if (parent.getAttributes().getNamedItemNS(attr.getNamespaceURI(), attr.getLocalName()) == null) {
                    String pfx = null;
                    if (xmlField.getNamespaceResolver() != null) {
                        pfx = getNamespaceResolverForField(xmlField).resolveNamespaceURI(attr.getNamespaceURI());
                    }
                    if (pfx != null) {
                        // If the namespace resolver has a prefix for the node's URI, use it
                        parentElement.setAttributeNS(attr.getNamespaceURI(), pfx + XMLConstants.COLON + attr.getLocalName(), attr.getNodeValue());
                    } else {
                        // No entry for the node's URI in the resolver, so use the node's
                        // prefix/uri pair and define the URI locally
                        parentElement.setAttributeNS(attr.getNamespaceURI(), attr.getName(), attr.getNodeValue());
                    }
                }
                return parent;
            }
            valueToWrite = value;
        } else {
            valueToWrite = getNonNodeValueToWrite(value, xmlField, session);
        }

        String attributeName = attributeFragment.getLocalName();
        String attributeNamespace = resolveNamespacePrefix(attributeFragment, getNamespaceResolverForField(xmlField));
        if ((valueToWrite != null) && (parent.getAttributes().getNamedItemNS(attributeNamespace, attributeName) == null)) {
            if (valueToWrite == this) {
                parentElement.setAttributeNS(attributeNamespace, attributeFragment.getShortName(), XMLConstants.EMPTY_STRING);
            } else if (valueToWrite instanceof String) {
                parentElement.setAttributeNS(attributeNamespace, attributeFragment.getShortName(), (String)valueToWrite);
            }
            if (attributeFragment.isGeneratedPrefix()) {
                parentElement.setAttributeNS(XMLConstants.XMLNS_URL, XMLConstants.XMLNS + XMLConstants.COLON + attributeFragment.getPrefix(), attributeFragment.getNamespaceURI());
            }
        }
        return parent;
    }

    // ==========================================================================================

    /**
    * Remove a node.  If <code>xpathString</code> points to an indexed element, the element will not be removed,
    * but will instead be reinitialzed (to maintain the index of the collection).
    *
    * @param xmlField Field containing XPath query string
    * @param element Root element at which to begin search
    *
    * @return <code>NodeList</code> containing the nodes that were removed.
    *
    * @exception org.eclipse.persistence.oxm.exceptions.XMLMarshalException Thrown if passed an invalid XPath string
    */
    public NodeList remove(XMLField xmlField, Node element) throws XMLMarshalException {
        return remove(xmlField, element, false);
    }

    /**
    * Remove a node.
    *
    * @param xmlField Field containing XPath query string
    * @param element Root element at which to begin search
    * @param forceRemove If <code>true</code>, then indexed elements will be truly deleted, otherwise they will be reinitialized
    *
    * @return <code>NodeList</code> containing the nodes that were removed.
    *
    * @exception org.eclipse.persistence.oxm.exceptions.XMLMarshalException Thrown if passed an invalid XPath string
    */
    public NodeList remove(XMLField xmlField, Node element, boolean forceRemove) throws XMLMarshalException {
        String xpathString = xmlField.getXPath();
        NodeList nodes = unmarshalXPathEngine.selectNodes(element, xmlField, getNamespaceResolverForField(xmlField));
        int numberOfNodes = nodes.getLength();

        boolean shouldNullOutNode = containsIndex(xpathString) && !forceRemove;

        // Remove the element or attribute, for positional element null-out instead of remove.
        for (int i = 0; i < numberOfNodes; i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                ((Attr)node).getOwnerElement().removeAttribute(node.getNodeName());
            } else {
                if (shouldNullOutNode) {
                    Node blankNode = node.getParentNode().getOwnerDocument().createElementNS(node.getNamespaceURI(), node.getNodeName());
                    node.getParentNode().replaceChild(blankNode, node);
                } else {
                    node.getParentNode().removeChild(node);
                }
            }
        }

        return nodes;
    }

    // ==========================================================================================

    /**
    * Replace the value of the nodes matching <code>xpathString</code> with <code>value</code>.
    * This method handles elements, indexed elements, and attributes.
    *
    * @param xmlField Field containing XPath query string
    * @param parent Parent element
    * @param value New value for the node
    *
    * @return <code>NodeList</code> containing the nodes that were replaced.
    */
    public NodeList replaceValue(XMLField xmlField, Node parent, Object value, AbstractSession session) throws XMLMarshalException {
        NodeList nodes = unmarshalXPathEngine.selectNodes(parent, xmlField, getNamespaceResolverForField(xmlField));
        int numberOfNodes = nodes.getLength();
        XMLNodeList createdElements = new XMLNodeList();
        for (int i = 0; i < numberOfNodes; i++) {
            Node node = nodes.item(i);

            // Handle Attributes and Text
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                if (((node.getNodeType() == Node.TEXT_NODE) || (node.getNodeType() == Node.CDATA_SECTION_NODE)) && (value == null)) {
                    Node parentNode = node.getParentNode();
                    Node grandParentNode = parentNode.getParentNode();
                    grandParentNode.removeChild(parentNode);
                } else {
                    node.setNodeValue((String) ((XMLConversionManager)session.getDatasourcePlatform().getConversionManager()).convertObject(value, ClassConstants.STRING));
                }
            } else {
                Element element = (Element)node;
                Node parentNode = element.getParentNode();
                if (value == null) {
                    parentNode.removeChild(element);
                } else {
                    String elementName = element.getTagName();

                    Element newElement = null;

                    Object valueToWrite = getValueToWrite(value, xmlField, session);
                    XPathFragment childFrag = new XPathFragment(elementName);
                    childFrag.setNamespaceURI(element.getNamespaceURI());
                    newElement = (Element)createElement(parentNode, childFrag, xmlField, valueToWrite, session);

                    createdElements.add(newElement);


                    if (newElement != element) {
                        parentNode.replaceChild(newElement, element);
                    }
                }
            }
        }
        if (xmlField.isTypedTextField()) {

            addTypeAttributes(createdElements, xmlField, value, resolveNamespacePrefixForURI(XMLConstants.SCHEMA_INSTANCE_URL, getNamespaceResolverForField(xmlField)));
        }
        return nodes;
    }

    public List<XMLEntry> replaceCollection(List<XMLField> xmlFields, List<XMLEntry> values, Node contextNode, DocumentPreservationPolicy docPresPolicy, XMLField lastUpdatedField, AbstractSession session) {
        List<XMLEntry> oldNodes = unmarshalXPathEngine.selectNodes(contextNode, xmlFields, getNamespaceResolverForField(xmlFields.get(0)));
        if(oldNodes == null || oldNodes.size() == 0) {
            return oldNodes;
        }

        Iterator<XMLEntry> oldValues = oldNodes.iterator();
        //Remove all the old values, and then call create to add them back in.
        while(oldValues.hasNext()) {
            XMLEntry entry = (XMLEntry)oldValues.next();
            Node nextNode = (Node)entry.getValue();
            Node parent = nextNode.getParentNode();
            parent.removeChild(nextNode);
            while(parent != contextNode) {
                if(parent.getChildNodes().getLength() == 0) {
                    nextNode = parent;
                    parent = nextNode.getParentNode();
                    parent.removeChild(nextNode);
                } else {
                    break;
                }
            }
        }

        create(xmlFields, contextNode, values, lastUpdatedField, xmlBinderPolicy, session);

        return oldNodes;
    }
    public NodeList replaceCollection(XMLField xmlField, Node parent, Collection values, AbstractSession session) throws XMLMarshalException {
        NodeList nodes = null;
        if (xmlField != null) {
            nodes = unmarshalXPathEngine.selectNodes(parent, xmlField, getNamespaceResolverForField(xmlField));
        } else {
            nodes = parent.getChildNodes();
        }
        if (nodes.getLength() == 0) {
            return nodes;
        }
        Iterator collectionValues = values.iterator();
        int i = 0;

        int nodesLength = nodes.getLength();
        Vector newNodes = new Vector();

        // Iterate over both collections until one runs out, creating a collection of correct nodes
        // while removing the old ones.
        boolean performedReplace = true;
        Object value = null;
        while ((i < nodesLength) && collectionValues.hasNext()) {
            //Keep track of which nodes have been replaced
            Node oldChild = nodes.item(i);
            Element newChild = null;
            if (performedReplace) {
                value = collectionValues.next();
            }
            Node parentNode = oldChild.getParentNode();

            // Handle Attributes and Text
            if (oldChild.getNodeType() != Node.ELEMENT_NODE) {
                if (((oldChild.getNodeType() == Node.TEXT_NODE) || (oldChild.getNodeType() == Node.CDATA_SECTION_NODE)) && (value == null)) {
                    Node grandParentNode = parentNode.getParentNode();
                    grandParentNode.removeChild(parentNode);
                } else {
                    oldChild.setNodeValue((String) ((XMLConversionManager)session.getDatasourcePlatform().getConversionManager()).convertObject(value, ClassConstants.STRING));
                }
            } else {
                Element element = (Element)oldChild;
                String elementName = element.getTagName();
                Object valueToWrite = getValueToWrite(value, xmlField, session);
                XPathFragment childFragment = new XPathFragment(elementName);
                childFragment.setNamespaceURI(element.getNamespaceURI());
                newChild = (Element)createElement(parentNode, childFragment, xmlField, valueToWrite, session);
                if (!newNodes.contains(oldChild)) {
                    if (newChild != oldChild) {
                        parentNode.replaceChild(newChild, oldChild);
                    }
                    newNodes.addElement(newChild);
                    performedReplace = true;
                } else {
                    performedReplace = false;
                }
            }
            i++;
        }

        // This means collection was ran out first. Remove left-overs.
        while (i < nodesLength) {
            Node toRemove = nodes.item(i);
            Node removedParent = toRemove.getParentNode();
            if ((removedParent != null) && !newNodes.contains(toRemove)) {
                //only remove it, if it's not already been added back in
                removedParent.removeChild(toRemove);
            }
            i++;
        }

        //Now add the nodes back in, in the correct order
        //for (Iterator newNodesIter = newNodes.iterator(); newNodesIter.hasNext();) {
        // Element newNode = (Element)newNodesIter.next();
        //this.create(xmlField, parent, newNode);
        //}
        if ((value != null) && !performedReplace) {
            //If we didn't add in the last element we tried then add it now
            if ((xmlField.getXPathFragment().getNextFragment() == null) || xmlField.getXPathFragment().getHasText()) {
                //if there's no grouping element, ensure that new collection elements
                //are added inline with the others
                create(xmlField, parent, value, xmlField, xmlBinderPolicy, session);
            } else {
                create(xmlField, parent, value, session);
            }
        }

        // Now add in any others that are left in the iterator
        while (collectionValues.hasNext()) {
            value = collectionValues.next();
            //If there's a grouping element, then just do the normal create
            if ((xmlField.getXPathFragment().getNextFragment() == null) || xmlField.getXPathFragment().getHasText()) {
                //if there's no grouping element, ensure that new collection elements
                //are added inline with the others
                create(xmlField, parent, value, xmlField, xmlBinderPolicy, session);
            } else {
                create(xmlField, parent, value, session);
            }
        }

        return nodes;
    }

    // ==========================================================================================

    /**
    * Determine if <code>xpathString</code> contains an index (e.g. 'element[index]').
    *
    * @param xpathString XPath expression to test
    *
    * @return <code>true</code> if <code>xpathString</code> contains an index, otherwise <code>false</code>.
    */
    private boolean containsIndex(String xpathString) {
        return (xpathString.lastIndexOf('[') != -1) && (xpathString.lastIndexOf(']') != -1);
    }

    private String resolveNamespacePrefix(XPathFragment fragment, NamespaceResolver namespaceResolver) {
        try {
            if (fragment.getNamespaceURI() != null) {
                return fragment.getNamespaceURI();
            }
            if(fragment.getPrefix() == null && fragment.isAttribute()) {
                return null;
            }
            return namespaceResolver.resolveNamespacePrefix(fragment.getPrefix());
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveNamespacePrefixForURI(String namespaceURI, NamespaceResolver namespaceResolver) {
        if (null == namespaceResolver) {
            return null;
        }

        Enumeration prefixes = namespaceResolver.getPrefixes();
        String prefix;
        while (prefixes.hasMoreElements()) {
            prefix = (String)prefixes.nextElement();
            if (namespaceResolver.resolveNamespacePrefix(prefix).equals(namespaceURI)) {
                return prefix;
            }
        }
        return null;
    }

    private Node addText(XMLField xmlField, Node element, String textValue) {
        if (xmlField.isCDATA()) {
            CDATASection cdata = element.getOwnerDocument().createCDATASection(textValue);
            element.appendChild(cdata);
            return cdata;
        } else {
            Text text = element.getOwnerDocument().createTextNode(textValue);
            element.appendChild(text);
            return text;
        }
    }
    private String getStringForQName(QName qName, NamespaceResolver namespaceResolver){
        if(null == qName) {
            return null;
        }

        if(null == qName.getNamespaceURI()) {
            return qName.getLocalPart();
        } else {
            String namespaceURI = qName.getNamespaceURI();
            if(namespaceResolver == null){
                throw XMLMarshalException.namespaceResolverNotSpecified(namespaceURI);
            }
            String prefix = namespaceResolver.resolveNamespaceURI(namespaceURI);
            if(null == prefix) {
                return qName.getLocalPart();
            } else {
                return prefix + XMLConstants.COLON + qName.getLocalPart();
            }
        }

    }

    private NamespaceResolver getNamespaceResolverForField(XMLField field){
        NamespaceResolver nr = field.getNamespaceResolver();
        if(nr == null){
            field.setNamespaceResolver(new NamespaceResolver());
        }
        return field.getNamespaceResolver();
    }
}
