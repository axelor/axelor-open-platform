/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
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
 * Software distributed under the License is distributed on an “AS IS”
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
package com.axelor.meta.service;

import java.util.List;
import java.util.Locale;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.Query;
import com.axelor.db.Translations;
import com.axelor.meta.db.MetaTranslation;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class MetaTranslations implements Translations, Provider<Translations> {

	private static Locale DEFAULT_LANGUAGE = new Locale("en");
	public static ThreadLocal<Locale> language = new ThreadLocal<Locale>();

	public static Locale getLanguage() {
		User preferences = AuthUtils.getUser();
		if (preferences != null && !Strings.isNullOrEmpty(preferences.getLanguage())) {
			return toLocale(preferences.getLanguage());
		}
		return language.get() == null ? DEFAULT_LANGUAGE : language.get();
	}

	private static Locale toLocale(String locale) {
	    String parts[] = locale.split("_", -1);
	    if (parts.length == 1) return new Locale(parts[0].toLowerCase());
	    return new Locale(parts[0].toLowerCase(), parts[1].toUpperCase());
	}

	public static String convertLanguage(Locale locale, boolean minimize) {
		StringBuilder format = new StringBuilder(locale.getLanguage().toLowerCase());
		if(!minimize && !Strings.isNullOrEmpty(locale.getCountry()))
			format.append("_").append(locale.getCountry().toUpperCase());
		return format.toString();
	}

	@Override
	public String get(String key) {
		return get(key, key, null);
	}

	@Override
	public String get(String key, String defaultValue) {
		return get(key, defaultValue, null);
	}

	@Override
	public String get(String key, String defaultValue, String domain) {
		return get(key, defaultValue, domain, null);
	}

	@Override
	public String get(String key, String defaultValue, String domain, String type) {

		if (key == null) {
			return defaultValue;
		}

		List<Object> params = Lists.newArrayList();
		String query = "self.key = ?1 AND (self.language = ?2 OR self.language = ?3)";
		params.add(key);
		params.add(convertLanguage(getLanguage(),false));
		params.add(convertLanguage(getLanguage(),true));

		if(domain != null) {
			query += " AND (self.domain IS NULL OR self.domain = ?4)";
			params.add(domain);
		}

		if(type != null) {
			query += " AND self.type = ?" + (params.size() + 1);
			params.add(type);
		}

		Query<MetaTranslation> metaQuery = MetaTranslation
				.all()
				.filter(query, params.toArray())
				.order("-language");

		if(domain != null) {
			metaQuery.order("domain");
		}
		else {
			metaQuery.order("-domain");
		}

		MetaTranslation translation = metaQuery.cacheable().fetchOne();

		if (translation != null && !Strings.isNullOrEmpty(translation.getTranslation())) {
			return translation.getTranslation();
		}
		return defaultValue;
	}

	@Override
	public Translations get() {
		return new MetaTranslations();
	}
}
