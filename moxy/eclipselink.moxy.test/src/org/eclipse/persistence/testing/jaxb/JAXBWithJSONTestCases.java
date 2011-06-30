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
 *     Blaise Doughan - 2.4 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import javax.xml.bind.Marshaller;

public abstract class JAXBWithJSONTestCases extends JAXBTestCases {

    private String controlJSONLocation;

    public JAXBWithJSONTestCases(String name) throws Exception {
        super(name);
    }

    public void setControlJSON(String location) {
        this.controlJSONLocation = location;
    }

    public void testJSONMarshalToOutputStream() throws Exception{
        Marshaller jsonMarshaller = getJAXBContext().createMarshaller();
        jsonMarshaller.setProperty("eclipselink.media.type", "application/json");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        jsonMarshaller.marshal(getWriteControlObject(), os);
        compareStrings("testJSONMarshalToOutputStream", new String(os.toByteArray()));
        os.close();
    }

    public void testJSONMarshalToOutputStream_FORMATTED() throws Exception{
        Marshaller jsonMarshaller = getJAXBContext().createMarshaller();
        jsonMarshaller.setProperty("eclipselink.media.type", "application/json");
        jsonMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        jsonMarshaller.marshal(getWriteControlObject(), os);
        compareStrings("testJSONMarshalToOutputStream", new String(os.toByteArray()));
        os.close();
    }

    public void testJSONMarshalToStringWriter() throws Exception{
        Marshaller jsonMarshaller = getJAXBContext().createMarshaller();
        jsonMarshaller.setProperty("eclipselink.media.type", "application/json");

        StringWriter sw = new StringWriter();
        jsonMarshaller.marshal(getWriteControlObject(), sw);
        compareStrings("**testJSONMarshalToStringWriter**", sw.toString());
    }

    public void testJSONMarshalToStringWriter_FORMATTED() throws Exception{
        Marshaller jsonMarshaller = getJAXBContext().createMarshaller();
        jsonMarshaller.setProperty("eclipselink.media.type", "application/json");
        jsonMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        StringWriter sw = new StringWriter();
        jsonMarshaller.marshal(getWriteControlObject(), sw);
        compareStrings("**testJSONMarshalToStringWriter**", sw.toString());
    }

    private void compareStrings(String test, String testString) {
        log(test);
        log("Expected (With All Whitespace Removed):");
        String expectedString = getJSONControlString(controlJSONLocation).replaceAll("[ \b\t\n\r' ']", "");
        log(expectedString);
        log("\nActual (With All Whitespace Removed):");
        testString = testString.replaceAll("[ \b\t\n\r]", "");
        log(testString);
        assertEquals(expectedString, testString);
    }

    protected String getJSONControlString(String fileName){
        StringBuffer sb = new StringBuffer();
        try {
            InputStream inputStream = ClassLoader.getSystemResourceAsStream(controlJSONLocation);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
           String str;
            while (bufferedReader.ready()) {
                sb.append(bufferedReader.readLine());
            }
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        return sb.toString();
    }

}