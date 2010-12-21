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
 * dmccann - March 19/2010 - 2.1 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.composite;

import java.io.File;
import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.eclipse.persistence.jaxb.JAXBContext;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.mappings.XMLCompositeObjectMapping;
import org.eclipse.persistence.oxm.mappings.XMLDirectMapping;
import org.eclipse.persistence.oxm.mappings.nullpolicy.NullPolicy;
import org.eclipse.persistence.oxm.mappings.nullpolicy.XMLNullRepresentationType;
import org.eclipse.persistence.sessions.Project;
import org.eclipse.persistence.sessions.factories.XMLProjectWriter;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.ExternalizedMetadataTestCases;
import org.w3c.dom.Document;

/**
 * Tests XmlCompositeObjectMappings via eclipselink-oxm.xml
 * 
 */
public class CompositeMappingTestCases extends ExternalizedMetadataTestCases {
    private static final String CONTEXT_PATH = "org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.composite";
    private static final String PATH = "org/eclipse/persistence/testing/jaxb/externalizedmetadata/mappings/composite/";
    private static final String CYCLIC_CONTEXT_PATH = CONTEXT_PATH + ".cyclic";
    private static final String CYCLIC_PATH = PATH + "cyclic/";
    private static final String MULTI_NS_CONTEXT_PATH = CONTEXT_PATH + ".multiplenamespaces";
    private static final String MULTI_NS_PATH = PATH + "multiplenamespaces/";
    private static final String MULTI_NS_CYCLIC_CONTEXT_PATH = CYCLIC_CONTEXT_PATH + ".multiplenamespaces";
    private static final String MULTI_NS_CYCLIC_PATH = CYCLIC_PATH + "multiplenamespaces/";
    
    private static final String HOME_CITY = "Kanata";
    private static final String HOME_STREET = "66 Lakview Drive";
    private static final String HOME_PROVINCE = "ON";
    private static final String HOME_POSTAL = "K2M2K7";
    private static final String WORK_CITY = "Ottawa";
    private static final String WORK_STREET = "45 O'Connor St.";
    private static final String WORK_PROVINCE = "ON";
    private static final String WORK_POSTAL = "K1P1A4";
    private static final String ALT_CITY = "Austin";
    private static final String PRIVATE_CITY = "Dallas";
    private static final String PRIVATE_STREET = "101 Texas Blvd.";
    private static final String PRIVATE_PROVINCE = "TX";
    private static final String PRIVATE_POSTAL = "78726";
    private static final String PHONE_1 = "613.288.0001";
    private static final String PHONE_2 = "613.288.0002";
    private static final String PRIVATE_NUMBER = "000.000.0000";
    private static final String FOO_NAME = "myfoo";
    private static final String DEPT_ID = "101";
    private static final String DEPT_NAME = "Sanitation";
    private static final String EMPLOYEES_NS = "http://www.example.com/employees"; 
    private static final String CONTACTS_NS = "http://www.example.com/contacts"; 
    private static final String ADDRESS_NS = "http://www.example.com/address"; 
    
    private MySchemaOutputResolver employeeResolver;

    /**
     * This is the preferred (and only) constructor.
     * 
     * @param name
     */
    public CompositeMappingTestCases(String name) {
        super(name);
    }
    
    /**
     * This method's primary purpose id to generate schema(s). Validation of
     * generated schemas will occur in the testXXXGen method(s) below. Note that
     * the JAXBContext is created from this call and is required for
     * marshal/unmarshal, etc. tests.
     * 
     */
    public void setUp() throws Exception {
        super.setUp();
        employeeResolver = generateSchemaWithFileName(new Class[] { Employee.class }, CONTEXT_PATH, PATH + "employee-oxm.xml", 2);
    }

    /**
     * Return the control Employee.
     * 
     * @return
     */
    public Employee getControlObject() {
        Address hAddress = new Address();
        hAddress.city = HOME_CITY;
        hAddress.street = HOME_STREET;
        hAddress.province = HOME_PROVINCE;
        hAddress.postalCode = HOME_POSTAL;
        
        Address wAddress = new Address();
        wAddress.city = WORK_CITY;
        wAddress.street = WORK_STREET;
        wAddress.province = WORK_PROVINCE;
        wAddress.postalCode = WORK_POSTAL;
        
        Address aAddress = new Address();
        aAddress.city = ALT_CITY;

        Address pAddress = new Address();
        pAddress.city = WORK_CITY;
        pAddress.street = WORK_STREET;
        pAddress.province = WORK_PROVINCE;
        pAddress.postalCode = WORK_POSTAL;

        Phone pOne = new Phone();
        pOne.number = PHONE_1;
        Phone pTwo = new Phone();
        pTwo.number = PHONE_2;
        
        Department dept = new Department();
        dept.deptId = DEPT_ID;
        dept.deptName = DEPT_NAME;
        
        Foo foo = new Foo();
        foo.foodata = FOO_NAME;
        
        Phone pPhone = new Phone();
        pPhone.number = PRIVATE_NUMBER;

        Employee emp = new Employee();
        emp.homeAddress = hAddress;
        emp.workAddress = wAddress;
        emp.alternateAddress = aAddress;
        emp.phone1 = pOne;
        emp.phone2 = pTwo;
        emp.foo = foo;
        emp.privatePhone = pPhone;
        emp.department = dept;

        return emp;
    }
    
    /**
     * Tests schema generation and instance document validation.
     * 
     */
    public void testSchemaGenAndValidation() {
        // validate employee schema
        compareSchemas(employeeResolver.schemaFiles.get(EMPLOYEES_NS), new File(PATH + "employee.xsd"));
        // validate contacts schema
        compareSchemas(employeeResolver.schemaFiles.get(CONTACTS_NS), new File(PATH + "contacts.xsd"));
        
        // validate employee.xml
        String src = PATH + "employee.xml";
        String result = validateAgainstSchema(src, EMPLOYEES_NS, employeeResolver);
        assertTrue("Instance doc validation (employee.xml) failed unxepectedly: " + result, result == null);

        // validate write-employee.xml
        src = PATH + "write-employee.xml";
        result = validateAgainstSchema(src, EMPLOYEES_NS, employeeResolver);
        assertTrue("Instance doc validation (write-employee.xml) failed unxepectedly: " + result, result == null);
        
        // validate schema generation with three namespaces (no cyclic imports)
        MySchemaOutputResolver resolver = generateSchemaWithFileName(new Class[] { 
                  org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.composite.multiplenamespaces.Employee.class 
                }, MULTI_NS_CONTEXT_PATH, MULTI_NS_PATH + "employee-oxm.xml", 3);
        
        // validate three namespace employee schema
        compareSchemas(resolver.schemaFiles.get(EMPTY_NAMESPACE), new File(MULTI_NS_PATH + "employee.xsd"));
        // validate three namespace contacts schema
        compareSchemas(resolver.schemaFiles.get(CONTACTS_NS), new File(MULTI_NS_PATH + "contacts.xsd"));
        // validate three namespace address schema
        compareSchemas(resolver.schemaFiles.get(ADDRESS_NS), new File(MULTI_NS_PATH + "address.xsd"));

        // validate schema generation with cyclic imports (due to xml-path w/multiple namespaces)
        resolver = generateSchemaWithFileName(new Class[] { 
                  org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.composite.cyclic.Employee.class 
                }, CYCLIC_CONTEXT_PATH, CYCLIC_PATH + "cyclic-oxm.xml", 2);
        
        // validate cyclic employee schema
        compareSchemas(resolver.schemaFiles.get(EMPLOYEES_NS), new File(CYCLIC_PATH + "employee.xsd"));
        // validate cyclic contacts schema
        compareSchemas(resolver.schemaFiles.get(CONTACTS_NS), new File(CYCLIC_PATH + "contacts.xsd"));

        // validate schema generation with three namespaces (cyclic imports)
        resolver = generateSchemaWithFileName(new Class[] { 
                  org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.composite.cyclic.multiplenamespaces.Employee.class 
                }, MULTI_NS_CYCLIC_CONTEXT_PATH, MULTI_NS_CYCLIC_PATH + "employee-oxm.xml", 3);
        
        // validate three namespace cyclic employee schema
        compareSchemas(resolver.schemaFiles.get(EMPLOYEES_NS), new File(MULTI_NS_CYCLIC_PATH + "employee.xsd"));
        // validate three namespace cyclic contacts schema
        compareSchemas(resolver.schemaFiles.get(CONTACTS_NS), new File(MULTI_NS_CYCLIC_PATH + "contacts.xsd"));
        // validate three namespace cyclic address schema
        compareSchemas(resolver.schemaFiles.get(ADDRESS_NS), new File(MULTI_NS_CYCLIC_PATH + "/address.xsd"));
    }
    
    /**
     * Tests XmlCompositeObjectMapping configuration via eclipselink-oxm.xml. 
     * Here an unmarshal operation is performed. Utilizes xml-attribute and 
     * xml-element.
     * 
     * Positive test.
     */
    public void testCompositeMappingUnmarshal() {
        // load instance doc
        InputStream iDocStream = loader.getResourceAsStream(PATH + "employee.xml");
        if (iDocStream == null) {
            fail("Couldn't load instance doc [" + PATH + "employee.xml" + "]");
        }

        // setup control Employee
        Employee ctrlEmp = getControlObject();
        // 'privatePhone' is write only, so no value should be unmarshalled for it
        ctrlEmp.privatePhone = null;

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Employee empObj = (Employee) unmarshaller.unmarshal(iDocStream);
            assertNotNull("Unmarshalled object is null.", empObj);
            assertTrue("Accessor method was not called as expected", empObj.wasSetCalled);
            assertTrue("Set was not called for absent node as expected", empObj.isADeptSet);
            assertTrue("Unmarshal failed:  Employee objects are not equal", ctrlEmp.equals(empObj));
        } catch (JAXBException e) {
            e.printStackTrace();
            fail("Unmarshal operation failed.");
        }
    }

    /**
     * Tests XmlCompositeObjectMapping configuration via eclipselink-oxm.xml. Here a
     * marshal operation is performed. Utilizes xml-attribute and xml-element
     * 
     * Positive test.
     */
    public void testCompositeMappingMarshal() {
        // load instance doc
        String src = PATH + "write-employee.xml";

        // setup control document
        Document testDoc = parser.newDocument();
        Document ctrlDoc = parser.newDocument();
        try {
            ctrlDoc = getControlDocument(src);
        } catch (Exception e) {
            e.printStackTrace();
            fail("An unexpected exception occurred loading control document [" + src + "].");
        }

        // test marshal
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            Employee ctrlEmp = getControlObject();
            marshaller.marshal(ctrlEmp, testDoc);
            //marshaller.marshal(ctrlEmp, System.out);
            assertTrue("Accessor method was not called as expected", ctrlEmp.wasGetCalled);
            assertTrue("Document comparison failed unxepectedly: ", compareDocuments(ctrlDoc, testDoc));
        } catch (JAXBException e) {
            e.printStackTrace();
            fail("Marshal operation failed.");
        }
    }
}