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
package org.eclipse.persistence.oxm;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.xml.namespace.QName;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.InheritancePolicy;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.internal.helper.DatabaseTable;
import org.eclipse.persistence.internal.helper.Helper;
import org.eclipse.persistence.internal.helper.NonSynchronizedVector;
import org.eclipse.persistence.internal.identitymaps.AbstractIdentityMap;
import org.eclipse.persistence.internal.oxm.TreeObjectBuilder;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;
import org.eclipse.persistence.oxm.record.XMLRecord;
import org.eclipse.persistence.oxm.schema.XMLSchemaReference;

/**
 * Use an XML project for nontransactional, nonpersistent (in-memory) conversions between Java objects and XML documents.
 *
 * An XMLDescriptor is a set of mappings that describe how an objects's data is to be represented in an
 * XML document. XML descriptors describe Java objects that you map to simple and complex types defined
 * by an XML schema document (XSD). Using XML descriptors in an EclipseLink XML project, you can configure XML mappings.
 *
 * @see org.eclipse.persistence.oxm.mappings
 */
public class XMLDescriptor extends ClassDescriptor {
    private NamespaceResolver namespaceResolver;
    private XMLSchemaReference schemaReference;
    private boolean shouldPreserveDocument = false;
    private XMLField defaultRootElementField;
    private boolean sequencedObject = false;
    private boolean isWrapper = false;
    private boolean resultAlwaysXMLRoot = false;

    /**
     * PUBLIC:
     * Return a new XMLDescriptor.
     */
    public XMLDescriptor() {
        this.tables = NonSynchronizedVector.newInstance(3);
        this.mappings = NonSynchronizedVector.newInstance();
        this.primaryKeyFields = new ArrayList(2);
        this.fields = NonSynchronizedVector.newInstance();
        this.allFields = NonSynchronizedVector.newInstance();
        this.constraintDependencies = NonSynchronizedVector.newInstance(2);
        this.multipleTableForeignKeys = new HashMap(5);
        this.queryKeys = new HashMap(5);
        this.initializationStage = UNINITIALIZED;
        this.interfaceInitializationStage = UNINITIALIZED;
        this.shouldAlwaysRefreshCache = false;
        this.shouldOnlyRefreshCacheIfNewerVersion = false;
        this.shouldDisableCacheHits = false;
        this.identityMapSize = 100;
        this.remoteIdentityMapSize = -1;
        this.identityMapClass = AbstractIdentityMap.getDefaultIdentityMapClass();
        this.remoteIdentityMapClass = null;
        this.descriptorType = NORMAL;
        this.shouldAlwaysRefreshCacheOnRemote = false;
        this.shouldDisableCacheHitsOnRemote = false;
        this.shouldOrderMappings = true;
        this.shouldBeReadOnly = false;
        this.shouldAlwaysConformResultsInUnitOfWork = false;
        this.shouldAcquireCascadedLocks = false;
        this.hasSimplePrimaryKey = false;
        this.cacheIsolation = null;

        // Policies        
        this.objectBuilder = new TreeObjectBuilder(this);
        this.cascadeLockingPolicies = NonSynchronizedVector.newInstance();

        this.shouldOrderMappings = false;
        this.descriptorIsAggregate();
    }

    /**
     * PUBLIC:
     * Return the default root element name for the ClassDescriptor
     * This value is stored in place of a table name
     * This value is mandatory for all root objects
     * @return the default root element specified on this ClassDescriptor
     */
    public String getDefaultRootElement() {
        if (getTables().isEmpty()) {
            return null;
        }
        return getTables().firstElement().getName();
    }
    
    /**
     * PUBLIC:
     * Return if unmapped information from the XML document should be maintained for this
     * descriptor
     * By default unmapped data is not preserved.
     * @return if this descriptor should preserve unmapped data
     */
    public boolean shouldPreserveDocument() {
        return this.shouldPreserveDocument;
    }

    /**
     * PUBLIC:
     * Specifies that object built from this descriptor should retain any unmapped
     * information from their original XML Document when being written back out.
     * By default unmapped data is not preserved.
     * @return if this descriptor should preserve unmapped data
     */
    public void setShouldPreserveDocument(boolean shouldPreserveDocument) {
        this.shouldPreserveDocument = shouldPreserveDocument;
    }

    /**
    * PUBLIC:
    * Add a root element name for the Descriptor
    * This value is stored in place of a table name
    * @param rootElementName a root element to specify on this Descriptor
    */
    public void addRootElement(String rootElementName) {
        if (rootElementName != null) {
            if (!getTableNames().contains(rootElementName)) {
                addTableName(rootElementName);
            }
        }
    }

    /**
    * PUBLIC:
    * Return the default root element name for the ClassDescriptor
    * This value is stored in place of a table name
    * This value is mandatory for all root objects
    * @param newDefaultRootElement the default root element to specify on this ClassDescriptor
    */
    public void setDefaultRootElement(String newDefaultRootElement) {
        if(setDefaultRootElementField(newDefaultRootElement)) {
            int index = getTableNames().indexOf(newDefaultRootElement);
            if (index == 0) {
                return;
            } else if (index >= 0) {
                getTables().remove(index);
                getTables().add(0, new DatabaseTable(newDefaultRootElement));
            } else {
                getTables().add(0, new DatabaseTable(newDefaultRootElement));
            }
        }
    }

    /**
    * PUBLIC:
    * Return the NamespaceResolver associated with this descriptor
    * @return the NamespaceResolver associated with this descriptor
    * @see org.eclipse.persistence.oxm.NamespaceResolver
    */
    public NamespaceResolver getNamespaceResolver() {
        return namespaceResolver;
    }

    public NamespaceResolver getNonNullNamespaceResolver() {
        if (namespaceResolver == null) {
            namespaceResolver = new NamespaceResolver();
        }
        return namespaceResolver;
    }

    /**
     * PUBLIC:
     * The inheritance policy is used to define how a descriptor takes part in inheritance.
     * All inheritance properties for both child and parent classes is configured in inheritance policy.
     * Caution must be used in using this method as it lazy initializes an inheritance policy.
     * Calling this on a descriptor that does not use inheritance will cause problems, #hasInheritance() must always first be called.
     * @return the InheritancePolicy associated with this descriptor
     */
    public InheritancePolicy getInheritancePolicy() {
        if (inheritancePolicy == null) {
            // Lazy initialize to conserve space in non-inherited classes.
            setInheritancePolicy(new org.eclipse.persistence.internal.oxm.QNameInheritancePolicy(this));
        }
        return inheritancePolicy;
    }

    /**
    * PUBLIC:
    * Set the NamespaceResolver to associate with this descriptor
    * @param newNamespaceResolver the NamespaceResolver to associate with this descriptor
    * @see org.eclipse.persistence.oxm.NamespaceResolver
    */
    public void setNamespaceResolver(NamespaceResolver newNamespaceResolver) {
        namespaceResolver = newNamespaceResolver;
    }

    /**
    * PUBLIC:
    * Return the SchemaReference associated with this descriptor
    * @return the SchemaReference associated with this descriptor
    * @see org.eclipse.persistence.oxm.schema
    */
    public XMLSchemaReference getSchemaReference() {
        return schemaReference;
    }

    /**
     * PUBLIC:
     * Set the SchemaReference to associate with this descriptor
     * @param newSchemaReference the SchemaReference to associate with this descriptor
     * @see org.eclipse.persistence.oxm.schema
     */
    public void setSchemaReference(XMLSchemaReference newSchemaReference) {
        schemaReference = newSchemaReference;
    }

    protected void validateMappingType(DatabaseMapping mapping) {
        if (!(mapping.isXMLMapping())) {
            throw DescriptorException.invalidMappingType(mapping);
        }
    }

    /**
     * INTERNAL:
     * Avoid SDK initialization.

    public void setQueryManager(DescriptorQueryManager queryManager) {
        this.queryManager = queryManager;
        if (queryManager != null) {
            queryManager.setDescriptor(this);
        }
    }*/
    /**
    * INTERNAL:
    * Build(if necessary) and return the nested XMLRecord from the specified field value.
    * The field value should be an XMLRecord or and XMLElement
    */
    public AbstractRecord buildNestedRowFromFieldValue(Object fieldValue) {
        if (fieldValue instanceof XMLRecord) {
            return (XMLRecord) fieldValue;
        }
        
        // BUG#2667762 - If the tag was empty this could be a string of whitespace.
        if (!(fieldValue instanceof Vector)) {
            return getObjectBuilder().createRecord(null);
        }

        Vector nestedRows = (Vector) fieldValue;
        if (nestedRows.isEmpty()) {
            return getObjectBuilder().createRecord(null);
        } else {
            // BUG#2667762 - If the tag was empty this could be a string of whitespace.
            if (!(nestedRows.firstElement() instanceof AbstractRecord)) {
                return getObjectBuilder().createRecord(null);
            }
            return (XMLRecord) nestedRows.firstElement();
        }
    }

    /**
    * INTERNAL:
    * Build(if necessary) and return a Vector of the nested XMLRecords from the specified field value.
    * The field value should be a Vector, an XMLRecord, or an XMLElement
    */
    public Vector buildNestedRowsFromFieldValue(Object fieldValue, AbstractSession session) {
        // BUG#2667762 - If the tag was empty this could be a string of whitespace.
        if (!(fieldValue instanceof Vector)) {
            return new Vector(0);
        }
        return (Vector) fieldValue;
    }

    /**
    * PUBLIC:
    * Add a direct mapping to the receiver. The new mapping specifies that
    * an instance variable of the class of objects which the receiver describes maps in
    * the default manner for its type to the indicated database field.
    *
    * @param attributeName  the name of an instance variable of the
    * class which the receiver describes.
    * @param xpathString the xpath of the xml element or attribute which corresponds
    * with the designated instance variable.
    * @return The newly created DatabaseMapping is returned.
    */
    public DatabaseMapping addDirectMapping(String attributeName, String xpathString) {
        XMLDirectMapping mapping = new XMLDirectMapping();

        mapping.setAttributeName(attributeName);
        mapping.setXPath(xpathString);

        return addMapping(mapping);
    }

    /**
    * PUBLIC:
    * Add a direct to node mapping to the receiver. The new mapping specifies that
    * a variable accessed by the get and set methods of the class of objects which
    * the receiver describes maps in  the default manner for its type to the indicated
    * database field.
    */
    public DatabaseMapping addDirectMapping(String attributeName, String getMethodName, String setMethodName, String xpathString) {
        XMLDirectMapping mapping = new XMLDirectMapping();

        mapping.setAttributeName(attributeName);
        mapping.setSetMethodName(setMethodName);
        mapping.setGetMethodName(getMethodName);
        mapping.setXPath(xpathString);

        return addMapping(mapping);
    }

    @Override
    public void addPrimaryKeyFieldName(String fieldName) {
        super.addPrimaryKeyField(new XMLField(fieldName));
    }

    @Override
    public void addPrimaryKeyField(DatabaseField field) {
        if (!(field instanceof XMLField)) {
            String fieldName = field.getName();
            field = new XMLField(fieldName);
        }
        super.addPrimaryKeyField(field);
    }

    @Override
    public void setPrimaryKeyFields(List<DatabaseField> thePrimaryKeyFields) {
        List<DatabaseField> xmlFields = new ArrayList(thePrimaryKeyFields.size());
        Iterator<DatabaseField> it = thePrimaryKeyFields.iterator();

        while (it.hasNext()) {
            DatabaseField field = it.next();
            if (!(field instanceof XMLField)) {
                String fieldName = field.getName();
                field = new XMLField(fieldName);
            }
            xmlFields.add(field);
        }

        super.setPrimaryKeyFields(xmlFields);
    }

    /**
     * INTERNAL:
     * Extract the direct values from the specified field value.
     * Return them in a vector.
     * The field value could be a vector or could be a text value if only a single value.
     */
    public Vector buildDirectValuesFromFieldValue(Object fieldValue) throws DatabaseException {
        if (!(fieldValue instanceof Vector)) {
            Vector fieldValues = new Vector(1);
            fieldValues.add(fieldValue);
            return fieldValues;
        }
        return (Vector) fieldValue;
    }

    /**
     * INTERNAL:
     * Build the appropriate field value for the specified
     * set of direct values.
     * The database better be expecting a Vector.
     */
    public Object buildFieldValueFromDirectValues(Vector directValues, String elementDataTypeName, AbstractSession session) throws DatabaseException {
        return directValues;
    }

    /**
     * INTERNAL:
     * Build and return the appropriate field value for the specified
     * set of nested rows.
     */
    public Object buildFieldValueFromNestedRows(Vector nestedRows, String structureName, AbstractSession session) throws DatabaseException {
        return nestedRows;
    }

    /**
     * INTERNAL:
     * A DatabaseField is built from the given field name.
     */
    public DatabaseField buildField(String fieldName) {
        XMLField xmlField = new XMLField(fieldName);
        xmlField.setNamespaceResolver(this.getNamespaceResolver());
        //xmlField.initialize();
        return xmlField;
    }

    /**
     * INTERNAL:
     * This is used only in initialization.
     */
    public DatabaseField buildField(DatabaseField field) {
        try {
            XMLField xmlField = (XMLField) field;
            xmlField.setNamespaceResolver(this.getNamespaceResolver());
            xmlField.initialize();
        } catch (ClassCastException e) {
            // Assumes fields are always XML.
        }
        return super.buildField(field);
    }

    /**
     * INTERNAL:
     * This is needed by regular aggregate descriptors (because they require review);
     * but not by XML aggregate descriptors.
     */
    public void initializeAggregateInheritancePolicy(AbstractSession session) {
        // do nothing, since the parent descriptor was already modified during pre-initialize
    }

    @Override
    public void setTableNames(Vector tableNames) {
        if (null != tableNames && tableNames.size() > 0) {
            setDefaultRootElementField((String) tableNames.get(0));
        }
        super.setTableNames(tableNames);
    }

    /**
     * INTERNAL:
     * Sets the tables
     */
    public void setTables(Vector<DatabaseTable> theTables) {
        if (null != theTables && theTables.size() > 0) {
            setDefaultRootElementField(theTables.get(0).getName());
         }
         super.setTables(theTables);
    }

    /**
     * INTERNAL:
     * Allow the descriptor to initialize any dependencies on this session.
     */
    public void preInitialize(AbstractSession session) throws DescriptorException {
        // Avoid repetitive initialization (this does not solve loops)
        if (isInitialized(PREINITIALIZED)) {
            return;
        }
        setInitializationStage(PREINITIALIZED);

        // Allow mapping pre init, must be done before validate.
        for (Enumeration mappingsEnum = getMappings().elements(); mappingsEnum.hasMoreElements();) {
            try {
                DatabaseMapping mapping = (DatabaseMapping) mappingsEnum.nextElement();
                mapping.preInitialize(session);
            } catch (DescriptorException exception) {
                session.getIntegrityChecker().handleError(exception);
            }
        }

        validateBeforeInitialization(session);

        preInitializeInheritancePolicy(session);

        // Make sure that parent is already preinitialized
        if (hasInheritance()) {
            // The default table will be set in this call once the duplicate
            // tables have been removed.
            getInheritancePolicy().preInitialize(session);
        } else {
            // This must be done now, after validate, before init anything else.
            setInternalDefaultTable();
        }

        verifyTableQualifiers(session.getDatasourcePlatform());
        initializeProperties(session);

        if (hasInterfacePolicy()) {
            preInterfaceInitialization(session);
        }

    }

    /**
     * INTERNAL:
     * Post initializations after mappings are initialized.
     */
    public void postInitialize(AbstractSession session) throws DescriptorException {
        // Avoid repetitive initialization (this does not solve loops)
        if (isInitialized(POST_INITIALIZED) || isInvalid()) {
            return;
        }

        setInitializationStage(POST_INITIALIZED);

        // Make sure that child is post initialized,
        // this initialize bottom up, unlike the two other phases that to top down.
        if (hasInheritance()) {
            for (ClassDescriptor child : getInheritancePolicy().getChildDescriptors()) {
                child.postInitialize(session);
            }
        }

        // Allow mapping to perform post initialization.
        for (DatabaseMapping mapping : getMappings()) {
            // This causes post init to be called multiple times in inheritance.
            mapping.postInitialize(session);
        }

        if (hasInheritance()) {
            getInheritancePolicy().postInitialize(session);
        }

        //PERF: Ensure that the identical primary key fields are used to avoid equals.
        for (int index = (getPrimaryKeyFields().size() - 1); index >= 0; index--) {
            DatabaseField primaryKeyField = getPrimaryKeyFields().get(index);
            int fieldIndex = getFields().indexOf(primaryKeyField);

            // Aggregate/agg-collections may not have a mapping for pk field.
            if (fieldIndex != -1) {
                primaryKeyField = getFields().get(fieldIndex);
                getPrimaryKeyFields().set(index, primaryKeyField);
            }
        }

        // Index and classify fields and primary key.
        // This is in post because it needs field classification defined in initializeMapping
        // this can come through a 1:1 so requires all descriptors to be initialized (mappings).
        // May 02, 2000 - Jon D.
        for (int index = 0; index < getFields().size(); index++) {
            DatabaseField field = getFields().elementAt(index);
            if (field.getType() == null) {
                DatabaseMapping mapping = getObjectBuilder().getMappingForField(field);
                if (mapping != null) {
                    field.setType(mapping.getFieldClassification(field));
                }
            }
            field.setIndex(index);
        }

        validateAfterInitialization(session);
    }

    /**
     * INTERNAL:
     * Initialize the mappings as a separate step.
     * This is done as a separate step to ensure that inheritance has been first resolved.
     */
    public void initialize(AbstractSession session) throws DescriptorException {
        if (this.hasInheritance()) {
            ((org.eclipse.persistence.internal.oxm.QNameInheritancePolicy) this.getInheritancePolicy()).setNamespaceResolver(this.getNamespaceResolver());
        }

        if(null != this.defaultRootElementField) {
            defaultRootElementField.setNamespaceResolver(this.namespaceResolver);
            defaultRootElementField.initialize();
        }

        for(int x = 0, primaryKeyFieldsSize = this.primaryKeyFields.size(); x<primaryKeyFieldsSize; x++) {
            XMLField pkField = (XMLField) this.primaryKeyFields.get(x);
            pkField.setNamespaceResolver(this.namespaceResolver);
            pkField.initialize();
        }

        // These cached settings on the project must be set even if descriptor is initialized.
        // If defined as read-only, add to it's project's default read-only classes collection.
        if (shouldBeReadOnly() && (!session.getDefaultReadOnlyClasses().contains(getJavaClass()))) {
            session.getDefaultReadOnlyClasses().add(getJavaClass());
        }

        // Avoid repetitive initialization (this does not solve loops)
        if (isInitialized(INITIALIZED) || isInvalid()) {
            return;
        }

        setInitializationStage(INITIALIZED);

        // make sure that parent mappings are initialized?
        if (isChildDescriptor()) {
            getInheritancePolicy().getParentDescriptor().initialize(session);
        }

        for (Enumeration mappingsEnum = getMappings().elements(); mappingsEnum.hasMoreElements();) {
            DatabaseMapping mapping = (DatabaseMapping) mappingsEnum.nextElement();
            validateMappingType(mapping);
            mapping.initialize(session);

            // Add all the fields in the mapping to myself.
            Helper.addAllUniqueToVector(getFields(), mapping.getFields());
        }

        // If this has inheritance then it needs to be initialized before all fields is set.
        if (hasInheritance()) {
            getInheritancePolicy().initialize(session);
        }

        // Initialize the allFields to its fields, this can be done now because the fields have been computed.
        setAllFields((Vector) getFields().clone());

        getObjectBuilder().initialize(session);
        if (hasInterfacePolicy()) {
            interfaceInitialization(session);
        }
        if (hasReturningPolicy()) {
            getReturningPolicy().initialize(session);
        }
        getEventManager().initialize(session);
        getCopyPolicy().initialize(session);
        getInstantiationPolicy().initialize(session);
        if (getSchemaReference() != null) {
            getSchemaReference().initialize(session);
        }
    }

    /**
     * INTERNAL:
     * XML descriptors are initialized normally, since they do
     * not need to be cloned by XML aggregate mappings.
     */
    public boolean requiresInitialization() {
        return (!isDescriptorForInterface());
    }

    /**
     * Aggregates use a dummy table as default.
     */
    protected DatabaseTable extractDefaultTable() {
        return new DatabaseTable();
    }

    /**
     * INTERNAL:
     * Determines the appropriate object to return from the unmarshal
     * call.  The method will either return the object created in the
     * xmlReader.parse() call or an instance of XMLRoot.  An XMLRoot
     * instance will be returned if the DOMRecord element being
     * unmarshalled does not equal the descriptor's default root
     * element.
     *
     * @param unmarshalRecord
     * @return object
     */
    public Object wrapObjectInXMLRoot(UnmarshalRecord unmarshalRecord, boolean forceWrap) {
        String elementLocalName = unmarshalRecord.getLocalName();
        String elementNamespaceUri = unmarshalRecord.getRootElementNamespaceUri();
        if (forceWrap || shouldWrapObject(unmarshalRecord.getCurrentObject(), elementNamespaceUri, elementLocalName, null)) {
            XMLRoot xmlRoot = new XMLRoot();
            xmlRoot.setLocalName(elementLocalName);
            xmlRoot.setNamespaceURI(elementNamespaceUri);
            xmlRoot.setObject(unmarshalRecord.getCurrentObject());
            xmlRoot.setEncoding(unmarshalRecord.getEncoding());
            xmlRoot.setVersion(unmarshalRecord.getVersion());
            xmlRoot.setSchemaLocation(unmarshalRecord.getSchemaLocation());
            xmlRoot.setNoNamespaceSchemaLocation(unmarshalRecord.getNoNamespaceSchemaLocation());
            xmlRoot.setNil(unmarshalRecord.isNil());
            return xmlRoot;
        }
        return unmarshalRecord.getCurrentObject();
    }

    /**
      * INTERNAL:
      * Determines the appropriate object to return from the unmarshal
      * call.  The method will either return the object created in the
      * xmlReader.parse() call or an instance of XMLRoot.  An XMLRoot
      * instance will be returned if the DOMRecord element being
      * unmarshalled does not equal the descriptor's default root
      * element.
      *
      * @param object
      * @param elementNamespaceUri
      * @param elementLocalName
      * @param elementPrefix
      * @return object
      */
    public Object wrapObjectInXMLRoot(Object object, String elementNamespaceUri, String elementLocalName, String elementPrefix, boolean forceWrap) {

        if (forceWrap || shouldWrapObject(object, elementNamespaceUri, elementLocalName, elementPrefix)) {
            // if the DOMRecord element != descriptor's default 
            // root element, create an XMLRoot, populate and return it
            XMLRoot xmlRoot = new XMLRoot();
            xmlRoot.setLocalName(elementLocalName);
            xmlRoot.setNamespaceURI(elementNamespaceUri);
            xmlRoot.setObject(object);
            return xmlRoot;
        }
        return object;
    }

    public Object wrapObjectInXMLRoot(Object object, String elementNamespaceUri, String elementLocalName, String elementPrefix, String encoding, String version, boolean forceWrap) {
        if (forceWrap || shouldWrapObject(object, elementNamespaceUri, elementLocalName, elementPrefix)) {
            // if the DOMRecord element != descriptor's default 
            // root element, create an XMLRoot, populate and return it
            XMLRoot xmlRoot = new XMLRoot();
            xmlRoot.setLocalName(elementLocalName);
            xmlRoot.setNamespaceURI(elementNamespaceUri);
            xmlRoot.setObject(object);
            xmlRoot.setEncoding(encoding);
            xmlRoot.setVersion(version);                        
            return xmlRoot;
        }
        return object;

    }
    
    public boolean shouldWrapObject(Object object, String elementNamespaceUri, String elementLocalName, String elementPrefix) {
        if(resultAlwaysXMLRoot){
            return true;
        }
        XMLField defaultRootField = getDefaultRootElementField();

        // if the descriptor's default root element is null, we want to 
        // create/return an XMLRoot - otherwise, we need to compare the 
        // default root element vs. the root element in the instance doc.
        if (defaultRootField != null) {
            // resolve namespace prefix if one exists
            String defaultRootName = defaultRootField.getXPathFragment().getLocalName();
            String defaultRootNamespaceUri = defaultRootField.getXPathFragment().getNamespaceURI();

            // if the DOMRecord element == descriptor's default 
            // root element, return the object as per usual
            if ((((defaultRootNamespaceUri == null) && (elementNamespaceUri == null)) || ((defaultRootNamespaceUri == null) && (elementNamespaceUri.length() == 0)) || ((elementNamespaceUri == null) && (defaultRootNamespaceUri.length() == 0)) || (((defaultRootNamespaceUri != null) && (elementNamespaceUri != null)) && (defaultRootNamespaceUri
                    .equals(elementNamespaceUri))))
                    && (defaultRootName.equals(elementLocalName))) {
                return false;
            }
        }
        return true;
    }

    public XMLField getDefaultRootElementField() {
        return defaultRootElementField;
    }

    /**
     * @return true if a new default root element field was created, else false.
     */
    private boolean setDefaultRootElementField(String newDefaultRootElement) {
        if (null == newDefaultRootElement || 0 == newDefaultRootElement.length()) {
            setDefaultRootElementField((XMLField) null);
            return false;
        }
        // create the root element xml field based on default root element name
        setDefaultRootElementField(new XMLField(newDefaultRootElement));
        return true;
    }

    public void setDefaultRootElementField(XMLField xmlField) {
        defaultRootElementField = xmlField;
    }

    public QName getDefaultRootElementType() {
        if (defaultRootElementField != null) {
            return defaultRootElementField.getLeafElementType();
        }
        return null;
    }

    /**
     * The default root element type string will be stored until
     * initialization - a QName will be created and stored on the
     * default root element field during initialize.
     *
     * @param type
     */
    public void setDefaultRootElementType(QName type) {
        if (defaultRootElementField != null) {
            defaultRootElementField.setLeafElementType(type);
        }
    }

    /**
     * INTERNAL:
     * <p>Indicates if the Object mapped by this descriptor is a sequenced data object
     * and should be marshalled accordingly.
     */
    public boolean isSequencedObject() {
        return sequencedObject;
    }

    public void setSequencedObject(boolean isSequenced) {
        this.sequencedObject = isSequenced;
    }

	public boolean isWrapper() {
		return isWrapper;
	}

	public void setIsWrapper(boolean value) {
		this.isWrapper = value;
	}

    public boolean isResultAlwaysXMLRoot() {
        return resultAlwaysXMLRoot;
    }

    public void setResultAlwaysXMLRoot(boolean resultAlwaysXMLRoot) {
        this.resultAlwaysXMLRoot = resultAlwaysXMLRoot;
    }

    @Override
    public DatabaseField getTypedField(DatabaseField field) {
        XMLField foundField = (XMLField) super.getTypedField(field);
        if(null != foundField) {
            return foundField;
        }
        StringTokenizer stringTokenizer = new StringTokenizer(field.getName(), "/");
        DatabaseField typedField = getTypedField(stringTokenizer);
        if(null == typedField) {
            DatabaseMapping selfMapping = objectBuilder.getMappingForField(new XMLField("."));
            if(null != selfMapping) {
                return selfMapping.getReferenceDescriptor().getTypedField(field); 
            }
        }
        return typedField;
    }

    protected DatabaseField getTypedField(StringTokenizer stringTokenizer) {
        String xPath = ""; 
        XMLField xmlField = new XMLField(); 
        xmlField.setNamespaceResolver(namespaceResolver); 
        while(stringTokenizer.hasMoreElements()) {
            String nextToken = stringTokenizer.nextToken();
            xmlField.setXPath(xPath + nextToken);
            xmlField.initialize();
            DatabaseMapping mapping = objectBuilder.getMappingForField(xmlField); 
            if(null == mapping) {
                XPathFragment xPathFragment = new XPathFragment(nextToken);
                if(xPathFragment.getIndexValue() > 0) {
                    xmlField.setXPath(xPath + nextToken.substring(0, nextToken.indexOf('[')));
                    xmlField.initialize();
                    mapping = objectBuilder.getMappingForField(xmlField);
                    if(null != mapping) {
                        if(mapping.isCollectionMapping()) {
                            if(mapping.getContainerPolicy().isListPolicy()) {
                                if(stringTokenizer.hasMoreElements()) {
                                    return ((XMLDescriptor) mapping.getReferenceDescriptor()).getTypedField(stringTokenizer); 
                                } else {
                                    return mapping.getField();
                                }
                            }
                        }
                    }
                }
            } else {
                if(stringTokenizer.hasMoreElements()) {
                    return ((XMLDescriptor) mapping.getReferenceDescriptor()).getTypedField(stringTokenizer);
                } else {
                    return mapping.getField();
                } 
            } 
            xPath = xPath + nextToken + "/"; 
        }
        return null; 
    }

}