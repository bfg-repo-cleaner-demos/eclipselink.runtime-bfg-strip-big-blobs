/*******************************************************************************
 * Copyright (c) 2011 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Blaise Doughan - 2.4 - initial implementation
 ******************************************************************************/
 
grammar JSON;

OBJECT : '{' (PAIR (',' PAIR)*)? '}';
 
PAIR : WHITE_SPACE STRING WHITE_SPACE ':' WHITE_SPACE VALUE WHITE_SPACE;

ARRAY : '[' (VALUE (',' VALUE)*)? ']';

VALUE   :       STRING
        |       NUMBER
        |       OBJECT
        |       ARRAY
        |       'true'
        |       'false'
        |       'null'
        ;

STRING  : '"' CHAR* '"';

fragment CHAR   : ~('"'| '\\')
                | '\\"'
                | '\\\\'
                | '\\/'
                | '\\b'
                | '\\f'
                | '\\n'
                | '\\r'
                | '\\t'     
                | '\\u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;

fragment HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F');

NUMBER  : INT FRAC? EXP?;

fragment INT : '-'? DIGITS;

fragment FRAC : '.' DIGITS;

fragment EXP : E DIGITS;

fragment DIGITS : '0'..'9'*;

fragment E :('e'|'E') ('+'|'-')?;

fragment WHITE_SPACE    : (' ' | '\r' | '\t' | '\u000C' | '\n')*;