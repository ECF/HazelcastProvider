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
				netConfig.setPort(port);
			}
		}
		String path = targetURI.getPath();
		while (path != null && path.startsWith("/")) {
			path = path.substring(1);
		}
		GroupConfig groupConfig = hazelcastConfig.getGroupConfig();
		if (groupConfig != null) {
			groupConfig.setName(path);
		}
	}
}
