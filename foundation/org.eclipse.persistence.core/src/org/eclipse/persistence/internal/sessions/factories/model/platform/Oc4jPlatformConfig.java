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
package org.eclipse.persistence.internal.sessions.factories.model.platform;

/**
 * INTERNAL:
 * The generic platform configuration for the OC4J server 
 */
public class Oc4jPlatformConfig extends ServerPlatformConfig {
    public Oc4jPlatformConfig() {
        super("org.eclipse.persistence.platform.server.oc4j.Oc4jPlatform");
    }
}
