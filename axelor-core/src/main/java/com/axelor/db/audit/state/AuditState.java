/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit.state;

import com.axelor.audit.db.AuditEventType;
import com.axelor.db.Model;
import java.time.LocalDateTime;
import java.util.Map;

/** Represents the complete audit record for a specific entity within a transaction. */
public class AuditState {

  private final LocalDateTime received;
  private final AuditEventType eventType;
  private final EntityState entityState;

  public AuditState(LocalDateTime received, AuditEventType eventType, EntityState entityState) {
    this.received = received;
    this.eventType = eventType;
    this.entityState = entityState;
  }

  public LocalDateTime getReceived() {
    return received;
  }

  public AuditEventType getEventType() {
    return eventType;
  }

  public EntityState getEntityState() {
    return entityState;
  }

  // --- Delegate Methods (Convenience) ---

  public Model getEntity() {
    return entityState.getEntity();
  }

  public Map<String, Object> getValues() {
    return entityState.getValues();
  }

  public Map<String, Object> getOldValues() {
    return entityState.getOldValues();
  }
}
