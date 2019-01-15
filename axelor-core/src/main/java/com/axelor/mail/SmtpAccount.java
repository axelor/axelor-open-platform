/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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

import static com.axelor.common.StringUtils.isBlank;

import com.axelor.common.StringUtils;
import com.google.common.base.Preconditions;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;

/** The default implementation of {@link MailAccount} for SMPT accounts. */
public class SmtpAccount implements MailAccount {

  private String host;
  private String port;
  private String user;
  private String password;
  private String channel;

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
    Preconditions.checkNotNull(host, "host can't be null");
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

  private Session init() {

    final boolean authenticating = !StringUtils.isBlank(user);
    final Properties props = new Properties();

    // set timeout
    props.setProperty("mail.smtp.connectiontimeout", "" + connectionTimeout);
    props.setProperty("mail.smtp.timeout", "" + timeout);

    if (properties != null) {
      props.putAll(properties);
    }

    props.setProperty("mail.smtp.host", host);
    props.setProperty("mail.smtp.port", port);
    props.setProperty("mail.smtp.auth", "" + authenticating);

    if (!isBlank(user)) {
      props.setProperty("mail.smtp.from", user);
    }

    if (!authenticating) {
      return Session.getInstance(props);
    }

    if (MailConstants.CHANNEL_STARTTLS.equalsIgnoreCase(channel)) {
      props.setProperty("mail.smtp.starttls.enable", "true");
    }
    if (MailConstants.CHANNEL_SSL.equalsIgnoreCase(channel)) {
      props.setProperty("mail.smtp.ssl.enable", "true");
      props.setProperty("mail.smtp.socketFactory.port", port);
      props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    }

    final Authenticator authenticator =
        new Authenticator() {

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
    this.properties = properties;
  }

  @Override
  public Session getSession() {
    if (session == null) {
      session = init();
    }
    return session;
  };
}
