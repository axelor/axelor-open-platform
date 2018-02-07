/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.mail;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;

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
	public Store getStore() throws AuthenticationFailedException, MessagingException {
		if (store.isConnected()) {
			return store;
		}
		store.connect();
		return store;
	}
}
