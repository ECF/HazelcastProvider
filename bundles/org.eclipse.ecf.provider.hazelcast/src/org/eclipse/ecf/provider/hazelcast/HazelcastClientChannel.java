/*******************************************************************************
* Copyright (c) 2015 Composent, Inc. and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Composent, Inc. - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.provider.hazelcast;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.core.util.Trace;
import org.eclipse.ecf.provider.comm.ConnectionEvent;
import org.eclipse.ecf.provider.comm.ISynchAsynchEventHandler;
import org.eclipse.ecf.provider.jms.channel.AbstractJMSClientChannel;
import org.eclipse.ecf.provider.jms.identity.JMSID;
import org.eclipse.ecf.remoteservice.util.ObjectSerializationUtil;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class HazelcastClientChannel extends AbstractJMSClientChannel {

	private static final long serialVersionUID = -4250141332659030158L;

	private static final int DEFAULT_KEEPALIVE = 20000;

	private HazelcastInstance hazelcast;
	private ITopic<byte[]> topic;

	private MessageListener<byte[]> callback = new MessageListener<byte[]>() {
		@Override
		public void onMessage(Message<byte[]> message) {
			Trace.trace("org.eclipse.ecf.provider.hazelcast", "handleMessageArrived message=" + message);
			HazelcastMessage m = HazelcastMessage.receive(message.getMessageObject());
			if (m == null) {
				Trace.exiting("org.eclipse.ecf.provider.jms.mqtt", "exiting", this.getClass(), "handleMessageArrived");
				return;
			} else
				handleMessage(m.getData(), m.getCorrelationId());
		}
	};

	public HazelcastClientChannel(ISynchAsynchEventHandler handler, Map<String, ?> options) {
		super(handler, DEFAULT_KEEPALIVE);
	}

	@Override
	public boolean isConnected() {
		return (this.topic != null);
	}

	@Override
	protected Serializable setupJMS(JMSID targetID, Object data) throws ECFException {
		try {
			if (!(data instanceof Serializable))
				throw new ECFException("connect data=" + data + " must be Serializable");
			// TopicConfig topicConfig = new TopicConfig();
			Config config = new Config();
			hazelcast = Hazelcast.newHazelcastInstance(config);
			this.topic = hazelcast.getTopic(targetID.getTopicOrQueueName());
			// Set callback
			this.topic.addMessageListener(callback);
			return (Serializable) data;
		} catch (Exception e) {
			throw new ECFException("Could not connect to targetID=" + targetID.getName());
		}
	}

	private static final ObjectSerializationUtil osu = new ObjectSerializationUtil();

	@Override
	protected void createAndSendMessage(Serializable object, String jmsCorrelationId) throws JMSException {
		byte[] serializedMessage;
		try {
			serializedMessage = osu.serializeToBytes(object);
		} catch (IOException e) {
			JMSException jmse = new JMSException(e.getMessage());
			jmse.setStackTrace(e.getStackTrace());
			throw jmse;
		}
		HazelcastMessage.send(this.topic, serializedMessage, jmsCorrelationId);
	}

	protected Object readObject(byte[] bytes) throws IOException, ClassNotFoundException {
		return osu.deserializeFromBytes(bytes);
	}

	@Override
	public void disconnect() {
		if (this.hazelcast != null) {
			this.hazelcast.shutdown();
			;
			this.topic = null;
		}
		synchronized (this.waitResponse) {
			waitResponse.notifyAll();
		}
		fireListenersDisconnect(new ConnectionEvent(this, null));
		connectionListeners.clear();
	}

	@Override
	protected ConnectionFactory createJMSConnectionFactory(JMSID targetID) throws IOException {
		return null;
	}

}
