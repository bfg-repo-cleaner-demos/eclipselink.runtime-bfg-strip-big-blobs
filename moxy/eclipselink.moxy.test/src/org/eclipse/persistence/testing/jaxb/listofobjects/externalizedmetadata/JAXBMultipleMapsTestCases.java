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
* Denise Smith - Sept 28/2009 - 2.0 
******************************************************************************/
package org.eclipse.persistence.testing.jaxb.listofobjects.externalizedmetadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.TreeMap;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.eclipse.persistence.internal.jaxb.JaxbClassLoader;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBTypeElement;
import org.eclipse.persistence.testing.jaxb.listofobjects.JAXBListOfObjectsTestCases;
import org.w3c.dom.Document;

public class JAXBMultipleMapsTestCases extends JAXBListOfObjectsTestCases {
	protected final static String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/listofobjects/multipleMaps.xml";
	
	public Map<String, Integer> mapField1;	
	
	private Type[] types;
	
	public JAXBMultipleMapsTestCases(String name) throws Exception {
		super(name);
		init();
	}

	public void init() throws Exception {
		setControlDocument(XML_RESOURCE);
		types = new Type[2];
		types[0] = getTypeToUnmarshalTo();
				  
		Type mapType2 = new ParameterizedType() {
		Type[] typeArgs = { Calendar.class, Float.class };
		 public Type[] getActualTypeArguments() { return typeArgs;}
		 public Type getOwnerType() { return null; }
		 public Type getRawType() { return Map.class; }      
		};
		types[1] = mapType2;		
	
		setTypes(types);
	}
	
	public void setUp() throws Exception{
		super.setUp();
		getXMLComparer().setIgnoreOrder(true);
	}
	
	public void tearDown(){
		super.tearDown();
		getXMLComparer().setIgnoreOrder(false);
	}
	
   public  Map<String, InputStream> getControlSchemaFiles(){
				
		InputStream instream1 = ClassLoader.getSystemResourceAsStream("org/eclipse/persistence/testing/jaxb/listofobjects/multipleMaps.xsd");
				
		Map<String,InputStream> controlSchema = new HashMap<String,InputStream>();
		controlSchema.put("",instream1);
		return controlSchema;
	}

    protected Object getControlObject() {
    	    	
    	Map<String, Integer> theMap = new HashMap<String, Integer>();
    	theMap.put("aaa", new Integer(1));
    	theMap.put("bbb", new Integer(2));

    	QName qname = new QName("root");
    	JAXBElement jaxbElement = new JAXBElement(qname, Object.class, null);
    	jaxbElement.setValue(theMap);
    	return jaxbElement;	 
    }

    protected Type getTypeToUnmarshalTo() throws Exception {
	    Field fld = getClass().getField("mapField1");
	    Type fieldType =  fld.getGenericType();
	    return fieldType;
    }

    protected String getNoXsiTypeControlResourceName() {    	
	    return XML_RESOURCE;
	}
    
    public void setTypes(Type[] newTypes) throws Exception {
		classLoader = new JaxbClassLoader(Thread.currentThread()
				.getContextClassLoader());
		JAXBContextFactory factory = new JAXBContextFactory();
		jaxbContext = factory.createContext(newTypes, getProperties(), classLoader);
		jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbUnmarshaller = jaxbContext.createUnmarshaller();
	}
    
    private Map<String, Object> getProperties() throws Exception{
    	String pkg = "java.util";
        
    	HashMap<String, Source> overrides = new HashMap<String, Source>();
    	overrides.put(pkg, getXmlSchemaOxm(pkg));
    	Map<String, Object> properties = new HashMap<String, Object>();
    	properties.put(JAXBContextFactory.ECLIPSELINK_OXM_XML_KEY, overrides);
    	return properties;
    }
    
    private Source getXmlSchemaOxm(String defaultTns) throws Exception {
        String oxm = 
          "<xml-bindings xmlns='http://www.eclipse.org/eclipselink/xsds/persistence/oxm'>" +
            "<xml-schema namespace='" + defaultTns + "'/>" + 
            "<java-types></java-types>" + 
          "</xml-bindings>";
        Document doc = parser.parse(new ByteArrayInputStream(oxm.getBytes()));        
        return new DOMSource(doc.getDocumentElement());
      }
    
	public void testTypeToSchemaTypeMap(){
		HashMap<Type, javax.xml.namespace.QName> typesMap = ((org.eclipse.persistence.jaxb.JAXBContext)jaxbContext).getTypeToSchemaType();		
		int mapSize = typesMap.size();
		assertEquals(2, mapSize);
		
		assertNotNull("Type was not found in TypeToSchemaType map.", typesMap.get(types[0]));
		assertNotNull("Type was not found in TypeToSchemaType map.", typesMap.get(types[1]));
	}
}
