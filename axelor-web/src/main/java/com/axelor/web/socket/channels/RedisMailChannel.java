/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web.socket.channels;

import com.axelor.cache.AxelorCache;
import com.axelor.cache.CacheBuilder;
import com.axelor.cache.redisson.RedissonProvider;
import com.axelor.common.ObjectUtils;
import com.axelor.db.JpaSecurity;
import com.axelor.mail.db.MailMessage;
import com.axelor.mail.db.repo.MailMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.websocket.Session;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.redisson.RedissonShutdownException;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mail channel using Redis/Valkey messaging */
@Singleton
public class RedisMailChannel extends MailChannel {

  public static final String TOPIC_NAME = "axelor.mail";

  private final String instanceId = UUID.randomUUID().toString();

  // Track session counts across all instances
  // key => model:recordId
  // value => instanceId => session count
  private final AxelorCache<String, Map<String, Integer>> globalSessions =
      CacheBuilder.<String, Map<String, Integer>>newBuilder("mail-sessions")
          .expireAfterAccess(Duration.ofHours(6))
          .build();

  private final RTopic topic;
  private final int listenerId;

  private static final Logger log = LoggerFactory.getLogger(RedisMailChannel.class);

  @Inject
  public RedisMailChannel(
      MailMessageRepository mailMessageRepo, ObjectMapper objectMapper, JpaSecurity jpaSecurity) {
    super(mailMessageRepo, objectMapper, jpaSecurity);
    var redisson = RedissonProvider.get();
    topic = redisson.getTopic(TOPIC_NAME);

    // Listen for messages from other instances.
    listenerId = topic.addListener(MailData.class, new MailMessageListener());
  }

  @Override
  protected void onJoin(Session session, String key) {
    updateGlobal(key, 1);
  }

  @Override
  protected void onLeft(Session session, String key) {
    updateGlobal(key, -1);
  }

  private void updateGlobal(String key, int delta) {
    var lock = globalSessions.getLock(key);
    lock.lock();
    try {
      var sessions = globalSessions.get(key);
      if (sessions == null) {
        sessions = new ConcurrentHashMap<>();
      }
      var count = sessions.getOrDefault(instanceId, 0) + delta;
      if (count <= 0) {
        sessions.remove(instanceId);
      } else {
        sessions.put(instanceId, count);
      }
      if (sessions.isEmpty()) {
        globalSessions.invalidate(key);
      } else {
        globalSessions.put(key, sessions);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  protected boolean shouldProcess(MailMessage message) {
    // Process if there are sessions for this message across all instances.
    var key = getKey(message.getRelatedModel(), message.getRelatedId());
    var sessions = globalSessions.get(key);
    return ObjectUtils.notEmpty(sessions);
  }

  @Override
  protected void broadcast(MailData data) {
    log.trace("Sending message: {}", data);

    // Broadcast to all instances.
    topic.publish(data);
  }

  @Override
  public void onUnsubscribe(Session session) {
    try {
      super.onUnsubscribe(session);
    } catch (RedissonShutdownException e) {
      log.info("Redisson is already shut down.");
    }
  }

  @Override
  public void shutdown() {
    try {
      topic.removeListener(listenerId);
      instanceSessions.keySet().forEach(this::leaveGlobal);
    } catch (RedissonShutdownException e) {
      log.info("Redisson is already shut down.");
    } finally {
      super.shutdown();
    }
  }

  private void leaveGlobal(String key) {
    var lock = globalSessions.getLock(key);
    lock.lock();
    try {
      var sessions = globalSessions.get(key);
      if (sessions != null && sessions.remove(instanceId) != null) {
        if (sessions.isEmpty()) {
          globalSessions.invalidate(key);
        } else {
          // Because of serialization, we need to put it back after modification.
          globalSessions.put(key, sessions);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  protected class MailMessageListener implements MessageListener<MailData> {

    @Override
    public void onMessage(CharSequence channel, MailData data) {
      log.trace("Received message: {}", data);

      // Forward message to local sessions.
      RedisMailChannel.super.broadcast(data);
    }
  }
}
