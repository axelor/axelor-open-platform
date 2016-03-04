/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2005-2016 Axelor (<http://axelor.com>).
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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.axelor.common.reflections.ClassFinder;
import com.axelor.common.reflections.Reflections;
import com.axelor.meta.loader.ModuleManager;
import com.google.common.collect.ImmutableList;

/**
 * This class provides some utility methods to scan classpath for
 * resources/classes.
 *
 */
public class MetaScanner {
	
	private MetaScanner() {
		
	}

	/**
	 * Find all resources matched by the given pattern.
	 * 
	 * @param pattern
	 *            the resource name pattern to match
	 * @return list of resources matched
	 */
	public static ImmutableList<URL> findAll(String pattern) {
		return Reflections.findResources(loader()).byName(pattern).find();
	}

	/**
	 * Find all resources within a directory of the given module matching the
	 * pattern.
	 * 
	 * @param module
	 *            the module name
	 * @param directory
	 *            the directory name
	 * @param pattern
	 *            the resource name pattern to match
	 * @return list of resources matched
	 */
	public static ImmutableList<URL> findAll(String module, String directory, String pattern) {

		URL pathUrl = ModuleManager.getModulePath(module);
		if (pathUrl == null) {
			return ImmutableList.of();
		}

		String path = pathUrl.getPath();
		String pathPattern = String.format("^%s", path.replaceFirst("module\\.properties$", ""));
		String namePattern = "(^|/|\\\\)" + directory + "(/|\\\\)" + pattern;

		try {
			Path parent = Paths.get(path).getParent();
			Path resources = parent.resolve("../../resources/main").normalize();
			if (Files.exists(resources)) {
				pathPattern = String.format("(%s)|(^%s)", pathPattern, resources);
			}
		} catch (Exception e) {
		}

		return Reflections.findResources().byName(namePattern).byURL(pathPattern).find();
	}

	/**
	 * Find module path URLs as in current classpath.
	 * 
	 */
	public static URL[] findURLs() {
		final List<URL> path = new ArrayList<>();
		final URLClassLoader loader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
		for (URL item : loader.getURLs()) {
			final URL[] urls = { item };
			try (final URLClassLoader cl = new URLClassLoader(urls)) {
				URL res = cl.findResource("module.properties");
				if (res == null) {
					res = cl.findResource("application.properties");
				}
				if (res != null || Paths.get(item.getPath()).endsWith("build/classes/test/")) {
					path.add(item);
				}
			} catch (IOException e) {
			}
		}
		return path.toArray(new URL[]{});
	}

	private static ClassLoader loader() {
		final ClassLoader loader = Thread.currentThread().getContextClassLoader();
		return new URLClassLoader(findURLs(), null) {
			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				return loader.loadClass(name);
			}
		};
	}

	/**
	 * Delegates to {@link Reflections#findSubTypesOf(Class, ClassLoader)}
	 * method and uses custom {@link ClassLoader} to speedup searching.
	 * 
	 * This method search within module jar/directories only.
	 * 
	 * @see Reflections#findSubTypesOf(Class)
	 */
	public static <T> ClassFinder<T> findSubTypesOf(Class<T> type) {
		return Reflections.findSubTypesOf(type, loader());
	}

	/**
	 * Delegates to {@link Reflections#findTypes(ClassLoader)}
	 * method and uses custom {@link ClassLoader} to speedup searching.
	 * 
	 * This method search within module jar/directories only.
	 * 
	 * @see Reflections#findTypes()
	 */
	public static ClassFinder<?> findTypes() {
		return Reflections.findTypes(loader());
	}
}
