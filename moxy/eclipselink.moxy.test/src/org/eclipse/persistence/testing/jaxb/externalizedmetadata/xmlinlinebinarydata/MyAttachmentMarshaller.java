/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * dmccann - January 13/2010 - 2.0 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlinlinebinarydata;

import javax.activation.DataHandler;
import javax.xml.bind.attachment.AttachmentMarshaller;

public class MyAttachmentMarshaller extends AttachmentMarshaller {
    static final String BOGUS_ID = "c_id0";

    public boolean isXOPPackage() {
        return true;
    }

    // SATISFY INTERFACE IMPLEMENTATION
    public String addSwaRefAttachment(DataHandler data) { return BOGUS_ID; }
    public String addSwaRefAttachment(byte[] data, int offset, int length) { return BOGUS_ID; }
    public String addMtomAttachment(byte[] bytes, int start, int offset, String mimeType, String elemtnName, String namespaceURI) { return BOGUS_ID; }
    public String addMtomAttachment(DataHandler data, String namespaceURI, String elementName) { return BOGUS_ID; }
}
