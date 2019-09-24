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
import org.osgi.util.tracker.ServiceTracker;

import com.hazelcast.osgi.HazelcastOSGiService;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.ecf.provider.jms.hazelcast";

	public static final String HAZELCAST_PREFIX = "ecf.jms.hazelcast";
	public static final String HAZELCAST_MANAGER_NAME = HAZELCAST_PREFIX + ".manager";
	public static final String HAZELCAST_MEMBER_NAME = HAZELCAST_PREFIX + ".member";

	private static Activator instance;
	private static BundleContext context;

	public static Activator getDefault() {
		return instance;
	}

	@Override
	public void start(final BundleContext context1) throws Exception {
		instance = this;
		this.context = context1;
		// Build and register hazelcast manager distribution provider
		context1.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(HAZELCAST_MANAGER_NAME)
						.setInstantiator(new HazelcastManagerContainer.Instantiator())
						.setDescription("ECF Hazelcast Manager").setServer(true)
						.setAdapterConfig(new AdapterConfig(new RemoteServiceContainerAdapterFactory(),
								HazelcastManagerContainer.class))
						.build(),
				null);
		// Build and register hazelcast member distribution provider
		context1.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(HAZELCAST_MEMBER_NAME)
						.setInstantiator(new HazelcastMemberContainer.Instantiator())
						.setDescription("ECF Hazelcast Member")
						.setAdapterConfig(new AdapterConfig(new RemoteServiceContainerAdapterFactory(),
								HazelcastMemberContainer.class))
						.build(),
				null);
	}

	private ServiceTracker<HazelcastOSGiService, HazelcastOSGiService> hazelcastTracker;

	private static BundleContext getContext() {
		return context;
	}

	public synchronized HazelcastOSGiService getHazelcastOSGiService() {
		if (hazelcastTracker == null) {
			hazelcastTracker = new ServiceTracker<HazelcastOSGiService, HazelcastOSGiService>(getContext(),
					HazelcastOSGiService.class, null);
			hazelcastTracker.open();
		}
		return hazelcastTracker.getService();
	}

	private synchronized void stopHazelcastOSGiService() {
		if (hazelcastTracker != null) {
			hazelcastTracker.close();
			hazelcastTracker = null;
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		stopHazelcastOSGiService();
		context = null;
		instance = null;
	}

}
