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
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/
package org.eclipse.persistence.oxm.mappings;

import java.util.Enumeration;
import java.util.Vector;
import org.eclipse.persistence.exceptions.DatabaseException;
import org.eclipse.persistence.exceptions.DescriptorException;
import org.eclipse.persistence.internal.queries.ContainerPolicy;
import org.eclipse.persistence.internal.queries.JoinedAttributeManager;
import org.eclipse.persistence.internal.sessions.AbstractRecord;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.foundation.AbstractCompositeDirectCollectionMapping;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.record.DOMRecord;
import org.eclipse.persistence.oxm.record.XMLRecord;
import org.eclipse.persistence.platform.xml.XMLPlatformFactory;
import org.eclipse.persistence.queries.ObjectBuildingQuery;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 *  @version $Header: XMLFragmentCollectionMapping.java 21-aug-2007.11:06:40 bdoughan Exp $
 *  @author  mmacivor
 *  @since   release specific (what release of product did this appear in)
 */
public class XMLFragmentCollectionMapping extends AbstractCompositeDirectCollectionMapping implements XMLMapping {
    private boolean isWriteOnly;
    private boolean reuseContainer;

    public XMLFragmentCollectionMapping() {
        super();
    }

    /**
     * INTERNAL:
     */
    public boolean isXMLMapping() {
        return true;
    }

    /**
     * INTERNAL:
     * Initialize the mapping.
     */
    public void initialize(AbstractSession session) throws DescriptorException {
        super.initialize(session);
    }

    /**
     * Set the Mapping field name attribute to the given XPath String
     * @param xpathString String
     */
    public void setXPath(String xpathString) {
        setField(new XMLField(xpathString));
    }

    /**
     * Get the XPath String
     * @return String the XPath String associated with this Mapping
     */
    public String getXPath() {
        return getFieldName();
    }

    /**
     * INTERNAL:
     * Build the nested collection from the database row.
     */
    public Object valueFromRow(AbstractRecord row, JoinedAttributeManager joinManager, ObjectBuildingQuery sourceQuery, AbstractSession executionSession) throws DatabaseException {
        ContainerPolicy cp = this.getContainerPolicy();

        Object fieldValue = ((DOMRecord)row).getValuesIndicatingNoEntry(this.getField(), true);

        Vector nestedRows = null;
        if (fieldValue instanceof Vector) {
            nestedRows = (Vector)fieldValue;
        }
        if ((nestedRows == null) || nestedRows.isEmpty()) {
            if (reuseContainer) {
                Object currentObject = ((XMLRecord) row).getCurrentObject();
                Object container = getAttributeAccessor().getAttributeValueFromObject(currentObject);
                return container != null ? container : cp.containerInstance();
            } else {
                return cp.containerInstance();
            }
        }

        Object result = null;
        if (reuseContainer) {
            Object currentObject = ((XMLRecord) row).getCurrentObject();
            Object container = getAttributeAccessor().getAttributeValueFromObject(currentObject);
            result = container != null ? container : cp.containerInstance();
        } else {
            result = cp.containerInstance();
        }

        for (Enumeration stream = nestedRows.elements(); stream.hasMoreElements();) {
            Object next = stream.nextElement();
            if (next instanceof Element) {
                XMLPlatformFactory.getInstance().getXMLPlatform().namespaceQualifyFragment((Element)next);
            }
            cp.addInto(next, result, executionSession);
        }
        return result;
    }

    /**
     * INTERNAL:
     */
    public void writeFromObjectIntoRow(Object object, AbstractRecord row, AbstractSession session) {
        if (this.isReadOnly()) {
            return;
        }

        Object attributeValue = this.getAttributeValueFromObject(object);
        if (attributeValue == null) {
            row.put(this.getField(), null);
            return;
        }

        ContainerPolicy cp = this.getContainerPolicy();

        Vector elements = new Vector(cp.sizeFor(attributeValue));
        for (Object iter = cp.iteratorFor(attributeValue); cp.hasNext(iter);) {
            Object element = cp.next(iter, session);
            if (element != null) {
                elements.addElement(element);
            }
        }

        Object fieldValue = null;
        if (!elements.isEmpty()) {
            fieldValue = this.getDescriptor().buildFieldValueFromDirectValues(elements, elementDataTypeName, session);
        }
        row.put(this.getField(), fieldValue);
    }

    public boolean isAbstractCompositeDirectCollectionMapping() {
        return false;
    }

    public void writeSingleValue(Object attributeValue, Object parent, XMLRecord row, AbstractSession session) {
        if (((XMLField)this.getField()).getLastXPathFragment().isAttribute()) {
            if (attributeValue instanceof Attr) {
                attributeValue = ((Attr)attributeValue).getValue();
            }
        } else if (((XMLField)this.getField()).getLastXPathFragment().nameIsText()) {
            if (attributeValue instanceof Text) {
                attributeValue = ((Text)attributeValue).getNodeValue();
            }
        }
        row.put(getField(), attributeValue);
    }

    public boolean isWriteOnly() {
        return this.isWriteOnly;
    }

    public void setIsWriteOnly(boolean b) {
        this.isWriteOnly = b;
    }

    public void preInitialize(AbstractSession session) throws DescriptorException {
        getAttributeAccessor().setIsWriteOnly(this.isWriteOnly());
        getAttributeAccessor().setIsReadOnly(this.isReadOnly());
        super.preInitialize(session);
    }

    public void setAttributeValueInObject(Object object, Object value) throws DescriptorException {
        if(isWriteOnly()) {
            return;
        }
        super.setAttributeValueInObject(object, value);
    }

    /**
     * Return true if the original container on the object should be used if
     * present.  If it is not present then the container policy will be used to
     * create the container.
     */
    public boolean getReuseContainer() {
        return reuseContainer;
    }

    /**
     * Specify whether the original container on the object should be used if
     * present.  If it is not present then the container policy will be used to
     * create the container.
     */
    public void setReuseContainer(boolean reuseContainer) {
        this.reuseContainer = reuseContainer;
    }

}
