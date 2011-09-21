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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.internal.oxm.XMLObjectBuilder;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLRoot;
import org.eclipse.persistence.oxm.XMLUnmarshaller;
import org.eclipse.persistence.oxm.record.DOMRecord;
import org.eclipse.persistence.platform.xml.SAXDocumentBuilder;
import org.eclipse.persistence.platform.xml.XMLParser;
import org.eclipse.persistence.platform.xml.XMLPlatformException;
import org.eclipse.persistence.platform.xml.XMLPlatformFactory;
import org.eclipse.persistence.queries.ReadObjectQuery;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * INTERNAL:
 * <p><b>Purpose:</b>Provide an implementation of PlatformUnmarshaller that makes use of the DOM
 * unmarshal code. Used by the DOMPlatform
 * <p><b>Responsibilities:</b><ul>
 * <li>Implement the required unmarshal methods from platform unmarshaller</li>
 * <li>Perform xml-to-object conversions</li>
 * </ul>
 * @author bdoughan
 * @see org.eclipse.persistence.oxm.platform.DOMPlatform
 *
 */
public class DOMUnmarshaller implements PlatformUnmarshaller {
    private XMLParser parser;
    private XMLUnmarshaller xmlUnmarshaller;
    private boolean isResultAlwaysXMLRoot;

    public DOMUnmarshaller(XMLUnmarshaller xmlUnmarshaller, Map<String, Boolean> parserFeatures) {
        super();
        if(null == parserFeatures) {
            parser = XMLPlatformFactory.getInstance().getXMLPlatform().newXMLParser();
        } else {
            parser = XMLPlatformFactory.getInstance().getXMLPlatform().newXMLParser(parserFeatures);
        }
        parser.setNamespaceAware(true);
        parser.setValidationMode(XMLParser.NONVALIDATING);
        this.xmlUnmarshaller = xmlUnmarshaller;
    }

    public EntityResolver getEntityResolver() {
        return parser.getEntityResolver();
    }

    public void setEntityResolver(EntityResolver entityResolver) {
        parser.setEntityResolver(entityResolver);
    }

    public ErrorHandler getErrorHandler() {
        return parser.getErrorHandler();
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        parser.setErrorHandler(errorHandler);
    }

    public int getValidationMode() {
        return parser.getValidationMode();
    }

    public void setValidationMode(int validationMode) {
        parser.setValidationMode(validationMode);
    }

    public void setWhitespacePreserving(boolean isWhitespacePreserving) {
        parser.setWhitespacePreserving(isWhitespacePreserving);
    }

    public void setSchemas(Object[] schemas) {
        try {
            parser.setXMLSchemas(schemas);
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.errorSettingSchemas(e, schemas);
        }
    }
    
    public void setSchema(Schema schema) {
        parser.setXMLSchema(schema);
    }
    
    public Schema getSchema() {
        Schema schema = null;
        try {
            schema = parser.getXMLSchema();
        } catch(UnsupportedOperationException ex) {
            //if this parser doesn't support this API, just return null for the schema
        }
        return schema;
    }

    public Object unmarshal(File file) {
        try {
            Document document = null;
            document = parser.parse(file);
            return xmlToObject(new DOMRecord(document));
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(File file, Class clazz) {
        try {
            Document document = null;
            document = parser.parse(file);
            return xmlToObject(new DOMRecord(document), clazz);
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(InputStream inputStream) {
        try {
            Document document = null;
            document = parser.parse(inputStream);
            return xmlToObject(new DOMRecord(document));
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(InputStream inputStream, Class clazz) {
        try {
            Document document = null;
            document = parser.parse(inputStream);
            return xmlToObject(new DOMRecord(document), clazz);
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(InputSource inputSource) {
        try {
            Document document = null;
            document = parser.parse(inputSource);
            return xmlToObject(new DOMRecord(document));
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(InputSource inputSource, Class clazz) {
        try {
            Document document = null;
            document = parser.parse(inputSource);
            return xmlToObject(new DOMRecord(document), clazz);
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(Node node) {
        Element element = null;
        switch (node.getNodeType()) {
        case Node.DOCUMENT_NODE: {
            element = ((Document) node).getDocumentElement();
            break;
        }
        case Node.ELEMENT_NODE: {
            element = (Element) node;
            break;
        }
        default:
            throw XMLMarshalException.unmarshalException();
        }
        return xmlToObject(new DOMRecord(element));
    }

    public Object unmarshal(Node node, Class clazz) {
        Element element = null;
        switch (node.getNodeType()) {
        case Node.DOCUMENT_NODE: {
            element = ((Document) node).getDocumentElement();
            break;
        }
        case Node.ELEMENT_NODE: {
            element = (Element) node;
            break;
        }
        default:
            throw XMLMarshalException.unmarshalException();
        }
        return xmlToObject(new DOMRecord(element), clazz);
    }

    public Object unmarshal(Reader reader) {
        try {
            Document document = null;
            document = parser.parse(reader);
            return xmlToObject(new DOMRecord(document));
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(Reader reader, Class clazz) {
        try {
            Document document = null;
            document = parser.parse(reader);
            return xmlToObject(new DOMRecord(document), clazz);
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(Source source) {
        try {
            Document document = null;
            document = parser.parse(source);
            return xmlToObject(new DOMRecord(document));
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(Source source, Class clazz) {
        try {
            Document document = null;
            document = parser.parse(source);
            return xmlToObject(new DOMRecord(document), clazz);
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(URL url) {
        try {
            Document document = null;
            document = parser.parse(url);
            return xmlToObject(new DOMRecord(document));
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(URL url, Class clazz) {
        try {
            Document document = null;
            document = parser.parse(url);
            return xmlToObject(new DOMRecord(document), clazz);
        } catch (XMLPlatformException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(XMLReader xmlReader, InputSource inputSource) {
        try {
            SAXDocumentBuilder saxDocumentBuilder = new SAXDocumentBuilder();
            xmlReader.setContentHandler(saxDocumentBuilder);
            xmlReader.parse(inputSource);
            return xmlToObject(new DOMRecord(saxDocumentBuilder.getDocument()));
        } catch(IOException e) {
            throw XMLMarshalException.unmarshalException(e);
        } catch(SAXException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    public Object unmarshal(XMLReader xmlReader, InputSource inputSource, Class clazz) {
        try {
            SAXDocumentBuilder saxDocumentBuilder = new SAXDocumentBuilder();
            xmlReader.setContentHandler(saxDocumentBuilder);
            xmlReader.parse(inputSource);
            return xmlToObject(new DOMRecord(saxDocumentBuilder.getDocument()), clazz);
        } catch(IOException e) {
            throw XMLMarshalException.unmarshalException(e);
        } catch(SAXException e) {
            throw XMLMarshalException.unmarshalException(e);
        } finally {
        	xmlUnmarshaller.getStringBuffer().reset();
        }
    }

    /**
     * INTERNAL: Return the descriptor for the document.
     */
    protected XMLDescriptor getDescriptor(DOMRecord xmlRecord) throws XMLMarshalException {
    	XMLDescriptor xmlDescriptor = null;
    	
    	XMLContext xmlContext = xmlUnmarshaller.getXMLContext();        
        // Try to find a descriptor based on the schema type
        String type = ((Element) xmlRecord.getDOM()).getAttributeNS(XMLConstants.SCHEMA_INSTANCE_URL, "type");
        if (null != type) {
            XPathFragment typeFragment = new XPathFragment(type);
            String namespaceURI = xmlRecord.resolveNamespacePrefix(typeFragment.getPrefix());
            typeFragment.setNamespaceURI(namespaceURI);
            xmlDescriptor = xmlContext.getDescriptorByGlobalType(typeFragment);
        }
                    
        if (null == xmlDescriptor) {
        	QName rootQName = new QName(xmlRecord.getNamespaceURI(), xmlRecord.getLocalName());
        	xmlDescriptor = xmlContext.getDescriptor(rootQName);
        	if (null == xmlDescriptor) {
                throw XMLMarshalException.noDescriptorWithMatchingRootElement(rootQName.toString());
            }
        }
        
        return xmlDescriptor;
    }

    /**
     * INTERNAL: Find the Descriptor corresponding to the context node of the
     * XMLRecord, and then convert the XMLRecord to an instance of the
     * corresponding object.
     * 
     * @param xmlRecord
     *            The XMLRecord to unmarshal from
     * @return the object which resulted from unmarshalling the given XMLRecord
     * @throws XMLMarshalException
     *             if an error occurred during unmarshalling
     */
    public Object xmlToObject(DOMRecord xmlRecord) throws XMLMarshalException {
        XMLDescriptor xmlDescriptor = getDescriptor(xmlRecord);
        return xmlToObject(xmlRecord, xmlDescriptor.getJavaClass());
    }

    /**
     * INTERNAL: Convert the Oracle XMLDocument to the reference-class.
     */
    public Object xmlToObject(DOMRecord xmlRow, Class referenceClass) throws XMLMarshalException {
    	try{
            //Try to get the Encoding and Version from DOM3 APIs if available
            String xmlEncoding = "UTF-8";
            String xmlVersion = "1.0";

            try {
                Method getEncoding = PrivilegedAccessHelper.getMethod(xmlRow.getDocument().getClass(), "getXmlEncoding", new Class[] {}, true);
                Method getVersion = PrivilegedAccessHelper.getMethod(xmlRow.getDocument().getClass(), "getXmlVersion", new Class[] {}, true);
                xmlEncoding = (String) PrivilegedAccessHelper.invokeMethod(getEncoding, xmlRow.getDocument(), new Object[] {});
                xmlVersion = (String) PrivilegedAccessHelper.invokeMethod(getVersion, xmlRow.getDocument(), new Object[] {});
            } catch (Exception ex) {
                //if the methods aren't available, then just use the default values
            }

            XMLContext xmlContext = xmlUnmarshaller.getXMLContext();

            // handle case where the reference class is a primitive wrapper - in
            // this case, we need to use the conversion manager to convert the 
            // node's value to the primitive wrapper class, then create, 
            // populate and return an XMLRoot
            if (XMLConversionManager.getDefaultJavaTypes().get(referenceClass) != null ||ClassConstants.XML_GREGORIAN_CALENDAR.isAssignableFrom(referenceClass)
         	    ||ClassConstants.DURATION.isAssignableFrom(referenceClass)){
                // we're assuming that since we're unmarshalling to a primitive
                // wrapper, the root element has a single text node
                Object nodeVal;
                try {
                    Text rootTxt = (Text) xmlRow.getDOM().getFirstChild();
                    nodeVal = rootTxt.getNodeValue();
                } catch (Exception ex) {
                    // here, either the root element doesn't have a text node as a
                    // first child, or there is no first child at all - in any case,
                    // try converting null
                    nodeVal = null;
                }
  
                Object obj = ((XMLConversionManager) xmlContext.getSession(0).getDatasourcePlatform().getConversionManager()).convertObject(nodeVal, referenceClass);
                XMLRoot xmlRoot = new XMLRoot();
                xmlRoot.setObject(obj);
                String lName = xmlRow.getDOM().getLocalName();
                if (lName == null) {
                    lName = xmlRow.getDOM().getNodeName();
                }
                xmlRoot.setLocalName(lName);
                xmlRoot.setNamespaceURI(xmlRow.getDOM().getNamespaceURI());
                xmlRoot.setEncoding(xmlEncoding);
                xmlRoot.setVersion(xmlVersion);
                return xmlRoot;
            }

            // for XMLObjectReferenceMappings we need a non-shared cache, so
            // try and get a Unit Of Work from the XMLContext
            AbstractSession readSession = xmlContext.getReadSession(referenceClass);

            XMLDescriptor descriptor = (XMLDescriptor) readSession.getDescriptor(referenceClass);
            if (descriptor == null) {
                throw XMLMarshalException.descriptorNotFoundInProject(referenceClass.getName());
            }

            Object object = null;
            if(null == xmlRow.getDOM().getAttributes().getNamedItemNS(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_NIL_ATTRIBUTE)) {
                xmlRow.setUnmarshaller(xmlUnmarshaller);
                xmlRow.setDocPresPolicy(xmlContext.getDocumentPreservationPolicy(readSession));
                XMLObjectBuilder objectBuilder = (XMLObjectBuilder) descriptor.getObjectBuilder();

                ReadObjectQuery query = new ReadObjectQuery();
                query.setReferenceClass(referenceClass);
                query.setSession(readSession);
                object = objectBuilder.buildObject(query, xmlRow, null);

                // resolve mapping references
                xmlUnmarshaller.resolveReferences(readSession);
            }
  
            String elementNamespaceUri = xmlRow.getDOM().getNamespaceURI();
            String elementLocalName = xmlRow.getDOM().getLocalName();
            if (elementLocalName == null) {
                elementLocalName = xmlRow.getDOM().getNodeName();
            }
            String elementPrefix = xmlRow.getDOM().getPrefix();
           return descriptor.wrapObjectInXMLRoot(object, elementNamespaceUri, elementLocalName, elementPrefix, xmlEncoding, xmlVersion, this.isResultAlwaysXMLRoot);
    	}finally{    		
            xmlUnmarshaller.getStringBuffer().reset();           
    	}
    }

    public boolean isResultAlwaysXMLRoot() {
        return this.isResultAlwaysXMLRoot;
    }

    public void setResultAlwaysXMLRoot(boolean alwaysReturnRoot) {
        this.isResultAlwaysXMLRoot = alwaysReturnRoot;
    }
}
