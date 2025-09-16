/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

import jakarta.mail.Session;
import java.util.Properties;

/**
 * The {@link MailAccount} provides a single definition {@link #getSession()} to create account
 * specific {@link Session} instance.
 */
public interface MailAccount {

  /**
   * Socket connection timeout value in milliseconds.
   *
   * @param connectionTimeout timeout value
   */
  void setConnectionTimeout(int connectionTimeout);

  /**
   * Socket read timeout value in milliseconds.
   *
   * @param timeout timeout value
   */
  void setTimeout(int timeout);

  /**
   * Set additional properties.
   *
   * @param properties the properties to set
   */
  void setProperties(Properties properties);

  /**
   * Get a {@link Session} for this account.<br>
   * <br>
   * The account implementation can decide whether to cache the session instance or not.
   *
   * @return a {@link Session} instance.
   */
  Session getSession();
}
