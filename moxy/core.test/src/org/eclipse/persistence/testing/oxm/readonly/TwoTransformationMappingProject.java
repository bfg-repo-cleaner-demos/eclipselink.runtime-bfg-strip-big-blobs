/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.readonly;

import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.oxm.*;
import org.eclipse.persistence.oxm.mappings.XMLTransformationMapping;
import org.eclipse.persistence.testing.oxm.readonly.NormalHours2Transformer;

public class TwoTransformationMappingProject extends Project 
{
	public TwoTransformationMappingProject() 
	{
		super();
		addEmployeeDescriptor();
	}
	
	public void addEmployeeDescriptor() 
	{
		XMLDescriptor descriptor = new XMLDescriptor();
		descriptor.setDefaultRootElement("employee");
		descriptor.setJavaClass(Employee.class);
		
		XMLTransformationMapping mapping = new XMLTransformationMapping();
		mapping.setAttributeName("normalHours");
		mapping.setAttributeTransformer(new NormalHoursTransformer());
		mapping.readOnly();
		descriptor.addMapping(mapping);
		
		XMLTransformationMapping mapping2 = new XMLTransformationMapping();
		mapping2.setAttributeName("normalHours2");
		NormalHours2Transformer transformer = new NormalHours2Transformer();
		mapping2.setAttributeTransformer(transformer);
		mapping2.addFieldTransformer("normal-hours/start-time/text()", transformer);
		mapping2.addFieldTransformer("normal-hours/end-time/text()", transformer);
		descriptor.addMapping(mapping2);
		
		this.addDescriptor(descriptor);
	}
}