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
package org.eclipse.persistence.tools.workbench.mappingsplugin.ui.query.relational;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.eclipse.persistence.tools.workbench.framework.context.ApplicationContext;
import org.eclipse.persistence.tools.workbench.framework.context.WorkbenchContextHolder;
import org.eclipse.persistence.tools.workbench.framework.ui.view.AbstractPanel;
import org.eclipse.persistence.tools.workbench.framework.uitools.SwingComponentFactory;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWAutoGeneratedQueryFormat;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWEJBQLQueryFormat;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWExpressionQueryFormat;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWQueryFormat;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWRelationalQuery;
import org.eclipse.persistence.tools.workbench.mappingsmodel.query.relational.MWSQLQueryFormat;
import org.eclipse.persistence.tools.workbench.mappingsplugin.MappingsPlugin;
import org.eclipse.persistence.tools.workbench.uitools.ComponentEnabler;
import org.eclipse.persistence.tools.workbench.uitools.app.CollectionValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.PropertyAspectAdapter;
import org.eclipse.persistence.tools.workbench.uitools.app.PropertyValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.ReadOnlyCollectionValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.SimplePropertyValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.ValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.swing.ComboBoxModelAdapter;
import org.eclipse.persistence.tools.workbench.uitools.cell.SimpleListCellRenderer;
import org.eclipse.persistence.tools.workbench.utility.TriStateBoolean;




/**
 * QueryFormatPanel is one of the nested tabs found on the NamedQueries tab of a descriptor.
 * The user chooses a query format(expression, SQL, or EJBQL) and the appropriate panel
 * is shown and populated.
 */
final class QuerySelectionCriteriaPanel 
	extends AbstractPanel
{
	private PropertyValueModel queryHolder;
	private PropertyValueModel relationalOptionsHolder;
	private PropertyValueModel queryFormatHolder;
	
	private AbstractQueryFormatPanel activeQueryFormatPanel;
	
	private AutoGeneratedQueryFormatPanel autoGeneratedQueryFormatPanel;
	private NonAutoGeneratedQueryFormatPanel nonAutoGeneratedQueryFormatPanel;

	private Map queryFormatPanelMap;
	
	private abstract class AbstractQueryFormatPanel extends AbstractPanel {
		protected AbstractQueryFormatPanel(ApplicationContext context) {
			super(context);
		}
		
	}
	
	private final class AutoGeneratedQueryFormatPanel extends AbstractQueryFormatPanel {
		public AutoGeneratedQueryFormatPanel(ApplicationContext context) {
			super(context);
			initialize();
		}
		
		protected void initialize() {
			setLayout(new BorderLayout());
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			JLabel autoGenCommentArea = buildLabel("AUTO_GENERATED_QUERY_COMMENT");
			autoGenCommentArea.setFocusable(true);
			autoGenCommentArea.setRequestFocusEnabled(true);
			add(autoGenCommentArea, BorderLayout.PAGE_START);
		}	
	}
	
	private final class NonAutoGeneratedQueryFormatPanel extends AbstractQueryFormatPanel {
		
		private PropertyValueModel queryFormatTypeHolder;
		
		
		private QueryFormatSubPanel activeQueryFormatSubPanel;
		
		private StringQueryFormatSubPanel stringQueryFormatPanel;
		private ExpressionQueryFormatSubPanel expressionPanel;

		private Map subQueryFormatPanelMap;


		public NonAutoGeneratedQueryFormatPanel(ApplicationContext context) {
			super(context);
			queryFormatHolder.addPropertyChangeListener(ValueModel.VALUE, buildQueryFormatListener());
			this.queryFormatTypeHolder = buildQueryFormatTypeAdapter();
			initialize();
			initializeMaps();
		}
		
		private void initializeMaps() {
			this.subQueryFormatPanelMap = new Hashtable(); 
					
			this.subQueryFormatPanelMap.put(MWExpressionQueryFormat.class, this.expressionPanel);
			this.subQueryFormatPanelMap.put(MWSQLQueryFormat.class, this.stringQueryFormatPanel);
			this.subQueryFormatPanelMap.put(MWEJBQLQueryFormat.class, this.stringQueryFormatPanel);
		}
		
		private void initialize()  {
			JPanel queryFormatTypePanel = initializeQueryFormatTypePanel();
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx      = 0;
			constraints.gridy      = 0;
			constraints.gridwidth  = 1;
			constraints.gridheight = 1;
			constraints.weightx    = 1;
			constraints.weighty    = 0;
			constraints.fill       = GridBagConstraints.HORIZONTAL;
			constraints.anchor     = GridBagConstraints.LINE_START;
			constraints.insets     = new Insets(5, 0, 0, 5);
			this.add(queryFormatTypePanel, constraints);
			this.stringQueryFormatPanel = new StringQueryFormatSubPanel(queryHolder, queryFormatHolder, QuerySelectionCriteriaPanel.this.getWorkbenchContextHolder());
			this.expressionPanel = new ExpressionQueryFormatSubPanel(queryHolder, queryFormatHolder, QuerySelectionCriteriaPanel.this.getWorkbenchContextHolder());
			setActiveQueryFormatSubPanel(this.stringQueryFormatPanel);
		}	
		private JPanel initializeQueryFormatTypePanel() {
			Collection components = new ArrayList();
			JPanel queryFormatTypeRadioButtonPanel = new JPanel(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();

            JLabel typeLabel = new JLabel(resourceRepository().getString("SELECTION_CRITERIA_TYPE_LABEL"));           
			components.add(typeLabel);
			constraints.gridx      = 0;
			constraints.gridy      = 0;
			constraints.gridwidth  = 1;
			constraints.gridheight = 1;
			constraints.weightx    = 0;
			constraints.weighty    = 0;
			constraints.fill       = GridBagConstraints.NONE;
			constraints.anchor     = GridBagConstraints.LINE_START;
			constraints.insets     = new Insets(0, 5, 0, 0);
			queryFormatTypeRadioButtonPanel.add(typeLabel, constraints);

            JComboBox comboBox = new JComboBox(buildSelectionCriteriaComboBoxModel());
            comboBox.setRenderer(buildSelectionCriteraComboBoxRenderer());

 			components.add(comboBox);
			constraints.gridx      = 1;
			constraints.gridy      = 0;
			constraints.gridwidth  = 1;
			constraints.gridheight = 1;
			constraints.weightx    = 1;
			constraints.weighty    = 0;
			constraints.fill       = GridBagConstraints.HORIZONTAL;
			constraints.anchor     = GridBagConstraints.LINE_START;
			constraints.insets     = new Insets(0, 5, 0, 0);
			queryFormatTypeRadioButtonPanel.add(comboBox, constraints);
	
			new ComponentEnabler(buildQueryComponentEnableBooleanHolder(), components);
			
			return queryFormatTypeRadioButtonPanel;
		}

		private PropertyChangeListener buildQueryFormatListener() {
			return new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					MWQueryFormat queryFormat = getQueryFormat();
					if (queryFormat != null) {
						setActiveQueryFormatSubPanel((QueryFormatSubPanel) subQueryFormatPanelMap.get(queryFormat.getClass()));
					}
					else {
						setActiveQueryFormatSubPanel((QueryFormatSubPanel) subQueryFormatPanelMap.get(MWSQLQueryFormat.class));
					}
				}
			};
		}


		private ComboBoxModel buildSelectionCriteriaComboBoxModel() {
		    return new ComboBoxModelAdapter(selectionCriteriaOptionsModel(), queryFormatTypeHolder);
        }
        
        private CollectionValueModel selectionCriteriaOptionsModel() {
            Collection options = new ArrayList();
            options.add(MWRelationalQuery.EXPRESSION_FORMAT);
            options.add(MWRelationalQuery.SQL_FORMAT);
            options.add(MWRelationalQuery.EJBQL_FORMAT);
            return new ReadOnlyCollectionValueModel(options);
        }
        private ListCellRenderer buildSelectionCriteraComboBoxRenderer() {
            return new SimpleListCellRenderer() {
                protected String buildText(Object value) {
                    if ((String) value == MWRelationalQuery.EXPRESSION_FORMAT) {
                        return resourceRepository().getString("EXPRESSION_OPTION");
                    }
                    else if ((String) value == MWRelationalQuery.SQL_FORMAT) {
                        return resourceRepository().getString("SQL_OPTION");
                    }
                    else {
                        return resourceRepository().getString("EJBQL_OPTION");
                    }
                }
            };
        }
		private PropertyValueModel buildQueryFormatTypeAdapter() {
			return new PropertyAspectAdapter(relationalOptionsHolder, MWRelationalQuery.QUERY_FORMAT_TYPE_PROPERTY) {
				protected Object getValueFromSubject() {
					return ((MWRelationalQuery) this.subject).getQueryFormatType();
				}
			
				protected void setValueOnSubject(Object value) {
					if (QuerySelectionCriteriaPanel.this.queryFormatCanChange()) {
						((MWRelationalQuery) this.subject).setQueryFormatType((String) value);
					}
				}
			};
		}
		
		private PropertyValueModel buildQueryComponentEnableBooleanHolder() {
			return new PropertyAspectAdapter(queryHolder) {
				protected Object buildValue() {
					return Boolean.valueOf(this.subject != null);
				}
			};
		}
		
				
		private void setActiveQueryFormatSubPanel(QueryFormatSubPanel newQueryFormatPanel) 
		{
			JPanel oldQueryFormatPanel = this.activeQueryFormatSubPanel;
			if (newQueryFormatPanel == oldQueryFormatPanel)
				return;
			this.activeQueryFormatSubPanel = newQueryFormatPanel;
			if (oldQueryFormatPanel != null) {
				remove(oldQueryFormatPanel);
			}
			if (this.activeQueryFormatSubPanel != null) {
				GridBagConstraints constraints = new GridBagConstraints();
				constraints.gridx      = 0;
				constraints.gridy      = 1;
				constraints.gridwidth  = 1;
				constraints.gridheight = 1;
				constraints.weightx    = 1;
				constraints.weighty    = 1;
				constraints.fill       = GridBagConstraints.BOTH;
				constraints.anchor     = GridBagConstraints.CENTER;
				constraints.insets     = new Insets(5, 5, 5, 5);
				add(this.activeQueryFormatSubPanel, constraints);
			}
			revalidate();
			repaint();
		}
	}
	
	QuerySelectionCriteriaPanel(PropertyValueModel queryHolder, WorkbenchContextHolder contextHolder) {
		super(contextHolder);
		this.queryHolder = queryHolder;
		this.relationalOptionsHolder = buildRelationalOptionsHolder();
		this.queryFormatHolder = buildQueryFormatHolder();
		this.queryFormatHolder.addPropertyChangeListener(ValueModel.VALUE, buildQueryFormatListener());
		initializeLayout();
		initializeMaps();
	}
	
	private PropertyValueModel buildQueryFormatHolder() {
		return new PropertyAspectAdapter(this.relationalOptionsHolder, MWRelationalQuery.QUERY_FORMAT_TYPE_PROPERTY) {
			protected Object getValueFromSubject() {
				return ((MWRelationalQuery) this.subject).getQueryFormat();
			}
		};
	}
	
	private PropertyValueModel buildRelationalOptionsHolder() {
		return new PropertyAspectAdapter(this.queryHolder) {
			protected Object getValueFromSubject() {
				return ((MWRelationalQuery) this.subject).getRelationalOptions();
			}
		};
	}
	
	private PropertyChangeListener buildQueryFormatListener() {
		return new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				MWQueryFormat queryFormat = getQueryFormat();
				if (queryFormat != null) {
					setActiveQueryFormatPanel((AbstractQueryFormatPanel) queryFormatPanelMap.get(queryFormat.getClass()));
				}
			}
		};
	}

	private void initializeLayout() 
	{
		this.autoGeneratedQueryFormatPanel = new AutoGeneratedQueryFormatPanel(getApplicationContext());
		this.nonAutoGeneratedQueryFormatPanel = new NonAutoGeneratedQueryFormatPanel(getApplicationContext());
		setActiveQueryFormatPanel(this.nonAutoGeneratedQueryFormatPanel);

		addHelpTopicId(this, helpTopicId());		
	}

	private void initializeMaps()
	{
		this.queryFormatPanelMap = new Hashtable();
		this.queryFormatPanelMap.put(MWAutoGeneratedQueryFormat.class, this.autoGeneratedQueryFormatPanel);
		this.queryFormatPanelMap.put(MWExpressionQueryFormat.class, this.nonAutoGeneratedQueryFormatPanel);
		this.queryFormatPanelMap.put(MWSQLQueryFormat.class, this.nonAutoGeneratedQueryFormatPanel);
		this.queryFormatPanelMap.put(MWEJBQLQueryFormat.class, this.nonAutoGeneratedQueryFormatPanel);
	}

	private String helpTopicId() {
		return "descriptor.queries.format";	
	}
	
	private void setActiveQueryFormatPanel(AbstractQueryFormatPanel newQueryFormatPanel) 
	{
		JPanel oldQueryFormatPanel = this.activeQueryFormatPanel;
		if (newQueryFormatPanel == oldQueryFormatPanel)
			return;
		this.activeQueryFormatPanel = newQueryFormatPanel;
		if (oldQueryFormatPanel != null)
			remove(oldQueryFormatPanel);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx      = 0;
		constraints.gridy      = 0;
		constraints.gridwidth  = 1;
		constraints.gridheight = 1;
		constraints.weightx    = 1;
		constraints.weighty    = 1;
		constraints.fill       = GridBagConstraints.BOTH;
		constraints.anchor     = GridBagConstraints.CENTER;
		constraints.insets     = new Insets(0, 0, 0, 0);
		add(this.activeQueryFormatPanel, constraints);
		revalidate();
		repaint();
	}
	
	private MWQueryFormat getQueryFormat() {
		return (MWQueryFormat) this.queryFormatHolder.getValue();
	}
	

	private boolean queryFormatCanChange() {
		String promptValue = TriStateBoolean.UNDEFINED.toString();
		String value = preferences().get(MappingsPlugin.CHANGE_QUERY_FORMAT_DO_NOT_THIS_SHOW_AGAIN_PREFERENCE, promptValue);
		boolean changeQueryType;

		if (value.equals(promptValue)) {
			changeQueryType = promptToChangeSelectionCriteriaType();
		}
		else {
			changeQueryType = TriStateBoolean.TRUE.toString().equals(value);
			if (!changeQueryType) {
				JOptionPane.showMessageDialog(
						this.getWorkbenchContext().getCurrentWindow(),
						this.resourceRepository().getString("QUERY_FORMAT_CHANGE_DISSALLOWED"));
			}

		}

		return changeQueryType;
	}

	private boolean promptToChangeSelectionCriteriaType() {
		if (this.preferences().getBoolean(MappingsPlugin.CHANGE_QUERY_FORMAT_DO_NOT_THIS_SHOW_AGAIN_PREFERENCE, false)) {
			return true;
		}

		// build dialog panel
		String title = this.resourceRepository().getString("QUERY_QUERY_FORMAT_TITLE");
		String message = this.resourceRepository().getString("QUERY_QUERY_FORMAT_MESSAGE");
		PropertyValueModel dontAskAgainHolder = new SimplePropertyValueModel(new Boolean(false));
		JComponent dontAskAgainPanel = 
			SwingComponentFactory.buildDoNotAskAgainPanel(message, dontAskAgainHolder, this.resourceRepository());

		// prompt user for response
		int response = 
			JOptionPane.showConfirmDialog(
				this.getWorkbenchContext().getCurrentWindow(),
				dontAskAgainPanel,
				title,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE
			);

		if (dontAskAgainHolder.getValue().equals(Boolean.TRUE)) {
			if (response == JOptionPane.YES_OPTION) {
				this.preferences().putBoolean(MappingsPlugin.CHANGE_QUERY_FORMAT_DO_NOT_THIS_SHOW_AGAIN_PREFERENCE, true);
			}
			else if (response == JOptionPane.NO_OPTION) {
				this.preferences().putBoolean(MappingsPlugin.CHANGE_QUERY_FORMAT_DO_NOT_THIS_SHOW_AGAIN_PREFERENCE, false);
			}
		}

		return (response == JOptionPane.OK_OPTION);
	}
}