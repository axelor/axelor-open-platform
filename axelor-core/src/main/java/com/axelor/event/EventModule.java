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

import com.google.inject.AbstractModule;
import com.google.inject.MembersInjector;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Qualifier;

public class EventModule extends AbstractModule {

  @Override
  protected void configure() {
    bindListener(
        Matchers.any(),
        new TypeListener() {

          @Override
          public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            Set<InjectionPoint> injectionPoints = findInjectionPoints(type);
            if (!injectionPoints.isEmpty()) {
              encounter.register(new EventTypeInjector<>(injectionPoints));
            }
          }
        });
  }

  private static Set<InjectionPoint> findInjectionPoints(TypeLiteral<?> type) {
    return InjectionPoint.forInstanceMethodsAndFields(type)
        .stream()
        .filter(ip -> ip.getMember() instanceof Field)
        .filter(
            ip ->
                ip.getDependencies()
                    .stream()
                    .map(d -> d.getKey())
                    .map(k -> k.getTypeLiteral())
                    .map(t -> t.getRawType())
                    .anyMatch(t -> t == Event.class))
        .collect(Collectors.toSet());
  }

  static class EventTypeInjector<T> implements MembersInjector<T> {

    private Set<InjectionPoint> injectionPoints;

    public EventTypeInjector(Set<InjectionPoint> injectionPoints) {
      this.injectionPoints = injectionPoints;
    }

    @Override
    public void injectMembers(T instance) {
      injectionPoints.forEach(ip -> injectMembers(instance, ip));
    }

    private void injectMembers(T instance, InjectionPoint ip) {
      Field field = (Field) ip.getMember();
      field.setAccessible(true);
      EventImpl<?> event;

      try {
        event = (EventImpl<?>) field.get(instance);
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new IllegalArgumentException(e);
      }

      ParameterizedType type = (ParameterizedType) field.getGenericType();
      event.setEventType(type.getActualTypeArguments()[0]);

      for (Annotation annotation : field.getAnnotations()) {
        if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
          event.addQualifier(annotation);
        }
      }
    }
  }
}
