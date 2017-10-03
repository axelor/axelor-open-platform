/**
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
package com.axelor.report;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.birt.report.model.api.IResourceLocator;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.datatools.connectivity.oda.flatfile.ResourceLocator;

import com.axelor.app.AppSettings;
import com.axelor.common.ClassUtils;
import com.axelor.common.FileUtils;

/**
 * This is a {@link ResourceLocator} that first searches external reports
 * directory for the resources and fallbacks to module resources if not found.
 *
 */
public class ReportResourceLocator implements IResourceLocator {

	public static final String CONFIG_REPORT_DIR = "axelor.report.dir";
	public static final String DEFAULT_REPORT_DIR = "{user.home}/axelor/reports";

	private static final Pattern URL_PATTERN = Pattern.compile("^(file|jar|http|https|ftp):/.*");

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

		// if already an url
		if (URL_PATTERN.matcher(fileName).matches()) {
			try {
				return new URL(fileName);
			} catch (MalformedURLException e) {
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
			found = searchPath.resolve(sub).normalize().resolve(fileName).toFile();
		}

		if (found.exists()) {
			try {
				return found.toURI().toURL();
			} catch (MalformedURLException e) {
			}
		}

		// otherwise locate from the modules
		URL url = ClassUtils.getResource(FileUtils.getFile("reports", fileName).getPath().replace("\\", "/"));
		if (url == null) {
			url = ClassUtils.getResource(FileUtils.getFile("reports", sub, fileName).getPath().replace("\\", "/"));
		}

		return url;
	}

}
