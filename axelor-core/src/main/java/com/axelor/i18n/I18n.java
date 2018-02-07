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
package com.axelor.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import com.axelor.app.internal.AppFilter;
import com.axelor.common.StringUtils;

/**
 * This class provider methods for internationalization (I18n) services.
 * 
 */
public final class I18n {

	private I18n() {
	
	}

	/**
	 * Get a resource bundle for the given locale.
	 * 
	 * @param locale
	 *            the locale for which a resource bundle is desired
	 * @return an instance of {@link I18nBundle}
	 */
	public static ResourceBundle getBundle(Locale locale) {
		if (locale == null) {
			locale = Locale.getDefault();
		}
		return ResourceBundle.getBundle("axelor", locale, new I18nControl());
	}

	/**
	 * Get the resource bundle for the current {@link Locale}.
	 * 
	 * @return an instance of {@link I18nBundle}
	 */
	public static ResourceBundle getBundle() {
		return getBundle(AppFilter.getLocale());
	}

	/**
	 * Return the localized translation of the given message, based on the
	 * current language.
	 * 
	 * @param message
	 *            the original message to be translated
	 * @return the localized message or the message itself if not found.
	 */
	public static String get(String message) {
		if (StringUtils.isBlank(message)) return message;
		return getBundle().getString(message);
	}

	/**
	 * Similar to the {@link #get(String)} but considers plural form based on
	 * the given number value.
	 * 
	 * <pre>
	 * assertEquals(&quot;1 record selected.&quot;, I18n.get(&quot;{0} record selected.&quot;, &quot;{0} records selected.&quot;, 1));
	 * assertEquals(&quot;5 records selected.&quot;, I18n.get(&quot;{0} record selected.&quot;, &quot;{0} records selected.&quot;, 5));
	 * </pre>
	 * 
	 * @param singular
	 *            the singular form of the original message
	 * @param plural
	 *            the plural form of the original message
	 * @param number
	 *            the value to decide plurality
	 * @return the localized singular or plural message or the original message
	 *         itself if not found.
	 */
	public static String get(String singular, String plural, int number) {
		String message = (number > 1) ? get(plural) : get(singular);
		// MessageFormat requires doubled single quotes
		char singleQuote = '\'';
		if (message != null && message.indexOf(singleQuote) > -1) {
			final StringBuilder builder = new StringBuilder();
			final int length = message.length();
			boolean seen = false;
			for (int i = 0; i < length; i++) {
				char last = message.charAt(i);
				if (seen && last != singleQuote) {
					builder.append(singleQuote);
				}
				builder.append(last);
				seen = !seen && last == singleQuote;
			}
			if (seen) {
				builder.append(singleQuote);
			}
			message = builder.toString();
		}
		return MessageFormat.format(message, number);
	}
}
