/*******************************************************************************
 * Copyright (c) 2011, 2012 Oracle and/or its affiliates. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle - initial API and implementation
 *
 ******************************************************************************/
package org.eclipse.persistence.jpa.tests.jpql;

import org.eclipse.persistence.jpa.jpql.AbstractSemanticValidator;
import org.eclipse.persistence.jpa.jpql.EclipseLinkJPQLQueryContext;
import org.eclipse.persistence.jpa.jpql.EclipseLinkSemanticValidator;
import org.eclipse.persistence.jpa.jpql.EclipseLinkSemanticValidatorExtension;
import org.eclipse.persistence.jpa.jpql.JPQLQueryContext;

/**
 * The unit-test class used for testing a JPQL query semantically when the JPA version is 1.0 and 2.0
 * and EclipseLink is the persistence provider. The EclipseLink version supported is 2.0, 2.1, 2.2
 * and 2.3.
 *
 * @version 2.5
 * @since 2.4
 * @author Pascal Filion
 */
public final class EclipseLinkSemanticValidatorTest extends AbstractSemanticValidatorTest {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JPQLQueryContext buildQueryContext() {
		return new EclipseLinkJPQLQueryContext(jpqlGrammar);
	}

	protected EclipseLinkSemanticValidatorExtension buildSemanticExtension() {
		return EclipseLinkSemanticValidatorExtension.NULL_EXTENSION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractSemanticValidator buildValidator() {
		return new EclipseLinkSemanticValidator(
			buildSemanticValidatorHelper(),
			buildSemanticExtension()
		);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isPathExpressionToCollectionMappingAllowed() {
		return true;
	}
}