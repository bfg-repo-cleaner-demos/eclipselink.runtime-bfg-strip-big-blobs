/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.mappingsplugin.ui.project.xml.login;

import java.awt.Component;

import javax.swing.BorderFactory;

import org.eclipse.persistence.tools.workbench.framework.context.WorkbenchContextHolder;
import org.eclipse.persistence.tools.workbench.uitools.app.PropertyValueModel;


public class EisLoginPropertiesPropertiesPage extends AbstractLoginPropertiesPage
{
	/**
	 * Creates a new <code>LoginPropertiesPropertiesPage</code>.
	 *
	 * @param nodeHolder The holder of {@link LoginAdapter}
	 */
	public EisLoginPropertiesPropertiesPage(PropertyValueModel nodeHolder, WorkbenchContextHolder contextHolder)
	{
		super(nodeHolder, contextHolder);
	}

	/**
	 * Initializes the layout of this pane.
	 *
	 * @return The container with all its widgets
	 */
	protected Component buildPage()
	{
		PropertyPane propertyPane = new PropertyPane(getSelectionHolder(), getWorkbenchContextHolder());
		propertyPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		addHelpTopicId(propertyPane, "session.login.properties");
		return propertyPane;
	}
}