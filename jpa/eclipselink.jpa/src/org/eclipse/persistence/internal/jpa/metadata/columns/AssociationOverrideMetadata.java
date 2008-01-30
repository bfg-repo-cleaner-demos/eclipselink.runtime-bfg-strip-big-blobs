/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/
package org.eclipse.persistence.internal.jpa.metadata.columns;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.JoinColumn;

import javax.persistence.AssociationOverride;

/**
 * Object to hold onto an association override meta data.
 * 
 * @author Guy Pelletier
 * @since EclipseLink 1.0
 */
public class AssociationOverrideMetadata extends OverrideMetadata {
	private List<JoinColumnMetadata> m_joinColumns;
	
	/**
	 * INTERNAL:
	 * Assumed to be used solely for OX loading.
	 */
	public AssociationOverrideMetadata() {}
	
	/**
	 * INTERNAL:
	 */
	public AssociationOverrideMetadata(AssociationOverride associationOverride, String className) {
		super(className);

		setName(associationOverride.name());
		
		m_joinColumns = new ArrayList<JoinColumnMetadata>();
		for (JoinColumn joinColumn : associationOverride.joinColumns()) {
			m_joinColumns.add(new JoinColumnMetadata(joinColumn));
		}
	}
	
	/**
	 * INTERNAL:
	 * Used for OX mapping.
	 */
	public List<JoinColumnMetadata> getJoinColumns() {
		return m_joinColumns;
	}
	
	/**
	 * INTERNAL:
	 * Used for OX mapping.
	 */
	public void setJoinColumns(List<JoinColumnMetadata> joinColumns) {
		m_joinColumns = joinColumns;
	}
}
