/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.scplugin.ui.session.clustering;

// JDK
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.eclipse.persistence.tools.workbench.framework.context.WorkbenchContextHolder;
import org.eclipse.persistence.tools.workbench.framework.resources.ResourceRepository;
import org.eclipse.persistence.tools.workbench.framework.ui.view.ScrollablePropertiesPage;
import org.eclipse.persistence.tools.workbench.framework.uitools.SwingTools;
import org.eclipse.persistence.tools.workbench.scplugin.model.adapter.SessionAdapter;
import org.eclipse.persistence.tools.workbench.scplugin.ui.tools.BooleanCellRendererAdapter;
import org.eclipse.persistence.tools.workbench.uitools.ComponentVisibilityEnabler;
import org.eclipse.persistence.tools.workbench.uitools.SwitcherPanel;
import org.eclipse.persistence.tools.workbench.uitools.app.CollectionAspectAdapter;
import org.eclipse.persistence.tools.workbench.uitools.app.CollectionValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.PropertyAspectAdapter;
import org.eclipse.persistence.tools.workbench.uitools.app.PropertyValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.TransformationPropertyValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.swing.CheckBoxModelAdapter;
import org.eclipse.persistence.tools.workbench.uitools.app.swing.ComboBoxModelAdapter;
import org.eclipse.persistence.tools.workbench.uitools.cell.AdaptableListCellRenderer;
import org.eclipse.persistence.tools.workbench.uitools.cell.CellRendererAdapter;
import org.eclipse.persistence.tools.workbench.utility.Transformer;
import org.eclipse.persistence.tools.workbench.utility.iterators.ArrayIterator;

// Mapping Workbench

/**
 * This page shows the Clustering combo box with three possible selections.
 * However, to have Cache Synchronization available, the property had to be in
 * sessions.xml when it was read.
 * <p>
 * By changing the Clustering type, the panel underneith will be changed to
 * reflect the information for the new choice.
 * <p>
 * Here the layout:<pre>
 * _________________________________________
 * |                                       |
 * | x Enable Clustering                   |
 * |               _______________________ |  _________________________
 * |   Clustering: |                   |v| |<-| Remote Command        |
 * |               ŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻ |  | Cache Synchronization |
 * |   ----------------------------------- |  ŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻ
 * |   |                                 | |
 * |   | {@link RemoteCommandManagerPane}        | |
 * |   |  or                             | |
 * |   | {@link CacheSynchronizationManagerPane} | |
 * |   |                                 | |
 * |   ----------------------------------- |
 * |                                       |
 * ŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻŻ</pre>
 *
 * Note: The Clustering combo box is only shown if the edited session supports
 * Cache Synchronization.
 *
 * @version 10.1.3
 * @author Pascal Filion
 */
public final class SessionClusteringPropertiesPage extends ScrollablePropertiesPage
{
	/**
	 * Creates a new <code>SessionClusteringPropertiesPage</code>.
	 *
	 * @param nodeHolder The holder of {@link SessionNode}
	 */
	public SessionClusteringPropertiesPage(PropertyValueModel nodeHolder, WorkbenchContextHolder contextHolder)
	{
		super(nodeHolder, contextHolder);
	}

	/**
	 * Creates the <code>ButtonModel</code> responsible to handle enabled state
	 * of Use Clustering Service check box.
	 * 
	 * @return A new <code>CheckBoxModelAdapter</code>
	 */
	private ButtonModel buildClusteringCheckBoxAdapter()
	{
		return new CheckBoxModelAdapter(buildClusteringCheckBoxHolder(), false)
		{
			protected void booleanChanged(PropertyChangeEvent e)
			{
				// Transform the values to Boolean values since the model passed a
				// Clustering type (RemoteCommandManager or CacheSynchronization)
				SessionAdapter adapter = (SessionAdapter) selection();

				if (adapter != null)
				{
					Boolean newValue = Boolean.valueOf(!adapter.hasNoClusteringService());
					Boolean oldValue = Boolean.valueOf(!newValue.booleanValue());
					e = new PropertyChangeEvent(e.getSource(), e.getPropertyName(), oldValue, newValue);
				}

				super.booleanChanged(e);
			}
		};
	}

	/**
	 * Creates the <code>PropertyValueModel</code> responsible to handle the
	 * Use Clustering Service property. The value desired by the
	 * <code>ButtonModel</code> is a <code>Boolean</code> and the model simply
	 * needs to be updated to use a Clustering service or not.
	 *
	 * @return A new <code>PropertyValueModel</code>
	 */
	private PropertyValueModel buildClusteringCheckBoxHolder()
	{
		String[] propertyNames = new String[]
		{
			SessionAdapter.REMOTE_COMMAND_MANAGER_CONFIG_PROPERTY,
			SessionAdapter.CACHE_SYNCHRONIZATION_MANAGER_CONFIG_PROPERTY
		};

		return new PropertyAspectAdapter(getSelectionHolder(), propertyNames)
		{
			protected Object getValueFromSubject()
			{
				SessionAdapter adapter = (SessionAdapter) subject;
				return Boolean.valueOf(!adapter.hasNoClusteringService());
			}

			protected void setValueOnSubject(Object value)
			{
				SessionAdapter adapter = (SessionAdapter) subject;
				boolean boolValue = Boolean.TRUE.equals(value);

				if (boolValue && adapter.hasNoClusteringService())
				{
					adapter.setClusteringToRemoteCommandManager();
				}
				else if (!boolValue)
				{
					adapter.setClusteringToNothing();
				}
			}
		};
	}

	/**
	 * Creates the <code>CollectionValueModel</code> containing the actual items
	 * to be shown in the Clustering combo box.
	 *
	 * @return The <code>CollectionValueModel</code> containing the items
	 */
	private CollectionValueModel buildClusteringCollectionHolder()
	{
		return new CollectionAspectAdapter(getSelectionHolder(), null)
		{
			protected Iterator getValueFromSubject()
			{
				return new ArrayIterator(new Object[] { Boolean.TRUE, Boolean.FALSE });
			}
		};
	}

	/**
	 * Creates the <code>ComboBoxModel</code> that keeps the selected item in the
	 * combo box in sync with the value in the model and vice versa.
	 *
	 * @return The model showing two choices: "Remote Command (True)" and "Cache
	 * Synchronization (False)"
	 */
	private ComboBoxModel buildClusteringComboBoxAdapter()
	{
		return new ComboBoxModelAdapter(buildClusteringCollectionHolder(),
												  buildClusteringTypeSelectionHolder());
	}

	/**
	 * Creates the decorator responsible to format the <code>Boolean</code>
	 * values in the Clustering combo box.
	 * 
	 * @return {@link SessionClusteringPropertiesPage.BooleanLabelDecorator}
	 */
	private CellRendererAdapter buildClusteringLabelDecorator()
	{
		ResourceRepository resourceRepository = resourceRepository();

		return new BooleanCellRendererAdapter(resourceRepository.getString("REMOTE_COMMAND"),
													resourceRepository.getString("CACHE_SYNCHRONIZATION"));
	}

	/**
	 * Creates the <code>SwitcherPanel</code> ...
	 *
	 * @return A new <code>SwitcherPanel</code>
	 */
	private SwitcherPanel buildClusteringSwitcherPanel()
	{
		return new SwitcherPanel(buildClusteringTypeHolder(),
										 buildClusteringTypeTransformer());
	}

	/**
	 * Creates a new <code>ComponentVisibilityEnabler</code> that is responsible
	 * to keep the visible state of the <code>Component</code> in sync with the
	 * type of clustering used.
	 *
	 * @param clusteringTypeWidgets The widgets used to change the Clustering Type
	 * @return A new <code>ComponentVisibilityEnabler</code>
	 */
	private ComponentVisibilityEnabler buildClusteringTypeComponentVisibilityUpdater(Component clusteringTypeWidgets)
	{
		return new ComponentVisibilityEnabler(buildClusteringTypeVisibilityHolder(), Collections.singleton(clusteringTypeWidgets));
	}

	/**
	 * Creates the <code>PropertyValueModel</code> responsible to handle the
	 * Clustering Type property.
	 *
	 * @return A new <code>PropertyValueModel</code>
	 */
	private PropertyValueModel buildClusteringTypeHolder()
	{
		String[] propertyNames = new String[]
		{
			SessionAdapter.REMOTE_COMMAND_MANAGER_CONFIG_PROPERTY,
			SessionAdapter.CACHE_SYNCHRONIZATION_MANAGER_CONFIG_PROPERTY
		};

		return new PropertyAspectAdapter(getSelectionHolder(), propertyNames)
		{
			protected Object getValueFromSubject()
			{
				SessionAdapter session = (SessionAdapter) subject;

				if (session.hasRemoteCommandManager())
					return session.getRemoteCommandManager();

				if (session.hasCacheSynchronizationManager())
					return session.getCacheSynchronizationManager();

				return null;
			}
		};
	}

	/**
	 * Creates the <code>PropertyValueModel</code> responsible to listen to
	 * changes made to the type of clustering to be used, which is either Remote
	 * Command Manager or Cache Synchronization.
	 *
	 * @return A new <code>PropertyValueModel</code>
	 */
	private PropertyValueModel buildClusteringTypeSelectionHolder()
	{
		String[] propertyNames = new String[]
		{
			SessionAdapter.REMOTE_COMMAND_MANAGER_CONFIG_PROPERTY,
			SessionAdapter.CACHE_SYNCHRONIZATION_MANAGER_CONFIG_PROPERTY
		};

		return new PropertyAspectAdapter(getSelectionHolder(), propertyNames)
		{
			protected Object getValueFromSubject()
			{
				SessionAdapter adapter = (SessionAdapter) subject;

				if (adapter.hasNoClusteringService())
					return null;

				return Boolean.valueOf(adapter.hasRemoteCommandManager());
			}

			protected void setValueOnSubject(Object value)
			{
				SessionAdapter adapter = (SessionAdapter) subject;

				if (Boolean.TRUE.equals(value))
				{
					adapter.setClusteringToRemoteCommandManager();
				}
				else if (Boolean.FALSE.equals(value))
				{
					adapter.setClusteringToCacheSynchronizationManager();
				}
			}
		};
	}

	/**
	 * Creates the <code>Transformer</code> responsible to convert the Clustering
	 * type into the corresponding <code>Component</code>.
	 *
	 * @return A new <code>Transformer</code>
	 */
	private Transformer buildClusteringTypeTransformer()
	{
		// Create the choices used to convert an object to a JComponent
		final Object[] items = new Object[]
		{
			new RemoteCommandManagerChoice(),
			new CacheSynchronizationManagerChoice(),
		};

		return new Transformer()
		{
			public Object transform(Object value)
			{
				SessionAdapter session = (SessionAdapter) selection();

				if ((value == null) || (session == null))
					return null;

				Transformer choice;

				if (value == session.getCacheSynchronizationManager())
					choice = (Transformer) items[1];
				else
					choice = (Transformer) items[0];

				return choice.transform(value);
			}
		};
	}

	/**
	 * Creates the <code>PropertyValueModel</code> responsible to handle the
	 * boolean holder used by the <code>ComponentVisibilityEnabler</code>.
	 *
	 * @return A new <code>PropertyValueModel</code>
	 */
	private PropertyValueModel buildClusteringTypeVisibilityHolder()
	{
		String[] propertyNames = new String[]
		{
			SessionAdapter.REMOTE_COMMAND_MANAGER_CONFIG_PROPERTY,
			SessionAdapter.CACHE_SYNCHRONIZATION_MANAGER_CONFIG_PROPERTY
		};

		PropertyAspectAdapter adapter = new PropertyAspectAdapter(getSelectionHolder(), propertyNames)
		{
			protected Object getValueFromSubject()
			{
				SessionAdapter session = (SessionAdapter) subject;

				if (session.hasCacheSynchronizationManager())
					return session.getCacheSynchronizationManager();

				if (session.hasRemoteCommandManager())
					return session.getRemoteCommandManager();

				return null;
			}
		};

		return new TransformationPropertyValueModel(adapter)
		{
			protected void valueChanged(PropertyChangeEvent e)
			{
				Boolean newValue = (Boolean) this.transform(e.getNewValue());
				Object oldValue = Boolean.valueOf(!newValue.booleanValue());
				this.firePropertyChanged(VALUE, oldValue, newValue);
			}

			protected Object transform(Object value)
			{
				SessionAdapter session = (SessionAdapter) selection();

				if ((session == null) ||
					 !session.hasCacheSynchronizationManager() &&
					 !session.hasRemoteCommandManager())
				{
					return Boolean.FALSE;
				}

				Boolean alwaysShown = Boolean.valueOf(System.getProperty("csm", "false"));
				return Boolean.valueOf(session.isCacheSynchronizationManagerAllowed() || alwaysShown.booleanValue());
			}
		};
	}

	/**
	 * Initializes the layout of the Clustering sub-panel.
	 *
	 * @return The container with all its widgets
	 */
	private JPanel buildInternalPage()
	{
		GridBagConstraints constraints = new GridBagConstraints();

		// Create the container
		JPanel panel = new JPanel(new GridBagLayout());

		// Create Clustering label
		JComponent clusteringWidgets = buildLabeledComboBox
		(
			"CLUSTERING_CLUSTERING_COMBO_BOX",
			buildClusteringComboBoxAdapter(),
			new AdaptableListCellRenderer(buildClusteringLabelDecorator())
		);
		clusteringWidgets.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

		constraints.gridx       = 0;
		constraints.gridy       = 0;
		constraints.gridwidth   = 1;
		constraints.gridheight  = 1;
		constraints.weightx     = 1;
		constraints.weighty     = 0;
		constraints.fill        = GridBagConstraints.HORIZONTAL;
		constraints.anchor      = GridBagConstraints.CENTER;
		constraints.insets      = new Insets(0, 0, 0, 0);

		panel.add(clusteringWidgets, constraints);
		buildClusteringTypeComponentVisibilityUpdater(clusteringWidgets);
		addHelpTopicId(clusteringWidgets, "session.clustering");

		// Create the sub-panel container
		SwitcherPanel clusteringPaneContainer = buildClusteringSwitcherPanel();

		constraints.gridx       = 0;
		constraints.gridy       = 1;
		constraints.gridwidth   = 1;
		constraints.gridheight  = 1;
		constraints.weightx     = 1;
		constraints.weighty     = 1;
		constraints.fill        = GridBagConstraints.BOTH;
		constraints.anchor      = GridBagConstraints.CENTER;
		constraints.insets      = new Insets(0, 0, 0, 0);

		panel.add(clusteringPaneContainer, constraints);

		return panel;
	}

	/**
	 * Initializes the layout of this pane.
	 *
	 * @return The container with all its widgets
	 */
	protected Component buildPage()
	{

		GridBagConstraints constraints = new GridBagConstraints();
		int offset = SwingTools.checkBoxIconWidth();

		// Create the container
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		// Use Clustering Service check box
		JCheckBox clusteringCheckBox = buildCheckBox("CLUSTERING_CLUSTERING_CHECK_BOX",
																	buildClusteringCheckBoxAdapter());

		constraints.gridx       = 0;
		constraints.gridy       = 0;
		constraints.gridwidth   = 1;
		constraints.gridheight  = 1;
		constraints.weightx     = 0;
		constraints.weighty     = 0;
		constraints.fill        = GridBagConstraints.NONE;
		constraints.anchor      = GridBagConstraints.LINE_START;
		constraints.insets      = new Insets(0, 0, 5, 0);

		panel.add(clusteringCheckBox, constraints);
		addHelpTopicId(clusteringCheckBox, "session.clustering");

		// Create internal pane
		JPanel internalPane = buildInternalPage();

		constraints.gridx       = 0;
		constraints.gridy       = 1;
		constraints.gridwidth   = 1;
		constraints.gridheight  = 1;
		constraints.weightx     = 1;
		constraints.weighty     = 1;
		constraints.fill        = GridBagConstraints.BOTH;
		constraints.anchor      = GridBagConstraints.CENTER;
		constraints.insets      = new Insets(0, offset, 0, 0);

		panel.add(internalPane, constraints);

		addHelpTopicId(this, "session.clustering");
		return panel;
	}

	/**
	 * This is one of the choice for Clustering Service that is used by
	 * {@link #buildClusteringTypeHolder()}. It is available if and only if the
	 * the session that is been edited contains a Cache Synchronization Manager.
	 * It is meant to support backward compatibility.
	 */
	private class CacheSynchronizationManagerChoice implements Transformer
	{
		/**
		 * The pane containing Cache Synchronization specific information.
		 */
		private CacheSynchronizationManagerPane pane;

		/**
		 * Creates the subject holder where it is two children down from the value
		 * contained in the given node holder.
		 *
		 * @return The <code>PropertyValueModel</code> containing the subject
		 * holder required for {@link CacheSynchronizationManagerPane}
		 */
		private PropertyValueModel buildChacheSynchronizationManagerHolder()
		{
			return new PropertyAspectAdapter(getSelectionHolder(), SessionAdapter.CACHE_SYNCHRONIZATION_MANAGER_CONFIG_PROPERTY)
			{
				protected Object getValueFromSubject()
				{
					SessionAdapter adapter = (SessionAdapter) subject;
					return adapter.getCacheSynchronizationManager();
				}
			};
		}

		/**
		 * Based on the given object, requests the associated component.
		 *
		 * @param value The value used to retrieve a pane
		 * @return {@link CacheSynchronizationManagerPane}
		 */
		public Object transform(Object value)
		{
			if (this.pane == null)
			{
				this.pane = new CacheSynchronizationManagerPane
				(
					buildChacheSynchronizationManagerHolder(),
					getApplicationContext()
				);

				addPaneForAlignment(this.pane);
			}

			return this.pane;
		}
	}

	/**
	 * This is one of the choice for Clustering Service that is used by
	 * {@link #buildClusteringTypeHolder()}.
	 */
	private class RemoteCommandManagerChoice implements Transformer
	{
		/**
		 * The pane containing Remote Command Manager specific information.
		 */
		private RemoteCommandManagerPane pane;

		/**
		 * Creates the <code>PropertyValueModel</code> that will be the subject
		 * holder for this pane. The subject will be a {@link RemoteCommandManagerAdapter}.
		 *
		 * @return A new <code>PropertyValueModel</code> listening for change of
		 * Remote Command Manager.
		 */
		private PropertyValueModel buildRemoteCommandManagerHolder()
		{
			return new PropertyAspectAdapter(getSelectionHolder(), SessionAdapter.REMOTE_COMMAND_MANAGER_CONFIG_PROPERTY)
			{
				protected Object getValueFromSubject()
				{
					SessionAdapter adapter = (SessionAdapter) subject;
					return adapter.getRemoteCommandManager();
				}
			};
		}

		/**
		 * Based on the given object, requests the associated component.
		 *
		 * @param value The value used to retrieve a pane
		 * @return {@link RemoteCommandManagerPane}
		 */
		public Object transform(Object value)
		{
			if (this.pane == null)
			{
				this.pane = new RemoteCommandManagerPane
				(
					buildRemoteCommandManagerHolder(),
					getWorkbenchContextHolder()
				);

				addPaneForAlignment(this.pane);
			}

			return this.pane;
		}
	}
}