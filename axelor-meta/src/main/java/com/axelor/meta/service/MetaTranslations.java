package com.axelor.meta.service;

import java.util.Locale;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;
import com.axelor.db.Translations;
import com.axelor.meta.db.MetaTranslation;
import com.google.common.base.Strings;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class MetaTranslations implements Translations, Provider<Translations> {
	
	private static Locale DEFAULT_LANGUAGE = new Locale("en", "US");
	public static ThreadLocal<Locale> language = new ThreadLocal<Locale>();
	
	private Locale getLanguage() {
		User user = AuthUtils.getUser();
		if(user != null && !Strings.isNullOrEmpty(user.getLanguage())) {
			return toLocale(user.getLanguage());
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
		
		MetaTranslation translation = MetaTranslation
				.all()
				.filter("self.key = ?1 "
						+ "AND (self.language = ?2 OR self.language = ?3) "
						+ "AND (self.domain IS NULL OR self.domain = ?4)" 
						+ "AND (self.type IS NULL OR self.type = ?5)", 
						key, convertLanguage(getLanguage(),false), convertLanguage(getLanguage(),true), domain, type)
				.order("-language").order("domain").order("type").fetchOne();

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
