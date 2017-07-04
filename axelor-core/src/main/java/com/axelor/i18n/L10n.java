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
package com.axelor.i18n;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.axelor.app.AppSettings;
import com.axelor.app.internal.AppFilter;

/**
 * This class provider methods for localization (L10n) services.
 *
 */
public final class L10n {

	private static final String DEFAULT_DATE_FORMAT = "yyy-MM-dd";
	private static final String DEFAULT_TIME_FORMAT = "HH:mm";

	private static final String DATE_FORMAT = AppSettings.get().get("date.format", DEFAULT_DATE_FORMAT);
	private static final String TIME_FORMAT = DEFAULT_TIME_FORMAT;
	private static final String DATE_TIME_FORMAT = DATE_FORMAT + " " + TIME_FORMAT;

	private Locale locale;

	private L10n(Locale locale) {
		this.locale = locale;
	}

	/**
	 * Get instance of {@link L10n} using contextual locale.
	 *
	 * @return {@link L10n} instance for the context
	 */
	public static L10n getInstance() {
		final Locale locale = AppFilter.getLocale();
		return new L10n(locale);
	}

	/**
	 * Get instance of {@link L10n} for the given locale.
	 *
	 * @param locale
	 *            the locale instance
	 * @return {@link L10n} instance for the given locale
	 */
	public static L10n getInstance(Locale locale) {
		return new L10n(locale);
	}

	/**
	 * Format the number value.
	 *
	 * @param value
	 *            the value to format
	 * @return value as formated string
	 */
	public String format(Number value) {
		if (value == null) {
			return null;
		}
		final NumberFormat fmt = NumberFormat.getInstance(locale);
		return fmt.format(value);
	}

	/**
	 * Format the number value.
	 *
	 * @param value
	 *            the value to format
	 * @param grouping
	 *            whether to use grouping in format
	 * @return value as formated string
	 */
	public String format(Number value, boolean grouping) {
		if (value == null) {
			return null;
		}
		final NumberFormat fmt = NumberFormat.getInstance(locale);
		fmt.setGroupingUsed(grouping);
		return fmt.format(value);
	}

	/**
	 * Format the date value.
	 *
	 * @param value
	 *            the value to format
	 * @return value as formated string
	 */
	public String format(LocalDate value) {
		if (value == null) {
			return null;
		}
		return DateTimeFormatter.ofPattern(DATE_FORMAT).format(value);
	}

	/**
	 * Format the date time value.
	 *
	 * @param value
	 *            the value to format
	 * @return value as formated string
	 */
	public String format(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).format(value);
	}

	/**
	 * Format the date time value.
	 *
	 * @param value
	 *            the value to format
	 * @return value as formated string
	 */
	public String format(ZonedDateTime value) {
		if (value == null) {
			return null;
		}
		return DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).format(value);
	}
}
