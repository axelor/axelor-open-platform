/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2014 Axelor (<http://axelor.com>).
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

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import com.axelor.common.StringUtils;

/**
 * The default implementation of {@link MailAccount} for IMAPS accounts.
 *
 */
public class ImapsAccount implements MailAccount {
	
	private String host;
	private String port;
	private String user;
	private String password;
	
	private int connectionTimeout = DEFAULT_TIMEOUT;
	private int timeout = DEFAULT_TIMEOUT;

	private Properties properties;

	private Session session;

	public ImapsAccount(String host, String port, String user, String password) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
	}
	
	private Session init() {

		final Properties props = new Properties();

		props.setProperty("mail.imaps.connectiontimeout", "" + DEFAULT_TIMEOUT);
		props.setProperty("mail.imaps.timeout", "" + DEFAULT_TIMEOUT);

		if (properties != null) {
			props.putAll(properties);
		}

		props.setProperty("mail.store.protocol", "imaps");
		props.setProperty("mail.imaps.host", host);
		if (!StringUtils.isBlank(port)) {
			props.setProperty("mail.imaps.port", port);
		}

		// respect manually set timeout
		if (connectionTimeout != DEFAULT_TIMEOUT) {
			props.setProperty("mail.imaps.connectiontimeout", "" + connectionTimeout);
		}
		if (timeout != DEFAULT_TIMEOUT) {
			props.setProperty("mail.imaps.timeout", "" + timeout);
		}

		final Authenticator authenticator = new Authenticator() {
			
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(user, password);
			}
		};

		return Session.getInstance(props, authenticator);
	}

	@Override
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	@Override
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@Override
	public void setProperties(Properties properties) {
		this.properties = new Properties(properties);
	}

	public Session getSession() {
		if (session == null) {
			session = init();
		}
		return session;
	};
}
