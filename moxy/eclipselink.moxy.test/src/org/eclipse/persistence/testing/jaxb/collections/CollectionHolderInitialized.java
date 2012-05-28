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
 *     Denise Smith - 2.3.3 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.collections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CollectionHolderInitialized extends CollectionHolder{

    public static final List TEST_LIST = new ArrayList();
    public static final Map TEST_MAP = new HashMap();

    public CollectionHolderInitialized(){
        collection1 = new ArrayList();
        collection2 = new ArrayList();
        collection3 = new ArrayList();
        collection4 = new ArrayList();
        collection5 = new ArrayList();
        collection6 = new ArrayList();
        collection7 = new ArrayList();
        collection8 = new ArrayList();
        collection9 = new ArrayList();
        collection10 = TEST_MAP;
        collection11 = new ArrayList();
        collection12 = new ArrayList();
        collection13 = new ArrayList();
        collection14 = new ArrayList();
    }

}