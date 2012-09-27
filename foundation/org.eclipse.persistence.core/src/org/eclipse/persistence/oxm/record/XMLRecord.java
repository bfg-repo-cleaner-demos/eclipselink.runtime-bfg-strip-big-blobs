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
package org.eclipse.persistence.oxm.record;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.oxm.XPathQName;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.oxm.MediaType;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.XMLMarshaller;
import org.eclipse.persistence.oxm.XMLUnmarshaller;
import org.eclipse.persistence.oxm.documentpreservation.DocumentPreservationPolicy;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * PUBLIC:
 * Provides a Record/Map API on an XML DOM element.
 */
public abstract class XMLRecord extends AbstractRecord {
    protected XMLMarshaller marshaller;
    protected XMLUnmarshaller unmarshaller;
    private DocumentPreservationPolicy docPresPolicy;
    private Object owningObject;
    protected Object currentObject;
    private XPathQName leafElementType;
    protected NamespaceResolver namespaceResolver;
    protected AbstractSession session;
    private boolean isXOPPackage;
    protected boolean namespaceAware;

    protected boolean hasCustomNamespaceMapper;
    protected boolean equalNamespaceResolvers = false;

    /**
     * INTERNAL:
     * Nil: This is used to indicate that this field represents xsi:nil="true"
     */
    public static final XMLRecord.Nil NIL = new XMLRecord.Nil();

    /**
     * INTERNAL:
     * Nil: This is used to indicate that this field represents xsi:nil="true"
     */
    private static class Nil {
        private Nil() {
        }
    }

    public XMLRecord() {
        super(null, null);
        namespaceAware = true;
        // Required for subclasses.
    }

    /**
     * PUBLIC:
     * Add the field-value pair to the row.
     */
    @Override
    public Object get(String key) {
        return get(new XMLField(key));
    }

    /**
     * PUBLIC:
     * Add the field-value pair to the row.
     */
    @Override
    public Object put(String key, Object value) {
        return put(new XMLField(key), value);
    }
    
    
    /**
     * Marshal an attribute for the give namespaceURI, localName, preifx and value
     * @param namespaceURI
     * @param localName
     * @param prefix     
     * @param value
     */
    public void attributeWithoutQName(String namespaceURI, String localName, String prefix, String value){
        String qualifiedName = localName;
        if(prefix != null && prefix.length() >0){
            qualifiedName = prefix + getNamespaceSeparator() + qualifiedName;
        }
        attribute(namespaceURI, localName, qualifiedName, value);
    }
       
    /**
     * Marshal an attribute for the give namespaceURI, localName, qualifiedName and value
     * @param namespaceURI
     * @param localName
     * @param qName     
     * @param value
     */
    public void attribute(String namespaceURI, String localName, String qName, String value){
        XMLField xmlField = new XMLField(XMLConstants.ATTRIBUTE +qName);
        xmlField.setNamespaceResolver(getNamespaceResolver());
        xmlField.getLastXPathFragment().setNamespaceURI(namespaceURI);
        add(xmlField, value);
    }
    
    /**
     * Marshal a namespace declaration for the given prefix and url
     * @param prefix
     * @param url
     */
    public void namespaceDeclaration(String prefix, String namespaceURI){
        
        String existingPrefix = getNamespaceResolver().resolveNamespaceURI(namespaceURI);
        if(existingPrefix == null || (existingPrefix != null && !existingPrefix.equals(XMLConstants.EMPTY_STRING) && !existingPrefix.equals(prefix))){        
            XMLField xmlField = new XMLField("@" + XMLConstants.XMLNS + XMLConstants.COLON + prefix);
            xmlField.setNamespaceResolver(getNamespaceResolver());
            xmlField.getXPathFragment().setNamespaceURI(XMLConstants.XMLNS_URL);
            add(xmlField, namespaceURI);
        }
    }
    
    /**
     * PUBLIC:
     * Get the local name of the context root element.
     */
    public abstract String getLocalName();

    /**
     * PUBLIC:
     *  Get the namespace URI for the context root element.
     */
    public abstract String getNamespaceURI();

    /**
     * PUBLIC:
     * Clear the sub-nodes of the DOM.
     */
    public abstract void clear();

    /**
     * PUBLIC:
     * Return the document.
     */
    public abstract Document getDocument();

    /**
     * PUBLIC:
     * Check if the value is contained in the row.
     */
    public boolean contains(Object value) {
        return values().contains(value);
    }

    /**
    * PUBLIC:
    * Return the DOM.
    */
    public abstract Node getDOM();

    /**
     * Return the XML string representation of the DOM.
     */
    public abstract String transformToXML();

    /**
     * INTERNAL:
     * Convert a DatabaseField to an XMLField
     */
    protected XMLField convertToXMLField(DatabaseField databaseField) {
        try {
            return (XMLField)databaseField;
        } catch (ClassCastException ex) {
            return new XMLField(databaseField.getName());
        }
    }
    
    protected List<XMLField> convertToXMLField(List<DatabaseField> databaseFields) {
        ArrayList<XMLField> xmlFields = new ArrayList(databaseFields.size());
        for(DatabaseField next:databaseFields) {
            try {
                xmlFields.add((XMLField)next);
            } catch(ClassCastException ex) {
                xmlFields.add(new XMLField(next.getName()));
            }
        }
        return xmlFields;
    }

    /**
     * INTERNAL:
     * Retrieve the value for the field. If missing null is returned.
     */
    public Object get(DatabaseField key) {
        return getIndicatingNoEntry(key);
    }
    /**
     * INTERNAL:
     * Retrieve the value for the field name.
     */
    public Object getIndicatingNoEntry(String fieldName) {
        return getIndicatingNoEntry(new XMLField(fieldName));
    }

    public String resolveNamespacePrefix(String prefix) {
        return null;
    }

    /**
     * INTERNAL:
     */
    public XMLMarshaller getMarshaller() {
        return marshaller;
    }

    /**
     * INTERNAL:
     */
    public void setMarshaller(XMLMarshaller marshaller) {
        this.marshaller = marshaller;
        if(marshaller != null){
            MediaType mediaType = marshaller.getMediaType();
            if(marshaller.getNamespacePrefixMapper() != null){
            	namespaceAware = true;             	
            }else{
            	namespaceAware = mediaType == MediaType.APPLICATION_XML;
            }
        }
    }

    /**
     * INTERNAL:
     */
    public XMLUnmarshaller getUnmarshaller() {
        return unmarshaller;
    }

    /**
     * INTERNAL:
     */
    public void setUnmarshaller(XMLUnmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    public void setDocPresPolicy(DocumentPreservationPolicy policy) {
        this.docPresPolicy = policy;
    }
    
    public DocumentPreservationPolicy getDocPresPolicy() {
        return docPresPolicy;
    }
    /**
     * INTERNAL:
     */
    public Object getOwningObject() {
        return owningObject;
    }

    /**
     * INTERNAL:
     */
    public void setOwningObject(Object obj) {
        this.owningObject = obj;
    }

    /**
     * INTERNAL:
     */
    public Object getCurrentObject() {
        return currentObject;
    }

    /**
     * INTERNAL:
     */
    public void setCurrentObject(Object obj) {
        this.currentObject = obj;
    }
    /**
     * INTERNAL:
     */
    public XPathQName getLeafElementType() {
        return leafElementType;
    }
    /**
     * INTERNAL:
     */
    public void setLeafElementType(XPathQName type) {
        leafElementType = type;
    }

    /**
     * INTERNAL:
     */
    public void setLeafElementType(QName type) {
    	if(type != null){
    	    setLeafElementType(new XPathQName(type, isNamespaceAware()));
    	}
    }
    
    public void setNamespaceResolver(NamespaceResolver nr) {
        namespaceResolver = nr;
    }

    public NamespaceResolver getNamespaceResolver() {
        return namespaceResolver;
    }

    public AbstractSession getSession() {
        return session;
    }

    public void setSession(AbstractSession session) {
        this.session = session;
    }

    public void setEqualNamespaceResolvers(boolean equalNRs) {
        this.equalNamespaceResolvers = equalNRs;
    }

    public boolean hasEqualNamespaceResolvers() {
        return equalNamespaceResolvers;
    }

    public boolean isXOPPackage() {
        return isXOPPackage;
    }

    public void setXOPPackage(boolean isXOPPackage) {
        this.isXOPPackage = isXOPPackage;
    }
    
    /**
     * INTERNAL:
     * Determine if namespaces will be considered during marshal/unmarshal operations.
     * @since 2.4
     */
    public boolean isNamespaceAware() {
    	return namespaceAware;    	
    }
    
    /**
     * INTERNAL:
	 * The character used to separate the prefix and uri portions when namespaces are present 
     * @since 2.4
     */    
    public char getNamespaceSeparator(){
    	return XMLConstants.COLON;
    }
	
    public boolean hasCustomNamespaceMapper() {
    	return hasCustomNamespaceMapper;    	
    }

    public void setCustomNamespaceMapper(boolean customNamespaceMapper) {
        this.hasCustomNamespaceMapper = customNamespaceMapper;
    }
    
}
