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
 *     Mike Norman - May 26 2008, creating packager for JAX-WS RI
 ******************************************************************************/

 package org.eclipse.persistence.tools.dbws;

// javase imports
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
// EclipseLink imports
import org.eclipse.persistence.internal.dbws.ProviderHelper;
import org.eclipse.persistence.internal.libraries.asm.ClassWriter;
import org.eclipse.persistence.internal.libraries.asm.CodeVisitor;
import org.eclipse.persistence.internal.libraries.asm.Label;
import org.eclipse.persistence.internal.libraries.asm.Type;
import org.eclipse.persistence.internal.libraries.asm.attrs.Annotation;
import org.eclipse.persistence.internal.libraries.asm.attrs.RuntimeVisibleAnnotations;
import org.eclipse.persistence.internal.libraries.asm.attrs.SignatureAttribute;
import org.eclipse.persistence.tools.dbws.DBWSBuilder;
import org.eclipse.persistence.tools.dbws.DBWSPackager;
import org.eclipse.persistence.tools.dbws.SimpleFilesPackager;
import static org.eclipse.persistence.internal.libraries.asm.Constants.AASTORE;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_BRIDGE;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_ENUM;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_FINAL;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_PRIVATE;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_PUBLIC;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_STATIC;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_SUPER;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACC_SYNTHETIC;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ACONST_NULL;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ALOAD;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ANEWARRAY;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ARETURN;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ASTORE;
import static org.eclipse.persistence.internal.libraries.asm.Constants.CHECKCAST;
import static org.eclipse.persistence.internal.libraries.asm.Constants.DUP;
import static org.eclipse.persistence.internal.libraries.asm.Constants.GOTO;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ICONST_0;
import static org.eclipse.persistence.internal.libraries.asm.Constants.ICONST_1;
import static org.eclipse.persistence.internal.libraries.asm.Constants.INVOKESPECIAL;
import static org.eclipse.persistence.internal.libraries.asm.Constants.INVOKESTATIC;
import static org.eclipse.persistence.internal.libraries.asm.Constants.INVOKEVIRTUAL;
import static org.eclipse.persistence.internal.libraries.asm.Constants.RETURN;
import static org.eclipse.persistence.internal.libraries.asm.Constants.V1_5;
import static org.eclipse.persistence.internal.xr.Util.DBWS_WSDL;
import static org.eclipse.persistence.internal.xr.Util.WEB_INF_DIR;
import static org.eclipse.persistence.internal.xr.Util.WSDL_DIR;
import static org.eclipse.persistence.tools.dbws.Util.DBWS_PROVIDER_NAME;
import static org.eclipse.persistence.tools.dbws.Util.DBWS_PROVIDER_CLASS_FILE;
import static org.eclipse.persistence.tools.dbws.Util.DBWS_PROVIDER_PACKAGE;
import static org.eclipse.persistence.tools.dbws.Util.DBWS_PROVIDER_SOURCE_FILE;
import static org.eclipse.persistence.tools.dbws.Util.WEB_XML_FILENAME;
import static org.eclipse.persistence.tools.dbws.Util.WEBSERVICES_FILENAME;

/**
 * <p>
 * <b>INTERNAL:</b> WebFilesPackager implements the {@link DBWSPackager} interface.
 * The output files from the {@link DBWSBuilder} are written to the <tt>stageDir</tt>
 * 'flat' with no directory structure.
 * <p>
 *
 * @author Mike Norman - michael.norman@oracle.com
 * @since EclipseLink 1.x
 * <pre>
 * \--- <b>stageDir</b> root directory
 *    |   <b>eclipselink-dbws-schema.xsd</b>
 *    |   swaref.xsd                  -- optional if attachements are enabled
 *    |   <b>eclipselink-dbws-or.xml</b>
 *    |   <b>eclipselink-dbws-ox.xml</b>
 *    |   <b>eclipselink-dbws-sessions.xml</b>
 *    |   <b>eclipselink-dbws.xml</b>
 *    |   <u>web.xml</u>
 *    |   <u>webservices.xml</u>
 *    |   <u>DBWSProvider.class</u>
 *    |   <u>DBWSProvider.java</u>    -- code-generated javax.xml.ws.Provider
 *    |   <b>eclipselink-dbws.wsdl</b>
 *
 * </pre>
 */
public class WebFilesPackager extends SimpleFilesPackager {

    public static final String WEBSERVICES_PREAMBLE =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
        "<webservices \n" +
          "  xmlns=\"http://java.sun.com/xml/ns/javaee\" \n" +
          "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
          "  xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/javaee_web_services_1_2.xsd\" \n" +
          "  version=\"1.2\" \n" +
          "  > \n" +
          "  <webservice-description> \n" +
            "  <webservice-description-name>";
                           // serviceName ^^ here
    public static final String WEBSERVICES_PORT_COMPONENT_NAME =
                                             "</webservice-description-name> \n" +
            "    <port-component> \n" +
            "      <port-component-name>";
    // dotted-format serviceName.portName ^^ here
    public static final String WEBSERVICES_WSDL_SERVICE =
                                             "</port-component-name> \n" +
            "      <wsdl-service xmlns:ns0=\"";
                          // service URI ^^ here
    public static final String WEBSERVICES_WSDL_PORT =
                                             "</wsdl-service> \n" +
            "      <wsdl-port xmlns:ns1=\"";
    public static final String WEBSERVICES_SUFFIX =
                                             "</wsdl-port> \n" +
            "      <service-impl-bean> \n" +
            "        <servlet-link>DBWSProvider</servlet-link> \n" +
            "      </service-impl-bean> \n" +
            "    </port-component> \n" +
            "  </webservice-description> \n" +
            "</webservices>";
    public static final String DBWS_PROVIDER_SOURCE_PREAMBLE =
        "package _dbws;\n" +
        "\n" +
        "import javax.annotation.PostConstruct;\n" +
        "import javax.annotation.PreDestroy;\n" +
        "import javax.servlet.ServletContext;\n" +
        "import javax.xml.soap.SOAPMessage;\n" +
        "import javax.xml.ws.Provider;\n" +
        "import javax.xml.ws.ServiceMode;\n" +
        "import javax.xml.ws.WebServiceProvider;\n" +
        "import static javax.xml.ws.Service.Mode.MESSAGE;\n" +
        "import " + ProviderHelper.class.getName() + ";\n" +
        "\n" +
        "@WebServiceProvider(\n" +
        "    wsdlLocation = \"" + WEB_INF_DIR + WSDL_DIR + DBWS_WSDL + "\",\n" +
        "    serviceName = \"";
    public static final String DBWS_PROVIDER_SOURCE_PORT_NAME =
        "\",\n    portName = \"";
    public static final String DBWS_PROVIDER_SOURCE_TARGET_NAMESPACE =
        "\",\n    targetNamespace = \"";
    public static final String DBWS_PROVIDER_SOURCE_SUFFIX =
        "\"\n)\n@ServiceMode(MESSAGE)\n" +
        "public class DBWSProvider extends ProviderHelper implements Provider<SOAPMessage> {\n" +
        "    public  DBWSProvider() {\n" +
        "        super();\n" +
        "    }\n" +
        "    private static final String CONTAINER_RESOLVER_CLASSNAME =\n" +
        "        \"com.sun.xml.ws.api.server.ContainerResolver\";\n" +
        "    @PostConstruct\n" +
        "    public void init() {\n" +
        "        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();\n" +
        "        ServletContext sc = null;\n" +
        "        //ServletContext sc = ContainerResolver.getInstance().getContainer().getSPI(ServletContext.class);\n" +
        "        try {\n" +
        "            Class<?> containerResolverClass = parentClassLoader.loadClass(\n" +
        "                CONTAINER_RESOLVER_CLASSNAME);\n" +
        "            Method getInstanceMethod = containerResolverClass.getMethod(\"getInstance\");\n" +
        "            Object containerResolver = getInstanceMethod.invoke(null);\n" +
        "            Method getContainerMethod = containerResolver.getClass().getMethod(\"getContainer\");\n" +
        "            getContainerMethod.setAccessible(true);\n" +
        "            Object container = getContainerMethod.invoke(containerResolver);\n" +
        "            Method getSPIMethod = container.getClass().getMethod(\"getSPI\", Class.class);\n" +
        "            getSPIMethod.setAccessible(true);\n" +
        "            sc = (ServletContext)getSPIMethod.invoke(container, ServletContext.class);\n" +
        "            super.init(parentClassLoader, sc);\n" +
        "        }\n" +
        "        catch (Exception e) {\n" +
        "            // e.printStackTrace();\n" +
        "        }\n" +
        "    }\n" +
        "    @Override\n" +
        "    public SOAPMessage invoke(SOAPMessage request) {\n" +
        "        return super.invoke(request);\n" +
        "    }\n" +
        "    @Override\n" +
        "    @PreDestroy\n" +
        "    public void destroy() {\n" +
        "        super.destroy();\n" +
        "    }\n" +
        "};";
    public static final String ASMIFIED_DBWS_PROVIDER_HELPER =
    	ProviderHelper.class.getName().replace('.', '/');
    public static final String ASMIFIED_JAX_WS_PROVIDER =
        "javax/xml/ws/Provider";
    public static final String ASMIFIED_JAX_WS_WEB_SERVICE_PROVIDER =
        "javax/xml/ws/WebServiceProvider";
    public static final String ASMIFIED_JAX_WS_SERVICE_MODE =
        "javax/xml/ws/ServiceMode";
    public static final String ASMIFIED_JSR_250_POSTCONSTRUCT =
        "javax/annotation/PostConstruct";
    public static final String ASMIFIED_JSR_250_PREDESTROY =
        "javax/annotation/PreDestroy";
    public static final String ASMIFIED_JAX_WS_SERVICE =
        "javax/xml/ws/Service";
    public static final String ASMIFIED_SOAP_MESSAGE =
        "javax/xml/soap/SOAPMessage";

	public WebFilesPackager() {
        super();
    }
    public WebFilesPackager(boolean useArchiver) {
        super(useArchiver);
    }
    public WebFilesPackager(boolean useArchiver, String name) {
        super();
        setArchiver(useArchiver
            ? (name == null
                ? new WarArchiver(this)
                : new WarArchiver(this, name + ".war"))
            : null);
    }

    @Override
	public OutputStream getWebXmlStream() throws FileNotFoundException {
        return new FileOutputStream(new File(stageDir, WEB_XML_FILENAME));
	}

	@Override
	public OutputStream getWSDLStream() throws FileNotFoundException {
        return new FileOutputStream(new File(stageDir, DBWS_WSDL));
	}

	@Override
    public OutputStream getWebservicesXmlStream() throws FileNotFoundException {
        return new FileOutputStream(new File(stageDir, WEBSERVICES_FILENAME));
    }
    @Override
	public void writeWebservicesXML(OutputStream webservicesXmlStream, DBWSBuilder builder) {
		StringBuilder sb = new StringBuilder(WEBSERVICES_PREAMBLE);
		String serviceName = builder.getWSDLGenerator().getServiceName();
		String serviceNameSpace = builder.getWSDLGenerator().getServiceNameSpace();
		sb.append(serviceName);
		sb.append(WEBSERVICES_PORT_COMPONENT_NAME);
		sb.append(serviceName + "." + serviceName);
		sb.append(WEBSERVICES_WSDL_SERVICE);
		sb.append(serviceNameSpace);
		sb.append("\">ns0:");
		sb.append(serviceName);
		sb.append(WEBSERVICES_WSDL_PORT);
		sb.append(serviceNameSpace);
		sb.append("\">ns1:");
		sb.append(serviceName);
		sb.append(WEBSERVICES_SUFFIX);
		OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(webservicesXmlStream));
		try {
		    osw.write(sb.toString());
		    osw.flush();
		}
		catch (IOException e) {/* ignore */}
	}

    @Override
    public OutputStream getCodeGenProviderStream() throws FileNotFoundException {
        return new FileOutputStream(new File(stageDir, DBWS_PROVIDER_CLASS_FILE));
    }
    @Override
    @SuppressWarnings("unchecked")
    public void writeDBWSProviderClass(OutputStream codeGenProviderStream, DBWSBuilder builder) {
    	String serviceName = builder.getWSDLGenerator().getServiceName();
        ClassWriter cw = new ClassWriter(true);
        CodeVisitor cv;

        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, DBWS_PROVIDER_PACKAGE + "/" + DBWS_PROVIDER_NAME,
            ASMIFIED_DBWS_PROVIDER_HELPER, new String[]{ASMIFIED_JAX_WS_PROVIDER}, null);
        cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "CONTAINER_RESOLVER_CLASSNAME",
            "Ljava/lang/String;", "com.sun.xml.ws.api.server.ContainerResolver", null);
        cw.visitInnerClass(ASMIFIED_JAX_WS_SERVICE + "$Mode", ASMIFIED_JAX_WS_SERVICE,
            "Mode", ACC_PUBLIC + ACC_FINAL + ACC_STATIC + ACC_ENUM);

        cv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        cv.visitVarInsn(ALOAD, 0);
        cv.visitMethodInsn(INVOKESPECIAL, ASMIFIED_DBWS_PROVIDER_HELPER, "<init>", "()V");
        cv.visitInsn(RETURN);
        cv.visitMaxs(0, 0);

        // METHOD ATTRIBUTES
        RuntimeVisibleAnnotations methodAttrs0 = new RuntimeVisibleAnnotations();
        Annotation methodAttrs1ann0 = new Annotation("L" + ASMIFIED_JSR_250_POSTCONSTRUCT + ";");
        methodAttrs0.annotations.add(methodAttrs1ann0);

        cv = cw.visitMethod(ACC_PUBLIC, "init", "()V", null, methodAttrs0);
        cv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread",
            "()Ljava/lang/Thread;");
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "getContextClassLoader",
            "()Ljava/lang/ClassLoader;");
        cv.visitVarInsn(ASTORE, 1);
        cv.visitInsn(ACONST_NULL);
        cv.visitVarInsn(ASTORE, 2);
        Label l0 = new Label();
        cv.visitLabel(l0);
        cv.visitVarInsn(ALOAD, 1);
        cv.visitLdcInsn("com.sun.xml.ws.api.server.ContainerResolver");
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass",
            "(Ljava/lang/String;)Ljava/lang/Class;");
        cv.visitVarInsn(ASTORE, 3);
        Label l1 = new Label();
        cv.visitLabel(l1);
        cv.visitVarInsn(ALOAD, 3);
        cv.visitLdcInsn("getInstance");
        cv.visitInsn(ICONST_0);
        cv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod",
            "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
        cv.visitVarInsn(ASTORE, 4);
        cv.visitVarInsn(ALOAD, 4);
        cv.visitInsn(ACONST_NULL);
        cv.visitInsn(ICONST_0);
        cv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
        cv.visitVarInsn(ASTORE, 5);
        cv.visitVarInsn(ALOAD, 5);
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        cv.visitLdcInsn("getContainer");
        cv.visitInsn(ICONST_0);
        cv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod",
            "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
        cv.visitVarInsn(ASTORE, 6);
        cv.visitVarInsn(ALOAD, 6);
        cv.visitInsn(ICONST_1);
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V");
        cv.visitVarInsn(ALOAD, 6);
        cv.visitVarInsn(ALOAD, 5);
        cv.visitInsn(ICONST_0);
        cv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
        cv.visitVarInsn(ASTORE, 7);
        cv.visitVarInsn(ALOAD, 7);
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        cv.visitLdcInsn("getSPI");
        cv.visitInsn(ICONST_1);
        cv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
        cv.visitInsn(DUP);
        cv.visitInsn(ICONST_0);
        cv.visitLdcInsn(Type.getType("Ljava/lang/Class;"));
        cv.visitInsn(AASTORE);
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getMethod",
            "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");
        cv.visitVarInsn(ASTORE, 8);
        cv.visitVarInsn(ALOAD, 8);
        cv.visitInsn(ICONST_1);
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "setAccessible", "(Z)V");
        cv.visitVarInsn(ALOAD, 8);
        cv.visitVarInsn(ALOAD, 7);
        cv.visitInsn(ICONST_1);
        cv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        cv.visitInsn(DUP);
        cv.visitInsn(ICONST_0);
        cv.visitLdcInsn(Type.getType("Ljavax/servlet/ServletContext;"));
        cv.visitInsn(AASTORE);
        cv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
        cv.visitTypeInsn(CHECKCAST, "javax/servlet/ServletContext");
        cv.visitVarInsn(ASTORE, 2);
        cv.visitVarInsn(ALOAD, 0);
        cv.visitVarInsn(ALOAD, 1);
        cv.visitVarInsn(ALOAD, 2);
        cv.visitMethodInsn(INVOKESPECIAL, ASMIFIED_DBWS_PROVIDER_HELPER,
            "init", "(Ljava/lang/ClassLoader;Ljavax/servlet/ServletContext;)V");
        Label l2 = new Label();
        cv.visitLabel(l2);
        Label l3 = new Label();
        cv.visitJumpInsn(GOTO, l3);
        Label l4 = new Label();
        cv.visitLabel(l4);
        cv.visitVarInsn(ASTORE, 3);
        cv.visitLabel(l3);
        cv.visitInsn(RETURN);
        cv.visitTryCatchBlock(l0, l2, l4, "java/lang/Exception");
        cv.visitMaxs(0, 0);

        cv = cw.visitMethod(ACC_PUBLIC, "invoke",
            "(L" + ASMIFIED_SOAP_MESSAGE + ";)L" + ASMIFIED_SOAP_MESSAGE + ";", null, null);
        cv.visitVarInsn(ALOAD, 0);
        cv.visitVarInsn(ALOAD, 1);
        cv.visitMethodInsn(INVOKESPECIAL, ASMIFIED_DBWS_PROVIDER_HELPER,
            "invoke", "(L" + ASMIFIED_SOAP_MESSAGE + ";)L" + ASMIFIED_SOAP_MESSAGE + ";");
        cv.visitInsn(ARETURN);
        cv.visitMaxs(0, 0);

        // METHOD ATTRIBUTES
        RuntimeVisibleAnnotations methodAttrs1 = new RuntimeVisibleAnnotations();
        Annotation methodAttrs1ann1 = new Annotation("L" + ASMIFIED_JSR_250_PREDESTROY + ";");
        methodAttrs1.annotations.add(methodAttrs1ann1);

        cv = cw.visitMethod(ACC_PUBLIC, "destroy", "()V", null, methodAttrs1);
        cv.visitVarInsn(ALOAD, 0);
        cv.visitMethodInsn(INVOKESPECIAL, ASMIFIED_DBWS_PROVIDER_HELPER,
            "destroy", "()V");
        cv.visitInsn(RETURN);
        cv.visitMaxs(0, 0);

        // synthetic
        cv = cw.visitMethod(ACC_PUBLIC + ACC_BRIDGE + ACC_SYNTHETIC, "invoke",
            "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        cv.visitVarInsn(ALOAD, 0);
        cv.visitVarInsn(ALOAD, 1);
        cv.visitTypeInsn(CHECKCAST, ASMIFIED_SOAP_MESSAGE);
        cv.visitMethodInsn(INVOKEVIRTUAL, DBWS_PROVIDER_PACKAGE + "/" + DBWS_PROVIDER_NAME,
            "invoke", "(L" + ASMIFIED_SOAP_MESSAGE + ";)L" + ASMIFIED_SOAP_MESSAGE + ";");
        cv.visitInsn(ARETURN);
        cv.visitMaxs(0, 0);

        // CLASS ATRIBUTE
        SignatureAttribute signatureAttr = new SignatureAttribute(
            "L" + ASMIFIED_DBWS_PROVIDER_HELPER + ";L" + ASMIFIED_JAX_WS_PROVIDER +
            "<L" + ASMIFIED_SOAP_MESSAGE + ";>;");
        cw.visitAttribute(signatureAttr);

        // CLASS ATRIBUTE
        RuntimeVisibleAnnotations classAttr = new RuntimeVisibleAnnotations();
        Annotation attrann0 = new Annotation("L" + ASMIFIED_JAX_WS_WEB_SERVICE_PROVIDER + ";");
        attrann0.add("wsdlLocation", WEB_INF_DIR + WSDL_DIR + DBWS_WSDL);
        attrann0.add("serviceName", serviceName);
        attrann0.add("portName", serviceName + "Port");
        attrann0.add("targetNamespace", builder.getWSDLGenerator().getServiceNameSpace());
        classAttr.annotations.add(attrann0);
        Annotation attrann1 = new Annotation("L" + ASMIFIED_JAX_WS_SERVICE_MODE + ";");
        attrann1.add("value", new Annotation.EnumConstValue(
            "L" + ASMIFIED_JAX_WS_SERVICE + "$Mode;", "MESSAGE"));
        classAttr.annotations.add(attrann1);
        cw.visitAttribute(classAttr);
        cv.visitMaxs(0, 0);

        cw.visitEnd();
        byte[] bytes = cw.toByteArray();
        try {
            codeGenProviderStream.write(bytes, 0, bytes.length);
        }
        catch (IOException e) {/* ignore */}
    }

    @Override
    public OutputStream getSourceProviderStream() throws FileNotFoundException {
        return new FileOutputStream(new File(stageDir, DBWS_PROVIDER_SOURCE_FILE));
    }
    @Override
    public void writeDBWSProviderSource(OutputStream sourceProviderStream, DBWSBuilder builder) {
        StringBuilder sb = new StringBuilder(DBWS_PROVIDER_SOURCE_PREAMBLE);
        String serviceName = builder.getWSDLGenerator().getServiceName();
        sb.append(serviceName);
        sb.append(DBWS_PROVIDER_SOURCE_PORT_NAME);
        sb.append(serviceName + "Port");
        sb.append(DBWS_PROVIDER_SOURCE_TARGET_NAMESPACE);
        sb.append(builder.getWSDLGenerator().getServiceNameSpace());
        sb.append(DBWS_PROVIDER_SOURCE_SUFFIX);
        OutputStreamWriter osw =
            new OutputStreamWriter(new BufferedOutputStream(sourceProviderStream));
        try {
            osw.write(sb.toString());
            osw.flush();
        }
        catch (IOException e) {/* ignore */}
    }
}
