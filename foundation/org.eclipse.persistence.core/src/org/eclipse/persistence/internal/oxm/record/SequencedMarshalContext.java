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
package org.eclipse.persistence.internal.oxm.record;

import java.util.List;
import org.eclipse.persistence.internal.oxm.ContainerValue;
import org.eclipse.persistence.internal.oxm.NodeValue;
import org.eclipse.persistence.internal.oxm.XPathFragment;
import org.eclipse.persistence.internal.oxm.XPathNode;
import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.record.MarshalRecord;
import org.eclipse.persistence.oxm.sequenced.Setting;

/**
 * An implementation of MarshalContext for handling sequenced objects that
 * are mapped to XML. 
 */
public class SequencedMarshalContext implements MarshalContext {

    private List<Setting> settings;
    private XPathFragment indexFragment;
    private Object value;

    public SequencedMarshalContext(List<Setting> settings) {
        super();
        indexFragment = new XPathFragment();
        this.settings = settings;
    }

    public SequencedMarshalContext(Object value) {
        super();
        indexFragment = new XPathFragment();
        this.value = value;
    }


    public MarshalContext getMarshalContext(int index) {
        Setting setting = settings.get(index);
        List<Setting> children = setting.getChildren();
        if(null == children) {
            return new SequencedMarshalContext(setting.getValue());
        } else {
            return new SequencedMarshalContext(children);
        }
    }

    public int getNonAttributeChildrenSize(XPathNode xPathNode) {
        if(null == settings) {
            return 0;
        }
        return settings.size();
    }

    public Object getNonAttributeChild(int index, XPathNode xPathNode) {
        Setting setting = settings.get(index);
        if(null == setting.getName()) {
            return xPathNode.getNonAttributeChildrenMap().get(null);
        } else {
            if (setting.getName().equals("text()")) {
                return xPathNode.getTextNode();
            } else {
                indexFragment.setLocalName(null);
                indexFragment.setXPath(setting.getName());
                indexFragment.setNamespaceURI(setting.getNamespaceURI());
                return xPathNode.getNonAttributeChildrenMap().get(indexFragment);
                
            }
        }
    }

    public Object getAttributeValue(Object object, DatabaseMapping mapping) {
        return value;
    }

    public boolean marshal(NodeValue nodeValue, XPathFragment xPathFragment, MarshalRecord marshalRecord, Object object, AbstractSession session, NamespaceResolver namespaceResolver) {
        if(nodeValue.isContainerValue()) {
            ((ContainerValue)nodeValue).marshalSingleValue(xPathFragment, marshalRecord, object, value, session, namespaceResolver, this);
            return true;
        } else {
            return nodeValue.marshal(xPathFragment, marshalRecord, object, session, namespaceResolver, this);
        }
    }

}
