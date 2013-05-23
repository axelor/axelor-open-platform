package com.axelor.db;

import javax.inject.Provider;

import com.google.inject.ImplementedBy;

@ImplementedBy(Translations.Dummy.class)
public interface Translations {

	static class Dummy implements Translations, Provider<Translations> {

		@Override
		public Translations get() {
			return new Dummy();
		}

		@Override
		public String get(String key) {
			return key;
		}
		
		@Override
		public String get(String key, String defaultValue) {
			return key;
		}

		@Override
		public String get(String key, String defaultValue, String domain) {
			return key;
		}
		
		@Override
		public String get(String key, String defaultValue, String domain, String type) {
			return key;
		}
	}

	String get(String key);
	
	String get(String key, String defaultValue);
	
	String get(String key, String defaultValue, String domain);
	
	String get(String key, String defaultValue, String domain, String type);
}
