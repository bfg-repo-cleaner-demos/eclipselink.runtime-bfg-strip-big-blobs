/*******************************************************************************
 * Copyright (c) 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Rick Barkhouse - 2.1 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.jaxb.javamodel.xjc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.persistence.dynamic.DynamicClassLoader;
import org.eclipse.persistence.exceptions.JAXBException;
import org.eclipse.persistence.internal.security.PrivilegedAccessHelper;
import org.eclipse.persistence.jaxb.javamodel.JavaAnnotation;
import org.eclipse.persistence.jaxb.javamodel.JavaClass;
import org.eclipse.persistence.jaxb.javamodel.JavaField;

import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JPrimitiveType;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;

public class XJCJavaFieldImpl implements JavaField {

    private JFieldVar xjcField;
    private JCodeModel jCodeModel;
    private DynamicClassLoader dynamicClassLoader;
    private JavaClass owningClass;

    private static Field JVAR_ANNOTATIONS = null;
    static {
        try {
            JVAR_ANNOTATIONS = PrivilegedAccessHelper.getDeclaredField(JVar.class, "annotations", true);
        } catch (Exception e) {
            throw JAXBException.errorCreatingDynamicJAXBContext(e);
        }
    }

    public XJCJavaFieldImpl(JFieldVar javaField, JCodeModel codeModel, DynamicClassLoader loader, JavaClass owner) {
        this.xjcField = javaField;
        this.jCodeModel = codeModel;
        this.dynamicClassLoader = loader;
        this.owningClass = owner;
    }

    @SuppressWarnings("unchecked")
    public JavaAnnotation getAnnotation(JavaClass aClass) {
        if (aClass != null) {
            Collection<JAnnotationUse> annotations = null;

            try {
                annotations = (Collection<JAnnotationUse>) PrivilegedAccessHelper.getValueFromField(JVAR_ANNOTATIONS, xjcField);
            } catch (Exception e) {
            }

            if (annotations == null) {
                return null;
            }

            for (JAnnotationUse annotationUse : annotations) {
                XJCJavaAnnotationImpl xjcAnnotation = new XJCJavaAnnotationImpl(annotationUse, dynamicClassLoader);
                if (xjcAnnotation.getJavaAnnotationClass().getCanonicalName().equals(aClass.getQualifiedName())) {
                    return xjcAnnotation;
                }
            }
            // Didn't find annotation so return null
            return null;
        }
        // aClass was null so return null
        return null;
    }

    @SuppressWarnings("unchecked")
    public Collection<JavaAnnotation> getAnnotations() {
        ArrayList<JavaAnnotation> annotationsList = new ArrayList<JavaAnnotation>();

        Collection<JAnnotationUse> annotations = null;

        try {
            annotations = (Collection<JAnnotationUse>) PrivilegedAccessHelper.getValueFromField(JVAR_ANNOTATIONS, xjcField);
        } catch (Exception e) {
        }

        for (JAnnotationUse annotationUse : annotations) {
            XJCJavaAnnotationImpl xjcAnnotation = new XJCJavaAnnotationImpl(annotationUse, dynamicClassLoader);
            annotationsList.add(xjcAnnotation);
        }
        return annotationsList;
    }

    public int getModifiers() {
        return xjcField.mods().getValue();
    }

    public String getName() {
        return xjcField.name();
    }

    @SuppressWarnings("unchecked")
    public JavaClass getResolvedType() {
        JType type = xjcField.type();
        JType basis = null;

        try {
            // Check to see if this type has a 'basis' field.
            // This would indicate it is a "parameterized type" (e.g. List<Employee>).
            // Cannot cache this field because JNarrowedClass is a protected class.
            Field basisField = PrivilegedAccessHelper.getDeclaredField(type.getClass(), "basis", true);
            basis = (JClass) PrivilegedAccessHelper.getValueFromField(basisField, type);
        } catch (Exception e) {
            // "basis" field not found
        }

        JClass classToReturn = null;

        if (type.isPrimitive()) {
            JPrimitiveType pType = (JPrimitiveType) type;
            classToReturn = pType.boxify();
        } else {
            if (type.getClass().getName().contains("JArrayClass")) {
                classToReturn = (JClass) type;
            } else {
                try {
                    classToReturn = jCodeModel._class(basis != null ? basis.fullName() : type.fullName());
                } catch (JClassAlreadyExistsException ex) {
                    classToReturn = jCodeModel._getClass(basis != null ? basis.fullName() : type.fullName());
                }
            }
        }

        if (basis != null) {
            try {
                // Cannot cache this field because JNarrowedClass is a protected class.
                Field argsField = PrivilegedAccessHelper.getDeclaredField(type.getClass(), "args", true);
                List<JClass> args = (List<JClass>) PrivilegedAccessHelper.getValueFromField(argsField, type);
                for (JClass jClass : args) {
                    ((JDefinedClass) classToReturn).generify("param", jClass);
                }
            } catch (Exception e) {
                throw JAXBException.errorCreatingDynamicJAXBContext(e);
            }
        }

        if (classToReturn instanceof JDefinedClass) {
            if (((XJCJavaClassImpl) getOwningClass()).getJavaModel() != null) {
                return ((XJCJavaClassImpl) getOwningClass()).getJavaModel().getClass(classToReturn.fullName());
            }
            return new XJCJavaClassImpl((JDefinedClass) classToReturn, jCodeModel, dynamicClassLoader);
        } else {
            if (((XJCJavaClassImpl) this.owningClass).getJavaModel() != null) {
                return ((XJCJavaClassImpl) this.owningClass).getJavaModel().getClass(classToReturn.fullName());
            }
            return new XJCJavaClassImpl((JDefinedClass) classToReturn, jCodeModel, dynamicClassLoader);
        }
    }

    public boolean isFinal() {
        return Modifier.isFinal(getModifiers());
    }

    public boolean isAbstract() {
        return Modifier.isAbstract(getModifiers());
    }

    public boolean isPrivate() {
        return Modifier.isPrivate(getModifiers());
    }

    public boolean isProtected() {
        return Modifier.isProtected(getModifiers());
    }

    public boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    public boolean isStatic() {
        return Modifier.isStatic(getModifiers());
    }

    public boolean isSynthetic() {
        throw new UnsupportedOperationException("isSynthetic");
    }

    public boolean isEnumConstant() {
        return getOwningClass().isEnum();
    }

    public JavaAnnotation getDeclaredAnnotation(JavaClass aClass) {
        return getAnnotation(aClass);
    }

    public Collection<JavaAnnotation> getDeclaredAnnotations() {
        return getAnnotations();
    }

    public void setOwningClass(JavaClass owningClass) {
        this.owningClass = owningClass;
    }

    public JavaClass getOwningClass() {
        return this.owningClass;
    }
}