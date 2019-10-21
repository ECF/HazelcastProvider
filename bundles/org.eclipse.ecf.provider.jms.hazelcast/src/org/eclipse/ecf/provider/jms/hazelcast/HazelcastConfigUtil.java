/****************************************************************************
 * Copyright (c) 2019 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/
package org.eclipse.ecf.provider.jms.hazelcast;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.provider.internal.jms.hazelcast.DebugOptions;
import org.eclipse.ecf.provider.internal.jms.hazelcast.LogUtility;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.config.NetworkConfig;

public class HazelcastConfigUtil {

	public static void ajustConfig(Config hazelcastConfig, ID targetID) throws URISyntaxException {
		URI targetURI = new URI(targetID.getName());
		int port = targetURI.getPort();
		if (port != -1) {
			NetworkConfig netConfig = hazelcastConfig.getNetworkConfig();
			if (netConfig != null) {
				LogUtility.trace("adjustConfig", DebugOptions.CONFIG, HazelcastConfigUtil.class,
						"Resetting port on Hazelcast NetworkConfig.  Port was=" + netConfig.getPort() + " and will be="
								+ port);
				netConfig.setPort(port);
			}
		}
		String path = targetURI.getPath();
		while (path != null && path.startsWith("/")) {
			path = path.substring(1);
		}
		GroupConfig groupConfig = hazelcastConfig.getGroupConfig();
		if (groupConfig != null) {
			LogUtility.trace("adjustConfig", DebugOptions.CONFIG, HazelcastConfigUtil.class,
					"Resetting group name on Hazelcast GroupConfig.  Group name was=" + groupConfig.getName()
							+ " and will be=" + path);
			groupConfig.setName(path);
		}
	}

	public static void ajustClientConfig(ClientConfig clientConfig, ID targetID) throws URISyntaxException {
		URI targetURI = new URI(targetID.getName());
		String path = targetURI.getPath();
		while (path != null && path.startsWith("/")) {
			path = path.substring(1);
		}
		GroupConfig groupConfig = clientConfig.getGroupConfig();
		if (groupConfig != null) {
			LogUtility.trace("adjustConfig", DebugOptions.CONFIG, HazelcastConfigUtil.class,
					"Resetting group name on Hazelcast GroupConfig.  Group name was=" + groupConfig.getName()
							+ " and will be=" + path);
			groupConfig.setName(path);
		}
	}

}
