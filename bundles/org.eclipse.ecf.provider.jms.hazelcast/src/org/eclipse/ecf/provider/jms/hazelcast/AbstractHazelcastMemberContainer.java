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

import org.eclipse.ecf.core.ContainerConnectException;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription;
import org.eclipse.ecf.provider.comm.ConnectionCreateException;
import org.eclipse.ecf.provider.comm.IConnection;
import org.eclipse.ecf.provider.comm.ISynchAsynchConnection;
import org.eclipse.ecf.provider.internal.jms.hazelcast.Activator;
import org.eclipse.ecf.provider.internal.jms.hazelcast.DebugOptions;
import org.eclipse.ecf.provider.internal.jms.hazelcast.LogUtility;
import org.eclipse.ecf.provider.jms.container.AbstractJMSClient;
import org.eclipse.ecf.provider.jms.container.JMSContainerConfig;
import org.eclipse.ecf.remoteservice.IRSAConsumerContainerAdapter;
import org.eclipse.ecf.remoteservice.IRemoteServiceContainerAdapter;
import org.eclipse.ecf.remoteservice.IRemoteServiceReference;
import org.osgi.framework.InvalidSyntaxException;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.osgi.HazelcastOSGiService;

public abstract class AbstractHazelcastMemberContainer extends AbstractJMSClient
		implements IRSAConsumerContainerAdapter {

	public AbstractHazelcastMemberContainer(JMSContainerConfig config) {
		super(config);
	}

	protected HazelcastInstance hazelcastInstance;

	protected HazelcastInstance getHazelcastInstance() {
		return hazelcastInstance;
	}

	@Override
	protected void disconnect(IConnection conn) {
		super.disconnect(conn);
		disconnectHazelcast();
	}

	protected abstract void createHazelcastInstance(HazelcastOSGiService hazelcastOSGiService, ID targetID, Object data)
			throws Exception;

	@Override
	protected ISynchAsynchConnection createConnection(ID targetID, Object data) throws ConnectionCreateException {
		if (this.hazelcastInstance == null) {
			HazelcastOSGiService hazelcastOSGiService = Activator.getDefault().getHazelcastOSGiService();
			if (hazelcastOSGiService == null) {
				throw new ConnectionCreateException("Cannot get HazelcastOSGiService for member container connection");
			}
			// create and set hazelcast instance
			try {
				createHazelcastInstance(hazelcastOSGiService, targetID, data);
			} catch (Exception e) {
				ConnectionCreateException e1 = new ConnectionCreateException(
						"Cannot create connection for targetID=" + targetID, e);
				e1.setStackTrace(e.getStackTrace());
				throw e1;
			}
			LogUtility.trace("createConnection", DebugOptions.MEMBER, this.getClass(), "Hazelcast instance created");
			return new HazelcastMemberChannel(getReceiver(), hazelcastInstance);
		} else
			throw new ConnectionCreateException(
					"Cannot create connection because already have hazelcast client instance");
	}

	private void disconnectHazelcast() {
		if (this.hazelcastInstance != null) {
			LogUtility.trace("disconnectHazelcast", DebugOptions.MEMBER, this.getClass(),
					"Shutting down Hazelcast instance");
			this.hazelcastInstance.shutdown();
			this.hazelcastInstance = null;
			LogUtility.trace("disconnectHazelcast", DebugOptions.MEMBER, this.getClass(),
					"Hazelcast instance shutdown");
		}
	}

	@Override
	public void disconnect() {
		super.disconnect();
		disconnectHazelcast();
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
		ID targetID = ed.getConnectTargetID();
		if (targetID == null) {
			targetID = ed.getContainerID();
		}
		return adapter.getRemoteServiceReferences(targetID, new ID[] { ed.getContainerID() },
				ed.getInterfaces().iterator().next(), filter);
	}

}
