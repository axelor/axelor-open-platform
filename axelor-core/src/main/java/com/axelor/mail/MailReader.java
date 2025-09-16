/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;

/** The {@link MailReader} provides features to read mails. */
public class MailReader {

  private Session session;
  private Store store;

  /**
   * Create a new instance of {@link MailReader} with the given account.
   *
   * @param account the account to use
   * @throws IllegalArgumentException if mail account can't get a {@link Store}
   */
  public MailReader(MailAccount account) {
    this.session = account.getSession();
    try {
      this.store = this.session.getStore();
    } catch (NoSuchProviderException e) {
      throw new IllegalArgumentException("Invalid mail account.", e);
    }
  }

  /**
   * Get a {@link Store} object and connect to the store if not connected.
   *
   * @return an instance of {@link Store}
   * @throws AuthenticationFailedException if authentication fails
   * @throws MessagingException if other failure
   */
  public Store getStore() throws AuthenticationFailedException, MessagingException {
    if (store.isConnected()) {
      return store;
    }
    store.connect();
    return store;
  }
}
