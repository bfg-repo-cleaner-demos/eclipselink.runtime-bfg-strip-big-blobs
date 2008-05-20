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
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata.listeners;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import java.security.AccessController;
import java.security.PrivilegedActionException;

import java.util.HashSet;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.jpa.metadata.MetadataDescriptor;
import org.eclipse.persistence.internal.jpa.metadata.MetadataLogger;
import org.eclipse.persistence.internal.jpa.metadata.ORMetadata;

import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataAccessibleObject;
import org.eclipse.persistence.internal.jpa.metadata.accessors.objects.MetadataMethod;

import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.internal.security.PrivilegedClassForName;
import org.eclipse.persistence.internal.security.PrivilegedGetDeclaredMethods;
import org.eclipse.persistence.internal.security.PrivilegedGetMethods;

/**
 * A MetadataEntityListener and is placed on the owning entity's descriptor. 
 * Callback methods from an EntityListener require a signature on the method. 
 * Namely, they must have an Object parameter.
 * 
 * @author Guy Pelletier
 * @since TopLink 10.1.3/EJB 3.0 Preview
 */
public class EntityListenerMetadata extends ORMetadata {
    private Class m_entityListenerClass;
    
    protected EntityListener m_listener;
    
    private String m_className;
    private String m_postLoad;
    private String m_postPersist;
    private String m_postRemove;
    private String m_postUpdate;
    private String m_prePersist;
    private String m_preRemove;
    private String m_preUpdate;

    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public EntityListenerMetadata() {
        super("<entity-listener>");
    }

    /**
     * INTERNAL:
     */
    public EntityListenerMetadata(Annotation entityListeners, Class entityListenerClass, MetadataAccessibleObject accessibleObject) {
        super(entityListeners, accessibleObject);
        
        m_entityListenerClass = entityListenerClass;
    }

    /**
     * INTERNAL:
     * This method should be called when dealing with default listeners.
     */
    public Object clone() {
        EntityListenerMetadata listener = new EntityListenerMetadata();
    
        listener.setClassName(getClassName());
        
        listener.setPostLoad(getPostLoad());
        listener.setPostPersist(getPostPersist());
        listener.setPostRemove(getPostRemove());
        listener.setPostUpdate(getPostUpdate());
        listener.setPrePersist(getPrePersist());
        listener.setPreRemove(getPreRemove());
        listener.setPreUpdate(getPreUpdate());
        
        return listener;
    }
    
    /**
     * INTERNAL:
     * Find the method in the list where method.getName() == methodName.
     */
    protected Method getCallbackMethod(String methodName, Method[] methods) {
        Method method = getMethod(methodName, methods);
        
        if (method == null) {
            throw ValidationException.invalidCallbackMethod(m_listener.getListenerClass(), methodName);
        }
        
        return method;
    }
    
    /**
     * INTERNAL:
     * Returns a list of methods from the given class, which can have private, 
     * protected, package and public access, AND will also return public 
     * methods from superclasses.
     */
    Method[] getCandidateCallbackMethodsForEntityListener() {
        HashSet candidateMethods = new HashSet();
        Class listenerClass = m_listener.getListenerClass();
        
        // Add all the declared methods ...
        Method[] declaredMethods = getDeclaredMethods(listenerClass);
        for (int i = 0; i < declaredMethods.length; i++) {
            candidateMethods.add(declaredMethods[i]);
        }
        
        // Now add any public methods from superclasses ...
        Method[] methods = getMethods(listenerClass);
        for (int i = 0; i < methods.length; i++) {
            if (candidateMethods.contains(methods[i])) {
                continue;
            }
            
            candidateMethods.add(methods[i]);
        }
        
        return (Method[]) candidateMethods.toArray(new Method[candidateMethods.size()]);
    }
    
    /**
     * INTERNAL:
     * Load a class from a given class name.
     */
    Class getClassForName(String classname, ClassLoader loader) {
        try {
            if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
                try {
                    return (Class) AccessController.doPrivileged(new PrivilegedClassForName(classname, true, loader));
                } catch (PrivilegedActionException exception) {
                    throw ValidationException.unableToLoadClass(classname, exception.getException());
                }
            } else {
                return PrivilegedAccessHelper.getClassForName(classname, true, loader);
            }
        } catch (ClassNotFoundException exception) {
            throw ValidationException.unableToLoadClass(classname, exception);
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getClassName() {
        return m_className;
    }
    
    /**
     * INTERNAL:
     * Get the declared methods from a class using the doPriveleged security
     * access. This call returns all methods (private, protected, package and
     * public) on the give class ONLY. It does not traverse the superclasses.
     */
    Method[] getDeclaredMethods(Class cls) {
        if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
            try {
                return (Method[])AccessController.doPrivileged(new PrivilegedGetDeclaredMethods(cls));
            } catch (PrivilegedActionException exception) {
                // we will not get here, there are no checked exceptions in this call
                return null;
            }
        } else {
            return org.eclipse.persistence.internal.security.PrivilegedAccessHelper.getDeclaredMethods(cls);
        }
    }

    /**
     * INTERNAL:
     */
    @Override
    public String getIdentifier() {
        return m_className;
    }
    
    /**
     * INTERNAL:
     * Find the method in the list where method.getName() == methodName.
     */
    Method getMethod(String methodName, Method[] methods) {
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
        
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        
        return null;
    }
    
    /**
     * INTERNAL:
     * Get the methods from a class using the doPriveleged security access. 
     * This call returns only public methods from the given class and its 
     * superclasses.
     */
    Method[] getMethods(Class cls) {
        if (PrivilegedAccessHelper.shouldUsePrivilegedAccess()){
            try {
                return (Method[])AccessController.doPrivileged(new PrivilegedGetMethods(cls));
            } catch (PrivilegedActionException exception) {
                return null;
            }
        } else {
            return PrivilegedAccessHelper.getMethods(cls);
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getPostLoad() {
        return m_postLoad;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping. 
     */
    public String getPostPersist() {
        return m_postPersist;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getPostRemove() {
        return m_postRemove;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping
     */
    public String getPostUpdate() {
        return m_postUpdate;
    }

    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getPrePersist() {
        return m_prePersist;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getPreRemove() {
        return m_preRemove;
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public String getPreUpdate() {
        return m_preUpdate;
    }

    /**
     * INTERNAL:
     */
    @Override
    public void initXMLObject(MetadataAccessibleObject accessibleObject) {
        super.initXMLObject(accessibleObject);
    
        m_entityListenerClass = initXMLClassName(m_className);
    }
    
    /**
     * INTERNAL: 
     */
    public void process(MetadataDescriptor descriptor, ClassLoader loader, boolean isDefaultListener) {
        // Make sure the entityListenerClass is initialized (default listeners
        // are cloned and m_entityListenerClass may be null)
        if (m_entityListenerClass == null) {
            m_entityListenerClass = getClassForName(m_className, loader);
        }
        
        // Initialize the listener class (reload the listener class)
        m_listener = new EntityListener(getClassForName(m_entityListenerClass.getName(), loader), descriptor.getJavaClass());
        
        // Process the callback methods defined from XML and annotations.
        processCallbackMethods(getCandidateCallbackMethodsForEntityListener(), descriptor.getLogger());
    
        // Add the listener to the descriptor.
        if (isDefaultListener) {
            descriptor.addDefaultEventListener(m_listener);
        } else {
            descriptor.addEntityListenerEventListener(m_listener);
        }
    }
    
    /**
     * INTERNAL:
     * Process the XML defined call back methods.
     */
    protected void processCallbackMethods(Method[] methods, MetadataLogger logger) {
        // 1 - Set the XML specified methods first.
        if (m_postLoad != null) {
            setPostLoad(getCallbackMethod(m_postLoad, methods));
        }   
        
        if (m_postPersist != null) {
            setPostPersist(getCallbackMethod(m_postPersist, methods));
        }
        
        if (m_postRemove != null) {
            setPostRemove(getCallbackMethod(m_postRemove, methods));
        }
        
        if (m_postUpdate != null) {
            setPostUpdate(getCallbackMethod(m_postUpdate, methods));
        }
        
        if (m_prePersist != null) {
            setPrePersist(getCallbackMethod(m_prePersist, methods));
        }

        if (m_preRemove != null) {
            setPreRemove(getCallbackMethod(m_preRemove, methods));
        }
        
        if (m_preUpdate != null) {
            setPreUpdate(getCallbackMethod(m_preUpdate, methods));
        }
        
        // 2 - Set any annotation defined methods second.
        for (Method method : methods) {
            MetadataMethod metadataMethod = new MetadataMethod(method, logger);
            
            if (metadataMethod.isAnnotationPresent(PostLoad.class)) {
                setPostLoad(method);
            }
            
            if (metadataMethod.isAnnotationPresent(PostPersist.class)) {
                setPostPersist(method);
            }
            
            if (metadataMethod.isAnnotationPresent(PostRemove.class)) {
                setPostRemove(method);
            }
            
            if (metadataMethod.isAnnotationPresent(PostUpdate.class)) {
                setPostUpdate(method);
            }
            
            if (metadataMethod.isAnnotationPresent(PrePersist.class)) {
                setPrePersist(method);
            }
            
            if (metadataMethod.isAnnotationPresent(PreRemove.class)) {
                setPreRemove(method);
            }
            
            if (metadataMethod.isAnnotationPresent(PreUpdate.class)) {
                setPreUpdate(method);
            }
        }
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setClassName(String className) {
        m_className = className;
    }

    /**
     * INTERNAL:
     * Set the post load event method on the listener.
     */
    protected void setPostLoad(Method method) {
        m_listener.setPostBuildMethod(method);
        m_listener.setPostCloneMethod(method);
        m_listener.setPostRefreshMethod(method);
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setPostLoad(String postLoad) {
        m_postLoad = postLoad;
    }
    
    /**
     * INTERNAL:
     * Set the post persist event method on the listener.
     */
    protected void setPostPersist(Method method) {
        m_listener.setPostInsertMethod(method); 
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setPostPersist(String postPersist) {
        m_postPersist = postPersist;
    }
    
    /**
     * INTERNAL:
     * Set the post remove event method on the listener.
     */
    protected void setPostRemove(Method method) {
        m_listener.setPostDeleteMethod(method);
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setPostRemove(String postRemove) {
        m_postRemove = postRemove;
    }
    
    /**
     * INTERNAL:
     * * Set the post update event method on the listener.
     */
    protected void setPostUpdate(Method method) {
        m_listener.setPostUpdateMethod(method);
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setPostUpdate(String postUpdate) {
        m_postUpdate = postUpdate;
    }
    
    /**
     * INTERNAL:
     * Set the pre persist event method on the listener.
     */
    protected void setPrePersist(Method method) {
        m_listener.setPrePersistMethod(method);
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping
     */
    public void setPrePersist(String prePersist) {
        m_prePersist = prePersist;
    }
    
    /**
     * INTERNAL:
     * Set the pre remove event method on the listener.
     */
    protected void setPreRemove(Method method) {
        m_listener.setPreRemoveMethod(method);
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setPreRemove(String preRemove) {
        m_preRemove = preRemove;
    }
    
    /**
     * INTERNAL:
     * Set the pre update event method on the listener.
     */
    protected void setPreUpdate(Method method) {
        m_listener.setPreUpdateWithChangesMethod(method);
    }
    
    /**
     * INTERNAL:
     * Used for OX mapping.
     */
    public void setPreUpdate(String preUpdate) {
        m_preUpdate = preUpdate;
    }
}
