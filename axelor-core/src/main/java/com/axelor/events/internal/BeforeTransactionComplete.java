/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
