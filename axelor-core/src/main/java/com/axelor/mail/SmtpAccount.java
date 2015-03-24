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
import com.google.common.base.Preconditions;

/**
 * The default implementation of {@link MailAccount} for SMPT accounts.
 *
 */
public class SmtpAccount implements MailAccount {

	public static final String ENCRYPTION_TLS = "tls";

	public static final String ENCRYPTION_SSL = "ssl";

	public static final String DEFAULT_PORT = "25";

	private String host;
	private String port;
	private String user;
	private String password;
	private String encryption;

	private int connectionTimeout = DEFAULT_TIMEOUT;
	private int timeout = DEFAULT_TIMEOUT;

	private Properties properties;

	private Session session;

	/**
	 * Create a non-authenticating SMTP account.
	 * 
	 * @param host
	 *            the smtp server host
	 * @param port
	 *            the smtp server port
	 */
	public SmtpAccount(String host, String port) {
		Preconditions.checkNotNull(host, "host can't be null");
		this.host = host;
		this.port = port;
	}

	/**
	 * Create an authenticating SMTP account.
	 * 
	 * @param host
	 *            the smtp server host
	 * @param port
	 *            the smtp server port
	 * @param user
	 *            the smtp server login user name
	 * @param password
	 *            the smtp server login passowrd
	 * @param encryption
	 *            the smtp encryption scheme (tls or ssl)
	 */
	public SmtpAccount(String host, String port, String user, String password, String encryption) {
		this(host, port);
		this.user = user;
		this.password = password;
		this.encryption = encryption;
	}

	private Session init() {

		final boolean authenticating = !StringUtils.isBlank(user);
		final Properties props = new Properties();
		final String port = StringUtils.isBlank(this.port) ? DEFAULT_PORT : this.port;

		props.setProperty("mail.smtp.connectiontimeout", "" + DEFAULT_TIMEOUT);
		props.setProperty("mail.smtp.timeout", "" + DEFAULT_TIMEOUT);

		if (properties != null) {
			props.putAll(properties);
		}

		props.setProperty("mail.smtp.host", host);
		props.setProperty("mail.smtp.port", port);
		props.setProperty("mail.smtp.auth", "" + authenticating);

		if (!StringUtils.isBlank(user)) {
			props.setProperty("mail.smtp.from", user);
		}

		// respect manually set timeout
		if (connectionTimeout != DEFAULT_TIMEOUT) {
			props.setProperty("mail.smtp.connectiontimeout", "" + connectionTimeout);
		}
		if (timeout != DEFAULT_TIMEOUT) {
			props.setProperty("mail.smtp.timeout", "" + timeout);
		}

		if (!authenticating) {
			return Session.getInstance(props);
		}
		
		if (ENCRYPTION_TLS.equals(encryption)) {
			props.setProperty("mail.smtp.starttls.enable", "true");
		}
		if (ENCRYPTION_SSL.equals(encryption)) {
			props.setProperty("mail.smtp.socketFactory.port", port);
			props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
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

	@Override
	public Session getSession() {
		if (session == null) {
			session = init();
		}
		return session;
	};
}
