/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.aggregate;

import java.util.*;
import org.eclipse.persistence.testing.framework.*;
import org.eclipse.persistence.sessions.*;
import org.eclipse.persistence.testing.framework.WriteObjectTest;
import org.eclipse.persistence.testing.models.aggregate.Language;
import org.eclipse.persistence.testing.models.aggregate.Responsibility;
import org.eclipse.persistence.testing.models.aggregate.Computer;
import org.eclipse.persistence.testing.models.aggregate.Employee;
import org.eclipse.persistence.testing.models.aggregate.ProjectDescription;

/**
 * This type was generated by a SmartGuide.
 * @author Load Builder
 */
public class MergingUnitOfWorkTest extends WriteObjectTest {
    public UnitOfWork unitOfWork1;
    public UnitOfWork unitOfWork2;
    public Employee workingCopy1;
    public Employee workingCopy2;
    public Employee mergedCopy;

    /**
     * MergingUnitOfWorkTest constructor comment.
     * @param originalObject java.lang.Object
     */
    public MergingUnitOfWorkTest(Object originalObject) {
        super(originalObject);
        setDescription("This suite tests merging of objects from two UOWs in the aggregate model.");
    }

    protected void changeWorkingCopy1() {
        ((Employee)this.workingCopy1).setFirstName("Kevin");
        ProjectDescription projectDescription = (ProjectDescription)((Employee)this.workingCopy1).getProjectDescription();
        projectDescription.setDescription("Quality Assurance project");
        ((Vector)projectDescription.getResponsibilities().getValue()).removeElement(((Vector)projectDescription.getResponsibilities().getValue()).firstElement());
        ((Vector)projectDescription.getResponsibilities().getValue()).addElement(Responsibility.example1(this.workingCopy1));
        ((Vector)projectDescription.getResponsibilities().getValue()).addElement(Responsibility.example2(this.workingCopy1));

        ((Vector)projectDescription.getLanguages().getValue()).removeElement(((Vector)projectDescription.getLanguages().getValue()).firstElement());
        ((Vector)projectDescription.getLanguages().getValue()).addElement(Language.example1());
        ((Vector)projectDescription.getLanguages().getValue()).addElement(Language.example2());
        ((Computer)projectDescription.getComputer().getValue()).setDescription("Commodore 64");
    }

    protected void deepMergeWorkingCopies() {
        // Everything should have changed
        this.unitOfWork2 = getSession().acquireUnitOfWork();
        this.workingCopy2 = (Employee)this.unitOfWork2.registerObject(this.originalObject);

        {
            this.mergedCopy = (Employee)this.unitOfWork2.deepMergeClone(workingCopy1);
            ProjectDescription projectDescription = this.mergedCopy.getProjectDescription();
            Vector responsibilities = (Vector)projectDescription.getResponsibilities().getValue();
            Vector languages = (Vector)projectDescription.getLanguages().getValue();
            Computer computer = (Computer)projectDescription.getComputer().getValue();

            if (this.mergedCopy.getFirstName() != "Kevin") {
                throw new TestErrorException("Deep Merge did not merge changes in Employee.");
            }
            if (projectDescription.getDescription() != "Quality Assurance project") {
                throw new TestErrorException("Deep Merge did not merge changes in ProjectDescription (Aggregate mapping).");
            }
            if (responsibilities.size() != ((Vector)this.workingCopy1.getProjectDescription().getResponsibilities().getValue()).size()) {
                throw new TestErrorException("Deep Merge did not merge changes in Responsibility (private 1:M).");
            }
            if (languages.size() != ((Vector)this.workingCopy1.getProjectDescription().getLanguages().getValue()).size()) {
                throw new TestErrorException("Deep Merge did not merge changes in Language (public M:M).");
            }
            if (computer.getDescription() != "Commodore 64") {
                throw new TestErrorException("Deep Merge did not merge changes in Computer (private 1:1).");
            }
        }

        {
            this.mergedCopy = (Employee)this.unitOfWork2.deepRevertObject(workingCopy2);
            ProjectDescription projectDescription = this.mergedCopy.getProjectDescription();
            Vector responsibilities = (Vector)projectDescription.getResponsibilities().getValue();
            Vector languages = (Vector)projectDescription.getLanguages().getValue();
            Computer computer = (Computer)projectDescription.getComputer().getValue();

            if (this.mergedCopy.getFirstName() == "Kevin") {
                throw new TestErrorException("Deep revert did not merge changes in Employee.");
            }
            if (projectDescription.getDescription() == "Quality Assurance project") {
                throw new TestErrorException("Deep revert did not merge changes in ProjectDescription (Aggregate mapping).");
            }
            if (responsibilities.size() == ((Vector)this.workingCopy1.getProjectDescription().getResponsibilities().getValue()).size()) {
                throw new TestErrorException("Deep revert did not merge changes in Responsibility (private 1:M).");
            }
            if (languages.size() == ((Vector)this.workingCopy1.getProjectDescription().getLanguages().getValue()).size()) {
                throw new TestErrorException("Deep revert did not merge changes in Language (public M:M).");
            }
            if (computer.getDescription() == "Commodore 64") {
                throw new TestErrorException("Deep revert did not merge changes in Computer (private 1:1).");
            }
        }

        {
            this.unitOfWork2.deepMergeClone(workingCopy1);
            this.mergedCopy = (Employee)this.unitOfWork2.shallowRevertObject(workingCopy2);
            ProjectDescription projectDescription = this.mergedCopy.getProjectDescription();
            Vector responsibilities = (Vector)projectDescription.getResponsibilities().getValue();
            Vector languages = (Vector)projectDescription.getLanguages().getValue();
            Computer computer = (Computer)projectDescription.getComputer().getValue();

            if (this.mergedCopy.getFirstName() == "Kevin") {
                throw new TestErrorException("Shallow revert did not merge changes in Employee.");
            }
            if (projectDescription.getDescription() == "Quality Assurance project") {
                throw new TestErrorException("Shallow revert did not merge changes in ProjectDescription (Aggregate mapping).");
            }
            if (responsibilities.size() == ((Vector)this.workingCopy1.getProjectDescription().getResponsibilities().getValue()).size()) {
                throw new TestErrorException("Shallow revert did not merge changes in Responsibility (private 1:M).");
            }
            if (languages.size() == ((Vector)this.workingCopy1.getProjectDescription().getLanguages().getValue()).size()) {
                throw new TestErrorException("Shallow revert did not merge changes in Language (public M:M).");
            }
            if (computer.getDescription() != "Commodore 64") {
                throw new TestErrorException("Shallow revert did not merge changes in Computer (private 1:1).");
            }
        }

        {
            this.unitOfWork2.deepMergeClone(workingCopy1);
            this.mergedCopy = (Employee)this.unitOfWork2.revertObject(workingCopy2);
            ProjectDescription projectDescription = this.mergedCopy.getProjectDescription();
            Vector responsibilities = (Vector)projectDescription.getResponsibilities().getValue();
            Vector languages = (Vector)projectDescription.getLanguages().getValue();
            Computer computer = (Computer)projectDescription.getComputer().getValue();

            if (this.mergedCopy.getFirstName() == "Kevin") {
                throw new TestErrorException("Revert did not merge changes in Employee.");
            }
            if (projectDescription.getDescription() == "Quality Assurance project") {
                throw new TestErrorException("Revert did not merge changes in ProjectDescription (Aggregate mapping).");
            }
            if (responsibilities.size() == ((Vector)this.workingCopy1.getProjectDescription().getResponsibilities().getValue()).size()) {
                throw new TestErrorException("Revert did not merge changes in Responsibility (private 1:M).");
            }
            if (languages.size() == ((Vector)this.workingCopy1.getProjectDescription().getLanguages().getValue()).size()) {
                throw new TestErrorException("Revert did not merge changes in Language (public M:M).");
            }
            if (computer.getDescription() == "Commodore 64") {
                throw new TestErrorException("Revert did not merge changes in Computer (private 1:1).");
            }
        }
    }

    protected void setup() {
        super.setup();

        this.unitOfWork1 = getSession().acquireUnitOfWork();
        this.workingCopy1 = (Employee)this.unitOfWork1.registerObject(this.originalObject);

        changeWorkingCopy1();
        standardMergeWorkingCopies();
        shallowMergeWorkingCopies();
        deepMergeWorkingCopies();
    }

    protected void shallowMergeWorkingCopies() {
        // Only Employee.firstName should have changed
        this.unitOfWork2 = getSession().acquireUnitOfWork();
        this.workingCopy2 = (Employee)this.unitOfWork2.registerObject(this.originalObject);
        this.mergedCopy = (Employee)this.unitOfWork2.shallowMergeClone(workingCopy1);

        ProjectDescription projectDescription = this.mergedCopy.getProjectDescription();
        Vector responsibilities = (Vector)projectDescription.getResponsibilities().getValue();
        Vector languages = (Vector)projectDescription.getLanguages().getValue();
        Computer computer = (Computer)projectDescription.getComputer().getValue();

        if (this.mergedCopy.getFirstName() != "Kevin") {
            throw new TestErrorException("Shallow Merge did not merge changes in Employee.");
        }

        if (projectDescription.getDescription() != "Quality Assurance project") {
            throw new TestErrorException("Shallow Merge merged changes in ProjectDescription (Aggregate mapping) but shouldn't have.");
        }

        if (responsibilities.size() == ((Vector)this.workingCopy1.getProjectDescription().getResponsibilities().getValue()).size()) {
            throw new TestErrorException("Shallow Merge merged changes in Responsibility (private 1:M) but shouldn't have.");
        }

        if (languages.size() == ((Vector)this.workingCopy1.getProjectDescription().getLanguages().getValue()).size()) {
            throw new TestErrorException("Shallow Merge merged changes in Language (public M:M) but shouldn't have.");
        }

        if (computer.getDescription() == "Commodore 64") {
            throw new TestErrorException("Shallow Merge merged changes in Computer (private 1:1) but shouldn't have.");
        }
    }

    protected void standardMergeWorkingCopies() {
        // Everything except languages should have changed
        this.unitOfWork2 = getSession().acquireUnitOfWork();
        this.workingCopy2 = (Employee)this.unitOfWork2.registerObject(this.originalObject);
        this.mergedCopy = (Employee)this.unitOfWork2.mergeClone(workingCopy1);

        ProjectDescription projectDescription = this.mergedCopy.getProjectDescription();
        Vector responsibilities = (Vector)projectDescription.getResponsibilities().getValue();
        Vector languages = (Vector)projectDescription.getLanguages().getValue();
        Computer computer = (Computer)projectDescription.getComputer().getValue();

        if (this.mergedCopy.getFirstName() != "Kevin") {
            throw new TestErrorException("Standard Merge did not merge changes in Employee.");
        }
        if (projectDescription.getDescription() != "Quality Assurance project") {
            throw new TestErrorException("Standard Merge did not merge changes in ProjectDescription (Aggregate mapping).");
        }
        if (responsibilities.size() != ((Vector)this.workingCopy1.getProjectDescription().getResponsibilities().getValue()).size()) {
            throw new TestErrorException("Standard Merge did not merge changes in Responsibility (private 1:M).");
        }
        if (languages.size() == ((Vector)this.workingCopy1.getProjectDescription().getLanguages().getValue()).size()) {
            throw new TestErrorException("Standard Merge merged changes in Language (public M:M) but shouldn't have.");
        }
        if (computer.getDescription() != "Commodore 64") {
            throw new TestErrorException("Standard Merge did not merge changes in Computer (private 1:1).");
        }
    }

    protected void test() {
        return;
    }

    protected void verify() {
        return;
    }
}