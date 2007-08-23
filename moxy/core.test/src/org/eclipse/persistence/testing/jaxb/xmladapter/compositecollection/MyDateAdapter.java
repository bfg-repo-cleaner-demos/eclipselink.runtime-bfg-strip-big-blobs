/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.jaxb.xmladapter.compositecollection;

import java.util.Date;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public final class MyDateAdapter extends XmlAdapter<MyDateType, Date> {
    public MyDateType marshal(Date arg0) throws Exception {
        MyDateType dType = new MyDateType();
        dType.day = arg0.getDate();
        dType.month = arg0.getMonth();
        dType.year = arg0.getYear();
        return dType;
    }
    
    public Date unmarshal(MyDateType arg0) throws Exception {
        return new Date(arg0.year, arg0.month, arg0.day);
    }
}
