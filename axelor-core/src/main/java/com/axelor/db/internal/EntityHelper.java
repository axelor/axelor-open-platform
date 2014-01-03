/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.db.internal;

import java.util.Arrays;
import java.util.List;

import com.axelor.db.Model;
import com.axelor.db.annotations.HashKey;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.db.mapper.PropertyType;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.collect.Lists;

/**
 * This class provides helper methods for model objects.
 *
 */
public final class EntityHelper {

	private static boolean isSimple(Property field) {
		if (field.isPrimary() || field.isVersion() || field.isCollection()
				|| field.isReference() || field.isVirtual() || field.isImage()
				|| field.getType() == PropertyType.TEXT
				|| field.getType() == PropertyType.BINARY) {
			return false;
		}
		return true;
	}

	/**
	 * The toString helper method.
	 *
	 * @param entity
	 *            generate toString for the given entity
	 * @return string
	 */
	public static <T extends Model> String toString(T entity) {
		if (entity == null) {
			return null;
		}
		final Mapper mapper = Mapper.of(entity.getClass());
		final ToStringHelper helper = Objects.toStringHelper(entity);

		helper.add("id", entity.getId());
		for (Property field : mapper.getProperties()) {
			if (isSimple(field)) {
				helper.add(field.getName(), field.get(entity));
			}
		}
		return helper.omitNullValues().toString();
	}

	/**
	 * The hashCode helper method.
	 *
	 * This method searches for all the fields marked with {@link HashKey} and
	 * uses them to generate the hash code.
	 *
	 * @param entity
	 *            generate the hashCode for the given entity
	 * @return hashCode
	 */
	public static <T extends Model> int hashCode(T entity) {
		if (entity == null) {
			return 0;
		}
		final Mapper mapper = Mapper.of(entity.getClass());
		final List<Object> values = Lists.newArrayList();

		for (Property p : mapper.getProperties()) {
			if (isSimple(p) && p.isHashKey()) {
				values.add(p.get(entity));
			}
		}

		if (values.isEmpty()) {
			return 0;
		}
		return Arrays.hashCode(values.toArray());
	}

	/**
	 * The equals helper method.
	 *
	 * This method searches for all the fields marked with {@link HashKey} and
	 * uses them to check for the equality.
	 *
	 * @param entity
	 *            the current entity
	 * @param other
	 *            the other entity
	 *
	 * @return true if both the objects are equals by their hashing keys
	 */
	public static <T extends Model> boolean equals(T entity, T other) {
		if (entity == other)
			return true;
		if (entity == null || other == null)
			return false;
		if (!entity.getClass().isInstance(other))
			return false;

		if (entity.getId() != null && other.getId() != null) {
			return Objects.equal(entity.getId(), other.getId());
		}

		final Mapper mapper = Mapper.of(entity.getClass());
		boolean hasHashKeys = false;

		for (Property field : mapper.getProperties()) {
			if (field.isHashKey()) {
				hasHashKeys = true;
				if (!Objects.equal(field.get(entity), field.get(other))) {
					return false;
				}
			}
		}

		return !hasHashKeys;
	}
}
