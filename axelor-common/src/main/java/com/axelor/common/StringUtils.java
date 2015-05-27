/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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
	 * Strip the leading indentation from the given text.
	 * 
	 * @param text
	 *            the text to strip
	 * @return stripped text
	 */
	public static String stripIndent(String text) {
		String string = text.replaceAll("\\t", "    ");
		StringBuilder builder = new StringBuilder();
		int leading = 0;
		for(String line : string.split("\\n")) {
			if (line.trim().length() == 0) continue;
			int n = 0;
			while(n++ < line.length()) {
				if (!Character.isWhitespace(line.charAt(n))) break;
			}
			if (leading == 0 || n < leading) {
				leading = n;
			}
			if (n >= leading) {
				builder.append(line.substring(leading)).append("\n");
			}
		}
		return builder.toString();
	}
}
