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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Timer;

class ScriptPolicy {

  private static final long DEFAULT_TIMEOUT = 5 * 60_000L; // milliseconds (5 minutes)

  private static final String[] ALLOW_PACKAGES = {
    "java.lang", "java.util", "java.util.regex", "java.time.*", "java.text", "java.math",
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

    // axelor rpc classes
    com.axelor.rpc.Context.class,
    com.axelor.rpc.JsonContext.class,
    com.axelor.rpc.ActionRequest.class,
    com.axelor.rpc.ActionResponse.class
  };

  private static final String[] DENY_PACKAGES = {
    // Short term, see the note in DENY_CLASSES: libraries axelor-core does not compile against.
    // BeanMap calls the getters and setters of any object given to it
    "org.apache.commons.beanutils",
    "org.springframework.cglib.beans",
    // BeanMap as above, and map.MultiValueMap.decorate(Map, Class) instantiates any class: the
    // whole of commons-collections 3, whose sub-packages are all one level deep
    "org.apache.commons.collections.*",
    // MultiValueMap.multiValueMap(Map, Class) instantiates any class
    "org.apache.commons.collections4.map",
    // AutoPopulatingList(Class) instantiates any class
    "org.springframework.util",
    // event loop and executor groups: start threads running script code
    "io.netty.channel.*",
    "io.netty.util.concurrent",
    // ServiceLoaderIterator instantiates service providers (ServiceLoader.load is guarded)
    "org.apache.commons.compress.utils",
    // ExtProperties (a Hashtable/Map) reads any key=value file given by path
    "org.apache.velocity.util",
  };

  private static final Class<?>[] DENY_CLASSES = {
    Class.class,
    ClassLoader.class,
    Process.class,
    ProcessBuilder.class,
    ProcessHandle.class,
    Runtime.class,
    System.class,
    Thread.class,
    ThreadGroup.class,
    Properties.class,
    ResourceBundle.class,
    Timer.class,
    // writes files: new Formatter('/path/to/file')
    Formatter.class,
    // reads any stream, file or channel given to it (the stream itself is not allowed)
    Scanner.class,
    // allowed as an Iterable, and reads or writes files through the groovy-nio extension methods
    Path.class,

    // Short term, until the class-level and nested class rules are reworked: the interfaces of
    // ALLOW_CLASSES (Map, Iterable...) allow any class implementing them, and a nested class is
    // checked as its top-level class, which lets these classes in (see also DENY_PACKAGES).
    // UIDefaults.ProxyLazyValue calls any public static method or constructor by name
    javax.swing.UIDefaults.class,
    // BeanContextSupport.instantiateChild instantiates any class by name
    java.beans.beancontext.BeanContext.class,
    // its builder reads any file given by path
    org.apache.commons.csv.CSVParser.class,
    // allowed as a Map; reads local files, classpath resources and URLs by path
    org.apache.shiro.config.Ini.class,
    // allowed as a List; its getProperty reads a property from each element through a metaclass
    // scripts can replace (setMetaClass), unchecked
    groovy.lang.Sequence.class,
    // allowed as a List; its static setMetaClass(Class, MetaClass) replaces the metaclass of any
    // class for the whole JVM
    groovy.util.NodeList.class,
    // allowed as an Iterable; load(Class) instantiates service providers, as ServiceLoader.load
    org.jboss.resteasy.spi.PriorityServiceLoader.class
  };

  private final List<String> allowPackages;
  private final List<Class<?>> allowClasses;

  private final List<String> denyPackages;
  private final List<Class<?>> denyClasses;

  private final long timeout;

  // Bound to the class, so it does not keep it from being unloaded.
  private final ClassValue<Boolean> allowedCache =
      new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
          return computeAllowed(type);
        }
      };

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

  /**
   * Whether the package matches the pattern: {@code a.b} matches only the package {@code a.b},
   * {@code a.b.*} matches {@code a.b} and its immediate sub-packages ({@code a.b.c}, not {@code
   * a.b.c.d}).
   */
  static boolean match(String pattern, String pkg) {
    if (pattern.endsWith(".*")) {
      String base = pattern.substring(0, pattern.length() - 2);
      if (pkg.equals(base)) {
        return true;
      }
      String prefix = base + ".";
      return pkg.startsWith(prefix) && pkg.indexOf('.', prefix.length()) < 0;
    }
    return pattern.equals(pkg);
  }

  /**
   * Whether the class is allowed. The answer is computed once per class and cached: the rules
   * cannot change once the policy is created, and this is called for every receiver, constructor,
   * conversion, etc. of every script.
   */
  public boolean allowed(Class<?> klass) {
    return allowedCache.get(klass);
  }

  /** Computes whether the class is allowed, see {@link #allowed(Class)}. */
  boolean computeAllowed(Class<?> klass) {
    // an array gives access to its elements only, so it is checked as its element type
    while (klass.isArray()) {
      klass = klass.getComponentType();
    }
    String pkg = klass.getPackageName();
    Class<?> cls = getTopClass(klass);

    if (denyClasses.stream().anyMatch(x -> x.isAssignableFrom(cls))
        || denyPackages.stream().anyMatch(x -> match(x, pkg))
        || isCollectionProxy(cls)) {
      return false;
    }

    if (isAnnotationPresent(cls, ScriptAllowed.class)
        || allowClasses.stream().anyMatch(x -> x.isAssignableFrom(cls))
        || allowPackages.stream().anyMatch(x -> match(x, pkg))) {
      return true;
    }

    return Model.class.isAssignableFrom(cls)
        || Repository.class.isAssignableFrom(cls)
        || ValueEnum.class.isAssignableFrom(cls);
  }

  /** The class loader of the proxy classes Groovy generates, not public. */
  private static final String PROXY_LOADER =
      "org.codehaus.groovy.runtime.ProxyGeneratorAdapter$InnerLoader";

  /**
   * Whether the class is a collection proxy Groovy generates when it converts a map or a closure to
   * a class, explicitly ({@code [size: { 1 }] as ArrayList}) or implicitly, to a variable or
   * parameter type ({@code ForwardingList l = { -> list }}). It would be allowed as its collection
   * supertype, but it is a {@code GroovyObject} whose metaclass reads a property it does not have
   * from each of its elements, unchecked ({@code l.getProperty('env')}), unlike the collections
   * declared in scripts (see {@link GroovySandbox.CollectionMetaClass}).
   */
  private static boolean isCollectionProxy(Class<?> klass) {
    final ClassLoader loader = klass.getClassLoader();
    return loader != null
        && Collection.class.isAssignableFrom(klass)
        && PROXY_LOADER.equals(loader.getClass().getName());
  }

  private static Class<?> getTopClass(Class<?> klass) {
    Class<?> current = klass;
    while (current.getEnclosingClass() != null) {
      current = current.getEnclosingClass();
    }
    return current;
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
