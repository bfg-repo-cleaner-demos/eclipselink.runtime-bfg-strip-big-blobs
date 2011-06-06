/*******************************************************************************
 * Copyright (c) 2006, 2011 Oracle. All rights reserved.
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
package org.eclipse.persistence.jpa.internal.jpql.parser;

import java.util.List;
import org.eclipse.persistence.jpa.internal.jpql.WordParser;

/**
 * Exact numeric literals support the use of Java integer literal syntax as well as SQL exact
 * numeric literal syntax. Approximate literals support the use of Java floating point literal
 * syntax as well as SQL approximate numeric literal
 * syntax.
 * <p>
 * Appropriate suffixes may be used to indicate the specific type of a numeric literal in accordance
 * with the Java Language Specification.
 *
 * @version 2.3
 * @since 2.3
 * @author Pascal Filion
 */
public final class NumericLiteral extends AbstractExpression {

	/**
	 * Creates a new <code>NumericLiteral</code>.
	 *
	 * @param parent The parent of this expression
	 * @param numeric The numeric value
	 */
	NumericLiteral(AbstractExpression parent, String numeric) {
		super(parent, numeric);
	}

	/**
	 * {@inheritDoc}
	 */
	public void accept(ExpressionVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public void acceptChildren(ExpressionVisitor visitor) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void addOrderedChildrenTo(List<StringExpression> children) {
		children.add(buildStringExpression(getText()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPQLQueryBNF getQueryBNF() {
		return queryBNF(NumericLiteralBNF.ID);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getText() {
		return super.getText();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void parse(WordParser wordParser, boolean tolerant) {

		String text = getText();
		wordParser.moveForward(text);

		// Complete the parsing of an approximate number that is of the form
		// <number>e-<integer>
		if (Character.toUpperCase(text.charAt(text.length() - 1)) == 'E') {
			int startIndex = wordParser.position();
			int endIndex = wordParser.position();

			for (int count = wordParser.length(); endIndex < count; endIndex++) {
				char character = wordParser.character(endIndex);

				if ((endIndex == startIndex) && (character == '-')) {
					continue;
				}

				if (!Character.isDigit(character)) {
					break;
				}
			}

			if (endIndex > 0) {
				text += wordParser.substring(wordParser.position(), endIndex);
				setText(text);
				wordParser.setPosition(endIndex);
			}
		}
		else if (text.equals(PLUS) || text.equals(MINUS)) {
			String word = wordParser.word();
			if (word.length() > 0 && Character.isDigit(word.charAt(0))) {
				text += word;
				wordParser.moveForward(word);
				setText(text);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toParsedText() {
		return getText();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	void toParsedText(StringBuilder writer, boolean includeVirtual) {
		writer.append(getText());
	}
}