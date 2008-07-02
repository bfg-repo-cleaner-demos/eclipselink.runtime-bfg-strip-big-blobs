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
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.sdo;

import commonj.sdo.DataObject;
import commonj.sdo.Property;
import commonj.sdo.Type;
import commonj.sdo.helper.HelperContext;
import commonj.sdo.impl.HelperProvider;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.eclipse.persistence.sdo.helper.SDOClassLoader;
import org.eclipse.persistence.sdo.helper.SDOTypeHelper;
import org.eclipse.persistence.sdo.helper.SDOXMLHelper;
import org.eclipse.persistence.sdo.helper.SDOXSDHelper;
import org.eclipse.persistence.sdo.helper.extension.SDOUtil;
import org.eclipse.persistence.exceptions.SDOException;
import org.eclipse.persistence.internal.descriptors.Namespace;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.mappings.XMLAnyAttributeMapping;
import org.eclipse.persistence.oxm.mappings.XMLAnyCollectionMapping;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.oxm.schema.XMLSchemaClassPathReference;
import org.eclipse.persistence.oxm.schema.XMLSchemaReference;

public class SDOType implements Type, Serializable {
    private QName qName;
    private boolean open;// if this Type is open
    private boolean isAbstract;// if this is an abstract Type
    protected boolean isDataType;// if this is a dataType
    private List baseTypes;// a list of this Type's base Types (added for now waitting for final decision)
    private List declaredProperties;// a list of Properties defined by this Type.(Optional, leave it for now)
    private transient Map declaredPropertiesMap;
    private List aliasNames;
    private boolean xsd;
    private boolean xsdList;
    private String xsdLocalName;
    private QName xsdType;
    protected transient XMLDescriptor xmlDescriptor;
    private Map propertyValues;
    private SDOProperty changeSummaryProperty;
    private List allProperties;
    private SDOProperty[] allPropertiesArr;
    private List subTypes;
    private boolean finalized;
    private Class javaClass;
    private String javaClassName;
    private String javaImplClassName;
    private Class javaImplClass;
    private List nonFinalizedReferencingProps;
    private List nonFinalizedMappingURIs;

    /** hold a wrapper object for primitive numeric defaults */
    private Object pseudoDefault;

    // hold the context containing all helpers so that we can preserve inter-helper relationships
    protected HelperContext aHelperContext;
    private List appInfoElements;
    private Map appInfoMap;

    /** Open Content setOpen constants */
    private static final String ANY_MAPPING_ATTRIBUTE_NAME = "openContentProperties";
    private static final String ANY_MAPPING_GET_METHOD_NAME = "_getOpenContentPropertiesWithXMLRoots";
    private static final String ANY_MAPPING_SET_METHOD_NAME = "_setOpenContentPropertiesWithXMLRoots";
    private static final String SDO_REF_MAPPING_ATTRIBUTE_NAME = "sdoRef";

    public SDOType(HelperContext helperContext) {
        this((SDOTypeHelper)helperContext.getTypeHelper());
    }

    public SDOType(SDOTypeHelper sdoTypeHelper) {
        this.xmlDescriptor = new XMLDescriptor();
        if(null != sdoTypeHelper) {
            aHelperContext = sdoTypeHelper.getHelperContext();
            this.xmlDescriptor.setNamespaceResolver(sdoTypeHelper.getNamespaceResolver());
        }
    }

    /**
     * INTERNAL:
     * Build up a Type with given name and uri and a default static HelperContext.
     * Use  {@link # SDOType(uri, typeName, aHelperContext)} instead
     * @param uri           the URI of this type
     * @param type_name     the unique of this Type
     */
    public SDOType(String uri, String type_name) {
        // JIRA129 - default to static global context - Do Not use this convenience constructor outside of JUnit testing
        this(uri, type_name, (SDOTypeHelper) HelperProvider.getDefaultContext().getTypeHelper());
    }

    /**
     * Build up a Type with given name and uri
     * @param uri           the URI of this type
     * @param type_name     the unique of this Type
     * @param aContext      the current HelperContext
     */
    public SDOType(String uri, String name, SDOTypeHelper sdoTypeHelper) {
        this(sdoTypeHelper);
        this.qName = new QName(uri, name);
    }

    public QName getQName() {
        return qName;
    }

    public String getName() {
        return qName.getLocalPart();
    }

    public String getURI() {
        String uri = qName.getNamespaceURI();
        if("".equals(uri)) {
            return null;
        } else {
            return uri;
        }
    }

    public Class getInstanceClass() {
        if ((javaClass == null) && (javaClassName != null)) {
            try {
                SDOClassLoader loader = ((SDOXMLHelper)aHelperContext.getXMLHelper()).getLoader();
                if (!isDataType() && (javaImplClass == null)) {
                    //Class interfaceClass = loader.loadClass(getInstanceClassName(), this);
                    javaImplClass = loader.loadClass(getImplClassName(), this);
                    getXmlDescriptor().setJavaClass(javaImplClass);
                }
                javaClass = loader.loadClass(javaClassName, this);
            } catch (ClassNotFoundException e) {
                throw SDOException.classNotFound(e, getURI(), getName());
            } catch (SecurityException e) {
                throw SDOException.classNotFound(e, getURI(), getName());
            }
        }
        return javaClass;
    }

    public boolean isInstance(Object object) {
        if ((!isDataType()) && (object instanceof DataObject)) {
            Type doType = ((DataObject)object).getType();
            if (doType != null) {
                return doType.equals(this);
            }
        }

        //this check is taken from page 77 of the spec
        Class instanceClass = getInstanceClass();
        if (instanceClass != null) {
            return instanceClass.isInstance(object);
        }

        return false;
    }

    public List getProperties() {
        if (allProperties == null) {
            allProperties = new ArrayList();
        }
        return allProperties;
    }

    public SDOProperty getProperty(String propertyName) {
        SDOProperty queriedProperty = (SDOProperty)getDeclaredPropertiesMap().get(propertyName);
        if (null == queriedProperty) {
            for (int i = 0; i < getBaseTypes().size(); i++) {
                queriedProperty = ((SDOType)getBaseTypes().get(i)).getProperty(propertyName);
                if (queriedProperty != null) {
                    break;
                }
            }
        }
        return queriedProperty;
    }

    public boolean isDataType() {
        return isDataType;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isSequenced() {
        return xmlDescriptor.isSequencedObject();
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public List getBaseTypes() {
        if (baseTypes == null) {
            baseTypes = new ArrayList();
        }
        return baseTypes;
    }

    public List getDeclaredProperties() {
        if (declaredProperties == null) {
            declaredProperties = new ArrayList();
        }
        return declaredProperties;
    }

    public List getAliasNames() {
        if (aliasNames == null) {
            aliasNames = new ArrayList();
        }
        return aliasNames;
    }

    /**
     * INTERNAL:
     * Assign a unique string to a type, which belongs to same uri, among types.
     * @param name  a unique string representing a type.
     */
    public void addAliasName(String name) {
        getAliasNames().add(name);
        QName qname = new QName(getURI(), name);
        ((SDOTypeHelper)aHelperContext.getTypeHelper()).getTypesHashMap().put(qname, this);
    }

    /**
     * INTERNAL:
     * Assign a list of alias names to a type, which are unique in URI.
     * @param names  a unique string representing a type.
     */
    public void setAliasNames(List names) {
        for (int i = 0; i < names.size(); i++) {
            addAliasName((String)names.get(i));
        }
    }

    /**
     * INTERNAL:
     * Make this Type an opened Type to allow open content by assigning true value
     * or a Type not to accept any additional properties by assigning false value,
     * {@link isOpen()}.
     * @param bOpen  boolean value implying if this Type is open
     */
    public void setOpen(boolean bOpen) {
        if (isDataType() && bOpen) {
            throw SDOException.typeCannotBeOpenAndDataType(getURI(), getName());
        }
        if (open != bOpen) {
            open = bOpen;
            if (open) {
                List baseTypes = getBaseTypes();
                if ((baseTypes != null) && !baseTypes.isEmpty()) {
                    Type baseType = (Type)baseTypes.get(0);
                    if (!baseType.isOpen()) {
                        addOpenMappings();
                    }
                } else {
                    addOpenMappings();
                }

                for (int i = 0; i < getSubTypes().size(); i++) {
                    SDOType nextSubType = (SDOType)getSubTypes().get(i);
                    nextSubType.setOpen(bOpen);
                }
            }
        }
    }

    private void addOpenMappings() {
        XMLAnyCollectionMapping anyMapping = new XMLAnyCollectionMapping();
        anyMapping.setAttributeName(ANY_MAPPING_ATTRIBUTE_NAME);
        anyMapping.setGetMethodName(ANY_MAPPING_GET_METHOD_NAME);
        anyMapping.setSetMethodName(ANY_MAPPING_SET_METHOD_NAME);
        anyMapping.setUseXMLRoot(true);
        getXmlDescriptor().addMapping(anyMapping);

        XMLAnyAttributeMapping anyAttrMapping = new XMLAnyAttributeMapping();
        anyAttrMapping.setAttributeName("openContentPropertiesAttributes");
        anyAttrMapping.setGetMethodName("_getOpenContentPropertiesAttributesMap");
        anyAttrMapping.setSetMethodName("_setOpenContentPropertiesAttributesMap");
        getXmlDescriptor().addMapping(anyAttrMapping);
    }

    /**
     * INTERNAL:
     * Change this Type's abstract setting. If it is true, this Type can't be instantiated and typically serve as base Type.
     * @param makeAbstract    boolean value implying if this Type is abstract.
     */
    public void setAbstract(boolean makeAbstract) {
        //TODO: if isDataType can't be abstract
        isAbstract = makeAbstract;
    }

    /**
     * INTERNAL:
     * If set as true, this Type specifies Sequenced DataObjects.
     * @param sequenced     boolean value implying if this type is sequenced.
     */
    public void setSequenced(boolean sequenced) {
        xmlDescriptor.setSequencedObject(sequenced);
    }

    /**
     * INTERNAL:
     * Set this Type to a simple Type by passing in boolean value true. Otherwise,
     * If boolean value is passed in, instances of this type implement DataObject.
     * @param datatype      boolean value implying if it is a simple Type
     */
    public void setDataType(boolean datatype) {
        if (datatype && isOpen()) {
            throw SDOException.typeCannotBeOpenAndDataType(getURI(), getName());
        }
        isDataType = datatype;
        if (datatype) {
            setFinalized(true);
        }
    }

    /**
     * INTERNAL:
     * Set a list of Types as This Type's base Types.
     * @param bTypes    a list types to become this Type's base Type.
     */
    public void setBaseTypes(List bTypes) {
        if (bTypes != null) {
            for (int i = 0; i < bTypes.size(); i++) {
                addBaseType((SDOType)bTypes.get(i));
            }
        } else {
            baseTypes = null;
        }
    }

    /**
     * INTERNAL:
     * @param type    a  type to become this Type's base Type.
     */
    public void addBaseType(SDOType type) {
        if (!this.getBaseTypes().contains(type)) {
            getBaseTypes().add(type);

            updateSubtypes(type);

            type.getSubTypes().add(this);
            if (type.isOpen() && this.isOpen()) {
                //don't want any mappings on this descriptor
                DatabaseMapping anyCollectionMapping = getXmlDescriptor().getMappingForAttributeName(ANY_MAPPING_ATTRIBUTE_NAME);
                getXmlDescriptor().getMappings().remove(anyCollectionMapping);

                DatabaseMapping anyAttrMapping = getXmlDescriptor().getMappingForAttributeName("openContentPropertiesAttributes");
                getXmlDescriptor().getMappings().remove(anyAttrMapping);
            }

            //int increaseBy = type.getProperties().size();
            //increaseIndices(increaseBy);
        }
    }

    private void updateSubtypes(Type baseType) {
        getProperties().addAll(0, baseType.getProperties());
        for (int i = 0; i < getSubTypes().size(); i++) {
            SDOType nextSubType = (SDOType)getSubTypes().get(i);
            nextSubType.updateSubtypes(baseType);
        }
    }

    /**
      * INTERNAL:
      * Sets the Java class that this type represents.
      * @param aClass the Java class that this type represents.
      */
    public void setInstanceClass(Class aClass) {
        javaClass = aClass;
        if (javaClass != null) {
            javaClassName = javaClass.getName();

            /*    if(isDataType()) {
                 setInstanceProperty(SDOConstants.JAVA_CLASS_PROPERTY, javaClassName);
                }
                */
        }
        if (getXmlDescriptor() != null) {
            getXmlDescriptor().setJavaClass(aClass);
        }
    }

    /**
      * INTERNAL:
      * Set if this property was declared in an XML schema.
      * @param bXsd a boolean representing if this property was declared in an XML schema
      */
    public void setXsd(boolean bXsd) {
        xsd = bXsd;
    }

    /**
      * INTERNAL:
      * Returns if this property was declared in an XML schema.  Defaults to false.
      * @return if this property was declared in an XML schema
      */
    public boolean isXsd() {
        return xsd;
    }

    /**
      * INTERNAL:
      * Set the local name of this property.
      * @param xsdLocalName a String representing the local name of this property if it was declared in an XML schema
      */
    public void setXsdLocalName(String xsdLocalNameString) {
        xsdLocalName = xsdLocalNameString;
    }

    /**
      * INTERNAL:
      * Returns the  local name of the Property.
      * @return the local name of the property.
      */
    public String getXsdLocalName() {
        return xsdLocalName;
    }

    /**
     * INTERNAL:
     * @param property
    */
    public void addDeclaredProperty(Property property) {
        addDeclaredProperty((SDOProperty)property);
    }

    /**
     * INTERNAL:
     * @param property
    */
    public void addDeclaredProperty(SDOProperty property) {
        int end = getDeclaredProperties().size();
        addDeclaredProperty(property, end);
    }

    /**
    * INTERNAL:
    * @param property
    */
    public void addDeclaredProperty(Property property, int index) {
        addDeclaredProperty((SDOProperty)property, index);
    }

    /**
    * INTERNAL:
    * @param property
    */
    public void addDeclaredProperty(SDOProperty property, int index) {
        if (!getDeclaredPropertiesMap().containsKey(property.getName())) {
            int currentSize = getDeclaredProperties().size();

            int allSize = getProperties().size();
            int insertPlace = allSize - currentSize + index;

            //updateSubTypesProps
            for (int i = 0; i < getSubTypes().size(); i++) {
                SDOType nextSubType = (SDOType)getSubTypes().get(i);
                nextSubType.updateIndices(insertPlace, property);
            }

            getDeclaredProperties().add(index, property);
            getProperties().add(insertPlace, property);

            ((SDOProperty)property).setContainingType(this);

            getDeclaredPropertiesMap().put(property.getName(), property);
            for (int j = 0; j < property.getAliasNames().size(); j++) {
                getDeclaredPropertiesMap().put(property.getAliasNames().get(j), property);
            }
            if ((property.getType() != null) && (property.getType().isChangeSummaryType())) {
                changeSummaryProperty = property;
            }
        }
    }

    /**
     * INTERNAL:
     */
    public void removeDeclaredProperties(Property p) {
        getDeclaredProperties().remove(p);
        getDeclaredPropertiesMap().remove(p.getName());
        getProperties().remove(p);
    }

    /**
     * INTERNAL:
     */
    public Map getDeclaredPropertiesMap() {
        if (null == declaredPropertiesMap) {
            declaredPropertiesMap = new HashMap();
        }
        return declaredPropertiesMap;
    }

    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        return super.equals(object);
    }

    /**
      * INTERNAL:
      * Sets the name of the Java class that this type represents.
      * @param instanceClassName the Java class that this type represents.
      */
    public void setInstanceClassName(String instanceClassName) {
        javaClassName = instanceClassName;
        javaClass = null;
        // TODO: below would cause an infinite loop, but if users call setinstanceclassname, open content prop wont get set with out this
        //if(isDataType()) {
        //   setInstanceProperty(SDOConstants.JAVA_CLASS_PROPERTY, instanceClassName);
        // }
    }

    /**
      * INTERNAL:
      * Returns the Java class name that this type represents.
      * @return the Java class name.
      */
    public String getInstanceClassName() {
        if ((javaClassName == null) && (javaClass != null)) {
            javaClassName = javaClass.getName();
        }
        return javaClassName;
    }

    /**
      * INTERNAL:
      * Set if this type is an xsd:list in the schema
      * @param xsdList a boolean representing if this type represents an xsd:list in the schema
      */
    public void setXsdList(boolean anXsdList) {
        xsdList = anXsdList;
    }

    /**
      * INTERNAL:
      * Indicates if this Type is an xsd:list in the schema
      * @return true if this Type represents an xsd:list in the schema
      */
    public boolean isXsdList() {
        return xsdList;
    }

    /**
      * INTERNAL:
      * Set the XMLDescriptor on this Type
      * @param anXMLDescriptor
      */
    public void setXmlDescriptor(XMLDescriptor anXMLDescriptor) {
        xmlDescriptor = anXMLDescriptor;
    }
   
    public XMLDescriptor getXmlDescriptor() {
        return xmlDescriptor;
    }

    /**
      * INTERNAL:
      * Get the XMLDescriptor associated with this Type or generate a new one.
      */
    public void initializeNamespaces(List namespaceResolvers) {
        if (!isDataType()) {
            NamespaceResolver nr = new NamespaceResolver();

            // copy namespaces between resolvers for well known and SDO namespaces
            if (namespaceResolvers != null) {
                for (int i = 0; i < namespaceResolvers.size(); i++) {
                    NamespaceResolver nextNR = (NamespaceResolver)namespaceResolvers.get(i);
                    if (nextNR != null) {
                        for (int j = 0, size = nextNR.getNamespaces().size(); j < size; j++) {
                            Namespace nextNamespace = (Namespace)nextNR.getNamespaces().get(j);
                            if ((!nextNamespace.getPrefix().equals(XMLConstants.XMLNS)) && (!nextNamespace.getNamespaceURI().equals(XMLConstants.SCHEMA_URL)) &&
                                (!nextNamespace.getNamespaceURI().equals(SDOConstants.SDOJAVA_URL)) && (!nextNamespace.getNamespaceURI().equals(SDOConstants.SDOXML_URL)) &&
                                (!nextNamespace.getNamespaceURI().equals(SDOConstants.SDO_URL))) {
                                String newPrefix = ((SDOTypeHelper)aHelperContext.getTypeHelper()).addNamespace(nextNamespace.getPrefix(), nextNamespace.getNamespaceURI());
                                nr.put(newPrefix, nextNamespace.getNamespaceURI());
                            }
                        }
                    }
                }
            }
            xmlDescriptor.setNamespaceResolver(nr);
            if (getURI() != null) {
                String prefix = ((SDOTypeHelper)aHelperContext.getTypeHelper()).getPrefix(getURI());
                xmlDescriptor.getNamespaceResolver().put(prefix, getURI());
            }
            xmlDescriptor.getNamespaceResolver().put(XMLConstants.SCHEMA_INSTANCE_PREFIX, XMLConstants.SCHEMA_INSTANCE_URL);
        }
    }

    /**
     * INTERNAL:
     * Convenience method that sets up class indicator and @sdoRef
     * attribute.
     * 
     * @param xdesc
     * @param pCls
     */
    private void addClassIndicator(XMLDescriptor xdesc, Class pCls) {
        XMLField field = (XMLField)getXmlDescriptor().buildField("@xsi:type");
        xdesc.getInheritancePolicy().setClassIndicatorField(field);

        String parentIndicator = getName();
        String parentPrefix = xdesc.getNamespaceResolver().resolveNamespaceURI(getURI());
        if (parentPrefix != null) {
            parentIndicator = parentPrefix + SDOConstants.SDO_XPATH_NS_SEPARATOR_FRAGMENT + parentIndicator;
        }
        xdesc.getInheritancePolicy().addClassIndicator(pCls, parentIndicator);

        // only add the @sdoRef attribute if necessary
        if (xdesc.getMappingForAttributeName(SDO_REF_MAPPING_ATTRIBUTE_NAME) == null) {
            String sdoPrefix = ((SDOTypeHelper)aHelperContext.getTypeHelper()).getPrefix(SDOConstants.SDO_URL);
            XMLDirectMapping sdoRefMapping = new XMLDirectMapping();
            sdoRefMapping.setAttributeName(SDO_REF_MAPPING_ATTRIBUTE_NAME);

            XMLField xmlField = new XMLField("@" + sdoPrefix + SDOConstants.SDO_XPATH_NS_SEPARATOR_FRAGMENT + SDOConstants.CHANGESUMMARY_REF);
            xmlField.getXPathFragment().setNamespaceURI(SDOConstants.SDO_URL);
            xmlField.getLastXPathFragment().setNamespaceURI(SDOConstants.SDO_URL);

            sdoRefMapping.setField(xmlField);
            xdesc.addMapping(sdoRefMapping);
        }
    }

    public void setupInheritance(SDOType parentType) {
        if (parentType == null) {
            // root of inheritance
            addClassIndicator(getXmlDescriptor(), getImplClass());
        } else {
            if ((parentType.getURI() != null) && (!parentType.getURI().equals(SDOConstants.SDO_URL))) {
                // set parent descriptor indicator if necessary
                if (!parentType.getXmlDescriptor().hasInheritance()) {
                    addClassIndicator(parentType.getXmlDescriptor(), parentType.getImplClass());
                }

                XMLDescriptor parentDescriptor = (XMLDescriptor)parentType.getXmlDescriptor().getInheritancePolicy().getRootParentDescriptor();
                NamespaceResolver parentNR = parentDescriptor.getNonNullNamespaceResolver();
                if (parentNR != null) {
                    for (int i = 0; i < parentNR.getNamespaces().size(); i++) {
                        Namespace nextNamespace = (Namespace)parentNR.getNamespaces().get(i);
                        if ((!nextNamespace.getPrefix().equals(XMLConstants.XMLNS)) && (!nextNamespace.getNamespaceURI().equals(XMLConstants.SCHEMA_URL)) &&
                            (!nextNamespace.getNamespaceURI().equals(SDOConstants.SDOJAVA_URL)) && (!nextNamespace.getNamespaceURI().equals(SDOConstants.SDOXML_URL)) &&
                            (!nextNamespace.getNamespaceURI().equals(SDOConstants.SDO_URL))) {
                            getXmlDescriptor().getNonNullNamespaceResolver().put(nextNamespace.getPrefix(), nextNamespace.getNamespaceURI());
                        }
                    }
                }
                if (getInstanceClassName() != null) {
                    String indicator = getName();
                    String prefix = parentDescriptor.getNamespaceResolver().resolveNamespaceURI(getURI());
                    if (prefix == null) {
                        prefix = getXmlDescriptor().getNamespaceResolver().resolveNamespaceURI(getURI());
                        if (prefix != null) {
                            parentDescriptor.getNamespaceResolver().put(prefix, getURI());
                        }
                    }
                    if (prefix != null) {
                        indicator = prefix + SDOConstants.SDO_XPATH_NS_SEPARATOR_FRAGMENT + indicator;
                    }
                    Class implClass = getImplClass();
                    parentDescriptor.getInheritancePolicy().addClassIndicator(implClass, indicator);
                    parentDescriptor.getInheritancePolicy().setShouldReadSubclasses(true);
                    Class parentClass = parentType.getImplClass();
                    getXmlDescriptor().getInheritancePolicy().setParentClass(parentClass);
                    getXmlDescriptor().getInheritancePolicy().setParentDescriptor(parentType.getXmlDescriptor());
                    parentType.getXmlDescriptor().getNamespaceResolver().put(XMLConstants.SCHEMA_INSTANCE_PREFIX, XMLConstants.SCHEMA_INSTANCE_URL);
                    getXmlDescriptor().getNamespaceResolver().put(XMLConstants.SCHEMA_INSTANCE_PREFIX, XMLConstants.SCHEMA_INSTANCE_URL);
                }
            }
        }
        // now setup inheritance for any subtypes
        for (int i = 0; i < subTypes.size(); i++) {
            SDOType nextSubType = (SDOType)subTypes.get(i);
            if (!nextSubType.isDataType() && (nextSubType.getBaseTypes() != null) && (nextSubType.getBaseTypes().size() > 0)) {
                nextSubType.setupInheritance(this);
            }
        }
    }

    /**
     * INTERNAL:
     * For this Type generate classes
     * @param packageName
     * @param nr
     */
    public void preInitialize(String packageName, List namespaceResolvers) {
        String instanceClassName = getInstanceClassName();
        if (null == instanceClassName) {
            if (null == packageName) {
                String uri = getURI();
                if (null == uri) {
                    packageName = SDOUtil.getDefaultPackageName() + SDOConstants.JAVA_PACKAGE_NAME_SEPARATOR;
                } else {
                    packageName = SDOUtil.getPackageNameFromURI(uri) + SDOConstants.JAVA_PACKAGE_NAME_SEPARATOR;
                }
            }

            // Verify and fix any Class name that does not conform to conventions
            // run the class name through the JAXB mangler
            String mangledClassName = SDOUtil.className(getName(), true);

            // we will not fix any type collision at this time as a result of class renaming
            // write fully qualified java class name
            StringBuffer fullClassName = new StringBuffer(packageName);
            fullClassName.append(mangledClassName);
            setInstanceClassName(fullClassName.toString());
        }
        AbstractSessionLog.getLog().log(AbstractSessionLog.FINER,//
                                        "sdo_type_generation_processing_type", //
                                        new Object[] { Helper.getShortClassName(getClass()), getInstanceClassName() });

        initializeNamespaces(namespaceResolvers);
        getXmlDescriptor().setJavaClassName(getImplClassName());
        // load classes by classloader by getting the current instance class
        getInstanceClass();

        // See SDOResolvable enhancement
        String schemaContext = getName();
        if (getXmlDescriptor().getNamespaceResolver() != null) {
            String prefix = getXmlDescriptor().getNamespaceResolver().resolveNamespaceURI(getURI());
            if ((prefix != null) && !prefix.equals(SDOConstants.EMPTY_STRING)) {
                schemaContext = prefix + SDOConstants.SDO_XPATH_NS_SEPARATOR_FRAGMENT + schemaContext;
            }
        }
        String schemaContextWithSlash = SDOConstants.SDO_XPATH_SEPARATOR_FRAGMENT + schemaContext;

        XMLSchemaReference schemaRef = new XMLSchemaClassPathReference();
        schemaRef.setSchemaContext(schemaContextWithSlash);
        schemaRef.setType(XMLSchemaReference.COMPLEX_TYPE);
        getXmlDescriptor().setSchemaReference(schemaRef);

    }

    /**
      * INTERNAL:
      */
    public void postInitialize() {
        String idPropName = (String)get(SDOConstants.ID_PROPERTY);
        if (idPropName != null) {
            SDOProperty idProp = (SDOProperty)getProperty(idPropName);
            if (idProp != null) {
                String targetxpath = idProp.getQualifiedXPath(getURI(), true);
                getXmlDescriptor().addPrimaryKeyFieldName(targetxpath);
            }
        }
        setFinalized(true);
        for (int i = 0; i < getNonFinalizedReferencingProps().size(); i++) {
            SDOProperty nextProp = (SDOProperty)getNonFinalizedReferencingProps().get(i);
            String nextURI = (String)getNonFinalizedMappingURIs().get(i);
            nextProp.buildMapping(nextURI, nextProp.getIndexInType());
        }
        // set @sdoRef attribute mapping for complex types that are not involved in inheritance
        if (!isDataType() && getBaseTypes().size() == 0 && getSubTypes().size() == 0) {
            String sdoPrefix = ((SDOTypeHelper)aHelperContext.getTypeHelper()).getPrefix(SDOConstants.SDO_URL);
            XMLDirectMapping sdoRefMapping = new XMLDirectMapping();
            sdoRefMapping.setAttributeName(SDO_REF_MAPPING_ATTRIBUTE_NAME);

            XMLField xmlField = new XMLField("@" + sdoPrefix + SDOConstants.SDO_XPATH_NS_SEPARATOR_FRAGMENT + SDOConstants.CHANGESUMMARY_REF);
            xmlField.getXPathFragment().setNamespaceURI(SDOConstants.SDO_URL);
            xmlField.getLastXPathFragment().setNamespaceURI(SDOConstants.SDO_URL);
            sdoRefMapping.setField(xmlField);
            xmlDescriptor.addMapping(sdoRefMapping);
        }
    }

    /**
      * INTERNAL:
      */
    public void setImplClassName(String implClassName) {
        javaImplClassName = implClassName;
        javaImplClass = null;
    }

    /**
      * INTERNAL:
      */
    public String getImplClassName() {
        if ((javaImplClassName == null) && (javaClassName != null)) {
            javaImplClassName = javaClassName + SDOConstants.SDO_IMPL_NAME;
        }
        return javaImplClassName;
    }

    /**
      * INTERNAL:
      */
    public Class getImplClass() {
        if ((javaImplClass == null) && (getImplClassName() != null)) {
            try {
                SDOClassLoader loader = ((SDOXMLHelper)aHelperContext.getXMLHelper()).getLoader();
                javaImplClass = loader.loadClass(getImplClassName(), this);
            } catch (ClassNotFoundException e) {
                throw SDOException.classNotFound(e, getURI(), getName());
            } catch (SecurityException e) {
                throw SDOException.classNotFound(e, getURI(), getName());
            }
        }
        return javaImplClass;
    }

    public Object get(Property property) {
        //TODO: SDO Jira issue 17        
        return getPropertyValues().get(property);
    }

    public List getInstanceProperties() {
        //TODO: SDO Jira issue 17                
        return new ArrayList(getPropertyValues().keySet());
    }

    /**
      * INTERNAL:
      */
    public void setPropertyValues(Map properties) {
        this.propertyValues = properties;
    }

    /**
      * INTERNAL:
      */
    public Map getPropertyValues() {
        if (propertyValues == null) {
            propertyValues = new HashMap();
        }
        return propertyValues;
    }

    /**
     * INTERNAL:
     *
     * @param property
     * @param value
     */
    public void setInstanceProperty(Property property, Object value) {
        if (property.equals(SDOConstants.JAVA_CLASS_PROPERTY) && value instanceof String) {
            setInstanceClassName((String)value);
        }      
        getPropertyValues().put(property, value);
    }

    /**
      * INTERNAL:
      */
    public SDOProperty getChangeSummaryProperty() {
        return changeSummaryProperty;
    }

    /**
     * INTERNAL:
     */
    public SDOProperty[] getPropertiesArray() {
        if ((allPropertiesArr == null) || (allPropertiesArr.length != getProperties().size())) {
            List l = getProperties();
            int s = (l == null) ? 0 : l.size();
            if (s > 0) {
                allPropertiesArr = (SDOProperty[])l.toArray(new SDOProperty[s]);
            } else {
                // initialize an empty array
                allPropertiesArr = new SDOProperty[0];
            }
        }
        return allPropertiesArr;
    }

    /**
      * INTERNAL:
      */
    public void setSubTypes(List subTypesList) {
        subTypes = subTypesList;
    }

    /**
      * INTERNAL:
      */
    public List getSubTypes() {
        if (subTypes == null) {
            subTypes = new ArrayList();
        }
        return subTypes;
    }

    private void updateIndices(int insertPosition, Property property) {
        int declaredSize = getDeclaredProperties().size();
        SDOProperty nextProp = null;
        for (int i = 0; i < declaredSize; i++) {
            nextProp = (SDOProperty)getDeclaredProperties().get(i);
            nextProp.setIndexInType(nextProp.getIndexInType() + 1);
        }
        getProperties().add(insertPosition, property);
        int subTypesSize = getSubTypes().size();

        SDOType nextSubType = null;
        for (int i = 0; i < subTypesSize; i++) {
            nextSubType = (SDOType)getSubTypes().get(i);
            nextSubType.updateIndices(insertPosition, property);
        }
    }

    /**
      * INTERNAL:
      */
    public void setXsdType(QName xsdTypeQName) {
        xsdType = xsdTypeQName;
    }

    /**
      * INTERNAL:
      */
    public QName getXsdType() {
        return xsdType;
    }

    /**
      * INTERNAL:
      */
    public void setFinalized(boolean bFinalized) {
        finalized = bFinalized;
    }

    /**
      * INTERNAL:
      */
    public boolean isFinalized() {
        return finalized;
    }

    /**
      * INTERNAL:
      */
    public void setNonFinalizedReferencingProps(List nonFinalizedReferencingProps) {
        this.nonFinalizedReferencingProps = nonFinalizedReferencingProps;
    }

    /**
      * INTERNAL:
      */
    public List getNonFinalizedReferencingProps() {
        if (nonFinalizedReferencingProps == null) {
            nonFinalizedReferencingProps = new ArrayList();
        }
        return nonFinalizedReferencingProps;
    }

    /**
      * INTERNAL:
      */
    public void setNonFinalizedMappingURIs(List nonFinalizedMappingURIsList) {
        nonFinalizedMappingURIs = nonFinalizedMappingURIsList;
    }

    /**
      * INTERNAL:
      */
    public List getNonFinalizedMappingURIs() {
        if (nonFinalizedMappingURIs == null) {
            nonFinalizedMappingURIs = new ArrayList();
        }
        return nonFinalizedMappingURIs;
    }

    /**
      * INTERNAL:
      */
    public void setAppInfoElements(List appInfoElementsList) {
        appInfoElements = appInfoElementsList;
    }

    /**
      * INTERNAL:
      */
    public List getAppInfoElements() {
        return appInfoElements;
    }

    /**
      * INTERNAL:
      */
    public Map getAppInfoMap() {
        if (appInfoMap == null) {
            appInfoMap = ((SDOXSDHelper)aHelperContext.getXSDHelper()).buildAppInfoMap(appInfoElements);
        }
        return appInfoMap;
    }

    /**
     * INTERNAL:
     * Return the wrapped initial value for the primitive numeric (when not defined)
     * See p.45 of Java Spec 4th edition.
     * See p.85 Sect 9.3 of the SDO Spec.
     * @return aDefault Object (primitive numerics) or null (DataObjects, String, Lists)
     */
    public Object getPseudoDefault() {
        return pseudoDefault;
    }

    /**
     * INTERNAL:
     * Set an Object wrapper around primitive numeric types
     * @param anObject
     */
    public void setPseudoDefault(Object anObject) {
        pseudoDefault = anObject;
    }
    
    /**
     * Return the HelperContext object associated with this type.
     * For example, the typeHelper associated with the returned HelperContext would contain this type
     */
    public HelperContext getHelperContext(){
      return aHelperContext;
    }

    public boolean isChangeSummaryType() {
        return false;
    }
    
    public boolean isDataObjectType() {
        return false;
    }
    
    public boolean isTypeType() {
        return false;
    }

    public boolean isOpenSequencedType() {
        return false;
    }

}