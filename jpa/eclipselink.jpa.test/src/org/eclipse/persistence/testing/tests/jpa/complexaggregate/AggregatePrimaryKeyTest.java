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
 *     08/27/2008-1.1 Guy Pelletier 
 *       - 211329: Add sequencing on non-id attribute(s) support to the EclipseLink-ORM.XML Schema
 ******************************************************************************/  
package org.eclipse.persistence.testing.tests.jpa.complexaggregate;

import org.eclipse.persistence.internal.jpa.EntityManagerImpl;
import org.eclipse.persistence.testing.models.jpa.complexaggregate.*;
import org.eclipse.persistence.testing.tests.jpa.EntityContainerTestBase;
import org.eclipse.persistence.testing.framework.TestErrorException;

/**
 * BUG 4219210 - EJB30: EMBEDABLE IDS ARE NOT SHARABLE
 *
 * @author Guy Pelletier
 * @date April 8, 2005
 * @version 1.0
 */
public class AggregatePrimaryKeyTest extends EntityContainerTestBase {
    protected boolean m_reset = false;    // reset gets called twice on error
    protected Exception m_exception;
        
    public AggregatePrimaryKeyTest() {
        setDescription("Tests an aggregate that is also the primary key.");
    }
    
    public void setup () {
        super.setup();
        m_reset = true;
        m_exception = null;
        ((EntityManagerImpl)getEntityManager()).getActiveSession().getIdentityMapAccessor().initializeAllIdentityMaps();
    }
    
    public void test() throws Exception {
        try {
            Name sharedName = new Name();
            sharedName.setFirstName("Tom");
            sharedName.setLastName("Ware");
            
            CountryDweller countryDweller = new CountryDweller();
            countryDweller.setAge(30);
            countryDweller.setName(sharedName);
            
            CitySlicker citySlicker = new CitySlicker();
            citySlicker.setAge(53);
            citySlicker.setName(sharedName);
            
            Name name = new Name();
            name.setFirstName("Guy");
            name.setLastName("Pelletier");
            
            CountryDweller countryDweller2 = new CountryDweller();
            countryDweller2.setAge(65);
            countryDweller2.setName(name);
        
            beginTransaction();
            getEntityManager().persist(countryDweller);
            getEntityManager().persist(countryDweller2);
            getEntityManager().persist(citySlicker);
            commitTransaction();
        
            // Clear the cache.
            ((EntityManagerImpl)getEntityManager()).getActiveSession().getIdentityMapAccessor().initializeAllIdentityMaps();            
            
            // Now read them back in and delete them.
            beginTransaction();
            
            CitySlicker cs = getEntityManager().find(CitySlicker.class, sharedName);
            CountryDweller cd = getEntityManager().merge(countryDweller);
            CountryDweller cd2 = getEntityManager().merge(countryDweller2);
            
            getEntityManager().remove(cs);
            getEntityManager().remove(cd);
            getEntityManager().remove(cd2);
            
            commitTransaction();
        } catch (RuntimeException e) {
            rollbackTransaction();
            m_exception = e;
        }
    }
    
    public void reset () {
        if (m_reset) {
            m_reset = false;
        }
    }
    
    public void verify() {
        if (m_exception != null) {
            throw new TestErrorException("Something went terribly wrong when creating a CountryDweller and a CitySlicker with the same name: " + m_exception.getMessage());
        }
    }
}
