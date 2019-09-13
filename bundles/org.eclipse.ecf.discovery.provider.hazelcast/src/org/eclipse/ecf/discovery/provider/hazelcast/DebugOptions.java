/*******************************************************************************
 * Copyright (c) 2019 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.discovery.provider.hazelcast;

public interface DebugOptions {

	public static final String DEBUG = Activator.PLUGIN_ID + "/debug"; //$NON-NLS-1$

	public static final String EXCEPTIONS_CATCHING = DEBUG + "/exceptions/catching"; //$NON-NLS-1$

	public static final String EXCEPTIONS_THROWING = DEBUG + "/exceptions/throwing"; //$NON-NLS-1$

	public static final String METHODS_ENTERING = DEBUG + "/methods/entering"; //$NON-NLS-1$

	public static final String METHODS_EXITING = DEBUG + "/methods/exiting"; //$NON-NLS-1$

	public static final String PROTOCOL = DEBUG + "/protocol"; //$NON-NLS-1$

	public static final String WATCHJOB = DEBUG + "/watchjob"; //$NON-NLS-1$

	public static final String TTLJOB = DEBUG + "/ttljob"; //$NON-NLS-1$

}
