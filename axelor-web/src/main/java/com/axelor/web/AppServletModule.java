/**
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 *
 * The contents of this file are subject to the Common Public
 * Attribution License Version 1.0 (the “License”); you may not use
 * this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://license.axelor.com/.
 *
 * The License is based on the Mozilla Public License Version 1.1 but
 * Sections 14 and 15 have been added to cover use of software over a
 * computer network and provide for limited attribution for the
 * Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 *
 * Software distributed under the License is distributed on an “AS IS”
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is part of "Axelor Business Suite", developed by
 * Axelor exclusively.
 *
 * The Original Developer is the Initial Developer. The Initial Developer of
 * the Original Code is Axelor.
 *
 * All portions of the code written by Axelor are
 * Copyright (c) 2012-2014 Axelor. All Rights Reserved.
 */
package com.axelor.web;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Path;

import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.axelor.db.Translations;
import com.axelor.meta.service.MetaTranslations;
import com.axelor.rpc.Response;
import com.google.inject.matcher.Matchers;
import com.google.inject.persist.PersistFilter;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.container.filter.GZIPContentEncodingFilter;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 * The main application module.
 *
 * It configures JPA and Jersy, registers a custom jackson context resolver,
 * binds essential web services and configures {@link GuiceContainer} to server
 * the web services on <i>/ws/*</i>.
 *
 */
public class AppServletModule extends JerseyServletModule {

	private static final String DEFAULT_PERSISTANCE_UNIT = "persistenceUnit";

	private Logger log = LoggerFactory.getLogger(getClass());

	private String jpaUnit;

	public AppServletModule() {
		this(DEFAULT_PERSISTANCE_UNIT);
	}

	public AppServletModule(String jpaUnit) {
		this.jpaUnit = jpaUnit;
	}

	@Override
	protected void configureServlets() {

		// load application settings
		bind(AppSettings.class).asEagerSingleton();
		AppSettings settings = AppSettings.get();

		StringBuilder builder = new StringBuilder("Starting application:");
		builder.append("\n  ").append("Name: ").append(settings.get("application.name"));
		builder.append("\n  ").append("Version: ").append(settings.get("application.version"));

		log.info(builder.toString());

		// initialize JPA
		Properties properties = new Properties();
		properties.put("hibernate.ejb.interceptor", "com.axelor.auth.db.AuditInterceptor");
		install(new JpaModule(jpaUnit, true, false).properties(properties));

		// trick to ensure PersistFilter is registered before anything else
		install(new ServletModule() {

			@Override
			protected void configureServlets() {
				// order is important, PersistFilter must be the first filter
				filter("*").through(PersistFilter.class);
				filter("*").through(LocaleFilter.class);
				filter("*").through(GuiceShiroFilter.class);
			}
		});

		// install the auth module
		install(new AuthModule(getServletContext()).properties(settings.getProperties()));

		// bind to translations provider
		bind(Translations.class).toProvider(MetaTranslations.class);

		// no-cache filter
		if ("dev".equals(settings.get("application.mode", "dev"))) {
			filter("*").through(NoCacheFilter.class);
		}

		// intercept all response methods
		bindInterceptor(Matchers.any(),
				Matchers.returns(Matchers.subclassesOf(Response.class)),
				new ResponseInterceptor());

		// bind all the web service resources
		Reflections reflections = new Reflections("com.axelor.web", new TypeAnnotationsScanner());
		for(Class<?> type : reflections.getTypesAnnotatedWith(Path.class)) {
			bind(type);
		}

		// register the session listener
		getServletContext().addListener(new AppSessionListener(settings));

		Map<String, String> params = new HashMap<String, String>();

		params.put(ResourceConfig.FEATURE_REDIRECT, "true");
		params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.axelor;");

		// enable GZIP encoding filter
		params.put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
				GZIPContentEncodingFilter.class.getName());
		params.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS,
				GZIPContentEncodingFilter.class.getName());

		serve("_init").with(InitServlet.class);
		serve("/ws/*").with(GuiceContainer.class, params);
	}
}
