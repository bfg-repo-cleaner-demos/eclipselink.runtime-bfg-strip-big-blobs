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
 *      Denise Smith - November 2, 2009
 ******************************************************************************/  
package org.eclipse.persistence.oxm.record;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.oxm.record.XMLFragmentReader;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * <p>Use this type of MarshalRecord when the marshal target is an OutputStream and the
 * XML should be formatted with carriage returns and indenting. This type is only 
 * used if the encoding of the OutputStream is UTF-8</p>
 * <p><code>
 * XMLContext xmlContext = new XMLContext("session-name");<br>
 * XMLMarshaller xmlMarshaller = xmlContext.createMarshaller();<br>
 * FormattedOutputStreamRecord record = new FormattedOutputStreamRecord();<br>
 * record.setOutputStream(myOutputStream);<br>
 * xmlMarshaller.marshal(myObject, record);<br>
 * </code></p>
 * <p>If the marshal(OutputStream) and setFormattedOutput(true) method is called on
 * XMLMarshaller and the encoding is UTF-8, then the OutputStream is automatically wrapped
 * in a FormattedOutputStreamRecord.</p>
 * <p><code>
 * XMLContext xmlContext = new XMLContext("session-name");<br>
 * XMLMarshaller xmlMarshaller = xmlContext.createMarshaller();<br>
 * xmlMarshaller xmlMarshaller.setFormattedOutput(true);<br>
 * xmlMarshaller.marshal(myObject, myOutputStream);<br>
 * </code></p>
 * @see org.eclipse.persistence.oxm.XMLMarshaller
 */
public class FormattedOutputStreamRecord extends OutputStreamRecord {
	private static byte[] TAB;
    private int numberOfTabs;
    private boolean complexType;
    private boolean isLastEventText;
    
    static {
        try {
        	TAB = "   ".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
        } catch (UnsupportedEncodingException e) {
        }
    }
    
    public FormattedOutputStreamRecord() {
        super();
        numberOfTabs = 0;
        complexType = true;
        isLastEventText = false;
    }

    /**
     * INTERNAL:
     */
    public void endDocument() {
        try {
          outputStream.write(CR);    
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }

    /**
     * INTERNAL:
     */
    public void openStartElement(XPathFragment xPathFragment, NamespaceResolver namespaceResolver) {
        this.addPositionalNodes(xPathFragment, namespaceResolver);
        try {
            if (isStartElementOpen) {
            	outputStream.write(CLOSE_ELEMENT);
            }
            if (!isLastEventText) {
                if (numberOfTabs > 0) {
                	outputStream.write(CR);                    
                }
                for (int x = 0; x < numberOfTabs; x++) {
                	outputStream.write(TAB);
                }
            }
            isStartElementOpen = true;
            outputStream.write(OPEN_START_ELEMENT);
            outputStream.write(xPathFragment.getShortNameBytes());
            numberOfTabs++;
            isLastEventText = false;
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }

    /**
     * INTERNAL:
     */
    public void element(XPathFragment frag) {
        try {
            isLastEventText = false;
            if (isStartElementOpen) {
            	outputStream.write(CLOSE_ELEMENT);
                isStartElementOpen = false;
            }
            outputStream.write(CR);
            for (int x = 0; x < numberOfTabs; x++) {
            	outputStream.write(TAB);
            }
            super.element(frag);
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }    
   
    /**
     * INTERNAL:
     */
    public void endElement(XPathFragment xPathFragment, NamespaceResolver namespaceResolver) {
        try {
            isLastEventText = false;
            numberOfTabs--;
            if (isStartElementOpen) {            	
            	outputStream.write(CLOSE_EMPTY_ELEMENT);
                isStartElementOpen = false;
                return;
            }
            if (complexType) {
            	outputStream.write(CR);
                for (int x = 0; x < numberOfTabs; x++) {
                	outputStream.write(TAB);
                }
            } else {
                complexType = true;
            }
            super.endElement(xPathFragment, namespaceResolver);
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }

    /**
     * INTERNAL:
     */
    public void characters(String value) {
        super.characters(value);
        isLastEventText = true;
        complexType = false;
    }
    
    /**
     * INTERNAL:
     */
    public void cdata(String value) {
        //Format the CDATA on it's own line
        try {
            if(isStartElementOpen) {
            	outputStream.write(CLOSE_ELEMENT);
                isStartElementOpen = false;
            }
            outputStream.write(CR);
            for (int x = 0; x < numberOfTabs; x++) {
            	outputStream.write(TAB);
            }
            super.cdata(value);
            complexType=true;
        }catch(IOException ex) {
            throw XMLMarshalException.marshalException(ex);
        }
    }
    
    /**
     * Receive notification of a node.
     * @param node The Node to be added to the document
     * @param namespaceResolver The NamespaceResolver can be used to resolve the
     * namespace URI/prefix of the node
     */
    public void node(Node node, NamespaceResolver namespaceResolver) {
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
            Attr attr = (Attr) node;
            String resolverPfx = null;
            if (namespaceResolver != null) {
                resolverPfx = namespaceResolver.resolveNamespaceURI(attr.getNamespaceURI());
            }
            // If the namespace resolver contains a prefix for the attribute's URI,
            // use it instead of what is set on the attribute
            if (resolverPfx != null) {
                attribute(attr.getNamespaceURI(), XMLConstants.EMPTY_STRING, resolverPfx+XMLConstants.COLON+attr.getLocalName(), attr.getNodeValue());
            } else {
                attribute(attr.getNamespaceURI(), XMLConstants.EMPTY_STRING, attr.getName(), attr.getNodeValue());
                // May need to declare the URI locally
                if (attr.getNamespaceURI() != null) {
                    attribute(XMLConstants.XMLNS_URL, XMLConstants.EMPTY_STRING,XMLConstants.XMLNS + XMLConstants.COLON + attr.getPrefix(), attr.getNamespaceURI());
                }
            }
        } else {
            try {
                FormattedOutputStreamRecordContentHandler handler = new FormattedOutputStreamRecordContentHandler();
                XMLFragmentReader xfragReader = new XMLFragmentReader(namespaceResolver);
                xfragReader.setContentHandler(handler);
                xfragReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
                xfragReader.parse(node);
            } catch (SAXException sex) {
                throw XMLMarshalException.marshalException(sex);
            }
        }
    }
    
    /**
     * This class will typically be used in conjunction with an XMLFragmentReader.
     * The XMLFragmentReader will walk a given XMLFragment node and report events
     * to this class - the event's data is then written to the enclosing class' 
     * writer.
     * 
     * @see org.eclipse.persistence.internal.oxm.record.XMLFragmentReader
     * @see org.eclipse.persistence.oxm.record.WriterRecord.WriterRecordContentHandler
     */
    private class FormattedOutputStreamRecordContentHandler extends OutputStreamRecordContentHandler {
        // --------------------- CONTENTHANDLER METHODS --------------------- //
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            try {
            	if (isStartElementOpen) {
            		outputStream.write(CLOSE_ELEMENT);
            	}
                if (!isLastEventText) {
                	outputStream.write(CR);
                    for (int x = 0; x < numberOfTabs; x++) {
                    	outputStream.write(TAB);
                    }
                }
                outputStream.write(OPEN_START_ELEMENT);
                outputStream.write(qName.getBytes(XMLConstants.DEFAULT_XML_ENCODING));
                numberOfTabs++;
                isStartElementOpen = true;
                isLastEventText = false;
                // Handle attributes
                handleAttributes(atts);
                // Handle prefix mappings
                writePrefixMappings();
            } catch (IOException e) {
                throw XMLMarshalException.marshalException(e);
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            try {
                isLastEventText = false;
                numberOfTabs--;
                if (isStartElementOpen) {
                	outputStream.write(CLOSE_EMPTY_ELEMENT);
                    isStartElementOpen = false;
                    complexType = true;
                    return;
                }
                if (complexType) {
                	outputStream.write(CR);
                    for (int x = 0; x < numberOfTabs; x++) {
                    	outputStream.write(TAB);
                    }
                } else {
                    complexType = true;
                }
                super.endElement(namespaceURI, localName, qName);
            } catch (IOException e) {
                throw XMLMarshalException.marshalException(e);
            }
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
        	if (isProcessingCData) {
            	cdata(new String (ch, start, length));
        		return;
        	}
        	if (new String(ch).trim().length() == 0) {
        		return;
        	}        	
            super.characters(ch, start, length);
            isLastEventText = true;
            complexType = false;
        }
        
        // --------------------- LEXICALHANDLER METHODS --------------------- //
	public void comment(char[] ch, int start, int length) throws SAXException {
            try {
            	if (isStartElementOpen) {
            		outputStream.write(CLOSE_ELEMENT);
            		outputStream.write(CR);
                    isStartElementOpen = false;
                }
            	writeComment(ch, start, length);
                complexType = false;
            } catch (IOException e) {
            	throw XMLMarshalException.marshalException(e);
            }
	}
    }
}
