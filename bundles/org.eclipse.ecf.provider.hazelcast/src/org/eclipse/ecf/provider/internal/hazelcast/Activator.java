package org.eclipse.ecf.provider.internal.hazelcast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.util.AdapterManagerTracker;
import org.eclipse.ecf.core.util.ExtensionRegistryRunnable;
import org.eclipse.ecf.provider.hazelcast.HazelcastClientContainer;
import org.eclipse.ecf.provider.hazelcast.HazelcastServerContainer;
import org.eclipse.ecf.provider.remoteservice.generic.RemoteServiceContainerAdapterFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private BundleContext context;
	
	private List<IAdapterFactory> rscAdapterFactories;

	private static IAdapterManager getAdapterManager(BundleContext ctx) {
		AdapterManagerTracker t = new AdapterManagerTracker(ctx);
		t.open();
		IAdapterManager am = t.getAdapterManager();
		t.close();
		return am;
	}

	@Override
	public void start(final BundleContext context1) throws Exception {
		this.context = context1;
		SafeRunner.run(new ExtensionRegistryRunnable(this.context) {
			protected void runWithoutRegistry() throws Exception {
				context1.registerService(ContainerTypeDescription.class, new ContainerTypeDescription(HazelcastServerContainer.HAZELCAST_MANAGER_NAME, new HazelcastServerContainer.HazelcastServerContainerInstantiator(), "ECF Hazelcast Manager", true, false), null); //$NON-NLS-1$
				context1.registerService(ContainerTypeDescription.class, new ContainerTypeDescription(HazelcastClientContainer.HAZELCAST_CLIENT_NAME, new HazelcastClientContainer.HazelcastClientContainerInstantiator(), "ECF Hazelcast Client", false, true), null); //$NON-NLS-1$
				IAdapterManager am = getAdapterManager(context1);
				if (am != null) {
					rscAdapterFactories = new ArrayList<IAdapterFactory>();
					IAdapterFactory af = new RemoteServiceContainerAdapterFactory();
					am.registerAdapters(af, org.eclipse.ecf.provider.hazelcast.HazelcastClientContainer.class);
					rscAdapterFactories.add(af);
					af = new RemoteServiceContainerAdapterFactory();
					am.registerAdapters(af, org.eclipse.ecf.provider.hazelcast.HazelcastServerContainer.class);
					rscAdapterFactories.add(af);
				}
			}
		});
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (rscAdapterFactories != null) {
			IAdapterManager am = getAdapterManager(this.context);
			if (am != null) 
				for (Iterator<IAdapterFactory> i = rscAdapterFactories.iterator(); i.hasNext();)
					am.unregisterAdapters((IAdapterFactory) i.next());
			rscAdapterFactories = null;
		}
		this.context = null;
	}

}
