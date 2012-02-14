/*******************************************************************************
 * Copyright (c) 2012 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Blaise Doughan - 2.3.3 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.jaxbcontext.empty.jaxbindex;

import javax.xml.bind.JAXBException;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

import junit.framework.TestCase;

public class JAXBIndexTestCases extends TestCase {

    public void testCreateJAXBContextWithEmptyJAXBIndex() throws JAXBException {
        JAXBContextFactory.createContext("org.eclipse.persistence.testing.jaxb.jaxbcontext.empty.jaxbindex", this.getClass().getClassLoader());
    }

}