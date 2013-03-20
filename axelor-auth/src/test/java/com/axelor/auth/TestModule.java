package com.axelor.auth;

import java.util.Properties;

import com.axelor.db.JpaModule;
import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {

	@Override
	protected void configure() {

		Properties properties = new Properties();
		properties.put("hibernate.ejb.interceptor", "com.axelor.auth.db.AuditInterceptor");
		
		install(new AuthModule.Simple());
		install(new JpaModule("testUnit").properties(properties));
	}
}
