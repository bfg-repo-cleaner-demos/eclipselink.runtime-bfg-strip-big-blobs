/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
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

import java.util.*;
import org.eclipse.persistence.mappings.converters.*;
import org.eclipse.persistence.internal.helper.*;
import org.eclipse.persistence.exceptions.*;
import org.eclipse.persistence.internal.queries.*;
import org.eclipse.persistence.internal.sessions.AbstractRecord;

/**
 * <p><b>Purpose</b>:
 * Concrete class to perform a direct read.
 * <p>
 * <p><b>Responsibilities</b>:
 * Used in conjunction with DirectCollectionMapping.
 * This can be used to read a single column of data (i.e. one field).
 * A container (implementing Collection) of the data values is returned.
 *
 * @author Yvon Lavoie
 * @since TOPLink/Java 1.0
 */
public class DirectReadQuery extends DataReadQuery {

    /** Allows user defined conversion between the result value and the database value. */
    protected Converter valueConverter;

    /**
     * PUBLIC:
     * Initialize the state of the query.
     */
    public DirectReadQuery() {
        super();
    }

    /**
     * PUBLIC:
     * Initialize the query to use the specified SQL string.
     * Warning: Allowing an unverified SQL string to be passed into this 
	 * method makes your application vulnerable to SQL injection attacks. 
     */
    public DirectReadQuery(String sqlString) {
        super(sqlString);
    }

    /**
     * PUBLIC:
     * Initialize the query to use the specified call.
     */
    public DirectReadQuery(Call call) {
        super(call);
    }

    /**
     * PUBLIC:
     * Return the converter on the query.
     * A converter can be used to convert between the result value and database value.
     */
    public Converter getValueConverter() {
        return valueConverter;
    }

    /**
     * PUBLIC:
     * Set the converter on the query.
     * A converter can be used to convert between the result value and database value.
     */
    public void setValueConverter(Converter valueConverter) {
        this.valueConverter = valueConverter;
    }

    /**
     * INTERNAL:
     * Used by cursored stream.
     * Return the first field in the row.
     */
    public Object buildObject(AbstractRecord row) {
        Object value = row.get(row.getFields().firstElement());
        if (getValueConverter() != null) {
            value = getValueConverter().convertDataValueToObjectValue(value, session);
        }
        return value;
    }

    /**
     * INTERNAL:
     * The results are *not* in a cursor, build the collection.
     */
    public Object executeNonCursor() throws DatabaseException, QueryException {
        ContainerPolicy cp = getContainerPolicy();

        Vector rows = getQueryMechanism().executeSelect();
        Object result = cp.containerInstance(rows.size());
        DatabaseField resultDirectField = null;

        for (Enumeration stream = rows.elements(); stream.hasMoreElements();) {
        	AbstractRecord row = (AbstractRecord)stream.nextElement();
            if (resultDirectField == null) {
                resultDirectField = (DatabaseField)row.getFields().firstElement();
            }
            Object value = row.get(resultDirectField);
            if (getValueConverter() != null) {
                value = getValueConverter().convertDataValueToObjectValue(value, session);
            }
            cp.addInto(value, result, getSession());
        }
        return result;
    }
    
    /**
     * PUBLIC:
     * Return if this is a direct read query.
     */
    public boolean isDirectReadQuery() {
        return true;
    }
}
