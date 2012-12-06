package com.axelor.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.runners.model.InitializationError;

import com.google.inject.Module;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class GuiceWebRunner extends GuiceRunner {

	public GuiceWebRunner(Class<?> klass) throws InitializationError {
		super(klass);
	}
	
	@Override
	protected List<com.google.inject.Module> getModules(Class<?> klass)
			throws InitializationError {
		
		List<Module> modules = super.getModules(klass);
		boolean hasJerseyModule = false;
		for(Module module : modules) {
			if (module instanceof JerseyServletModule) {
				hasJerseyModule = true;
				break;
			}
		}
		if (!hasJerseyModule)
			modules.add(new MyModule());
		return modules;
	}
	
	static class MyModule extends JerseyServletModule {

		@Override
		protected void configureServlets() {
			
			Map<String, String> params = new HashMap<String, String>();
			params.put(ResourceConfig.FEATURE_REDIRECT, "true");

			serve("/*").with(GuiceContainer.class, params);
		}
	}
}
