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
import java.util.Set;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.core.util.Trace;
import org.eclipse.ecf.provider.comm.ConnectionEvent;
import org.eclipse.ecf.provider.comm.ISynchAsynchEventHandler;
import org.eclipse.ecf.provider.jms.channel.AbstractJMSServerChannel;
import org.eclipse.ecf.provider.jms.identity.JMSID;
import org.eclipse.ecf.remoteservice.util.ObjectSerializationUtil;

import com.hazelcast.config.Config;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class HazelcastServerChannel extends AbstractJMSServerChannel {

	private static final long serialVersionUID = 6598998936222020495L;

	private static final int DEFAULT_KEEPALIVE = 20000;

	private HazelcastInstance hazelcast;
	private ITopic<byte[]> topic;

	private MessageListener<byte[]> callback = new MessageListener<byte[]>() {
		@Override
		public void onMessage(Message<byte[]> message) {
			Trace.trace("org.eclipse.ecf.provider.hazelcast", "handleMessageArrived message=" + message);
			HazelcastMessage m = HazelcastMessage.receive(message.getMessageObject());
			if (m == null) {
				Trace.exiting("org.eclipse.ecf.provider.hazelcast", "exiting", getClass(), "handleMessageArrived");
				return;
			} else
				handleMessage(m.getData(), m.getCorrelationId());
		}
	};

	public HazelcastServerChannel(ISynchAsynchEventHandler handler, Map<String, ?> options) throws ECFException {
		super(handler, DEFAULT_KEEPALIVE);
		JMSID targetID = (JMSID) getLocalID();
		try {
			Config config = new Config();
			hazelcast = Hazelcast.newHazelcastInstance(config);
			System.out.println("server.hazelcastname=" + hazelcast.getName());
			Cluster cluster = hazelcast.getCluster();
			Set<Member> members = cluster.getMembers();
			for (Member m : members)
				System.out.println("server.hazelcast member=" + m);

			this.topic = hazelcast.getTopic(targetID.getTopicOrQueueName());
			// Set callback
			this.topic.addMessageListener(callback);
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

	public Client createClient(ID remoteID) {
		Client newclient = new Client(remoteID, false);
		newclient.start();
		return newclient;
	}

	@Override
	public boolean isConnected() {
		return (this.topic != null);
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

	protected Object readObject(byte[] bytes) throws IOException, ClassNotFoundException {
		return osu.deserializeFromBytes(bytes);
	}

	@Override
	protected ConnectionFactory createJMSConnectionFactory(JMSID targetID) throws IOException {
		return null;
	}

}
