/*******************************************************************************
* Copyright (c) 2007 Oracle. All rights reserved.
* This program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0, which accompanies this distribution and is available at
* http://www.eclipse.org/legal/epl-v10.html.
*
* Contributors:
*     Oracle - initial API and implementation
******************************************************************************/
package org.eclipse.persistence.tools.workbench.test.models.complexmapping;

import java.io.Serializable;

import org.eclipse.persistence.tools.schemaframework.TableDefinition;

public class Cubicle implements Serializable {
	public Number id;
	public String location;

/**
 * This method was created by a SmartGuide.
 */
public Cubicle ( ) 
{
	}
public static Cubicle example1()
{
	Cubicle example = new Cubicle();
	example.setLocation("3rd floor, Section R, Third qubicle on left");
	return example;
}
public static Cubicle example2()
{
	Cubicle example = new Cubicle();
	example.setLocation("2nd floor, Section P, Close to the Middle");
	return example;
}
public void setLocation(String location)
{
		this.location = location; 
}
/**
 * Return a platform independant definition of the database table.
 */

public static TableDefinition tableDefinition() 
{
	TableDefinition definition = new TableDefinition();

	definition.setName("MAP_CUB");

	definition.addIdentityField("C_ID", String.class, 15);
	definition.addField("LOCATION", String.class, 255);
		return definition;
}
}
