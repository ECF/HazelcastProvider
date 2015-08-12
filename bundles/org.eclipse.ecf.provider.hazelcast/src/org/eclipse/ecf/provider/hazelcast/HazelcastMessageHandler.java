package org.eclipse.ecf.provider.hazelcast;

public interface HazelcastMessageHandler {
	public void onMessage(HazelcastMessage message);
}
