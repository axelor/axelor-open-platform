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
package com.axelor.app.internal;

import com.axelor.app.AppSettings;
import com.axelor.common.logging.LoggerConfiguration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;

/**
 * This class is used to launch app from IDE.
 * 
 * <p>
 * It will locate <code>axelor-tomcat.jar</code> and launch application using
 * isolated class loader with hotswap-agent support.
 * </p>
 * 
 */
public final class AppRunner {

	private static class FilteringClassLoader extends URLClassLoader {

		private static final ClassLoader EXT_LOADER = ClassLoader.getSystemClassLoader().getParent();

		private static final String[] ALLOWED_PACKAGES = { "org.hotswap" };

		public FilteringClassLoader(ClassLoader parent) {
			super(new URL[0], parent);
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			try {
				return EXT_LOADER.loadClass(name);
			} catch (ClassNotFoundException ignore) {
			}

			if (!classAllowed(name)) {
				throw new ClassNotFoundException(name);
			}

			Class<?> cl = super.loadClass(name, false);
			if (resolve) {
				resolveClass(cl);
			}

			return cl;
		}

		private boolean classAllowed(String className) {
			for (String prefix : ALLOWED_PACKAGES) {
				if (className.startsWith(prefix)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public URL getResource(String name) {
			return EXT_LOADER.getResource(name);
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return EXT_LOADER.getResources(name);
		}
	}

	private static void run(String[] args) {
		final Path runnerJar = Paths.get("build", "tomcat", "axelor-tomcat.jar");
		final List<URL> urls = new ArrayList<>();

		try (JarFile jar = new JarFile(runnerJar.toFile())) {
			String cp = jar.getManifest().getMainAttributes().getValue("Class-Path");
			for (String s : cp.split(" ")) {
				urls.add(Paths.get(s.trim()).toUri().toURL());
			}
		} catch (IOException e) {
			System.err.println("invalid jar: " + runnerJar);
			System.err.println("please run 'runnerJar' grade task");
			return;
		}

		ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
		ClassLoader parentClassLoader = systemClassLoader.getParent();

		for (URL url : ((URLClassLoader) systemClassLoader).getURLs()) {
			if (url.toString().contains("hotswap-agent")) {
				urls.add(url);
				parentClassLoader = new FilteringClassLoader(systemClassLoader);
				break;
			}
		}

		try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[] {}), parentClassLoader)) {
			Thread.currentThread().setContextClassLoader(loader);
			Class<?> runnerClass = loader.loadClass("com.axelor.tomcat.TomcatRunner");
			Method runnerMain = runnerClass.getMethod("main", String[].class);
			runnerMain.invoke(null, (Object) args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		LoggerConfiguration conf = new LoggerConfiguration(AppSettings.get().getProperties());
		conf.install();
		try {
			run(args);
		} finally {
			conf.uninstall();
		}
	}
}
