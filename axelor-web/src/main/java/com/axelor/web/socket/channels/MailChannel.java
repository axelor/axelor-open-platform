/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket.channels;

import com.axelor.auth.AuthUtils;
import com.axelor.cache.CacheBuilder;
import com.axelor.concurrent.ContextAware;
import com.axelor.db.JpaSecurity;
import com.axelor.db.Model;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.axelor.mail.event.MailMessageEvent;
import com.axelor.web.socket.Channel;
import com.axelor.web.socket.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.websocket.Session;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MailChannel extends Channel {

  private static final String NAME = "mail";

  protected final ConcurrentMap<String, Set<Session>> instanceSessions =
      CacheBuilder.newInMemoryBuilder()
          .expireAfterAccess(Duration.ofHours(6))
          .<String, Set<Session>>build()
          .asMap();

  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  private final MailMessageRepository mailMessageRepo;
  private final ObjectMapper objectMapper;
  private final JpaSecurity jpaSecurity;

  private static final Logger log = LoggerFactory.getLogger(MailChannel.class);

  @Inject
  public MailChannel(
      MailMessageRepository mailMessageRepo, ObjectMapper objectMapper, JpaSecurity jpaSecurity) {
    this.mailMessageRepo = mailMessageRepo;
    this.objectMapper = objectMapper;
    this.jpaSecurity = jpaSecurity;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void onMessage(Session session, Message message) {
    if (message.getData() == null) {
      log.warn("No message data from session: {}", session.getId());
      return;
    }

    var data = getData(message);
    var command = data.command();
    var model = data.model();
    var recordId = data.recordId();

    if (command == null || model == null || recordId == null) {
      log.warn("Invalid message data from session: {}", session.getId());
      return;
    }

    if (!isPermitted(session, model, recordId)) {
      return;
    }

    var key = getKey(model, recordId);

    switch (command) {
      case JOIN -> {
        instanceSessions.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(session);
        onJoin(session, key);
      }
      case LEFT ->
          instanceSessions.computeIfPresent(
              key,
              (k, sessions) -> {
                if (sessions.remove(session)) {
                  onLeft(session, key);
                }
                return sessions.isEmpty() ? null : sessions;
              });
      default -> log.warn("Unexpected command {} from session: {}", command, session.getId());
    }
  }

  @Override
  public void onUnsubscribe(Session session) {
    instanceSessions.forEach(
        (key, sessions) -> {
          if (sessions.remove(session)) {
            onLeft(session, key);
          }
        });
    instanceSessions.values().removeIf(Collection::isEmpty);
  }

  protected void onJoin(Session session, String key) {
    // Nothing to do by default
  }

  protected void onLeft(Session session, String key) {
    // Nothing to do by default
  }

  protected String getKey(String model, Long recordId) {
    return model + ":" + recordId;
  }

  public void processMailMessage(MailMessageEvent event) {
    switch (event.type()) {
      case CREATED ->
          executor.submit(
              ContextAware.of()
                  .withUser(null)
                  .withTransaction(false)
                  .build(() -> processCreated(event.message())));
      case DELETED ->
          executor.submit(
              ContextAware.of()
                  .withUser(null)
                  .withTransaction(false)
                  .build(() -> processDeleted(event.message())));
      default -> log.trace("Unhandled event type: {}", event.type());
    }
  }

  public void shutdown() {
    executor.shutdown();
  }

  protected boolean shouldProcess(MailMessage message) {
    // Process if there are sessions for this message.
    return instanceSessions.containsKey(getKey(message.getRelatedModel(), message.getRelatedId()));
  }

  private void processCreated(MailMessage message) {
    if (message == null || message.getRelatedModel() == null || message.getRelatedId() == null) {
      log.warn("Invalid created message: {}", message);
      return;
    }

    if (!shouldProcess(message)) {
      return;
    }

    var details = mailMessageRepo.details(message);
    var data =
        new MailData(
            MailCommand.MESSAGES,
            message.getRelatedModel(),
            message.getRelatedId(),
            List.of(details));

    broadcast(data);
  }

  private void processDeleted(MailMessage message) {
    if (message == null
        || message.getRelatedModel() == null
        || message.getRelatedId() == null
        || message.getId() == null) {
      log.warn("Invalid deleted message: {}", message);
      return;
    }

    if (!shouldProcess(message)) {
      return;
    }

    var data =
        new MailData(
            MailCommand.DELETED,
            message.getRelatedModel(),
            message.getRelatedId(),
            List.of(Map.of("id", message.getId())));

    broadcast(data);
  }

  protected void broadcast(MailData data) {
    var key = getKey(data.model(), data.recordId());
    var sessions = instanceSessions.get(key);
    if (sessions != null) {
      broadcast(sessions, data);
      instanceSessions.computeIfPresent(key, (k, s) -> s.isEmpty() ? null : s);
    }
  }

  private void broadcast(Set<Session> sessions, MailData data) {
    for (var it = sessions.iterator(); it.hasNext(); ) {
      var session = it.next();
      if (session.isOpen() && isPermitted(session, data.model(), data.recordId())) {
        sendAsync(
            session,
            data,
            result -> {
              if (!result.isOK()) {
                log.error(
                    "Error sending message to session: %s".formatted(session.getId()),
                    result.getException());
              }
            });
      } else {
        it.remove();
      }
    }
  }

  private MailData getData(Message message) {
    return objectMapper.convertValue(message.getData(), MailData.class);
  }

  private boolean isPermitted(Session session, String modelName, Long id) {
    var user = getUser(session);

    if (user == null) {
      log.warn("User not found for session: {}", session.getId());
      return false;
    }

    AuthUtils.setCurrentUser(user);
    try {
      var modelClass = getModelClass(modelName);
      var permitted = jpaSecurity.isPermitted(JpaSecurity.CAN_READ, modelClass, id);

      if (!permitted) {
        log.atWarn()
            .setMessage("User {} is not permitted to read {}")
            .addArgument(user::getCode)
            .addArgument(() -> getKey(modelName, id))
            .log();
      }

      return permitted;
    } finally {
      AuthUtils.removeCurrentUser();
    }
  }

  private Class<? extends Model> getModelClass(String modelName) {
    try {
      return Class.forName(modelName).asSubclass(Model.class);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
