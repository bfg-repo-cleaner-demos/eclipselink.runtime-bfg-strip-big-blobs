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
package org.eclipse.persistence.exceptions.i18n;

import java.util.ListResourceBundle;

public class PersistenceUnitLoadingExceptionResource extends ListResourceBundle {
 
        static final Object[][] contents = {
                                           { "30001", "An exception was thrown while trying to load a persistence unit for directory: {0}"},
                                           { "30002", "An exception was thrown while trying to load a persistence unit for jar file: {0}"},
                                           { "30003", "An exception was thrown while processing persistence unit at URL: {0}"},
                                           { "30004", "An exception was thrown while processing persistence.xml from URL: {0}"},
                                           { "30005", "An exception was thrown while searching for persistence archives with ClassLoader: {0}"},
                                           { "30006", "An exception was thrown while searching for entities at URL: {0}"},
                                           { "30007", "An exception was thrown while loading class: {0} to check whether it implements @Entity, @Embeddable, or @MappedSuperclass."},
                                           { "30008", "File path returned was empty or null"},                                                                  
                                           { "30009", "An exception was thrown while trying to load persistence unit at url: {0}"},
                                           { "30010", "An exception was thrown while loading ORM XML file: {0}"},
                                           { "30011", "TopLink could not get classes from the URL: {0}.  TopLink attempted to read this URL as a jarFile and as a Directory and was unable to process it."},
                                           { "30012", "TopLink could not get persistence unit info from the URL:{0}"},
                                           { "30013", "An exception was thrown while trying to build a persistence unit name for the persistence unit [{1}] from URL: {0}."}
    };

    /**
      * Return the lookup table.
      */
    protected Object[][] getContents() {
        return contents;
    }
}
