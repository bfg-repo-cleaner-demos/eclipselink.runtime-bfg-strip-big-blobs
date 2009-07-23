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
package org.eclipse.persistence.internal.oxm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.InheritancePolicy;
import org.eclipse.persistence.internal.oxm.XMLObjectBuilder;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.oxm.record.MarshalContext;
import org.eclipse.persistence.internal.oxm.record.ObjectMarshalContext;
import org.eclipse.persistence.internal.oxm.record.SequencedMarshalContext;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.foundation.AbstractDirectMapping;
import org.eclipse.persistence.mappings.foundation.AbstractTransformationMapping;
import org.eclipse.persistence.mappings.transformers.FieldTransformer;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.XMLMarshaller;
import org.eclipse.persistence.oxm.mappings.XMLAnyCollectionMapping;
import org.eclipse.persistence.oxm.mappings.XMLAnyAttributeMapping;
import org.eclipse.persistence.oxm.mappings.XMLAnyObjectMapping;
import org.eclipse.persistence.oxm.mappings.XMLBinaryDataMapping;
import org.eclipse.persistence.oxm.mappings.XMLBinaryDataCollectionMapping;
import org.eclipse.persistence.oxm.mappings.XMLCompositeCollectionMapping;
import org.eclipse.persistence.oxm.mappings.XMLCompositeDirectCollectionMapping;
import org.eclipse.persistence.oxm.mappings.XMLCompositeObjectMapping;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.oxm.mappings.XMLCollectionReferenceMapping;
import org.eclipse.persistence.oxm.mappings.XMLFragmentCollectionMapping;
import org.eclipse.persistence.oxm.mappings.XMLFragmentMapping;
import org.eclipse.persistence.oxm.mappings.XMLObjectReferenceMapping;
import org.eclipse.persistence.oxm.mappings.XMLChoiceObjectMapping;
import org.eclipse.persistence.oxm.mappings.XMLChoiceCollectionMapping;
import org.eclipse.persistence.oxm.record.MarshalRecord;
import org.eclipse.persistence.oxm.record.NodeRecord;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;
import org.eclipse.persistence.oxm.sequenced.SequencedObject;
import org.w3c.dom.Node;

/**
 * INTERNAL:
 * <p><b>Purpose</b>:  Perform the unmarshal and marshal operations based on the
 * object-to-XML mapping metadata.</p>
 * <p><b>Responsibilities</b>:<ul>
 * <li>Convert mapping metadata to a tree of XPathNodes.  This tree is then
 * traversed during unmarshal and marshal operations.</li>
 * <li>Create records appropriate to this implementation of ObjectBuilder.</li>
 * </ul>
 */
public class TreeObjectBuilder extends XMLObjectBuilder {
    private XPathNode rootXPathNode;
    private List transformationMappings;
    private List containerValues;
    private List nullCapableValues;

    public TreeObjectBuilder(ClassDescriptor descriptor) {
        super(descriptor);
        rootXPathNode = new XPathNode();
    }

    public XPathNode getRootXPathNode() {
        return this.rootXPathNode;
    }

    public void addTransformationMapping(AbstractTransformationMapping transformationMapping) {
        if (null == getTransformationMappings()) {
            this.transformationMappings = new ArrayList();
        }
        transformationMappings.add(transformationMapping);
    }

    public List getTransformationMappings() {
        return this.transformationMappings;
    }

    public List getContainerValues() {
        return this.containerValues;
    }

    public void addContainerValue(ContainerValue containerValue) {
        if (null == getContainerValues()) {
            this.containerValues = new ArrayList();
        }
        this.containerValues.add(containerValue);
    }

    public List getNullCapableValues() {
        return this.nullCapableValues;
    }

    public void addNullCapableValue(NullCapableValue nullCapableValue) {
        if (null == getNullCapableValues()) {
            this.nullCapableValues = new ArrayList();
        }
        this.nullCapableValues.add(nullCapableValue);
    }

    public void initialize(org.eclipse.persistence.internal.sessions.AbstractSession session) {
        super.initialize(session);
        XMLDescriptor xmlDescriptor = (XMLDescriptor)getDescriptor();

        // MAPPINGS
        Iterator mappingIterator = xmlDescriptor.getMappings().iterator();
        Iterator fieldTransformerIterator;
        DatabaseMapping xmlMapping;

        // Transformation Mapping
        AbstractTransformationMapping transformationMapping;
        FieldTransformerNodeValue fieldTransformerNodeValue;
        Object[] nextFieldToTransformer;

        // Simple Type Translator
        TypeNodeValue typeNodeValue;

        NodeValue mappingNodeValue = null;
        XMLField xmlField;
        while (mappingIterator.hasNext()) {
            xmlMapping = (DatabaseMapping)mappingIterator.next();
            xmlField = (XMLField)xmlMapping.getField();
            if (xmlMapping.isTransformationMapping()) {
                transformationMapping = (AbstractTransformationMapping)xmlMapping;
                addTransformationMapping(transformationMapping);
                fieldTransformerIterator = transformationMapping.getFieldToTransformers().iterator();
                while (fieldTransformerIterator.hasNext()) {
                    fieldTransformerNodeValue = new FieldTransformerNodeValue();
                    nextFieldToTransformer = (Object[])fieldTransformerIterator.next();
                    xmlField = (XMLField)nextFieldToTransformer[0];
                    fieldTransformerNodeValue.setXMLField(xmlField);
                    fieldTransformerNodeValue.setFieldTransformer((FieldTransformer)nextFieldToTransformer[1]);
                    addChild(xmlField.getXPathFragment(), fieldTransformerNodeValue, xmlDescriptor.getNamespaceResolver());
                }
            } else {
                if (xmlMapping.isAbstractDirectMapping()) {
                    mappingNodeValue = new XMLDirectMappingNodeValue((XMLDirectMapping)xmlMapping);
                } else if (xmlMapping.isAbstractCompositeObjectMapping()) {
                    mappingNodeValue = new XMLCompositeObjectMappingNodeValue((XMLCompositeObjectMapping)xmlMapping);
                } else if (xmlMapping.isAbstractCompositeDirectCollectionMapping()) {
                    mappingNodeValue = new XMLCompositeDirectCollectionMappingNodeValue((XMLCompositeDirectCollectionMapping)xmlMapping);
                } else if (xmlMapping.isAbstractCompositeCollectionMapping()) {
                    mappingNodeValue = new XMLCompositeCollectionMappingNodeValue((XMLCompositeCollectionMapping)xmlMapping);
                } else if (xmlMapping instanceof XMLAnyObjectMapping) {
                    mappingNodeValue = new XMLAnyObjectMappingNodeValue((XMLAnyObjectMapping)xmlMapping);
                } else if (xmlMapping instanceof XMLAnyCollectionMapping) {
                    mappingNodeValue = new XMLAnyCollectionMappingNodeValue((XMLAnyCollectionMapping)xmlMapping);
                } else if (xmlMapping instanceof XMLAnyAttributeMapping) {
                    mappingNodeValue = new XMLAnyAttributeMappingNodeValue((XMLAnyAttributeMapping)xmlMapping);
                } else if (xmlMapping instanceof XMLBinaryDataMapping) {
                    mappingNodeValue = new XMLBinaryDataMappingNodeValue((XMLBinaryDataMapping)xmlMapping);
                } else if (xmlMapping instanceof XMLBinaryDataCollectionMapping) {
                    mappingNodeValue = new XMLBinaryDataCollectionMappingNodeValue((XMLBinaryDataCollectionMapping)xmlMapping);
                } else if (xmlMapping instanceof XMLFragmentMapping) {
                    mappingNodeValue = new XMLFragmentMappingNodeValue((XMLFragmentMapping)xmlMapping);
                } else if (xmlMapping instanceof XMLFragmentCollectionMapping) {
                    mappingNodeValue = new XMLFragmentCollectionMappingNodeValue((XMLFragmentCollectionMapping)xmlMapping);
                } else if (xmlMapping instanceof XMLCollectionReferenceMapping) {
                    XMLCollectionReferenceMapping xmlColMapping = (XMLCollectionReferenceMapping)xmlMapping;
                    Iterator fieldIt = xmlColMapping.getFields().iterator();
                    while (fieldIt.hasNext()) {
                        XMLField xmlFld = (XMLField)fieldIt.next();
                        mappingNodeValue = new XMLCollectionReferenceMappingNodeValue(xmlColMapping, xmlFld);
                        if (mappingNodeValue.isContainerValue()) {
                            addContainerValue((ContainerValue)mappingNodeValue);
                        }
                        if (mappingNodeValue.isNullCapableValue()) {
                            addNullCapableValue((NullCapableValue)mappingNodeValue);
                        }
                        addChild(xmlFld.getXPathFragment(), mappingNodeValue, xmlDescriptor.getNamespaceResolver());
                    }
                    continue;
                } else if (xmlMapping instanceof XMLObjectReferenceMapping) {
                    XMLObjectReferenceMapping xmlORMapping = (XMLObjectReferenceMapping)xmlMapping;
                    Iterator fieldIt = xmlORMapping.getFields().iterator();
                    while (fieldIt.hasNext()) {
                        XMLField xmlFld = (XMLField)fieldIt.next();
                        mappingNodeValue = new XMLObjectReferenceMappingNodeValue(xmlORMapping, xmlFld);
                        if (mappingNodeValue.isContainerValue()) {
                            addContainerValue((ContainerValue)mappingNodeValue);
                        }
                        if (mappingNodeValue.isNullCapableValue()) {
                            addNullCapableValue((NullCapableValue)mappingNodeValue);
                        }
                        addChild(xmlFld.getXPathFragment(), mappingNodeValue, xmlDescriptor.getNamespaceResolver());
                    }
                    continue;
                } else if (xmlMapping instanceof XMLChoiceObjectMapping) {
                    XMLChoiceObjectMapping xmlChoiceMapping = (XMLChoiceObjectMapping)xmlMapping;
                    Iterator fields = xmlChoiceMapping.getChoiceElementMappings().keySet().iterator();
                    XMLField firstField = (XMLField)fields.next();
                    XMLChoiceObjectMappingNodeValue firstNodeValue = new XMLChoiceObjectMappingNodeValue(xmlChoiceMapping, firstField);
                    firstNodeValue.setNullCapableNodeValue(firstNodeValue);
                    this.addNullCapableValue(firstNodeValue);
                    addChild(firstField.getXPathFragment(), firstNodeValue, xmlDescriptor.getNamespaceResolver());
                    while(fields.hasNext()) {
                        XMLField next = (XMLField)fields.next();
                        XMLChoiceObjectMappingNodeValue nodeValue = new XMLChoiceObjectMappingNodeValue(xmlChoiceMapping, next);
                        nodeValue.setNullCapableNodeValue(firstNodeValue);
                        addChild(next.getXPathFragment(), nodeValue, xmlDescriptor.getNamespaceResolver());
                    }
                    continue;
                } else if(xmlMapping instanceof XMLChoiceCollectionMapping) {
                    XMLChoiceCollectionMapping xmlChoiceMapping = (XMLChoiceCollectionMapping)xmlMapping;
                    Iterator fields = xmlChoiceMapping.getChoiceElementMappings().keySet().iterator();
                    XMLField firstField = (XMLField)fields.next();
                    XMLChoiceCollectionMappingUnmarshalNodeValue unmarshalValue = new XMLChoiceCollectionMappingUnmarshalNodeValue(xmlChoiceMapping, firstField);
                    XMLChoiceCollectionMappingMarshalNodeValue marshalValue = new XMLChoiceCollectionMappingMarshalNodeValue(xmlChoiceMapping, firstField);
                    HashMap<XMLField, NodeValue> fieldToNodeValues = new HashMap<XMLField, NodeValue>();
                    unmarshalValue.setContainerNodeValue(unmarshalValue);
                    marshalValue.setFieldToNodeValues(fieldToNodeValues);
                    this.addContainerValue(unmarshalValue);
                    fieldToNodeValues.put(firstField, unmarshalValue);
                    addChild(firstField.getXPathFragment(), unmarshalValue, xmlDescriptor.getNamespaceResolver());
                    addChild(firstField.getXPathFragment(), marshalValue, xmlDescriptor.getNamespaceResolver());
                    while(fields.hasNext()) {
                        XMLField next = (XMLField)fields.next();
                        XMLChoiceCollectionMappingUnmarshalNodeValue nodeValue = new XMLChoiceCollectionMappingUnmarshalNodeValue(xmlChoiceMapping, next);
                        nodeValue.setContainerNodeValue(unmarshalValue);
                        addChild(next.getXPathFragment(), nodeValue, xmlDescriptor.getNamespaceResolver());
                        fieldToNodeValues.put(next, nodeValue);
                    }
                    continue;
                }
                if (mappingNodeValue.isContainerValue()) {
                    addContainerValue((ContainerValue)mappingNodeValue);
                }
                if (mappingNodeValue.isNullCapableValue()) {
                    addNullCapableValue((NullCapableValue)mappingNodeValue);
                }
                if (xmlField != null) {
                    addChild(xmlField.getXPathFragment(), mappingNodeValue, xmlDescriptor.getNamespaceResolver());
                } else {
                    addChild(null, mappingNodeValue, xmlDescriptor.getNamespaceResolver());
                }
                if (xmlMapping.isAbstractDirectMapping() && xmlField.isTypedTextField()) {
                    XPathFragment nextFragment = xmlField.getXPathFragment();
                    String typeXPath = "";
                    while (nextFragment.getNextFragment() != null) {
                        typeXPath += nextFragment.getXPath();
                        nextFragment = nextFragment.getNextFragment();
                    }
                    XMLField typeField = new XMLField();
                    if (!"".equals(typeXPath)) {
                        typeXPath += "/";
                    }
                    typeField.setXPath(typeXPath + "@" + xmlDescriptor.getNonNullNamespaceResolver().resolveNamespaceURI(XMLConstants.SCHEMA_INSTANCE_URL) + ":type");
                    typeNodeValue = new TypeNodeValue();
                    typeNodeValue.setDirectMapping((AbstractDirectMapping)xmlMapping);
                    addChild(typeField.getXPathFragment(), typeNodeValue, xmlDescriptor.getNamespaceResolver());
                }
            }
        }

        // INHERITANCE
        if (xmlDescriptor.hasInheritance()) {
            InheritancePolicy inheritancePolicy = xmlDescriptor.getInheritancePolicy();
            
            if (!inheritancePolicy.hasClassExtractor()) {
                XMLField classIndicatorField = new XMLField(inheritancePolicy.getClassIndicatorFieldName());
                classIndicatorField.setNamespaceResolver(xmlDescriptor.getNamespaceResolver());
                    
                InheritanceNodeValue inheritanceNodeValue = new InheritanceNodeValue();
                inheritanceNodeValue.setInheritancePolicy(inheritancePolicy);
    
                addChild(classIndicatorField.getXPathFragment(), inheritanceNodeValue, xmlDescriptor.getNamespaceResolver());
            }
        }
    }

    public void addChild(XPathFragment xPathFragment, NodeValue nodeValue, NamespaceResolver namespaceResolver) {
        getRootXPathNode().addChild(xPathFragment, nodeValue, namespaceResolver);
    }

    public AbstractRecord buildRow(AbstractRecord record, Object object, org.eclipse.persistence.internal.sessions.AbstractSession session) {
        return buildRow(record, object, session, null);
    }

    public AbstractRecord buildRow(AbstractRecord record, Object object, org.eclipse.persistence.internal.sessions.AbstractSession session, XMLMarshaller marshaller) {
        if (null == getRootXPathNode().getNonAttributeChildren()) {
            return record;
        }
        XMLDescriptor xmlDescriptor = (XMLDescriptor) this.getDescriptor();
        XPathNode xPathNode;
        NamespaceResolver namespaceResolver = xmlDescriptor.getNamespaceResolver();
        MarshalContext marshalContext = null;
        if(xmlDescriptor.isSequencedObject()) {
            SequencedObject sequencedObject = (SequencedObject) object;
            marshalContext = new SequencedMarshalContext(sequencedObject.getSettings());
        } else {
            marshalContext = ObjectMarshalContext.getInstance();
        }
        int size = marshalContext.getNonAttributeChildrenSize(getRootXPathNode());
        for (int x = 0; x < size; x++) {
            xPathNode = (XPathNode)marshalContext.getNonAttributeChild(x, getRootXPathNode());
            xPathNode.marshal((MarshalRecord)record, object, session, namespaceResolver, marshaller, marshalContext.getMarshalContext(x));
        }
        return record;
    }

    public boolean marshalAttributes(MarshalRecord marshalRecord, Object object, AbstractSession session) {
        boolean hasValue = false;

        XPathNode attributeNode;
        NamespaceResolver namespaceResolver;
        if (rootXPathNode.getAttributeChildren() != null) {
            int size = rootXPathNode.getAttributeChildren().size();
            for (int x = 0; x < size; x++) {
                attributeNode = (XPathNode)rootXPathNode.getAttributeChildren().get(x);
                namespaceResolver = ((XMLDescriptor)this.getDescriptor()).getNamespaceResolver();
                hasValue = attributeNode.marshal(marshalRecord, object, session, namespaceResolver, ObjectMarshalContext.getInstance()) || hasValue;
            }
        }

        if (rootXPathNode.getAnyAttributeNode() != null) {
            namespaceResolver = ((XMLDescriptor)this.getDescriptor()).getNamespaceResolver();
            hasValue = rootXPathNode.getAnyAttributeNode().marshal(marshalRecord, object, session, namespaceResolver, ObjectMarshalContext.getInstance()) || hasValue;
        }
        
        if (rootXPathNode.getSelfChildren() != null) {
            XPathNode childNode;
            for (int x = 0; x < rootXPathNode.getSelfChildren().size(); x++) {
                childNode = (XPathNode)rootXPathNode.getSelfChildren().get(x);
                namespaceResolver = ((XMLDescriptor)this.getDescriptor()).getNamespaceResolver();
                childNode.marshalSelfAttributes(marshalRecord, object, session, namespaceResolver, marshalRecord.getMarshaller());
            }
        }
        
        return hasValue;
    }

    /**
     * Create a new row/record for the object builder.
     * This allows subclasses to define different record types.
     */
    public AbstractRecord createRecord(AbstractSession session) {
    	UnmarshalRecord uRec = new UnmarshalRecord(this);
    	uRec.setSession(session);
        return uRec;
    }

    /**
     * Create a new row/record for the object builder with the given name.
     * This allows subclasses to define different record types.
     */
    public AbstractRecord createRecord(String rootName, AbstractSession session) {
        NodeRecord nRec = new NodeRecord(rootName, getNamespaceResolver());
        nRec.setSession(session);
    	return nRec;
    }

    /**
     * Create a new row/record for the object builder with the given name.
     * This allows subclasses to define different record types.
     */
    public AbstractRecord createRecord(String rootName, Node parent, AbstractSession session) {
        NodeRecord nRec = new NodeRecord(rootName, getNamespaceResolver(), parent);
        nRec.setSession(session);
    	return nRec;
    }

    /**
     * Create a new row/record for the object builder.
     * This allows subclasses to define different record types.
     */
    public AbstractRecord createRecord(int size, AbstractSession session) {
        return createRecord(session);
    }
        
}
