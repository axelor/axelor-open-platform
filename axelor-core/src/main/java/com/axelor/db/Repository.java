/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.db;

import com.axelor.db.mapper.Property;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The repository interface defines common data access methods.
 *
 * @param <T> the domain model type the repository manages
 */
public interface Repository<T extends Model> {

  /**
   * Return list of properties on the domain model managed by this repository.
   *
   * @return list of {@link Property}
   */
  List<Property> fields();

  /**
   * Get the {@link Query} instance of the managed domain class.
   *
   * @return instance of {@link Query}
   */
  Query<T> all();

  /**
   * Create a new instance of the domain model with the given default values.
   *
   * @param values the default values
   * @return an instance of the domain model managed by this repository
   */
  T create(Map<String, Object> values);

  /**
   * Create a duplicate copy of the given entity.<br>
   * <br>
   * In case of deep copy, one-to-many records are duplicated. Otherwise, one-to-many records will
   * be skipped.
   *
   * @param entity the entity bean to copy
   * @param deep whether to create a deep copy
   * @return a copy of the given entity
   */
  T copy(T entity, boolean deep);

  /**
   * Finds an entity by its primary key.
   *
   * @param id the entity id to load
   * @return entity found by the given id, null otherwise
   */
  T find(Long id);

  /**
   * Finds an entity by its primary key.
   *
   * @param id the entity id to load
   * @return an {@code Optional} containing the found entity, or {@code Optional#empty()} if not
   *     found
   */
  Optional<T> findById(Long id);

  /**
   * Find multiple entities by their primary key.
   *
   * @param ids The ids to load
   * @return list of all the matched records
   * @see com.axelor.db.JPA#findByIds(Class, List)
   */
  List<T> findByIds(List<Long> ids);

  /**
   * Retrieves a reference (proxy) to an entity instance with the specified ID without immediately
   * loading its state from the database.
   *
   * <p>This method delegates to {@link jakarta.persistence.EntityManager#getReference(Class,
   * Object)}. It is designed for performance optimization, particularly when you need to associate
   * an entity (set a foreign key) without the overhead of a database SELECT query.
   *
   * <p><strong>Note:</strong> The returned object is likely a dynamic proxy. The database will only
   * be accessed when you invoke a method on the proxy (other than getting the ID). If the entity
   * does not exist in the database, an {@link jakarta.persistence.EntityNotFoundException} will be
   * thrown at the time of that access, not at the time of calling this method.
   *
   * @param id the primary key of the entity
   * @return a managed entity proxy instance with the state lazily fetched
   * @throws jakarta.persistence.EntityNotFoundException if the entity state is accessed, and the
   *     entity does not exist in the database
   * @see jakarta.persistence.EntityManager#getReference(Class, Object)
   */
  T getReferenceById(Long id);

  /**
   * Save the given entity.
   *
   * <p>Depending on the implementation, it may return same entity or a copy of it. For example JPA
   * implementation may return a copy if the given entity can't be managed.
   *
   * @param entity the entity object to save
   * @return an instance of the entity with saved state
   */
  T save(T entity);

  /**
   * Remove the given entity.
   *
   * @param entity the entity object
   */
  void remove(T entity);

  /**
   * Refresh the state of the instance from the database, overwriting changes made to the entity, if
   * any.
   *
   * @param entity the entity object to refresh
   */
  void refresh(T entity);

  /** Synchronize the persistence context to the underlying database. */
  void flush();

  /**
   * Validate the given json map before persisting.
   *
   * <p>This method is called before the json map is converted to model object.
   *
   * @param json the json map to validate
   * @param context the context
   * @return validated json map
   */
  Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context);

  /**
   * Populate the given json map with additional data.
   *
   * <p>This method is called before returning the json data as response.
   *
   * @param json the json map to populate
   * @param context the context
   * @return the json map itself
   */
  Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context);
}
