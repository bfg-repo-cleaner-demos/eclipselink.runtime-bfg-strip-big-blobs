/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved.
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
package org.eclipse.persistence.internal.sessions.factories.model.transport;

import org.eclipse.persistence.internal.sessions.factories.model.transport.naming.*;

/**
 * INTERNAL:
 */
public class JMSTopicTransportManagerConfig extends TransportManagerConfig {
    private String m_topicHostURL;
    private String m_topicConnectionFactoryName;
    private String m_topicName;
    private JNDINamingServiceConfig m_jndiNamingServiceConfig;

    public JMSTopicTransportManagerConfig() {
        super();
    }

    public void setTopicHostURL(String topicHostURL) {
        m_topicHostURL = topicHostURL;
    }

    public String getTopicHostURL() {
        return m_topicHostURL;
    }

    public void setTopicConnectionFactoryName(String topicConnectionFactoryName) {
        m_topicConnectionFactoryName = topicConnectionFactoryName;
    }

    public String getTopicConnectionFactoryName() {
        return m_topicConnectionFactoryName;
    }

    public void setTopicName(String topicName) {
        m_topicName = topicName;
    }

    public String getTopicName() {
        return m_topicName;
    }

    public void setJNDINamingServiceConfig(JNDINamingServiceConfig jndiNamingServiceConfig) {
        m_jndiNamingServiceConfig = jndiNamingServiceConfig;
    }

    public JNDINamingServiceConfig getJNDINamingServiceConfig() {
        return m_jndiNamingServiceConfig;
    }
}
