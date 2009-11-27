/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.platform.database.oracle;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.logging.SessionLog;

/**
 * <p><b>Purpose:</b>
 * Supports usage of certain Oracle JDBC specific APIs.
 */
public class Oracle10Platform extends Oracle9Platform  {
    
    /**
     * INTERNAL:
     * Indicate whether app. server should unwrap connection
     * to use lob locator.
     * No need to unwrap connection because 
     * writeLob method doesn't use oracle proprietary classes.
     */
    public boolean isNativeConnectionRequiredForLobLocator() {
        return false;
    }
    
    /**
     * INTERNAL:
     * Write LOB value - Oracle 10 deprecates some methods used in the superclass
     */
    @Override
    public void writeLOB(DatabaseField field, Object value, ResultSet resultSet, AbstractSession session) throws SQLException {
        if (isBlob(field.getType())) {
            java.sql.Blob blob = (java.sql.Blob)resultSet.getObject(field.getNameDelimited(this));
            blob.setBytes(1, (byte[])value);
            //impose the locallization
            session.log(SessionLog.FINEST, SessionLog.SQL, "write_BLOB", new Long(blob.length()), field.getNameDelimited(this));
        } else if (isClob(field.getType())) {
            java.sql.Clob clob = (java.sql.Clob)resultSet.getObject(field.getNameDelimited(this));
            clob.setString(1, (String)value);
            //impose the locallization
            session.log(SessionLog.FINEST, SessionLog.SQL, "write_CLOB", new Long(clob.length()), field.getNameDelimited(this));
        } else {
            //do nothing for now, open to BFILE or NCLOB types
        }
    }
}
