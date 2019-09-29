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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.ecf.core.ContainerConnectException;
import org.eclipse.ecf.core.events.ContainerConnectedEvent;
import org.eclipse.ecf.core.events.ContainerConnectingEvent;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.discovery.AbstractDiscoveryContainerAdapter;
import org.eclipse.ecf.discovery.IServiceInfo;
import org.eclipse.ecf.discovery.IServiceListener;
import org.eclipse.ecf.discovery.IServiceTypeListener;
import org.eclipse.ecf.discovery.ServiceContainerEvent;
import org.eclipse.ecf.discovery.identity.IServiceID;
import org.eclipse.ecf.discovery.identity.IServiceTypeID;
import org.eclipse.ecf.discovery.provider.hazelcast.Activator;
import org.eclipse.ecf.discovery.provider.hazelcast.DebugOptions;
import org.eclipse.ecf.discovery.provider.hazelcast.LogUtility;
import org.eclipse.ecf.discovery.provider.hazelcast.identity.HazelcastNamespace;
import org.eclipse.ecf.discovery.provider.hazelcast.identity.HazelcastServiceID;

import com.hazelcast.config.Config;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MapEvent;
import com.hazelcast.core.MembershipAdapter;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.core.ReplicatedMap;
import com.hazelcast.osgi.HazelcastOSGiService;

public class HazelcastDiscoveryContainer extends AbstractDiscoveryContainerAdapter {

	private static final String DEFAULT_MAP_NAME = System
			.getProperty(HazelcastDiscoveryContainer.class.getName() + ".defaultMapName", "default");

	// Hazelcast targetID (from HazelcastDiscoveryContainerConfig
	private HazelcastServiceID targetID;
	// Hazelcast instance
	private HazelcastInstance hazelcastInstance;
	// ReplicatedMap of serviceLocation (id) -> HazelcastServiceInfo
	private ReplicatedMap<String, HazelcastServiceInfo> hazelcastReplicatedMap;
	private Map<String, HazelcastServiceInfo> services;
	
	// Entry listener for replicated map.  Set on to replicated map in connect
	private EntryListener<String, HazelcastServiceInfo> entryListener = new EntryListener<String, HazelcastServiceInfo>() {

		@Override
		public void entryAdded(EntryEvent<String, HazelcastServiceInfo> event) {
			trace("entryAdded", "key=" + event.getKey() + ";" + event.getValue());
			HazelcastServiceInfo si = event.getValue();
			boolean fire = false;
			synchronized (services) {
				String key = si.getKey();
				if (!services.containsKey(key)) {
					trace("entryAdded","adding to services");
					services.put(key, si);
					fire = initialized;
				}
			}
			if (fire) {
				fireServiceDiscovered(si);
			}
		}

		@Override
		public void entryUpdated(EntryEvent<String, HazelcastServiceInfo> event) {
			trace("entryUpdated", "key=" + event.getKey() + ";" + event.getValue());
		}

		@Override
		public void entryRemoved(EntryEvent<String, HazelcastServiceInfo> event) {
			trace("entryRemoved", "key=" + event.getKey() + ";" + event.getOldValue());
			fireServiceInfoUndiscovered(event.getKey());
		}

		@Override
		public void entryEvicted(EntryEvent<String, HazelcastServiceInfo> event) {
			trace("entryEvicted", "key=" + event.getKey() + ";" + event.getOldValue());
			fireServiceInfoUndiscovered(event.getKey());
		}

		@Override
		public void mapCleared(MapEvent event) {
			trace("mapCleared", "name=" + event.getName());
			removeAllServiceInfos();
		}

		@Override
		public void mapEvicted(MapEvent event) {
			trace("mapEvicted", "name=" + event.getName());
			removeAllServiceInfos();
		}
	};

	// Membership changes in group  Responds by adding or removing service infos
	// for all service infos provided by member
	private MembershipListener membershipListener = new MembershipAdapter() {

		@Override
		public void memberAdded(MembershipEvent membershipEvent) {
			trace("memberAdded", "memberId=" + membershipEvent.getMember().getUuid());
			if (!membershipEvent.getMember().getUuid().equals(getMemberId())) {
				addServiceInfoForMember(membershipEvent.getMember().getUuid());
			}
		};

		@Override
		public void memberRemoved(MembershipEvent membershipEvent) {
			trace("memberRemoved", "memberId=" + membershipEvent.getMember().getUuid());
			removeServiceInfosForMember(membershipEvent.getMember().getUuid());
		}
	};

	// If we haven't been completely initialized (a listener has been added), then
	// we hold onto the notification until initialNotify called
	private boolean initialized = false;

	private void initialNotify() {
		List<HazelcastServiceInfo> notify = null;
		synchronized (services) {
			if (!initialized) {
				notify = new ArrayList<HazelcastServiceInfo>(this.services.values());
				initialized = true;
			}
		}
		if (notify != null) {
			notify.forEach(s -> fireServiceDiscovered(s));
		}
	}

	public HazelcastDiscoveryContainer(HazelcastDiscoveryContainerConfig config) {
		super(HazelcastNamespace.NAME, config);
		// Create locally synchronized map for multi-threaded local access
		this.services = Collections.synchronizedMap(new HashMap<String,HazelcastServiceInfo>());
	}

	@Override
	public void registerService(IServiceInfo serviceInfo) {
		synchronized (services) {
			HazelcastServiceInfo localServiceInfo = createHazelcastServiceInfo(serviceInfo);
			this.hazelcastReplicatedMap.put(localServiceInfo.getKey(), localServiceInfo);
		}
	}

	@Override
	public void unregisterService(IServiceInfo serviceInfo) {
		synchronized (services) {
			this.hazelcastReplicatedMap.remove(serviceInfo.getLocation().toString());
		}
	}

	@Override
	public void connect(ID targetID, IConnectContext connectContext) throws ContainerConnectException {
		if (this.targetID != null)
			throw new ContainerConnectException("Already connected"); //$NON-NLS-1$
		HazelcastDiscoveryContainerConfig config = getHazelcastConfig();
		if (config == null)
			throw new ContainerConnectException("Container has been disposed"); //$NON-NLS-1$
		// Fire connecting event
		fireContainerEvent(new ContainerConnectingEvent(getID(), targetID, connectContext));

		// set targetID from config
		if (targetID == null) {
			this.targetID = config.getTargetID();
		} else {
			if (!(targetID instanceof HazelcastServiceID))
				throw new ContainerConnectException("targetID must be of type HazelcastServiceID"); //$NON-NLS-1$
			targetID = (HazelcastServiceID) targetID;
		}
		HazelcastOSGiService hazelcastOSGiService = Activator.getDefault().getHazelcastOSGiService();
		if (hazelcastOSGiService == null) {
			throw new ContainerConnectException("Cannot get HazelcastOSGiService to create Hazelcast discovery");
		}

		Config hazelcastConfig = config.getHazelcastConfig();
		// Prepare hazelcast config to add entry listener
		if (hazelcastConfig != null) {
			hazelcastConfig.getReplicatedMapConfig(DEFAULT_MAP_NAME)
					.addEntryListenerConfig(new EntryListenerConfig(entryListener, true, true));
		}

		synchronized (services) {
			try {
				this.hazelcastInstance = (hazelcastConfig == null) ? hazelcastOSGiService.newHazelcastInstance()
						: hazelcastOSGiService.newHazelcastInstance(hazelcastConfig);
				this.hazelcastInstance.getCluster().addMembershipListener(membershipListener);
				this.hazelcastReplicatedMap = this.hazelcastInstance.getReplicatedMap(DEFAULT_MAP_NAME);
				// Copy all existing entries to the local cache
				services.putAll(this.hazelcastReplicatedMap);
			} catch (Exception e) {
				throw new ContainerConnectException(
						"Could not create hazelcast instance with hazelcastConfig=" + hazelcastConfig);
			}
		}
		// Fire connected event
		fireContainerEvent(new ContainerConnectedEvent(getID(), targetID));
	}

	@Override
	public ID getConnectedID() {
		return targetID;
	}


	@Override
	public void disconnect() {
		trace("disconnect", "disconnecting from hazelcast group");
		synchronized (services) {
			if (this.hazelcastInstance != null) {
				this.hazelcastInstance.shutdown();
				this.hazelcastInstance = null;
				this.targetID = null;
				this.initialized = false;
			}
			services.clear();
		}
	}

	@Override
	public String getContainerName() {
		return HazelcastDiscoveryContainerInstantiator.NAME;
	}

	@Override
	public IServiceInfo getServiceInfo(IServiceID aServiceID) {
		return servicesStream().filter(s -> {
			return s.getServiceID().equals(aServiceID);
		}).findFirst().get();
	}

	private List<IServiceInfo> getServices0() {
		return servicesStream().collect(Collectors.toList());
	}

	@Override
	public IServiceInfo[] getServices() {
		List<IServiceInfo> results = getServices0();
		return results.toArray(new IServiceInfo[results.size()]);
	}

	private Stream<HazelcastServiceInfo> servicesStream() {
		return services.values().stream();
	}

	private IServiceInfo[] filterServicesByTypeID(IServiceTypeID aServiceTypeID) {
		List<IServiceInfo> results = servicesStream().filter(s -> {
			return (aServiceTypeID != null) ? s.getServiceID().getServiceTypeID().equals(aServiceTypeID) : true;
		}).collect(Collectors.toList());
		return results.toArray(new IServiceInfo[results.size()]);
	}

	@Override
	public IServiceInfo[] getServices(IServiceTypeID aServiceTypeID) {
		return filterServicesByTypeID(aServiceTypeID);
	}

	@Override
	public IServiceTypeID[] getServiceTypes() {
		List<IServiceTypeID> typeIDs = servicesStream().map(s -> s.getServiceID().getServiceTypeID())
				.collect(Collectors.toList());
		return typeIDs.toArray(new IServiceTypeID[typeIDs.size()]);
	}

	private HazelcastServiceInfo createHazelcastServiceInfo(IServiceInfo serviceInfo) {
		return new HazelcastServiceInfo(getMemberId(), serviceInfo.getLocation(), serviceInfo.getServiceName(),
				serviceInfo.getServiceID().getServiceTypeID(), serviceInfo.getServiceProperties());
	}

	private void trace(String methodName, String message) {
		LogUtility.trace(methodName, DebugOptions.DEBUG, getClass(), message);
	}

	private HazelcastDiscoveryContainerConfig getHazelcastConfig() {
		return (HazelcastDiscoveryContainerConfig) getConfig();
	}

	private String getMemberId() {
		return hazelcastInstance.getLocalEndpoint().getUuid();
	}

	private void fireServiceInfoUndiscovered(String siKey) {
		HazelcastServiceInfo si = services.remove(siKey);
		if (si != null) {
			fireServiceUndiscovered(new ServiceContainerEvent(si, getID()));
		}
	}

	private void fireServiceDiscovered(HazelcastServiceInfo si) {
		trace("fireServiceDiscovered", "hazelcastserviceinfo=" + si);
		fireServiceDiscovered(new ServiceContainerEvent(si, getID()));
	}

	@Override
	public void addServiceListener(IServiceListener aListener) {
		super.addServiceListener(aListener);
		initialNotify();
	}

	@Override
	public void addServiceListener(IServiceTypeID aType, IServiceListener aListener) {
		super.addServiceListener(aType, aListener);
		initialNotify();
	}

	@Override
	public void addServiceTypeListener(IServiceTypeListener aListener) {
		super.addServiceTypeListener(aListener);
		initialNotify();
	}

	private void removeAllServiceInfos() {
		removeServiceInfosForMember(null);
	}

	private void addServiceInfoForMember(String member) {
		trace("addServiceInfoForMember", "member=" + member);
		
		Map<String, HazelcastServiceInfo> addedServices = new HashMap<String, HazelcastServiceInfo>();
		boolean fire = false;
		// No changes to services while happening
		synchronized (services) {
			// Iterate through all replicated map values
			for (HazelcastServiceInfo si : this.hazelcastReplicatedMap.values()) {
				// If the new member has a service info then
				if (si.getMemberId().equals(member)) {
					// get key
					String key = si.getKey();
					// If key not already in services
					if (!services.containsKey(key)) {
						// then add it
						trace("addServiceInfoForMember","adding key="+key);
						services.put(key, si);
						// and add to addedServices to notify if we've already been initialized
						addedServices.put(key, si);
					}
				}
			}
			fire = initialized;
		}
		if (fire) {
			addedServices.values().forEach(si -> fireServiceDiscovered(si));
		}
	}

	private void removeServiceInfosForMember(String member) {
		trace("removeServiceInfosForMember", "member=" + member);
		List<HazelcastServiceInfo> removedServices = null;
		synchronized (services) {
			removedServices = services.values().stream().filter(si -> {
				return (member == null) ? true : member.equals(si.getMemberId());
			}).collect(Collectors.toList());
			for (HazelcastServiceInfo si : removedServices) {
				services.remove(si.getKey());
			}
		}
		// Now notify about those that actually are removed
		removedServices.forEach(si -> fireServiceUndiscovered(new ServiceContainerEvent(si, getID())));
	}

}
