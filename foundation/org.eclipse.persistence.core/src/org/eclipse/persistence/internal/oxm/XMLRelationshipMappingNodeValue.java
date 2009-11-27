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

import java.lang.reflect.Modifier;
import java.util.List;
import javax.xml.namespace.QName;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.descriptors.Namespace;
import org.eclipse.persistence.internal.oxm.record.deferred.DescriptorNotFoundContentHandler;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.mappings.UnmarshalKeepAsElementPolicy;
import org.eclipse.persistence.oxm.mappings.XMLMapping;
import org.eclipse.persistence.oxm.mappings.converters.XMLConverter;
import org.eclipse.persistence.oxm.record.MarshalRecord;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;
import org.eclipse.persistence.oxm.record.XMLRecord;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

public abstract class XMLRelationshipMappingNodeValue extends MappingNodeValue {

    // Protected to public
    public void processChild(XPathFragment xPathFragment, UnmarshalRecord unmarshalRecord, Attributes atts, XMLDescriptor xmlDescriptor, DatabaseMapping mapping) throws SAXException {
        if(xmlDescriptor == null){
            //Use the DescriptorNotFoundContentHandler to "look ahead" and determine if this is a simple or complex element
            //if it is complex the exception should be thrown
            DescriptorNotFoundContentHandler handler = new DescriptorNotFoundContentHandler(unmarshalRecord, mapping);
            String qnameString = xPathFragment.getLocalName();
            if(xPathFragment.getPrefix() != null) {
                qnameString = xPathFragment.getPrefix()  +XMLConstants.COLON + qnameString;
            }
            handler.startElement(xPathFragment.getNamespaceURI(), xPathFragment.getLocalName(), qnameString, atts);
            unmarshalRecord.getXMLReader().setContentHandler(handler);
            return;
        }

        if (xmlDescriptor.hasInheritance()) {
            unmarshalRecord.setAttributes(atts);
            Class classValue = xmlDescriptor.getInheritancePolicy().classFromRow(unmarshalRecord, unmarshalRecord.getSession());
            if (classValue == null) {
                // no xsi:type attribute - look for type indicator on the default root element
                QName leafElementType = unmarshalRecord.getLeafElementType();

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
                xmlDescriptor = (XMLDescriptor)unmarshalRecord.getSession().getDescriptor(classValue);
            } else {
                // since there is no xsi:type attribute, use the reference descriptor set
                // on the mapping -  make sure it is non-abstract
                if (Modifier.isAbstract(xmlDescriptor.getJavaClass().getModifiers())) {
                    // need to throw an exception here
                    throw DescriptorException.missingClassIndicatorField(unmarshalRecord, xmlDescriptor.getInheritancePolicy().getDescriptor());
                }
            }
        }
        TreeObjectBuilder targetObjectBuilder = (TreeObjectBuilder)xmlDescriptor.getObjectBuilder();
        UnmarshalRecord childRecord = unmarshalRecord.getChildUnmarshalRecord(targetObjectBuilder);
        unmarshalRecord.setChildRecord(childRecord);
        childRecord.setAttributes(atts);
        childRecord.startDocument();
        childRecord.startElement(xPathFragment.getNamespaceURI(), xPathFragment.getLocalName(), xPathFragment.getShortName(), atts);
        unmarshalRecord.getXMLReader().setContentHandler(unmarshalRecord.getChildRecord());
        try {
            unmarshalRecord.getXMLReader().setProperty("http://xml.org/sax/properties/lexical-handler", unmarshalRecord.getChildRecord());
        } catch (SAXNotRecognizedException ex) {
        } catch (SAXNotSupportedException ex) {
            //if lexical handling is not supported by this parser, just ignore.
        }
    }

    protected XMLDescriptor findReferenceDescriptor(XPathFragment xPathFragment, UnmarshalRecord unmarshalRecord, Attributes atts, DatabaseMapping mapping, UnmarshalKeepAsElementPolicy policy) {
        XMLDescriptor returnDescriptor = null;
        //try xsi:type
        if(atts != null){
            XMLContext xmlContext = unmarshalRecord.getUnmarshaller().getXMLContext();
            String schemaType = atts.getValue(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_TYPE_ATTRIBUTE);
            if(schemaType != null){
                schemaType = schemaType.trim();
                if(schemaType.length() > 0) {
                    XPathFragment frag = new XPathFragment();
                frag.setXPath(schemaType);

                QName qname = null;
                if (frag.hasNamespace()) {
                    String prefix = frag.getPrefix();
                    String url = unmarshalRecord.resolveNamespacePrefix(prefix);
                    frag.setNamespaceURI(url);

                    qname = new QName(url, frag.getLocalName());
                    unmarshalRecord.setTypeQName(qname);
                }
                returnDescriptor = xmlContext.getDescriptorByGlobalType(frag);
                if(returnDescriptor == null){
                    if(policy ==null || (policy != null && policy != UnmarshalKeepAsElementPolicy.KEEP_UNKNOWN_AS_ELEMENT && policy != UnmarshalKeepAsElementPolicy.KEEP_ALL_AS_ELEMENT)){
                        Class theClass = (Class)((XMLConversionManager) unmarshalRecord.getSession().getDatasourcePlatform().getConversionManager()).getDefaultXMLTypes().get(qname);
                        if(theClass == null){
                            throw XMLMarshalException.noDescriptorFound(mapping);
                            }
                        }
                    }
               }
            }
        }
        return returnDescriptor;
    }

    protected void addTypeAttribute(XMLDescriptor descriptor, MarshalRecord marshalRecord, String schemaContext) {
        String typeValue = schemaContext.substring(1);

        String xsiPrefix = null;
        if (descriptor.getNamespaceResolver() != null) {
            xsiPrefix = descriptor.getNamespaceResolver().resolveNamespaceURI(XMLConstants.SCHEMA_INSTANCE_URL);
        } else {
            xsiPrefix = XMLConstants.SCHEMA_INSTANCE_PREFIX;
            marshalRecord.attribute(XMLConstants.XMLNS_URL, xsiPrefix, XMLConstants.XMLNS + XMLConstants.COLON + xsiPrefix, XMLConstants.SCHEMA_INSTANCE_URL);
        }
        if (xsiPrefix == null) {
            xsiPrefix = descriptor.getNamespaceResolver().generatePrefix(XMLConstants.SCHEMA_INSTANCE_PREFIX);
            marshalRecord.attribute(XMLConstants.XMLNS_URL, xsiPrefix, XMLConstants.XMLNS + XMLConstants.COLON + xsiPrefix, XMLConstants.SCHEMA_INSTANCE_URL);
        }
        marshalRecord.attribute(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_TYPE_ATTRIBUTE, xsiPrefix + XMLConstants.COLON + XMLConstants.SCHEMA_TYPE_ATTRIBUTE, typeValue);
    }

    protected void writeExtraNamespaces(List extraNamespaces, XMLRecord xmlRecord, AbstractSession session) {
        if (extraNamespaces == null) {
            return;
        }
        for (int i = 0, extraNamespacesSize=extraNamespaces.size(); i < extraNamespacesSize; i++) {
            Namespace next = (Namespace)extraNamespaces.get(i);
            ((MarshalRecord)xmlRecord).attribute(XMLConstants.XMLNS_URL, next.getPrefix(), XMLConstants.XMLNS + XMLConstants.COLON + next.getPrefix(), next.getNamespaceURI());
        }

    }

    protected void setupHandlerForKeepAsElementPolicy(UnmarshalRecord unmarshalRecord, XPathFragment xPathFragment, Attributes atts) {
        SAXFragmentBuilder builder = unmarshalRecord.getFragmentBuilder();
        builder.setOwningRecord(unmarshalRecord);
        try {
            String namespaceURI = XMLConstants.EMPTY_STRING;
            if (xPathFragment.getNamespaceURI() != null) {
                namespaceURI = xPathFragment.getNamespaceURI();
            }
            String qName = xPathFragment.getLocalName();
            if (xPathFragment.getPrefix() != null) {
                qName = xPathFragment.getPrefix() + XMLConstants.COLON + qName;
            }

            builder.startElement(namespaceURI, xPathFragment.getLocalName(), qName, atts);
            unmarshalRecord.getXMLReader().setContentHandler(builder);
        } catch (SAXException ex) {
        }
    }

    protected void setOrAddAttributeValueForKeepAsElement(SAXFragmentBuilder builder, XMLMapping mapping, XMLConverter converter, UnmarshalRecord unmarshalRecord, boolean isCollection, Object collection) {
        Object node = builder.getNodes().remove(builder.getNodes().size() -1);
        if (converter != null) {
            node = converter.convertDataValueToObjectValue(node, unmarshalRecord.getSession(), unmarshalRecord.getUnmarshaller());
        }

        if (isCollection) {
            if(collection != null){
                unmarshalRecord.addAttributeValue((ContainerValue) this, node, collection);
            }else{
                unmarshalRecord.addAttributeValue((ContainerValue) this, node);
            }
        } else {
            unmarshalRecord.setAttributeValue(node, (DatabaseMapping) mapping);
        }
    }

    protected void endElementProcessText(UnmarshalRecord unmarshalRecord, Converter converter, XPathFragment xPathFragment, Object collection) {
        Object value = unmarshalRecord.getStringBuffer().toString();
        if(converter != null) {
            if(converter instanceof XMLConverter){
                value = ((XMLConverter)converter).convertDataValueToObjectValue(value, unmarshalRecord.getSession(), unmarshalRecord.getUnmarshaller());
            }else{
                value = converter.convertDataValueToObjectValue(value, unmarshalRecord.getSession());
            }
        }
        unmarshalRecord.resetStringBuffer();
        if (!XMLConstants.EMPTY_STRING.equals(value)) {
            QName qname = unmarshalRecord.getTypeQName();
            if (qname != null) {
                Class theClass = (Class) XMLConversionManager.getDefaultXMLTypes().get(qname);
                if (theClass != null) {
                    value = ((XMLConversionManager) unmarshalRecord.getSession().getDatasourcePlatform().getConversionManager()).convertObject(value, theClass, qname);
                }
            }
            setOrAddAttributeValue(unmarshalRecord, value, xPathFragment, collection);
        }
    }

    protected abstract void setOrAddAttributeValue(UnmarshalRecord unmarshalRecord, Object value, XPathFragment xPathFragment, Object collection);
}
