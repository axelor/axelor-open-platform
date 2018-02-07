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
package com.axelor.common;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * This class defines from static helper methods to deal with objects.
 * 
 */
public final class ObjectUtils {

	/**
	 * Check whether the given value is an array.
	 *
	 * @param value
	 *            the value to check
	 * @return true if value is array false otherwise
	 */
	public static boolean isArray(Object value) {
		return value != null && value.getClass().isArray();
	}

	/**
	 * Check whether the given value is empty.
	 * <p>
	 * An object value is empty if:
	 * <ul>
	 * <li>value is null</li>
	 * <li>value is {@link Optional} and {@link Optional#empty()}</li>
	 * <li>value is {@link Array} with length 0</li>
	 * <li>value is {@link CharSequence} with length 0</li>
	 * <li>value is {@link Collection} or {@link Map} with size 0</li>
	 * </ul>
	 * 
	 * @param value
	 *            the object value to check
	 * @return true if empty false otherwise
	 */
	public static boolean isEmpty(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof Optional) {
			return !((Optional<?>) value).isPresent();
		}
		if (value.getClass().isArray()) {
			return Array.getLength(value) == 0;
		}
		if (value instanceof CharSequence) {
			return ((CharSequence) value).length() == 0;
		}
		if (value instanceof Collection) {
			return ((Collection<?>) value).size() == 0;
		}
		if (value instanceof Map) {
			return ((Map<?, ?>) value).size() == 0;
		}
		return false;
	}

	/**
	 * Check whether the given value is not empty.
	 *
	 * @param value
	 *            the object value to check
	 * @return true if empty false otherwise
	 * @see #isEmpty(Object)
	 */
	public static boolean notEmpty(Object value) {
		return !isEmpty(value);
	}
}
