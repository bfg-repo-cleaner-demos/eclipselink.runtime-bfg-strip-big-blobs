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
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.internal.eis.adapters.xmlfile;

import javax.resource.cci.*;
import org.eclipse.persistence.Version;

/**
 * Defines the meta-data for the XML file adapter
 *
 * @author James
 * @since OracleAS TopLink 10<i>g</i> (10.0.3)
 */
public class XMLFileAdapterMetaData implements ResourceAdapterMetaData {

    /**
     * Default constructor.
     */
    public XMLFileAdapterMetaData() {
    }

    public String getAdapterName() {
        return "XML File Adapter";
    }

    public String getAdapterShortDescription() {
        return "Emulated XML file JCA adapter.";
    }

    public String getAdapterVendorName() {
        return "Oracle";
    }

    public String getAdapterVersion() {
        return Version.getVersion();
    }

    public String[] getInteractionSpecsSupported() {
        String[] specs = new String[1];
        specs[0] = "org.eclipse.persistence.eis.xml.file.XMLFileInteractionSpec";
        return specs;
    }

    public String getSpecVersion() {
        return "1.5";
    }

    public boolean supportsExecuteWithInputAndOutputRecord() {
        return true;
    }

    public boolean supportsExecuteWithInputRecordOnly() {
        return true;
    }

    public boolean supportsLocalTransactionDemarcation() {
        return true;
    }
}
