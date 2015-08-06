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

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.tests.osgi.services.distribution.AbstractRemoteServiceRegisterTest;
import org.eclipse.ecf.tests.provider.hazelcast.Hazelcast;

public class HazelcastRemoteServiceRegisterTest extends AbstractRemoteServiceRegisterTest {

	protected String getServerContainerTypeName() {
		return Hazelcast.SERVER_CONTAINER_NAME;
	}

	protected String getClientContainerName() {
		return Hazelcast.CLIENT_CONTAINER_NAME;
	}

	protected ID createServerID() throws Exception {
		return IDFactory.getDefault().createID(Hazelcast.NAMESPACE_NAME, Hazelcast.TARGET_NAME);
	}

	protected String getServerIdentity() {
		return Hazelcast.TARGET_NAME;
	}

}
