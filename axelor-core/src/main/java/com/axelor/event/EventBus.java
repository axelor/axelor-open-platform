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

import com.google.inject.Injector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class EventBus {

  private Injector injector;

  private Map<Class<?>, List<Observer>> observers;

  private Object lock = new Object();

  @Inject
  public EventBus(Injector injector) {
    this.injector = injector;
  }

  private List<Observer> find(Class<?> runtimeType, Type eventType, Set<Annotation> qualifiers) {
    if (observers == null) {
      synchronized (lock) {
        observers = new ConcurrentHashMap<>();
        injector
            .getAllBindings()
            .entrySet()
            .stream()
            .map(e -> e.getKey())
            .map(k -> k.getTypeLiteral())
            .map(t -> t.getRawType())
            .flatMap(t -> Arrays.stream(t.getDeclaredMethods()))
            .filter(m -> Observer.isObserver(m))
            .map(m -> new Observer(m))
            .forEach(
                o -> {
                  List<Observer> items =
                      observers.computeIfAbsent(o.eventRawType, k -> new ArrayList<>());
                  items.add(o);
                });
        observers.values().forEach(items -> Collections.sort(items, Observer::compareTo));
      }
    }

    final List<Observer> found = observers.getOrDefault(runtimeType, Collections.emptyList());
    final Set<Annotation> annotations = qualifiers == null ? Collections.emptySet() : qualifiers;

    return found
        .stream()
        .filter(o -> o.matches(eventType, annotations))
        .collect(Collectors.toList());
  }

  public void fire(Object event, Type eventType, Set<Annotation> qualifiers) {
    Class<?> t = event.getClass();
    this.find(t, eventType, qualifiers).forEach(o -> o.invoke(event));
  }
}
