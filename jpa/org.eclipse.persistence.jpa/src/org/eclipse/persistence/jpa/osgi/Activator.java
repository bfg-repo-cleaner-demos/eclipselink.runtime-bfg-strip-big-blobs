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
 *     tware, ssmith = 1.0 - Activator for EclipseLink persistence
 ******************************************************************************/  
package org.eclipse.persistence.jpa.osgi;

import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * Activator for JPA OSGi service.
 * @author tware
 *
 */
public class Activator implements BundleActivator, SynchronousBundleListener {
    private static BundleContext context;
    
    /**
     * Simply add bundles to our bundle list as they start and remove them as they stop
     */
    public void bundleChanged(BundleEvent event) {
        switch (event.getType()) {
            case BundleEvent.STARTING:
                registerBundle(event.getBundle());
                break;
    
            case BundleEvent.STOPPING:
                deregisterBundle(event.getBundle());
                break;
        }
    }

    /**
     * On start, we do two things
     * We register a listener for bundles and we start our JPA server
     */
    public void start(BundleContext context) throws Exception {
        Activator.context = context;    
        registerBundleListener();
        registerProviderService();
    }

    /**
     * Add our bundle listener
     */
    private void registerBundleListener() {
        getContext().addBundleListener(this);
        Bundle bundles[] = getContext().getBundles();
        for (int i = 0; i < bundles.length; i++) {
            Bundle bundle = bundles[i];
            registerBundle(bundle);
        }
    }

    /**
     * Store a reference to a bundle as it is started so the bundle 
     * can be accessed later
     * @param bundle
     */
    private void registerBundle(Bundle bundle) {
        if ((bundle.getState() & (Bundle.STARTING | Bundle.ACTIVE)) != 0) {
            try {
                String[] persistenceUnitNames = getPersistenceUnitNames(bundle);
                if (persistenceUnitNames != null) {
                    org.eclipse.persistence.internal.jpa.deployment.osgi.OSGiPersistenceInitializationHelper.addBundle(bundle, persistenceUnitNames);
                }
            } catch (Exception e) {
                // TODO: Log this properly
                e.printStackTrace();
            }
        }
    }
    
    private String[] getPersistenceUnitNames(Bundle bundle) {
        String names = (String) bundle.getHeaders().get("JPA-PersistenceUnits");
        if (names != null) {
            return names.split(",");
        } else {
            return null;
        }
    }
    
    private void deregisterBundle(Bundle bundle) {
        org.eclipse.persistence.internal.jpa.deployment.osgi.OSGiPersistenceInitializationHelper.removeBundle(bundle);
    }

    public void stop(BundleContext context) throws Exception {
        getContext().removeBundleListener(this);    
    }

    public static BundleContext getContext() {
        return Activator.context;
    }
    
    public static final String PERSISTENCE_PROVIDER = "javax.persistence.spi.PersistenceProvider";
    public static final String ECLIPSELINK_OSGI_PROVIDER = "org.eclipse.persistence.jpa.osgi.PersistenceProviderOSGi";
    
    /**
     * Our service provider provides the javax.persistence.spi.PersistenceProvider service.
     * In this method, we register as a provider of that service
     * @throws Exception
     */
    public void registerProviderService() throws Exception {
        // Create and register ourselves as a JPA persistence provider service
        PersistenceProvider providerService = new PersistenceProvider();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(PERSISTENCE_PROVIDER, ECLIPSELINK_OSGI_PROVIDER);
        getContext().registerService(PERSISTENCE_PROVIDER, providerService, props);
    }
}


