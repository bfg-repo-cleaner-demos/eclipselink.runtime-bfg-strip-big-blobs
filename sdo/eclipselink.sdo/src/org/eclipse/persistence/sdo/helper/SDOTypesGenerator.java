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
package org.eclipse.persistence.sdo.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import org.eclipse.persistence.sdo.SDOConstants;
import org.eclipse.persistence.sdo.SDOProperty;
import org.eclipse.persistence.sdo.SDOType;
import org.eclipse.persistence.sdo.helper.extension.SDOUtil;
import org.eclipse.persistence.exceptions.SDOException;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.internal.oxm.schema.model.Annotation;
import org.eclipse.persistence.internal.oxm.schema.model.Any;
import org.eclipse.persistence.internal.oxm.schema.model.Attribute;
import org.eclipse.persistence.internal.oxm.schema.model.Choice;
import org.eclipse.persistence.internal.oxm.schema.model.ComplexType;
import org.eclipse.persistence.internal.oxm.schema.model.Element;
import org.eclipse.persistence.internal.oxm.schema.model.Import;
import org.eclipse.persistence.internal.oxm.schema.model.Include;
import org.eclipse.persistence.internal.oxm.schema.model.Schema;
import org.eclipse.persistence.internal.oxm.schema.model.Sequence;
import org.eclipse.persistence.internal.oxm.schema.model.SimpleComponent;
import org.eclipse.persistence.internal.oxm.schema.model.SimpleType;
import org.eclipse.persistence.internal.oxm.schema.model.TypeDefParticle;
import org.eclipse.persistence.internal.oxm.schema.model.Union;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import commonj.sdo.Property;
import commonj.sdo.Type;
import commonj.sdo.helper.HelperContext;

/**
 * <p><b>Purpose</b>: Called from XSDHelper define methods to generate SDO Types from a Schema
 *
 * @see commonj.sdo.XSDHelper
 */
public class SDOTypesGenerator extends SchemaParser {
    private java.util.Map<QName, Type> generatedTypes;    
    private java.util.Map<QName, Property> generatedGlobalElements;
    private java.util.Map<QName, Property> generatedGlobalAttributes;
    private String packageName;
    private List<NonContainmentReference> nonContainmentReferences;
    private Map<Type, List<GlobalRef>> globalRefs;
    private boolean isImportProcessor;
    
    public SDOTypesGenerator(HelperContext aContext) {
        super(aContext);
    }

    protected void processImport(Import theImport) {
        try {
        	processImportIncludeInternal(theImport);
        } catch (Exception e) {
            throw SDOException.errorProcessingImport(theImport.getSchemaLocation(), theImport.getNamespace(), e);
        }
    }

    protected void processInclude(Include theInclude) {
        try {
        	processImportIncludeInternal(theInclude);
        } catch (Exception e) {
            throw SDOException.errorProcessingInclude(theInclude.getSchemaLocation(), e);
        }
    }

    /**
     * INTERNAL:
     * This function is referenced by processImport or processInclude possibly recursively
     * @param Include theImportOrInclude
     * @throws Exception
     */
    private void processImportIncludeInternal(Include theImportOrInclude) throws Exception {
        if (theImportOrInclude.getSchema() != null) {
            SDOTypesGenerator generator = new SDOTypesGenerator(aHelperContext);
            generator.setGeneratedTypes(getGeneratedTypes());
            // Both imports and includes are treated the same when checking for a mid-schema tree walk state
            generator.setIsImportProcessor(true);
            // May throw an IAE if a global type: local part cannot be null when creating a QName			
            java.util.List<Type> importedTypes = generator.define(theImportOrInclude.getSchema(), isReturnAllTypes(), isProcessImports());
            processedComplexTypes.putAll(generator.processedComplexTypes);
            processedSimpleTypes.putAll(generator.processedSimpleTypes);
            processedElements.putAll(generator.processedElements);
            processedAttributes.putAll(generator.processedAttributes);
            if (null != importedTypes) {
                for (int i = 0, size = importedTypes.size(); i < size; i++) {
                    Type nextType = importedTypes.get(i);
                    String name = nextType.getName();
                    QName qname = new QName(nextType.getURI(), name);
                    getGeneratedTypes().put(qname, nextType);
                }
            }

            //copy over any global properties            
            Iterator<QName> globalPropsIter = generator.getGeneratedGlobalElements().keySet().iterator();
            while (globalPropsIter.hasNext()) {
                QName nextKey = globalPropsIter.next();
                getGeneratedGlobalElements().put(nextKey, generator.getGeneratedGlobalElements().get(nextKey));
            }
            
            globalPropsIter = generator.getGeneratedGlobalAttributes().keySet().iterator();
            while (globalPropsIter.hasNext()) {
                QName nextKey = globalPropsIter.next();
                getGeneratedGlobalAttributes().put(nextKey, generator.getGeneratedGlobalAttributes().get(nextKey));
            }

            //copy over any unfinished globalRefs
            Iterator<Type> globalRefsIter = generator.getGlobalRefs().keySet().iterator();
            while (globalRefsIter.hasNext()) {
                Type nextKey = globalRefsIter.next();
                getGlobalRefs().put(nextKey, generator.getGlobalRefs().get(nextKey));
            }
        }
    }

    public List<Type> define(Source xsdSource, SchemaResolver schemaResolver) {
        return define(xsdSource, schemaResolver, false, true);
    }

    public List<Type> define(Source xsdSource, SchemaResolver schemaResolver, boolean includeAllTypes, boolean processImports) {
        Schema schema = getSchema(xsdSource, schemaResolver);
        return define(schema, includeAllTypes, processImports);
    }

    public List<Type> define(Schema schema, boolean includeAllTypes, boolean processImports) {
    	// Initialize the List of Types before we process the schema
    	List<Type> returnList = new ArrayList<Type>();
    	
    	setReturnAllTypes(includeAllTypes);
        setProcessImports(processImports);
        processSchema(schema);
             
        returnList.addAll(getGeneratedTypes().values());
        
        if(!this.isImportProcessor()){      
            List descriptorsToAdd = new ArrayList(getGeneratedTypes().values());
            Iterator<Type> iter = getGeneratedTypes().values().iterator();            
            while (iter.hasNext()) {
                SDOType nextSDOType = (SDOType)iter.next();
                if (!nextSDOType.isFinalized()) {
                    //Only throw this error if we're not processing an import.
                    throw SDOException.typeReferencedButNotDefined(nextSDOType.getURI(), nextSDOType.getName());
                }        
            }
            Iterator<Property> propertiesIter = getGeneratedGlobalElements().values().iterator();
            while (propertiesIter.hasNext()) {
                SDOProperty nextSDOProperty = (SDOProperty)propertiesIter.next();
                if (!nextSDOProperty.isFinalized()) {
                    //Only throw this error if we're not processing an import.
                    throw SDOException.referencedPropertyNotFound(nextSDOProperty.getUri(), nextSDOProperty.getName());
                }
            }
            
            propertiesIter = getGeneratedGlobalAttributes().values().iterator();
            while (propertiesIter.hasNext()) {
                SDOProperty nextSDOProperty = (SDOProperty)propertiesIter.next();
                if (!nextSDOProperty.isFinalized()) {
                    //Only throw this error if we're not processing an import.
                    throw SDOException.referencedPropertyNotFound(nextSDOProperty.getUri(), nextSDOProperty.getName());
                }
            }
            
            
            iter = getGeneratedTypes().values().iterator();
            //If we get her all types were finalized correctly
            while (iter.hasNext()) {
               SDOType nextSDOType = (SDOType)iter.next();                              
               
               ((SDOTypeHelper)aHelperContext.getTypeHelper()).addType(nextSDOType);
                                                       
               if (!nextSDOType.isDataType() && nextSDOType.getBaseTypes().size() == 0 && nextSDOType.getSubTypes().size() > 0) {
                    nextSDOType.setupInheritance(null);
               } else if (!nextSDOType.isDataType() && nextSDOType.getBaseTypes().size() > 0 && !getGeneratedTypes().values().contains(nextSDOType.getBaseTypes().get(0))) {
                    SDOType baseType = (SDOType)nextSDOType.getBaseTypes().get(0);
                    while (baseType != null) {
                        descriptorsToAdd.add(baseType);
                        if (baseType.getBaseTypes().size() == 0) {
                            descriptorsToAdd.add(baseType);
                            //baseType should now be root of inheritance
                            baseType.setupInheritance(null);
                            baseType = null;
                        } else {
                            baseType = (SDOType)baseType.getBaseTypes().get(0);
                        }
                    }
                }
            }
            ((SDOXMLHelper)aHelperContext.getXMLHelper()).addDescriptors(descriptorsToAdd);

            //go through generatedGlobalProperties and add to xsdhelper
            Iterator<QName> qNameIter = getGeneratedGlobalElements().keySet().iterator();
            while (qNameIter.hasNext()) {
              QName nextQName = (QName)qNameIter.next();              
              SDOProperty nextSDOProperty = (SDOProperty)getGeneratedGlobalElements().get(nextQName);              
              ((SDOXSDHelper)aHelperContext.getXSDHelper()).addGlobalProperty(nextQName, nextSDOProperty, true);
            }
            
            qNameIter = getGeneratedGlobalAttributes().keySet().iterator();
            while (qNameIter.hasNext()) {
              QName nextQName = (QName)qNameIter.next();              
              SDOProperty nextSDOProperty = (SDOProperty)getGeneratedGlobalAttributes().get(nextQName);              
              ((SDOXSDHelper)aHelperContext.getXSDHelper()).addGlobalProperty(nextQName, nextSDOProperty, false);
            }
         
            Iterator<List<GlobalRef>> globalRefsIter = getGlobalRefs().values().iterator();
            while (globalRefsIter.hasNext()) {
                List<GlobalRef> nextList = (List)globalRefsIter.next();
                if (nextList.size() > 0) {
                    GlobalRef ref = nextList.get(0);
                    throw SDOException.referencedPropertyNotFound(((SDOProperty)ref.getProperty()).getUri(), ref.getProperty().getName());
                }
            }
        }

        return returnList;
    }

    public boolean typesExists(String targetNamespace, String sdoTypeName) {
        boolean alreadyProcessed = super.typesExists(targetNamespace, sdoTypeName);
        if (!alreadyProcessed) {
            SDOType lookup = (SDOType)aHelperContext.getTypeHelper().getType(targetNamespace, sdoTypeName);
            if ((lookup != null) && lookup.isFinalized()) {
                if (isReturnAllTypes()) {
                    QName qname = new QName(targetNamespace, sdoTypeName);
                    getGeneratedTypes().put(qname, lookup);
                }
                return true;
            }
        }

        return alreadyProcessed;
    }

    public void startNewComplexType(String targetNamespace, String sdoTypeName, String xsdLocalName, ComplexType complexType) {
        SDOType currentType = createSDOTypeForName(targetNamespace, sdoTypeName, xsdLocalName);

        if (complexType.isMixed()) {
            currentType.setSequenced(true);
            // currentType.setOpen(true); Remove as part of SDO JIRA-106           
        }

        if (complexType.getAnyAttribute() != null) {
            currentType.setOpen(true);
        }
        currentType.setAbstract(complexType.isAbstractValue());
        currentType.setDataType(false);

        String value = (String)complexType.getAttributesMap().get(SDOConstants.SDOXML_ALIASNAME_QNAME);
        if (value != null) {
            List names = (List)XMLConversionManager.getDefaultXMLManager().convertObject(value, List.class);
            currentType.setAliasNames(names);
        }

        String sequencedValue = (String)complexType.getAttributesMap().get(SDOConstants.SDOXML_SEQUENCE_QNAME);
        if (sequencedValue != null) {
            Boolean sequencedBoolean = new Boolean(sequencedValue);
            currentType.setSequenced(sequencedBoolean.booleanValue());
        }
        Annotation annotation = complexType.getAnnotation();
        if (annotation != null) {
            List documentation = annotation.getDocumentation();
            if ((documentation != null) && (documentation.size() > 0)) {
                currentType.setInstanceProperty(SDOConstants.DOCUMENTATION_PROPERTY, documentation);
            }
        }
        currentType.preInitialize(packageName, namespaceResolvers);
        if (complexType.getAnnotation() != null) {
            currentType.setAppInfoElements(complexType.getAnnotation().getAppInfo());
        }
    }

    public void finishComplexType(String targetNamespace, String defaultNamespace, String name) {
        SDOType currentType = getSDOTypeForName(targetNamespace, defaultNamespace, false, name);
        currentType.postInitialize();
    }

    public void finishNestedComplexType(String targetNamespace, String defaultNamespace, TypeDefParticle typeDefParticle, String name) {
    }

    public void startNewSimpleType(String targetNamespace, String sdoTypeName, String xsdLocalName,SimpleType simpleType) {
        SDOType currentType = createSDOTypeForName(targetNamespace, sdoTypeName,xsdLocalName);
        currentType.setDataType(true);

        if (simpleType.getAnnotation() != null) {
            currentType.setAppInfoElements(simpleType.getAnnotation().getAppInfo());
        }
    }

    public void finishSimpleType(String targetNamespace, String defaultNamespace, String sdoTypeName, SimpleType simpleType) {
        SDOType currentType = getSDOTypeForName(targetNamespace, defaultNamespace, false, sdoTypeName);
        String value = (String)simpleType.getAttributesMap().get(SDOConstants.SDOXML_ALIASNAME_QNAME);
        if (value != null) {
            List names = (List)XMLConversionManager.getDefaultXMLManager().convertObject(value, List.class);
            currentType.setAliasNames(names);
        }

        String instanceClassValue = (String)simpleType.getAttributesMap().get(SDOConstants.SDOJAVA_INSTANCECLASS_QNAME);
        if (instanceClassValue != null) {
            //TODO: also set class?            
            currentType.setInstanceProperty(SDOConstants.JAVA_CLASS_PROPERTY, instanceClassValue);
            currentType.setBaseTypes(null);
        }

        String extendedInstanceClassValue = (String)simpleType.getAttributesMap().get(SDOConstants.SDOJAVA_EXTENDEDINSTANCECLASS_QNAME);
        if (extendedInstanceClassValue != null) {
            //TODO: also set class?
            //TODO: make sure extended Instance class extend the base Type's instance class
            currentType.setInstanceClassName(extendedInstanceClassValue);
        }
        currentType.postInitialize();
    }

    protected void processUnion(String targetNamespace, String defaultNamespace, String sdoTypeName, Union union) {
        if (union != null) {
            java.util.List allMemberTypes = union.getAllMemberTypes();
            SDOType type = getSDOTypeForName(targetNamespace, defaultNamespace, sdoTypeName);
            String firstInstanceClassName = null;
            for (int i = 0; i < allMemberTypes.size(); i++) {
                String nextMemberType = (String)allMemberTypes.get(i);
                SDOType typeForMemberType = getTypeForName(targetNamespace, defaultNamespace, nextMemberType);
                if (i == 0) {
                    firstInstanceClassName = typeForMemberType.getInstanceClassName();
                    if (firstInstanceClassName == null) {
                        break;
                    }
                } else {
                    String nextClassName = typeForMemberType.getInstanceClassName();
                    if (!firstInstanceClassName.equals(nextClassName)) {
                        type.setInstanceClass(java.lang.Object.class);
                        return;
                    }
                }

                // TODO: process union spec page. 84
            }
            if (firstInstanceClassName != null) {
                type.setInstanceClassName(firstInstanceClassName);
            } else {
                type.setInstanceClass(java.lang.Object.class);
            }
        }
    }

    protected void processList(String targetNamespace, String defaultNamespace, String sdoTypeName, org.eclipse.persistence.internal.oxm.schema.model.List list) {
        if (list != null) {
            //String itemType = list.getItemType();
            //SimpleType simpleType = list.getSimpleType();
            SDOType type = getSDOTypeForName(targetNamespace, defaultNamespace, sdoTypeName);
            type.setXsdList(true);

            //TODO: process union spec page. 84        
        }
    }

    public void processBaseType(String targetNamespace, String defaultNamespace, String ownerName, String qualifiedName, boolean simpleContentExtension) {
        if (qualifiedName == null) {
            return;
        }
        SDOType baseType = getSDOTypeForName(targetNamespace, defaultNamespace, qualifiedName);

        if (simpleContentExtension) {
            Type ownerType = getTypeForName(targetNamespace, defaultNamespace, ownerName);
            if (ownerType != null) {
                SDOProperty prop = new SDOProperty(aHelperContext);
                prop.setName("value");
                prop.setType(baseType);
                prop.setValueProperty(true);
                prop.setInstanceProperty(SDOConstants.XMLELEMENT_PROPERTY, Boolean.TRUE);
                ((SDOType)ownerType).addDeclaredProperty(prop);
                prop.buildMapping(null, -1);
                prop.setFinalized(true);

            }
            return;
        }

        List<Type> baseTypes = new ArrayList<Type>();
        baseTypes.add(baseType);

        if (ownerName != null) {
            SDOType owner = getTypeForName(targetNamespace, defaultNamespace, ownerName);
            if (owner.isDataType()) {
                owner.setInstanceClassName(baseType.getInstanceClassName());
                if (baseType.getInstanceClass() != null) {
                    owner.setInstanceClass(baseType.getInstanceClass());
                }

                QName baseQName = getQNameForString(defaultNamespace, qualifiedName);
                if ((baseQName.equals(XMLConstants.BASE_64_BINARY_QNAME)) || (baseQName.equals(XMLConstants.HEX_BINARY_QNAME)) || (baseQName.equals(XMLConstants.DATE_QNAME)) || (baseQName.equals(XMLConstants.TIME_QNAME)) || (baseQName.equals(XMLConstants.DATE_TIME_QNAME))) {
                    owner.setXsdType(baseQName);
                }
            }

            if (!owner.getBaseTypes().contains(baseType)) {
                owner.addBaseType(baseType);
            }
        }

        //currentType.setBaseTypes(baseTypes);
        //TODO: need owner currentType.setOpen(true);
    }

    public void processTypeDef(String targetNamespace, String defaultNamespace, String owner, TypeDefParticle typeDefParticle) {
        SDOType currentType = getTypeForName(targetNamespace, defaultNamespace, owner);

        if (maxOccursGreaterThanOne(typeDefParticle.getMaxOccurs())) {
            if (!currentType.isSequenced() && shouldBeSequenced(typeDefParticle)) {
                currentType.setSequenced(true);
            }
        }
    }

    private boolean shouldBeSequenced(TypeDefParticle typeDefParticle) {
        List elements = typeDefParticle.getElements();
        if ((elements != null) && (elements.size() > 1)) {
            return true;
        }
        if (typeDefParticle instanceof Sequence) {
            List allItems = ((Sequence)typeDefParticle).getOrderedElements();
            for (int i = 0; i < allItems.size(); i++) {
                Object nextItem = allItems.get(i);
                if (nextItem instanceof TypeDefParticle) {
                    boolean sequenced = shouldBeSequenced((TypeDefParticle)nextItem);
                    if (sequenced) {
                        return true;
                    }
                }
            }
        } else if (typeDefParticle instanceof Choice) {
            List allItems = ((Choice)typeDefParticle).getOrderedElements();
            for (int i = 0; i < allItems.size(); i++) {
                Object nextItem = allItems.get(i);
                if (nextItem instanceof TypeDefParticle) {
                    boolean sequenced = shouldBeSequenced((TypeDefParticle)nextItem);
                    if (sequenced) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void processAny(String targetNamespace, String defaultNamespace, Any any, String owner, TypeDefParticle typeDefParticle) {
        if (any == null) {
            return;
        }
        SDOType currentType = getTypeForName(targetNamespace, defaultNamespace, owner);

        if (typeDefParticle instanceof Choice && ((Choice)typeDefParticle).hasAny()) {
            currentType.setOpen(true);
        } else if (typeDefParticle instanceof Sequence && ((Sequence)typeDefParticle).hasAny()) {
            currentType.setOpen(true);
        }

        if (maxOccursGreaterThanOne(any.getMaxOccurs())) {
            currentType.setSequenced(true);
        }

        //TODO: need owner currentType.setOpen(true);??
    }

    public void processSimpleElement(//
    String targetNamespace,//
    String defaultNamespace,//
    String ownerName,//
    TypeDefParticle typeDefParticle,//
    Element element,//
    boolean isQualified,//
    boolean isGlobal,//
    boolean isMany) {
        if (element == null) {
            return;
        }

        String manyValue = (String)element.getAttributesMap().get(SDOConstants.SDOXML_MANY_QNAME);

        if (manyValue != null) {
            Boolean manyBoolean = new Boolean(manyValue);
            isMany = manyBoolean.booleanValue();
        }

        SDOProperty p = null;
        SDOType owningType = null;
        String typeName = null;
        SDOType sdoPropertyType = null;
        String mappingUri = null;
        if (typeDefParticle != null) {
            owningType = getTypeForName(targetNamespace, defaultNamespace, ownerName);
            mappingUri = owningType.getURI();
        }

        if (element.getRef() != null) {
            String ref = element.getRef();
            String localName = null;
            String uri = null;

            int index = ref.indexOf(':');
            if (index != -1) {
                String prefix = ref.substring(0, index);
                localName = ref.substring(index + 1, ref.length());
                uri = getURIForPrefix( prefix);
            } else {
                localName = ref;
                uri = defaultNamespace;
            }

            Property lookedUp = owningType.getProperty(localName);
            if (lookedUp != null) {
                if (inRestriction) {
                    return;
                }
                updateCollisionProperty(owningType, (SDOProperty)lookedUp);
            } else {
                SDOProperty theProp = new SDOProperty(aHelperContext);
                theProp.setName(localName);

                theProp.setGlobal(false);
                theProp.setContainment(true);
                theProp.setXsd(true);
                theProp.setMany(isMany);
                theProp.setInstanceProperty(SDOConstants.XMLELEMENT_PROPERTY, Boolean.TRUE);
                if (element.getAnnotation() != null) {
                    List doc =element.getAnnotation().getDocumentation();
                    if (doc != null) {
                        theProp.setInstanceProperty(SDOConstants.DOCUMENTATION_PROPERTY, doc);
                    }
                }

                owningType.addDeclaredProperty(theProp);

                GlobalRef globalRef = new GlobalRef();
                globalRef.setProperty(theProp);

                globalRef.setIsElement(true);
                globalRef.setOwningType(owningType);
                globalRef.setUri(uri);
                globalRef.setLocalName(localName);
                addGlobalRef(globalRef);
            }
            return;

        } else {
            //TODO: if global prop already exists don't modify it
            if (isGlobal) {                
                SDOProperty lookedUpProp = getExistingGlobalProperty(targetNamespace, element.getName(), true);
                if (lookedUpProp != null && lookedUpProp.isFinalized()) {
                    return;
                }
            }
            p = createNewProperty(targetNamespace, element.getName(), isQualified, isGlobal, true, element.isNillable(), element.getAnnotation());
            if (element.getAnnotation() != null) {
                p.setAppInfoElements(element.getAnnotation().getAppInfo());
            }

            if (element.getType() != null) {
                typeName = element.getType();
                p.setName(element.getName());
                String xsdType = element.getType();
                QName qname = getQNameForString(defaultNamespace, xsdType);
                if (isGlobal) {
                    //if type is found set it other wise process new type                    
                    processGlobalItem(targetNamespace, defaultNamespace, xsdType);

                    //SDOType theType = (SDOType)aHelperContext.getTypeHelper().getType(qname.getNamespaceURI(), qname.getLocalPart());
                    //p.setType(theType);
                }
                if ((qname.equals(XMLConstants.BASE_64_BINARY_QNAME)) || (qname.equals(XMLConstants.HEX_BINARY_QNAME)) || (qname.equals(XMLConstants.DATE_QNAME)) || (qname.equals(XMLConstants.TIME_QNAME)) || (qname.equals(XMLConstants.DATE_TIME_QNAME))) {
                    p.setXsdType(qname);
                }

                String mimeType = (String)element.getAttributesMap().get(SDOConstants.XML_MIME_TYPE_QNAME);
                if (mimeType != null) {
                    p.setInstanceProperty(SDOConstants.MIME_TYPE_PROPERTY, mimeType);
                }
                String mimeTypePropName = (String)element.getAttributesMap().get(SDOConstants.XML_MIME_TYPE_PROPERTY_QNAME);
                if (mimeTypePropName != null) {
                    p.setInstanceProperty(SDOConstants.MIME_TYPE_PROPERTY_PROPERTY, mimeTypePropName);
                }

                sdoPropertyType = getSDOTypeForName(targetNamespace, defaultNamespace, xsdType);
                if ((p.getXsdType() == null) && (sdoPropertyType.getXsdType() != null)) {
                    p.setXsdType(sdoPropertyType.getXsdType());
                }

                //TODO: is this right ??????  - 20070208 - I think so - only dataobjects are in the containment tree          
                if (sdoPropertyType.isDataType()) {
                    p.setContainment(false);
                }
            } else if (element.getComplexType() != null) {
                //if ((typeDefParticle != null) && (maxOccursGreaterThanOne(typeDefParticle.getMaxOccurs()))) {
                //TODO this check is probably wrong                
                sdoPropertyType = getTypeForName(targetNamespace, defaultNamespace, element.getComplexType().getNameOrOwnerName());
                typeName = element.getName();

                //sdoPropertyType = getTypeForName(ownerName);
                //??p.setContainingType(sdoPropertyType);
                p.setName(element.getComplexType().getNameOrOwnerName());

                //TODO: is this right
                // }
            } else if (element.getSimpleType() != null) {
                //String ownerName = typeDefParticle.getOwnerName();
                //sdoPropertyType = getTypeForName(ownerName);		
                typeName = element.getName();
                sdoPropertyType = getTypeForName(targetNamespace, defaultNamespace, element.getName());
                p.setName(element.getName());
                if (sdoPropertyType.isDataType()) {
                    p.setContainment(false);
                }
            } else {
                //TODO: is this right
                //p.setContainment(false);
                //we assume the xsd type to be any simple type
                p.setName(element.getName());
                sdoPropertyType = ((SDOTypeHelper)aHelperContext.getTypeHelper()).getSDOTypeFromXSDType(SDOConstants.ANY_TYPE_QNAME);
            }
        }
        sdoPropertyType = processSimpleComponentAnnotations(owningType, element, p, targetNamespace, defaultNamespace, sdoPropertyType);

        p.setType(sdoPropertyType);
        //TODO:??? anonymous complexType has null name? null or name from containing element
        //could set complexType.setName earlier	 
        setDefaultValue(p, element);

        p.setMany(isMany);

        if (p.getType().equals(SDOConstants.SDO_CHANGESUMMARY)) {
            p.setReadOnly(true);
        }

        if (typeDefParticle != null) {
            updateOwnerAndBuildMapping(owningType, p, defaultNamespace, targetNamespace, element, typeName, mappingUri);
        }
        if (isGlobal) {
            //we have a global element           
            addRootElementToDescriptor(p, targetNamespace, element.getName());
        }
        p.setFinalized(true);
    }

    private SDOProperty processRef(GlobalRef globalRef) {
        boolean isElement = globalRef.isElement();
        SDOProperty p = null;
        
        SDOProperty refProp = getExistingGlobalProperty(globalRef.getUri(), globalRef.getLocalName(), isElement);
        
        if (refProp != null && refProp.isFinalized()) {
            p = (SDOProperty)globalRef.getProperty();
            p.setValueProperty(refProp.isValueProperty());
            p.setNullable(refProp.isNullable());
            p.setName(refProp.getName());
            p.setXsdLocalName(refProp.getXsdLocalName());
            p.setNamespaceQualified(refProp.isNamespaceQualified());
            p.setAliasNames(refProp.getAliasNames());
            p.setDefault(refProp.getDefault());
            p.setSubstitutable(refProp.isSubstitutable());
            p.setSubstitutableElements(refProp.getSubstitutableElements());

            if (p.getType() == null) {
                p.setType(refProp.getType());
                if (refProp.getType().isDataType()) {
                    p.setContainment(false);
                }
            }
            p.setOpposite(refProp.getOpposite());
            p.setReadOnly(refProp.isReadOnly());
            p.setXsd(refProp.isXsd());
            p.setAppInfoElements(refProp.getAppInfoElements());
                        
            int index = ((SDOProperty)globalRef.getProperty()).getIndexInDeclaredProperties();
            p.buildMapping(globalRef.getUri(), index);
            p.setFinalized(true);
        } else {
            if (isImportProcessor) {
                p = new SDOProperty(aHelperContext);
                p.setGlobal(true);
                p.setUri(globalRef.getUri());
                p.setName(globalRef.getLocalName());
                QName qname = new QName(globalRef.getUri(), globalRef.getLocalName());
                if (isElement) {
                    p.setInstanceProperty(SDOConstants.XMLELEMENT_PROPERTY, Boolean.TRUE);
                    getGeneratedGlobalElements().put(qname, p);
                }else{
                  getGeneratedGlobalAttributes().put(qname, p);
                }                                
            } else {
                throw SDOException.referencedPropertyNotFound(globalRef.getUri(), globalRef.getLocalName());
            }
        }
        return p;
    }

    private void updateCollisionProperty(SDOType owningType, SDOProperty p) {
        owningType.setSequenced(true);
        Type baseType = owningType;
        while ((baseType.getBaseTypes() != null) && (baseType.getBaseTypes().size() > 0)) {
            baseType = (Type)baseType.getBaseTypes().get(0);
            ((SDOType)baseType).setSequenced(true);
        }
        p.setNameCollision(true);
        p.setType(SDOConstants.SDO_OBJECT);
        p.setContainment(true);
        p.setMany(true);
        p.setInstanceProperty(SDOConstants.XMLELEMENT_PROPERTY, Boolean.TRUE);
    }

    private SDOProperty createNewProperty(String targetNamespace, String xsdLocalName, boolean isQualified, boolean isGlobal, boolean isElement, boolean isNillable, Annotation annotation) {
        SDOProperty p = null;
        if (isGlobal) {            
            p = getExistingGlobalProperty(targetNamespace, xsdLocalName, isElement);
        }
        if (p == null) {
            p = new SDOProperty(aHelperContext);
        }
        p.setGlobal(isGlobal);
        p.setXsd(true);
        p.setNullable(isNillable);
        if (isElement) {
            p.setInstanceProperty(SDOConstants.XMLELEMENT_PROPERTY, Boolean.TRUE);
        }
        p.setXsdLocalName(xsdLocalName);
        p.setNamespaceQualified(isQualified);
        p.setContainment(true);
        if (isGlobal) {
            QName qname = new QName(targetNamespace, xsdLocalName);                      
            if(isElement){
               getGeneratedGlobalElements().put(qname, p);
            }else{
               getGeneratedGlobalAttributes().put(qname, p);
            }
        }

        if (annotation != null) {
            List documentation = annotation.getDocumentation();
            if ((documentation != null) && (documentation.size() > 0)) {
                p.setInstanceProperty(SDOConstants.DOCUMENTATION_PROPERTY, documentation);
            }
        }
               
        return p;
    }

    private void addRootElementToDescriptor(SDOProperty p, String targetNamespace, String xsdName) {
        if (!p.getType().isDataType()) {
            NamespaceResolver nr = ((SDOType)p.getType()).getXmlDescriptor().getNamespaceResolver();
            String prefix = null;
            if (nr != null) {
                prefix = nr.resolveNamespaceURI(targetNamespace);
            }
            if ((prefix == null) || prefix.equals(SDOConstants.EMPTY_STRING)) {
                ((SDOType)p.getType()).getXmlDescriptor().addRootElement(xsdName);
            } else {
                ((SDOType)p.getType()).getXmlDescriptor().addRootElement(prefix + //
                		SDOConstants.SDO_XPATH_NS_SEPARATOR_FRAGMENT + xsdName);
            }
        }
    }

    private void updateOwnerAndBuildMapping(SDOType owningType, SDOProperty p, String defaultNamespace, String targetNamespace, SimpleComponent simpleComponent, String typeName, String mappingUri) {
        boolean buildMapping = true;
        Property lookedUp = owningType.getProperty(p.getName());

        if (lookedUp != null) {
            p = (SDOProperty)lookedUp;
            if (inRestriction) {
                return;
            }
            updateCollisionProperty(owningType, p);
        } else {
            owningType.addDeclaredProperty(p);
        }

        QName xsdType = getQNameForString( defaultNamespace, typeName);

        if ((xsdType != null) && xsdType.getNamespaceURI().equals(XMLConstants.SCHEMA_URL)) {
            if (xsdType.getLocalPart().equals(SDOConstants.ID)) {
                owningType.setInstanceProperty(SDOConstants.ID_PROPERTY, p.getName());
            } else if (xsdType.getLocalPart().equals(SDOConstants.IDREF)) {
                p.setContainment(false);
                String propertyTypeValue = (String)simpleComponent.getAttributesMap().get(SDOConstants.SDOXML_PROPERTYTYPE_QNAME);
                if (propertyTypeValue != null) {
                    buildMapping = false;
                }
            } else if (xsdType.getLocalPart().equals(SDOConstants.IDREFS)) {
                p.setContainment(false);
                p.setMany(true);
                String propertyTypeValue = (String)simpleComponent.getAttributesMap().get(SDOConstants.SDOXML_PROPERTYTYPE_QNAME);
                if (propertyTypeValue != null) {
                    buildMapping = false;
                }
            }
        }

        if (buildMapping) {
            p.buildMapping(mappingUri);
        }
    }

    private void setDefaultValue(SDOProperty p, SimpleComponent sc) {
        if (sc.getFixed() != null) {
            Object convertedValue = convertDefaultValue(p.getType(), sc.getFixed());
            p.setDefault(convertedValue);
        } else if (sc.getDefaultValue() != null) {
            Object convertedValue = convertDefaultValue(p.getType(), sc.getDefaultValue());
            p.setDefault(convertedValue);
        } 
    }

    private Object convertDefaultValue(Type type, String stringValue) {
        if (type != null) {
            Class javaClass = ((SDOTypeHelper)aHelperContext.getTypeHelper()).getJavaWrapperTypeForSDOType(type);
            if (javaClass != null) {
                Object objectValue = ((SDODataHelper)aHelperContext.getDataHelper()).convertFromStringValue(stringValue, javaClass);
                return objectValue;
            }
        }
        return stringValue;
    }

    public void processSimpleAttribute(String targetNamespace, String defaultNamespace, String ownerName, Attribute attribute, boolean isGlobal, boolean isQualified) {
        if (attribute == null) {
            return;
        }

        SDOProperty p = null;
        SDOType owningType = null;
        String typeName = null;
        SDOType sdoPropertyType = null;
        String mappingUri = null;
        if (ownerName != null) {
            owningType = getTypeForName(targetNamespace, defaultNamespace, ownerName);
            mappingUri = owningType.getURI();
        }
        if (attribute.getRef() != null) {
            String ref = attribute.getRef();

            String localName = null;
            String uri = null;

            int index = ref.indexOf(':');
            if (index != -1) {
                String prefix = ref.substring(0, index);
                localName = ref.substring(index + 1, ref.length());
                uri = getURIForPrefix(prefix);
            } else {
                localName = ref;
                uri = defaultNamespace;
            }

            Property lookedUp = owningType.getProperty(localName);
            if (lookedUp != null) {
                if (inRestriction) {
                    return;
                }
                updateCollisionProperty(owningType, (SDOProperty)lookedUp);
            } else {
                SDOProperty theProp = new SDOProperty(aHelperContext);
                theProp.setName(localName);
                theProp.setGlobal(false);
                theProp.setContainment(false);
                theProp.setXsd(true);
                theProp.setMany(false);
                if (attribute.getAnnotation() != null) {
                    List doc = attribute.getAnnotation().getDocumentation();
                    if (doc != null) {
                        theProp.setInstanceProperty(SDOConstants.DOCUMENTATION_PROPERTY, doc);
                    }
                }
                theProp.setFinalized(true);
                GlobalRef globalRef = new GlobalRef();
                globalRef.setProperty(theProp);
                owningType.addDeclaredProperty(theProp);

                globalRef.setIsElement(false);
                globalRef.setOwningType(owningType);
                globalRef.setUri(uri);
                globalRef.setLocalName(localName);
                addGlobalRef(globalRef);
            }
            return;

        } else {
            if (isGlobal) {                
                SDOProperty lookedUpProp = getExistingGlobalProperty(targetNamespace, attribute.getName(), false);
                if (lookedUpProp != null && lookedUpProp.isFinalized()) {
                    return;
                }
            }

            p = createNewProperty(targetNamespace, attribute.getName(), isQualified, isGlobal, false, false, attribute.getAnnotation());
            if (attribute.getAnnotation() != null) {
                p.setAppInfoElements(attribute.getAnnotation().getAppInfo());
            }

            typeName = attribute.getType();

            //TODO: is containment always false for attributes
            //p.setContainment(true);
            if (typeName != null) {
                p.setName(attribute.getName());
                //String xsdType = typeName;
                QName qname = getQNameForString(defaultNamespace, typeName);
                if (isGlobal) {
                    //if type is found set it other wise process new type                    
                    processGlobalItem(targetNamespace, defaultNamespace, typeName);

                    // SDOType theType = (SDOType)aHelperContext.getTypeHelper().getType(qname.getNamespaceURI(), qname.getLocalPart());
                    // p.setType(theType);
                }

                if ((qname.equals(XMLConstants.BASE_64_BINARY_QNAME)) || (qname.equals(XMLConstants.HEX_BINARY_QNAME)) || (qname.equals(XMLConstants.DATE_QNAME)) || (qname.equals(XMLConstants.TIME_QNAME)) || (qname.equals(XMLConstants.DATE_TIME_QNAME))) {
                    p.setXsdType(qname);
                }

                sdoPropertyType = getSDOTypeForName(targetNamespace, defaultNamespace, typeName);
                if ((p.getXsdType() == null) && (sdoPropertyType.getXsdType() != null)) {
                    p.setXsdType(sdoPropertyType.getXsdType());
                }
            } else if (attribute.getSimpleType() != null) {
                p.setName(attribute.getName());
                sdoPropertyType = getSDOTypeForName(targetNamespace, defaultNamespace, attribute.getName());
                typeName = attribute.getName();
            } else {
                p.setName(attribute.getName());
                sdoPropertyType = ((SDOTypeHelper)aHelperContext.getTypeHelper()).getSDOTypeFromXSDType(SDOConstants.ANY_TYPE_QNAME);                
            }
        }
        sdoPropertyType = processSimpleComponentAnnotations(owningType, attribute, p, targetNamespace, defaultNamespace, sdoPropertyType);
        p.setType(sdoPropertyType);
        p.setContainment(false);
        setDefaultValue(p, attribute);

        if (p.getType().equals(SDOConstants.SDO_CHANGESUMMARY)) {
            p.setReadOnly(true);
        }

        if (owningType != null) {
            updateOwnerAndBuildMapping(owningType, p, defaultNamespace, targetNamespace, attribute, typeName, mappingUri);
        }
        p.setFinalized(true);
    }

    private SDOType processSimpleComponentAnnotations(SDOType owningType, SimpleComponent simpleComponent, SDOProperty p, String targetNamespace, String defaultNamespace, SDOType sdoPropertyType) {
        //aliasName annotation
        String aliasNamesValue = (String)simpleComponent.getAttributesMap().get(SDOConstants.SDOXML_ALIASNAME_QNAME);
        if (aliasNamesValue != null) {
            List names = (List)XMLConversionManager.getDefaultXMLManager().convertObject(aliasNamesValue, List.class);
            p.setAliasNames(names);
        }

        //readOnly annotation
        String readOnlyValue = (String)simpleComponent.getAttributesMap().get(SDOConstants.SDOXML_READONLY_QNAME);
        if (readOnlyValue != null) {
            Boolean readOnlyBoolean = new Boolean(readOnlyValue);
            p.setReadOnly(readOnlyBoolean.booleanValue());
        }

        //dataType annotation
        String dataTypeValue = (String)simpleComponent.getAttributesMap().get(SDOConstants.SDOXML_DATATYPE_QNAME);
        if (dataTypeValue != null) {
            //QName dataTypeQname = (QName)XMLConversionManager.getDefaultXMLManager().convertObject(dataTypeValue, QName.class);                      
            QName xsdQName = ((SDOTypeHelper)aHelperContext.getTypeHelper()).getXSDTypeFromSDOType(sdoPropertyType);
            if ((xsdQName == null) && !sdoPropertyType.isDataType()) {
                xsdQName = new QName(sdoPropertyType.getURI(), sdoPropertyType.getName());
            }
            p.setXsdType(xsdQName);
            SDOType sdoType = getSDOTypeForName(targetNamespace, defaultNamespace, dataTypeValue);
            sdoPropertyType = sdoType;
            p.setInstanceProperty(SDOConstants.XMLDATATYPE_PROPERTY, sdoType);
        }

        //string annotation
        String stringValue = (String)simpleComponent.getAttributesMap().get(SDOConstants.SDOXML_STRING_QNAME);
        if (stringValue != null) {            
            QName xsdTypeQName = getQNameForString(defaultNamespace, simpleComponent.getType());
            p.setXsdType(xsdTypeQName);
            sdoPropertyType = SDOConstants.SDO_STRING;
        }

        //name annotation
        String nameValue = (String)simpleComponent.getAttributesMap().get(SDOConstants.SDOXML_NAME_QNAME);
        if (nameValue != null) {
            //itemNameToSDOName.put(sdoTypeName, nameValue);
            p.setName(nameValue);
            if(p.isGlobal() && targetNamespace != null){
              QName propertyName = new QName(targetNamespace, nameValue);
              ((SDOTypeHelper)aHelperContext.getTypeHelper()).getOpenContentProperties().put(propertyName, p);
            }
        }else{
          if(p.isGlobal() && targetNamespace != null){
              QName propertyName = new QName(targetNamespace, p.getName());              
              ((SDOTypeHelper)aHelperContext.getTypeHelper()).getOpenContentProperties().put(propertyName, p);
            }
        }

        //propertyType annotation
        //TODO: only process if !datatype if(!sdoPropertyType.isDataType()){
        String propertyTypeValue = (String)simpleComponent.getAttributesMap().get(SDOConstants.SDOXML_PROPERTYTYPE_QNAME);
        if (propertyTypeValue != null) {
            String uri = targetNamespace;
            int colonIndex = propertyTypeValue.indexOf(':');
            if (colonIndex > -1) {
                String prefix = propertyTypeValue.substring(0, colonIndex);
                uri = getURIForPrefix(prefix);
            }
            NonContainmentReference nonContainmentReference = new NonContainmentReference();
            nonContainmentReference.setPropertyTypeName(propertyTypeValue);
            nonContainmentReference.setPropertyTypeURI(uri);
            nonContainmentReference.setOwningType(owningType);
            nonContainmentReference.setOwningProp(p);
            String oppositePropValue = (String)simpleComponent.getAttributesMap().get(SDOConstants.SDOXML_OPPOSITEPROPERTY_QNAME);

            nonContainmentReference.setOppositePropName(oppositePropValue);
            getNonContainmentReferences().add(nonContainmentReference);
        }

        return sdoPropertyType;
    }

    protected void postProcessing() {
        int size = getNonContainmentReferences().size();
        for (int i = 0; i < size; i++) {
            NonContainmentReference nonContainmentReference = (NonContainmentReference)getNonContainmentReferences().get(i);
            SDOType owningType = nonContainmentReference.getOwningType();

            if (owningType != null) {
                String propertyTypeName = nonContainmentReference.getPropertyTypeName();
                String propertyTypeUri = nonContainmentReference.getPropertyTypeURI();

                SDOType oppositeType = getSDOTypeForName(propertyTypeUri, propertyTypeUri, propertyTypeName);
                if (oppositeType != null) {
                    SDOProperty owningProp = nonContainmentReference.getOwningProp();
                    if (owningProp != null) {
                        // Spec sect 9.2 (1) oppositeType.dataType must be false
                        if (oppositeType.isDataType()) {
                            throw SDOException.propertyTypeAnnotationTargetCannotBeDataTypeTrue(//
                            oppositeType.getName(), owningProp.getName());
                        }
                        owningProp.setType(oppositeType);
                        owningProp.setContainment(false);
                        owningProp.buildMapping(owningProp.getType().getURI());
                        // Bidirectional property name
                        String oppositePropName = nonContainmentReference.getOppositePropName();
                        if (oppositePropName != null) {
                            SDOProperty prop = (SDOProperty)oppositeType.getProperty(oppositePropName);
                            owningProp.setOpposite(prop);
                            prop.setOpposite(owningProp);
                        }
                    }
                }
            }
        }

        Iterator<Type> iter = getGlobalRefs().keySet().iterator();
        while (iter.hasNext()) {
            Object nextKey = iter.next();
            List<GlobalRef> value = getGlobalRefs().get(nextKey);
            List refsToRemove = new ArrayList();
            if (value != null) {
                for (int i = 0; i < value.size(); i++) {
                    GlobalRef nextGlobalRef = value.get(i);
                    SDOProperty p = processRef(nextGlobalRef);
                    if(p.isFinalized()){                     
                     refsToRemove.add(nextGlobalRef);
                    }                    
                }               
            }
            for(int i=0;i<refsToRemove.size(); i++){          
              value.remove(refsToRemove.get(i));
            }
        }        
    }

    private void addGlobalRef(GlobalRef ref) {
        List<GlobalRef> refs = getGlobalRefs().get(ref.getOwningType());
        if (null == refs) {
            refs = new ArrayList<GlobalRef>();
            refs.add(ref);
            getGlobalRefs().put(ref.getOwningType(), refs);
        } else {
            refs.add(ref);
        }
    }

    /**
     * INTERNAL:
     * Initialize this SchemaParser by configuring the package name based on the targetNamespace.
     */
    public void initialize() {
        if (null == packageName) {
            String packageValue = (String)rootSchema.getAttributesMap().get(SDOConstants.SDOJAVA_PACKAGE_QNAME);
            if (null != packageValue) {
                packageName = packageValue;
            } else if ((null == rootSchema.getTargetNamespace()) || rootSchema.getTargetNamespace().equals(SDOConstants.EMPTY_STRING)) {
                packageName = SDOConstants.JAVA_TYPEGENERATION_NO_NAMESPACE;
            } else {
            	packageName = SDOUtil.getPackageNameFromURI(rootSchema.getTargetNamespace());            	
            }
            packageName += SDOConstants.JAVA_PACKAGE_NAME_SEPARATOR;
        }
    }

    private SDOType createSDOTypeForName(String targetNamespace, String name, String xsdLocalName) {
        SDOType returnType = null;
        int index = name.indexOf(':');
        if (index != -1) {
            String prefix = name.substring(0, index);
            String localName = name.substring(index + 1, name.length());
            String theURI = getURIForPrefix(prefix);
            returnType = getOrCreateType(theURI, localName, xsdLocalName);
        } else {            
            returnType = getOrCreateType(targetNamespace, name, xsdLocalName);
        }
        if (returnType != null) {
            QName qname = new QName(returnType.getURI(), name);
            getGeneratedTypes().put(qname, returnType);
        }        
        return returnType;
    }

    private SDOType getSDOTypeForName(String targetNamespace, String defaultNamespace, String name) {
        return getSDOTypeForName(targetNamespace, defaultNamespace, true, name);
    }

    private SDOType getSDOTypeForName(String targetNamespace, String defaultNamespace, boolean checkDefaultNamespace, String name) {
        int index = name.indexOf(':');
        if (index != -1) {
            String prefix = name.substring(0, index);
            String localName = name.substring(index + 1, name.length());
            String theURI = getURIForPrefix(prefix);
            QName qname = new QName(theURI, localName);
            SDOType sdoType = ((SDOTypeHelper)aHelperContext.getTypeHelper()).getSDOTypeFromXSDType(qname);
            if (null == sdoType) {                
                sdoType  = getExisitingType(theURI, localName);
                if (null == sdoType) {
                    sdoType = findSdoType(targetNamespace, defaultNamespace, name, localName, theURI);
                }
            }
            if (null == sdoType) {                
                sdoType = getOrCreateType(theURI, localName, localName);
                if (!sdoType.isFinalized()) {
                    //if it's not finalized, then it's new, so add it to the generated types map
                    getGeneratedTypes().put(new QName(sdoType.getURI(), sdoType.getName()), sdoType);
                }
            }
            return sdoType;

        } else {
            String sdoName = (String)itemNameToSDOName.get(name);
            if (sdoName != null) {
                name = sdoName;
            }

            SDOType sdoType = null;
            if (checkDefaultNamespace && (defaultNamespace != null)) {
                QName qname = new QName(defaultNamespace, name);
                sdoType = ((SDOTypeHelper)aHelperContext.getTypeHelper()).getSDOTypeFromXSDType(qname);
            }
            if (null == sdoType) {
                sdoType  = getExisitingType(targetNamespace, name);
                if (null == sdoType) {
                    return findSdoType(targetNamespace, defaultNamespace, name, name, targetNamespace);
                }
                return getOrCreateType(targetNamespace, name, name);
            }
            return sdoType;
        }
    }
    
    //Since types aren't registered until the end of the define call we need to check typeHelper
    //and the generatedTypesmap to see if a type already exists.  The if the type doesn't exist
    //we create a new one 
    private SDOType getOrCreateType(String uri, String typeName, String xsdLocalName){
        String lookupName = typeName;
        int index = lookupName.indexOf(':');
        if (index != -1) {
            lookupName = lookupName.substring(index + 1, lookupName.length());
        }
        SDOType returnType = (SDOType)aHelperContext.getTypeHelper().getType(uri, lookupName);
        if (returnType == null) {
            QName qname = new QName(uri, lookupName);
            returnType = (SDOType)getGeneratedTypes().get(qname);
            
            if(returnType == null){            
                returnType = new SDOType(uri, lookupName, aHelperContext);
                returnType.setXsd(true);            
                returnType.setXsdLocalName(xsdLocalName);
            }           
        }
        return returnType;             
    }
    
    //Since global properties aren't registered until the end of the define call we need to check XSDhelper
    //and the generatedProperties map to see if a type already exists
    private SDOProperty getExistingGlobalProperty(String uri, String localName, boolean isElement){
      SDOProperty prop = (SDOProperty)aHelperContext.getXSDHelper().getGlobalProperty(uri, localName, isElement);      
      if(prop == null) {                
        QName qName = new QName(uri, localName);
        if(isElement){
          prop = (SDOProperty)getGeneratedGlobalElements().get(qName);
        }else{
          prop = (SDOProperty)getGeneratedGlobalAttributes().get(qName);
        }        
      }
      return prop;
    }
    
    //Since types aren't registered until the end of the define call we need to check type helper
    //and the generatedTypesmap to see if a type already exists
    private SDOType getExisitingType(String uri, String localName){
      SDOType type = (SDOType)((SDOTypeHelper)aHelperContext.getTypeHelper()).getType(uri, localName);
      if(type == null) {                
        QName qName = new QName(uri, localName);
        type = (SDOType)getGeneratedTypes().get(qName);
      }
      return type;
    }

    private SDOType findSdoType(String targetNamespace, String defaultNamespace, String qualifiedName, String localName, String theURI) {
        //need to also check imports        
        SDOType type  = getExisitingType(theURI, localName);
        if (null == type) {
            processGlobalItem(targetNamespace, defaultNamespace, qualifiedName);
            String sdoName = (String)itemNameToSDOName.get(localName);
            if (sdoName != null) {
                localName = sdoName;
            }
            type  = getExisitingType(theURI, localName);
        }
        if (null == type) {            
            type = getOrCreateType(theURI, localName, localName);

            if (!type.isFinalized()) {
                //if it's not finalized, then it's new, so add it to the generated types map
                getGeneratedTypes().put(new QName(type.getURI(), type.getName()), type);
            }

        }
        return type;
    }

    public void setGeneratedTypes(Map<QName, Type> generatedTypes) {
        this.generatedTypes = generatedTypes;
    }

    public Map<QName, Type> getGeneratedTypes() {
        if (null == generatedTypes) {
            generatedTypes = new HashMap<QName, Type>();
        }
        return generatedTypes;
    }
   
    public Map<QName, Property> getGeneratedGlobalElements() {
        if (null == generatedGlobalElements) {
            generatedGlobalElements = new HashMap<QName, Property>();
        }
        return generatedGlobalElements;
    }
    public Map<QName, Property> getGeneratedGlobalAttributes() {
        if (null == generatedGlobalAttributes) {
            generatedGlobalAttributes = new HashMap<QName, Property>();
        }
        return generatedGlobalAttributes;
    }

    protected void processSubstitutionGroups(Collection elements, String targetNamespace, String defaultNamespace){
        Iterator elementsIter = elements.iterator();
        while (elementsIter.hasNext()) {
            Element nextElement = (Element)elementsIter.next();
            if(nextElement.getSubstitutionGroup() != null) {
                String substitutionGroup = nextElement.getSubstitutionGroup();
                String localName = null;
                String uri = null;

                int index = substitutionGroup.indexOf(':');
                if (index != -1) {
                    String prefix = substitutionGroup.substring(0, index);
                    localName = substitutionGroup.substring(index + 1, substitutionGroup.length());
                    uri = getURIForPrefix(defaultNamespace, prefix);
                } else {
                    localName = substitutionGroup;
                    uri = defaultNamespace;
                }
                SDOProperty rootProp = getExistingGlobalProperty(uri, localName, true);
                SDOProperty thisProperty = getExistingGlobalProperty(targetNamespace, nextElement.getName(), true);
           
                if(rootProp != null && thisProperty != null) {
                    if(rootProp.getSubstitutableElements() == null) {
                        rootProp.setSubstitutableElements(new java.util.ArrayList<SDOProperty>());
                        rootProp.setSubstitutable(true);
                    }
                    rootProp.getSubstitutableElements().add(thisProperty);
                }
            }
        }
    }

    protected void processAnyAttribute(String targetNamespace, String defaultNamespace, String ownerName) {
        SDOType owningType = getTypeForName(targetNamespace, defaultNamespace, ownerName);
        owningType.setOpen(true);
    }

    public SDOType getTypeForName(String targetNamespace, String defaultNamespace, String typeName) {
        Object value = getGeneratedTypes().get(typeName);
        if (value != null) {
            return (SDOType)value;
        } else {
            String sdoName = (String)itemNameToSDOName.get(typeName);
            if (sdoName != null) {
                return getTypeForName(targetNamespace, defaultNamespace, sdoName);
            } else {
                return getSDOTypeForName(targetNamespace, defaultNamespace, false, typeName);
            }
        }
    }

    private QName getQNameForString(String defaultNamespace, String name) {
        if (null == name) {
            return null;
        }
        int index = name.indexOf(':');
        if (index != -1) {
            String prefix = name.substring(0, index);
            String localName = name.substring(index + 1, name.length());
            String theURI = getURIForPrefix(prefix);
            QName qname = new QName(theURI, localName);
            return qname;
        } else {
            QName qname = new QName(defaultNamespace, name);
            return qname;
        }
    }

    protected String getURIForPrefix(String prefix) {
        String uri = null;

        for (int i = namespaceResolvers.size() - 1; i >= 0; i--) {
            NamespaceResolver next = (NamespaceResolver)namespaceResolvers.get(i);
            uri = next.resolveNamespacePrefix(prefix);
            if ((uri != null) && !uri.equals(SDOConstants.EMPTY_STRING)) {
                break;
            }
        }                
                
        if (null == uri) {
            throw SDOException.prefixUsedButNotDefined(prefix);            
        }
        return uri;
    }

    public class NonContainmentReference {
        private SDOType owningType;
        private SDOProperty owningProp;
        private String propertyTypeName;
        private String propertyTypeURI;
        private String oppositePropName;

        public void setOwningType(SDOType owningType) {
            this.owningType = owningType;
        }

        public SDOType getOwningType() {
            return owningType;
        }

        public void setOwningProp(SDOProperty owningProp) {
            this.owningProp = owningProp;
        }

        public SDOProperty getOwningProp() {
            return owningProp;
        }

        public void setPropertyTypeName(String propertyTypeName) {
            this.propertyTypeName = propertyTypeName;
        }

        public String getPropertyTypeName() {
            return propertyTypeName;
        }

        public void setPropertyTypeURI(String propertyTypeURI) {
            this.propertyTypeURI = propertyTypeURI;
        }

        public String getPropertyTypeURI() {
            return propertyTypeURI;
        }

        public void setOppositePropName(String oppositePropName) {
            this.oppositePropName = oppositePropName;
        }

        public String getOppositePropName() {
            return oppositePropName;
        }
    }

    public class GlobalRef {
        private SDOType owningType;
        private boolean isElement;
        private String uri;
        private String localName;
        private Property property;

        public void setOwningType(SDOType owningType) {
            this.owningType = owningType;
        }

        public SDOType getOwningType() {
            return owningType;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String theUri) {
            uri = theUri;
        }

        public Property getProperty() {
            return property;
        }

        public void setProperty(Property theProperty) {
            property = theProperty;
        }

        public String getLocalName() {
            return localName;
        }

        public void setLocalName(String theLocalName) {
            localName = theLocalName;
        }

        public boolean isElement() {
            return isElement;
        }

        public void setIsElement(boolean isElem) {
            isElement = isElem;
        }
    }

    private List<NonContainmentReference> getNonContainmentReferences() {
        if (null == nonContainmentReferences) {
            nonContainmentReferences = new ArrayList<NonContainmentReference>();
        }
        return nonContainmentReferences;
    }

    private Map<Type, List<GlobalRef>> getGlobalRefs() {
        if (null == globalRefs) {
            globalRefs = new HashMap<Type, List<GlobalRef>>();
        }
        return globalRefs;
    }
    
    public boolean isImportProcessor() {
        return isImportProcessor;
    }
    
    public void setIsImportProcessor(boolean isImport) {
        isImportProcessor = isImport;
    }  
  
}
