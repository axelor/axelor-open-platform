package com.axelor.web;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import com.axelor.auth.db.User;
import com.fasterxml.jackson.databind.ObjectMapper;

@Singleton
public class AppSettings {

	private Properties properties;
	private static String DEFAULT_LANGUAGE = "en";
	private static AppSettings INSTANCE;

	@Inject
	private AppSettings() {
			
		InputStream is = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("application.properties");

		if (is == null) {
			throw new RuntimeException(
					"Unable to locate application configuration file.");
		}

		properties = new Properties();
		
		try {
			properties.load(is);
		} catch (Exception e) {
			throw new RuntimeException("Error reading application configuration.");
		}
	}

	public static AppSettings get() {
		if (INSTANCE == null)
			INSTANCE = new AppSettings();
		return INSTANCE;
	}

	public String get(String key) {
		return properties.getProperty(key);
	}

	public String get(String key, String defaultValue) {
		String value = properties.getProperty(key, defaultValue);
		if (value == null || "".equals(value.trim()))
			return defaultValue;
		return value;
	}
	
	public int getInt(String key, int defaultValue) {
		try {
			return Integer.parseInt(get(key).toString());
		} catch (Exception e){}
		return defaultValue;
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		try {
			return Boolean.parseBoolean(get(key).toString());
		} catch (Exception e){}
		return defaultValue;
	}
	
	public void putAll(Properties properties) {
		this.properties.putAll(properties);
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	public String toJSON() {
		
		Properties settings = new Properties();
		
		try {
			Subject subject = SecurityUtils.getSubject();
			User user = User.all().filter("self.code = ?1", subject.getPrincipal()).fetchOne();
			settings.put("user.name", user.getName());
			settings.put("user.login", user.getCode());
		} catch (Exception e){
		}
		
		settings.putAll(properties);
		
		// remove server only properties
		settings.remove("temp.dir");
		
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(settings);
		} catch (Exception e) {
		}
		return "{}";
	}
	
	private static String convertLanguage(Locale locale, boolean minimize){
		StringBuilder format = new StringBuilder(locale.getLanguage().toLowerCase());
		if(!minimize && locale.getCountry() != null)
			format.append("_").append(locale.getCountry().toUpperCase());
		return format.toString();
	}
	
	public static String getLocaleJS(HttpServletRequest request, ServletContext context){
		
		//application.properties
		String appLocale = AppSettings.get().get("application.locale", null);
		
		//Servlet Locale
		Locale locale = request.getLocale();
		if(locale == null ){
			locale = new Locale(appLocale);
		}
		
		String convertLanguage = convertLanguage(locale,false);
		String minimizeLanguage = convertLanguage(locale,true);

		if(checkResources(context, "/js/i18n/"+convertLanguage +".js")){
			return convertLanguage;
		}
		else if(checkResources(context, "/js/i18n/"+minimizeLanguage +".js")){
			return minimizeLanguage;
		}
		
		return DEFAULT_LANGUAGE;

	}
	
	private static boolean checkResources(ServletContext context, String resourcesPath){
		try{
			URL path = context.getResource(resourcesPath);
			return path == null ? false : true;
		} catch(MalformedURLException e){
			return false;
		}
	}
	
	public static String getAppJS(ServletContext context) {
		String appJs = "js/application-all.min.js";

		if ("dev".equals(AppSettings.get().get("application.mode", "dev")) || checkResources(context, "/" + appJs) == false) {
			appJs = "js/application.js";
		}

		return appJs;
	}
}
