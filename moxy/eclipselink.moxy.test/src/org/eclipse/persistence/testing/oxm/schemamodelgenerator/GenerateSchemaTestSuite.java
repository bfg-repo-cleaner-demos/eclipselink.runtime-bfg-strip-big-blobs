package org.eclipse.persistence.testing.oxm.schemamodelgenerator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class GenerateSchemaTestSuite extends TestCase {
    public GenerateSchemaTestSuite(String name) {
        super(name);
    }
    
    public static void main(String[] args) {
        String[] arguments = { "-c", "org.eclipse.persistence.testing.oxm.schemamodelgenerator.GenerateSchemaTestSuite" };
        junit.textui.TestRunner.main(arguments);
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite("Schema Model Generator Test Suite");
        suite.addTestSuite(GenerateSingleSchemaTestCases.class);
        return suite;
    }

}
