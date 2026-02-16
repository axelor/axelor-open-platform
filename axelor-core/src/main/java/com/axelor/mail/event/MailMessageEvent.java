/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail.event;

import com.axelor.mail.db.MailMessage;

/** Event for mail messages */
public record MailMessageEvent(MailMessage message, Type type) {

  public enum Type {
    CREATED,
    UPDATED,
    DELETED
  }
}
