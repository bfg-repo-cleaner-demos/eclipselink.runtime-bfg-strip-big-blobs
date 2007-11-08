/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.sdo.helper.xmlhelper.loadandsave;

import commonj.sdo.helper.XMLDocument;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.persistence.sdo.helper.SDOClassGenerator;
import org.eclipse.persistence.sdo.helper.SDOXMLHelper;
import org.eclipse.persistence.testing.sdo.util.CompileUtil;

public abstract class LoadAndSaveTestCases extends LoadAndSaveWithOptionsTestCases {
    public LoadAndSaveTestCases(String name) {
        super(name);
    }
    
    abstract protected String getRootInterfaceName();

    public void testLoadFromStringSaveDocumentToWriter() throws Exception {
        List types = defineTypes();

        FileInputStream inputStream = new FileInputStream(getControlFileName());
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        XMLDocument document = xmlHelper.load(new String(bytes));
        verifyAfterLoad(document);

        StringWriter writer = new StringWriter();
        xmlHelper.save(document, writer, null);
        compareXML(getControlWriteFileName(), writer.toString());

    }

    public void testClassGenerationLoadAndSave() throws Exception {
        // TODO: hardcoded path should be parameterized as an option to the test suite 
        String tmpDirName = tempFileDir + "/tmp/";
        File f = new File(tmpDirName);
        f.mkdir();
        f.deleteOnExit();

        generateClasses(tmpDirName);

        setUp();

        compileFiles(tmpDirName, getPackages());

        URL[] urls = new URL[1];
        urls[0] = f.toURL();
        URLClassLoader myURLLoader = new URLClassLoader(urls);
        String package1 = (String)getPackages().get(0);
        String className = package1 + "/" + getRootInterfaceName();
        className = className.replaceAll("/", ".");

        Class urlLoadedClass = myURLLoader.loadClass(className);

        ((SDOXMLHelper)xmlHelper).getLoader().setDelegateLoader(myURLLoader);
        Class loadedClass2 = ((SDOXMLHelper)xmlHelper).getLoader().loadClass(className);

        defineTypes();

        assertEquals(urlLoadedClass, loadedClass2);
        FileInputStream inputStream = new FileInputStream(getControlFileName());
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        XMLDocument document = xmlHelper.load(new String(bytes));
        Class loadedClass = document.getRootObject().getType().getInstanceClass();
        assertEquals(urlLoadedClass, loadedClass);

        verifyAfterLoad(document);

        StringWriter writer = new StringWriter();
        xmlHelper.save(document, writer, null);
        compareXML(getControlWriteFileName(), writer.toString());
    }

    //first package should be the package that contains the class for the get root interface name class
    protected List getPackages() {
        List packages = new ArrayList();
        packages.add("defaultPackage");
        return packages;
    }

    protected void generateClasses(String tmpDirName) throws Exception {
        String xsdString = getSchema(getSchemaName());
        StringReader reader = new StringReader(xsdString);

        SDOClassGenerator classGenerator = new SDOClassGenerator(aHelperContext);
        classGenerator.generate(reader, tmpDirName);
    }

    public void compileFiles(String dirName, List packages) throws Exception {
        // deleteDirsOnExit(new File(dirName));
        List allFilesInAllPackages = new ArrayList();

        for (int i = 0; i < packages.size(); i++) {
            String nextPackage = (String)packages.get(i);
            nextPackage = dirName + nextPackage;

            File f = new File(nextPackage);

            File[] filesInDir = f.listFiles();

            for (int j = 0; j < filesInDir.length; j++) {
                File nextFile = filesInDir[j];
                String fullName = nextFile.getAbsolutePath();
                nextFile.deleteOnExit();
                allFilesInAllPackages.add(fullName);

                String fullClassName = fullName.replace(".java", ".class");
                File nextClassFile = new File(fullClassName);
                nextClassFile.deleteOnExit();
            }
        }
        Object[] fileArray = allFilesInAllPackages.toArray();

        int returnVal = CompileUtil.instance().compile(getClassPathForCompile(), fileArray);
        assertEquals(0, returnVal);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        // TODO: hardcoded path should be parameterized as an option to the test suite 
        String tmpDirName = tempFileDir + "/tmp/";
        emptyAndDeleteDirectory(new File(tmpDirName));
    }
}