package com.axelor.meta.service;

import java.util.Locale;

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
		return language.get() == null ? DEFAULT_LANGUAGE : language.get();
	}
	
	private String getConvertLanguage() {
		StringBuilder format = new StringBuilder(getLanguageCode());
		format.append("_").append(getLanguageCountryCode().toUpperCase());
		return format.toString();
	}
	
	private String getLanguageCountryCode() {
		return getLanguage().getLanguage().toUpperCase();
	}
	
	private String getLanguageCode() {
		return getLanguage().getLanguage().toLowerCase();
	}

	@Override
	public String get(String key) {
		return get(key, null, key);
	}

	@Override
	public String get(String key, String defaultValue) {
		return get(key, null, defaultValue);
	}
	
	@Override
	public String get(String key, String domain, String defaultValue) {

		if (key == null) {
			return defaultValue;
		}
		
		MetaTranslation translation = MetaTranslation
				.all()
				.filter("self.key = ?1 "
						+ "AND (self.language = ?2 OR self.language = ?3) "
						+ "AND (self.domain IS NULL OR self.domain = ?4)", key,
						getConvertLanguage(), getLanguageCode(), domain)
				.order("-language").order("domain").fetchOne();

		if (translation != null && !Strings.isNullOrEmpty(translation.getTranslation())){
			return translation.getTranslation();
		}
		return defaultValue;
	}

	@Override
	public Translations get() {
		return new MetaTranslations();
	}
}
