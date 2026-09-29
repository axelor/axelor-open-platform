/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.AxelorTopic;
import com.axelor.concurrent.ContextAware;
import com.axelor.db.tenants.TenantResolver;
import org.redisson.api.RTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Adapter for {@link org.redisson.api.RTopic} to conform to the {@link AxelorTopic}. */
public class RedissonTopicAdapter implements AxelorTopic {

  private final RTopic topic;

  private static final Logger log = LoggerFactory.getLogger(RedissonTopicAdapter.class);

  record MessageWrapper<M>(String tenantId, M message) {}

  public RedissonTopicAdapter(RTopic topic) {
    this.topic = topic;
  }

  @Override
  public long publish(Object message) {
    return topic.publish(new MessageWrapper<>(TenantResolver.currentTenantIdentifier(), message));
  }

  /**
   * {@inheritDoc}
   *
   * <p>Listeners run on shared Redisson worker threads. Each listener runs in a {@link
   * ContextAware} task with the publisher's tenant and a dedicated database session.
   */
  @SuppressWarnings("unchecked")
  @Override
  public <M> int addListener(Class<M> type, MessageListener<? extends M> listener) {
    return topic.addListener(
        MessageWrapper.class,
        (channel, wrapper) -> {
          if (!type.isInstance(wrapper.message())) {
            return;
          }

          try {
            ContextAware.of(wrapper.tenantId(), null, null, null, false)
                .build(() -> ((MessageListener<M>) listener).onMessage((M) wrapper.message()))
                .run();
          } catch (RuntimeException e) {
            log.error("Topic listener failed", e);
          }
        });
  }

  @Override
  public void removeListener(Integer... listenerIds) {
    topic.removeListener(listenerIds);
  }
}
