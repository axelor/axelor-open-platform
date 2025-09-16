/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.internal.AppLogger;
import com.axelor.meta.loader.ViewWatcher;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import dev.resteasy.guice.GuiceResourceFactory;
import dev.resteasy.guice.ModuleProcessor;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.SessionCookieConfig;
import java.util.Map;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ListenerBootstrap;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/** Servlet context listener. */
public class AppContextListener extends GuiceServletContextListener {

  private ResteasyDeployment deployment;

  private void configureRestEasy(ServletContextEvent servletContextEvent) {
    final ServletContext context = servletContextEvent.getServletContext();
    final ListenerBootstrap config = new ListenerBootstrap(context);

    final Map<Class<?>, Object> map = ResteasyContext.getContextDataMap();
    map.put(ServletContext.class, context);

    final Injector injector = (Injector) context.getAttribute(Injector.class.getName());

    // use custom registry for hotswap-agent support
    final ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
    final ResourceMethodRegistry registry =
        new ResourceMethodRegistry(providerFactory) {

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

    deployment = config.createDeployment();
    deployment.setProviderFactory(providerFactory);
    deployment.setAsyncJobServiceEnabled(false);
    deployment.setDispatcher(dispatcher);
    deployment.start();

    context.setAttribute(ResteasyDeployment.class.getName(), deployment);

    final ModuleProcessor processor = new ModuleProcessor(registry, providerFactory);

    // process all injectors
    Injector current = injector;
    while (current != null) {
      processor.processInjector(current);
      current = injector.getParent();
    }
  }

  private void configureCookie(ServletContextEvent servletContextEvent) {
    final ServletContext context = servletContextEvent.getServletContext();
    final SessionCookieConfig cookieConfig = context.getSessionCookieConfig();
    cookieConfig.setHttpOnly(true);
    final boolean sessionCookieSecure =
        AppSettings.get().getBoolean(AvailableAppSettings.SESSION_COOKIE_SECURE, false);

    if (sessionCookieSecure) {
      cookieConfig.setSecure(sessionCookieSecure);
      cookieConfig.setAttribute("SameSite", "None");
    }
  }

  private void beforeStart(ServletContextEvent servletContextEvent) {
    AppLogger.install();
  }

  private void afterStart(ServletContextEvent servletContextEvent) {
    configureCookie(servletContextEvent);
    configureRestEasy(servletContextEvent);
  }

  private void beforeStop(ServletContextEvent servletContextEvent) {
    ViewWatcher.getInstance().stop();
    servletContextEvent.getServletContext().removeAttribute(ResteasyDeployment.class.getName());
    deployment.stop();
  }

  private void afterStop(ServletContextEvent servletContextEvent) {
    AppLogger.uninstall();
  }

  @Override
  public void contextInitialized(ServletContextEvent servletContextEvent) {
    this.beforeStart(servletContextEvent);
    super.contextInitialized(servletContextEvent);
    this.afterStart(servletContextEvent);
  }

  @Override
  public void contextDestroyed(ServletContextEvent servletContextEvent) {
    this.beforeStop(servletContextEvent);
    super.contextDestroyed(servletContextEvent);
    this.afterStop(servletContextEvent);
  }

  @Override
  protected Injector getInjector() {
    return Guice.createInjector(new AppServletModule());
  }
}
