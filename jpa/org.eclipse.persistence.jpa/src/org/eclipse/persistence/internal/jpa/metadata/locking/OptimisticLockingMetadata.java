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
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.locking;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.persistence.annotations.OptimisticLockingType;
import org.eclipse.persistence.descriptors.AllFieldsLockingPolicy;
import org.eclipse.persistence.descriptors.ChangedFieldsLockingPolicy;
import org.eclipse.persistence.descriptors.SelectedFieldsLockingPolicy;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.ORMetadata;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAnnotation;
import org.eclipse.persistence.internal.jpa.metadata.columns.ColumnMetadata;

/**
 * Object to hold onto optimistic locking metadata.
 * 
 * @author Guy Pelletier
 * @since TopLink 11g
 */
public class OptimisticLockingMetadata extends ORMetadata {
    private Boolean m_cascade;
    private List<ColumnMetadata> m_selectedColumns = new ArrayList<ColumnMetadata>();
    private String m_type;
    
    /**
     * INTERNAL:
     */
    public OptimisticLockingMetadata() {
        super("<optimistic-locking>");
    }
    
    /**
     * INTERNAL:
     */
    public OptimisticLockingMetadata(MetadataAnnotation optimisticLocking, MetadataAccessibleObject accessibleObject) {
        super(optimisticLocking, accessibleObject);
        
        m_type = (String) optimisticLocking.getAttribute("type");
        m_cascade = (Boolean) optimisticLocking.getAttribute("cascade");
        
        for (Object selectedColumn : (Object[]) optimisticLocking.getAttributeArray("selectedColumns")) {
            m_selectedColumns.add(new ColumnMetadata((MetadataAnnotation)selectedColumn, accessibleObject));
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public Boolean getCascade() {
        return m_cascade;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public List<ColumnMetadata> getSelectedColumns() {
        return m_selectedColumns;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getType() {
        return m_type;
    }
    
    /**
     * INTERNAL:
     */
    public boolean hasSelectedColumns() {
        return ! m_selectedColumns.isEmpty();
    }
    
    /**
     * INTERNAL:
     */
    public void process(MetadataDescriptor descriptor) {
        // Process the type. A null will default to VERSION_COLUMN.
        if (m_type == null || m_type.equals(OptimisticLockingType.VERSION_COLUMN.name())) {
            // A version annotation or element should be define and discovered
            // in later processing.
            descriptor.setUsesCascadedOptimisticLocking(m_cascade != null && m_cascade.booleanValue());
        } else if (m_type.equals(OptimisticLockingType.ALL_COLUMNS.name())) {
            descriptor.setOptimisticLockingPolicy(new AllFieldsLockingPolicy());
        } else if (m_type.equals(OptimisticLockingType.CHANGED_COLUMNS.name())) {
            descriptor.setOptimisticLockingPolicy(new ChangedFieldsLockingPolicy());
        } else if (m_type.equals(OptimisticLockingType.SELECTED_COLUMNS.name())) {
            if (m_selectedColumns.isEmpty()) {
                throw ValidationException.optimisticLockingSelectedColumnNamesNotSpecified(descriptor.getJavaClass());
            } else {
                SelectedFieldsLockingPolicy policy = new SelectedFieldsLockingPolicy();
                
                // Process the selectedColumns
                for (ColumnMetadata selectedColumn : m_selectedColumns) {
                    if (selectedColumn.getName().equals("")) {  
                        throw ValidationException.optimisticLockingSelectedColumnNamesNotSpecified(descriptor.getJavaClass());
                    } else {
                        policy.addLockFieldName(selectedColumn.getName());
                    }
                }
                
                descriptor.setOptimisticLockingPolicy(policy);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setCascade(Boolean cascade) {
        m_cascade = cascade;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setSelectedColumns(List<ColumnMetadata> selectedColumns) {
        m_selectedColumns = selectedColumns;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setType(String type) {
        m_type = type;
    }
}
