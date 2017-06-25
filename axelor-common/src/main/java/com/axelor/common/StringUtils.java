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

/**
 * This class provides static helper methods for {@link String}.
 * 
 */
public final class StringUtils {

	/**
	 * Check whether the given string value is empty. The value is empty if null
	 * or length is 0.
	 * 
	 * @param value
	 *            the string value to test
	 * @return true if empty false otherwise
	 */
	public static boolean isEmpty(String value) {
		return value == null || value.length() == 0;
	}

	/**
	 * Check whether the given string value is not empty. The value is empty if
	 * null or length is 0.
	 *
	 * @param value
	 *            the string value to test
	 * @return true if not empty false otherwise
	 */
	public static boolean notEmpty(String value) {
		return !isEmpty(value);
	}

	/**
	 * Check whether the given string value is blank. The value is blank if null
	 * or contains white spaces only.
	 * 
	 * @param value
	 *            the string value to test
	 * @return true if empty false otherwise
	 */
	public static boolean isBlank(String value) {
		if (isEmpty(value)) {
			return true;
		}
		for (int i = 0; i < value.length(); i++) {
			if (!Character.isWhitespace(value.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Check whether the given string value is not blank. The value is blank if
	 * null or contains white spaces only.
	 *
	 * @param value
	 *            the string value to test
	 * @return true if not empty false otherwise
	 */
	public static boolean notBlank(String value) {
		return !isBlank(value);
	}

	/**
	 * Strip the leading indentation from the given text.
	 * 
	 * @param text
	 *            the text to strip
	 * @return stripped text
	 */
	public static String stripIndent(String text) {
		if (isBlank(text)) {
			return text;
		}

		final String[] lines = text.split("\\n");
		final StringBuilder builder = new StringBuilder();

		int leading = -1;
		for (String line : lines) {
			if (isBlank(line)) { continue; }
			int index = 0;
			int length = line.length();
			if (leading == -1) {
				leading = length;
			}
			while(index < length && index < leading && Character.isWhitespace(line.charAt(index))) { index++; }
			if (leading > index) {
				leading = index;
			}
		}

		for(String line : lines) {
			if (!isBlank(line)) {
				builder.append(leading <= line.length() ? line.substring(leading) : "");
			}
			builder.append("\n");
		}

		return builder.toString();
	}
}
