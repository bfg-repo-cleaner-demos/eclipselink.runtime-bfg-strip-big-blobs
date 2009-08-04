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
package org.eclipse.persistence.platform.database.oracle.publisher.sqlrefl;

//javase imports
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//EclipseLink imports
import org.eclipse.persistence.platform.database.oracle.publisher.MethodFilter;
import org.eclipse.persistence.platform.database.oracle.publisher.Util;
import org.eclipse.persistence.platform.database.oracle.publisher.viewcache.FieldInfo;
import org.eclipse.persistence.platform.database.oracle.publisher.viewcache.MethodInfo;
import org.eclipse.persistence.platform.database.oracle.publisher.viewcache.ParamInfo;
import org.eclipse.persistence.platform.database.oracle.publisher.viewcache.ResultInfo;
import org.eclipse.persistence.platform.database.oracle.publisher.viewcache.ViewRow;
import org.eclipse.persistence.platform.database.oracle.publisher.visit.PublisherVisitor;
import org.eclipse.persistence.platform.database.oracle.publisher.visit.PublisherWalker;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.ALL_ARGUMENTS;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.ARGUMENT_NAME;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.DATA_LEVEL;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.NOT_NULL;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.OBJECT_NAME;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.OVERLOAD;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.OWNER;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.PACKAGE_NAME;
import static org.eclipse.persistence.platform.database.oracle.publisher.Util.POSITION;

public class SqlToplevelType extends SqlTypeWithMethods {

    public boolean isPackage() {
        return true;
    }

    public boolean isTopLevel() {
        return true;
    }

    public SqlToplevelType(SqlName sqlName, SqlType parentType, MethodFilter methodFilter,
        SqlReflector reflector) throws SQLException {
        super(sqlName, OracleTypes.PACKAGE, true, parentType, methodFilter, reflector);
    }

    protected List<FieldInfo> getFieldInfo() {
        return null;
    }

    protected MethodInfo[] getMethodInfo(String schema, String name) throws SQLException {
        /*
         * POSITION of Nth argument is N SEQUENCE of Nth argument is >= N POSITION of function
         * result is 0 SEQUENCE of function result is 1 Special case: If there are no arguments or
         * function results, a row appears anyway, with POSITION=1, SEQUENCE=0.
         *
         * All of which helps to explain the rather strange query below. #sql smi = {SELECT
         * ALL_ARGUMENTS.OBJECT_NAME AS METHOD_NAME, ALL_ARGUMENTS.OVERLOAD AS METHOD_NO, 'PUBLIC'
         * AS METHOD_TYPE, NVL(MAX(DECODE(SEQUENCE, 0, 0, POSITION)), 0) AS PARAMETERS,
         * NVL(MAX(1-POSITION), 0) AS RESULTS FROM ALL_ARGUMENTS, ALL_OBJECTS WHERE
         * ALL_ARGUMENTS.OWNER = :schema AND ALL_OBJECTS.OWNER = :schema AND
         * ALL_ARGUMENTS.OBJECT_NAME = ALL_OBJECTS.OBJECT_NAME AND ALL_ARGUMENTS.DATA_LEVEL = 0 AND
         * (ALL_OBJECTS.OBJECT_TYPE = 'PROCEDURE' OR ALL_OBJECTS.OBJECT_TYPE = 'FUNCTION') GROUP BY
         * ALL_ARGUMENTS.OBJECT_NAME, OVERLOAD};
         *
         * Iterator iter = getRows("ALL_OBJECTS", new String[]{Util.OWNER, "OBJECT_TYPE",
         * "OBJECT_TYPE"}, new String[]{schema, "PROCEDURE", "FUNCTION"});
         */
        Iterator<ViewRow> iter;
        List<String> names = new ArrayList<String>();
        List<Object> values = new ArrayList<Object>();
        names.add(Util.OWNER);
        values.add(schema);

        names.add(Util.PACKAGE_NAME);
        values.add(null);
        names.add(Util.DATA_LEVEL);
        values.add(new Integer(0));
        if (m_methodFilter != null) {
            List<String> methodNames = m_methodFilter.getMethodNames();
            if (methodNames != null) {
                for (int i = 0, len = methodNames.size(); i < len; i++) {
                    names.add(Util.OBJECT_NAME);
                    values.add(SqlName.dbifyName(methodNames.get(i), m_reflector));
                }
            }
        }
        iter = m_viewCache.getRows(ALL_ARGUMENTS, new String[0],
            (String[])names.toArray(new String[0]), values.toArray(new Object[0]), new String[0]);
        MethodInfo[] minfo = MethodInfo.groupBy(iter);
        return minfo;
    }

    protected ResultInfo getResultInfo(String schema, String name, String method, String methodNo)
        throws SQLException {
        Iterator<ViewRow> iter = null;
        if (methodNo == null) {
            iter = m_viewCache.getRows(ALL_ARGUMENTS, new String[0], new String[]{OWNER,
                PACKAGE_NAME, PACKAGE_NAME, OBJECT_NAME, DATA_LEVEL,
                POSITION}, new Object[]{schema, method, null, method, new Integer(0),
                new Integer(0)}, new String[0]);

        }
        else {
            iter = m_viewCache.getRows(ALL_ARGUMENTS, new String[0], new String[]{OWNER,
                PACKAGE_NAME, PACKAGE_NAME, OBJECT_NAME, OVERLOAD,
                DATA_LEVEL, POSITION}, new Object[]{schema, method, null, method,
                methodNo, new Integer(0), new Integer(0)}, new String[0]);
        }
        return ResultInfo.getResultInfo(iter);
    }

    protected ParamInfo[] getParamInfo(String schema, String name, String method, String methodNo)
        throws SQLException {
        Iterator<ViewRow> iter = null;
        if (methodNo == null) {
            iter = m_viewCache.getRows(ALL_ARGUMENTS, new String[0], new String[]{OWNER,
                PACKAGE_NAME, PACKAGE_NAME, OBJECT_NAME, DATA_LEVEL,
                ARGUMENT_NAME}, new Object[]{schema, method, null, method, new Integer(0),
                NOT_NULL}, new String[]{POSITION});

        }
        else {
            iter = m_viewCache.getRows(ALL_ARGUMENTS, new String[0], new String[]{OWNER,
                PACKAGE_NAME, PACKAGE_NAME, OBJECT_NAME, OVERLOAD,
                DATA_LEVEL, ARGUMENT_NAME}, new Object[]{schema, method, null, method,
                methodNo, new Integer(0), NOT_NULL}, new String[]{POSITION});
        }
        return ParamInfo.getParamInfo(iter);
    }

    public void accept(PublisherVisitor v) {
        ((PublisherWalker)v).visit(this);
    }
}