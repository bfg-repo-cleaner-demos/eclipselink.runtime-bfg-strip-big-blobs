/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
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
package org.eclipse.persistence.internal.helper;


import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.persistence.logging.SessionLog;


/**
 * @author Mitesh Meswani
 * This class is responsible to translate given database name to a DatabasePlatform.
 */
public class DBPlatformHelper {
    private static final String DEFAULTPLATFORM  = "org.eclipse.persistence.platform.database.DatabasePlatform"; // NOI18N

    private final static String PROPERTY_PATH = "org/eclipse/persistence/internal/helper/"; // NOI18N

    private final static String VENDOR_NAME_TO_PLATFORM_RESOURCE_NAME =
        PROPERTY_PATH + "VendorNameToPlatformMapping.properties";  //NOI18N

    /**
     * Holds mapping between possible vendor names to internal platforms defined above.
     * vendor names are treated as regular expressions.
     */
    private static Properties _nameToVendorPlatform = null;

    /** Get Database Platform from vendor name.
     * @param vendorName Input vendor name. Typically this is obtained by querying
     * <code>DatabaseMetaData</code>.
     * @param logger The logger.
     * @return Database platform that corresponds to <code>vendorName</code>.
     * If vendorName does not match any of predefined vendor names, <code>
     * DEFAULTPLATFORM </code> is returned.
     */
    public static String getDBPlatform(String vendorName, SessionLog logger) {

        initializeNameToVendorPlatform(logger);

        String detectedDbPlatform = null;
        if(vendorName != null) {
            detectedDbPlatform = matchVendorNameInProperties(vendorName, _nameToVendorPlatform, logger);
        }
        if (logger.shouldLog(SessionLog.FINE) ) {
            logger.log(SessionLog.FINE, "dbPlatformHelper_detectedVendorPlatform", detectedDbPlatform ); // NOI18N
        }
        if (detectedDbPlatform == null) {
            if(logger.shouldLog(SessionLog.INFO)) {
                logger.log(SessionLog.INFO, "dbPlatformHelper_defaultingPlatform",  vendorName, DEFAULTPLATFORM); // NOI18N
            }
            detectedDbPlatform = DEFAULTPLATFORM;
        }
        return detectedDbPlatform;
    }

    /**
     * Allocate and initialize nameToVendorPlatform if not already done.
     */
    private static Properties initializeNameToVendorPlatform(SessionLog logger) {
        synchronized(DBPlatformHelper.class) {
            if(_nameToVendorPlatform == null) {
                _nameToVendorPlatform = new Properties();
                try {
                    loadFromResource(_nameToVendorPlatform, VENDOR_NAME_TO_PLATFORM_RESOURCE_NAME,
                                            DBPlatformHelper.class.getClassLoader() );
                } catch (IOException e) {
                    logger.log(SessionLog.WARNING, "dbPlatformHelper_noMappingFound", VENDOR_NAME_TO_PLATFORM_RESOURCE_NAME);
                }
            }
        }
        return _nameToVendorPlatform;
    }

    /**
     * Match vendorName in properties specified by _nameToVendorPlatform.
     */
    private static String matchVendorNameInProperties(String vendorName,
            Properties nameToVendorPlatform, SessionLog logger) {
        String dbPlatform = null;
        //Iterate over all properties till we find match.
        for( Iterator iterator = nameToVendorPlatform.entrySet().iterator();
                dbPlatform == null && iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String regExpr = (String) entry.getKey();
            String value = (String) entry.getValue();
            if(logger.shouldLog(SessionLog.FINEST)) {
                logger.log(SessionLog.FINEST, "dbPlatformHelper_regExprDbPlatform", regExpr, value); // NOI18N
            }
            if( matchPattern(regExpr, vendorName, logger) ) {
                dbPlatform = value;
            }
        }
        return dbPlatform;
    }

   /** Matches target to pattern specified regExp. Returns false if there is
    * any error compiling regExp.
    * @param regExp The regular expression.
    * @param target The target against which we are trying to match regExp.
    * @param logger
    * @return false if there is error compiling regExp or target does not
    * match regExp. true if regExp matches pattern.
    */
    private static boolean matchPattern(String regExp, String target, SessionLog logger) {
        boolean matches = false;
        try {
            matches = Pattern.matches(regExp,target);
        } catch (PatternSyntaxException e){
            if(logger.shouldLog(SessionLog.FINE)) {
                logger.log(SessionLog.FINE, "dbPlatformHelper_patternSyntaxException", e); // NOI18N
            }
        }
        return matches;
    }

    //-----Property Loading helper methods ----/
    private static void loadFromResource(Properties properties, String resourceName, ClassLoader classLoader)
            throws IOException {
        load(properties, resourceName, classLoader);
    }

    /**
     * Loads properties list from the specified resource
     * into specified Properties object.
     * @param properties	Properties object to load
     * @param resourceName	Name of resource.
     *                      If loadFromFile  is true, this is fully qualified path name to a file.
     *                      param classLoader is ignored.
     *                      If loadFromFile  is false,this is resource name.
     * @param classLoader   The class loader that should be used to load the resource. If null,primordial
     *                      class loader is used.
     */
    private static void load(Properties properties, final String resourceName,
            final ClassLoader classLoader)
                            throws IOException {

        InputStream bin = new BufferedInputStream(openResourceInputStream(resourceName,classLoader));

        try {
            properties.load(bin);
        } finally {
            try {
                bin.close();
            } catch (Exception e) {
                // no action
            }
        }
    }

    /**
     * Open resourceName as input stream inside doPriviledged block
     */
    private static InputStream openResourceInputStream(final String resourceName, final ClassLoader classLoader) {
        return (InputStream) AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    if (classLoader != null) {
                        return classLoader.getResourceAsStream(resourceName);
                    } else {
                        return ClassLoader.getSystemResourceAsStream(resourceName);
                    }
                }
            }
        );
    }

}
