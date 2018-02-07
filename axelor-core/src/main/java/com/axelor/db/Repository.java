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
package com.axelor.db;

import java.util.List;
import java.util.Map;

import com.axelor.db.mapper.Property;

/**
 * The repository interface defines common data access methods.
 *
 * @param <T>
 *            the domain model type the repository manages
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
	 * @param values
	 *            the default values
	 * @return an instance of the domain model managed by this repository
	 */
	T create(Map<String, Object> values);

	/**
	 * Create a duplicate copy of the given entity.<br>
	 * <br>
	 * In case of deep copy, one-to-many records are duplicated. Otherwise,
	 * one-to-many records will be skipped.
	 *
	 * @param entity
	 *            the entity bean to copy
	 * @param deep
	 *            whether to create a deep copy
	 * @return a copy of the given entity
	 */
	T copy(T entity, boolean deep);

	/**
	 * Find by primary key.
	 *
	 * @param id
	 *            the record id
	 * @return a domain object found by the given id, null otherwise
	 */
	T find(Long id);

	/**
	 * Save the given entity.
	 * <p>
	 * Depending on the implementation, it may return same entity or a copy of
	 * it. For example JPA implementation may return a copy if the given entity
	 * can't be managed.
	 * </p>
	 *
	 * @param entity
	 *            the entity object to save
	 * @return an instance of the entity with saved state
	 */
	T save(T entity);

	/**
	 * Remove the given entity.
	 *
	 * @param entity
	 *            the entity object
	 */
	void remove(T entity);

	/**
	 * Refresh the state of the instance from the database, overwriting changes
	 * made to the entity, if any.
	 *
	 * @param entity
	 *            the entity object to refresh
	 */
	void refresh(T entity);

	/**
	 * Synchronize the persistence context to the underlying database.
	 *
	 */
	void flush();

	/**
	 * Validate the given json map before persisting.
	 *
	 * <p>
	 * This method is called before the json map is converted to model object.
	 * </p>
	 *
	 * @param json
	 *            the json map to validate
	 * @param context
	 *            the context
	 * @return validated json map
	 */
	Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context);

	/**
	 * Populate the given json map with additional data.
	 * 
	 * <p>
	 * This method is called before returning the json data as response.
	 * </p>
	 *
	 * @param json
	 *            the json map to populate
	 * @param context
	 *            the context
	 * @return the json map itself
	 */
	Map<String, Object> populate(Map<String, Object> json, Map<String, Object> context);
}
