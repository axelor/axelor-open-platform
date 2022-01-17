/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2022 Axelor (<http://axelor.com>).
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
import com.hierynomus.gradle.license.LicenseBasePlugin;
import com.hierynomus.gradle.license.LicenseReportingPlugin;
import com.hierynomus.gradle.license.tasks.LicenseCheck;
import com.hierynomus.gradle.license.tasks.LicenseFormat;
import java.io.File;
import java.util.Calendar;
import nl.javadude.gradle.plugins.license.License;
import nl.javadude.gradle.plugins.license.LicenseExtension;
import nl.javadude.gradle.plugins.license.LicensePlugin;
import nl.javadude.gradle.plugins.license.PluginHelper;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaBasePlugin;

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
    project.getPlugins().apply(AxelorLicensePlugin.class);

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
    license.mapping("scss", "JAVADOC_STYLE");

    license.include("**/*.java");
    license.include("**/*.groovy");
    license.include("**/*.scala");
    license.include("**/*.js");
    license.include("**/*.css");
    license.include("**/*.scss");
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
    license.exclude("**/webapp/dist/**");
    license.exclude("**/webapp/node_modules/**");
    license.exclude("**/webapp/WEB-INF/web.xml");

    final File src = FileUtils.getFile(project.getProjectDir(), "src");
    final File webapp = FileUtils.getFile(project.getProjectDir(), "src", "main", "webapp");

    project.afterEvaluate(
        p ->
            project
                .getTasks()
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

  /** License checking that is not added to check lifecycle */
  static class AxelorLicensePlugin extends LicensePlugin {

    @Override
    public void apply(Project project) {
      project.getPlugins().apply(LicenseBasePlugin.class);
      project.getPlugins().apply(LicenseReportingPlugin.class);

      baseCheckTask = project.task(LicenseBasePlugin.getLICENSE_TASK_BASE_NAME());
      baseFormatTask = project.task(LicenseBasePlugin.getFORMAT_TASK_BASE_NAME());

      baseCheckTask.setGroup("License");
      baseFormatTask.setGroup(baseCheckTask.getGroup());
      baseCheckTask.setDescription("Checks for header consistency.");
      baseFormatTask.setDescription(
          "Applies the license found in the header file in files missing the header.");

      project.getPlugins().withType(JavaBasePlugin.class, plugin -> linkLicenseTasks(project));
      PluginHelper.withAndroidPlugin(project, plugin -> linkLicenseTasks(project));
    }

    private void linkLicenseTasks(Project project) {
      project.getTasks().withType(LicenseCheck.class, lt -> baseCheckTask.dependsOn(lt));
      project.getTasks().withType(LicenseFormat.class, lt -> baseFormatTask.dependsOn(lt));
    }
  }
}
