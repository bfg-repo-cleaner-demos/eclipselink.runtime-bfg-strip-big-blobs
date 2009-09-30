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
 *     03/26/2008-1.0M6 Guy Pelletier 
 *       - 211302: Add variable 1-1 mapping support to the EclipseLink-ORM.XML Schema
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files  
 *     02/06/2009-2.0 Guy Pelletier 
 *       - 248293: JPA 2.0 Element Collections (part 2)
 *     03/27/2009-2.0 Guy Pelletier 
 *       - 241413: JPA 2.0 Add EclipseLink support for Map type attributes
 *     05/1/2009-2.0 Guy Pelletier 
 *       - 249033: JPA 2.0 Orphan removal   
 *     09/29/2009-2.0 Guy Pelletier 
 *       - 282553: JPA 2.0 JoinTable support for OneToOne and ManyToOne
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.mappings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;

import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EntityAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.InterfaceAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.columns.DiscriminatorClassMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.DiscriminatorColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.columns.JoinColumnMetadata;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;

import org.eclipse.persistence.mappings.VariableOneToOneMapping;

/**
 * INTERNAL:
 * A variable one to one relationship accessor. A VariableOneToOne annotation 
 * currently is not required to be defined on the accessible object, that is, 
 * a v1-1 can default if the raw class is an interface.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 1.0
 */
public class VariableOneToOneAccessor extends ObjectAccessor {
    public static final String DEFAULT_QUERY_KEY = "id";
    
    private Integer m_lastDiscriminatorIndex;
    private DiscriminatorColumnMetadata m_discriminatorColumn;
    private List<DiscriminatorClassMetadata> m_discriminatorClasses;
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public VariableOneToOneAccessor() {
        super("<variable-one-to-one>");
    }
    
    /**
     * INTERNAL:
     */
    public VariableOneToOneAccessor(MetadataAnnotation variableOneToOne, MetadataAccessibleObject accessibleObject, ClassAccessor classAccessor) {
        super(variableOneToOne, accessibleObject, classAccessor);
        
        // Initialiaze the discriminator classes list.
        m_discriminatorClasses = new ArrayList<DiscriminatorClassMetadata>();
        
        // We must check because VariableOneToOne's can default.
        if (variableOneToOne != null) {
            // Parent class looks for 'targetEntity' and not 'targetInterface'
            // Need to set it correctly.
            setTargetEntity(getMetadataClass((String)variableOneToOne.getAttribute("targetInterface")));
            setOrphanRemoval((Boolean) variableOneToOne.getAttribute("orphanRemoval"));
            
            m_discriminatorColumn = new DiscriminatorColumnMetadata((MetadataAnnotation) variableOneToOne.getAttribute("discriminatorColumn"), accessibleObject);
            
            // Set the discriminator classes if specified.
            for (Object discriminatorClass : (Object[]) variableOneToOne.getAttributeArray("discriminatorClasses")) {
                m_discriminatorClasses.add(new DiscriminatorClassMetadata((MetadataAnnotation)discriminatorClass, accessibleObject));
            }
        }
    }
    
    /**
     * INTERNAL:
     * In stage 2 processing entities may be added to the discriminator
     * class list for this variable one to one accessor if they were not
     * explicitely added but define the interface associated with this
     * accessors target interface.
     */
    public void addDiscriminatorClassFor(EntityAccessor accessor) {                
        // We didn't find a discriminator class metadata for the given entity
        // accessor so we need to default one.
        VariableOneToOneMapping mapping = (VariableOneToOneMapping) getDescriptor().getMappingForAttributeName(getAttributeName());

        // Also need to add the interface to the descriptor.
        accessor.getDescriptor().getClassDescriptor().getInterfacePolicy().addParentInterfaceName(mapping.getReferenceClassName());
        
        for (DiscriminatorClassMetadata discriminatorClass : m_discriminatorClasses) {
            if (discriminatorClass.getValue().equals(accessor.getJavaClass())) {
                // A discriminator class was configured for this entity, do
                // nothing and return.
                return;
            }
        }
        
        Class type = mapping.getTypeField().getType();
        if (type.equals(String.class)) {
            mapping.addClassNameIndicator(accessor.getJavaClassName(), accessor.getDescriptor().getAlias());  
        } else if (type.equals(Character.class)) {
            mapping.addClassNameIndicator(accessor.getJavaClassName(), accessor.getJavaClassName().substring(0, 1));  
        } else {
            if (m_lastDiscriminatorIndex == null) {
                // Our discriminators are added as Strings ...
                for (String stringIndex : (List<String>) mapping.getTypeIndicatorNameTranslation().values()) {
                    Integer index = new Integer(stringIndex);

                    if (m_lastDiscriminatorIndex == null || m_lastDiscriminatorIndex < index) {
                        m_lastDiscriminatorIndex = index;
                    }
                }
            }
            
            mapping.addClassNameIndicator(accessor.getJavaClassName(), ++m_lastDiscriminatorIndex);
        } 
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<DiscriminatorClassMetadata> getDiscriminatorClasses() {
        return m_discriminatorClasses;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public DiscriminatorColumnMetadata getDiscriminatorColumn() {
        return m_discriminatorColumn;
    }
    
    /**
     * INTERNAL:
     * Return the logging context for this accessor.
     */
    protected String getLoggingContext() {
        return MetadataLogger.VARIABLE_ONE_TO_ONE_MAPPING_REFERENCE_CLASS;
    }
    
    /**
     * INTERNAL:
     * In a variable one to one case, there is no knowledge of a reference
     * descriptor and the join columns should be defaulted based on the owner 
     * of the variable one to one's descriptor.
     */
    @Override
    public MetadataDescriptor getReferenceDescriptor() {
        return getDescriptor();
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject, XMLEntityMappings entityMappings) {
        super.initXMLObject(accessibleObject, entityMappings);
    
        // Init the single ORMetadata objects.
        initXMLObject(m_discriminatorColumn, accessibleObject);
        
        // Init the lists of ORMetadata objects.
        initXMLObjects(m_discriminatorClasses, accessibleObject);
    }
    
    /**
     * INTERNAL:
     */
    @Override
    public boolean isVariableOneToOne() {
        return true;
    }
    
    /**
     * INTERNAL:
     * Process a variable one to one setting into an EclipseLink 
     * VariableOneToOneMapping.
     */
    public void process() {
        // Add ourselves to the list of variable one to one accessors to this
        // interface. If an InterfaceAccessor doesn't exist, create one. It
        // will be re-used for each variable one to one accessor that uses
        // the same interface class.
        InterfaceAccessor interfaceAccessor = getProject().getInterfaceAccessor(getReferenceClassName());
        if (interfaceAccessor == null) {
            interfaceAccessor = new InterfaceAccessor(null, getReferenceClass(), getProject());
            interfaceAccessor.process();
            getProject().addInterfaceAccessor(interfaceAccessor);
        }
        interfaceAccessor.addVariableOneToOneAccessor(this);
        
        // Now process our variable one to one mapping.
        VariableOneToOneMapping mapping = new VariableOneToOneMapping();
        setMapping(mapping);
        
        mapping.setIsReadOnly(false);
        mapping.setIsOptional(isOptional());
        mapping.setAttributeName(getAttributeName());
        mapping.setReferenceClassName(getReferenceClassName());
        
        // Process the orphanRemoval or PrivateOwned
        processOrphanRemoval(mapping);
        
        // Process the indirection.
        processIndirection(mapping);
        
        // Set the getter and setter methods if access is PROPERTY.
        setAccessorMethods(mapping);
        
        // Process the cascade types.
        processCascadeTypes(mapping);
        
        // Process a @ReturnInsert and @ReturnUpdate (to log a warning message)
        processReturnInsertAndUpdate();

        // Process the discriminator column.
        if (m_discriminatorColumn == null) {
            mapping.setTypeField(new DiscriminatorColumnMetadata().process(getDescriptor(), getAnnotatedElementName(), MetadataLogger.VARIABLE_ONE_TO_ONE_DISCRIMINATOR_COLUMN));
        } else {
            mapping.setTypeField(m_discriminatorColumn.process(getDescriptor(), getAnnotatedElementName(), MetadataLogger.VARIABLE_ONE_TO_ONE_DISCRIMINATOR_COLUMN));
        }
        
        // Process the discriminator classes.
        for (DiscriminatorClassMetadata discriminatorClass : m_discriminatorClasses) {
            discriminatorClass.process(mapping);
        }
        
        // Process the foreign query keys from the join columns.
        processForeignQueryKeyNames(mapping);
    }
    
    /**
     * INTERNAL:
     */
    protected void processForeignQueryKeyNames(VariableOneToOneMapping mapping) {
        // Add the source foreign key fields to the mapping.
        for (JoinColumnMetadata joinColumn : getJoinColumns(getJoinColumns(), getReferenceDescriptor())) {
            // The query key name will be extracted from the referenced column
            // name. It defaults to ID otherwise.
            String queryKeyName = getName(joinColumn.getReferencedColumnName(), DEFAULT_QUERY_KEY, MetadataLogger.QK_COLUMN);
            
            DatabaseField fkField = joinColumn.getForeignKeyField();
            setFieldName(fkField, getDefaultAttributeName() + "_ID", MetadataLogger.FK_COLUMN);
            // Set the table name if one is not already set.
            if (fkField.getTableName().equals("")) {
                fkField.setTable(getDescriptor().getPrimaryTable());
            }

            // Add the foreign query key to the mapping.
            mapping.addForeignQueryKeyName(fkField, queryKeyName);
            
            // If any of the join columns is marked read-only then set the 
            // mapping to be read only.
            if (fkField.isReadOnly()) {
                mapping.setIsReadOnly(true);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setDiscriminatorClasses(List<DiscriminatorClassMetadata> discriminatorClasses) {
        m_discriminatorClasses = discriminatorClasses;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setDiscriminatorColumn(DiscriminatorColumnMetadata discriminatorColumn) {
        m_discriminatorColumn = discriminatorColumn;
    }
}

