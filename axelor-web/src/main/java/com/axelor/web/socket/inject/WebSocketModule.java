/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket.inject;

import com.axelor.web.socket.Channel;
import com.axelor.web.socket.WebSocketEndpoint;
import com.axelor.web.socket.channels.TagsChannel;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class WebSocketModule extends AbstractModule {

  @Override
  protected void configure() {
    requestStaticInjection(WebSocketConfigurator.class);

    final Multibinder<Channel> multibinder = Multibinder.newSetBinder(binder(), Channel.class);
    multibinder.addBinding().to(TagsChannel.class);

    bind(WebSocketEndpoint.class).asEagerSingleton();
  }
}
