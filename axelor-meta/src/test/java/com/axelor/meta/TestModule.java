package com.axelor.meta;

import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.axelor.db.Translations;
import com.axelor.meta.service.MetaTranslations;
import com.google.inject.AbstractModule;

public class TestModule extends AbstractModule {

	@Override
	protected void configure() {
		install(new JpaModule("testUnit"));
		install(new AuthModule.Simple());
		bind(Translations.class).toProvider(MetaTranslations.class);
	}
}
