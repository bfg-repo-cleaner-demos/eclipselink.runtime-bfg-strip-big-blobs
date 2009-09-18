/*******************************************************************************
 * Copyright (c) 2008, 2009 Sun Microsystems. All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0 
 * which accompanies this distribution. 
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * 
 * Contributors:
 *     Linda DeMichiel -Java Persistence 2.0 - Proposed Final Draft, Version 2.0 (August 31, 2009)
 *     Specification available from http://jcp.org/en/jsr/detail?id=317
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

import java.util.Set;
import java.util.Map;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.criteria.QueryBuilder;

/**
 * Interface used to interact with the entity manager factory
 * for the persistence unit.
 *
 * <p>When the application has finished using the entity manager
 * factory, and/or at application shutdown, the application should
 * close the entity manager factory.  Once an
 * <code>EntityManagerFactory</code> has been closed, all its entity managers
 * are considered to be in the closed state.
 *
 * @since Java Persistence 1.0
 */
public interface EntityManagerFactory {

    /**
     * Create a new application-managed <code>EntityManager</code>.
     * This method returns a new <code>EntityManager</code> instance each time
     * it is invoked. 
     * The <code>isOpen</code> method will return true on the returned instance.
     * @return entity manager instance
     * @throws IllegalStateException if the entity manager factory
     * has been closed.
     */
    public EntityManager createEntityManager();
    
    /**
     * Create a new application-managed <code>EntityManager</code> with the 
     * specified Map of properties. 
     * This method returns a new <code>EntityManager</code> instance each time
     * it is invoked. 
     * The <code>isOpen</code> method will return true on the returned instance.
     * @param map properties for entity manager
     * @return entity manager instance
     * @throws IllegalStateException if the entity manager factory
     * has been closed.
     */
    public EntityManager createEntityManager(Map map);

    /**
     * Return an instance of <code>QueryBuilder</code> for the creation of
     * <code>CriteriaQuery</code> objects.
     * @return QueryBuilder instance
     * @throws IllegalStateException if the entity manager factory 
     * has been closed.
     *
     * @since Java Persistence 2.0
     */
    public QueryBuilder getQueryBuilder();
    
    /**
     * Return an instance of <code>Metamodel</code> interface for access to the
     * metamodel of the persistence unit.
     * @return Metamodel instance
     * @throws IllegalStateException if the entity manager factory
     * has been closed.
     *
     * @since Java Persistence 2.0
     */
    public Metamodel getMetamodel();

    /**
     * Indicates whether the factory is open. Returns true
     * until the factory has been closed.
     * @return boolean indicating whether the factory is open
     */
    public boolean isOpen();
    
    /**
     * Close the factory, releasing any resources that it holds.
     * After a factory instance has been closed, all methods invoked
     * on it will throw an <code>IllegalStateException</code>, except
     * for <code>isOpen</code>, which will return false. Once an
     * <code>EntityManagerFactory</code> has been closed, all its
     * entity managers are considered to be in the closed state.
     * @throws IllegalStateException if the entity manager factory
     * has been closed.
     */
    public void close();

    /**
     * Get the properties and associated values that are in effect
     * for the entity manager factory. Changing the contents of the
     * map does not change the configuration in effect.
     * @return properties
     *
     * @since Java Persistence 2.0
     */
    public Map<String, Object> getProperties();

    /**
     * Get the names of the properties that are supported for use 
     * with the entity manager factory.  These correspond to 
     * properties that may be passed to the methods of the 
     * <code>EntityManagerFactory</code> interface that take a properties 
     * argument. These include all standard properties as well as 
     * vendor-specific properties supported by the provider. These 
     * properties may or may not currently be in effect.
     * @return properties and hints
     *
     * @since Java Persistence 2.0
     */
    public Set<String> getSupportedProperties();

    /**
     * Access the cache that is associated with the entity manager 
     * factory (the "second level cache").
     * @return instance of the <code>Cache</code> interface
     * @throws IllegalStateException if the entity manager factory
     * has been closed.
     *
     * @since Java Persistence 2.0
     */
    public Cache getCache();

    /**
     * Return interface providing access to utility methods
     * for the persistence unit.
     * @return <code>PersistenceUnitUtil</code> interface
     * @throws IllegalStateException if the entity manager factory
     * has been closed.
     *
     * @since Java Persistence 2.0
     */
    public PersistenceUnitUtil getPersistenceUnitUtil();
}
