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
* mmacivor - June 05/2008 - 1.0 - Initial implementation
******************************************************************************/
package org.eclipse.persistence.internal.oxm;

import org.xml.sax.SAXException;

import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.mappings.XMLBinaryDataCollectionMapping;
import org.eclipse.persistence.oxm.mappings.XMLBinaryDataMapping;
import org.eclipse.persistence.oxm.mappings.converters.XMLConverter;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;



public class XMLInlineBinaryHandler extends UnmarshalRecord {
    NodeValue nodeValue;
    DatabaseMapping mapping;
    boolean isCollection = false;
    Converter converter;
    UnmarshalRecord parent;
    
    
    public XMLInlineBinaryHandler(UnmarshalRecord parent, NodeValue nodeValue, DatabaseMapping mapping, Converter converter, boolean isCollection) {
        super(null);
        this.nodeValue = nodeValue;
        this.isCollection = isCollection;
        this.mapping = mapping;
        this.parent = parent;
        this.setUnmarshaller(parent.getUnmarshaller());
    }
    
    @Override
    public void characters(char[] ch, int offset, int length) throws SAXException {
        this.getStringBuffer().append(ch, offset, length);
    }
    
   
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
       //Since we know this was a simple or empty element, we know that we only got a characters event and then this. Process the
       //text.
       XMLField field = null;
       Object value = this.getStringBuffer().toString();
       resetStringBuffer();

       boolean isSwaRef = false;
       if(isCollection) {
           isSwaRef = ((XMLBinaryDataCollectionMapping)mapping).isSwaRef();
           field = (XMLField)((XMLBinaryDataCollectionMapping)mapping).getField();
       } else {
           isSwaRef = ((XMLBinaryDataMapping)mapping).isSwaRef();
           field = (XMLField)((XMLBinaryDataMapping)mapping).getField();
       }
           

       if (isSwaRef && (parent.getUnmarshaller().getAttachmentUnmarshaller() != null)) {
           if(mapping.getAttributeClassification() == XMLBinaryDataHelper.getXMLBinaryDataHelper().DATA_HANDLER) {
               value = parent.getUnmarshaller().getAttachmentUnmarshaller().getAttachmentAsDataHandler((String)value);
           } else {
               value = parent.getUnmarshaller().getAttachmentUnmarshaller().getAttachmentAsByteArray((String)value);
           }
           if (converter != null) {
               if (converter instanceof XMLConverter) {
                   value = ((XMLConverter)converter).convertDataValueToObjectValue(value, parent.getSession(), parent.getUnmarshaller());
               } else {
                   value = converter.convertDataValueToObjectValue(value, parent.getSession());
               }
           }
       } else {
           value = XMLConversionManager.getDefaultXMLManager().convertSchemaBase64ToByteArray(value);
           if (converter != null) {
               if (converter instanceof XMLConverter) {
                   value = ((XMLConverter)converter).convertDataValueToObjectValue(value, parent.getSession(), parent.getUnmarshaller());
               } else {
                   value = converter.convertDataValueToObjectValue(value, parent.getSession());
               }
           }
       }
       value = XMLBinaryDataHelper.getXMLBinaryDataHelper().convertObject(value, mapping.getAttributeClassification(), parent.getSession());
       if(isCollection) {
           if(value != null) {
               parent.addAttributeValue((ContainerValue)nodeValue, value);
           }
       } else {
           parent.setAttributeValue(value, mapping);
       }
       
       if(!field.isSelfField()){
           //Return control to the parent record
           parent.getXMLReader().setContentHandler(parent);
           parent.endElement(namespaceURI, localName, qName);       
       }
   }
}

