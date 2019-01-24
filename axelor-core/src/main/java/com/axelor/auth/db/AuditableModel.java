/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.auth.db;

import com.axelor.db.Model;
import com.axelor.db.annotations.Widget;
import java.time.LocalDateTime;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * The base abstract class with update logging feature.
 *
 * <p>The model instance logs the creation date, last modified date, the authorized user who created
 * the record and the user who updated the record last time.
 */
@MappedSuperclass
public abstract class AuditableModel extends Model {

  @Widget(readonly = true, title = /*$$(*/ "Created on" /*)*/)
  private LocalDateTime createdOn;

  @Widget(readonly = true, title = /*$$(*/ "Updated on" /*)*/)
  private LocalDateTime updatedOn;

  @Widget(readonly = true, title = /*$$(*/ "Created by" /*)*/)
  @ManyToOne(
    fetch = FetchType.LAZY,
    cascade = {CascadeType.PERSIST, CascadeType.MERGE}
  )
  private User createdBy;

  @Widget(readonly = true, title = /*$$(*/ "Updated by" /*)*/)
  @ManyToOne(
    fetch = FetchType.LAZY,
    cascade = {CascadeType.PERSIST, CascadeType.MERGE}
  )
  private User updatedBy;

  public LocalDateTime getCreatedOn() {
    return createdOn;
  }

  @Access(AccessType.FIELD)
  private void setCreatedOn(LocalDateTime createdOn) {
    this.createdOn = createdOn;
  }

  public LocalDateTime getUpdatedOn() {
    return updatedOn;
  }

  @Access(AccessType.FIELD)
  private void setUpdatedOn(LocalDateTime updatedOn) {
    this.updatedOn = updatedOn;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  @Access(AccessType.FIELD)
  private void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }

  public User getUpdatedBy() {
    return updatedBy;
  }

  @Access(AccessType.FIELD)
  private void setUpdatedBy(User updatedBy) {
    this.updatedBy = updatedBy;
  }
}
