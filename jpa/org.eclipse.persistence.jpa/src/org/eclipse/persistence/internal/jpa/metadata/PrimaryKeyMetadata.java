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
 *     James Sutherland - initial impl
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.persistence.annotations.IdValidation;
import org.eclipse.persistence.annotations.PrimaryKey;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.ORMetadata;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.columns.ColumnMetadata;

/**
 * Object to hold onto primary key metadata.
 * 
 * @see PrimaryKey
 * @author James Sutherland
 * @since EclipseLink 1.1
 */
public class PrimaryKeyMetadata extends ORMetadata {
    private Enum m_validation;
    private List<ColumnMetadata> m_columns = new ArrayList<ColumnMetadata>();

    public PrimaryKeyMetadata() {
        super("<primary-key>");
    }

    public PrimaryKeyMetadata(Annotation primaryKey, MetadataAccessibleObject accessibleObject) {
        super(primaryKey, accessibleObject);
        
        m_validation = (Enum) MetadataHelper.invokeMethod("validation", primaryKey);
        
        for (Annotation selectedColumn : (Annotation[]) MetadataHelper.invokeMethod("columns", primaryKey)) {
        	m_columns.add(new ColumnMetadata(selectedColumn, accessibleObject));
        }
    }

    public boolean hasColumns() {
        return ! m_columns.isEmpty();
    }

    /**
     * Process the meta-data, configure primary key and idValidation in descriptor.
     */
    public void process(MetadataDescriptor descriptor) {
        descriptor.getClassDescriptor().setIdValidation(IdValidation.valueOf(m_validation.name()));
        if (hasColumns()) {
        	List fields = new ArrayList(m_columns.size());
            for (ColumnMetadata column : m_columns) {
                if (column.getName().equals("")) {  
                    throw ValidationException.optimisticLockingSelectedColumnNamesNotSpecified(descriptor.getJavaClass());
                } else {
                	fields.add(column.getDatabaseField());
                }
            }
            descriptor.getClassDescriptor().setPrimaryKeyFields(fields);
        }
    }
}
