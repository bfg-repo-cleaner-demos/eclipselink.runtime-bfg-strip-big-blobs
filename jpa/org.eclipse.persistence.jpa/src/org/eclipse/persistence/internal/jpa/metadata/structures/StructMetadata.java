/*******************************************************************************
 * Copyright (c) 2011, 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial implementation
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.structures;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.ORMetadata;
import org.eclipse.persistence.internal.jpa.metadata.accessors.MetadataAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.mappings.structures.ObjectRelationalDataTypeDescriptor;

/**
 * Defines the metadata for the @Struct annotation for mapping
 * ObjectRelationshipDataTypeDescriptor and StructureMappings.
 * 
 * Key notes:
 * - any metadata mapped from XML to this class must be compared in the
 *   equals method.
 * - when loading from annotations, the constructor accepts the metadata
 *   accessor this metadata was loaded from. Used it to look up any 
 *   'companion' annotation needed for processing.
 * - methods should be preserved in alphabetical order.
 * 
 * @author James Sutherland
 * @since EclipseLink 2.3
 */
public class StructMetadata extends ORMetadata {
    private String name;
    private List<String> fields = new ArrayList<String>();
    
    /**
     * Used for XML loading.
     */
    public StructMetadata() {
        super("<struct>");
    }
    
    /**
     * Used for annotation loading.
     */
    public StructMetadata(MetadataAnnotation struct, MetadataAccessor accessor) {
        super(struct, accessor);
        
        this.name = (String)struct.getAttribute("name");
        for (Object field : (Object[])struct.getAttributeArray("fields")) {
            this.fields.add((String)field);
        }
    }
    
    /**
     * INTERNAL:
     * For merging and overriding to work properly, all ORMetadata must be able 
     * to compare themselves for metadata equality.
     */
    @Override
    public boolean equals(Object objectToCompare) {
        if (objectToCompare instanceof StructMetadata) {
            StructMetadata struct = (StructMetadata) objectToCompare;
            if (! valuesMatch(this.fields, struct.getFields())) {
                return false;
            }
            return valuesMatch(this.name, ((StructMetadata) objectToCompare).getName());
        }
        
        return false;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<String> getFields() {
        return fields;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Switch the descriptor to the correct type and
     * set the structure name and properties.
     */
    public void process(MetadataDescriptor descriptor) {
        ClassDescriptor oldDesriptor = descriptor.getClassDescriptor();
        ObjectRelationalDataTypeDescriptor newDescriptor = new ObjectRelationalDataTypeDescriptor();
        newDescriptor.setJavaClassName(oldDesriptor.getJavaClassName());
        newDescriptor.setStructureName(getName());
        for (String field : this.fields) {
            newDescriptor.addFieldOrdering(field);
        }
        newDescriptor.setAlias("");        
        // This is the default, set it in case no existence-checking is set.
        newDescriptor.getQueryManager().checkDatabaseForDoesExist();
        if (oldDesriptor.isAggregateDescriptor()) {
            newDescriptor.descriptorIsAggregate();
        }
        descriptor.setDescriptor(newDescriptor);
        // Also need to switch the descriptor in the project.
        descriptor.getProject().getProject().getOrderedDescriptors().remove(oldDesriptor);
        descriptor.getProject().getProject().getOrderedDescriptors().add(newDescriptor);
    }

    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setFields(List<String> fields) {
        this.fields = fields;
    }  
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setName(String name) {
        this.name = name;
    }  
}
    
