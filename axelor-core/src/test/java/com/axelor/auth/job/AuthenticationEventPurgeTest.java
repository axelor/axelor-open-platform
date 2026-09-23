/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.axelor.JpaTest;
import com.axelor.auth.db.AuthenticationEvent;
import com.axelor.auth.db.AuthenticationStatus;
import com.axelor.auth.events.job.AuthenticationEventPurgeJob;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthenticationEventPurgeTest extends JpaTest {

  private final AuthenticationEventPurgeJob job = new AuthenticationEventPurgeJob();

  @BeforeEach
  void clearEvents() {
    JPA.runInTransaction(
        () -> JPA.em().createQuery("DELETE FROM AuthenticationEvent").executeUpdate());
  }

  private void createEvents(int count, LocalDateTime createdOn) {
    JPA.runInTransaction(
        () -> {
          for (int i = 0; i < count; i++) {
            final AuthenticationEvent event = new AuthenticationEvent();
            event.setStatus(AuthenticationStatus.SUCCESS);
            event.setCreatedOn(createdOn);
            JPA.em().persist(event);
          }
        });
  }

  private long countEvents() {
    return Query.of(AuthenticationEvent.class).count();
  }

  @Test
  void purgeDeletesOnlyOlderEvents() {
    final LocalDateTime now = LocalDateTime.now();
    createEvents(3, now.minusDays(200));
    createEvents(2, now.minusDays(10));

    final long deleted = job.purgeEventsBefore(now.minusDays(180));

    assertEquals(3, deleted);
    assertEquals(2, countEvents());
  }

  @Test
  void purgeDeletesInBatches() {
    final LocalDateTime now = LocalDateTime.now();
    // more than one batch
    createEvents(1005, now.minusDays(200));
    createEvents(1, now);

    final long deleted = job.purgeEventsBefore(now.minusDays(180));

    assertEquals(1005, deleted);
    assertEquals(1, countEvents());
  }

  @Test
  void purgeWithNothingToDelete() {
    createEvents(2, LocalDateTime.now());

    assertEquals(0, job.purgeEventsBefore(LocalDateTime.now().minusDays(180)));
    assertEquals(2, countEvents());
  }
}
