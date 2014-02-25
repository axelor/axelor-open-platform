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
	
	private static final String DEFAULT_PORT = "25";
	
	private String host;
	private String port;
	private String user;
	private String password;
	private String encryption;
	
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

		props.setProperty("mail.smtp.host", host);
		props.setProperty("mail.smtp.port", port);
		props.setProperty("mail.smtp.auth", "" + authenticating);

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

	public Session getSession() {
		if (session == null) {
			session = init();
		}
		return session;
	};
}
