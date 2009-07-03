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
package org.eclipse.persistence.testing.tests.workbenchintegration;

import java.util.Enumeration;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.internal.indirection.TransparentIndirectionPolicy;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.ForeignReferenceMapping;
import org.eclipse.persistence.testing.models.employee.domain.Employee;


/** This class has been modified as per instructions from Tom Ware and Development
 *  the test(), verify() are all inherited from the parent class
 *  We pass in the TopLink project and the string we are looking for and the superclass does the verification and testing
 */
public class TransparentIndirectionPolicyInstanceTest extends ProjectClassGeneratorResultFileTest {
    ClassDescriptor descriptorToModify;
    DatabaseMapping mappingToModify;

    public TransparentIndirectionPolicyInstanceTest() {
        super(new org.eclipse.persistence.testing.models.employee.relational.EmployeeProject(), 
              "useTransparentCollection();");
        setDescription("Test addForeignReferenceMappingLines method ->  policy instanceof TransparentIndirectionPolicy is true");
    }

    protected void setup() {
        getSession().getIdentityMapAccessor().initializeAllIdentityMaps();

        descriptorToModify = (ClassDescriptor)project.getDescriptors().get(Employee.class);
        for (Enumeration mappingsEnum = (descriptorToModify.getMappings()).elements(); 
             mappingsEnum.hasMoreElements(); ) {
            mappingToModify = (DatabaseMapping)mappingsEnum.nextElement();

            if (mappingToModify.isForeignReferenceMapping()) {
                ((ForeignReferenceMapping)mappingToModify).setIndirectionPolicy(new TransparentIndirectionPolicy());
            }
        }
    }
}
