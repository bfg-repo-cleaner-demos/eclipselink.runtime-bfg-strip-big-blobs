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
package org.eclipse.persistence.internal.oxm.record;

import java.io.IOException;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.ext.LexicalHandler;
import javax.xml.validation.ValidatorHandler;

import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.oxm.mappings.XMLMapping;

/**
 * INTERNAL:
 * <p><b>Purpose:</b>Provide a wrapper for an org.xml.sax.XMLReader instance and define some extra
 * event methods that can be used by TopLink during the unmarshal process. These events are no ops
 * in this class, but may be overridden in subclasses.
 * <p><b>Responsibilities</b><ul>
 * <li>Wrap an instance of org.xml.sax.XMLReader and provide all the required API</li>
 * <li>Provide empty implementations of some callback methods that can be overridden in subclasses</li>
 * 
 *  @see org.eclipse.persistence.internal.oxm.record.DOMReader
 *  @author  mmacivor
 *  @since   release specific (what release of product did this appear in)
 */

public class XMLReader implements org.xml.sax.XMLReader {

    protected static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";
    public static final String NAMESPACE_PREFIXES_FEATURE = "http://xml.org/sax/features/namespace-prefixes";
    public static final String REPORT_IGNORED_ELEMENT_CONTENT_WHITESPACE_FEATURE = "http://java.sun.com/xml/schema/features/report-ignored-element-content-whitespace";

    private org.xml.sax.XMLReader reader;
    private boolean supportsLexicalHandler;
    private LexicalHandlerWrapper lexicalHandlerWrapper;
    protected ValidatorHandler validatorHandler;   

    public XMLReader(org.xml.sax.XMLReader internalReader) {
        this();
        this.reader = internalReader;
    }

    public XMLReader() {
        this.supportsLexicalHandler = true;
    }

    public ContentHandler getContentHandler () {
        return reader.getContentHandler();
    }

    public void setContentHandler (ContentHandler handler) {
        if(validatorHandler != null) {
            validatorHandler.setContentHandler(handler);
        } else {
            reader.setContentHandler(handler);
        }
    }

    public DTDHandler getDTDHandler () {
        return reader.getDTDHandler();
    }

    public void setDTDHandler (DTDHandler handler) {
        reader.setDTDHandler(handler);
    }

    public void setEntityResolver (EntityResolver resolver) {
        reader.setEntityResolver(resolver);
    }

    public EntityResolver getEntityResolver () {
        return reader.getEntityResolver();
    }

    public ErrorHandler getErrorHandler () {
        return reader.getErrorHandler();
    }

    public void setErrorHandler (ErrorHandler handler) {
        if(validatorHandler != null) {
            validatorHandler.setErrorHandler(handler);
        } else {
            reader.setErrorHandler(handler);
        }
    }

    public LexicalHandler getLexicalHandler() {
        if(supportsLexicalHandler) {
            try {
                return (LexicalHandler) reader.getProperty(LEXICAL_HANDLER_PROPERTY);
            } catch (SAXException e) {
                supportsLexicalHandler = false;
            }
        }
        return null;
    }

    public void setLexicalHandler(LexicalHandler lexicalHandler) {
        if(supportsLexicalHandler) {
                if(null == lexicalHandlerWrapper) {
                    try {
                        lexicalHandlerWrapper = new LexicalHandlerWrapper(lexicalHandler);
                        reader.setProperty(LEXICAL_HANDLER_PROPERTY, lexicalHandlerWrapper);
                    } catch (SAXException e) {
                        supportsLexicalHandler = false;
                    }
                } else {
                    lexicalHandlerWrapper.setLexicalHandler(lexicalHandler);
                }
        }
    }

    public boolean getFeature (String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        return reader.getFeature(name);
    }

    public void setFeature (String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        reader.setFeature(name, value);
    }

    public Object getProperty (String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if(LEXICAL_HANDLER_PROPERTY.equals(name)) {
            return getLexicalHandler();
        } else {
            return reader.getProperty(name);
        }
    }

    public void setProperty (String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if(LEXICAL_HANDLER_PROPERTY.equals(name)) {
            setLexicalHandler((LexicalHandler) value);
        } else {
            reader.setProperty(name, value);
        }
    }

    public void parse(InputSource input) throws IOException, SAXException {
        try {
            reader.parse(input);
        } catch(SAXNotSupportedException e) {
            String message = e.getMessage();
            if(message != null && message.contains("namespace-prefix")) {
                reader.setFeature(NAMESPACE_PREFIXES_FEATURE, false);
                reader.parse(input);
            } else {
                throw e;
            }
        }
    }

    public void parse (String systemId) throws IOException, SAXException {
        try {
            reader.parse(systemId);
        } catch(SAXNotSupportedException e) {
            String message = e.getMessage();
            if(message != null && message.contains("namespace-prefix")) {
                reader.setFeature(NAMESPACE_PREFIXES_FEATURE, false);
                reader.parse(systemId);
            } else {
                throw e;
            }
        }
    }

    public void setValidatorHandler(ValidatorHandler validatorHandler) {
        if(reader != null) {
            reader.setContentHandler(validatorHandler);
        }
        this.validatorHandler = validatorHandler;
        if(validatorHandler != null) {
            validatorHandler.setErrorHandler(getErrorHandler());
        }

    }
    
    public ValidatorHandler getValidatorHandler() {
        return this.validatorHandler;
    }
    
    public void newObjectEvent(Object object, Object parent, XMLMapping selfRecordMapping) {
        //no op in this class.
    }

    public Object getCurrentObject(AbstractSession session, XMLMapping selfRecordMapping) {
        return null;
    }

    /**
     * This call back mechanism provides an opportunity for the XMLReader to
     * provide an alternate conversion.  This optimization is currently only 
     * leveraged for properties annotated with @XmlInlineBinaryData.
     * @param characters The characters to be converted.
     * @param dataType The type to be converted to.
     * @return The converted value
     */
    public Object getValue(CharSequence characters, Class<?> dataType) {
        return null;
    }

    /**
     * Performance Optimization:
     * It is expensive to change the LexicalHandler on the underlying XMLReader
     * constantly through the setProperty(String, Object) mechanism.  So instead
     * the LexicalHandlerWrapper is set once this way, and the "real" 
     * LexicalHandler is changed on the LexicalHandlerWrapper.
     */
    private static class LexicalHandlerWrapper implements LexicalHandler {

        private LexicalHandler lexicalHandler;

        public LexicalHandlerWrapper(LexicalHandler lexicalHandler) {
            this.lexicalHandler = lexicalHandler;
        }

        public void setLexicalHandler(LexicalHandler lexicalHandler) {
            this.lexicalHandler = lexicalHandler;
        }

        public void comment(char[] ch, int start, int length) throws SAXException {
            if(null != lexicalHandler) {
                lexicalHandler.comment(ch, start, length);
            }
        }

        public void endCDATA() throws SAXException {
            if(null  != lexicalHandler) {
                lexicalHandler.endCDATA();
            }
        }

        public void endDTD() throws SAXException {
            if(null != lexicalHandler) {
                lexicalHandler.endDTD();
            }
        }

        public void endEntity(String name) throws SAXException {
            if(null != lexicalHandler) {
                lexicalHandler.endEntity(name);
            }
        }

        public void startCDATA() throws SAXException {
            if(null != lexicalHandler) {
                lexicalHandler.startCDATA();
            }
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            if(null != lexicalHandler) {
                lexicalHandler.startCDATA();
            }
        }

        public void startEntity(String name) throws SAXException {
            if(null != lexicalHandler) {
                lexicalHandler.startEntity(name);
            }
        }

    }

}