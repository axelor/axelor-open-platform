/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2021 Axelor (<http://axelor.com>).
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

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.ResourceUtils;
import com.axelor.common.StringUtils;
import com.lowagie.text.FontFactory;
import java.io.File;
import java.net.MalformedURLException;
import javax.inject.Provider;
import javax.inject.Singleton;
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
