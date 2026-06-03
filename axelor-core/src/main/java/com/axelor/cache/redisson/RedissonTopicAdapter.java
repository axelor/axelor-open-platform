/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache.redisson;

import com.axelor.cache.AxelorTopic;
import com.axelor.db.tenants.TenantResolver;
import org.redisson.api.RTopic;

/** Adapter for {@link org.redisson.api.RTopic} to conform to the {@link AxelorTopic}. */
public class RedissonTopicAdapter implements AxelorTopic {

  private final RTopic topic;

  record MessageWrapper<M>(String tenantId, M message) {}

  public RedissonTopicAdapter(RTopic topic) {
    this.topic = topic;
  }

  @Override
  public long publish(Object message) {
    return topic.publish(new MessageWrapper<>(TenantResolver.currentTenantIdentifier(), message));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <M> int addListener(Class<M> type, MessageListener<? extends M> listener) {
    return topic.addListener(
        MessageWrapper.class,
        (channel, wrapper) -> {
          if (!type.isInstance(wrapper.message())) {
            return;
          }

          String previousTenantId = TenantResolver.currentTenantIdentifier();
          TenantResolver.setCurrentTenant(wrapper.tenantId());
          try {
            ((MessageListener<M>) listener).onMessage((M) wrapper.message());
          } finally {
            TenantResolver.setCurrentTenant(previousTenantId);
          }
        });
  }

  @Override
  public void removeListener(Integer... listenerIds) {
    topic.removeListener(listenerIds);
  }
}
