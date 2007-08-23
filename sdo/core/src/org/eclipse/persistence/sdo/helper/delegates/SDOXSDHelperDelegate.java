/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.sdo.helper.delegates;

import commonj.sdo.Property;
import commonj.sdo.Type;
import commonj.sdo.helper.HelperContext;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.eclipse.persistence.sdo.SDOConstants;
import org.eclipse.persistence.sdo.SDOProperty;
import org.eclipse.persistence.sdo.SDOType;
import org.eclipse.persistence.sdo.helper.DefaultSchemaResolver;
import org.eclipse.persistence.sdo.helper.SDOSchemaGenerator;
import org.eclipse.persistence.sdo.helper.SDOTypesGenerator;
import org.eclipse.persistence.sdo.helper.SDOXSDHelper;
import org.eclipse.persistence.sdo.helper.SchemaLocationResolver;
import org.eclipse.persistence.sdo.helper.SchemaResolver;
import org.eclipse.persistence.exceptions.SDOException;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.platform.xml.XMLPlatformFactory;
import org.w3c.dom.Element;

/**
 * <p><b>Purpose</b>: Provides access to additional information when the Type or Property is defined by an XML Schema (XSD)..
 * <p><b>Responsibilities</b>:<ul>
 * <li> Define methods defines Types from an XSD.
 * <li> Generate methods an XSD from Types.
 * <li> Other Methods return null/false otherwise or if the information is unavailable.
 * </ul>
 */
public class SDOXSDHelperDelegate implements SDOXSDHelper {

    /*Map of global attributes keyed on qname*/
    private Map globalAttributes;

    /*Map of global elements keyed on qname*/
    private Map globalElements;

    // hold the context containing all helpers so that we can preserve inter-helper relationships
    private HelperContext aHelperContext;

    public SDOXSDHelperDelegate(HelperContext aContext) {
        aHelperContext = aContext;
    }

    /**
     * Returns the local name as declared in the XSD.
     * @param type to return local name for.
     * @return the local name as declared in the XSD.
     */
    public String getLocalName(Type type) {
        return ((SDOType)type).getXsdLocalName();
    }

    /**
     * Returns the local name as declared in the XSD.
     * @param property to return local name for.
     * @return the local name as declared in the XSD.
     */
    public String getLocalName(Property property) {
        return ((SDOProperty)property).getXsdLocalName();
    }

    /**
     * Returns the namespace URI as declared in the XSD.
     * @param property to return namespace URI for.
     * @return the namespace URI as declared in the XSD.
     */
    public String getNamespaceURI(Property property) {
        return property.getContainingType().getURI();
    }

    /**
     * Returns true if the property is declared as an attribute in the XSD.
     * Returns false if not known or for advanced cases.
     * It is possible for both isAttribute and isElement to return false
     * but they will not both return true.
     * @param property to identify if an attribute.
     * @return true if the property is declared as an attribute in the XSD.
     */
    public boolean isAttribute(Property property) {
        Object value = property.get(SDOConstants.XMLELEMENT_PROPERTY);
        if ((value != null) && value instanceof Boolean) {
            boolean isElement = ((Boolean)value).booleanValue();
            if (isElement) {
                return false;
            }
        }

        if ((property.getOpposite() != null) && (property.getOpposite().isContainment())) {
            return false;
        } else if (property.isMany() || property.isContainment() || property.isNullable()) {
            return false;
        }
        // Case: open content non-attribute property
        return true;
    }

    /**
     * Returns true if the property is declared as an element in the XSD.
     * Returns false if not known or for advanced cases.
     * It is possible for both isAttribute and isElement to return false
     * but they will not both return true.
     * @param property to identify if an element.
     * @return true if the property is declared as an element in the XSD.
     */
    public boolean isElement(Property property) {
        Object value = property.get(SDOConstants.XMLELEMENT_PROPERTY);
        if ((value != null) && value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        }

        if ((property.getOpposite() != null) && (property.getOpposite().isContainment())) {
            return false;
        } else if (property.isMany() || property.isContainment() || property.isNullable()) {
            return true;
        }

        //TODO: what is this case? else if nextProperty.get(xmlElement) != null and nextProperty.get(xmlElement) ==true
        // Case: open content non-element property
        return false;

        //TODO:???undefined
        //return false;                      
    }

    /**
     * Returns true if the Type is declared to contain mixed content.
     * A DataObject's mixed content values are typically accessed via a Sequence.
     * @param type to identify if mixed content.
     * @return true if the Type is declared to contain mixed content.
     */
    public boolean isMixed(Type type) {
        return type.isSequenced();
    }

    /**
     * Indicates if this helper contains XSD information for the specified type.
     * @param type the type.
     * @return true if this helper contains XSD information for the specified type.
     */
    public boolean isXSD(Type type) {
        return ((SDOType)type).isXsd();
    }

    /**
     * Returns the Property defined by the named global element or attribute
     *   in the targetNamespace uri, or null if not found.
     * @param uri The uri of the targetNamespace.
     * @param propertyName The name of the global property.
     * @param isElement is true for global elements, false for global attributes.
     * @return the Property defined by the named global element or attribute
     *    in the targetNamespace uri, or null if not found.
     */
    public Property getGlobalProperty(String uri, String propertyName, boolean isElement) {
        QName qname = new QName(uri, propertyName);
        return getGlobalProperty(qname, isElement);
    }

    public Property getGlobalProperty(QName qname, boolean isElement) {
        if (isElement) {
            return (Property)getGlobalElements().get(qname);
        } else {
            return (Property)getGlobalAttributes().get(qname);
        }
    }

    /**
     * Return the appinfo declared for this Type and source.
     * The appinfo start and end tags and content are returned.
     * The xml namespace context is preserved in the appinfo element.
     * If more than one appinfo with the same source is declared on the same
     * Type their contents are concatenated.
     * @param type the type with the appinfo declaration
     * @param source the source of the appinfo declaration.
     * @return the appinfo declared for this Type and source.
     */
    public String getAppinfo(Type type, String source) {
        if (type == null) {
            throw SDOException.noAppInfoForNull();
        }
        if (source == null) {
            source = "";
        }
        return (String)((SDOType)type).getAppInfoMap().get(source);
    }

    /**
     * Return the content of the appinfo declared for this Property and source.
     * If the property is defined by ref= the appinfo of the referenced
     * element or attribute is included.
     * The appinfo start and end tags and content are returned.
     * The xml namespace context is preserved in the appinfo element.
     * If more than one appinfo with the same source is declared on the same
     * Type their contents are concatenated.
     * @param property the Property with the appinfo declaration
     * @param source the source of the appinfo declaration.
     * @return the appinfo declared for this Property and source.
     */
    public String getAppinfo(Property property, String source) {
        if (property == null) {
            throw SDOException.noAppInfoForNull();
        }
        if (source == null) {
            source = "";
        }
        return (String)((SDOProperty)property).getAppInfoMap().get(source);
    }

    /**
     * Define the XML Schema as Types.
     * The Types are available through TypeHelper and DataGraph getType() methods.
     * Same as define(new StringReader(xsd), null)
     * @param xsd the XML Schema.
     * @return the defined Types.
     * @throws IllegalArgumentException if the Types could not be defined.
     */
    public List define(String xsd) {
        StringReader reader = new StringReader(xsd);
        SchemaResolver schemaResolver = new DefaultSchemaResolver();
        return define(reader, schemaResolver);
    }

    /**
     * Define XML Schema as Types.
     * The Types are available through TypeHelper and DataGraph getType() methods.
     * @param xsdReader reader to an XML Schema.
     * @param schemaLocation the URI of the location of the schema, used
     *    for processing relative imports and includes.  May be null if not used.
     * @return the defined Types.
     * @throws IllegalArgumentException if the Types could not be defined.
     */
    public List define(Reader xsdReader, String schemaLocation) {
        DefaultSchemaResolver schemaResolver = new DefaultSchemaResolver();
        schemaResolver.setBaseSchemaLocation(schemaLocation);

        return define(new StreamSource(xsdReader), schemaResolver);
    }

    /**
     * Define XML Schema as Types.
     * The Types are available through TypeHelper and DataGraph getType() methods.
     * @param xsdReader reader to an XML Schema.
     * @param schemaLocation the URI of the location of the schema, used
     *    for processing relative imports and includes.  May be null if not used.
     * @return the defined Types.
     * @throws IllegalArgumentException if the Types could not be defined.
     */
    public List define(Reader xsdReader, SchemaResolver schemaResolver) {
        return define(new StreamSource(xsdReader), schemaResolver);
    }

    public List define(Source xsdSource, SchemaResolver schemaResolver) {
        return new SDOTypesGenerator(aHelperContext).define(xsdSource, schemaResolver);
    }

    /**
     * Define XML Schema as Types.
     * The Types are available through TypeHelper and DataGraph getType() methods.
     * @param xsdInputStream input stream to an XML Schema.
     * @param schemaLocation the URI of the location of the schema, used
     *    for processing relative imports and includes.  May be null if not used.
     * @return the defined Types.
     * @throws IllegalArgumentException if the Types could not be defined.
     */
    public List define(InputStream xsdInputStream, String schemaLocation) {
        InputStreamReader xsdReader = new InputStreamReader(xsdInputStream);
        return define(xsdReader, schemaLocation);
    }

    /**
     * Generate an XML Schema Declaration (XSD) from Types.
     * Same as generate(types, null);
     * @param types a List containing the Types
     * @return a String containing the generated XSD.
     * @throws IllegalArgumentException if the XSD could not be generated.
     */
    public String generate(List types) {
        HashMap map = null;
        return generate(types, map);
    }

    /**
     * Generate an XML Schema Declaration (XSD) from Types.
     * Round trip from SDO to XSD to SDO is supported.
     * Round trip from XSD to SDO to XSD is not supported.
     *  Use the original schema if one exists instead of generating a new one, as
     *  the generated XSD validates a different set of documents than the original XSD.
     * Generating an XSD does not affect the XSDHelper or the Types.
     * The Types must all have the same URI.
     * The result is a String containing the generated XSD.
     * All Types referenced with the same URI will be generated in the XSD
     *  and the list will be expanded to include all types generated.
     * Any Types referenced with other URIs will cause
     *  imports to be produced as appropriate.
     * Imports will include a schemaLocation if a Map is provided with an entry
     *  of the form key=import target namespace, value=schemaLocation
     * @param types a List containing the Types
     * @param namespaceToSchemaLocation map of target namespace to schema locations or null
     * @return a String containing the generated XSD.
     * @throws IllegalArgumentException if the XSD could not be generated.
     */
    public String generate(List types, Map namespaceToSchemaLocation) {
        return new SDOSchemaGenerator(aHelperContext).generate(types, namespaceToSchemaLocation);
    }

    public String generate(List types, SchemaLocationResolver schemaLocationResolver) {
        return new SDOSchemaGenerator(aHelperContext).generate(types, schemaLocationResolver);
    }

    /**
      * Assign a map of properties representing global attributes keyed on QName
      * @param globalAttributes  a Map of global elements keyed on QName
      */
    public void setGlobalAttributes(Map globalAttributes) {
        this.globalAttributes = globalAttributes;
    }

    /**
      * Return a map of properties representing global attributes keyed on QName
      * @return a map of global attributes keyed on QName
      */
    public Map getGlobalAttributes() {
        if (globalAttributes == null) {
            globalAttributes = new HashMap();
        }
        return globalAttributes;
    }

    /**
      * Assign a map of properties representing global elements keyed on QName
      * @param globalElements  a Map of global elements keyed on QName
      */
    public void setGlobalElements(Map globalElements) {
        this.globalElements = globalElements;
    }

    /**
      * Return a map of properties representing global elements keyed on QName
      * @return a map of global elements keyed on QName
      */
    public Map getGlobalElements() {
        if (globalElements == null) {
            globalElements = new HashMap();
        }
        return globalElements;
    }

    public Map buildAppInfoMap(List appInfoElements) {
        HashMap appInfoMap = new HashMap();

        //build AppInfoMap
        if (appInfoElements != null) {
            for (int i = 0; i < appInfoElements.size(); i++) {
                Element nextElement = (Element)appInfoElements.get(i);

                if (nextElement.getNamespaceURI().equals(XMLConstants.SCHEMA_URL) && nextElement.getLocalName().equals("appinfo")) {
                    String key = nextElement.getAttribute(SDOConstants.APPINFO_SOURCE_ATTRIBUTE);
                    String value = (String)appInfoMap.get(key);
                    if (value == null) {
                        StringWriter sw = new StringWriter();
                        XMLPlatformFactory.getInstance().getXMLPlatform().newXMLTransformer().transform(nextElement, sw);
                        appInfoMap.put(key, sw.toString());
                    } else {
                        //need to concatenate Strings
                        StringWriter sw = new StringWriter();
                        XMLPlatformFactory.getInstance().getXMLPlatform().newXMLTransformer().transform(nextElement, sw);
                        String concat = value + sw.toString();
                        appInfoMap.put(key, concat);
                    }
                }
            }
        }
        return appInfoMap;
    }

    public void reset() {
        getGlobalAttributes().clear();
        getGlobalElements().clear();
    }

    public HelperContext getHelperContext() {
        return aHelperContext;
    }

    public void setHelperContext(HelperContext helperContext) {
        aHelperContext = helperContext;
    }
}