/****************************************************************************
 * Copyright (c) 2015 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/
package org.eclipse.ecf.provider.jms.hazelcast;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.provider.ContainerIntentException;
import org.eclipse.ecf.provider.internal.jms.hazelcast.Activator;
import org.eclipse.ecf.provider.internal.jms.hazelcast.DebugOptions;
import org.eclipse.ecf.provider.internal.jms.hazelcast.LogUtility;
import org.eclipse.ecf.provider.jms.identity.JMSID;
import org.eclipse.ecf.provider.jms.identity.JMSNamespace;
import org.eclipse.ecf.remoteservice.provider.PeerRemoteServiceContainerInstantiator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;

public abstract class AbstractHazelcastContainerInstantiator extends PeerRemoteServiceContainerInstantiator {

	public static final String DEFAULT_SERVER_ID = "hazelcast://localhost/defaultRemoteServicesGroup";

	public static final String ID_PARAM = "id";
	public static final String CONFIGURL_PARAM = "configURL";
	public static final String MEMBER_CONFIGURL_PARAM = "memberConfigURL";
	public static final String DEFAULT_CONFIG_BUNDLE_PATH = "/hazelcast.xml";
	public static final String DEFAULT_CLIENT_CONFIG_BUNDLE_PATH = "/hazelcast-client.xml";

	protected static final String[] hazelcastIntents = { "hazelcast" };

	public AbstractHazelcastContainerInstantiator() {
		super(Activator.HAZELCAST_MANAGER_NAME, Activator.HAZELCAST_MEMBER_NAME);
	}

	protected JMSID getJMSIDFromParameter(Map<String, ?> parameters, String key, String def) {
		Object p = getParameterValue(parameters, key, Object.class, def);
		if (p instanceof String) {
			return (JMSID) IDFactory.getDefault().createID(JMSNamespace.NAME, (String) p);
		} else if (p instanceof JMSID) {
			return (JMSID) p;
		} else if (def != null)
			return (JMSID) IDFactory.getDefault().createID(JMSNamespace.NAME, def);
		else
			return null;
	}

	@Override
	protected boolean supportsOSGIAsyncIntent(ContainerTypeDescription description) {
		return true;
	}

	public String[] getSupportedIntents(ContainerTypeDescription description) {
		List<String> results = new ArrayList<String>(Arrays.asList(super.getSupportedIntents(description)));
		results.addAll(Arrays.asList(hazelcastIntents));
		return (String[]) results.toArray(new String[results.size()]);
	}

	protected Config getURLConfigFromArg(Map<String, ?> parameters, BundleContext bundleContext) throws Exception {
		Object o = getParameterValue(parameters, CONFIGURL_PARAM, Object.class, null);
		if (o instanceof String) {
			String hazelcastConfigURL = (String) o;
			LogUtility.trace("getURLConfigFromArg", DebugOptions.DEBUG, this.getClass(),
					"Using configURL=" + hazelcastConfigURL + " for loading Hazelcast config");
			URL url = getURLFromConfigURL(hazelcastConfigURL, bundleContext, DEFAULT_CONFIG_BUNDLE_PATH, false);
			LogUtility.trace("getURLConfigFromArg", DebugOptions.DEBUG, this.getClass(),
					"Using configURL=" + url + " to load Hazelcast config");
			Config config = createHazelcastConfig(url);
			LogUtility.trace("getURLConfigFromArg", DebugOptions.DEBUG, this.getClass(),
					"Loaded Hazelcast config=" + config);
			return config;
		}
		return null;
	}

	protected ClientConfig getURLClientConfigFromArg(Map<String, ?> parameters, BundleContext bundleContext)
			throws Exception {
		Object o = getParameterValue(parameters, MEMBER_CONFIGURL_PARAM, Object.class, null);
		if (o instanceof String) {
			String hazelcastConfigURL = (String) o;
			LogUtility.trace("getURLClientConfigFromArg", DebugOptions.DEBUG, this.getClass(),
					"Using configURL=" + hazelcastConfigURL + " for loading Hazelcast config");
			URL url = getURLFromConfigURL(hazelcastConfigURL, bundleContext, DEFAULT_CLIENT_CONFIG_BUNDLE_PATH, false);
			LogUtility.trace("getURLClientConfigFromArg", DebugOptions.DEBUG, this.getClass(),
					"Using configURL=" + url + " to load Hazelcast config");
			ClientConfig config = createHazelcastClientConfig(url);
			LogUtility.trace("getURLClientConfigFromArg", DebugOptions.DEBUG, this.getClass(),
					"Loaded Hazelcast client config=" + config);
			return config;
		}
		return null;
	}

	protected void checkOSGIIntents(ContainerTypeDescription description, Config config, Map<String, ?> properties)
			throws ContainerIntentException {
		checkAsyncIntent(description, properties);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Dictionary getPropertiesForImportedConfigs(ContainerTypeDescription description, String[] importedConfigs,
			Dictionary exportedProperties) {
		Object o = exportedProperties.get(Activator.HAZELCAST_MANAGER_NAME + "." + MEMBER_CONFIGURL_PARAM);
		Hashtable h = new Hashtable();
		if (o != null) {
			LogUtility.trace("getPropertiesForImportedConfig", DebugOptions.CONFIG, this.getClass(),
					"Setting member configURL to manager.configURL=" + o);
			h.put(MEMBER_CONFIGURL_PARAM, o);
		}
		if (h.isEmpty()) {
			o = exportedProperties.get(Activator.HAZELCAST_MEMBER_NAME + "." + MEMBER_CONFIGURL_PARAM);
			if (o != null) {
				LogUtility.trace("getPropertiesForImportedConfig", DebugOptions.CONFIG, this.getClass(),
						"Setting member configURL to member.configURL=" + o);
				h.put(MEMBER_CONFIGURL_PARAM, o);
			}
		}
		return h;
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

	private ClientConfig createHazelcastClientConfig(URL hazelcastConfigURL) throws Exception {
		InputStream hazelcastInputStream = null;
		try {
			LogUtility.trace("createHazelcastClientConfig", DebugOptions.CONFIG, this.getClass(),
					"Loading hazelcast client config from URL=" + hazelcastConfigURL);
			hazelcastInputStream = hazelcastConfigURL.openStream();
			ClientConfig config = new XmlClientConfigBuilder(hazelcastInputStream).build();
			LogUtility.trace("createHazelcastClientConfig", DebugOptions.CONFIG, this.getClass(),
					"Loaded hazelcast client config from URL=" + hazelcastConfigURL);
			return config;
		} finally {
			if (hazelcastInputStream != null) {
				try {
					hazelcastInputStream.close();
				} catch (Exception e) {
					LogUtility.logError("start", DebugOptions.CONFIG, this.getClass(),
							"Exception closing input stream from url=" + hazelcastConfigURL);
				}
			}
		}
	}

	public IContainer createInstance(ContainerTypeDescription description, Map<String, ?> parameters)
			throws ContainerCreateException {
		String descriptionName = description.getName();
		JMSID id = null;
		try {
			if (descriptionName.equals(Activator.HAZELCAST_MANAGER_NAME)) {
				id = getJMSIDFromParameter(parameters, ID_PARAM, DEFAULT_SERVER_ID);
				LogUtility.trace("createInstance", DebugOptions.CONFIG, this.getClass(),
						"ID for new Hazelcast Manager container=" + id);
				Config config = getURLConfigFromArg(parameters, Activator.getContext());
				// If the above returns null, we should be using the hazelcast-default.xml in HC
				// bundle
				if (config == null) {
					LogUtility.trace("getURLConfigFromArg", DebugOptions.CONFIG, this.getClass(),
							"Loading Hazelcast config default -Dhazelcast.config="
									+ System.getProperty("hazelcast.config", "com.hazelcast/hazelcast-default.xml"));
					config = new XmlConfigBuilder().build();
				}
				config.setClassLoader(this.getClass().getClassLoader());
				checkOSGIIntents(description, config, parameters);
				LogUtility.trace("createInstance", DebugOptions.CONFIG, this.getClass(),
						"Using Hazelcast config=" + config);
				LogUtility.trace("createInstance", DebugOptions.CONFIG, this.getClass(),
						"Creating Hazelcast Manager Container");
				return createHazelcastManagerContainer(id, parameters, config);

			} else if (descriptionName.equals(Activator.HAZELCAST_MEMBER_NAME)) {
				id = getJMSIDFromParameter(parameters, ID_PARAM, UUID.randomUUID().toString());
				LogUtility.trace("createInstance", DebugOptions.CONFIG, this.getClass(),
						"ID for new Hazelcast member container=" + id);
				ClientConfig clientConfig = getURLClientConfigFromArg(parameters, Activator.getContext());
				// If the above returns null, we should be using the hazelcast-default.xml in HC
				// bundle
				if (clientConfig == null) {
					LogUtility.trace("getURLClientConfigFromArg", DebugOptions.CONFIG, this.getClass(),
							"Loading Hazelcast config default -Dhazelcastclient.config=" + System
									.getProperty("hazelcastclient.config", "com.hazelcast:/hazelcast-client-default.xml"));
					clientConfig = new XmlClientConfigBuilder().build();
				}
				LogUtility.trace("createInstance", DebugOptions.CONFIG, this.getClass(),
						"Using Hazelcast client config=" + clientConfig);
				LogUtility.trace("createInstance", DebugOptions.CONFIG, this.getClass(),
						"Creating Hazelcast Member Container");
				return createHazelcastMemberContainer(id, parameters, clientConfig);
			}
			throw new ContainerCreateException("Cannot create container with type=" + descriptionName);
		} catch (Exception e) {
			if (e instanceof ContainerIntentException)
				throw (ContainerIntentException) e;
			return throwCreateException("Could not create hazelcast container with name " + descriptionName, e);
		}
	}

	protected IContainer createHazelcastManagerContainer(JMSID serverID, @SuppressWarnings("rawtypes") Map props,
			Config config) throws Exception {
		throw new Exception("error");
	}

	protected IContainer createHazelcastMemberContainer(JMSID id, Map<String, ?> parameters, ClientConfig config)
			throws Exception {
		throw new Exception("error");
	}

}
