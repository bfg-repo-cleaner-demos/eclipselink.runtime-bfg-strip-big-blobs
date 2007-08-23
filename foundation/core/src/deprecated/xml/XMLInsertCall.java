/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package deprecated.xml;

import java.util.*;
import java.io.*;
import org.eclipse.persistence.internal.databaseaccess.*;
import org.eclipse.persistence.internal.localization.TraceLocalization;
import org.eclipse.persistence.internal.sessions.AbstractRecord;

/**
 * XMLInsertCall simply writes a new XML document.
 *
 * @author Big Country
 * @since TOPLink/Java 3.0
 * @deprecated since OracleAS TopLink 10<i>g</i> (10.1.3).  This class is replaced by
 *         {@link org.eclipse.persistence.oxm}
 */
public class XMLInsertCall extends XMLWriteCall {

    /**
     * Default constructor.
     */
    public XMLInsertCall() {
        super();
    }

    /**
     * Returns a new write stream.
     */
    protected Writer getWriteStream(Accessor accessor, String rootElementName, AbstractRecord translationRow, Vector orderedPrimaryKeyElements) throws XMLDataStoreException {
        return this.getStreamPolicy().getNewWriteStream(rootElementName, translationRow, orderedPrimaryKeyElements, accessor);
    }

    /**
     * Append a string describing the call to the specified writer.
     */
    protected void writeLogDescription(PrintWriter writer) {
        writer.write(TraceLocalization.buildMessage("XML_insert", (Object[])null));
    }
}