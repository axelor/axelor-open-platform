/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
 * Defines constants for mail server configuration settings.
 *
 */
public interface MailConstants {

	public static final String CONFIG_SMTP_HOST = "mail.smtp.host";
	public static final String CONFIG_SMTP_PORT = "mail.smtp.port";
	public static final String CONFIG_SMTP_USER = "mail.smtp.user";
	public static final String CONFIG_SMTP_PASS = "mail.smtp.pass";
	public static final String CONFIG_SMTP_CHANNEL = "mail.smtp.channel";
	public static final String CONFIG_SMTP_TIMEOUT = "mail.smtp.timeout";
	public static final String CONFIG_SMTP_CONNECTION_TIMEOUT = "mail.smtp.connectionTimeout";

	public static final String CONFIG_IMAPS_HOST = "mail.imaps.host";
	public static final String CONFIG_IMAPS_PORT = "mail.imaps.port";
	public static final String CONFIG_IMAPS_USER = "mail.imaps.user";
	public static final String CONFIG_IMAPS_PASS = "mail.imaps.pass";
	public static final String CONFIG_IMAPS_TIMEOUT = "mail.imaps.timeout";
	public static final String CONFIG_IMAPS_CONNECTION_TIMEOUT = "mail.imaps.connectionTimeout";

	public static final int DEFAULT_TIMEOUT = MailAccount.DEFAULT_TIMEOUT;
}
