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
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.jaxb;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLRoot;
import org.eclipse.persistence.oxm.XMLUnmarshaller;

import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.internal.oxm.record.XMLEventReaderInputSource;
import org.eclipse.persistence.internal.oxm.record.XMLEventReaderReader;
import org.eclipse.persistence.internal.oxm.record.XMLStreamReaderInputSource;
import org.eclipse.persistence.internal.oxm.record.XMLStreamReaderReader;
import org.eclipse.persistence.jaxb.JAXBErrorHandler;
import org.eclipse.persistence.jaxb.JAXBUnmarshallerHandler;
import org.eclipse.persistence.jaxb.JAXBContext.RootLevelXmlAdapter;
import org.eclipse.persistence.jaxb.attachment.AttachmentUnmarshallerAdapter;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.jaxb.WrappedValue;
import org.eclipse.persistence.internal.jaxb.many.ManyValue;

/**
 * INTERNAL:
 * <p><b>Purpose:</b>To Provide an implementation of the JAXB 2.0 Unmarshaller Interface
 * <p><b>Responsibilities:</b>
 * <ul>
 * <li>Provide a JAXB wrapper on the XMLUnmarshaller API</li>
 * <li>Perform XML to Object Conversions</li>
 * </ul>
 * <p>This implementation of the JAXB 2.0 Unmarshaller interface provides the required functionality
 * by acting as a thin wrapper on the existing XMLMarshaller API.
 *
 * @author mmacivor
 * @since Oracle TopLink 11.1.1.0.0
 * @see javax.xml.bind.Unmarshaller
 * @see org.eclipse.persistence.oxm.XMLUnmarshaller
 */
public class JAXBUnmarshaller implements Unmarshaller {

    private ValidationEventHandler validationEventHandler;
    private XMLUnmarshaller xmlUnmarshaller;
    private JAXBContext jaxbContext;
    public static final String XML_JAVATYPE_ADAPTERS = "xml-javatype-adapters";
    public static final String STAX_SOURCE_CLASS_NAME = "javax.xml.transform.stax.StAXSource";

    public JAXBUnmarshaller(XMLUnmarshaller newXMLUnmarshaller) {
        super();
        validationEventHandler = new DefaultValidationEventHandler();
        xmlUnmarshaller = newXMLUnmarshaller;
        xmlUnmarshaller.setValidationMode(XMLUnmarshaller.NONVALIDATING);
        xmlUnmarshaller.setUnmarshalListener(new JAXBUnmarshalListener(this));
    }

    public XMLUnmarshaller getXMLUnmarshaller() {
        return xmlUnmarshaller;
    }

    public Object unmarshal(File file) throws JAXBException {
        if (file == null) {
            throw new IllegalArgumentException();
        }
        try {
            Object value = xmlUnmarshaller.unmarshal(file);
            return createJAXBElementIfRequired(value);
        } catch (Exception e) {
            throw new UnmarshalException(e);
        }
    }

    public Object unmarshal(InputStream inputStream) throws JAXBException {
        if (inputStream == null) {
            throw new IllegalArgumentException();
        }
        try {
            return createJAXBElementIfRequired(xmlUnmarshaller.unmarshal(inputStream));
        } catch (Exception e) {
            throw new UnmarshalException(e);
        }
    }

    public Object unmarshal(URL url) throws JAXBException {
        if (url == null) {
            throw new IllegalArgumentException();
        }
        try {
            Object value = xmlUnmarshaller.unmarshal(url);
            return createJAXBElementIfRequired(value);
        } catch (Exception e) {
            throw new UnmarshalException(e);
        }
    }

    public Object unmarshal(InputSource inputSource) throws JAXBException {
        if (inputSource == null) {
            throw new IllegalArgumentException();
        }
        try {
            Object value = xmlUnmarshaller.unmarshal(inputSource);
            return createJAXBElementIfRequired(value);
        } catch (Exception e) {
            throw new UnmarshalException(e);
        }
    }

    public Object unmarshal(Reader reader) throws JAXBException {
        if(reader == null) {
            throw new IllegalArgumentException();
        }
        try {
            Object value = xmlUnmarshaller.unmarshal(reader);
            return createJAXBElementIfRequired(value);
        } catch(Exception ex) {
            throw new JAXBException(ex);
        }
    }

    public Object unmarshal(Node node) throws JAXBException {
        if (node == null) {
            throw new IllegalArgumentException();
        }
        try {
            Object value = xmlUnmarshaller.unmarshal(node);
            return createJAXBElementIfRequired(value);
        } catch (Exception e) {
            throw new UnmarshalException(e);
        }
    }

    /**
     * Create a JAXBElement instance.  If the object is an instance
     * of XMLRoot, we will use its field values to create the
     * JAXBElement.  If the object is not an XMLRoot instance, we
     * will have to determine the 'name' value.  This will be done
     * using the object's descriptor default root element - any
     * prefix will be resolved, and a QName created.
     *
     * @param obj
     * @return
     */
    private JAXBElement buildJAXBElementFromObject(Object obj, Class declaredClass) {
        // if an XMLRoot was returned, the root element != the default root
        // element of the object being marshalled to - need to create a
        // JAXBElement from the returned XMLRoot object
        if (obj instanceof XMLRoot) {
            return createJAXBElementFromXMLRoot(((XMLRoot)obj), declaredClass);
        }


        if(obj instanceof JAXBElement) {
            return (JAXBElement) obj;
        }

        // at this point, the default root element of the object being marshalled
        // to == the root element - here we need to create a JAXBElement
        // instance using information from the returned object
        org.eclipse.persistence.sessions.Session sess = xmlUnmarshaller.getXMLContext().getSession(obj);
        XMLDescriptor desc = (XMLDescriptor) sess.getClassDescriptor(obj);

        // here we are assuming that if we've gotten this far, there
        // must be a default root element set on the descriptor.  if
        // this is incorrect, we need to check for null and throw an
        // exception
        String rootName = desc.getDefaultRootElement();
        if (rootName == null) {
            return createJAXBElement(new QName(""), obj.getClass(), obj);
        }
        String rootNamespaceUri = null;
        int idx = rootName.indexOf(":");
        if (idx != -1) {
            rootNamespaceUri = desc.getNamespaceResolver().resolveNamespacePrefix(rootName.substring(0, idx));
            rootName = rootName.substring(idx + 1);
        }

        QName qname;
        if (rootNamespaceUri == null) {
            qname = new QName(rootName);
        } else {
            qname = new QName(rootNamespaceUri, rootName);
        }
        return createJAXBElement(qname, obj.getClass(), obj);
    }

    public JAXBElement unmarshal(Node node, Class javaClass) throws JAXBException {
        Class classToUnmarshalTo = javaClass;
        if(jaxbContext.getArrayClassesToGeneratedClasses() != null) {
            Class generatedClass = jaxbContext.getArrayClassesToGeneratedClasses().get(javaClass.getCanonicalName());
            if(generatedClass != null){
                classToUnmarshalTo = generatedClass;
            }
        }
        return buildJAXBElementFromObject(xmlUnmarshaller.unmarshal(node, classToUnmarshalTo), javaClass);
    }

    public Object unmarshal(Source source) throws JAXBException {
        if (source == null) {
            throw new IllegalArgumentException();
        }
        try {
            Object value = xmlUnmarshaller.unmarshal(source);
            return createJAXBElementIfRequired(value);
        } catch (Exception e) {
            throw new UnmarshalException(e);
        }
    }

    public JAXBElement unmarshal(Source source, Class javaClass) throws JAXBException {
        Class classToUnmarshalTo = getClassToUnmarshalTo(javaClass);

        return buildJAXBElementFromObject(xmlUnmarshaller.unmarshal(source, classToUnmarshalTo), javaClass);
    }

    private JAXBElement unmarshal(Source source, Class javaClass, Class declaredType) {
        Class classToUnmarshalTo = javaClass;
        if(jaxbContext.getArrayClassesToGeneratedClasses() != null) {
            Class generatedClass = jaxbContext.getArrayClassesToGeneratedClasses().get(javaClass.getCanonicalName());
            if(generatedClass != null){
                classToUnmarshalTo = generatedClass;
            }
        }
        return buildJAXBElementFromObject(xmlUnmarshaller.unmarshal(source, classToUnmarshalTo), declaredType);
    }

    public JAXBElement unmarshal(Source source, Type type) throws JAXBException {
        if(jaxbContext.getTypeToTypeMappingInfo() != null) {
            TypeMappingInfo tmi = jaxbContext.getTypeToTypeMappingInfo().get(type);
            if(tmi != null) {
                return unmarshal(source, tmi);
            }
        }

        Class unmarshalClass = jaxbContext.getCollectionClassesToGeneratedClasses().get(type);
        if(unmarshalClass != null){
            JAXBElement unmarshalled =  unmarshal(source, unmarshalClass, Object.class);
            Class declaredClass = null;
            if(type instanceof Class){
                declaredClass = (Class)type;
            }else{
                declaredClass = Object.class;
            }
            JAXBElement returnVal = new JAXBElement(unmarshalled.getName(), declaredClass, unmarshalled.getScope(), unmarshalled.getValue());
            return returnVal;
        }else if(type instanceof Class){
            return  unmarshal(source, (Class)type, Object.class);
        }
        return null;
    }

    /**
     * Unmarshal the object based on the binding metadata associated with the
     * TypeMappingInfo.
     */
    public JAXBElement unmarshal(Source source, TypeMappingInfo type) throws JAXBException {
        Class unmarshalClass = jaxbContext.getTypeMappingInfoToGeneratedType().get(type);
        RootLevelXmlAdapter adapter = jaxbContext.getTypeMappingInfoToJavaTypeAdapters().get(type);
        if(unmarshalClass != null){
            JAXBElement unmarshalled = unmarshal(source, unmarshalClass);
            Class declaredClass = null;
            if(type.getType() instanceof Class){
                declaredClass = (Class)type.getType();
            }else{
                declaredClass = Object.class;
            }
            Object value = unmarshalled.getValue();
            if(adapter != null) {
                try {
                    value = adapter.getXmlAdapter().unmarshal(value);
                } catch(Exception ex) {
                    throw new JAXBException(XMLMarshalException.marshalException(ex));
                }
            }
            JAXBElement returnVal = new JAXBElement(unmarshalled.getName(), declaredClass, unmarshalled.getScope(), unmarshalled.getValue());
            return returnVal;
        }else if(type.getType() instanceof Class){
            if(adapter != null) {
                JAXBElement element = unmarshal(source, adapter.getBoundType());
                try {
                    Object value = adapter.getXmlAdapter().unmarshal(element.getValue());
                    element.setValue(value);
                    return element;
                } catch(Exception ex) {
                    throw new JAXBException(XMLMarshalException.marshalException(ex));
                }
            }
            return  unmarshal(source, (Class)type.getType());
        }
        return null;
    }

    public JAXBElement unmarshal(XMLStreamReader streamReader, Class javaClass) throws JAXBException {
        Class classToUnmarshalTo = getClassToUnmarshalTo(javaClass);
        XMLStreamReaderReader staxReader = new XMLStreamReaderReader();
        staxReader.setErrorHandler(xmlUnmarshaller.getErrorHandler());
        XMLStreamReaderInputSource inputSource = new XMLStreamReaderInputSource(streamReader);
        JAXBElement unmarshalled = buildJAXBElementFromObject(xmlUnmarshaller.unmarshal(staxReader, inputSource, classToUnmarshalTo), javaClass);

        if(classToUnmarshalTo != javaClass){
            JAXBElement returnVal = new JAXBElement(unmarshalled.getName(), javaClass, unmarshalled.getScope(), unmarshalled.getValue());
            return returnVal;
        }
        return unmarshalled;
    }

    public JAXBElement unmarshal(XMLStreamReader streamReader, Type type) throws JAXBException {
        if(jaxbContext.getTypeToTypeMappingInfo() != null) {
            TypeMappingInfo tmi = jaxbContext.getTypeToTypeMappingInfo().get(type);
            if(tmi != null) {
                return unmarshal(streamReader, tmi);
            }
        }

        Class unmarshalClass = jaxbContext.getCollectionClassesToGeneratedClasses().get(type);
        if(unmarshalClass != null){
            JAXBElement unmarshalled = unmarshal(streamReader, unmarshalClass);
            Class declaredClass = null;
            if(type instanceof Class){
                declaredClass = (Class)type;
            }else{
                declaredClass = Object.class;
            }
            JAXBElement returnVal = new JAXBElement(unmarshalled.getName(), declaredClass, unmarshalled.getScope(), unmarshalled.getValue());
            return returnVal;

        }else if(type instanceof Class){
            return  unmarshal(streamReader, (Class)type);
        }
        return null;
    }

    /**
     * Unmarshal the object based on the binding metadata associated with the
     * TypeMappingInfo.
     */
    public JAXBElement unmarshal(XMLStreamReader streamReader, TypeMappingInfo type) throws JAXBException {
        if(jaxbContext.getTypeMappingInfoToGeneratedType() == null) {
            return unmarshal(streamReader, type.getType());
        }
        RootLevelXmlAdapter adapter = jaxbContext.getTypeMappingInfoToJavaTypeAdapters().get(type);
        Class unmarshalClass = jaxbContext.getTypeMappingInfoToGeneratedType().get(type);
        if(unmarshalClass != null){

            JAXBElement unmarshalled = unmarshal(streamReader, unmarshalClass);
            Class declaredClass = null;
            if(type.getType() instanceof Class){
                declaredClass = (Class)type.getType();
            }else{
                declaredClass = Object.class;
            }
            Object value = unmarshalled.getValue();
            if(adapter != null) {
                try {
                    value = adapter.getXmlAdapter().unmarshal(value);
                } catch(Exception ex) {
                    throw new JAXBException(XMLMarshalException.marshalException(ex));
                }
            }
            JAXBElement returnVal = new JAXBElement(unmarshalled.getName(), declaredClass, unmarshalled.getScope(), value);
            return returnVal;
        }else if(type.getType() instanceof Class){
            if(adapter != null) {
                JAXBElement element = unmarshal(streamReader, adapter.getBoundType());
                try {
                    Object value = adapter.getXmlAdapter().unmarshal(element.getValue());
                    element.setValue(value);
                    return element;
                } catch(Exception ex) {
                    throw new JAXBException(XMLMarshalException.marshalException(ex));
                }
            }
            return  unmarshal(streamReader, (Class)type.getType());
        }
        return null;
    }

    public Object unmarshal(XMLStreamReader streamReader) throws JAXBException {
        XMLStreamReaderReader staxReader = new XMLStreamReaderReader();
        staxReader.setErrorHandler(xmlUnmarshaller.getErrorHandler());
        XMLStreamReaderInputSource inputSource = new XMLStreamReaderInputSource(streamReader);
        Object value = xmlUnmarshaller.unmarshal(staxReader, inputSource);
        return createJAXBElementIfRequired(value);
    }

    public JAXBElement unmarshal(XMLEventReader eventReader, Class javaClass) throws JAXBException {
        Class classToUnmarshalTo = getClassToUnmarshalTo(javaClass);
        XMLEventReaderReader staxReader = new XMLEventReaderReader();
        staxReader.setErrorHandler(xmlUnmarshaller.getErrorHandler());
        XMLEventReaderInputSource inputSource = new XMLEventReaderInputSource(eventReader);
        JAXBElement unmarshalled =  buildJAXBElementFromObject(xmlUnmarshaller.unmarshal(staxReader, inputSource, classToUnmarshalTo), javaClass);

        if(classToUnmarshalTo != javaClass){
            JAXBElement returnVal = new JAXBElement(unmarshalled.getName(), javaClass, unmarshalled.getScope(), unmarshalled.getValue());
            return returnVal;
        }
        return unmarshalled;
    }

    public JAXBElement unmarshal(XMLEventReader eventReader, Type type) throws JAXBException {
        if(jaxbContext.getTypeToTypeMappingInfo() != null) {
            TypeMappingInfo tmi = jaxbContext.getTypeToTypeMappingInfo().get(type);
            if(tmi != null) {
                return unmarshal(eventReader, tmi);
            }
        }
        Class unmarshalClass = jaxbContext.getCollectionClassesToGeneratedClasses().get(type);
        if(unmarshalClass != null){
            JAXBElement unmarshalled = unmarshal(eventReader, unmarshalClass);
            Class declaredClass = null;
            if(type instanceof Class){
                declaredClass = (Class)type;
            }else{
                declaredClass = Object.class;
            }
            JAXBElement returnVal = new JAXBElement(unmarshalled.getName(), declaredClass, unmarshalled.getScope(), unmarshalled.getValue());
            return returnVal;
        }else if(type instanceof Class){
            return  unmarshal(eventReader, (Class)type);
        }
        return null;
    }

    /**
     * Unmarshal the object based on the binding metadata associated with the
     * TypeMappingInfo.
     */
    public JAXBElement unmarshal(XMLEventReader eventReader, TypeMappingInfo type) throws JAXBException {
        if(jaxbContext.getTypeMappingInfoToGeneratedType() == null) {
            return unmarshal(eventReader, type.getType());
        }

        Class unmarshalClass = jaxbContext.getTypeMappingInfoToGeneratedType().get(type);
        RootLevelXmlAdapter adapter = jaxbContext.getTypeMappingInfoToJavaTypeAdapters().get(type);
        if(unmarshalClass != null){
            JAXBElement unmarshalled = unmarshal(eventReader, unmarshalClass);
            Class declaredClass = null;
            if(type.getType() instanceof Class){
                declaredClass = (Class)type.getType();
            }else{
                declaredClass = Object.class;
            }
            Object value = unmarshalled.getValue();
            if(adapter != null) {
                try {
                    value = adapter.getXmlAdapter().unmarshal(value);
                } catch(Exception ex) {
                    throw new JAXBException(XMLMarshalException.marshalException(ex));
                }
            }
            JAXBElement returnVal = new JAXBElement(unmarshalled.getName(), declaredClass, unmarshalled.getScope(), value);
            return returnVal;
        }else if(type.getType() instanceof Class){
            if(adapter != null) {
                JAXBElement element = unmarshal(eventReader, adapter.getBoundType());
                try {
                    Object value = adapter.getXmlAdapter().unmarshal(element.getValue());
                    element.setValue(value);
                    return element;
                } catch(Exception ex) {
                    throw new JAXBException(XMLMarshalException.marshalException(ex));
                }
            }
            return  unmarshal(eventReader, (Class)type.getType());
        }
        return null;
    }

    public Object unmarshal(XMLEventReader eventReader) throws JAXBException {
        XMLEventReaderReader staxReader = new XMLEventReaderReader();
        staxReader.setErrorHandler(xmlUnmarshaller.getErrorHandler());
        XMLEventReaderInputSource inputSource = new XMLEventReaderInputSource(eventReader);
        Object value = xmlUnmarshaller.unmarshal(staxReader, inputSource);
        return createJAXBElementIfRequired(value);
    }
    public UnmarshallerHandler getUnmarshallerHandler() {
        return new JAXBUnmarshallerHandler(this);
    }

    public void setValidating(boolean validate) throws JAXBException {
        if (validate) {
            xmlUnmarshaller.setValidationMode(XMLUnmarshaller.SCHEMA_VALIDATION);
        } else {
            xmlUnmarshaller.setValidationMode(XMLUnmarshaller.NONVALIDATING);
        }
    }

    public boolean isValidating() throws JAXBException {
        return xmlUnmarshaller.getValidationMode() != XMLUnmarshaller.NONVALIDATING;
    }

    public void setEventHandler(ValidationEventHandler newValidationEventHandler) throws JAXBException {
        if (null == newValidationEventHandler) {
            validationEventHandler = new DefaultValidationEventHandler();
        } else {
            validationEventHandler = newValidationEventHandler;
        }
        xmlUnmarshaller.setErrorHandler(new JAXBErrorHandler(validationEventHandler));
    }

    public ValidationEventHandler getEventHandler() throws JAXBException {
        return validationEventHandler;
    }

    public void setProperty(String key, Object value) throws PropertyException {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        throw new PropertyException(key, value);
    }

    public Object getProperty(String key) throws PropertyException {
        if (key == null) {
            throw new IllegalArgumentException();
        }
        throw new PropertyException("Unsupported Property");
    }

    public Unmarshaller.Listener getListener() {
        return ((JAXBUnmarshalListener)xmlUnmarshaller.getUnmarshalListener()).getListener();
    }

    public void setListener(Unmarshaller.Listener listener) {
        ((JAXBUnmarshalListener)xmlUnmarshaller.getUnmarshalListener()).setListener(listener);
    }

    public XmlAdapter getAdapter(Class javaClass) {
        HashMap result = (HashMap) xmlUnmarshaller.getProperty(XML_JAVATYPE_ADAPTERS);
        if (result == null) {
            return null;
        }
        return (XmlAdapter) result.get(javaClass);
    }

    public void setAdapter(Class javaClass, XmlAdapter adapter) {
        HashMap result = (HashMap) xmlUnmarshaller.getProperty(XML_JAVATYPE_ADAPTERS);
        if (result == null) {
            result = new HashMap();
            xmlUnmarshaller.getProperties().put(XML_JAVATYPE_ADAPTERS, result);
        }
        result.put(javaClass, adapter);
    }

    public void setAdapter(XmlAdapter adapter) {
        setAdapter(adapter.getClass(), adapter);
    }

    public void setSchema(Schema schema) {
        this.xmlUnmarshaller.setSchema(schema);
    }

    public Schema getSchema() {
        return this.xmlUnmarshaller.getSchema();
    }

    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
        return ((AttachmentUnmarshallerAdapter)xmlUnmarshaller.getAttachmentUnmarshaller()).getAttachmentUnmarshaller();
    }

    public void setAttachmentUnmarshaller(AttachmentUnmarshaller unmarshaller) {
        xmlUnmarshaller.setAttachmentUnmarshaller(new AttachmentUnmarshallerAdapter(unmarshaller));
    }

    public void setUnmarshalCallbacks(java.util.HashMap callbacks) {
        ((JAXBUnmarshalListener)xmlUnmarshaller.getUnmarshalListener()).setClassBasedUnmarshalEvents(callbacks);
    }

    private Object createJAXBElementIfRequired(Object value){
        if(value instanceof XMLRoot){
            return createJAXBElementFromXMLRoot((XMLRoot)value, Object.class);
        }
        return value;
    }

    private JAXBElement createJAXBElementFromXMLRoot(XMLRoot xmlRoot, Class declaredType){
        Object value = xmlRoot.getObject();

        if(value instanceof WrappedValue){
            QName qname = new QName(xmlRoot.getNamespaceURI(), xmlRoot.getLocalName());
            return new JAXBElement(qname, ((WrappedValue)value).getDeclaredType(), ((WrappedValue)value).getValue());
        }

        if(value instanceof JAXBElement) {
           return (JAXBElement) value;
        }

        QName qname = new QName(xmlRoot.getNamespaceURI(), xmlRoot.getLocalName());
        if(value instanceof ManyValue){
            value = ((ManyValue)value).getItem();
        }

        Map<QName, Class> qNamesToDeclaredClasses = jaxbContext.getQNamesToDeclaredClasses();
        if(qNamesToDeclaredClasses != null){
            Class declaredClass = qNamesToDeclaredClasses.get(qname);
            if(declaredClass != null){
                return createJAXBElement(qname, declaredClass, value);
            }
        }

        XMLDescriptor descriptorForQName = xmlUnmarshaller.getXMLContext().getDescriptor(qname);
        if(descriptorForQName != null){
            return createJAXBElement(qname, descriptorForQName.getJavaClass(), value);
        }

        if(value != null){
            XMLConversionManager xcm = ((XMLConversionManager)xmlUnmarshaller.getXMLContext().getSession(0).getDatasourcePlatform().getConversionManager());
            if( xcm.getDefaultJavaTypes().get(value.getClass()) != null){
                return createJAXBElement(qname, declaredType, value);
            }
        }

        return createJAXBElement(qname, declaredType, value);
    }

    private JAXBElement createJAXBElement(QName qname, Class theClass, Object value){

        if(theClass == null){
            return new JAXBElement(qname, Object.class, value);
        }

        if(ClassConstants.XML_GREGORIAN_CALENDAR.isAssignableFrom(theClass)){
            theClass = ClassConstants.XML_GREGORIAN_CALENDAR;
        }else if(ClassConstants.DURATION.isAssignableFrom(theClass)){
            theClass = ClassConstants.DURATION;
        }

        return new JAXBElement(qname, theClass, value);
    }

    public void setJaxbContext(JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    private Class getClassToUnmarshalTo(Class originalClass) {
    Class classToUnmarshalTo = originalClass;
        if(jaxbContext.getArrayClassesToGeneratedClasses() != null) {
            Class generatedClass = jaxbContext.getArrayClassesToGeneratedClasses().get(originalClass.getCanonicalName());
            if(generatedClass != null){
                classToUnmarshalTo = generatedClass;
            }
        }
        if(jaxbContext.getCollectionClassesToGeneratedClasses() != null){
            Class generatedClass = jaxbContext.getCollectionClassesToGeneratedClasses().get(originalClass);
            if(generatedClass != null){
                classToUnmarshalTo = generatedClass;
            }
        }
        if(jaxbContext.getTypeToTypeMappingInfo() != null){
              TypeMappingInfo tmi = jaxbContext.getTypeToTypeMappingInfo().get(originalClass);
              if(tmi != null && jaxbContext.getTypeMappingInfoToGeneratedType() != null) {
                  Class generatedClass = jaxbContext.getTypeMappingInfoToGeneratedType().get(tmi);
                  if(generatedClass != null){
                      classToUnmarshalTo = generatedClass;
                  }
              }
        }
        return classToUnmarshalTo;
    }

}
