/*******************************************************************************
 * Copyright (c) 2006, 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation
 *
 ******************************************************************************/
package org.eclipse.persistence.utils.jpa.query;

import java.util.Iterator;
import org.eclipse.persistence.utils.jpa.query.spi.IMappingType;

/**
 * This object stores the various choices available for content assist for a certain position within
 * a JPQL query. The choices are stored in categories (abstract schema names, identifiers,
 * identification variables and properties).
 *
 * @version 11.2.0
 * @since 11.2.0
 * @author Pascal Filion
 */
public interface ContentAssistItems {

	/**
	 * Returns the possible abstract schema names, which are the entity names.
	 *
	 * @return The possible abstract schema names that are allowed at the position of the cursor
	 * within the query
	 */
	Iterator<String> abstractSchemaNames();

	/**
	 * Retrieves the abstract schema name that is mapped with the given identification variable.
	 *
	 * @param identificationVariable
	 * @return The abstract schema name mapped with the given identification variable or <code>null</code>
	 * if the given variable is mapped to something else
	 */
	String getAbstractSchemaName(String identificationVariable);

	/**
	 * Retrieves the {@link IMappingType} for the property with the given name.
	 *
	 * @param propertyName The name of the property that should be part of this holder
	 * @return The {@link IMappingType} for the property with the given name
	 */
	IMappingType getMappingType(String propertyName);

	/**
	 * Determines whether this holder has at least one item.
	 *
	 * @return <code>true</code> if there is at least one item; <code>false</code> if this holder is
	 * empty
	 */
	boolean hasItems();

	/**
	 * Returns the list of possible identification variables.
	 *
	 * @return The list of possible identification variables
	 */
	Iterator<String> identificationVariables();

	/**
	 * Returns the list of possible JPQL identifiers.
	 *
	 * @return The list of possible JPQL identifiers
	 */
	Iterator<String> identifiers();

	/**
	 * Returns the names of the state fields and collection valued fields.
	 *
	 * @return The list of possible choices that are the names of state fields and
	 * collection valued fields
	 */
	Iterator<String> properties();
}