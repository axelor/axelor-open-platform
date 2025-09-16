/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * A generic implementation of {@link Model}.
 *
 * <p>This class uses {@link GenerationType#AUTO} strategy for id generation which is best suited
 * for models where sequence of record ids is important.
 *
 * <p>This is the most optimal strategy so this class should be used whenever possible.
 */
@MappedSuperclass
public abstract class JpaModel extends Model {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
