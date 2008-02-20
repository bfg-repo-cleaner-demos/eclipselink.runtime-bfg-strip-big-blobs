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
 ******************************************************************************/
package org.eclipse.persistence.internal.weaving;

// J2SE imports
import java.lang.instrument.*;
import java.io.FileOutputStream;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.File;
import javax.persistence.spi.ClassTransformer;

// ASM imports
import org.eclipse.persistence.internal.libraries.asm.*;
import org.eclipse.persistence.internal.libraries.asm.attrs.Attributes;

// TopLink imports
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.logging.SessionLog;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.internal.helper.Helper;


/**
 * INTERNAL:
 * This class performs dynamic bytecode weaving: for each attribute
 * mapped with One To One mapping with Basic Indirection it substitutes the
 * original attribute's type for ValueHolderInterface. 
 */
public class PersistenceWeaver implements ClassTransformer {
    
    public static final String WEAVING_OUTPUT_PATH = "eclipselink.weaving.output.path";
    public static final String WEAVING_SHOULD_OVERWRITE = "eclipselink.weaving.overwrite.existing";
    public static final String WEAVER_NOT_OVERWRITING = "weaver_not_overwriting";
    public static final String WEAVER_COULD_NOT_WRITE = "weaver_could_not_write";
    
    protected Session session; // for logging
    // Map<String, ClassDetails> where the key is className in JVM '/' format 
    protected Map classDetailsMap;
    
    public PersistenceWeaver(Session session, Map classDetailsMap) {
        this.session = session;
        this.classDetailsMap = classDetailsMap;
    }
    
    public Map getClassDetailsMap() {
        return classDetailsMap;
    }

    // @Override: well, not precisely. I wanted the code to be 1.4 compatible,
    // so the method is written without any Generic type <T>'s in the signature
    public byte[] transform(ClassLoader loader, String className,
            Class classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {    
        try {
            /*
             * The ClassFileTransformer callback - when called by the JVM's
             * Instrumentation implementation - is invoked for every class loaded.
             * Thus, we must check the classDetailsMap to see if we are 'interested'
             * in the class.
             * 
             * Note: when invoked by the OC4J wrapper class
             * org.eclipse.persistence.internal.jpa.oc4j.OC4JClassTransformer,
             * callbacks are made only for the 'interesting' classes
             */
            ClassDetails classDetails = (ClassDetails)classDetailsMap.get(Helper.toSlashedClassName(className));
    
            if (classDetails != null) {
                ((AbstractSession)session).log(SessionLog.FINEST, SessionLog.WEAVER, "begin_weaving_class", className);
                ClassReader classReader = new ClassReader(classfileBuffer);
                ClassWriter classWriter = new ClassWriter(true, true);
                ClassWeaver classWeaver = new ClassWeaver(classWriter, classDetails);
                classReader.accept(classWeaver, Attributes.getDefaultAttributes(), false);
                if (classWeaver.alreadyWeaved) {
                    ((AbstractSession)session).log(SessionLog.FINEST, SessionLog.WEAVER, "end_weaving_class", className);
                    return null;
                }
    
                if (classWeaver.weaved) {
                    byte[] bytes = classWriter.toByteArray();
                    String outputPath = System.getProperty(WEAVING_OUTPUT_PATH, "");
    
                    if (!outputPath.equals("")) {
                        outputFile(className, bytes, outputPath);
                    }
                    if (classWeaver.weavedPersistenceEntity) {
                        ((AbstractSession)session).log(SessionLog.FINEST, SessionLog.WEAVER, "weaved_persistenceentity", className);
                    }
                    if (classWeaver.weavedChangeTracker) {
                        ((AbstractSession)session).log(SessionLog.FINEST, SessionLog.WEAVER, "weaved_changetracker", className);
                    }
                    if (classWeaver.weavedLazy) {
                        ((AbstractSession)session).log(SessionLog.FINEST, SessionLog.WEAVER, "weaved_lazy", className);
                    }
                    if (classWeaver.weavedFetchGroups) {
                        ((AbstractSession)session).log(SessionLog.FINEST, SessionLog.WEAVER, "weaved_fetchgroups", className);
                    }
                    ((AbstractSession)session).log(SessionLog.FINEST, SessionLog.WEAVER, "end_weaving_class", className);
                    return bytes;
                }
                ((AbstractSession)session).log(SessionLog.FINEST, SessionLog.WEAVER, "end_weaving_class", className);
            }
        } catch (Throwable exception) {
            ((AbstractSession)session).log(SessionLog.WARNING, SessionLog.WEAVER, WEAVER_COULD_NOT_WRITE, className, exception);            
        }
        return null; // returning null means 'use existing class bytes'
    }
    
    protected void outputFile(String className, byte[] classBytes, String outputPath){
        StringBuffer directoryName = new StringBuffer();;
        StringTokenizer tokenizer = new StringTokenizer(className, "\n\\/");
        String token = null;
        while (tokenizer.hasMoreTokens()){
            token = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()){
                directoryName.append(token + File.separator);
            }
        }
        try{
            String usedOutputPath = outputPath;
            if (!outputPath.endsWith(File.separator)){
                usedOutputPath = outputPath + File.separator;
            }
            File file = new File(usedOutputPath + directoryName);
            file.mkdirs();
            file = new File(file, token + ".class");
            if (!file.exists()){
                file.createNewFile();
            } else {
                if (!System.getProperty(WEAVING_SHOULD_OVERWRITE, "false").equalsIgnoreCase("true")){
                    ((AbstractSession)session).log(
                            SessionLog.WARNING, SessionLog.WEAVER, WEAVER_NOT_OVERWRITING, className);
                    return;
                }
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(classBytes);
            fos.close();
        } catch (Exception e){
            ((AbstractSession)session).log(
                    SessionLog.WARNING, SessionLog.WEAVER, WEAVER_COULD_NOT_WRITE, className, e);
        }
    }
    
    // same as in org.eclipse.persistence.internal.helper.Helper, but uses
    // '/' slash as delimiter, not '.'
    protected static String getShortName(String name) {
        int pos  = name.lastIndexOf('/');
        if (pos >= 0) {
            name = name.substring(pos+1);
            if (name.endsWith(";")) {
                name = name.substring(0, name.length()-1);
            }
            return name;
        }
        return "";
    }
}
