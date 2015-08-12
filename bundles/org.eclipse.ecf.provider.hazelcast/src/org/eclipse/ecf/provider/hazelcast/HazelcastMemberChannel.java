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

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.provider.comm.ConnectionEvent;
import org.eclipse.ecf.provider.comm.ISynchAsynchEventHandler;
import org.eclipse.ecf.provider.jms.channel.AbstractJMSClientChannel;
import org.eclipse.ecf.provider.jms.identity.JMSID;

import com.hazelcast.core.HazelcastInstance;

public class HazelcastMemberChannel extends AbstractJMSClientChannel {

	private static final long serialVersionUID = -4250141332659030158L;

	private final HazelcastChannel channel;

	public HazelcastMemberChannel(ISynchAsynchEventHandler handler, HazelcastInstance hazelcast) {
		super(handler, 30000);
		this.channel = new HazelcastChannel(new HazelcastMessageHandler() {
			@Override
			public void onMessage(HazelcastMessage message) {
				handleMessage(message.getData(), message.getCorrelationId());
			}
		}, hazelcast);
	}

	public synchronized boolean isConnected() {
		return (this.channel != null && this.channel.isConnected());
	}

	@Override
	protected Serializable setupJMS(JMSID targetID, Object data) throws ECFException {
		this.channel.setupTopic(targetID.getTopicOrQueueName());
		return (Serializable) data;
	}

	@Override
	protected void createAndSendMessage(Serializable object, String jmsCorrelationId) throws JMSException {
		this.channel.createAndSendMessage(object, jmsCorrelationId);
	}

	protected Object readObject(byte[] bytes) throws IOException, ClassNotFoundException {
		return this.channel.readObject(bytes);
	}

	@Override
	public synchronized void disconnect() {
		if (isConnected()) {
			this.channel.shutdown();
			synchronized (this.waitResponse) {
				waitResponse.notifyAll();
			}
			fireListenersDisconnect(new ConnectionEvent(this, null));
			connectionListeners.clear();
		}
	}

	@Override
	protected ConnectionFactory createJMSConnectionFactory(JMSID targetID) throws IOException {
		return null;
	}

}
