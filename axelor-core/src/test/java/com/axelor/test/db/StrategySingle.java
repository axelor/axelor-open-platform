/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
package com.axelor.test.db;

import com.axelor.auth.db.AuditableModel;
import com.google.common.base.MoreObjects;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "TEST_STRATEGY_SINGLE")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class StrategySingle extends AuditableModel {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TEST_STRATEGY_SINGLE_SEQ")
  @SequenceGenerator(
      name = "TEST_STRATEGY_SINGLE_SEQ",
      sequenceName = "TEST_STRATEGY_SINGLE_SEQ",
      allocationSize = 1)
  private Long id;

  private Integer myField = 0;

  public StrategySingle() {}

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
    this.id = id;
  }

  public Integer getMyField() {
    return myField == null ? 0 : myField;
  }

  public void setMyField(Integer myField) {
    this.myField = myField;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (this == obj) return true;
    if (!(obj instanceof StrategySingle)) return false;

    final StrategySingle other = (StrategySingle) obj;
    if (this.getId() != null || other.getId() != null) {
      return Objects.equals(this.getId(), other.getId());
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 31;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", getId())
        .add("myField", getMyField())
        .omitNullValues()
        .toString();
  }
}
