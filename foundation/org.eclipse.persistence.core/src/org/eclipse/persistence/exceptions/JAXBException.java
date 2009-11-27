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
* mmacivor - June 11/2008 - 1.0 - Initial implementation
******************************************************************************/
package org.eclipse.persistence.exceptions;

import org.eclipse.persistence.exceptions.i18n.ExceptionMessageGenerator;

/**
 * <b>Purpose:</b>
 * <ul><li>This class provides an implementation of EclipseLinkException specific to the EclipseLink JAXB implementation</li>
 * </ul>
 * <p/>
 * <b>Responsibilities:</b>
 * <ul>
 * <li>Return a JAXBException that can be thrown around input parameters.
 * <li>Return a JAXBException that wraps an existing exception with additional input parameters.
 * </ul>
 * @since Oracle EclipseLink 1.0
 */
public class JAXBException extends EclipseLinkException {

    public static final int NO_OBJECT_FACTORY_OR_JAXB_INDEX_IN_PATH = 50000;
    public static final int FACTORY_METHOD_OR_ZERO_ARG_CONST_REQ = 50001;
    public static final int FACTORY_CLASS_WITHOUT_FACTORY_METHOD = 50002;
    public static final int FACTORY_METHOD_NOT_DECLARED = 50003;
    public static final int ANY_ATTRIBUTE_ON_NON_MAP_PROPERTY = 50004;
    public static final int MULTIPLE_ANY_ATTRIBUTE_MAPPING = 50005;
    public static final int INVALID_XML_ELEMENT_REF = 50006;
    public static final int NAME_COLLISION = 50007;
    public static final int UNSUPPORTED_NODE_CLASS = 50008;
    public static final int TRANSIENT_IN_PROP_ORDER = 50009;
    public static final int XMLVALUE_ATTRIBUTE_CONFLICT = 50010;
    public static final int SUBCLASS_CANNOT_HAVE_XMLVALUE = 50011;
    public static final int NON_EXISTENT_PROPERTY_IN_PROP_ORDER = 50012;
    public static final int MISSING_PROPERTY_IN_PROP_ORDER = 50013;
    public static final int INVALID_TYPE_FOR_XMLVALUE_PROPERTY = 50014;
    public static final int INVALID_XML_ELEMENT_WRAPPER = 50015;
    public static final int INVALID_ID = 50016;
    public static final int INVALID_IDREF = 50017;
    public static final int INVALID_LIST = 50018;
    public static final int VALUE_PARAMETER_TYPE_INCORRECT_FOR_OXM_XML = 50019;
    public static final int KEY_PARAMETER_TYPE_INCORRECT = 50021;
    public static final int VALUE_PARAMETER_TYPE_INCORRECT = 50022;
    public static final int NULL_METADATA_SOURCE = 50023;
    public static final int NULL_MAP_KEY = 50024;
    public static final int COULD_NOT_LOAD_CLASS_FROM_METADATA = 50025;
    public static final int COULD_NOT_CREATE_CONTEXT_FOR_XML_MODEL = 50026;
    public static final int COULD_NOT_UNMARSHAL_METADATA = 50027;
    public static final int COULD_NOT_CREATE_CUSTOMIZER_INSTANCE = 50028;
    public static final int INVALID_CUSTOMIZER_CLASS = 50029;
    public static final int ID_ALREADY_SET = 50030;
    public static final int XMLVALUE_ALREADY_SET = 50031;
    public static final int XMLANYELEMENT_ALREADY_SET = 50032;
    public static final int COULD_NOT_INITIALIZE_DOM_HANDLER_CONVERTER = 50033;
    public static final int INVALID_TYPE_FOR_XMLATTRIBUTEREF_PROPERTY = 50034;

    protected JAXBException(String message) {
        super(message);
    }

    protected JAXBException(String message, Exception internalException) {
        super(message, internalException);
    }

    public static JAXBException noObjectFactoryOrJaxbIndexInPath(String path) {
        Object[] args = { path };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, NO_OBJECT_FACTORY_OR_JAXB_INDEX_IN_PATH, args));
        exception.setErrorCode(NO_OBJECT_FACTORY_OR_JAXB_INDEX_IN_PATH);
        return exception;
    }

    public static JAXBException factoryMethodOrConstructorRequired(String className) {
        Object[] args = { className };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, FACTORY_METHOD_OR_ZERO_ARG_CONST_REQ, args));
        exception.setErrorCode(FACTORY_METHOD_OR_ZERO_ARG_CONST_REQ);
        return exception;
    }

    public static JAXBException factoryClassWithoutFactoryMethod(String className) {
        Object[] args = { className };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, FACTORY_CLASS_WITHOUT_FACTORY_METHOD, args));
        exception.setErrorCode(FACTORY_CLASS_WITHOUT_FACTORY_METHOD);
        return exception;
    }

    public static JAXBException factoryMethodNotDeclared(String methodName, String className) {
        Object[] args = { methodName, className };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, FACTORY_METHOD_NOT_DECLARED, args));
        exception.setErrorCode(FACTORY_METHOD_NOT_DECLARED);
        return exception;

    }

    public static JAXBException multipleAnyAttributeMapping(String className) {
        Object[] args = { className };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, MULTIPLE_ANY_ATTRIBUTE_MAPPING, args));
        exception.setErrorCode(MULTIPLE_ANY_ATTRIBUTE_MAPPING);
        return exception;
    }

    public static JAXBException anyAttributeOnNonMap(String propertyName) {
        Object[] args = { propertyName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, ANY_ATTRIBUTE_ON_NON_MAP_PROPERTY, args));
        exception.setErrorCode(ANY_ATTRIBUTE_ON_NON_MAP_PROPERTY);
        return exception;
    }

    public static JAXBException invalidElementRef(String propertyName, String className) {
        Object[] args = { propertyName, className };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, INVALID_XML_ELEMENT_REF, args));
        exception.setErrorCode(INVALID_XML_ELEMENT_REF);
        return exception;
    }

    public static JAXBException invalidElementWrapper(String propertyName) {
        Object[] args = { propertyName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, INVALID_XML_ELEMENT_WRAPPER, args));
        exception.setErrorCode(INVALID_XML_ELEMENT_WRAPPER);
        return exception;
    }

    public static JAXBException invalidId(String propertyName) {
        Object[] args = { propertyName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, INVALID_ID, args));
        exception.setErrorCode(INVALID_ID);
        return exception;
    }

    public static JAXBException invalidIdRef(String propertyName, String className) {
        Object[] args = { propertyName, className };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, INVALID_IDREF, args));
        exception.setErrorCode(INVALID_IDREF);
        return exception;
    }

    public static JAXBException invalidList(String propertyName) {
        Object[] args = { propertyName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, INVALID_LIST, args));
        exception.setErrorCode(INVALID_LIST);
        return exception;
    }

    public static JAXBException nameCollision(String uri, String name) {
        Object[] args = { uri, name };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, NAME_COLLISION, args));
        exception.setErrorCode(NAME_COLLISION);
        return exception;
    }

    public static JAXBException unsupportedNodeClass(String className) {
        Object[] args = { className };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, UNSUPPORTED_NODE_CLASS, args));
        exception.setErrorCode(UNSUPPORTED_NODE_CLASS);
        return exception;
    }

    public static JAXBException transientInProporder(String fieldName) {
        Object[] args = { fieldName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, TRANSIENT_IN_PROP_ORDER, args));
        exception.setErrorCode(TRANSIENT_IN_PROP_ORDER);
        return exception;
    }

    public static JAXBException nonExistentPropertyInPropOrder(String fieldName) {
        Object[] args = { fieldName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, NON_EXISTENT_PROPERTY_IN_PROP_ORDER, args));
        exception.setErrorCode(NON_EXISTENT_PROPERTY_IN_PROP_ORDER);
        return exception;
    }

    public static JAXBException missingPropertyInPropOrder(String fieldName) {
        Object[] args = { fieldName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, MISSING_PROPERTY_IN_PROP_ORDER, args));
        exception.setErrorCode(MISSING_PROPERTY_IN_PROP_ORDER);
        return exception;
    }

    public static JAXBException propertyOrFieldShouldBeAnAttribute(String fieldName) {
        Object[] args = { fieldName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, XMLVALUE_ATTRIBUTE_CONFLICT, args));
        exception.setErrorCode(XMLVALUE_ATTRIBUTE_CONFLICT);
        return exception;
    }

    public static JAXBException propertyOrFieldCannotBeXmlValue(String fieldName) {
        Object[] args = { fieldName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, SUBCLASS_CANNOT_HAVE_XMLVALUE, args));
        exception.setErrorCode(SUBCLASS_CANNOT_HAVE_XMLVALUE);
        return exception;
    }

    public static JAXBException invalidTypeForXmlValueField(String fieldName) {
        Object[] args = { fieldName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, INVALID_TYPE_FOR_XMLVALUE_PROPERTY, args));
        exception.setErrorCode(INVALID_TYPE_FOR_XMLVALUE_PROPERTY);
        return exception;
    }

    /**
     * This exception would typically be used by JAXBContextFactory during externalized metadata processing (i.e.
     * eclipselink-oxm.xml).  This exception applies to the case where the Key parameter type of the package name to 
     * metadata source map is something other than String.  We require Map<String, Source>.
     * 
     * @return
     */
    public static JAXBException incorrectKeyParameterType() {
        Object[] args = {};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, KEY_PARAMETER_TYPE_INCORRECT, args));
        exception.setErrorCode(KEY_PARAMETER_TYPE_INCORRECT);
        return exception;
    }

    /**
     * This exception would typically be used by JAXBContextFactory during externalized metadata processing (i.e.
     * eclipselink-oxm.xml).  This exception applies to the case where the Value parameter type (of the package 
     * name to metadata source map) is something other than Source.  We require Map<String, Source>.
     * 
     * @return
     */
    public static JAXBException incorrectValueParameterType() {
        Object[] args = {};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, VALUE_PARAMETER_TYPE_INCORRECT, args));
        exception.setErrorCode(VALUE_PARAMETER_TYPE_INCORRECT);
        return exception;
    }

    /**
     * This exception would typically be used by JAXBContextFactory during externalized metadata processing (i.e.
     * eclipselink-oxm.xml).  This exception applies to the case where the Value parameter type associated with 
     * the 'eclipselink-oxm-xml' Key (in the properties map) is something other than Map<String, Source>.
     * 
     * @return
     */
    public static JAXBException incorrectValueParameterTypeForOxmXmlKey() {
        Object[] args = {};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, VALUE_PARAMETER_TYPE_INCORRECT_FOR_OXM_XML, args));
        exception.setErrorCode(VALUE_PARAMETER_TYPE_INCORRECT_FOR_OXM_XML);
        return exception;
    }

    /**
     * This exception would typically be used by JAXBContextFactory during externalized metadata processing (i.e.
     * eclipselink-oxm.xml).  This exception applies to the case where the Value (in the package name 
     * to metadata source map) is null.
     * 
     * @param key
     * @return
     */
    public static JAXBException nullMetadataSource(String key) {
        Object[] args = { key };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, NULL_METADATA_SOURCE, args));
        exception.setErrorCode(NULL_METADATA_SOURCE);
        return exception;
    }

    /**
     * This exception would typically be used by JAXBContextFactory during externalized metadata processing (i.e.
     * eclipselink-oxm.xml).  This exception applies to the case where the Key (in the package name 
     * to metadata source map) is null.
     * 
     * @return
     */
    public static JAXBException nullMapKey() {
        Object[] args = {};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, NULL_MAP_KEY, args));
        exception.setErrorCode(NULL_MAP_KEY);
        return exception;
    }

    /**
     * This exception would typically be used by JAXBContextFactory during externalized metadata processing (i.e.
     * eclipselink-oxm.xml).  This exception applies to the case where a class that is declared in the metadata
     * file cannot be loaded by the classloader. 
     * 
     * @param classname
     * @return
     */
    public static JAXBException couldNotLoadClassFromMetadata(String classname) {
        Object[] args = { classname };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, COULD_NOT_LOAD_CLASS_FROM_METADATA, args));
        exception.setErrorCode(COULD_NOT_LOAD_CLASS_FROM_METADATA);
        return exception;
    }

    /**
     * This exception would typically be used by JAXBContextFactory during externalized metadata processing (i.e.
     * eclipselink-oxm.xml).  This exception applies to the case where JAXBContext creation fails for our
     * XmlModel.
     * 
     * @return
     */
    public static JAXBException couldNotCreateContextForXmlModel() {
        Object[] args = {};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, COULD_NOT_CREATE_CONTEXT_FOR_XML_MODEL, args));
        exception.setErrorCode(COULD_NOT_CREATE_CONTEXT_FOR_XML_MODEL);
        return exception;
    }

    /**
     * This exception would typically be used by JAXBContextFactory during externalized metadata processing (i.e.
     * eclipselink-oxm.xml).  This exception applies to the case where JAXBContext creation fails for our
     * XmlModel.
     * 
     * @param ex
     * @return
     */
    public static JAXBException couldNotCreateContextForXmlModel(Exception ex) {
        Object[] args = { ex };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, COULD_NOT_CREATE_CONTEXT_FOR_XML_MODEL, args), ex);
        exception.setErrorCode(COULD_NOT_CREATE_CONTEXT_FOR_XML_MODEL);
        return exception;
    }

    /**
     * This exception would typically be used by JAXBContextFactory during externalized metadata processing (i.e.
     * eclipselink-oxm.xml).  This exception applies to the case where an exception occurs while unmarshalling 
     * the eclipselink metadata file.
     * 
     * @param e
     * @return
     */
    public static JAXBException couldNotUnmarshalMetadata(Exception e) {
        Object[] args = {};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, COULD_NOT_UNMARSHAL_METADATA, args), e);
        exception.setErrorCode(COULD_NOT_UNMARSHAL_METADATA);
        return exception;
    }

    /**
     * This exception should be used when a descriptor customizer instance cannot be created.
     * 
     * @param e
     * @param javaClassName
     * @param customizerClassName
     * @return
     */
    public static JAXBException couldNotCreateCustomizerInstance(Exception e, String customizerClassName) {
        Object[] args = { customizerClassName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, COULD_NOT_CREATE_CUSTOMIZER_INSTANCE, args), e);
        exception.setErrorCode(COULD_NOT_CREATE_CUSTOMIZER_INSTANCE);
        return exception;
    }

    /**
     * This exception would typically be thrown when a customizer class is set
     * that is not an instance of DescriptorCustomizer. 
     * 
     * @param e
     * @param customizerClassName
     * @return
     */
    public static JAXBException invalidCustomizerClass(Exception e, String customizerClassName) {
        Object[] args = { customizerClassName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, INVALID_CUSTOMIZER_CLASS, args), e);
        exception.setErrorCode(INVALID_CUSTOMIZER_CLASS);
        return exception;
    }

    /**
     * This exception should be used when an attempt is made to set an ID property
     * when one has already been set.
     *  
     * @param propertyName attempting to set this property as ID
     * @param idPropertyName existing ID property
     * @param className class in question
     * @return
     */
    public static JAXBException idAlreadySet(String propertyName, String idPropertyName, String className) {
        Object[] args = { propertyName, className, idPropertyName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, ID_ALREADY_SET, args));
        exception.setErrorCode(ID_ALREADY_SET);
        return exception;
    }

    /**
     * This exception should be used when an attempt is made to set an XmlValue property
     * when one has already been set.
     *  
     * @param propertyName attempting to set this property as XmlValue
     * @param xmlValuePropertyName existing XmlValue property
     * @param className class in question
     * @return
     */
    public static JAXBException xmlValueAlreadySet(String propertyName, String xmlValuePropertyName, String className) {
        Object[] args = { className, propertyName, xmlValuePropertyName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, XMLVALUE_ALREADY_SET, args));
        exception.setErrorCode(XMLVALUE_ALREADY_SET);
        return exception;
    }

    /**
     * This exception should be used when an attempt is made to set an XmlAnyElement 
     * property when one has already been set.
     *  
     * @param propertyName attempting to set this property as XmlAnyElement
     * @param xmlAnyElementPropertyName existing XmlAnyElement property
     * @param className class in question
     * @return
     */
    public static JAXBException xmlAnyElementAlreadySet(String propertyName, String xmlAnyElementPropertyName, String className) {
        Object[] args = { className, propertyName, xmlAnyElementPropertyName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, XMLANYELEMENT_ALREADY_SET, args));
        exception.setErrorCode(XMLANYELEMENT_ALREADY_SET);
        return exception;
    }

    /**
     * This exception should be used when DomHandlerConverter initialization fails.
     *  
     * @param nestedException
     * @param domHandlerClassName
     * @param propertyName
     * @return
     */
    public static JAXBException couldNotInitializeDomHandlerConverter(Exception nestedException, String domHandlerClassName, String propertyName) {
        Object[] args = { domHandlerClassName, propertyName };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, COULD_NOT_INITIALIZE_DOM_HANDLER_CONVERTER, args), nestedException);
        exception.setErrorCode(COULD_NOT_INITIALIZE_DOM_HANDLER_CONVERTER);
        return exception;
    }

    /**
     * This exception should be used when an @XmlAttributeRef or xml-attribute-ref appears 
     * on a non-DataHandler property.
     * 
     * @param propertyName
     * @param className
     * @return
     */
    public static JAXBException invalidAttributeRef(String propertyName, String className) {
        Object[] args = { propertyName, className };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, INVALID_TYPE_FOR_XMLATTRIBUTEREF_PROPERTY, args));
        exception.setErrorCode(INVALID_TYPE_FOR_XMLATTRIBUTEREF_PROPERTY);
        return exception;
    }
}
