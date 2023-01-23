/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.web.socket.inject;

import com.axelor.web.socket.Channel;
import com.axelor.web.socket.WebSocketEndpoint;
import com.axelor.web.socket.channels.CollaborationChannel;
import com.axelor.web.socket.channels.TagsChannel;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class WebSocketModule extends AbstractModule {

  @Override
  protected void configure() {
    requestStaticInjection(WebSocketConfigurator.class);

    final Multibinder<Channel> multibinder = Multibinder.newSetBinder(binder(), Channel.class);
    multibinder.addBinding().to(TagsChannel.class);
    multibinder.addBinding().to(CollaborationChannel.class);

    bind(WebSocketEndpoint.class).asEagerSingleton();
  }
}
