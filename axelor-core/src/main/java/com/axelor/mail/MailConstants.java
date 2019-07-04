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

import com.axelor.mail.db.MailMessage;

/** Defines constants for mail server configuration settings. */
public interface MailConstants {

  public static final String PROTOCOL_IMAP = "imap";
  public static final String PROTOCOL_IMAPS = "imaps";
  public static final String PROTOCOL_POP3 = "pop3";

  public static final int DEFAULT_TIMEOUT = 60000;

  public static final String CHANNEL_STARTTLS = "starttls";
  public static final String CHANNEL_SSL = "ssl";

  /** {@link MailMessage} type used for notification messages */
  public static final String MESSAGE_TYPE_NOTIFICATION = "notification";

  /** {@link MailMessage} type used for comments */
  public static final String MESSAGE_TYPE_COMMENT = "comment";

  /** {@link MailMessage} type used for email messages */
  public static final String MESSAGE_TYPE_EMAIL = "email";
}
