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
package org.eclipse.persistence.internal.sessions.factories;

// javase imports
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

// EclipseLink imports
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.helper.NonSynchronizedVector;
import org.eclipse.persistence.mappings.converters.Converter;
import org.eclipse.persistence.mappings.converters.EnumTypeConverter;
import org.eclipse.persistence.mappings.transformers.ConstantTransformer;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.mappings.UnmarshalKeepAsElementPolicy;
import org.eclipse.persistence.oxm.mappings.XMLCompositeCollectionMapping;
import org.eclipse.persistence.oxm.mappings.XMLCompositeObjectMapping;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.oxm.mappings.nullpolicy.NullPolicy;
import org.eclipse.persistence.oxm.schema.XMLSchemaClassPathReference;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.sessions.DatabaseLogin;
import static org.eclipse.persistence.sessions.factories.XMLProjectReader.ECLIPSELINK_SCHEMA;
import static org.eclipse.persistence.sessions.factories.XMLProjectReader.SCHEMA_DIR;

/**
 * INTERNAL: Define the EclipseLInk OX project and descriptor information to read an EclipseLink
 * project from an XML file. Note any changes must be reflected in the EclipseLink XML schema.
 */
public class EclipseLinkObjectPersistenceRuntimeXMLProject extends ObjectPersistenceRuntimeXMLProject_11_1_1 {

    /**
     * INTERNAL: Return a new descriptor project.
     */
    public EclipseLinkObjectPersistenceRuntimeXMLProject() {
        super();
    }

    @Override
    protected void buildNamespaceResolver() {
        ns = new NamespaceResolverWithPrefixes();
        ns.put("xsi", W3C_XML_SCHEMA_INSTANCE_NS_URI);
        ns.put("xsd", W3C_XML_SCHEMA_NS_URI); 
        ns.put(ECLIPSELINK_PREFIX, getPrimaryNamespace());
        ns.setDefaultNamespaceURI(getPrimaryNamespace());
    }

    @Override
    public void buildDescriptors() {
        super.buildDescriptors();
        
        // Any new mappings go after call to super.buildDescriptors();
    }

    @Override
    public String getPrimaryNamespacePrefix() {
        return null;
    }

    @Override
    public String getPrimaryNamespace() {
        return ECLIPSELINK_NAMESPACE;
    }

    @Override
    public String getSecondaryNamespacePrefix() {
        return null;
    }

    @Override
    public String getSecondaryNamespace() {
        return ECLIPSELINK_NAMESPACE;
    }

    @Override
    protected ClassDescriptor buildProjectDescriptor() {
        XMLDescriptor descriptor = (XMLDescriptor)super.buildProjectDescriptor();        
        descriptor.setSchemaReference(new XMLSchemaClassPathReference(SCHEMA_DIR + ECLIPSELINK_SCHEMA));
        
        XMLCompositeCollectionMapping projectQueriesMapping = new XMLCompositeCollectionMapping();
        projectQueriesMapping.useCollectionClass(NonSynchronizedVector.class);
        projectQueriesMapping.setAttributeName("queries");
        projectQueriesMapping.setSetMethodName("setQueries");
        projectQueriesMapping.setGetMethodName("getQueries");
        projectQueriesMapping.setReferenceClass(DatabaseQuery.class);
        projectQueriesMapping.setXPath(getSecondaryNamespaceXPath() + "queries/" + getSecondaryNamespaceXPath() + "query");
        descriptor.addMapping(projectQueriesMapping);
        
        return descriptor;
    }

    @Override
    protected ConstantTransformer getConstantTransformerForProjectVersionMapping() {
        return new ConstantTransformer(DatabaseLogin.getVersion());
    }
    
    protected ClassDescriptor buildXMLChoiceFieldToClassAssociationDescriptor() {
        ClassDescriptor descriptor = super.buildXMLChoiceFieldToClassAssociationDescriptor();
        
        XMLCompositeObjectMapping converterMapping = new XMLCompositeObjectMapping();
        converterMapping.setAttributeName("converter");
        converterMapping.setXPath(getPrimaryNamespacePrefix() + "value-converter");
        converterMapping.setReferenceClass(Converter.class);
        
        descriptor.addMapping(converterMapping);
        
        return descriptor;
    }
    
    @Override
    protected ClassDescriptor buildXMLCompositeObjectMappingDescriptor() {
        ClassDescriptor descriptor = super.buildXMLCompositeObjectMappingDescriptor();
        //Add container accessor mapping
        
        XMLDirectMapping containerAttributeMapping = new XMLDirectMapping();
        containerAttributeMapping.setAttributeName("containerAttributeName");
        containerAttributeMapping.setGetMethodName("getContainerAttributeName");
        containerAttributeMapping.setSetMethodName("setContainerAttributeName");
        containerAttributeMapping.setXPath("container-attribute/text()");
        ((NullPolicy)containerAttributeMapping.getNullPolicy()).setSetPerformedForAbsentNode(false);
        descriptor.addMapping(containerAttributeMapping);
        
        XMLDirectMapping containerGetMethodMapping = new XMLDirectMapping();
        containerGetMethodMapping.setAttributeName("containerGetMethodName");
        containerGetMethodMapping.setGetMethodName("getContainerGetMethodName");
        containerGetMethodMapping.setSetMethodName("setContainerGetMethodName");
        containerGetMethodMapping.setXPath("container-get-method/text()");
        ((NullPolicy)containerGetMethodMapping.getNullPolicy()).setSetPerformedForAbsentNode(false);
        descriptor.addMapping(containerGetMethodMapping);
        
        XMLDirectMapping containerSetMethodMapping = new XMLDirectMapping();
        containerSetMethodMapping.setAttributeName("containerSetMethodName");
        containerSetMethodMapping.setGetMethodName("getContainerSetMethodName");
        containerSetMethodMapping.setSetMethodName("setContainerSetMethodName");
        containerSetMethodMapping.setXPath("container-set-method/text()");
        ((NullPolicy)containerSetMethodMapping.getNullPolicy()).setSetPerformedForAbsentNode(false);
        descriptor.addMapping(containerSetMethodMapping);
        
        XMLDirectMapping keepAsElementMapping = new XMLDirectMapping();
        keepAsElementMapping.setAttributeName("keepAsElementPolicy");
        keepAsElementMapping.setGetMethodName("getKeepAsElementPolicy");
        keepAsElementMapping.setSetMethodName("setKeepAsElementPolicy");
        keepAsElementMapping.setXPath(getPrimaryNamespaceXPath() + "keep-as-element-policy");
        EnumTypeConverter converter = new EnumTypeConverter(keepAsElementMapping, UnmarshalKeepAsElementPolicy.class, false);
        keepAsElementMapping.setConverter(converter);
        descriptor.addMapping(keepAsElementMapping);
        
        return descriptor;
    }
        
    
    @Override
    protected ClassDescriptor buildXMLCompositeCollectionMappingDescriptor() {
        ClassDescriptor descriptor = super.buildXMLCompositeCollectionMappingDescriptor();
        //Add container accessor mapping

        XMLDirectMapping containerAttributeMapping = new XMLDirectMapping();
        containerAttributeMapping.setAttributeName("containerAttributeName");
        containerAttributeMapping.setGetMethodName("getContainerAttributeName");
        containerAttributeMapping.setSetMethodName("setContainerAttributeName");
        containerAttributeMapping.setXPath("container-attribute/text()");
        ((NullPolicy)containerAttributeMapping.getNullPolicy()).setSetPerformedForAbsentNode(false);
        descriptor.addMapping(containerAttributeMapping);
        
        XMLDirectMapping containerGetMethodMapping = new XMLDirectMapping();
        containerGetMethodMapping.setAttributeName("containerGetMethodName");
        containerGetMethodMapping.setGetMethodName("getContainerGetMethodName");
        containerGetMethodMapping.setSetMethodName("setContainerGetMethodName");
        containerGetMethodMapping.setXPath("container-get-method/text()");
        ((NullPolicy)containerGetMethodMapping.getNullPolicy()).setSetPerformedForAbsentNode(false);
        descriptor.addMapping(containerGetMethodMapping);
        
        XMLDirectMapping containerSetMethodMapping = new XMLDirectMapping();
        containerSetMethodMapping.setAttributeName("containerSetMethodName");
        containerSetMethodMapping.setGetMethodName("getContainerSetMethodName");
        containerSetMethodMapping.setSetMethodName("setContainerSetMethodName");
        containerSetMethodMapping.setXPath("container-set-method/text()");
        ((NullPolicy)containerSetMethodMapping.getNullPolicy()).setSetPerformedForAbsentNode(false);
        descriptor.addMapping(containerSetMethodMapping);
        
        XMLDirectMapping keepAsElementMapping = new XMLDirectMapping();
        keepAsElementMapping.setAttributeName("keepAsElementPolicy");
        keepAsElementMapping.setGetMethodName("getKeepAsElementPolicy");
        keepAsElementMapping.setSetMethodName("setKeepAsElementPolicy");
        keepAsElementMapping.setXPath(getPrimaryNamespaceXPath() + "keep-as-element-policy");
        EnumTypeConverter converter = new EnumTypeConverter(keepAsElementMapping, UnmarshalKeepAsElementPolicy.class, false);
        keepAsElementMapping.setConverter(converter);
        descriptor.addMapping(keepAsElementMapping);
        
        return descriptor;
    }   
    
    protected ClassDescriptor buildXMLAnyAttributeMappingDescriptor() {
        ClassDescriptor descriptor = super.buildXMLAnyAttributeMappingDescriptor();
        
        XMLDirectMapping includeNamespaceDeclarationMapping = new XMLDirectMapping();
        includeNamespaceDeclarationMapping.setAttributeName("isNamespaceDeclarationIncluded");        
        includeNamespaceDeclarationMapping.setGetMethodName("isNamespaceDeclarationIncluded");
        includeNamespaceDeclarationMapping.setSetMethodName("setNamespaceDeclarationIncluded");
        includeNamespaceDeclarationMapping.setXPath(getPrimaryNamespaceXPath() + "include-namespace-declaration/text()");
        descriptor.addMapping(includeNamespaceDeclarationMapping);
     
        XMLDirectMapping includeSchemaInstanceMapping = new XMLDirectMapping();
        includeSchemaInstanceMapping.setAttributeName("isSchemaInstanceIncluded");        
        includeSchemaInstanceMapping.setGetMethodName("isSchemaInstanceIncluded");
        includeSchemaInstanceMapping.setSetMethodName("setSchemaInstanceIncluded");
        includeSchemaInstanceMapping.setXPath(getPrimaryNamespaceXPath() + "include-schema-instance/text()");
        descriptor.addMapping(includeSchemaInstanceMapping);
        
        return descriptor;
    }
    
    protected ClassDescriptor buildXMLAnyObjectMappingDescriptor() {
        ClassDescriptor descriptor = super.buildXMLAnyObjectMappingDescriptor();
        
        XMLDirectMapping keepAsElementMapping = new XMLDirectMapping();
        keepAsElementMapping.setAttributeName("keepAsElementPolicy");
        keepAsElementMapping.setGetMethodName("getKeepAsElementPolicy");
        keepAsElementMapping.setSetMethodName("setKeepAsElementPolicy");
        keepAsElementMapping.setXPath(getPrimaryNamespaceXPath() + "keep-as-element-policy");
        EnumTypeConverter converter = new EnumTypeConverter(keepAsElementMapping, UnmarshalKeepAsElementPolicy.class, false);
        keepAsElementMapping.setConverter(converter);
        descriptor.addMapping(keepAsElementMapping);
        
        return descriptor;
    }
    
}