package org.eclipse.ecf.discovery.provider.hazelcast.container;

import java.io.Serializable;
import java.net.URI;

import org.eclipse.ecf.discovery.IServiceProperties;
import org.eclipse.ecf.discovery.ServiceInfo;
import org.eclipse.ecf.discovery.identity.IServiceTypeID;

public class HazelcastServiceInfo extends ServiceInfo implements Serializable {

	private static final long serialVersionUID = -4332317254270840177L;
	private String hazelcastId;

	public HazelcastServiceInfo(String hazelcastId, URI anURI, String aServiceName, IServiceTypeID aServiceTypeID,
			IServiceProperties props) {
		super(anURI, aServiceName, aServiceTypeID, props);
		this.hazelcastId = hazelcastId;
	}

	public String getHazelcastId() {
		return this.hazelcastId;
	}

	public String getKey() {
		return this.getLocation().toString();
	}
}