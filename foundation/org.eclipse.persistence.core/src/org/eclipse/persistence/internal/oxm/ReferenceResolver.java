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
package org.eclipse.persistence.internal.oxm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;

import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.InheritancePolicy;
import org.eclipse.persistence.exceptions.ConversionException;
import org.eclipse.persistence.exceptions.XMLMarshalException;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.identitymaps.CacheId;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.AttributeAccessor;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.oxm.IDResolver;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.mappings.XMLCollectionReferenceMapping;
import org.eclipse.persistence.oxm.mappings.XMLInverseReferenceMapping;
import org.eclipse.persistence.oxm.mappings.XMLMapping;
import org.eclipse.persistence.oxm.mappings.XMLObjectReferenceMapping;

/**
 * This class is leveraged by reference mappings. It plays 3 roles:
 * <ul>
 * <li>Stores objects with an ID</li>
 * <li>Stores key based relationships</li>
 * <li>Resolves key based relationships based on the objects stored by ID</li>
 * </ul>
 */
public class ReferenceResolver {

    private ArrayList<Reference> references;    
    private ReferenceKey lookupKey;
    private Map<Class, Map<Object, Object>> cache;

    /**
     * The default constructor initializes the list of References.
     */
    public ReferenceResolver() {
        references = new ArrayList();
        lookupKey = new ReferenceKey(null, null);
        cache = new HashMap<Class, Map<Object, Object>>();
    }

    /**
     * Add a Reference object to the list - these References will
     * be resolved after unmarshalling is complete.
     *
     * @param ref
     */
    public void addReference(Reference ref) {
        references.add(ref);
    }

    /**
     * INTERNAL:
     * Create primary key values to be used for cache lookup.  The map
     * of primary keys on the reference is keyed on the reference descriptors primary
     * key field names.  Each of these primary keys contains all of the values for a
     * particular key - in the order that they we read in from the document.  For
     * example, if the key field names are A, B, and C, and there are three reference
     * object instances, then the hashmap would have the following:
     * (A=[1,2,3], B=[X,Y,Z], C=[Jim, Joe, Jane]).  If the primary key field names on
     * the reference descriptor contained [B, C, A], then the result of this method call
     * would be reference.primaryKeys=([X, Jim, 1], [Y, Joe, 2], [Z, Jane, 3]).
     *
     * @param reference
     */
    private void createPKVectorsFromMap(Reference reference, XMLCollectionReferenceMapping mapping) {
        ClassDescriptor referenceDescriptor = mapping.getReferenceDescriptor();
        Vector pks = new Vector();
        if(null == referenceDescriptor) {
            CacheId pkVals = (CacheId) reference.getPrimaryKeyMap().get(null);
            if(null == pkVals) {
                return;
            }
            for(int x=0;x<pkVals.getPrimaryKey().length; x++) {
                Object[] values = new Object[1];
                values[0] = pkVals.getPrimaryKey()[x];
                pks.add(new CacheId(values));
            }
        } else{ 
            Vector pkFields = referenceDescriptor.getPrimaryKeyFieldNames();
            if (pkFields.isEmpty()) {
                return;
            }

            boolean init = true;

            // for each primary key field name
            for (Iterator pkFieldNameIt = pkFields.iterator(); pkFieldNameIt.hasNext(); ) {
                CacheId pkVals = (CacheId) reference.getPrimaryKeyMap().get(pkFieldNameIt.next());

                if (pkVals == null) {
                    return;
                }
                // initialize the list of pk vectors once and only once
                if (init) {
                    for (int i=0; i<pkVals.getPrimaryKey().length; i++) {
                        pks.add(new CacheId(new Object[0]));
                    }
                    init = false;
                }

                // now add each value for the current target key to it's own vector
                for (int i=0; i<pkVals.getPrimaryKey().length; i++) {
                    Object val = pkVals.getPrimaryKey()[i];
                    ((CacheId)pks.get(i)).add(val);
                }
            }
        }
        reference.setPrimaryKey(pks);
    }

    /**
     * Retrieve the reference for a given mapping instance.
     *
     * @param mapping
     */
    public Reference getReference(XMLObjectReferenceMapping mapping, Object sourceObject) {
        for (int x = 0; x < references.size(); x++) {
            Reference reference = (Reference) references.get(x);
            if (reference.getMapping() == mapping && reference.getSourceObject() == sourceObject) {
                return reference;
            }
        }
        return null;
    }
    
    /**
     * Return a reference for the given mapping and source object, that doesn't already
     * contain an entry for the provided field. 
     * @return
     */
    public Reference getReference(XMLObjectReferenceMapping mapping, Object sourceObject, XMLField xmlField) {
        XMLField targetField = (XMLField)mapping.getSourceToTargetKeyFieldAssociations().get(xmlField);
        String tgtXpath = null;
        if(!(mapping.getReferenceClass() == null || mapping.getReferenceClass() == Object.class)) {
            if(targetField != null) {
                tgtXpath = targetField.getXPath();
            }
        }
        for (int x = 0; x < references.size(); x++) {
            Reference reference = (Reference) references.get(x);
            if (reference.getMapping() == mapping && reference.getSourceObject() == sourceObject) {
                if(reference.getPrimaryKeyMap().get(tgtXpath) == null) {
                    return reference;
                }
            }
        }
        return null;
    }    

    /**
     * INTERNAL:
     * @param session typically will be a unit of work
     * @param userSpecifiedResolver a user-provided subclass of IDResolver, may be null 
     */
    public void resolveReferences(AbstractSession session, IDResolver userSpecifiedResolver) {
        for (int x = 0, referencesSize = references.size(); x < referencesSize; x++) {
            Reference reference = (Reference) references.get(x);
            Object referenceSourceObject = reference.getSourceObject();
            if (reference.getMapping() instanceof XMLCollectionReferenceMapping) {
                XMLCollectionReferenceMapping mapping = (XMLCollectionReferenceMapping) reference.getMapping();
                ContainerPolicy cPolicy = mapping.getContainerPolicy();
                //container should never be null                
                Object container = reference.getContainer();              

                // create vectors of primary key values - one vector per reference instance
                createPKVectorsFromMap(reference, mapping);
                // if the we could not generate the primary key for the reference, it will not resolve - skip it
                if (reference.getPrimaryKey() == null) {
                    continue;
                }
                // loop over each pk vector and get object from cache - then add to collection and set on object
                Object value = null;
                if(!mapping.isWriteOnly()) {
                    for (Iterator pkIt = ((Vector)reference.getPrimaryKey()).iterator(); pkIt.hasNext();) {
                        CacheId primaryKey = (CacheId) pkIt.next();
                        value = getValue(session, reference, primaryKey);
                        if (value != null) {
                             cPolicy.addInto(value, container, session);
                        }
                    }
                }
                // for each reference, get the source object and add it to the container policy
                // when finished, set the policy on the mapping
                mapping.setAttributeValueInObject(referenceSourceObject, container);
                XMLInverseReferenceMapping inverseReferenceMapping = mapping.getInverseReferenceMapping();
                if(inverseReferenceMapping != null && value != null) {
                    AttributeAccessor backpointerAccessor = inverseReferenceMapping.getAttributeAccessor();
                    ContainerPolicy backpointerContainerPolicy = inverseReferenceMapping.getContainerPolicy();
                    if(backpointerContainerPolicy == null) {
                        backpointerAccessor.setAttributeValueInObject(value, referenceSourceObject);
                    } else {
                        Object backpointerContainer = backpointerAccessor.getAttributeValueFromObject(value);
                        if(backpointerContainer == null) {
                            backpointerContainer = backpointerContainerPolicy.containerInstance();
                            backpointerAccessor.setAttributeValueInObject(value, backpointerContainer);
                        }
                        backpointerContainerPolicy.addInto(referenceSourceObject, backpointerContainer, session);
                    }
                }
            } else if (reference.getMapping() instanceof XMLObjectReferenceMapping) {
                CacheId primaryKey = (CacheId) reference.getPrimaryKey();
                Object value = null;
                if (userSpecifiedResolver != null) {
                    final Callable c;
                    try {
                        if (primaryKey.getPrimaryKey().length > 1) {
                            Map<String, Object> idWrapper = new HashMap<String, Object>();
                            for (int y = 0; y < primaryKey.getPrimaryKey().length; y++) {
                                XMLObjectReferenceMapping refMapping = (XMLObjectReferenceMapping) reference.getMapping();
                                String idName = refMapping.getReferenceDescriptor().getPrimaryKeyFieldNames().elementAt(y);
                                Object idValue = primaryKey.getPrimaryKey()[y];
                                idWrapper.put(idName, idValue);
                            }
                            c = userSpecifiedResolver.resolve(idWrapper, reference.getTargetClass());
                        } else {
                            c = userSpecifiedResolver.resolve(primaryKey.getPrimaryKey()[0], reference.getTargetClass());
                        }
                        if (c != null) {
                            value = c.call();
                        }
                    } catch (Exception e) {
                        throw XMLMarshalException.unmarshalException(e);
                    }
                } else {
                    value = getValue(session, reference, primaryKey);
                }

                XMLObjectReferenceMapping mapping = (XMLObjectReferenceMapping)reference.getMapping();
                if (value != null) {
                    mapping.setAttributeValueInObject(reference.getSourceObject(), value);
                }
                if (null != reference.getSetting()) {
                    reference.getSetting().setValue(value);
                }

                XMLInverseReferenceMapping inverseReferenceMapping = mapping.getInverseReferenceMapping();
                if(inverseReferenceMapping != null) {
                    AttributeAccessor backpointerAccessor = inverseReferenceMapping.getAttributeAccessor();
                    ContainerPolicy backpointerContainerPolicy = inverseReferenceMapping.getContainerPolicy();
                    if(backpointerContainerPolicy == null) {
                        backpointerAccessor.setAttributeValueInObject(value, referenceSourceObject);
                    } else {
                        Object backpointerContainer = backpointerAccessor.getAttributeValueFromObject(value);
                        if(backpointerContainer == null) {
                            backpointerContainer = backpointerContainerPolicy.containerInstance();
                            backpointerAccessor.setAttributeValueInObject(value, backpointerContainer);
                        }
                        backpointerContainerPolicy.addInto(reference.getSourceObject(), backpointerContainer, session);
                    }
                }
            }
        }

        // reset the references list
        references = new ArrayList<Reference>();
        cache.clear();
    }

    private Object getValue(AbstractSession session, Reference reference, CacheId primaryKey) {
        Class referenceTargetClass = reference.getTargetClass();
        if(null == referenceTargetClass || referenceTargetClass == ClassConstants.OBJECT) {
            for(Object entry : session.getDescriptors().values()) {
                Object value = null;
                XMLDescriptor targetDescriptor = (XMLDescriptor) entry;
                List pkFields = targetDescriptor.getPrimaryKeyFields();
                if(null != pkFields && 1 == pkFields.size()) {
                    XMLField pkField = (XMLField) pkFields.get(0);
                    pkField = (XMLField) targetDescriptor.getTypedField(pkField);
                    Class targetType = pkField.getType();
                    if(targetType == ClassConstants.STRING || targetType == ClassConstants.OBJECT) {
                    	value = getValue(targetDescriptor.getJavaClass(), primaryKey);
                    } else {
                        try {
                            Object[] pkValues = primaryKey.getPrimaryKey();
                            Object[] convertedPkValues = new Object[pkValues.length];
                            for(int x=0; x<pkValues.length; x++) {
                                convertedPkValues[x] = session.getDatasourcePlatform().getConversionManager().convertObject(pkValues[x], targetType);
                            }
                            value = getValue(targetDescriptor.getJavaClass(), new CacheId(convertedPkValues));
                        } catch(ConversionException e) {
                        }
                    }
                    if(null != value) {
                        return value;
                    }
                }
            }
            return null;
        } else {
            Object value = getValue(referenceTargetClass, primaryKey);
            if(null == value) {
                DatabaseMapping mapping = (DatabaseMapping) reference.getMapping();
                ClassDescriptor targetDescriptor = mapping.getReferenceDescriptor();
                if(targetDescriptor.hasInheritance()) {
                    InheritancePolicy inheritancePolicy = targetDescriptor.getInheritancePolicy();
                    List<ClassDescriptor> childDescriptors = inheritancePolicy.getAllChildDescriptors();
                    for(ClassDescriptor childDescriptor : childDescriptors) {
                        value = getValue(childDescriptor.getJavaClass(), primaryKey);
                        if(null != value) {
                            return value;
                        }
                    }
                }
            }
            return value;
        }
    }

    private Object getValue(Class clazz, CacheId primaryKey) {
        Map<Object, Object> keyToObject = cache.get(clazz);
        if(null != keyToObject) {
        	return keyToObject.get(primaryKey);
        }
        return null;
    }

    private class ReferenceKey {
        private Object sourceObject;
        private XMLMapping mapping;

        public ReferenceKey(Object sourceObject, XMLMapping mapping) {
            this.sourceObject = sourceObject;
            this.mapping = mapping;
        }
        
        public Object getSourceObject() {
            return sourceObject;
        }
        
        public XMLMapping getMapping() {
            return mapping;
        }
        
        public void setSourceObject(Object obj) {
            this.sourceObject = obj;
        }
        
        public void setMapping(XMLMapping mapping) {
            this.mapping = mapping;
        }
        
        @Override
        public int hashCode() {
            return this.mapping.hashCode() ^ this.sourceObject.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            if(obj == null) {
                return false;
            }
            if(obj.getClass() != this.getClass()) {
                return false;
            }
            ReferenceKey key = (ReferenceKey)obj;
            return this.sourceObject == key.getSourceObject() && this.mapping == key.getMapping();
        }
    }

    /**
     * Store an instance by key based on a mapped class.  These values will be 
     * used when it comes time to resolve the references.
     * @since EclipseLink 2.5.0
     */
    public void putValue(Class clazz, Object key, Object object) {
        Map<Object, Object> keyToObject = cache.get(clazz);
        if(null == keyToObject) {
            keyToObject = new HashMap<Object, Object>();
            cache.put(clazz, keyToObject);
        }
        keyToObject.put(key, object);
    }

}