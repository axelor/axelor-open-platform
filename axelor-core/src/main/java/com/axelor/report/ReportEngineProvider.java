/*
 * SPDX-FileCopyrightText: Axelor <https://axelor.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.axelor.report;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import com.lowagie.text.FontFactory;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.io.File;
import java.net.MalformedURLException;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.ReportEngine;

@Singleton
public class ReportEngineProvider implements Provider<IReportEngine> {

  private IReportEngine engine;

  private IReportEngine init() {
    EngineConfig config = new EngineConfig();
    String fontConfig = AppSettings.get().getPath(AvailableAppSettings.REPORTS_FONTS_CONFIG, null);

    config.setResourceLocator(new ReportResourceLocator());

    if (StringUtils.notBlank(fontConfig)) {
      try {
        config.setFontConfig(new File(fontConfig).toURI().toURL());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    } else {
      // register default fontsConfig.xml
      config.setFontConfig(ResourceUtils.getResource("/com/axelor/report/fonts/fontsConfig.xml"));
      // register default fonts
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSans.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSans-Bold.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSans-Oblique.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSans-BoldOblique.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSerif.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSerif-Bold.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSerif-Italic.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSerif-BoldItalic.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSans.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSansMono-Bold.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSansMono-Oblique.ttf");
      FontFactory.register("/com/axelor/report/fonts/dejavu/DejaVuSansMono-BoldOblique.ttf");
    }

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
