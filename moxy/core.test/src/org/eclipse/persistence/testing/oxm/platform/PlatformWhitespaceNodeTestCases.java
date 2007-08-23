/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.testing.oxm.platform;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.persistence.platform.xml.XMLPlatform;
import org.eclipse.persistence.platform.xml.XMLPlatformFactory;
import org.eclipse.persistence.testing.oxm.OXTestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Text;

public class PlatformWhitespaceNodeTestCases extends OXTestCase {
	public PlatformWhitespaceNodeTestCases(String name) {
		super(name);
	}
	
	private Text createTextNode(String value) throws Exception{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();				
		Document doc = factory.newDocumentBuilder().newDocument();
		
		Text textNode = doc.createTextNode(value);
				
		return textNode;
	}
	
	public void testWhitespaceNodeFalse() throws Exception{
		XMLPlatform xmlPlatform = XMLPlatformFactory.getInstance().getXMLPlatform();
		Text textNode = createTextNode("this-is-not-whitespace");				
		boolean isWhiteSpaceNode = xmlPlatform.isWhitespaceNode(textNode);
		
		this.assertFalse(isWhiteSpaceNode);
	}
	
	public void testWhitespaceNodeNull() throws Exception{
		XMLPlatform xmlPlatform = XMLPlatformFactory.getInstance().getXMLPlatform();
		Text textNode = createTextNode(null);				
		boolean isWhiteSpaceNode = xmlPlatform.isWhitespaceNode(textNode);
		
		this.assertFalse(isWhiteSpaceNode);
	}
	
	public void testWhitespaceNodeTrue() throws Exception{
		XMLPlatform xmlPlatform = XMLPlatformFactory.getInstance().getXMLPlatform();
		Text textNode = createTextNode("   ");						
		boolean isWhiteSpaceNode = xmlPlatform.isWhitespaceNode(textNode);
		
		this.assertTrue(isWhiteSpaceNode);		
	}
	
	public void testWhitespaceNodeEmptyTrue() throws Exception{
		XMLPlatform xmlPlatform = XMLPlatformFactory.getInstance().getXMLPlatform();
		Text textNode = createTextNode("");				
		boolean isWhiteSpaceNode = xmlPlatform.isWhitespaceNode(textNode);
		
		this.assertTrue(isWhiteSpaceNode);		
	}
}