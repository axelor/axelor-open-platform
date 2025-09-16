/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import jakarta.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
