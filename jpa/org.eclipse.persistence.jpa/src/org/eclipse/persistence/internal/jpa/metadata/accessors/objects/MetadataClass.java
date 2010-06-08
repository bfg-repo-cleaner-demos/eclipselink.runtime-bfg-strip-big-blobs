/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
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
 *     03/08/2010-2.1 Guy Pelletier 
 *       - 303632: Add attribute-type for mapping attributes to EclipseLink-ORM
 *     05/04/2010-2.1 Guy Pelletier 
 *       - 309373: Add parent class attribute to EclipseLink-ORM
 *     05/14/2010-2.1 Guy Pelletier 
 *       - 253083: Add support for dynamic persistence using ORM.xml/eclipselink-orm.xml
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.accessors.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.libraries.asm.Constants;

/**
 * INTERNAL:
 * An object to hold onto a valid JPA decorated class.
 * 
 * @author Guy Pelletier
 * @since TopLink 10.1.3/EJB 3.0 Preview
 */
public class MetadataClass extends MetadataAnnotatedElement {
    protected boolean m_isAccessible;
    protected boolean m_isPrimitive;
    protected boolean m_isJDK;
    protected int m_modifiers;
    
    // Stores the implements interfaces of this class.
    protected List<String> m_interfaces;
    
    // Stores a list of enclosed classes found inside this metadata class. 
    // E.g. inner classes, enums etc.
    protected List<MetadataClass> m_enclosedClasses;
    
    // Store the classes field metadata, keyed by the field's name.
    protected Map<String, MetadataField> m_fields;
    
    // Store the classes method metadata, keyed by the method's name.
    // Method's next is used if multiple method with the same name.
    protected Map<String, MetadataMethod> m_methods;
    
    protected MetadataClass m_superclass;
    protected String m_superclassName;

    /**
     * Create the metadata class with the class name.
     */
    public MetadataClass(MetadataFactory factory, String name) {
        super(factory);
        setName(name);
        
        // By default, set the type to be the same as the name. The canonical
        // model generator relies on types which in most cases is the name, but
        // the generator resolves generic types a little differently to 
        // correctly generate model classes.
        setType(name); 
        
        m_isAccessible = true;
        m_interfaces = new ArrayList<String>();
        m_enclosedClasses = new ArrayList<MetadataClass>();
        m_fields = new HashMap<String, MetadataField>(); 
        m_methods = new HashMap<String, MetadataMethod>();
    }
    
    /**
     * Create the metadata class based on the class.
     * Mainly used for primitive defaults.
     */
    public MetadataClass(MetadataFactory factory, Class cls) {
        this(factory, cls.getName());
        m_isPrimitive = cls.isPrimitive();
    }

    /**
     * INTERNAL:
     */
    public void addEnclosedClass(MetadataClass enclosedClass) {
        m_enclosedClasses.add(enclosedClass);
    }
    
    /**
     * INTERNAL:
     */
    public void addField(MetadataField field) {
        m_fields.put(field.getName(), field);
    }
    
    /**
     * INTERNAL:
     */
    public void addInterface(String interfaceName) {
        m_interfaces.add(interfaceName);
    }
    
    /**
     * INTERNAL:
     */
    public void addMethod(MetadataMethod method) {
        m_methods.put(method.getName(), method);
    }
    
    /**
     * Allow comparison to Java classes and Metadata classes.
     */
    public boolean equals(Object object) {
        if (object instanceof Class) {
            if (getName() == null) {
                // Void's name is null.
                return ((Class)object).getName() == null;
            }
            
            return getName().equals(((Class)object).getName());
        }
        
        return super.equals(object);
    }
    
    /**
     * INTERNAL:
     * Return if this class is or extends, or super class extends the class.
     */
    public boolean extendsClass(Class javaClass) {
        return extendsClass(javaClass.getName());
    }
    
    /**
     * INTERNAL:
     * Return if this class is or extends, or super class extends the class.
     */
    public boolean extendsClass(String className) {
        if (getName() == null) {
            return className == null;
        }
        
        if (getName().equals(className)) {
            return true;
        }
        
        if (getSuperclassName() == null) {
            return false;
        }
        
        if (getSuperclassName().equals(className)) {
            return true;
        }
        
        return getSuperclass().extendsClass(className);
    }
    
    /**
     * INTERNAL:
     * Return if this class is or extends, or super class extends the interface.
     */
    public boolean extendsInterface(Class javaClass) {
        return extendsInterface(javaClass.getName());
    }
    
    /**
     * INTERNAL:
     * Return if this class is or extends, or super class extends the interface.
     */
    public boolean extendsInterface(String className) {
        if (getName() == null) {
            return false;
        }
        
        if (getName().equals(className)) {
            return true;
        }
        
        if (getInterfaces().contains(className)) {
            return true;
        }
        
        for (String interfaceName : getInterfaces()) {
            if (getMetadataClass(interfaceName).extendsInterface(className)) {
                return true;
            }
        }
        
        if (getSuperclassName() == null) {
            return false;
        }
        
        return getSuperclass().extendsInterface(className);
    }

    /**
     * INTERNAL:
     * Return the list of classes defined within this metadata class. E.g.
     * enums and inner classes.
     */
    public List<MetadataClass> getEnclosedClasses() {
        return m_enclosedClasses;
    }
    
    /**
     * INTERNAL:
     * Return the field with the name.
     * Search for any declared or inherited field.
     */
    public MetadataField getField(String name) {
        return getField(name, true);
    }
    
    /**
     * INTERNAL:
     * Return the field with the name.
     * Search for any declared or inherited field.
     */
    public MetadataField getField(String name, boolean checkSuperClass) {
        MetadataField field = m_fields.get(name);
        
        if (checkSuperClass && (field == null) && (getSuperclassName() != null)) {
            return getSuperclass().getField(name);
        }
        
        return field;
    }
    
    /**
     * INTERNAL:
     */
    public Map<String, MetadataField> getFields() {
        return m_fields;
    }

    /**
     * INTERNAL:
     */
    public List<String> getInterfaces() {
        return m_interfaces;
    }
    
    /**
     * INTERNAL:
     * Return the method with the name and no arguments.
     */
    protected MetadataMethod getMethod(String name) {
        return m_methods.get(name);
    }

    /**
     * INTERNAL:
     * Return the method with the name and argument types.
     */
    public MetadataMethod getMethod(String name, Class[] arguments) {
        List<String> argumentNames = new ArrayList<String>(arguments.length);
        for (int index = 0; index < arguments.length; index++) {
            argumentNames.add(arguments[index].getName());
        }
        return getMethod(name, argumentNames);
    }

    /**
     * INTERNAL:
     * Return the method with the name and argument types (class names).
     */
    public MetadataMethod getMethod(String name, List<String> arguments) {
        return getMethod(name, arguments, true);
    }

    /**
     * INTERNAL:
     * Return the method with the name and argument types (class names).
     */
    public MetadataMethod getMethod(String name, List<String> arguments, boolean checkSuperClass) {
        MetadataMethod method = m_methods.get(name);
        while ((method != null) && !method.getParameters().equals(arguments)) {
            method = method.getNext();
        }
        if (checkSuperClass && (method == null) && (getSuperclassName() != null)) {
            return getSuperclass().getMethod(name, arguments);
        }
        return method;
    }
    
    /**
     * INTERNAL:
     * Return the method with the name and argument types (class names).
     */
    public MetadataMethod getMethod(String name, String[] arguments) {
        return getMethod(name, Arrays.asList(arguments));
    }
    
    /**
     * INTERNAL:
     * Return the method for the given property name.
     */
    public MetadataMethod getMethodForPropertyName(String propertyName) {
        MetadataMethod method;
        
        String leadingChar = String.valueOf(propertyName.charAt(0)).toUpperCase();
        String restOfName = propertyName.substring(1);
        
        // Look for a getPropertyName() method
        method = getMethod(Helper.GET_PROPERTY_METHOD_PREFIX.concat(leadingChar).concat(restOfName), new String[]{});
        
        if (method == null) {
            // Look for an isPropertyName() method
            method = getMethod(Helper.IS_PROPERTY_METHOD_PREFIX.concat(leadingChar).concat(restOfName), new String[]{});
        }
        
        if (method != null) {
            method.setSetMethod(method.getSetMethod(this));
        }
        
        return method;
    }
    
    /**
     * INTERNAL:
     */
    public Map<String, MetadataMethod> getMethods() {
        return m_methods;
    }
    
    /**
     * INTERNAL:
     */
    public int getModifiers() {
        return m_modifiers;
    }
    
    /**
     * INTERNAL:
     */
    public MetadataClass getSuperclass() {
        if (m_superclass == null) {
            m_superclass = getMetadataClass(m_superclassName);
        }
        
        return m_superclass;
    }

    /**
     * INTERNAL:
     */
    public String getSuperclassName() {
        return m_superclassName;
    }
    
    /**
     * Return the ASM type name.
     */
    public String getTypeName() {
        if (isArray()) {
            return getName().replace('.', '/');
        } else if (isPrimitive()) {
            if (getName().equals("int")) {
                return "I";
            } else if (getName().equals("long")) {
                return "J";
            } else if (getName().equals("short")) {
                return "S";
            } else if (getName().equals("boolean")) {
                return "Z";
            } else if (getName().equals("float")) {
                return "F";
            } else if (getName().equals("double")) {
                return "D";
            } else if (getName().equals("char")) {
                return "C";
            } else if (getName().equals("byte")) {
                return "B";
            }
        }
        return "L" + getName().replace('.', '/') + ";";
    }
    
    /**
     * INTERNAL:
     * Return true is this class accessible to be found.
     */
    public boolean isAccessible() {
        return m_isAccessible;
    }
    
    /**
     * INTERNAL:
     * Return if this class is an array type.
     */
    public boolean isArray() {
        return (getName() != null) && (getName().charAt(0) == '[');
    }
    
    /**
     * INTERNAL:
     * Return if this is extends Collection.
     */
    public boolean isCollection() {
        return extendsInterface(Collection.class);
    }
    
    /**
     * INTERNAL:
     * Return if this is extends Enum.
     */
    public boolean isEnum() {
        return extendsClass(Enum.class);
    }
    
    /**
     * INTERNAL:
     * Return if this is an interface (super is null).
     */
    public boolean isInterface() {
        return (Constants.ACC_INTERFACE & m_modifiers) != 0;
    }
    
    /**
     * INTERNAL:
     * Return if this is a JDK (java/javax) class.
     */
    public boolean isJDK() {
        return m_isJDK;
    }
    
    /**
     * INTERNAL:
     * Return if this is extends List.
     */
    public boolean isList() {
        return extendsInterface(List.class);
    }
    
    /**
     * INTERNAL:
     * Return if this is extends Map.
     */
    public boolean isMap() {
        return extendsInterface(Map.class);
    }
    
    /**
     * INTERNAL:
     * Return if this is Object class.
     */
    public boolean isObject() {
        return getName().equals(Object.class.getName());
    }
    
    /**
     * INTERNAL:
     * Return if this is a primitive.
     */
    public boolean isPrimitive() {
        return m_isPrimitive;
    }
    
    /**
     * INTERNAL:
     * Return if this class extends Serializable or is an array type.
     */
    public boolean isSerializable() {
        if (isArray()) {
            return true;
        }
        return extendsInterface(Serializable.class);
    }
    
    /**
     * INTERNAL:
     * Return true if this extends Set.
     */
    public boolean isSet() {
        return extendsInterface(Set.class);
    }
    
    /**
     * INTERNAL:
     * Return if this is the void class.
     */
    public boolean isVoid() {
        return getName().equals(void.class.getName());
    }
    
    /**
     * INTERNAL:
     */
    public void setIsAccessible(boolean isAccessible) {
        m_isAccessible = isAccessible;
    }
    
    /**
     * INTERNAL:
     */
    public void setIsJDK(boolean isJDK) {
        m_isJDK = isJDK;
    }
    
    /**
     * INTERNAL:
     */
    public void setModifiers(int modifiers) {
        m_modifiers = modifiers;
    }

    /**
     * INTERNAL:
     */
    public void setSuperclass(MetadataClass superclass) {
        m_superclass = superclass;
    } 
    
    /**
     * INTERNAL:
     */
    public void setSuperclassName(String superclass) {
        m_superclassName = superclass;
    } 
}
