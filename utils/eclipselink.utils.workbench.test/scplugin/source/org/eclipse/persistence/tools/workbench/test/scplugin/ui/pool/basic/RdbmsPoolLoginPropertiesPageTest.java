/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.scplugin.ui.pool.basic;

import javax.swing.JComponent;

import org.eclipse.persistence.tools.workbench.test.scplugin.ui.login.RdbmsLoginPaneTest;
import org.eclipse.persistence.tools.workbench.test.scplugin.ui.pool.AbstractPoolPanelTest;

import org.eclipse.persistence.tools.workbench.scplugin.model.adapter.ConnectionPoolAdapter;
import org.eclipse.persistence.tools.workbench.scplugin.model.adapter.DatabaseLoginAdapter;
import org.eclipse.persistence.tools.workbench.scplugin.ui.pool.basic.RdbmsPoolLoginPropertiesPage;



/**
 * @version 10.0.3
 * @author Pascal Filion
 */
public class RdbmsPoolLoginPropertiesPageTest extends AbstractPoolPanelTest
{
	public RdbmsPoolLoginPropertiesPageTest(String name)
	{
		super(name);
	}

	public static void main(String[] args) throws Exception
	{
		new RdbmsPoolLoginPropertiesPageTest(null).execute(args);
	}

	protected void _testSubPaneRdbmsLoginPaneTest() throws Throwable
	{
		ConnectionPoolAdapter pool = (ConnectionPoolAdapter) getSelection();
		runTestCase(new RdbmsLoginPaneTest(this, getNodeHolder(), (DatabaseLoginAdapter) pool.getLogin()));
	}

	protected JComponent buildPane() throws Exception
	{
		return buildPage(RdbmsPoolLoginPropertiesPage.class, getNodeHolder());
	}

	protected String windowTitle()
	{
		return "Connection Pool Login Tab Test";
	}
}