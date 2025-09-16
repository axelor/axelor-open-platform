/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.report;

import com.axelor.JpaTestModule;
import org.eclipse.birt.report.engine.api.IReportEngine;

public class MyModule extends JpaTestModule {

  @Override
  protected void configure() {
    super.configure();
    bind(IReportEngine.class).toProvider(ReportEngineProvider.class);
  }
}
