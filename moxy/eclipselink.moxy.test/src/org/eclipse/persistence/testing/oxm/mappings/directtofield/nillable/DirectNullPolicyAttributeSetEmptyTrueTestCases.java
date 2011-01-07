/*******************************************************************************
 * Copyright (c) 1998, 2011 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.oxm.mappings.directtofield.nillable;

import org.eclipse.persistence.oxm.mappings.nullpolicy.AbstractNullPolicy;
import org.eclipse.persistence.oxm.mappings.nullpolicy.NullPolicy;
import org.eclipse.persistence.oxm.mappings.nullpolicy.XMLNullRepresentationType;

import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.testing.oxm.mappings.XMLMappingTestCases;

public class DirectNullPolicyAttributeSetEmptyTrueTestCases extends XMLMappingTestCases {
	// TC  UC 6-2(marshal) and 5-1 to 5-4(unmarshal)
    private final static String XML_RESOURCE = //
    "org/eclipse/persistence/testing/oxm/mappings/directtofield/nillable/DirectNullPolicyAttributeSetEmptyTrue.xml";

    public DirectNullPolicyAttributeSetEmptyTrueTestCases(String name) throws Exception {
        super(name);
        setControlDocument(XML_RESOURCE);

        AbstractNullPolicy aNullPolicy = new NullPolicy();
    	// Alter unmarshal policy state
        ((NullPolicy)aNullPolicy).setSetPerformedForAbsentNode(true); // no effect
    	aNullPolicy.setNullRepresentedByEmptyNode(true); // no effect
    	aNullPolicy.setNullRepresentedByXsiNil(false);  // no effect
    	// Alter marshal policy state
    	aNullPolicy.setMarshalNullRepresentation(XMLNullRepresentationType.EMPTY_NODE);
        Project aProject = new DirectNodeNullPolicyProject(false);
        XMLDirectMapping aMapping = (XMLDirectMapping)aProject.getDescriptor(Employee.class)//
        .getMappingForAttributeName("firstName");
        aMapping.setNullPolicy(aNullPolicy);
        setProject(aProject);
    }

    protected Object getControlObject() {
        Employee anEmployee = new Employee();
        anEmployee.setId(123);
        anEmployee.setFirstName(null);
        anEmployee.setLastName("Doe");
        return anEmployee;
    }
    
/*	public void testObjectToXMLDocument() throws Exception {}
    public void testObjectToXMLStringWriter() throws Exception {}
    public void testObjectToContentHandler() throws Exception {}
    public void testXMLToObjectFromURL() throws Exception {}
    public void testUnmarshallerHandler() throws Exception {}
//    public void testXMLToObjectFromInputStream() throws Exception {}*/
    
}
