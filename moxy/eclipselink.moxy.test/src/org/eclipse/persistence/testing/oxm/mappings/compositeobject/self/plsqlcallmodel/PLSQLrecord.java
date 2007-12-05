package org.eclipse.persistence.testing.oxm.mappings.compositeobject.self.plsqlcallmodel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.eclipse.persistence.testing.oxm.mappings.compositeobject.self.plsqlcallmodel.PLSQLargument.IN;

public class PLSQLrecord implements ComplexDatabaseType, OraclePLSQLType {

    protected String recordName;
    protected String typeName;
    protected String compatibleType;
    protected List<PLSQLargument> fields = new ArrayList<PLSQLargument>();

    public PLSQLrecord() {
        super();
    }

    public boolean isComplexDatabaseType() {
        return true;
    }
    
    public boolean isJDBCType() {
        return false;
    }

    public String getRecordName() {
        return recordName;
    }
    public void setRecordName(String name) {
        this.recordName = name;
    }

    public String getTypeName() {
        return typeName;
    }
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    public void addField(String fieldName, DatabaseType databaseType, int precision, int scale) {
        fields.add(
            new PLSQLargument(fieldName, -1, IN, databaseType, precision, scale));
    }
    public void addField(String fieldName, DatabaseType databaseType, int length) {
        fields.add(new PLSQLargument(fieldName, -1, IN, databaseType, length));
    }

    public void addField(String fieldName, DatabaseType databaseType) {
        fields.add(new PLSQLargument(fieldName, databaseType));
    }
    
    public boolean equals(Object obj) {
        PLSQLrecord complexObj;
        try {
            complexObj = (PLSQLrecord) obj;
        } catch (Exception x) {
            return false;
        }
        if (!complexObj.recordName.equals(this.recordName)) {
            return false;
        }
        if (!complexObj.typeName.equals(this.typeName)) {
            return false;
        }
        if ((complexObj.compatibleType == null && this.compatibleType != null) ||(complexObj.compatibleType != null && this.compatibleType == null)) {
            return false;
        }
        if (complexObj.compatibleType != null && (!complexObj.compatibleType.equals(this.compatibleType))) {
            return false;
        }
        if (complexObj.fields == null) {
            return fields == null;
        }
        if (fields == null) {
            return false;
        }
        if (fields.size() != complexObj.fields.size()) {
            return false;
        }
        Iterator<PLSQLargument> fieldIt = fields.iterator();
        while (fieldIt.hasNext()) {
            PLSQLargument arg = fieldIt.next();
            if (!complexObj.fields.contains(arg)) {
                return false;
            }
        }
        return true;
    }        
}