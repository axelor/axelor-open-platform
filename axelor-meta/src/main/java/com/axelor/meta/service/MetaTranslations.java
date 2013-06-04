package com.axelor.meta.service;

import java.util.List;
import java.util.Locale;

import com.axelor.auth.AuthUtils;
import com.axelor.db.Translations;
import com.axelor.meta.db.MetaTranslation;
import com.axelor.meta.db.MetaUser;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class MetaTranslations implements Translations, Provider<Translations> {
	
	private static Locale DEFAULT_LANGUAGE = new Locale("en", "US");
	public static ThreadLocal<Locale> language = new ThreadLocal<Locale>();
	
	private Locale getLanguage() {
		MetaUser preferences = MetaUser.findByUser(AuthUtils.getUser());
		if (preferences != null && !Strings.isNullOrEmpty(preferences.getLanguage())) {
			return toLocale(preferences.getLanguage());
		}
		return language.get() == null ? DEFAULT_LANGUAGE : language.get();
	}
	
	private Locale toLocale(String locale) {
	    String parts[] = locale.split("_", -1);
	    if (parts.length == 1) return new Locale(parts[0].toLowerCase());
	    return new Locale(parts[0].toLowerCase(), parts[1].toUpperCase());
	}
	
	private String convertLanguage(Locale locale, boolean minimize) {
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
		
		if(domain != null){
			query += " AND (self.domain IS NULL OR self.domain = ?4)";
			params.add(domain);
		}
		
		if(type != null){
			query += " AND self.type = ?" + (params.size() + 1);
			params.add(type);
		}

		MetaTranslation translation = MetaTranslation
				.all()
				.filter(query, params.toArray())
				.order("-language").order("domain").order("type")
				.fetchOne();
		
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
