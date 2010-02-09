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
 *     Denise Smith -  November, 2009 
 ******************************************************************************/  
package org.eclipse.persistence.testing.jaxb.typemappinginfo;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.jaxb.JAXBMarshaller;
import org.eclipse.persistence.jaxb.JAXBUnmarshaller;
import org.eclipse.persistence.jaxb.TypeMappingInfo;

import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLRoot;
import org.eclipse.persistence.testing.jaxb.JAXBXMLComparer;
import org.eclipse.persistence.testing.oxm.OXTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public abstract class TypeMappingInfoTestCases extends OXTestCase {
	protected JAXBContext jaxbContext;
	protected Marshaller jaxbMarshaller;
	protected Unmarshaller jaxbUnmarshaller;	
	public String resourceName;
	protected Document controlDocument;
    protected Document writeControlDocument;
    
    protected DocumentBuilder parser;
    
    protected String controlDocumentLocation;
    protected String writeControlDocumentLocation;
    
    protected TypeMappingInfo[] typeMappingInfos;
        
	public TypeMappingInfoTestCases(String name) throws Exception {
		super(name);
	}

	public void setUp() throws Exception {    	
	    setupParser();
	    setupControlDocs();
	}
	
	public void tearDown() throws Exception{
	    super.tearDown();
	    jaxbContext = null;
	    jaxbMarshaller = null;
	    jaxbUnmarshaller = null;
	}

    
    public void setTypeMappingInfos(TypeMappingInfo[] newTypes) throws Exception {
    	typeMappingInfos = newTypes;
    	jaxbContext  = new org.eclipse.persistence.jaxb.JAXBContextFactory().createContext(newTypes, getProperties(), Thread.currentThread().getContextClassLoader());
    	jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbUnmarshaller = jaxbContext.createUnmarshaller();
     } 
    public TypeMappingInfo getTypeMappingInfo(){
    	return typeMappingInfos[0];
    }
	
    protected Map getProperties() throws Exception{
    	return null;
    }
    
    public void testXMLToObjectFromXMLEventReaderWithTypeMappingInfo() throws Exception {
	    if(null != XML_INPUT_FACTORY) {
	        InputStream instream = ClassLoader.getSystemResourceAsStream(resourceName);
	        javax.xml.stream.XMLEventReader reader = XML_INPUT_FACTORY.createXMLEventReader(instream);
	        Object obj = ((org.eclipse.persistence.jaxb.JAXBUnmarshaller)jaxbUnmarshaller).unmarshal(reader, getTypeMappingInfo());

	        Object controlObj = getReadControlObject();            
	        xmlToObjectTest(obj, controlObj);
	    }
	}   
	  
    public void testXMLToObjectFromXMLStreamReaderWithTypeMappingInfo() throws Exception { 
        if(null != XML_INPUT_FACTORY) {
            InputStream instream = ClassLoader.getSystemResourceAsStream(resourceName);
            XMLStreamReader xmlStreamReader = XML_INPUT_FACTORY.createXMLStreamReader(instream);
            Object testObject = ((JAXBUnmarshaller)jaxbUnmarshaller).unmarshal(xmlStreamReader, getTypeMappingInfo());
            instream.close();

            Object controlObj = getReadControlObject();
            xmlToObjectTest(testObject, controlObj);          
        } 
    } 
    
    public void testObjectToXMLStreamWriterWithTypeMappingInfo() throws Exception {
        if(XML_OUTPUT_FACTORY != null) {
            StringWriter writer = new StringWriter();

            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            factory.setProperty(factory.IS_REPAIRING_NAMESPACES, new Boolean(false));
            XMLStreamWriter streamWriter= factory.createXMLStreamWriter(writer);

            Object objectToWrite = getWriteControlObject();
            XMLDescriptor desc = null;
            if (objectToWrite instanceof XMLRoot) {
            	
                desc = (XMLDescriptor)((org.eclipse.persistence.jaxb.JAXBContext)jaxbContext).getXMLContext().getSession(0).getProject().getDescriptor(((XMLRoot)objectToWrite).getObject().getClass());
            } else {
                desc = (XMLDescriptor)((org.eclipse.persistence.jaxb.JAXBContext)jaxbContext).getXMLContext().getSession(0).getProject().getDescriptor(objectToWrite.getClass());
            }

            int sizeBefore = getNamespaceResolverSize(desc);
            ((JAXBMarshaller)jaxbMarshaller).marshal(objectToWrite, streamWriter, getTypeMappingInfo());

            streamWriter.flush();
            int sizeAfter = getNamespaceResolverSize(desc);

            assertEquals(sizeBefore, sizeAfter);
            StringReader reader = new StringReader(writer.toString());
            InputSource inputSource = new InputSource(reader);
            Document testDocument = parser.parse(inputSource);
            writer.close();
            reader.close();
            objectToXMLDocumentTest(testDocument);
        }
    }
    
    public void testObjectToXMLEventWriterWithTypeMappingInfo() throws Exception {
        if(XML_OUTPUT_FACTORY != null) {
            StringWriter writer = new StringWriter();

            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            factory.setProperty(factory.IS_REPAIRING_NAMESPACES, new Boolean(false));
            XMLEventWriter eventWriter= factory.createXMLEventWriter(writer);

            Object objectToWrite = getWriteControlObject();
            XMLDescriptor desc = null;
            if (objectToWrite instanceof XMLRoot) {
                desc = (XMLDescriptor)((org.eclipse.persistence.jaxb.JAXBContext)jaxbContext).getXMLContext().getSession(0).getProject().getDescriptor(((XMLRoot)objectToWrite).getObject().getClass());
            } else {
                desc = (XMLDescriptor)((org.eclipse.persistence.jaxb.JAXBContext)jaxbContext).getXMLContext().getSession(0).getProject().getDescriptor(objectToWrite.getClass());
            }

            int sizeBefore = getNamespaceResolverSize(desc);
            ((JAXBMarshaller)jaxbMarshaller).marshal(objectToWrite, eventWriter, getTypeMappingInfo());

            eventWriter.flush();
            int sizeAfter = getNamespaceResolverSize(desc);

            assertEquals(sizeBefore, sizeAfter);
            StringReader reader = new StringReader(writer.toString());
            InputSource inputSource = new InputSource(reader);
            Document testDocument = parser.parse(inputSource);
            writer.close();
            reader.close();
            objectToXMLDocumentTest(testDocument);
        }
    }    

	public void xmlToObjectTest(Object testObject, Object controlObject) throws Exception {
        log("\n**xmlToObjectTest**");
        log("Expected:");
        log(controlObject.toString());
        log("Actual:");
        log(testObject.toString());

        JAXBElement controlObj = (JAXBElement)controlObject;
        JAXBElement testObj = (JAXBElement)testObject;
        compareJAXBElementObjects(controlObj, testObj);
    }	
	
	public abstract Map<String, InputStream> getControlSchemaFiles();
	
	public void testSchemaGen() throws Exception {
		testSchemaGen(getControlSchemaFiles());
	}
	
	 protected void compareValues(Object controlValue, Object testValue){
         if(controlValue instanceof Node && testValue instanceof Node) {
             assertXMLIdentical(((Node)controlValue).getOwnerDocument(), ((Node)testValue).getOwnerDocument());
         } else if(controlValue instanceof DataHandler && testValue instanceof DataHandler){
        	 compareDataHandlers((DataHandler)controlValue, (DataHandler)testValue); 
         } else if(controlValue instanceof Image && testValue instanceof Image) {
             compareImages((Image)controlValue, (Image) testValue);
         } else if (controlValue instanceof Byte[] && testValue instanceof Byte[]){
       	     compareByteArrays((Byte[])controlValue, (Byte[])testValue);
         } else if (controlValue instanceof byte[] && testValue instanceof byte[]){
        	     compareByteArrays((byte[])controlValue, (byte[])testValue);
         } else {
             assertEquals(controlValue, testValue);
         }
     }
	 
	 private void compareDataHandlers(DataHandler controlValue, DataHandler testValue){
		 assertEquals(controlValue.getContentType(), testValue.getContentType());
		 try{
	         assertEquals(controlValue.getContent(), testValue.getContent());	     
	     }catch(Exception e){
	          e.printStackTrace();
	          fail();
	      }
	 }
	 
	 private void compareImages(Image controlImage, Image testImage) {
	     assertEquals(controlImage.getWidth(null), testImage.getWidth(null));
	     assertEquals(controlImage.getHeight(null), testImage.getHeight(null));
	 }
	   protected void setupParser() {
	        try {
	            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
	            builderFactory.setNamespaceAware(true);
	            builderFactory.setIgnoringElementContentWhitespace(true);
	            parser = builderFactory.newDocumentBuilder();
	        } catch (Exception e) {
	            e.printStackTrace();
	            fail("An exception occurred during setup");
	        }
	    }

	   public void setupControlDocs() throws Exception{
	        if(this.controlDocumentLocation != null) {
	            InputStream inputStream = ClassLoader.getSystemResourceAsStream(controlDocumentLocation);
	            resourceName = controlDocumentLocation;
	            controlDocument = parser.parse(inputStream);	          
	            removeEmptyTextNodes(controlDocument);
	          
	            inputStream.close();
	        }

	    if(this.writeControlDocumentLocation != null) {
	            InputStream inputStream = ClassLoader.getSystemResourceAsStream(writeControlDocumentLocation);
	            writeControlDocument = parser.parse(inputStream);
                removeEmptyTextNodes(writeControlDocument);

	            inputStream.close();
	        }
	    }	   
	   protected int getNamespaceResolverSize(XMLDescriptor desc){
	       int size = -1;
	        if (desc != null) {
	            NamespaceResolver nr = desc.getNamespaceResolver();
	            if (nr != null) {
	                size = nr.getNamespaces().size();
	            }else{
	              size =0;
	            }
	        }
	        return size;
	    }

	   protected Document getControlDocument() {
	        return controlDocument;
	    }

	    /**
	     * Override this function to implement different read/write control documents.
	     * @return
	     * @throws Exception
	     */
	    protected Document getWriteControlDocument() throws Exception {
	        if(writeControlDocument != null){
	            return writeControlDocument;
	        }
	        return getControlDocument();
	    }

	    protected void setControlDocument(String xmlResource) throws Exception {
	        this.controlDocumentLocation = xmlResource;
	    }

	    /**
	     * Provide an alternative write version of the control document when rountrip is not enabled.
	     * If this function is not called and getWriteControlDocument() is not overridden then the write and read control documents are the same.
	     * @param xmlResource
	     * @throws Exception
	     */
	    protected void setWriteControlDocument(String xmlResource) throws Exception {
	        writeControlDocumentLocation = xmlResource;
	    }

	    abstract protected Object getControlObject();	 
	    /*
	     * Returns the object to be used in a comparison on a read
	     * This will typically be the same object used to write
	     */
	    public Object getReadControlObject() {
	        return getControlObject();
	    }

	    /*
	     * Returns the object to be written to XML which will be compared
	     * to the control document.
	     */
	    public Object getWriteControlObject() {
	        return getControlObject();
	    }
	    
	    public void objectToXMLDocumentTest(Document testDocument) throws Exception {
	        log("**objectToXMLDocumentTest**");
	        log("Expected:");
	        log(getWriteControlDocument());
	        log("\nActual:");
	        log(testDocument);
	        assertXMLIdentical(getWriteControlDocument(), testDocument);
	    }
	    
	    public void compareJAXBElementObjects(JAXBElement controlObj, JAXBElement testObj) {
	        assertEquals(controlObj.getName().getLocalPart(), testObj.getName().getLocalPart());
	        assertEquals(controlObj.getName().getNamespaceURI(), testObj.getName().getNamespaceURI());
	        assertEquals(controlObj.getDeclaredType(), testObj.getDeclaredType());

	        Object controlValue = controlObj.getValue();
	        Object testValue = testObj.getValue();

	        if(controlValue == null) {
	        	if(testValue == null){
	        		return;
	        	}
	        	fail("Test value should have been null");
	        }else{
	        	if(testValue == null){
	        		fail("Test value should not have been null");	
	        	}
	        }
	        
	        if(controlValue.getClass() == ClassConstants.ABYTE && testValue.getClass() == ClassConstants.ABYTE ||
	        	controlValue.getClass() == ClassConstants.APBYTE && testValue.getClass() == ClassConstants.APBYTE){
	        	compareValues(controlValue, testValue);
	        }else if(controlValue.getClass().isArray()){
	            if(testValue.getClass().isArray()){
	                if(controlValue.getClass().getComponentType().isPrimitive()){
	                    comparePrimitiveArrays(controlValue, testValue);
	                }else{
	                	compareObjectArrays(controlValue, testValue);                   
	                }
	            }else{
	                fail("Expected an array value but was an " + testValue.getClass().getName());
	            }
	        }
	        else if (controlValue instanceof Collection){
	            Collection controlCollection = (Collection)controlValue;
	            Collection testCollection = (Collection)testValue;
	            Iterator<Object> controlIter = controlCollection.iterator();
	            Iterator<Object> testIter = testCollection.iterator();
	            assertEquals(controlCollection.size(), testCollection.size());
	            while(controlIter.hasNext()){
	                Object nextControl = controlIter.next();
	                Object nextTest = testIter.next();
	                compareValues(nextControl, nextTest);
	            }
	        }else{
	        	compareValues(controlValue, testValue);
	        }
	    }
	    
	    protected void comparePrimitiveArrays(Object controlValue, Object testValue){
	        fail("NEED TO COMPARE PRIMITIVE ARRAYS");
	    }
	    
	    protected void  compareObjectArrays(Object controlValue, Object testValue){
	        assertEquals(((Object[])controlValue).length,((Object[])testValue).length);
	        for(int i=0; i<((Object[])controlValue).length-1; i++){
	            assertEquals(((Object[])controlValue)[i], ((Object[])testValue)[i]);
	        }
	    }	
	    
	    public void testSchemaGen(Map<String, InputStream> controlSchemas) throws Exception {
	    	MySchemaOutputResolver outputResolver = new MySchemaOutputResolver();
			jaxbContext.generateSchema(outputResolver);
			
			Map<String, File> generatedSchemas = outputResolver.getSchemaFiles();
			assertEquals(controlSchemas.size(), generatedSchemas.size());

			for(String next:controlSchemas.keySet()){
				InputStream nextControlValue = controlSchemas.get(next);						
				File nextGeneratedValue = generatedSchemas.get(next);
				
				assertNotNull("Generated Schema not found.", nextGeneratedValue);
				
				Document control = parser.parse(nextControlValue);
				Document test = parser.parse(nextGeneratedValue);
				
				JAXBXMLComparer xmlComparer = new JAXBXMLComparer();	        
				boolean isEqual = xmlComparer.isSchemaEqual(control, test);
				if(!isEqual){
				    log("Expected Schema\n");
					log(control);
					log("ActualSchema\n");
					log(test);
				}
				assertTrue("generated schema did not match control schema", isEqual);
			}
	    }
	    
	    public class MySchemaOutputResolver extends SchemaOutputResolver {
			// keep a list of processed schemas for the validation phase of the
			// test(s)
			public Map<String, File> schemaFiles;

			public MySchemaOutputResolver() {
				schemaFiles = new HashMap<String, File>();
			}

			public Result createOutput(String namespaceURI, String suggestedFileName)throws IOException {
				File schemaFile = new File(suggestedFileName);
				if(namespaceURI == null){
					namespaceURI ="";
				}
				schemaFiles.put(namespaceURI, schemaFile);
				return new StreamResult(schemaFile);
			}		
			
			public Map<String, File> getSchemaFiles() {
				return schemaFiles;
			}
		}
	    
    /**
     * Return an Element for a given xml-element snippet.
     * 
     * @param xmlelement
     * @return
     * @throws Exception
     */
    protected Element getXmlElement(String xmlelement) throws Exception {
        StringReader str = new StringReader(xmlelement);
        InputSource is = new InputSource(str);
        try {
            return parser.parse(is).getDocumentElement();
        } catch (Exception e) {
            throw e;
        }
    }
    
    private boolean compareByteArrays(Byte[] first, Byte[] second){
		if(first.length != second.length){
			return false;
		}

		for(int i=0; i<first.length; i++){
			if (first[i] != second[i]){
				return false;
			}
		}
		return true;
	}

     private boolean compareByteArrays(byte[] first, byte[] second){
		if(first.length != second.length){
			return false;
		}

		for(int i=0; i<first.length; i++){
			if (first[i] != second[i]){
				return false;
			}
		}
		return true;
	}    
}





