/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available athttp://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Oracle
 *
 ******************************************************************************/
package org.eclipse.persistence.utils.jpa.query;

import org.eclipse.persistence.utils.jpa.query.spi.IType;
import org.eclipse.persistence.utils.jpa.query.spi.ITypeDeclaration;

/**
 * This resolver is responsible to calculate the type based on the type of the state field path.
 *
 * @version 11.2.0
 * @since 11.2.0
 * @author Pascal Filion
 */
final class SumFunctionResolver extends AbstractTypeResolver
{
	/**
	 * The resolver used to find the type of the state field path.
	 */
	private final TypeResolver typeResolver;

	/**
	 * Creates a new <code>SumFunctionResolver</code>.
	 *
	 * @param parent The parent of this resolver, which is never <code>null</code>
	 * @param typeResolver The resolver used to find the type of the state field path
	 */
	SumFunctionResolver(TypeResolver parent, TypeResolver typeResolver)
	{
		super(parent);
		this.typeResolver = typeResolver;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IType getType()
	{
		IType type = getTypeDeclaration().getType();

		// Integral types: int/Integer, long/Long => the result is a Long
		if (isIntegralType(type))
		{
			return longType();
		}

		// Floating types: float/Float, double/Double => the result is a Double
		if (isFloatingType(type))
		{
			return doubleType();
		}

		// BigInteger, the result is the same
		IType bigIntegerType = bigInteger();

		if (type.equals(bigIntegerType))
		{
			return bigIntegerType;
		}

		// BigDecimal, the result is the same
		IType bigDecimalType = bigDecimal();

		if (type.equals(bigDecimalType))
		{
			return bigDecimalType;
		}

		// Anything else is an invalid type
		return objectType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ITypeDeclaration getTypeDeclaration()
	{
		return typeResolver.getTypeDeclaration();
	}
}