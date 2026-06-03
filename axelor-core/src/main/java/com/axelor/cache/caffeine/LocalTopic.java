/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.caffeine;

import com.axelor.cache.AxelorTopic;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A topic for single-instance usage */
public class LocalTopic implements AxelorTopic {

  private record Subscription<M>(Class<M> type, MessageListener<? extends M> listener) {}

  private final Map<Integer, Subscription<?>> listeners = new ConcurrentHashMap<>();
  private final AtomicInteger nextId = new AtomicInteger();

  private static final Logger log = LoggerFactory.getLogger(LocalTopic.class);

  @Override
  public long publish(Object message) {
    long count = 0;

    for (var sub : listeners.values()) {
      if (sub.type().isInstance(message)) {
        dispatch(sub, message);
        ++count;
      }
    }

    return count;
  }

  @Override
  public <M> int addListener(Class<M> type, MessageListener<? extends M> listener) {
    int id = nextId.incrementAndGet();
    listeners.put(id, new Subscription<>(type, listener));
    return id;
  }

  @Override
  public void removeListener(Integer... ids) {
    for (Integer id : ids) {
      listeners.remove(id);
    }
  }

  @SuppressWarnings("unchecked")
  private <M> void dispatch(Subscription<M> sub, Object message) {
    try {
      ((MessageListener<M>) sub.listener()).onMessage((M) message);
    } catch (RuntimeException e) {
      log.error("Topic listener failed", e);
    }
  }
}
