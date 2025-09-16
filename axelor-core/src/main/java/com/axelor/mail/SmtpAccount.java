/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

import com.axelor.common.StringUtils;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.Objects;
import java.util.Properties;

/** The default implementation of {@link MailAccount} for SMPT accounts. */
public class SmtpAccount implements MailAccount {

  private String host;
  private String port;
  private String user;
  private String password;
  private String channel;
  private String from;

  private int connectionTimeout = MailConstants.DEFAULT_TIMEOUT;
  private int timeout = MailConstants.DEFAULT_TIMEOUT;

  private Properties properties;

  private Session session;

  /**
   * Create a non-authenticating SMTP account.
   *
   * @param host the smtp server host
   * @param port the smtp server port
   */
  public SmtpAccount(String host, String port) {
    Objects.requireNonNull(host, "host can't be null");
    this.host = host;
    this.port = port;
  }

  /**
   * Create an authenticating SMTP account.
   *
   * @param host the smtp server host
   * @param port the smtp server port
   * @param user the smtp server login user name
   * @param password the smtp server login password
   */
  public SmtpAccount(String host, String port, String user, String password) {
    this(host, port);
    this.user = user;
    this.password = password;
  }

  /**
   * Create an authenticating SMTP account.
   *
   * @param host the smtp server host
   * @param port the smtp server port
   * @param user the smtp server login user name
   * @param password the smtp server login password
   * @param channel the smtp encryption channel (starttls or ssl)
   */
  public SmtpAccount(String host, String port, String user, String password, String channel) {
    this(host, port, user, password);
    this.channel = channel;
  }

  /**
   * Create an authenticating SMTP account.
   *
   * @param host the smtp server host
   * @param port the smtp server port
   * @param user the smtp server login user name
   * @param password the smtp server login password
   * @param channel the smtp encryption channel (starttls or ssl)
   * @param from the envelope return address
   */
  public SmtpAccount(
      String host, String port, String user, String password, String channel, String from) {
    this(host, port, user, password, channel);
    this.from = from;
  }

  private Session init() {

    final boolean authenticating = !StringUtils.isBlank(user);
    final Properties props = new Properties();

    // set timeout
    props.setProperty("mail.smtp.connectiontimeout", "" + connectionTimeout);
    props.setProperty("mail.smtp.timeout", "" + timeout);

    props.setProperty("mail.smtp.host", host);
    props.setProperty("mail.smtp.port", port);
    props.setProperty("mail.smtp.auth", "" + authenticating);

    if (StringUtils.notBlank(from)) {
      try {
        final InternetAddress fromAddress = new InternetAddress(from);
        if (StringUtils.notBlank(fromAddress.getAddress())) {
          props.setProperty("mail.smtp.from", fromAddress.getAddress());
        }
        if (StringUtils.notBlank(fromAddress.getPersonal())) {
          props.setProperty("mail.smtp.from.personal", fromAddress.getPersonal());
        }
      } catch (AddressException e) {
        throw new RuntimeException(e);
      }
    }

    if (MailConstants.CHANNEL_STARTTLS.equalsIgnoreCase(channel)) {
      props.setProperty("mail.smtp.starttls.enable", "true");
    }
    if (MailConstants.CHANNEL_SSL.equalsIgnoreCase(channel)) {
      props.setProperty("mail.smtp.ssl.enable", "true");
      props.setProperty("mail.smtp.socketFactory.port", port);
      props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    }

    Authenticator authenticator = null;
    if (authenticating) {
      authenticator =
          new Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(user, password);
            }
          };
    }

    if (properties != null) {
      props.putAll(properties);
    }

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
    this.properties = properties;
  }

  @Override
  public Session getSession() {
    if (session == null) {
      session = init();
    }
    return session;
  }
}
