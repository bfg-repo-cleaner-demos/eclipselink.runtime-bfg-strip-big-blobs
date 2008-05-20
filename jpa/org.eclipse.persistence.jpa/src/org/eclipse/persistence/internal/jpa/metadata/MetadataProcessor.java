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
 *     05/16/2008-1.0M8 Guy Pelletier 
 *       - 218084: Implement metadata merging functionality between mapping files
 ******************************************************************************/  
package org.eclipse.persistence.internal.jpa.metadata;

import java.io.IOException;

import java.net.URISyntaxException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.spi.PersistenceUnitInfo;

import org.eclipse.persistence.internal.jpa.deployment.Archive;
import org.eclipse.persistence.internal.jpa.deployment.ArchiveFactoryImpl;
import org.eclipse.persistence.internal.jpa.deployment.PersistenceUnitProcessor;

import org.eclipse.persistence.exceptions.PersistenceUnitLoadingException;
import org.eclipse.persistence.exceptions.ValidationException;

import org.eclipse.persistence.internal.jpa.EntityManagerSetupImpl;

import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.ClassAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EmbeddableAccessor;
import org.eclipse.persistence.internal.jpa.metadata.accessors.classes.EntityAccessor;

import org.eclipse.persistence.internal.jpa.metadata.converters.StructConverterMetadata;

import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappingsReader;
import org.eclipse.persistence.internal.jpa.metadata.xml.XMLEntityMappings;

import org.eclipse.persistence.internal.sessions.AbstractSession;

import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;

import org.eclipse.persistence.internal.sessions.factories.DescriptorCustomizer;
import org.eclipse.persistence.jpa.config.PersistenceUnitProperties;
import org.eclipse.persistence.platform.database.converters.StructConverter;

/**
 * INTERNAL:
 * The object/relational metadata processor for the EJB3.0 specification. 
 * 
 * @author Guy Pelletier
 * @since TopLink EJB 3.0 Reference Implementation
 */
public class MetadataProcessor {    
    protected ClassLoader m_loader;
    protected MetadataProject m_project;
    protected AbstractSession m_session;

    /**
     * INTERNAL:
     * Called from EntityManagerSetupImpl. The 'real' EJB 3.0 processing
     * that includes XML and annotations.
     */
    public MetadataProcessor(PersistenceUnitInfo puInfo, AbstractSession session, ClassLoader loader, boolean enableLazyForOneToOne, boolean weaveEager) {
        m_loader = loader;
        m_session = session;
        m_project = new MetadataProject(puInfo, session, enableLazyForOneToOne, weaveEager);
    }
    
    /**
     * INTERNAL: 
     * Method to place EntityListener's on the descriptors from the given 
     * session. This call is made from the EntityManagerSetup deploy call.
     */
    public void addEntityListeners() {
        // Process the listeners for all the class accessors, but before
        // doing so, update the accessors associated class since the loader 
        // should have changed changed.
        for (EntityAccessor accessor : m_project.getEntityAccessors()) { 
            accessor.setJavaClass(accessor.getDescriptor().getClassDescriptor().getJavaClass());
            accessor.processListeners(m_loader);
        }
    }
    
    /**
     * INTERNAL:
     * Method to place NamedQueries and NamedNativeQueries on the given session. 
     * This call is made from the EntityManagerSetup deploy call.
     */
    public void addNamedQueries() {
        m_project.processQueries(m_loader);
    }
    
    /**
     * INTERNAL:
     * Return a set of class names for each entity, embeddable and mapped 
     * superclass found in the mapping files to be processed by the 
     * MetadataProcessor.
     */
    public Set<String> getPersistenceUnitClassSetFromMappingFiles() {
        HashSet<String> classSet = new HashSet<String>();
        
        for (XMLEntityMappings entityMappings : m_project.getEntityMappings()) {
            for (ClassAccessor entity : entityMappings.getEntities()) {
                classSet.add(entityMappings.getFullyQualifiedClassName(entity.getClassName()));
            }
            
            for (ClassAccessor embeddable : entityMappings.getEmbeddables()) {
                classSet.add(entityMappings.getFullyQualifiedClassName(embeddable.getClassName()));
            }
            
            for (ClassAccessor mappedSuperclass : entityMappings.getMappedSuperclasses()) {
                classSet.add(entityMappings.getFullyQualifiedClassName(mappedSuperclass.getClassName()));
            }
        }
        
        return classSet;
    }

    /**
     * INTERNAL:
     */
    public MetadataProject getProject() {
        return m_project;
    }
    
    /**
     * INTERNAL:
     * Return a list of instantiated StructConverter objects that were defined in the
     * metadata of this project.
     * 
     * These StructConverters can be added to the Session 
     */
    public List<StructConverter> getStructConverters() {
        List<StructConverter> structConverters = new ArrayList<StructConverter>();
        for (StructConverterMetadata converter: m_project.getStructConverters()) {
            StructConverter structConverter = (StructConverter) MetadataHelper.getClassInstance(converter.getConverterClassName(), m_loader);
            structConverters.add(structConverter);
        }
        
        return structConverters;
    }
     
    /**
     * INTERNAL:
     * Handle an exception that occurred while processing ORM xml.
     */
    protected void handleORMException(RuntimeException e, String mappingFile, boolean throwException){
        if (m_session == null) {
            // Metadata processor is mainly used with a session. Java SE 
            // bootstraping uses some functions such as ORM processing without
            // a session. In these cases, it is impossible to get the session 
            // to properly handle the exception. As a result we log an error. 
            // The same code will be called later in the bootstrapping code 
            // and the error will be handled then.
            AbstractSessionLog.getLog().log(SessionLog.CONFIG, EntityManagerSetupImpl.ERROR_LOADING_XML_FILE, new Object[] {mappingFile, e});
        } else if (!throwException) {
            // fail quietly
            m_session.log(SessionLog.CONFIG, SessionLog.EJB_OR_METADATA, EntityManagerSetupImpl.ERROR_LOADING_XML_FILE, new Object[] {mappingFile, e});
        } else {
            // fail loudly
            m_session.handleException(e);
        }
    }
    
    /**
     * INTERNAL:
     * This method is responsible for discovering all the entity classes for 
     * this PU and adding corresponding MetadataDescriptor in the 
     * MetadataProject.
     * 
     * This method will also gather all the weavable classes for this PU. 
     * Currently, entity and embeddable classes are weavable.
     */
    protected void initPersistenceUnitClasses() {
        // 1 - Iterate through the classes that are defined in the <mapping>
        // files and add them to the map. This will merge the accessors where
        // necessary.
        HashMap<String, EntityAccessor> entities = new HashMap<String, EntityAccessor>();
        HashMap<String, EmbeddableAccessor> embeddables = new HashMap<String, EmbeddableAccessor>();
        
        for (XMLEntityMappings entityMappings : m_project.getEntityMappings()) {
            entityMappings.initPersistenceUnitClasses(entities, embeddables);  
        }

        // 2 - Iterate though all the XML entities and add them to the project 
        // and apply any persistence unit defaults.
        for (EntityAccessor entity : entities.values()) {
            // This will apply global persistence unit defaults.
            m_project.addEntityAccessor(entity);
            
            // This will override any global settings.
            entity.getAccessibleObject().getEntityMappings().processEntityMappingsDefaults(entity);
        }

        // 3 - Iterate though all the XML embeddables and add them to the 
        // project and apply any persistence unit defaults.
        for (EmbeddableAccessor embeddable : embeddables.values()) {
            // This will apply global persistence unit defaults.
            m_project.addEmbeddableAccessor(embeddable);
            
            // This will override any global settings.
            embeddable.getAccessibleObject().getEntityMappings().processEntityMappingsDefaults(embeddable);
        }
        
        // 4 - Iterate through the classes that are references from the
        // persistence.xml file.
        PersistenceUnitInfo persistenceUnitInfo = m_project.getPersistenceUnitInfo();
        List<String> classNames = new ArrayList<String>();
        
        // Add all the <class> specifications.
        classNames.addAll(persistenceUnitInfo.getManagedClassNames());

        // Add all the classes from the <jar> specifications.
        for (URL url : persistenceUnitInfo.getJarFileUrls()) {
            classNames.addAll(PersistenceUnitProcessor.getClassNamesFromURL(url));
        }

        // Add all the classes off the classpath at the persistence unit root url.
        if (! persistenceUnitInfo.excludeUnlistedClasses()) {
            classNames.addAll(PersistenceUnitProcessor.getClassNamesFromURL(persistenceUnitInfo.getPersistenceUnitRootUrl()));
        }
        
        // 5 - Go through all the class names we found and add those classes 
        // that have not yet been added. Be sure to check that the accessor
        // does not already exist since adding an accessor will merge its 
        // contents with an existing accessor and we only want that to happen 
        // in the XML case.
        for (String className : classNames) {
            Class candidateClass = PersistenceUnitProcessor.loadClass(className, m_loader, true, m_project);
            // Bug 227630: Do not process a null class whether it was from a 
            // NPE or a CNF, a warning or exception is thrown in loadClass() 
            if (candidateClass != null) {
                if (PersistenceUnitProcessor.isEntity(candidateClass) && ! m_project.hasEntity(candidateClass)) {
                    m_project.addEntityAccessor(new EntityAccessor(PersistenceUnitProcessor.getEntityAnnotation(candidateClass), candidateClass, m_project));
                } else if (PersistenceUnitProcessor.isEmbeddable(candidateClass) && ! m_project.hasEmbeddable(candidateClass)) {
                    m_project.addEmbeddableAccessor(new EmbeddableAccessor(PersistenceUnitProcessor.getEmbeddableAnnotation(candidateClass), candidateClass, m_project));
                }
            }
            
            // Mapped-superclasses will be discovered automatically.
        }
    }
    
    /**
     * INTERNAL:
     * This method is responsible for figuring out list of mapping files to
     * read into XMLEntityMappings objects and store on the project. Note,
     * the order the files are discovered and read is very important so do
     * not change the order of invocation.
     */
    public void loadMappingFiles(boolean throwExceptionOnFail) {
        // Read all the standard XML mapping files first.
        loadStandardMappingFiles(MetadataHelper.JPA_ORM_FILE);
        
        // Read all the explicitly specified mapping files second.
        loadSpecifiedMappingFiles(throwExceptionOnFail);

        // Read all the standard eclipselink files last (if the user hasn't
        // explicitly excluded them). The eclipselink orm files will be 
        // processed last therefore allowing them to override and merge 
        // metadata where necessary. Note: we want the eclipselink orm
        // metadata to merge into standard jpa files and not vice versa. 
        // Loading them last will ensure this happens.
        Boolean excludeEclipseLinkORM = new Boolean((String) m_project.getPersistenceUnitInfo().getProperties().get(PersistenceUnitProperties.EXCLUDE_ECLIPSELINK_ORM_FILE));
        if (! excludeEclipseLinkORM) {
            loadStandardMappingFiles(MetadataHelper.ECLIPSELINK_ORM_FILE);
        }
    }
    
    /**
     * INTERNAL:
     */
    protected void loadSpecifiedMappingFiles(boolean throwExceptionOnFail) {
        PersistenceUnitInfo puInfo = m_project.getPersistenceUnitInfo();
        
        for (String mappingFileName : puInfo.getMappingFileNames()) {
            try {
                Enumeration<URL> mappingFileURLs = m_loader.getResources(mappingFileName);
                
                if (mappingFileURLs.hasMoreElements()) {
                    URL nextURL = mappingFileURLs.nextElement();

                    if (mappingFileURLs.hasMoreElements()) {
                        handleORMException(ValidationException.nonUniqueMappingFileName(puInfo.getPersistenceUnitName(), mappingFileName), mappingFileName, throwExceptionOnFail);
                    }
                    
                    // Read the document through OX and add it to the project.
                    m_project.addEntityMappings(XMLEntityMappingsReader.read(nextURL, m_loader));
                } else {
                    handleORMException(ValidationException.mappingFileNotFound(puInfo.getPersistenceUnitName(), mappingFileName), mappingFileName, throwExceptionOnFail);
                }
            } catch (IOException e) {
                handleORMException(PersistenceUnitLoadingException.exceptionLoadingORMXML(mappingFileName, e), mappingFileName, throwExceptionOnFail);
            }
        }
    }
    
    /**
     * INTERNAL:
     */
    protected void loadStandardMappingFiles(String ormXMLFile) {
        PersistenceUnitInfo puInfo = m_project.getPersistenceUnitInfo();
        Collection<URL> rootUrls = new HashSet<URL>(puInfo.getJarFileUrls());
        rootUrls.add(puInfo.getPersistenceUnitRootUrl());
        
        for (URL rootURL : rootUrls) {
            logMessage("Searching for default mapping file in " + rootURL);
            URL ormURL = null;
            
            try {
                Archive m_par = null;
                m_par = new ArchiveFactoryImpl().createArchive(rootURL);
                ormURL = m_par.getEntryAsURL(ormXMLFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        
            if (ormURL != null) {
                logMessage("Found a default mapping file at " + ormURL + " for root URL " + rootURL);
                
                try {
                    // Read the document through OX and add it to the project.
                    XMLEntityMappings entityMappings = XMLEntityMappingsReader.read(ormURL, m_loader);
                    entityMappings.setIsEclipseLinkORMFile(ormXMLFile.equals(MetadataHelper.ECLIPSELINK_ORM_FILE));
                    m_project.addEntityMappings(entityMappings);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } 
            }
        }
    }
    
    /**
     * INTERNAL:
     * Log an untranslated message to the EclipseLink log at FINER level.
     * The message should not contain a bundle key.
     */
    protected void logMessage(String message) {
        if (m_session == null) {
            AbstractSessionLog.getLog().log(SessionLog.FINER, message);
        } else {
            m_session.logMessage(message);
        }
    }
    
    /**
     * INTERNAL: 
     * Process the customizer for those entities and embeddables that have one
     * defined. This must be the last thing called on this processor before
     * cleanup.
     */
    public void processCustomizers() {
        for (ClassAccessor classAccessor: m_project.getAccessorsWithCustomizer()) {
            DescriptorCustomizer customizer = (DescriptorCustomizer) MetadataHelper.getClassInstance(classAccessor.getCustomizerClass().getName(), m_loader);
            
            try {
                customizer.customize(classAccessor.getDescriptor().getClassDescriptor());
            } catch (Exception e) {
                // fail silently ...
            }
        }
    }

    /**
     * INTERNAL:
     * Performs the initialization of the persistence unit classes and then
     * processes the xml metadata.
     * Note: Do not change the order of invocation of various methods.
     */
    public void processEntityMappings() {
        // 1 - Process persistence unit meta data/defaults defined in ORM XML 
        // instance documents in the persistence unit. If multiple conflicting 
        // persistence unit meta data is found, this call will throw an 
        // exception. The meta data we find here will be applied in the
        // initialize call below.
        for (XMLEntityMappings entityMappings : m_project.getEntityMappings()) {
            // Since this our first iteration through the entity mappings list,
            // set the project and loader references. This is very important
            // and must be done first!
            entityMappings.setLoader(m_loader);
            entityMappings.setProject(m_project);
            
            // Process the persistence unit metadata if defined.
            entityMappings.processPersistenceUnitMetadata();
        }
        
        // 2 - Initialize all the persistence unit class with the meta data we
        // processed in step 1.
        initPersistenceUnitClasses();

        // 3 - Now process the entity mappings metadata.
        for (XMLEntityMappings entityMappings : m_project.getEntityMappings()) {
            entityMappings.process();
        }
    }

    /**
     * INTERNAL:
     * Process the ORM metadata for all the entities that were added to the
     * project for this persistence unit.
     */
    public void processORMMetadata() {
        // Step 1: Process all the entity accessors that were added to the 
        // project. Mapped superclasses and embeddables are processed/discovered
        // through the entity processing. 
        for (EntityAccessor entity : m_project.getEntityAccessors()) {
            // If the accessor hasn't been processed yet, then process it.
            if (! entity.isProcessed()) {
                // Tell the entity to process itself ...
                entity.process();
                
                // Once it's done, set the flag to processed to avoid multiple
                // processing of the same entity.
                entity.setIsProcessed();
            }
        }
        
        // Step 2: Process any left over metadata we couldn't complete in step 1
        m_project.process();
    }

    /**
     * INTERNAL:
     * Use this method to set the correct class loader that should be used
     * during processing. Currently, the class loader should only change
     * once, from preDeploy to deploy.
     */
    public void setClassLoader(ClassLoader loader) {
        m_loader = loader;
        
        // Update the loader on all the entity mappings for this project.
        for (XMLEntityMappings entityMappings : m_project.getEntityMappings()) {
            entityMappings.setLoader(m_loader);
        }
    }
}
