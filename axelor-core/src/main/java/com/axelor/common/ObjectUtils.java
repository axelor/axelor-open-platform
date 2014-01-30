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
