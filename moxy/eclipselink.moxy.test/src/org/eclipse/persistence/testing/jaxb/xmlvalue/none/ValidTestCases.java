/*******************************************************************************
 * Copyright (c) 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Blaise Doughan - 2.3 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.xmlvalue.none;

import java.io.InputStream;
import java.util.ArrayList;

import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class ValidTestCases extends JAXBTestCases {

    private static final String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/xmlvalue/none/input.xml";

    public ValidTestCases(String name) throws Exception {
        super(name);
        setClasses(new Class[] {ValidChild.class});
        setControlDocument(XML_RESOURCE);
    }

    @Override
    protected ValidChild getControlObject() {
        ValidChild child = new ValidChild();
        child.setParentAttributeProperty("PARENT");
        child.setChildProperty("CHILD");
        return child;
    }

}