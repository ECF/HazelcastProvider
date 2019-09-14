/*******************************************************************************
 * Copyright (c) 2019 Composent, Inc. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Scott Lewis - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.discovery.provider.hazelcast.identity;

import java.net.URI;

import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDCreateException;
import org.eclipse.ecf.core.identity.Namespace;
import org.eclipse.ecf.discovery.identity.IServiceID;
import org.eclipse.ecf.discovery.identity.IServiceTypeID;
import org.eclipse.ecf.discovery.identity.ServiceTypeID;

public class HazelcastNamespace extends Namespace {

	private static final long serialVersionUID = 1632329846237549054L;

	public static final String SCHEME = "hazelcast"; //$NON-NLS-1$
	public static final String NAME = "ecf.namespace.hazelcast"; //$NON-NLS-1$

	public static HazelcastNamespace INSTANCE;

	public HazelcastNamespace() {
		super(NAME, "Hazelcast Discovery Namespace"); //$NON-NLS-1$
		INSTANCE = this;
	}

	@Override
	public ID createInstance(Object[] parameters) throws IDCreateException {
		// error case
		if (parameters == null || parameters.length < 1 || parameters.length > 2) {
			throw new IDCreateException("Parameters cannot be null and must be of length 1 or 2"); //$NON-NLS-1$

			// error case
		} else if (parameters[0] == null || parameters[0].equals("")) { //$NON-NLS-1$
			throw new IDCreateException("First parameter cannot be null or empty String"); //$NON-NLS-1$

			// conversion call where conversion isn't necessary
		} else if (parameters.length == 1 && parameters[0] instanceof HazelcastServiceID) {
			return (ID) parameters[0];

			// convert from IServiceID to IServiceTypeID, String
		} else if (parameters.length == 1 && parameters[0] instanceof IServiceID) {
			final IServiceID anId = (IServiceID) parameters[0];
			final Object[] newParams = new Object[2];
			newParams[0] = anId.getServiceTypeID();
			newParams[1] = anId.getName();
			return createInstance(newParams);

			// create by ECF discovery generic IServiceTypeID (but not
			// JSLPServiceID!!!)
		} else if (parameters[0] instanceof IServiceTypeID) {
			final IServiceTypeID stid = (IServiceTypeID) parameters[0];
			parameters[0] = stid.getName();
			return createInstance(parameters);

			// create IServiceID by ECF discovery generic String representation
		} else if (parameters.length == 2 && parameters[0] instanceof String && ((String) parameters[0]).startsWith("_") //$NON-NLS-1$
				&& parameters[1] instanceof URI) {
			final String type = (String) parameters[0];
			final URI anURI = (URI) parameters[1];
			final HazelcastServiceTypeID serviceType = new HazelcastServiceTypeID(this, new ServiceTypeID(this, type));
			return new HazelcastServiceID(this, serviceType, anURI);

			// create IServiceTypeID by ECF discovery generic ServiceType
		} else if (parameters.length == 1 && parameters[0] instanceof String
				&& ((String) parameters[0]).startsWith("_")) { //$NON-NLS-1$
			final String type = (String) parameters[0];
			return new HazelcastServiceTypeID(this, new ServiceTypeID(this, type));

			// error case second parameter not a String
		} else if (parameters.length == 2 && parameters[1] != null && !(parameters[1] instanceof String)) {
			throw new IDCreateException("Second parameter must be of type String"); //$NON-NLS-1$

			// error case
		} else {
			throw new IDCreateException("Wrong EtcdServiceID creation parameters"); //$NON-NLS-1$
		}
	}

	@Override
	public String getScheme() {
		return SCHEME;
	}

	@Override
	public Class<?>[][] getSupportedParameterTypes() {
		return new Class<?>[][] { { HazelcastServiceID.class }, { IServiceTypeID.class }, { String.class },
				{ String.class, URI.class }, {} };
	}
}
