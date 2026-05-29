/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket;

import com.axelor.auth.db.User;
import com.axelor.db.JPA;
import jakarta.websocket.EncodeException;
import jakarta.websocket.SendHandler;
import jakarta.websocket.Session;
import java.io.IOException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;

public abstract class Channel {

  public abstract String getName();

  public void onSubscribe(Session session) {}

  public void onUnsubscribe(Session session) {}

  public abstract void onMessage(Session session, Message message);

  public void send(Session session, Object data) throws IOException, EncodeException {
    Message message = createMessage(data);
    session.getBasicRemote().sendObject(message);
  }

  public void sendAsync(Session session, Object data, SendHandler handler) {
    Message message = createMessage(data);
    session.getAsyncRemote().sendObject(message, handler);
  }

  private Message createMessage(Object data) {
    Message message = new Message();
    message.setChannel(getName());
    message.setType(MessageType.MSG);
    message.setData(data);
    return message;
  }

  public boolean isEnabled() {
    return true;
  }

  /** Gets the user from the subject associated with the session. */
  protected User getUser(Session session) {
    Long userId = getUserId(session);
    return userId == null ? null : JPA.find(User.class, userId);
  }

  /** Gets the user ID from the subject associated with the session. */
  protected Long getUserId(Session session) {
    PrincipalCollection principals = getPrincipals(session);
    if (principals == null) {
      return null;
    }

    return principals.oneByType(Long.class);
  }

  /** Gets the user code from the subject associated with the session. */
  protected String getUserCode(Session session) {
    PrincipalCollection principals = getPrincipals(session);
    if (principals == null) {
      return null;
    }

    return principals.getPrimaryPrincipal().toString();
  }

  /** Gets principals from the subject associated with the session. */
  private PrincipalCollection getPrincipals(Session session) {
    if (session.getUserPrincipal() == null) {
      return null;
    }

    final Subject subject = (Subject) session.getUserProperties().get(Subject.class.getName());
    return subject.getPrincipals();
  }
}
