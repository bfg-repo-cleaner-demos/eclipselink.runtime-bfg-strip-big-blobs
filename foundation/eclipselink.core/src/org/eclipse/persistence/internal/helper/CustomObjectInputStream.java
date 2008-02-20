/*******************************************************************************
 * Copyright (c) 1998, 2008 Oracle. All rights reserved.
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
package org.eclipse.persistence.internal.helper;

import java.lang.Class;
import java.lang.ClassNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectInputStream;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.internal.helper.ConversionManager;

/**
 * INTERNAL:
 * Defines a custom ObjectInputStream that is used with SerializedObjectConverter
 * to ensure the correct class loader is used.
 * BUG# 2813583
 *
 * @auther Guy Pelletier
 * @version 1.0 March 25/03
 */
public class CustomObjectInputStream extends ObjectInputStream {
    Session m_session;

    public CustomObjectInputStream(InputStream stream, Session session) throws IOException {
        super(stream);
        m_session = session;
    }

    public Class resolveClass(ObjectStreamClass classDesc) throws ClassNotFoundException, IOException {
        ConversionManager cm = m_session.getDatasourceLogin().getDatasourcePlatform().getConversionManager();
        return (Class)cm.convertObject(classDesc.getName(), Class.class);
    }
}