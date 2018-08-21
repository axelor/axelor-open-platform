/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2018 Axelor (<http://axelor.com>).
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

import com.axelor.web.db.Repository;
import com.axelor.web.service.RestService;
import com.axelor.web.service.ViewService;
import com.google.inject.persist.PersistFilter;
import com.google.inject.servlet.ServletModule;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

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
