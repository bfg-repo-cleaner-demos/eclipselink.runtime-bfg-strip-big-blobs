/*******************************************************************************
 * Copyright (c) 1998-2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Mike Norman - from Proof-of-concept, become production code
 ******************************************************************************/
package org.eclipse.persistence.platform.database.oracle.publisher.viewcache;

import java.sql.ResultSet;

// Includes all the columns in ALL_TYPE_METHODS

public class AllObjects extends ViewRowFactory implements ViewRow {
    // Attributes
    public String objectName;
    public String objectType;

    public static int iOBJECT_NAME = -1;
    public static int iOBJECT_TYPE = -1;

    public AllObjects(ResultSet rset) throws java.sql.SQLException {
        super();
        if (iOBJECT_TYPE == -1) {
            iOBJECT_TYPE = rset.findColumn("OBJECT_TYPE");
            iOBJECT_NAME = rset.findColumn("OBJECT_NAME");
        }
        objectName = rset.getString(iOBJECT_NAME);
        objectType = rset.getString(iOBJECT_TYPE);
    }

    public String toString() {
        return objectName + "," + objectType;
    }

    public static String[] getProjectList() {
        return new String[]{"OBJECT_NAME", "OBJECT_TYPE"};
    }
}
