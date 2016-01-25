/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

import javax.mail.Session;

/**
 * The {@link MailAccount} provides a single definition {@link #getSession()} to
 * create account specific {@link Session} instance.
 * 
 */
public interface MailAccount {

	/**
	 * Socket connection timeout value in milliseconds.
	 *
	 * @param connectionTimeout
	 *            timeout value
	 */
	void setConnectionTimeout(int connectionTimeout);

	/**
	 * Socket read timeout value in milliseconds.
	 *
	 * @param timeout
	 *            timeout value
	 */
	void setTimeout(int timeout);

	/**
	 * Set additional properties.
	 *
	 */
	void setProperties(Properties properties);

	/**
	 * Get a {@link Session} for this account.<br>
	 * <br>
	 * The account implementation can decide whether to cache the session
	 * instance or not.
	 * 
	 * @return a {@link Session} instance.
	 */
	Session getSession();
}
