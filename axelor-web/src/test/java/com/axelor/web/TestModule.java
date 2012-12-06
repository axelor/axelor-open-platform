package com.axelor.web;

import com.axelor.db.JpaModule;
import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {

	@Override
	protected void configure() {
		// initialize JPA
		install(new JpaModule("testUnit", true, false));
	}
}
