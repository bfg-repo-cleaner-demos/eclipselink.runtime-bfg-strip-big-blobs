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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.bind.Binder;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Validator;
import javax.xml.namespace.QName;
import java.util.HashMap;

import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.exceptions.JAXBException;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.jaxb.JAXBSchemaOutputResolver;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.internal.oxm.schema.SchemaModelGenerator;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.jaxb.compiler.Generator;
import org.eclipse.persistence.jaxb.compiler.MarshalCallback;
import org.eclipse.persistence.jaxb.compiler.UnmarshalCallback;
import org.eclipse.persistence.jaxb.javamodel.JavaClass;

/**
 * INTERNAL:
 * <p><b>Purpose:</b>Provide a TopLink implementation of the JAXBContext interface.
 * <p><b>Responsibilities:</b><ul>
 * <li>Create Marshaller instances</li>
 * <li>Create Unmarshaller instances</li>
 * <li>Create Binder instances</li>
 * <li>Create Introspector instances</li>
 * <li>Create Validator instances</li>
 * <li>Generate Schema Files</li>
 * <ul>
 * <p>This is the TopLink JAXB 2.0 implementation of javax.xml.bind.JAXBContext. This class
 * is created by the JAXBContextFactory and is used to create Marshallers, Unmarshallers, Validators,
 * Binders and Introspectors. A JAXBContext can also be used to create Schema Files.
 * 
 * @see javax.xml.bind.JAXBContext
 * @see org.eclipse.persistence.jaxb.JAXBMarshaller
 * @see org.eclipse.persistence.jaxb.JAXBUnmarshaller
 * @see org.eclipse.persistence.jaxb.JAXBBinder
 * @see org.eclipse.persistence.jaxb.JAXBIntrospector
 * 
 * @author mmacivor
 * @since Oracle TopLink 11.1.1.0.0
 */

public class JAXBContext extends javax.xml.bind.JAXBContext {

    private static final Map<String, Boolean> PARSER_FEATURES = new HashMap<String, Boolean>(2);
    static {
        PARSER_FEATURES.put("http://apache.org/xml/features/validation/schema/normalized-value", false);
        PARSER_FEATURES.put("http://apache.org/xml/features/validation/schema/element-default", false);            
    }

    private XMLContext xmlContext;
    private org.eclipse.persistence.jaxb.compiler.Generator generator;    
    private HashMap<QName, Class> qNameToGeneratedClasses;
    private HashMap<String, Class> classToGeneratedClasses;
    private HashMap<QName, Class> qNamesToDeclaredClasses;
    private HashMap<java.lang.reflect.Type, QName> typeToSchemaType;
    private Type[] boundTypes;
    
    public JAXBContext(XMLContext context) {
        super();
        xmlContext = context;
    }
    
    public JAXBContext(XMLContext context, Generator generator, Type[] boundTypes) {
        
        this(context);
        this.generator = generator;
        this.qNameToGeneratedClasses = generator.getMappingsGenerator().getQNamesToGeneratedClasses();
        this.classToGeneratedClasses = generator.getMappingsGenerator().getClassToGeneratedClasses();
        this.qNamesToDeclaredClasses = generator.getMappingsGenerator().getQNamesToDeclaredClasses();
        this.boundTypes = boundTypes;
    }
    
    public XMLContext getXMLContext() {
        return this.xmlContext;
    }
    
    public void generateSchema(SchemaOutputResolver outputResolver) {
        generateSchema(outputResolver, null);
    }
    
    public void generateSchema(SchemaOutputResolver outputResolver, Map<QName, Type> additonalGlobalElements) {
        if (generator == null) {
            List<XMLDescriptor> descriptorsToProcess = new ArrayList<XMLDescriptor>();
            List<Session> sessions = xmlContext.getSessions();
            for (Session session : sessions) {
                Vector<XMLDescriptor> descriptors = session.getProject().getOrderedDescriptors();
                for (XMLDescriptor xDesc : descriptors) {
                    descriptorsToProcess.add(xDesc);
                }
            }
            SchemaModelGenerator smGen = new SchemaModelGenerator();
            smGen.generateSchemas(descriptorsToProcess, null, new JAXBSchemaOutputResolver(outputResolver), additonalGlobalElements);
        } else {
            generator.generateSchemaFiles(outputResolver, additonalGlobalElements);
        }
    }
    
    public Marshaller createMarshaller() {
    	// create a JAXBIntrospector and set it on the marshaller
        JAXBMarshaller marshaller = new JAXBMarshaller(xmlContext.createMarshaller(), new JAXBIntrospector(xmlContext));
        if (generator != null && generator.hasMarshalCallbacks()) {
            // initialize each callback in the map
            for (Iterator callIt = generator.getMarshalCallbacks().keySet().iterator(); callIt.hasNext(); ) {
                MarshalCallback cb = (MarshalCallback) generator.getMarshalCallbacks().get(callIt.next());
                cb.initialize(generator.getClass().getClassLoader());
            }
            marshaller.setMarshalCallbacks(generator.getMarshalCallbacks());
        }
        marshaller.setQNameToGeneratedClasses(this.qNameToGeneratedClasses);
        //marshaller.setClassToGeneratedClasses(this.classToGeneratedClasses);
        marshaller.setJaxbContext(this);
        return marshaller;
    }

    public Unmarshaller createUnmarshaller() {
        JAXBUnmarshaller unmarshaller = new JAXBUnmarshaller(xmlContext.createUnmarshaller(PARSER_FEATURES));
        if (generator != null && generator.hasUnmarshalCallbacks()) {
            // initialize each callback in the map
            for (Iterator callIt = generator.getUnmarshalCallbacks().keySet().iterator(); callIt.hasNext(); ) {
                UnmarshalCallback cb = (UnmarshalCallback) generator.getUnmarshalCallbacks().get(callIt.next());
                cb.initialize(generator.getClass().getClassLoader());
            }
            unmarshaller.setUnmarshalCallbacks(generator.getUnmarshalCallbacks());
        }        
        unmarshaller.setJaxbContext(this);
        return unmarshaller;
    }

    public Validator createValidator() {
        return new JAXBValidator(xmlContext.createValidator());
    }
    
    public Binder createBinder() {
        return new JAXBBinder(this.xmlContext);
    }
    
    public Binder createBinder(Class nodeClass) {
    	if(nodeClass.getName().equals("org.w3c.dom.Node")){
    		return new JAXBBinder(this.xmlContext);
    	}else{
    		throw new UnsupportedOperationException(JAXBException.unsupportedNodeClass(nodeClass.getName()));
    	}
    }
    
    public JAXBIntrospector createJAXBIntrospector() {
        return new JAXBIntrospector(xmlContext);
    }      
    
    public void setQNameToGeneratedClasses(HashMap<QName, Class> qNameToClass) {
    	this.qNameToGeneratedClasses = qNameToClass;
    }
    
    public HashMap<String, Class> getClassToGeneratedClasses() {
    	return classToGeneratedClasses;
    }
    
    public void setClassToGeneratedClasses(HashMap<String, Class> classToClass) {
    	this.classToGeneratedClasses = classToClass;
    }
    
    /**
     * ADVANCED:
     * Adjust the OXM metadata to take into accound ORM mapping metadata,
     */
     public void applyORMMetadata(AbstractSession ormSession) {
    	 this.xmlContext.applyORMMetadata(ormSession);
     }

	public HashMap<QName, Class> getQNamesToDeclaredClasses() {
		return qNamesToDeclaredClasses;
	}

	public void setQNamesToDeclaredClasses(
			HashMap<QName, Class> nameToDeclaredClasses) {
		qNamesToDeclaredClasses = nameToDeclaredClasses;
	}
	
	public Map<String, Class>getArrayClassesToGeneratedClasses(){
		return generator.getAnnotationsProcessor().getArrayClassesToGeneratedClasses();
	}
	
	public Map<Type, Class>getCollectionClassesToGeneratedClasses(){
		return generator.getAnnotationsProcessor().getCollectionClassesToGeneratedClasses();
	}
	
	public void initTypeToSchemaType() {
	    this.typeToSchemaType = new HashMap<Type, QName>();
	    Iterator descriptors = xmlContext.getSession(0).getProject().getOrderedDescriptors().iterator();
	    
	    HashMap defaults = XMLConversionManager.getDefaultJavaTypes();
	    
	    //Add schema types generated for mapped domain classes
	    while(descriptors.hasNext()) {
	        XMLDescriptor next = (XMLDescriptor)descriptors.next();
	        Class javaClass = next.getJavaClass();
	        
	        if(next.getSchemaReference() != null){
		        QName schemaType = next.getSchemaReference().getSchemaContextAsQName(next.getNamespaceResolver());
		        Type type;
		        if(generator != null) {
		            type = generator.getAnnotationsProcessor().getGeneratedClassesToCollectionClasses().get(javaClass);
		            if(type == null) {
		                JavaClass arrayClass = (JavaClass)generator.getAnnotationsProcessor().getGeneratedClassesToArrayClasses().get(javaClass);
		                if(arrayClass != null) {
		                    String arrayClassName = arrayClass.getName();
		                    try {
		                        type = PrivilegedAccessHelper.getClassForName(arrayClassName);
		                    } catch (Exception ex){}
		                }
		            }
		            if(type == null) {
		                type = javaClass;
		            }
		        } else {
		            type = javaClass;
		        }
		        this.typeToSchemaType.put(type, schemaType);
	        }
	    }
	    
	    //Add any types that we didn't generate descriptors for (built in types)
	    if(boundTypes != null) {
	        for(Type next:this.boundTypes) {
	            if(this.typeToSchemaType.get(next) == null) {
	                QName name = null;
	                //Check for annotation overrides
	                if(next instanceof Class) {
	                    name = this.generator.getAnnotationsProcessor().getUserDefinedSchemaTypes().get(((Class)next).getName());
	                }
	                if(name == null) {
	                    //Change default for byte[] to Base64 (JAXB 2.0 default)
	                    if(next == ClassConstants.ABYTE || next == ClassConstants.APBYTE) {
	                        name = XMLConstants.BASE_64_BINARY_QNAME;
	                    } else {
	                        name = (QName)defaults.get(next);
	                    }
	                }
	                if(name != null) {
	                    this.typeToSchemaType.put(next, name);
	                }
	            }
	        }
	    }
	}

	public HashMap<Type, QName> getTypeToSchemaType() {
	    if(typeToSchemaType == null) {
	        initTypeToSchemaType();
	    }
	    return typeToSchemaType;
	}
	
}
