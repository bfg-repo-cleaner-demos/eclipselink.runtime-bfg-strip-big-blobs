/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.mappings.transformers;

import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.mappings.foundation.AbstractTransformationMapping;

/**
 * <p><b>Purpose</b>: Provides an empty implementation of FieldTransformer.
 * Users who do not require the full FieldTransformer API can subclass this class
 * and implement only the methods required.
 *  @see org.eclipse.persistence.mappings.FieldTransformer
 *  @version $Header: FieldTransformerAdapter.java 11-jul-2006.10:33:44 gyorke Exp $
 *  @author  mmacivor
 *  @since   10
 */
public class FieldTransformerAdapter implements FieldTransformer {
    public void initialize(AbstractTransformationMapping mapping) {
    }

    public Object buildFieldValue(Object object, String fieldName, Session session) {
        return null;
    }
}