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

import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.provider.comm.ConnectionCreateException;
import org.eclipse.ecf.provider.comm.ISynchAsynchConnection;
import org.eclipse.ecf.provider.jms.container.AbstractJMSClient;
import org.eclipse.ecf.provider.jms.container.JMSContainerConfig;
import org.eclipse.ecf.provider.jms.identity.JMSID;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastClientContainer extends AbstractJMSClient {

	public static final String HAZELCAST_CLIENT_NAME = "ecf.jms.hazelcast.client";

	public static class HazelcastClientContainerInstantiator extends AbstractHazelcastContainerInstantiator {

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

		@Override
		protected IContainer createContainer(JMSID clientID, Integer ka, @SuppressWarnings("rawtypes") Map props)
				throws Exception {
			return new HazelcastClientContainer(new JMSContainerConfig(clientID, ka, props),
					Hazelcast.newHazelcastInstance());
		}
	}

	private final HazelcastInstance hazelcast;

	public HazelcastClientContainer(JMSContainerConfig config, HazelcastInstance hazelcast) {
		super(config);
		this.hazelcast = hazelcast;
	}

	@Override
	protected ISynchAsynchConnection createConnection(ID targetID, Object data) throws ConnectionCreateException {
		return new HazelcastClientChannel(getReceiver(), hazelcast);
	}

}
