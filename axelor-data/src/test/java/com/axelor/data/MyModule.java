package com.axelor.data;

import com.axelor.db.JpaModule;
import com.google.inject.AbstractModule;

public class MyModule extends AbstractModule {
	
	@Override
	protected void configure() {
		install(new JpaModule("testUnit", true, true));
		configureImport();
	}
	
	protected void configureImport() {
		
	}
}