/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2023 Axelor (<http://axelor.com>).
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
package com.axelor.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
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

  public Type getEventType() {
    return eventType;
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
    copy.qualifiers =
        new HashSet<>(Optional.ofNullable(this.qualifiers).orElse(Collections.emptySet()));

    Arrays.stream(qualifiers).forEach(copy.qualifiers::add);

    return copy;
  }
}
