/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.events.internal;

import com.axelor.db.Model;
import java.util.Set;

/** For internal use only. */
public class BeforeTransactionComplete {

  private Set<? extends Model> updated;

  private Set<? extends Model> deleted;

  public BeforeTransactionComplete(Set<? extends Model> updated, Set<? extends Model> deleted) {
    this.updated = updated;
    this.deleted = deleted;
  }

  public Set<? extends Model> getUpdated() {
    return updated;
  }

  public Set<? extends Model> getDeleted() {
    return deleted;
  }
}
