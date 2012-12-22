package com.axelor.web;

import java.util.HashMap;
import java.util.Map;

import org.apache.shiro.guice.web.GuiceShiroFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.axelor.rpc.Response;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.persist.PersistFilter;
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
		install(new JpaModule(jpaUnit, true, false));
		filter("*").through(PersistFilter.class);
		
		// install auth module
		install(new AuthModule(getServletContext()));
		filter("*").through(GuiceShiroFilter.class);
		
		// no-cache filter
		if ("dev".equals(settings.get("application.mode", "dev"))) {
			filter("*").through(NoCacheFilter.class);
		}
		
		// intercept all response methods
		bindInterceptor(Matchers.any(),
				Matchers.returns(Matchers.subclassesOf(Response.class)),
				new ResponseInterceptor());
		
		Map<String, String> params = new HashMap<String, String>();

		params.put(ResourceConfig.FEATURE_REDIRECT, "true");
		params.put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.axelor;");
		
		// enable GZIP encoding filter
		params.put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
				GZIPContentEncodingFilter.class.getName());
		params.put(ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS,
				GZIPContentEncodingFilter.class.getName());
		
		bindConstant()
			.annotatedWith(Names.named("shiro.globalSessionTimeout"))
			.to(settings.getInt("session.timeout", 30) * 1000);
		
		serve("_init").with(InitServlet.class);
		serve("/ws/*").with(GuiceContainer.class, params);
	}
}
