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
package org.eclipse.persistence.internal.oxm.record;

import java.lang.reflect.Modifier;

import javax.xml.namespace.QName;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.security.PrivilegedNewInstanceFromClass;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLUnmarshaller;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;
import org.eclipse.persistence.oxm.unmapped.UnmappedContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.Locator2;
import org.eclipse.persistence.internal.oxm.record.XMLReader;
import org.eclipse.persistence.internal.oxm.record.namespaces.StackUnmarshalNamespaceResolver;
import org.eclipse.persistence.internal.oxm.record.namespaces.UnmarshalNamespaceResolver;

/**
 * INTERNAL:
 * <p><b>Purpose:</b>An implementation of ContentHandler used to handle the root element of an
 * XML Document during unmarshal.
 * <p><b>Responsibilities:</b><ul>
 * <li>Implement ContentHandler interface</li>
 * <li>Handle startElement event for the root-level element of an xml document</li>
 * <li>Handle inheritance, and descriptor lookup to determine the correct class associated with
 * this XML Element.</li>
 * </ul>
 *
 * @author bdoughan
 *
 */
public class SAXUnmarshallerHandler implements ContentHandler {
    private static final String EMPTY_STRING = "";
    private XMLReader xmlReader;
    private XMLContext xmlContext;
    private Object object;
    private XMLUnmarshaller unmarshaller;
    private AbstractSession session;
    private Locator2 locator;
    private UnmarshalNamespaceResolver unmarshalNamespaceResolver;

    public SAXUnmarshallerHandler(XMLContext xmlContext) {
        super();
        this.xmlContext = xmlContext;
        unmarshalNamespaceResolver = new StackUnmarshalNamespaceResolver();
    }

    public XMLReader getXMLReader() {
        return this.xmlReader;
    }

    public void setXMLReader(XMLReader xmlReader) {
        this.xmlReader = xmlReader;
    }

    public Object getObject() {
        return this.object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public void setDocumentLocator(Locator locator) {
        if (locator instanceof Locator2) {
            this.locator = (Locator2)locator;
        }
    }

    public UnmarshalNamespaceResolver getUnmarshalNamespaceResolver() {
        return this.unmarshalNamespaceResolver;
    }

    public void setUnmarshalNamespaceResolver(UnmarshalNamespaceResolver unmarshalNamespaceResolver) {
        this.unmarshalNamespaceResolver = unmarshalNamespaceResolver;
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        unmarshalNamespaceResolver.push(prefix, uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        unmarshalNamespaceResolver.pop(prefix);
    }

    /**
     * INTERNAL:
     *
     * Resolve any mapping references.
     */
    public void resolveReferences() {
        unmarshaller.resolveReferences(session);
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        try {
            String name;
            if (EMPTY_STRING.equals(localName) || (localName == null)) {
                name = qName;
            } else {
                name = localName;
            }

            QName rootQName;
            if (EMPTY_STRING.equals(namespaceURI) || (namespaceURI == null)) {
                rootQName = new QName(name);
            } else {
                rootQName = new QName(namespaceURI, name);
            }

            XMLDescriptor xmlDescriptor = null;
            String type = atts.getValue(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_TYPE_ATTRIBUTE);
            if (null != type) {
                XPathFragment typeFragment = new XPathFragment(type);

                // set the prefix using a reverse key lookup by uri value on namespaceMap 
                if (null != unmarshalNamespaceResolver) {
                    typeFragment.setNamespaceURI(unmarshalNamespaceResolver.getNamespaceURI(typeFragment.getPrefix()));
                }
                xmlDescriptor = xmlContext.getDescriptorByGlobalType(typeFragment);
            }
            if(xmlDescriptor == null){
            	xmlDescriptor = xmlContext.getDescriptor(rootQName);
            	
                if (null == xmlDescriptor) {
                    //check for a cached object and look for descriptor by class
                    Object obj = this.xmlReader.getCurrentObject(session, null);
                    if (obj != null) {
                        xmlDescriptor = (XMLDescriptor)xmlContext.getSession(obj.getClass()).getDescriptor(obj.getClass());
                    }
                }
                if (null == xmlDescriptor) {
                    Class unmappedContentHandlerClass = unmarshaller.getUnmappedContentHandlerClass();
                    if (null == unmappedContentHandlerClass) {
                        throw XMLMarshalException.noDescriptorWithMatchingRootElement(rootQName.toString());
                    } else {
                        UnmappedContentHandler unmappedContentHandler;

                        try {
                            PrivilegedNewInstanceFromClass privilegedNewInstanceFromClass = new PrivilegedNewInstanceFromClass(unmappedContentHandlerClass);
                            unmappedContentHandler = (UnmappedContentHandler)privilegedNewInstanceFromClass.run();
                        } catch (ClassCastException e) {
                            throw XMLMarshalException.unmappedContentHandlerDoesntImplement(e, unmappedContentHandlerClass.getName());
                        } catch (IllegalAccessException e) {
                            throw XMLMarshalException.errorInstantiatingUnmappedContentHandler(e, unmappedContentHandlerClass.getName());
                        } catch (InstantiationException e) {
                            throw XMLMarshalException.errorInstantiatingUnmappedContentHandler(e, unmappedContentHandlerClass.getName());
                        }
                        
                        UnmappedContentHandlerWrapper unmappedContentHandlerWrapper = new UnmappedContentHandlerWrapper(unmappedContentHandler, this);
                        unmappedContentHandler.setUnmarshalRecord(unmappedContentHandlerWrapper);

                        unmappedContentHandler.startElement(namespaceURI, localName, qName, atts);
                        xmlReader.setContentHandler(unmappedContentHandler);

                        setObject(unmappedContentHandlerWrapper.getCurrentObject());
                        return;

                    }
                }
            }

            // for XMLObjectReferenceMappings we need a non-shared cache, so
            // try and get a Unit Of Work from the XMLContext
            session = xmlContext.getReadSession(xmlDescriptor);

            UnmarshalRecord unmarshalRecord;
            if (xmlDescriptor.hasInheritance()) {
                unmarshalRecord = new UnmarshalRecord(null);
                unmarshalRecord.setUnmarshalNamespaceResolver(unmarshalNamespaceResolver);
                unmarshalRecord.setAttributes(atts);
                Class classValue = xmlDescriptor.getInheritancePolicy().classFromRow(unmarshalRecord, session);
                if (classValue == null) {
                    // no xsi:type attribute - look for type indicator on the default root element
                    QName leafElementType = xmlDescriptor.getDefaultRootElementType();

                    // if we have a user-set type, try to get the class from the inheritance policy
                    if (leafElementType != null) {
                        Object indicator = xmlDescriptor.getInheritancePolicy().getClassIndicatorMapping().get(leafElementType);

                        // if the inheritance policy does not contain the user-set type, throw an exception
                        if (indicator == null) {
                            throw DescriptorException.missingClassForIndicatorFieldValue(leafElementType, xmlDescriptor.getInheritancePolicy().getDescriptor());
                        }
                        classValue = (Class)indicator;
                    }
                }
                if (classValue != null) {
                    xmlDescriptor = (XMLDescriptor)session.getDescriptor(classValue);
                } else {
                    // since there is no xsi:type attribute, we'll use the descriptor
                    // that was retrieved based on the rootQName -  we need to make 
                    // sure it is non-abstract
                    if (Modifier.isAbstract(xmlDescriptor.getJavaClass().getModifiers())) {
                        // need to throw an exception here
                        throw DescriptorException.missingClassIndicatorField(unmarshalRecord, xmlDescriptor.getInheritancePolicy().getDescriptor());
                    }
                }
            }

            unmarshalRecord = (UnmarshalRecord)xmlDescriptor.getObjectBuilder().createRecord(session);
            if (locator != null) {
                unmarshalRecord.setDocumentLocator(locator);
            }
            unmarshalRecord.setUnmarshaller(this.unmarshaller);
            unmarshalRecord.setXMLReader(this.getXMLReader());
            unmarshalRecord.setAttributes(atts);

            if(atts != null && null == atts.getValue(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_NIL_ATTRIBUTE)) {
                unmarshalRecord.setUnmarshalNamespaceResolver(unmarshalNamespaceResolver);
                unmarshalRecord.startDocument();
                xmlReader.setContentHandler(unmarshalRecord);
                unmarshalRecord.startElement(namespaceURI, localName, qName, atts);
                try {
                    unmarshalRecord.getXMLReader().setProperty("http://xml.org/sax/properties/lexical-handler", unmarshalRecord);
                } catch (SAXNotRecognizedException ex) {
                } catch (SAXNotSupportedException ex) {
                    //if lexical handling is not supported by this parser, just ignore. 
                }
            } else {
                unmarshalRecord.setRootElementName(qName);
                unmarshalRecord.setRootElementNamespaceUri(namespaceURI);
            }

            // if we located the descriptor via xsi:type attribute, create and 
            // return an XMLRoot object 
            object = xmlDescriptor.wrapObjectInXMLRoot(unmarshalRecord, unmarshaller.isResultAlwaysXMLRoot());
        } catch (EclipseLinkException e) {
            if (null == xmlReader.getErrorHandler()) {
                throw e;
            } else {
                SAXParseException saxParseException = new SAXParseException(null, null, null, 0, 0, e);
                xmlReader.getErrorHandler().error(saxParseException);
            }
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    public void setUnmarshaller(XMLUnmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    public XMLUnmarshaller getUnmarshaller() {
        return this.unmarshaller;
    }

}
