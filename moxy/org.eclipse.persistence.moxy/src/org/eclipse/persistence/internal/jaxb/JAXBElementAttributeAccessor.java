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
* mmacivor - June 05/2008 - 1.0 - Initial implementation
******************************************************************************/
package org.eclipse.persistence.internal.jaxb;

import org.eclipse.persistence.mappings.AttributeAccessor;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.oxm.XMLRoot;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

public class JAXBElementAttributeAccessor extends AttributeAccessor {
	private AttributeAccessor nestedAccessor;
	private ContainerPolicy containerPolicy;
	private boolean isContainer;
	
	public JAXBElementAttributeAccessor(AttributeAccessor nestedAccessor) {
		this.nestedAccessor = nestedAccessor;
		this.isContainer = false;
	}
	public JAXBElementAttributeAccessor(AttributeAccessor nestedAccessor, ContainerPolicy containerPolicy) {
		this.nestedAccessor = nestedAccessor;
		this.containerPolicy = containerPolicy;
		this.isContainer = true;
	}
	
	public Object getAttributeValueFromObject(Object object) {
		Object value = nestedAccessor.getAttributeValueFromObject(object);
		//Swap JAXBElements for XMLRoots
		//May need a better way to do this for perf.
		if(isContainer) {
			Object results = containerPolicy.containerInstance(containerPolicy.sizeFor(value));
			Object iterator = containerPolicy.iteratorFor(value);
			while(containerPolicy.hasNext(iterator)) {
				Object next = containerPolicy.next(iterator, null);
				if(next instanceof JAXBElement) {
					JAXBElement element = (JAXBElement)next;
					XMLRoot root = new XMLRoot();
					root.setLocalName(element.getName().getLocalPart());
					root.setNamespaceURI(element.getName().getNamespaceURI());
					root.setObject(element.getValue());
					containerPolicy.addInto(root, results, null);
				} else {
					containerPolicy.addInto(next, results, null);
				}
			}
			value = results;
		} else {
			if(value instanceof JAXBElement) {
				JAXBElement element = (JAXBElement)value;
				XMLRoot root = new XMLRoot();
				root.setLocalName(element.getName().getLocalPart());
				root.setNamespaceURI(element.getName().getNamespaceURI());
				root.setObject(element.getValue());
				value = root;
			}
		}
		return value;
	}
	
	public void setAttributeValueInObject(Object object, Object value) {
		Object attributeValue = value;
		if(isContainer) {
			Object results = containerPolicy.containerInstance(containerPolicy.sizeFor(attributeValue));
			Object iterator = containerPolicy.iteratorFor(attributeValue);
			while(containerPolicy.hasNext(iterator)) {
				Object next = containerPolicy.next(iterator, null);
				if(next instanceof XMLRoot) {
					XMLRoot root = (XMLRoot)next;
					QName name = new QName(root.getNamespaceURI(), root.getLocalName());
					JAXBElement element = new JAXBElement(name, root.getObject().getClass(), root.getObject());
					containerPolicy.addInto(element, results, null);
				} else {
					containerPolicy.addInto(next, results, null);
				}
			}
			attributeValue = results;
		} else {
			if(attributeValue instanceof XMLRoot) {
				XMLRoot root = (XMLRoot)attributeValue;
				QName name = new QName(root.getNamespaceURI(), root.getLocalName());
				JAXBElement element = new JAXBElement(name, root.getObject().getClass(), root.getObject());
				attributeValue = element;
			}			
		}
		nestedAccessor.setAttributeValueInObject(object, attributeValue);
	}
	
    public void initializeAttributes(Class theJavaClass) throws DescriptorException {
    	nestedAccessor.initializeAttributes(theJavaClass);
    }
	
}
