/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import java.lang.annotation.Annotation;

@SuppressWarnings("all")
class BeforeImpl implements Before {

  @Override
  public Class<? extends Annotation> annotationType() {
    return Before.class;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Before;
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
