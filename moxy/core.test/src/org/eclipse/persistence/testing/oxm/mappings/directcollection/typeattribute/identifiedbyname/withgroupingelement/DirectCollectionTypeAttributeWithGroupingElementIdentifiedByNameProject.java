/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.mappings.directcollection.typeattribute.identifiedbyname.withgroupingelement;

import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.oxm.mappings.XMLCompositeDirectCollectionMapping;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.testing.oxm.mappings.directcollection.Employee;
import org.eclipse.persistence.internal.oxm.*;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLField;

public class DirectCollectionTypeAttributeWithGroupingElementIdentifiedByNameProject extends Project {
    public DirectCollectionTypeAttributeWithGroupingElementIdentifiedByNameProject() {
        addDescriptor(getEmployeeDescriptor());
    }

    private XMLDescriptor getEmployeeDescriptor() {
        XMLDescriptor descriptor = new XMLDescriptor();
        descriptor.setJavaClass(Employee.class);
        descriptor.setDefaultRootElement("employee");

        NamespaceResolver resolver = new NamespaceResolver();
        resolver.put(XMLConstants.SCHEMA_INSTANCE_PREFIX, XMLConstants.SCHEMA_INSTANCE_URL);
        resolver.put(XMLConstants.SCHEMA_PREFIX, XMLConstants.SCHEMA_URL);
        descriptor.setNamespaceResolver(resolver);

        XMLDirectMapping idMapping = new XMLDirectMapping();
        idMapping.setAttributeName("id");
        idMapping.setXPath("@id");
        descriptor.addMapping(idMapping);

        XMLCompositeDirectCollectionMapping responsibilitiesMapping = new XMLCompositeDirectCollectionMapping();
        responsibilitiesMapping.setAttributeName("responsibilities");
        XMLField field = new XMLField("responsibilities/list/responsibility/text()");
        field.setIsTypedTextField(true);
        responsibilitiesMapping.setField(field);
        descriptor.addMapping(responsibilitiesMapping);

        return descriptor;
    }
}