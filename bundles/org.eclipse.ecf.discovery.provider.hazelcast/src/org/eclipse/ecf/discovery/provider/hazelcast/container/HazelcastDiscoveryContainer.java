/*******************************************************************************
 * Copyright (c) 2019 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.discovery.provider.hazelcast.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.ecf.core.ContainerConnectException;
import org.eclipse.ecf.core.events.ContainerConnectedEvent;
import org.eclipse.ecf.core.events.ContainerConnectingEvent;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.discovery.AbstractDiscoveryContainerAdapter;
import org.eclipse.ecf.discovery.IServiceInfo;
import org.eclipse.ecf.discovery.ServiceContainerEvent;
import org.eclipse.ecf.discovery.ServiceInfo;
import org.eclipse.ecf.discovery.identity.IServiceID;
import org.eclipse.ecf.discovery.identity.IServiceTypeID;
import org.eclipse.ecf.discovery.provider.hazelcast.identity.HazelcastNamespace;
import org.eclipse.ecf.discovery.provider.hazelcast.identity.HazelcastServiceID;

import com.hazelcast.config.Config;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MapEvent;

public class HazelcastDiscoveryContainer extends AbstractDiscoveryContainerAdapter {

	private static final int TTL_DEFAULT = Integer
			.valueOf(System.getProperty(HazelcastDiscoveryContainer.class.getName() + ".ttlDefault", "30"));
	private HazelcastServiceID targetID;
	private HazelcastInstance hazelcastInstance;
	private IMap<String, List<ServiceInfo>> hazelcastMap;

	private Map<String, List<ServiceInfo>> services = Collections
			.synchronizedMap(new HashMap<String, List<ServiceInfo>>());

	private List<ServiceInfo> localServices = Collections.synchronizedList(new ArrayList<ServiceInfo>());

	public HazelcastDiscoveryContainer(HazelcastDiscoveryContainerConfig config) {
		super(HazelcastNamespace.NAME, config);
	}

	@Override
	public IServiceInfo getServiceInfo(IServiceID aServiceID) {
		return flattenServices().filter(s -> {
			return s.getServiceID().equals(aServiceID);
		}).findFirst().get();
	}

	private List<IServiceInfo> getServices0() {
		return flattenServices().collect(Collectors.toList());
	}

	@Override
	public IServiceInfo[] getServices() {
		List<IServiceInfo> results = getServices0();
		return results.toArray(new IServiceInfo[results.size()]);
	}

	private Stream<IServiceInfo> flattenServices() {
		return services.values().stream().flatMap(List::stream);
	}

	private IServiceInfo[] filterServicesByTypeID(IServiceTypeID aServiceTypeID) {
		List<IServiceInfo> results = flattenServices().filter(s -> {
			if (aServiceTypeID != null) {
				return s.getServiceID().getServiceTypeID().equals(aServiceTypeID);
			} else {
				return true;
			}
		}).collect(Collectors.toList());
		return results.toArray(new IServiceInfo[results.size()]);
	}

	@Override
	public IServiceInfo[] getServices(IServiceTypeID aServiceTypeID) {
		return filterServicesByTypeID(aServiceTypeID);
	}

	@Override
	public IServiceTypeID[] getServiceTypes() {
		List<IServiceTypeID> typeIDs = flattenServices().map(s -> s.getServiceID().getServiceTypeID())
				.collect(Collectors.toList());
		return typeIDs.toArray(new IServiceTypeID[typeIDs.size()]);
	}

	@Override
	public void registerService(IServiceInfo serviceInfo) {
		synchronized (this) {
			if (this.hazelcastInstance == null) {
				System.out.println("hazelcastInstance is null. Cannot register " + serviceInfo);
				return;
			}
			ServiceInfo localServiceInfo = new ServiceInfo(serviceInfo.getLocation(), serviceInfo.getServiceName(),
					serviceInfo.getServiceID().getServiceTypeID(), serviceInfo.getServiceProperties());
			this.localServices.add(localServiceInfo);
			this.hazelcastMap.putTransient(getSessionId(), this.localServices, TTL_DEFAULT, TimeUnit.SECONDS, 0,
					TimeUnit.SECONDS);
		}
	}

	@Override
	public void unregisterService(IServiceInfo serviceInfo) {
		synchronized (this) {
			if (this.hazelcastInstance == null) {
				System.out.println("hazelcastInstance is null. Cannot register " + serviceInfo);
				return;
			}
			ServiceInfo localServiceInfo = new ServiceInfo(serviceInfo.getLocation(), serviceInfo.getServiceName(),
					serviceInfo.getServiceID().getServiceTypeID(), serviceInfo.getServiceProperties());
			if (this.localServices.remove(localServiceInfo)) {
				this.hazelcastMap.putTransient(getSessionId(), this.localServices, TTL_DEFAULT, TimeUnit.SECONDS, 0,
						TimeUnit.SECONDS);
			}
		}
	}

	private HazelcastDiscoveryContainerConfig getHazelcastConfig() {
		return (HazelcastDiscoveryContainerConfig) getConfig();
	}

	private String getSessionId() {
		return hazelcastInstance.getLocalEndpoint().getUuid();
	}

	private EntryListener<String, List<ServiceInfo>> entryListener = new EntryListener<String, List<ServiceInfo>>() {

		@Override
		public void entryAdded(EntryEvent<String, List<ServiceInfo>> event) {
			System.out.println("entryAdded key=" + event.getKey() + ";" + event.getValue());
			addEntry(event);
		}

		@Override
		public void entryUpdated(EntryEvent<String, List<ServiceInfo>> event) {
			System.out.println("entryUpdated key=" + event.getKey() + ";" + event.getValue());
		}

		@Override
		public void entryRemoved(EntryEvent<String, List<ServiceInfo>> event) {
			System.out.println("entryRemoved key=" + event.getKey() + ";" + event.getValue());
			removeEntry(event);
		}

		@Override
		public void entryEvicted(EntryEvent<String, List<ServiceInfo>> event) {
			System.out.println("entryEvicted key=" + event.getKey() + ";" + event.getValue());
			removeEntry(event);
		}

		@Override
		public void mapCleared(MapEvent event) {
			removeAllEntries();
		}

		@Override
		public void mapEvicted(MapEvent event) {
			removeAllEntries();
		}
	};

	protected void removeAllEntries() {
		List<IServiceInfo> sis = null;
		synchronized (services) {
			sis = getServices0();
			services.clear();
		}
		sis.forEach(s -> fireServiceInfoUndiscovered(s));
	}

	protected void addEntry(EntryEvent<String, List<ServiceInfo>> event) {
		String key = event.getKey();
		List<ServiceInfo> sis = event.getValue();
		services.put(key, sis);
		sis.forEach(s -> fireServiceInfoDiscovered(s));
	}

	private void entryUpdated(EntryEvent<String, List<ServiceInfo>> event) {
		event.getOldValue();
	}

	private void removeEntry(EntryEvent<String, List<ServiceInfo>> event) {
		List<ServiceInfo> sis = services.remove(event.getKey());
		if (sis != null) {
			sis.forEach(s -> fireServiceInfoUndiscovered(s));
		}
	}

	private void fireServiceInfoDiscovered(IServiceInfo s) {
		fireServiceUndiscovered(new ServiceContainerEvent(s, getID()));
	}

	private void fireServiceInfoUndiscovered(IServiceInfo s) {
		fireServiceUndiscovered(new ServiceContainerEvent(s, getID()));
	}

	@Override
	public void connect(ID targetID, IConnectContext connectContext) throws ContainerConnectException {
		if (this.targetID != null)
			throw new ContainerConnectException("Already connected"); //$NON-NLS-1$
		HazelcastDiscoveryContainerConfig config = getHazelcastConfig();
		if (config == null)
			throw new ContainerConnectException("Container has been disposed"); //$NON-NLS-1$

		fireContainerEvent(new ContainerConnectingEvent(getID(), targetID, connectContext));

		// set targetID from config
		if (targetID == null) {
			this.targetID = config.getTargetID();
		} else {
			if (!(targetID instanceof HazelcastServiceID))
				throw new ContainerConnectException("targetID must be of type HazelcastServiceID"); //$NON-NLS-1$
			targetID = (HazelcastServiceID) targetID;
		}
		// Prepare hazelcast config with context class loader and entry listener
		Config hazelcastConfig = config.getHazelcastConfig();
		MapConfig mapConfig = hazelcastConfig.getMapConfig("default");
		mapConfig.addEntryListenerConfig(new EntryListenerConfig(entryListener, true, true));
		ClassLoader ccl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

		synchronized (this) {
			try {
				this.hazelcastInstance = Hazelcast.newHazelcastInstance(config.getHazelcastConfig());
				this.hazelcastMap = this.hazelcastInstance.getMap("default");
			} finally {
				Thread.currentThread().setContextClassLoader(ccl);
			}
		}
		fireContainerEvent(new ContainerConnectedEvent(getID(), targetID));
	}

	@Override
	public ID getConnectedID() {
		return targetID;
	}

	@Override
	public void disconnect() {
		synchronized (this) {
			if (this.hazelcastInstance != null) {
				hazelcastInstance.shutdown();
				this.hazelcastInstance = null;
				this.hazelcastMap = null;
				this.targetID = null;
			}
		}
	}

	@Override
	public String getContainerName() {
		return HazelcastDiscoveryContainerInstantiator.NAME;
	}

}
