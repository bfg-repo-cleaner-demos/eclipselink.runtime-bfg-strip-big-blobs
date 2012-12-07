/*******************************************************************************
 * Copyright (c) 1998, 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.eclipse.persistence.oxm;

import javax.xml.namespace.QName;

/**
 * <p>XMLConstants maintains a list of useful XML constants.</p>
 *
 * <p>This includes constants for built-in schema types as well as QNames
 * which represent those built-in schema types.  These QName constants can be used,
 * for example, when adding conversion pairs to XMLFields and when adding
 * schema types to an XMLUnionField.</p>
 *
 * <p><em>Code Sample</em></p>
 *
 * <p><code>
 * XMLUnionField unionField = new XMLUnionField("myElement");<br>
 * unionField.addSchemaType(XMLConstants.INTEGER_QNAME);<br>
 * unionField.addSchemaType(XMLConstants.DATE_QNAME);
 * </code></p>
 */
public class XMLConstants {

    public static final char COLON = ':';
    public static final char DOT = '.';	
    public static final Character ATTRIBUTE = '@';
    public static final String XPATH_SEPARATOR = "/";
    public static final String TEXT = "text()";
    public static final String EMPTY_STRING = "";
    public static final String CDATA = "CDATA";
    public static final String SCHEMA_PREFIX = "xsd";
    public static final String SCHEMA_URL = javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
    public static final String SCHEMA_INSTANCE_PREFIX = "xsi";
    public static final String SCHEMA_INSTANCE_URL = javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
    public static final String TARGET_NAMESPACE_PREFIX = "toplinktn";
    public static final String NO_NS_SCHEMA_LOCATION = "noNamespaceSchemaLocation";
    public static final String SCHEMA_LOCATION = "schemaLocation";
    public static final String XMLNS = javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
    public static final String XMLNS_URL = javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
    public static final String XML_NAMESPACE_PREFIX = javax.xml.XMLConstants.XML_NS_PREFIX;
    public static final String XML_NAMESPACE_URL = javax.xml.XMLConstants.XML_NS_URI;
    public static final String XML_NAMESPACE_SCHEMA_LOCATION = "http://www.w3.org/XML/2001/xml.xsd";
    public static final String SCHEMA_TYPE_ATTRIBUTE = "type";
    public static final String SCHEMA_NIL_ATTRIBUTE = "nil";
    public static final String REF_URL = "http://ws-i.org/profiles/basic/1.1/xsd";
    public static final String REF_PREFIX = "ref";
    public static final String SWAREF_XSD = "http://ws-i.org/profiles/basic/1.1/swaref.xsd";
    public static final String XOP_URL = "http://www.w3.org/2004/08/xop/include";
    public static final String XOP_PREFIX = "xop";
    public static final Class QNAME_CLASS = QName.class;
    public static final String DEFAULT_XML_ENCODING = "UTF-8";
    public final static String CONTENT_TYPE = "contentType";
    public static final String EXPECTED_CONTENT_TYPES = "expectedContentTypes";
    public static final String XML_MIME_URL = "http://www.w3.org/2005/05/xmlmime";
    public static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";

    // Built-in Schema Types
    public static final String ANY = "any";
    public static final String BASE_64_BINARY = "base64Binary";
    public static final String BOOLEAN = "boolean";
    public static final String BYTE = "byte";
    public static final String DATE = "date";
    public static final String DATE_TIME = "dateTime";
    public static final String DECIMAL = "decimal";
    public static final String DOUBLE = "double";
    public static final String DURATION = "duration";
    public static final String FLOAT = "float";
    public static final String G_DAY = "gDay";
    public static final String G_MONTH = "gMonth";
    public static final String G_MONTH_DAY = "gMonthDay";
    public static final String G_YEAR = "gYear";
    public static final String G_YEAR_MONTH = "gYearMonth";
    public static final String HEX_BINARY = "hexBinary";
    public static final String INT = "int";
    public static final String INTEGER = "integer";
    public static final String LONG = "long";
    public static final String NAME = "Name";
    public static final String NCNAME = "NCName";
    public static final String NEGATIVE_INTEGER = "negativeInteger";
    public static final String NON_NEGATIVE_INTEGER = "nonNegativeInteger";
    public static final String NON_POSITIVE_INTEGER = "nonPositiveInteger";
    public static final String POSITIVE_INTEGER = "positiveInteger";
    public static final String NORMALIZED_STRING = "normalizedString";
    public static final String QNAME = "QName";
    public static final String QUALIFIED = "qualified";
    public static final String SHORT = "short";
    public static final String STRING = "string";
    public static final String TIME = "time";
    public static final String UNQUALIFIED = "unqualified";
    public static final String UNSIGNED_BYTE = "unsignedByte";
    public static final String UNSIGNED_INT = "unsignedInt";
    public static final String UNSIGNED_SHORT = "unsignedShort";
    public static final String UNSIGNED_LONG = "unsignedLong";
    public static final String ANY_SIMPLE_TYPE = "anySimpleType";
    public static final String ANY_TYPE = "anyType";
    public static final String ANY_URI = "anyURI";
    public static final String SWA_REF = "swaRef";
    public static final String UNKNOWN_OR_TRANSIENT_CLASS = "UNKNOWN_OR_TRANSIENT_CLASS";

    // Schema Type QNames
    public static final QName ANY_QNAME = new QName(SCHEMA_URL, ANY);
    public static final QName ANY_SIMPLE_TYPE_QNAME = new QName(SCHEMA_URL, ANY_SIMPLE_TYPE);
    public static final QName ANY_TYPE_QNAME = new QName(SCHEMA_URL, ANY_TYPE);
    public static final QName ANY_URI_QNAME = new QName(SCHEMA_URL, ANY_URI);
    public static final QName BASE_64_BINARY_QNAME = new QName(SCHEMA_URL, BASE_64_BINARY);
    public static final QName HEX_BINARY_QNAME = new QName(SCHEMA_URL, HEX_BINARY);
    public static final QName DATE_QNAME = new QName(SCHEMA_URL, DATE);
    public static final QName TIME_QNAME = new QName(SCHEMA_URL, TIME);
    public static final QName DATE_TIME_QNAME = new QName(SCHEMA_URL, DATE_TIME);
    public static final QName BOOLEAN_QNAME = new QName(SCHEMA_URL, BOOLEAN);
    public static final QName BYTE_QNAME = new QName(SCHEMA_URL, BYTE);
    public static final QName DECIMAL_QNAME = new QName(SCHEMA_URL, DECIMAL);
    public static final QName DOUBLE_QNAME = new QName(SCHEMA_URL, DOUBLE);
    public static final QName DURATION_QNAME = new QName(SCHEMA_URL, DURATION);
    public static final QName FLOAT_QNAME = new QName(SCHEMA_URL, FLOAT);
    public static final QName G_DAY_QNAME = new QName(SCHEMA_URL, G_DAY);
    public static final QName G_MONTH_QNAME = new QName(SCHEMA_URL, G_MONTH);
    public static final QName G_MONTH_DAY_QNAME = new QName(SCHEMA_URL, G_MONTH_DAY);
    public static final QName G_YEAR_QNAME = new QName(SCHEMA_URL, G_YEAR);
    public static final QName G_YEAR_MONTH_QNAME = new QName(SCHEMA_URL, G_YEAR_MONTH);
    public static final QName INT_QNAME = new QName(SCHEMA_URL, INT);
    public static final QName INTEGER_QNAME = new QName(SCHEMA_URL, INTEGER);
    public static final QName LONG_QNAME = new QName(SCHEMA_URL, LONG);
    public static final QName NAME_QNAME = new QName(SCHEMA_URL, NAME);
    public static final QName NCNAME_QNAME = new QName(SCHEMA_URL, NCNAME);
    public static final QName NEGATIVE_INTEGER_QNAME = new QName(SCHEMA_URL, NEGATIVE_INTEGER);
    public static final QName NON_NEGATIVE_INTEGER_QNAME = new QName(SCHEMA_URL, NON_NEGATIVE_INTEGER);
    public static final QName NON_POSITIVE_INTEGER_QNAME = new QName(SCHEMA_URL, NON_POSITIVE_INTEGER);
    public static final QName POSITIVE_INTEGER_QNAME = new QName(SCHEMA_URL, POSITIVE_INTEGER);    
    public static final QName NORMALIZEDSTRING_QNAME = new QName(SCHEMA_URL, NORMALIZED_STRING);
    public static final QName QNAME_QNAME = new QName(SCHEMA_URL, QNAME);
    public static final QName SHORT_QNAME = new QName(SCHEMA_URL, SHORT);
    public static final QName STRING_QNAME = new QName(SCHEMA_URL, STRING);
    public static final QName UNSIGNED_BYTE_QNAME = new QName(SCHEMA_URL, UNSIGNED_BYTE);
    public static final QName UNSIGNED_INT_QNAME = new QName(SCHEMA_URL, UNSIGNED_INT);
    public static final QName UNSIGNED_SHORT_QNAME = new QName(SCHEMA_URL, UNSIGNED_SHORT);
    public static final QName UNSIGNED_LONG_QNAME = new QName(SCHEMA_URL, UNSIGNED_LONG);
    public static final QName SWA_REF_QNAME = new QName(REF_URL, SWA_REF);
    public static final QName EXPECTED_CONTENT_TYPES_QNAME = new QName(XML_MIME_URL, EXPECTED_CONTENT_TYPES);

    public static final char[] EMPTY_CHAR_ARRAY = new char[0];

    // Schema Special values for Double and Float
    public static final String POSITIVE_INFINITY = "INF";
    public static final String NEGATIVE_INFINITY = "-INF";

    public static final String BOOLEAN_STRING_TRUE = "true";
    public static final String BOOLEAN_STRING_FALSE = "false";

    public static final String ANY_NAMESPACE_ANY = "##any";
    public static final String ANY_NAMESPACE_LOCAL = "##local";
    public static final String ANY_NAMESPACE_OTHER = "##other";
    public static final String ANY_NAMESPACE_TARGETNS = "##targetNamespace";

    public static final Class UUID = java.util.UUID.class;
    public static final Class URI = java.net.URI.class;
    public static final Class LOCATOR_CLASS = org.xml.sax.Locator.class;
    public static final String LOCATOR_CLASS_NAME = "org.xml.sax.Locator";

    public static final String VALUE_WRAPPER = "value";

    public static final String JAXB_FRAGMENT = "jaxb.fragment";
    public static final String JAXB_MARSHALLER = "jaxb.marshaller";

}