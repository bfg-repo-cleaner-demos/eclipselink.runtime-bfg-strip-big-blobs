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
 * Denise Smith - October 2011
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.xmlbindings;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.eclipse.persistence.jaxb.xmlmodel.JavaType;
import org.eclipse.persistence.jaxb.xmlmodel.JavaType.JavaAttributes;
import org.eclipse.persistence.jaxb.xmlmodel.XmlBindings;
import org.eclipse.persistence.jaxb.xmlmodel.XmlBindings.JavaTypes;
import org.eclipse.persistence.jaxb.xmlmodel.XmlElement;
import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class XMLBindingsTestCases extends JAXBTestCases{

    private final static String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/xmlbindings/bindings.xml";
	
	public XMLBindingsTestCases(String name) throws Exception {
		super(name);
		setClasses(new Class[]{XmlBindings.class});
		setControlDocument(XML_RESOURCE);
	}

	protected Object getControlObject() {
		XmlBindings xmlBindings = new XmlBindings();
		xmlBindings.setPackageName("myPackage");
		JavaTypes types = new JavaTypes();
		JavaType javaType = new JavaType();
		javaType.setName("myType");		
		JavaAttributes javaAttributes = new JavaAttributes();
		XmlElement javaAttribute = new XmlElement();
		javaAttribute.setName("elementName");		
		javaAttribute.setJavaAttribute("theJavaAttributeValue");
		JAXBElement jbe = new JAXBElement<XmlElement>(new QName("http://www.eclipse.org/eclipselink/xsds/persistence/oxm", "xml-element"), XmlElement.class, javaAttribute);
		javaAttributes.getJavaAttribute().add(jbe);
		javaType.setJavaAttributes(javaAttributes);
		types.getJavaType().add(javaType);
		xmlBindings.setJavaTypes(types);
		return xmlBindings;
	}
	
	public void xmlToObjectTest(Object testObject) throws Exception {
        log("\n**xmlToObjectTest**");
        log("Expected:");
        Object controlObject = getReadControlObject();
        if(null == controlObject) {
            log((String) null);
        } else {
            log(controlObject.toString());
        }
        log("Actual:");
        if(null == testObject) {
            log((String) null);
        } else {
            log(testObject.toString());
        }
        
        XmlBindings expectedBindings = (XmlBindings)getReadControlObject();
        XmlBindings actualBindings = (XmlBindings)testObject;
        assertEquals(expectedBindings.getPackageName(), actualBindings.getPackageName());
        assertEquals(1, actualBindings.getJavaTypes().getJavaType().size());
        
        assertEquals(expectedBindings.getJavaTypes().getJavaType().get(0).getClass(), actualBindings.getJavaTypes().getJavaType().get(0).getClass());
        assertEquals(expectedBindings.getJavaTypes().getJavaType().get(0).getName(), actualBindings.getJavaTypes().getJavaType().get(0).getName());
        
        assertEquals(expectedBindings.getJavaTypes().getJavaType().get(0).getJavaAttributes().getJavaAttribute().size(), actualBindings.getJavaTypes().getJavaType().get(0).getJavaAttributes().getJavaAttribute().size());
        assertEquals(expectedBindings.getJavaTypes().getJavaType().get(0).getJavaAttributes().getJavaAttribute().get(0).getName(), actualBindings.getJavaTypes().getJavaType().get(0).getJavaAttributes().getJavaAttribute().get(0).getName());
        assertEquals(expectedBindings.getJavaTypes().getJavaType().get(0).getJavaAttributes().getJavaAttribute().get(0).getDeclaredType(), actualBindings.getJavaTypes().getJavaType().get(0).getJavaAttributes().getJavaAttribute().get(0).getDeclaredType());
        assertEquals(expectedBindings.getJavaTypes().getJavaType().get(0).getJavaAttributes().getJavaAttribute().get(0).getValue().getJavaAttribute(), actualBindings.getJavaTypes().getJavaType().get(0).getJavaAttributes().getJavaAttribute().get(0).getValue().getJavaAttribute());
    }

}
