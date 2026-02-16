/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail.event;

import com.axelor.event.Event;
import com.google.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class MailMessageEventFirer {

  private final Event<MailMessageEvent> mailMessageEvent;

  @Inject
  public MailMessageEventFirer(Event<MailMessageEvent> mailMessageEvent) {
    this.mailMessageEvent = mailMessageEvent;
  }

  public void fire(MailMessageEvent event) {
    mailMessageEvent.fire(event);
  }
}
