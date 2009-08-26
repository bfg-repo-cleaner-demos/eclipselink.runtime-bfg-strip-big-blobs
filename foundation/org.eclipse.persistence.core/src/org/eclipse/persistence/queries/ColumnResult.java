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
package org.eclipse.persistence.queries;

import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.sessions.DatabaseRecord;

/**
 * <p><b>Purpose</b>:
 * Concrete class to represent the ColumnResult structure as defined by
 * the EJB 3.0 Persistence specification.  This class is a subcomponent of the 
 * EntityResult
 * 
 * @see EntityResult
 * @author Gordon Yorke
 * @since TopLink Java Essentials
 */

public class ColumnResult extends SQLResult{
    
    /** Stores the Columns name from the result set */
    protected String columnName;
    
    public ColumnResult(String column){
        this.columnName = column;
        if (this.columnName == null){
            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("null_value_for_column_result"));
        }
    }
    
    public String getColumnName(){
        return this.columnName;
    }
    
    /**
     * INTERNAL:
     * This method is a convience method for extracting values from Results
     */
    public Object getValueFromRecord(DatabaseRecord record, ResultSetMappingQuery query){
        return record.get(this.columnName);
    }
    
    public boolean isColumnResult(){
        return true;
    }
    
}
