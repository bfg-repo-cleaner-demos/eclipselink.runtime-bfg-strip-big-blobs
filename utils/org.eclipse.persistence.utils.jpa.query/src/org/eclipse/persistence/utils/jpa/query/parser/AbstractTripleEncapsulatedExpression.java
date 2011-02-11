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
package org.eclipse.persistence.utils.jpa.query.parser;

import java.util.Collection;
import java.util.List;

/**
 * This {@link Expression} takes care of parsing an expression that encapsulates
 * three expressions separated by a comma.
 *
 * <div nowrap><b>BNF:</b> <code>expression ::= &lt;identifier&gt;(first_expression, second_expression, third_expression)</code><p>
 *
 * @see LocateExpression
 * @see SubstringExpression
 *
 * @version 11.2.0
 * @since 11.2.0
 * @author Pascal Filion
 */
public abstract class AbstractTripleEncapsulatedExpression extends AbstractEncapsulatedExpression
{
	/**
	 * The {@link Expression} that represents the first expression.
	 */
	private AbstractExpression firstExpression;

	/**
	 * Determines whether the comma separating the first and second expression
	 * was parsed.
	 */
	private boolean hasFirstComma;

	/**
	 * Determines whether the comma separating the first and second expression
	 * was parsed.
	 */
	private boolean hasSecondComma;

	/**
	 * Determines whether a whitespace is following the comma.
	 */
	private boolean hasSpaceAfterFirstComma;

	/**
	 * Determines whether a whitespace is following the comma.
	 */
	private boolean hasSpaceAfterSecondComma;

	/**
	 * The {@link Expression} that represents the second expression.
	 */
	private AbstractExpression secondExpression;

	/**
	 * The {@link Expression} that represents the first expression.
	 */
	private AbstractExpression thirdExpression;

	/**
	 * Creates a new <code>AbstractTripleEncapsulatedExpression</code>.
	 *
	 * @param parent The parent of this expression
	 */
	AbstractTripleEncapsulatedExpression(AbstractExpression parent)
	{
		super(parent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void addChildrenTo(Collection<Expression> children)
	{
		children.add(getFirstExpression());
		children.add(getSecondExpression());
		children.add(getThirdExpression());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void addOrderedEncapsulatedExpressionTo(List<StringExpression> children)
	{
		// Fist expression
		if (firstExpression != null)
		{
			children.add(firstExpression);
		}

		// ','
		if (hasFirstComma)
		{
			children.add(buildStringExpression(COMMA));
		}

		if (hasSpaceAfterFirstComma)
		{
			children.add(buildStringExpression(SPACE));
		}

		// Second expression
		if (secondExpression != null)
		{
			children.add(secondExpression);
		}

		// ','
		if (hasSecondComma)
		{
			children.add(buildStringExpression(COMMA));
		}

		if (hasSpaceAfterSecondComma)
		{
			children.add(buildStringExpression(SPACE));
		}

		// Third expression
		if (thirdExpression != null)
		{
			children.add(thirdExpression);
		}
	}

	/**
	 * Returns the {@link Expression} that represents the first expression.
	 *
	 * @return The expression that was parsed representing the first expression
	 */
	public final Expression getFirstExpression()
	{
		if (firstExpression == null)
		{
			firstExpression = buildNullExpression();
		}

		return firstExpression;
	}

	/**
	 * Returns the {@link Expression} that represents the second expression.
	 *
	 * @return The expression that was parsed representing the second expression
	 */
	public final Expression getSecondExpression()
	{
		if (secondExpression == null)
		{
			secondExpression = buildNullExpression();
		}

		return secondExpression;
	}

	/**
	 * Returns the {@link Expression} that represents the first expression.
	 *
	 * @return The expression that was parsed representing the first expression
	 */
	public final Expression getThirdExpression()
	{
		if (thirdExpression == null)
		{
			thirdExpression = buildNullExpression();
		}

		return thirdExpression;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	boolean hasEncapsulatedExpression()
	{
		return hasFirstExpression()  || hasFirstComma  ||
		       hasSecondExpression() || hasSecondComma ||
		       hasThirdExpression();
	}

	/**
	 * Determines whether the comma was parsed after the first expression.
	 *
	 * @return <code>true</code> if a comma was parsed after the first expression;
	 * <code>false</code> otherwise
	 */
	public final boolean hasFirstComma()
	{
		return hasFirstComma;
	}

	/**
	 * Determines whether the first expression of the query was parsed.
	 *
	 * @return <code>true</code> if the first expression was parsed;
	 * <code>false</code> if it was not parsed
	 */
	public final boolean hasFirstExpression()
	{
		return firstExpression != null &&
		      !firstExpression.isNull();
	}

	/**
	 * Determines whether the comma was parsed after the second expression.
	 *
	 * @return <code>true</code> if a comma was parsed after the second expression;
	 * <code>false</code> otherwise
	 */
	public final boolean hasSecondComma()
	{
		return hasSecondComma;
	}

	/**
	 * Determines whether the second expression of the query was parsed.
	 *
	 * @return <code>true</code> if the second expression was parsed;
	 * <code>false</code> if it was not parsed
	 */
	public final boolean hasSecondExpression()
	{
		return secondExpression != null &&
		      !secondExpression.isNull();
	}

	/**
	 * Determines whether a whitespace was parsed after the first comma.
	 *
	 * @return <code>true</code> if there was a whitespace after the first comma;
	 * <code>false</code> otherwise
	 */
	public final boolean hasSpaceAfterFirstComma()
	{
		return hasSpaceAfterFirstComma;
	}

	/**
	 * Determines whether a whitespace was parsed after the second comma.
	 *
	 * @return <code>true</code> if there was a whitespace after the second comma;
	 * <code>false</code> otherwise
	 */
	public final boolean hasSpaceAfterSecondComma()
	{
		return hasSpaceAfterSecondComma;
	}

	/**
	 * Determines whether the third expression of the query was parsed.
	 *
	 * @return <code>true</code> if the third expression was parsed;
	 * <code>false</code> if it was not parsed
	 */
	public final boolean hasThirdExpression()
	{
		return thirdExpression != null &&
		      !thirdExpression.isNull();
	}

	/**
	 * Determines whether the third expression is an optional expression, which means a valid query
	 * can have it or not.
	 *
	 * @return <code>true</code> if the third expression can either be present or not in a valid
	 * query; <code>false</code> if it's mendatory
	 */
	abstract boolean isThirdExpressionOptional();

	/**
	 * Returns the BNF to be used to parse one of the encapsulated expression.
	 *
	 * @param index The position of the encapsulated {@link Expression} that
	 * needs to be parsed within the parenthesis
	 * @return The BNF to be used to parse one of the encapsulated expression
	 */
	abstract JPQLQueryBNF parameterExpressionBNF(int index);

	/**
	 * {@inheritDoc}
	 */
	@Override
	void parseEncapsulatedExpression(WordParser wordParser, boolean tolerant)
	{
		int count = 0;

		// Parse the first expression
		firstExpression = parse
		(
			wordParser,
			parameterExpressionBNF(1),
			tolerant
		);

		if (hasFirstExpression())
		{
			count = wordParser.skipLeadingWhitespace();
		}

		// Parse ','
		hasFirstComma = wordParser.startsWith(COMMA);

		if (hasFirstComma)
		{
			count = 0;
			wordParser.moveForward(1);
			hasSpaceAfterFirstComma = wordParser.skipLeadingWhitespace() > 0;
		}

		// Parse the second expression
		secondExpression = parse
		(
			wordParser,
			parameterExpressionBNF(2),
			tolerant
		);

		if (!hasFirstComma)
		{
			hasSpaceAfterFirstComma = (count > 0);
		}

		count = wordParser.skipLeadingWhitespace();

		// Parse ','
		hasSecondComma = wordParser.startsWith(COMMA);

		if (hasSecondComma)
		{
			count = 0;
			wordParser.moveForward(1);
			hasSpaceAfterSecondComma = wordParser.skipLeadingWhitespace() > 0;
		}

		// Parse the third expression
		thirdExpression = parse
		(
			wordParser,
			parameterExpressionBNF(3),
			tolerant
		);

		if (!hasSecondComma && (!isThirdExpressionOptional() || hasThirdExpression()))
		{
			hasSpaceAfterSecondComma = (count > 0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	final void toParsedTextEncapsulatedExpression(StringBuilder writer)
	{
		// First expression
		if (firstExpression != null)
		{
			firstExpression.toParsedText(writer);
		}

		// ','
		if (hasFirstComma)
		{
			writer.append(COMMA);
		}

		if (hasSpaceAfterFirstComma)
		{
			writer.append(SPACE);
		}

		// Second expression
		if (secondExpression != null)
		{
			secondExpression.toParsedText(writer);
		}

		// ','
		if (hasSecondComma)
		{
			writer.append(COMMA);
		}

		if (hasSpaceAfterSecondComma)
		{
			writer.append(SPACE);
		}

		// Third expression
		if (thirdExpression != null)
		{
			thirdExpression.toParsedText(writer);
		}
	}
}