/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.axelor.common.StringUtils;
import com.axelor.db.JPA;
import com.axelor.db.Query;
import com.axelor.meta.db.MetaTranslation;

/**
 * The database backed {@link ResourceBundle} that loads translations from the
 * axelor database.
 * 
 */
public class I18nBundle extends ResourceBundle {

	private final Locale locale;
	private final Map<String, String> messages = new ConcurrentHashMap<>();

	public I18nBundle(Locale locale) {
		this.locale = locale;
	}

	@Override
	protected Object handleGetObject(String key) {
		if (StringUtils.isBlank(key)) {
			return key;
		}
		final String result = load().get(key);
		if (StringUtils.isBlank(result)) {
			return key;
		}
		return result;
	}

	@Override
	protected Set<String> handleKeySet() {
		return load().keySet();
	}

	@Override
	public Enumeration<String> getKeys() {
		return Collections.enumeration(load().keySet());
	}
	
	private Map<String, String> load() {
		
		if (!messages.isEmpty()) {
			return messages;
		}
		
		try {
			JPA.em();
		} catch (Throwable e) {
			return messages;
		}

		String lang = locale.getLanguage();
		Query<MetaTranslation> query = Query.of(MetaTranslation.class).filter("self.language = ?1", lang).autoFlush(false);
		
		if (query.count() == 0 && lang.length() > 2) {
			query = Query.of(MetaTranslation.class).filter("self.language = ?1", lang.substring(0, 2));
		}

		long total = query.count();
		int offset = 0;
		int limit = 100;

		while (offset < total) {
			for (MetaTranslation tr : query.fetch(limit, offset)) {
				if (tr.getMessage() != null) {
					messages.put(tr.getKey(), tr.getMessage());
				}
			}
			offset += limit;
		}
		return messages;
	}

	public static void invalidate() {
		ResourceBundle.clearCache();
	}
}
