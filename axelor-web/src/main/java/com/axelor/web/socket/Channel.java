/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.web.socket;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import java.io.IOException;
import javax.websocket.EncodeException;
import javax.websocket.Session;

public abstract class Channel {

  public abstract String getName();

  public void onSubscribe(Session session) {}

  public void onUnsubscribe(Session session) {}

  public abstract void onMessage(Session session, Message message);

  public void send(Session session, Object data) throws IOException, EncodeException {
    Message message = new Message();
    message.setChannel(getName());
    message.setType(MessageType.MSG);
    message.setData(data);
    session.getBasicRemote().sendObject(message);
  }

  public boolean isEnabled() {
    return true;
  }

  protected User getUser(Session session) {
    return session.getUserPrincipal() == null
        ? null
        : AuthUtils.getUser(session.getUserPrincipal().getName());
  }
}
