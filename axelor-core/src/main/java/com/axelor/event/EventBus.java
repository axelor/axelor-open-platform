/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.event;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.LinkedKeyBinding;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
class EventBus {

  private final Injector injector;

  private final AtomicReference<List<Observer>> observersRef = new AtomicReference<>();

  private final LoadingCache<Class<?>, Map<Entry<Type, Set<Annotation>>, List<Observer>>>
      observersCache = Caffeine.newBuilder().weakKeys().build(k -> new ConcurrentHashMap<>());

  @Inject
  public EventBus(Injector injector) {
    this.injector = injector;
  }

  private List<Observer> findObservers() {
    return injector.getAllBindings().entrySet().stream()
        .filter(entry -> !(entry.getValue() instanceof LinkedKeyBinding))
        .map(Entry::getKey)
        .map(Key::getTypeLiteral)
        .map(TypeLiteral::getRawType)
        .flatMap(this::getAllObservers)
        .sorted(Observer::compareTo)
        .collect(Collectors.toList());
  }

  /**
   * Walks the full class hierarchy of {@code bindingClass} collecting all observer methods,
   * including ones inherited from superclasses. When a subclass overrides a superclass observer
   * method, only the subclass version is kept.
   */
  private Stream<Observer> getAllObservers(Class<?> bindingClass) {
    final List<Observer> result = new ArrayList<>();
    final Set<String> seenSignatures = new HashSet<>();
    Class<?> current = bindingClass;
    while (current != null && current != Object.class) {
      for (Method method : current.getDeclaredMethods()) {
        if (Observer.isObserver(method)
            && (current == bindingClass || !Modifier.isPrivate(method.getModifiers()))) {
          final String signature = method.getName() + Arrays.toString(method.getParameterTypes());
          if (seenSignatures.add(signature)) {
            result.add(new Observer(method, bindingClass));
          }
        }
      }
      current = current.getSuperclass();
    }
    return result.stream();
  }

  private List<Observer> find(Type eventType, Set<Annotation> qualifiers) {
    final List<Observer> allObservers =
        observersRef.updateAndGet(observers -> observers != null ? observers : findObservers());
    final Set<Annotation> annotations =
        Optional.ofNullable(qualifiers).orElse(Collections.emptySet());

    return allObservers.stream()
        .filter(o -> o.matches(eventType, annotations))
        .collect(Collectors.toList());
  }

  public void fire(Object event, Type eventType, Set<Annotation> qualifiers) {
    final Map<Entry<Type, Set<Annotation>>, List<Observer>> observersByTypeAndQualifiers =
        observersCache.get(event.getClass());
    final List<Observer> foundObservers =
        observersByTypeAndQualifiers.computeIfAbsent(
            new SimpleImmutableEntry<>(eventType, qualifiers), k -> find(k.getKey(), k.getValue()));
    foundObservers.forEach(o -> o.invoke(event));
  }
}
