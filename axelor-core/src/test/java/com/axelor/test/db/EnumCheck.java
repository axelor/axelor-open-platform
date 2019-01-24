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
package com.axelor.test.db;

import com.axelor.db.JpaModel;
import com.google.common.base.MoreObjects;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "CONTACT_ENUM_CHECK")
public class EnumCheck extends JpaModel {

  @Basic
  @Enumerated(EnumType.STRING)
  private EnumStatus status;

  @Basic
  @Type(type = "com.axelor.db.hibernate.type.ValueEnumType")
  private EnumStatusNumber statusNumber;

  public EnumStatus getStatus() {
    return status;
  }

  public void setStatus(EnumStatus status) {
    this.status = status;
  }

  public EnumStatusNumber getStatusNumber() {
    return statusNumber;
  }

  public void setStatusNumber(EnumStatusNumber statusNumber) {
    this.statusNumber = statusNumber;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass())
        .add("status", status)
        .add("statusNumber", statusNumber)
        .toString();
  }
}
