/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import jakarta.inject.Named;
import java.io.Serializable;
import java.lang.annotation.Annotation;

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
