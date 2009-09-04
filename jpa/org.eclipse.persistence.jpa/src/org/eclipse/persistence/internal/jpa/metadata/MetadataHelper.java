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
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 *     07/15/2008-1.0.1 Guy Pelletier 
 *       - 240679: MappedSuperclass Id not picked when on get() method accessor
 *     06/29/2009-2.0 Michael O'Brien 
 *       - 266912: change MappedSuperclass handling in stage2 to pre process accessors
 *          in support of the custom descriptors holding mappings required by the Metamodel 
 *          getClassForName is now public and referenced by MappingAccessor.getMapKeyReferenceClass()
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata;

import static org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProperties.CANONICAL_MODEL_PACKAGE_SUFFIX;
import static org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProperties.CANONICAL_MODEL_PACKAGE_SUFFIX_DEFAULT;
import static org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProperties.CANONICAL_MODEL_QUALIFIER;
import static org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProperties.CANONICAL_MODEL_QUALIFIER_DEFAULT;
import static org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProperties.CANONICAL_MODEL_QUALIFIER_POSITION;
import static org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProperties.CANONICAL_MODEL_QUALIFIER_POSITION_DEFAULT;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.Map;

import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProperties.QUALIFIER_POSITION;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.security.PrivilegedNewInstanceFromClass;
import org.eclipse.persistence.internal.sessions.AbstractSession;

/**
 * INTERNAL:
 * Common helper methods for the metadata processing.
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public class MetadataHelper {
    public static final String JPA_ORM_FILE = "META-INF/orm.xml";
    public static final String ECLIPSELINK_ORM_FILE = "META-INF/eclipselink-orm.xml";
    
    /**
     * INTERNAL:
     * Return the canonical name. This will apply the default qualifier given 
     * in the default position given. If the defaults given are null, then the
     * default qualifier "_" in the default position "POST" will be applied.
     */
    protected static String getCanonicalName(String name, Map<String, String> properties) {
        String qualifier = properties.get(CANONICAL_MODEL_QUALIFIER);
        String qualifierPosition = properties.get(CANONICAL_MODEL_QUALIFIER_POSITION);
        
        if (qualifier == null) {
            qualifier = CANONICAL_MODEL_QUALIFIER_DEFAULT;
        }
        
        if (qualifierPosition == null) {
            qualifierPosition = CANONICAL_MODEL_QUALIFIER_POSITION_DEFAULT;
        }
        
        if (qualifierPosition.equals(QUALIFIER_POSITION.PRE.name())) {
            return qualifier + name;
        } else {
            return name + qualifier;
        } 
    }
    
    /**
     * INTERNAL: 
     * Load a class from a given class name. (XMLEntityMappings calls this one)
     */
    public static Class getClassForName(String classname, ClassLoader loader) {
        try {
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                try {
                    return (Class) AccessController.doPrivileged(new PrivilegedClassForName(classname, true, loader));
                } catch (PrivilegedActionException exception) {
                    throw ValidationException.unableToLoadClass(classname, exception.getException());
                }
            } else {
                return PrivilegedAccessHelper.getClassForName(classname, true, loader);
            }
        } catch (ClassNotFoundException exception) {
            if (classname.indexOf('$') != -1) {
                String outer = classname.substring(0, classname.indexOf('$'));
                Class outerClass = getClassForName(outer, loader);
                for (int index = 0; index < outerClass.getClasses().length; index++)
                {
                    if (outerClass.getClasses()[index].getName().equals(classname))
                    {
                        return outerClass.getClasses()[index];
                    }
                }
            }
            throw ValidationException.unableToLoadClass(classname, exception);
        }
    }
    
    /**
     * INTERNAL:
     * Create a new instance of the class given.
     */
    static Object getClassInstance(Class cls) {
        try {
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                try {
                    return AccessController.doPrivileged(new PrivilegedNewInstanceFromClass(cls));
                } catch (PrivilegedActionException exception) {
                    throw ValidationException.errorInstantiatingClass(cls, exception.getException());
                }
            } else {
                return PrivilegedAccessHelper.newInstanceFromClass(cls);
            }
        } catch (IllegalAccessException exception) {
            throw ValidationException.errorInstantiatingClass(cls, exception);
        } catch (InstantiationException exception) {
            throw ValidationException.errorInstantiatingClass(cls, exception);
        }
    }
    
    /**
     * INTERNAL:
     * Create a new instance of the class name.
     */
    static Object getClassInstance(String className, ClassLoader loader) {
        return getClassInstance(getClassForName(className, loader));
    }
    
    /**
     * INTERNAL:
     * Helper method to return a field name from a candidate field name and a 
     * default field name.
     * 
     * Requires the context from where this method is called to output the 
     * correct logging message when defaulting the field name.
     *
     * In some cases, both the name and defaultName could be "" or null,
     * therefore, don't log a message and return name.
     */
    public static String getName(String name, String defaultName, String context, MetadataLogger logger, Object location) {
        String actualName = null;
        // Check if a candidate was specified otherwise use the default.
        if (name != null && !name.equals("")) {
            actualName =  name;
        } else if (defaultName == null || defaultName.equals("")) {
            return "";
        } else {
            // Log the defaulting field name based on the given context.
            logger.logConfigMessage(context, location, defaultName);
            actualName =  defaultName;
        }
        return actualName;
    }
    
    /**
     * INTERNAL:
     * Return the qualified canonical name of the given qualified class name.
     * This method will check the session for a corresponding class that was
     * processed during deploy. If one is not found, will build the canonical
     * name applying any default package, the default qualifier "_" in the 
     * default position "POST" on the name portion.  
     */
    public static String getQualifiedCanonicalName(String qualifiedName, AbstractSession session) {
        String sessionStaticMetamodelClass = session.getStaticMetamodelClass(qualifiedName);
        
        if (sessionStaticMetamodelClass == null) {
            return getQualifiedCanonicalName(qualifiedName, session.getProperties());
        } else {
            return sessionStaticMetamodelClass;
        }
    }
    
    /**
     * INTERNAL:
     * Return the canonical name applying any default package. This will apply 
     * the default qualifier given in the default position given. If the 
     * defaults given are null, then the default qualifier "_" in the default 
     * position "POST" will be applied.
     */
    public static String getQualifiedCanonicalName(String qualifiedName, Map<String, String> properties) {
        String packageSuffix = properties.get(CANONICAL_MODEL_PACKAGE_SUFFIX);
        if (packageSuffix == null) {
            packageSuffix = CANONICAL_MODEL_PACKAGE_SUFFIX_DEFAULT;
        }
       
        if (qualifiedName.indexOf(".") > -1) {
            String canonicalName = getCanonicalName(qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1), properties);
            String pkg = qualifiedName.substring(0, qualifiedName.lastIndexOf(".") + 1);
           
            return pkg + packageSuffix + "." + canonicalName;
        } else {
            return packageSuffix + "." + getCanonicalName(qualifiedName, properties);
        }
    }
    
    /**
     * INTERNAL:
     * Helper method to return a string value if specified, otherwise returns
     * the default value. 
     */
    public static Integer getValue(Integer value, Integer defaultValue) {
        // Check if a candidate was specified otherwise use the default.
        if (value == null) {
            return defaultValue;
        } else {
            // TODO: log a defaulting message
            return value;
        } 
    }
    
    /**
     * INTERNAL:
     * Helper method to return a string value if specified, otherwise returns
     * the default value.
     */
    public static String getValue(String value, String defaultValue) {
        // Check if a candidate was specified otherwise use the default.
        if (value != null && ! value.equals("")) {
            return value;
        } else {
            // TODO: log a defaulting message
            return defaultValue;
        }
    }
}
