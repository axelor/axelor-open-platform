package com.axelor.wkf;

import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.google.inject.AbstractModule;

public class WkfTest extends AbstractModule {

	@Override
	protected void configure() {
		install(new JpaModule("testUnit"));
        install(new AuthModule.Simple());
	}

}
 