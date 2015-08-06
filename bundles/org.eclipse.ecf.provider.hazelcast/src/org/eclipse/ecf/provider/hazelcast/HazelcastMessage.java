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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.jms.JMSException;

import org.eclipse.ecf.core.util.Trace;

import com.hazelcast.core.ITopic;

public class HazelcastMessage {

	private byte[] data;
	private String correlationId;

	private HazelcastMessage(byte[] bytes) throws IOException, ClassNotFoundException {
		ObjectInputStream oos = new ObjectInputStream(
				new ByteArrayInputStream(bytes, ECFPREFIX.length, bytes.length - ECFPREFIX.length));
		this.correlationId = (String) oos.readObject();
		this.data = (byte[]) oos.readObject();
	}

	public String getCorrelationId() {
		return this.correlationId;
	}

	public byte[] getData() {
		return data;
	}

	public static void send(final ITopic<byte[]> client, byte[] message, String jmsCorrelationId) throws JMSException {
		try {
			ByteArrayOutputStream bouts = new ByteArrayOutputStream();
			bouts.write(ECFPREFIX);
			ObjectOutputStream oos = new ObjectOutputStream(bouts);
			oos.writeObject(jmsCorrelationId);
			oos.writeObject(message);
			client.publish(bouts.toByteArray());
		} catch (Exception e) {
			JMSException t = new JMSException(e.getMessage());
			t.setStackTrace(e.getStackTrace());
			throw t;
		}

	}

	private static byte[] ECFPREFIX = { 27, 69, 67, 70 };

	public static HazelcastMessage receive(byte[] bytes) {
		// Check the first four bytes
		if (!checkMessagePrefix(bytes))
			return null;
		// else it's an ECF message
		try {
			return new HazelcastMessage(bytes);
		} catch (IOException e) {
			Trace.throwing("org.eclipse.ecf.provider.hazelcast", "throwing", HazelcastMessage.class, "deserialize", e);
			return null;
		} catch (ClassNotFoundException e) {
			Trace.throwing("org.eclipse.ecf.provider.hazelcast", "throwing", HazelcastMessage.class, "deserialize", e);
			return null;
		}
	}

	private static boolean checkMessagePrefix(byte[] bytes) {
		for (int i = 0; i < ECFPREFIX.length; i++)
			if (ECFPREFIX[i] != bytes[i])
				return false;
		return true;
	}
}
