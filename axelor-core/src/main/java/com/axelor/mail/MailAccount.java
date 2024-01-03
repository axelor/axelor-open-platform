/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2024 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.mail;

import java.util.Properties;
import javax.mail.Session;

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
