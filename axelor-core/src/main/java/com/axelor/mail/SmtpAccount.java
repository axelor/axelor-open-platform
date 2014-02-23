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
		
		if ("tls".equals(encryption)) {
			props.setProperty("mail.smtp.starttls.enable", "true");
		}
		if ("ssl".equals(encryption)) {
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
