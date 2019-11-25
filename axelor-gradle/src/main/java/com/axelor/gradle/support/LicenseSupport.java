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
package com.axelor.gradle.support;

import com.axelor.common.FileUtils;
import java.io.File;
import java.util.Calendar;
import nl.javadude.gradle.plugins.license.License;
import nl.javadude.gradle.plugins.license.LicenseExtension;
import nl.javadude.gradle.plugins.license.LicensePlugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;

public class LicenseSupport extends AbstractSupport {

  private File findHeaderFile(Project project) {
    final String[] paths = {
      ".", "..", project.getRootDir().getPath(), project.getRootDir() + "/src/license"
    };
    for (String path : paths) {
      final File file = project.file(path + "/header.txt");
      if (file.exists()) {
        return file;
      }
    }
    return project.file("src/license/header.txt");
  }

  @Override
  public void apply(Project project) {
    final File header = findHeaderFile(project);
    final boolean headerExists = header != null && header.exists();
    project.getPlugins().apply(LicensePlugin.class);

    final LicenseExtension license = project.getExtensions().getByType(LicenseExtension.class);
    final ExtraPropertiesExtension ext =
        ((ExtensionAware) license).getExtensions().getExtraProperties();

    ext.set("product", "Axelor Business Solutions");
    ext.set("inception", "2005");
    ext.set("year", Calendar.getInstance().get(Calendar.YEAR));
    ext.set("owner", "Axelor");
    ext.set("website", "http://axelor.com");

    license.setHeader(header);
    license.setIgnoreFailures(true);

    license.mapping("java", "SLASHSTAR_STYLE");

    license.include("**/*.java");
    license.include("**/*.groovy");
    license.include("**/*.scala");
    license.include("**/*.js");
    license.include("**/*.css");
    license.include("**/*.jsp");

    license.exclude("**/LICENSE");
    license.exclude("**/LICENSE.md");
    license.exclude("**/README");
    license.exclude("**/README.md");
    license.exclude("**/*.properties");
    license.exclude("**/*.txt");
    license.exclude("**/*.json");

    license.exclude("**/src-gen/**");
    license.exclude("**/data-init/**");
    license.exclude("**/data-demo/**");
    license.exclude("**/resources/**");
    license.exclude("**/webapp/lib/**");
    license.exclude("**/webapp/lib/**");
    license.exclude("**/webapp/dist/**");
    license.exclude("**/webapp/node_modules/**");
    license.exclude("**/webapp/WEB-INF/web.xml");

    final File src = FileUtils.getFile(project.getProjectDir(), "src");
    final File webapp = FileUtils.getFile(project.getProjectDir(), "src", "main", "webapp");

    project.afterEvaluate(
        p ->
            p.getTasks()
                .withType(License.class)
                .all(
                    task -> {
                      task.setEnabled(headerExists);
                      task.setSource(project.fileTree(src));
                      task.source(
                          project.fileTree(
                              webapp,
                              tree -> {
                                tree.exclude("lib/**");
                                tree.exclude("dist/**");
                                tree.exclude("node_modules/**");
                                tree.exclude("WEB-INF/web.xml");
                              }));
                    }));
  }
}
