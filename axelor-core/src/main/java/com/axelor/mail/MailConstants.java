/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
