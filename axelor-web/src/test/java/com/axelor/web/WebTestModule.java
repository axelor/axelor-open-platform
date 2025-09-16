/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web;

import com.axelor.web.db.Repository;
import com.axelor.web.service.RestService;
import com.axelor.web.service.ViewService;
import com.google.inject.persist.PersistFilter;
import com.google.inject.servlet.ServletModule;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

public class WebTestModule extends ServletModule {

  @Singleton
  @SuppressWarnings("all")
  public static class DataLoaderServlet extends HttpServlet {

    @Inject private Repository repository;

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

    bind(RestService.class);
    bind(ViewService.class);

    bind(ObjectMapperResolver.class).asEagerSingleton();

    serve("_init").with(DataLoaderServlet.class);
  }
}
