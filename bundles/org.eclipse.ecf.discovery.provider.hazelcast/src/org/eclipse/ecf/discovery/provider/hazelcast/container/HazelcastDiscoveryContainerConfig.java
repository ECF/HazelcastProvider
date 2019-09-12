/*******************************************************************************
 * Copyright (c) 2019 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.discovery.provider.hazelcast.container;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.identity.URIID;
import org.eclipse.ecf.discovery.DiscoveryContainerConfig;
import org.eclipse.ecf.discovery.identity.IServiceTypeID;
import org.eclipse.ecf.discovery.identity.ServiceIDFactory;
import org.eclipse.ecf.discovery.provider.hazelcast.identity.HazelcastNamespace;
import org.eclipse.ecf.discovery.provider.hazelcast.identity.HazelcastServiceID;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;

public class HazelcastDiscoveryContainerConfig extends DiscoveryContainerConfig {

	private HazelcastServiceID targetID;
	private Config hazelcastConfig;
	
	private static URI createURIFromConfig(Config hazelcastConfig) throws URISyntaxException {
		StringBuffer buf = new StringBuffer("hazelcast://");
		NetworkConfig netConfig = hazelcastConfig.getNetworkConfig(); 
		int port = netConfig.getPort();
		JoinConfig joinConfig = netConfig.getJoin();
		MulticastConfig mcConfig = joinConfig.getMulticastConfig();
		TcpIpConfig tcpConfig = joinConfig.getTcpIpConfig();
		String host = null;
		if (mcConfig.isEnabled()) {
			host = mcConfig.getMulticastGroup();
		} else if (tcpConfig.isEnabled()) {
			host = tcpConfig.getRequiredMember();
			if (host == null) {
				host = "127.0.0.1";
			}
		}
		if (host == null) {
			throw new RuntimeException("Cannot config ECF Hazelcast Discovery because config="+hazelcastConfig+" does not have multicast or tcpip enabled");
		}
		buf.append(host).append(":").append(port).append("/").append(hazelcastConfig.getGroupConfig().getName());
		return new URI(buf.toString());

	}
	public HazelcastDiscoveryContainerConfig(Config hazelcastConfig) throws URISyntaxException {
		super(IDFactory.getDefault().createURIID(createURIFromConfig(hazelcastConfig)));
		IServiceTypeID serviceTypeID = ServiceIDFactory.getDefault().createServiceTypeID(HazelcastNamespace.INSTANCE,
				HazelcastNamespace.SCHEME);
		this.targetID = (HazelcastServiceID) IDFactory.getDefault().createID(HazelcastNamespace.NAME,
				new Object[] { serviceTypeID,((URIID) getID()).toURI() });
		this.hazelcastConfig = hazelcastConfig;
	}

	public HazelcastServiceID getTargetID() {
		return targetID;
	}

	public Config getHazelcastConfig() {
		return hazelcastConfig;
	}
}
