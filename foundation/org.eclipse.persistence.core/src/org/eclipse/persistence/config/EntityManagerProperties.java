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
 *    Gyorke
 *    
 *     05/28/2008-1.0M8 Andrei Ilitchev 
 *        - 224964: Provide support for Proxy Authentication through JPA.
 *        Now properties' names that could be used both in createEM and createEMF are the same. 
 ******************************************************************************/  
package org.eclipse.persistence.config;

import org.eclipse.persistence.config.PersistenceUnitProperties;

/**
 * The class defines EclipseLink properties' names for use at the EntityManager level.
 * 
 * This properties are specific to an EnityManger and should be 
 * passed to createEntityManager methods of EntityManagerFactory.
 * 
 * Property values are usually case-insensitive with some common sense exceptions,
 * for instance class names.
 * 
 */
public class EntityManagerProperties {
	
    /**
     * Set to "true" this property forces persistence context to read through JTA-managed ("write") connection
     * in case there is an active transaction.
     * Valid values are case-insensitive "false" and "true"; "false" is default.
     * The property could also be set in persistence.xml or passed to createEntityManagerFactory,
     * Note that if the property set to "true" then objects read during transaction won't be placed into the
     * shared cache unless they have been updated.
     * in that case it affects all EntityManagers created by the factory. 
     */
    public static final String JOIN_EXISTING_TRANSACTION = PersistenceUnitProperties.JOIN_EXISTING_TRANSACTION;
    
    /**
     * Specifies whether there should be hard or soft references used within the Persistence Context.
     * Default is "HARD".  With soft references entities no longer referenced by the application
     * may be garbage collected freeing resources.  Any changes that have not been flushed in these
     * entities will be lost.
     * The property could also be set in persistence.xml or passed to createEntityManagerFactory,
     * in that case it affects all EntityManagers created by the factory. 
     * @see org.eclipse.persistence.sessions.factories.ReferenceMode
     */
    public static final String PERSISTENCE_CONTEXT_REFERENCE_MODE = PersistenceUnitProperties.PERSISTENCE_CONTEXT_REFERENCE_MODE;

    /**
     * Specifies that the EntityManager will not be close or not used after commit (not extended).
     * In general this is normally always the case for a container managed EntityManager,
     * and common for application managed.
     * This can be used to avoid additional performance overhead of resuming the persistence context
     * after a commit().
     * The property set in persistence.xml or passed to createEntityManagerFactory affects all EntityManagers
     * created by the factory.
     * Alternatively, to apply the property only to some EntityManagers pass it to createEntityManager method.
     * Either "true" or "false.  "false" is the default.
     */
    public static final String PERSISTENCE_CONTEXT_CLOSE_ON_COMMIT = PersistenceUnitProperties.PERSISTENCE_CONTEXT_CLOSE_ON_COMMIT;
    
    /**
     * This property is used to specify proxy type that should be passed to OarcleConnection.openProxySession method.
     * Requires Oracle jdbc version 10.1.0.2 or later.
     * Requires Oracle9Platform or later as a database platform 
     * (TARGET_DATABASE property value should be TargetDatabase.Oracle9 or later).
     * The valid values are:
     * OracleConnection.PROXYTYPE_USER_NAME, OracleConnection.PROXYTYPE_DISTINGUISHED_NAME, OracleConnection.PROXYTYPE_CERTIFICATE.
     * Property property corresponding to the specified type should be also provided:
     * OracleConnection.PROXY_USER_NAME, OracleConnection.PROXY_DISTINGUISHED_NAME, OracleConnection.PROXY_CERTIFICATE.
     * Typically these properties should be set into EntityManager (either through createEntityManager method or
     * using proprietary setProperties method on EntityManagerImpl) - that causes EntityManager to use proxy connection for
     * writing and reading inside transaction. 
     * If proxy-type and the corresponding proxy property set into EntityManagerFactory then all connections
     * created by the factory will be proxy connections.
     */
    public static final String ORACLE_PROXY_TYPE = PersistenceUnitProperties.ORACLE_PROXY_TYPE;
    
    /**
     * Determines when reads are performed through the write connection.
     * This property alters ConnectionPolicy.
     * @see ExclusiveConnectionMode
     */
    public static final String EXCLUSIVE_CONNECTION_MODE = PersistenceUnitProperties.EXCLUSIVE_CONNECTION_MODE;
    
    /**
     * Determines when write connection is acquired lazily.
     * Valid values are case-insensitive "false" and "true"; "true" is default.
     * This property alters ConnectionPolicy.
     */
    public static final String EXCLUSIVE_CONNECTION_IS_LAZY = PersistenceUnitProperties.EXCLUSIVE_CONNECTION_IS_LAZY;
    
    /**
     * JTA DataSource.
     * The value may be either data source or its name.
     * Note that this property will be ignore in case persistence unit was setup to NOT use JTA:
     * persistence.xml or createEntityManagerFactory had property "javax.persistence.transactionType" with RESOURCE_LOCAL value.
     * To avoid a conflict resulting in exception don't specify this property together with either JDBC_DRIVER or JDBC_URL;
     * however this property may override JDBC_DRIVER or JDBC_URL specified in persistence.xml or in createEntityManagerFactory method.
     * This property alters ConnectionPolicy.
     */
    public static final String JTA_DATASOURCE = PersistenceUnitProperties.JTA_DATASOURCE;

    /**
     * NON JTA DataSource.
     * The value may be either data source or its name.
     * Note that this property will be ignore in case persistence unit was setup to use JTA:
     * persistence.xml or createEntityManagerFactory had property "javax.persistence.transactionType" with JTA value. 
     * To avoid a conflict resulting in exception don't specify this property together with either JDBC_DRIVER or JDBC_URL;
     * however this property may override JDBC_DRIVER or JDBC_URL specified in persistence.xml or in createEntityManagerFactory method.
     * This property alters ConnectionPolicy.
     */
    public static final String NON_JTA_DATASOURCE = PersistenceUnitProperties.NON_JTA_DATASOURCE;
    
    /** JDBC Driver class name. 
     * To avoid a conflict resulting in exception don't specify this property together with either JTA_DATASOURCE or JTA_DATASOURCE;
     * however this property may override JTA_DATASOURCE or JTA_DATASOURCE specified in persistence.xml or in createEntityManagerFactory method.
     * This property alters ConnectionPolicy.
     */
    public static final String JDBC_DRIVER = PersistenceUnitProperties.JDBC_DRIVER;

    /** JDBC Connection String. 
     * To avoid a conflict resulting in exception don't specify this property together with either JTA_DATASOURCE or JTA_DATASOURCE;
     * however this property may override JTA_DATASOURCE or JTA_DATASOURCE specified in persistence.xml or in createEntityManagerFactory method.
     * This property alters ConnectionPolicy.
     */
    public static final String JDBC_URL = PersistenceUnitProperties.JDBC_URL;

    /** DataSource or JDBC DriverManager user name. 
     * Non-empty value overrides the value assigned in persistence.xml or in createEntityManagerFactory;
     * empty string value causes removal this property and JDBC_PASSWORD property 
     * specified in persistence.xml or in createEntityManagerFactory method.
     * This property alters ConnectionPolicy.
     */
    public static final String JDBC_USER = PersistenceUnitProperties.JDBC_USER;

    /** DataSource or JDBC DriverManager password. 
     * Non-empty value overrides the value assigned in persistence.xml or in createEntityManagerFactory;
     * empty string value causes removal this property 
     * specified in persistence.xml or in createEntityManagerFactory method.
     * This property alters ConnectionPolicy.
     */
    public static final String JDBC_PASSWORD = PersistenceUnitProperties.JDBC_PASSWORD;

    /** ConnectionPolicy 
     * Allows to specify an entire ConnectionPolicy.
     * Note that in case any other ConnectionPolicy-altering properties are present
     * they will be applied to this ConnectionPolicy. 
     */
    public static final String CONNECTION_POLICY = "eclipselink.jdbc.connection-policy";
}
