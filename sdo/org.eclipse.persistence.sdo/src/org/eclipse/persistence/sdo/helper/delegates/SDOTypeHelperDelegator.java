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
package org.eclipse.persistence.sdo.helper.delegates;

import commonj.sdo.DataObject;
import commonj.sdo.Property;
import commonj.sdo.Type;
import commonj.sdo.helper.HelperContext;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.eclipse.persistence.sdo.SDOType;
import org.eclipse.persistence.sdo.helper.SDOHelperContext;
import org.eclipse.persistence.sdo.helper.SDOTypeHelper;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.oxm.NamespaceResolver;

/**
 * <p><b>Purpose</b>: Helper to provide access to declared SDO Types.
 * <p><b>Responsibilities</b>:<ul>
 * <li> Finds the appropriate SDOTypeHelperDelegate for the classLoader/application name and delegates work to that
 * <li> Look up a Type given the uri and typeName or interfaceClass.
 * <li> SDO Types are available through the getType("commonj.sdo", typeName) method.
 * <li> Defines Types from DataObjects.
 * </ul>
 */
public class SDOTypeHelperDelegator extends AbstractHelperDelegator implements SDOTypeHelper {

    public SDOTypeHelperDelegator() {
        // TODO: JIRA129 - default to static global context - Do Not use this convenience constructor outside of JUnit testing
    }

    public SDOTypeHelperDelegator(HelperContext aContext) {
        aHelperContext = aContext;
    }

    public Class getJavaWrapperTypeForSDOType(Type sdoType) {
        return getSDOTypeHelperDelegate().getJavaWrapperTypeForSDOType(sdoType);
    }

    public Type getType(String uri, String typeName) {
        return getSDOTypeHelperDelegate().getType(uri, typeName);
    }

    public Type getTypeForSimpleJavaType(Class implClass) {
        return getSDOTypeHelperDelegate().getTypeForSimpleJavaType(implClass);
    }

    public void addType(SDOType newType) {
        getSDOTypeHelperDelegate().addType(newType);
    }

    public Type getType(Class interfaceClass) {
        return getSDOTypeHelperDelegate().getType(interfaceClass);
    }

    public Type define(DataObject dataObject) {
        return getSDOTypeHelperDelegate().define(dataObject);
    }

    public List define(List types) {
        return getSDOTypeHelperDelegate().define(types);
    }

    public QName getXSDTypeFromSDOType(Type aType) {
        return getSDOTypeHelperDelegate().getXSDTypeFromSDOType(aType);
    }

    public SDOType getSDOTypeFromXSDType(QName aName) {
        return getSDOTypeHelperDelegate().getSDOTypeFromXSDType(aName);
    }

    public void setTypesHashMap(Map typesHashMap) {
        getSDOTypeHelperDelegate().setTypesHashMap(typesHashMap);
    }

    public Map getTypesHashMap() {
        return getSDOTypeHelperDelegate().getTypesHashMap();
    }

    /**
     * INTERNAL:
     * Return the map of Wrapper objects (SDOWrapperTypes that wrap a primitive document).
     * @return a HashMap of SDOWrapperTypes, keyed on the XSD type that it wraps.
     */
    public Map getWrappersHashMap() {
        return getSDOTypeHelperDelegate().getWrappersHashMap();
    }

    /**
     * INTERNAL:
     * Set the map of Wrapper objects (SDOWrapperTypes that wrap a primitive document).
     * @param   aMap        a HashMap of SDOWrapperTypes, keyed on the XSD type that it wraps.
     */
    public void setWrappersHashMap(Map aMap) {
        getSDOTypeHelperDelegate().setWrappersHashMap(aMap);
    }

    public void reset() {
        getSDOTypeHelperDelegate().reset();
    }

    public Property defineOpenContentProperty(String uri, DataObject property) {
        return getSDOTypeHelperDelegate().defineOpenContentProperty(uri, property);
    }

    public Property getOpenContentProperty(String uri, String propertyName) {
        return getSDOTypeHelperDelegate().getOpenContentProperty(uri, propertyName);
    }
    
    private SDOTypeHelperDelegate getSDOTypeHelperDelegate() {
        return (SDOTypeHelperDelegate) SDOHelperContext.getHelperContext().getTypeHelper();
    }

    /**
      * INTERNAL:
      * Add the given namespace uri and prefix to the global namespace resolver.
      */
    public String addNamespace(String prefix, String uri) {
        return getSDOTypeHelperDelegate().addNamespace(prefix, uri);
    }

    /**
      * INTERNAL:
      * Return the prefix for the given uri, or generate a new one if necessary
      */
    public String getPrefix(String uri) {
        return getSDOTypeHelperDelegate().getPrefix(uri);
    }

    /**
      * INTERNAL:
      * Return the NamespaceResolver
      */
    public NamespaceResolver getNamespaceResolver() {
        return getSDOTypeHelperDelegate().getNamespaceResolver();
    }

    /**
    * INTERNAL:
    * Return the Map of Open Content Properties
    */
    public Map getOpenContentProperties() {
        return getSDOTypeHelperDelegate().getOpenContentProperties();
    }

    public void addWrappersToProject(Project toplinkProject) {
        getSDOTypeHelperDelegate().addWrappersToProject(toplinkProject);
    }

    public Map getInterfacesToSDOTypeHashMap() {
        return getSDOTypeHelperDelegate().getInterfacesToSDOTypeHashMap();
    }

}