/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.mappings.anyattribute.withoutgroupingelement;

/**
 *  @version $Header: AnyAttributeNoAttributesTestCases.java 24-apr-2006.15:08:39 mmacivor Exp $
 *  @author  mmacivor
 *  @since   release specific (what release of product did this appear in)
 */

import java.util.HashMap;
import java.util.Vector;

import javax.xml.namespace.QName;

import org.eclipse.persistence.testing.oxm.mappings.XMLMappingTestCases;

public class AnyAttributeNoAttributesTestCases extends XMLMappingTestCases {
    public AnyAttributeNoAttributesTestCases(String name) throws Exception {
        super(name);
        setProject(new AnyAttributeWithoutGroupingElementProject());
        setControlDocument("org/eclipse/persistence/testing/oxm/mappings/anyattribute/withoutgroupingelement/no_attributes.xml");
    }
    
    public Object getControlObject() {
        Root root = new Root();
        HashMap any = new HashMap();
        root.setAny(any);
//        System.out.println(root);
        return root;
    }
}
