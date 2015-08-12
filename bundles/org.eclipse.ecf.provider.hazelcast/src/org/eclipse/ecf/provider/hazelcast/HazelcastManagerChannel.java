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

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.provider.comm.ConnectionEvent;
import org.eclipse.ecf.provider.comm.ISynchAsynchEventHandler;
import org.eclipse.ecf.provider.jms.channel.AbstractJMSServerChannel;
import org.eclipse.ecf.provider.jms.identity.JMSID;

import com.hazelcast.core.HazelcastInstance;

public class HazelcastManagerChannel extends AbstractJMSServerChannel {

	private static final long serialVersionUID = 6598998936222020495L;

	private final HazelcastChannel channel;

	public HazelcastManagerChannel(ISynchAsynchEventHandler handler, HazelcastInstance hazelcast) throws ECFException {
		super(handler, 30000);
		this.channel = new HazelcastChannel(new HazelcastMessageHandler() {
			@Override
			public void onMessage(HazelcastMessage message) {
				handleMessage(message.getData(), message.getCorrelationId());
			}
		}, hazelcast);
		this.channel.setupTopic(((JMSID) getLocalID()).getTopicOrQueueName());
	}

	@Override
	protected void createAndSendMessage(Serializable object, String jmsCorrelationId) throws JMSException {
		this.channel.createAndSendMessage(object, jmsCorrelationId);
	}

	public Client createClient(ID remoteID) {
		Client newclient = new Client(remoteID, false);
		newclient.start();
		return newclient;
	}

	@Override
	public synchronized boolean isConnected() {
		return (this.channel != null && this.channel.isConnected());
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

	protected synchronized Object readObject(byte[] bytes) throws IOException, ClassNotFoundException {
		return this.channel.readObject(bytes);
	}

	@Override
	protected ConnectionFactory createJMSConnectionFactory(JMSID targetID) throws IOException {
		return null;
	}

}
