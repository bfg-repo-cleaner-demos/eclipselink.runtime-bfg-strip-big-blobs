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

import org.eclipse.persistence.utils.jpa.query.spi.IMapping;
import org.eclipse.persistence.utils.jpa.query.spi.IMappingType;

/**
 * This class has helper methods that determine the type of the {@link IMapping}.
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
public final class MappingTypeHelper {

	/**
	 * Determines whether the given {@link IMapping} is a collection type mapping.
	 *
	 * @return <code>true</code> if the given {@link IMapping} is a collection mapping; <code>false</code>
	 * otherwise
	 */
	static boolean isCollectionMapping(IMapping mapping) {
		return isCollectionMapping(mappingType(mapping));
	}

	/**
	 * Determines whether the given {@link IMappingType} is considered a collection mapping.
	 *
	 * @return <code>true</code> if the given {@link IMapping} is considered a collection mapping;
	 * <code>false</code> otherwise
	 */
	static boolean isCollectionMapping(IMappingType mappingType) {
		switch (mappingType) {
			case ELEMENT_COLLECTION:
			case MANY_TO_MANY:
			case ONE_TO_MANY:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Determines whether the given {@link IMapping} is a foreign reference mapping.
	 *
	 * @return <code>true</code> if the given {@link IMapping} is a foreign reference mapping;
	 * <code>false</code> otherwise
	 */
	static boolean isForeignReferenceMapping(IMapping mapping) {
		return isForeignReferenceMapping(mappingType(mapping));
	}

	/**
	 * Determines whether the given {@link IMappingType} is considered a foreign reference mapping.
	 *
	 * @return <code>true</code> if the given {@link IMapping} is considered a foreign reference
	 * mapping; <code>false</code> otherwise
	 */
	static boolean isForeignReferenceMapping(IMappingType mappingType) {
		switch (mappingType) {
			case ELEMENT_COLLECTION:
			case MANY_TO_MANY:
			case MANY_TO_ONE:
			case ONE_TO_MANY:
			case ONE_TO_ONE:
			case VARIABLE_ONE_TO_ONE: return true;
			default:                  return false;
		}
	}

	/**
	 * Determines whether the given {@link IMapping} is a relationship type mapping.
	 *
	 * @return <code>true</code> if the given {@link IMapping} is a relationship mapping;
	 * <code>false</code> otherwise
	 */
	static boolean isRelationshipMapping(IMapping mapping) {
		return isRelationshipMapping(mappingType(mapping));
	}

	/**
	 * Determines whether the given {@link IMappingType} is considered a relationship mapping.
	 *
	 * @return <code>true</code> if the given {@link IMapping} is considered a relationship mapping;
	 * <code>false</code> otherwise
	 */
	static boolean isRelationshipMapping(IMappingType mappingType) {
		switch (mappingType) {
			case ELEMENT_COLLECTION:
			case MANY_TO_MANY:
			case ONE_TO_MANY:
			case ONE_TO_ONE:
			case MANY_TO_ONE:
			case VARIABLE_ONE_TO_ONE:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Determines whether the given {@link IMapping} is a transient mapping.
	 *
	 * @return <code>true</code> if the given {@link IMapping} is a transient mapping;
	 * <code>false</code> otherwise
	 */
	static boolean isTransientMapping(IMapping mapping) {
		return mappingType(mapping) == IMappingType.TRANSIENT;
	}

	/**
	 * Retrieves the {@link IMappingType} from the given {@link IMapping}.
	 *
	 * @param mapping The {@link IMapping}, which can be <code>null</code>
	 * @return The given {@link IMapping}'s {@link IMappingType type} or {@link IMappingType#TRANSIENT}
	 * if the mapping is <code>null</code>
	 */
	static IMappingType mappingType(IMapping mapping) {
		return (mapping != null) ? mapping.getMappingType() : IMappingType.TRANSIENT;
	}
}