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

import org.eclipse.persistence.core.descriptors.CoreDescriptor;
import org.eclipse.persistence.core.mappings.CoreAttributeAccessor;
import org.eclipse.persistence.core.sessions.CoreSession;
import org.eclipse.persistence.internal.core.helper.CoreField;
import org.eclipse.persistence.internal.core.queries.CoreContainerPolicy;
import org.eclipse.persistence.oxm.mappings.UnmarshalKeepAsElementPolicy;
import org.eclipse.persistence.oxm.mappings.nullpolicy.AbstractNullPolicy;

public interface CompositeObjectMapping<
    ATTRIBUTE_ACCESSOR extends CoreAttributeAccessor,
    CONTAINER_POLICY extends CoreContainerPolicy,
    DESCRIPTOR extends CoreDescriptor,
    FIELD extends CoreField,
    SESSION extends CoreSession> extends Mapping<ATTRIBUTE_ACCESSOR, CONTAINER_POLICY, DESCRIPTOR, FIELD>, XMLConverterMapping<SESSION> {
    
    public InverseReferenceMapping getInverseReferenceMapping();

    public UnmarshalKeepAsElementPolicy getKeepAsElementPolicy();

    public AbstractNullPolicy getNullPolicy();

    public Object getReferenceClassName();

    public boolean hasConverter();

    public void setXPath(String string);

}