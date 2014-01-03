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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.axelor.web.db.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

		ObjectMapperProvider resolver = new ObjectMapperProvider();
		bind(ObjectMapperProvider.class).toInstance(resolver);
		
		// enable formated json output
		ObjectMapper mapper = resolver.getContext(null);
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		
		params.put(ResourceConfig.FEATURE_REDIRECT, "true");
		params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.axelor;");
		
		ClientConfig cc = new DefaultClientConfig();
		cc.getFeatures().put(ResourceConfig.FEATURE_REDIRECT, Boolean.TRUE);

		bind(ClientConfig.class).toInstance(cc);
		
		serve("/ws/*").with(GuiceContainer.class, params);
	}
}
