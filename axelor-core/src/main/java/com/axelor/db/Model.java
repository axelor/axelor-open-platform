/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import com.axelor.db.annotations.Widget;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * The base abstract model class to extend all domain objects.
 *
 * <p>The derived model classes should implement {@link #getId()} and {@link #setId(Long)} using
 * appropriate {@link GeneratedValue#strategy()}.
 *
 * <p>A generic implementation {@link JpaModel} should be used in most cases if sequence of record
 * ids are important.
 */
@MappedSuperclass
public abstract class Model {

  @Version private Integer version;

  // Represents the collection id of the record in the UI widgets (collection fields)
  @Widget(copyable = false)
  @Transient
  private transient Long cid;

  // Represents the selected state of the record in the UI widgets
  @Transient private transient boolean selected;

  @Widget(massUpdate = true)
  private Boolean archived;

  public abstract Long getId();

  public abstract void setId(Long id);

  public Boolean getArchived() {
    return archived;
  }

  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  /**
   * Get the collection id of the record. The collection widgets use this value to identify exact
   * record from the action/save request responses.
   *
   * @return collection id
   */
  public Long getCid() {
    return cid;
  }

  /**
   * Set the collection id for the record. The collection widgets use this value to identify exact
   * record from the action/save request responses.
   *
   * @param cid the collection id
   */
  public void setCid(Long cid) {
    this.cid = cid;
  }

  /**
   * Set the selected state of the record. The UI widget will use this flag to mark/unmark the
   * selection state.
   *
   * @param selected selected state flag
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  /**
   * Check whether the record is selected in the UI widget.
   *
   * @return selection state
   */
  public boolean isSelected() {
    return selected;
  }
}
