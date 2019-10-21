/*******************************************************************************
* Copyright (c) 2019 Composent, Inc. and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Composent, Inc. - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.provider.jms.hazelcast;

import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.provider.comm.ConnectionCreateException;
import org.eclipse.ecf.provider.internal.jms.hazelcast.DebugOptions;
import org.eclipse.ecf.provider.internal.jms.hazelcast.LogUtility;
import org.eclipse.ecf.provider.jms.container.JMSContainerConfig;
import org.eclipse.ecf.provider.jms.identity.JMSID;
import org.eclipse.ecf.remoteservice.IRSAConsumerContainerAdapter;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.osgi.HazelcastOSGiService;

public class HazelcastMemberContainer extends AbstractHazelcastMemberContainer implements IRSAConsumerContainerAdapter {

	public static class Instantiator extends AbstractHazelcastContainerInstantiator {

		@Override
		protected IContainer createHazelcastMemberContainer(JMSID serverID, @SuppressWarnings("rawtypes") Map props,
				ClientConfig config) throws Exception {
			return new HazelcastMemberContainer(serverID, props, config);
		}
	}

	private final ClientConfig hazelcastConfig;

	protected HazelcastMemberContainer(JMSID id, @SuppressWarnings("rawtypes") Map props, ClientConfig config) {
		super(new JMSContainerConfig(id, 0, props));
		this.hazelcastConfig = config;
	}

	@Override
	protected void createHazelcastInstance(HazelcastOSGiService hazelcastOSGiService, ID targetID, Object data)
			throws Exception {
		if (this.hazelcastConfig != null) {
			try {
				HazelcastConfigUtil.ajustClientConfig(this.hazelcastConfig, targetID);
			} catch (URISyntaxException e) {
				throw new ConnectionCreateException("Could not adjust hazelcastConfig with targetID=" + targetID, e);
			}
			LogUtility.trace("createConnection", DebugOptions.MEMBER, this.getClass(),
					"Creating HazelcastClient instance with config=" + this.hazelcastConfig);
			hazelcastInstance = HazelcastClient.newHazelcastClient(this.hazelcastConfig);
		} else {
			LogUtility.trace("createConnection", DebugOptions.MEMBER, this.getClass(),
					"Creating Hazelcast instance with default config");
			hazelcastInstance = HazelcastClient.newHazelcastClient();
		}

	}

}
