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
import java.net.URI;
import java.net.URL;
import java.util.Hashtable;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainerFactory;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.util.BundleStarter;
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
import com.hazelcast.osgi.HazelcastOSGiService;

public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "org.eclipse.ecf.discovery.provider.hazelcast"; //$NON-NLS-1$

	private static final String[] DEPENDENT_BUNDLES = new String[] { "com.hazelcast" };

	private static final boolean HAZELCAST_ENABLED = Boolean
			.valueOf(System.getProperty(HazelcastDiscoveryContainerInstantiator.NAME + ".enabled", "true"))
			.booleanValue();

	private static final String HAZELCAST_CONFIG_PROP = HazelcastDiscoveryContainerInstantiator.NAME + ".configURL";
	private static final String HAZELCAST_CONFIG = System.getProperty(HAZELCAST_CONFIG_PROP);

	private static final String HAZELCAST_CONFIG_DEFAULT_PATH = "/hc-discovery-config.xml";

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

	private ServiceTracker<HazelcastOSGiService, HazelcastOSGiService> hazelcastTracker;

	public synchronized HazelcastOSGiService getHazelcastOSGiService() {
		if (hazelcastTracker == null) {
			hazelcastTracker = new ServiceTracker<HazelcastOSGiService, HazelcastOSGiService>(getContext(),
					HazelcastOSGiService.class, null);
			hazelcastTracker.open();
		}
		return hazelcastTracker.getService();
	}

	private synchronized void stopHazelcastOSGiService() {
		if (hazelcastTracker != null) {
			hazelcastTracker.close();
			hazelcastTracker = null;
		}
	}

	private Bundle findBundleForHazelcastConfig(String bundleSymbolicName, BundleContext context)
			throws BundleException {
		if (bundleSymbolicName != null) {
			Bundle candidate = null;
			for (Bundle b : context.getBundles()) {
				if (b.getSymbolicName().equals(bundleSymbolicName)) {
					if (candidate == null || b.getVersion().compareTo(candidate.getVersion()) > 0) {
						candidate = b;
					}
				}
			}
			if (candidate != null) {
				return candidate;
			} else {
				throw new BundleException("Could not find a bundle with symbolic name=" + bundleSymbolicName);
			}
		} else {
			// use this bundle (default)
			return context.getBundle();
		}
	}

	private URL getURLFromConfigURL(String hazelcastConfigURI, BundleContext bundleContext, String bundleConfigFilePath,
			boolean useDefaultBundle) throws Exception {
		String bundleSymbolicName = null;
		String configPath = bundleConfigFilePath;
		Bundle bundle = useDefaultBundle ? bundleContext.getBundle() : null;
		if (hazelcastConfigURI != null) {
			URI uri = new URI(hazelcastConfigURI);
			if ("bundle".equalsIgnoreCase(uri.getScheme())) {
				// Bundle URL given
				String bundleAndPath = uri.getSchemeSpecificPart();
				int pathLocation = bundleAndPath.indexOf("/");
				if (pathLocation != -1) {
					bundleSymbolicName = bundleAndPath.substring(0, pathLocation);
					configPath = bundleAndPath.substring(pathLocation);
				} else {
					bundleSymbolicName = bundleAndPath;
				}
				LogUtility.trace("getURLWithHazelcastConfig", DebugOptions.CONFIG, this.getClass(),
						"Hazelcast config defined by bundle URI. bundleSymbolicName=" + bundleSymbolicName + ";path="
								+ configPath);
				bundle = findBundleForHazelcastConfig(bundleSymbolicName, bundleContext);
				LogUtility.trace("getURLWithHazelcastConfig", DebugOptions.CONFIG, this.getClass(),
						"Found bundle=" + bundle + " for symbolic name=" + bundleSymbolicName);

			} else {
				URL url = uri.toURL();
				LogUtility.trace("getURLWithHazelcastConfig", DebugOptions.CONFIG, this.getClass(),
						"Returning URL=" + url);
				return url;
			}
		}
		if (bundle != null) {
			URL url = bundle.getEntry(configPath);
			LogUtility.trace("getURLWithHazelcastConfig", DebugOptions.CONFIG, this.getClass(),
					"Returning URL=" + url + " from bundle=" + bundle + ";configPath=" + configPath);
			return url;
		} else {
			LogUtility.trace("getURLWithHazelcastConfig", DebugOptions.CONFIG, this.getClass(), "Returning NULL");
			return null;
		}
	}

	private Config createHazelcastConfig(URL hazelcastConfigURL) throws Exception {
		InputStream hazelcastInputStream = null;
		try {
			LogUtility.trace("getHazelcastConfig", DebugOptions.CONFIG, this.getClass(),
					"Loading hazelcast config from URL=" + hazelcastConfigURL);
			hazelcastInputStream = hazelcastConfigURL.openStream();
			Config config = new XmlConfigBuilder(hazelcastInputStream).build();
			LogUtility.trace("getHazelcastConfig", DebugOptions.CONFIG, this.getClass(),
					"Loaded hazelcast config from URL=" + hazelcastConfigURL);
			return config;
		} finally {
			if (hazelcastInputStream != null) {
				try {
					hazelcastInputStream.close();
				} catch (Exception e) {
					LogUtility.logError("start", DebugOptions.CONFIG, this.getClass(),
							"Exception closing hazelcast input stream from url=" + hazelcastConfigURL);
				}
			}
		}
	}

	public void start(BundleContext ctxt) throws Exception {
		plugin = this;
		context = ctxt;

		if (HAZELCAST_ENABLED) {
			LogUtility.trace("start", DebugOptions.CONFIG, this.getClass(), "Hazelcast Discovery enabled");
			BundleStarter.startDependents(ctxt, DEPENDENT_BUNDLES, Bundle.RESOLVED | Bundle.STARTING);
			// Register Namespace and ContainerTypeDescription first
			IDFactory.getDefault().addNamespace(new HazelcastNamespace());
			context.registerService(ContainerTypeDescription.class, ctd, null);
			// Get URL given system props (constants above)
			if (HAZELCAST_CONFIG != null) {
				LogUtility.trace("start", DebugOptions.CONFIG, this.getClass(),
						"Hazelcast discovery config set via system property= " + HAZELCAST_CONFIG_PROP + ";value="
								+ HAZELCAST_CONFIG);
			}
			Bundle b = context.getBundle();
			URL hazelcastConfigURL = getURLFromConfigURL(HAZELCAST_CONFIG, context, HAZELCAST_CONFIG_DEFAULT_PATH,
					true);
			if (hazelcastConfigURL == null) {
				throw new BundleException("Could not get hazelcastConfig URL with HAZELCAST_CONFIG=" + HAZELCAST_CONFIG
						+ ";bundle=" + b + ";defaultPath=" + HAZELCAST_CONFIG_DEFAULT_PATH);
			}
			LogUtility.trace("start", DebugOptions.CONFIG, this.getClass(),
					"Hazelcast discovery configURL=" + hazelcastConfigURL);
			Config hazelcastConfig = createHazelcastConfig(hazelcastConfigURL);
			if (hazelcastConfig == null) {
				throw new BundleException(
						"Could not retrieve or create Hazelcast config for configURL=" + hazelcastConfigURL);
			}
			LogUtility.trace("start", DebugOptions.CONFIG, this.getClass(),
					"Using Hazelcast config=" + hazelcastConfig + " for discovery");

			// Have config so we set classloader
			hazelcastConfig.setClassLoader(this.getClass().getClassLoader());
			// Create HazelcastDiscoveryContainerConfig
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
		} else {
			LogUtility.trace("start", DebugOptions.CONFIG, this.getClass(), "Hazelcast Discovery DISABLED");
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
		stopHazelcastOSGiService();
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
