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

package org.eclipse.persistence.internal.dbws;

// Javase imports
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Java extension imports
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

// TopLink imports
import org.eclipse.persistence.dbws.DBWSModelProject;
import org.eclipse.persistence.exceptions.DBWSException;
import org.eclipse.persistence.internal.dbws.DBWSAdapter;
import org.eclipse.persistence.internal.oxm.XMLConversionManager;
import org.eclipse.persistence.internal.oxm.schema.SchemaModelProject;
import org.eclipse.persistence.internal.oxm.schema.model.ComplexType;
import org.eclipse.persistence.internal.oxm.schema.model.Schema;
import org.eclipse.persistence.internal.sessions.DatabaseSessionImpl;
import org.eclipse.persistence.internal.xr.Invocation;
import org.eclipse.persistence.internal.xr.Operation;
import org.eclipse.persistence.internal.xr.ValueObject;
import org.eclipse.persistence.internal.xr.XRServiceAdapter;
import org.eclipse.persistence.internal.xr.XRServiceFactory;
import org.eclipse.persistence.internal.xr.XRServiceModel;
import org.eclipse.persistence.mappings.AttributeAccessor;
import org.eclipse.persistence.oxm.NamespaceResolver;
import org.eclipse.persistence.oxm.XMLContext;
import org.eclipse.persistence.oxm.XMLDescriptor;
import org.eclipse.persistence.oxm.XMLRoot;
import org.eclipse.persistence.oxm.XMLUnmarshaller;
import org.eclipse.persistence.oxm.mappings.XMLAnyCollectionMapping;
import org.eclipse.persistence.sessions.Project;
import static org.eclipse.persistence.internal.xr.Util.DBWS_SCHEMA_XML;
import static org.eclipse.persistence.internal.xr.Util.DBWS_SERVICE_XML;
import static org.eclipse.persistence.internal.xr.Util.DBWS_WSDL;
import static org.eclipse.persistence.internal.xr.Util.META_INF_PATHS;
import static org.eclipse.persistence.internal.xr.Util.SCHEMA_2_CLASS;
import static org.eclipse.persistence.internal.xr.Util.WEB_INF_DIR;
import static org.eclipse.persistence.internal.xr.Util.WSDL_DIR;
import static org.eclipse.persistence.oxm.mappings.UnmarshalKeepAsElementPolicy.KEEP_UNKNOWN_AS_ELEMENT;


/**
 * <p>
 * <b>INTERNAL:</b> ProviderHelper bridges between {@link DBWSAdapter}'s and JAX-WS {@link Provider}'s
 * <p>
 *
 * @author Mike Norman - michael.norman@oracle.com
 * @since EclipseLink 1.0
 * <pre>
 * packaging required for deployment as a Web Service
 * \--- root of war file
 *      |
 *      \---web-inf
 *          |   webservices.xml
 *          |   <vendor_specific_files: sun-jaxws.xml, weblogic-webservices.xml>
 *          |   web.xml
 *          |
 *          +---classes
 *          |   +---META-INF
 *          |   |    eclipselink-dbws.xml
 *          |   |    eclipselink-dbws-sessions.xml -- name can be overriden by <sessions-file> entry in eclipselink-dbws.xml
 *          |   |    eclipselink-dbws-or.xml
 *          |   |    eclipselink-dbws-ox.xml
 *          |   |
 *          |   +---_dbws
 *          |   |    DBWSProvider.class        -- code-generated javax.xml.ws.Provider
 *          |   |
 *          |   \---foo                        -- optional domain classes
 *          |       \---bar
 *          |             Address.class
 *          |             Employee.class
 *          |             PhoneNumber.class
 *          \---wsdl
 *                 swaref.xsd                  -- optional to handle attachements
 *                 eclipselink-dbws.wsdl
 *                 eclipselink-dbws-schema.xsd
 * </pre>
 */
public class ProviderHelper extends XRServiceFactory {

    public static final String SERVICE_NS_PREFIX = "srvc";
    protected static final String XSL_PREAMBLE =
      "<?xml version=\"1.0\"?> " +
      "<xsl:stylesheet " +
        "xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" " +
        "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
        "> " +
      "<xsl:output method=\"xml\" encoding=\"UTF-8\"/> ";
    protected static final String XSL_POSTSCRIPT = "</xsl:stylesheet>";
    protected static final String MATCH_SCHEMA =
      XSL_PREAMBLE +
        "<xsl:template match=\"/\">" +
             "<xsl:apply-templates/>" +
        "</xsl:template>" +
        "<xsl:template match=\"//xsd:schema\">" +
          "<xsl:copy-of select=\".\"/>" +
        "</xsl:template>" +
      XSL_POSTSCRIPT;
    protected static TransformerFactory tf = TransformerFactory.newInstance();
    protected SOAPResponseWriter responseWriter;

    // Default constructor required by servlet/jax-ws spec
    public ProviderHelper() {
        super();
    }

    @SuppressWarnings("unchecked")
    public void init() {
        parentClassLoader = Thread.currentThread().getContextClassLoader();
        InputStream xrServiceStream = null;
        for (String searchPath : META_INF_PATHS) {
            String path = searchPath + DBWS_SERVICE_XML;
            xrServiceStream = parentClassLoader.getResourceAsStream(path);
            if (xrServiceStream != null) {
                break;
            }
        }
        if (xrServiceStream == null) {
            throw DBWSException.couldNotLocateFile(DBWS_SERVICE_XML);
        }
        DBWSModelProject xrServiceModelProject = new DBWSModelProject();
        XMLContext xmlContext = new XMLContext(xrServiceModelProject);
        XMLUnmarshaller unmarshaller = xmlContext.createUnmarshaller();
        XRServiceModel xrServiceModel = (XRServiceModel)unmarshaller.unmarshal(xrServiceStream);
        try {
            xrServiceStream.close();
        }
        catch (IOException ioe) {
            /* ignore */
        }

        String path = WEB_INF_DIR + WSDL_DIR + DBWS_SCHEMA_XML;
        xrSchemaStream = parentClassLoader.getResourceAsStream(path);
        if (xrSchemaStream == null) {
            throw DBWSException.couldNotLocateFile(DBWS_SCHEMA_XML);
        }
        buildService(xrServiceModel); // inherit xrService processing from XRServiceFactory

        // the xrService built by 'buildService' above is overridden to produce an
        // instance of DBWSAdapter (a sub-class of XRService)
        DBWSAdapter dbwsAdapter = (DBWSAdapter)xrService;

        // get extended schema from WSDL - has additional types for the operations
        StringWriter sw = new StringWriter();
        path = WEB_INF_DIR + WSDL_DIR + DBWS_WSDL;
        InputStream wsdlInputStream = parentClassLoader.getResourceAsStream(path);
        if (wsdlInputStream == null) {
            throw DBWSException.couldNotLocateFile(DBWS_WSDL);
        }
        try {
            StreamSource wsdlStreamSource = new StreamSource(wsdlInputStream);
			Transformer t = tf.newTransformer(new StreamSource(new StringReader(MATCH_SCHEMA)));
			StreamResult streamResult = new StreamResult(sw);
			t.transform(wsdlStreamSource, streamResult);
			sw.toString();
			wsdlInputStream.close();
		}
        catch (Exception e) {
			// e.printStackTrace();
		}

        SchemaModelProject schemaProject = new SchemaModelProject();
        XMLContext xmlContext2 = new XMLContext(schemaProject);
        unmarshaller = xmlContext2.createUnmarshaller();
        Schema extendedSchema = (Schema)unmarshaller.unmarshal(new StringReader(sw.toString()));
        dbwsAdapter.setExtendedSchema(extendedSchema);
        Project oxProject = dbwsAdapter.getOXSession().getProject();
        for (Iterator i = oxProject.getDescriptors().values().iterator(); i.hasNext();) {
          XMLDescriptor d = (XMLDescriptor)i.next();
          NamespaceResolver ns = d.getNamespaceResolver();
          String tns = dbwsAdapter.getExtendedSchema().getTargetNamespace();
          if (ns != null) {
            ns.put(SERVICE_NS_PREFIX, tns);
          }
          String defaultRootElement = d.getDefaultRootElement();
          if (defaultRootElement != null ) {
              int idx = defaultRootElement.indexOf(':');
              if (idx > 0) {
                  defaultRootElement = defaultRootElement.substring(idx+1);
              }
              d.addRootElement(SERVICE_NS_PREFIX + ":" + defaultRootElement);
          }
        }
        XMLDescriptor invocationDescriptor = new XMLDescriptor();
        invocationDescriptor.setJavaClass(Invocation.class);
        NamespaceResolver ns = new NamespaceResolver();
        invocationDescriptor.setNamespaceResolver(ns);
        ns.put(SERVICE_NS_PREFIX, dbwsAdapter.getExtendedSchema().getTargetNamespace());
        XMLAnyCollectionMapping parametersMapping = new XMLAnyCollectionMapping();
        parametersMapping.setAttributeName("parameters");
        parametersMapping.setAttributeAccessor(new AttributeAccessor() {
            Project oxProject;
            @Override
            public Object getAttributeValueFromObject(Object object) {
              return ((Invocation)object).getParameters();
            }
            @Override
            public void setAttributeValueInObject(Object object, Object value) {
                Invocation invocation = (Invocation)object;
                Vector values = (Vector)value;
                for (Iterator i = values.iterator(); i.hasNext();) {
                  /* scan through values:
                   *  if XML conforms to something mapped, it an object; else it is a DOM Element
                   *  (probably a scalar). Walk through operations for the types, converting
                   *   as required. The 'key' is the local name of the element - for mapped objects,
                   *   have to get the root element name from the descriptor for the object
                   */
                  Object o = i.next();
                  if (o instanceof Element) {
                    Element e = (Element)o;
                    String key = e.getLocalName();
                    if ("theInstance".equals(key)) {
                        NodeList nl = e.getChildNodes();
                        for (int j = 0; j < nl.getLength(); j++) {
                            Node n = nl.item(j);
                            if (n.getNodeType() == Node.ELEMENT_NODE) {
                                Object theInstance = new XMLContext(oxProject).createUnmarshaller().unmarshal(n);
                                if (theInstance instanceof XMLRoot) {
                                    theInstance = ((XMLRoot)theInstance).getObject();
                                }
                                invocation.setParameter(key, theInstance);
                                break;
                            }
                        }
                    }
                    else {
                        String val = e.getTextContent();
                        invocation.setParameter(key, val);
                    }
                  }
                  else {
                    XMLDescriptor descriptor = (XMLDescriptor)oxProject.getDescriptor(o.getClass());
                    String key = descriptor.getDefaultRootElement();
                    int idx = key.indexOf(':');
                    if (idx != -1) {
                      key = key.substring(idx+1);
                    }
                    invocation.setParameter(key, o);
                  }
                }
            }
            public AttributeAccessor setProject(Project oxProject) {
              this.oxProject = oxProject;
              return this;
            }
        }.setProject(oxProject));
        parametersMapping.setKeepAsElementPolicy(KEEP_UNKNOWN_AS_ELEMENT);
        invocationDescriptor.addMapping(parametersMapping);
        oxProject.addDescriptor(invocationDescriptor);
        ((DatabaseSessionImpl)dbwsAdapter.getOXSession()).initializeDescriptorIfSessionAlive(invocationDescriptor);
        dbwsAdapter.getXMLContext().storeXMLDescriptorByQName(invocationDescriptor);

        // create SOAP message response handler
        responseWriter = new SOAPResponseWriter(dbwsAdapter);
        responseWriter.initialize();
    }

    @SuppressWarnings("unchecked")
    public SOAPMessage invoke(SOAPMessage request) {

        SOAPMessage response = null;
        DBWSAdapter dbwsAdapter = (DBWSAdapter)xrService;
        try {
            SOAPElement body = getSOAPBodyElement(request);
            if (body == null) {
                throw new IllegalArgumentException("unknown SOAPMessage request format");
            }
            XMLContext xmlContext = dbwsAdapter.getXMLContext();
            XMLRoot xmlRoot = (XMLRoot)xmlContext.createUnmarshaller().unmarshal(body,
              Invocation.class);
            Invocation invocation = (Invocation)xmlRoot.getObject();
            invocation.setName(xmlRoot.getLocalName());
            Operation op = dbwsAdapter.getOperation(invocation.getName());
            /*
             * Fix up types for arguments - scan the extended schema for the operation's Request type.
             *
             * For most parameters, the textual node content is fine, but for date/time and
             * binary objects, we must convert
             */
            org.eclipse.persistence.internal.oxm.schema.model.Element invocationElement =
              (org.eclipse.persistence.internal.oxm.schema.model.Element)
               dbwsAdapter.getExtendedSchema().getTopLevelElements().get(invocation.getName());
            String typeName = invocationElement.getType();
            int idx = typeName.indexOf(':');
            if (idx != -1) {
              // strip-off any namespace prefix
              typeName = typeName.substring(idx+1);
            }
            ComplexType complexType =
              (ComplexType)dbwsAdapter.getExtendedSchema().getTopLevelComplexTypes().get(typeName);
            if (complexType.getSequence() != null) {
                // for each operation, there is a corresponding top-level Request type
                // which has the arguments to the operation
                for (Iterator i = complexType.getSequence().getOrderedElements().iterator(); i .hasNext();) {
                    org.eclipse.persistence.internal.oxm.schema.model.Element e =
                    (org.eclipse.persistence.internal.oxm.schema.model.Element)i.next();
                  String argName = e.getName();
                  Object argValue = invocation.getParameter(argName);
                  String argType = e.getType();
                  if (argType != null) {
                     String argTypePrefix = null;
                     String nameSpaceURI = null;
                     idx = argType.indexOf(':');
                     if (idx != -1) {
                       argTypePrefix = argType.substring(0,idx);
                       argType = argType.substring(idx+1);
                       nameSpaceURI =
                         dbwsAdapter.getSchema().getNamespaceResolver().resolveNamespacePrefix(argTypePrefix);
                     }
                     QName argQName = argTypePrefix == null ? new QName(nameSpaceURI, argType) :
                         new QName(nameSpaceURI, argType, argTypePrefix);
                     Class clz = SCHEMA_2_CLASS.get(argQName);
                     if (clz != null) {
                       argValue = ((XMLConversionManager)dbwsAdapter.getOXSession().getDatasourcePlatform().
                         getConversionManager()).convertObject(argValue, clz, argQName);
                       invocation.setParameter(argName, argValue);
                     }
                  }
                  // incoming attachments ?
                }
            }
            Object result = op.invoke(dbwsAdapter, invocation);
            if (result instanceof ValueObject) {
                result = ((ValueObject)result).value;
            }
            response = responseWriter.generateResponse(op, result);
        }
        catch (Exception e) {
          //e.printStackTrace();
          throw new RuntimeException("something went wrong parsing SOAPMessage", e);
        }
        return response;
    }

    public void destroy() {
        logoutSessions();
        responseWriter = null;
        try {
            xrSchemaStream.close();
        }
        catch (IOException ioe) {
            /* ignore */
        }
        xrSchemaStream = null;
        parentClassLoader = null;
        xrService.setXMLContext(null);
        xrService = null;
    }

    @Override
    public XRServiceAdapter buildService(XRServiceModel xrServiceModel) {
        xrService = new DBWSAdapter(); // use subclass to hold extended WSDL schema
        DBWSAdapter dbws = (DBWSAdapter)xrService;
        dbws.setName(xrServiceModel.getName());
        dbws.setSessionsFile(xrServiceModel.getSessionsFile());
        dbws.setOperations(xrServiceModel.getOperations());
        initializeService(parentClassLoader, xrSchemaStream);
        return dbws;
    }

    public static SOAPElement getSOAPBodyElement(SOAPMessage message) throws SOAPException {
        NodeList nodes = message.getSOAPPart().getEnvelope().getBody().getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof SOAPBodyElement) {
                return (SOAPElement)node;
            }
        }
        return null;
    }
}