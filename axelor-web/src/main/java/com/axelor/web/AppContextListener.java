/*
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
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.guice.GuiceResourceFactory;
import org.jboss.resteasy.plugins.guice.ModuleProcessor;
import org.jboss.resteasy.plugins.server.servlet.ListenerBootstrap;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.axelor.app.AppSettings;
import com.axelor.common.logging.LoggerConfiguration;
import com.axelor.meta.loader.ViewWatcher;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
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
		loggerConfig = createLoggerConfig();
		loggerConfig.install();
		super.contextInitialized(servletContextEvent);

		final ServletContext context = servletContextEvent.getServletContext();
		final ListenerBootstrap config = new ListenerBootstrap(context);
		final Injector injector = (Injector) context.getAttribute(Injector.class.getName());

		deployment = config.createDeployment();

		// use custom registry for hotswap-agent support
		final ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
		final ResourceMethodRegistry registry = new ResourceMethodRegistry(providerFactory) {

			@Override
			@SuppressWarnings("all")
			public void addPerRequestResource(Class clazz) {
				final Binding<?> binding = injector.getBinding(clazz);
				if (binding == null) {
					super.addPerRequestResource(clazz);
				} else {
					super.addResourceFactory(new GuiceResourceFactory(binding.getProvider(), clazz));
				}
			}
		};
		final Dispatcher dispatcher = new SynchronousDispatcher(providerFactory, registry);

		deployment.setProviderFactory(providerFactory);
		deployment.setAsyncJobServiceEnabled(false);
		deployment.setDispatcher(dispatcher);
		deployment.start();

		context.setAttribute(ResteasyProviderFactory.class.getName(), providerFactory);
		context.setAttribute(Dispatcher.class.getName(), dispatcher);
		context.setAttribute(Registry.class.getName(), registry);

		final ModuleProcessor processor = new ModuleProcessor(registry, providerFactory);

		// process all injectors
		Injector current = injector;
		while (current != null) {
			processor.processInjector(current);
			current = injector.getParent();
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		ViewWatcher.getInstance().stop();
		deployment.stop();
		loggerConfig.uninstall();
		super.contextDestroyed(servletContextEvent);
	}

	@Override
	protected Injector getInjector() {
		final Stage stage = AppSettings.get().isProduction() ? Stage.PRODUCTION : Stage.DEVELOPMENT;
		return Guice.createInjector(stage, new AppServletModule());
	}
}
