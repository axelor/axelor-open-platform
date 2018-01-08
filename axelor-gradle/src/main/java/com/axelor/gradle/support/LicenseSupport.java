/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2017 Axelor (<http://axelor.com>).
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

import java.io.File;
import java.util.Calendar;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import com.axelor.gradle.AxelorPlugin;
import com.axelor.gradle.tasks.GenerateCode;

import nl.javadude.gradle.plugins.license.License;
import nl.javadude.gradle.plugins.license.LicenseExtension;
import nl.javadude.gradle.plugins.license.LicensePlugin;

public class LicenseSupport extends AbstractSupport {

	private File findHeaderFile(Project project) {
		final String[] paths = {
			".",
			"..",
			project.getRootDir().getPath(),
			project.getRootDir() + "/src/license"
		};
		for (String path : paths) {
			final File file = project.file(path + "/header.txt");
			if (file.exists()) {
				return file;
			}
		}
		return project.file("src/license/header.txt");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void apply(Project project) {
		final File header = findHeaderFile(project);
		project.getPlugins().apply(LicensePlugin.class);
		
		project.getTasks().create("licenseFormatGenerated", License.class, task -> {
			final ConfigurableFileTree source = project.fileTree(GenerateCode.getJavaOutputDir(project));
			source.include("**/*.java");
			source.include("**/*.groovy");
			if (AxelorPlugin.GRADLE_VERSION_3_X) {
				task.setSource((Object) source);
			}
			task.getLogging().setLevel(LogLevel.QUIET);
			task.onlyIf(spec -> header != null && header.exists());
		});
		
		// format generated code with license header
		project.getTasks().withType(GenerateCode.class).all(task -> task.finalizedBy("licenseFormatGenerated"));

		final LicenseExtension license = project.getExtensions().getByType(LicenseExtension.class);

		license.setHeader(header);
		license.setIgnoreFailures(true);

		license.mapping("java", "SLASHSTAR_STYLE");
		
		license.include("**/*.java");
		license.include("**/*.groovy");
		license.include("**/*.scala");
		license.include("**/*.js");
		license.include("**/*.css");
		
		license.exclude("**/LICENSE");
		license.exclude("**/LICENSE.md");
		license.exclude("**/README");
		license.exclude("**/README.md");
		license.exclude("**/*.properties");
		license.exclude("**/*.txt");
		license.exclude("**/*.json");
		
		license.exclude("**/data-init/**");
		license.exclude("**/data-demo/**");
		license.exclude("**/resources/**");
		license.exclude("**/webapp/lib/**");
		license.exclude("**/webapp/WEB-INF/web.xml");

		final ExtraPropertiesExtension ext = ((ExtensionAware) license).getExtensions().getExtraProperties();

		ext.set("product", "Axelor Business Solutions");
		ext.set("inception", "2005");
		ext.set("year", Calendar.getInstance().get(Calendar.YEAR));
		ext.set("owner", "Axelor");
		ext.set("website", "http://axelor.com");
		
		project.afterEvaluate(p -> {
			project.getTasks().withType(License.class).all(task -> task.onlyIf(spec -> header != null && header.exists()));
		});
	}
}
