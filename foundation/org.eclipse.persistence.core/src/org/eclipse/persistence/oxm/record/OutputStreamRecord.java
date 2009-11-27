/***************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Denise Smith - November 2, 2009
 ******************************************************************************/  
package org.eclipse.persistence.oxm.record;

import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.oxm.record.XMLFragmentReader;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

/**
 * <p>Use this type of MarshalRecord when the marshal target is an OutputStream and the
 * XML should be not be formatted with carriage returns and indenting.  This type is only 
 * used if the encoding of the OutputStream is UTF-8</p>
 * <p><code>
 * XMLContext xmlContext = new XMLContext("session-name");<br>
 * XMLMarshaller xmlMarshaller = xmlContext.createMarshaller();<br>
 * OutputStreamRecord record = new OutputStreamRecord();<br>
 * record.setOutputStreamr(myOutputStream);<br>
 * xmlMarshaller.marshal(myObject, record);<br>
 * </code></p>
 * <p>If the marshal(OutputStream) and setFormattedOutput(false) method is called on
 * XMLMarshaller and the encoding is UTF-8, then the OutputStream is automatically wrapped 
 * in an OutputStream.</p>
 * <p><code>
 * XMLContext xmlContext = new XMLContext("session-name");<br>
 * XMLMarshaller xmlMarshaller = xmlContext.createMarshaller();<br>
 * xmlMarshaller xmlMarshaller.setFormattedOutput(false);<br>
 * xmlMarshaller.marshal(myObject, myOutputStream);<br>
 * </code></p>
 * @see org.eclipse.persistence.oxm.XMLMarshaller
 */
public class OutputStreamRecord extends MarshalRecord {
    protected static byte[] OPEN_XML_PI_AND_VERSION_ATTRIBUTE;
    protected static byte[] OPEN_ENCODING_ATTRIBUTE;
    protected static byte[] CLOSE_PI;
    protected static byte[] SPACE;
    protected static byte[] CR;
    protected static byte[] OPEN_ATTRIBUTE_VALUE;
    protected static byte[] CLOSE_ATTRIBUTE_VALUE;
    protected static byte[] OPEN_CDATA;
    protected static byte[] CLOSE_CDATA;
    protected static byte[] OPEN_COMMENT;
    protected static byte[] CLOSE_COMMENT;
    protected static byte[] OPEN_START_ELEMENT;
    protected static byte[] OPEN_END_ELEMENT;
    protected static byte[] CLOSE_ELEMENT;
    protected static byte[] CLOSE_EMPTY_ELEMENT;
    protected static byte[] AMP;
    protected static byte[] LT;
    protected static byte[] ENCODING;
    protected static byte[] EQUALS;
    protected static byte[] DOUBLE_QUOTE;
    
    static {
        try {
            OPEN_XML_PI_AND_VERSION_ATTRIBUTE = "<?xml version=\"".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            OPEN_ENCODING_ATTRIBUTE = " encoding=\"".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            CLOSE_PI = "?>".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            SPACE = " ".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            CR = Helper.cr().getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            OPEN_ATTRIBUTE_VALUE = "=\"".getBytes(XMLConstants.DEFAULT_XML_ENCODING);             
            CLOSE_ATTRIBUTE_VALUE = "\"".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            OPEN_CDATA = "<![CDATA[".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            CLOSE_CDATA = "]]>".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            OPEN_COMMENT = "<!--".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            CLOSE_COMMENT = "-->".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            OPEN_START_ELEMENT = "<".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            OPEN_END_ELEMENT = "</".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            CLOSE_ELEMENT = ">".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            CLOSE_EMPTY_ELEMENT = "/>".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            AMP = "&amp;".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            LT = "&lt;".getBytes(XMLConstants.DEFAULT_XML_ENCODING);
            ENCODING = XMLConstants.DEFAULT_XML_ENCODING.getBytes(XMLConstants.DEFAULT_XML_ENCODING);
        } catch (UnsupportedEncodingException e) {        	
        }
    }

    protected OutputStream outputStream;
    protected boolean isStartElementOpen = false;
    protected boolean isProcessingCData = false;

    /**
     * Return the OutputStream that the object will be marshalled to.
     * @return The marshal target.
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * Set the OutputStream that the object will be marshalled to.
     * @param writer The marshal target.
     */
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * INTERNAL:
     * override so we don't iterate over namespaces when startPrefixMapping doesn't do anything
     */
    public void startPrefixMappings(NamespaceResolver namespaceResolver) {        
    }
    
    /**
     * INTERNAL:
     */
    public void startDocument(String encoding, String version) {
        try {
            outputStream.write(OPEN_XML_PI_AND_VERSION_ATTRIBUTE);
            outputStream.write(version.getBytes(XMLConstants.DEFAULT_XML_ENCODING));            
            outputStream.write(CLOSE_ATTRIBUTE_VALUE);
            if (null != encoding) {
                outputStream.write(OPEN_ENCODING_ATTRIBUTE);
                outputStream.write(ENCODING);
                outputStream.write(CLOSE_ATTRIBUTE_VALUE);
            }
            outputStream.write(CLOSE_PI);
            outputStream.write(CR);
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }

    /**
     * INTERNAL:
     */
    public void endDocument() {}

    /**
     * INTERNAL:
     */
    public void openStartElement(XPathFragment xPathFragment, NamespaceResolver namespaceResolver) {
        super.openStartElement(xPathFragment, namespaceResolver);
        try {
            if (isStartElementOpen) {
                outputStream.write(CLOSE_ELEMENT);
            }
            isStartElementOpen = true;
            outputStream.write(OPEN_START_ELEMENT);
            outputStream.write(xPathFragment.getShortNameBytes());
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }

    /**
     * INTERNAL:
     */
    
    public void element(XPathFragment frag) {
        try {
            if (isStartElementOpen) {
                outputStream.write(CLOSE_ELEMENT);
                isStartElementOpen = false;
            }
            outputStream.write(OPEN_START_ELEMENT);
            outputStream.write(frag.getShortNameBytes());
            outputStream.write(CLOSE_EMPTY_ELEMENT);
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }

    /**
     * INTERNAL:
     */
    public void attribute(XPathFragment xPathFragment, NamespaceResolver namespaceResolver, String value) {
        attribute(null, xPathFragment.getLocalName(), xPathFragment.getShortName(), value);
    }

    /**
     * INTERNAL:
     */
    public void attribute(String namespaceURI, String localName, String qName, String value) {
        try {
            outputStream.write(SPACE);
            outputStream.write(qName.getBytes(XMLConstants.DEFAULT_XML_ENCODING));
            outputStream.write(OPEN_ATTRIBUTE_VALUE);
            writeValue(value);
            outputStream.write(CLOSE_ATTRIBUTE_VALUE);
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }

    /**
     * INTERNAL:
     */
    public void closeStartElement() {}

    /**
     * INTERNAL:
     */
    public void endElement(XPathFragment xPathFragment, NamespaceResolver namespaceResolver) {
        try {
            if (isStartElementOpen) {
                outputStream.write(CLOSE_EMPTY_ELEMENT);
                isStartElementOpen = false;
            } else {
                outputStream.write(OPEN_END_ELEMENT);
                outputStream.write(xPathFragment.getShortNameBytes());
                outputStream.write(CLOSE_ELEMENT);
            }
            isStartElementOpen = false;
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }

    /**
     * INTERNAL:
     */
    public void characters(String value) {
        try {
            if (isStartElementOpen) {
                isStartElementOpen = false;
                outputStream.write(CLOSE_ELEMENT);
            }
            writeValue(value);
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }
    
    /**
     * INTERNAL:
     */
    public void cdata(String value) {
        try {
            if(isStartElementOpen) {
                isStartElementOpen = false;
                outputStream.write(CLOSE_ELEMENT);
            }
            outputStream.write(OPEN_CDATA);
            outputStream.write(value.getBytes(XMLConstants.DEFAULT_XML_ENCODING));
            outputStream.write(CLOSE_CDATA);
        } catch(IOException e) {
            throw XMLMarshalException.marshalException(e);
        }
    }
    
    /**
     * INTERNAL:
     */
    protected void writeValue(String value) {
        try {
            char[] chars = value.toCharArray();
            for (int x = 0, charsSize = chars.length; x < charsSize; x++) {
                char character = chars[x];
                switch (character) {
                case '&': {
                    outputStream.write(AMP);
                    break;
                }
                case '<': {
                    outputStream.write(LT);
                    break;
                }
                default:
                    outputStream.write(character);
                }
            }
        } catch (IOException e) {
            throw XMLMarshalException.marshalException(e);
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
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            characters(node.getNodeValue());
        } else {
            try {
            	OutputStreamRecordContentHandler handler = new OutputStreamRecordContentHandler();
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
     */
    protected class OutputStreamRecordContentHandler implements ContentHandler, LexicalHandler {
        Map<String, String> prefixMappings;
        
        OutputStreamRecordContentHandler() {
            prefixMappings = new HashMap<String, String>();
        }
        
        // --------------------- CONTENTHANDLER METHODS --------------------- //
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            try {
                if (isStartElementOpen) {
                    outputStream.write(CLOSE_ELEMENT);
                }

                outputStream.write(OPEN_START_ELEMENT);
                outputStream.write(qName.getBytes(XMLConstants.DEFAULT_XML_ENCODING));
                isStartElementOpen = true;
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
                if (isStartElementOpen) {
                    outputStream.write(CLOSE_EMPTY_ELEMENT);
                } else {
                        outputStream.write(OPEN_END_ELEMENT);
                        outputStream.write(qName.getBytes(XMLConstants.DEFAULT_XML_ENCODING));
                        outputStream.write(CLOSE_ELEMENT);
                }
                isStartElementOpen = false;
            } catch (IOException e) {
                throw XMLMarshalException.marshalException(e);
            }
        }
        
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            String namespaceUri = getNamespaceResolver().resolveNamespacePrefix(prefix);
            if(namespaceUri == null || !namespaceUri.equals(uri)) {
                prefixMappings.put(prefix, uri);
            }
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (isProcessingCData) {
                cdata(new String (ch, start, length));
                return;
            }

            if (isStartElementOpen) {
                try {
                    outputStream.write(CLOSE_ELEMENT);
                    isStartElementOpen = false;
                } catch (IOException e) {
                    throw XMLMarshalException.marshalException(e);
                }
            }
        	writeValue(new String(ch, start, length));
        }

        // --------------------- LEXICALHANDLER METHODS --------------------- //
        public void comment(char[] ch, int start, int length) throws SAXException {
            try {
                if (isStartElementOpen) {
                    outputStream.write(CLOSE_ELEMENT);
                    isStartElementOpen = false;
                }
                writeComment(ch, start, length);
            } catch (IOException e) {
                throw XMLMarshalException.marshalException(e);
            }
        }

		public void startCDATA() throws SAXException {
			isProcessingCData = true;
		}
		
		public void endCDATA() throws SAXException {
			isProcessingCData = false;
		}
        
        // --------------------- CONVENIENCE METHODS --------------------- //
        protected void writePrefixMappings() {
            try {
                if (!prefixMappings.isEmpty()) {
                    for (java.util.Iterator<String> keys = prefixMappings.keySet().iterator(); keys.hasNext();) {
                        String prefix = keys.next();
                        outputStream.write(SPACE);
                        outputStream.write(XMLConstants.XMLNS.getBytes(XMLConstants.DEFAULT_XML_ENCODING));
                        if(prefix.length() > 0) {
                            outputStream.write(XMLConstants.COLON);
                            outputStream.write(prefix.getBytes(XMLConstants.DEFAULT_XML_ENCODING));
                        }                        
                        outputStream.write(OPEN_ATTRIBUTE_VALUE);
                        outputStream.write(prefixMappings.get(prefix).getBytes(XMLConstants.DEFAULT_XML_ENCODING));
                        outputStream.write(CLOSE_ATTRIBUTE_VALUE);
                    }
                    prefixMappings.clear();
                }
            } catch (IOException e) {
                throw XMLMarshalException.marshalException(e);
            }
        }
        
        protected void handleAttributes(Attributes atts) {
            for (int i=0, attsLength = atts.getLength(); i<attsLength; i++) {
            	String qName = atts.getQName(i);
                if((qName != null && (qName.startsWith(XMLConstants.XMLNS + XMLConstants.COLON) || qName.equals(XMLConstants.XMLNS)))) {
                    continue;
                }
                attribute(atts.getURI(i), atts.getLocalName(i), qName, atts.getValue(i));
            }
        }
        
        protected void writeComment(char[] chars, int start, int length) {
            try {
                outputStream.write(OPEN_COMMENT);
                for (int x = start; x < length; x++) {
                    outputStream.write(chars[x]);
                }
                outputStream.write(CLOSE_COMMENT);
            } catch (IOException e) {
                throw XMLMarshalException.marshalException(e);
            }
        }

        protected void writeCharacters(char[] chars, int start, int length) {
            try {
                for (int x = start; x < length; x++) {
                    outputStream.write(chars[x]);
                }
            } catch (IOException e) {
                throw XMLMarshalException.marshalException(e);
            }
        }
        // --------------- SATISFY CONTENTHANDLER INTERFACE --------------- //
        public void endPrefixMapping(String prefix) throws SAXException {}
        public void processingInstruction(String target, String data) throws SAXException {}
        public void setDocumentLocator(Locator locator) {}
        public void startDocument() throws SAXException {}
        public void endDocument() throws SAXException {}
        public void skippedEntity(String name) throws SAXException {}
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}

        // --------------- SATISFY LEXICALHANDLER INTERFACE --------------- //
        public void startEntity(String name) throws SAXException {}
		public void endEntity(String name) throws SAXException {}
		public void startDTD(String name, String publicId, String systemId) throws SAXException {}
		public void endDTD() throws SAXException {}
    }

}
