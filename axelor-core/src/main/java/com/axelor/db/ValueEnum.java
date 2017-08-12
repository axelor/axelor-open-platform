/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import java.util.Objects;

/**
 * Enum type fields with custom values should implement this interface.
 *
 */
public interface ValueEnum<T> {

	/**
	 * Get the value.
	 * 
	 * @return custom value associated with this enum constant.
	 */
	T getValue();

	/**
	 * Get the constant of the specified enum type with the specified value.
	 * 
	 * @param <V>
	 *            the value type
	 * @param <T>
	 *            the enum type whose constant is to be returned
	 * @param enumType
	 *            the {@code Class} object of the enum type from which to return a
	 *            constant
	 * @param value
	 *            the value of the constant to return
	 * 
	 * @return the enum constant of the specified enum type with the specified value
	 * @throws NullPointerException
	 *             if the specified value is null
	 * @throws IllegalArgumentException
	 *             if specified enumType is not an enum or no constant found for the
	 *             specified value
	 */
	@SuppressWarnings("unchecked")
	static <V, T extends ValueEnum<V>> T of(Class<T> enumType, V value) {
		if (value == null) {
			throw new NullPointerException("Value is null.");
		}
		if (!enumType.isEnum()) {
			throw new IllegalArgumentException("Not enum type " + enumType.getCanonicalName());
		}
		for (T item : enumType.getEnumConstants()) {
			if (Objects.equals(item.getValue(), value)) {
				return item;
			}
		}
		if (value instanceof String) {
			try {
				return (T) Enum.valueOf(enumType.asSubclass(Enum.class), (String) value);
			} catch (Exception e) {
			}
		}
		throw new IllegalArgumentException(
				"No enum constant found in " + enumType.getCanonicalName() + " for value: " + value);
	}
}
