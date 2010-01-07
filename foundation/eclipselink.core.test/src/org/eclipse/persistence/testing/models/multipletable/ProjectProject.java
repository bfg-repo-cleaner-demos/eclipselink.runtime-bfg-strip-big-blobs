/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
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
package org.eclipse.persistence.testing.models.multipletable;

import java.util.*;
import org.eclipse.persistence.descriptors.RelationalDescriptor;

/**
 * TopLink generated Project class. 
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
public class ProjectProject extends org.eclipse.persistence.sessions.Project {

    /**
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
    public ProjectProject() {
        applyPROJECT();
        applyLOGIN();
        buildBudgetDescriptor();
        buildBusinessProjectDescriptor();
        buildLargeBusinessProjectDescriptor();
    }

    /**
 * TopLink generated method. 
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
    protected void applyLOGIN() {
    }

    /**
 * TopLink generated method. 
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
    protected void applyPROJECT() {
        setName("SimpleMultipleTableSystem");
    }

    /**
 * TopLink generated method. 
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
    protected void buildBudgetDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.multipletable.Budget.class);
        Vector vector = new Vector();
        vector.addElement("BUDGET");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("BUDGET.ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setSequenceNumberName("BUDG_SEQ");
        descriptor.setSequenceNumberFieldName("ID");
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("amount");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("BUDGET.AMNT");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("currency");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("BUDGET.CUR");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("id");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setFieldName("BUDGET.ID");
        descriptor.addMapping(directtofieldmapping2);
        addDescriptor(descriptor);
    }

    /**
 * TopLink generated method. 
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
    protected void buildBusinessProjectDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.multipletable.BusinessProject.class);
        Vector vector = new Vector();
        vector.addElement("PROJ");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("PROJ.PROJ_ID");

        // SECTION: PROPERTIES
        descriptor.setSequenceNumberName("PROJ_SEQ");
        descriptor.useVersionLocking("VERSION");
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setSequenceNumberFieldName("PROJ_ID");
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);
        descriptor.getInheritancePolicy().setShouldReadSubclasses(true);
        descriptor.getInheritancePolicy().setClassIndicatorFieldName("PROJ_TYPE");
        descriptor.getInheritancePolicy().setShouldUseClassNameAsIndicator(false);
        descriptor.getInheritancePolicy().addClassIndicator(org.eclipse.persistence.testing.models.multipletable.LargeBusinessProject.class, "L");

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("description");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("PROJ.DESCRIP");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping1 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping1.setAttributeName("id");
        directtofieldmapping1.setIsReadOnly(false);
        directtofieldmapping1.setFieldName("PROJ.PROJ_ID");
        descriptor.addMapping(directtofieldmapping1);

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping2 = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping2.setAttributeName("name");
        directtofieldmapping2.setIsReadOnly(false);
        directtofieldmapping2.setFieldName("PROJ.PROJ_NAME");
        descriptor.addMapping(directtofieldmapping2);
        addDescriptor(descriptor);
    }

    /**
 * TopLink generated method. 
 * <b>WARNING</b>: This code was generated by an automated tool.
 * Any changes will be lost when the code is re-generated
 */
    protected void buildLargeBusinessProjectDescriptor() {
        RelationalDescriptor descriptor = new RelationalDescriptor();

        // SECTION: DESCRIPTOR
        descriptor.setJavaClass(org.eclipse.persistence.testing.models.multipletable.LargeBusinessProject.class);
        descriptor.getInheritancePolicy().setParentClass(org.eclipse.persistence.testing.models.multipletable.BusinessProject.class);
        Vector vector = new Vector();
        vector.addElement("LPROJ");
        descriptor.setTableNames(vector);
        descriptor.addPrimaryKeyFieldName("LPROJ.PROJ_ID");

        // SECTION: PROPERTIES
        descriptor.setIdentityMapClass(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        descriptor.setExistenceChecking("Check cache");
        descriptor.setIdentityMapSize(100);
        descriptor.getInheritancePolicy().setShouldReadSubclasses(false);

        // SECTION: COPY POLICY
        descriptor.createCopyPolicy("constructor");

        // SECTION: INSTANTIATION POLICY
        descriptor.createInstantiationPolicy("constructor");

        // SECTION: DIRECTTOFIELDMAPPING
        org.eclipse.persistence.mappings.DirectToFieldMapping directtofieldmapping = new org.eclipse.persistence.mappings.DirectToFieldMapping();
        directtofieldmapping.setAttributeName("milestoneVersion");
        directtofieldmapping.setIsReadOnly(false);
        directtofieldmapping.setFieldName("LPROJ.MILESTONE");
        descriptor.addMapping(directtofieldmapping);

        // SECTION: ONETOONEMAPPING
        org.eclipse.persistence.mappings.OneToOneMapping onetoonemapping = new org.eclipse.persistence.mappings.OneToOneMapping();
        onetoonemapping.setAttributeName("budget");
        onetoonemapping.setIsReadOnly(false);
        onetoonemapping.setUsesIndirection(false);
        onetoonemapping.setReferenceClass(org.eclipse.persistence.testing.models.multipletable.Budget.class);
        onetoonemapping.setIsPrivateOwned(true);
        onetoonemapping.addForeignKeyFieldName("LPROJ.BUDGET_ID", "BUDGET.ID");
        descriptor.addMapping(onetoonemapping);
        addDescriptor(descriptor);
    }
}
