/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.mail.event;

import com.axelor.db.JPA;
import com.axelor.inject.Beans;
import com.axelor.mail.db.MailMessage;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import org.hibernate.engine.spi.SessionImplementor;

public class MailMessageListener {

  @PostPersist
  private void onPostPersist(MailMessage message) {
    fire(message, MailMessageEvent.Type.CREATED);
  }

  @PostRemove
  private void onPostRemove(MailMessage message) {
    fire(message, MailMessageEvent.Type.DELETED);
  }

  private void fire(MailMessage message, MailMessageEvent.Type type) {
    SessionImplementor session = JPA.em().unwrap(SessionImplementor.class);
    session
        .getActionQueue()
        .registerProcess(
            (success, sessionImplementor) -> {
              if (success) {
                Beans.get(MailMessageEventFirer.class).fire(new MailMessageEvent(message, type));
              }
            });
  }
}
