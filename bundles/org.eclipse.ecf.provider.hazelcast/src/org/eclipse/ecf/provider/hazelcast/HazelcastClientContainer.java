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
import java.util.UUID;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.provider.comm.ConnectionCreateException;
import org.eclipse.ecf.provider.comm.ISynchAsynchConnection;
import org.eclipse.ecf.provider.generic.GenericContainerInstantiator;
import org.eclipse.ecf.provider.jms.container.AbstractJMSClient;
import org.eclipse.ecf.provider.jms.container.JMSContainerConfig;
import org.eclipse.ecf.provider.jms.identity.JMSID;
import org.eclipse.ecf.provider.jms.identity.JMSNamespace;

public class HazelcastClientContainer extends AbstractJMSClient {

	public static final String HAZELCAST_CLIENT_NAME = "ecf.jms.hazelcast.client";

	public static class HazelcastClientContainerInstantiator extends GenericContainerInstantiator {
		public static final String[] hazelcastIntents = { "HAZELCAST" };
		public static final String ID_PARAM = "id";
		public static final String KEEPALIVE_PARAM = "keepAlive";

		private JMSID getJMSIDFromParameter(Object p) {
			if (p instanceof String) {
				return (JMSID) IDFactory.getDefault().createID(JMSNamespace.NAME, (String) p);
			} else if (p instanceof JMSID) {
				return (JMSID) p;
			} else
				return null;
		}

		@SuppressWarnings("rawtypes")
		public IContainer createInstance(ContainerTypeDescription description, Object[] args)
				throws ContainerCreateException {
			try {
				JMSID clientID = null;
				Integer ka = null;
				Map props = null;
				if (args == null)
					clientID = getJMSIDFromParameter(UUID.randomUUID().toString());
				else if (args.length > 0) {
					if (args[0] instanceof Map) {
						props = (Map) args[0];
						Object o = props.get(ID_PARAM);
						if (o != null && o instanceof String)
							clientID = getJMSIDFromParameter(o);
						o = props.get(KEEPALIVE_PARAM);
						if (o != null)
							ka = getIntegerFromArg(o);
					} else {
						clientID = getJMSIDFromParameter(args[0]);
						if (args.length > 1)
							ka = getIntegerFromArg(args[1]);
					}
				}
				if (clientID == null)
					clientID = getJMSIDFromParameter(UUID.randomUUID().toString());
				if (ka == null)
					ka = new Integer(HazelcastServerContainer.DEFAULT_KEEPALIVE);
				return new HazelcastClientContainer(new JMSContainerConfig(clientID, ka, props));
			} catch (Exception e) {
				ContainerCreateException t = new ContainerCreateException(
						"Exception creating activemq client container", e);
				t.setStackTrace(e.getStackTrace());
				throw t;
			}
		}

		public String[] getSupportedIntents(ContainerTypeDescription description) {
			List<String> results = new ArrayList<String>();
			for (int i = 0; i < genericProviderIntents.length; i++)
				results.add(genericProviderIntents[i]);
			for (int i = 0; i < hazelcastIntents.length; i++)
				results.add(hazelcastIntents[i]);
			return (String[]) results.toArray(new String[] {});
		}

		public String[] getImportedConfigs(ContainerTypeDescription description, String[] exporterSupportedConfigs) {
			List<String> results = new ArrayList<String>();
			List<String> supportedConfigs = Arrays.asList(exporterSupportedConfigs);
			if (HAZELCAST_CLIENT_NAME.equals(description.getName())) {
				if (
				// If it's a normal manager
				supportedConfigs.contains(HazelcastServerContainer.HAZELCAST_MANAGER_NAME)
						// Or the service exporter is a client
						|| supportedConfigs.contains(HAZELCAST_CLIENT_NAME)) {
					results.add(HAZELCAST_CLIENT_NAME);
				}
			}
			if (results.size() == 0)
				return null;
			return (String[]) results.toArray(new String[] {});
		}

		public String[] getSupportedConfigs(ContainerTypeDescription description) {
			return new String[] { HAZELCAST_CLIENT_NAME };
		}
	}

	public HazelcastClientContainer(JMSContainerConfig config) {
		super(config);
	}

	@Override
	protected ISynchAsynchConnection createConnection(ID targetID, Object data) throws ConnectionCreateException {
		return new HazelcastClientChannel(getReceiver(), null);
	}

}
