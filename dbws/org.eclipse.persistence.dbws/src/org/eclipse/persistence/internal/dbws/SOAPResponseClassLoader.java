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

package org.eclipse.persistence.internal.dbws;

// Javase imports

// Java extension imports

// EclipseLink imports
import org.eclipse.persistence.internal.libraries.asm.ClassWriter;
import org.eclipse.persistence.internal.libraries.asm.CodeVisitor;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_PUBLIC;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_SUPER;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ALOAD;
import static org.eclipse.persistence.internal.libraries.asm.Constants.INVOKESPECIAL;
import static org.eclipse.persistence.internal.libraries.asm.Constants.RETURN;
import static org.eclipse.persistence.internal.libraries.asm.Constants.V1_5;

/**
 * <p><b>INTERNAL</b>: A subclass of {@link ClassLoader} that exposes a build method to the hidden
 * private {@link ClassLoader#defineClass} method.
 *
 * This loader is only used to define auto-generated subclasses of the {@link SOAPResponse}
 * class; it should <b>never</b> be used to load actual existing classes.
 *
 * @author Mike Norman
 */
public class SOAPResponseClassLoader extends ClassLoader {

    public static final String SOAP_RESPONSE_CLASSNAME_SLASHES =
      SOAPResponse.class.getName().replace('.', '/');

    public SOAPResponseClassLoader(ClassLoader parent) {
      super(parent);
    }

    public Class<?> buildClass(String className) {
      byte[] data = generateClassBytes(className);
      return super.defineClass(className, data, 0, data.length);
    }

    protected byte[] generateClassBytes(String className) {
      /*
       * Pattern is as follows:
       *   public class 'classname' extends org.eclipse.persistence.internal.dbws.SOAPResponse {
       *     public 'classname'() {
       *       super();
       *     }
       *   }
       */
      ClassWriter cw = new ClassWriter(true);
      cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, className, SOAP_RESPONSE_CLASSNAME_SLASHES, null, null);

      CodeVisitor cv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      cv.visitVarInsn(ALOAD, 0);
      cv.visitMethodInsn(INVOKESPECIAL, SOAP_RESPONSE_CLASSNAME_SLASHES, "<init>", "()V");
      cv.visitInsn(RETURN);
      cv.visitMaxs(0, 0);

      cw.visitEnd();
      return cw.toByteArray();
    }
}
