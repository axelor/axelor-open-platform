/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.axelor.JpaTest;
import com.axelor.meta.db.MetaJsonField;
import com.axelor.meta.db.MetaJsonRecord;
import com.axelor.test.db.Title;
import jakarta.persistence.EntityManager;
import java.lang.ref.WeakReference;
import java.time.Duration;
import java.util.function.Supplier;
import org.hibernate.engine.spi.SessionImplementor;
import org.junit.jupiter.api.Test;

/**
 * Hibernate does not run after-completion processes when a session is closed with a rollback-only
 * transaction that was never rolled back. Per-transaction state removed by such processes must not
 * keep the transaction, and so its session, reachable.
 */
class UncompletedTransactionTest extends JpaTest {

  @Test
  void auditTrail_releasesTransaction_whenClosedWithoutCompletion() {
    final String suffix = String.valueOf(System.nanoTime());
    assertCollected(
        closeWithoutCompletion(
            () -> {
              Title title = new Title();
              title.setCode("leak-" + suffix);
              title.setName("Leak " + suffix);
              return title;
            }));
  }

  @Test
  void metaStoreCacheInvalidator_releasesTransaction_whenClosedWithoutCompletion() {
    final String suffix = String.valueOf(System.nanoTime());
    assertCollected(
        closeWithoutCompletion(
            () -> {
              MetaJsonField field = new MetaJsonField();
              field.setName("leak" + suffix);
              field.setType("string");
              field.setModel(MetaJsonRecord.class.getName());
              field.setModelField("attrs");
              return field;
            }));
  }

  /**
   * Flushes a new entity in a separate session, marks its transaction rollback-only and closes the
   * session without completing the transaction.
   */
  private WeakReference<Object> closeWithoutCompletion(Supplier<Object> entity) {
    final EntityManager em = JPA.em().getEntityManagerFactory().createEntityManager();
    em.getTransaction().begin();
    em.persist(entity.get());
    em.flush();

    assertTrue(
        em.unwrap(SessionImplementor.class).getActionQueue().hasAfterTransactionActions(),
        "flush should have registered after-completion processes");

    final WeakReference<Object> transaction = new WeakReference<>(em.getTransaction());
    em.getTransaction().setRollbackOnly();
    em.close();
    return transaction;
  }

  private static void assertCollected(WeakReference<?> ref) {
    await()
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              System.gc();
              assertNull(ref.get(), "transaction is still reachable after its session was closed");
            });
  }
}
