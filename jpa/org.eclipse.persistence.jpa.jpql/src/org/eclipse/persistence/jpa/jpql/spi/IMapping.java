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
package org.eclipse.persistence.jpa.jpql.spi;

import java.lang.annotation.Annotation;

/**
 * The external representation of a mapping, which represents a single persistence property
 * of a managed type.
 *
 * @see IManagedType
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
public interface IMapping extends IExternalForm,
                                  Comparable<IMapping> {

	/**
	 * Returns the type of this mapping.
	 *
	 * @return One of the supported mapping type
	 */
	IMappingType getMappingType();

	/**
	 * Returns the name of the persistence property represented by this mapping.
	 *
	 * @return The name of this mapping
	 */
	String getName();

	/**
	 * Returns the parent managed type owning this mapping.
	 *
	 * @return The parent of this mapping
	 */
	IManagedType getParent();

	/**
	 * Returns the type of this mapping. If this mapping is a relationship mapping, the parameter
	 * type of the collection is returned.
	 * <p>
	 * <code>@OneToMany<br>private Collection&lt;Employee&gt; employees;</code>
	 * <p>
	 * "Employee" is the type. To retrieve {@link java.util.Collection}, {@link #getTypeDeclaration()}
	 * needs to be used, its type will be {@link java.util.Collection} and it's generic type will be
	 * <code>Employee</code>.
	 *
	 * @return The external form representing the type of this mapping
	 */
	IType getType();

	/**
	 * Returns the declaration of the Java class, which gives the information about type parameters,
	 * dimensionality, etc.
	 * <p>
	 * <code>@OneToMany<br>private Collection&lt;Employee&gt; employees;</code>
	 * <p>
	 * "Collection&lt;Employee&gt;" is the type declaration.
	 *
	 * @return The external form of the class' type declaration
	 */
	ITypeDeclaration getTypeDeclaration();

	/**
	 * Determines whether the given annotation is present on this type.
	 *
	 * @param annotationType The class of the annotation
	 * @return <code>true</code> if the annotation is defined on this type; <code>false</code>
	 * otherwise
	 */
	boolean hasAnnotation(Class<? extends Annotation> annotationType);
}