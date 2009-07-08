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
import org.eclipse.persistence.queries.FetchGroupTracker;


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
        return query instanceof JpaQuery;
    } 

    /** 
     * Determine if the JPA query is a EclipseLink ReportQuery. Useful for 
     * frameworks that want to determine which get_X_Query method they can 
     * safely invoke. 
     */ 
    public static boolean isReportQuery(Query query) { 
        return isEclipseLink(query) && getDatabaseQuery(query).isReportQuery();
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
        DatabaseQuery dbQuery = getDatabaseQuery(query);
        if (dbQuery.isReportQuery()) { 
            return (ReportQuery)dbQuery; 
        } 

        throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa_helper_invalid_report_query" + query.getClass()));
    }

    /** 
     * Access the internal EclipseLink query wrapped within the JPA query.
     */ 
    public static DatabaseQuery getDatabaseQuery(Query query) { 
        if (query instanceof JpaQuery) {
            return ((JpaQuery)query).getDatabaseQuery();
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
        DatabaseQuery dbQuery = getDatabaseQuery(query);
        if (dbQuery.isReadAllQuery()) { 
            return (ReadAllQuery)dbQuery; 
        } 

        throw new IllegalArgumentException(ExceptionLocalization.buildMessage("jpa_helper_invalid_read_all_query" + query.getClass()));           
    } 

    /** 
     * Create a EclipseLink JPA query dynamically given a EclipseLink query. 
     */ 
    public static Query createQuery(DatabaseQuery query, javax.persistence.EntityManager em) { 
        return getEntityManager(em).createQuery(query);
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
        if (entityManager instanceof JpaEntityManager) { 
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
        if (emf instanceof EntityManagerFactoryImpl) { 
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

    /**
     * Load/fetch the unfetched object.  This method is used by the ClassWeaver.
     */
    public static void loadUnfetchedObject(Object object) {
        ReadObjectQuery query = new ReadObjectQuery(object);
        query.setShouldUseDefaultFetchGroup(false);
        Object result = ((FetchGroupTracker)object)._persistence_getSession().executeQuery(query);
        if (result == null) {
            Object[] args = {query.getSelectionKey()};
            String message = ExceptionLocalization.buildMessage("no_entities_retrieved_for_get_reference", args);
            throw new javax.persistence.EntityNotFoundException(message);
        }
    }

}
