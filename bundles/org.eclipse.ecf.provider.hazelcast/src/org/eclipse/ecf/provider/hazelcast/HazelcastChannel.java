package org.eclipse.ecf.provider.hazelcast;

import java.io.IOException;
import java.io.Serializable;

import javax.jms.JMSException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ecf.core.util.Trace;
import org.eclipse.ecf.remoteservice.util.ObjectSerializationUtil;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class HazelcastChannel {

	private final HazelcastMessageHandler handler;
	private final HazelcastInstance hazelcast;
	private ITopic<byte[]> topic;
	private MessageListener<byte[]> callback = new MessageListener<byte[]>() {
		@Override
		public void onMessage(Message<byte[]> message) {
			Trace.trace("org.eclipse.ecf.provider.hazelcast", "onMessage message=" + message);
			HazelcastMessage m = HazelcastMessage.receive(message.getMessageObject());
			if (m == null) {
				Trace.exiting("org.eclipse.ecf.provider.hazelcast", "exiting", getClass(), "onMessage");
				return;
			} else
				handler.onMessage(m);
		}
	};

	public HazelcastChannel(HazelcastMessageHandler handler, HazelcastInstance instance) {
		this.handler = handler;
		Assert.isNotNull(this.handler);
		this.hazelcast = instance;
		Assert.isNotNull(this.hazelcast);
	}

	synchronized void setupTopic(String topic) {
		this.topic = this.hazelcast.getTopic(topic);
		this.topic.addMessageListener(callback);
	}

	synchronized void shutdown() {
		if (this.topic != null) {
			this.hazelcast.shutdown();
			this.topic = null;
		}
	}

	synchronized boolean isConnected() {
		return (topic != null);
	}

	private static final ObjectSerializationUtil osu = new ObjectSerializationUtil();

	void createAndSendMessage(Serializable object, String jmsCorrelationId) throws JMSException {
		byte[] serializedMessage;
		try {
			serializedMessage = osu.serializeToBytes(object);
		} catch (IOException e) {
			JMSException jmse = new JMSException(e.getMessage());
			jmse.setStackTrace(e.getStackTrace());
			throw jmse;
		}
		synchronized (this) {
			if (this.topic == null)
				throw new JMSException("Not connected");
			HazelcastMessage.send(this.topic, serializedMessage, jmsCorrelationId);
		}
	}

	Object readObject(byte[] bytes) throws IOException, ClassNotFoundException {
		return osu.deserializeFromBytes(bytes);
	}

}
