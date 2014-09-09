/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.tools.x2j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.groovy.runtime.StringGroovyMethods;

public class Utils {

	private static final Pattern TRAILING_WS = Pattern.compile("\\s+$", Pattern.MULTILINE);

	/**
	 * Strip the extra leading white space from the code string.
	 *
	 */
	public static String stripCode(String code, String joinWith) {
		if (code == null || code.trim().length() == 0) {
			return "";
		}
		String text = StringGroovyMethods.stripIndent(code.replaceAll("    ", "\t"));
		text = text.trim().replaceAll("\n", joinWith).trim();
		return text;
	}

	public static String stringTrailing(String code) {
		if (code == null || code.trim().length() == 0) {
			return "";
		}
		Matcher matcher = TRAILING_WS.matcher(code);
		return matcher.replaceAll("\n");
	}

	public static String firstUpper(String string) {
		return string.substring(0, 1).toUpperCase() + string.substring(1);
	}

	public static String firstLower(String string) {
		return string.substring(0, 1).toLowerCase() + string.substring(1);
	}
}
