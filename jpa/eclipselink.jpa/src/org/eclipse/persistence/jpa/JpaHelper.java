/*******************************************************************************
 * Copyright (c) 1998, 2007 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0, which accompanies this distribution
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Oracle - initial API and implementation from Oracle TopLink
 ******************************************************************************/  
package org.eclipse.persistence.jpa; 

import javax.persistence.EntityManagerFactory; 
import javax.persistence.Query; 
import org.eclipse.persistence.internal.jpa.*; 
import org.eclipse.persistence.internal.localization.ExceptionLocalization;
import org.eclipse.persistence.jpa.JpaEntityManager; 
import org.eclipse.persistence.queries.*; 
import org.eclipse.persistence.sessions.server.Server; 
import org.eclipse.persistence.sessions.server.ServerSession; 
import org.eclipse.persistence.sessions.factories.SessionFactory; 

/** 
 * This sample illustrates the JPA helper methods that may be of use 
 * to EclipseLink customers attempting to leverage EclipseLink specific functionality. 
 * 
 * @author dclarke, gpelletie 
 */ 
public class JpaHelper { 
    /** 
     * Verify if the JPA provider is EclipseLink. If you are in a container 
     * and not in a transaction this method may incorrectly return false. 
     * It is always more reliable to check isEclipseLink on the EMF or Query. 
     */ 
    public static boolean isEclipseLink(javax.persistence.EntityManager em) {
    	return getEntityManager(em) != null; 
    } 

    /** 
     * Verify if the JPA provider is EclipseLink 
     */ 
    public static boolean isEclipseLink(EntityManagerFactory emf) { 
        try { 
            getEntityManagerFactory(emf); 
        } catch (IllegalArgumentException iae) { 
            return false; 
        } 

        return true; 
    } 

    /** 
     * Verify if the JPA provider is EclipseLink 
     */ 
    public static boolean isEclipseLink(Query query) { 
        try { 
            getReadAllQuery(query); 
        } catch (IllegalArgumentException iae) { 
            return false; 
        }

        return true; 
    } 

    /** 
     * Determine if the JPA query is a EclipseLink ReportQuery. Useful for 
     * frameworks that want to determine which get_X_Query method they can 
     * safely invoke. 
     */ 
    public static boolean isReportQuery(Query query) { 
        try { 
            getReportQuery(query); 
        } catch (IllegalArgumentException iae) { 
            return false; 
        } 
        return true; 
    } 

    /** 
     * Access the internal EclipseLink query wrapped within the JPA query. A 
     * EclipseLink JPA created from JP QL  contains a ReportQuery if multiple 
     * items or a non-entity type is being returned. This method will fail 
     * if a single entity type is being returned as the query is a ReadAllQuery. 
     * 
     * @see JpaHelper#getReadAllQuery 
     */ 
    public static ReportQuery getReportQuery(Query query) { 
        if (EJBQueryImpl.class.isAssignableFrom(query.getClass())) { 
            DatabaseQuery dbQuery = ((EJBQueryImpl)query).getDatabaseQuery(); 
            if (dbQuery.isReportQuery()) { 
                return (ReportQuery)dbQuery; 
            } 

            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa_helper_invalid_report_query" + query.getClass()));
        } 
 
        throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa_helper_invalid_query" + query.getClass()));         
    } 

    /** 
     * Access the internal EclipseLink query wrapped within the JPA query. A EclipseLink 
     * JPA created from JP QL only contains a ReadAllQuery if only a single entity 
     * type is being returned. 
     * 
     * A ReadAllQuery is the super class of a ReportQuery so this method will 
     * always work for either a ReportQuery or ReadAllQuery. 
     */ 
    public static ReadAllQuery getReadAllQuery(Query query) { 
        if (EJBQueryImpl.class.isAssignableFrom(query.getClass())) { 
            DatabaseQuery dbQuery = ((EJBQueryImpl)query).getDatabaseQuery(); 
            if (dbQuery.isReadAllQuery()) { 
                return (ReadAllQuery)dbQuery; 
            } 

            throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa_helper_invalid_read_all_query" + query.getClass()));
        } 
        
        throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa_helper_invalid_query" + query.getClass()));                
    } 

    /** 
     * Create a EclipseLink JPA query dynamically given a EclipseLink query. 
     */ 
    public static Query createQuery(ReadAllQuery query, javax.persistence.EntityManager em) { 
        EntityManagerImpl emImpl = (EntityManagerImpl)getEntityManager(em); 
        return new EJBQueryImpl(query, emImpl); 
    } 

    /** 
     * Convert a JPA entityManager into a EclipseLink specific one. This will work 
     * both within a JavaSE deployment as well as within a container where the 
     * EntityManager may be wrapped. 
     * 
     * In the case where the container is not in a transaction it may return null 
     * for its delegate. When this happens the only way to access an EntityManager 
     * is to use the EntityManagerFactory to create a temporary one where the 
     * application manage its lifecycle. 
     */ 
    public static JpaEntityManager getEntityManager(javax.persistence.EntityManager entityManager) { 
        if (JpaEntityManager.class.isAssignableFrom(entityManager.getClass())) { 
            return (JpaEntityManager)entityManager; 
        } 

        if (entityManager.getDelegate() != null) { 
            return getEntityManager((JpaEntityManager)entityManager.getDelegate()); 
        } 

        return null; 
    } 

    /** 
     * Given a JPA EntityManagerFactory attempt to cast it to a EclipseLink EMF. 
     */ 
    public static EntityManagerFactoryImpl getEntityManagerFactory(EntityManagerFactory emf) { 
        if (EntityManagerFactoryImpl.class.isAssignableFrom(emf.getClass())) { 
            return (EntityManagerFactoryImpl)emf; 
        } 

        throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa_helper_invalid_entity_manager_factory" + emf.getClass()));
    } 

    /** 
     * Retrieve the shared server session from the EMF. 
     */ 
    public static Server getServerSession(EntityManagerFactory emf) { 
        return getEntityManagerFactory(emf).getServerSession(); 
    } 

    /** 
     * Create a EclipseLink EMF given a ServerSession that has already been created 
     * and logged in. 
     */ 
    public static javax.persistence.EntityManagerFactory createEntityManagerFactory(Server session) { 
        return new EntityManagerFactoryImpl((ServerSession)session); 
    } 

    /** 
     * Create a EclipseLink EMF using a session name and sessions.xml. This is 
     * equivalent to using the EclipseLink.session-xml and EclipseLink.session-name PU 
     * properties with the exception that no persistence.xml is required. 
     * 
     * The application would be required to manage this singleton EMF. 
     */ 
    public static EntityManagerFactoryImpl createEntityManagerFactory(String sessionName) { 
        SessionFactory sf = new SessionFactory(sessionName); 
        // Verify that shared session is a ServerSession 
        return new EntityManagerFactoryImpl((ServerSession)sf.getSharedSession()); 
    } 
}