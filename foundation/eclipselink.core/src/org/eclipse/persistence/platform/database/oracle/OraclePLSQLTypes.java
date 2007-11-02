/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/

package org.eclipse.persistence.platform.database.oracle;

// Javse imports
import static java.sql.Types.OTHER;

// EclipseLink imports
import org.eclipse.persistence.internal.helper.SimpleDatabaseType;
import static org.eclipse.persistence.platform.database.jdbc.JDBCTypes.NUMERIC_TYPE;

/**
 * <b>PUBLIC</b>: Oracle PL/SQL types
 * @author  Mike Norman - michael.norman@oracle.com
 * @since  Oracle TopLink 11.x.x
 */
public enum OraclePLSQLTypes implements SimpleDatabaseType, OraclePLSQLType {

    BinaryInteger("BINARY_INTEGER"),
    Dec("DEC") ,
    Int("INT"),
    Natural("NATURAL"),
    NaturalN("NATURALN"),
    PLSQLBoolean("BOOLEAN") {
    },
    PLSQLInteger("PLS_INTEGER"),
    Positive("POSITIVE"),
    PositiveN("POSITIVEN"),
    SignType("SIGNTYPE"),
    ;


    private final String typeName;

    OraclePLSQLTypes(String typeName) {
        this.typeName = typeName;
    }

    public boolean isComplexDatabaseType() {
        return false;
    }
    public int getSqlCode() {
        return OTHER;
    }

    public int getConversionCode() {
        // widest compatible type java.sql.Types.NUMERIC <-> BigDecimal 
        return NUMERIC_TYPE.getConversionCode(); 
    }

    public String getTypeName() {
        return typeName;
    }

    public boolean isJDBCType() {
        return false;
    }
}
