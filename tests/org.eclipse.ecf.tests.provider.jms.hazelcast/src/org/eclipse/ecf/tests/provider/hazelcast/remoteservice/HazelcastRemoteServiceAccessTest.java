/*******************************************************************************
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.tests.provider.hazelcast.remoteservice;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ecf.core.ContainerFactory;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.tests.osgi.services.distribution.AbstractRemoteServiceAccessTest;
import org.eclipse.ecf.tests.provider.hazelcast.Hazelcast;

public class HazelcastRemoteServiceAccessTest extends AbstractRemoteServiceAccessTest {

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		setClientCount(1);
		createServerAndClients();
		connectClients();
		setupRemoteServiceAdapters();
	}

	protected void tearDown() throws Exception {
		cleanUpServerAndClients();
		super.tearDown();
	}

	protected String getServerContainerName() {
		return Hazelcast.SERVER_CONTAINER_NAME;
	}

	protected String getClientContainerName() {
		return Hazelcast.CLIENT_CONTAINER_NAME;
	}

	protected ID createServerID() throws Exception {
		return IDFactory.getDefault().createID(Hazelcast.NAMESPACE_NAME, Hazelcast.TARGET_NAME);
	}

	protected IContainer createServer() throws Exception {
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("id", Hazelcast.TARGET_NAME);
		return ContainerFactory.getDefault().createContainer(getServerContainerName(), parameters);
	}


	protected String getServerIdentity() {
		return Hazelcast.TARGET_NAME;
	}
}
