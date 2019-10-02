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
package org.eclipse.ecf.provider.internal.jms.hazelcast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.HostContainerSelector;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.IHostContainerSelector;
import org.eclipse.ecf.remoteservice.IRemoteServiceContainerAdapter;
import org.eclipse.ecf.remoteservice.RemoteServiceContainer;
import org.osgi.framework.ServiceReference;

public class HazelcastHostContainerSelector extends HostContainerSelector implements IHostContainerSelector {

	public HazelcastHostContainerSelector(String[] defaultConfigTypes) {
		super(defaultConfigTypes, true);
	}

	@Override
	protected boolean matchRequireServer(ContainerTypeDescription description) {
		return true;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Collection selectExistingHostContainers(ServiceReference serviceReference,
			Map<String, Object> overridingProperties, String[] serviceExportedInterfaces,
			String[] serviceExportedConfigs, String[] serviceIntents) {
		List results = new ArrayList();
		// Get all existing containers
		IContainer[] containers = getContainers();
		// If nothing there, then return empty array
		if (containers == null || containers.length == 0)
			return results;

		for (int i = 0; i < containers.length; i++) {
			ID cID = containers[i].getID();
			trace("selectExistingHostContainers", "Considering existing container=" + cID); //$NON-NLS-1$ //$NON-NLS-2$
			// Check to make sure it's a rs container adapter. If it's not go
			// onto next one
			IRemoteServiceContainerAdapter adapter = hasRemoteServiceContainerAdapter(containers[i]);
			if (adapter == null) {
				trace("selectExistingHostContainers", //$NON-NLS-1$
						"Existing container=" + cID + " does not implement IRemoteServiceContainerAdapter"); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}
			// Get container type description and intents
			ContainerTypeDescription description = getContainerTypeDescription(containers[i]);
			// If it has no description go onto next
			if (description == null) {
				trace("selectExistingHostContainers", //$NON-NLS-1$
						"Existing container=" + cID + " does not have container type description"); //$NON-NLS-1$ //$NON-NLS-2$
				continue;
			}
			if (matchExistingHostContainer(serviceReference, overridingProperties, containers[i], adapter, description,
					serviceExportedConfigs, serviceIntents)) {
				trace("selectExistingHostContainers", "INCLUDING containerID=" //$NON-NLS-1$ //$NON-NLS-2$
						+ containers[i].getID() + " configs=" //$NON-NLS-1$
						+ ((serviceExportedConfigs == null) ? "null" //$NON-NLS-1$
								: Arrays.asList(serviceExportedConfigs).toString())
						+ " intents=" //$NON-NLS-1$
						+ ((serviceIntents == null) ? "null" //$NON-NLS-1$
								: Arrays.asList(serviceIntents).toString()));
				results.add(new RemoteServiceContainer(containers[i], adapter));
			} else {
				trace("selectExistingHostContainers", "EXCLUDING containerID=" //$NON-NLS-1$ //$NON-NLS-2$
						+ containers[i].getID() + " configs=" //$NON-NLS-1$
						+ ((serviceExportedConfigs == null) ? "null" //$NON-NLS-1$
								: Arrays.asList(serviceExportedConfigs).toString())
						+ " intents=" //$NON-NLS-1$
						+ ((serviceIntents == null) ? "null" //$NON-NLS-1$
								: Arrays.asList(serviceIntents).toString()));
			}
		}
		return results;
	}
}
