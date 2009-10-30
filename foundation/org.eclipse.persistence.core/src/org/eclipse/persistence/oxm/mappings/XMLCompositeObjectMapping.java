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
package org.eclipse.persistence.oxm.mappings;

import java.lang.reflect.Modifier;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.descriptors.ObjectBuilder;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.internal.oxm.XMLObjectBuilder;
import org.eclipse.persistence.internal.oxm.XPathEngine;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.queries.JoinedAttributeManager;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.AttributeAccessor;
import org.eclipse.persistence.mappings.foundation.AbstractCompositeObjectMapping;
import org.eclipse.persistence.oxm.XMLConstants;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.mappings.converters.XMLConverter;
import org.eclipse.persistence.oxm.mappings.nullpolicy.AbstractNullPolicy;
import org.eclipse.persistence.oxm.mappings.nullpolicy.NullPolicy;
import org.eclipse.persistence.oxm.record.DOMRecord;
import org.eclipse.persistence.oxm.record.XMLRecord;
import org.eclipse.persistence.oxm.schema.XMLSchemaReference;
import org.eclipse.persistence.platform.xml.XMLPlatformFactory;
import org.eclipse.persistence.queries.ObjectBuildingQuery;

/**
 * <p>Composite object XML mappings represent a relationship between two classes.  In XML, the "owned"
 * class may be nested with the element tag representing the "owning" class.  This mapping is, by
 * definition, privately owned.
 *
 * <p><b>Composite object XML mappings can be used in the following scenarios</b>:<ul>
 * <li> Mapping into the Parent Record </li>
 * <li> Mapping to an Element </li>
 * <li> Mapping to Different Elements by Element Name </li>
 * <li> Mapping to Different Elements by Element Position </li>
 * </ul>
 *
 * <p><b>Setting the XPath</b>: TopLink XML mappings make use of XPath statements to find the relevant
 * data in an XML document.  The XPath statement is relative to the context node specified in the descriptor.
 * The XPath may contain path and positional information;  the last node in the XPath forms the local
 * root node for the composite object.  The XPath is specified on the mapping using the <code>setXPath</code>
 * method.
 *
 * <p>The following XPath statements may be used to specify the location of XML data relating to an object's
 * name attribute:
 *
 * <p><table border="1">
 * <tr>
 * <th id="c1" align="left">XPath</th>
 * <th id="c2" align="left">Description</th>
 * </tr>
 * <tr>
 * <td headers="c1">.</td>
 * <td headers="c2">Indicates "self".</td>
 * </tr>
 * <tr>
 * <td headers="c1">phone-number</td>
 * <td headers="c2">The phone-number information is stored in the phone-number element.</td>
 * </tr>
 * <tr>
 * <td headers="c1" nowrap="true">contact-info/phone-number</td>
 * <td headers="c2">The XPath statement may be used to specify any valid path.</td>
 * </tr>
 * <tr>
 * <td headers="c1">phone-number[2]</td>
 * <td headers="c2">The XPath statement may contain positional information.  In this case the phone-number
 * information is stored in the second occurrence of the phone-number element.</td>
 * </tr>
 * </table>
 *
 * <p><b>Mapping into the Parent Record</b>: The composite object may be mapped into the parent
 * record in a corresponding XML document.
 *
 * <!--
 *    <?xml version="1.0" encoding="UTF-8"?>
 *    <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema">
 *        <xsd:element name="customer" type="customer-type"/>
 *     <xsd:complexType name="customer-type">
 *            <xsd:sequence>
 *             <xsd:element name="first-name" type="xsd:string"/>
 *                <xsd:element name="last-name" type="xsd:string"/>
 *                <xsd:element name="street" type="xsd:string"/>
 *                <xsd:element name="city" type="xsd:string"/>
 *            </xsd:sequence>
 *        </xsd:complexType>
 *    </xsd:schema>
 * -->
 *
 * <p><em>XML Schema</em><br>
 * <code>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;<br>
 * &lt;xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;<br>
 * &nbsp;&nbsp;&lt;xsd:element name="customer" type="customer-type"/&gt;<br>
 * &nbsp;&nbsp;&lt;xsd:complexType name="customer-type"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="first-name" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="last-name" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="street" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="city" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&lt;/xsd:complexType&gt;<br>
 * &lt;/xsd:schema&gt;
 * </code>
 *
 * <p><em>Code Sample</em><br>
 * <code>
 * XMLCompositeObjectMapping addressMapping = new XMLCompositeObjectMapping();<br>
 * addressMapping.setAttributeName("address");<br>
 * addressMapping.setXPath(".");<br>
 * addressMapping.setReferenceClass(Address.class);<br>
 * </code>
 *
 * <p><b>Mapping to an Element</b>: The composite object may be mapped to an element in a corresponding
 * XML document.
 *
 * <!--
 * <?xml version="1.0" encoding="UTF-8"?>
 * <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema">
 *     <xsd:element name="customer" type="customer-type"/>
 *     <xsd:complexType name="customer-type">
 *         <xsd:sequence>
 *             <xsd:element name="first-name" type="xsd:string"/>
 *             <xsd:element name="last-name" type="xsd:string"/>
 *             <xsd:element name="address">
 *                 <xsd:complexType>
 *                     <xsd:sequence>
 *                         <xsd:element name="street" type="xsd:string"/>
 *                         <xsd:element name="city" type="xsd:string"/>
 *                     </xsd:sequence>
 *                 </xsd:complexType>
 *             </xsd:element>
 *         </xsd:sequence>
 *     </xsd:complexType>
 * </xsd:schema>
 * -->
 *
 * <p><em>XML Schema</em><br>
 * <code>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;<br>
 * &lt;xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"&gt;<br>
 * &nbsp;&nbsp;&lt;xsd:element name="customer" type="customer-type"/&gt;<br>
 * &nbsp;&nbsp;&lt;xsd:complexType name="customer-type"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="first-name" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="last-name" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="address"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:complexType&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="street" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;xsd:element name="city" type="xsd:string"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/xsd:complexType&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;/xsd:element&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&lt;/xsd:sequence&gt;<br>
 * &nbsp;&nbsp;&lt;/xsd:complexType&gt;<br>
 * &lt;/xsd:schema&gt;<br>
 * </code>
 *
 * <p><em>Code Sample</em><br>
 * <code>
 * XMLCompositeObjectMapping addressMapping = new XMLCompositeObjectMapping();<br>
 * addressMapping.setAttributeName("address");<br>
 * addressMapping.setXPath("address");<br>
 * addressMapping.setReferenceClass(Address.class);<br>
 * </code>
 *
 * <p><b>More Information</b>: For more information about using the XML Composite Object Mapping, see the
 * "Understanding XML Mappings" chapter of the Oracle TopLink Developer's Guide.
 *
 * @since Oracle TopLink 10<i>g</i> Release 2 (10.1.3)
 */
public class XMLCompositeObjectMapping extends AbstractCompositeObjectMapping implements XMLMapping, XMLNillableMapping {
    AbstractNullPolicy nullPolicy;
    private BidirectionalPolicy bidirectionalPolicy;
    private UnmarshalKeepAsElementPolicy keepAsElementPolicy;
    private boolean isWriteOnly;

    public XMLCompositeObjectMapping() {
        super();
        // The default policy is NullPolicy
        nullPolicy = new NullPolicy();
        bidirectionalPolicy = new BidirectionalPolicy();
    }

    /**
     * Gets the AttributeAccessor that is used to get and set the value of the
     * container on the target object.
     */
    public AttributeAccessor getContainerAccessor() {
        return bidirectionalPolicy.getBidirectionalTargetAccessor();
    }
    
    /**
     * Sets the AttributeAccessor that is used to get and set the value of the 
     * container on the target object.
     * 
     * @param anAttributeAccessor - the accessor to be used.
     */
    public void setContainerAccessor(AttributeAccessor anAttributeAccessor) {
        this.bidirectionalPolicy.setBidirectionalTargetAccessor(anAttributeAccessor);
    }
    
    /**
     * Sets the name of the backpointer attribute on the target object. Used to 
     * populate the backpointer. If the specified attribute doesn't exist on 
     * the reference class of this mapping, a DescriptorException will be thrown
     * during initialize.
     * 
     * @param attributeName - the name of the backpointer attribute to be populated
     */
    public void setContainerAttributeName(String attributeName) {
        this.bidirectionalPolicy.setBidirectionalTargetAttributeName(attributeName);

    }
    
    /**
     * Gets the name of the backpointer attribute on the target object. Used to 
     * populate the backpointer.
     */    
    public String getContainerAttributeName() {
        return this.bidirectionalPolicy.getBidirectionalTargetAttributeName();
    }
    
    /**
     * Sets the method name to be used when accessing the value of the back pointer 
     * on the target object of this mapping. If the specified method doesn't exist
     * on the reference class of this mapping, a DescriptorException will be thrown
     * during initialize.
     * 
     * @param methodName - the getter method to be used.
     */
    public void setContainerGetMethodName(String methodName) {
        this.bidirectionalPolicy.setBidirectionalTargetGetMethodName(methodName);
    }
    
    /**
     * Sets the name of the method to be used when setting the value of the back pointer 
     * on the target object of this mapping. If the specified method doesn't exist
     * on the reference class of this mapping, a DescriptorException will be thrown
     * during initialize.
     * 
     * @param methodName - the setter method to be used.
     */
    public void setContainerSetMethodName(String methodName) {
        this.bidirectionalPolicy.setBidirectionalTargetSetMethodName(methodName);
    }    
    
    /**
     * Gets the name of the method to be used when accessing the value of the 
     * back pointer on the target object of this mapping.
     */    
    public String getContainerGetMethodName() {
        return this.bidirectionalPolicy.getBidirectionalTargetGetMethodName();
    }
    
    /**
     * Gets the name of the method to be used when setting the value of the 
     * back pointer on the target object of this mapping.
     */    
    public String getContainerSetMethodName() {
        return this.bidirectionalPolicy.getBidirectionalTargetSetMethodName();
    }    
    
    /**
     * INTERNAL:
     * The mapping is initialized with the given session. This mapping is fully initialized
     * after this.
     */
    public void initialize(AbstractSession session) throws DescriptorException {
        //modified so that reference class on composite mappings is no longer mandatory
        if ((getReferenceClass() == null) && (getReferenceClassName() != null)) {
            setReferenceClass(session.getDatasourcePlatform().getConversionManager().convertClassNameToClass(getReferenceClassName()));
        }
        if (getReferenceClass() != null) {
            super.initialize(session);
        } else {
            //below should be the same as AbstractCompositeObjectMapping.initialize
            if (getField() == null) {
                throw DescriptorException.fieldNameNotSetInMapping(this);
            }

            setField(getDescriptor().buildField(getField()));
            setFields(collectFields());
            // initialize the converter - if necessary
            if (hasConverter()) {
                getConverter().initialize(this, session);
            }
        }
        if(null != getContainerAccessor()) {
            getContainerAccessor().initializeAttributes(this.referenceClass);
        }
        
    }

    /**
     * Set the AbstractNullPolicy on the mapping<br>
     * The default policy is NullPolicy.<br>
     *
     * @param aNullPolicy
     */
    public void setNullPolicy(AbstractNullPolicy aNullPolicy) {
        nullPolicy = aNullPolicy;
    }

    /**
     * INTERNAL:
     * Get the AbstractNullPolicy from the Mapping.<br>
     * The default policy is NullPolicy.<br>
     * @return
     */
    public AbstractNullPolicy getNullPolicy() {
        return nullPolicy;
    }

    /**
     * INTERNAL:
     */
    public boolean isXMLMapping() {
        return true;
    }

    /**
     * Get the XPath String
     * @return String the XPath String associated with this Mapping
     */
    public String getXPath() {
        return getField().getName();
    }

    /**
     * Set the Mapping field name attribute to the given XPath String
     * @param xpathString String
     */
    public void setXPath(String xpathString) {
        this.setField(new XMLField(xpathString));
    }

    protected Object buildCompositeRow(Object attributeValue, AbstractSession session, AbstractRecord databaseRow) {
        XMLDescriptor xmlReferenceDescriptor = null;
        try{
            xmlReferenceDescriptor = (XMLDescriptor) getReferenceDescriptor(attributeValue, session);
        }catch(Exception e){
            //do nothing
        }
        
        XMLField xmlFld = (XMLField) getField();
        if (xmlFld.hasLastXPathFragment() && xmlFld.getLastXPathFragment().hasLeafElementType()) {
            XMLRecord xmlRec = (XMLRecord) databaseRow;
            xmlRec.setLeafElementType(xmlFld.getLastXPathFragment().getLeafElementType());
        }
        XMLRecord parent = (XMLRecord) databaseRow;
        
        if (xmlReferenceDescriptor != null) {            
            XMLObjectBuilder objectBuilder = (XMLObjectBuilder) xmlReferenceDescriptor.getObjectBuilder();
                       
            boolean addXsiType = shouldAddXsiType((XMLRecord) databaseRow, xmlReferenceDescriptor);
            XMLRecord child = (XMLRecord) objectBuilder.createRecordFor(attributeValue, (XMLField) getField(), parent, this);
            child.setNamespaceResolver(parent.getNamespaceResolver());
            objectBuilder.buildIntoNestedRow(child, attributeValue, session, addXsiType);
            return child;
        } else {
            if (attributeValue instanceof Element && getKeepAsElementPolicy() == UnmarshalKeepAsElementPolicy.KEEP_UNKNOWN_AS_ELEMENT) {
                return new DOMRecord((Element) attributeValue);
            }else{
                Node newNode = XPathEngine.getInstance().create((XMLField)getField(), parent.getDOM(), attributeValue, session);
                DOMRecord newRow = new DOMRecord(newNode);
                return newRow;            
            }            
        }        
    }

    protected Object buildCompositeObject(ObjectBuilder objectBuilder, AbstractRecord nestedRow, ObjectBuildingQuery query, JoinedAttributeManager joinManager) {
        return objectBuilder.buildObject(query, nestedRow, joinManager);
    }

    public Object readFromRowIntoObject(AbstractRecord databaseRow, JoinedAttributeManager joinManager, Object targetObject, ObjectBuildingQuery sourceQuery, AbstractSession executionSession) throws DatabaseException {

        Object fieldValue = databaseRow.getIndicatingNoEntry(getField());
        // 20071002: noEntry ineffective as a check for an absent node, empty nodes are DOMRecords, absent nodes are null)
        //        if(fieldValue == AbstractRecord.noEntry && !getNullPolicy().getIsSetPerformedForAbsentNode()) {           
        // Do not perform a set for an absent node
        //          return null;
        //        } else {
        // Check for absent nodes based on policy flag
        if ((null == fieldValue) || fieldValue instanceof String) {
            if (getNullPolicy().getIsSetPerformedForAbsentNode()) {
                setAttributeValueInObject(targetObject, null);
            } else {
                return null;
            }
            return null;
        }
        //        }

        // Empty or xsi:nil nodes (non-absent) will arrive here along with populated nodes 
        XMLRecord nestedRow = (XMLRecord) this.getDescriptor().buildNestedRowFromFieldValue(fieldValue);
        // Check the policy to see if this DOM empty/xsi:nil or filled record represents null
        if (getNullPolicy().valueIsNull((Element) nestedRow.getDOM())) {
            setAttributeValueInObject(targetObject, null);
            return null;
        }
        Object attributeValue = valueFromRow(fieldValue, nestedRow, joinManager, sourceQuery, executionSession);
        setAttributeValueInObject(targetObject, attributeValue);
        if(null != getContainerAccessor()) {
            getContainerAccessor().setAttributeValueInObject(attributeValue, targetObject);
        }
        return attributeValue;
    }

    public Object valueFromRow(Object fieldValue, XMLRecord nestedRow, JoinedAttributeManager joinManager, ObjectBuildingQuery sourceQuery, AbstractSession executionSession) throws DatabaseException {
        // pretty sure we can ignore inheritance here:                
        Object toReturn = null;
        // Use local descriptor - not the instance variable on DatabaseMapping       
        ClassDescriptor aDescriptor =  getReferenceDescriptor((DOMRecord) nestedRow);

        if (aDescriptor == null) {  
            if ((getKeepAsElementPolicy() == UnmarshalKeepAsElementPolicy.KEEP_UNKNOWN_AS_ELEMENT) || (getKeepAsElementPolicy() == UnmarshalKeepAsElementPolicy.KEEP_ALL_AS_ELEMENT)) {
                XMLPlatformFactory.getInstance().getXMLPlatform().namespaceQualifyFragment((Element) nestedRow.getDOM());
                toReturn = nestedRow.getDOM();                
                if (getConverter() != null) {                    
                    if (getConverter() instanceof XMLConverter) {
                        toReturn = ((XMLConverter) getConverter()).convertDataValueToObjectValue(toReturn, executionSession, nestedRow.getUnmarshaller());
                    } else {
                        toReturn = getConverter().convertDataValueToObjectValue(toReturn, executionSession);
                    }
                }
                
                //try simple case
                toReturn = convertToSimpleTypeIfPresent(toReturn,  nestedRow, executionSession);
                return toReturn;
            } else {   
                NodeList children =((Element) nestedRow.getDOM()).getChildNodes();
                for(int i=0, childrenLength=children.getLength(); i<childrenLength ; i++){
                    Node nextNode = children.item(i);
                    if(nextNode.getNodeType() == nextNode.ELEMENT_NODE){
                        //complex child
                        throw XMLMarshalException.noDescriptorFound(this);
                    }
                }
                
                //simple case           
                toReturn = convertToSimpleTypeIfPresent(toReturn, nestedRow, executionSession);
            }
        } else {
            if (aDescriptor.hasInheritance()) {
                Class classValue = aDescriptor.getInheritancePolicy().classFromRow(nestedRow, executionSession);
                if (classValue == null) {
                    // no xsi:type attribute - look for type indicator on the field
                    QName leafElementType = ((XMLField) getField()).getLeafElementType();
                    if (leafElementType != null) {
                        Object indicator = aDescriptor.getInheritancePolicy().getClassIndicatorMapping().get(leafElementType);
                        // if the inheritance policy does not contain the user-set type, throw an exception
                        if (indicator == null) {
                            throw DescriptorException.missingClassForIndicatorFieldValue(leafElementType, aDescriptor.getInheritancePolicy().getDescriptor());
                        }
                        classValue = (Class) indicator;
                    }
                }
                if (classValue != null) {
                    aDescriptor = this.getReferenceDescriptor(classValue, executionSession);
                } else {
                    // since there is no xsi:type attribute or leaf element type set, 
                    // use the reference descriptor -  make sure it is non-abstract
                    if (Modifier.isAbstract(aDescriptor.getJavaClass().getModifiers())) {
                        // throw an exception
                        throw DescriptorException.missingClassIndicatorField(nestedRow, aDescriptor.getInheritancePolicy().getDescriptor());
                    }
                }
            }
            ObjectBuilder objectBuilder = aDescriptor.getObjectBuilder();
            toReturn = buildCompositeObject(objectBuilder, nestedRow, sourceQuery, joinManager);
        }
        
        if (getConverter() != null) {
            if (getConverter() instanceof XMLConverter) {
                toReturn = ((XMLConverter) getConverter()).convertDataValueToObjectValue(toReturn, executionSession, nestedRow.getUnmarshaller());
            } else {
                toReturn = getConverter().convertDataValueToObjectValue(toReturn, executionSession);
            }
        }
        return toReturn;
    }
    
    private Object convertToSimpleTypeIfPresent(Object toReturn, XMLRecord nestedRow, AbstractSession executionSession){
        Node textchild = ((Element) nestedRow.getDOM()).getFirstChild(); 	
        String stringValue = null;
        if ((textchild != null) && (textchild.getNodeType() == Node.TEXT_NODE)) {
            stringValue = ((Text) textchild).getNodeValue();
            if(stringValue != null && getKeepAsElementPolicy() != UnmarshalKeepAsElementPolicy.KEEP_UNKNOWN_AS_ELEMENT && getKeepAsElementPolicy()!=UnmarshalKeepAsElementPolicy.KEEP_ALL_AS_ELEMENT){
                toReturn = stringValue; 
            }
    	 }
        if ((stringValue == null) || stringValue.length() == 0) {
            return toReturn;
    	}
    	 
        String type = ((Element) nestedRow.getDOM()).getAttributeNS(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_TYPE_ATTRIBUTE);
    
        if ((null != type) && type.length() > 0) {
            XPathFragment typeFragment = new XPathFragment(type);
            String namespaceURI = nestedRow.resolveNamespacePrefix(typeFragment.getPrefix());
            
            QName schemaTypeQName = new QName(namespaceURI, typeFragment.getLocalName());                       
            Class theClass = (Class) XMLConversionManager.getDefaultXMLTypes().get(schemaTypeQName);
            if (theClass != null) {            	  
                toReturn = ((XMLConversionManager) executionSession.getDatasourcePlatform().getConversionManager()).convertObject(stringValue, theClass, schemaTypeQName);
            }                                    
        }
        return toReturn;        
    }
  
    public Object valueFromRow(AbstractRecord row, JoinedAttributeManager joinManager, ObjectBuildingQuery sourceQuery, AbstractSession executionSession) throws DatabaseException {
        Object fieldValue = row.get(this.getField());
        // BUG#2667762 there could be whitespace in the row instead of null
        if ((fieldValue == null) || (fieldValue instanceof String)) {
            return null;
        }

        XMLRecord nestedRow = (XMLRecord) this.getDescriptor().buildNestedRowFromFieldValue(fieldValue);
        // Check the policy to see if this DOM record represents null
        if (getNullPolicy().valueIsNull((Element) nestedRow.getDOM())) {
            return null;
        }
        return valueFromRow(fieldValue, nestedRow, joinManager, sourceQuery, executionSession);
    }

    /**
     * INTERNAL:
     */
    public void writeFromObjectIntoRow(Object object, AbstractRecord databaseRow, AbstractSession session) throws DescriptorException {
        if (this.isReadOnly()) {
            return;
        }
        Object attributeValue = this.getAttributeValueFromObject(object);
        writeSingleValue(attributeValue, object, (XMLRecord) databaseRow, session);
    }

    public void writeSingleValue(Object value, Object parent, XMLRecord record, AbstractSession session) {
        Object attributeValue = value;
        if (getConverter() != null) {
            if (getConverter() instanceof XMLConverter) {
                attributeValue = ((XMLConverter) getConverter()).convertObjectValueToDataValue(attributeValue, session, record.getMarshaller());
            } else {
                attributeValue = getConverter().convertObjectValueToDataValue(attributeValue, session);
            }
        }
        // handle "self" xpath
        if (((XMLField) getField()).isSelfField()) {
             if (((keepAsElementPolicy == UnmarshalKeepAsElementPolicy.KEEP_UNKNOWN_AS_ELEMENT) || (keepAsElementPolicy == UnmarshalKeepAsElementPolicy.KEEP_ALL_AS_ELEMENT)) && attributeValue instanceof org.w3c.dom.Node) {
                 //write out node
            	 org.w3c.dom.Document doc = record.getDocument();
            	 Node root = record.getDOM();
            	 NodeList children = ((Node) attributeValue).getChildNodes();
            	 for(int i=0,childrenLength=children.getLength();i<childrenLength; i++){
                     Node importedCopy = doc.importNode(children.item(i), true);                     
                     root.appendChild(importedCopy);
                 }
             }else{      
            	 ClassDescriptor desc =  this.getReferenceDescriptor(attributeValue.getClass(), session);
            	 if(desc != null){
            		 XMLObjectBuilder objectBuilder = (XMLObjectBuilder)desc.getObjectBuilder();
            		 boolean addXsiType = shouldAddXsiType((XMLRecord) record, desc);
            		 objectBuilder.buildIntoNestedRow(record, attributeValue, session, addXsiType);            		
            	 }else{
            		 //simple case 
             		 record.put(this.getField(), attributeValue);
            	 }
             }
        } else {
            Object fieldValue = null;
            if (attributeValue != null) {                               
                fieldValue = buildCompositeRow(attributeValue, session, record);
            } else if (getNullPolicy().compositeObjectMarshal(record, parent, (XMLField) getField(), session)) {
                // If the null policy marshal method returns true (i.e. marshalled something)
                // don't add/put null in the record
                return;
            }
            // handle document preservation
            record.put(this.getField(), fieldValue);
        }
    }

    public void configureNestedRow(AbstractRecord parent, AbstractRecord child) {
        XMLRecord parentRecord = (XMLRecord) parent;
        XMLRecord childRecord = (XMLRecord) child;

        childRecord.setUnmarshaller(parentRecord.getUnmarshaller());
        childRecord.setOwningObject(parentRecord.getCurrentObject());
    }

    public ClassDescriptor getReferenceDescriptor(DOMRecord xmlRecord) {
        ClassDescriptor returnDescriptor = referenceDescriptor;

        if (returnDescriptor == null) {
            // Try to find a descriptor based on the schema type
            String type = ((Element) xmlRecord.getDOM()).getAttributeNS(XMLConstants.SCHEMA_INSTANCE_URL, XMLConstants.SCHEMA_TYPE_ATTRIBUTE);

            if ((null != type) && type.length() > 0) {
                XPathFragment typeFragment = new XPathFragment(type);
                String namespaceURI = xmlRecord.resolveNamespacePrefix(typeFragment.getPrefix());
                typeFragment.setNamespaceURI(namespaceURI);

                returnDescriptor = xmlRecord.getUnmarshaller().getXMLContext().getDescriptorByGlobalType(typeFragment);

            } else {
                //try leaf element type
                QName leafType = ((XMLField) getField()).getLastXPathFragment().getLeafElementType();
                if (leafType != null) {
                    XPathFragment frag = new XPathFragment();
                    String xpath = leafType.getLocalPart();
                    String uri = leafType.getNamespaceURI();
                    if ((uri != null) && uri.length() > 0) {
                        frag.setNamespaceURI(uri);
                        String prefix = ((XMLDescriptor) getDescriptor()).getNonNullNamespaceResolver().resolveNamespaceURI(uri);
                        if ((prefix != null) && prefix.length() > 0) {
                            xpath = prefix + XMLConstants.COLON + xpath;
                        }
                    }
                    frag.setXPath(xpath);

                    returnDescriptor = xmlRecord.getUnmarshaller().getXMLContext().getDescriptorByGlobalType(frag);
                }
            }
        }        
        return returnDescriptor;

    }

    protected ClassDescriptor getReferenceDescriptor(Class theClass, AbstractSession session) {
        if ((getReferenceDescriptor() != null) && getReferenceDescriptor().getJavaClass().equals(theClass)) {
            return getReferenceDescriptor();
        }

        ClassDescriptor subDescriptor = session.getDescriptor(theClass);
        if (subDescriptor == null && getKeepAsElementPolicy() != UnmarshalKeepAsElementPolicy.KEEP_UNKNOWN_AS_ELEMENT) {
            throw DescriptorException.noSubClassMatch(theClass, this);
        } else {
            return subDescriptor;
        }
    }

    /**
     * INTERNAL:
     */
    public boolean shouldAddXsiType(XMLRecord record, ClassDescriptor aDescriptor) {
        XMLDescriptor xmlDescriptor = (XMLDescriptor) aDescriptor;
        if ((getReferenceDescriptor() == null) && (xmlDescriptor.getSchemaReference() != null)) {
            if (aDescriptor.hasInheritance()) {
                XMLField indicatorField = (XMLField) aDescriptor.getInheritancePolicy().getClassIndicatorField();
                if ((indicatorField.getLastXPathFragment().getNamespaceURI() != null) //
                        && indicatorField.getLastXPathFragment().getNamespaceURI().equals(XMLConstants.SCHEMA_INSTANCE_URL) //
                        && indicatorField.getLastXPathFragment().getLocalName().equals(XMLConstants.SCHEMA_TYPE_ATTRIBUTE)) {
                    return false;
                }
            }

            XMLSchemaReference xmlRef = xmlDescriptor.getSchemaReference();
            if ((xmlRef.getType() == XMLSchemaReference.COMPLEX_TYPE) && xmlRef.isGlobalDefinition()) {
                QName ctxQName = xmlRef.getSchemaContextAsQName(xmlDescriptor.getNamespaceResolver());
                QName leafType = ((XMLField) getField()).getLeafElementType();

                if ((leafType == null) || (!ctxQName.equals(record.getLeafElementType()))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public UnmarshalKeepAsElementPolicy getKeepAsElementPolicy() {
        return keepAsElementPolicy;
    }

    public void setKeepAsElementPolicy(UnmarshalKeepAsElementPolicy keepAsElementPolicy) {
        this.keepAsElementPolicy = keepAsElementPolicy;
    }
    
    protected XMLDescriptor getDescriptor(XMLRecord xmlRecord, AbstractSession session, QName rootQName) throws XMLMarshalException {
        if (rootQName == null) {
            rootQName = new QName(xmlRecord.getNamespaceURI(), xmlRecord.getLocalName());
        }
        XMLContext xmlContext = xmlRecord.getUnmarshaller().getXMLContext();
        XMLDescriptor xmlDescriptor = xmlContext.getDescriptor(rootQName);
        if (null == xmlDescriptor) {
            if (!((getKeepAsElementPolicy() == UnmarshalKeepAsElementPolicy.KEEP_UNKNOWN_AS_ELEMENT) || (getKeepAsElementPolicy() == UnmarshalKeepAsElementPolicy.KEEP_ALL_AS_ELEMENT))) {
                throw XMLMarshalException.noDescriptorWithMatchingRootElement(xmlRecord.getLocalName());
            }
        }
        return xmlDescriptor;
    }
    
    public void setIsWriteOnly(boolean b) {
        isWriteOnly = b;
    }
    
    public boolean isWriteOnly() {
        return isWriteOnly;
    }
    
    public void preInitialize(AbstractSession session) throws DescriptorException {
        getAttributeAccessor().setIsWriteOnly(this.isWriteOnly());
        getAttributeAccessor().setIsReadOnly(this.isReadOnly());
        super.preInitialize(session);
    }
    
    public void setAttributeValueInObject(Object object, Object value) throws DescriptorException {
        if(isWriteOnly()) {
            return;
        }
        super.setAttributeValueInObject(object, value);
    }    
    
    
}
