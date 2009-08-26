/*******************************************************************************
 * Copyright (c) 1998, 2009 Oracle. All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * 
 * Contributors:
 *     dclarke - Java Persistence 2.0 - Proposed Final Draft (March 13, 2009)
 *     		     Specification available from http://jcp.org/en/jsr/detail?id=317
 *
 * Java(TM) Persistence API, Version 2.0 - EARLY ACCESS
 * This is an implementation of an early-draft specification developed under the 
 * Java Community Process (JCP).  The code is untested and presumed not to be a  
 * compatible implementation of JSR 317: Java(TM) Persistence API, Version 2.0.   
 * We encourage you to migrate to an implementation of the Java(TM) Persistence 
 * API, Version 2.0 Specification that has been tested and verified to be compatible 
 * as soon as such an implementation is available, and we encourage you to retain 
 * this notice in any implementation of Java(TM) Persistence API, Version 2.0 
 * Specification that you distribute.
 ******************************************************************************/
package javax.persistence;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceProviderResolver;
import javax.persistence.spi.PersistenceProviderResolverHolder;
import javax.persistence.spi.LoadState;

/**
 * Bootstrap class that is used to obtain an {@link EntityManagerFactory}.
 * 
 * @since Java Persistence 1.0
 */
public class Persistence {
    
    /**
     * Create and return an EntityManagerFactory for the named persistence unit.
     * 
     * @param persistenceUnitName
     *            The name of the persistence unit
     * @return The factory that creates EntityManagers configured according to
     *         the specified persistence unit
     */
    public static EntityManagerFactory createEntityManagerFactory(String persistenceUnitName) {
        return createEntityManagerFactory(persistenceUnitName, null);
    }

    /**
     * Create and return an EntityManagerFactory for the named persistence unit
     * using the given properties.
     * 
     * @param persistenceUnitName
     *            The name of the persistence unit
     * @param properties
     *            Additional properties to use when creating the factory. The
     *            values of these properties override any values that may have
     *            been configured elsewhere.
     * @return The factory that creates EntityManagers configured according to
     *         the specified persistence unit.
     */
    public static EntityManagerFactory createEntityManagerFactory(String persistenceUnitName, Map properties) {

        EntityManagerFactory emf = null;
        PersistenceProviderResolver resolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();

        List<PersistenceProvider> providers = resolver.getPersistenceProviders();

        for (PersistenceProvider provider : providers) {
            emf = provider.createEntityManagerFactory(persistenceUnitName, properties);
            if (emf != null) {
                break;
            }
        }
        if (emf == null) {
            throw new PersistenceException("No Persistence provider for EntityManager named " + persistenceUnitName);
        }
        return emf;
    }

    /**
     * Return the PersistenceUtil instance
     */
    public static PersistenceUtil getPersistenceUtil() {
       return new PersistenceUtilImpl();
    }

    
    /**
     * Implementation of PersistenceUtil interface
     * @since Java Persistence 2.0
     */
    private static class PersistenceUtilImpl implements PersistenceUtil {
        public boolean isLoaded(Object entity, String attributeName) {
            PersistenceProviderResolver resolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();

            List<PersistenceProvider> providers = resolver.getPersistenceProviders();

            for (PersistenceProvider provider : providers) {
                LoadState loadstate = provider.isLoadedWithoutReference(entity, attributeName);
                if(loadstate == LoadState.LOADED) {
                    return true;
                } else if (loadstate == LoadState.NOT_LOADED) {
                    return false;
                } // else continue
            }

            //None of the providers could determine the load state try isLoadedWithReference
            for (PersistenceProvider provider : providers) {
                LoadState loadstate = provider.isLoadedWithReference(entity, attributeName);
                if(loadstate == LoadState.LOADED) {
                    return true;
                } else if (loadstate == LoadState.NOT_LOADED) {
                    return false;
                } // else continue
            }

            //None of the providers could determine the load state.
            return true;
        }

        public boolean isLoaded(Object entity) {
            PersistenceProviderResolver resolver = PersistenceProviderResolverHolder.getPersistenceProviderResolver();

            List<PersistenceProvider> providers = resolver.getPersistenceProviders();

            for (PersistenceProvider provider : providers) {
                LoadState loadstate = provider.isLoaded(entity);
                if(loadstate == LoadState.LOADED) {
                    return true;
                } else if (loadstate == LoadState.NOT_LOADED) {
                    return false;
                } // else continue
            }
            //None of the providers could determine the load state
            return true;
        }
    }

    /**
     * This final String is deprecated and should be removed and is only here for TCK backward compatibility
     * @since Java Persistence 1.0
     * @deprecated
     */
    @Deprecated
    public static final String PERSISTENCE_PROVIDER = "javax.persistence.spi.PeristenceProvider";
    
    /**
     * This instance variable is deprecated and should be removed and is only here for TCK backward compatibility
     * @since Java Persistence 1.0
     * @deprecated
     */
    @Deprecated
    protected static final Set<PersistenceProvider> providers = new HashSet<PersistenceProvider>();
}
