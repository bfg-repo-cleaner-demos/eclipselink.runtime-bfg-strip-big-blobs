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
package org.eclipse.persistence.testing.jaxb.innerclasses;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="root")
public class UnmappedInnerClassRoot {

    public String name;

    @Override
    public boolean equals(Object obj) {
        if(null == obj || obj.getClass() != this.getClass()) {
            return false;
        }
        UnmappedInnerClassRoot test = (UnmappedInnerClassRoot) obj;
        if(null == name) {
            return null == test.name;
        }
        return name.equals(test.name);
    }

    public class Inner {
    }

}
