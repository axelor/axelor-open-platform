/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db.audit.state;

import com.axelor.db.Model;
import java.util.Map;

/** Captures the raw snapshot of an entity's state changes. */
public class EntityState {
  final Model entity;
  final Map<String, Object> values;
  final Map<String, Object> oldValues;

  public EntityState(Model entity, Map<String, Object> values, Map<String, Object> oldValues) {
    this.entity = entity;
    this.values = values;
    this.oldValues = oldValues;
  }

  public Model getEntity() {
    return entity;
  }

  public Map<String, Object> getValues() {
    return values;
  }

  public Map<String, Object> getOldValues() {
    return oldValues;
  }
}
