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
*     bdoughan - Jan 27/2009 - 1.1 - Initial implementation
******************************************************************************/
package org.eclipse.persistence.testing.sdo.helper.jaxbhelper.containment;

import java.util.Collection;

public class Root {
    public Child childProperty;
    public Collection<Child> childCollectionProperty;

    public Child getChildProperty() {
        return childProperty;
    }

    public void setChildProperty(Child childProperty) {
        this.childProperty = childProperty;
    }

    public Collection<Child> getChildCollectionProperty() {
        return childCollectionProperty;
    }

    public void setChildCollectionProperty(Collection<Child> childCollectionProperty) {
        this.childCollectionProperty = childCollectionProperty;
    }

}
