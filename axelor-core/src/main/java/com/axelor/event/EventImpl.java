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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

class EventImpl<T> implements Event<T> {

  private EventBus eventBus;

  private Type eventType;

  private Set<Annotation> qualifiers;

  @Inject
  public EventImpl(EventBus eventBus) {
    this.eventBus = eventBus;
  }

  public void setEventType(Type eventType) {
    this.eventType = eventType;
  }

  public void addQualifier(Annotation qualifier) {
    if (this.qualifiers == null) {
      this.qualifiers = new HashSet<>();
    }
    this.qualifiers.add(qualifier);
  }

  @Override
  public void fire(T event) {
    eventBus.fire(event, eventType, qualifiers);
  }

  @Override
  public Event<T> select(Annotation... qualifiers) {
    if (qualifiers == null || qualifiers.length == 0) {
      return this;
    }

    EventImpl<T> copy = new EventImpl<>(eventBus);
    copy.eventType = eventType;
    copy.qualifiers = new HashSet<>();

    Arrays.stream(qualifiers).forEach(copy.qualifiers::add);

    return copy;
  }
}
