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
package org.eclipse.persistence.internal.jpa.weaving;

import java.util.*;

import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataClass;
import org.eclipse.persistence.mappings.DatabaseMapping;

/**
 * Internal helper class that holds details of a persistent class.
 * Used by {@link PersistenceWeaver}
 */

public class ClassDetails {
    
    protected MetadataClass describedClass;    
    /** Name of this class. */
    protected String className;
    /** Superclass' name. */
    protected String superClassName;
    /** Superclass' ClassDetails - only populated if superclass is also persistent. */
    protected ClassDetails superClassDetails;
    /** Define if lazy value holders should be weaved in this class. */
    protected boolean shouldWeaveValueHolders = false;
    /** Define if change tracking should be weaved in this class. */    
    protected boolean shouldWeaveChangeTracking = false;
    /** Define if fetch groups should be weaved in this class. */    
    protected boolean shouldWeaveFetchGroups = false;
    /** Define if internal optimizations should be weaved in this class. */    
    protected boolean shouldWeaveInternal = false;
    /** Map of this class' persistent attributes where the key is the Attribute name. */
    protected Map<String, AttributeDetails> attributesMap;
    /** Map of this class' persistent get methods where the key is the getMethod name. */
    protected Map<String, AttributeDetails> getterMethodToAttributeDetails;
    /** Map of this class' persistent set methods where the key is the setMethod name. */    
    protected Map<String, AttributeDetails> setterMethodToAttributeDetails;
    /** Set of mappings using value holder indirection. */
    protected List<DatabaseMapping> lazyMappings;
    /** Determine if a JPA "mapped superclass". */
    protected boolean isMappedSuperClass = false;
    /** Determine if a JPA "embedable" (aggregate). */
    protected boolean isEmbedable = false;
    /** Determine if class uses attribute access, lazily initialized. */
    protected Boolean usesAttributeAccess = null;
    /** Determine if this class specifically implements a clone method */
    protected boolean implementsCloneMethod = false;
    /** Determine if a new constructor can be used to bypass setting variables to default values. */
    protected boolean shouldWeaveConstructorOptimization = true;
    
    public ClassDetails() {
    }
    
    public MetadataClass getDescribedClass(){
        return describedClass;
    }
    
    public String getClassName() {
        return className;
    }
    
    public void setDescribedClass(MetadataClass describedClass){
        this.describedClass = describedClass;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }

    public String getSuperClassName() {
        return superClassName;
    }
    
    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public ClassDetails getSuperClassDetails() {
        return superClassDetails;
    }
    
    public void setSuperClassDetails(ClassDetails superClassDetails) {
        this.superClassDetails = superClassDetails;
    }

    public boolean shouldWeaveValueHolders() {
        return shouldWeaveValueHolders;
    }
    
    public void setShouldWeaveValueHolders(boolean shouldWeaveValueHolders) {
        this.shouldWeaveValueHolders = shouldWeaveValueHolders;
    }
    
    public boolean shouldWeaveChangeTracking() {
        return shouldWeaveChangeTracking;
    }
    
    public void setShouldWeaveChangeTracking(boolean shouldWeaveChangeTracking) {
        this.shouldWeaveChangeTracking = shouldWeaveChangeTracking;
    }
    
    public void setShouldWeaveConstructorOptimization(boolean shouldWeaveConstructorOptimization) {
        this.shouldWeaveConstructorOptimization = shouldWeaveConstructorOptimization;
    }
    
    public boolean shouldWeaveFetchGroups() {
        return shouldWeaveFetchGroups;
    }
    
    public void setShouldWeaveFetchGroups(boolean shouldWeaveFetchGroups) {
        this.shouldWeaveFetchGroups = shouldWeaveFetchGroups;
    }
    
    public boolean shouldWeaveInternal() {
        return shouldWeaveInternal;
    }
    
    public void setShouldWeaveInternal(boolean shouldWeaveInternal) {
        this.shouldWeaveInternal = shouldWeaveInternal;
    }
    
    public Map<String, AttributeDetails> getAttributesMap() {
        return attributesMap;
    }

    public Map<String, AttributeDetails> getGetterMethodToAttributeDetails(){
        return getterMethodToAttributeDetails;
    }
    
    public List<DatabaseMapping> getLazyMappings(){
        return lazyMappings;
    }
    
    public Map<String, AttributeDetails> getSetterMethodToAttributeDetails(){
        return setterMethodToAttributeDetails;
    }
    
    public void setAttributesMap(Map<String, AttributeDetails> attributesMap) {
        this.attributesMap = attributesMap;
    }
    
    public void setGetterMethodToAttributeDetails(Map<String, AttributeDetails> map){
        this.getterMethodToAttributeDetails = map;
    }
    
    public void setLazyMappings(List<DatabaseMapping> lazyMappings){
        this.lazyMappings = lazyMappings;
    }
    
    public boolean getImplementsCloneMethod(){
        return implementsCloneMethod;
    }
    
    
    public void setImplementsCloneMethod(boolean implementsCloneMethod){
        this.implementsCloneMethod = implementsCloneMethod;
    }
    
    /**
     * Return the name of the most direct superclass that has a direct implementation of 
     * a clone method.
     * If there is not one, return null
     * @return
     */
    public String getNameOfSuperclassImplementingCloneMethod(){
        if (superClassDetails == null){
            return null;
        } else if (superClassDetails.getImplementsCloneMethod()){
            return superClassDetails.getClassName();
        } else {
            return superClassDetails.getNameOfSuperclassImplementingCloneMethod();
        }   
    }
    
    public boolean isMappedSuperClass(){
        return isMappedSuperClass;
    }
    
    public void setIsMappedSuperClass(boolean isMappedSuperClass){
        this.isMappedSuperClass = isMappedSuperClass;
    }
    
    public boolean isEmbedable(){
        return isEmbedable;
    }
    
    public void setIsEmbedable(boolean isEmbedable){
        this.isEmbedable = isEmbedable;
    }
    
    public void setSetterMethodToAttributeDetails(Map map){
        this.setterMethodToAttributeDetails = map;
    }
    
    /**
     * If one attribute of this class uses attribute access, by the JPA specification, all
     * attributes must use attribute access
     * 
     * This method assumes it is called when this class details is completely initialized.
     */
    public boolean usesAttributeAccess() {
        if (this.usesAttributeAccess != null){
            return this.usesAttributeAccess.booleanValue();
        } else {
            Iterator i = this.attributesMap.values().iterator();
            while (i.hasNext()){
                AttributeDetails details = (AttributeDetails)i.next();
                if (details.isMappedWithAttributeAccess()){
                    this.usesAttributeAccess = Boolean.TRUE;
                    return true;
                }
            }
            if (getSuperClassDetails() != null){
                return getSuperClassDetails().usesAttributeAccess();
            } else {
                this.usesAttributeAccess = Boolean.FALSE;
                return false;
            }
        }
    }
    
    public AttributeDetails getAttributeDetailsFromClassOrSuperClass(String attributeName){
        AttributeDetails attribute = attributesMap.get(attributeName);
        if (attribute == null && superClassDetails != null) {
            return superClassDetails.getAttributeDetailsFromClassOrSuperClass(attributeName);
        }
       return attribute; 
    }
    
    public boolean doesSuperclassWeaveChangeTracking(){
        if (getSuperClassDetails() == null){
            return false;
        }
        if (getSuperClassDetails().shouldWeaveChangeTracking()) {
            return true;
        }
        
        return getSuperClassDetails().doesSuperclassWeaveChangeTracking();
    }
    
    public boolean canWeaveChangeTracking(){
        if ((getSuperClassDetails() == null) || (!shouldWeaveChangeTracking())) {
            return shouldWeaveChangeTracking();
        }
        
        return getSuperClassDetails().canWeaveChangeTracking();
    }
    
    /**   
     * Returns true if 
     * Used with field access, and is set to false if transient variables are discovered
     */
    public boolean canWeaveConstructorOptimization(){
        if (!shouldWeaveConstructorOptimization || (getSuperClassDetails() == null)) {
            return shouldWeaveConstructorOptimization;
        }
        return getSuperClassDetails().canWeaveConstructorOptimization();
    }


    /**
     * Returns true if the given class name represents this class, or any
     * superclass that can be navigated to by recursively navigating up the
     * structure of superClassDetails stored in this class.
     * 
     * Assume java.lang.Object is in the hierarchy
     *
     * @param className
     * @return
     */
    public boolean isInMetadataHierarchy(String className){
        if (className.equals(Object.class.getName().replace('.', '/'))){
            return true;
        }            
        if (className.equals(this.className) || (superClassName != null && className.equals(superClassName))){
            return true;
        }
        if (superClassDetails != null){
            return superClassDetails.isInMetadataHierarchy(className);
        }
        return false;
    }
    
    /**
     * Returns true if the given class name represents this class, or any
     * superclass that can be navigated to by recursively navigating up the
     * structure of superClassDetails stored in this class.
     * 
     * Assume java.lang.Object is in the hierarchy
     *
     * @param className
     * @return
     */
    public boolean isInSuperclassHierarchy(String className){
        if (className.equals(Object.class.getName().replace('.', '/'))){
            return true;
        }            
        if (superClassName != null && className.equals(superClassName)){
            return true;
        }
        if (superClassDetails != null){
            return superClassDetails.isInMetadataHierarchy(className);
        }
        return false;
    }
    
}
