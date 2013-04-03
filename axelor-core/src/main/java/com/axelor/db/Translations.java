package com.axelor.db;

public interface Translations {
	
	String get(String key);
	
	String get(String key, String defaultValue);
	
	String get(String key, String domain, String defaultValue);

}
