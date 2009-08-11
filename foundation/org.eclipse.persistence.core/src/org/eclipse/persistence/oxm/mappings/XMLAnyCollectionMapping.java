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
package org.eclipse.persistence.oxm.mappings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import javax.xml.namespace.QName;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.descriptors.DescriptorIterator;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.internal.oxm.XMLObjectBuilder;
import org.eclipse.persistence.internal.oxm.XPathEngine;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.internal.queries.JoinedAttributeManager;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.internal.sessions.ChangeRecord;
import org.eclipse.persistence.internal.sessions.MergeManager;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.internal.sessions.UnitOfWorkImpl;
import org.eclipse.persistence.mappings.ContainerMapping;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.oxm.mappings.converters.XMLConverter;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.XMLMarshaller;
import org.eclipse.persistence.oxm.XMLRoot;
import org.eclipse.persistence.oxm.record.DOMRecord;
import org.eclipse.persistence.oxm.record.XMLRecord;
import org.eclipse.persistence.platform.xml.XMLPlatform;
import org.eclipse.persistence.platform.xml.XMLPlatformFactory;
import org.eclipse.persistence.queries.ObjectBuildingQuery;
import org.eclipse.persistence.queries.ObjectLevelReadQuery;
import org.eclipse.persistence.sessions.remote.RemoteSession;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * <p>Any collection XML mappings map an attribute that contains a heterogenous collection of
 * objects to multiple XML elements.  Unlike composite collection XML mappings, the referenced
 * objects may be of different types (including String), and do not need to be related to each
 * other through inheritance or a common interface.  The corresponding object attribute should
 * be generic enough for all possible application values.  Note that each of the referenced objects
 * (except String) must specify a default root element on their descriptor.
 *
 * <p><b>Any collection mappings are useful with the following XML schema constructs</b>:<ul>
 * <li> any </li>
 * <li> choice </li>
 * <li> substitution groups </li>
 * </ul>
 *
 * <p><b>Setting the XPath</b>: TopLink XML mappings make use of XPath statements to find the relevant
 * data in an XML document.  The XPath statement is relative to the context node specified in the descriptor.
 * The XPath may contain node type, path, and positional information.  The XPath is specified on the
 * mapping using the <code>setXPath</code> method.  Note that for XML Any Collection Mappings the XPath
 * is optional.
 *
 * <p>The following XPath statements may be used to specify the location of XML data relating to an object's
 * name attribute:
 *
 * <p><table border="1">
 * <tr>
 * <th id="c1" align="left">XPath</th>
 * <th id="c2" align="left">Description</th>
 * </tr>
 * <tr>
 * <td headers="c1">contact-methods</td>
 * <td headers="c2">The name information is stored in the contact-methods element.</td>
 * </tr>
 * <tr>
 * <td headers="c1" nowrap="true">contact-methods/info</td>
 * <td headers="c2">The XPath statement may be used to specify any valid path.</td>
 * </tr>
 * <tr>
 * <td headers="c1">contact-methods[2]</td>
 * <td headers="c2">The XPath statement may contain positional information.  In this case the contact
 * information is stored in the second occurrence of the contact-methods element.</td>
 * </tr>
 * </table>
 *
 * <p><b>Mapping an element of type xs:anyType as an Any Collection Mapping</b>:
 *  <!--
 * <?xml version="1.0" encoding="UTF-8"?>
 * <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema">
 *     <xsd:element name="customer" type="customer-type"/>
 *    <xsd:complexType name="customer-type">
 *         <xsd:sequence>
 *             <xsd:element name="contact-methods" type="xsd:anyType"/>
 *         </xsd:sequence>
 *     </xsd:complexType>
 *     <xsd:element name="address">
 *         <xsd:complexType>
 *             <xsd:sequence>
 *                 <xsd:element name="street" type="xsd:string"/>
 *                 <xsd:element name="city" type="xsd:string"/>
 *             </xsd:sequence>
 *         </xsd:complexType>
 *     </xsd:element>
 *     <xsd:element name="phone-number" type="xsd:string"/>
 * </xsd:schema>
 *  -->
 *
 * <p><em>XML Schema</em><br>
 * <code>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;<br>
 * &lt;xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;<br>
 * &nbsp;&nbsp;&lt;xsd:element name="customer" type="customer-type"/&gt;<br>
 * &nbsp;&nbsp;&lt;xsd:complexType name="customer-type"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="contact-methods" type="xsd:anyType"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&lt;/xsd:complexType&gt;<br>
 * &nbsp;&nbsp;&lt;xsd:element name="address"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:complexType&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="street" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="city" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/xsd:complexType&gt;<br>
 * &nbsp;&nbsp;&lt;/xsd:element&gt;<br>
 * &nbsp;&nbsp;&lt;xsd:element name="phone-number" type="xsd:string"/&gt;<br>
 * &lt;/xsd:schema&gt;<br>
 * </code>
 *
 * <p><em>Code Sample</em><br>
 * <code>
 * XMLAnyCollectionMapping contactMethodsMapping = new XMLAnyCollectionMapping();<br>
 * contactMethodsMapping.setAttributeName("contactMethods");<br>
 * contactMethodsMapping.setXPath("contact-methods");<br>
 * </code>
 *
 * <p><b>More Information</b>: For more information about using the XML Any Collection Mapping, see the
 * "Understanding XML Mappings" chapter of the Oracle TopLink Developer's Guide.
 *
 * @since Oracle TopLink 10<i>g</i> Release 2 (10.1.3)
 */
public class XMLAnyCollectionMapping extends XMLAbstractAnyMapping implements XMLMapping, ContainerMapping {
    private XMLField field;
    private ContainerPolicy containerPolicy;
    private boolean useXMLRoot;
    private boolean mixedContent = true;
    private boolean isWhitespacePreservedForMixedContent = false;
    private boolean areOtherMappingInThisContext = true;
    private XMLConverter valueConverter;
    private boolean reuseContainer;

    public XMLAnyCollectionMapping() {
        this.containerPolicy = ContainerPolicy.buildDefaultPolicy();
        useXMLRoot = false;
        setKeepAsElementPolicy(UnmarshalKeepAsElementPolicy.KEEP_NONE_AS_ELEMENT);
    }

    /**
    * INTERNAL:
    * Clone the attribute from the clone and assign it to the backup.
    */
    public void buildBackupClone(Object clone, Object backup, UnitOfWorkImpl unitOfWork) {
        throw DescriptorException.invalidMappingOperation(this, "buildBackupClone");
    }

    /**
    * INTERNAL:
    * Clone the attribute from the original and assign it to the clone.
    */
    public void buildClone(Object original, Object clone, UnitOfWorkImpl unitOfWork) {
        throw DescriptorException.invalidMappingOperation(this, "buildClone");
    }

    public void buildCloneFromRow(AbstractRecord databaseRow, JoinedAttributeManager joinManager, Object clone, ObjectBuildingQuery sourceQuery, UnitOfWorkImpl unitOfWork, AbstractSession executionSession) {
        throw DescriptorException.invalidMappingOperation(this, "buildCloneFromRow");
    }

    /**
     * INTERNAL:
     * Cascade perform delete through mappings that require the cascade
     */
    public void cascadePerformRemoveIfRequired(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        //objects referenced by this mapping are not registered as they have
        // no identity, this is a no-op.
    }

    /**
      * INTERNAL:
      * Cascade registerNew for Create through mappings that require the cascade
      */
    public void cascadeRegisterNewIfRequired(Object object, UnitOfWorkImpl uow, Map visitedObjects) {
        //Our current XML support does not make use of the UNitOfWork.
    }

    public Object clone() {
        // Bug 3037701 - clone the AttributeAccessor
        XMLAnyCollectionMapping mapping = null;
        mapping = (XMLAnyCollectionMapping) super.clone();
        mapping.setContainerPolicy(this.getContainerPolicy());
        mapping.setField(this.getField());
        return mapping;
    }

    /**
    * INTERNAL:
    * This method was created in VisualAge.
    * @return prototype.changeset.ChangeRecord
    */
    public ChangeRecord compareForChange(Object clone, Object backup, ObjectChangeSet owner, AbstractSession session) {
        throw DescriptorException.invalidMappingOperation(this, "compareForChange");
    }

    /**
    * INTERNAL:
    * Compare the attributes belonging to this mapping for the objects.
    */
    public boolean compareObjects(Object firstObject, Object secondObject, AbstractSession session) {
        throw DescriptorException.invalidMappingOperation(this, "compareObjects");
    }

    /**
    * INTERNAL:
    * An object has been serialized from the server to the client.
    * Replace the transient attributes of the remote value holders
    * with client-side objects.
    */
    public void fixObjectReferences(Object object, Map objectDescriptors, Map processedObjects, ObjectLevelReadQuery query, RemoteSession session) {
        throw DescriptorException.invalidMappingOperation(this, "fixObjectReferences");
    }

    /**
    * INTERNAL:
    * Return the mapping's containerPolicy.
    */
    public ContainerPolicy getContainerPolicy() {
        return containerPolicy;
    }

    public DatabaseField getField() {
        return field;
    }

    public void initialize(AbstractSession session) throws DescriptorException {
        if (getField() != null) {
            setField(getDescriptor().buildField(getField()));
        }
        if(valueConverter != null) {
            valueConverter.initialize(this, session);
        }
    }

    /**
    * INTERNAL:
    * Iterate on the appropriate attribute value.
    */
    public void iterate(DescriptorIterator iterator) {
        throw DescriptorException.invalidMappingOperation(this, "iterate");
    }

    public void setXPath(String xpath) {
        this.field = new XMLField(xpath);
    }

    /**
    * INTERNAL:
    * Merge changes from the source to the target object.
    */
    public void mergeChangesIntoObject(Object target, ChangeRecord changeRecord, Object source, MergeManager mergeManager) {
        throw DescriptorException.invalidMappingOperation(this, "mergeChangesIntoObject");
    }

    /**
    * INTERNAL:
    * Merge changes from the source to the target object.
    */
    public void mergeIntoObject(Object target, boolean isTargetUninitialized, Object source, MergeManager mergeManager) {
        throw DescriptorException.invalidMappingOperation(this, "mergeIntoObject");
    }

    public void setContainerPolicy(ContainerPolicy cp) {
        this.containerPolicy = cp;
    }

    public void setField(DatabaseField field) {
        this.field = (XMLField) field;
    }

    public Object valueFromRow(AbstractRecord row, JoinedAttributeManager joinManager, ObjectBuildingQuery sourceQuery, AbstractSession executionSession) throws DatabaseException {
        XMLRecord record = (XMLRecord) row;

        if (getField() != null) {
            //Get the nested row represented by this field to build the collection from
            Object nested = record.get(getField());
            if (nested instanceof Vector) {
                nested = ((Vector) nested).firstElement();
            }
            if (!(nested instanceof XMLRecord)) {
                return null;
            }
            record = (XMLRecord) nested;
        }
        return buildObjectValuesFromDOMRecord((DOMRecord) record, executionSession, sourceQuery, joinManager);
    }

    private Object buildObjectValuesFromDOMRecord(DOMRecord record, AbstractSession session, ObjectBuildingQuery query, JoinedAttributeManager joinManager) {
        //This DOMRecord represents the root node of the AnyType instance
        //Grab ALL children to populate the collection.
        Node root = record.getDOM();
        NodeList children = root.getChildNodes();
        ContainerPolicy cp = getContainerPolicy();
        Object container = cp.containerInstance();
        int length = children.getLength();
        Node next = null;
        if(length > 0) {
            next = record.getDOM().getFirstChild();
        }
        while(next != null) {
            Object objectValue = null;
            if (isUnmappedContent(next)) {
                if ((next.getNodeType() == Node.TEXT_NODE) && this.isMixedContent()) {
                    if (next.getNodeValue().trim().length() > 0) {
                        objectValue = next.getNodeValue();
                        if(getConverter() != null) {
                            objectValue = getConverter().convertDataValueToObjectValue(objectValue, session, record.getUnmarshaller());
                        }                       
                        cp.addInto(objectValue, container, session);
                    }
                } else if (next.getNodeType() == Node.ELEMENT_NODE) {
                    ClassDescriptor referenceDescriptor = null;

                    //In this case it must be an element so we need to dig up the descriptor
                    //make a nested record and build an object from it.                
                    DOMRecord nestedRecord = (DOMRecord) record.buildNestedRow((Element) next);

                    if (!useXMLRoot) {
                        referenceDescriptor = getDescriptor(nestedRecord, session, null);
                        objectValue = buildObjectForNonXMLRoot(referenceDescriptor, getConverter(), query, record, nestedRecord, joinManager, session, next, container, cp);
                    } else {
                        String schemaType = ((Element) next).getAttributeNS(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_TYPE_ATTRIBUTE);
                        QName schemaTypeQName = null;
                        XPathFragment frag = new XPathFragment();
                        if ((null != schemaType) && (!schemaType.equals(""))) {
                            frag.setXPath(schemaType);
                            if (frag.hasNamespace()) {
                                String prefix = frag.getPrefix();
                                XMLPlatform xmlPlatform = XMLPlatformFactory.getInstance().getXMLPlatform();
                                String url = xmlPlatform.resolveNamespacePrefix(next, prefix);
                                frag.setNamespaceURI(url);
                                schemaTypeQName = new QName(url, frag.getLocalName());
                            }
                            XMLContext xmlContext = nestedRecord.getUnmarshaller().getXMLContext();
                            referenceDescriptor = xmlContext.getDescriptorByGlobalType(frag);
                        }
                        if (referenceDescriptor == null) {
                            try {
                                QName qname = new QName(nestedRecord.getNamespaceURI(), nestedRecord.getLocalName());
                                referenceDescriptor = getDescriptor(nestedRecord, session, qname);
                            } catch (XMLMarshalException e) {
                                referenceDescriptor = null;
                            }
                            // Check if descriptor is for a wrapper, if it is null it out and let continue
                            XMLDescriptor xmlReferenceDescriptor = (XMLDescriptor) referenceDescriptor;
                            if (referenceDescriptor != null && xmlReferenceDescriptor.isWrapper()) {
                                referenceDescriptor = null;
                            }
                        }
                        if ((referenceDescriptor != null) && (getKeepAsElementPolicy() != UnmarshalKeepAsElementPolicy.KEEP_ALL_AS_ELEMENT)) {
                            buildObjectAndWrapInXMLRoot(referenceDescriptor, getConverter(), query, record, nestedRecord, joinManager, session, next, container, cp);
                        } else if ((referenceDescriptor == null) && (getKeepAsElementPolicy() == UnmarshalKeepAsElementPolicy.KEEP_UNKNOWN_AS_ELEMENT)) {
                            buildObjectNoReferenceDescriptor(record, session, next, container, cp);
                        } else {
                            Object value = null;
                            Node textchild = ((Element) next).getFirstChild();
                            if ((textchild != null) && (textchild.getNodeType() == Node.TEXT_NODE)) {
                                value = ((Text) textchild).getNodeValue();
                            }
                            if ((value != null) && !value.equals("")) {
                                if (schemaTypeQName != null) {
                                    Class theClass = (Class) XMLConversionManager.getDefaultXMLTypes().get(schemaTypeQName);
                                    if (theClass != null) {
                                        value = ((XMLConversionManager) session.getDatasourcePlatform().getConversionManager()).convertObject(value, theClass, schemaTypeQName);
                                    }
                                }
                                if(getConverter() != null) {
                                    value = getConverter().convertDataValueToObjectValue(value, session, record.getUnmarshaller());
                                }                       

                                XMLRoot rootValue = new XMLRoot();
                                rootValue.setLocalName(next.getLocalName());
                                rootValue.setSchemaType(schemaTypeQName);
                                rootValue.setNamespaceURI(next.getNamespaceURI());
                                rootValue.setObject(value);
                                cp.addInto(rootValue, container, session);
                            }
                        }
                    }
                }
            }
            next = next.getNextSibling();
        }
        return container;
    }

    public void writeFromObjectIntoRow(Object object, AbstractRecord row, AbstractSession session) throws DescriptorException {
        if (this.isReadOnly()) {
            return;
        }
        DOMRecord record = (DOMRecord) row;
        Object attributeValue = this.getAttributeValueFromObject(object);
        Node root = record.getDOM();
        org.w3c.dom.Document doc = record.getDocument();
        XMLField xmlRootField = null;
        if (attributeValue == null) {
            return;
        }
        if (field != null) {
            root = XPathEngine.getInstance().create((XMLField) getField(), record.getDOM(), session);
            DOMRecord rootRecord = new DOMRecord(root);
            rootRecord.setSession(session);
            rootRecord.setDocPresPolicy(record.getDocPresPolicy());
            rootRecord.setNamespaceResolver(record.getNamespaceResolver());
            rootRecord.setMarshaller(record.getMarshaller());
            record = rootRecord;
        }

        ContainerPolicy cp = this.getContainerPolicy();
        ArrayList childNodes = getUnmappedChildNodes(record.getDOM().getChildNodes());
        Object iter = cp.iteratorFor(attributeValue);
        int childNodeCount = 0;
        boolean wasXMLRoot = false;
        while (cp.hasNext(iter) && (childNodeCount < childNodes.size())) {
            Object element = cp.next(iter, session);
            if(this.getConverter() != null) {
                element = getConverter().convertObjectValueToDataValue(element, session, record.getMarshaller());
            }
            Object originalObject = element;
            Node nextChild = null;
            while (childNodeCount < childNodes.size()) {
                Node nextPossible = (Node) childNodes.get(childNodeCount);
                if ((nextPossible.getNodeType() == Node.ELEMENT_NODE) || (nextPossible.getNodeType() == Node.TEXT_NODE) || (nextPossible.getNodeType() == Node.CDATA_SECTION_NODE)) {
                    nextChild = nextPossible;
                    childNodeCount++;
                    break;
                }
                childNodeCount++;
            }
            if (nextChild == null) {
                break;
            }
            if (usesXMLRoot() && (element instanceof XMLRoot)) {
                xmlRootField = new XMLField();
                wasXMLRoot = true;
                XPathFragment frag = new XPathFragment();

                if ((((XMLRoot) element)).getRootFragment().getNamespaceURI() != null) {
                    frag.setNamespaceURI(((XMLRoot) element).getNamespaceURI());
                } else {
                    frag.setXPath(((XMLRoot) element).getLocalName());
                }
                xmlRootField.setXPathFragment(frag);

                xmlRootField.setNamespaceResolver(record.getNamespaceResolver());
                element = ((XMLRoot) element).getObject();
            }
            if (element instanceof String) {
                if (wasXMLRoot) {
                    if (((XMLRoot) originalObject).getRootFragment().getNamespaceURI() != null) {
                        String prefix = record.getNamespaceResolver().resolveNamespaceURI(((XMLRoot) originalObject).getRootFragment().getNamespaceURI());
                        if ((prefix == null) || prefix.equals("")) {
                            xmlRootField.getXPathFragment().setGeneratedPrefix(true);
                            prefix = record.getNamespaceResolver().generatePrefix();
                        }
                        xmlRootField.getXPathFragment().setXPath(prefix + ":" + ((XMLRoot) originalObject).getLocalName());
                    }
                }

                if (xmlRootField != null) {
                    XPathEngine.getInstance().create(xmlRootField, root, element, session);
                } else {
                    Text textNode = doc.createTextNode((String) element);
                    root.replaceChild(textNode, nextChild);
                }
            } else if (element instanceof org.w3c.dom.Node) {
                Node importedCopy = doc.importNode((Node) element, true);
                root.replaceChild(importedCopy, nextChild);
            } else {
                XMLDescriptor referenceDescriptor = (XMLDescriptor) session.getDescriptor(element.getClass());

                if (wasXMLRoot) {
                    if (((XMLRoot) originalObject).getRootFragment().getNamespaceURI() != null) {
                        String prefix = referenceDescriptor.getNonNullNamespaceResolver().resolveNamespaceURI(((XMLRoot) originalObject).getNamespaceURI());
                        if ((prefix == null) || prefix.equals("")) {
                            prefix = record.getNamespaceResolver().resolveNamespaceURI(((XMLRoot) originalObject).getRootFragment().getNamespaceURI());
                        }
                        if ((prefix == null) || prefix.equals("")) {
                            xmlRootField.getXPathFragment().setGeneratedPrefix(true);
                            prefix = record.getNamespaceResolver().generatePrefix();
                        }
                        xmlRootField.getXPathFragment().setXPath(prefix + ":" + ((XMLRoot) originalObject).getLocalName());
                    }
                }

                DOMRecord nestedRecord = (DOMRecord) buildCompositeRow(element, session, referenceDescriptor, row, xmlRootField, element, wasXMLRoot);
                if (nestedRecord != null) {
                    if (nestedRecord.getDOM() != nextChild) {
                        root.replaceChild(nestedRecord.getDOM(), nextChild);
                    }
                }
            }
        }
        if (childNodeCount < childNodes.size()) {
            //Remove whatever's left
            for (int i = childNodeCount; i < childNodes.size(); i++) {
                Node child = (Node) childNodes.get(i);
                if ((child.getNodeType() == Node.ELEMENT_NODE) || (child.getNodeType() == Node.TEXT_NODE) || (child.getNodeType() == Node.CDATA_SECTION_NODE)) {
                    root.removeChild(child);
                }
            }
        }
        if (cp.hasNext(iter)) {
            while (cp.hasNext(iter)) {
                //add in extras
                Object element = cp.next(iter, session);
                writeSingleValue(element, object, record, session);
            }
        }
    }

    public void writeSingleValue(Object element, Object parent, XMLRecord row, AbstractSession session) {
        XMLField xmlRootField = null;
        Object originalObject = element;
        DOMRecord record = (DOMRecord) row;
        Node root = record.getDOM();
        org.w3c.dom.Document doc = row.getDocument();
        boolean wasXMLRoot = false;

        if (usesXMLRoot() && (element instanceof XMLRoot)) {
            wasXMLRoot = true;
            xmlRootField = new XMLField();
            XPathFragment frag = new XPathFragment();
            if ((((XMLRoot) element)).getRootFragment().getNamespaceURI() != null) {
                frag.setNamespaceURI(((XMLRoot) element).getNamespaceURI());
            } else {
                frag.setXPath(((XMLRoot) element).getLocalName());
            }
            xmlRootField.setXPathFragment(frag);

            xmlRootField.setNamespaceResolver(row.getNamespaceResolver());
            element = ((XMLRoot) element).getObject();
        }
        if (element instanceof String) {
            writeSimpleValue(xmlRootField, element, originalObject, record, doc, root, wasXMLRoot, session);
        } else if (element instanceof org.w3c.dom.Node) {
            Node importedCopy = doc.importNode((Node) element, true);
            root.appendChild(importedCopy);
        } else {
            XMLDescriptor referenceDescriptor = (XMLDescriptor) session.getDescriptor(element.getClass());
            if (referenceDescriptor == null) {
                writeSimpleValue(xmlRootField, element, originalObject, record, doc, root, wasXMLRoot, session);
                return;
            }
            if (wasXMLRoot) {
                if (((XMLRoot) originalObject).getRootFragment().getNamespaceURI() != null) {
                    String prefix = referenceDescriptor.getNonNullNamespaceResolver().resolveNamespaceURI(((XMLRoot) originalObject).getNamespaceURI());
                    if ((prefix == null) || prefix.equals("")) {
                        prefix = record.getNamespaceResolver().resolveNamespaceURI(((XMLRoot) originalObject).getRootFragment().getNamespaceURI());
                    }
                    if ((prefix == null) || prefix.equals("")) {
                        xmlRootField.getXPathFragment().setGeneratedPrefix(true);
                        prefix = record.getNamespaceResolver().generatePrefix();
                    }
                    xmlRootField.getXPathFragment().setXPath(prefix + ":" + ((XMLRoot) originalObject).getLocalName());
                }
            }

            DOMRecord nestedRecord = (DOMRecord) buildCompositeRow(element, session, referenceDescriptor, row, xmlRootField, originalObject, wasXMLRoot);
            if (nestedRecord != null) {
                root.appendChild(nestedRecord.getDOM());
            }
        }
    }

    private void writeSimpleValue(XMLField xmlRootField, Object element, Object originalObject, DOMRecord record, org.w3c.dom.Document doc, Node root, boolean wasXMLRoot, AbstractSession session) {
        if (wasXMLRoot) {
            if (((XMLRoot) originalObject).getRootFragment().getNamespaceURI() != null) {
                String prefix = record.getNamespaceResolver().resolveNamespaceURI(((XMLRoot) originalObject).getRootFragment().getNamespaceURI());
                if ((prefix == null) || prefix.equals("")) {
                    xmlRootField.getXPathFragment().setGeneratedPrefix(true);
                    prefix = record.getNamespaceResolver().generatePrefix();
                }
                xmlRootField.getXPathFragment().setXPath(prefix + ":" + ((XMLRoot) originalObject).getLocalName());
            }
        }
        if (xmlRootField != null) {
            xmlRootField.setNamespaceResolver(record.getNamespaceResolver());
            QName qname = ((XMLRoot) originalObject).getSchemaType();
            if(qname != null){
                if(!qname.equals(XMLConstants.STRING_QNAME)){                   
                    xmlRootField.setSchemaType(qname);
                    xmlRootField.setIsTypedTextField(true);
                    xmlRootField.addJavaConversion(((XMLRoot) originalObject).getObject().getClass(), qname);
                }
            }
           Node newNode = XPathEngine.getInstance().create(xmlRootField, root, element, session);            
        } else {
            Text textNode = doc.createTextNode((String) element);
            root.appendChild(textNode);
        }
    }

    protected AbstractRecord buildCompositeRow(Object attributeValue, AbstractSession session, XMLDescriptor referenceDescriptor, AbstractRecord parentRow, DatabaseField field, Object originalObject, boolean wasXMLRoot) {
        if ((field == null) && (referenceDescriptor != null) && (referenceDescriptor.getDefaultRootElement() != null)) {
            field = referenceDescriptor.buildField(referenceDescriptor.getDefaultRootElement());
        }

        if ((field != null) && (referenceDescriptor != null)) {
            ((XMLRecord) parentRow).setLeafElementType(referenceDescriptor.getDefaultRootElementType());
            XMLObjectBuilder objectBuilder = (XMLObjectBuilder) referenceDescriptor.getObjectBuilder();

            boolean addXsiType = shouldAddXsiType(((XMLRecord) parentRow).getMarshaller(), referenceDescriptor, originalObject, wasXMLRoot);

            XMLRecord child = (XMLRecord) objectBuilder.createRecordFor(attributeValue, (XMLField) field, (XMLRecord) parentRow, this);
            child.setNamespaceResolver(((XMLRecord) parentRow).getNamespaceResolver());
            objectBuilder.buildIntoNestedRow(child, attributeValue, session, addXsiType);
            return child;
        }
        return null;
    }

    public boolean isXMLMapping() {
        return true;
    }

    public Vector getFields() {
        return this.collectFields();
    }

    /**
     * PUBLIC:
     * Configure the mapping to use an instance of the specified container class
     * to hold the target objects.
     * <p>jdk1.2.x: The container class must implement (directly or indirectly) the Collection interface.
     * <p>jdk1.1.x: The container class must be a subclass of Vector.
     */
    public void useCollectionClass(Class concreteContainerClass) {
        this.setContainerPolicy(ContainerPolicy.buildPolicyFor(concreteContainerClass));
    }

    public void useMapClass(Class concreteContainerClass, String methodName) {
        throw DescriptorException.invalidMappingOperation(this, "useMapClass");
    }

    public void setUseXMLRoot(boolean useXMLRoot) {
        this.useXMLRoot = useXMLRoot;
    }

    public boolean usesXMLRoot() {
        return useXMLRoot;
    }

    public boolean isMixedContent() {
        return mixedContent;
    }

    public void setMixedContent(boolean mixed) {
        mixedContent = mixed;
    }

    /**
     * INTERNAL:
     */
    public boolean shouldAddXsiType(XMLMarshaller xmlmarshaller, XMLDescriptor xmlDescriptor, Object originalObject, boolean wasXMLRoot) {
        if ((xmlDescriptor.getSchemaReference() != null) && xmlmarshaller.shouldWriteTypeAttribute(originalObject, xmlDescriptor, wasXMLRoot)) {
            return true;
        }
        return false;
    }

    private boolean isUnmappedContent(Node node) {
        if (!areOtherMappingInThisContext) {
            return true;
        }
        XMLDescriptor parentDesc = (XMLDescriptor) this.getDescriptor();
        XMLField field = (XMLField) this.getField();
        Iterator mappings = parentDesc.getMappings().iterator();
        int mappingsInContext = 0;
        while (mappings.hasNext()) {
            DatabaseMapping next = (DatabaseMapping) mappings.next();
            if (!(next == this)) {
                XMLField nextField = (XMLField) next.getField();
                XPathFragment frag = getFragmentToCompare(nextField, field);
                if (frag != null) {
                    mappingsInContext++;
                    if (((node.getNodeType() == Node.TEXT_NODE) || (node.getNodeType() == Node.CDATA_SECTION_NODE)) && frag.nameIsText()) {
                        return false;
                    }
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        String nodeNS = node.getNamespaceURI();
                        String fragNS = frag.getNamespaceURI();
                        String nodeLocalName = node.getLocalName();
                        String fragLocalName = frag.getLocalName();
                        if ((nodeNS == fragNS) || ((nodeNS != null) && (fragNS != null) && nodeNS.equals(fragNS))) {
                            if ((nodeLocalName == fragLocalName) || ((nodeLocalName != null) && (fragLocalName != null) && nodeLocalName.equals(fragLocalName))) {
                                return false;
                            }
                        }
                    }
                }
            }
            if (mappingsInContext == 0) {
                this.areOtherMappingInThisContext = false;
            }
        }
        return true;
    }

    private XPathFragment getFragmentToCompare(XMLField field, XMLField context) {
        if (field == null) {
            return null;
        }
        if (context == null) {
            return field.getXPathFragment();
        }
        XPathFragment fieldFrag = field.getXPathFragment();
        XPathFragment contextFrag = context.getXPathFragment();

        while ((fieldFrag != null) && (contextFrag != null)) {
            if (fieldFrag.equals(contextFrag)) {
                if (contextFrag.getNextFragment() == null) {
                    return fieldFrag.getNextFragment();
                } else {
                    contextFrag = contextFrag.getNextFragment();
                    fieldFrag = fieldFrag.getNextFragment();
                }
            } else {
                return null;
            }
        }
        return null;
    }

    private ArrayList getUnmappedChildNodes(NodeList nodes) {
        ArrayList unmappedNodes = new ArrayList();
        int length = nodes.getLength();
        for (int i = 0; i < length; i++) {
            Node next = nodes.item(i);
            if (isUnmappedContent(next)) {
                unmappedNodes.add(next);
            }
        }
        return unmappedNodes;
    }
   
    public boolean isCollectionMapping() {
        return true;
    }
    
    public void setConverter(XMLConverter conv) {
        this.valueConverter = conv;
    }
    
    public XMLConverter getConverter() {
        return this.valueConverter;
    }
    
    /**
     * Setting this to true indicates that text nodes containing *only* whitespaces should still be 
     * added to the collection as strings for mixed content.
     * 
     * If mixedContent is false, this setting has no effect.
     * @return
     */
    public boolean isWhitespacePreservedForMixedContent() {
        return this.isWhitespacePreservedForMixedContent;
    }
    
    public void setPreserveWhitespaceForMixedContent(boolean preserveWhitespace) {
        this.isWhitespacePreservedForMixedContent = preserveWhitespace;
    }

    /**
     * Return true if the original container on the object should be used if 
     * present.  If it is not present then the container policy will be used to
     * create the container. 
     */
    public boolean getReuseContainer() {
        return reuseContainer;
    }

    /**
     * Specify whether the original container on the object should be used if 
     * present.  If it is not present then the container policy will be used to
     * create the container. 
     */
    public void setReuseContainer(boolean reuseContainer) {
        this.reuseContainer = reuseContainer;
    }

}
