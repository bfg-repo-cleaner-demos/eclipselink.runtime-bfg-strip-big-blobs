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
package org.eclipse.persistence.testing.oxm.readonly;

import java.util.Vector;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.oxm.*;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.oxm.mappings.XMLCompositeObjectMapping;


public class OneCompositeObjectMappingProject extends Project 
{
	public OneCompositeObjectMappingProject() 
	{
		super();
		addEmployeeDescriptor();
		addAddressDescriptor();
	}
	
	public void addEmployeeDescriptor() 
	{
		XMLDescriptor descriptor = new XMLDescriptor();
		descriptor.setJavaClass(Employee.class);
		descriptor.setDefaultRootElement("employee");
		
		XMLCompositeObjectMapping addr = new XMLCompositeObjectMapping();
		addr.setAttributeName("primaryAddress");
		addr.setXPath("primary-address");
		addr.setReferenceClass(Address.class);
		addr.readOnly();
		descriptor.addMapping(addr);
		
		this.addDescriptor(descriptor);
	}
	public void addAddressDescriptor() 
	{
		XMLDescriptor descriptor = new XMLDescriptor();
		descriptor.setJavaClass(Address.class);
		
		XMLDirectMapping street = new XMLDirectMapping();
		street.setAttributeName("street");
		street.setXPath("street/text()");
		descriptor.addMapping(street);
		
		this.addDescriptor(descriptor);
	}
}