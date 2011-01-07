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
 *     dmccann - August 6/2009 - 2.0 - Initial implementation     
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlcustomizer;

import org.eclipse.persistence.config.DescriptorCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;

/**
 * DescriptorCustomizer implementation for testing external metadata functionality.
 * 
 * @see DescriptorCustomizer
 */
public class MyEmployeeCustomizer implements DescriptorCustomizer {

    public void customize(ClassDescriptor descriptor) throws Exception {
        XMLDirectMapping firstNameMapping = (XMLDirectMapping) descriptor.getMappingForAttributeName("firstName");
        XMLField fnxField = (XMLField) firstNameMapping.getField();
        fnxField.setXPath("first-name/text()");
        
        XMLDirectMapping lastNameMapping  = (XMLDirectMapping) descriptor.getMappingForAttributeName("lastName" );
        XMLField lnxField = (XMLField) lastNameMapping.getField();
        lnxField.setXPath("last-name/text()");
    }
    
}