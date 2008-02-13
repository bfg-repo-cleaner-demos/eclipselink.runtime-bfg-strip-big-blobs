/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** 
 * The TypeConverter annotation is used to specify an EclipseLink 
 * org.eclipse.persistence.mappings.converters.TypeConversionConverter for modification 
 * of the data value(s) during the reading and writing of a mapped attribute.
 * 
 * A TypeConverter must be be uniquely identified by name and can be defined at 
 * the class, field and property level and can be specified within an Entity, 
 * MappedSuperclass and Embeddable class.
 * 
 * The usage of an TypeConverter is always specified via the Convert annotation 
 * and is supported on a Basic, BasicMap or BasicCollection mapping.
 * 
 * @see org.eclipse.persistence.annotations.Convert
 * @author Guy Pelletier
 * @since Oracle TopLink 11.1.1.0.0 
 */ 
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface TypeConverter {
    /**
     * (Required) Name this converter. The name should be unique across the 
     * whole persistence unit.
     */
    String name();

    /**
     * (Optional) Specify the type stored on the database. The default is 
     * inferred from the type of the persistence field or property.
     */
    Class dataType() default void.class;

    /**
     * (Optional) Specify the type stored on the entity. The default is inferred 
     * from the type of the persistent field or property.
     */
    Class objectType() default void.class;
}
