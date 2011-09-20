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
 *    2.3.1
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.typemappinginfo.object.prefixes;

import javax.xml.bind.annotation.XmlType;

@XmlType(name="employee", namespace="someuri")
public class Employee {

    public String name;

    public String id;

    public boolean equals(Object obj) {
        return name.equals(((Employee)obj).name) && id.equals(((Employee)obj).id);
    }
}
