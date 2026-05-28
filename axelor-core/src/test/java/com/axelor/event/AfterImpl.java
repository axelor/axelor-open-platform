/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import java.lang.annotation.Annotation;

@SuppressWarnings("all")
class AfterImpl implements After {

  @Override
  public Class<? extends Annotation> annotationType() {
    return After.class;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof After;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
