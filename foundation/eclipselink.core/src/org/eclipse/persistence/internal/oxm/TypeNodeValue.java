/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.oxm;

import java.util.ArrayList;
import javax.xml.namespace.QName;
import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.foundation.AbstractDirectMapping;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.XMLUnionField;
import org.eclipse.persistence.oxm.record.MarshalRecord;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;

public class TypeNodeValue extends NodeValue {
    private AbstractDirectMapping directMapping;

    public boolean isOwningNode(XPathFragment xPathFragment) {
        return (null != xPathFragment) && xPathFragment.isAttribute();
    }

    public boolean marshal(XPathFragment xPathFragment, MarshalRecord marshalRecord, Object object, AbstractSession session, NamespaceResolver namespaceResolver) {
        Object objectValue = directMapping.getAttributeValueFromObject(object);
        Object fieldValue = directMapping.getFieldValue(objectValue, session);
        if ((null == fieldValue) || (null == namespaceResolver)) {
            return false;
        }
        XMLField xmlField = (XMLField)directMapping.getField();
        QName schemaType = getSchemaType(xmlField, fieldValue);
        if (null == schemaType) {
            return false;
        }
        XPathFragment groupingFragment = marshalRecord.openStartGroupingElements(namespaceResolver);
        String typeQName = namespaceResolver.resolveNamespaceURI(XMLConstants.SCHEMA_INSTANCE_URL) + ":type";
        String schemaTypePrefix = namespaceResolver.resolveNamespaceURI(schemaType.getNamespaceURI());
        marshalRecord.attribute(XMLConstants.SCHEMA_INSTANCE_URL, schemaType.getLocalPart(), typeQName, schemaTypePrefix + ':' + schemaType.getLocalPart());
        marshalRecord.closeStartGroupingElements(groupingFragment);
        return true;
    }

    public AbstractDirectMapping getDirectMapping() {
        return directMapping;
    }

    public void setDirectMapping(AbstractDirectMapping directMapping) {
        this.directMapping = directMapping;
    }

    private QName getSchemaType(XMLField xmlField, Object value) {
        QName schemaType = null;
        if (xmlField.isTypedTextField()) {
            schemaType = xmlField.getXMLType(value.getClass());
        } else if (xmlField.isUnionField()) {
            return getSchemaTypeForUnion((XMLUnionField)xmlField, value);
        } else if (xmlField.getSchemaType() != null) {
            schemaType = xmlField.getSchemaType();
        }
        return schemaType;
    }

    private QName getSchemaTypeForUnion(XMLUnionField xmlField, Object value) {
        ArrayList schemaTypes = xmlField.getSchemaTypes();
        QName schemaType = null;
        QName nextQName;
        Class javaClass;
        for (int i = 0; i < schemaTypes.size(); i++) {
            nextQName = (QName)(xmlField).getSchemaTypes().get(i);
            try {
                if (nextQName != null) {
                    javaClass = xmlField.getJavaClass(nextQName);
                    value = XMLConversionManager.getDefaultXMLManager().convertObject(value, javaClass, nextQName);
                    schemaType = nextQName;
                    break;
                }
            } catch (ConversionException ce) {
                if (i == (schemaTypes.size() - 1)) {
                    schemaType = nextQName;
                }
            }
        }
        return schemaType;
    }

    public void attribute(UnmarshalRecord unmarshalRecord, String namespaceURI, String localName, String value) {
        //assume this is being called for xsi:type field
        if(value != null) {
            String namespace = null;
            int colonIndex = value.indexOf(COLON);
            if (colonIndex > -1) {
                String prefix = value.substring(0, colonIndex);
                namespace = unmarshalRecord.resolveNamespacePrefix(prefix);
                value = value.substring(colonIndex + 1);
            }
            unmarshalRecord.setTypeQName(new QName(namespace, value));            
        }
    }

}