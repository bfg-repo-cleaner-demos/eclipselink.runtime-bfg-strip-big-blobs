/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.eis.cobol;


/**
* This is for exceptions encountered while extracting or inserting data in byte arrays.
*/
public class ByteArrayException extends org.eclipse.persistence.exceptions.EclipseLinkException {
    public static final int UNRECOGNIZED_DATA_FORMAT = 16000;

    public ByteArrayException() {
        super("");
    }

    public ByteArrayException(String message) {
        super(message);
    }

    public ByteArrayException(String message, Exception exception) {
        super(message, exception);
    }

    /**
    * thrown when the byte converter cannot recognize the data format in the byte array
    */
    public static ByteArrayException unrecognizedDataType() {
        ByteArrayException exception = new ByteArrayException("An invalid data format has been encountered.");
        exception.setErrorCode(UNRECOGNIZED_DATA_FORMAT);
        return exception;
    }
}