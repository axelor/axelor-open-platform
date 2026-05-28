/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket.channels;

import com.axelor.event.Observes;
import com.axelor.events.ShutdownEvent;
import com.axelor.mail.event.MailMessageEvent;
import com.axelor.web.socket.Channel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;

@Singleton
public class MailChannelObserver {

  private final MailChannel mailChannel;

  @Inject
  public MailChannelObserver(Set<Channel> channels) {
    this.mailChannel =
        channels.stream()
            .filter(MailChannel.class::isInstance)
            .map(MailChannel.class::cast)
            .findAny()
            .orElseThrow(() -> new IllegalStateException("MailChannel not found"));
  }

  public void onMailMessage(@Observes MailMessageEvent event) {
    mailChannel.processMailMessage(event);
  }

  public void onShutdown(@Observes ShutdownEvent event) {
    mailChannel.shutdown();
  }
}
