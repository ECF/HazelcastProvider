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

import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryXmlConfig;
import com.hazelcast.config.UrlXmlConfig;
import com.hazelcast.config.XmlConfigBuilder;

public abstract class AbstractHazelcastContainerInstantiator extends PeerRemoteServiceContainerInstantiator {

	public static final String DEFAULT_SERVER_ID = "hazelcast://localhost/defaultRemoteServicesTopic";

	public static final String ID_PARAM = "id";
	public static final String KEEPALIVE_PARAM = "keepAlive";
	public static final String CONFIG_PARAM = "config";
	public static final String CONFIGURL_PARAM = "configURL";

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

	protected Config getConfigFromArg(Map<String, ?> parameters) throws Exception {
		Object o = getParameterValue(parameters, CONFIG_PARAM, Object.class, null);
		if (o instanceof Config) {
			Config config = (Config) o;
			LogUtility.trace("getConfigFromArg", DebugOptions.CONFIG, this.getClass(),
					"Loading Hazelcast config from exising config=" + config);
			return config;
		} else if (o instanceof InputStream) {
			InputStream ins = (InputStream) o;
			LogUtility.trace("getConfigFromArg", DebugOptions.CONFIG, this.getClass(),
					"Loading Hazelcast config from inputstream=" + ins);
			return new XmlConfigBuilder((InputStream) o).build();
		} else if (o instanceof URL) {
			URL url = (URL) o;
			LogUtility.trace("getConfigFromArg", DebugOptions.CONFIG, this.getClass(),
					"Loading Hazelcast config from url=" + url);
			return new UrlXmlConfig(url);
		} else if (o instanceof String) {
			String str = (String) o;
			LogUtility.trace("getConfigFromArg", DebugOptions.CONFIG, this.getClass(),
					"Loading Hazelcast config from string=" + str);
			return new InMemoryXmlConfig(str);
		}
		LogUtility.trace("getConfigFromArg", DebugOptions.CONFIG, this.getClass(),
				"Loading Hazelcast config default -Dhazelcast.config="
						+ System.getProperty("hazelcast.config", "<default>"));
		return new XmlConfigBuilder().build();
	}

	protected Config getURLConfigFromArg(Map<String, ?> parameters) throws Exception {
		Object o = getParameterValue(parameters, CONFIGURL_PARAM, Object.class, null);
		if (o instanceof String) {
			URL url = new URL((String) o);
			LogUtility.trace("getURLConfigFromArg", DebugOptions.DEBUG, this.getClass(),
					"Using configURL=" + url + " for loading Hazelcast config");
			return new UrlXmlConfig(new URL((String) o));
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
		Object o = exportedProperties.get(Activator.HAZELCAST_MANAGER_NAME + "." + CONFIGURL_PARAM);
		Hashtable h = new Hashtable();
		if (o != null) {
			LogUtility.trace("getPropertiesForImportedConfig", DebugOptions.CONFIG, this.getClass(),
					"Setting member configURL to manager.configURL=" + o);
			h.put(CONFIGURL_PARAM, o);
		} else {
			o = exportedProperties.get(Activator.HAZELCAST_MANAGER_NAME + "." + CONFIG_PARAM);
			if (o != null) {
				LogUtility.trace("getPropertiesForImportedConfig", DebugOptions.CONFIG, this.getClass(),
						"Setting member config to manager.config=" + o);
				h.put(CONFIG_PARAM, o);
			}
		}
		if (h.isEmpty()) {
			o = exportedProperties.get(Activator.HAZELCAST_MEMBER_NAME + "." + CONFIGURL_PARAM);
			if (o != null) {
				LogUtility.trace("getPropertiesForImportedConfig", DebugOptions.CONFIG, this.getClass(),
						"Setting member configURL to member.configURL=" + o);
				h.put(CONFIGURL_PARAM, o);
			} else {
				o = exportedProperties.get(Activator.HAZELCAST_MEMBER_NAME + "." + CONFIG_PARAM);
				if (o != null) {
					LogUtility.trace("getPropertiesForImportedConfig", DebugOptions.CONFIG, this.getClass(),
							"Setting member config to member.config=" + o);
					h.put(CONFIG_PARAM, o);
				}
			}
		}
		return h;
	}

	public IContainer createInstance(ContainerTypeDescription description, Map<String, ?> parameters)
			throws ContainerCreateException {
		try {
			Config config = getURLConfigFromArg(parameters);
			if (config == null)
				config = getConfigFromArg(parameters);

			boolean isServer = description.getName().equals(Activator.HAZELCAST_MANAGER_NAME);
			LogUtility.trace("createInstance", DebugOptions.CONFIG, this.getClass(),
					"Creating Hazelcast" + ((isServer) ? "Manager" : "Member") + "Container");
			JMSID id = getJMSIDFromParameter(parameters, ID_PARAM,
					isServer ? DEFAULT_SERVER_ID : UUID.randomUUID().toString());
			LogUtility.trace("createInstance", DebugOptions.CONFIG, this.getClass(),
					"ID for new Hazelcast container=" + id);
			checkOSGIIntents(description, config, parameters);
			// set config classloader
			config.setClassLoader(this.getClass().getClassLoader());
			return createHazelcastContainer(id, parameters, config);
		} catch (Exception e) {
			if (e instanceof ContainerIntentException)
				throw (ContainerIntentException) e;
			return throwCreateException("Could not create hazelcast container with name " + description.getName(), e);
		}
	}

	protected abstract IContainer createHazelcastContainer(JMSID id, Map<String, ?> parameters, Config config)
			throws Exception;
}
