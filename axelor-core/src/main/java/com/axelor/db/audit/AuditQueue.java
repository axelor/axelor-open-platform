/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.google.inject.ImplementedBy;

/**
 * Represents an interface for queuing and triggering the processing of audit logs.
 *
 * <p>This abstraction allows audit log processing to be decoupled from the main transaction flow,
 * enabling different implementation strategies
 */
@ImplementedBy(NoopAuditQueue.class)
public interface AuditQueue {

  /**
   * Process audit records for the given transaction ID.
   *
   * @param txId the transaction ID
   */
  void process(String txId);

  default QueueStats getStatistics() {
    return new QueueStats(0, 0, false, 0);
  }

  record QueueStats(
      int pending, // Pending items
      long completed, // Total processed
      boolean isActive, // Is busy?
      long failure // Total errors
      ) {}
}
