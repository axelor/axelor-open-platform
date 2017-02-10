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
package com.axelor.rpc;

import static com.axelor.common.StringUtils.isBlank;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.axelor.app.internal.AppFilter;
import com.axelor.db.JPA;
import com.axelor.db.Model;
import com.axelor.db.mapper.Mapper;
import com.axelor.db.mapper.Property;
import com.axelor.i18n.I18n;
import com.axelor.i18n.I18nBundle;
import com.axelor.inject.Beans;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.repo.MetaTranslationRepository;

final class Translator {

	private Translator() {
	}

	private static Long findId(Map<String, Object> values) {
		try {
			return Long.parseLong(values.get("id").toString());
		} catch (Exception e){}
		return null;
	}

	public static String getTranslation(Property property, String value) {
		if (property.isTranslatable()) {
			return I18n.get(value);
		}
		return value;
	}

	@SuppressWarnings("all")
	public static void applyTranslatables(Map<String, Object> values, Class<?> model) {
		if (values == null || values.isEmpty()) return;
		final Mapper mapper = Mapper.of(model);
		for (Property property : mapper.getProperties()) {
			final String name = property.getName();
			final Object value = values.get(name);
			if (property.isTranslatable() && value instanceof String) {
				values.put(property.getName(), I18n.get((String) value));
			}
			if (property.getTarget() != null && value instanceof Map) {
				applyTranslatables((Map) value, property.getTarget());
			}
			if (property.getTarget() != null && value instanceof Collection) {
				for (Object item : (Collection) value) {
					if (item instanceof Map) {
						applyTranslatables((Map) item, property.getTarget());
					}
				}
			}
		}
	}

	@SuppressWarnings("all")
	public static void saveTranslatables(Map<String, Object> json, Class<?> model) {
		final Mapper mapper = Mapper.of(model);
		final Set<String> translated = new HashSet<>();
		final Long id = findId(json);
		final Model bean = id == null ? null : JPA.find((Class) model, id);
		for (String name : json.keySet()) {
			final Property property = mapper.getProperty(name);
			if (property == null) continue;
			if (property.isTranslatable()) {
				String val = (String) json.get(property.getName());
				String key = bean == null ? val : (String) property.get(bean);
				if (saveTranslation(property, key, val)) {
					translated.add(name);
				}
				continue;
			}
			if (property.getTarget() == null) {
				continue;
			}
			final Object value = json.get(name);
			if (value instanceof Map) {
				saveTranslatables((Map) value, property.getTarget());
			}
			if (value instanceof Collection) {
				for (Object item : (Collection) value) {
					if (item instanceof Map) {
						saveTranslatables((Map) item, property.getTarget());
					}
				}
			}
		}

		if (findId(json) != null) {
			// remove translated values from json to prevent persistence
			for (String name : translated) {
				json.remove(name);
			}
		}
	}

	private static boolean saveTranslation(Property property, String key, String value) {
		Locale locale = AppFilter.getLocale();
		if (locale == null) {
			locale = Locale.getDefault();
		}
		if (isBlank(key)) {
			key = value;
		}
		if (isBlank(key)) {
			return false;
		}
		MetaTranslationRepository repo = Beans.get(MetaTranslationRepository.class);
		MetaTranslation record = repo.findByKey(key, locale.getLanguage());
		if (record == null) {
			record = new MetaTranslation();
			record.setKey(key);
			record.setMessage(value);
			record.setLanguage(locale.getLanguage());
		} else {
			record.setMessage(value);
		}
		repo.save(record);
		I18nBundle.invalidate();
		return true;
	}
}
