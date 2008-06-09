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
 *     tware, ssmith - Initial Equinox weaving code
 ******************************************************************************/  
 package org.eclipse.persistence.jpa.equinox.weaving;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class WeaverRegistry implements ClassLoadingHook, ServiceTrackerCustomizer {
	private static WeaverRegistry instance = new WeaverRegistry();
	private List weaverServices = new ArrayList();
	private BundleContext ctx;
	private ServiceTracker serviceTracker;
	
	private WeaverRegistry() {}
	
	public static WeaverRegistry getInstance() {
		return instance;
	}

	public boolean addClassPathEntry(ArrayList cpEntries, String cp,
			ClasspathManager hostmanager, BaseData sourcedata,
			ProtectionDomain sourcedomain) {
		return false;
	}

	public BaseClassLoader createClassLoader(ClassLoader parent,
			ClassLoaderDelegate delegate, BundleProtectionDomain domain,
			BaseData data, String[] bundleclasspath) {
		return null;
	}

	public String findLibrary(BaseData data, String libName) {
		return null;
	}

	public ClassLoader getBundleClassLoaderParent() {
		return null;
	}

	public void initializedClassLoader(BaseClassLoader baseClassLoader,
			BaseData data) {		
	}

	public byte[] processClass(String name, byte[] classbytes,
			ClasspathEntry classpathEntry, BundleEntry entry,
			ClasspathManager manager) {
		if (this.weaverServices.isEmpty()) {
			return null;
		}
		for (Iterator iterator = this.weaverServices.iterator(); iterator.hasNext();) {
			ServiceReference reference = (ServiceReference) iterator.next();
			IWeaver weaver = (IWeaver)ctx.getService(reference);
			if (weaver != null) {
				byte[] transformedBytes = weaver.transform(name, classbytes);
				if (transformedBytes != null) {
					System.out.println(name + " woven");
					return transformedBytes;
				}
			}
		}
		return null;

//		try {
//			for (Iterator iterator = this.weaverServices.iterator(); iterator.hasNext();) {
//				ServiceReference reference = (ServiceReference) iterator.next();
//				Object weaver = ctx.getService(reference);
//				if (weaver != null) {
//					Class weaverClass = weaver.getClass();
//					Method transformMethod;
//						transformMethod = weaverClass.getMethod("transform", new Class[]{String.class, byte[].class});
//					byte[] transformedBytes = (byte[]) transformMethod.invoke(weaver, new Object[]{name, classbytes});
//					if (transformedBytes != null) {
//						return transformedBytes;
//					}
//				}
//			}
//		} catch (SecurityException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchMethodException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	public void start(BundleContext context) {
		this.ctx = context;
		serviceTracker = new ServiceTracker(context, IWeaver.class.getName(), this);
		serviceTracker.open();
	}

	public void stop(BundleContext context) {
		// Close the service tracker
		serviceTracker.close();
		serviceTracker = null;
		
		weaverServices = new ArrayList();
	}
	
	public Object addingService(ServiceReference reference) {
		System.out.println("Registering Service " + reference);
		this.weaverServices.add(reference);
		return reference;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// Rogue provider -- we don't support modifying provider services
		removedService(reference, service);
	}

	public void removedService(ServiceReference reference, Object service) {
		this.weaverServices.remove(reference);				
	}

}
