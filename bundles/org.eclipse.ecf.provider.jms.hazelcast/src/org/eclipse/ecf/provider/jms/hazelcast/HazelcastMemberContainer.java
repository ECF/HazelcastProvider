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
import org.eclipse.ecf.provider.comm.ISynchAsynchConnection;
import org.eclipse.ecf.provider.jms.container.AbstractJMSClient;
import org.eclipse.ecf.provider.jms.container.JMSContainerConfig;
import org.eclipse.ecf.provider.jms.identity.JMSID;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastMemberContainer extends AbstractJMSClient {

	public static class Instantiator extends AbstractHazelcastContainerInstantiator {

		@Override
		protected IContainer createHazelcastContainer(JMSID id, Integer ka, @SuppressWarnings("rawtypes") Map props,
				Config config) throws Exception {
			return new HazelcastMemberContainer(new JMSContainerConfig(id, ka, props), config);
		}
	}

	private final Config hazelcastConfig;
	private HazelcastInstance hazelcastInstance;

	protected HazelcastMemberContainer(JMSContainerConfig config, Config hazelcastConfig) {
		super(config);
		this.hazelcastConfig = hazelcastConfig;
	}

	@Override
	protected ISynchAsynchConnection createConnection(ID targetID, Object data) throws ConnectionCreateException {
		if (this.hazelcastInstance == null) {
			if (this.hazelcastConfig != null) {
				try {
					HazelcastConfigUtil.ajustConfig(this.hazelcastConfig, targetID);
				} catch (URISyntaxException e) {
					throw new ConnectionCreateException("Could not adjust hazelcastConfig with targetID=" + targetID,
							e);
				}
				hazelcastInstance = Hazelcast.newHazelcastInstance(this.hazelcastConfig);
			} else {
				hazelcastInstance = Hazelcast.newHazelcastInstance();
			}
			return new HazelcastMemberChannel(getReceiver(), hazelcastInstance);
		} else
			throw new ConnectionCreateException("Cannot connect because already have hazelcast instance");
	}

	@Override
	public void disconnect() {
		super.disconnect();
		if (this.hazelcastInstance != null) {
			this.hazelcastInstance.shutdown();
			this.hazelcastInstance = null;
		}
	}
}
