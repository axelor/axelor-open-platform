/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
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
package com.axelor.meta;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.axelor.common.reflections.Reflections;
import com.axelor.meta.loader.ModuleManager;
import com.google.common.collect.ImmutableList;

public class MetaScanner {
	
	private MetaScanner() {
		
	}

	public static ImmutableList<URL> findAll(String regex) {
		return Reflections.findResources().byName(regex).find();
	}
	
	public static ImmutableList<URL> findAll(String module, String directory, String pattern) {

		String path = ModuleManager.getModulePath(module);
		if (path == null) {
			return ImmutableList.of();
		}

		String namePattern = "(^|/|\\\\)" + directory + "(/|\\\\)" + pattern;
		String pathPattern = String.format("^%s", path.replaceFirst("module\\.properties$", ""));

		Path parent = Paths.get(path).getParent();
		Path resources = parent.resolve("../../resources/main").normalize();

		if (Files.exists(resources)) {
			pathPattern = String.format("(%s)|(^%s)", pathPattern, resources);
		}
		return Reflections.findResources().byName(namePattern).byURL(pathPattern).find();
	}
}
