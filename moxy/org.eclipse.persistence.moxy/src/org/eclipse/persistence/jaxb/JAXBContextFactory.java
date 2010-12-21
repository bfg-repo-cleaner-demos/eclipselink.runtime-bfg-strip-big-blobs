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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.helper.ConversionManager;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.jaxb.compiler.*;
import org.eclipse.persistence.jaxb.javamodel.reflection.AnnotationHelper;
import org.eclipse.persistence.jaxb.javamodel.reflection.JavaModelImpl;
import org.eclipse.persistence.jaxb.javamodel.reflection.JavaModelInputImpl;
import org.eclipse.persistence.jaxb.xmlmodel.JavaType;
import org.eclipse.persistence.jaxb.xmlmodel.XmlBindings;
import org.eclipse.persistence.jaxb.xmlmodel.XmlBindings.JavaTypes;
import org.eclipse.persistence.internal.jaxb.JaxbClassLoader;
import org.eclipse.persistence.internal.jaxb.SessionEventListener;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.exceptions.EclipseLinkException;
import org.eclipse.persistence.exceptions.SessionLoaderException;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.oxm.XMLLogin;
import org.eclipse.persistence.oxm.platform.SAXPlatform;
import org.eclipse.persistence.oxm.platform.XMLPlatform;
import org.eclipse.persistence.sessions.Project;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * <p>
 * <b>Purpose:</b>An EclipseLink specific JAXBContextFactory. This class can be specified in a
 * jaxb.properties file to make use of EclipseLink's JAXB 2.1 implementation.
 * <p>
 * <b>Responsibilities:</b>
 * <ul>
 * <li>Create a JAXBContext from an array of Classes and a Properties object</li>
 * <li>Create a JAXBContext from a context path and a classloader</li>
 * </ul>
 * <p>
 * This class is the entry point into in EclipseLink's JAXB 2.1 Runtime. It provides the required
 * factory methods and is invoked by javax.xml.bind.JAXBContext.newInstance() to create new
 * instances of JAXBContext. When creating a JAXBContext from a contextPath, the list of classes is
 * derived either from an ObjectFactory class (schema-to-java) or a jaxb.index file
 * (java-to-schema).
 *
 * @author mmacivor
 * @see javax.xml.bind.JAXBContext
 * @see org.eclipse.persistence.jaxb.JAXBContext
 * @see org.eclipse.persistence.jaxb.compiler.Generator
 */
public class JAXBContextFactory {

    public static final String ECLIPSELINK_OXM_XML_KEY = "eclipselink-oxm-xml";
    public static final String DEFAULT_TARGET_NAMESPACE_KEY = "defaultTargetNamespace";
    public static final String ANNOTATION_HELPER_KEY = "annotationHelper";
    public static final String PKG_SEPARATOR = ".";

    /**
     * Create a JAXBContext on the array of Class objects.  The JAXBContext will
     * also be aware of classes reachable from the classes in the array.
     */
    public static javax.xml.bind.JAXBContext createContext(Class[] classesToBeBound, Map properties) throws JAXBException {
        ClassLoader loader = null;
        if (classesToBeBound.length > 0) {
            loader = classesToBeBound[0].getClassLoader();
            if(null == loader) {
                loader = getDefaultClassLoader();
            }
        }
        return createContext(classesToBeBound, properties, loader);
    }

    /**
     * Create a JAXBContext on the array of Class objects.  The JAXBContext will
     * also be aware of classes reachable from the classes in the array.
     */
    public static javax.xml.bind.JAXBContext createContext(Class[] classesToBeBound, Map properties, ClassLoader classLoader) throws JAXBException {
        Type[] types = new Type[classesToBeBound.length];
        System.arraycopy(classesToBeBound, 0, types, 0, classesToBeBound.length);
        return createContext(types, properties, classLoader);
    }

    /**
     * Create a JAXBContext on context path.  The JAXBContext will
     * also be aware of classes reachable from the classes on the context path.
     */
    public static javax.xml.bind.JAXBContext createContext(String contextPath, ClassLoader classLoader) throws JAXBException {
        return createContext(contextPath, classLoader, null);
    }

    /**
     * Create a JAXBContext on context path.  The JAXBContext will
     * also be aware of classes reachable from the classes on the context path.
     */
    public static javax.xml.bind.JAXBContext createContext(String contextPath, ClassLoader classLoader, Map properties) throws JAXBException {
        if(null == classLoader) {
            classLoader = getDefaultClassLoader();
        }
        EclipseLinkException sessionLoadingException = null;
        try {
            XMLContext xmlContext = new XMLContext(contextPath, classLoader);
            return new org.eclipse.persistence.jaxb.JAXBContext(xmlContext);
        } catch (ValidationException vex) {
            if (vex.getErrorCode() != ValidationException.NO_SESSIONS_XML_FOUND) {
                sessionLoadingException = vex;
            }
        } catch (SessionLoaderException ex) {
            sessionLoadingException = ex;
        } catch (Exception ex) {
            throw new JAXBException(ex);
        }
        ArrayList<Class> classes = new ArrayList<Class>();

        // Check properties map for eclipselink-oxm.xml entries
        Map<String, XmlBindings> xmlBindingMap = getXmlBindingsFromProperties(properties, classLoader);
        classes = getXmlBindingsClassesFromMap(xmlBindingMap, classLoader, classes);

        StringTokenizer tokenizer = new StringTokenizer(contextPath, ":");
        while (tokenizer.hasMoreElements()) {
            String path = tokenizer.nextToken();
            try {
                Class objectFactory = classLoader.loadClass(path + ".ObjectFactory");
                if (isJAXB2ObjectFactory(objectFactory)) {
                    classes.add(objectFactory);
                }
            } catch (Exception ex) {
                // if there's no object factory, don't worry about it. Check for jaxb.index next
            }
            try {
                // try to load package info just to be safe
                classLoader.loadClass(path + ".package-info");
            } catch (Exception ex) {
            }
            // Next check for a jaxb.index file in case there's one available
            InputStream jaxbIndex = classLoader.getResourceAsStream(path.replace('.', '/') + "/jaxb.index");
            if (jaxbIndex != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(jaxbIndex));
                try {
                    String line = reader.readLine();
                    while (line != null) {
                        String className = path + PKG_SEPARATOR + line.trim();
                        try {
                            classes.add(classLoader.loadClass(className));
                        } catch (Exception ex) {
                            // just ignore for now if the class isn't available.
                        }
                        line = reader.readLine();
                    }
                } catch (Exception ex) {
                }
            }
        }
        if (classes.size() == 0) {
            org.eclipse.persistence.exceptions.JAXBException jaxbException = org.eclipse.persistence.exceptions.JAXBException.noObjectFactoryOrJaxbIndexInPath(contextPath);
            if (sessionLoadingException != null) {
                jaxbException.setInternalException(sessionLoadingException);
            }
            throw new JAXBException(jaxbException);
        }
        Class[] classArray = new Class[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            classArray[i] = (Class) classes.get(i);
        }
        return createContext(classArray, properties, classLoader, xmlBindingMap);
    }

    /**
     * Create a JAXBContext on the array of Type objects.  The JAXBContext will
     * also be aware of classes reachable from the types in the array.  The
     * preferred means of creating a Type aware JAXBContext is to create the
     * JAXBContext with an array of TypeMappingInfo objects.
     */
    public static javax.xml.bind.JAXBContext createContext(Type[] typesToBeBound, Map properties, ClassLoader classLoader) throws JAXBException {
        Map<Type, TypeMappingInfo> typeToTypeMappingInfo = new HashMap<Type, TypeMappingInfo>();
        TypeMappingInfo[] typeMappingInfo = new TypeMappingInfo[typesToBeBound.length];
        for(int i = 0; i < typesToBeBound.length; i++) {
            TypeMappingInfo tmi = new TypeMappingInfo();
            tmi.setType(typesToBeBound[i]);
            typeToTypeMappingInfo.put(typesToBeBound[i], tmi);
            typeMappingInfo[i] = tmi;
        }

        JAXBContext context = (JAXBContext)createContext(typeMappingInfo, properties, classLoader);
        context.setTypeToTypeMappingInfo(typeToTypeMappingInfo);

        return context;
    }

    /**
     * Create a JAXBContext on the array of TypeMappingInfo objects.  The
     * JAXBContext will also be aware of classes reachable from the types in the
     * array.  This is the preferred means of creating a Type aware JAXBContext.
     */
    public static javax.xml.bind.JAXBContext createContext(TypeMappingInfo[] typesToBeBound, Map properties, ClassLoader classLoader) throws JAXBException {
        if(null == classLoader) {
            classLoader = getDefaultClassLoader();
        }
         // Check properties map for eclipselink-oxm.xml entries
        Map<String, XmlBindings> xmlBindings = getXmlBindingsFromProperties(properties, classLoader);
        String defaultTargetNamespace = null;
        AnnotationHelper annotationHelper = null;
        if(properties != null) {
            defaultTargetNamespace = (String)properties.get(DEFAULT_TARGET_NAMESPACE_KEY);
            annotationHelper = (AnnotationHelper)properties.get(ANNOTATION_HELPER_KEY);
        }
        for (Entry<String, XmlBindings> entry : xmlBindings.entrySet()) {
            typesToBeBound = getXmlBindingsClasses(entry.getValue(), classLoader, typesToBeBound);
        }

        JaxbClassLoader loader = new JaxbClassLoader(classLoader, typesToBeBound);
        typesToBeBound = updateTypesWithObjectFactory(typesToBeBound, loader);

        JavaModelImpl jModel;
        if(annotationHelper != null) {
        	jModel = new JavaModelImpl(loader, annotationHelper);
        } else {
        	jModel = new JavaModelImpl(loader);
        }
        
        // create Map of package names to metadata complete indicators
        Map<String, Boolean> metadataComplete = new HashMap<String, Boolean>();
        for (String packageName : xmlBindings.keySet()) {
            if (xmlBindings.get(packageName).isXmlMappingMetadataComplete()) {
                metadataComplete.put(packageName, true);
            }
        }
        if (metadataComplete.size() > 0) {
            jModel.setMetadataCompletePackageMap(metadataComplete);
        }
        
        JavaModelInputImpl inputImpl = new JavaModelInputImpl(typesToBeBound, jModel);
        try {
            Generator generator = new Generator(inputImpl, typesToBeBound, inputImpl.getJavaClasses(), null, xmlBindings, classLoader, defaultTargetNamespace);
            return createContext(generator, properties, classLoader, loader, typesToBeBound);
        } catch (Exception ex) {
            throw new JAXBException(ex.getMessage(), ex);
        }
    }

    /**
     * This means of creating a JAXBContext is aimed at creating a JAXBContext
     * based on method parameters.  This method is useful when JAXB is used as
     * the binding layer for a Web Service provider.
     */
    private static JAXBContext createContext(Class[] classesToBeBound, Map properties, ClassLoader classLoader, Map<String, XmlBindings> xmlBindings) throws JAXBException {
        JaxbClassLoader loader = new JaxbClassLoader(classLoader, classesToBeBound);
        String defaultTargetNamespace = null;
        AnnotationHelper annotationHelper = null;
        if(properties != null) {
            defaultTargetNamespace = (String)properties.get(DEFAULT_TARGET_NAMESPACE_KEY);
            annotationHelper = (AnnotationHelper)properties.get(ANNOTATION_HELPER_KEY);
        }
        
        JavaModelImpl jModel;
        if(annotationHelper != null) {
        	jModel = new JavaModelImpl(loader, annotationHelper);
        } else {
        	jModel = new JavaModelImpl(loader);
        }
        
        // create Map of package names to metadata complete indicators
        Map<String, Boolean> metadataComplete = new HashMap<String, Boolean>();
        for (String packageName : xmlBindings.keySet()) {
            if (xmlBindings.get(packageName).isXmlMappingMetadataComplete()) {
                metadataComplete.put(packageName, true);
            }
        }
        if (metadataComplete.size() > 0) {
            jModel.setMetadataCompletePackageMap(metadataComplete);
        }
        
        JavaModelInputImpl inputImpl = new JavaModelInputImpl(classesToBeBound, jModel);
        try {
            Generator generator = new Generator(inputImpl, xmlBindings, loader, defaultTargetNamespace);
            return createContext(generator, properties, classLoader, loader, classesToBeBound);
        } catch (Exception ex) {
            throw new JAXBException(ex.getMessage(), ex);
        }
    }

    private static JAXBContext createContext(Generator generator, Map properties, ClassLoader classLoader, JaxbClassLoader loader, Type[] typesToBeBound) throws Exception {
        Project proj = generator.generateProject();
        ConversionManager conversionManager = null;
        if (classLoader != null) {
            conversionManager = new ConversionManager();
            conversionManager.setLoader(loader);
        } else {
            conversionManager = ConversionManager.getDefaultManager();
        }
        proj.convertClassNamesToClasses(conversionManager.getLoader());
        // need to make sure that the java class is set properly on each
        // descriptor when using java classname - req'd for JOT api implementation
        for (Iterator<ClassDescriptor> descriptorIt = proj.getOrderedDescriptors().iterator(); descriptorIt.hasNext();) {
            ClassDescriptor descriptor = descriptorIt.next();
            if (descriptor.getJavaClass() == null) {
                descriptor.setJavaClass(conversionManager.convertClassNameToClass(descriptor.getJavaClassName()));
            }
        }

        // disable instantiation policy validation during descriptor initialization
        SessionEventListener eventListener = new SessionEventListener();
        eventListener.setShouldValidateInstantiationPolicy(false);
        XMLPlatform platform = new SAXPlatform();
        platform.getConversionManager().setLoader(loader);
        XMLContext xmlContext = new XMLContext(proj, loader, eventListener);

        if(generator.getAnnotationsProcessor().getPackageToNamespaceMappings().size() > 1){
            ((XMLLogin)xmlContext.getSession(0).getDatasourceLogin()).setEqualNamespaceResolvers(false);
        }

        return new JAXBContext(xmlContext, generator, typesToBeBound);
    }

    private static JAXBContext createContext(Generator generator, Map properties, ClassLoader classLoader, JaxbClassLoader loader, TypeMappingInfo[] typesToBeBound) throws Exception {
        Project proj = generator.generateProject();
        ConversionManager conversionManager = null;
        if (classLoader != null) {
            conversionManager = new ConversionManager();
            conversionManager.setLoader(loader);
        } else {
            conversionManager = ConversionManager.getDefaultManager();
        }
        proj.convertClassNamesToClasses(conversionManager.getLoader());
        // need to make sure that the java class is set properly on each
        // descriptor when using java classname - req'd for JOT api implementation
        for (Iterator<ClassDescriptor> descriptorIt = proj.getOrderedDescriptors().iterator(); descriptorIt.hasNext();) {
            ClassDescriptor descriptor = descriptorIt.next();
            if (descriptor.getJavaClass() == null) {
                descriptor.setJavaClass(conversionManager.convertClassNameToClass(descriptor.getJavaClassName()));
            }
        }

        // disable instantiation policy validation during descriptor initialization
        SessionEventListener eventListener = new SessionEventListener();
        eventListener.setShouldValidateInstantiationPolicy(false);
        XMLPlatform platform = new SAXPlatform();
        platform.getConversionManager().setLoader(loader);
        XMLContext xmlContext = new XMLContext(proj, loader, eventListener);

        if(generator.getAnnotationsProcessor().getPackageToNamespaceMappings().size() > 1){
            ((XMLLogin)xmlContext.getSession(0).getDatasourceLogin()).setEqualNamespaceResolvers(false);
        }

        return new JAXBContext(xmlContext, generator, typesToBeBound);
    }

    private static boolean isJAXB2ObjectFactory(Class objectFactoryClass) {
        try {
            Class xmlRegistry = PrivilegedAccessHelper.getClassForName("javax.xml.bind.annotation.XmlRegistry");
            if (objectFactoryClass.isAnnotationPresent(xmlRegistry)) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Convenience method for processing a properties map and creating a map of 
     * package names to XmlBindings instances.
     *
     * It is assumed that the given map's key will be ECLIPSELINK_OXM_XML_KEY,
     * and the value will be:
     * 
     * 1)  Map<String, Object>
     *     - Object is one of those listed in 3) below
     * 2)  List<Object>
     *     - Object is one of those listed in 3) below
     *     - Bindings file must contain package-name attribute on 
     *       xml-bindings element
     * 3)  One of:
     *     - java.io.File
     *     - java.io.InputStream
     *     - java.io.Reader
     *     - java.net.URL
     *     - javax.xml.stream.XMLEventReader
     *     - javax.xml.stream.XMLStreamReader
     *     - javax.xml.transform.Source
     *     - org.w3c.dom.Node
     *     - org.xml.sax.InputSource
     *      
     *     - Bindings file must contain package-name attribute on 
     *       xml-bindings element
     */
    public static Map<String, XmlBindings> getXmlBindingsFromProperties(Map properties, ClassLoader classLoader) {
        Map<String, XmlBindings> bindings = new HashMap<String, XmlBindings>();
        Object value;
        if (properties != null && ((value = properties.get(ECLIPSELINK_OXM_XML_KEY)) != null)) {
            // handle Map<String, Object>
            if (value instanceof Map) {
                Map<String, Object> metadataFiles = null;
                try {
                    metadataFiles = (Map<String, Object>) properties.get(ECLIPSELINK_OXM_XML_KEY);
                } catch (ClassCastException x) {
                    throw org.eclipse.persistence.exceptions.JAXBException.incorrectValueParameterTypeForOxmXmlKey();
                }
                if (metadataFiles != null) {
                    for(Entry<String, Object> entry : metadataFiles.entrySet()) {
                        String key = null;
                        try {
                            key = entry.getKey();
                            if (key == null) {
                                throw org.eclipse.persistence.exceptions.JAXBException.nullMapKey();
                            }
                        } catch (ClassCastException cce) {
                            throw org.eclipse.persistence.exceptions.JAXBException.incorrectKeyParameterType();
                        }
                        Object metadataSource = entry.getValue();
                        if (metadataSource == null) {
                            throw org.eclipse.persistence.exceptions.JAXBException.nullMetadataSource(key);
                        }
                        XmlBindings binding = getXmlBindings(metadataSource, classLoader);
                        if (binding != null) {
                            bindings.put(key, binding);
                        }
                    }
                }
            // handle List<Object>
            } else if (value instanceof List) {
                for (Object metadataSource : (List) value) {
                    if (metadataSource == null) {
                        throw org.eclipse.persistence.exceptions.JAXBException.nullMetadataSource();
                    }
                    bindings = processBindingFile(bindings, metadataSource, classLoader);
                }
            // handle Object
            } else {
                bindings = processBindingFile(bindings, value, classLoader);
            }
        }
        return bindings;
    }
    
    /**
     * Processing a bindings file and add it to a given Map of package name to binding
     * files.
     * 
     * @param originalBindings Map of bindings to be updated
     * @param bindingHandle handle to bindings file
     * @param classLoader
     * @return
     */
    private static Map<String, XmlBindings> processBindingFile(Map<String, XmlBindings> originalBindings, Object bindingHandle, ClassLoader classLoader) {
        Map<String, XmlBindings> bindingMap = originalBindings;
        XmlBindings binding = getXmlBindings(bindingHandle, classLoader);
        if (binding != null) {
            String key = binding.getPackageName();
            if (key.equals(XMLProcessor.DEFAULT)) {
                throw org.eclipse.persistence.exceptions.JAXBException.packageNotSetForBindingException();
            }
            // may need to prepend the package-name
            JavaTypes jTypes = binding.getJavaTypes();
            if (jTypes != null) {
                for (JavaType javaType : jTypes.getJavaType()) {
                    String javaTypeName = javaType.getName();
                    if (!(javaTypeName.contains(key))) {
                        javaType.setName(key + PKG_SEPARATOR + javaTypeName);
                    }
                }
            }
            bindingMap.put(key, binding);
        }
        return bindingMap;
    }

    /**
     * Convenience method for creating an XmlBindings object based on a given Object. The method
     * will load the eclipselink metadata model and unmarshal the Object. This assumes that the
     * Object represents the eclipselink-oxm.xml metadata file to be unmarshalled.
     * 
     * @param metadata assumed to be one of:  File, InputSource, InputStream, Reader, Source
     */
    private static XmlBindings getXmlBindings(Object metadata, ClassLoader classLoader) {
        XmlBindings xmlBindings = null;
        Unmarshaller unmarshaller;
        // only create the JAXBContext for our XmlModel once
        JAXBContext jaxbContext = CompilerHelper.getXmlBindingsModelContext();
        try {
            unmarshaller = jaxbContext.createUnmarshaller();
            if (metadata instanceof File) {
                xmlBindings = (XmlBindings) unmarshaller.unmarshal((File) metadata);
            } else if (metadata instanceof InputSource) {
                xmlBindings = (XmlBindings) unmarshaller.unmarshal((InputSource) metadata);
            } else if (metadata instanceof InputStream) {
                xmlBindings = (XmlBindings) unmarshaller.unmarshal((InputStream) metadata);
            } else if (metadata instanceof Node) {
                xmlBindings = (XmlBindings) unmarshaller.unmarshal((Node) metadata);
            } else if (metadata instanceof Reader) {
                xmlBindings = (XmlBindings) unmarshaller.unmarshal((Reader) metadata);
            } else if (metadata instanceof Source) {
                xmlBindings = (XmlBindings) unmarshaller.unmarshal((Source) metadata);
            } else if (metadata instanceof URL) {
                xmlBindings = (XmlBindings) unmarshaller.unmarshal((URL) metadata);
            } else if (metadata instanceof XMLEventReader) {
                xmlBindings = (XmlBindings) unmarshaller.unmarshal((XMLEventReader) metadata);
            } else if (metadata instanceof XMLStreamReader) {
                xmlBindings = (XmlBindings) unmarshaller.unmarshal((XMLStreamReader) metadata);
            } else {
                throw org.eclipse.persistence.exceptions.JAXBException.incorrectValueParameterTypeForOxmXmlKey();
            }
        } catch (JAXBException jaxbEx) {
            throw org.eclipse.persistence.exceptions.JAXBException.couldNotUnmarshalMetadata(jaxbEx);
        }
        return xmlBindings;
    }

    /**
     * Convenience method that returns an array of Types based on a given XmlBindings. The resulting
     * array will not contain duplicate entries.
     */
    private static TypeMappingInfo[] getXmlBindingsClasses(XmlBindings xmlBindings, ClassLoader classLoader, TypeMappingInfo[] existingTypes) {
        JavaTypes jTypes = xmlBindings.getJavaTypes();
        if (jTypes != null) {
            List<Class> existingClasses = new ArrayList<Class>(existingTypes.length);
             for (TypeMappingInfo typeMappingInfo : existingTypes) {
                Type type  = typeMappingInfo.getType();
                if(type == null){
                    throw org.eclipse.persistence.exceptions.JAXBException.nullTypeOnTypeMappingInfo(typeMappingInfo.getXmlTagName());
                }
                 // ignore ParameterizedTypes
                 if (type instanceof Class) {
                     Class cls = (Class) type;
                     existingClasses.add(cls);
                 }
             }

             List<TypeMappingInfo> additionalTypeMappingInfos = new ArrayList<TypeMappingInfo>(jTypes.getJavaType().size());

             for (JavaType javaType : jTypes.getJavaType()) {
                try {
                    Class nextClass = classLoader.loadClass(javaType.getName());
                    if(!(existingClasses.contains(nextClass))){
                        TypeMappingInfo typeMappingInfo = new TypeMappingInfo();
                        typeMappingInfo.setType(nextClass);
                        additionalTypeMappingInfos.add(typeMappingInfo);
                        existingClasses.add(nextClass);
                    }
                } catch (ClassNotFoundException e) {
                    throw org.eclipse.persistence.exceptions.JAXBException.couldNotLoadClassFromMetadata(javaType.getName());
                }
            }

            TypeMappingInfo[] allTypeMappingInfos = new TypeMappingInfo[existingTypes.length + additionalTypeMappingInfos.size()];
            System.arraycopy(existingTypes, 0, allTypeMappingInfos, 0, existingTypes.length);
            Object[] additionalTypes = additionalTypeMappingInfos.toArray();
            System.arraycopy(additionalTypes, 0, allTypeMappingInfos, existingTypes.length, additionalTypes.length);
            return allTypeMappingInfos;
        }else{
            return existingTypes;
        }
    }

    /**
     * Convenience method that returns a list of Classes based on a given XmlBindings and an array
     * of existing classes. The resulting array will not contain duplicate entries.
     */
    private static ArrayList<Class> getXmlBindingsClasses(XmlBindings xmlBindings, ClassLoader classLoader, ArrayList<Class> existingClasses) {
        ArrayList<Class> additionalClasses = existingClasses;
        JavaTypes jTypes = xmlBindings.getJavaTypes();
        if (jTypes != null) {
            for (JavaType javaType : jTypes.getJavaType()) {
                try {
                    Class jClass = classLoader.loadClass(javaType.getName());
                    if (!additionalClasses.contains(jClass)) {
                        additionalClasses.add(jClass);
                    }
                } catch (ClassNotFoundException e) {
                    throw org.eclipse.persistence.exceptions.JAXBException.couldNotLoadClassFromMetadata(javaType.getName());
                }
            }
        }
        return additionalClasses;
    }

    /**
     * Convenience method that returns an array of Classes based on a map given XmlBindings and an
     * array of existing classes. The resulting array will not contain duplicate entries.
     */
    private static ArrayList<Class> getXmlBindingsClassesFromMap(Map<String, XmlBindings> xmlBindingMap, ClassLoader classLoader, ArrayList<Class> existingClasses) {
        ArrayList<Class> additionalClasses = existingClasses;
        // for each xmlBindings
        for (Entry<String, XmlBindings> entry : xmlBindingMap.entrySet()) {
            additionalClasses = getXmlBindingsClasses(entry.getValue(), classLoader, additionalClasses);
        }
        return additionalClasses;
    }

    private static TypeMappingInfo[] updateTypesWithObjectFactory(TypeMappingInfo[] typeMappingInfos, ClassLoader loader) {
        ArrayList<TypeMappingInfo> updatedTypes = new ArrayList<TypeMappingInfo>();
        for (TypeMappingInfo next : typeMappingInfos) {
            if (!(updatedTypes.contains(next))) {
                updatedTypes.add(next);
            }
            Type theType = next.getType();
            if (theType instanceof Class) {
                if (((Class) theType).getPackage() != null) {
                    String packageName = ((Class) theType).getPackage().getName();
                    try {
                        Class objectFactoryClass = loader.loadClass(packageName + ".ObjectFactory");
                        if (!(updatedTypes.contains(objectFactoryClass))) {
                            TypeMappingInfo objectFactoryTypeMappingInfo = new TypeMappingInfo();
                            objectFactoryTypeMappingInfo.setType(objectFactoryClass);
                            updatedTypes.add(objectFactoryTypeMappingInfo);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        }
        return updatedTypes.toArray(new TypeMappingInfo[updatedTypes.size()]);
    }

    private static ClassLoader getDefaultClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

}