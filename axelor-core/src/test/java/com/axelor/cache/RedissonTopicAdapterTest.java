/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.cache;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.axelor.JpaTest;
import com.axelor.cache.redisson.RedissonProvider;
import com.axelor.cache.redisson.RedissonTopicAdapter;
import com.axelor.db.JPA;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RedissonTopicAdapterTest extends JpaTest {

  @BeforeAll
  static void startRedis() {
    RedisTest.startRedis();
  }

  @AfterAll
  static void stopRedis() {
    RedisTest.stopRedis();
  }

  @Test
  void testListenerUnitOfWork() {
    var topic =
        new RedissonTopicAdapter(RedissonProvider.get().getTopic("test:" + UUID.randomUUID()));
    List<EntityManager> used = new CopyOnWriteArrayList<>();
    int listenerId = topic.addListener(String.class, message -> used.add(JPA.em()));

    try {
      topic.publish("first");
      topic.publish("second");

      await().atMost(Duration.ofSeconds(5)).until(() -> used.size() == 2);

      // each message gets its own entity manager, closed once the listener is done
      assertNotSame(used.get(0), used.get(1));
      await()
          .atMost(Duration.ofSeconds(5))
          .untilAsserted(
              () -> {
                assertFalse(used.get(0).isOpen());
                assertFalse(used.get(1).isOpen());
              });
      assertEquals(2, used.size());
    } finally {
      topic.removeListener(listenerId);
    }
  }
}
