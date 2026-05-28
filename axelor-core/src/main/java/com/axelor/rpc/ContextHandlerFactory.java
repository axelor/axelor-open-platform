/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.rpc;

import static net.bytebuddy.description.modifier.FieldPersistence.TRANSIENT;
import static net.bytebuddy.description.modifier.Visibility.PRIVATE;
import static net.bytebuddy.implementation.FieldAccessor.ofBeanProperty;
import static net.bytebuddy.implementation.MethodDelegation.toField;
import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isGetter;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isSetter;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;

import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.persistence.Entity;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

/** Factory to create {@link ContextHandler}. */
public final class ContextHandlerFactory {

  private static final ByteBuddy BYTE_BUDDY = new ByteBuddy();

  private static final String FIELD_HANDLER = "contextHandler";

  private static final String COMPUTE_METHOD_PREFIX = "compute";

  private static final LoadingCache<Class<?>, Class<?>> PROXY_CACHE =
      Caffeine.newBuilder().weakKeys().maximumSize(500).build(ContextHandlerFactory::makeProxy);

  private ContextHandlerFactory() {}

  private static boolean isEntity(Class<?> beanClass) {
    return !Modifier.isAbstract(beanClass.getModifiers())
        && beanClass.getAnnotation(Entity.class) != null;
  }

  /**
   * Asynchronously refresh proxy cache.
   *
   * @param models the entity classes for which proxy classes are required
   */
  public static void refresh(Collection<Class<?>> models) {
    PROXY_CACHE.invalidateAll();
    models.parallelStream().filter(ContextHandlerFactory::isEntity).forEach(PROXY_CACHE::refresh);
  }

  private static boolean hasJsonFields(Class<?> beanClass) {
    final Property attrs = Mapper.of(beanClass).getProperty(Context.KEY_JSON_ATTRS);
    return attrs != null && attrs.isJson();
  }

  private static <T> Class<? extends T> makeProxy(final Class<T> beanClass) {
    Builder<T> builder =
        BYTE_BUDDY
            .subclass(beanClass)
            .method(isPublic().and(isGetter().or(isSetter())))
            .intercept(toField(FIELD_HANDLER))
            .method(isProtected().and(nameStartsWith(COMPUTE_METHOD_PREFIX)))
            .intercept(toField(FIELD_HANDLER))
            .implement(ContextEntity.class)
            .intercept(toField(FIELD_HANDLER))
            .defineField(FIELD_HANDLER, ContextHandler.class, PRIVATE, TRANSIENT)
            .implement(HandlerAccessor.class)
            .intercept(ofBeanProperty());

    // allow to seamlessly handle json field values from scripts
    if (hasJsonFields(beanClass)) {
      builder =
          builder
              .implement(Map.class)
              .method(isDeclaredBy(Map.class))
              .intercept(
                  InvocationHandlerAdapter.of(
                      (proxy, method, args) ->
                          ((HandlerAccessor) proxy)
                              .getContextHandler()
                              .intercept(null, method, args)));
    }

    return builder.make().load(beanClass.getClassLoader()).getLoaded();
  }

  public static <T> ContextHandler<T> newHandler(Class<T> beanClass, Map<String, Object> values) {
    final ContextHandler<T> handler = new ContextHandler<>(beanClass, values);
    try {
      final Class<? extends T> proxyClass = PROXY_CACHE.get(beanClass).asSubclass(beanClass);
      final T proxy = proxyClass.getDeclaredConstructor().newInstance();
      ((HandlerAccessor) proxy).setContextHandler(handler);
      handler.setProxy(proxy);
      return handler;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static interface HandlerAccessor {

    public ContextHandler<?> getContextHandler();

    public void setContextHandler(ContextHandler<?> contextHandler);
  }
}
