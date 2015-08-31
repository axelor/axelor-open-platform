/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import java.lang.reflect.Method;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.app.AppModule;
import com.axelor.app.AppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.axelor.meta.MetaScanner;
import com.axelor.quartz.SchedulerModule;
import com.axelor.rpc.ObjectMapperProvider;
import com.axelor.rpc.Request;
import com.axelor.rpc.RequestFilter;
import com.axelor.rpc.Response;
import com.axelor.rpc.ResponseInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.persist.PersistFilter;
import com.google.inject.servlet.ServletModule;

/**
 * The main application module.
 *
 */
public class AppServletModule extends ServletModule {

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
		filter("/js/*", NoCacheFilter.STATIC_URL_PATTERNS).through(NoCacheFilter.class);

		// intercept all response methods
		bindInterceptor(Matchers.any(),
				Matchers.returns(Matchers.subclassesOf(Response.class)),
				new ResponseInterceptor());

		// intercept request accepting methods
		bindInterceptor(Matchers.annotatedWith(Path.class),
				new AbstractMatcher<Method>() {
					@Override
					public boolean matches(Method t) {
						for (Class<?> c : t.getParameterTypes()) {
							if (Request.class.isAssignableFrom(c)) {
								return true;
							}
						}
						return false;
					}
				}, new RequestFilter());

		// bind all the web service resources
		for (Class<?> type : MetaScanner
				.findTypes()
				.having(Path.class)
				.having(Provider.class)
				.any().find()) {
			bind(type);
		}

		// register the session listener
		getServletContext().addListener(new AppSessionListener(settings));

		// register initialization servlet
		serve("_init").with(InitServlet.class);
	}
}
