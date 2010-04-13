package org.eclipse.persistence.testing.jaxb.simpledocument;

import javax.xml.bind.JAXBElement;

import org.eclipse.persistence.testing.jaxb.JAXBTestCases;

public class StringNilTestCases extends JAXBTestCases {
    private final static String XML_RESOURCE = "org/eclipse/persistence/testing/jaxb/simpledocument/string_nil.xml";

    private final static String XML_WRITE_RESOURCE = "org/eclipse/persistence/testing/jaxb/simpledocument/string_empty.xml";
    public StringNilTestCases(String name) throws Exception {
        super(name);
        setControlDocument(XML_RESOURCE);        
        //setWriteControlDocument(XML_WRITE_RESOURCE);
        Class[] classes = new Class[1];
        classes[0] = StringObjectFactory.class;
        setClasses(classes);
    }

    protected Object getControlObject() {
        JAXBElement value = new StringObjectFactory().createStringRoot();
        value.setValue(null);
        value.setNil(true);
        return value;      
    }
    
    
}
