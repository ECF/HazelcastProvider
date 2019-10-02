/****************************************************************************
 * Copyright (c) 2019 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/
package org.eclipse.ecf.provider.internal.jms.hazelcast;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.sharedobject.AbstractSharedObjectContainerAdapterFactory;
import org.eclipse.ecf.core.sharedobject.ISharedObject;
import org.eclipse.ecf.core.sharedobject.ISharedObjectContainer;
import org.eclipse.ecf.provider.jms.hazelcast.HazelcastManagerContainer;
import org.eclipse.ecf.provider.jms.hazelcast.HazelcastMemberContainer;
import org.eclipse.ecf.provider.remoteservice.generic.RegistrySharedObject;
import org.eclipse.ecf.remoteservice.IRSAConsumerContainerAdapter;
import org.eclipse.ecf.remoteservice.IRemoteServiceContainerAdapter;

public class HazelcastRemoteServiceContainerAdapterFactory extends AbstractSharedObjectContainerAdapterFactory {

	@SuppressWarnings({ "rawtypes" })
	@Override
	protected ISharedObject createAdapter(ISharedObjectContainer container, Class adapterType, ID adapterID) {
		if (adapterType.equals(IRemoteServiceContainerAdapter.class)) {
			if (container instanceof HazelcastManagerContainer) {
				return new HazelcastManagerRegistrySharedObject((HazelcastManagerContainer) container);
			} else if (container instanceof HazelcastMemberContainer) {

			}
			return new RegistrySharedObject();
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public Class[] getAdapterList() {
		return new Class[] { IRemoteServiceContainerAdapter.class };
	}

	class HazelcastManagerRegistrySharedObject extends RegistrySharedObject {

		private HazelcastManagerContainer hostContainer;

		public HazelcastManagerRegistrySharedObject(HazelcastManagerContainer hostContainer) {
			this.hostContainer = hostContainer;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Object getAdapter(Class adapter) {
			if (adapter.equals(IRSAConsumerContainerAdapter.class)) {
				return hostContainer;
			}
			return super.getAdapter(adapter);
		}
	}

	class HazelcastMemberRegistrySharedObject extends RegistrySharedObject {

		private HazelcastMemberContainer memberContainer;

		public HazelcastMemberRegistrySharedObject(HazelcastMemberContainer memberContainer) {
			this.memberContainer = memberContainer;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Object getAdapter(Class adapter) {
			if (adapter.equals(IRSAConsumerContainerAdapter.class)) {
				return memberContainer;
			}
			return super.getAdapter(adapter);
		}
	}
}
