/*******************************************************************************
 * Copyright (c) 2019 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.discovery.provider.hazelcast.identity;

import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.discovery.identity.IServiceTypeID;
import org.eclipse.ecf.discovery.identity.ServiceTypeID;

public class HazelcastServiceTypeID extends ServiceTypeID {

	private static final long serialVersionUID = -3942572308397400846L;

	public HazelcastServiceTypeID(Namespace ns, IServiceTypeID id) {
		super(ns, id);
	}

	public HazelcastServiceTypeID(Namespace namespace, String aType) {
		super(namespace, aType);
	}

}
