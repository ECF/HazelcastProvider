package org.eclipse.ecf.provider.hazelcast;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.provider.generic.GenericContainerInstantiator;
import org.eclipse.ecf.provider.jms.identity.JMSID;
import org.eclipse.ecf.provider.jms.identity.JMSNamespace;

import com.hazelcast.config.Config;
import com.hazelcast.config.InMemoryXmlConfig;
import com.hazelcast.config.UrlXmlConfig;
import com.hazelcast.config.XmlConfigBuilder;

public abstract class AbstractHazelcastContainerInstantiator extends GenericContainerInstantiator {
	public static final String DEFAULT_SERVER_ID = "hazelcast://localhost/exampleTopic";

	public static final String ID_PARAM = "id";
	public static final String KEEPALIVE_PARAM = "keepAlive";
	public static final String CONFIG_PARAM = "hazelcastConfig";

	protected static final String[] hazelcastIntents = { "HAZELCAST" };

	protected JMSID getJMSIDFromParameter(Object p) {
		if (p instanceof String) {
			return (JMSID) IDFactory.getDefault().createID(JMSNamespace.NAME, (String) p);
		} else if (p instanceof JMSID) {
			return (JMSID) p;
		} else
			return null;
	}

	public String[] getSupportedIntents(ContainerTypeDescription description) {
		List<String> results = new ArrayList<String>();
		for (int i = 0; i < genericProviderIntents.length; i++)
			results.add(genericProviderIntents[i]);
		for (int i = 0; i < hazelcastIntents.length; i++)
			results.add(hazelcastIntents[i]);
		return (String[]) results.toArray(new String[] {});
	}

	protected Config getConfigFromArg(Object o) throws Exception {
		if (o instanceof Config)
			return (Config) o;
		else if (o instanceof InputStream)
			return new XmlConfigBuilder((InputStream) o).build();
		else if (o instanceof URL)
			return new UrlXmlConfig((URL) o);
		else if (o instanceof String)
			return new InMemoryXmlConfig((String) o);
		return null;
	}

	@SuppressWarnings("rawtypes")
	public IContainer createInstance(ContainerTypeDescription description, Object[] args)
			throws ContainerCreateException {
		try {
			JMSID id = null;
			Integer ka = null;
			Map props = null;
			Config config = null;
			if (args == null)
				id = getJMSIDFromParameter(UUID.randomUUID().toString());
			else if (args.length > 0) {
				if (args[0] instanceof Map) {
					props = (Map) args[0];
					Object o = props.get(ID_PARAM);
					if (o != null && o instanceof String)
						id = getJMSIDFromParameter(o);
					o = props.get(KEEPALIVE_PARAM);
					if (o != null)
						ka = getIntegerFromArg(o);
					o = props.get(CONFIG_PARAM);
					if (o != null)
						config = getConfigFromArg(o);
				} else {
					id = getJMSIDFromParameter(args[0]);
					if (args.length > 1)
						ka = getIntegerFromArg(args[1]);
				}
			}
			if (id == null)
				id = getJMSIDFromParameter(UUID.randomUUID().toString());
			if (ka == null)
				ka = new Integer(HazelcastManagerContainer.DEFAULT_KEEPALIVE);
			return createHazelcastContainer(id, ka, props, config);
		} catch (Exception e) {
			ContainerCreateException t = new ContainerCreateException("Exception creating activemq client container",
					e);
			t.setStackTrace(e.getStackTrace());
			throw t;
		}
	}

	protected abstract IContainer createHazelcastContainer(JMSID id, Integer ka,
			@SuppressWarnings("rawtypes") Map props, Config config) throws Exception;
}
