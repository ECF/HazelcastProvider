/*******************************************************************************
 * Copyright (c) 2019 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.discovery.provider.hazelcast;

import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainerFactory;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.util.LogHelper;
import org.eclipse.ecf.core.util.SystemLogService;
import org.eclipse.ecf.discovery.IDiscoveryAdvertiser;
import org.eclipse.ecf.discovery.IDiscoveryLocator;
import org.eclipse.ecf.discovery.provider.hazelcast.container.HazelcastDiscoveryContainer;
import org.eclipse.ecf.discovery.provider.hazelcast.container.HazelcastDiscoveryContainerConfig;
import org.eclipse.ecf.discovery.provider.hazelcast.container.HazelcastDiscoveryContainerInstantiator;
import org.eclipse.ecf.discovery.provider.hazelcast.identity.HazelcastNamespace;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;

public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "org.eclipse.ecf.discovery.provider.hazelcast"; //$NON-NLS-1$

	private static final boolean HAZELCAST_ENABLED = Boolean
			.valueOf(System.getProperty(HazelcastDiscoveryContainerInstantiator.NAME + ".enabled", "true"))
			.booleanValue();

	private static final String HAZELCAST_CONFIG = System
			.getProperty(HazelcastDiscoveryContainerInstantiator.NAME + ".configURL");
	private static final String HAZELCAST_CONFIG_BUNDLE_SYMBOLIC_NAME = System
			.getProperty(HazelcastDiscoveryContainerInstantiator.NAME + ".configBundleSymbolicName");
	private static final String HAZELCAST_CONFIG_BUNDLE_ENTRY_PATH = System
			.getProperty(HazelcastDiscoveryContainerInstantiator.NAME + ".configBundleEntryPath", "/hazelcast.xml");

	private static Activator plugin;

	public static Activator getDefault() {
		return plugin;
	}

	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	private ContainerTypeDescription ctd = new ContainerTypeDescription(HazelcastDiscoveryContainerInstantiator.NAME,
			new HazelcastDiscoveryContainerInstantiator(), "Hazelcast Discovery Container", true, false); //$NON-NLS-1$

	private ServiceTracker<LogService, LogService> logServiceTracker = null;
	private LogService logService = null;
	// container instance
	private HazelcastDiscoveryContainer container;
	private ServiceTracker<IContainerFactory, IContainerFactory> cfTracker;

	private Bundle findBundleForHazelcastConfig() throws BundleException {
		if (HAZELCAST_CONFIG_BUNDLE_SYMBOLIC_NAME != null) {
			Bundle candidate = null;
			for (Bundle b : context.getBundles()) {
				if (b.getSymbolicName().equals(HAZELCAST_CONFIG_BUNDLE_SYMBOLIC_NAME)) {
					if (candidate == null || b.getVersion().compareTo(candidate.getVersion()) > 0) {
						candidate = b;
					}
				}
			}
			if (candidate != null) {
				return candidate;
			} else {
				throw new BundleException(
						"Could not find a bundle with symbolic name=" + HAZELCAST_CONFIG_BUNDLE_SYMBOLIC_NAME);
			}
		} else {
			// use this bundle (default)
			return context.getBundle();
		}
	}

	private URL getURLWithHazelcastConfig() throws Exception {
		return (HAZELCAST_CONFIG != null) ? new URL(HAZELCAST_CONFIG)
				: findBundleForHazelcastConfig().getEntry(HAZELCAST_CONFIG_BUNDLE_ENTRY_PATH);
	}

	public void start(BundleContext ctxt) throws Exception {
		plugin = this;
		context = ctxt;

		if (HAZELCAST_ENABLED) {
			// Get URL given system props (constants above)
			URL hazelcastConfigFile = getURLWithHazelcastConfig();
			// Register Namespace and ContainerTypeDescription first
			IDFactory.getDefault().addNamespace(new HazelcastNamespace());
			context.registerService(ContainerTypeDescription.class, ctd, null);

			Config hazelcastConfig = null;
			InputStream hazelcastInputStream = null;
			try {
				hazelcastInputStream = hazelcastConfigFile.openStream();
				hazelcastConfig = new XmlConfigBuilder(hazelcastInputStream).build();
			} finally {
				if (hazelcastInputStream != null) {
					try {
						hazelcastInputStream.close();
					} catch (Exception e) {
					}
				}
			}

			hazelcastConfig.setClassLoader(this.getClass().getClassLoader());
			final HazelcastDiscoveryContainerConfig config = new HazelcastDiscoveryContainerConfig(hazelcastConfig);

			final Hashtable<String, Object> props = new Hashtable<String, Object>();
			props.put(IDiscoveryLocator.CONTAINER_NAME, HazelcastDiscoveryContainerInstantiator.NAME);
			props.put(Constants.SERVICE_RANKING, new Integer(500));
			context.registerService(
					new String[] { IDiscoveryAdvertiser.class.getName(), IDiscoveryLocator.class.getName() },
					new ServiceFactory<HazelcastDiscoveryContainer>() {
						public HazelcastDiscoveryContainer getService(Bundle bundle,
								ServiceRegistration<HazelcastDiscoveryContainer> registration) {
							return getHazelcastContainer(config);
						}

						public void ungetService(Bundle bundle,
								ServiceRegistration<HazelcastDiscoveryContainer> registration,
								HazelcastDiscoveryContainer service) {
							ungetHazelcastContainer();
						}
					}, props);
		}
	}

	synchronized void ungetHazelcastContainer() {
		if (container != null) {
			container.disconnect();
			container = null;
		}
	}

	synchronized HazelcastDiscoveryContainer getHazelcastContainer(HazelcastDiscoveryContainerConfig config) {
		if (container == null) {
			try {
				container = (HazelcastDiscoveryContainer) getContainerFactory().createContainer(ctd,
						(Object[]) new Object[] { config });
				container.connect(null, null);
				LogUtility.logInfo("getHazelcastContainer", DebugOptions.DEBUG, this.getClass(), //$NON-NLS-1$
						"Discovery connected to Hazelcast container with id=" + container.getID().getName() //$NON-NLS-1$
								+ ";targetID=" + container.getConnectedID().getName());
			} catch (Exception e) {
				LogUtility.logError("getHazelcastContainer", DebugOptions.DEBUG, this.getClass(), //$NON-NLS-1$
						"Hazelcast discovery setup failed", e); //$NON-NLS-1$
				container = null;
			}
		}
		return container;
	}

	public void stop(BundleContext context) throws Exception {
		if (cfTracker != null) {
			cfTracker.close();
			cfTracker = null;
		}
		if (logServiceTracker != null) {
			logServiceTracker.close();
			logServiceTracker = null;
			logService = null;
		}
		ungetHazelcastContainer();
		context = null;
		plugin = null;
	}

	IContainerFactory getContainerFactory() {
		if (cfTracker == null) {
			cfTracker = new ServiceTracker<IContainerFactory, IContainerFactory>(context, IContainerFactory.class,
					null);
			cfTracker.open();
		}
		return cfTracker.getService();
	}

	public LogService getLogService() {
		if (logServiceTracker == null) {
			logServiceTracker = new ServiceTracker<LogService, LogService>(context, LogService.class.getName(), null);
			logServiceTracker.open();
		}
		logService = logServiceTracker.getService();
		if (logService == null)
			logService = new SystemLogService(PLUGIN_ID);
		return logService;
	}

	@SuppressWarnings("deprecation")
	public void log(IStatus status) {
		if (logService == null)
			logService = getLogService();
		if (logService != null)
			logService.log(null, LogHelper.getLogCode(status), LogHelper.getLogMessage(status), status.getException());
	}

	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, IStatus status) {
		log(sr, LogHelper.getLogCode(status), LogHelper.getLogMessage(status), status.getException());
	}

	@SuppressWarnings("deprecation")
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message, Throwable t) {
		if (logService == null)
			logService = getLogService();
		if (logService != null)
			logService.log(sr, level, message, t);
	}

}
