package org.eclipse.ecf.provider.internal.jms.hazelcast;

import java.util.Map;

import org.eclipse.ecf.core.ContainerConnectException;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription;
import org.eclipse.ecf.remoteservice.IRemoteServiceContainerAdapter;
import org.eclipse.ecf.remoteservice.IRemoteServiceReference;
import org.osgi.framework.InvalidSyntaxException;

public class HazelcastImportHelper {

	public IRemoteServiceReference[] getRemoteServiceReferences(IRemoteServiceContainerAdapter adapter,
			ID connectTarget, Map<String, Object> endpointDescriptionProperties)
			throws ContainerConnectException, InvalidSyntaxException {
		EndpointDescription ed = new EndpointDescription(endpointDescriptionProperties);
		Long rsId = ed.getRemoteServiceId();
		String filter = new StringBuffer("(&(") //$NON-NLS-1$
				.append(org.eclipse.ecf.remoteservice.Constants.SERVICE_ID).append("=").append(rsId).append(")") //$NON-NLS-1$ //$NON-NLS-2$
				.append(")").toString();
		if (connectTarget == null) {
			connectTarget = ed.getConnectTargetID();
		}
		return adapter.getRemoteServiceReferences(connectTarget, new ID[] { ed.getContainerID() },
				ed.getInterfaces().iterator().next(), filter);
	}

}
