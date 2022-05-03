/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
package com.axelor.db;

import com.axelor.db.annotations.EqualsInclude;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.axelor.rpc.Context;
import com.axelor.rpc.ContextEntity;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hibernate.proxy.HibernateProxy;

/** This class provides helper methods for model objects. */
public final class EntityHelper {

  private static boolean isSimple(Property field) {
    if (field.isTransient()
        || field.isPrimary()
        || field.isVersion()
        || field.isCollection()
        || field.isReference()
        || field.isVirtual()
        || field.isImage()
        || field.getType() == PropertyType.TEXT
        || field.getType() == PropertyType.BINARY) {
      return false;
    }
    return true;
  }

  /**
   * The toString helper method.
   *
   * @param <T> type of the entity
   * @param entity generate toString for the given entity
   * @return string
   */
  public static <T extends Model> String toString(T entity) {
    if (entity == null) {
      return null;
    }
    final Mapper mapper = Mapper.of(entity.getClass());
    final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(entity);

    helper.add("id", entity.getId());
    for (Property field : mapper.getProperties()) {
      if (isSimple(field) && !field.isPassword()) {
        helper.add(field.getName(), field.get(entity));
      }
    }
    return helper.omitNullValues().toString();
  }

  /**
   * The equals helper method.
   *
   * <p>This method searches for all the fields marked with {@link EqualsInclude} and uses them to
   * check for the equality.
   *
   * @param <T> type of the entity
   * @param entity the current entity
   * @param other the other entity
   * @return true if both objects are equal by their fields included for equality check
   */
  public static <T extends Model> boolean equals(T entity, Object other) {
    if (entity == other) {
      return true;
    }
    if (entity == null || other == null) {
      return false;
    }
    if (!entity.getClass().isInstance(other)) {
      return false;
    }
    final Model that = (Model) other;
    if (entity.getId() != null || that.getId() != null) {
      return Objects.equals(entity.getId(), that.getId());
    }

    final Mapper mapper = Mapper.of(entity.getClass());
    final List<Object> equalsValues = new ArrayList<>();

    for (Property field : mapper.getProperties()) {
      if (field.isEqualsInclude()) {
        final Object value = field.get(entity);
        equalsValues.add(value);
        if (!Objects.equals(value, field.get(other))) {
          return false;
        }
      }
    }

    return equalsValues.stream().anyMatch(Objects::nonNull);
  }

  /**
   * Get the real persistence class of the given entity.
   *
   * <p>This method can be used to find real class name of a proxy object returned by hibernate
   * entity manager or {@link Context#asType(Class) } instance.
   *
   * @param <T> type of the entity
   * @param entity an entity instance
   * @return real class of the entity
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getEntityClass(T entity) {
    Preconditions.checkNotNull(entity);
    if (entity instanceof HibernateProxy) {
      return ((HibernateProxy) entity).getHibernateLazyInitializer().getPersistentClass();
    }
    Class<?> klass = entity.getClass();
    while (ContextEntity.class.isAssignableFrom(klass)) {
      klass = klass.getSuperclass();
    }
    return (Class<T>) klass;
  }

  /**
   * Get unproxied instance of the given entity.
   *
   * <p>This method can be used to convert hibernate proxy object to real implementation instance.
   *
   * <p>If called for instances returned with {@link Context#asType(Class) }, it returns partial
   * context instance.
   *
   * @param <T> type of the entity
   * @param entity proxied entity
   * @return unproxied instance of the entity
   */
  @SuppressWarnings("unchecked")
  public static <T> T getEntity(T entity) {
    if (entity == null) {
      return null;
    }
    if (entity instanceof HibernateProxy) {
      return (T) ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
    }
    if (entity instanceof ContextEntity) {
      return (T) ((ContextEntity) entity).getContextEntity();
    }
    return entity;
  }

  /**
   * Check whether the given lazy loading proxy instance is uninitialized.
   *
   * @param <T> type of the entity
   * @param entity the lazy loading entity instance
   * @return true if uninitialized false otherwise
   */
  public static <T> boolean isUninitialized(T entity) {
    return entity instanceof HibernateProxy
        && ((HibernateProxy) entity).getHibernateLazyInitializer().isUninitialized();
  }
}
