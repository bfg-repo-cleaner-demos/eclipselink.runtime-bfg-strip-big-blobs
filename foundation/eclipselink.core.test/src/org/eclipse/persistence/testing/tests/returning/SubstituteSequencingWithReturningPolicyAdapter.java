/*******************************************************************************
 * Copyright (c) 1998, 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.eclipse.persistence.testing.tests.returning;

import java.util.*;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.sequencing.NativeSequence;
import org.eclipse.persistence.sessions.*;
import org.eclipse.persistence.descriptors.ReturningPolicy;
import org.eclipse.persistence.queries.DataModifyQuery;
import org.eclipse.persistence.tools.schemaframework.SequenceObjectDefinition;
import org.eclipse.persistence.testing.framework.*;
import org.eclipse.persistence.tools.schemaframework.SchemaManager;

/**
 * This adapter creates INSERT trigger for the original model's tables
 * that return a generated sequence value, and modifies the descriptors
 * to use a ReturningPolicy to assign this sequence value.
 */
public class SubstituteSequencingWithReturningPolicyAdapter implements ProjectAndDatabaseAdapter {

    public SubstituteSequencingWithReturningPolicyAdapter() {
        this(true);
    }

    /**
     * If isReadOnly parameter is false, then the PK value will not be generated by the trigger if a non-null
     * value is passed in insert, otherwise the passed value is ignored.
     */
    public SubstituteSequencingWithReturningPolicyAdapter(boolean isReadOnly) {
        this(isReadOnly, true);
    }

    /**
     * useExistingSequenceName parameter indicates whether to use the sequence name used by the project
     * or just create sequence name using table name.
     * For instance, for Employee class mapped to EMPLOYEE table and with sequenceNumberName==EMP_SEQ,
     * useExistingSequenceName==true causes EMP_SEQ to be used
     * as an Oracle sequence name, otherwise EMPLOYEE_SEQ name is used (tableName +"_SEQ").
     */
    public SubstituteSequencingWithReturningPolicyAdapter(boolean isReadOnly, boolean useExistingSequenceName) {
        this.isReadOnly = isReadOnly;
        tableToField = new Hashtable();
        if (useExistingSequenceName) {
            tableToSequence = new Hashtable();
        }
    }

    protected Hashtable tableToField;
    protected Hashtable tableToSequence;
    protected boolean isReadOnly;

    public boolean usesExistingSequenceNames() {
        return tableToSequence != null;
    }

    public boolean isOriginalSetupRequired() {
        return true;
    }

    public void updateProject(Project project, Session session) {
        Iterator it = project.getDescriptors().values().iterator();
        while (it.hasNext()) {
            ClassDescriptor desc = (ClassDescriptor)it.next();
            if (desc.isAggregateDescriptor()) {
                continue;
            }
            String sequenceName = desc.getSequenceNumberName();
            String fieldName = desc.getSequenceNumberFieldName();
            if (fieldName != null && sequenceName != null) {
                String tableName = desc.getTableName();
                int indexOfDot = fieldName.indexOf('.');
                if (indexOfDot != -1) {
                    fieldName = fieldName.substring(indexOfDot + 1);
                }
                tableToField.put(tableName, fieldName);
                if (tableToSequence != null) {
                    tableToSequence.put(tableName, sequenceName);
                }
                // remove sequencing from descriptor
                desc.setSequenceNumberFieldName(null);
                desc.setSequenceNumberName(null);
                // add ReturningPolicy
                String fieldQualifiedName = tableName + '.' + fieldName;
                desc.setReturningPolicy(new ReturningPolicy());
                if (isReadOnly) {
                    desc.getReturningPolicy().addFieldForInsertReturnOnly(fieldQualifiedName);
                } else {
                    desc.getReturningPolicy().addFieldForInsert(fieldQualifiedName);
                }
            }
        }
    }

    public void updateDatabase(Session session) {
        try {
            createSequences(session);
            replaceOrCreateTriggers(session);
        } finally {
            clear();
        }
    }

    public void createSequences(Session session) {
        if (!session.getPlatform().supportsSequenceObjects()) {
            throw new TestWarningException("Requires database platform that supports sequence objects (like Oracle) - they will be used by triggers");
        }
        SchemaManager schemaManager = new SchemaManager((DatabaseSession)session);
        Hashtable sequenceNameToDefinition = new Hashtable();
        Enumeration tableNames = tableToField.keys();
        while (tableNames.hasMoreElements()) {
            String tableName = (String)tableNames.nextElement();
            String sequenceName = getSequenceNameFromTableName(tableName);
            if (!sequenceNameToDefinition.containsKey(sequenceName)) {
                SequenceObjectDefinition definition = new SequenceObjectDefinition(new NativeSequence(sequenceName, 1, false));
                sequenceNameToDefinition.put(sequenceName, definition);
                schemaManager.createObject(definition);
            }
        }
    }

    public void replaceOrCreateTriggers(Session session) {
        if (!session.getPlatform().isOracle()) {
            throw new TestWarningException("Currently supports Oracle platform only");
        }
        Enumeration tableNames = tableToField.keys();
        while (tableNames.hasMoreElements()) {
            String tableName = (String)tableNames.nextElement();
            String fieldName = (String)tableToField.get(tableName);
            String sequenceName = getSequenceNameFromTableName(tableName);
            String triggerName = getTriggerNameFromTableName(tableName);
            String strCommand = "CREATE OR REPLACE TRIGGER " + triggerName + " BEFORE INSERT ON " + tableName + " FOR EACH ROW ";
            String strBegin = "BEGIN\n";
            String strIf = "  IF (:new." + fieldName + " IS NULL) OR (:new." + fieldName + " = 0) THEN\n  ";
            String strSeq = "  SELECT " + sequenceName + ".NEXTVAL INTO :new." + fieldName + " FROM DUAL;\n";
            String strEndIf = "  END IF;\n";
            String strEnd = "END;";
            String str;
            if (isReadOnly) {
                str = strCommand + strBegin + strSeq + strEnd;
            } else {
                str = strCommand + strBegin + strIf + strSeq + strEndIf + strEnd;
            }
            execute(session, str, true);
        }
    }

    public Map getTableToField() {
        return this.tableToField;
    }
    
    public Map getTableToSequence() {
        return this.tableToSequence;
    }
    
    protected String getSequenceNameFromTableName(String tableName) {
        String sequenceName;
        if (tableToSequence != null) {
            sequenceName = (String)tableToSequence.get(tableName);
        } else {
            sequenceName = tableName + "_SEQ";
        }
        return sequenceName;
    }

    protected String getTriggerNameFromTableName(String tableName) {
        return tableName + "_TRIGGER";
    }

    protected void execute(Session session, String str, boolean shouldThrowException) {
        try {
            DataModifyQuery query = new DataModifyQuery(str);
            query.setShouldBindAllParameters(false);
            session.executeQuery(query);
        } catch (Exception e) {
            if (shouldThrowException) {
                throw new TestErrorException("FAILED: " + str, e);
            }
        }
    }

    protected void clear() {
        tableToField.clear();
        if (tableToSequence != null) {
            tableToSequence.clear();
        }
    }
}
