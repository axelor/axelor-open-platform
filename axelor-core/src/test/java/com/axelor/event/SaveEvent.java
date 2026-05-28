/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import com.axelor.db.Model;

public class SaveEvent<T extends Model> {

  private T entity;

  public SaveEvent(T entity) {
    this.entity = entity;
  }

  public T getEntity() {
    return entity;
  }
}
