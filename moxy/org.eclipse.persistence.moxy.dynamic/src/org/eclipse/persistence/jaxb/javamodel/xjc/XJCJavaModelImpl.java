/*******************************************************************************
 * Copyright (c) 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Rick Barkhouse - 2.1 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.jaxb.javamodel.xjc;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.jaxb.javamodel.JavaAnnotation;
import org.eclipse.persistence.jaxb.javamodel.JavaClass;
import org.eclipse.persistence.jaxb.javamodel.JavaModel;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;

public class XJCJavaModelImpl implements JavaModel {

    private JCodeModel jCodeModel;
    private DynamicClassLoader dynamicClassLoader;
    private Map<String, JavaClass> javaModelClasses = new HashMap<String, JavaClass>();

    public XJCJavaModelImpl(JCodeModel codeModel, DynamicClassLoader dynLoader) {
        this.jCodeModel = codeModel;
        this.dynamicClassLoader = dynLoader;
    }

    public JavaClass getClass(Class<?> jClass) {
        if (jClass == null) {
            return null;
        }

        JavaClass cachedClass = this.javaModelClasses.get(jClass.getCanonicalName());
        if (cachedClass != null) {
            return cachedClass;
        }

        try {
            XJCJavaClassImpl jc = new XJCJavaClassImpl(jCodeModel._class(jClass.getCanonicalName()), jCodeModel, dynamicClassLoader);
            jc.setJavaModel(this);
            this.javaModelClasses.put(jClass.getCanonicalName(), jc);
            return jc;
        } catch (JClassAlreadyExistsException ex) {
            XJCJavaClassImpl jc = new XJCJavaClassImpl(jCodeModel._getClass(jClass.getCanonicalName()), jCodeModel, dynamicClassLoader);
            this.javaModelClasses.put(jClass.getCanonicalName(), jc);
            jc.setJavaModel(this);
            return jc;
        }
    }

    public JavaClass getClass(String className) {
        JavaClass cachedClass = this.javaModelClasses.get(className);
        if (cachedClass != null) {
            return cachedClass;
        }

        String componentName = className;
        boolean isArray = className.contains("[]");
        if (isArray) {
            componentName = className.replace("[]", "");
        }

        boolean isTyped = className.contains("<");
        if (isTyped) {
            // Only keep the generic part
            componentName = componentName.substring(0, className.indexOf('<'));
        }

        boolean isPrimitive = XMLConversionManager.getPrimitiveClass(componentName) != null;
        try {
            JavaClass jc = new XJCJavaClassImpl(jCodeModel._class(componentName), jCodeModel, dynamicClassLoader, isArray, isPrimitive);
            this.javaModelClasses.put(className, jc);
            return jc;
        } catch (JClassAlreadyExistsException ex) {
            JavaClass jc = new XJCJavaClassImpl(jCodeModel._getClass(componentName), jCodeModel, dynamicClassLoader, isArray, isPrimitive);
            this.javaModelClasses.put(className, jc);
            return jc;
        }
    }

    public Annotation getAnnotation(JavaAnnotation annotation, Class<?> jClass) {
        return ((XJCJavaAnnotationImpl) annotation).getJavaAnnotation();
    }

    public Map<String, JavaClass> getJavaModelClasses() {
        return javaModelClasses;
    }

    public void setJavaModelClasses(Map<String, JavaClass> javaModelClasses) {
        this.javaModelClasses = javaModelClasses;
    }

    public ClassLoader getClassLoader() {
        return this.dynamicClassLoader;
    }

}