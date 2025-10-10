/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.script;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.db.Model;
import com.axelor.db.Repository;
import com.axelor.db.ValueEnum;
import com.axelor.inject.Beans;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

class ScriptPolicy {

  private static final long DEFAULT_TIMEOUT = 5 * 60_000L; // milliseconds (5 minutes)

  private static final String[] ALLOW_PACKAGES = {
    "java.lang", "java.util", "java.time.*", "java.text", "java.math",
  };

  private static final Class<?>[] ALLOW_CLASSES = {
    // java
    java.util.Map.class,
    java.util.Map.Entry.class,
    java.util.Iterator.class,
    java.lang.Iterable.class,
    java.lang.CharSequence.class,

    // axelor db classes
    com.axelor.db.Model.class,
    com.axelor.db.Query.class,
    com.axelor.db.Repository.class,
    com.axelor.db.ValueEnum.class,
    com.axelor.db.EntityHelper.class,
    com.axelor.i18n.I18n.class,
    com.axelor.i18n.L10n.class,

    // jakarta.persistence
    jakarta.persistence.Query.class,

    // axelor common utils
    com.axelor.common.StringUtils.class,
    com.axelor.common.ObjectUtils.class,
    com.axelor.common.HtmlUtils.class,
    com.axelor.common.Inflections.class,
    com.axelor.common.Inflector.class,

    // hibernate proxy and collections
    org.hibernate.proxy.HibernateProxy.class,
    org.hibernate.collection.spi.PersistentCollection.class,
    org.hibernate.collection.spi.LazyInitializable.class,

    // allow ContextConfig
    ConfigContext.class,

    // allow Context
    com.axelor.rpc.Context.class,
    com.axelor.rpc.JsonContext.class
  };

  private static final String[] DENY_PACKAGES = {};

  private static final Class<?>[] DENY_CLASSES = {
    Class.class, System.class, Process.class, ProcessBuilder.class, Thread.class, Properties.class
  };

  private final List<String> allowPackages;
  private final List<Class<?>> allowClasses;

  private final List<String> denyPackages;
  private final List<Class<?>> denyClasses;

  private final long timeout;

  private ScriptPolicy() {
    List<String> initialAllowPackages = new ArrayList<>();
    List<Class<?>> initialAllowClasses = new ArrayList<>();
    List<String> initialDenyPackages = new ArrayList<>();
    List<Class<?>> initialDenyClasses = new ArrayList<>();

    Beans.get(ScriptPolicyConfiguratorService.class)
        .configure(
            initialAllowPackages, initialAllowClasses, initialDenyPackages, initialDenyClasses);

    Collections.addAll(initialAllowPackages, ALLOW_PACKAGES);
    Collections.addAll(initialAllowClasses, ALLOW_CLASSES);

    Collections.addAll(initialDenyPackages, DENY_PACKAGES);
    Collections.addAll(initialDenyClasses, DENY_CLASSES);

    // Unmodifiable rules after configuration
    allowPackages = List.copyOf(initialAllowPackages);
    allowClasses = List.copyOf(initialAllowClasses);
    denyPackages = List.copyOf(initialDenyPackages);
    denyClasses = List.copyOf(initialDenyClasses);

    timeout =
        AppSettings.get().getLong(AvailableAppSettings.APPLICATION_SCRIPT_TIMEOUT, DEFAULT_TIMEOUT);
  }

  public static ScriptPolicy getInstance() {
    return Holder.INSTANCE;
  }

  public long getTimeout() {
    return timeout;
  }

  public <T> Class<T> check(Class<T> type) {
    if (type == null || allowed(type)) {
      return type;
    }
    throw new ScriptPolicyException(String.format("Class '%s' not allowed.", type.getName()));
  }

  private boolean match(String pattern, String pkg) {
    // wild-card package?
    if (pattern.endsWith(".*")) {
      return pkg.replace(pattern.substring(0, pattern.length() - 1), "").split("\\.").length == 1
          || pkg.equals(pattern.substring(0, pattern.length() - 2));
    }
    // exact match
    return pattern.equals(pkg);
  }

  public boolean allowed(Class<?> klass) {
    String pkg = klass.getPackageName();

    if (denyClasses.stream().anyMatch(x -> x.isAssignableFrom(klass))
        || denyPackages.stream().anyMatch(x -> match(x, pkg))) {
      return false;
    }

    if (isAnnotationPresent(klass, ScriptAllowed.class)
        || allowClasses.stream().anyMatch(x -> x.isAssignableFrom(klass))
        || allowPackages.stream().anyMatch(x -> match(x, pkg))) {
      return true;
    }

    return Model.class.isAssignableFrom(klass)
        || Repository.class.isAssignableFrom(klass)
        || ValueEnum.class.isAssignableFrom(klass);
  }

  private static boolean isAnnotationPresent(
      Class<?> klass, Class<? extends Annotation> annotation) {

    if (klass.isAnnotationPresent(annotation)
        || Arrays.stream(klass.getInterfaces())
            .anyMatch(cls -> isAnnotationPresent(cls, annotation))) {
      return true;
    }

    Class<?> superclass = klass.getSuperclass();

    return superclass != null && isAnnotationPresent(superclass, annotation);
  }

  private static class Holder {
    private static final ScriptPolicy INSTANCE = new ScriptPolicy();
  }
}
