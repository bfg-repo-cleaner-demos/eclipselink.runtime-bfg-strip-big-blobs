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
 ******************************************************************************/  
package org.eclipse.persistence.internal.oxm;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.QName;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.InheritancePolicy;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.record.XMLRecord;

/**
 * INTERNAL:
 * <p><b>Purpose</b>: A Subclass of Inheritance Policy to be used with XML
 * Descriptors. If the class indicator field is an xsi:type, the value of that
 * field may be a qualified type name. For example xsi:type="myns:my-type-name".
 * Since any given XML document can use different prefixes for these namespaces,
 * we must be able to find the class based on QName instead of just the string
 * "myns:my-type-name".</p>
 * @author  mmacivor
 * @since   10.1.3
 */
public class QNameInheritancePolicy extends InheritancePolicy {
    //used for initialization. Prefixed type names will be changed to QNames.
    private NamespaceResolver namespaceResolver;

    public QNameInheritancePolicy() {
        super();
    }

    public QNameInheritancePolicy(ClassDescriptor desc) {
        super(desc);
    }
    
    /**
     * Override to control order of uniqueTables, child tablenames should be first since 
     * getDefaultRootElement on an XMLDescriptor will return the first table.
     */
    protected void updateTables(){
        // Unique is required because the builder can add the same table many times.
        Vector<DatabaseTable> childTables = getDescriptor().getTables();
        Vector<DatabaseTable> parentTables = getParentDescriptor().getTables();
        Vector<DatabaseTable> uniqueTables = Helper.concatenateUniqueVectors(childTables, parentTables);
        getDescriptor().setTables(uniqueTables);
        
        
        // After filtering out any duplicate tables, set the default table
        // if one is not already set. This must be done now before any other
        // initialization occurs. In a joined strategy case, the default 
        // table will be at an index greater than 0. Which is where
        // setDefaultTable() assumes it is. Therefore, we need to send the 
        // actual default table instead.
        if (childTables.isEmpty()) {
            getDescriptor().setInternalDefaultTable();
        } else {
            getDescriptor().setInternalDefaultTable(uniqueTables.get(uniqueTables.indexOf(childTables.get(0))));
        }
    }

    /**
     * INTERNAL:
     * Initialized the inheritance properties of the descriptor once the mappings are initialized.
     * This is done before formal postInitialize during the end of mapping initialize.
     */
    public void initialize(AbstractSession session) {
        super.initialize(session);

        // If we have a namespace resolver, check any of the class-indicator values
        // for prefixed type names and resolve the namespaces.
        if (!this.shouldUseClassNameAsIndicator() && (namespaceResolver != null)) {
            // Must first clone the map to avoid concurrent modification.
            Iterator<Map.Entry> entries = new HashMap(getClassIndicatorMapping()).entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = entries.next();
                Object key = entry.getKey();
                if (key instanceof String) {
                    QName qname;
                    String indicatorValue = (String)key;
                    int index = indicatorValue.indexOf(XMLConstants.COLON);
                    if (index != -1) {
                        //if it's a prefixed string, key it on QName and 
                        //local name, in case the namespace can't be resolved
                        //at runtime. Needs to be revisited.
                        String prefix = indicatorValue.substring(0, index);
                        String localPart = indicatorValue.substring(index + 1);
                        String uri = namespaceResolver.resolveNamespacePrefix(prefix);
                        qname = new QName(uri, localPart);
                    } else {
                        // we always want to create/insert QNames into the map
                        if (namespaceResolver != null) {
                            qname = new QName(namespaceResolver.getDefaultNamespaceURI(),indicatorValue);
                        }
                        else {
                            qname = new QName(indicatorValue);
                        }
                    }
                    getClassIndicatorMapping().put(qname, entry.getValue());
                }
            }
        }
        
        //bug 6012173 - changed to initialize namespare uri on indicator field
        //need to be able to compare uri and local name during marshal to see if field is xsi type field
        if(getClassIndicatorField() != null){
          XMLField classIndicatorXMLField;
          try {
              classIndicatorXMLField = (XMLField)getClassIndicatorField();
          } catch (ClassCastException ex) {
              classIndicatorXMLField = new XMLField(getClassIndicatorField().getName());            
              setClassIndicatorField(classIndicatorXMLField);
          }
          XPathFragment frag = classIndicatorXMLField.getLastXPathFragment();                        
          if ((frag != null) && frag.hasNamespace() && (namespaceResolver != null)) {
              String uri = namespaceResolver.resolveNamespacePrefix(frag.getPrefix());
              classIndicatorXMLField.getLastXPathFragment().setNamespaceURI(uri);
          }          
        }
    }

    /**
     * INTERNAL:
     * This method is invoked only for the abstract descriptors.
     */
    public Class classFromRow(AbstractRecord rowFromDatabase, AbstractSession session) throws DescriptorException {
        ((XMLRecord) rowFromDatabase).setSession(session);
        
        if (hasClassExtractor() || shouldUseClassNameAsIndicator()) {
            return super.classFromRow(rowFromDatabase, session);
        }
        Object indicator = rowFromDatabase.get(getClassIndicatorField());
        if (indicator == AbstractRecord.noEntry) {
            return null;
        }
        Object classFieldValue = session.getDatasourcePlatform().getConversionManager().convertObject(indicator, getClassIndicatorField().getType());

        if (classFieldValue == null) {
            return null;
        }

        Class concreteClass;
        if (classFieldValue instanceof String) {
            String indicatorValue = (String)classFieldValue;
            int index = indicatorValue.indexOf(XMLConstants.COLON);
            if (index == -1) {
                String uri = ((XMLRecord)rowFromDatabase).resolveNamespacePrefix(null);
                if(uri == null) {
                    concreteClass = (Class)this.classIndicatorMapping.get(classFieldValue);
                } else {
                    QName qname = new QName(uri, indicatorValue);
                    concreteClass = (Class)this.classIndicatorMapping.get(qname);
                }
            } else {
                String prefix = indicatorValue.substring(0, index);
                String localPart = indicatorValue.substring(index + 1);
                String uri = ((XMLRecord)rowFromDatabase).resolveNamespacePrefix(prefix);
                if (uri != null) {
                    QName qname = new QName(uri, localPart);
                    concreteClass = (Class)this.classIndicatorMapping.get(qname);
                } else {
                    concreteClass = (Class)this.classIndicatorMapping.get(indicatorValue);
                }
            }
        } else {
            concreteClass = (Class)this.classIndicatorMapping.get(classFieldValue);
        }
        if (concreteClass == null) {
            throw DescriptorException.missingClassForIndicatorFieldValue(classFieldValue, getDescriptor());
        }
        return concreteClass;
    }

    public void setNamespaceResolver(NamespaceResolver resolver) {
        this.namespaceResolver = resolver;
    }

    /**
     * PUBLIC:
     * To set the class indicator field name.
     * This is the name of the field in the table that stores what type of object this is.
     */
    public void setClassIndicatorFieldName(String fieldName) {
        if (fieldName == null) {
            setClassIndicatorField(null);
        } else {
            setClassIndicatorField(new XMLField(fieldName));
        }
    }
}
