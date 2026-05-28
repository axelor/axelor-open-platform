/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app.cli;

import com.axelor.app.AppModule;
import com.axelor.auth.AuthModule;
import com.axelor.db.JpaModule;
import com.axelor.inject.Beans;
import com.axelor.rpc.ObjectMapperProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.persist.PersistService;
import com.google.inject.servlet.RequestScoped;
import com.google.inject.servlet.RequestScoper;
import com.google.inject.servlet.ServletScopes;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCliCommand implements CliCommand {

  private static final String PERSISTENCE_UNIT = "persistenceUnit";

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected void withContainer(String jpaUnit, Runnable task) {
    Guice.createInjector(new MyModule(jpaUnit));
    final RequestScoper scope = ServletScopes.scopeRequest(Collections.emptyMap());
    try (final RequestScoper.CloseableScope ignored = scope.open()) {
      task.run();
    }
  }

  protected void withContainer(Runnable task) {
    withContainer(PERSISTENCE_UNIT, task);
  }

  protected void withSession(String jpaUnit, Runnable task) {
    withContainer(
        jpaUnit,
        () -> {
          PersistService service = Beans.get(PersistService.class);
          try {
            service.start();
            task.run();
          } finally {
            service.stop();
          }
        });
  }

  public void withSession(Runnable task) {
    withSession(PERSISTENCE_UNIT, task);
  }

  private static class MyModule extends AbstractModule {

    private String jpaUnit;

    private boolean autoScan = true;

    private boolean autoStart = false;

    public MyModule(String jpaUnit) {
      this.jpaUnit = jpaUnit;
    }

    @Override
    protected void configure() {
      bindScope(RequestScoped.class, ServletScopes.REQUEST);
      bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);
      install(new JpaModule(jpaUnit, autoScan, autoStart));
      install(new AuthModule());
      install(new AppModule());
    }
  }
}
