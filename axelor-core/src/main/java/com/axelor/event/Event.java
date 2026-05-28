/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import com.google.inject.ImplementedBy;
import java.lang.annotation.Annotation;

@ImplementedBy(EventImpl.class)
public interface Event<T> {

  void fire(T event);

  Event<T> select(Annotation... qualifiers);
}
