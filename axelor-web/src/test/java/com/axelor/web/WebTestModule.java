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
