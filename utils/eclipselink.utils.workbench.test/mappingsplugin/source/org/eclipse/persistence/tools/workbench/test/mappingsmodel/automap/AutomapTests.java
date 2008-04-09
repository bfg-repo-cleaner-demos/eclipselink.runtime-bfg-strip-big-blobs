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
package org.eclipse.persistence.tools.workbench.test.mappingsmodel.automap;

import org.eclipse.persistence.tools.workbench.test.models.projects.EmployeeProject;

import org.eclipse.persistence.tools.workbench.mappingsmodel.project.MWProject;
import junit.framework.TestCase;

public class AutomapTests extends TestCase
{
	public AutomapTests(String name)
	{
		super(name);
	}

	private void startTest(MWProject mwProject,
								  AutomapVerifier verifier) throws Exception
	{
		new AutomapProject(mwProject).startTest(verifier);
	}

	public void testEmployeeProject() throws Exception
	{
		startTest(new EmployeeProject().getProject(),
					 new EmployeeProjectVerifier());
	}
}