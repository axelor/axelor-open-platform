/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

/** A no-operation implementation of the {@link AuditQueue} interface. */
public class NoopAuditQueue implements AuditQueue {

  @Override
  public void process(String txId) {
    // No operation
  }
}
