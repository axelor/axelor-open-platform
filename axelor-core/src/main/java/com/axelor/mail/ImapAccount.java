/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

/** The default implementation of {@link MailAccount} for IMAP/IMAPS accounts. */
public class ImapAccount extends AbstractMailAccount {

  /**
   * Create a new IMAP account.
   *
   * @param host server hostname
   * @param port server port
   * @param user login name
   * @param password login password
   */
  public ImapAccount(String host, String port, String user, String password) {
    this(host, port, user, password, null);
  }

  /**
   * Create a new IMAP/IMAPS account.
   *
   * <p>If the given channel is {@link MailConstants#CHANNEL_SSL} then it will use <code>imaps
   * </code> protocol over <code>ssl</code>.
   *
   * @param host server hostname
   * @param port server port
   * @param user login name
   * @param password login password
   * @param channel encryption channel (ssl, starttls or null)
   */
  public ImapAccount(String host, String port, String user, String password, String channel) {
    super(
        MailConstants.CHANNEL_SSL.equals(channel)
            ? MailConstants.PROTOCOL_IMAPS
            : MailConstants.PROTOCOL_IMAP,
        host,
        port,
        user,
        password,
        channel);
  }
}
