/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the "License"); you may not use
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
 * Software distributed under the License is distributed on an "AS IS"
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
