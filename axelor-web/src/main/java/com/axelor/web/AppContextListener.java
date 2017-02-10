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

import java.util.Properties;
import java.util.function.Predicate;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.guice.ModuleProcessor;
import org.jboss.resteasy.plugins.server.servlet.ListenerBootstrap;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.axelor.app.AppSettings;
import com.axelor.common.logging.LoggerConfiguration;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

/**
 * Servlet context listener.
 *
 */
public class AppContextListener extends GuiceServletContextListener {

	private ResteasyDeployment deployment;

	private LoggerConfiguration loggerConfig;
	
	private LoggerConfiguration createLoggerConfig() {
		final AppSettings settings = AppSettings.get();
		final Properties loggingConfig = new Properties();
		final Predicate<String> isLogging = (n) -> n.startsWith("logging.");
		settings.getProperties().stringPropertyNames().stream().filter(isLogging).forEach(n -> {
			loggingConfig.setProperty(n, settings.get(n));
		});
		if (loggingConfig.containsKey("logging.path")) {
			loggingConfig.setProperty("logging.path", settings.getPath("logging.path", null));
		}
		return new LoggerConfiguration(loggingConfig);
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		
		servletContextEvent.getServletContext();
		
		loggerConfig = createLoggerConfig();
		loggerConfig.install();

		super.contextInitialized(servletContextEvent);

		final ServletContext context = servletContextEvent.getServletContext();
		final ListenerBootstrap config = new ListenerBootstrap(context);

		deployment = config.createDeployment();
		deployment.start();

		context.setAttribute(ResteasyProviderFactory.class.getName(),
				deployment.getProviderFactory());
		context.setAttribute(Dispatcher.class.getName(),
				deployment.getDispatcher());
		context.setAttribute(Registry.class.getName(),
				deployment.getRegistry());

		final Registry registry = (Registry) context.getAttribute(Registry.class.getName());
		final ResteasyProviderFactory providerFactory = (ResteasyProviderFactory) context.getAttribute(ResteasyProviderFactory.class.getName());
		final ModuleProcessor processor = new ModuleProcessor(registry, providerFactory);
		final Injector injector = (Injector) context.getAttribute(Injector.class.getName());

		processor.processInjector(injector);

		// load parent injectors
		Injector parent = injector.getParent();
		while (parent != null) {
			parent = injector.getParent();
			processor.processInjector(parent);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		deployment.stop();
		loggerConfig.uninstall();
		super.contextDestroyed(servletContextEvent);
	}

	@Override
	protected Injector getInjector() {
		return Guice.createInjector(new AppServletModule());
	}
}
