/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.web;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;

import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppModule;
import com.axelor.app.AppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthModule;
import com.axelor.common.reflections.Reflections;
import com.axelor.db.JpaModule;
import com.axelor.quartz.SchedulerModule;
import com.axelor.rpc.ObjectMapperProvider;
import com.axelor.rpc.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
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
		AppSettings settings = AppSettings.get();

		StringBuilder builder = new StringBuilder("Starting application:");
		builder.append("\n  ").append("Name: ").append(settings.get("application.name"));
		builder.append("\n  ").append("Version: ").append(settings.get("application.version"));

		log.info(builder.toString());
		
		// some common bindings
		bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);

		// initialize JPA
		install(new JpaModule(jpaUnit, true, false));

		// trick to ensure PersistFilter is registered before anything else
		install(new ServletModule() {

			@Override
			protected void configureServlets() {
				// order is important, PersistFilter must be the first filter
				filter("*").through(PersistFilter.class);
				filter("*").through(AppFilter.class);
				filter("*").through(GuiceShiroFilter.class);
			}
		});

		// install the auth module
		install(new AuthModule(getServletContext()).properties(settings.getProperties()));

		// install the app modules
		install(new AppModule());
		
		// install the scheduler module
		install(new SchedulerModule());

		// no-cache filter
		if (!settings.isProduction()) {
			filter("*").through(NoCacheFilter.class);
		}

		// intercept all response methods
		bindInterceptor(Matchers.any(),
				Matchers.returns(Matchers.subclassesOf(Response.class)),
				new ResponseInterceptor());

		// bind all the web service resources
		for (Class<?> type : Reflections
				.findTypes()
				.within("com.axelor.web")
				.having(Path.class)
				.find()) {
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
