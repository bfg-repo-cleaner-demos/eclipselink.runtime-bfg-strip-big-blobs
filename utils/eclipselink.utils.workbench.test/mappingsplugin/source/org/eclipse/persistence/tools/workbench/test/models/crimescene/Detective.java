/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.models.crimescene;

public class Detective extends Person {
	private String precinct;
/**
 * Detective constructor comment.
 */
public Detective() {
	super();
}
/**
 * Return this detective's precinct
 * @author Christopher Garrett
 * @since TopLink for Java Course 1.1
 * @return java.lang.String
 */
public String getPrecinct() {
	return precinct;
}
/**
 * Set this detective's precinct
 * @author Christopher Garrett
 * @since TopLink for Java Course 1.1
 * @param newValue java.lang.String
 */
public void setPrecinct(String newValue) {
	this.precinct = newValue;
}
/**
 * Returns a string representing this detective, e.g.
 * "Detective Christopher Garrett"
 * @return a string representation of this detective
 */
@Override
public String toString() {
	return "Detective " + super.toString();
}
}
