/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit;

import com.axelor.audit.db.AuditEventType;
import com.axelor.audit.db.AuditLog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Audit logs of the same record and event type within a transaction, processed together. */
class AuditWorkGroup {

  String txId;
  String relatedModel;
  Long relatedId;
  Long firstAuditLogId;
  Long lastAuditLogId;
  AuditEventType eventType;
  int retryCount;

  List<AuditLog> logs = new ArrayList<>();

  public AuditWorkGroup(
      String txId, String relatedModel, Long relatedId, AuditEventType eventType) {
    this.eventType = eventType;
    this.relatedId = relatedId;
    this.relatedModel = relatedModel;
    this.txId = txId;
  }

  public AuditWorkGroup(
      String txId,
      String relatedModel,
      Long relatedId,
      AuditEventType eventType,
      Long firstAuditLogId,
      Long lastAuditLogId,
      int retryCount) {
    this.txId = txId;
    this.relatedModel = relatedModel;
    this.relatedId = relatedId;
    this.firstAuditLogId = firstAuditLogId;
    this.lastAuditLogId = lastAuditLogId;
    this.eventType = eventType;
    this.retryCount = retryCount;
  }

  public AuditEventType getEventType() {
    return eventType;
  }

  public Long getRelatedId() {
    return relatedId;
  }

  public String getRelatedModel() {
    return relatedModel;
  }

  public String getTxId() {
    return txId;
  }

  public Long getFirstAuditLogId() {
    return firstAuditLogId;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public Long getLastAuditLogId() {
    return lastAuditLogId;
  }

  public void addAuditLog(AuditLog log) {
    this.logs.add(log);
  }

  public void clearAuditLogs() {
    this.logs.clear();
  }

  public List<AuditLog> getLogs() {
    return logs;
  }

  public AuditLog getFirstAuditLog() {
    return logs.stream().min(Comparator.comparingLong(AuditLog::getId)).orElse(null);
  }

  public AuditLog getLastAuditLog() {
    return logs.stream().max(Comparator.comparingLong(AuditLog::getId)).orElse(null);
  }

  @Override
  public String toString() {
    return String.format("%s#%d (%s) [Tx: %s]", relatedModel, relatedId, eventType, txId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AuditWorkGroup that = (AuditWorkGroup) o;
    return Objects.equals(txId, that.txId)
        && Objects.equals(relatedModel, that.relatedModel)
        && Objects.equals(relatedId, that.relatedId)
        && eventType == that.eventType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(txId, relatedModel, relatedId, eventType);
  }
}
