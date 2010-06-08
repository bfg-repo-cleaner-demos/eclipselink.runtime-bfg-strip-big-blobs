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
*     bdoughan - January 13/2010 - 2.0.1 - Initial implementation
******************************************************************************/
package org.eclipse.persistence.testing.jaxb.typemappinginfo.rootelement;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.eclipse.persistence.jaxb.TypeMappingInfo;
import org.eclipse.persistence.jaxb.TypeMappingInfo.ElementScope;
import org.eclipse.persistence.testing.jaxb.typemappinginfo.TypeMappingInfoTestCases;

public class RootFromJAXBElementTestCases extends TypeMappingInfoTestCases {

    private static final String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/typemappinginfo/rootelement/JAXBElement.xml";
    private static final String XSD_RESOURCE = "org/eclipse/persistence/testing/jaxb/typemappinginfo/rootelement/JAXBElement.xsd";

    private static final String ADDRESS_NAMESPACE_URI = null;
    private static final String ADDRESS_LOCAL_NAME = "address";
    private static final String JAXB_ELEMENT_NAMESPACE_URI = null;
    private static final String JAXB_ELEMENT_LOCAL_NAME = "jaxb-element-address";
    private static final String CONTROL_STREET = "123 A St.";

    public RootFromJAXBElementTestCases(String name) throws Exception {
        super(name);
        setControlDocument(XML_RESOURCE);

        TypeMappingInfo[] typeMappingInfos = new TypeMappingInfo[1];
        TypeMappingInfo addressTypeMappingInfo = new TypeMappingInfo(); 
        addressTypeMappingInfo.setType(Address.class); 
        addressTypeMappingInfo.setXmlTagName(new QName(ADDRESS_NAMESPACE_URI, ADDRESS_LOCAL_NAME)); 
        addressTypeMappingInfo.setElementScope(ElementScope.Local); 
        addressTypeMappingInfo.setAnnotations(new Annotation[0]); 
        typeMappingInfos[0] = addressTypeMappingInfo;
        setTypeMappingInfos(typeMappingInfos);
    }

    @Override
    protected JAXBElement<Address> getControlObject() {
        Address address = new Address();
        address.setStreet(CONTROL_STREET);
        return new JAXBElement<Address>(new QName(JAXB_ELEMENT_NAMESPACE_URI, JAXB_ELEMENT_LOCAL_NAME), Address.class, address);
    }
    
    @Override   
    public JAXBElement<Object> getWriteControlObject() {
        Address address = new Address();
        address.setStreet(CONTROL_STREET);
        return new JAXBElement<Object>(new QName(JAXB_ELEMENT_NAMESPACE_URI, JAXB_ELEMENT_LOCAL_NAME), Object.class, address);
    }

    @Override
    public Map<String, InputStream> getControlSchemaFiles() {
        Map<String, InputStream> schemas = new HashMap<String, InputStream>(1);
        InputStream instream = ClassLoader.getSystemResourceAsStream(XSD_RESOURCE);
        schemas.put("", instream);
        return schemas;
    }

}