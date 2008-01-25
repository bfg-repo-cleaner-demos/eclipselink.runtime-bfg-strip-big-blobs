/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.uitools.app.swing;

import javax.swing.ButtonModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.eclipse.persistence.tools.workbench.test.utility.TestTools;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.swingui.TestRunner;

import org.eclipse.persistence.tools.workbench.uitools.app.PropertyValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.SimplePropertyValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.ValueModel;
import org.eclipse.persistence.tools.workbench.uitools.app.swing.CheckBoxModelAdapter;
import org.eclipse.persistence.tools.workbench.utility.ClassTools;


public class CheckBoxModelAdapterTests extends TestCase {
	private PropertyValueModel booleanHolder;
	private ButtonModel buttonModelAdapter;
	boolean eventFired;

	public static void main(String[] args) {
		TestRunner.main(new String[] {"-c", CheckBoxModelAdapterTests.class.getName()});
	}
	
	public static Test suite() {
		return new TestSuite(CheckBoxModelAdapterTests.class);
	}
	
	public CheckBoxModelAdapterTests(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		this.booleanHolder = new SimplePropertyValueModel(Boolean.TRUE);
		this.buttonModelAdapter = new CheckBoxModelAdapter(this.booleanHolder);
	}

	protected void tearDown() throws Exception {
		TestTools.clear(this);
		super.tearDown();
	}

	public void testSetSelected() throws Exception {
		this.eventFired = false;
		this.buttonModelAdapter.addChangeListener(new TestChangeListener() {
			public void stateChanged(ChangeEvent e) {
				CheckBoxModelAdapterTests.this.eventFired = true;
			}
		});
		this.buttonModelAdapter.setSelected(false);
		assertTrue(this.eventFired);
		assertEquals(Boolean.FALSE, this.booleanHolder.getValue());
	}

	public void testSetValue() throws Exception {
		this.eventFired = false;
		this.buttonModelAdapter.addChangeListener(new TestChangeListener() {
			public void stateChanged(ChangeEvent e) {
				CheckBoxModelAdapterTests.this.eventFired = true;
			}
		});
		assertTrue(this.buttonModelAdapter.isSelected());
		this.booleanHolder.setValue(Boolean.FALSE);
		assertTrue(this.eventFired);
		assertFalse(this.buttonModelAdapter.isSelected());
	}

	public void testDefaultValue() throws Exception {
		this.eventFired = false;
		this.buttonModelAdapter.addChangeListener(new TestChangeListener() {
			public void stateChanged(ChangeEvent e) {
				CheckBoxModelAdapterTests.this.eventFired = true;
			}
		});
		assertTrue(this.buttonModelAdapter.isSelected());
		this.booleanHolder.setValue(null);
		assertTrue(this.eventFired);
		assertFalse(this.buttonModelAdapter.isSelected());

		this.eventFired = false;
		this.booleanHolder.setValue(Boolean.FALSE);
		assertFalse(this.eventFired);
		assertFalse(this.buttonModelAdapter.isSelected());
	}

	public void testHasListeners() throws Exception {
		SimplePropertyValueModel localBooleanHolder = (SimplePropertyValueModel) this.booleanHolder;
		assertFalse(localBooleanHolder.hasAnyPropertyChangeListeners(ValueModel.VALUE));
		this.verifyHasNoListeners(this.buttonModelAdapter);

		ChangeListener listener = new TestChangeListener();
		this.buttonModelAdapter.addChangeListener(listener);
		assertTrue(localBooleanHolder.hasAnyPropertyChangeListeners(ValueModel.VALUE));
		this.verifyHasListeners(this.buttonModelAdapter);

		this.buttonModelAdapter.removeChangeListener(listener);
		assertFalse(localBooleanHolder.hasAnyPropertyChangeListeners(ValueModel.VALUE));
		this.verifyHasNoListeners(this.buttonModelAdapter);
	}

	private void verifyHasNoListeners(Object model) throws Exception {
		EventListenerList listenerList = (EventListenerList) ClassTools.getFieldValue(model, "listenerList");
		assertEquals(0, listenerList.getListenerList().length);
	}

	private void verifyHasListeners(Object model) throws Exception {
		EventListenerList listenerList = (EventListenerList) ClassTools.getFieldValue(model, "listenerList");
		assertFalse(listenerList.getListenerList().length == 0);
	}


private class TestChangeListener implements ChangeListener {
	public void stateChanged(ChangeEvent e) {
		fail("unexpected event");
	}
}

}
