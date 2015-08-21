/****************************************************************************
 * Copyright (c) 2015 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/
package org.eclipse.ecf.provider.internal.jms.hazelcast;

import org.eclipse.ecf.provider.jms.hazelcast.HazelcastManagerContainer;
import org.eclipse.ecf.provider.jms.hazelcast.HazelcastMemberContainer;
import org.eclipse.ecf.provider.remoteservice.generic.RemoteServiceContainerAdapterFactory;
import org.eclipse.ecf.remoteservice.provider.AdapterConfig;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceDistributionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.ecf.provider.jms.hazelcast";

	@Override
	public void start(final BundleContext context1) throws Exception {
		// Build and register hazelcast manager distribution provider
		context1.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder()
						.setName(HazelcastManagerContainer.HAZELCAST_MANAGER_NAME)
						.setInstantiator(new HazelcastManagerContainer.Instantiator())
						.setDescription("ECF Hazelcast Manager").setServer(true)
						.setAdapterConfig(new AdapterConfig(new RemoteServiceContainerAdapterFactory(),
								HazelcastManagerContainer.class))
						.build(),
				null);
		// Build and register hazelcast member distribution provider
		context1.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(HazelcastMemberContainer.HAZELCAST_MEMBER_NAME)
						.setInstantiator(new HazelcastMemberContainer.Instantiator())
						.setDescription("ECF Hazelcast Member")
						.setAdapterConfig(new AdapterConfig(new RemoteServiceContainerAdapterFactory(),
								HazelcastMemberContainer.class))
						.build(),
				null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}

}
