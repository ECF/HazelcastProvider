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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.core.util.BundleStarter;
import org.eclipse.ecf.core.util.LogHelper;
import org.eclipse.ecf.core.util.SystemLogService;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.IHostContainerSelector;
import org.eclipse.ecf.provider.jms.hazelcast.HazelcastManagerContainer;
import org.eclipse.ecf.provider.jms.hazelcast.HazelcastMemberContainer;
import org.eclipse.ecf.remoteservice.provider.AdapterConfig;
import org.eclipse.ecf.remoteservice.provider.IRemoteServiceDistributionProvider;
import org.eclipse.ecf.remoteservice.provider.RemoteServiceDistributionProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import com.hazelcast.osgi.HazelcastOSGiService;

public class Activator implements BundleActivator {

	public static final String ID = "org.eclipse.ecf.provider.jms.hazelcast";
	private static final String[] DEPENDENT_BUNDLES = new String[] { "com.hazelcast", "org.eclipse.ecf.provider.jms" };

	public static final String HAZELCAST_PREFIX = "ecf.jms.hazelcast";
	public static final String HAZELCAST_MANAGER_NAME = HAZELCAST_PREFIX + ".manager";
	public static final String HAZELCAST_MEMBER_NAME = HAZELCAST_PREFIX + ".member";
	public static final String HAZELCAST_CLIENT_NAME = HAZELCAST_PREFIX + ".client";
	
	private static Activator instance;
	private static BundleContext context;

	private ServiceTracker<LogService, LogService> logServiceTracker = null;
	private LogService logService = null;

	public static Activator getDefault() {
		return instance;
	}

	@Override
	public void start(final BundleContext context1) throws Exception {
		instance = this;
		context = context1;
		BundleStarter.startDependents(context1, DEPENDENT_BUNDLES, Bundle.RESOLVED | Bundle.STARTING);

		// Register our impl of the IHostContainerSelector service, to override the
		// default RSA one
		context1.registerService(IHostContainerSelector.class,
				new HazelcastHostContainerSelector(new String[] { HAZELCAST_MANAGER_NAME, HAZELCAST_MEMBER_NAME }),
				null);

		// Build and register hazelcast manager distribution provider
		context1.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(HAZELCAST_MANAGER_NAME)
						.setInstantiator(new HazelcastManagerContainer.Instantiator())
						.setDescription("ECF Hazelcast Manager").setServer(true)
						.setAdapterConfig(new AdapterConfig(new HazelcastRemoteServiceContainerAdapterFactory(),
								HazelcastManagerContainer.class))
						.build(),
				null);
		// Build and register hazelcast member distribution provider
		context1.registerService(IRemoteServiceDistributionProvider.class,
				new RemoteServiceDistributionProvider.Builder().setName(HAZELCAST_MEMBER_NAME)
						.setInstantiator(new HazelcastMemberContainer.Instantiator())
						.setDescription("ECF Hazelcast Member").setServer(false)
						.setAdapterConfig(new AdapterConfig(new HazelcastRemoteServiceContainerAdapterFactory(),
								HazelcastMemberContainer.class))
						.build(),
				null);
	}

	private ServiceTracker<HazelcastOSGiService, HazelcastOSGiService> hazelcastTracker;

	public static BundleContext getContext() {
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

	public LogService getLogService() {
		if (logServiceTracker == null) {
			logServiceTracker = new ServiceTracker<LogService, LogService>(context, LogService.class.getName(), null);
			logServiceTracker.open();
		}
		logService = logServiceTracker.getService();
		if (logService == null)
			logService = new SystemLogService(ID);
		return logService;
	}

	@SuppressWarnings("deprecation")
	public void log(IStatus status) {
		if (logService == null)
			logService = getLogService();
		if (logService != null)
			logService.log(null, LogHelper.getLogCode(status), LogHelper.getLogMessage(status), status.getException());
	}

	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, IStatus status) {
		log(sr, LogHelper.getLogCode(status), LogHelper.getLogMessage(status), status.getException());
	}

	@SuppressWarnings("deprecation")
	public void log(@SuppressWarnings("rawtypes") ServiceReference sr, int level, String message, Throwable t) {
		if (logService == null)
			logService = getLogService();
		if (logService != null)
			logService.log(sr, level, message, t);
	}

}
