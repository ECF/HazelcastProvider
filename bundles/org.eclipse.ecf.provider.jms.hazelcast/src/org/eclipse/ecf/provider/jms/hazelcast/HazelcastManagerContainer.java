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

import java.util.Map;

import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.provider.comm.ISynchAsynchConnection;
import org.eclipse.ecf.provider.jms.container.AbstractJMSServer;
import org.eclipse.ecf.provider.jms.container.JMSContainerConfig;
import org.eclipse.ecf.provider.jms.identity.JMSID;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastManagerContainer extends AbstractJMSServer {

	public static class Instantiator extends AbstractHazelcastContainerInstantiator {

		@Override
		protected IContainer createHazelcastContainer(JMSID serverID, Integer ka,
				@SuppressWarnings("rawtypes") Map props, Config config) throws Exception {
			HazelcastManagerContainer sc = new HazelcastManagerContainer(
					new JMSContainerConfig(serverID, ka.intValue(), props),
					(config == null) ? Hazelcast.newHazelcastInstance() : Hazelcast.newHazelcastInstance(config));
			sc.start();
			return sc;
		}
	}

	private final HazelcastInstance hazelcast;

	protected HazelcastManagerContainer(JMSContainerConfig config, HazelcastInstance hazelcast) {
		super(config);
		this.hazelcast = hazelcast;
	}

	@Override
	public void start() throws ECFException {
		final ISynchAsynchConnection connection = new HazelcastManagerChannel(this.getReceiver(), this.hazelcast);
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
