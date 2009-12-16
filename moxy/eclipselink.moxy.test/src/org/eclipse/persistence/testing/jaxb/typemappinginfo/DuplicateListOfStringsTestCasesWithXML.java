/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * dmccann - December 15/2009 - 2.0.1 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.typemappinginfo;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.persistence.jaxb.TypeMappingInfo;
import org.eclipse.persistence.jaxb.TypeMappingInfo.ElementScope;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class DuplicateListOfStringsTestCasesWithXML extends DuplicateListOfStringsTestCases {
    public DuplicateListOfStringsTestCasesWithXML(String name) throws Exception {
        super(name);
        //useLogging = true;
    }

    // override this method such that XML metadata can be used instead of annotations
    protected TypeMappingInfo[] getTypeMappingInfos()throws Exception {
        if (typeMappingInfos == null){
            typeMappingInfos = new TypeMappingInfo[4];
        
            TypeMappingInfo tpi = new TypeMappingInfo();
            tpi.setXmlTagName(new QName("someUri","testTagname"));      
            tpi.setElementScope(ElementScope.Global);
            
            tpi.setXmlElement(getXmlElement("<xml-element xml-list=\"true\" />"));
            
            tpi.setType(List.class);        
            typeMappingInfos[0] = tpi;
        
            TypeMappingInfo tpi2 = new TypeMappingInfo();
            tpi2.setXmlTagName(new QName("someUri","testTagname2"));        
            tpi2.setElementScope(ElementScope.Global);      
            tpi2.setType(List.class);       
            typeMappingInfos[1] = tpi2;
        
            TypeMappingInfo tpi3 = new TypeMappingInfo();
            tpi3.setXmlTagName(new QName("someUri","testTagname3"));        
            tpi3.setElementScope(ElementScope.Global);                  
            tpi3.setType(getClass().getField("myListOfStrings").getGenericType());      
            typeMappingInfos[2] = tpi3;
            
            TypeMappingInfo tpi4 = new TypeMappingInfo();
            tpi4.setXmlTagName(new QName("someUri","testTagname4"));        
            tpi4.setElementScope(ElementScope.Global);      
            tpi4.setType(List.class);       
            typeMappingInfos[3] = tpi4;
        }
        
        return typeMappingInfos;
    }
}
