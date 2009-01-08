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
 ******************************************************************************/  
package org.eclipse.persistence.exceptions.i18n;

import java.util.ListResourceBundle;

/**
 * INTERNAL:
 * <b>Purpose:</b><p>English ResourceBundle for SDOException.</p>
 */
public class SDOExceptionResource extends ListResourceBundle {
    static final Object[][] contents = {
                                           { "45000", "Could not find ID element of type ID on type with uri [{0}] and name [{1}] ." },
                                           { "45001", "An error occurred processing the import with schemaLocation [{0}] and namespace [{1}] ." },
                                           { "45002", "An error occurred processing the include with schemaLocation [{0}] ." },
                                           { "45003", "A referenced property with uri [{0}] and name [{1}] was not found." },
                                           { "45004", "Old sequence was not found in the changesummary." },
                                           { "45005", "Value of property named [{0}] must be a DataObject" },                                           
                                           { "45006", "Sequence cannot be null when type.isSequenced() is true." },                         
                                           { "45007", "A type was not set on the property with name [{0}]" }, 
                                           { "45008", "An IOException occurred." }, 
                                           { "45009", "Type not found with uri [{0}] and name [{1}]" },                                          
                                           { "45010", "Type not found for interface [{0}]" },
                                           { "45011", "Could not create a DataObject for type with uri [{0}] and name [{1}] because type.isAbstract() returns true." },                                           
                                           { "45012", "Could not create a DataObject for interface [{0}], trying to create a DataObject for type with uri [{1}] and name [{2}] " },                                           
                                           { "45013", "Cannot look up app info for null argument." },                                                                                   
                                           { "45014", "Could not define type.  Types can only be defined for DataObjects with Type set to commonj.sdo.Type" },                                                                                   
                                           { "45015", "Could not define type with a null name." },                                                                                
                                           { "45016", "A modified object in the changesummary XML is missing a ref attribute or the value is not specified." }, 
                                           { "45017", "An error occurred processing xpath [{0}] ." },
                                           { "45018", "Adding a duplicate entry for the complex single setting [{1}] into a sequence at position [{0}] is not supported." },
                                           { "45019", "Adding an attribute property [{0}] to a sequence is not supported." },
                                           { "45020", "No sequence found for path [{0}]." },
                                           { "45021", "An error occurred attempting to instantiate a Sequence object with a null dataObject instance field." },
                                           { "45022", "No sequence is supported for property [{0}]." },
                                           { "45023", "Could not set property of type with uri [{0}] and name [{1}] to value of class [{2}]" },                                                                                      
                                           { "45024", "A conversion error occurred." },                                                                                      
                                           { "45025", "Could not find property at index [{0}]" },                                                                                      
                                           { "45026", "Cannot perform operation [{0}] on null parameter." },
                                           { "45027", "Cannot find class for type with uri [{0}] and name [{1}]." },
                                           { "45028", "A type can not be set to open and datatype.  Error with type with uri [{0}] and name [{1}]." },
                                           { "45029", "Invalid index [{0}] passed to method [{1}]." },
                                           { "45030", "An error occurred invoking the constructor with a String argument on the class [{0}]." },
                                           { "45031", "Cannot set invalid target type [{0}] on property [{1}] because target type.dataType is true." },                                           
                                           { "45032", "An XMLMarshalException occurred for uri [{1}] and local name [{2}].  Exception: [{0}]" },                                         
                                           { "45033", "An error occurred generating types. The XML Schema component with name [{1}] and namespace URI [{0}] is referenced but never defined.  An import or include for namespace URI[{0}] might be missing from the XML Schema." },
                                           { "45034", "The value of the options parameter must be a DataObject of Type with uri [{0}] and name [{1}]." },
                                           { "45035", "The value that corresponds to the 'type' property must be a Type object." },
                                           { "45036", "A global property corresponding to the XML node being loaded was not found." },                                           
                                           { "45037", "The prefix [{0}] is used but not declared in the XML schema." },                                                                          
                                           { "45038", "Cannot perform operation on property [{0}] because it is not reachable from the path [{1}]. The path is invalid, or one or more Data Objects on the path are null." }, 
                                           { "45039", "An error occurred accessing the externalizableDelegator field [{0}] on the DataObject." }
    };

    /**
     * Return the lookup table.
     */
    protected Object[][] getContents() {
        return contents;
    }
}