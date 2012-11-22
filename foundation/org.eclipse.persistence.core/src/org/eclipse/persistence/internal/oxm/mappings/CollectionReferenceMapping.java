/*******************************************************************************
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Blaise Doughan - 2.5 - initial implementation
 ******************************************************************************/
package org.eclipse.persistence.internal.oxm.mappings;

import java.util.List;
import java.util.Map;

import org.eclipse.persistence.core.descriptors.CoreDescriptor;
import org.eclipse.persistence.core.mappings.CoreAttributeAccessor;
import org.eclipse.persistence.internal.core.helper.CoreField;
import org.eclipse.persistence.internal.core.queries.CoreContainerPolicy;
import org.eclipse.persistence.internal.core.sessions.CoreAbstractSession;
import org.eclipse.persistence.oxm.XMLField;
import org.eclipse.persistence.oxm.record.UnmarshalRecord;

public interface CollectionReferenceMapping<
    ABSTRACT_SESSION extends CoreAbstractSession,
    ATTRIBUTE_ACCESSOR extends CoreAttributeAccessor,
    CONTAINER_POLICY extends CoreContainerPolicy,
    DESCRIPTOR extends CoreDescriptor,
    FIELD extends CoreField> extends Mapping<ATTRIBUTE_ACCESSOR, CONTAINER_POLICY, DESCRIPTOR, FIELD>, XMLContainerMapping {

    public Object buildFieldValue(Object targetObject, XMLField xmlField, ABSTRACT_SESSION session);

    public void buildReference(UnmarshalRecord unmarshalRecord, XMLField xmlField,
            Object value, ABSTRACT_SESSION session, Object container);

    public List<FIELD> getFields();

    public InverseReferenceMapping getInverseReferenceMapping();

    /**
     * Return a list of source-target xmlfield pairs.
     */
    public Map getSourceToTargetKeyFieldAssociations();

    public boolean isWriteOnly();

    public boolean usesSingleNode();

}