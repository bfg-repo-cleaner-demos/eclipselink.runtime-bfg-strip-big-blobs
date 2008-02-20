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
 package org.eclipse.persistence.testing.framework.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import javax.ejb.EJBException;

import junit.framework.TestCase;

import org.eclipse.persistence.testing.framework.junit.JUnitTestCase;

/**
 * Server side JUnit test invocation implemented as a stateless session bean.
 * 
 * @author mschinca
 */
public class TestRunnerBean implements TestRunner {

    /**
     * Execute a test case method. The test class is loaded dynamically and
     * must therefore be visible to the TestRunnerBean classloader.
     */
    public Throwable runTest(String className, String test, Properties props) {
        // load the test class and create an instance
        TestCase testInstance = null;
        try {
            Class testClass = getClass().getClassLoader().loadClass(className);
            Constructor c = testClass.getConstructor(new Class[] { String.class });
            testInstance = (TestCase) c.newInstance(new Object[] { test });
        } catch (ClassNotFoundException e) {
            throw new EJBException(e);
        } catch (NoSuchMethodException e) {
            throw new EJBException(e);
        } catch (InstantiationException e) {
            throw new EJBException(e);
        } catch (IllegalAccessException e) {
            throw new EJBException(e);
        } catch (InvocationTargetException e) {
            throw new EJBException(e);
        }
        
        // if any properties were passed in, set them into 
        // the server's VM
        if (props != null) {
            System.getProperties().putAll(props);
        }
        
        // execute the bare test case
        Throwable result = null;
        try {
            if (testInstance instanceof JUnitTestCase) {
                ((JUnitTestCase)testInstance).runBareServer();
            } else {
                testInstance.runBare();
            }
        } catch (Throwable t) {
            result = t;
        }
        return result;
    }

}
