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

import com.axelor.inject.Beans;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.inject.Qualifier;

class Observer implements Comparable<Observer> {
  public final Method method;
  public final Type eventActualType;
  public final Class<?> eventRawType;
  public final Class<?> declaringClass;

  private int priority;
  private Set<Annotation> qualifiers = new HashSet<>();

  public Observer(Method method) {
    assert method.getParameters().length == 1;
    final Parameter param = method.getParameters()[0];

    for (Annotation annotation : param.getAnnotations()) {
      if (annotation instanceof Priority) {
        this.priority = ((Priority) annotation).value();
      }
      if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
        qualifiers.add(annotation);
      }
    }

    this.method = method;
    this.method.setAccessible(true);
    this.declaringClass = method.getDeclaringClass();
    this.eventActualType = param.getParameterizedType();
    this.eventRawType = param.getType();
  }

  public boolean matches(Type eventType, Set<Annotation> qualifiers) {
    return matches(eventType) && hasQualifiers(qualifiers);
  }

  public boolean matches(Type eventType) {
    if (isUnbounded(eventType) && !isUnbounded(eventActualType)) {
      return false;
    }
    return TypeToken.of(eventType).isSubtypeOf(eventActualType);
  }

  private boolean isUnbounded(Type type) {
    if (type instanceof ParameterizedType) {
      for (Type t : ((ParameterizedType) type).getActualTypeArguments()) {
        if (t instanceof WildcardType && t.getTypeName().equals("?")) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasQualifiers(Set<Annotation> qualifiers) {
    Objects.requireNonNull(qualifiers, "qualifiers cannot be null");
    // always match observers with no qualifiers
    if (this.qualifiers.isEmpty()) return true;
    if (qualifiers.isEmpty()) return false;
    return this.qualifiers.stream().allMatch(o -> qualifiers.stream().anyMatch(o::equals));
  }

  public static boolean isObserver(Method method) {
    return !Modifier.isAbstract(method.getModifiers())
        && !Modifier.isAbstract(method.getDeclaringClass().getModifiers())
        && method.getParameterCount() == 1
        && method.getParameters()[0].isAnnotationPresent(Observes.class);
  }

  public void invoke(Object event) {
    Object target = Beans.get(this.declaringClass);
    try {
      method.invoke(target, event);
    } catch (InvocationTargetException e) {
      // Exception raised by a synchronous or transactional observer for a synchronous event stops
      // the notification chain and the exception is propagated immediately.
      Throwable throwable = e.getCause();
      if (throwable instanceof RuntimeException) {
        throw (RuntimeException) throwable;
      }
      throw new ObserverException(throwable);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int compareTo(Observer o) {
    return priority - o.priority;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Observer)) return false;
    if (this == obj) return true;
    final Observer other = (Observer) obj;
    return Objects.equals(method, other.method);
  }

  @Override
  public int hashCode() {
    return Objects.hash(31, method);
  }

  @Override
  public String toString() {
    return "Observer(method=" + method + ", qualifiers=" + qualifiers + ")";
  }
}
