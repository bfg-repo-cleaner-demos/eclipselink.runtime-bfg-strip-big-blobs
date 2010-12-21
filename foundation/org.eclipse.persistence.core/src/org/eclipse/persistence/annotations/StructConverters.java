/*******************************************************************************
 * Copyright (c) 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     08/19/2010-2.2 Guy Pelletier 
 *       - 282773: Add plural converter annotations
 ******************************************************************************/  
package org.eclipse.persistence.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** 
 * A StructConverters annotation allows the definition of multiple 
 * StructConverter.
 * 
 * @see org.eclipse.persistence.annotations.StructConverter
 * 
 * @author Guy Pelletier
 * @since EclipseLink 2.2 
 */ 
@Target({TYPE, METHOD, FIELD})
@Retention(RUNTIME)
public @interface StructConverters {
    /**
     * (Required) An array of struct converter.
     */
    StructConverter[] value(); 
}
