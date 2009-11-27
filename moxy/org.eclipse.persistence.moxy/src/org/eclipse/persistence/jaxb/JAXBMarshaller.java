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
package org.eclipse.persistence.jaxb;

import java.io.OutputStream;
import java.io.Writer;
import java.io.File;
import java.util.HashMap;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;

import java.lang.reflect.Type;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLMarshaller;
import org.eclipse.persistence.oxm.XMLRoot;
import org.eclipse.persistence.oxm.record.XMLEventWriterRecord;
import org.eclipse.persistence.oxm.record.XMLStreamWriterRecord;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.jaxb.many.ManyValue;
import org.eclipse.persistence.internal.jaxb.WrappedValue;

import org.eclipse.persistence.jaxb.attachment.*;

/**
 * INTERNAL:
 * <p><b>Purpose:</b>To Provide an implementation of the JAXB 2.0 Marshaller Interface
 * <p><b>Responsibilities:</b>
 * <ul>
 * <li>Provide a JAXB wrapper on the XMLMarshaller API</li>
 * <li>Perform Object to XML Conversions</li>
 * </ul>
 * <p>This implementation of the JAXB 2.0 Marshaller interface provides the required functionality
 * by acting as a thin wrapper on the existing XMLMarshaller API.
 *
 * @author mmacivor
 * @since Oracle TopLink 11.1.1.0.0
 * @see javax.xml.bind.Marshaller
 * @see org.eclipse.persistence.oxm.XMLMarshaller
 */

public class JAXBMarshaller implements javax.xml.bind.Marshaller {
	private ValidationEventHandler validationEventHandler;
	private XMLMarshaller xmlMarshaller;
	private JAXBContext jaxbContext;
    public static final String XML_JAVATYPE_ADAPTERS = "xml-javatype-adapters";
    private HashMap<QName, Class> qNameToGeneratedClasses;

	/**
	 * This constructor initializes various settings on the XML marshaller, and
	 * stores the provided JAXBIntrospector instance for usage in marshal()
	 * calls.
	 * 
	 * @param newXMLMarshaller
	 * @param newIntrospector
	 */
	public JAXBMarshaller(XMLMarshaller newXMLMarshaller, JAXBIntrospector newIntrospector) {
		super();
		validationEventHandler = new DefaultValidationEventHandler();
		xmlMarshaller = newXMLMarshaller;
		xmlMarshaller.setEncoding("UTF-8");
		xmlMarshaller.setFormattedOutput(false);
		JAXBMarshalListener listener = new JAXBMarshalListener(this);
		xmlMarshaller.setMarshalListener(listener);
		
	}

	/**
	 * Create an instance of XMLRoot populated from the contents of the provided
	 * JAXBElement. XMLRoot will be used to hold the contents of the JAXBElement
	 * while the marshal operation is performed by TopLink OXM. This will avoid
	 * adding any runtime dependencies to TopLink.
	 * 
	 * @param elt
	 * @return
	 */
	private XMLRoot createXMLRootFromJAXBElement(JAXBElement elt) {
		// create an XMLRoot to hand into the marshaller
		XMLRoot xmlroot = new XMLRoot();
		Object objectValue = elt.getValue();
		xmlroot.setObject(objectValue);
		QName qname = elt.getName();
		xmlroot.setLocalName(qname.getLocalPart());
		xmlroot.setNamespaceURI(qname.getNamespaceURI());
		xmlroot.setDeclaredType(elt.getDeclaredType());
		if(elt.getDeclaredType() == ClassConstants.ABYTE || elt.getDeclaredType() == ClassConstants.APBYTE || elt.getDeclaredType().getCanonicalName().equals("javax.activation.DataHandler")) {
			xmlroot.setSchemaType(XMLConstants.BASE_64_BINARY_QNAME);
			//need a binary data mapping so need to wrap
			Class generatedClass = getClassToGeneratedClasses().get(elt.getDeclaredType().getCanonicalName());
			if(generatedClass != null && WrappedValue.class.isAssignableFrom(generatedClass)) {
				ClassDescriptor desc = xmlMarshaller.getXMLContext().getSession(generatedClass).getDescriptor(generatedClass);
				Object newObject = desc.getInstantiationPolicy().buildNewInstance();
				((WrappedValue)newObject).setValue(objectValue);
				xmlroot.setObject(newObject);
				return xmlroot;
			}   			
		}else {
		    xmlroot.setSchemaType((QName)org.eclipse.persistence.internal.oxm.XMLConversionManager.getDefaultJavaTypes().get(elt.getDeclaredType()));
		}
		
		if(qNameToGeneratedClasses != null){		
			Class theClass = qNameToGeneratedClasses.get(qname);			
			if(theClass != null && WrappedValue.class.isAssignableFrom(theClass)) {
				ClassDescriptor desc = xmlMarshaller.getXMLContext().getSession(theClass).getDescriptor(theClass);
				Object newObject = desc.getInstantiationPolicy().buildNewInstance();
				((WrappedValue)newObject).setValue(objectValue);
				xmlroot.setObject(newObject);
				return xmlroot;
			}   
		}
					
		Class generatedClass = null;				
		
		if(elt.getDeclaredType() != null && elt.getDeclaredType().isArray()){
			if(jaxbContext.getArrayClassesToGeneratedClasses() != null){
				generatedClass = jaxbContext.getArrayClassesToGeneratedClasses().get(elt.getDeclaredType().getCanonicalName());
			}
		}else if(elt instanceof JAXBTypeElement){			 
			Type objectType = ((JAXBTypeElement)elt).getType();
			generatedClass = jaxbContext.getCollectionClassesToGeneratedClasses().get(objectType);
		} 
			
		if(generatedClass != null ) {
			ClassDescriptor desc = xmlMarshaller.getXMLContext().getSession(generatedClass).getDescriptor(generatedClass);
			Object newObject = desc.getInstantiationPolicy().buildNewInstance();			
			((ManyValue)newObject).setItem(objectValue);				
			xmlroot.setObject(newObject);
		}		
		
		return xmlroot;
	}
	
	public XmlAdapter getAdapter(Class javaClass) {
        HashMap result = (HashMap) xmlMarshaller.getProperty(XML_JAVATYPE_ADAPTERS);
        if (result == null) {
            return null;
        }
        return (XmlAdapter) result.get(javaClass);
	}
	
	public AttachmentMarshaller getAttachmentMarshaller() {
		return ((AttachmentMarshallerAdapter)xmlMarshaller.getAttachmentMarshaller()).getAttachmentMarshaller();
	}

	public ValidationEventHandler getEventHandler() throws JAXBException {
		return validationEventHandler;
	}

	public Marshaller.Listener getListener() {
		return ((JAXBMarshalListener) xmlMarshaller.getMarshalListener()).getListener();
	}
	
	public Node getNode(Object object) throws JAXBException {
		throw new UnsupportedOperationException();
	}

	public Object getProperty(String key) throws PropertyException {
		if (key == null) {
			throw new IllegalArgumentException();
		} else if (JAXB_FORMATTED_OUTPUT.equals(key)) {
			return new Boolean(xmlMarshaller.isFormattedOutput());
		} else if (JAXB_ENCODING.equals(key)) {
			return xmlMarshaller.getEncoding();
		} else if (JAXB_SCHEMA_LOCATION.equals(key)) {
			return xmlMarshaller.getSchemaLocation();
		} else if (JAXB_NO_NAMESPACE_SCHEMA_LOCATION.equals(key)) {
			return xmlMarshaller.getNoNamespaceSchemaLocation();
		} else if (XMLConstants.JAXB_FRAGMENT.equals(key)) {
			return new Boolean(xmlMarshaller.isFragment());
		}
		throw new PropertyException("Unsupported Property");
	}

	public Schema getSchema() {
		return xmlMarshaller.getSchema();
	}

	public void marshal(Object object, ContentHandler contentHandler) throws JAXBException {
        if (object == null || contentHandler == null) {
            throw new IllegalArgumentException();
        }
        if (object instanceof JAXBElement) {
            // use the JAXBElement's properties to populate an XMLRoot
			object = createXMLRootFromJAXBElement((JAXBElement) object);
		}
		try {
			xmlMarshaller.marshal(object, contentHandler);
		} catch (Exception e) {
			throw new MarshalException(e);
		}
	}

	public void marshal(Object object, XMLEventWriter eventWriter) throws JAXBException {
        if (object == null || eventWriter == null) {
            throw new IllegalArgumentException();
        }
        if (object instanceof JAXBElement) {
            // use the JAXBElement's properties to populate an XMLRoot
            object = createXMLRootFromJAXBElement((JAXBElement) object);
        }
        try {
            XMLEventWriterRecord record = new XMLEventWriterRecord(eventWriter);
            record.setMarshaller(this.xmlMarshaller);
            this.xmlMarshaller.marshal(object, record);
        } catch (Exception ex) {
            throw new MarshalException(ex);
        }
    }

    /**
     * Marshal the object based on the binding metadata associated with the
     * TypeMappingInfo.
     */
    public void marshal(Object object, XMLEventWriter eventWriter, TypeMappingInfo type) throws JAXBException {
        marshal(object, eventWriter);
    }

	public void marshal(Object object, Node node) throws JAXBException {
        if (object == null || node == null) {
            throw new IllegalArgumentException();
        }
		if (object instanceof JAXBElement) {
			// use the JAXBElement's properties to populate an XMLRoot
			object = createXMLRootFromJAXBElement((JAXBElement) object);
		}
		try {
			xmlMarshaller.marshal(object, node);
		} catch (Exception e) {
			throw new MarshalException(e);
		}
	}
	
	public void marshal(Object object, OutputStream outputStream) throws JAXBException {
        if (object == null || outputStream == null) {
            throw new IllegalArgumentException();
        }
		if (object instanceof JAXBElement) {
			// use the JAXBElement's properties to populate an XMLRoot
			object = createXMLRootFromJAXBElement((JAXBElement) object);
		}
		try {
			xmlMarshaller.marshal(object, outputStream);
		} catch (Exception e) {
			throw new MarshalException(e);
		}
	}

	public void marshal(Object object, File file) throws JAXBException {
		try {
			java.io.FileWriter writer = new java.io.FileWriter(file);
			marshal(object, writer);
		} catch(Exception ex) {
			throw new MarshalException(ex);
		}
	}
	public void marshal(Object object, Result result) throws JAXBException {
        if (object == null || result == null) {
            throw new IllegalArgumentException();
        }
		if (object instanceof JAXBElement) {
			// use the JAXBElement's properties to populate an XMLRoot
			object = createXMLRootFromJAXBElement((JAXBElement) object);
		}
		try {
			xmlMarshaller.marshal(object, result);
		} catch (Exception e) {
			throw new MarshalException(e);
		}
	}

    /**
     * Marshal the object based on the binding metadata associated with the
     * TypeMappingInfo.
     */
    public void marshal(Object object, Result result, TypeMappingInfo type) throws JAXBException {
        marshal(object, result);
    }

	public void marshal(Object object, XMLStreamWriter streamWriter) throws JAXBException {
        if (object == null || streamWriter == null) {
            throw new IllegalArgumentException();
        }
		if (object instanceof JAXBElement) {
			// use the JAXBElement's properties to populate an XMLRoot
			object = createXMLRootFromJAXBElement((JAXBElement) object);
		}
	    try {
	        XMLStreamWriterRecord record = new XMLStreamWriterRecord(streamWriter);
	        record.setMarshaller(this.xmlMarshaller);
	        this.xmlMarshaller.marshal(object, record);
	    } catch (Exception ex) {
	        throw new MarshalException(ex);
	    }
	}

    /**
     * Marshal the object based on the binding metadata associated with the
     * TypeMappingInfo.
     */
    public void marshal(Object object, XMLStreamWriter streamWriter, TypeMappingInfo type) throws JAXBException {
        marshal(object, streamWriter);
    }

	public void marshal(Object object, Writer writer) throws JAXBException {
        if (object == null || writer == null) {
            throw new IllegalArgumentException();
        }
		if (object instanceof JAXBElement) {
			// use the JAXBElement's properties to populate an XMLRoot
			object = createXMLRootFromJAXBElement((JAXBElement) object);
		}
		try {
			xmlMarshaller.marshal(object, writer);
		} catch (Exception e) {
			throw new MarshalException(e);
		}
	}

	public void setAdapter(Class javaClass, XmlAdapter adapter) {
        HashMap result = (HashMap) xmlMarshaller.getProperty(XML_JAVATYPE_ADAPTERS);
        if (result == null) {
            result = new HashMap();
            xmlMarshaller.getProperties().put(XML_JAVATYPE_ADAPTERS, result);
        }
        result.put(javaClass, adapter);
	}

	public void setAdapter(XmlAdapter adapter) {
        setAdapter(adapter.getClass(), adapter);
	}

	public void setAttachmentMarshaller(AttachmentMarshaller attachmentMarshaller) {
		xmlMarshaller.setAttachmentMarshaller(new AttachmentMarshallerAdapter(attachmentMarshaller));
	}

	public void setEventHandler(ValidationEventHandler newValidationEventHandler) throws JAXBException {
		if (null == newValidationEventHandler) {
			validationEventHandler = new DefaultValidationEventHandler();
		} else {
			validationEventHandler = newValidationEventHandler;
		}
	}
	
	public void setListener(Marshaller.Listener listener) {
		((JAXBMarshalListener) xmlMarshaller.getMarshalListener()).setListener(listener);
	}

	public void setMarshalCallbacks(java.util.HashMap callbacks) {
		((JAXBMarshalListener) xmlMarshaller.getMarshalListener()).setClassBasedMarshalEvents(callbacks);
	}

	public void setProperty(String key, Object value) throws PropertyException {
		try {
			if (key == null) {
				throw new IllegalArgumentException();
			} else if (JAXB_FORMATTED_OUTPUT.equals(key)) {
				Boolean formattedOutput = (Boolean) value;
				xmlMarshaller.setFormattedOutput(formattedOutput.booleanValue());
			} else if (JAXB_ENCODING.equals(key)) {
				xmlMarshaller.setEncoding((String) value);
			} else if (JAXB_SCHEMA_LOCATION.equals(key)) {
				xmlMarshaller.setSchemaLocation((String) value);
			} else if (JAXB_NO_NAMESPACE_SCHEMA_LOCATION.equals(key)) {
				xmlMarshaller.setNoNamespaceSchemaLocation((String) value);
			} else if (XMLConstants.JAXB_FRAGMENT.equals(key)) {
				Boolean fragment = (Boolean) value;
				xmlMarshaller.setFragment(fragment.booleanValue());
			} else {
				throw new PropertyException(key, value);
			}
		} catch (ClassCastException exception) {
			throw new PropertyException(key, exception);
		}
	}

	public void setSchema(Schema schema) {
	    this.xmlMarshaller.setSchema(schema);
	}

	public void setQNameToGeneratedClasses(HashMap<QName, Class> qNameToClass) {
	    this.qNameToGeneratedClasses = qNameToClass;
	}
	
	private HashMap<String, Class> getClassToGeneratedClasses(){
		return jaxbContext.getClassToGeneratedClasses();
	}

	public void setJaxbContext(JAXBContext jaxbContext) {
		this.jaxbContext = jaxbContext;
	}

    public XMLMarshaller getXMLMarshaller() {
        return this.xmlMarshaller;
    }
}
