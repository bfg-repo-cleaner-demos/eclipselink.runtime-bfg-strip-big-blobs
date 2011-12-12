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
 *     Blaise Doughan - 2.4 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.listofobjects;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.eclipse.persistence.jaxb.JAXBContextFactory;

import junit.framework.TestCase;

public class NullSetsMapTestCases extends TestCase {

    public void testMarshal() throws JAXBException {
        JAXBContext jc = JAXBContextFactory.createContext(new Class[] {NullSetsMapRoot.class}, null);

        NullSetsMapRoot nsmr = new NullSetsMapRoot();
        nsmr.setNullSetsMap(new NullSetsMap());

        Marshaller marshaller = jc.createMarshaller();
        marshaller.marshal(nsmr, new StringWriter());
    }
}