/*******************************************************************************
* Copyright (c) 1998, 2008 Oracle. All rights reserved.
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
        Object[] args = {className };
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, FACTORY_CLASS_WITHOUT_FACTORY_METHOD, args));
        exception.setErrorCode(FACTORY_CLASS_WITHOUT_FACTORY_METHOD);
        return exception;
    }
    
    public static JAXBException factoryMethodNotDeclared(String methodName, String className) {
        Object[] args = {methodName, className};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, FACTORY_METHOD_NOT_DECLARED, args));
        exception.setErrorCode(FACTORY_METHOD_NOT_DECLARED);
        return exception;

    }
    
    public static JAXBException multipleAnyAttributeMapping(String className) {
        Object[] args = {className};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, MULTIPLE_ANY_ATTRIBUTE_MAPPING, args));
        exception.setErrorCode(MULTIPLE_ANY_ATTRIBUTE_MAPPING);
        return exception;
    }
    
    public static JAXBException anyAttributeOnNonMap(String propertyName) {
        Object[] args = {propertyName};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, ANY_ATTRIBUTE_ON_NON_MAP_PROPERTY, args));
        exception.setErrorCode(ANY_ATTRIBUTE_ON_NON_MAP_PROPERTY);
        return exception;
    }
    
    public static JAXBException invalidElementRef(String propertyName, String className) {
        Object[] args = {propertyName, className};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, INVALID_XML_ELEMENT_REF, args));
        exception.setErrorCode(INVALID_XML_ELEMENT_REF);
        return exception;
    }    
    
    public static JAXBException nameCollision(String uri, String name) {
        Object[] args = {uri, name};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, NAME_COLLISION, args));
        exception.setErrorCode(NAME_COLLISION);
        return exception;
    }    
    
    public static JAXBException unsupportedNodeClass(String className) {
        Object[] args = {className};
        JAXBException exception = new JAXBException(ExceptionMessageGenerator.buildMessage(JAXBException.class, UNSUPPORTED_NODE_CLASS, args));
        exception.setErrorCode(UNSUPPORTED_NODE_CLASS);
        return exception;
    }   
}
