/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2025 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.axelor.web;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
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

    startupEvent.fire(new StartupEvent());
    log.info("Ready to serve...");
  }

  @Override
  public void destroy() {
    try {
      shutdownEvent.fire(new ShutdownEvent());
      jobRunner.shutdown();
      ReportExecutor.shutdown();
      FileStoreFactory.shutdown();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
