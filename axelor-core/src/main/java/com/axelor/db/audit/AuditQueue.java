/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.google.inject.ImplementedBy;

@ImplementedBy(AuditQueueImpl.Noop.class)
public interface AuditQueue {

  /**
   * Process audit records for the given transaction ID.
   *
   * @param txId the transaction ID
   */
  void process(String txId);
}
