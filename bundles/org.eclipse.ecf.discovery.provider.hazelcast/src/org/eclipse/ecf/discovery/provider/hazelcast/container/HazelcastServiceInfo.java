/*******************************************************************************
 * Copyright (c) 2019 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.discovery.provider.hazelcast.container;

import java.io.Serializable;
import java.net.URI;

import org.eclipse.ecf.discovery.IServiceProperties;
import org.eclipse.ecf.discovery.ServiceInfo;
import org.eclipse.ecf.discovery.identity.IServiceTypeID;

public class HazelcastServiceInfo extends ServiceInfo implements Serializable {

	private static final long serialVersionUID = -4332317254270840177L;
	private String memberId;

	public HazelcastServiceInfo(String memberId, URI anURI, String aServiceName, IServiceTypeID aServiceTypeID,
			IServiceProperties props) {
		super(anURI, aServiceName, aServiceTypeID, props);
		this.memberId = memberId;
	}

	public String getMemberId() {
		return this.memberId;
	}

	public String getKey() {
		return this.getLocation().toString();
	}
}