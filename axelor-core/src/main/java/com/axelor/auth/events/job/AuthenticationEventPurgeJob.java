/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.events.job;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.auth.db.AuthenticationEvent;
import com.axelor.db.JPA;
import java.time.LocalDateTime;
import java.util.List;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job to delete the {@link AuthenticationEvent} older than the retention period.
 *
 * <p>The retention period is defined in days by the {@value
 * AvailableAppSettings#AUTH_EVENTS_RETENTION} setting (defaults to {@value
 * #DEFAULT_RETENTION_DAYS}). A value of zero or less keeps all events.
 */
@DisallowConcurrentExecution
public class AuthenticationEventPurgeJob implements Job {

  /** Default number of days authentication events are kept. */
  public static final int DEFAULT_RETENTION_DAYS = 180;

  private static final int PURGE_BATCH_SIZE = 1000;

  private static final Logger log = LoggerFactory.getLogger(AuthenticationEventPurgeJob.class);

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    try {
      purgeExpiredEvents();
    } catch (Exception e) {
      throw new JobExecutionException(e);
    }
  }

  /**
   * Deletes the authentication events older than the retention period.
   *
   * @return the number of deleted events
   */
  public long purgeExpiredEvents() {
    final int retentionDays =
        AppSettings.get()
            .getInt(AvailableAppSettings.AUTH_EVENTS_RETENTION, DEFAULT_RETENTION_DAYS);

    if (retentionDays <= 0) {
      log.debug("Authentication events retention is disabled, nothing to purge");
      return 0;
    }

    final long count = purgeEventsBefore(LocalDateTime.now().minusDays(retentionDays));
    log.info("Purged {} authentication events older than {} days", count, retentionDays);
    return count;
  }

  /**
   * Deletes the authentication events created before the given date.
   *
   * <p>Events are deleted in batches, each in its own transaction, to avoid long-running
   * transactions and locks on large tables.
   *
   * @param date the date before which events are deleted
   * @return the number of deleted events
   */
  public long purgeEventsBefore(LocalDateTime date) {
    long total = 0;
    int deleted;

    do {
      deleted =
          JPA.callInTransaction(
              () -> {
                final List<Long> ids =
                    JPA.em()
                        .createQuery(
                            "SELECT self.id FROM AuthenticationEvent self"
                                + " WHERE self.createdOn < :date",
                            Long.class)
                        .setParameter("date", date)
                        .setMaxResults(PURGE_BATCH_SIZE)
                        .getResultList();

                if (ids.isEmpty()) {
                  return 0;
                }

                return JPA.em()
                    .createQuery("DELETE FROM AuthenticationEvent self WHERE self.id IN :ids")
                    .setParameter("ids", ids)
                    .executeUpdate();
              });
      total += deleted;
    } while (deleted == PURGE_BATCH_SIZE);

    return total;
  }
}
