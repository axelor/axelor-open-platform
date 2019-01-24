/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2019 Axelor (<http://axelor.com>).
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
package com.axelor.app;

import com.axelor.event.EventModule;
import com.axelor.inject.Beans;
import com.axelor.inject.logger.LoggerModule;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.loader.ViewObserver;
import com.axelor.report.ReportEngineProvider;
import com.google.inject.AbstractModule;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The application module scans the classpath and finds all the {@link AxelorModule} and installs
 * them in the dependency order.
 */
public class AppModule extends AbstractModule {

  private static Logger log = LoggerFactory.getLogger(AppModule.class);

  @Override
  protected void configure() {

    // initialize Beans helps
    bind(Beans.class).asEagerSingleton();

    // report engine
    bind(IReportEngine.class).toProvider(ReportEngineProvider.class);

    // Observe changes for views
    bind(ViewObserver.class);

    // Logger injection support
    install(new LoggerModule());

    // events support
    install(new EventModule());

    final List<Class<? extends AxelorModule>> moduleClasses =
        ModuleManager.findInstalled()
            .stream()
            .flatMap(name -> MetaScanner.findSubTypesOf(name, AxelorModule.class).find().stream())
            .collect(Collectors.toList());

    if (moduleClasses.isEmpty()) {
      return;
    }

    log.info("Configuring app modules...");

    for (Class<? extends AxelorModule> module : moduleClasses) {
      try {
        log.debug("Configure module: {}", module.getName());
        install(module.newInstance());
      } catch (InstantiationException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
