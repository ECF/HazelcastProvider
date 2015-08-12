/*******************************************************************************
* Copyright (c) 2015 Composent, Inc. and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Composent, Inc. - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.provider.hazelcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.provider.comm.ISynchAsynchConnection;
import org.eclipse.ecf.provider.jms.container.AbstractJMSServer;
import org.eclipse.ecf.provider.jms.container.JMSContainerConfig;
import org.eclipse.ecf.provider.jms.identity.JMSID;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastServerContainer extends AbstractJMSServer {

	public static final String HAZELCAST_MANAGER_NAME = "ecf.jms.hazelcast.manager";

	public static class HazelcastServerContainerInstantiator extends AbstractHazelcastContainerInstantiator {
		@SuppressWarnings("rawtypes")
		public IContainer createInstance(ContainerTypeDescription description, Object[] args)
				throws ContainerCreateException {
			try {
				JMSID serverID = null;
				Map props = null;
				Integer ka = null;
				if (args == null)
					serverID = getJMSIDFromParameter((String) DEFAULT_SERVER_ID);
				else if (args.length > 0) {
					if (args[0] instanceof Map) {
						props = (Map) args[0];
						Object o = props.get(ID_PARAM);
						if (o != null && o instanceof String)
							serverID = getJMSIDFromParameter(o);
					} else {
						serverID = getJMSIDFromParameter(args[0]);
						if (args.length > 1)
							ka = getIntegerFromArg(args[1]);
					}
				}
				if (ka == null)
					ka = new Integer(DEFAULT_KEEPALIVE);
				HazelcastServerContainer server = new HazelcastServerContainer(
						new JMSContainerConfig(serverID, ka.intValue(), props), Hazelcast.newHazelcastInstance());
				server.start();
				return server;
			} catch (Exception e) {
				throw new ContainerCreateException("Exception creating activemq server container", e);
			}
		}

		public String[] getImportedConfigs(ContainerTypeDescription description, String[] exporterSupportedConfigs) {
			List<String> results = new ArrayList<String>();
			List<String> supportedConfigs = Arrays.asList(exporterSupportedConfigs);
			// For a manager, if a client is exporter then we are an importer
			if (HAZELCAST_MANAGER_NAME.equals(description.getName())) {
				if (supportedConfigs.contains(HazelcastClientContainer.HAZELCAST_CLIENT_NAME))
					results.add(HAZELCAST_MANAGER_NAME);
			}
			if (results.size() == 0)
				return null;
			return (String[]) results.toArray(new String[] {});
		}

		public String[] getSupportedConfigs(ContainerTypeDescription description) {
			return new String[] { HAZELCAST_MANAGER_NAME };
		}

		@Override
		protected IContainer createHazelcastContainer(JMSID serverID, Integer ka,
				@SuppressWarnings("rawtypes") Map props, Config config) throws Exception {
			HazelcastServerContainer sc = new HazelcastServerContainer(
					new JMSContainerConfig(serverID, ka.intValue(), props),
					(config == null) ? Hazelcast.newHazelcastInstance() : Hazelcast.newHazelcastInstance(config));
			sc.start();
			return sc;
		}

	}

	private final HazelcastInstance hazelcast;

	public HazelcastServerContainer(JMSContainerConfig config, HazelcastInstance hazelcast) {
		super(config);
		this.hazelcast = hazelcast;
	}

	@Override
	public void start() throws ECFException {
		final ISynchAsynchConnection connection = new HazelcastServerChannel(this.getReceiver(), this.hazelcast);
		setConnection(connection);
		connection.start();
	}

	@Override
	public void disconnect() {
		super.disconnect();
		ISynchAsynchConnection conn = getConnection();
		if (conn != null)
			conn.disconnect();
		setConnection(null);
	}

}
