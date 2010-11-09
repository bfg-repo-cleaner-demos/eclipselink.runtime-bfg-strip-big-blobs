/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * dmccann - June 17/2009 - 2.0 - Initial implementation
 ******************************************************************************/
package org.eclipse.persistence.testing.jaxb.externalizedmetadata;

import org.eclipse.persistence.testing.jaxb.externalizedmetadata.exceptions.ExceptionHandlingTestSuite;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.exceptions.contextfactory.ExceptionHandlingTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.jaxbcontextfactory.JAXBContextFactoryTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.MappingsTestSuite;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.anyattribute.AnyAttributeMappingTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.anyobject.AnyObjectMappingTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.composite.CompositeMappingTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.compositecollection.CompositeCollecitonMappingTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.direct.DirectMappingTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.mappings.directcollection.DirectCollectionMappingTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlaccessororder.XmlAccessorOrderTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlaccessortype.XmlAccessorTypeTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmladapter.XmlAdapterTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmladapter.negative.XmlAdapterNegativeTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmladapter.unmappable.ClassLevelTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmladapter.unmappable.PackageLevelTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmladapter.unmappable.PropertyLevelTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlanyattribute.XmlAnyAttributeTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlanyelement.XmlAnyElementTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlattachmentref.XmlAttachmentRefCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlattribute.XmlAttributeTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlclassextractor.XmlClassExtractorTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlcustomizer.XmlCustomizerTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmldiscriminator.XmlDiscriminatorTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlelement.XmlElementTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlelementref.XmlElementRefTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlelementrefs.XmlElementRefsTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlelements.XmlElementsTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlelementwrapper.XmlElementWrapperTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlenum.XmlEnumTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlidref.XmlIdRefTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlinlinebinarydata.XmlInlineBinaryDataTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmljoinnode.XmlJoinNodeTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmllist.XmlListTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlmetadatacomplete.XmlMetadataCompleteTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlmimetype.XmlMimeTypeCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlmixed.XmlMixedTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlregistry.XmlRegistryTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlrootelement.XmlRootElementTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlschema.XmlSchemaTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlschema.namespace.NamespaceTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlschematype.XmlSchemaTypeTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlschematypes.XmlSchemaTypesTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlseealso.XmlSeeAlsoTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmltransient.XmlTransientTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmltype.XmlTypeTestCases;
import org.eclipse.persistence.testing.jaxb.externalizedmetadata.xmlvalue.XmlValueTestCases;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Suite for testing eclipselink-oxm.xml processing.
 *
 */
public class ExternalizedMetadataTestSuite extends TestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite("Externalized Metadata Test Suite");
        suite.addTest(MappingsTestSuite.suite());
        suite.addTest(ExceptionHandlingTestSuite.suite());
        suite.addTestSuite(XmlTransientTestCases.class);
        suite.addTestSuite(XmlSeeAlsoTestCases.class);
        suite.addTestSuite(XmlSchemaTestCases.class);
        suite.addTestSuite(NamespaceTestCases.class);
        suite.addTestSuite(XmlRootElementTestCases.class);
        suite.addTestSuite(XmlTypeTestCases.class);
        suite.addTestSuite(XmlAccessorTypeTestCases.class);
        suite.addTestSuite(XmlAccessorOrderTestCases.class);
        suite.addTestSuite(JAXBContextFactoryTestCases.class);
        suite.addTestSuite(XmlElementTestCases.class);
        suite.addTestSuite(XmlAdapterTestCases.class);
        suite.addTestSuite(XmlAdapterNegativeTestCases.class);
        suite.addTestSuite(ClassLevelTestCases.class);
        suite.addTestSuite(PackageLevelTestCases.class);
        suite.addTestSuite(PropertyLevelTestCases.class);
        suite.addTestSuite(XmlAttributeTestCases.class);
        suite.addTestSuite(XmlCustomizerTestCases.class);
        suite.addTestSuite(XmlElementWrapperTestCases.class);
        suite.addTestSuite(XmlValueTestCases.class);
        suite.addTestSuite(XmlListTestCases.class);
        suite.addTestSuite(XmlAnyElementTestCases.class);
        suite.addTestSuite(XmlIdRefTestCases.class);
        suite.addTestSuite(XmlMixedTestCases.class);
        suite.addTestSuite(XmlAnyAttributeTestCases.class);
        suite.addTestSuite(XmlMimeTypeCases.class);
        suite.addTestSuite(XmlAttachmentRefCases.class);
        suite.addTestSuite(XmlElementsTestCases.class);
        suite.addTestSuite(XmlElementRefTestCases.class);
        suite.addTestSuite(XmlElementRefsTestCases.class);
        suite.addTestSuite(XmlSchemaTypeTestCases.class);
        suite.addTestSuite(XmlSchemaTypesTestCases.class);
        suite.addTestSuite(XmlEnumTestCases.class);
        suite.addTestSuite(XmlInlineBinaryDataTestCases.class);
        suite.addTestSuite(XmlRegistryTestCases.class);
        suite.addTestSuite(XmlClassExtractorTestCases.class);
        suite.addTestSuite(XmlDiscriminatorTestCases.class);
        suite.addTestSuite(XmlJoinNodeTestCases.class);
        suite.addTestSuite(XmlMetadataCompleteTestCases.class);
        return suite;
    }
    
    public static void main(String[] args) {
        String[] arguments = { "-c", "org.eclipse.persistence.testing.jaxb.externalizedmetadata.ExternalizedMetadataTestSuite" };
        junit.textui.TestRunner.main(arguments);
    }
}
