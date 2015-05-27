/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2015 Axelor (<http://axelor.com>).
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.birt.report.model.api.IResourceLocator;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.datatools.connectivity.oda.flatfile.ResourceLocator;

import com.axelor.app.AppSettings;
import com.axelor.common.ClassUtils;

/**
 * This is a {@link ResourceLocator} that first searches external reports
 * directory for the resources and fallbacks to module resources if not found.
 *
 */
public class ReportResourceLocator implements IResourceLocator {

	public static final String CONFIG_REPORT_DIR = "axelor.report.dir";
	public static final String DEFAULT_REPORT_DIR = "{user.home}/axelor/reports";

	private Path searchPath;

	public ReportResourceLocator() {
		final String dir = AppSettings.get().getPath(CONFIG_REPORT_DIR,
				DEFAULT_REPORT_DIR);
		this.searchPath = Paths.get(dir);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public URL findResource(ModuleHandle moduleHandle, String fileName,
			int type, Map appContext) {
		return findResource(moduleHandle, fileName, type);
	}

	@Override
	public URL findResource(ModuleHandle moduleHandle, String fileName, int type) {

		Path path = searchPath;
		String sub = ".";

		switch (type) {
		case IResourceLocator.LIBRARY:
			sub = "lib";
			break;
		case IResourceLocator.IMAGE:
			sub = "img";
			break;
		case IResourceLocator.CASCADING_STYLE_SHEET:
			sub = "css";
			break;
		}

		path = path.resolve(sub).normalize();

		// first search in the external repository
		File found = path.resolve(fileName).toFile();

		if (!found.exists()) {
			found = searchPath.resolve(fileName).toFile();
		}

		if (found.exists()) {
			try {
				return found.toURI().toURL();
			} catch (MalformedURLException e) {
			}
		}

		// otherwise locate from the modules
		path = Paths.get("reports", sub, fileName).normalize();

		return ClassUtils.getResource(path.toString());
	}

}
