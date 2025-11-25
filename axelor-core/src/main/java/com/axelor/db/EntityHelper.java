/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
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

  /**
   * Retrieves the {@link EntityPersister} for the given entity class model.
   *
   * @param model the entity class for which the EntityPersister is to be fetched
   * @return the EntityPersister associated with the given entity class
   */
  public static EntityPersister getEntityPersister(Class<?> model) {
    SessionFactoryImplementor sessionFactory =
        JPA.em().getEntityManagerFactory().unwrap(SessionFactoryImplementor.class);
    return sessionFactory.getMetamodel().entityPersister(model);
  }

  /**
   * Determines whether the specified model class is safe for bulk update operations.
   *
   * <p>As soon as the model involves multi tables to update, it shouldn't be safe for bulk update.
   *
   * @param model the entity class to check for bulk update safety
   * @return true if the model is safe for bulk update; false otherwise
   */
  public static boolean isSafeForBulkUpdate(Class<?> model) {
    EntityPersister entityPersister = getEntityPersister(model);

    // Case TABLE_PER_CLASS
    // isMultiTable() -> isAbstract() || hasSubclasses() -> implies UNION query -> Unsafe
    if (entityPersister instanceof UnionSubclassEntityPersister) {
      return !((UnionSubclassEntityPersister) entityPersister).isMultiTable();
    }

    // Case JOINED
    // getTableSpan() < 2 -> implies Root Entity (1 table) -> Safe
    // If getTableSpan() >= 2 -> implies Child Entity (joined tables) -> Unsafe
    if (entityPersister instanceof JoinedSubclassEntityPersister) {
      return ((JoinedSubclassEntityPersister) entityPersister).getTableSpan() < 2;
    }

    // Case SINGLE_TABLE (Default) -> Always Safe
    return true;
  }
}
