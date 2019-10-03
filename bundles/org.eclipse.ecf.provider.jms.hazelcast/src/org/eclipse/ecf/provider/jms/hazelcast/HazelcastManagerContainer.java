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

import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.ecf.core.ContainerConnectException;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription;
import org.eclipse.ecf.provider.comm.ConnectionCreateException;
import org.eclipse.ecf.provider.comm.ISynchAsynchConnection;
import org.eclipse.ecf.provider.internal.jms.hazelcast.Activator;
import org.eclipse.ecf.provider.jms.container.AbstractJMSServer;
import org.eclipse.ecf.provider.jms.container.JMSContainerConfig;
import org.eclipse.ecf.provider.jms.identity.JMSID;
import org.eclipse.ecf.remoteservice.IRSAConsumerContainerAdapter;
import org.eclipse.ecf.remoteservice.IRemoteServiceContainerAdapter;
import org.eclipse.ecf.remoteservice.IRemoteServiceReference;
import org.osgi.framework.InvalidSyntaxException;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.osgi.HazelcastOSGiService;

public class HazelcastManagerContainer extends AbstractJMSServer implements IRSAConsumerContainerAdapter {

	public static class Instantiator extends AbstractHazelcastContainerInstantiator {

		@Override
		protected IContainer createHazelcastContainer(JMSID serverID, @SuppressWarnings("rawtypes") Map props,
				Config config) throws Exception {
			HazelcastManagerContainer sc = new HazelcastManagerContainer(new JMSContainerConfig(serverID, 0, props),
					config);
			sc.start();
			return sc;
		}
	}

	private final Config hazelcastConfig;
	private HazelcastInstance hazelcastInstance;

	protected HazelcastManagerContainer(JMSContainerConfig config, Config hazelcastConfig) {
		super(config);
		this.hazelcastConfig = hazelcastConfig;
	}

	@Override
	public void start() throws ECFException {
		if (this.hazelcastInstance == null) {
			HazelcastOSGiService hazelcastOSGiService = Activator.getDefault().getHazelcastOSGiService();
			if (hazelcastOSGiService == null) {
				throw new ConnectionCreateException("Cannot get HazelcastOSGiService for member container connection");
			}
			if (this.hazelcastConfig != null) {
				try {
					HazelcastConfigUtil.ajustConfig(this.hazelcastConfig, getID());
				} catch (URISyntaxException e) {
					throw new ECFException("Cannot start HazelcastManagerContainer", e);
				}
				this.hazelcastInstance = hazelcastOSGiService.newHazelcastInstance(this.hazelcastConfig);
			} else {
				this.hazelcastInstance = hazelcastOSGiService.newHazelcastInstance();
			}
		}
		final ISynchAsynchConnection connection = new HazelcastManagerChannel(this.getReceiver(),
				this.hazelcastInstance);
		setConnection(connection);
		connection.start();
	}

	@Override
	public void dispose() {
		disconnect();
		super.dispose();
	}

	@Override
	public void disconnect() {
		super.disconnect();
		ISynchAsynchConnection conn = getConnection();
		if (conn != null)
			conn.disconnect();
		setConnection(null);
		if (hazelcastInstance != null) {
			this.hazelcastInstance.shutdown();
			this.hazelcastInstance = null;
		}
	}

	@Override
	public IRemoteServiceReference[] importEndpoint(Map<String, Object> endpointDescriptionProperties)
			throws ContainerConnectException, InvalidSyntaxException {
		EndpointDescription ed = new EndpointDescription(endpointDescriptionProperties);
		Long rsId = ed.getRemoteServiceId();
		String filter = new StringBuffer("(&(") //$NON-NLS-1$
				.append(org.eclipse.ecf.remoteservice.Constants.SERVICE_ID).append("=").append(rsId).append(")") //$NON-NLS-1$ //$NON-NLS-2$
				.append(")").toString();
		IRemoteServiceContainerAdapter adapter = (IRemoteServiceContainerAdapter) getAdapter(
				IRemoteServiceContainerAdapter.class);
		return adapter.getRemoteServiceReferences(getID(), new ID[] { ed.getContainerID() },
				ed.getInterfaces().iterator().next(), filter);
	}

}
