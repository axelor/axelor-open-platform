package com.axelor.meta;

import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new JpaModule("testUnit"));
		install(new AuthModule.Simple());
	}
}
