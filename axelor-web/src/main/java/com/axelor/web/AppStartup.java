/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.web;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.app.leader.LeaderElectionWorker;
import com.axelor.cache.redisson.RedissonProvider;
import com.axelor.event.Event;
import com.axelor.events.ShutdownEvent;
import com.axelor.events.StartupEvent;
import com.axelor.file.store.FileStoreFactory;
import com.axelor.meta.loader.ModuleManager;
import com.axelor.quartz.JobRunner;
import com.axelor.report.tool.ReportExecutor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import org.slf4j.Logger;

@Singleton
public class AppStartup extends HttpServlet {

  private static final long serialVersionUID = -2493577642638670615L;

  @Inject private Logger log;

  @Inject private ModuleManager moduleManager;

  @Inject private JobRunner jobRunner;

  @Inject private Event<StartupEvent> startupEvent;

  @Inject private Event<ShutdownEvent> shutdownEvent;

  @Override
  public void init() throws ServletException {
    log.info("Initializing...");
    try {
      moduleManager.initialize(
          false, AppSettings.get().getBoolean(AvailableAppSettings.DATA_IMPORT_DEMO_DATA, true));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    try {
      if (jobRunner.isEnabled()) {
        jobRunner.init();
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    LeaderElectionWorker.getInstance().start();
    startupEvent.fire(new StartupEvent());
    log.info("Ready to serve...");
  }

  @Override
  public void destroy() {
    try {
      shutdownEvent.fire(new ShutdownEvent());
      jobRunner.shutdown();
      LeaderElectionWorker.getInstance().stop();
      ReportExecutor.shutdown();
      FileStoreFactory.shutdown();
      RedissonProvider.shutdown();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
