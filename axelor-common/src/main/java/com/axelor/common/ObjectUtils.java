/**
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
package com.axelor.common;

import java.util.Collection;
import java.util.Map;

/**
 * This class defines from static helper methods to deal with objects.
 * 
 */
public final class ObjectUtils {
	
	/**
	 * Check whether the given value is empty. <br>
	 * <br>
	 * An object value is empty if:
	 * <ul>
	 * <li>value is null</li>
	 * <li>value is string with length 0</li>
	 * <li>value is map/collection with size 0</li>
	 * </ul>
	 * 
	 * @param value
	 *            the object value to check
	 * @return true if empty false otherwise
	 */
	public static boolean isEmpty(Object value) {
		if (value == null) return true;
		if (value instanceof String && "".equals(value)) return true;
		if (value instanceof Map && ((Map<?, ?>) value).size() == 0) return true;
		if (value instanceof Collection && ((Collection<?>) value).size() == 0) return true;
		return false;
	}
}
