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
package com.axelor.report;

import com.axelor.app.AppSettings;
import com.axelor.app.AvailableAppSettings;
import com.axelor.common.FileUtils;
import com.axelor.common.ResourceUtils;
import com.axelor.db.internal.DBHelper;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;
import org.eclipse.birt.report.model.api.IResourceLocator;
import org.eclipse.birt.report.model.api.ModuleHandle;

/**
 * This is a {@link IResourceLocator} that first searches external reports directory for the
 * resources and falls back to module resources if not found.
 */
public class ReportResourceLocator implements IResourceLocator {

  public static final String DEFAULT_REPORT_DIR = "{user.home}/axelor/reports";

  private static final Pattern URL_PATTERN = Pattern.compile("^(file|jar|http|https|ftp):/.*");

  private Path searchPath;

  public ReportResourceLocator() {
    final String dir =
        AppSettings.get().getPath(AvailableAppSettings.REPORTS_DESIGN_DIR, DEFAULT_REPORT_DIR);
    this.searchPath = Path.of(dir);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public URL findResource(ModuleHandle moduleHandle, String fileName, int type, Map appContext) {
    return findResource(moduleHandle, fileName, type);
  }

  @Override
  public URL findResource(ModuleHandle moduleHandle, String fileName, int type) {
    if (DBHelper.isOracle() && fileName.endsWith(".rptlibrary")) {
      final URL found =
          find(moduleHandle, fileName.replace(".rptlibrary", ".oracle.rptlibrary"), type);
      if (found != null) {
        return found;
      }
    }
    return find(moduleHandle, fileName, type);
  }

  private URL find(ModuleHandle moduleHandle, String fileName, int type) {

    final String subDir;

    switch (type) {
      case IResourceLocator.LIBRARY:
        subDir = "lib";
        break;
      case IResourceLocator.IMAGE:
        subDir = "img";
        break;
      case IResourceLocator.CASCADING_STYLE_SHEET:
        subDir = "css";
        break;
      default:
        subDir = ".";
        break;
    }

    // if already an url
    if (URL_PATTERN.matcher(fileName).matches()) {
      try {
        return URI.create(fileName).toURL();
      } catch (MalformedURLException | IllegalArgumentException e) {
      }
    }

    // first resolve absolute path
    File found = new File(fileName);

    // next search in the top directory
    if (!found.exists()) {
      found = searchPath.resolve(fileName).toFile();
    }

    // else search in sub directory
    if (!found.exists()) {
      found = searchPath.resolve(subDir).normalize().resolve(fileName).toFile();
    }

    if (found.exists()) {
      try {
        return found.toURI().toURL();
      } catch (MalformedURLException e) {
      }
    }

    // otherwise locate from the modules
    URL url =
        ResourceUtils.getResource(
            FileUtils.getFile("reports", fileName).getPath().replace("\\", "/"));
    if (url == null) {
      url =
          ResourceUtils.getResource(
              FileUtils.getFile("reports", subDir, fileName).getPath().replace("\\", "/"));
    }

    return url;
  }
}
