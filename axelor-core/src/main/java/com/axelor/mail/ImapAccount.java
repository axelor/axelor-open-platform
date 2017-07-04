/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

/**
 * The default implementation of {@link MailAccount} for IMAP/IMAPS accounts.
 *
 */
public class ImapAccount extends AbstractMailAccount {

	/**
	 * Create a new IMAP account.
	 *
	 * @param host
	 *            server hostname
	 * @param port
	 *            server port
	 * @param user
	 *            login name
	 * @param password
	 *            login password
	 */
	public ImapAccount(String host, String port, String user, String password) {
		this(host, port, user, password, null);
	}

	/**
	 * Create a new IMAP/IMAPS account.
	 *
	 * <p>
	 * If the given channel is {@link MailConstants#CHANNEL_SSL} then it will
	 * use <code>imaps</code> protocol over <code>ssl</code>.
	 * </p>
	 *
	 * @param host
	 *            server hostname
	 * @param port
	 *            server port
	 * @param user
	 *            login name
	 * @param password
	 *            login password
	 * @param channel
	 *            encryption channel (ssl, starttls or null)
	 */
	public ImapAccount(String host, String port, String user, String password, String channel) {
		super(MailConstants.CHANNEL_SSL.equals(channel) ? MailConstants.PROTOCOL_IMAPS : MailConstants.PROTOCOL_IMAP,
				host, port, user, password, channel);
	}
}
