/*******************************************************************************
 * Copyright (c) 1998, 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.StringTokenizer;
import javax.xml.namespace.QName;

import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.oxm.record.MarshalContext;
import org.eclipse.persistence.internal.oxm.record.ObjectMarshalContext;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.mappings.XMLCompositeDirectCollectionMapping;
import org.eclipse.persistence.oxm.mappings.converters.XMLConverter;
import org.eclipse.persistence.oxm.mappings.nullpolicy.AbstractNullPolicy;
import org.eclipse.persistence.oxm.mappings.nullpolicy.XMLNullRepresentationType;
import org.eclipse.persistence.oxm.record.MarshalRecord;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;
import org.xml.sax.Attributes;

/**
 * INTERNAL:
 * <p><b>Purpose</b>: This is how the XML Composite Direct Collection Mapping is 
 * handled when used with the TreeObjectBuilder.</p>
 */

public class XMLCompositeDirectCollectionMappingNodeValue extends MappingNodeValue implements ContainerValue {
    private static final String SPACE = " ";
    private XMLCompositeDirectCollectionMapping xmlCompositeDirectCollectionMapping;
    private int index = -1;

    public XMLCompositeDirectCollectionMappingNodeValue(XMLCompositeDirectCollectionMapping xmlCompositeDirectCollectionMapping) {
        super();
        this.xmlCompositeDirectCollectionMapping = xmlCompositeDirectCollectionMapping;
    }

    public boolean isOwningNode(XPathFragment xPathFragment) {
        XPathFragment nextFragment = xPathFragment.getNextFragment();
        if(nextFragment == null || xmlCompositeDirectCollectionMapping.usesSingleNode()){
            return xPathFragment.isAttribute() || xPathFragment.nameIsText();
        } else {
            return (nextFragment != null) && (nextFragment.nameIsText() || nextFragment.isAttribute());
        }
    }

    /**
     * Override the method in XPathNode such that the marshaller can be set on the
     * marshalRecord - this is required for XMLConverter usage.
     */
    public boolean marshal(XPathFragment xPathFragment, MarshalRecord marshalRecord, Object object, AbstractSession session, NamespaceResolver namespaceResolver) {
        if (xmlCompositeDirectCollectionMapping.isReadOnly()) {
            return false;
        }

        ContainerPolicy cp = getContainerPolicy();
        Object collection = xmlCompositeDirectCollectionMapping.getAttributeAccessor().getAttributeValueFromObject(object);
        if (null == collection) {
            AbstractNullPolicy wrapperNP = xmlCompositeDirectCollectionMapping.getWrapperNullPolicy();
            if (wrapperNP != null && wrapperNP.getMarshalNullRepresentation() == XMLNullRepresentationType.XSI_NIL) {
                marshalRecord.nilSimple(namespaceResolver);
                return true;
            } else {
                return false;
            }
        }
        Object iterator = cp.iteratorFor(collection);
        XMLField xmlField = (XMLField) xmlCompositeDirectCollectionMapping.getField();
        if (cp.hasNext(iterator)) {
            XPathFragment groupingFragment = marshalRecord.openStartGroupingElements(namespaceResolver);
            marshalRecord.closeStartGroupingElements(groupingFragment);
        } else {
            if ((xmlField.usesSingleNode() && !xmlCompositeDirectCollectionMapping.isDefaultEmptyContainer()) || xmlCompositeDirectCollectionMapping.getWrapperNullPolicy() != null) {
                XPathFragment groupingFragment = marshalRecord.openStartGroupingElements(namespaceResolver);
                marshalRecord.closeStartGroupingElements(groupingFragment);
            } else {
                return false;
            }
        }
        Object objectValue;
        if (xmlField.usesSingleNode()) {
            StringBuilder stringValueStringBuilder = new StringBuilder();
            String newValue;
            QName schemaType;
            Converter valueConverter = xmlCompositeDirectCollectionMapping.getValueConverter();
            while (cp.hasNext(iterator)) {
                objectValue = cp.next(iterator, session);
                if (null != valueConverter) {
                    if (valueConverter instanceof XMLConverter) {
                        objectValue = ((XMLConverter) valueConverter).convertObjectValueToDataValue(objectValue, session, marshalRecord.getMarshaller());
                    } else {
                        objectValue = valueConverter.convertObjectValueToDataValue(objectValue, session);
                    }
                }                
                schemaType = xmlField.getSchemaTypeForValue(objectValue, session);

                newValue = getValueToWrite(schemaType, objectValue, (XMLConversionManager) session.getDatasourcePlatform().getConversionManager(), marshalRecord);
                if (null != newValue) {
                    stringValueStringBuilder.append(newValue);
                    if (cp.hasNext(iterator)) {
                        stringValueStringBuilder.append(SPACE);
                    }
                }
            }
            XPathFragment groupingFragment = marshalRecord.openStartGroupingElements(namespaceResolver);
            if (xPathFragment.isAttribute()) {
                marshalRecord.attribute(xPathFragment, namespaceResolver, stringValueStringBuilder.toString());
                marshalRecord.closeStartGroupingElements(groupingFragment);
            } else {
                marshalRecord.closeStartGroupingElements(groupingFragment);
                if (xmlCompositeDirectCollectionMapping.isCDATA()) {
                    marshalRecord.cdata(stringValueStringBuilder.toString());
                } else {
                    marshalRecord.characters(stringValueStringBuilder.toString());
                }
            }
        } else {
            while (cp.hasNext(iterator)) {
                objectValue = cp.next(iterator, session);
                marshalSingleValue(xPathFragment, marshalRecord, object, objectValue, session, namespaceResolver, ObjectMarshalContext.getInstance());
            }
        }
        return true;
    }

    public void attribute(UnmarshalRecord unmarshalRecord, String namespaceURI, String localName, String value) {
        Object collection = unmarshalRecord.getContainerInstance(this);
        if (xmlCompositeDirectCollectionMapping.usesSingleNode()) {
            StringTokenizer stringTokenizer = new StringTokenizer(value);
            while (stringTokenizer.hasMoreTokens()) {
                addUnmarshalValue(unmarshalRecord, stringTokenizer.nextToken(), collection);
            }
        } else {
            addUnmarshalValue(unmarshalRecord, value, collection);
        }
    }

    public boolean startElement(XPathFragment xPathFragment, UnmarshalRecord unmarshalRecord, Attributes atts) {
        XMLField xmlField = (XMLField) xmlCompositeDirectCollectionMapping.getField();
        XPathFragment lastXPathFragment = xmlField.getLastXPathFragment();
        if (lastXPathFragment.nameIsText()) {
            String type = atts.getValue(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_TYPE_ATTRIBUTE);
            if (null != type) {
                String namespaceURI = null;
                int colonIndex = type.indexOf(XMLConstants.COLON);
                if (colonIndex > -1) {
                    String prefix = type.substring(0, colonIndex);
                    namespaceURI = unmarshalRecord.resolveNamespacePrefix(prefix);
                    type = type.substring(colonIndex + 1);
                }
                unmarshalRecord.setTypeQName(new QName(namespaceURI, type));
            }

            if (unmarshalRecord.isNil() && xmlCompositeDirectCollectionMapping.getNullPolicy().isNullRepresentedByXsiNil()) {
                getContainerPolicy().addInto(null, unmarshalRecord.getContainerInstance(this), unmarshalRecord.getSession());
            }

        } else if (lastXPathFragment.isAttribute()) {
            if (!xmlField.usesSingleNode()) {
                String namespaceURI = lastXPathFragment.getNamespaceURI();
                if (namespaceURI == null) {
                    namespaceURI = XMLConstants.EMPTY_STRING;
                }
                String value = atts.getValue(namespaceURI, lastXPathFragment.getLocalName());
                Object collection = unmarshalRecord.getContainerInstance(this);
                addUnmarshalValue(unmarshalRecord, value, collection);
            }
        }
        return true;
    }

    public void endElement(XPathFragment xPathFragment, UnmarshalRecord unmarshalRecord) {
        XMLField xmlField = (XMLField) xmlCompositeDirectCollectionMapping.getField();
        XPathFragment lastXPathFragment = xmlField.getLastXPathFragment();
        if (!lastXPathFragment.nameIsText()) {
            return;
        }
        
        String value = unmarshalRecord.getCharacters().toString();
        Object collection = unmarshalRecord.getContainerInstance(this);
        unmarshalRecord.resetStringBuffer();

        if (xmlField.usesSingleNode()) {
            StringTokenizer stringTokenizer = new StringTokenizer(value);
            while (stringTokenizer.hasMoreTokens()) {
                addUnmarshalValue(unmarshalRecord, stringTokenizer.nextToken(), collection);
            }
        } else {
            addUnmarshalValue(unmarshalRecord, value, collection);            
        }
    }

    public void endElement(XPathFragment xPathFragment, UnmarshalRecord unmarshalRecord, Object collection) {
        String value = unmarshalRecord.getCharacters().toString();
        unmarshalRecord.resetStringBuffer();

        XMLField xmlField = (XMLField) xmlCompositeDirectCollectionMapping.getField();
        if (xmlField.usesSingleNode()) {
            StringTokenizer stringTokenizer = new StringTokenizer(value);
            while (stringTokenizer.hasMoreTokens()) {
                addUnmarshalValue(unmarshalRecord, stringTokenizer.nextToken(), collection);
            }
        } else {
            if(xmlField.getLastXPathFragment().nameIsText()){
            	addUnmarshalValue(unmarshalRecord, value, collection);
            }
        }
    }

    private void addUnmarshalValue(UnmarshalRecord unmarshalRecord, Object value, Object collection) {
        if (null == value) {
            return;
        }

        if (unmarshalRecord.isNil() && xmlCompositeDirectCollectionMapping.getNullPolicy().isNullRepresentedByXsiNil()) {            
            return;            
        }

               
        if ((!isWhitespaceAware() && XMLConstants.EMPTY_STRING.equals(value))) {
            value = null;
        }

        XMLField xmlField = (XMLField) xmlCompositeDirectCollectionMapping.getField();
	
        XMLConversionManager xmlConversionManager = (XMLConversionManager) unmarshalRecord.getSession().getDatasourcePlatform().getConversionManager();
        if (unmarshalRecord.getTypeQName() != null) {
            Class typeClass = xmlField.getJavaClass(unmarshalRecord.getTypeQName());
            value = xmlConversionManager.convertObject(value, typeClass, unmarshalRecord.getTypeQName());
        } else {
            value = xmlField.convertValueBasedOnSchemaType(value, xmlConversionManager, unmarshalRecord);
        }       

        Converter valueConverter = xmlCompositeDirectCollectionMapping.getValueConverter();
        if (null != valueConverter) {
            if (valueConverter instanceof XMLConverter) {
                value = ((XMLConverter) valueConverter).convertDataValueToObjectValue(value, unmarshalRecord.getSession(), unmarshalRecord.getUnmarshaller());
            } else {
                value = valueConverter.convertDataValueToObjectValue(value, unmarshalRecord.getSession());
            }
        }
        if(value != null && value.getClass() == ClassConstants.STRING) {
            if(xmlCompositeDirectCollectionMapping.isCollapsingStringValues()) {
                value = XMLConversionManager.getDefaultXMLManager().collapseStringValue((String)value);
            } else if(xmlCompositeDirectCollectionMapping.isNormalizingStringValues()) {
                value = XMLConversionManager.getDefaultXMLManager().normalizeStringValue((String)value);
            }
        }
        unmarshalRecord.addAttributeValue(this, value, collection);
    }

    public Object getContainerInstance() {
        return getContainerPolicy().containerInstance();
    }

    public void setContainerInstance(Object object, Object containerInstance) {
        xmlCompositeDirectCollectionMapping.setAttributeValueInObject(object, containerInstance);
    }

    public ContainerPolicy getContainerPolicy() {
        return xmlCompositeDirectCollectionMapping.getContainerPolicy();
    }

    public boolean isContainerValue() {
        return true;
    }

    public boolean marshalSingleValue(XPathFragment xPathFragment, MarshalRecord marshalRecord, Object object, Object value, AbstractSession session, NamespaceResolver namespaceResolver, MarshalContext marshalContext) {
        Converter valueConverter = xmlCompositeDirectCollectionMapping.getValueConverter();
        if (null != valueConverter) {
            if (valueConverter instanceof XMLConverter) {
                value = ((XMLConverter) valueConverter).convertObjectValueToDataValue(value, session, marshalRecord.getMarshaller());
            } else {
                value = valueConverter.convertObjectValueToDataValue(value, session);
            }
        }
        if (null != value) {            
        XMLField xmlField = (XMLField) xmlCompositeDirectCollectionMapping.getField();
            QName schemaType = xmlField.getSchemaTypeForValue(value, session);

            boolean isElementOpen = false;
            if(XMLConstants.QNAME_QNAME.equals(schemaType)) {
                QName fieldValue = (QName)value;
                if(fieldValue.getNamespaceURI() == null || fieldValue.getNamespaceURI().equals("") && marshalRecord.getNamespaceResolver().getDefaultNamespaceURI() != null) {
                    //In this case, an extra xmlns="" declaration is going to be added. This may 
                    //require adjusting the namespace of the current fragment.
                    String defaultNamespaceURI = namespaceResolver.getDefaultNamespaceURI();
                    if (defaultNamespaceURI.equals(xPathFragment.getNamespaceURI()) && xPathFragment.getPrefix() == null) {
                        String prefix = namespaceResolver.generatePrefix();
                        String xPath = prefix + XMLConstants.COLON + xPathFragment.getShortName(); 
                        XPathFragment newFragment = new XPathFragment(xPath);
                        newFragment.setNamespaceURI(defaultNamespaceURI);
                        newFragment.setNextFragment(xPathFragment.getNextFragment());
                        marshalRecord.openStartElement(newFragment, namespaceResolver);
                        isElementOpen = true;                       
                        marshalRecord.namespaceDeclaration(prefix,  defaultNamespaceURI);
                        marshalRecord.predicateAttribute(xPathFragment, namespaceResolver);
                        xPathFragment = newFragment;
                    } 
                }
            }
            if(!isElementOpen) {
                marshalRecord.openStartElement(xPathFragment, namespaceResolver);
            }
            String stringValue = getValueToWrite(schemaType, value, (XMLConversionManager) session.getDatasourcePlatform().getConversionManager(), marshalRecord);
            XPathFragment nextFragment = xPathFragment.getNextFragment();
            if (nextFragment.isAttribute()) {
                marshalRecord.predicateAttribute(xPathFragment, namespaceResolver);
                marshalRecord.attribute(nextFragment, namespaceResolver, stringValue);
                marshalRecord.closeStartElement();
            } else {
                if (xmlField.isTypedTextField()) {
                    updateNamespaces(schemaType, marshalRecord, xmlField);
                }
                marshalRecord.closeStartElement();
                marshalRecord.predicateAttribute(xPathFragment, namespaceResolver);
                if (xmlCompositeDirectCollectionMapping.isCDATA()) {
                    marshalRecord.cdata(stringValue);
                } else {
                    marshalRecord.characters(stringValue);
                }
            }
            marshalRecord.endElement(xPathFragment, namespaceResolver);
        } else {
            AbstractNullPolicy nullPolicy = xmlCompositeDirectCollectionMapping.getNullPolicy();
            if (nullPolicy.getMarshalNullRepresentation() != XMLNullRepresentationType.ABSENT_NODE) {
                marshalRecord.openStartElement(xPathFragment, namespaceResolver);
                XPathFragment nextFragment = xPathFragment.getNextFragment();
                nullPolicy.directMarshal(nextFragment, marshalRecord, object, session, namespaceResolver);

                marshalRecord.endElement(xPathFragment, namespaceResolver);
            }
        }
        return true;
    }

    public XMLCompositeDirectCollectionMapping getMapping() {
        return xmlCompositeDirectCollectionMapping;
    }

    public boolean isWhitespaceAware() {
        return !xmlCompositeDirectCollectionMapping.getNullPolicy().isNullRepresentedByEmptyNode();
    }

    public boolean getReuseContainer() {
        return getMapping().getReuseContainer();
    }
    
    /**
     *  INTERNAL:
     *  Used to track the index of the corresponding containerInstance in the containerInstances Object[] on UnmarshalRecord 
     */  
    public void setIndex(int index){
    	this.index = index;
    }
    
    /**
     * INTERNAL:
     * Set to track the index of the corresponding containerInstance in the containerInstances Object[] on UnmarshalRecord
     * Set during TreeObjectBuilder initialization 
     */
    public int getIndex(){
    	return index;
    }

    /**
     * INTERNAL
     * Return true if an empty container should be set on the object if there
     * is no presence of the collection in the XML document.
     * @since EclipseLink 2.3.3
     */
    public boolean isDefaultEmptyContainer() {
        return getMapping().isDefaultEmptyContainer();
    }

}