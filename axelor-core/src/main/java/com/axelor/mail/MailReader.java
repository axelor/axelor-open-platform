/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.mail;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

import com.google.common.base.Throwables;

/**
 * The {@link MailReader} provides features to read mails.
 * 
 */
public class MailReader {

	private Session session;
	private Store store;

	/**
	 * Create a new instance of {@link MailReader} with the given account.
	 * 
	 * @param account
	 *            the account to use
	 * @throws IllegalArgumentException
	 *             if mail account can't get a {@link Store}
	 */
	public MailReader(MailAccount account) {
		this.session = account.getSession();
		try {
			this.store = this.session.getStore();
		} catch (NoSuchProviderException e) {
			throw new IllegalArgumentException("Invalid mail account.", e);
		}
	}

	/**
	 * Get a {@link Store} object and connect to the store if not connected.
	 * 
	 * @return an instance of {@link Store}
	 * @throws AuthenticationFailedException if authentication fails
	 * @throws MessagingException if other failure
	 */
	public Store getStore() {
		if (store.isConnected()) {
			return store;
		}
		try {
			store.connect();
		} catch (MessagingException e) {
			throw Throwables.propagate(e);
		}
		return store;
	}
}
