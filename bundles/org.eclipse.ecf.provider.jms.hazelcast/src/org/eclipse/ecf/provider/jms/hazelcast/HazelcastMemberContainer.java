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
			return new HazelcastMemberContainer(new JMSContainerConfig(id, ka, props),
					(config == null) ? Hazelcast.newHazelcastInstance() : Hazelcast.newHazelcastInstance(config));
		}
	}

	private final HazelcastInstance hazelcast;

	protected HazelcastMemberContainer(JMSContainerConfig config, HazelcastInstance hazelcast) {
		super(config);
		this.hazelcast = hazelcast;
	}

	@Override
	protected ISynchAsynchConnection createConnection(ID targetID, Object data) throws ConnectionCreateException {
		return new HazelcastMemberChannel(getReceiver(), hazelcast);
	}

}
