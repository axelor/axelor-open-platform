/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2026 Axelor (<http://axelor.com>).
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

import com.axelor.db.EntityHelper;
import com.axelor.db.annotations.Widget;
import com.axelor.db.converters.EncryptedStringConverter;
import javax.persistence.Basic;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Table;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "STRATEGY_JOINED_CHILD")
public class StrategyJoinedChild extends StrategyJoined {

  private String myStringChild;

  @Convert(converter = EncryptedStringConverter.class)
  private String mySecureStringChild;

  @Widget(title = "Attributes")
  @Basic(fetch = FetchType.LAZY)
  @Type(type = "json")
  private String attrs;

  public StrategyJoinedChild() {}

  public String getMyStringChild() {
    return myStringChild;
  }

  public void setMyStringChild(String myStringChild) {
    this.myStringChild = myStringChild;
  }

  public String getMySecureStringChild() {
    return mySecureStringChild;
  }

  public void setMySecureStringChild(String mySecureStringChild) {
    this.mySecureStringChild = mySecureStringChild;
  }

  public String getAttrs() {
    return attrs;
  }

  public void setAttrs(String attrs) {
    this.attrs = attrs;
  }

  @Override
  public boolean equals(Object obj) {
    return EntityHelper.equals(this, obj);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  @Override
  public String toString() {
    return EntityHelper.toString(this);
  }
}
