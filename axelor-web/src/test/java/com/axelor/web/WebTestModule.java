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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.axelor.web.db.Repository;
import com.google.inject.persist.PersistFilter;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class WebTestModule extends JerseyServletModule {
	
	@Singleton @SuppressWarnings("all")
	public static class DataLoaderServlet extends HttpServlet {
		
		@Inject
		private Repository repository;
		
		@Override
		public void init() throws ServletException {
			super.init();
			repository.load();
		}
	}
	
	@Override
	protected void configureServlets() {
		
		install(new TestModule());
		filter("*").through(PersistFilter.class);
		
		serve("_init").with(DataLoaderServlet.class);
		
		Map<String, String> params = new HashMap<String, String>();
		bind(ObjectMapperResolver.class).asEagerSingleton();

		params.put(ResourceConfig.FEATURE_REDIRECT, "true");
		params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.axelor;");
		
		ClientConfig cc = new DefaultClientConfig();
		cc.getFeatures().put(ResourceConfig.FEATURE_REDIRECT, Boolean.TRUE);

		bind(ClientConfig.class).toInstance(cc);
		
		serve("/ws/*").with(GuiceContainer.class, params);
	}
}
