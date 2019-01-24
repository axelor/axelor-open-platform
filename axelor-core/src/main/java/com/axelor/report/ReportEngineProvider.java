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
package com.axelor.report;

import javax.inject.Provider;
import javax.inject.Singleton;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.ReportEngine;

@Singleton
public class ReportEngineProvider implements Provider<IReportEngine> {

  private IReportEngine engine;

  private IReportEngine init() {
    final EngineConfig config = new EngineConfig();
    config.setResourceLocator(new ReportResourceLocator());
    return new ReportEngine(config);
  }

  @Override
  public IReportEngine get() {
    if (engine == null) {
      engine = init();
    }
    return engine;
  }
}
