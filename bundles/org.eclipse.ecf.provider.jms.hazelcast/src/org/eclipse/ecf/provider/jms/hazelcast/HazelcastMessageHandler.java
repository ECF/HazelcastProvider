package org.eclipse.ecf.provider.jms.hazelcast;

public interface HazelcastMessageHandler {
	public void onMessage(HazelcastMessage message);
}
