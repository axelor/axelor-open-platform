/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

/** The default implementation of {@link MailAccount} for POP3 accounts. */
public class Pop3Account extends AbstractMailAccount {

  /**
   * Create a new POP3 account.
   *
   * @param host server hostname
   * @param port server port
   * @param user login name
   * @param password login password
   */
  public Pop3Account(String host, String port, String user, String password) {
    super(MailConstants.PROTOCOL_POP3, host, port, user, password, null);
  }

  /**
   * Create a new POP3 account.
   *
   * @param host server hostname
   * @param port server port
   * @param user login name
   * @param password login password
   * @param channel encryption channel (ssl, starttls or null)
   */
  public Pop3Account(String host, String port, String user, String password, String channel) {
    super(MailConstants.PROTOCOL_POP3, host, port, user, password, channel);
  }
}
