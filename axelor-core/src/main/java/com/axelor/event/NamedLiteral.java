/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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
package com.axelor.event;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import javax.inject.Named;

@SuppressWarnings("all")
public class NamedLiteral implements Named, Serializable {

  private static final long serialVersionUID = 3230933387532064885L;

  private final String value;

  private NamedLiteral(String value) {
    this.value = value;
  }

  @Override
  public Class<? extends Annotation> annotationType() {
    return Named.class;
  }

  public static NamedLiteral of(String value) {
    return new NamedLiteral(value);
  }

  @Override
  public String value() {
    return value;
  }

  public int hashCode() {
    return (127 * "value".hashCode()) ^ value.hashCode();
  }

  public boolean equals(Object o) {
    if (!(o instanceof Named)) {
      return false;
    }

    Named other = (Named) o;
    return value.equals(other.value());
  }

  public String toString() {
    return "@" + Named.class.getName() + "(value=" + value + ")";
  }
}
