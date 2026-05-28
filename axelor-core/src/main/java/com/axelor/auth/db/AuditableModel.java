/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.auth.db;

import com.axelor.db.Model;
import com.axelor.db.annotations.Widget;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

/**
 * The base abstract class with update logging feature.
 *
 * <p>The model instance logs the creation date, last modified date, the authorized user who created
 * the record and the user who updated the record last time.
 */
@MappedSuperclass
public abstract class AuditableModel extends Model {

  @Widget(readonly = true, copyable = false, title = /*$$(*/ "Created on" /*)*/)
  private LocalDateTime createdOn;

  @Widget(readonly = true, copyable = false, title = /*$$(*/ "Updated on" /*)*/)
  private LocalDateTime updatedOn;

  @Widget(readonly = true, copyable = false, title = /*$$(*/ "Created by" /*)*/)
  @ManyToOne(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  private User createdBy;

  @Widget(readonly = true, copyable = false, title = /*$$(*/ "Updated by" /*)*/)
  @ManyToOne(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
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
