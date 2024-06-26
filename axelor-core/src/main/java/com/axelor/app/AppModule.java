/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.app;

import com.axelor.cache.CacheBuilder;
import com.axelor.db.audit.HibernateListenerConfigurator;
import com.axelor.event.EventModule;
import com.axelor.inject.Beans;
import com.axelor.inject.logger.LoggerModule;
import com.axelor.meta.MetaScanner;
import com.axelor.meta.db.repo.MetaJsonReferenceUpdater;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.meta.loader.ViewObserver;
import com.axelor.meta.loader.ViewWatcherObserver;
import com.axelor.meta.service.ViewProcessor;
import com.axelor.meta.theme.MetaThemeService;
import com.axelor.meta.theme.MetaThemeServiceImpl;
import com.axelor.report.ReportEngineProvider;
import com.axelor.script.ScriptPolicyConfigurator;
import com.axelor.ui.QuickMenuCreator;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
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

    // Observe updates to fix m2o names in json values
    bind(MetaJsonReferenceUpdater.class);

    // Logger injection support
    install(new LoggerModule());

    // events support
    install(new EventModule());

    // Init QuickMenuCreator
    Multibinder.newSetBinder(binder(), QuickMenuCreator.class);

    // Hibernate listener configurator binder
    Multibinder.newSetBinder(binder(), HibernateListenerConfigurator.class);

    bind(AppSettingsObserver.class);
    bind(ViewWatcherObserver.class);

    bind(MetaThemeService.class).to(MetaThemeServiceImpl.class);

    final List<Class<? extends AxelorModule>> moduleClasses =
        ModuleManager.getResolution().stream()
            .flatMap(name -> MetaScanner.findSubTypesOf(name, AxelorModule.class).find().stream())
            .collect(Collectors.toList());

    if (!moduleClasses.isEmpty()) {
      log.info("Configuring app modules...");

      for (Class<? extends AxelorModule> module : moduleClasses) {
        try {
          log.debug("Configure module: {}", module.getName());
          install(module.getDeclaredConstructor().newInstance());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    // Configure view processors
    configureViewProcessors();

    // Configure script policy
    configureScriptPolicy();

    var cacheProviderInfo = CacheBuilder.getCacheProviderInfo();
    log.info("Cache provider: {}", cacheProviderInfo.getProvider());
  }

  private void configureViewProcessors() {
    Multibinder<ViewProcessor> viewProcessorBinder =
        Multibinder.newSetBinder(binder(), ViewProcessor.class);
    List<Class<? extends ViewProcessor>> viewProcessorClasses =
        ModuleManager.getResolution().stream()
            .flatMap(name -> MetaScanner.findSubTypesOf(name, ViewProcessor.class).find().stream())
            .toList();

    if (!viewProcessorClasses.isEmpty()) {
      log.atInfo()
          .setMessage("View processors: {}")
          .addArgument(
              () ->
                  viewProcessorClasses.stream()
                      .map(Class::getName)
                      .collect(Collectors.joining(", ")))
          .log();

      viewProcessorClasses.forEach(
          viewProcessor -> viewProcessorBinder.addBinding().to(viewProcessor));
    }
  }

  private void configureScriptPolicy() {
    Multibinder<ScriptPolicyConfigurator> scriptPolicyConfiguratorBinder =
        Multibinder.newSetBinder(binder(), ScriptPolicyConfigurator.class);

    List<Class<? extends ScriptPolicyConfigurator>> configuratorClasses =
        ModuleManager.getResolution().stream()
            .flatMap(
                name ->
                    MetaScanner.findSubTypesOf(name, ScriptPolicyConfigurator.class)
                        .find()
                        .stream())
            .toList();

    if (!configuratorClasses.isEmpty()) {
      log.atInfo()
          .setMessage("Script policy configurators: {}")
          .addArgument(
              () ->
                  configuratorClasses.stream()
                      .map(Class::getName)
                      .collect(Collectors.joining(", ")))
          .log();

      configuratorClasses.forEach(
          configurator -> scriptPolicyConfiguratorBinder.addBinding().to(configurator));
    }
  }
}
