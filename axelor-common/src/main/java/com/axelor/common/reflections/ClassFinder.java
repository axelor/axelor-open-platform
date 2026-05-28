/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.common.reflections;

import com.axelor.common.ClassUtils;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** The helper class to find sub types of a given super class. */
public final class ClassFinder<T> {

  private Class<T> type;
  private ClassLoader loader;

  private Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
  private Set<String> packages = new LinkedHashSet<>();
  private Set<String> pathPatterns = new LinkedHashSet<>();

  private boolean matchAll = true;

  ClassFinder(Class<T> type, ClassLoader loader) {
    this.type = type;
    this.loader = loader;
  }

  ClassFinder(Class<T> type) {
    this(type, ClassUtils.getDefaultClassLoader());
  }

  /**
   * Find with the given URL pattern.
   *
   * @param pattern the URL pattern
   * @return the same finder
   */
  public ClassFinder<T> byURL(String pattern) {
    Objects.requireNonNull(pattern, "pattern must not be null");
    pathPatterns.add(pattern);
    return this;
  }

  /**
   * Only search within the given package name.
   *
   * @param packageName the package name
   * @return same class finder instance
   */
  public ClassFinder<T> within(String packageName) {
    packages.add(packageName);
    return this;
  }

  /**
   * Search using the given class loader.
   *
   * @param loader the class loader
   * @return the class finder instance
   */
  public ClassFinder<?> using(ClassLoader loader) {
    this.loader = loader;
    return this;
  }

  /**
   * Only search classes with the given annotation.
   *
   * @param annotation the annotation to check
   * @return same class finder instance
   */
  public ClassFinder<T> having(final Class<? extends Annotation> annotation) {
    this.annotations.add(annotation);
    return this;
  }

  /**
   * In case of multiple {@link #having(Class)} calls, whether to check any one annotation (by
   * default all annotations are checked).
   *
   * @return same class finder instance
   */
  public ClassFinder<T> any() {
    this.matchAll = false;
    return this;
  }

  private boolean hasAnnotation(Class<?> cls) {
    boolean matched = false;
    for (Class<? extends Annotation> annotation : annotations) {
      if (cls.isAnnotationPresent(annotation)) {
        if (!matchAll) {
          return true;
        }
        matched = true;
      } else if (matchAll) {
        return false;
      }
    }
    return annotations.size() == 0 || matched;
  }

  /**
   * Find the classes.
   *
   * @return set of matched classes
   */
  public Set<Class<? extends T>> find() {
    final Set<Class<? extends T>> classes = new HashSet<>();
    final ClassScanner scanner = new ClassScanner(loader, packages.toArray(new String[] {}));

    for (String pattern : pathPatterns) {
      scanner.byURL(pattern);
    }

    if (Object.class == type && annotations.isEmpty()) {
      throw new IllegalStateException("please provide some annnotations.");
    }
    if (Object.class == type) {
      for (Class<?> a : annotations) {
        for (Class<?> c : scanner.getTypesAnnotatedWith(a)) {
          if (type.isAssignableFrom(c)) {
            classes.add(c.asSubclass(type));
          }
        }
      }
      return Collections.unmodifiableSet(classes);
    }
    final Set<Class<? extends T>> all = scanner.getSubTypesOf(type);
    for (Class<? extends T> cls : all) {
      if (hasAnnotation(cls)) {
        classes.add(cls);
      }
    }
    return Collections.unmodifiableSet(classes);
  }
}
