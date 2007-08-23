/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.models.events;

import org.eclipse.persistence.mappings.*;
import org.eclipse.persistence.descriptors.RelationalDescriptor;
import org.eclipse.persistence.sessions.*;

/**
 * This class was generated by the TopLink project class generator.
 * It stores the meta-data (descriptors) that define the TopLink mappings.
 * ## TopLink - 9.0.3 (Build 423) ##
 * @see org.eclipse.persistence.sessions.factories.ProjectClassGenerator
 */
public class AboutToInsertProject extends org.eclipse.persistence.sessions.Project {
    public AboutToInsertProject() {
        setName("AboutToInsertProject");
        applyLogin();

        addDescriptor(buildAboutToInsertMultiTableObjectRelationalDataTypeDescriptor());
        addDescriptor(buildAboutToInsertSingleTableObjectRelationalDataTypeDescriptor());
    }

    public void applyLogin() {
        DatabaseLogin login = new DatabaseLogin();
        setLogin(login);
    }

    public RelationalDescriptor buildAboutToInsertMultiTableObjectRelationalDataTypeDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.events.AboutToInsertMultiTableObject.class);
        descriptor.addTableName("AboutToInsertMulti1");
        descriptor.addTableName("AboutToInsertMulti2");
        descriptor.addPrimaryKeyFieldName("AboutToInsertMulti1.ID");

        // RelationalDescriptor properties.
        descriptor.useSoftCacheWeakIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setAlias("AboutToInsertMultiTableObject");

        descriptor.setSequenceNumberFieldName("ID");
        descriptor.setSequenceNumberName("MULTI_SEQ");

        // Query manager.
        descriptor.getQueryManager().checkCacheForDoesExist();

        //Named Queries
        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setGetMethodName("getId");
        idMapping.setSetMethodName("setId");
        idMapping.setFieldName("AboutToInsertMulti1.ID");
        descriptor.addMapping(idMapping);

        descriptor.getEventManager().addListener(new AboutToInsertEventAdapter("AboutToInsertMulti1", "EXTRA_NUMBER"));

        return descriptor;
    }

    public RelationalDescriptor buildAboutToInsertSingleTableObjectRelationalDataTypeDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.events.AboutToInsertSingleTableObject.class);
        descriptor.addTableName("AboutToInsertSingle");
        descriptor.addPrimaryKeyFieldName("AboutToInsertSingle.ID");

        // RelationalDescriptor properties.
        descriptor.useSoftCacheWeakIdentityMap();
        descriptor.setIdentityMapSize(100);
        descriptor.setAlias("AboutToInsertSingleTableObject");

        descriptor.setSequenceNumberFieldName("ID");
        descriptor.setSequenceNumberName("SINGLE_SEQ");

        // Query manager.
        descriptor.getQueryManager().checkCacheForDoesExist();

        //Named Queries
        // Event manager.
        // Mappings.
        DirectToFieldMapping idMapping = new DirectToFieldMapping();
        idMapping.setAttributeName("id");
        idMapping.setGetMethodName("getId");
        idMapping.setSetMethodName("setId");
        idMapping.setFieldName("AboutToInsertSingle.ID");
        descriptor.addMapping(idMapping);

        descriptor.getEventManager().addListener(new AboutToInsertEventAdapter("AboutToInsertSingle", "EXTRA_NUMBER"));

        return descriptor;
    }
}