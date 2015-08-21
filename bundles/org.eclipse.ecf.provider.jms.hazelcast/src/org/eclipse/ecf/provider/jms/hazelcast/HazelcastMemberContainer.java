/*******************************************************************************
* Copyright (c) 2015 Composent, Inc. and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Composent, Inc. - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.provider.jms.hazelcast;

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

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastMemberContainer extends AbstractJMSClient {

	public static final String HAZELCAST_MEMBER_NAME = "ecf.jms.hazelcast.member";
	public static final String HAZELCAST_MEMBER_CONFIG_PARAM = "hazelcastMemberConfig";
	
	public static class HazelcastMemberContainerInstantiator extends AbstractHazelcastContainerInstantiator {

		public String[] getImportedConfigs(ContainerTypeDescription description, String[] exporterSupportedConfigs) {
			List<String> results = new ArrayList<String>();
			List<String> supportedConfigs = Arrays.asList(exporterSupportedConfigs);
			if (HAZELCAST_MEMBER_NAME.equals(description.getName())) {
				if (
				// If it's a normal manager
				supportedConfigs.contains(HazelcastManagerContainer.HAZELCAST_MANAGER_NAME)
						// Or the service exporter is a client
						|| supportedConfigs.contains(HAZELCAST_MEMBER_NAME)
						) {
					results.add(HAZELCAST_MEMBER_NAME);
				}
			}
			if (results.size() == 0)
				return null;
			return (String[]) results.toArray(new String[] {});
		}

		public String[] getSupportedConfigs(ContainerTypeDescription description) {
			return new String[] { HAZELCAST_MEMBER_NAME };
		}

		@Override
		protected IContainer createHazelcastContainer(JMSID id, Integer ka, @SuppressWarnings("rawtypes") Map props,
				Config config) throws Exception {
			return new HazelcastMemberContainer(new JMSContainerConfig(id, ka, props),
					(config == null) ? Hazelcast.newHazelcastInstance() : Hazelcast.newHazelcastInstance(config));
		}

		@Override
		protected String getHazelcastConfigParam() {
			return HAZELCAST_MEMBER_CONFIG_PARAM;
		}
	}

	private final HazelcastInstance hazelcast;

	public HazelcastMemberContainer(JMSContainerConfig config, HazelcastInstance hazelcast) {
		super(config);
		this.hazelcast = hazelcast;
	}

	@Override
	protected ISynchAsynchConnection createConnection(ID targetID, Object data) throws ConnectionCreateException {
		return new HazelcastMemberChannel(getReceiver(), hazelcast);
	}

}
