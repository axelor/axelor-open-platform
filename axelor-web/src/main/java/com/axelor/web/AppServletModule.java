/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web;

import com.axelor.app.AppModule;
import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.internal.AppFilter;
import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.axelor.db.tenants.TenantFilter;
import com.axelor.meta.MetaScanner;
import com.axelor.quartz.SchedulerModule;
import com.axelor.rpc.ObjectMapperProvider;
import com.axelor.rpc.Request;
import com.axelor.rpc.RequestFilter;
import com.axelor.rpc.Response;
import com.axelor.rpc.ResponseInterceptor;
import com.axelor.web.openapi.OpenApiModule;
import com.axelor.web.servlet.CacheAssetsFilter;
import com.axelor.web.servlet.CorsFilter;
import com.axelor.web.servlet.I18nServlet;
import com.axelor.web.servlet.MaintenanceFilter;
import com.axelor.web.servlet.NoCacheFilter;
import com.axelor.web.servlet.ProxyFilter;
import com.axelor.web.socket.inject.WebSocketModule;
import com.axelor.web.socket.inject.WebSocketSecurity;
import com.axelor.web.socket.inject.WebSocketSecurityInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Module;
import com.google.inject.matcher.Matchers;
import com.google.inject.persist.PersistFilter;
import com.google.inject.servlet.ServletModule;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.List;
import org.apache.shiro.guice.web.GuiceShiroFilter;

/** The main application module. */
public class AppServletModule extends ServletModule {

  private static final String DEFAULT_PERSISTANCE_UNIT = "persistenceUnit";

  private String jpaUnit;

  public AppServletModule() {
    this(DEFAULT_PERSISTANCE_UNIT);
  }

  public AppServletModule(String jpaUnit) {
    this.jpaUnit = jpaUnit;
  }

  protected List<? extends Module> getModules() {
    final AuthModule authModule = new AuthModule(getServletContext());
    final AppModule appModule = new AppModule();
    final SchedulerModule schedulerModule = new SchedulerModule();
    final WebSocketModule webSocketModule = new WebSocketModule();
    return Arrays.asList(authModule, appModule, schedulerModule, webSocketModule);
  }

  protected void afterConfigureServlets() {
    // register initialization servlet
    serve("__init__").with(AppStartup.class);
  }

  @Override
  protected void configureServlets() {

    // some common bindings
    bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);

    // initialize JPA
    install(new JpaModule(jpaUnit, true, false));

    // trick to ensure PersistFilter is registered before anything else
    install(
        new ServletModule() {

          @Override
          protected void configureServlets() {
            // check for CORS requests earlier
            filter("*").through(ProxyFilter.class);
            filter("*").through(CorsFilter.class);
            // tenant filter should be come before PersistFilter
            filter("*").through(TenantFilter.class);
            // order is important, PersistFilter must come first
            filter("*").through(PersistFilter.class);
            filter("*").through(AppFilter.class);
            filter("*").through(GuiceShiroFilter.class);
          }
        });

    final AppSettings settings = AppSettings.get();

    final boolean swaggerUIEnabled =
        settings.getBoolean(AvailableAppSettings.APPLICATION_SWAGGER_UI_ENABLED, true);
    final boolean openApiEnabled =
        settings.getBoolean(AvailableAppSettings.APPLICATION_OPENAPI_ENABLED, swaggerUIEnabled);

    if (openApiEnabled) {
      install(new OpenApiModule());
    }

    // install additional modules
    for (Module module : getModules()) {
      install(module);
    }

    // no-cache filter
    filter(NoCacheFilter.STATIC_URL_PATTERNS).through(NoCacheFilter.class);

    // cache version/hash-named assets filter
    filter(CacheAssetsFilter.URL_PATTERN).through(CacheAssetsFilter.class);

    // Maintenance mode (503 Service Unavailable)
    filter("/", "/index.html", "/ws/*").through(MaintenanceFilter.class);

    // i18n bundle
    serve("/js/messages.js").with(I18nServlet.class);

    // intercept all response methods
    bindInterceptor(
        Matchers.any(),
        Matchers.returns(Matchers.subclassesOf(Response.class)),
        new ResponseInterceptor());

    // intercept request accepting methods
    bindInterceptor(
        Matchers.annotatedWith(Path.class),
        t -> {
          for (Class<?> c : t.getParameterTypes()) {
            if (Request.class.isAssignableFrom(c)) {
              return true;
            }
          }
          return false;
        },
        new RequestFilter());

    // intercept WebSocket endpoint
    bindInterceptor(
        Matchers.annotatedWith(WebSocketSecurity.class),
        Matchers.any(),
        new WebSocketSecurityInterceptor());

    // bind all the web service resources
    for (Class<?> type :
        MetaScanner.findSubTypesOf(Object.class)
            .having(Path.class)
            .having(Provider.class)
            .any()
            .find()) {
      bind(type);
    }

    // run additional configuration tasks
    this.afterConfigureServlets();
  }
}
